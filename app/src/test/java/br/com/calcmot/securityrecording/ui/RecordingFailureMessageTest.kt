package br.com.calcmot.securityrecording.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecordingFailureMessageTest {
    @Test
    fun `recovery failure is translated to user language`() {
        val message = recordingFailureMessage("interrupted_no_valid_segment")

        assertEquals("Interrompida sem arquivo válido", message)
        assertFalse(message.contains("_"))
    }

    @Test
    fun `unknown failure remains safe and actionable`() {
        assertEquals(
            "Sessão indisponível para reprodução",
            recordingFailureMessage("future_internal_code")
        )
    }
}
