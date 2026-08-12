package br.com.calcmot.telemetry

interface CrashReporter {
    fun recordNonFatal(
        error: Throwable,
        reason: String,
        params: Map<String, String> = emptyMap()
    )
}

internal object NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(error: Throwable, reason: String, params: Map<String, String>) = Unit
}
