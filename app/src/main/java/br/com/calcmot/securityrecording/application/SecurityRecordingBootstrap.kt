package br.com.calcmot.securityrecording.application

import android.content.Context
import br.com.calcmot.securityrecording.data.RecordingLibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** One bootstrap per process. Failure is explicit and never authorizes a new capture. */
object SecurityRecordingBootstrap {
    enum class State { CHECKING, READY, FAILED }
    val processEpoch: String = UUID.randomUUID().toString()
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(State.CHECKING)
    val state = mutableState.asStateFlow()
    val isReady: Boolean get() = state.value == State.READY

    internal fun invalidate() { mutableState.value = State.CHECKING }

    suspend fun awaitReady(context: Context): Boolean = mutex.withLock {
        if (isReady) return@withLock true
        mutableState.value = State.CHECKING
        try {
            check(!RecordingResourceCoordinator.hasOpenOutputs()) { "output_close_pending" }
            RecordingLibraryRepository.create(context).reconcilePreviousProcess()
            br.com.calcmot.securityrecording.data.RecordingFilesRepository(context).recoverOperations()
            val runtime = RecordingRuntimeStore.snapshot.value
            runtime.sessionId?.let { id ->
                br.com.calcmot.securityrecording.data.RecordingFilesRepository(context).session(id)?.let { session ->
                    if(session.phase in setOf("VERIFIED", "FAILED")) RecordingRuntimeStore.publish(
                        br.com.calcmot.securityrecording.domain.RecordingRuntimeSnapshot(id,
                            br.com.calcmot.securityrecording.domain.RecordingPhase.valueOf(session.phase), session.revision,
                            if(session.completionReason == "unexpected_end") "A gravação foi interrompida. Confira os trechos disponíveis." else runtime.failureMessage,
                            session.capturedDurationMs, false))
                }
            }
            mutableState.value = State.READY
            true
        } catch (failure: Exception) {
            mutableState.value = State.FAILED
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            false
        }
    }
}
