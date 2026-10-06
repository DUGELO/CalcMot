package br.com.calcmot.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductScreenNameTest {
    @Test fun recordingVisitsAreGroupedWithoutDynamicSessionIdentifiers() {
        assertEquals(productScreenName("recording", "detail/{id}"),
            productScreenName("recording", "detail/synthetic-session-123"))
        assertFalse(productScreenName("recording", "detail/synthetic-session-123").contains("synthetic"))
    }

    @Test fun mainAndRecordingRoutesDoNotShareAScreenIdentity() {
        assertEquals("app_security_player", productScreenName("app", "security-player/{sessionId}"))
        assertEquals("recording_main", productScreenName("recording", "main"))
    }
}
