package br.com.calcmot.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostReconnectCandidateGuardTest {

    private var now = 1_000L

    @Test
    fun `keeps baseline candidate after service reconnect`() {
        val guard = guard()

        guard.onServiceConnected(currentOverlayFingerprint = "8.75|4.4|11")

        assertFalse(guard.shouldIgnore("8.75|4.4|11"))
    }

    @Test
    fun `ignores first different candidate and repeated stale candidate during reconnect settle`() {
        val guard = guard()

        guard.onServiceConnected(currentOverlayFingerprint = "8.75|4.4|11")

        assertTrue(guard.shouldIgnore("14.69|8.7|16"))
        assertTrue(guard.shouldIgnore("14.69|8.7|16"))
    }

    @Test
    fun `allows second different candidate during reconnect settle`() {
        val guard = guard()

        guard.onServiceConnected(currentOverlayFingerprint = "8.75|4.4|11")

        assertTrue(guard.shouldIgnore("14.69|8.7|16"))
        assertFalse(guard.shouldIgnore("11.55|7.4|12"))
    }

    @Test
    fun `allows candidates after reconnect settle window`() {
        val guard = guard(settleWindowMillis = 5_000)

        guard.onServiceConnected(currentOverlayFingerprint = "8.75|4.4|11")
        now += 5_001

        assertFalse(guard.shouldIgnore("14.69|8.7|16"))
    }

    @Test
    fun `does not arm settle guard when reconnect happens without visible overlay`() {
        val guard = guard()

        guard.onServiceConnected(currentOverlayFingerprint = null)

        assertFalse(guard.shouldIgnore("18.50|13.8|22"))
        assertFalse(guard.shouldIgnore("10.50|4.1|12"))
    }

    private fun guard(settleWindowMillis: Long = 5_000): PostReconnectCandidateGuard {
        now = 1_000L
        return PostReconnectCandidateGuard(
            settleWindowMillis = settleWindowMillis,
            clockMillis = { now }
        )
    }
}
