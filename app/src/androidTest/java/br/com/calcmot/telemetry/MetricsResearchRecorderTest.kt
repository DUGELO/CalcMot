package br.com.calcmot.telemetry

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.calcmot.AppSettings
import br.com.calcmot.DriverApp
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.OfferCaptureSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetricsResearchRecorderTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(MetricsResearchRecorder.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        AppSettings.setMetricsResearchEnabled(context, true)
    }

    @After
    fun tearDown() {
        AppSettings.setMetricsResearchEnabled(context, false)
        context.getSharedPreferences(MetricsResearchRecorder.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun observationsStayLocalUntilDayChangesAndFlushOnlyCategories() {
        val analytics = RecordingAnalyticsTracker()
        var epochDay = 100L
        val recorder = MetricsResearchRecorder(
            context = context,
            analytics = analytics,
            currentEpochDay = { epochDay }
        )

        recorder.recordOfferObservation(
            driverApp = DriverApp.UBER,
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = offer(),
            observedAtMillis = 1_000_000L,
            ledgerResult = MetricsStudyLedgerResult.NEW_RECORD
        )
        recorder.recordOfferObservation(
            driverApp = DriverApp.UBER,
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = offer(),
            observedAtMillis = 1_030_000L,
            ledgerResult = MetricsStudyLedgerResult.DEDUPLICATED
        )

        assertTrue(analytics.events.isEmpty())
        val stored = context.getSharedPreferences(MetricsResearchRecorder.PREFS_NAME, Context.MODE_PRIVATE).all
        assertFalse(stored.toString().contains("12.50"))
        assertFalse(stored.toString().contains(offer().fingerprint))

        epochDay = 101L
        recorder.recordOfferObservation(
            driverApp = DriverApp.NINETY_NINE,
            source = OfferCaptureSource.NINETY_NINE_OCR,
            candidate = offer(),
            observedAtMillis = 2_000_000L,
            ledgerResult = MetricsStudyLedgerResult.NOT_RECORDED
        )

        assertEquals(2, analytics.events.size)
        analytics.events.forEach { event ->
            assertEquals(AnalyticsEvents.METRICS_STUDY_DAILY_SUMMARY, event.first)
            assertEquals("offer_episode_v1", event.second[AnalyticsParams.STUDY_VERSION])
            assertFalse(event.second.containsKey("price"))
            assertFalse(event.second.containsKey("fingerprint"))
        }
    }

    @Test
    fun disablingConsentClearsPendingCountersAndStopsRecording() {
        val analytics = RecordingAnalyticsTracker()
        val recorder = MetricsResearchRecorder(context, analytics = analytics, currentEpochDay = { 100L })
        recorder.recordOfferObservation(
            DriverApp.UBER,
            OfferCaptureSource.ACCESSIBILITY_TREE,
            offer(),
            1_000_000L,
            MetricsStudyLedgerResult.NEW_RECORD
        )

        AppSettings.setMetricsResearchEnabled(context, false)
        recorder.onConsentChanged(false)
        recorder.recordOfferObservation(
            DriverApp.UBER,
            OfferCaptureSource.ACCESSIBILITY_TREE,
            offer(),
            1_030_000L,
            MetricsStudyLedgerResult.DEDUPLICATED
        )

        assertTrue(
            context.getSharedPreferences(MetricsResearchRecorder.PREFS_NAME, Context.MODE_PRIVATE)
                .all
                .isEmpty()
        )
        assertTrue(analytics.events.isEmpty())
    }

    private fun offer() = OfferCandidate(
        price = 12.50,
        pickupDistanceKm = 1.0,
        pickupTimeMin = 5,
        tripDistanceKm = 5.0,
        tripTimeMin = 12,
        passengerRating = 4.9
    )

    private class RecordingAnalyticsTracker : AnalyticsTracker {
        val events = mutableListOf<Pair<String, Map<String, String>>>()

        override fun track(event: String, params: Map<String, String>) {
            events += event to params
        }
    }
}
