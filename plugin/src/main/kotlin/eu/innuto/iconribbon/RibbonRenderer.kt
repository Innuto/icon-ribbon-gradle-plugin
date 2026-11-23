package eu.innuto.iconribbon

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Geometry is laid out against a square "frame" assumed to have the visible icon shape (circle,
 * squircle, ...) inscribed in it, so the text stays inside even with a circular launcher mask.
 */
internal object RibbonRenderer {

    /** Adaptive icon canvas is 108dp; the launcher shows the inner 72dp. Rendered at xxxhdpi. */
    private const val ADAPTIVE_CANVAS_PX = 432
    private const val ADAPTIVE_VIEWPORT_PX = 288

    private const val BAND_THICKNESS = 0.20
    private const val DIAGONAL_DISTANCE_FROM_CORNER = 0.30
    private const val HORIZONTAL_DISTANCE_FROM_EDGE = 0.20
    private const val TEXT_HEIGHT = 0.62
    private const val SHADOW_OFFSET = 0.06
    private val SHADOW_COLOR = Color(0, 0, 0, 0x40)

    fun decorate(image: BufferedImage, styles: List<RibbonStyle>): BufferedImage {
        val result = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        result.createGraphics().apply {
            drawImage(image, 0, 0, null)
            // Paint only where the icon itself is opaque, so the ribbon follows round/shaped icons.
            composite = AlphaComposite.SrcAtop
            val frame = Rectangle2D.Double(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
            styles.forEach { style -> drawRibbon(this, frame, style) }
            dispose()
        }
        return result
    }

    /** A transparent 108dp layer carrying only the ribbons, to be stacked over an adaptive foreground. */
    fun renderAdaptiveOverlay(styles: List<RibbonStyle>): BufferedImage {
        val result = BufferedImage(ADAPTIVE_CANVAS_PX, ADAPTIVE_CANVAS_PX, BufferedImage.TYPE_INT_ARGB)
        val inset = (ADAPTIVE_CANVAS_PX - ADAPTIVE_VIEWPORT_PX) / 2.0
        val frame = Rectangle2D.Double(inset, inset, ADAPTIVE_VIEWPORT_PX.toDouble(), ADAPTIVE_VIEWPORT_PX.toDouble())
        result.createGraphics().apply {
            styles.forEach { style -> drawRibbon(this, frame, style) }
            dispose()
        }
        return result
    }

    private fun drawRibbon(target: Graphics2D, frame: Rectangle2D, style: RibbonStyle) {
        // Own copy, so this ribbon's translate/rotate don't leak into the next one.
        val graphics = target.create() as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

        val size = frame.width
        val thickness = size * BAND_THICKNESS
        val halfLength = hypot(frame.maxX, frame.maxY) * 2
        val maxTextWidth: Double

        when (style.position) {
            RibbonPosition.TOP, RibbonPosition.BOTTOM -> {
                val offset = size * HORIZONTAL_DISTANCE_FROM_EDGE
                val centerY = if (style.position == RibbonPosition.TOP) frame.minY + offset else frame.maxY - offset
                graphics.translate(frame.centerX, centerY)
                maxTextWidth = size * 0.55
            }
            RibbonPosition.TOP_LEFT, RibbonPosition.TOP_RIGHT, RibbonPosition.BOTTOM_LEFT, RibbonPosition.BOTTOM_RIGHT -> {
                val (cornerX, cornerY, inwardX, inwardY) = frame.corner(style.position)
                val distance = size * DIAGONAL_DISTANCE_FROM_CORNER / sqrt(2.0)
                graphics.translate(cornerX + inwardX * distance, cornerY + inwardY * distance)
                // Band runs perpendicular to the inward direction, text reading left to right.
                graphics.rotate(if (inwardX * inwardY > 0) -Math.PI / 4 else Math.PI / 4)
                // A 45° chord at distance d from a corner is 2d long; keep the text inside the band's inner edge.
                maxTextWidth = 2 * (size * DIAGONAL_DISTANCE_FROM_CORNER - thickness / 2) * 0.9
            }
        }

        graphics.color = SHADOW_COLOR
        graphics.fill(Rectangle2D.Double(-halfLength, -thickness / 2 + thickness * SHADOW_OFFSET, halfLength * 2, thickness))
        graphics.color = style.backgroundColor
        graphics.fill(Rectangle2D.Double(-halfLength, -thickness / 2, halfLength * 2, thickness))
        drawCenteredText(graphics, style, thickness * TEXT_HEIGHT, maxTextWidth)
        graphics.dispose()
    }

    private fun drawCenteredText(graphics: Graphics2D, style: RibbonStyle, maxHeight: Double, maxWidth: Double) {
        if (style.text.isBlank()) return
        var font = Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(maxHeight.toFloat())
        var layout = TextLayout(style.text, font, graphics.fontRenderContext)
        // Font size → cap height is not 1:1, so fit by the measured glyph bounds.
        val heightScale = maxHeight / layout.bounds.height
        val widthScale = maxWidth / layout.bounds.width
        font = font.deriveFont((font.size2D * minOf(heightScale, widthScale)).toFloat())
        layout = TextLayout(style.text, font, graphics.fontRenderContext)

        val bounds = layout.bounds
        graphics.color = style.textColor
        layout.draw(graphics, (-bounds.centerX).toFloat(), (-bounds.centerY).toFloat())
    }

    private fun Rectangle2D.corner(position: RibbonPosition): Corner = when (position) {
        RibbonPosition.TOP_LEFT -> Corner(minX, minY, 1.0, 1.0)
        RibbonPosition.TOP_RIGHT -> Corner(maxX, minY, -1.0, 1.0)
        RibbonPosition.BOTTOM_LEFT -> Corner(minX, maxY, 1.0, -1.0)
        RibbonPosition.BOTTOM_RIGHT -> Corner(maxX, maxY, -1.0, -1.0)
        RibbonPosition.TOP, RibbonPosition.BOTTOM -> error("$position is not a corner")
    }

    private data class Corner(val x: Double, val y: Double, val inwardX: Double, val inwardY: Double)
}
