package br.com.calcmot.finance.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinancialAnalyticsTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `hour requires at least three official records`() {
        val offers = listOf(offer(7, 0), offer(7, 1), offer(8, 0), offer(8, 1), offer(8, 2))
        val analysis = FinancialAnalytics.analyze(offers, zone)

        assertEquals(listOf(8), analysis.eligibleHourlyBuckets.map { it.hourOfDay })
    }

    @Test
    fun `partial records never enter official peak analysis`() {
        val offers = listOf(
            offer(9, 0),
            offer(9, 1),
            offer(9, 2, confidence = LedgerConfidence.PARTIAL)
        )
        assertTrue(FinancialAnalytics.analyze(offers, zone).eligibleHourlyBuckets.isEmpty())
    }

    private fun offer(
        hour: Int,
        minute: Int,
        confidence: LedgerConfidence = LedgerConfidence.CONFIRMED
    ): OfferRecordEntity {
        val time = LocalDate.of(2026, 8, 13).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        return OfferRecordEntity(
            id = "$hour-$minute",
            sessionId = null,
            platform = "uber",
            source = LedgerSource.UBER_ACCESSIBILITY.name,
            confidence = confidence.name,
            observedAtMillis = time,
            lastSeenAtMillis = time,
            fingerprint = "$hour-$minute",
            dedupKey = "$hour-$minute",
            fareCents = 2_000L,
            pickupDistanceMeters = 1_000L,
            pickupTimeSeconds = 300,
            tripDistanceMeters = 4_000L,
            tripTimeSeconds = 900,
            bonusCents = null,
            category = null,
            passengerRatingMilli = null,
            passengerRatingCount = null,
            specialOfferType = null,
            realizationState = OfferRealizationState.UNKNOWN.name,
            classification = "GOOD",
            classificationReason = "Boa por km e por hora",
            goalPerKmMicros = 1_800_000L,
            goalPerHourCents = 3_500L,
            goalMode = "BALANCED"
        )
    }
}
