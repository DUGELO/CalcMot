package br.com.calcmot.securityrecording.application

import kotlinx.coroutines.*

/** Work survives navigation; process death is recovered from the operation claim. */
internal object RecordingOperationRunner {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun <T> start(block: suspend () -> T): Deferred<T> = scope.async { block() }
}
