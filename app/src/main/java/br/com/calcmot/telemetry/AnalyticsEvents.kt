package br.com.calcmot.telemetry

object AnalyticsEvents {
    const val PLATFORM_SELECTED = "platform_selected"
    const val ACCESSIBILITY_STATUS_CHECKED = "accessibility_status_checked"
    const val OVERLAY_PERMISSION_STATUS_CHECKED = "overlay_permission_status_checked"
    const val DRIVER_APP_DETECTED = "driver_app_detected"
    const val OFFER_DETECTED = "offer_detected"
    const val OFFER_PARSED = "offer_parsed"
    const val OFFER_REJECTED = "offer_rejected"
    const val OVERLAY_SHOWN = "overlay_shown"
    const val OVERLAY_FAILED = "overlay_failed"
    const val NINETY_NINE_OCR_STARTED = "ninetynine_ocr_started"
    const val NINETY_NINE_OCR_SUCCESS = "ninetynine_ocr_success"
    const val NINETY_NINE_OCR_FAILED = "ninetynine_ocr_failed"
    const val PIPELINE_RESET = "pipeline_reset"
    const val MANUAL_RESTART_READING = "manual_restart_reading"
    const val PAYWALL_VIEWED = "paywall_viewed"

    internal val allowed = setOf(
        PLATFORM_SELECTED,
        ACCESSIBILITY_STATUS_CHECKED,
        OVERLAY_PERMISSION_STATUS_CHECKED,
        DRIVER_APP_DETECTED,
        OFFER_DETECTED,
        OFFER_PARSED,
        OFFER_REJECTED,
        OVERLAY_SHOWN,
        OVERLAY_FAILED,
        NINETY_NINE_OCR_STARTED,
        NINETY_NINE_OCR_SUCCESS,
        NINETY_NINE_OCR_FAILED,
        PIPELINE_RESET,
        MANUAL_RESTART_READING,
        PAYWALL_VIEWED
    )
}
