package br.com.calcmot.securityrecording.domain
import org.junit.Assert.*
import org.junit.Test
class RecordingCaptureEvidenceTest {
    @Test fun `neither permission nor Start nor video alone proves audio video capture`() {
        for(start in listOf(false,true)) for(audio in listOf(false,true)) for(duration in listOf(0L,1L)) {
            assertEquals(start && audio && duration > 0, RecordingCaptureEvidence.confirmed(start,audio,duration))
        }
    }
    @Test fun `Resume waits for progress beyond the paused baseline`() {
        assertFalse(RecordingCaptureEvidence.confirmed(true,true,5_000,5_000))
        assertTrue(RecordingCaptureEvidence.confirmed(true,true,5_001,5_000))
    }
    @Test fun `Rec defaults to unconfirmed and transitional phases never show it`() {
        assertFalse(RecordingRuntimeSnapshot("id",RecordingPhase.RECORDING,1).isRecVisible)
        RecordingPhase.entries.forEach { phase ->
            assertEquals(phase == RecordingPhase.RECORDING, RecordingRuntimeSnapshot("id",phase,1,audioVideoConfirmed=true).isRecVisible)
        }
    }
    @Test fun `notification from an older session cannot stop current capture`() {
        val command=RecordingCommand("command",8,RecordingCommand.Action.STOP,"old")
        assertFalse(RecordingCaptureEvidence.matches(command,"current",8))
        assertFalse(RecordingCaptureEvidence.matches(command.copy(sessionId="current"),"current",9))
        assertTrue(RecordingCaptureEvidence.matches(command.copy(sessionId="current"),"current",8))
    }
    @Test fun `storage reservation includes existing segments two next files and headroom`() {
        val existing=100L
        val next=45L*1024*1024*10
        assertEquals(existing+2*next+RecordingStoragePolicy.RESERVE_BYTES,RecordingStoragePolicy.requiredBytes(10,existing))
        assertEquals(existing+2*RecordingStoragePolicy.MAX_SEGMENT_BYTES+RecordingStoragePolicy.RESERVE_BYTES,RecordingStoragePolicy.requiredBytes(60,existing))
        val boundary=existing+RecordingStoragePolicy.RESERVE_BYTES
        assertTrue(RecordingStoragePolicy.canContinue(boundary,existing))
        assertFalse(RecordingStoragePolicy.canContinue(boundary-1,existing))
    }
}
