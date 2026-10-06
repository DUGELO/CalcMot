package br.com.calcmot.securityrecording.domain

enum class TemporaryAvailability { AVAILABLE, EXPIRED, DELETED }
enum class GalleryCopyState { NONE, COPYING, PUBLISHED, FAILED, MISSING }
enum class RecordingExportState { IDLE, BUILDING, READY, FAILED }
enum class RecordingOperationKind { RECOVERY, FINALIZE, PUBLISH, EXPORT, EXPIRE, DELETE, EXTEND }
enum class RecordingOperationStatus { CLAIMED, COMMITTING, ROLLBACK_REQUIRED }

object RecordingRetention {
    const val DEFAULT_DAYS = 1
    const val DAY_MS = 86_400_000L
    val options = listOf(1, 3, 7, 15, 30)

    fun expiresAt(finalizedAtUtcMs: Long, days: Int): Long {
        require(days in options)
        return Math.addExact(finalizedAtUtcMs, days * DAY_MS)
    }

    fun canExtend(currentDays: Int?, newDays: Int, expiresAt: Long?, now: Long): Boolean =
        currentDays != null && newDays in options && newDays > currentDays &&
            expiresAt != null && now < expiresAt

    fun isAvailable(availability: TemporaryAvailability, expiresAt: Long?, now: Long): Boolean =
        availability == TemporaryAvailability.AVAILABLE && (expiresAt == null || now < expiresAt)

    fun shareCleanupAt(expiresAt: Long, lastShareLaunchedAt: Long?): Long =
        if (lastShareLaunchedAt == null) expiresAt else maxOf(expiresAt, Math.addExact(lastShareLaunchedAt, 3_600_000L))
}
