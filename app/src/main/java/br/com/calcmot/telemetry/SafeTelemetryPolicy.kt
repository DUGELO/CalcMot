package br.com.calcmot.telemetry

internal object SafeTelemetryPolicy {
    private val safeToken = Regex("^[a-z0-9_]{1,40}$")
    private val safeVersion = Regex("^[A-Za-z0-9._-]{1,36}$")
    private val safeDeviceValue = Regex("^[A-Za-z0-9 ._()/-]{1,36}$")

    private val platforms = setOf("uber", "99", "unknown")
    private val sources = setOf(
        "home",
        "system",
        "accessibility_service",
        "accessibility_tree",
        "ninety_nine_ocr",
        "overlay",
        "premium",
        "uiautomator_lab"
    )
    private val pipelineStates = setOf("idle", "capturing", "ocr", "busy", "failed")
    private val classifications = setOf("good", "medium", "bad", "excellent", "unknown")
    private val reasons = setOf(
        "active",
        "inactive",
        "unknown",
        "invalid_frame",
        "no_price",
        "multiple_primary_prices",
        "no_action_button",
        "incomplete_time_distance_blocks",
        "invalid_vertical_order",
        "not_card_like",
        "parser_rejected",
        "invalid_context_still_there",
        "invalid_context_request_unavailable",
        "invalid_context_no_request",
        "invalid_context_offline",
        "invalid_target",
        "cooldown",
        "busy",
        "paused",
        "unchanged_frame",
        "capture_failed",
        "capture_timeout",
        "ocr_failed",
        "ocr_timeout",
        "internal_error",
        "closed",
        "bad_token",
        "exception",
        "boundary_failure",
        "platform_selected",
        "manual",
        "watchdog_busy",
        "watchdog_failed",
        "watchdog_stalled",
        "scope",
        "service_connected",
        "accessibility_event",
        "capture_pipeline",
        "pipeline_reset",
        "destroy_shell_bridge",
        "destroy_jobs",
        "destroy_accessibility_tree_lab",
        "destroy_capture_learning_lab",
        "destroy_99_diagnostics",
        "destroy_99_capture_engine",
        "destroy_reading_runtime",
        "destroy_overlay",
        "destroy_service_scope",
        "destroy_main_scope",
        "destroy_capture_dispatcher",
        "destroy_super"
    )
    private val buckets = setOf(
        "under_3",
        "3_to_7",
        "8_to_14",
        "15_plus",
        "under_10",
        "10_to_19",
        "20_to_39",
        "40_plus",
        "under_1_40",
        "1_40_to_1_74",
        "1_75_to_2_49",
        "2_50_plus",
        "under_25",
        "25_to_34",
        "35_to_49",
        "50_plus"
    )

    fun sanitizeEvent(event: String, params: Map<String, String>): SafeEvent? {
        if (event !in AnalyticsEvents.allowed) return null
        return SafeEvent(event, sanitizeParams(params))
    }

    fun sanitizeParams(params: Map<String, String>): Map<String, String> {
        return params.mapNotNull { (key, value) ->
            if (key !in AnalyticsParams.allowed || !isSafeValue(key, value)) null else key to value
        }.toMap()
    }

    fun safeReason(value: String): String = value.takeIf(reasons::contains) ?: "unknown"

    private fun isSafeValue(key: String, value: String): Boolean = when (key) {
        AnalyticsParams.PLATFORM -> value in platforms
        AnalyticsParams.SOURCE -> value in sources
        AnalyticsParams.REASON -> value in reasons
        AnalyticsParams.PIPELINE_STATE -> value in pipelineStates
        AnalyticsParams.CLASSIFICATION -> value in classifications
        AnalyticsParams.APP_VERSION,
        AnalyticsParams.ANDROID_VERSION -> safeVersion.matches(value)
        AnalyticsParams.DEVICE_MANUFACTURER,
        AnalyticsParams.DEVICE_MODEL -> safeDeviceValue.matches(value)
        AnalyticsParams.PRICE_BUCKET,
        AnalyticsParams.KM_BUCKET,
        AnalyticsParams.DURATION_BUCKET,
        AnalyticsParams.VALUE_PER_KM_BUCKET,
        AnalyticsParams.VALUE_PER_HOUR_BUCKET -> value in buckets
        else -> false
    }

    data class SafeEvent(val name: String, val params: Map<String, String>)
}
