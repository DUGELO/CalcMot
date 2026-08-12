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
                "paywall_viewed"
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
