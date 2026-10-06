package br.com.calcmot.securityrecording.domain

import java.util.UUID

/** Commands from UI and notification share this envelope; no caller may mutate capture directly. */
data class RecordingCommand(
    val commandId: String = UUID.randomUUID().toString(),
    val expectedSessionRevision: Long,
    val action: Action,
    val sessionId: String? = null
) {
    enum class Action { START, STOP, PAUSE, RESUME }
}

sealed interface CommandResult {
    data class Accepted(val sessionId: String, val revision: Long) : CommandResult
    data class Rejected(val reason: RejectReason) : CommandResult
    enum class RejectReason { STALE_REVISION, INVALID_STATE, NOT_CONFIRMED, WRONG_SESSION }
}

/** Thread-safe, deliberately small state machine used by the service's single writer. */
class RecordingCommandGate(initialRevision: Long = 0L) {
    private val completed = mutableMapOf<String, CommandResult>()
    private var revision = initialRevision
    private var phase = RecordingPhase.IDLE
    private var activeSessionId: String? = null

    @Synchronized fun execute(command: RecordingCommand, explicitActivityConfirmation: Boolean): CommandResult {
        completed[command.commandId]?.let { return it }
        val result = when (command.action) {
            RecordingCommand.Action.START -> when {
                !explicitActivityConfirmation -> CommandResult.Rejected(CommandResult.RejectReason.NOT_CONFIRMED)
                command.expectedSessionRevision != revision -> CommandResult.Rejected(CommandResult.RejectReason.STALE_REVISION)
                phase != RecordingPhase.IDLE && phase != RecordingPhase.VERIFIED && phase != RecordingPhase.FAILED -> CommandResult.Rejected(CommandResult.RejectReason.INVALID_STATE)
                else -> {
                    activeSessionId = command.sessionId ?: UUID.randomUUID().toString()
                    phase = RecordingPhase.PREPARING
                    revision += 1
                    CommandResult.Accepted(activeSessionId!!, revision)
                }
            }
            RecordingCommand.Action.PAUSE, RecordingCommand.Action.RESUME -> CommandResult.Rejected(CommandResult.RejectReason.INVALID_STATE)
            RecordingCommand.Action.STOP -> when {
                command.sessionId != null && command.sessionId != activeSessionId -> CommandResult.Rejected(CommandResult.RejectReason.WRONG_SESSION)
                command.expectedSessionRevision != revision -> CommandResult.Rejected(CommandResult.RejectReason.STALE_REVISION)
                phase != RecordingPhase.RECORDING && phase != RecordingPhase.PREPARING -> CommandResult.Rejected(CommandResult.RejectReason.INVALID_STATE)
                else -> {
                    phase = RecordingPhase.FINALIZING
                    revision += 1
                    CommandResult.Accepted(activeSessionId ?: return CommandResult.Rejected(CommandResult.RejectReason.INVALID_STATE), revision)
                }
            }
        }
        completed[command.commandId] = result
        return result
    }

    @Synchronized fun markCaptureConfirmed() { if (phase == RecordingPhase.PREPARING) { phase = RecordingPhase.RECORDING; revision += 1 } }
    @Synchronized fun markTerminal(success: Boolean) { phase = if (success) RecordingPhase.VERIFIED else RecordingPhase.FAILED; revision += 1; activeSessionId = null }
    @Synchronized fun snapshot() = RecordingRuntimeSnapshot(activeSessionId, phase, revision, audioVideoConfirmed = phase == RecordingPhase.RECORDING)
}

data class RecordingRuntimeSnapshot(
    val sessionId: String?,
    val phase: RecordingPhase,
    val sessionRevision: Long,
    val failureMessage: String? = null,
    val capturedDurationMs: Long? = null,
    val audioVideoConfirmed: Boolean = false
) {
    val isRecVisible: Boolean get() = phase == RecordingPhase.RECORDING && audioVideoConfirmed
}
