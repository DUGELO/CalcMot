package br.com.calcmot.telemetry

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Local-only S01 receipt. Never pass this object, its ID or its owner to an SDK. */
internal data class CoreAnalysisReceipt(
    val analysisId: String,
    val identityKey: String,
    val occurredAt: String,
    val localDate: String,
    val timeZoneId: String,
    val appFamily: String,
    val classification: String
) {
    fun isValid(): Boolean = runCatching {
        require(analysisId.isNotBlank() && analysisId.length <= 128)
        require(identityKey.isNotBlank() && identityKey.length <= 256)
        require(timeZoneId in zoneIds)
        require(appFamily in families)
        require(classification in classifications)
        val utc = format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"))
        val parsed = requireNotNull(utc.parse(occurredAt))
        require(utc.format(parsed) == occurredAt)
        require(format("yyyy-MM-dd", TimeZone.getTimeZone(timeZoneId)).format(parsed) == localDate)
    }.isSuccess

    companion object {
        private val zoneIds = TimeZone.getAvailableIDs().toSet()
        private val families = setOf("uber", "99")
        private val classifications = setOf("good", "medium", "bad", "excellent", "unknown")
        /** Called at original success, never on replay. Caller must establish clock confidence. */
        fun atSuccess(
            analysisId: String,
            identityKey: String,
            utcMillis: Long,
            timeZoneId: String,
            appFamily: String,
            classification: String
        ): CoreAnalysisReceipt {
            require(timeZoneId in zoneIds)
            val instant = Date(utcMillis)
            return CoreAnalysisReceipt(analysisId, identityKey,
                format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC")).format(instant),
                format("yyyy-MM-dd", TimeZone.getTimeZone(timeZoneId)).format(instant),
                timeZoneId, appFamily, classification).also { require(it.isValid()) }
        }

        private fun format(pattern: String, zone: TimeZone) = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = zone
            isLenient = false
        }
    }
}

internal enum class CoreAnalysisKind { REAL, DEMO }
