package br.com.calcmot.accessibility

import android.content.Context
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.AccessibilityExploratoryParser
import br.com.calcmot.processor.TreeOfferInspection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class AccessibilityTreeLab(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sequence = AtomicInteger(0)
    private val enabled = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_RAW_TREE_SNAPSHOTS_ENABLED, false)
    private val sessionDir = if (enabled) {
        File(
            context.applicationContext.filesDir,
            "accessibility-lab/session-${System.currentTimeMillis()}"
        ).apply {
            mkdirs()
        }
    } else {
        null
    }

    fun record(snapshot: AccessibilityTreeSnapshot, inspection: TreeOfferInspection) {
        val targetDir = sessionDir ?: return
        val nextSequence = sequence.incrementAndGet()
        if (nextSequence > MAX_SNAPSHOTS_PER_SESSION) return

        scope.launch {
            val fileName = "%05d_%s.json".format(
                nextSequence,
                snapshot.sourceName.sanitizeFileName()
            )
            targetDir.mkdirs()
            File(targetDir, fileName).writeText(snapshot.toJson(inspection), Charsets.UTF_8)
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun AccessibilityTreeSnapshot.toJson(inspection: TreeOfferInspection): String {
        return buildString {
            append("{")
            field("sourceName", sourceName)
            field("generationId", generationId)
            field("scanId", scanId)
            field("sourceKind", sourceKind.name)
            field("capturedAtMillis", capturedAtMillis)
            field("eventAtMillis", eventAtMillis)
            field("delayMs", delayMs)
            field("elapsedSinceEventMs", elapsedSinceEventMs)
            field("screenWidth", screenWidth)
            field("screenHeight", screenHeight)
            field("windowCount", windowCount)
            field("rootCount", rootCount)
            field("nodeCount", nodeCount)
            field("maxDepthReached", maxDepthReached)
            field("truncated", truncated)
            field("rootPackageName", rootPackageName)
            field("rootClassName", rootClassName)
            field("treeDump", toReadableTreeDump())
            append("\"inspection\":")
            append(inspection.toJson())
            append(",")
            append("\"exploratoryHits\":[")
            AccessibilityExploratoryParser.inspect(this@toJson).forEachIndexed { index, hit ->
                if (index > 0) append(",")
                append("{")
                field("type", hit.type.name)
                field("rawValue", hit.rawValue)
                field("source", hit.source.name)
                field("nodeSnapshotId", hit.nodeSnapshotId)
                field("viewIdResourceName", hit.viewIdResourceName)
                append("\"bounds\":{")
                field("left", hit.bounds.left)
                field("top", hit.bounds.top)
                field("right", hit.bounds.right)
                field("bottom", hit.bounds.bottom, trailingComma = false)
                append("}")
                append("}")
            }
            append("],")
            append("\"lines\":[")
            lines.forEachIndexed { index, line ->
                if (index > 0) append(",")
                append("{")
                field("text", line.text)
                field("packageName", line.packageName)
                field("className", line.className)
                field("viewId", line.viewId)
                field("depth", line.depth)
                field("source", line.source.name)
                field("visibleToUser", line.visibleToUser)
                append("\"bounds\":{")
                field("left", line.bounds.left)
                field("top", line.bounds.top)
                field("right", line.bounds.right)
                field("bottom", line.bounds.bottom, trailingComma = false)
                append("}")
                append("}")
            }
            append("],")
            append("\"nodes\":[")
            nodes.forEachIndexed { index, node ->
                if (index > 0) append(",")
                append("{")
                field("snapshotId", node.snapshotId)
                field("parentSnapshotId", node.parentSnapshotId)
                field("generationId", node.generationId)
                field("sourceKind", node.sourceKind.name)
                field("path", node.path)
                field("depth", node.depth)
                field("indexInParent", node.indexInParent)
                field("windowId", node.windowId)
                field("packageName", node.packageName)
                field("className", node.className)
                field("textRaw", node.textRaw)
                field("textNormalized", node.textNormalized)
                field("contentDescriptionRaw", node.contentDescriptionRaw)
                field("contentDescriptionNormalized", node.contentDescriptionNormalized)
                field("stateDescriptionRaw", node.stateDescriptionRaw)
                field("paneTitleRaw", node.paneTitleRaw)
                field("hintTextRaw", node.hintTextRaw)
                field("tooltipTextRaw", node.tooltipTextRaw)
                field("extrasSummary", node.extrasSummary)
                field("viewIdResourceName", node.viewIdResourceName)
                field("visibleToUser", node.visibleToUser)
                field("clickable", node.clickable)
                field("enabled", node.enabled)
                field("focused", node.focused)
                field("selected", node.selected)
                field("childCount", node.childCount)
                field("timestamp", node.timestamp)
                append("\"boundsInScreen\":{")
                field("left", node.boundsInScreen.left)
                field("top", node.boundsInScreen.top)
                field("right", node.boundsInScreen.right)
                field("bottom", node.boundsInScreen.bottom, trailingComma = false)
                append("}")
                append("}")
            }
            append("]")
            append("}")
        }
    }

    private fun AccessibilityTreeSnapshot.toReadableTreeDump(): String {
        val textNodes = nodes.count { !it.textRaw.isNullOrBlank() }
        val descNodes = nodes.count { !it.contentDescriptionRaw.isNullOrBlank() }
        val header = "generation=$generationId source=$sourceKind delay=${delayMs}ms " +
            "nodes=$nodeCount textNodes=$textNodes descNodes=$descNodes " +
            "maxDepth=$maxDepthReached truncated=$truncated"
        return buildString {
            append(header)
            nodes
                .sortedWith(compareBy({ it.path.pathSortKey() }, { it.depth }, { it.boundsInScreen.top }, { it.boundsInScreen.left }))
                .forEach { node ->
                    append("\n")
                    append("depth=").append(node.depth)
                    append(" path=").append(node.path)
                    append(" index=").append(node.indexInParent)
                    append(" children=").append(node.childCount)
                    append(" class=").append(node.className.orEmpty())
                    append(" package=").append(node.packageName.orEmpty())
                    append(" id=").append(node.viewIdResourceName.orEmpty())
                    append(" text=").append(node.textRaw.orEmpty())
                    append(" desc=").append(node.contentDescriptionRaw.orEmpty())
                    append(" state=").append(node.stateDescriptionRaw.orEmpty())
                    append(" pane=").append(node.paneTitleRaw.orEmpty())
                    append(" hint=").append(node.hintTextRaw.orEmpty())
                    append(" tooltip=").append(node.tooltipTextRaw.orEmpty())
                    append(" extras=").append(node.extrasSummary.orEmpty())
                    append(" visible=").append(node.visibleToUser)
                    append(" enabled=").append(node.enabled)
                    append(" clickable=").append(node.clickable)
                    append(" focused=").append(node.focused)
                    append(" selected=").append(node.selected)
                    append(" bounds=").append(node.boundsInScreen.left)
                        .append(",").append(node.boundsInScreen.top)
                        .append(",").append(node.boundsInScreen.right)
                        .append(",").append(node.boundsInScreen.bottom)
                }
        }
    }

    private fun TreeOfferInspection.toJson(): String {
        return buildString {
            append("{")
            field("sourceName", sourceName)
            field("elapsedSinceEventMs", elapsedSinceEventMs)
            field("nodeCount", nodeCount)
            field("lineCount", lineCount)
            field("hasPrice", hasPrice)
            field("hasActionButton", hasActionButton)
            field("timeDistanceBlockCount", timeDistanceBlockCount)
            field("isCompleteOffer", isCompleteOffer)
            field("rejectionReason", rejectionReason?.name)
            append("\"fieldCandidates\":[")
            fieldCandidates.forEachIndexed { index, candidate ->
                if (index > 0) append(",")
                append("{")
                field("fieldType", candidate.fieldType.name)
                field("rawValue", candidate.rawValue)
                field("normalizedValue", candidate.normalizedValue)
                field("source", candidate.source.name)
                field("nodeSnapshotId", candidate.nodeSnapshotId)
                field("viewIdResourceName", candidate.viewIdResourceName)
                field("confidence", candidate.confidence)
                field("evidence", candidate.evidence)
                append("\"bounds\":{")
                field("left", candidate.bounds.left)
                field("top", candidate.bounds.top)
                field("right", candidate.bounds.right)
                field("bottom", candidate.bounds.bottom, trailingComma = false)
                append("}")
                append("}")
            }
            append("],")
            append("\"knownNodeMappings\":[")
            knownNodeMappings.forEachIndexed { index, mapping ->
                if (index > 0) append(",")
                append("{")
                field("viewIdResourceName", mapping.viewIdResourceName)
                field("expectedFieldType", mapping.expectedFieldType.name)
                field("confidenceBoost", mapping.confidenceBoost)
                field("notes", mapping.notes)
                field("firstSeenAt", mapping.firstSeenAt)
                field("lastSeenAt", mapping.lastSeenAt, trailingComma = false)
                append("}")
            }
            append("]")
            append("}")
        }
    }

    private fun StringBuilder.field(name: String, value: String?, trailingComma: Boolean = true) {
        append("\"").append(name.escapeJson()).append("\":")
        if (value == null) {
            append("null")
        } else {
            append("\"").append(value.escapeJson()).append("\"")
        }
        if (trailingComma) append(",")
    }

    private fun StringBuilder.field(name: String, value: Int, trailingComma: Boolean = true) {
        append("\"").append(name.escapeJson()).append("\":").append(value)
        if (trailingComma) append(",")
    }

    private fun StringBuilder.field(name: String, value: Int?, trailingComma: Boolean = true) {
        append("\"").append(name.escapeJson()).append("\":")
        if (value == null) {
            append("null")
        } else {
            append(value)
        }
        if (trailingComma) append(",")
    }

    private fun StringBuilder.field(name: String, value: Long, trailingComma: Boolean = true) {
        append("\"").append(name.escapeJson()).append("\":").append(value)
        if (trailingComma) append(",")
    }

    private fun StringBuilder.field(name: String, value: Double, trailingComma: Boolean = true) {
        append("\"").append(name.escapeJson()).append("\":").append(value)
        if (trailingComma) append(",")
    }

    private fun StringBuilder.field(name: String, value: Boolean, trailingComma: Boolean = true) {
        append("\"").append(name.escapeJson()).append("\":").append(value)
        if (trailingComma) append(",")
    }

    private fun String.escapeJson(): String {
        return buildString(length) {
            this@escapeJson.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u%04x".format(char.code))
                        } else {
                            append(char)
                        }
                    }
                }
            }
        }
    }

    private fun String.sanitizeFileName(): String {
        return replace(Regex("[^A-Za-z0-9._-]+"), "_").take(80).ifBlank { "snapshot" }
    }

    private fun String.pathSortKey(): String {
        return split("/")
            .joinToString(separator = "/") { part ->
                part.toIntOrNull()?.toString()?.padStart(4, '0') ?: part
            }
    }

    private companion object {
        const val PREFS_NAME = "calcmot_lab"
        const val KEY_RAW_TREE_SNAPSHOTS_ENABLED = "raw_tree_snapshots_enabled"
        const val MAX_SNAPSHOTS_PER_SESSION = 2_000
    }
}
