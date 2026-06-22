package br.com.calcmot.accessibility

import android.view.accessibility.AccessibilityEvent
import br.com.calcmot.DriverApp
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverAccessibilityEventPolicyTest {

    @Test
    fun `uber keeps exact production event profile`() {
        val expected =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED

        assertEquals(expected, DriverAccessibilityEventPolicy.eventTypesFor(DriverApp.UBER))
        assertTrue(
            DriverAccessibilityEventPolicy.isRelevant(
                DriverApp.UBER,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            )
        )
        assertFalse(
            DriverAccessibilityEventPolicy.isRelevant(
                DriverApp.UBER,
                AccessibilityEvent.TYPE_ANNOUNCEMENT
            )
        )
    }

    @Test
    fun `99 alone receives semantic focus announcement and text events`() {
        val extraEvents = listOf(
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
            AccessibilityEvent.TYPE_ANNOUNCEMENT,
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        )

        extraEvents.forEach { eventType ->
            assertTrue(DriverAccessibilityEventPolicy.isRelevant(DriverApp.NINETY_NINE, eventType))
            assertFalse(DriverAccessibilityEventPolicy.isRelevant(DriverApp.UBER, eventType))
        }
    }

    @Test
    fun `99 timing extension does not alter uber burst`() {
        val debugDelays = longArrayOf(0L, 80L, 160L, 300L, 500L, 750L, 1_000L)

        assertArrayEquals(
            debugDelays,
            DriverAccessibilityEventPolicy.burstDelaysFor(
                driverApp = DriverApp.UBER,
                debugTimingSweepEnabled = true,
                debugTimingSweepDelaysMs = debugDelays
            )
        )
        assertArrayEquals(
            longArrayOf(0L, 80L, 160L, 300L, 500L, 750L, 1_000L, 1_250L, 1_500L, 2_000L, 2_500L),
            DriverAccessibilityEventPolicy.burstDelaysFor(
                driverApp = DriverApp.NINETY_NINE,
                debugTimingSweepEnabled = true,
                debugTimingSweepDelaysMs = debugDelays
            )
        )
    }
}
