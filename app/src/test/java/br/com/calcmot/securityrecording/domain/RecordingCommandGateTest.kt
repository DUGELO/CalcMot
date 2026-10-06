package br.com.calcmot.securityrecording.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingCommandGateTest {
    @Test fun `start requires explicit visible activity confirmation`() {
        val result = RecordingCommandGate().execute(RecordingCommand("start", 0, RecordingCommand.Action.START), false)
        assertEquals(CommandResult.Rejected(CommandResult.RejectReason.NOT_CONFIRMED), result)
    }

    @Test fun `duplicate start returns the original outcome and creates one session`() {
        val gate = RecordingCommandGate()
        val command = RecordingCommand("start", 0, RecordingCommand.Action.START, "session")
        val first = gate.execute(command, true)
        val duplicate = gate.execute(command, true)
        assertEquals(first, duplicate)
        assertEquals("session", (first as CommandResult.Accepted).sessionId)
    }

    @Test fun `stale and concurrent stop commands fail closed`() {
        val gate = RecordingCommandGate()
        gate.execute(RecordingCommand("start", 0, RecordingCommand.Action.START, "session"), true)
        gate.markCaptureConfirmed()
        val stale = gate.execute(RecordingCommand("stop-stale", 1, RecordingCommand.Action.STOP), true)
        assertEquals(CommandResult.Rejected(CommandResult.RejectReason.STALE_REVISION), stale)
        val accepted = gate.execute(RecordingCommand("stop", 2, RecordingCommand.Action.STOP), true)
        assertTrue(accepted is CommandResult.Accepted)
        assertFalse(gate.snapshot().isRecVisible)
    }

    @Test fun `stop can safely cancel a session while camera is still preparing`() {
        val gate = RecordingCommandGate()
        gate.execute(RecordingCommand("start", 0, RecordingCommand.Action.START, "session"), true)

        val stopped = gate.execute(
            RecordingCommand("stop-preparing", 1, RecordingCommand.Action.STOP, "session"),
            false
        )
        gate.markCaptureConfirmed()

        assertTrue(stopped is CommandResult.Accepted)
        assertEquals(RecordingPhase.FINALIZING, gate.snapshot().phase)
        assertFalse(gate.snapshot().isRecVisible)
    }
}
