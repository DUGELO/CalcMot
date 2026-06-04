package br.com.calcmot.overlay

import br.com.calcmot.model.TripData
import br.com.calcmot.model.ProfitabilitySettings
import org.junit.Assert.assertEquals
import org.junit.Test

class TripQualityTest {

    @Test
    fun `trip quality uses boa media ruim labels`() {
        assertEquals(TripQuality.GOOD, getTripQuality(trip(valorPorKm = 2.5)))
        assertEquals("BOA", TripQuality.GOOD.text)

        assertEquals(TripQuality.MEDIUM, getTripQuality(trip(valorPorKm = 1.8)))
        assertEquals("MÉDIA", TripQuality.MEDIUM.text)

        assertEquals(TripQuality.BAD, getTripQuality(trip(valorPorKm = 1.79)))
        assertEquals("RUIM", TripQuality.BAD.text)
    }

    @Test
    fun `trip quality uses net profit settings`() {
        val settings = ProfitabilitySettings(
            vehicleEfficiencyKmPerUnit = 10.0,
            inputPricePerUnit = 5.0,
            maintenanceCostPerKm = 0.5,
            goodNetPerKm = 2.0,
            mediumNetPerKm = 1.5,
            minimumNetPerHour = 0.0
        )

        assertEquals(TripQuality.MEDIUM, getTripQuality(trip(valorPorKm = 2.5), settings))
    }

    private fun trip(valorPorKm: Double): TripData {
        return TripData(
            valor = valorPorKm * 5.0,
            distanciaKm = 5.0,
            minutosTotais = 20,
            valorPorKm = valorPorKm,
            valorPorHora = valorPorKm * 15.0
        )
    }
}
