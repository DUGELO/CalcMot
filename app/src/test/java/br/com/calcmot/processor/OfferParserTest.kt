package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferParserTest {

    @Test
    fun `overlay metrics are never parsed as trip fare`() {
        val text = """
            RUIM
            R$ 0,22/km
            R$ 5,27/h
            14 min
            UberX
            R$ 7,90
            4 minutos (1.4 km) de distância
            Viagem de 10 minutos (4.2 km)
            Selecionar
        """.trimIndent()

        val candidate = OfferParser.parse(text)

        assertNotNull(candidate)
        assertEquals(7.90, candidate!!.price, 0.01)
        assertEquals(5.6, candidate.totalDistanceKm, 0.01)
        assertEquals(14, candidate.totalTimeMin)
    }

    @Test
    fun `text with only overlay metrics is rejected`() {
        val text = """
            BOA
            R$ 2,50/km
            R$ 41,86/h
            43 min
            4 minutos (1.4 km) de distância
            Viagem de 10 minutos (4.2 km)
        """.trimIndent()

        assertNull(OfferParser.parse(text))
    }

    @Test
    fun `priority bonus is ignored when selecting main fare`() {
        val text = """
            Priority
            Exclusivo
            R$ 13,93
            +R$ 2,04 incluído para prioridade de partida
            12 minutos (3.7 km) de distância
            Viagem de 13 minutos (4.7 km)
            Aceitar
        """.trimIndent()

        val candidate = OfferParser.parse(text)

        assertNotNull(candidate)
        assertEquals(13.93, candidate!!.price, 0.01)
    }
}
