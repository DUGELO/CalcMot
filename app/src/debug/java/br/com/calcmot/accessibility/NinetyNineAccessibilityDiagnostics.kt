package br.com.calcmot.accessibility

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.TreeOfferInspection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class NinetyNineAccessibilityDiagnostics(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sequence = AtomicInteger(0)
    private val sessionDir = File(
        context.applicationContext.filesDir,
        "99-accessibility/session-${System.currentTimeMillis()}"
    ).apply { mkdirs() }
    private val fileLock = Any()
    private var lastOutcomeSignature: String? = null

    fun recordServiceConfiguration(details: String) {
        record("SERVICE_CONFIG", details)
    }

    fun recordDriverAppSwitch(previous: String, next: String, packageName: String?) {
        record(
            "APP_SWITCH",
            "previous=$previous next=$next package=${packageName.orEmpty()}"
        )
    }

    fun recordEvent(event: AccessibilityEvent, eventName: String) {
        val source = runCatching { event.source }.getOrNull()
        val sourceSummary = if (source == null) {
            "null"
        } else {
            "package=${source.packageName} class=${source.className} id=${source.viewIdResourceName} " +
                "window=${source.windowId} children=${source.childCount} visible=${source.isVisibleToUser} " +
                "clickable=${source.isClickable} focused=${source.isFocused}"
        }
        val eventText = event.text
            ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
            .orEmpty()
            .joinToString(" | ")
            .take(MAX_FIELD_LENGTH)
        record(
            "EVENT",
            "type=$eventName typeId=${event.eventType} package=${event.packageName} " +
                "class=${event.className} window=${event.windowId} contentChanges=${event.contentChangeTypes} " +
                "windowChanges=${event.windowChanges} action=${event.action} movement=${event.movementGranularity} " +
                "recordCount=${event.recordCount} text=${eventText.quoted()} " +
                "description=${event.contentDescription?.toString().orEmpty().take(MAX_FIELD_LENGTH).quoted()} " +
                "source={$sourceSummary}"
        )
    }

    fun recordCaptureSession(
        generation: Long,
        shouldStartBurst: Boolean,
        eventType: Int,
        windowId: Int,
        coalesced: Boolean
    ) {
        record(
            "CAPTURE_SESSION",
            "generation=$generation startBurst=$shouldStartBurst coalesced=$coalesced " +
                "eventType=$eventType window=$windowId"
        )
    }

    fun recordRootSelection(
        generation: Long,
        delayMs: Long,
        rootNull: Boolean,
        windowCount: Int,
        roots: List<String>
    ) {
        record(
            "ROOTS",
            "generation=$generation delayMs=$delayMs rootInActiveWindowNull=$rootNull " +
                "windowCount=$windowCount rootCount=${roots.size} roots=${roots.joinToString(" || ").take(MAX_LIST_LENGTH)}"
        )
    }

    fun recordSemanticBridgeProbe(
        generation: Long,
        delayMs: Long,
        touchExplorationRequested: Boolean,
        results: List<String>
    ) {
        record(
            "SEMANTIC_BRIDGE",
            "generation=$generation delayMs=$delayMs touchExplorationRequested=$touchExplorationRequested " +
                "results=${results.joinToString(" || ").take(MAX_LIST_LENGTH)}"
        )
    }

    fun recordSnapshot(
        snapshot: AccessibilityTreeSnapshot,
        inspection: TreeOfferInspection,
        candidate: OfferCandidate?
    ) {
        val textNodes = snapshot.nodes.count { !it.textRaw.isNullOrBlank() }
        val descriptionNodes = snapshot.nodes.count { !it.contentDescriptionRaw.isNullOrBlank() }
        val stateNodes = snapshot.nodes.count { !it.stateDescriptionRaw.isNullOrBlank() }
        val extrasNodes = snapshot.nodes.count { !it.extrasSummary.isNullOrBlank() }
        val idNodes = snapshot.nodes.count { !it.viewIdResourceName.isNullOrBlank() }
        val invisibleNodes = snapshot.nodes.count { !it.visibleToUser }
        val clickableNodes = snapshot.nodes.count { it.clickable }
        val classes = snapshot.nodes
            .groupingBy { it.className.orEmpty().ifBlank { "<none>" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(MAX_PREVIEW_ITEMS)
            .joinToString(" | ") { "${it.key}:${it.value}" }
        val semanticPreview = snapshot.lines
            .filter { it.source.name != "VIEW_ID_RESOURCE_NAME" }
            .take(MAX_PREVIEW_ITEMS)
            .joinToString(" | ") {
                "${it.source}:${it.text.take(MAX_VALUE_PREVIEW)}@${it.bounds.left},${it.bounds.top}," +
                    "${it.bounds.right},${it.bounds.bottom}"
            }
        val idPreview = snapshot.nodes
            .mapNotNull { it.viewIdResourceName }
            .distinct()
            .take(MAX_PREVIEW_ITEMS)
            .joinToString(" | ")
        val outcomeSignature = listOf(
            snapshot.sourceKind,
            snapshot.nodeCount,
            snapshot.lines.size,
            inspection.hasPrice,
            inspection.hasActionButton,
            inspection.timeDistanceBlockCount,
            inspection.rejectionReason,
            candidate?.fingerprint
        ).joinToString(":")
        val previous = lastOutcomeSignature
        lastOutcomeSignature = outcomeSignature

        record(
            "SNAPSHOT",
            "generation=${snapshot.generationId} scan=${snapshot.scanId} source=${snapshot.sourceName} " +
                "kind=${snapshot.sourceKind} delayMs=${snapshot.delayMs} elapsedMs=${snapshot.elapsedSinceEventMs} " +
                "rootPackage=${snapshot.rootPackageName} rootClass=${snapshot.rootClassName} " +
                "windows=${snapshot.windowCount} roots=${snapshot.rootCount} nodes=${snapshot.nodeCount} " +
                "lines=${snapshot.lines.size} textNodes=$textNodes descNodes=$descriptionNodes " +
                "stateNodes=$stateNodes extrasNodes=$extrasNodes idNodes=$idNodes invisibleNodes=$invisibleNodes " +
                "clickableNodes=$clickableNodes maxDepth=${snapshot.maxDepthReached} truncated=${snapshot.truncated} " +
                "price=${inspection.hasPrice} action=${inspection.hasActionButton} " +
                "blocks=${inspection.timeDistanceBlockCount} complete=${inspection.isCompleteOffer} " +
                "rejection=${inspection.rejectionReason} candidate=${candidate?.fingerprint ?: "none"} " +
                "transition=${if (previous == outcomeSignature) "stable" else "changed"} " +
                "previous=${previous ?: "none"} classes={$classes} semantics={$semanticPreview} ids={$idPreview}"
        )
    }

    fun recordNoCandidate(
        generation: Long,
        delayMs: Long,
        bestReason: String,
        rejectedReasons: List<String>
    ) {
        record(
            "NO_CANDIDATE",
            "generation=$generation delayMs=$delayMs bestReason=$bestReason " +
                "rejected=${rejectedReasons.joinToString(" | ").take(MAX_LIST_LENGTH)}"
        )
    }

    fun recordOcrResult(status: String, detail: String, rawText: String?) {
        record(
            "OCR",
            "status=$status detail=${detail.take(MAX_FIELD_LENGTH)} " +
                "text=${rawText.orEmpty().take(MAX_LIST_LENGTH).quoted()}"
        )
    }

    fun close() {
        scope.cancel()
    }

    private fun record(stage: String, details: String) {
        val next = sequence.incrementAndGet()
        if (next > MAX_RECORDS_PER_SESSION) return
        val now = System.currentTimeMillis()
        val message = "CALCMOT_99_$stage seq=$next at=$now $details"
        Log.w(TAG, message)
        scope.launch {
            sessionDir.mkdirs()
            synchronized(fileLock) {
                File(sessionDir, "timeline.ndjson").appendText(
                    """{"sequence":$next,"atMillis":$now,"stage":"${stage.escapeJson()}","details":"${details.escapeJson()}"}""" +
                        "\n",
                    Charsets.UTF_8
                )
            }
        }
    }

    private fun String.quoted(): String = "\"${replace("\"", "\\\"")}\""

    private fun String.escapeJson(): String {
        return buildString(length) {
            this@escapeJson.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

    private companion object {
        const val TAG = "CalcMot99"
        const val MAX_RECORDS_PER_SESSION = 5_000
        const val MAX_FIELD_LENGTH = 1_000
        const val MAX_LIST_LENGTH = 6_000
        const val MAX_PREVIEW_ITEMS = 80
        const val MAX_VALUE_PREVIEW = 180
    }
}
