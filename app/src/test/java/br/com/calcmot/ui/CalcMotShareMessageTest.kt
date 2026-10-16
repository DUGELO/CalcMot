package br.com.calcmot.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcMotShareMessageTest {
    @Test
    fun `invite includes the canonical Play Store package`() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=br.com.calcmot",
            CalcMotShareMessage.PLAY_STORE_URL
        )
        val text = CalcMotShareMessage.text()
        assertTrue(text.contains("Uber"))
        assertTrue(text.contains("99"))
        assertTrue(text.endsWith(CalcMotShareMessage.PLAY_STORE_URL))
    }

    @Test
    fun `invite never promises a paid reward or includes personal data`() {
        val text = CalcMotShareMessage.text().lowercase()
        for (term in listOf("grátis por", "premium", "ganhe dias", "assinatura", "telefone", "localização")) {
            assertFalse("Unexpected invitation claim: $term", text.contains(term))
        }
    }
}
