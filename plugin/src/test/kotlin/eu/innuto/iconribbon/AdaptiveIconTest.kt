package eu.innuto.iconribbon

import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AdaptiveIconTest {

    @Test
    fun `swaps the foreground reference and keeps the other layers`() {
        val icon = parse(
            """
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@color/ic_launcher_background"/>
                <foreground android:drawable="@drawable/ic_launcher_foreground"/>
                <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
            </adaptive-icon>
            """,
        )

        val rewritten = icon.withForeground("@drawable/ic_launcher_ribbon_foreground")

        assertContains(rewritten, """android:drawable="@drawable/ic_launcher_ribbon_foreground"""")
        assertContains(rewritten, """android:drawable="@color/ic_launcher_background"""")
        assertContains(rewritten, """android:drawable="@drawable/ic_launcher_monochrome"""")
        assertFalse(rewritten.contains("\"@drawable/ic_launcher_foreground\""))
    }

    @Test
    fun `layer list stacks the overlay over a referenced foreground`() {
        val icon = parse(
            """
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
            </adaptive-icon>
            """,
        )

        val layerList = icon.foregroundLayerList("@drawable/icon_ribbon_overlay")

        val original = layerList.indexOf("@mipmap/ic_launcher_foreground")
        val overlay = layerList.indexOf("@drawable/icon_ribbon_overlay")
        assert(original in 0 until overlay) { layerList }
    }

    @Test
    fun `layer list keeps an inline foreground drawable and its namespaces`() {
        val icon = parse(
            """
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:aapt="http://schemas.android.com/aapt">
                <foreground>
                    <inset android:inset="16dp" android:drawable="@drawable/logo"/>
                </foreground>
            </adaptive-icon>
            """,
        )

        val layerList = icon.foregroundLayerList("@drawable/icon_ribbon_overlay")

        assertContains(layerList, "<inset")
        assertContains(layerList, """xmlns:aapt="http://schemas.android.com/aapt"""")
    }

    @Test
    fun `non adaptive xml is not parsed`() {
        assertNull(AdaptiveIcon.parseOrNull(file("""<vector xmlns:android="http://schemas.android.com/apk/res/android"/>""")))
    }

    private fun parse(xml: String) = assertNotNull(AdaptiveIcon.parseOrNull(file(xml)))

    private fun file(xml: String): File =
        createTempFile(suffix = ".xml").toFile().apply {
            writeText(xml.trimIndent())
            deleteOnExit()
        }
}
