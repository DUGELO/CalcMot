package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationParserTest {

    @Test
    fun `parses simple minutes`() {
        assertEquals(25, DurationParser.parseMinutes("25 min"))
        assertEquals(25, DurationParser.parseMinutes("25 minutos"))
    }

    @Test
    fun `parses simple hours`() {
        assertEquals(60, DurationParser.parseMinutes("1 h"))
        assertEquals(120, DurationParser.parseMinutes("2 horas"))
    }

    @Test
    fun `parses hours and minutes with common uber formats`() {
        assertEquals(70, DurationParser.parseMinutes("1 h 10 min"))
        assertEquals(70, DurationParser.parseMinutes("1 hora 10 minutos"))
        assertEquals(70, DurationParser.parseMinutes("1 hora e 10 minutos"))
        assertEquals(70, DurationParser.parseMinutes("1h10"))
        assertEquals(70, DurationParser.parseMinutes("1h 10min"))
    }

    @Test
    fun `rejects text without clear duration`() {
        assertNull(DurationParser.parseMinutes("Q. 45 Conj. E"))
        assertNull(DurationParser.parseMinutes("15.5 km"))
        assertNull(DurationParser.parseMinutes("tempo estimado"))
    }

    @Test
    fun `parses common digit confusion for eleven minutes`() {
        assertEquals(11, DurationParser.parseMinutes("ll minutos"))
    }

    @Test
    fun `parses common digit confusion for trailing one in minutes`() {
        assertEquals(21, DurationParser.parseMinutes("2l minutos"))
    }
}
