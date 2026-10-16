package br.com.calcmot.ui

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import br.com.calcmot.ui.design.tokens.CalcMotColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcMotWordmarkTest {
    @Test
    fun `wordmark preserves existing brand colors and style`() {
        val wordmark = buildAnnotatedString { appendCalcMotWordmark() }
        assertEquals("CalcMot", wordmark.text)
        assertEquals(2, wordmark.spanStyles.size)

        val calc = wordmark.spanStyles[0]
        val mot = wordmark.spanStyles[1]
        assertEquals(0, calc.start)
        assertEquals(4, calc.end)
        assertEquals(CalcMotColors.TextPrimary, calc.item.color)
        assertEquals(FontStyle.Italic, calc.item.fontStyle)
        assertEquals(FontWeight.Black, calc.item.fontWeight)

        assertEquals(4, mot.start)
        assertEquals(7, mot.end)
        assertEquals(CalcMotColors.Success, mot.item.color)
        assertEquals(FontStyle.Italic, mot.item.fontStyle)
        assertEquals(FontWeight.Black, mot.item.fontWeight)
    }

    @Test
    fun `brand may be embedded into longer headings`() {
        val heading = buildAnnotatedString {
            append("Leve o ")
            appendCalcMotWordmark()
            append(" para seu grupo")
        }
        assertEquals("Leve o CalcMot para seu grupo", heading.text)
        assertEquals(7, heading.spanStyles[0].start)
        assertEquals(11, heading.spanStyles[0].end)
        assertEquals(11, heading.spanStyles[1].start)
        assertEquals(14, heading.spanStyles[1].end)
    }
}
