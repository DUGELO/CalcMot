package br.com.calcmot.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GrowthShareInviteRequestsTest {
    @Before
    fun reset() {
        GrowthShareInviteRequests.consumePending()
    }

    @Test
    fun `notification tap is consumed at most once across Home recreations`() {
        GrowthShareInviteRequests.request()
        assertTrue(GrowthShareInviteRequests.consumePending())
        assertFalse(GrowthShareInviteRequests.consumePending())
    }

    @Test
    fun `a new user tap enables a new one-shot invite`() {
        GrowthShareInviteRequests.request()
        assertTrue(GrowthShareInviteRequests.consumePending())
        GrowthShareInviteRequests.request()
        assertTrue(GrowthShareInviteRequests.consumePending())
        assertFalse(GrowthShareInviteRequests.consumePending())
    }

    @Test
    fun `multiple taps before Home appears do not stack popups`() {
        GrowthShareInviteRequests.request()
        GrowthShareInviteRequests.request()
        assertTrue(GrowthShareInviteRequests.consumePending())
        assertFalse(GrowthShareInviteRequests.consumePending())
    }
}
