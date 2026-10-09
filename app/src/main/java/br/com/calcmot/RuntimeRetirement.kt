package br.com.calcmot

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout

/** Await retired work even when Android recreates the Service object in the same process. */
internal object RuntimeRetirement {
    private val retired = AtomicReference<List<Job>>(emptyList())

    fun retain(jobs: List<Job>) {
        jobs.distinct().forEach { job ->
            while (!job.isCompleted) {
                val previous = retired.get()
                if (job in previous) break
                if (retired.compareAndSet(previous, (previous + job).filterNot { it.isCompleted })) {
                    job.invokeOnCompletion { retired.updateAndGet { list -> list.filterNot { it.isCompleted } } }
                    break
                }
            }
        }
    }

    suspend fun await(predecessor: Job?, timeoutMillis: Long) {
        awaitAll(listOfNotNull(predecessor), timeoutMillis)
    }
    suspend fun awaitAll(predecessors: List<Job>, timeoutMillis: Long) {
        val pending = (retired.get() + predecessors).distinct()
        withTimeout(timeoutMillis) { pending.forEach { it.join() } }
    }
}
