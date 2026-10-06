package br.com.calcmot.securityrecording.application
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test
class RecordingResourceCoordinatorTest {
    @Test fun `simultaneous Starts have one process owner`() {
        val ready=CountDownLatch(24); val go=CountDownLatch(1); val count=AtomicInteger()
        val threads=(1..24).map { Thread { ready.countDown(); go.await(); if(RecordingResourceCoordinator.acquireCapture()) count.incrementAndGet() }.apply { start() } }
        ready.await(); go.countDown(); threads.forEach { it.join() }
        try { assertEquals(1,count.get()) } finally { RecordingResourceCoordinator.releaseCapture() }
    }
    @Test fun `reader protects original but allows copying and blocks Start`() {
        assertTrue(RecordingResourceCoordinator.acquireReader("session"))
        try {
            assertFalse(RecordingResourceCoordinator.acquireCapture())
            assertFalse(RecordingResourceCoordinator.acquireOperation("session",true))
            assertTrue(RecordingResourceCoordinator.acquireOperation("session",false))
            try { assertFalse(RecordingResourceCoordinator.acquireReader("other")); assertFalse(RecordingResourceCoordinator.acquireOperation("session",false)) }
            finally { RecordingResourceCoordinator.releaseOperation("session") }
        } finally { RecordingResourceCoordinator.releaseReader("session") }
        assertTrue(RecordingResourceCoordinator.acquireCapture()); RecordingResourceCoordinator.releaseCapture()
    }
    @Test fun `operation blocks capture until commit lease is released`() {
        assertTrue(RecordingResourceCoordinator.acquireOperation("session",true))
        try { assertFalse(RecordingResourceCoordinator.acquireCapture()) }
        finally { RecordingResourceCoordinator.releaseOperation("session") }
        assertTrue(RecordingResourceCoordinator.acquireCapture()); RecordingResourceCoordinator.releaseCapture()
    }
    @Test fun `destroyed service does not authorize reads or Start while output close is pending`() {
        assertTrue(RecordingResourceCoordinator.acquireCapture())
        RecordingResourceCoordinator.registerOpenOutput("synthetic-output")
        RecordingResourceCoordinator.releaseCapture()
        try {
            assertTrue(RecordingResourceCoordinator.hasOpenOutputs())
            assertFalse(RecordingResourceCoordinator.acquireCapture())
            assertFalse(RecordingResourceCoordinator.acquireReader("session"))
            assertFalse(RecordingResourceCoordinator.acquireOperation("session",true))
        } finally { RecordingResourceCoordinator.releaseOpenOutput("synthetic-output") }
        // Finalize may be delivered twice; releasing the already closed handle is harmless.
        RecordingResourceCoordinator.releaseOpenOutput("synthetic-output")
        assertTrue(RecordingResourceCoordinator.acquireCapture()); RecordingResourceCoordinator.releaseCapture()
    }

}
