package br.com.calcmot.analytics

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import br.com.calcmot.BuildConfig
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.microsoft.clarity.models.LogLevel

/** App-wide SDK defaults; no app-defined masking, pause policy, or Activity exclusion. */
internal object ClarityIntegration {
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private var initialized = false
    private var sessionReady = false
    private var currentScreen: String? = null
    private data class Signal(val event: String, val tags: Map<String, String>)
    private val pending = ArrayDeque<Signal>()

    fun initialize(context: Context) {
        if (initialized || Build.VERSION.SDK_INT < 29 || BuildConfig.CLARITY_PROJECT_ID.isBlank()) return
        initialized = runCatching {
            Clarity.initialize(context.applicationContext, ClarityConfig(
                projectId = BuildConfig.CLARITY_PROJECT_ID,
                logLevel = LogLevel.None
            ))
        }.getOrDefault(false)
        if (initialized) runCatching {
            Clarity.setOnSessionStartedCallback {
                sessionReady = true
                currentScreen?.let { Clarity.setCurrentScreenName(it) }
                while (pending.isNotEmpty()) send(pending.removeFirst())
            }
        }
    }

    fun screenView(namespace: String, route: String) = onMain {
        if (!initialized) return@onMain
        val screen = productScreenName(namespace, route)
        if (screen == currentScreen) return@onMain
        currentScreen = screen
        if (sessionReady) runCatching { Clarity.setCurrentScreenName(screen) }
        enqueue(Signal("view_$screen", emptyMap()))
    }

    /** Existing pipeline signals arrive only after its sanitizer/throttle; no raw OCR text. */
    fun track(event: String, tags: Map<String, String> = emptyMap()) {
        val signal = Signal(event, tags.toMap())
        onMain { if (initialized) enqueue(signal) }
    }

    private fun enqueue(signal: Signal) {
        if (sessionReady) send(signal)
        else {
            // This bounds memory only during asynchronous startup; SDK manages session buffering.
            if (pending.size >= 256) pending.removeFirst()
            pending.addLast(signal)
        }
    }

    private fun send(signal: Signal) {
        runCatching {
            signal.tags.forEach { (key, value) -> Clarity.setCustomTag("calcmot_$key", value) }
            Clarity.sendCustomEvent(signal.event)
        }
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else main.post { action() }
    }
}
