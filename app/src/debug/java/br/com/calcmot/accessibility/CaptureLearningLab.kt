package br.com.calcmot.accessibility

import android.content.Context
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.OfferCaptureRejectionReason
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.model.TripData
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.TreeOfferInspection
import br.com.calcmot.processor.overlayFingerprint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CaptureLearningLab(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sequence = AtomicInteger(0)
    private val sessionDir = runCatching {
        File(
            context.applicationContext.filesDir,
            "capture-learning/session-${System.currentTimeMillis()}"
        ).apply {
            mkdirs()
        }
    }.getOrNull()

    fun recordCandidate(source: OfferCaptureSource, candidate: OfferCandidate) {
        record(
            status = "candidate",
            source = source,
            fingerprint = candidate.overlayFingerprint(),
            rejectionReason = null
        )
    }

    fun recordOverlay(source: OfferCaptureSource?, tripData: TripData) {
        record(
            status = "overlay_shown",
            source = source,
            fingerprint = tripData.overlayFingerprint(),
            rejectionReason = null
        )
    }

    fun recordOverlayHidden(
        status: String,
        source: OfferCaptureSource?,
        fingerprint: String?,
        reason: OfferCaptureRejectionReason?
    ) {
        record(
            status = status,
            source = source,
            fingerprint = fingerprint,
            rejectionReason = reason
        )
    }

    fun recordRejected(source: OfferCaptureSource, reason: OfferCaptureRejectionReason) {
        record(
            status = "rejected",
            source = source,
            fingerprint = null,
            rejectionReason = reason
        )
    }

    fun recordTreeInspection(
        snapshot: AccessibilityTreeSnapshot,
        inspection: TreeOfferInspection
    ) {
        val next = sequence.incrementAndGet()
        if (next > MAX_EVENTS_PER_SESSION) return
        val targetDir = sessionDir ?: return
        val now = System.currentTimeMillis()

        scope.launch {
            targetDir.mkdirs()
            File(targetDir, "%05d_tree_snapshot.json".format(next)).writeText(
                buildString {
                    append("{")
                    field("capturedAtMillis", now)
                    field("status", "tree_snapshot")
                    field("source", OfferCaptureSource.ACCESSIBILITY_TREE.id)
                    field("snapshotSourceName", snapshot.sourceName)
                    field("snapshotSourceKind", snapshot.sourceKind.name)
                    field("generationId", snapshot.generationId)
                    field("delayMs", snapshot.delayMs)
                    field("elapsedSinceEventMs", snapshot.elapsedSinceEventMs)
                    field("windowCount", snapshot.windowCount)
                    field("rootCount", snapshot.rootCount)
                    field("nodeCount", snapshot.nodeCount)
                    field("lineCount", inspection.lineCount)
                    field("hasPrice", inspection.hasPrice)
                    field("hasActionButton", inspection.hasActionButton)
                    field("timeDistanceBlockCount", inspection.timeDistanceBlockCount)
                    field("isCompleteOffer", inspection.isCompleteOffer)
                    field("rejectionReason", inspection.rejectionReason?.name)
                    field("fieldCandidateCount", inspection.fieldCandidates.size)
                    field("knownNodeMappingCount", inspection.knownNodeMappings.size)
                    field("fingerprint", null)
                    field("containsRawText", false, trailingComma = false)
                    append("}")
                },
                Charsets.UTF_8
            )
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun record(
        status: String,
        source: OfferCaptureSource?,
        fingerprint: String?,
        rejectionReason: OfferCaptureRejectionReason?
    ) {
        val next = sequence.incrementAndGet()
        if (next > MAX_EVENTS_PER_SESSION) return
        val targetDir = sessionDir ?: return
        val now = System.currentTimeMillis()

        scope.launch {
            targetDir.mkdirs()
            File(targetDir, "%05d_%s.json".format(next, status)).writeText(
                buildString {
                    append("{")
                    field("capturedAtMillis", now)
                    field("status", status)
                    field("source", source?.id)
                    field("fingerprint", fingerprint)
                    field("rejectionReason", rejectionReason?.id, trailingComma = false)
                    append("}")
                },
                Charsets.UTF_8
            )
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

    private fun StringBuilder.field(name: String, value: Long, trailingComma: Boolean = true) {
        append("\"").append(name.escapeJson()).append("\":").append(value)
        if (trailingComma) append(",")
    }

    private fun StringBuilder.field(name: String, value: Int, trailingComma: Boolean = true) {
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
                    else -> append(char)
                }
            }
        }
    }

    private companion object {
        const val MAX_EVENTS_PER_SESSION = 5_000
    }
}
