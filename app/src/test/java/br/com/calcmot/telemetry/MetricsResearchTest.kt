package br.com.calcmot.telemetry

import br.com.calcmot.model.OfferCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsResearchTest {
    @Test
    fun `exact card update is classified separately from a new observation`() {
        val evaluator = OfferEpisodeStudyEvaluator()
        val offer = offer()

        assertEquals(
            OfferEpisodeStudyStrategy.NEW_OBSERVATION,
            evaluator.evaluate("uber", 1_000_000L, offer)
        )
        assertEquals(
            OfferEpisodeStudyStrategy.EXACT_MATCH,
            evaluator.evaluate("uber", 1_030_000L, offer)
        )
    }

    @Test
    fun `shadow evaluator compares strict 60 90 and 180 second windows`() {
        assertStrategyAt(60_000L, OfferEpisodeStudyStrategy.STRICT_60_SECONDS)
        assertStrategyAt(61_000L, OfferEpisodeStudyStrategy.STRICT_90_SECONDS)
        assertStrategyAt(91_000L, OfferEpisodeStudyStrategy.STRICT_180_SECONDS)
        assertStrategyAt(181_000L, OfferEpisodeStudyStrategy.NEW_OBSERVATION)
    }

    @Test
    fun `platform histories remain isolated`() {
        val evaluator = OfferEpisodeStudyEvaluator()
        val offer = offer()

        evaluator.evaluate("uber", 1_000_000L, offer)

        assertEquals(
            OfferEpisodeStudyStrategy.NEW_OBSERVATION,
            evaluator.evaluate("99", 1_030_000L, offer)
        )
    }

    @Test
    fun `materially different trip remains a new observation`() {
        val evaluator = OfferEpisodeStudyEvaluator()
        evaluator.evaluate("uber", 1_000_000L, offer())

        assertEquals(
            OfferEpisodeStudyStrategy.NEW_OBSERVATION,
            evaluator.evaluate(
                "uber",
                1_030_000L,
                offer().copy(tripDistanceKm = 7.0)
            )
        )
    }

    @Test
    fun `daily sample counts use coarse buckets only`() {
        assertEquals("1", MetricsResearchRecorder.sampleBucket(1))
        assertEquals("2_to_5", MetricsResearchRecorder.sampleBucket(5))
        assertEquals("6_to_20", MetricsResearchRecorder.sampleBucket(20))
        assertEquals("21_plus", MetricsResearchRecorder.sampleBucket(21))
    }

    private fun assertStrategyAt(deltaMillis: Long, expected: OfferEpisodeStudyStrategy) {
        val evaluator = OfferEpisodeStudyEvaluator()
        evaluator.evaluate("uber", 1_000_000L, offer())

        assertEquals(
            expected,
            evaluator.evaluate(
                "uber",
                1_000_000L + deltaMillis,
                offer().copy(pickupDistanceKm = 0.9, pickupTimeMin = 4)
            )
        )
    }

    private fun offer() = OfferCandidate(
        price = 12.50,
        pickupDistanceKm = 1.0,
        pickupTimeMin = 5,
        tripDistanceKm = 5.0,
        tripTimeMin = 12,
        passengerRating = 4.9
    )
}
