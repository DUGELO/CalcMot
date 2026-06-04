package br.com.calcmot.overlay

class OverlayStateMachine(
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    var state: OverlayUiState = OverlayUiState.Hidden
        private set

    fun showRequested(fingerprint: String): OverlayTransition {
        val previous = state
        state = when (previous) {
            is OverlayUiState.Showing -> {
                if (previous.fingerprint == fingerprint) {
                    OverlayUiState.Updating(fingerprint = fingerprint, updatedAtMillis = clockMillis())
                } else {
                    OverlayUiState.Showing(fingerprint = fingerprint, shownAtMillis = clockMillis())
                }
            }

            is OverlayUiState.Updating -> {
                if (previous.fingerprint == fingerprint) {
                    OverlayUiState.Updating(fingerprint = fingerprint, updatedAtMillis = clockMillis())
                } else {
                    OverlayUiState.Showing(fingerprint = fingerprint, shownAtMillis = clockMillis())
                }
            }

            else -> OverlayUiState.Showing(fingerprint = fingerprint, shownAtMillis = clockMillis())
        }

        return if (
            previous is OverlayUiState.Showing && previous.fingerprint == fingerprint ||
            previous is OverlayUiState.Updating && previous.fingerprint == fingerprint
        ) {
            OverlayTransition.UpdateInPlace
        } else {
            OverlayTransition.AttachOrReplace
        }
    }

    fun markShown(fingerprint: String) {
        state = OverlayUiState.Showing(fingerprint = fingerprint, shownAtMillis = clockMillis())
    }

    fun markTokenRecovering(fingerprint: String?) {
        state = OverlayUiState.TokenRecovering(fingerprint = fingerprint, startedAtMillis = clockMillis())
    }

    fun markHidden() {
        state = OverlayUiState.Hidden
    }

    fun markExpired(fingerprint: String?) {
        state = OverlayUiState.Expired(fingerprint = fingerprint, expiredAtMillis = clockMillis())
    }

    fun markDismissed(fingerprint: String?, suppressMillis: Long) {
        state = OverlayUiState.Dismissed(
            fingerprint = fingerprint,
            dismissedAtMillis = clockMillis(),
            suppressUntilMillis = clockMillis() + suppressMillis
        )
    }

    fun currentFingerprint(): String? {
        return when (val current = state) {
            is OverlayUiState.Showing -> current.fingerprint
            is OverlayUiState.Updating -> current.fingerprint
            is OverlayUiState.Dismissed -> current.fingerprint
            is OverlayUiState.Expired -> current.fingerprint
            is OverlayUiState.TokenRecovering -> current.fingerprint
            OverlayUiState.Hidden -> null
        }
    }
}

sealed interface OverlayUiState {
    data object Hidden : OverlayUiState
    data class Showing(val fingerprint: String, val shownAtMillis: Long) : OverlayUiState
    data class Updating(val fingerprint: String, val updatedAtMillis: Long) : OverlayUiState
    data class Dismissed(
        val fingerprint: String?,
        val dismissedAtMillis: Long,
        val suppressUntilMillis: Long
    ) : OverlayUiState

    data class Expired(val fingerprint: String?, val expiredAtMillis: Long) : OverlayUiState
    data class TokenRecovering(val fingerprint: String?, val startedAtMillis: Long) : OverlayUiState
}

enum class OverlayTransition {
    AttachOrReplace,
    UpdateInPlace
}
