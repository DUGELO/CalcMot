package br.com.calcmot.growth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayReviewPolicyTest {
    private val day = 86_400_000L

    @Test
    fun `only drivers with five consecutive days of displayed offers qualify`() {
        val empty = PlayReviewState()
        assertFalse(PlayReviewPolicy.eligible(empty, 0, 100L * day))
        assertFalse(PlayReviewPolicy.eligible(empty, 4, 100L * day))
        assertTrue(PlayReviewPolicy.eligible(empty, 5, 100L * day))
    }

    @Test
    fun `normal engagement allows another request after twenty one days`() {
        val first = PlayReviewPolicy.attempted(PlayReviewState(), 100L * day)
        assertFalse(PlayReviewPolicy.eligible(first, 5, 120L * day))
        assertTrue(PlayReviewPolicy.eligible(first, 5, 121L * day))
        assertFalse(PlayReviewPolicy.eligible(first, 5, 99L * day))
    }

    @Test
    fun `high engagement earns a shorter fourteen day interval`() {
        val first = PlayReviewPolicy.attempted(PlayReviewState(), 100L * day)
        assertEquals(14L * day, PlayReviewPolicy.cooldownMillis(10))
        assertEquals(21L * day, PlayReviewPolicy.cooldownMillis(5))
        assertFalse(PlayReviewPolicy.eligible(first, 10, 113L * day))
        assertTrue(PlayReviewPolicy.eligible(first, 10, 114L * day))
        assertFalse(PlayReviewPolicy.eligible(first, 4, 200L * day))
    }

    @Test
    fun `there is no lifetime cap even for migrated two attempt state`() {
        var state = PlayReviewState(lastAttemptAt = 100L * day, attempts = 2)
        for (i in 1..6) {
            val now = (100L + 21L * i) * day
            assertTrue(PlayReviewPolicy.eligible(state, 5, now))
            state = PlayReviewPolicy.attempted(state, now)
        }
        assertEquals(8, state.attempts)
    }

    @Test
    fun `request completion never implies a rating was given`() {
        val state = PlayReviewPolicy.attempted(PlayReviewState(), 100L * day)
        assertEquals(1, state.attempts)
        assertEquals(100L * day, state.lastAttemptAt)
        assertFalse(PlayReviewPolicy.eligible(state, 10, 100L * day))
    }
}
