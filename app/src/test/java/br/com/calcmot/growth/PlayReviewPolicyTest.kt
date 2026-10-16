package br.com.calcmot.growth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayReviewPolicyTest {
    private val day = 86_400_000L

    @Test
    fun `no review before five consecutive days of successful offers`() {
        val empty = PlayReviewState()
        assertFalse(PlayReviewPolicy.eligible(empty, 0, 100L * day))
        assertFalse(PlayReviewPolicy.eligible(empty, 4, 100L * day))
        assertTrue(PlayReviewPolicy.eligible(empty, 5, 100L * day))
    }

    @Test
    fun `cooldown prevents repeated requests for one hundred twenty days`() {
        val first = PlayReviewPolicy.attempted(PlayReviewState(), 100L * day)
        assertEquals(1, first.attempts)
        assertFalse(PlayReviewPolicy.eligible(first, 200, 219L * day))
        assertTrue(PlayReviewPolicy.eligible(first, 5, 220L * day))
        assertFalse(PlayReviewPolicy.eligible(first, 20, 99L * day))
    }

    @Test
    fun `at most two lifetime automatic requests`() {
        val one = PlayReviewPolicy.attempted(PlayReviewState(), 100L * day)
        val two = PlayReviewPolicy.attempted(one, 220L * day)
        assertEquals(2, two.attempts)
        assertFalse(PlayReviewPolicy.eligible(two, 365, 500L * day))
    }
}
