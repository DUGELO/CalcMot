package br.com.calcmot.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseCrashReporter : CrashReporter {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun recordNonFatal(
        error: Throwable,
        reason: String,
        params: Map<String, String>
    ) {
        val safeReason = SafeTelemetryPolicy.safeReason(reason)
        val safeParams = SafeTelemetryPolicy.sanitizeParams(
            params + (AnalyticsParams.REASON to safeReason)
        )
        runCatching {
            safeParams.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
            val sanitized = RuntimeException(
                "CalcMot non-fatal: $safeReason (${error.javaClass.simpleName})"
            ).apply {
                stackTrace = error.stackTrace
            }
            crashlytics.recordException(sanitized)
        }
    }
}
