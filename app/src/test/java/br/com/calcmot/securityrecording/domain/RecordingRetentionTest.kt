package br.com.calcmot.securityrecording.domain

import org.junit.Assert.*
import org.junit.Test

class RecordingRetentionTest {
    @Test fun `expiry uses finalization and excludes exact deadline`() {
        val deadline = RecordingRetention.expiresAt(50_000L, 1)
        assertEquals(86_450_000L, deadline)
        assertTrue(RecordingRetention.isAvailable(TemporaryAvailability.AVAILABLE, deadline, deadline - 1))
        assertFalse(RecordingRetention.isAvailable(TemporaryAvailability.AVAILABLE, deadline, deadline))
    }
    @Test fun `extension is a higher total from finalization and never reopens expiry`() {
        val deadline = RecordingRetention.expiresAt(100L, 3)
        assertFalse(RecordingRetention.canExtend(3, 3, deadline, 101))
        assertFalse(RecordingRetention.canExtend(3, 1, deadline, 101))
        assertTrue(RecordingRetention.canExtend(3, 30, deadline, 101))
        assertFalse(RecordingRetention.canExtend(3, 30, deadline, deadline))
        assertFalse(RecordingRetention.canExtend(30, 31, deadline, 101))
        assertFalse(RecordingRetention.canExtend(null, 30, null, 101))
    }
    @Test fun `share grace affects physical cleanup only`() {
        assertEquals(4_600_000L, RecordingRetention.shareCleanupAt(2_000_000L, 1_000_000L))
        assertFalse(RecordingRetention.isAvailable(TemporaryAvailability.EXPIRED, 2_000_000L, 2_000_001L))
        assertFalse(RecordingRetention.isAvailable(TemporaryAvailability.DELETED, null, 0))
    }
    @Test(expected = ArithmeticException::class) fun `expiry overflow fails closed`() {
        RecordingRetention.expiresAt(Long.MAX_VALUE, 30)
    }
}
