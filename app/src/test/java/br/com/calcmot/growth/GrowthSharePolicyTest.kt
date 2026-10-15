package br.com.calcmot.growth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthSharePolicyTest {
    private val day = 86_400_000L
    private fun now(dayNumber: Long) = dayNumber * day + 12 * 3_600_000L

    @Test
    fun `only two consecutive days of real overlays unlock prompt`() {
        val first = GrowthSharePolicy.onOverlayShown(GrowthShareState(), 100)
        assertEquals(1, first.consecutiveDays)
        assertFalse(GrowthSharePolicy.eligible(first, now(100), 100))
        assertEquals(first, GrowthSharePolicy.onOverlayShown(first, 100))
        val second = GrowthSharePolicy.onOverlayShown(first, 101)
        assertEquals(2, second.consecutiveDays)
        assertTrue(GrowthSharePolicy.eligible(second, now(101), 101))
        assertTrue(GrowthSharePolicy.eligible(second, now(102), 102))
        assertFalse(GrowthSharePolicy.eligible(second, now(103), 103))
    }

    @Test
    fun `a missed day resets consecutive usage and a backward clock does not award days`() {
        val first = GrowthSharePolicy.onOverlayShown(GrowthShareState(), 100)
        val later = GrowthSharePolicy.onOverlayShown(first, 102)
        assertEquals(1, later.consecutiveDays)
        assertEquals(later, GrowthSharePolicy.onOverlayShown(later, 101))
        assertFalse(GrowthSharePolicy.eligible(later, now(102), 102))
    }

    @Test
    fun `dismissal cooldown and two prompts per rolling thirty days`() {
        val eligible = GrowthShareState(lastUsageDay = 110, consecutiveDays = 2)
        val shownOnce = GrowthSharePolicy.shown(eligible, now(110))
        assertFalse(GrowthSharePolicy.eligible(shownOnce, now(123), 110))
        val onceDismissed = GrowthSharePolicy.dismissed(shownOnce, now(110))
        val secondWindow = onceDismissed.copy(lastUsageDay = 124)
        assertTrue(GrowthSharePolicy.eligible(secondWindow, now(124), 124))
        val shownTwice = GrowthSharePolicy.shown(secondWindow, now(124))
        assertFalse(GrowthSharePolicy.eligible(shownTwice.copy(lastUsageDay = 125), now(125), 125))
        val twiceDismissed = GrowthSharePolicy.dismissed(shownTwice, now(124))
        assertFalse(GrowthSharePolicy.eligible(twiceDismissed.copy(lastUsageDay = 180), now(180), 180))
        assertTrue(GrowthSharePolicy.eligible(twiceDismissed.copy(lastUsageDay = 184), now(184), 184))
    }

    @Test
    fun `opt out is permanent and share tap suppresses sixty days`() {
        val initial = GrowthShareState(lastUsageDay = 201, consecutiveDays = 2)
        assertFalse(GrowthSharePolicy.eligible(GrowthSharePolicy.optOut(initial), now(201), 201))
        val shared = GrowthSharePolicy.shared(initial, now(201))
        assertFalse(GrowthSharePolicy.eligible(shared.copy(lastUsageDay = 260), now(260), 260))
        assertTrue(GrowthSharePolicy.eligible(shared.copy(lastUsageDay = 261), now(261), 261))
    }

    @Test
    fun `prompt eligibility respects clock rollback`() {
        val state = GrowthSharePolicy.shown(
            GrowthShareState(lastUsageDay = 100, consecutiveDays = 2), now(100)
        )
        assertFalse(GrowthSharePolicy.eligible(state, now(99), 99))
    }
}
