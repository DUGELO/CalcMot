package br.com.calcmot.growth

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Bounded work on the overlay thread (atomic + nonblocking enqueue).
 * Exactly one IO worker processes calendar days in order, so crossing midnight
 * does not accidentally lose the first day of a streak.
 *
 * A failed/throwing persist is retryable on the next actual overlay.
 * Never schedules retries, notifications or disk work on the overlay thread.
 */
internal class GrowthShareUsageQueue(scope: CoroutineScope) {
    private sealed interface Work {
        data class Record(val day: Long, val persist: () -> Boolean) : Work
        data class Barrier(val done: CompletableDeferred<Unit>) : Work
    }

    private val lastSubmittedDay = AtomicLong(-1L)
    private val work = Channel<Work>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (item in work) {
                when (item) {
                    is Work.Record -> {
                        val saved = runCatching(item.persist).getOrDefault(false)
                        if (!saved) {
                            // Avoid overwriting a more recent day's submitted value.
                            lastSubmittedDay.compareAndSet(item.day, -1L)
                        }
                    }
                    is Work.Barrier -> item.done.complete(Unit)
                }
            }
        }
    }

    fun enqueue(day: Long, persist: () -> Boolean) {
        if (lastSubmittedDay.getAndSet(day) == day) return
        if (work.trySend(Work.Record(day, persist)).isFailure) {
            lastSubmittedDay.compareAndSet(day, -1L)
        }
    }

    /** A deterministic barrier for JVM/instrumented tests, not used in production. */
    suspend fun awaitIdle() {
        val done = CompletableDeferred<Unit>()
        work.send(Work.Barrier(done))
        done.await()
    }
}
