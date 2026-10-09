package br.com.calcmot

/** Bounds consecutive automatic resets; only fresh successful reading or explicit restart renews it. */
internal class WatchdogRecoveryBudget {
    private var attempts = 0
    private var successfulRead = 0L
    @Synchronized
    fun allow(lastSuccessfulRead: Long): Boolean {
        if (lastSuccessfulRead > successfulRead) { attempts = 0; successfulRead = lastSuccessfulRead }
        if (attempts >= 3) return false
        attempts++
        return true
    }
    @Synchronized
    fun reset() { attempts = 0 }
}
