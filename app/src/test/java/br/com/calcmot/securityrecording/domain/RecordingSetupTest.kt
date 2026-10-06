package br.com.calcmot.securityrecording.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingSetupTest {
    @Test fun `defaults to front camera 480p and ten minute segments`() {
        val setup = RecordingSetup(lens = RecordingLens.FRONT)
        assertEquals(RecordingQuality.P480, setup.quality)
        assertEquals(10, setup.segmentMinutes)
    }

    @Test fun `offers exactly the approved segment durations`() {
        assertEquals(listOf(5, 10, 20, 30, 60), RecordingSetup.SEGMENT_DURATION_OPTIONS_MINUTES)
    }

    @Test fun `rejects an unsupported segment duration`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecordingSetup(RecordingLens.BACK, segmentMinutes = 15)
        }
    }

    @Test fun `storage guard includes finalize headroom and scales with duration`() {
        val fiveMinutes = RecordingStoragePolicy.requiredBytes(5)
        val sixtyMinutes = RecordingStoragePolicy.requiredBytes(60)

        assertTrue(fiveMinutes > 5L * 45L * 1024L * 1024L)
        assertTrue(sixtyMinutes > fiveMinutes)
        assertTrue(RecordingStoragePolicy.hasCapacity(sixtyMinutes, 60))
        assertFalse(RecordingStoragePolicy.hasCapacity(sixtyMinutes - 1, 60))
    }
}
