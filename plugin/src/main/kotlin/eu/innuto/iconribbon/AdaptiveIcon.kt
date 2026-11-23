package eu.innuto.iconribbon

import org.w3c.dom.Document
import java.io.File
import javax.xml.XMLConstants

/** An `<adaptive-icon>` resource whose foreground layer can be swapped for a ribbon-decorated one. */
internal class AdaptiveIcon private constructor(private val document: Document) {

    /** The same adaptive icon, with its foreground pointing at [drawable]. */
    fun withForeground(drawable: String): String {
        val copy = document.cloneNode(true) as Document
        val copyRoot = copy.documentElement
        val foreground = copyRoot.firstChildElement(FOREGROUND)
            ?: copy.createElementNS(null, FOREGROUND).also { copyRoot.appendChild(it) }
        while (foreground.hasChildNodes()) foreground.removeChild(foreground.firstChild)
        foreground.setAttributeNS(ANDROID_NAMESPACE, "android:drawable", drawable)
        return copy.serialize()
    }

    /** A `<layer-list>` drawing the original foreground with [overlay] on top. */
    fun foregroundLayerList(overlay: String): String {
        val root = document.documentElement
        val layerList = newDocumentBuilder().newDocument()
        val listRoot = layerList.createElementNS(null, "layer-list")
        layerList.appendChild(listRoot)
        // Keep every namespace the original declared (android, aapt, ...) so inline drawables stay valid.
        for (index in 0 until root.attributes.length) {
            val attribute = root.attributes.item(index)
            if (attribute.nodeName.startsWith("xmlns")) {
                listRoot.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attribute.nodeName, attribute.nodeValue)
            }
        }
        listRoot.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:android", ANDROID_NAMESPACE)

        root.firstChildElement(FOREGROUND)?.let { original ->
            val item = layerList.createElementNS(null, "item")
            val drawable = original.getAttributeNS(ANDROID_NAMESPACE, "drawable")
            if (drawable.isNotEmpty()) {
                item.setAttributeNS(ANDROID_NAMESPACE, "android:drawable", drawable)
            } else {
                original.childElements().forEach { item.appendChild(layerList.importNode(it, true)) }
            }
            listRoot.appendChild(item)
        }
        val overlayItem = layerList.createElementNS(null, "item")
        overlayItem.setAttributeNS(ANDROID_NAMESPACE, "android:drawable", overlay)
        listRoot.appendChild(overlayItem)
        return layerList.serialize()
    }

    companion object {
        private const val FOREGROUND = "foreground"

        fun parseOrNull(file: File): AdaptiveIcon? {
            val document = runCatching { newDocumentBuilder().parse(file) }.getOrNull() ?: return null
            return if (document.documentElement.localName == "adaptive-icon") AdaptiveIcon(document) else null
        }
    }
}
