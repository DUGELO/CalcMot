package br.com.calcmot.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfitabilityCalculatorTest {

    @Test
    fun `default overlay thresholds are realistic for common UberX screening`() {
        assertEquals(1.75, ProfitabilitySettings.DEFAULT_GOOD_NET_PER_KM, 0.001)
        assertEquals(1.40, ProfitabilitySettings.DEFAULT_MEDIUM_NET_PER_KM, 0.001)
    }

    @Test
    fun `calculates operating cost and net profitability`() {
        val trip = TripData(
            valor = 30.0,
            distanciaKm = 12.0,
            minutosTotais = 30,
            valorPorKm = 2.5,
            valorPorHora = 60.0
        )
        val settings = ProfitabilitySettings(
            vehicleEfficiencyKmPerUnit = 10.0,
            inputPricePerUnit = 6.0,
            maintenanceCostPerKm = 0.4,
            goodNetPerKm = 2.0,
            mediumNetPerKm = 1.5,
            minimumNetPerHour = 40.0
        )

        val result = ProfitabilityCalculator.calculate(trip, settings)!!

        assertEquals(1.0, result.operatingCostPerKm, 0.001)
        assertEquals(12.0, result.tripCost, 0.001)
        assertEquals(18.0, result.netProfit, 0.001)
        assertEquals(1.5, result.netPerKm, 0.001)
        assertEquals(36.0, result.netPerHour, 0.001)
        assertEquals(ProfitabilityQuality.MEDIUM, result.quality)
    }

    @Test
    fun `classifies good only when km and hour goals are met`() {
        val settings = ProfitabilitySettings(
            goodNetPerKm = 2.5,
            mediumNetPerKm = 1.8,
            minimumNetPerHour = 45.0
        )

        assertEquals(
            ProfitabilityQuality.GOOD,
            ProfitabilityCalculator.classify(
                netPerKm = 2.5,
                netPerHour = 45.0,
                settings = settings
            )
        )
        assertEquals(
            ProfitabilityQuality.MEDIUM,
            ProfitabilityCalculator.classify(
                netPerKm = 2.5,
                netPerHour = 44.99,
                settings = settings
            )
        )
    }

    @Test
    fun `rejects invalid trip data`() {
        val invalidTrip = TripData(
            valor = 0.0,
            distanciaKm = 10.0,
            minutosTotais = 20,
            valorPorKm = 0.0,
            valorPorHora = 0.0
        )

        assertEquals(null, ProfitabilityCalculator.calculate(invalidTrip, ProfitabilitySettings()))
    }
}
