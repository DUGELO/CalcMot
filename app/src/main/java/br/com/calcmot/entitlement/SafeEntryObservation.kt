package br.com.calcmot.entitlement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whole immutable observation from a future trusted runtime producer. No IDLE/toggle inference.
 * generation must change on new work/session/service lifetime, and completion must acknowledge
 * exactly that generation after all work drained. Missing producer -> UNKNOWN, never qualified.
 */
internal data class SessionObservation(val session: String?, val generation: Long,
    val endRequested: Boolean?, val completedGeneration: Long?, val inFlight: Int?,
    val overlayVisible: Boolean?, val producerHealthy: Boolean?)
internal enum class EntryOrigin { VOLUNTARY_MAIN_ACTION, RESUME, EXTERNAL, UNKNOWN }
internal enum class SessionReadiness { UNKNOWN, WORKING, END_CONFIRMED }
internal data class SafeEntryState(val observation: SessionObservation? = null,
    val readiness: SessionReadiness = SessionReadiness.UNKNOWN)

/** Observation only. Not instantiated by Core and no UI, network, stop/hide command or gate.
 * Actual drain acknowledgment cannot be manufactured from existing runtime Snapshot fields.
 */
internal class SafeEntryObservation {
    private val mutable = MutableStateFlow(SafeEntryState())
    val state: StateFlow<SafeEntryState> = mutable.asStateFlow()
    val commercialGatesEnabled: Boolean get() = false
    private var highWater = -1L
    private var minimumGeneration = 0L

    @Synchronized fun observe(observation: SessionObservation) {
        val before = mutable.value.observation
        if (observation.generation !in minimumGeneration until Long.MAX_VALUE ||
            (before != null && observation.generation < before.generation) ||
            (before != null && observation.generation == before.generation && observation.session != before.session)) {
            invalidate(); return
        }
        highWater = maxOf(highWater, observation.generation)
        if (observation.producerHealthy != true) { invalidate(); return }
        val known = !observation.session.isNullOrBlank() && observation.producerHealthy == true &&
            observation.endRequested != null && observation.inFlight != null && observation.inFlight >= 0 &&
            observation.overlayVisible != null
        val readiness = when {
            !known -> SessionReadiness.UNKNOWN
            observation.endRequested == true && observation.completedGeneration == observation.generation &&
                observation.inFlight == 0 && observation.overlayVisible == false -> SessionReadiness.END_CONFIRMED
            else -> SessionReadiness.WORKING
        }
        mutable.value = SafeEntryState(observation, readiness)
    }

    @Synchronized fun invalidate() {
        minimumGeneration = highWater + 1
        mutable.value = SafeEntryState()
    }

    /** Check again at intent consumption; captured generation cannot authorize after new work. */
    @Synchronized fun qualified(origin: EntryOrigin, mainFocused: Boolean, expectedGeneration: Long): Boolean =
        origin == EntryOrigin.VOLUNTARY_MAIN_ACTION && mainFocused &&
            mutable.value.readiness == SessionReadiness.END_CONFIRMED &&
            mutable.value.observation?.generation == expectedGeneration
}
