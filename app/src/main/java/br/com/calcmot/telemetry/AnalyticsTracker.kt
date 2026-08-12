package br.com.calcmot.telemetry

interface AnalyticsTracker {
    fun track(event: String, params: Map<String, String> = emptyMap())
}

internal object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) = Unit
}
