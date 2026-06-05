package br.com.calcmot.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinancialImpactCalculatorTest {

    @Test
    fun `calculates impact by km and hour using conservative final impact`() {
        val trip = trip(
            price = 20.0,
            distanceKm = 10.0,
            minutes = 30
        )
        val goal = DriverGoal(minValuePerKm = 1.7, minValuePerHour = 50.0)

        val impact = FinancialImpactCalculator.calculate(trip, goal)!!

        assertEquals(17.0, impact.targetValueByKm, 0.001)
        assertEquals(3.0, impact.impactByKm, 0.001)
        assertEquals(25.0, impact.targetValueByHour, 0.001)
        assertEquals(-5.0, impact.impactByHour, 0.001)
        assertEquals(-5.0, impact.finalImpact, 0.001)
        assertEquals(ImpactMetric.HOUR, impact.finalMetric)
        assertEquals(OfferClassification.WARNING, impact.classification)
        assertEquals("R$ 5,00 abaixo da sua meta", impact.message)
        assertEquals("R$ 5,00 abaixo na hora", impact.subtext)
    }

    @Test
    fun `classifies great good warning and bad offers`() {
        val goal = DriverGoal(minValuePerKm = 2.0, minValuePerHour = 60.0)

        assertEquals(
            OfferClassification.GREAT,
            FinancialImpactCalculator.classify(
                trip(price = 30.0, distanceKm = 10.0, minutes = 20),
                goal
            )
        )
        assertEquals(
            OfferClassification.GOOD,
            FinancialImpactCalculator.classify(
                trip(price = 22.0, distanceKm = 10.0, minutes = 20),
                goal
            )
        )
        assertEquals(
            OfferClassification.WARNING,
            FinancialImpactCalculator.classify(
                trip(price = 21.0, distanceKm = 10.0, minutes = 30),
                goal
            )
        )
        assertEquals(
            OfferClassification.BAD,
            FinancialImpactCalculator.classify(
                trip(price = 15.0, distanceKm = 10.0, minutes = 30),
                goal
            )
        )
    }

    @Test
    fun `goal mode can prioritize km or hour impact`() {
        val trip = trip(price = 20.0, distanceKm = 10.0, minutes = 30)

        val kmFirst = FinancialImpactCalculator.calculate(
            trip,
            DriverGoal(minValuePerKm = 1.7, minValuePerHour = 50.0, mode = GoalMode.PRIORITIZE_KM)
        )!!
        val hourFirst = FinancialImpactCalculator.calculate(
            trip,
            DriverGoal(minValuePerKm = 1.7, minValuePerHour = 50.0, mode = GoalMode.PRIORITIZE_HOUR)
        )!!

        assertEquals(ImpactMetric.KM, kmFirst.finalMetric)
        assertEquals(3.0, kmFirst.finalImpact, 0.001)
        assertEquals(ImpactMetric.HOUR, hourFirst.finalMetric)
        assertEquals(-5.0, hourFirst.finalImpact, 0.001)
    }

    @Test
    fun `rejects invalid trip data`() {
        assertNull(
            FinancialImpactCalculator.calculate(
                trip(price = 0.0, distanceKm = 10.0, minutes = 30),
                DriverGoal()
            )
        )
        assertNull(
            FinancialImpactCalculator.calculate(
                trip(price = 20.0, distanceKm = 0.0, minutes = 30),
                DriverGoal()
            )
        )
        assertNull(
            FinancialImpactCalculator.calculate(
                trip(price = 20.0, distanceKm = 10.0, minutes = 0),
                DriverGoal()
            )
        )
    }

    private fun trip(price: Double, distanceKm: Double, minutes: Int): TripData {
        return TripData(
            valor = price,
            distanciaKm = distanceKm,
            minutosTotais = minutes,
            valorPorKm = if (distanceKm > 0.0) price / distanceKm else 0.0,
            valorPorHora = if (minutes > 0) price / (minutes / 60.0) else 0.0
        )
    }
}
