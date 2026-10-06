package br.com.calcmot.securityrecording.application

import br.com.calcmot.securityrecording.domain.RecordingPhase
import br.com.calcmot.securityrecording.domain.RecordingRuntimeSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Read-only process projection. Only RecordingSessionService publishes capture facts. */
object RecordingRuntimeStore {
    private val mutableOutcome = MutableStateFlow<String?>(null)
    val outcome = mutableOutcome.asStateFlow()
    internal fun publishOutcome(message: String?) { mutableOutcome.value = message }
    private val mutableSnapshot = MutableStateFlow(
        RecordingRuntimeSnapshot(sessionId = null, phase = RecordingPhase.IDLE, sessionRevision = 0L)
    )
    val snapshot: StateFlow<RecordingRuntimeSnapshot> = mutableSnapshot.asStateFlow()

    internal fun publish(value: RecordingRuntimeSnapshot) {
        mutableSnapshot.value = value
    }
}
