package eu.innuto.iconribbon

import java.awt.Color

/** Rendering form of a [RibbonSpec], with parsed colors. */
internal data class RibbonStyle(
    val text: String,
    val backgroundColor: Color,
    val textColor: Color,
    val position: RibbonPosition,
)

/** Parses `#RRGGBB` or `#AARRGGBB`. */
internal fun parseColor(value: String): Color {
    val hex = value.removePrefix("#")
    require((hex.length == 6 || hex.length == 8) && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        "icon-ribbon: color must be #RRGGBB or #AARRGGBB, was '$value'"
    }
    val argb = hex.toLong(16).toInt()
    return if (hex.length == 6) Color(argb) else Color(argb, true)
}
