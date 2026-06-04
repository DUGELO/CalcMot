package br.com.calcmot

import android.content.Context
import br.com.calcmot.model.OfferCaptureOutcome
import br.com.calcmot.model.OfferCaptureRejectionReason
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.TreeOfferInspection

object AppDiagnostics {
    private const val PREFS_NAME = "calcmot_diagnostics"
    private const val KEY_EVENT_COUNT = "event_count"
    private const val KEY_LAST_EVENT_TYPE = "last_event_type"
    private const val KEY_LAST_STAGE = "last_stage"
    private const val KEY_LAST_UPDATED_AT = "last_updated_at"
    private const val KEY_TREE_ROOTS_SEEN = "tree_roots_seen"
    private const val KEY_TREE_TEXTS_SEEN = "tree_texts_seen"
    private const val KEY_TREE_PRICE_SEEN = "tree_price_seen"
    private const val KEY_TREE_BUTTON_SEEN = "tree_button_seen"
    private const val KEY_TREE_BLOCKS_SEEN = "tree_blocks_seen"
    private const val KEY_TREE_REJECTED = "tree_rejected"
    private const val KEY_LAST_CAPTURE_SOURCE = "last_capture_source"
    private const val KEY_LAST_CAPTURE_REJECTION_REASON = "last_capture_rejection_reason"
    private const val STAGE_COUNT_PREFIX = "stage_count_"
    private const val CAPTURE_COUNT_PREFIX = "capture_count_"
    private const val CAPTURE_COMPLETE_PREFIX = "capture_complete_"
    private const val CAPTURE_REJECTED_PREFIX = "capture_rejected_"
    private const val CAPTURE_REJECTION_PREFIX = "capture_rejection_"

    fun recordEvent(context: Context, eventType: Int) {
        val appContext = context.safeApplicationContext() ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_EVENT_COUNT, prefs.getLong(KEY_EVENT_COUNT, 0L) + 1L)
            .putInt(KEY_LAST_EVENT_TYPE, eventType)
            .putString(KEY_LAST_STAGE, Stage.EVENT_RECEIVED.value)
            .putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun recordStage(context: Context, stage: Stage) {
        val appContext = context.safeApplicationContext() ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val countKey = stageCountKey(stage)
        prefs.edit()
            .putString(KEY_LAST_STAGE, stage.value)
            .putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())
            .putLong(countKey, prefs.getLong(countKey, 0L) + 1L)
            .apply()
    }

    fun recordTreeInspection(
        context: Context,
        snapshot: AccessibilityTreeSnapshot,
        inspection: TreeOfferInspection
    ) {
        val appContext = context.safeApplicationContext() ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())

        if (snapshot.nodeCount > 0) {
            editor.putLong(
                KEY_TREE_ROOTS_SEEN,
                prefs.getLong(KEY_TREE_ROOTS_SEEN, 0L) + snapshot.rootCount.coerceAtLeast(1)
            )
        }
        if (inspection.lineCount > 0) {
            editor.putLong(KEY_TREE_TEXTS_SEEN, prefs.getLong(KEY_TREE_TEXTS_SEEN, 0L) + 1L)
        }
        if (inspection.hasPrice) {
            editor.putLong(KEY_TREE_PRICE_SEEN, prefs.getLong(KEY_TREE_PRICE_SEEN, 0L) + 1L)
        }
        if (inspection.hasActionButton) {
            editor.putLong(KEY_TREE_BUTTON_SEEN, prefs.getLong(KEY_TREE_BUTTON_SEEN, 0L) + 1L)
        }
        if (inspection.timeDistanceBlockCount >= 2) {
            editor.putLong(KEY_TREE_BLOCKS_SEEN, prefs.getLong(KEY_TREE_BLOCKS_SEEN, 0L) + 1L)
        }

        if (inspection.isCompleteOffer) {
            val countKey = stageCountKey(Stage.TREE_CANDIDATE)
            editor
                .putString(KEY_LAST_STAGE, Stage.TREE_CANDIDATE.value)
                .putLong(countKey, prefs.getLong(countKey, 0L) + 1L)
        } else if (inspection.lineCount > 0) {
            editor
                .putString(KEY_LAST_STAGE, Stage.TREE_REJECTED.value)
                .putLong(KEY_TREE_REJECTED, prefs.getLong(KEY_TREE_REJECTED, 0L) + 1L)
        }

        editor.apply()
    }

    fun recordCaptureOutcome(context: Context, outcome: OfferCaptureOutcome) {
        val appContext = context.safeApplicationContext() ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putLong(KEY_LAST_UPDATED_AT, outcome.capturedAtMillis)
            .putString(KEY_LAST_CAPTURE_SOURCE, outcome.source.id)
            .putLong(
                captureCountKey(outcome.source),
                prefs.getLong(captureCountKey(outcome.source), 0L) + 1L
            )

        if (outcome.isComplete) {
            editor
                .putLong(
                    captureCompleteKey(outcome.source),
                    prefs.getLong(captureCompleteKey(outcome.source), 0L) + 1L
                )
                .remove(KEY_LAST_CAPTURE_REJECTION_REASON)
        } else {
            val reason = outcome.rejectionReason ?: OfferCaptureRejectionReason.UNKNOWN
            editor
                .putString(KEY_LAST_CAPTURE_REJECTION_REASON, reason.id)
                .putLong(
                    captureRejectedKey(outcome.source),
                    prefs.getLong(captureRejectedKey(outcome.source), 0L) + 1L
                )
                .putLong(
                    captureRejectionKey(outcome.source, reason),
                    prefs.getLong(captureRejectionKey(outcome.source, reason), 0L) + 1L
                )
        }

        editor.apply()
    }

    fun read(context: Context): Snapshot {
        val appContext = context.safeApplicationContext() ?: return emptySnapshot()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Snapshot(
            eventCount = prefs.getLong(KEY_EVENT_COUNT, 0L),
            lastEventType = prefs.getInt(KEY_LAST_EVENT_TYPE, 0),
            lastStage = Stage.fromValue(prefs.getString(KEY_LAST_STAGE, null)),
            lastUpdatedAt = prefs.getLong(KEY_LAST_UPDATED_AT, 0L),
            treeCandidateCount = prefs.getLong(stageCountKey(Stage.TREE_CANDIDATE), 0L),
            firstFrameCount = prefs.getLong(stageCountKey(Stage.FIRST_FRAME), 0L),
            stableOfferCount = prefs.getLong(stageCountKey(Stage.STABLE_OFFER), 0L),
            overlayShownCount = prefs.getLong(stageCountKey(Stage.OVERLAY_SHOWN), 0L),
            overlayErrorCount = prefs.getLong(stageCountKey(Stage.OVERLAY_ERROR), 0L),
            frameRejectedCount = prefs.getLong(stageCountKey(Stage.FRAME_REJECTED), 0L),
            treeRootsSeenCount = prefs.getLong(KEY_TREE_ROOTS_SEEN, 0L),
            treeTextsSeenCount = prefs.getLong(KEY_TREE_TEXTS_SEEN, 0L),
            treePriceSeenCount = prefs.getLong(KEY_TREE_PRICE_SEEN, 0L),
            treeButtonSeenCount = prefs.getLong(KEY_TREE_BUTTON_SEEN, 0L),
            treeBlocksSeenCount = prefs.getLong(KEY_TREE_BLOCKS_SEEN, 0L),
            treeRejectedCount = prefs.getLong(KEY_TREE_REJECTED, 0L),
            uiautomatorCompleteCards = prefs.getLong(
                captureCompleteKey(OfferCaptureSource.UIAUTOMATOR_LAB),
                0L
            ),
            internalTreeCompleteCards = prefs.getLong(
                captureCompleteKey(OfferCaptureSource.ACCESSIBILITY_TREE),
                0L
            ),
            uiautomatorRejectedFrames = prefs.getLong(
                captureRejectedKey(OfferCaptureSource.UIAUTOMATOR_LAB),
                0L
            ),
            internalTreeRejectedFrames = prefs.getLong(
                captureRejectedKey(OfferCaptureSource.ACCESSIBILITY_TREE),
                0L
            ),
            lastCaptureSource = OfferCaptureSource.fromId(prefs.getString(KEY_LAST_CAPTURE_SOURCE, null)),
            lastCaptureRejectionReason = OfferCaptureRejectionReason.fromId(
                prefs.getString(KEY_LAST_CAPTURE_REJECTION_REASON, null)
            )
        )
    }

    private fun stageCountKey(stage: Stage): String = STAGE_COUNT_PREFIX + stage.value
    private fun captureCountKey(source: OfferCaptureSource): String = CAPTURE_COUNT_PREFIX + source.id
    private fun captureCompleteKey(source: OfferCaptureSource): String = CAPTURE_COMPLETE_PREFIX + source.id
    private fun captureRejectedKey(source: OfferCaptureSource): String = CAPTURE_REJECTED_PREFIX + source.id
    private fun captureRejectionKey(
        source: OfferCaptureSource,
        reason: OfferCaptureRejectionReason
    ): String = CAPTURE_REJECTION_PREFIX + source.id + "_" + reason.id

    private fun Context.safeApplicationContext(): Context? {
        return runCatching { applicationContext }.getOrNull()
    }

    private fun emptySnapshot(): Snapshot {
        return Snapshot(
            eventCount = 0L,
            lastEventType = 0,
            lastStage = Stage.NEVER,
            lastUpdatedAt = 0L,
            treeCandidateCount = 0L,
            firstFrameCount = 0L,
            stableOfferCount = 0L,
            overlayShownCount = 0L,
            overlayErrorCount = 0L,
            frameRejectedCount = 0L,
            treeRootsSeenCount = 0L,
            treeTextsSeenCount = 0L,
            treePriceSeenCount = 0L,
            treeButtonSeenCount = 0L,
            treeBlocksSeenCount = 0L,
            treeRejectedCount = 0L,
            uiautomatorCompleteCards = 0L,
            internalTreeCompleteCards = 0L,
            uiautomatorRejectedFrames = 0L,
            internalTreeRejectedFrames = 0L,
            lastCaptureSource = null,
            lastCaptureRejectionReason = null
        )
    }

    data class Snapshot(
        val eventCount: Long,
        val lastEventType: Int,
        val lastStage: Stage,
        val lastUpdatedAt: Long,
        val treeCandidateCount: Long,
        val firstFrameCount: Long,
        val stableOfferCount: Long,
        val overlayShownCount: Long,
        val overlayErrorCount: Long,
        val frameRejectedCount: Long,
        val treeRootsSeenCount: Long,
        val treeTextsSeenCount: Long,
        val treePriceSeenCount: Long,
        val treeButtonSeenCount: Long,
        val treeBlocksSeenCount: Long,
        val treeRejectedCount: Long,
        val uiautomatorCompleteCards: Long,
        val internalTreeCompleteCards: Long,
        val uiautomatorRejectedFrames: Long,
        val internalTreeRejectedFrames: Long,
        val lastCaptureSource: OfferCaptureSource?,
        val lastCaptureRejectionReason: OfferCaptureRejectionReason?
    )

    enum class Stage(val value: String, val label: String) {
        NEVER("never", "Aguardando evento da Uber"),
        EVENT_RECEIVED("event_received", "Evento da Uber recebido"),
        MONITORING_DISABLED("monitoring_disabled", "Pausado"),
        TREE_SCAN_SCHEDULED("tree_scan_scheduled", "Leitura da acessibilidade agendada"),
        TREE_SCAN_RUNNING("tree_scan_running", "Lendo acessibilidade"),
        TREE_SNAPSHOT_CAPTURED("tree_snapshot_captured", "Snapshot da acessibilidade capturado"),
        TREE_REFRESH_REQUESTED("tree_refresh_requested", "Atualizando acessibilidade"),
        TREE_REFRESHED("tree_refreshed", "Acessibilidade atualizada"),
        TREE_CANDIDATE("tree_candidate", "Card encontrado na acessibilidade"),
        TREE_REJECTED("tree_rejected", "Card incompleto"),
        CARD_UNCERTAIN("card_uncertain", "Card incerto"),
        FIRST_FRAME("first_frame", "Aguardando segundo frame"),
        STABLE_OFFER("stable_offer", "Card confirmado"),
        OVERLAY_SHOWN("overlay_shown", "Overlay exibido"),
        OVERLAY_ERROR("overlay_error", "Erro ao exibir overlay"),
        FRAME_REJECTED("frame_rejected", "Card incompleto");

        companion object {
            fun fromValue(value: String?): Stage {
                return entries.firstOrNull { it.value == value } ?: NEVER
            }
        }
    }
}
