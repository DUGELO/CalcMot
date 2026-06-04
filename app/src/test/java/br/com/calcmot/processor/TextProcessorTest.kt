package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TextProcessorTest {

    @Test
    fun `processText with complete uber offer returns total trip metrics`() {
        val rawText = listOf(
            "UberX",
            "R$ 78,37",
            "4,98 (402)",
            "25 minutos (15.5 km) de dist\u00e2ncia",
            "Q. 45 Conj. E, Brazlandia",
            "Viagem de 54 minutos (54.6 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(78.37, result!!.valor, 0.01)
        assertEquals(70.1, result.distanciaKm, 0.01)
        assertEquals(79, result.minutosTotais)
        assertEquals(1.12, result.valorPorKm, 0.01)
        assertEquals(59.52, result.valorPorHora, 0.01)
        assertEquals(4.98, result.nota!!, 0.01)
    }

    @Test
    fun `processText accepts offer without passenger rating`() {
        val rawText = listOf(
            "UberX",
            "R$ 15",
            "10 minutos (5.2 km) de distancia",
            "Viagem de 15 minutos (8.0 km)",
            "Aceitar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(15.0, result!!.valor, 0.01)
        assertEquals(13.2, result.distanciaKm, 0.01)
        assertEquals(25, result.minutosTotais)
        assertNull(result.nota)
    }

    @Test
    fun `processText accepts decimal price with dot`() {
        val rawText = listOf(
            "UberX",
            "R$ 7.50",
            "6 minutos (1.9 km) de distancia",
            "Viagem de 2 minutos (0.2 km)",
            "Aceitar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(7.5, result!!.valor, 0.01)
        assertEquals(2.1, result.distanciaKm, 0.01)
        assertEquals(8, result.minutosTotais)
    }

    @Test
    fun `processText rejects offer without price`() {
        val rawText = listOf(
            "UberX",
            "6 minutos (1.9 km) de distancia",
            "Viagem de 2 minutos (0.2 km)",
            "Aceitar"
        ).joinToString("\n")

        assertNull(TextProcessor.processText(rawText))
    }

    @Test
    fun `processText rejects offer without complete pickup and trip data`() {
        val rawText = listOf(
            "UberX",
            "R$ 20",
            "Viagem de 15 minutos (8.0 km)",
            "Aceitar"
        ).joinToString("\n")

        assertNull(TextProcessor.processText(rawText))
    }

    @Test
    fun `processText rejects zero values`() {
        val rawText = listOf(
            "UberX",
            "R$ 0",
            "10 minutos (5.2 km) de distancia",
            "Viagem de 15 minutos (8.0 km)",
            "Aceitar"
        ).joinToString("\n")

        assertNull(TextProcessor.processText(rawText))
    }

    @Test
    fun `processText accepts pickup and trip durations with hours`() {
        val rawText = listOf(
            "UberX",
            "R$ 100,00",
            "4,98 (402)",
            "1 h 10 min (15.5 km) de distancia",
            "Viagem de 1 hora e 5 minutos (54.6 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(100.0, result!!.valor, 0.01)
        assertEquals(70.1, result.distanciaKm, 0.01)
        assertEquals(135, result.minutosTotais)
    }

    @Test
    fun `processText accepts time and distance split across nearby accessibility lines`() {
        val rawText = listOf(
            "UberX",
            "R$ 78,37",
            "25 minutos",
            "(15.5 km) de distancia",
            "Q. 45 Conj. E, Brazlandia",
            "Viagem de 54 minutos",
            "(54.6 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(70.1, result!!.distanciaKm, 0.01)
        assertEquals(79, result.minutosTotais)
    }

    @Test
    fun `processText accepts changed text with same two block structure`() {
        val rawText = listOf(
            "UberX",
            "R$ 30,00",
            "8 min (2,0 km)",
            "35 min (10,0 km)",
            "Aceitar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(12.0, result!!.distanciaKm, 0.01)
        assertEquals(43, result.minutosTotais)
    }

    @Test
    fun `processText accepts accessibility text with non breaking spaces`() {
        val rawText = listOf(
            "UberX",
            "R$\u00A07,44",
            "4,78 (82)",
            "4 minutos (1.1\u00A0km) de distancia",
            "R. Trinta e Tres, Aguas Lindas de Goias",
            "Viagem de 9 minutos (3.7\u00A0km)",
            "Av. Santa Luzia",
            "Aceitar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(7.44, result!!.valor, 0.01)
        assertEquals(4.8, result.distanciaKm, 0.01)
        assertEquals(13, result.minutosTotais)
        assertEquals(1.55, result.valorPorKm, 0.01)
    }

    @Test
    fun `processText accepts common digit confusions in distance and duration`() {
        val rawText = listOf(
            "UberX",
            "R$ 8,95",
            "4,93 (89)",
            "3 minutos (0.9 km) de distancia",
            "Av. JK, Aguas Lindas de Goias",
            "Viagem de ll minutos (4.7 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(8.95, result!!.valor, 0.01)
        assertEquals(5.6, result.distanciaKm, 0.01)
        assertEquals(14, result.minutosTotais)
    }

    @Test
    fun `processText accepts digit confusion in decimal distance`() {
        val rawText = listOf(
            "UberX",
            "R$ 7,44",
            "4 minutos (1.l km) de distancia",
            "Viagem de 9 minutos (3.7 km)",
            "Aceitar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(4.8, result!!.distanciaKm, 0.01)
        assertEquals(13, result.minutosTotais)
    }

    @Test
    fun `processText rejects bonus only price`() {
        val rawText = listOf(
            "4,90 (710)",
            "Verificado",
            "+R$ 2,04 incluido para prioridade",
            "2 minutos (0.5 km) de distancia",
            "Viagem de 12 minutos (4.7 km)",
            "Aceitar"
        ).joinToString("\n")

        assertNull(TextProcessor.processText(rawText))
    }

    @Test
    fun `processText ignores bonus price when primary fare is present`() {
        val rawText = listOf(
            "UberX",
            "R$ 21,67",
            "+R$ 2,04 incluido para prioridade",
            "8 minutos (2.4 km) de distancia",
            "Viagem de 21 minutos (16.7 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(21.67, result!!.valor, 0.01)
        assertEquals(19.1, result.distanciaKm, 0.01)
        assertEquals(29, result.minutosTotais)
    }

    @Test
    fun `processText uses first primary fare when accessibility duplicates price text`() {
        val rawText = listOf(
            "UberX",
            "R$ 10,85",
            "R$ 10,85",
            "+R$ 2,04 incluido para prioridade",
            "4 minutos (1.4 km) de distancia",
            "Viagem de 13 minutos (5.5 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(10.85, result!!.valor, 0.01)
        assertEquals(6.9, result.distanciaKm, 0.01)
        assertEquals(17, result.minutosTotais)
    }

    @Test
    fun `processText repairs fare without decimal separator when literal fare is impossible`() {
        val rawText = listOf(
            "UberX",
            "R$ 1734",
            "4,86 (56)",
            "14 minutos (9.2 km) de distancia",
            "Viagem de 11 minutos (6.6 km)",
            "Selecionar"
        ).joinToString("\n")

        val result = TextProcessor.processText(rawText)

        assertNotNull(result)
        assertEquals(17.34, result!!.valor, 0.01)
        assertEquals(15.8, result.distanciaKm, 0.01)
        assertEquals(25, result.minutosTotais)
    }
}
