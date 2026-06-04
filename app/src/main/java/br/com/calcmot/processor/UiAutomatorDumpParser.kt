package br.com.calcmot.processor

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object UiAutomatorDumpParser {

    fun parse(
        xml: String,
        sourceName: String = "uiautomator-dump",
        capturedAtMillis: Long = System.currentTimeMillis(),
        eventAtMillis: Long = capturedAtMillis
    ): AccessibilityTreeSnapshot {
        val document = documentBuilderFactory.newDocumentBuilder()
            .parse(InputSource(StringReader(xml.extractHierarchyXml())))

        val nodes = mutableListOf<UiNode>()
        document.documentElement.childElements("node").forEach { child ->
            collectNodes(child, depth = 0, output = nodes)
        }

        val lines = nodes.flatMap { node ->
            val textLine = node.text.takeIf { it.isNotBlank() }?.let {
                node.toAccessibleLine(text = it, source = AccessibleTextSource.TEXT)
            }
            val descriptionLine = node.contentDescription
                .takeIf { it.isNotBlank() && it != node.text }
                ?.let { node.toAccessibleLine(text = it, source = AccessibleTextSource.CONTENT_DESCRIPTION) }

            listOfNotNull(textLine, descriptionLine)
        }

        val screenWidth = nodes.maxOfOrNull { it.bounds.right } ?: 0
        val screenHeight = nodes.maxOfOrNull { it.bounds.bottom } ?: 0

        return AccessibilityTreeSnapshot(
            sourceName = sourceName,
            capturedAtMillis = capturedAtMillis,
            eventAtMillis = eventAtMillis,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            windowCount = 1,
            rootCount = if (nodes.isEmpty()) 0 else 1,
            nodeCount = nodes.size,
            rootPackageName = nodes.firstNotNullOfOrNull { it.packageName.takeIf(String::isNotBlank) },
            rootClassName = nodes.firstNotNullOfOrNull { it.className.takeIf(String::isNotBlank) },
            lines = lines
        )
    }

    private fun collectNodes(element: Element, depth: Int, output: MutableList<UiNode>) {
        output += element.toUiNode(depth)
        element.childElements("node").forEach { child ->
            collectNodes(child, depth = depth + 1, output = output)
        }
    }

    private fun Element.toUiNode(depth: Int): UiNode {
        return UiNode(
            text = getAttribute("text").orEmpty(),
            contentDescription = getAttribute("content-desc").orEmpty(),
            bounds = parseBounds(getAttribute("bounds")),
            packageName = getAttribute("package").orEmpty(),
            className = getAttribute("class").orEmpty(),
            viewId = getAttribute("resource-id").orEmpty().ifBlank { null },
            depth = depth,
            visibleToUser = isVisibleToUser()
        )
    }

    private fun UiNode.toAccessibleLine(text: String, source: AccessibleTextSource): AccessibleLine {
        return AccessibleLine(
            text = text,
            bounds = bounds,
            packageName = packageName.ifBlank { null },
            className = className.ifBlank { null },
            viewId = viewId,
            depth = depth,
            source = source,
            visibleToUser = visibleToUser
        )
    }

    private fun Element.isVisibleToUser(): Boolean {
        val visibleToUser = getAttribute("visible-to-user")
        val displayed = getAttribute("displayed")
        return visibleToUser.lowercase() != "false" && displayed.lowercase() != "false"
    }

    private fun Element.childElements(tagName: String): List<Element> {
        val matches = mutableListOf<Element>()
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == tagName) {
                matches += child as Element
            }
        }
        return matches
    }

    private fun parseBounds(value: String): ScreenBounds {
        val match = boundsRegex.matchEntire(value.trim())
        return if (match == null) {
            ScreenBounds(left = 0, top = 0, right = 0, bottom = 0)
        } else {
            ScreenBounds(
                left = match.groupValues[1].toInt(),
                top = match.groupValues[2].toInt(),
                right = match.groupValues[3].toInt(),
                bottom = match.groupValues[4].toInt()
            )
        }
    }

    private fun String.extractHierarchyXml(): String {
        val clean = trim().removePrefix("\uFEFF")
        val start = clean.indexOf("<hierarchy")
        val end = clean.lastIndexOf("</hierarchy>")
        return if (start >= 0 && end >= start) {
            clean.substring(start, end + "</hierarchy>".length)
        } else {
            clean
        }
    }

    private data class UiNode(
        val text: String,
        val contentDescription: String,
        val bounds: ScreenBounds,
        val packageName: String,
        val className: String,
        val viewId: String?,
        val depth: Int,
        val visibleToUser: Boolean
    )

    private val boundsRegex = Regex("""\[(\d+),(\d+)]\[(\d+),(\d+)]""")

    private val documentBuilderFactory: DocumentBuilderFactory
        get() = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setXIncludeAware(false)
            setExpandEntityReferences(false)
        }
}
