package eu.innuto.iconribbon

import java.io.Serializable

/** Task input form of one ribbon, resolved from an [IconRibbonConfig]. */
data class RibbonSpec(
    val text: String,
    val backgroundColor: String,
    val textColor: String,
    val position: RibbonPosition,
) : Serializable {

    internal fun toStyle() = RibbonStyle(
        text = text,
        backgroundColor = parseColor(backgroundColor),
        textColor = parseColor(textColor),
        position = position,
    )

    private companion object {
        private const val serialVersionUID = 1L
    }
}
