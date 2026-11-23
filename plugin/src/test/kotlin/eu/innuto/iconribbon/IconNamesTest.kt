package eu.innuto.iconribbon

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class IconNamesTest {

    @Test
    fun `accepts plain resource names`() {
        requireValidIconNames(listOf("ic_launcher", "ic_launcher_round", "ic_launcher_dev2"), "'dev'")
    }

    @Test
    fun `rejects references, extensions and upper case`() {
        listOf("@mipmap/ic_launcher", "ic_launcher.png", "IcLauncher", "").forEach { name ->
            val error = assertFailsWith<IllegalArgumentException> { requireValidIconNames(listOf(name), "'dev'") }
            assertContains(error.message.orEmpty(), "'dev'")
        }
    }
}
