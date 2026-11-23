package eu.innuto.iconribbon

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IconRibbonPluginFunctionalTest {

    @TempDir
    lateinit var dir: File

    private val project by lazy { AndroidTestProject(dir) }

    @Test
    fun `decorates raster and adaptive icons of the variants that have ribbons`() {
        project.buildScript(
            """
            buildTypes {
                debug { iconRibbon { } }
            }
            flavorDimensions += "environment"
            productFlavors {
                create("dev")
                create("staging") {
                    iconRibbon { text = "STAGE"; position = RibbonPosition.BOTTOM }
                }
                create("prod") {
                    iconRibbon { enabled = false }
                }
            }
            """,
        )
        // A flavor override of the raster icon: the ribbon must be drawn on this one.
        project.icon("src/staging/res/mipmap-xxxhdpi/ic_launcher.png", Color.GREEN)

        val result = project.build("tasks", "--all", "generateDevDebugIconRibbon", "generateStagingDebugIconRibbon")

        // devDebug: DEBUG, stagingDebug: STAGE + DEBUG, stagingRelease: STAGE; prod and devRelease: nothing.
        listOf("DevDebug", "StagingDebug", "StagingRelease").forEach { assertContains(result.output, "generate${it}IconRibbon") }
        listOf("DevRelease", "ProdDebug", "ProdRelease").forEach { assertFalse("generate${it}IconRibbon" in result.output, it) }
        assertContains(result.output, "Draws the STAGE, DEBUG ribbon(s) over the launcher icons of stagingDebug.")

        val output = project.generated("stagingDebug")
        listOf(
            "mipmap-xxxhdpi/ic_launcher.png",
            "mipmap-xxxhdpi/ic_launcher_round.png",
            "mipmap-anydpi-v26/ic_launcher.xml",
            "drawable/ic_launcher_ribbon_foreground_mipmap_anydpi_v26.xml",
            "drawable-nodpi/icon_ribbon_overlay.png",
        ).forEach { assertTrue(File(output, it).isFile, "missing $it") }
        assertContains(
            File(output, "mipmap-anydpi-v26/ic_launcher.xml").readText(),
            "@drawable/ic_launcher_ribbon_foreground_mipmap_anydpi_v26",
        )

        val stagingIcon = ImageIO.read(File(output, "mipmap-xxxhdpi/ic_launcher.png"))
        val mainIcon = ImageIO.read(File(output, "mipmap-xxxhdpi/ic_launcher_round.png"))
        // Left edge, mid height: away from both the BOTTOM and the TOP_RIGHT ribbon.
        assertEquals(Color.GREEN.rgb, stagingIcon.getRGB(2, 96), "staging override decorated")
        assertEquals(Color.BLUE.rgb, mainIcon.getRGB(2, 96), "main icon decorated")
        // Top-right corner area is covered by the DEBUG ribbon.
        assertFalse(stagingIcon.getRGB(192 - 41, 41) == Color.GREEN.rgb, "DEBUG ribbon drawn")
    }

    @Test
    fun `ribbons of one variant at the same position fail the build`() {
        project.buildScript(
            """
            buildTypes {
                debug { iconRibbon { } }
            }
            flavorDimensions += "environment"
            productFlavors {
                create("staging") { iconRibbon { } }
            }
            """,
        )

        val result = project.buildAndFail("help")

        assertContains(result.output, "stagingDebug would draw 'staging' and 'debug' on top of each other at TOP_RIGHT")
    }

    @Test
    fun `invalid color fails during configuration naming its owner`() {
        project.buildScript(
            """
            buildTypes {
                debug { iconRibbon { backgroundColor = "red" } }
            }
            """,
        )

        val result = project.buildAndFail("help")

        assertContains(result.output, "color must be #RRGGBB or #AARRGGBB, was 'red' (in iconRibbon of 'debug')")
    }

    @Test
    fun `iconNames are set module wide and replaced per flavor`() {
        project.buildScript(
            android = """
            buildTypes {
                debug { iconRibbon { } }
            }
            flavorDimensions += "environment"
            productFlavors {
                create("dev")
                create("staging") {
                    iconRibbon { iconNames = listOf("ic_launcher_round"); position = RibbonPosition.BOTTOM }
                }
            }
            """,
            topLevel = """
            iconRibbon {
                iconNames = listOf("ic_launcher")
            }
            """,
        )

        project.build("generateDevDebugIconRibbon", "generateStagingDebugIconRibbon")

        assertEquals(
            setOf("ic_launcher.png", "ic_launcher.xml", "ic_launcher_ribbon_foreground_mipmap_anydpi_v26.xml", "icon_ribbon_overlay.png"),
            project.generated("devDebug").walk().filter { it.isFile }.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("ic_launcher_round.png"),
            project.generated("stagingDebug").walk().filter { it.isFile }.map { it.name }.toSet(),
        )
    }

    @Test
    fun `invalid iconNames fail naming their owner`() {
        project.buildScript(
            """
            buildTypes {
                debug { iconRibbon { iconNames = listOf("@mipmap/ic_launcher") } }
            }
            """,
        )

        val result = project.buildAndFail("help")

        assertContains(result.output, "iconNames of 'debug' must be plain resource names")
    }

    @Test
    fun `works with the Groovy DSL`() {
        project.file(
            "build.gradle",
            """
            plugins {
                id 'com.android.application'
                id 'eu.innuto.iconribbon'
            }

            android {
                namespace 'com.example.app'
                compileSdk 35
                defaultConfig { minSdk 24 }
                buildTypes {
                    debug {
                        iconRibbon { text = 'DBG'; backgroundColor = '#1565C0' }
                    }
                }
            }
            """.trimIndent(),
        )

        val result = project.build("generateDebugIconRibbon")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateDebugIconRibbon")?.outcome)
        assertTrue(File(project.generated("debug"), "mipmap-xxxhdpi/ic_launcher.png").isFile)
    }

    @Test
    fun `is configuration cache compatible and up to date on a second run`() {
        project.buildScript(
            """
            buildTypes {
                debug { iconRibbon { } }
            }
            """,
        )

        val first = project.build("generateDebugIconRibbon", "--configuration-cache")
        val second = project.build("generateDebugIconRibbon", "--configuration-cache")

        assertContains(first.output, "Configuration cache entry stored.")
        assertContains(second.output, "Configuration cache entry reused.")
        assertEquals(TaskOutcome.SUCCESS, first.task(":generateDebugIconRibbon")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":generateDebugIconRibbon")?.outcome)
    }
}
