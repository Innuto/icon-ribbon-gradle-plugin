package eu.innuto.iconribbon

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GenerateIconRibbonWorkTest {

    private fun name(path: String) = GenerateIconRibbonWork.foregroundDrawableName(File(path))

    @Test
    fun `foreground name includes the source folder`() {
        assertEquals("ic_launcher_ribbon_foreground_mipmap_anydpi_v26", name("res/mipmap-anydpi-v26/ic_launcher.xml"))
    }

    @Test
    fun `same icon in different folders gets different foreground names`() {
        assertNotEquals(name("res/mipmap-anydpi-v26/ic_launcher.xml"), name("res/mipmap-anydpi/ic_launcher.xml"))
        assertNotEquals(name("res/mipmap-anydpi-v26/ic_launcher.xml"), name("res/drawable-anydpi-v26/ic_launcher.xml"))
    }

    @Test
    fun `qualifiers are turned into a valid resource name`() {
        assertEquals("ic_launcher_ribbon_foreground_mipmap_b_sr_latn_anydpi_v26", name("res/mipmap-b+sr+Latn-anydpi-v26/ic_launcher.xml"))
    }
}
