package br.com.calcmot

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import br.com.calcmot.telemetry.AnalyticsEvents
import br.com.calcmot.telemetry.AnalyticsParams
import br.com.calcmot.telemetry.AnalyticsValues
import br.com.calcmot.telemetry.TelemetryProvider
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-local bridge between the product UI and the AccessibilityService runtime. */
object ReadingPipelineRuntime {
    private const val TAG = "CalcMotReading"

    private val commandHandler = AtomicReference<CommandHandler?>(null)
    private val mutableSnapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = mutableSnapshot.asStateFlow()

    fun register(handler: CommandHandler) {
        commandHandler.set(handler)
        Log.w(TAG, "CALCMOT_READING_SERVICE_REGISTER handler=${System.identityHashCode(handler)}")
        update { it.copy(serviceConnected = true, lastError = null) }
    }

    fun unregister(handler: CommandHandler) {
        val removed = commandHandler.compareAndSet(handler, null)
        Log.w(
            TAG,
            "CALCMOT_READING_SERVICE_UNREGISTER handler=${System.identityHashCode(handler)} removed=$removed"
        )
        if (!removed) return
        update {
            it.copy(
                serviceConnected = false,
                pipelineState = PipelineState.FAILED,
                stateSinceElapsedRealtime = SystemClock.elapsedRealtime(),
                lastError = "Serviço de acessibilidade desconectado"
            )
        }
    }

    fun selectPlatform(context: Context, platform: DriverApp) {
        if (platform == DriverApp.UNKNOWN) return
        AppSettings.setLastDriverApp(context, platform)
        Log.w(TAG, "CALCMOT_PLATFORM_SELECTED platform=${platform.id}")
        TelemetryProvider.analytics.track(
            AnalyticsEvents.PLATFORM_SELECTED,
            mapOf(
                AnalyticsParams.PLATFORM to platform.id,
                AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_HOME
            )
        )
        update { it.copy(selectedPlatform = platform, lastError = null) }
        requestRestart(platform = platform, reason = RestartReason.PLATFORM_SELECTED)
    }

    fun manualRestart(context: Context): Boolean {
        val platform = AppSettings.getLastDriverApp(context)
        Log.w(TAG, "CALCMOT_MANUAL_RESTART_READING platform=${platform.id}")
        TelemetryProvider.analytics.track(
            AnalyticsEvents.MANUAL_RESTART_READING,
            mapOf(
                AnalyticsParams.PLATFORM to platform.id,
                AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_HOME,
                AnalyticsParams.PIPELINE_STATE to current().pipelineState.name.lowercase()
            )
        )
        update { it.copy(selectedPlatform = platform, lastError = null) }
        return requestRestart(platform = null, reason = RestartReason.MANUAL)
    }

    fun markPipelineStarted(platform: DriverApp) {
        Log.w(TAG, "CALCMOT_PIPELINE_START platform=${platform.id}")
        val now = SystemClock.elapsedRealtime()
        update {
            it.copy(
                selectedPlatform = platform,
                serviceConnected = commandHandler.get() != null,
                pipelineState = PipelineState.IDLE,
                stateSinceElapsedRealtime = now,
                lastActivityElapsedRealtime = now,
                lastError = null
            )
        }
    }

    fun markPipelineStopped(platform: DriverApp, reason: String) {
        Log.w(TAG, "CALCMOT_PIPELINE_STOP platform=${platform.id} reason=$reason")
        update {
            it.copy(
                pipelineState = PipelineState.IDLE,
                stateSinceElapsedRealtime = SystemClock.elapsedRealtime()
            )
        }
    }

    fun markPipelineState(platform: DriverApp, state: PipelineState) {
        update {
            it.copy(
                selectedPlatform = platform.takeIf { value -> value != DriverApp.UNKNOWN }
                    ?: it.selectedPlatform,
                pipelineState = state,
                stateSinceElapsedRealtime = SystemClock.elapsedRealtime(),
                lastActivityElapsedRealtime = SystemClock.elapsedRealtime()
            )
        }
    }

    fun markDriverEvent(platform: DriverApp) {
        update {
            it.copy(
                selectedPlatform = platform,
                lastDriverEventAtMillis = System.currentTimeMillis(),
                lastActivityElapsedRealtime = SystemClock.elapsedRealtime()
            )
        }
    }

    fun markSuccessfulRead(platform: DriverApp) {
        update {
            it.copy(
                selectedPlatform = platform,
                pipelineState = PipelineState.IDLE,
                stateSinceElapsedRealtime = SystemClock.elapsedRealtime(),
                lastSuccessfulReadAtMillis = System.currentTimeMillis(),
                lastActivityElapsedRealtime = SystemClock.elapsedRealtime(),
                lastError = null
            )
        }
    }

    fun markFailure(platform: DriverApp, message: String) {
        update {
            it.copy(
                selectedPlatform = platform.takeIf { value -> value != DriverApp.UNKNOWN }
                    ?: it.selectedPlatform,
                pipelineState = PipelineState.FAILED,
                stateSinceElapsedRealtime = SystemClock.elapsedRealtime(),
                lastError = message
            )
        }
    }

    fun markReset(platform: DriverApp, reason: String) {
        Log.w(TAG, "CALCMOT_PIPELINE_RESET platform=${platform.id} reason=$reason")
        TelemetryProvider.analytics.track(
            AnalyticsEvents.PIPELINE_RESET,
            mapOf(
                AnalyticsParams.PLATFORM to platform.id,
                AnalyticsParams.SOURCE to if (reason.startsWith("watchdog_")) {
                    "system"
                } else {
                    AnalyticsValues.SOURCE_HOME
                },
                AnalyticsParams.REASON to reason,
                AnalyticsParams.PIPELINE_STATE to PipelineState.IDLE.name.lowercase()
            )
        )
        update {
            it.copy(
                selectedPlatform = platform,
                pipelineState = PipelineState.IDLE,
                stateSinceElapsedRealtime = SystemClock.elapsedRealtime(),
                lastActivityElapsedRealtime = SystemClock.elapsedRealtime(),
                lastResetAtMillis = System.currentTimeMillis(),
                lastError = null
            )
        }
    }

    fun markReadingRestarted(platform: DriverApp) {
        Log.w(TAG, "CALCMOT_READING_RESTARTED platform=${platform.id}")
    }

    fun markBusyTooLong(platform: DriverApp) {
        Log.w(TAG, "CALCMOT_PIPELINE_BUSY_TOO_LONG platform=${platform.id}")
    }

    fun current(): Snapshot = mutableSnapshot.value

    fun diagnosticsText(context: Context, accessibilityActive: Boolean): String {
        val current = current()
        val overlayActive = accessibilityActive
        val battery = batteryOptimizationLabel(context)
        Log.w(TAG, "CALCMOT_ACCESSIBILITY_STATUS active=$accessibilityActive")
        Log.w(TAG, "CALCMOT_OVERLAY_PERMISSION_STATUS active=$overlayActive")
        Log.w(TAG, "CALCMOT_BATTERY_OPTIMIZATION_STATUS value=$battery")
        return buildString {
            appendLine("CalcMot ${BuildConfig.VERSION_NAME}")
            appendLine("Plataforma: ${current.selectedPlatform.displayName}")
            appendLine("Acessibilidade: ${activeLabel(accessibilityActive)}")
            appendLine("Overlay: ${activeLabel(overlayActive)}")
            appendLine("Bateria: $battery")
            appendLine("Pipeline: ${current.pipelineState.label}")
            appendLine("Serviço conectado: ${activeLabel(current.serviceConnected)}")
            appendLine("Última leitura: ${formatDate(current.lastSuccessfulReadAtMillis)}")
            appendLine("Último evento: ${formatDate(current.lastDriverEventAtMillis)}")
            appendLine("Último erro: ${current.lastError ?: "Nenhum"}")
        }.trim()
    }

    fun batteryOptimizationLabel(context: Context): String {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true) {
            "Sem restrição"
        } else {
            "Otimização ativa"
        }
    }

    private fun requestRestart(platform: DriverApp?, reason: RestartReason): Boolean {
        val handler = commandHandler.get()
        if (handler == null) {
            update {
                it.copy(
                    pipelineState = PipelineState.FAILED,
                    stateSinceElapsedRealtime = SystemClock.elapsedRealtime(),
                    lastError = "A acessibilidade está inativa ou desconectada"
                )
            }
            return false
        }
        handler.restartReading(platform, reason)
        return true
    }

    private fun update(transform: (Snapshot) -> Snapshot) {
        mutableSnapshot.value = transform(mutableSnapshot.value)
    }

    private fun activeLabel(active: Boolean): String = if (active) "Ativa" else "Inativa"

    private fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Ainda não houve"
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(timestamp))
    }

    fun interface CommandHandler {
        fun restartReading(platform: DriverApp?, reason: RestartReason)
    }

    enum class RestartReason(val logValue: String) {
        PLATFORM_SELECTED("platform_selected"),
        MANUAL("manual"),
        WATCHDOG_BUSY("watchdog_busy"),
        WATCHDOG_FAILED("watchdog_failed"),
        WATCHDOG_STALLED("watchdog_stalled")
    }

    enum class PipelineState(val label: String) {
        IDLE("Idle"),
        CAPTURING("Capturing"),
        OCR("OCR"),
        BUSY("Busy"),
        FAILED("Failed")
    }

    data class Snapshot(
        val selectedPlatform: DriverApp = DriverApp.UBER,
        val serviceConnected: Boolean = false,
        val pipelineState: PipelineState = PipelineState.IDLE,
        val stateSinceElapsedRealtime: Long = 0L,
        val lastActivityElapsedRealtime: Long = 0L,
        val lastSuccessfulReadAtMillis: Long = 0L,
        val lastDriverEventAtMillis: Long = 0L,
        val lastResetAtMillis: Long = 0L,
        val lastError: String? = null
    )
}

internal object ReadingPipelineWatchdogPolicy {
    const val BUSY_TIMEOUT_MS = 25_000L
    const val STALLED_TIMEOUT_MS = 15 * 60_000L
    const val MIN_RESET_INTERVAL_MS = 5 * 60_000L

    fun reason(
        snapshot: ReadingPipelineRuntime.Snapshot,
        nowElapsedRealtime: Long,
        lastResetElapsedRealtime: Long
    ): ReadingPipelineRuntime.RestartReason? {
        if (nowElapsedRealtime - lastResetElapsedRealtime < MIN_RESET_INTERVAL_MS) return null
        val stateAge = nowElapsedRealtime - snapshot.stateSinceElapsedRealtime
        if (snapshot.pipelineState == ReadingPipelineRuntime.PipelineState.FAILED &&
            stateAge >= BUSY_TIMEOUT_MS
        ) {
            return ReadingPipelineRuntime.RestartReason.WATCHDOG_FAILED
        }
        if (snapshot.pipelineState in setOf(
                ReadingPipelineRuntime.PipelineState.CAPTURING,
                ReadingPipelineRuntime.PipelineState.OCR,
                ReadingPipelineRuntime.PipelineState.BUSY
            ) && stateAge >= BUSY_TIMEOUT_MS
        ) {
            return ReadingPipelineRuntime.RestartReason.WATCHDOG_BUSY
        }
        val activityAge = nowElapsedRealtime - snapshot.lastActivityElapsedRealtime
        return ReadingPipelineRuntime.RestartReason.WATCHDOG_STALLED
            .takeIf { activityAge >= STALLED_TIMEOUT_MS }
    }
}
