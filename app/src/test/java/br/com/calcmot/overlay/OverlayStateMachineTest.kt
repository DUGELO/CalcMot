package br.com.calcmot.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStateMachineTest {

    private var now = 1_000L

    @Test
    fun `first show attaches overlay and stores fingerprint`() {
        val stateMachine = stateMachine()

        val transition = stateMachine.showRequested("10.00|6.0|16")

        assertEquals(OverlayTransition.AttachOrReplace, transition)
        assertTrue(stateMachine.state is OverlayUiState.Showing)
        assertEquals("10.00|6.0|16", stateMachine.currentFingerprint())
    }

    @Test
    fun `same fingerprint updates in place`() {
        val stateMachine = stateMachine()

        stateMachine.showRequested("10.00|6.0|16")
        val transition = stateMachine.showRequested("10.00|6.0|16")

        assertEquals(OverlayTransition.UpdateInPlace, transition)
        assertTrue(stateMachine.state is OverlayUiState.Updating)
    }

    @Test
    fun `different fingerprint attaches or replaces`() {
        val stateMachine = stateMachine()

        stateMachine.showRequested("10.00|6.0|16")
        val transition = stateMachine.showRequested("12.00|7.0|18")

        assertEquals(OverlayTransition.AttachOrReplace, transition)
        assertTrue(stateMachine.state is OverlayUiState.Showing)
        assertEquals("12.00|7.0|18", stateMachine.currentFingerprint())
    }

    @Test
    fun `bad token moves overlay to recovering state`() {
        val stateMachine = stateMachine()

        stateMachine.showRequested("10.00|6.0|16")
        stateMachine.markTokenRecovering("10.00|6.0|16")

        assertTrue(stateMachine.state is OverlayUiState.TokenRecovering)
        assertEquals("10.00|6.0|16", stateMachine.currentFingerprint())
    }

    @Test
    fun `double tap marks dismissed with suppression window`() {
        val stateMachine = stateMachine()

        stateMachine.showRequested("10.00|6.0|16")
        stateMachine.markDismissed("10.00|6.0|16", suppressMillis = 10_000)

        val state = stateMachine.state
        assertTrue(state is OverlayUiState.Dismissed)
        assertEquals(11_000L, (state as OverlayUiState.Dismissed).suppressUntilMillis)
    }

    @Test
    fun `expired state keeps fingerprint for diagnostics`() {
        val stateMachine = stateMachine()

        stateMachine.showRequested("10.00|6.0|16")
        stateMachine.markExpired("10.00|6.0|16")

        assertTrue(stateMachine.state is OverlayUiState.Expired)
        assertEquals("10.00|6.0|16", stateMachine.currentFingerprint())
    }

    private fun stateMachine(): OverlayStateMachine {
        now = 1_000L
        return OverlayStateMachine(clockMillis = { now })
    }
}
