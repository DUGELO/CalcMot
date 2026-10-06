package br.com.calcmot.telemetry

import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.TripData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryPolicyTest {
    @Test
    fun `event whitelist contains exactly the approved product events`() {
        assertEquals(
            setOf(
                "platform_selected",
                "accessibility_status_checked",
                "overlay_permission_status_checked",
                "driver_app_detected",
                "offer_detected",
                "offer_parsed",
                "offer_rejected",
                "overlay_shown",
                "overlay_failed",
                "ninetynine_ocr_started",
                "ninetynine_ocr_success",
                "ninetynine_ocr_failed",
                "pipeline_reset",
                "manual_restart_reading",
                "paywall_viewed",
                "open_dashboard_finance_opened",
                "financial_record_created",
                "financial_history_consent",
                "financial_history_deleted",
                "financial_report_exported",
                "financial_record_corrected",
                "financial_session_updated",
                "financial_report_opened",
                "metrics_study_consent",
                "metrics_study_daily_summary"
            ),
            AnalyticsEvents.allowed
        )
    }

    @Test
    fun `parameter whitelist contains no screen content fields`() {
        val forbidden = setOf(
            "text",
            "raw_text",
            "ocr_text",
            "address",
            "passenger",
            "latitude",
            "longitude",
            "coordinates",
            "screenshot",
            "fingerprint"
        )

        assertTrue(AnalyticsParams.allowed.intersect(forbidden).isEmpty())
    }

    @Test
    fun `unknown events are discarded`() {
        assertNull(
            SafeTelemetryPolicy.sanitizeEvent(
                event = "screen_text_captured",
                params = mapOf(AnalyticsParams.SOURCE to "accessibility_tree")
            )
        )
    }

    @Test
    fun `dashboard event accepts only boolean availability flags`() {
        val safeEvent = SafeTelemetryPolicy.sanitizeEvent(
            event = AnalyticsEvents.OPEN_DASHBOARD_FINANCE_OPENED,
            params = mapOf(
                AnalyticsParams.HAS_ANY_DATA to "true",
                AnalyticsParams.HAS_UBER_DATA to "false",
                AnalyticsParams.HAS_NINETY_NINE_DATA to "true",
                "financial_value" to "123.45"
            )
        )

        requireNotNull(safeEvent)
        assertEquals(3, safeEvent.params.size)
        assertFalse(safeEvent.params.containsKey("financial_value"))
    }

    @Test
    fun `metrics study accepts only categorical daily summaries`() {
        val safeEvent = SafeTelemetryPolicy.sanitizeEvent(
            event = AnalyticsEvents.METRICS_STUDY_DAILY_SUMMARY,
            params = mapOf(
                AnalyticsParams.PLATFORM to "uber",
                AnalyticsParams.SOURCE to "accessibility_tree",
                AnalyticsParams.STUDY_VERSION to "offer_episode_v1",
                AnalyticsParams.STUDY_STRATEGY to "strict_90",
                AnalyticsParams.STUDY_LEDGER_RESULT to "deduplicated",
                AnalyticsParams.SAMPLE_BUCKET to "6_to_20",
                "exact_fare" to "37.90",
                "offer_fingerprint" to "37.90|1.0|5|8.0|20"
            )
        )

        requireNotNull(safeEvent)
        assertEquals(6, safeEvent.params.size)
        assertFalse(safeEvent.params.containsKey("exact_fare"))
        assertFalse(safeEvent.params.containsKey("offer_fingerprint"))
    }

    @Test
    fun `metrics study discards unknown strategies and free form values`() {
        val safeEvent = SafeTelemetryPolicy.sanitizeEvent(
            event = AnalyticsEvents.METRICS_STUDY_DAILY_SUMMARY,
            params = mapOf(
                AnalyticsParams.STUDY_VERSION to "secret_experiment_42",
                AnalyticsParams.STUDY_STRATEGY to "driver_at_avenida_brasil",
                AnalyticsParams.STUDY_LEDGER_RESULT to "R$ 99,00",
                AnalyticsParams.SAMPLE_BUCKET to "1234"
            )
        )

        requireNotNull(safeEvent)
        assertTrue(safeEvent.params.isEmpty())
    }

    @Test
    fun `free-form values and unknown parameters are discarded`() {
        val safeEvent = SafeTelemetryPolicy.sanitizeEvent(
            event = AnalyticsEvents.OFFER_REJECTED,
            params = mapOf(
                AnalyticsParams.PLATFORM to "uber",
                AnalyticsParams.SOURCE to "accessibility_tree",
                AnalyticsParams.REASON to "Rua Exemplo 123",
                "raw_text" to "R$ 15,00 passageiro e endereco"
            )
        )

        requireNotNull(safeEvent)
        assertEquals("uber", safeEvent.params[AnalyticsParams.PLATFORM])
        assertEquals("accessibility_tree", safeEvent.params[AnalyticsParams.SOURCE])
        assertFalse(safeEvent.params.containsKey(AnalyticsParams.REASON))
        assertFalse(safeEvent.params.containsKey("raw_text"))
    }

    @Test
    fun `candidate values are represented only by coarse buckets`() {
        val buckets = AnalyticsBuckets.from(
            OfferCandidate(
                price = 18.50,
                pickupDistanceKm = 1.5,
                pickupTimeMin = 5,
                tripDistanceKm = 4.5,
                tripTimeMin = 12
            )
        )

        assertEquals("10_to_19", buckets[AnalyticsParams.PRICE_BUCKET])
        assertEquals("3_to_7", buckets[AnalyticsParams.KM_BUCKET])
        assertEquals("10_to_19", buckets[AnalyticsParams.DURATION_BUCKET])
        assertEquals("2_50_plus", buckets[AnalyticsParams.VALUE_PER_KM_BUCKET])
        assertEquals("50_plus", buckets[AnalyticsParams.VALUE_PER_HOUR_BUCKET])
        assertEquals(5, buckets.size)
    }

    @Test
    fun `trip values are represented only by coarse buckets`() {
        val buckets = AnalyticsBuckets.from(
            TripData(
                valor = 9.0,
                distanciaKm = 8.0,
                minutosTotais = 25,
                valorPorKm = 1.125,
                valorPorHora = 21.6
            )
        )

        assertEquals("under_10", buckets[AnalyticsParams.PRICE_BUCKET])
        assertEquals("8_to_14", buckets[AnalyticsParams.KM_BUCKET])
        assertEquals("20_to_39", buckets[AnalyticsParams.DURATION_BUCKET])
        assertEquals("under_1_40", buckets[AnalyticsParams.VALUE_PER_KM_BUCKET])
        assertEquals("under_25", buckets[AnalyticsParams.VALUE_PER_HOUR_BUCKET])
    }
}
