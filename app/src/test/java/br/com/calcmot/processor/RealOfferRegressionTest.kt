package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RealOfferRegressionTest {

    @Test
    fun `real field offers keep expected metrics`() {
        val cases = listOf(
            RealOfferCase(
                name = "full screenshot card with selecionar button",
                fare = "R$ 10,78",
                pickup = "6 minutos (1.7 km) de distancia",
                trip = "Viagem de 13 minutos (5.5 km)",
                expectedKm = 7.2,
                expectedMin = 19,
                expectedPerKm = 1.50,
                expectedPerHour = 34.04
            ),
            RealOfferCase(
                name = "exclusive card with aceitar button",
                fare = "R$ 10,29",
                extra = "Exclusivo",
                pickup = "3 minutos (0.7 km) de distancia",
                trip = "Viagem de 14 minutos (5.5 km)",
                expectedKm = 6.2,
                expectedMin = 17,
                expectedPerKm = 1.66,
                expectedPerHour = 36.32
            ),
            RealOfferCase(
                name = "longer city offer keeps pickup and trip totals",
                fare = "R$ 16,74",
                extra = "Verificado",
                pickup = "4 minutos (1.2 km) de distancia",
                trip = "Viagem de 19 minutos (11.6 km)",
                expectedKm = 12.8,
                expectedMin = 23,
                expectedPerKm = 1.31,
                expectedPerHour = 43.67
            ),
            RealOfferCase(
                name = "medium distance offer from captured card",
                fare = "R$ 13,07",
                pickup = "4 minutos (1.4 km) de distancia",
                trip = "Viagem de 13 minutos (9.5 km)",
                expectedKm = 10.9,
                expectedMin = 17,
                expectedPerKm = 1.20,
                expectedPerHour = 46.13
            ),
            RealOfferCase(
                name = "verified short card with decimal cents",
                fare = "R$ 7,02",
                extra = "Verificado",
                pickup = "3 minutos (1.2 km) de distancia",
                trip = "Viagem de 8 minutos (3.5 km)",
                expectedKm = 4.7,
                expectedMin = 11,
                expectedPerKm = 1.49,
                expectedPerHour = 38.29
            ),
            RealOfferCase(
                name = "long distance to Brasilia from captured card",
                fare = "R$ 21,67",
                pickup = "8 minutos (2.4 km) de distancia",
                trip = "Viagem de 21 minutos (16.7 km)",
                expectedKm = 19.1,
                expectedMin = 29,
                expectedPerKm = 1.13,
                expectedPerHour = 44.83
            ),
            RealOfferCase(
                name = "good integer fare offer",
                fare = "R$ 7",
                extra = "Exclusivo",
                pickup = "3 minutos (0.7 km) de distancia",
                trip = "Viagem de 8 minutos (2.7 km)",
                expectedKm = 3.4,
                expectedMin = 11,
                expectedPerKm = 2.06,
                expectedPerHour = 38.18
            ),
            RealOfferCase(
                name = "integer fare with pickup and trip both seven minutes",
                fare = "R$ 7",
                extra = "Verificado",
                pickup = "7 minutos (1.8 km) de distancia",
                trip = "Viagem de 7 minutos (2.1 km)",
                expectedKm = 3.9,
                expectedMin = 14,
                expectedPerKm = 1.79,
                expectedPerHour = 30.00
            ),
            RealOfferCase(
                name = "captured card with radar popup nearby",
                fare = "R$ 8,82",
                extra = "Radar de Viagens 2",
                pickup = "4 minutos (1.5 km) de distancia",
                trip = "Viagem de 9 minutos (5.5 km)",
                expectedKm = 7.0,
                expectedMin = 13,
                expectedPerKm = 1.26,
                expectedPerHour = 40.71
            ),
            RealOfferCase(
                name = "regular release card after overlay repositioning",
                fare = "R$ 10,85",
                extra = "Verificado",
                pickup = "4 minutos (1.4 km) de distancia",
                trip = "Viagem de 13 minutos (5.5 km)",
                expectedKm = 6.9,
                expectedMin = 17,
                expectedPerKm = 1.57,
                expectedPerHour = 38.29
            ),
            RealOfferCase(
                name = "comma fare that once produced 155 per km",
                fare = "R$ 7,46",
                pickup = "3 minutos (1.1 km) de distancia",
                trip = "Viagem de 9 minutos (3.7 km)",
                expectedKm = 4.8,
                expectedMin = 12,
                expectedPerKm = 1.55,
                expectedPerHour = 37.30
            ),
            RealOfferCase(
                name = "priority card ignores bonus and uses primary fare",
                fare = "R$ 10,07",
                extra = "+R$ 2,04 incluido para prioridade de",
                pickup = "2 minutos (0.5 km) de distancia",
                trip = "Viagem de 12 minutos (4.7 km)",
                expectedKm = 5.2,
                expectedMin = 14,
                expectedPerKm = 1.94,
                expectedPerHour = 43.16
            ),
            RealOfferCase(
                name = "larger priority card keeps primary fare",
                fare = "R$ 29,71",
                extra = "+R$ 4,43 incluido para prioridade de",
                pickup = "4 minutos (1.5 km) de distancia",
                trip = "Viagem de 28 minutos (22.4 km)",
                expectedKm = 23.9,
                expectedMin = 32,
                expectedPerKm = 1.24,
                expectedPerHour = 55.71
            ),
            RealOfferCase(
                name = "integer fare stays valid",
                fare = "R$ 7",
                pickup = "2 minutos (0.5 km) de distancia",
                trip = "Viagem de 5 minutos (1.6 km)",
                expectedKm = 2.1,
                expectedMin = 7,
                expectedPerKm = 3.33,
                expectedPerHour = 60.00
            ),
            RealOfferCase(
                name = "multi stop label does not affect two core blocks",
                fare = "R$ 8,76",
                pickup = "5 minutos (1.5 km) de distancia",
                trip = "Viagem de 13 minutos (3.1 km)",
                extra = "Varias paradas",
                expectedKm = 4.6,
                expectedMin = 18,
                expectedPerKm = 1.90,
                expectedPerHour = 29.20
            )
        )

        cases.forEach { case ->
            val result = TextProcessor.processText(case.rawText())

            assertNotNull(case.name, result)
            assertEquals(case.name, case.expectedKm, result!!.distanciaKm, 0.01)
            assertEquals(case.name, case.expectedMin, result.minutosTotais)
            assertEquals(case.name, case.expectedPerKm, result.valorPorKm, 0.01)
            assertEquals(case.name, case.expectedPerHour, result.valorPorHora, 0.01)
        }
    }

    @Test
    fun `lost decimal separator in short fare is repaired when literal fare is impossible`() {
        val rawText = listOf(
            "UberX",
            "R$ 746",
            "4,97 (970)",
            "3 minutos (1.1 km) de distancia",
            "Viagem de 9 minutos (3.7 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(7.46, result!!.valor, 0.01)
        assertEquals(4.8, result.distanciaKm, 0.01)
        assertEquals(12, result.minutosTotais)
    }

    @Test
    fun `integer fare remains literal when metrics are plausible`() {
        val rawText = listOf(
            "UberX",
            "R$ 100",
            "4,97 (970)",
            "20 minutos (10.0 km) de distancia",
            "Viagem de 60 minutos (40.0 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(100.0, result!!.valor, 0.01)
        assertEquals(50.0, result.distanciaKm, 0.01)
        assertEquals(80, result.minutosTotais)
    }

    @Test
    fun `impossible metrics are rejected by sanity guard`() {
        val rawText = listOf(
            "UberX",
            "R$ 750",
            "1 minuto (0.1 km) de distancia",
            "Viagem de 1 minuto (0.1 km)",
            "Aceitar"
        ).joinToString("\n")

        assertNull(TextProcessor.processText(rawText))
    }

    private data class RealOfferCase(
        val name: String,
        val fare: String,
        val pickup: String,
        val trip: String,
        val expectedKm: Double,
        val expectedMin: Int,
        val expectedPerKm: Double,
        val expectedPerHour: Double,
        val extra: String? = null
    ) {
        fun rawText(): String {
            return listOfNotNull(
                "UberX",
                fare,
                "4,90 (100)",
                extra,
                pickup,
                "Endereco de origem",
                trip,
                "Endereco de destino",
                "Selecionar"
            ).joinToString("\n")
        }
    }
}
