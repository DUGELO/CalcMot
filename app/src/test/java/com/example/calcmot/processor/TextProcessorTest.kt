package com.example.calcmot.processor

import com.example.calcmot.model.TripData
import org.junit.Assert.*
import org.junit.Test

class TextProcessorTest {

    @Test
    fun `processText com dados completos e validos retorna TripData correto`() {
        // Cenário: Texto bruto de uma oferta completa e válida
        val rawText = """
            UberX Exclusivo
            R$ 7
            ★ 5,00 (27)
            6 minutos (1.9 km) de distância
            Viagem de 2 minutos (0.2 km)
            Aceitar
        """

        // Ação
        val result = TextProcessor.processText(rawText)

        // Verificação
        assertNotNull("O resultado não deveria ser nulo para dados completos", result)
        assertEquals(7.0, result!!.valor, 0.01)
        assertEquals(5.00, result.nota, 0.01)
        assertEquals(2.1, result.distanciaKm, 0.01) // 1.9 + 0.2
        assertEquals(8, result.minutosTotais) // 6 + 2
        assertEquals(3.33, result.valorPorKm, 0.01) // 7 / 2.1
        assertEquals(52.50, result.valorPorHora, 0.01) // 7 / (8/60)
    }

    @Test
    fun `processText com nota faltando deve retornar null`() {
        // Cenário: Texto onde a nota do passageiro não foi capturada
        val rawText = """
            UberX Exclusivo
            R$ 15
            10 minutos (5.2 km) de distância
            Viagem de 15 minutos (8.0 km)
            Aceitar
        """

        // Ação
        val result = TextProcessor.processText(rawText)

        // Verificação
        assertNull("O resultado deveria ser nulo se a nota estiver faltando", result)
    }

    @Test
    fun `processText com apenas um bloco de tempo-distancia deve ser valido`() {
        // Cenário: Uma oferta mais curta ou layout diferente
        val rawText = """
            UberX Exclusivo
            R$ 10
            ★ 4,90 (123)
            Viagem de 12 minutos (4.5 km)
            Aceitar
        """

        // Ação
        val result = TextProcessor.processText(rawText)

        // Verificação
        assertNotNull("O resultado não deveria ser nulo para dados mínimos válidos", result)
        assertEquals(10.0, result!!.valor, 0.01)
        assertEquals(4.90, result.nota, 0.01)
        assertEquals(4.5, result.distanciaKm, 0.01)
        assertEquals(12, result.minutosTotais)
    }
}