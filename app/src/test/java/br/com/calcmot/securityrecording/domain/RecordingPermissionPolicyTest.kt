package br.com.calcmot.securityrecording.domain
import org.junit.Assert.*
import org.junit.Test
class RecordingPermissionPolicyTest {
    @Test fun `preparation asks one resource in order and last grant means ready`() {
        assertEquals(RecordingPermissionPolicy.Resource.CAMERA,RecordingPermissionPolicy.next(false,false,false))
        assertEquals(RecordingPermissionPolicy.Resource.MICROPHONE,RecordingPermissionPolicy.next(true,false,false))
        assertEquals(RecordingPermissionPolicy.Resource.NOTIFICATIONS,RecordingPermissionPolicy.next(true,true,false))
        assertNull(RecordingPermissionPolicy.next(true,true,true))
    }
    @Test fun `first request differs from permanent denial and rationale retry`() {
        assertFalse(RecordingPermissionPolicy.permanentlyDenied(false,false))
        assertFalse(RecordingPermissionPolicy.permanentlyDenied(true,true))
        assertTrue(RecordingPermissionPolicy.permanentlyDenied(true,false))
    }
}
