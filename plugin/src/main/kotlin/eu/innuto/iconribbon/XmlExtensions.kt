package eu.innuto.iconribbon

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

/** Namespace-aware builder; resource XML never has a DOCTYPE, so refuse one (no external entities). */
internal fun newDocumentBuilder(): DocumentBuilder = DocumentBuilderFactory.newInstance()
    .apply {
        isNamespaceAware = true
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }
    .newDocumentBuilder()

internal fun Element.childElements(): List<Element> =
    (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>()

internal fun Element.firstChildElement(localName: String): Element? =
    childElements().firstOrNull { it.localName == localName }

internal fun Document.serialize(): String {
    val transformer = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "utf-8")
    }
    return StringWriter().also { transformer.transform(DOMSource(this), StreamResult(it)) }.toString()
}
