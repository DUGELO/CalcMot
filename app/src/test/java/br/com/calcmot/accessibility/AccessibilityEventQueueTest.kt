package br.com.calcmot.accessibility

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEventQueueTest {
    @Test
    fun `content overflow is bounded and keeps foreground decision`() = runBlocking {
        val released = mutableListOf<Int>()
        val queue = AccessibilityEventQueue<Int>(capacity = 2, release = released::add)
        queue.offer(1, changesContext = true)
        (2..100).forEach { queue.offer(it, changesContext = false) }
        assertEquals((2..98).toList(), released)
        val foreground = queue.receive()
        assertEquals(1, foreground.value)
        assertFalse(queue.isContextSettled)
        queue.markApplied(foreground)
        queue.release(foreground)
        assertTrue(queue.isContextSettled)
        for (value in 99..100) {
            val content = queue.receive()
            assertEquals(value, content.value)
            queue.release(content)
        }
        queue.cancel()
        assertEquals((1..100).toSet(), released.toSet())
        assertEquals(100, released.size)
    }

    @Test
    fun `late root result cannot settle a newer foreground context`() = runBlocking {
        val queue = AccessibilityEventQueue<String> {}
        queue.offer("uber", changesContext = true)
        val oldRootRead = queue.receive()
        queue.offer("blocked-app", changesContext = true)
        assertFalse(queue.isCurrent(oldRootRead))
        queue.markApplied(oldRootRead)
        assertFalse(queue.isContextSettled)
        val current = queue.receive()
        assertEquals("blocked-app", current.value)
        queue.markApplied(current)
        assertTrue(queue.isContextSettled)
        queue.cancel()
        assertFalse(queue.isCurrent(current))
        assertFalse(queue.isContextSettled)
    }

    @Test
    fun `foreground transition takes priority and invalidates old content`() = runBlocking {
        val released = mutableListOf<String>()
        val queue = AccessibilityEventQueue<String>(release = released::add)
        queue.offer("old card", changesContext = false)
        queue.offer("99 foreground", changesContext = true)
        queue.offer("99 text", changesContext = false)
        val foreground = queue.receive()
        assertEquals("99 foreground", foreground.value)
        queue.markApplied(foreground)
        queue.release(foreground)
        val oldContent = queue.receive()
        assertEquals("old card", oldContent.value)
        assertFalse(queue.isCurrent(oldContent))
        queue.release(oldContent)
        val newContent = queue.receive()
        assertEquals("99 text", newContent.value)
        assertTrue(queue.isCurrent(newContent))
        queue.release(newContent)
        queue.cancel()
        assertEquals(3, released.size)
    }

    @Test
    fun `ignored events do not authorize a pending context`() = runBlocking {
        val queue = AccessibilityEventQueue<String> {}
        queue.offer("99", changesContext = true)
        val foreground = queue.receive()
        queue.offer("system", changesContext = false)
        val ignored = queue.receive()
        assertTrue(queue.isCurrent(foreground))
        queue.markApplied(ignored)
        assertFalse(queue.isContextSettled)
        queue.markApplied(foreground)
        assertTrue(queue.isContextSettled)
        queue.cancel()
    }

    @Test
    fun `obsolete foreground and shutdown recycle every pending event exactly once`() {
        val released = mutableListOf<Int>()
        val queue = AccessibilityEventQueue<Int>(release = released::add)
        queue.offer(1, changesContext = true)
        queue.offer(2, changesContext = true)
        queue.offer(3, changesContext = false)
        queue.cancel()
        queue.cancel()
        queue.offer(4, changesContext = true)
        queue.offer(5, changesContext = false)
        assertEquals((1..5).toSet(), released.toSet())
        assertEquals(5, released.size)
    }

    @Test
    fun `blocked worker does not block callback submissions and rejects stale result`() = runBlocking {
        val released = java.util.concurrent.CopyOnWriteArrayList<Int>()
        val queue = AccessibilityEventQueue<Int>(capacity = 2, release = released::add)
        val enteredRead = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            queue.offer(1, changesContext = true)
            val worker = launch(dispatcher) {
                val pending = queue.receive()
                try {
                    enteredRead.countDown()
                    check(releaseRead.await(5, TimeUnit.SECONDS))
                    assertFalse(queue.isCurrent(pending))
                    queue.markApplied(pending)
                } finally {
                    queue.release(pending)
                }
            }
            assertTrue(enteredRead.await(5, TimeUnit.SECONDS))
            // These calls must finish while the root-query worker is still blocked.
            queue.offer(2, changesContext = true)
            (3..100).forEach { queue.offer(it, changesContext = false) }
            assertFalse(queue.isContextSettled)
            assertEquals(1L, releaseRead.count)
            releaseRead.countDown()
            worker.join()
            assertFalse(queue.isContextSettled)
            queue.cancel()
            assertEquals(100, released.size)
            assertEquals(100, released.toSet().size)
        } finally {
            releaseRead.countDown()
            queue.cancel()
            dispatcher.close()
        }
    }
}
