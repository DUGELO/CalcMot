package br.com.calcmot

/** Main-thread lifecycle retry budget; never enables accessibility or survives unbind. */
internal class ServiceConnectionRecovery {
    private var connected = false
    private var attempts = 0
    fun connected() { connected = true; attempts = 0 }
    fun disconnected() { connected = false }
    fun beginAttempt(): Boolean {
        if (!connected || attempts >= 3) return false
        attempts++
        return true
    }
    fun canRetry(): Boolean = connected && attempts < 3
}
