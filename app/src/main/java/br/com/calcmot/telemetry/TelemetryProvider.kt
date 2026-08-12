package br.com.calcmot.telemetry

import android.content.Context

object TelemetryProvider {
    @Volatile
    var analytics: AnalyticsTracker = NoOpAnalyticsTracker
        private set

    @Volatile
    var crashReporter: CrashReporter = NoOpCrashReporter
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (analytics !== NoOpAnalyticsTracker || crashReporter !== NoOpCrashReporter) return
        analytics = runCatching {
            FirebaseAnalyticsTracker(context.applicationContext)
        }.getOrDefault(NoOpAnalyticsTracker)
        crashReporter = runCatching {
            FirebaseCrashReporter()
        }.getOrDefault(NoOpCrashReporter)
    }

    internal fun installForTests(
        analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
        reporter: CrashReporter = NoOpCrashReporter
    ) {
        analytics = analyticsTracker
        crashReporter = reporter
    }
}
