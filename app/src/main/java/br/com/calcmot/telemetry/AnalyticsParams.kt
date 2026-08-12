package br.com.calcmot.telemetry

object AnalyticsParams {
    const val PLATFORM = "platform"
    const val SOURCE = "source"
    const val REASON = "reason"
    const val PIPELINE_STATE = "pipeline_state"
    const val CLASSIFICATION = "classification"
    const val APP_VERSION = "app_version"
    const val ANDROID_VERSION = "android_version"
    const val DEVICE_MANUFACTURER = "device_manufacturer"
    const val DEVICE_MODEL = "device_model"
    const val PRICE_BUCKET = "price_bucket"
    const val KM_BUCKET = "km_bucket"
    const val DURATION_BUCKET = "duration_bucket"
    const val VALUE_PER_KM_BUCKET = "value_per_km_bucket"
    const val VALUE_PER_HOUR_BUCKET = "value_per_hour_bucket"

    internal val allowed = setOf(
        PLATFORM,
        SOURCE,
        REASON,
        PIPELINE_STATE,
        CLASSIFICATION,
        APP_VERSION,
        ANDROID_VERSION,
        DEVICE_MANUFACTURER,
        DEVICE_MODEL,
        PRICE_BUCKET,
        KM_BUCKET,
        DURATION_BUCKET,
        VALUE_PER_KM_BUCKET,
        VALUE_PER_HOUR_BUCKET
    )
}

object AnalyticsValues {
    const val PLATFORM_UBER = "uber"
    const val PLATFORM_NINETY_NINE = "99"
    const val PLATFORM_UNKNOWN = "unknown"

    const val SOURCE_HOME = "home"
    const val SOURCE_SYSTEM = "system"
    const val SOURCE_ACCESSIBILITY_SERVICE = "accessibility_service"
    const val SOURCE_ACCESSIBILITY_TREE = "accessibility_tree"
    const val SOURCE_NINETY_NINE_OCR = "ninety_nine_ocr"
    const val SOURCE_OVERLAY = "overlay"
    const val SOURCE_PREMIUM = "premium"
    const val SOURCE_UIAUTOMATOR_LAB = "uiautomator_lab"

    const val STATUS_ACTIVE = "active"
    const val STATUS_INACTIVE = "inactive"
    const val REASON_UNKNOWN = "unknown"
    const val REASON_BAD_TOKEN = "bad_token"
    const val REASON_EXCEPTION = "exception"
    const val REASON_BOUNDARY_FAILURE = "boundary_failure"
}
