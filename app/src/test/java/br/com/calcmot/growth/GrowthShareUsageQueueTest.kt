package br.com.calcmot.growth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthShareUsageQueueTest {
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queue = GrowthShareUsageQueue(background)

    @After fun cleanup() = background.cancel()

    @Test
    fun `hundreds of overlays on the same day make only one disk write`() = runBlocking {
        val writes = mutableListOf<Long>()
        repeat(500) { queue.enqueue(100L) { writes.add(100L); true } }
        withTimeout(5_000) { queue.awaitIdle() }
        assertEquals(listOf(100L), writes)
    }

    @Test
    fun `days are persisted in order across midnight preserving consecutive streak`() = runBlocking {
        var state = GrowthShareState()
        fun record(day: Long) {
            queue.enqueue(day) {
                state = GrowthSharePolicy.onOverlayShown(state, day)
                true
            }
        }
        record(100L)
        record(101L)
        record(102L)
        withTimeout(5_000) { queue.awaitIdle() }
        assertEquals(102L, state.lastUsageDay)
        assertEquals(3, state.consecutiveDays)
    }

    @Test
    fun `failed commit can be retried by a later overlay on same day`() = runBlocking {
        var writes = 0
        queue.enqueue(110L) { writes++; false }
        withTimeout(5_000) { queue.awaitIdle() }
        queue.enqueue(110L) { writes++; true }
        withTimeout(5_000) { queue.awaitIdle() }
        assertEquals(2, writes)
    }

    @Test
    fun `unexpected exceptions do not stop the worker or poison deduplication`() = runBlocking {
        queue.enqueue(200L) { error("disk unavailable") }
        withTimeout(5_000) { queue.awaitIdle() }
        var success = false
        queue.enqueue(200L) { success = true; true }
        withTimeout(5_000) { queue.awaitIdle() }
        assertTrue(success)
    }

    @Test
    fun `failed earlier write does not clear newer queued day`() = runBlocking {
        val writes = mutableListOf<Long>()
        queue.enqueue(300L) { writes.add(300L); false }
        queue.enqueue(301L) { writes.add(301L); true }
        withTimeout(5_000) { queue.awaitIdle() }
        queue.enqueue(301L) { writes.add(301L); true }
        withTimeout(5_000) { queue.awaitIdle() }
        assertEquals(listOf(300L, 301L), writes)
    }

    @Test
    fun `persist callback runs outside caller thread`() = runBlocking {
        val caller = Thread.currentThread().name
        var writer: String? = null
        queue.enqueue(401L) { writer = Thread.currentThread().name; true }
        withTimeout(5_000) { queue.awaitIdle() }
        assertNotEquals(caller, writer)
    }
}
