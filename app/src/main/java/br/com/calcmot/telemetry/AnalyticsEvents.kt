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
    const val OPEN_DASHBOARD_FINANCE_OPENED = "open_dashboard_finance_opened"
    const val FINANCIAL_RECORD_CREATED = "financial_record_created"
    const val FINANCIAL_HISTORY_CONSENT = "financial_history_consent"
    const val FINANCIAL_HISTORY_DELETED = "financial_history_deleted"
    const val FINANCIAL_REPORT_EXPORTED = "financial_report_exported"
    const val FINANCIAL_RECORD_CORRECTED = "financial_record_corrected"
    const val FINANCIAL_SESSION_UPDATED = "financial_session_updated"
    const val FINANCIAL_REPORT_OPENED = "financial_report_opened"
    const val METRICS_STUDY_CONSENT = "metrics_study_consent"
    const val METRICS_STUDY_DAILY_SUMMARY = "metrics_study_daily_summary"

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
        PAYWALL_VIEWED,
        OPEN_DASHBOARD_FINANCE_OPENED,
        FINANCIAL_RECORD_CREATED,
        FINANCIAL_HISTORY_CONSENT,
        FINANCIAL_HISTORY_DELETED,
        FINANCIAL_REPORT_EXPORTED,
        FINANCIAL_RECORD_CORRECTED,
        FINANCIAL_SESSION_UPDATED,
        FINANCIAL_REPORT_OPENED,
        METRICS_STUDY_CONSENT,
        METRICS_STUDY_DAILY_SUMMARY
    )
}
