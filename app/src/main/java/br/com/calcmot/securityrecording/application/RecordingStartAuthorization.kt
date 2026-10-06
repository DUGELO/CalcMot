package br.com.calcmot.securityrecording.application

import android.os.SystemClock
import java.util.UUID

/**
 * Single-use, short-lived proof minted only by the visible confirmation handler.
 * It replaces a forgeable boolean extra and is intentionally process-local.
 */
object RecordingStartAuthorization {
    private const val VALIDITY_MS = 15_000L
    private val expirations = mutableMapOf<String, Long>()

    @Synchronized
    fun issue(): String {
        removeExpired()
        return UUID.randomUUID().toString().also {
            expirations[it] = SystemClock.elapsedRealtime() + VALIDITY_MS
        }
    }

    @Synchronized
    fun consume(token: String?): Boolean {
        removeExpired()
        val expiration = expirations.remove(token) ?: return false
        return expiration >= SystemClock.elapsedRealtime()
    }

    private fun removeExpired() {
        val now = SystemClock.elapsedRealtime()
        expirations.entries.removeAll { it.value < now }
    }
}
