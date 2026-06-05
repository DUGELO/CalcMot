package br.com.calcmot.accessibility

internal class PostReconnectCandidateGuard(
    private val settleWindowMillis: Long = DEFAULT_SETTLE_WINDOW_MILLIS,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private var settleUntilMillis = 0L
    private var baselineFingerprint: String? = null
    private var ignoredFingerprint: String? = null

    fun onServiceConnected(currentOverlayFingerprint: String?) {
        if (currentOverlayFingerprint == null) {
            settleUntilMillis = 0L
            baselineFingerprint = null
            ignoredFingerprint = null
            return
        }

        settleUntilMillis = clockMillis() + settleWindowMillis
        baselineFingerprint = currentOverlayFingerprint
        ignoredFingerprint = null
    }

    fun shouldIgnore(overlayFingerprint: String): Boolean {
        if (clockMillis() > settleUntilMillis) return false
        if (overlayFingerprint == baselineFingerprint) return false

        if (ignoredFingerprint == null) {
            ignoredFingerprint = overlayFingerprint
            return true
        }

        return ignoredFingerprint == overlayFingerprint
    }

    private companion object {
        const val DEFAULT_SETTLE_WINDOW_MILLIS = 5_000L
    }
}
