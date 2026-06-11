package br.com.calcmot.overlay

import br.com.calcmot.model.ImpactMetric
import br.com.calcmot.model.OfferClassification
import br.com.calcmot.model.OfferFinancialImpact
import br.com.calcmot.model.ProfitabilitySettings
import br.com.calcmot.model.TripData
import org.junit.Assert.assertEquals
import org.junit.Test

class TripQualityTest {

    @Test
    fun `trip quality uses boa atencao ruim labels`() {
        assertEquals(TripQuality.GOOD, getTripQuality(trip(valorPorKm = 1.75)))
        assertEquals("BOA", TripQuality.GOOD.text)

        assertEquals(TripQuality.MEDIUM, getTripQuality(trip(valorPorKm = 1.4)))
        assertEquals("ATENÇÃO", TripQuality.MEDIUM.text)

        assertEquals(TripQuality.BAD, getTripQuality(trip(valorPorKm = 1.39)))
        assertEquals("RUIM", TripQuality.BAD.text)
    }

    @Test
    fun `overlay quality uses premium great from financial impact`() {
        val impact = OfferFinancialImpact(
            targetValueByKm = 10.0,
            impactByKm = 4.0,
            targetValueByHour = 8.0,
            impactByHour = 6.0,
            finalImpact = 4.0,
            finalMetric = ImpactMetric.KM,
            classification = OfferClassification.GREAT,
            message = "Excelente: acima da sua meta",
            subtext = "+R$ 4,00 estimado"
        )

        assertEquals(
            OverlayOfferQuality.GREAT,
            resolveOfferQuality(
                profitability = null,
                financialImpact = impact
            )
        )
        assertEquals("ÓTIMA", OverlayOfferQuality.GREAT.label)
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
