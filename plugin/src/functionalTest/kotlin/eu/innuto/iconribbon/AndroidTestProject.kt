package eu.innuto.iconribbon

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * A minimal Android app in [dir] with launcher icons in `src/main/res`:
 * a blue raster `ic_launcher` / `ic_launcher_round` and an adaptive `ic_launcher`.
 */
class AndroidTestProject(val dir: File) {

    init {
        file("settings.gradle.kts", SETTINGS)
        file("gradle.properties", "org.gradle.jvmargs=-Xmx1g\n")
        file("local.properties", "sdk.dir=${androidSdk().replace("\\", "\\\\")}\n")
        file("src/main/AndroidManifest.xml", "<manifest/>\n")
        listOf("ic_launcher", "ic_launcher_round").forEach { icon("src/main/res/mipmap-xxxhdpi/$it.png", Color.BLUE) }
        file("src/main/res/mipmap-anydpi-v26/ic_launcher.xml", ADAPTIVE_ICON)
        file("src/main/res/drawable/ic_launcher_foreground.xml", VECTOR)
    }

    /** `build.gradle.kts` with [android] inside `android { }` and [topLevel] after it. */
    fun buildScript(android: String, topLevel: String = "") = file(
        "build.gradle.kts",
        """
        |import eu.innuto.iconribbon.RibbonPosition
        |import eu.innuto.iconribbon.iconRibbon
        |
        |plugins {
        |    id("com.android.application")
        |    id("eu.innuto.iconribbon")
        |}
        |
        |android {
        |    namespace = "com.example.app"
        |    compileSdk = 35
        |    defaultConfig { minSdk = 24 }
        |${android.trimIndent().prependIndent("    ")}
        |}
        |
        |${topLevel.trimIndent()}
        |""".trimMargin(),
    )

    fun file(path: String, content: String): File =
        File(dir, path).apply {
            parentFile.mkdirs()
            writeText(content)
        }

    fun icon(path: String, color: Color, size: Int = 192): File =
        File(dir, path).apply {
            parentFile.mkdirs()
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            image.createGraphics().apply {
                this.color = color
                fillRect(0, 0, size, size)
                dispose()
            }
            ImageIO.write(image, "png", this)
        }

    fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    fun buildAndFail(vararg arguments: String): BuildResult = runner(*arguments).buildAndFail()

    /** Generated res folder of [variant], e.g. `stagingDebug`. */
    fun generated(variant: String): File =
        File(dir, "build/generated/res/generate${variant.replaceFirstChar(Char::uppercase)}IconRibbon")

    private fun runner(vararg arguments: String) = GradleRunner.create()
        .withProjectDir(dir)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")
        .forwardOutput()
        .apply { System.getProperty("testGradleVersion")?.let { withGradleVersion(it) } }

    private companion object {
        fun androidSdk(): String = System.getProperty("androidSdk")
            ?: error("Set ANDROID_HOME or sdk.dir in local.properties to run the functional tests.")

        val SETTINGS = """
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "test-app"
        """.trimIndent()

        val ADAPTIVE_ICON = """
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@android:color/white"/>
                <foreground android:drawable="@drawable/ic_launcher_foreground"/>
            </adaptive-icon>
        """.trimIndent()

        val VECTOR = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="108dp" android:height="108dp"
                android:viewportWidth="108" android:viewportHeight="108">
                <path android:fillColor="#000000" android:pathData="M36,36h36v36h-36z"/>
            </vector>
        """.trimIndent()
    }
}
