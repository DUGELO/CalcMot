package br.com.calcmot.accessibility

import android.view.accessibility.AccessibilityEvent
import br.com.calcmot.DriverApp

object DriverAccessibilityEventPolicy {
    val baseEventTypes: Int =
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
            AccessibilityEvent.TYPE_WINDOWS_CHANGED

    val ninetyNineExtraEventTypes: Int =
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
            AccessibilityEvent.TYPE_VIEW_FOCUSED or
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED or
            AccessibilityEvent.TYPE_ANNOUNCEMENT or
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED

    val uberBurstDelaysMs: LongArray =
        longArrayOf(0L, 80L, 160L, 300L, 500L, 750L, 1_000L)

    val ninetyNineBurstDelaysMs: LongArray =
        longArrayOf(0L, 80L, 160L, 300L, 500L, 750L, 1_000L, 1_250L, 1_500L, 2_000L, 2_500L)

    fun eventTypesFor(driverApp: DriverApp): Int {
        return if (driverApp == DriverApp.NINETY_NINE) {
            baseEventTypes or ninetyNineExtraEventTypes
        } else {
            baseEventTypes
        }
    }

    fun isRelevant(driverApp: DriverApp, eventType: Int): Boolean {
        if (eventType and baseEventTypes != 0) return true
        return driverApp == DriverApp.NINETY_NINE &&
            eventType and ninetyNineExtraEventTypes != 0
    }

    fun burstDelaysFor(
        driverApp: DriverApp,
        debugTimingSweepEnabled: Boolean,
        debugTimingSweepDelaysMs: LongArray
    ): LongArray {
        return when {
            driverApp == DriverApp.NINETY_NINE -> ninetyNineBurstDelaysMs
            debugTimingSweepEnabled -> debugTimingSweepDelaysMs
            else -> uberBurstDelaysMs
        }
    }
}
