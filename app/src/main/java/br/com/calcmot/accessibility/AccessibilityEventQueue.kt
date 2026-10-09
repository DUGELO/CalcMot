package br.com.calcmot.accessibility

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

/** Owns copied events until consumption, overflow, or service shutdown. */
internal class AccessibilityEventQueue<T>(
    capacity: Int = 16,
    private val release: (T) -> Unit
) {
    class Pending<T>(val value: T, val contextRevision: Long, val changesContext: Boolean)

    private val closed = AtomicBoolean(false)
    private val revision = AtomicLong(0)
    private val appliedRevision = AtomicLong(0)
    // Content bursts must never evict the newest foreground decision.
    private val contexts = Channel<Pending<T>>(
        capacity = Channel.CONFLATED,
        onUndeliveredElement = { release(it.value) }
    )
    private val events = Channel<Pending<T>>(
        capacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { release(it.value) }
    )

    fun offer(value: T, changesContext: Boolean) {
        val contextRevision = if (changesContext) revision.incrementAndGet() else revision.get()
        val destination = if (changesContext) contexts else events
        if (destination.trySend(Pending(value, contextRevision, changesContext)).isFailure) release(value)
    }

    suspend fun receive(): Pending<T> = select {
        contexts.onReceive { it }
        events.onReceive { it }
    }

    fun isCurrent(pending: Pending<T>): Boolean = isCurrent(pending.contextRevision)

    fun isCurrent(contextRevision: Long): Boolean =
        !closed.get() && contextRevision == revision.get()

    fun markApplied(pending: Pending<T>) {
        if (pending.changesContext && isCurrent(pending)) appliedRevision.set(pending.contextRevision)
    }

    val isContextSettled: Boolean
        get() = !closed.get() && appliedRevision.get() == revision.get()

    fun release(pending: Pending<T>) = release(pending.value)

    fun cancel() {
        closed.set(true)
        contexts.cancel()
        events.cancel()
    }
}
