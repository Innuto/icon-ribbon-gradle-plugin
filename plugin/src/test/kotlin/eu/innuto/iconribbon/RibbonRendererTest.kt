package eu.innuto.iconribbon

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RibbonRendererTest {

    private val style = RibbonStyle("DEBUG", Color.RED, Color.WHITE, RibbonPosition.TOP_RIGHT)

    @Test
    fun `parses rgb and argb colors`() {
        assertEquals(Color(0xD3, 0x2F, 0x2F), parseColor("#D32F2F"))
        assertEquals(Color(0x11, 0x22, 0x33, 0x80), parseColor("#80112233"))
    }

    @Test
    fun `rejects malformed colors`() {
        assertFailsWith<IllegalArgumentException> { parseColor("#12345") }
        assertFailsWith<IllegalArgumentException> { parseColor("#GGGGGG") }
    }

    @Test
    fun `top right ribbon covers the top right corner and leaves the opposite corner untouched`() {
        val source = solidImage(192, Color.BLUE)

        val result = RibbonRenderer.decorate(source, listOf(style))

        // Point on the band's centre line, 30% of the frame away from the top-right corner along the diagonal.
        val offset = (192 * 0.30 / Math.sqrt(2.0)).toInt()
        val onBand = 192 - offset to offset
        assertNotEquals(Color.BLUE.rgb, result.getRGB(onBand.first, onBand.second))
        assertEquals(Color.BLUE.rgb, result.getRGB(5, 186))
        assertEquals(192, result.width)
    }

    @Test
    fun `draws every ribbon at its own position`() {
        val source = solidImage(192, Color.BLUE)
        val bottom = style.copy(backgroundColor = Color.GREEN, position = RibbonPosition.BOTTOM)

        val result = RibbonRenderer.decorate(source, listOf(style, bottom))

        val offset = (192 * 0.30 / Math.sqrt(2.0)).toInt()
        assertNotEquals(Color.BLUE.rgb, result.getRGB(192 - offset, offset))
        // Left end of the bottom band, away from its centred text.
        assertEquals(Color.GREEN.rgb, result.getRGB(3, (192 - 192 * 0.20).toInt()))
    }

    @Test
    fun `ribbon is not painted over transparent parts of the icon`() {
        val transparent = BufferedImage(192, 192, BufferedImage.TYPE_INT_ARGB)

        val result = RibbonRenderer.decorate(transparent, listOf(style))

        assertEquals(0, result.getRGB(191, 0) ushr 24)
    }

    @Test
    fun `adaptive overlay is transparent outside the ribbon`() {
        val overlay = RibbonRenderer.renderAdaptiveOverlay(listOf(style.copy(position = RibbonPosition.BOTTOM)))

        assertEquals(432, overlay.width)
        assertEquals(0, overlay.getRGB(216, 100) ushr 24)
        assertNotEquals(0, overlay.getRGB(10, 360 - (288 * 0.20).toInt()) ushr 24)
    }

    private fun solidImage(size: Int, color: Color) =
        BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply { this.color = color; fillRect(0, 0, size, size); dispose() }
        }
}
