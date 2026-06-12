package br.com.calcmot.telemetry

import android.os.Looper
import android.os.SystemClock
import android.util.Log
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.TripData
import java.util.Locale
import java.util.UUID

class OverlayLatencyTrace private constructor(
    val traceId: String,
    private val packageName: String,
    private val eventType: Int,
    private val windowId: Int,
    private val visualProbe: VisualProbe?
) {
    private val stageTimes = linkedMapOf<Stage, Long>()
    private var fingerprint: String? = null
    private var offerPrice: Double? = null
    private var distanceKm: Double? = null
    private var durationMin: Int? = null
    private var closed = false

    init {
        visualProbe?.let { probe ->
            stageTimes[Stage.T_MINUS1_VISUAL_OFFER_APPEARED] = probe.elapsedRealtime
        }
        mark(Stage.T0_ACCESSIBILITY_EVENT_RECEIVED)
        visualProbe?.let { probe ->
            metric(
                name = "visualToAccessibilityEventMs",
                durationMs = (
                    requireNotNull(stageTimes[Stage.T0_ACCESSIBILITY_EVENT_RECEIVED]) - probe.elapsedRealtime
                    ).coerceAtLeast(0L),
                details = "label=${probe.label ?: "none"} source=${probe.source ?: "none"}"
            )
        }
    }

    fun withCandidate(candidate: OfferCandidate): OverlayLatencyTrace {
        fingerprint = candidate.fingerprint
        offerPrice = candidate.price
        distanceKm = candidate.totalDistanceKm
        durationMin = candidate.totalTimeMin
        return this
    }

    fun withTripData(tripData: TripData): OverlayLatencyTrace {
        fingerprint = "%.2f|%.1f|%d".format(
            Locale.US,
            tripData.valor,
            tripData.distanciaKm,
            tripData.minutosTotais
        )
        offerPrice = tripData.valor
        distanceKm = tripData.distanciaKm
        durationMin = tripData.minutosTotais
        return this
    }

    fun mark(stage: Stage, reason: String? = null): OverlayLatencyTrace {
        if (closed) return this
        val now = SystemClock.elapsedRealtime()
        stageTimes[stage] = now
        logStage(stage, now, reason)
        if (stage == Stage.T13_OVERLAY_VISIBLE_TO_USER) {
            logSummary()
            close(EndReason.VISIBLE_TO_USER, visible = true)
        }
        return this
    }

    fun close(reason: EndReason, visible: Boolean = false): OverlayLatencyTrace {
        if (closed) return this
        closed = true
        logEnd(reason, visible)
        return this
    }

    fun metric(name: String, durationMs: Long, details: String? = null): OverlayLatencyTrace {
        if (closed) return this
        Log.w(
            TAG,
            buildString {
                append("CALCMOT_LATENCY_METRIC")
                append(" traceId=").append(traceId)
                append(" metric=").append(name)
                append(" durationMs=").append(durationMs.coerceAtLeast(0L))
                append(" package=").append(packageName)
                append(" eventType=").append(eventType)
                append(" windowId=").append(windowId)
                append(" fingerprint=").append(fingerprint ?: "none")
                append(" thread=").append(Thread.currentThread().name)
                append(" isMainThread=").append(Looper.myLooper() == Looper.getMainLooper())
                if (!details.isNullOrBlank()) append(" details=").append(details.sanitizeDetails())
            }
        )
        return this
    }

    fun packageNameForDiagnostics(): String = packageName

    fun eventTypeForDiagnostics(): Int = eventType

    fun windowIdForDiagnostics(): Int = windowId

    private fun logStage(stage: Stage, now: Long, reason: String?) {
        val eventAt = stageTimes[Stage.T0_ACCESSIBILITY_EVENT_RECEIVED] ?: now
        Log.w(
            TAG,
            buildString {
                append("CALCMOT_LATENCY_TRACE")
                append(" traceId=").append(traceId)
                append(" stage=").append(stage.name)
                append(" elapsed=").append(now)
                append(" deltaFromEventMs=").append(now - eventAt)
                append(" package=").append(packageName)
                append(" eventType=").append(eventType)
                append(" windowId=").append(windowId)
                append(" fingerprint=").append(fingerprint ?: "none")
                append(" offerPrice=").append(offerPrice?.formatMetric() ?: "none")
                append(" distanceKm=").append(distanceKm?.formatMetric() ?: "none")
                append(" durationMin=").append(durationMin ?: "none")
                append(" thread=").append(Thread.currentThread().name)
                append(" isMainThread=").append(Looper.myLooper() == Looper.getMainLooper())
                if (!reason.isNullOrBlank()) append(" reason=").append(reason)
            }
        )
    }

    private fun logSummary() {
        val totalToVisibleMs = delta(Stage.T0_ACCESSIBILITY_EVENT_RECEIVED, Stage.T13_OVERLAY_VISIBLE_TO_USER)
        val visualToVisibleMs = delta(Stage.T_MINUS1_VISUAL_OFFER_APPEARED, Stage.T13_OVERLAY_VISIBLE_TO_USER)
        val visualToEventMs = delta(Stage.T_MINUS1_VISUAL_OFFER_APPEARED, Stage.T0_ACCESSIBILITY_EVENT_RECEIVED)
        val requestToVisibleMs = delta(Stage.T9_OVERLAY_REQUESTED, Stage.T13_OVERLAY_VISIBLE_TO_USER)
        val rootReadMs = delta(Stage.T2_ROOT_READ_START, Stage.T3_ROOT_READ_END)
        val extractMs = delta(Stage.T4_TREE_EXTRACT_START, Stage.T5_TREE_EXTRACT_END)
        val stabilityMs = delta(Stage.T7_CANDIDATE_COMPLETE, Stage.T8_STABILITY_ACCEPTED)
        val largest = largestStage()
        Log.w(
            TAG,
            "CALCMOT_LATENCY_TRACE_SUMMARY traceId=$traceId package=$packageName " +
                "fingerprint=${fingerprint ?: "none"} totalToVisibleMs=${totalToVisibleMs ?: -1} " +
                "visualToVisibleMs=${visualToVisibleMs ?: -1} visualToEventMs=${visualToEventMs ?: -1} " +
                "requestToVisibleMs=${requestToVisibleMs ?: -1} rootReadMs=${rootReadMs ?: -1} " +
                "extractMs=${extractMs ?: -1} stabilityMs=${stabilityMs ?: -1} " +
                "largestStage=${largest.first} largestStageMs=${largest.second} " +
                "latencyClass=${latencyClass(totalToVisibleMs)}"
        )
    }

    private fun logEnd(reason: EndReason, visible: Boolean) {
        val last = lastStage()
        val totalToVisibleMs = delta(Stage.T0_ACCESSIBILITY_EVENT_RECEIVED, Stage.T13_OVERLAY_VISIBLE_TO_USER)
        val largest = largestStage()
        Log.w(
            TAG,
            "CALCMOT_LATENCY_TRACE_END traceId=$traceId package=$packageName " +
                "fingerprint=${fingerprint ?: "none"} closed=true visible=$visible " +
                "endReason=${reason.name} lastStage=${last.first} lastDeltaMs=${last.second} " +
                "totalToVisibleMs=${totalToVisibleMs ?: -1} largestStage=${largest.first} " +
                "largestStageMs=${largest.second} latencyClass=${latencyClass(totalToVisibleMs)}"
        )
    }

    private fun delta(start: Stage, end: Stage): Long? {
        val startAt = stageTimes[start] ?: return null
        val endAt = stageTimes[end] ?: return null
        return (endAt - startAt).coerceAtLeast(0L)
    }

    private fun largestStage(): Pair<String, Long> {
        val segments = listOf(
            "visualToEventMs" to delta(Stage.T_MINUS1_VISUAL_OFFER_APPEARED, Stage.T0_ACCESSIBILITY_EVENT_RECEIVED),
            "eventToRootMs" to delta(Stage.T0_ACCESSIBILITY_EVENT_RECEIVED, Stage.T2_ROOT_READ_START),
            "rootReadMs" to delta(Stage.T2_ROOT_READ_START, Stage.T3_ROOT_READ_END),
            "extractMs" to delta(Stage.T4_TREE_EXTRACT_START, Stage.T5_TREE_EXTRACT_END),
            "parseToCompleteMs" to delta(Stage.T6_CANDIDATE_PARSED, Stage.T7_CANDIDATE_COMPLETE),
            "stabilityMs" to delta(Stage.T7_CANDIDATE_COMPLETE, Stage.T8_STABILITY_ACCEPTED),
            "requestDelayMs" to delta(Stage.T8_STABILITY_ACCEPTED, Stage.T9_OVERLAY_REQUESTED),
            "overlayWindowMs" to delta(Stage.T10_OVERLAY_ADD_OR_UPDATE_START, Stage.T11_OVERLAY_ADD_OR_UPDATE_END),
            "requestToVisibleMs" to delta(Stage.T9_OVERLAY_REQUESTED, Stage.T13_OVERLAY_VISIBLE_TO_USER)
        )
        return segments
            .mapNotNull { (name, value) -> value?.let { name to it } }
            .maxByOrNull { it.second }
            ?: ("unknown" to -1L)
    }

    private fun lastStage(): Pair<String, Long> {
        val eventAt = stageTimes[Stage.T0_ACCESSIBILITY_EVENT_RECEIVED]
        val last = stageTimes.maxByOrNull { it.value } ?: return "unknown" to -1L
        return last.key.name to if (eventAt == null) -1L else (last.value - eventAt).coerceAtLeast(0L)
    }

    private fun latencyClass(totalToVisibleMs: Long?): String {
        val value = totalToVisibleMs ?: return "UNKNOWN"
        return when {
            value >= 5_000L -> "CRITICAL_5000"
            value > 1_000L -> "SLOW_1000"
            value > 700L -> "WARN_700"
            else -> "OK"
        }
    }

    private fun Double.formatMetric(): String {
        return String.format(Locale.US, "%.2f", this)
    }

    enum class Stage {
        T_MINUS1_VISUAL_OFFER_APPEARED,
        T0_ACCESSIBILITY_EVENT_RECEIVED,
        T1_PACKAGE_ALLOWED_CONFIRMED,
        T2_ROOT_READ_START,
        T3_ROOT_READ_END,
        T4_TREE_EXTRACT_START,
        T5_TREE_EXTRACT_END,
        T6_CANDIDATE_PARSED,
        T7_CANDIDATE_COMPLETE,
        T8_STABILITY_ACCEPTED,
        T9_OVERLAY_REQUESTED,
        T10_OVERLAY_ADD_OR_UPDATE_START,
        T11_OVERLAY_ADD_OR_UPDATE_END,
        T12_OVERLAY_FIRST_DRAW,
        T13_OVERLAY_VISIBLE_TO_USER
    }

    enum class EndReason {
        VISIBLE_TO_USER,
        RENEWED_VISIBLE_OVERLAY,
        COALESCED_INTO_ACTIVE_CAPTURE,
        NO_CANDIDATE_AFTER_TREE,
        CANDIDATE_RENEWED_VISIBLE_OVERLAY,
        WAITING_FOR_STABILITY,
        MONITORING_DISABLED,
        INVALID_CONTEXT,
        CARD_GONE,
        TRACE_SUPERSEDED_BY_NEW_DRIVER_EVENT,
        SAFE_MODE_BLOCKED_USER_APP,
        OVERLAY_BLOCKED,
        SAFE_MODE,
        BAD_TOKEN,
        PREDRAW_TIMEOUT
    }

    companion object {
        private const val TAG = "CalcMotLatency"

        fun start(
            packageName: String,
            eventType: Int,
            windowId: Int,
            visualProbe: VisualProbe? = null
        ): OverlayLatencyTrace {
            return OverlayLatencyTrace(
                traceId = UUID.randomUUID().toString().take(8),
                packageName = packageName,
                eventType = eventType,
                windowId = windowId,
                visualProbe = visualProbe
            )
        }
    }

    data class VisualProbe(
        val elapsedRealtime: Long,
        val label: String?,
        val source: String?
    )

    private fun String.sanitizeDetails(): String {
        return replace(Regex("""\s+"""), "_").take(240)
    }
}
