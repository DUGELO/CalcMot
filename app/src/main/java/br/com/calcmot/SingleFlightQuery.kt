package br.com.calcmot

import java.util.concurrent.atomic.AtomicBoolean

/** Bounded on-demand query: a slow Binder cannot accumulate queued refreshes. */
internal class SingleFlightQuery {
    private val running = AtomicBoolean(false)
    fun begin(): Boolean = running.compareAndSet(false, true)
    fun end() { running.set(false) }
}
