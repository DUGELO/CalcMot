package br.com.calcmot.telemetry

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import br.com.calcmot.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.concurrent.ConcurrentHashMap

class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {
    private val analytics = FirebaseAnalytics.getInstance(context.applicationContext)
    private val lastSentAt = ConcurrentHashMap<String, Long>()

    override fun track(event: String, params: Map<String, String>) {
        val enriched = params + environmentParams()
        val safeEvent = SafeTelemetryPolicy.sanitizeEvent(event, enriched) ?: return
        if (shouldThrottle(safeEvent)) return

        val bundle = Bundle().apply {
            safeEvent.params.forEach { (key, value) -> putString(key, value) }
        }
        runCatching { analytics.logEvent(safeEvent.name, bundle) }
    }

    private fun shouldThrottle(event: SafeTelemetryPolicy.SafeEvent): Boolean {
        val interval = THROTTLE_INTERVALS[event.name] ?: return false
        val key = buildString {
            append(event.name)
            event.params.toSortedMap().forEach { (param, value) -> append('|').append(param).append('=').append(value) }
        }
        val now = SystemClock.elapsedRealtime()
        val previous = lastSentAt.put(key, now) ?: return false
        return now - previous < interval
    }

    private fun environmentParams(): Map<String, String> = mapOf(
        AnalyticsParams.APP_VERSION to BuildConfig.VERSION_NAME,
        AnalyticsParams.ANDROID_VERSION to Build.VERSION.RELEASE.orEmpty().ifBlank { "unknown" },
        AnalyticsParams.DEVICE_MANUFACTURER to Build.MANUFACTURER.safeDeviceValue(),
        AnalyticsParams.DEVICE_MODEL to Build.MODEL.safeDeviceValue()
    )

    private fun String.safeDeviceValue(): String {
        return take(36).ifBlank { "unknown" }
    }

    private companion object {
        const val THIRTY_SECONDS_MS = 30_000L
        const val ONE_MINUTE_MS = 60_000L
        const val TEN_MINUTES_MS = 10 * ONE_MINUTE_MS

        val THROTTLE_INTERVALS = mapOf(
            AnalyticsEvents.ACCESSIBILITY_STATUS_CHECKED to TEN_MINUTES_MS,
            AnalyticsEvents.OVERLAY_PERMISSION_STATUS_CHECKED to TEN_MINUTES_MS,
            AnalyticsEvents.DRIVER_APP_DETECTED to ONE_MINUTE_MS,
            AnalyticsEvents.OFFER_DETECTED to 1_000L,
            AnalyticsEvents.OFFER_REJECTED to THIRTY_SECONDS_MS,
            AnalyticsEvents.NINETY_NINE_OCR_STARTED to ONE_MINUTE_MS,
            AnalyticsEvents.NINETY_NINE_OCR_SUCCESS to ONE_MINUTE_MS,
            AnalyticsEvents.NINETY_NINE_OCR_FAILED to THIRTY_SECONDS_MS
        )
    }
}
