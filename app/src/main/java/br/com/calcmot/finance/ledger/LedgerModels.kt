package br.com.calcmot.finance.ledger

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LedgerConfidence {
    CONFIRMED,
    PARTIAL,
    ESTIMATED,
    INSUFFICIENT,
    PENDING_CONFIRMATION,
    DRIVER_PROVIDED,
    DRIVER_CORRECTED
}

enum class LedgerSource {
    UBER_ACCESSIBILITY,
    NINETY_NINE_VISUAL,
    DRIVER,
    CALCMOT_ESTIMATE,
    LEGACY_MANUAL
}

enum class TripStatus { ACCEPTED, TO_PICKUP, STARTED, COMPLETED, CANCELLED, UNKNOWN }
enum class EarningKind { FARE, BONUS, TIP, ADJUSTMENT, CANCELLATION, CASH, MANUAL }
enum class ExpenseCategory { FUEL, MAINTENANCE, FOOD, TOLL, RENTAL, OTHER }
enum class PlatformSnapshotKind { DAILY, WEEKLY, MONTHLY, PROMOTION, MISSION, HISTORY }
enum class LedgerRecordType { OFFER, TRIP, EARNING, EXPENSE, SESSION, SNAPSHOT }
enum class OfferRealizationState { UNKNOWN, REALIZED_CONFIRMED, NOT_REALIZED_CONFIRMED }

@Entity(
    tableName = "offer_records",
    indices = [
        Index(value = ["dedupKey"], unique = true),
        Index(value = ["platform", "observedAtMillis"]),
        Index(value = ["sessionId"])
    ]
)
data class OfferRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val platform: String,
    val source: String,
    val confidence: String,
    val observedAtMillis: Long,
    val lastSeenAtMillis: Long,
    val fingerprint: String,
    val dedupKey: String,
    val fareCents: Long,
    val pickupDistanceMeters: Long,
    val pickupTimeSeconds: Int,
    val tripDistanceMeters: Long,
    val tripTimeSeconds: Int,
    val bonusCents: Long?,
    val category: String?,
    val passengerRatingMilli: Int?,
    val passengerRatingCount: Int?,
    val specialOfferType: String?,
    val realizationState: String,
    val classification: String,
    val classificationReason: String,
    val goalPerKmMicros: Long,
    val goalPerHourCents: Long,
    val goalMode: String
)

@Entity(
    tableName = "trip_records",
    indices = [Index(value = ["platform", "startedAtMillis"]), Index(value = ["offerId"])]
)
data class TripRecordEntity(
    @PrimaryKey val id: String,
    val offerId: String?,
    val sessionId: String?,
    val platform: String,
    val status: String,
    val acceptedAtMillis: Long?,
    val startedAtMillis: Long?,
    val completedAtMillis: Long?,
    val durationSeconds: Int?,
    val distanceMeters: Long?,
    val linkConfidence: String,
    val source: String
)

@Entity(
    tableName = "earning_records",
    indices = [
        Index(value = ["platform", "occurredAtMillis"]),
        Index(value = ["tripId"]),
        Index(value = ["legacyId"], unique = true)
    ]
)
data class EarningRecordEntity(
    @PrimaryKey val id: String,
    val tripId: String?,
    val sessionId: String?,
    val platform: String?,
    val amountCents: Long,
    val kind: String,
    val occurredAtMillis: Long,
    val source: String,
    val confidence: String,
    val note: String?,
    val legacyId: String?
)

@Entity(tableName = "work_sessions", indices = [Index(value = ["startedAtMillis"])])
data class WorkSessionEntity(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val lastActivityAtMillis: Long,
    val endedAtMillis: Long?,
    val autoDetected: Boolean,
    val durationConfidence: String,
    val dailyGrossGoalCentsSnapshot: Long,
    val goalPerKmMicrosSnapshot: Long,
    val goalPerHourCentsSnapshot: Long
)

@Entity(
    tableName = "expense_records",
    indices = [Index(value = ["occurredAtMillis"]), Index(value = ["legacyId"], unique = true)]
)
data class ExpenseRecordEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val amountCents: Long,
    val category: String,
    val occurredAtMillis: Long,
    val description: String?,
    val affectsOperatingProfit: Boolean,
    val source: String,
    val confidence: String,
    val legacyId: String?
)

@Entity(
    tableName = "data_corrections",
    indices = [Index(value = ["recordType", "recordId", "correctedAtMillis"])]
)
data class DataCorrectionEntity(
    @PrimaryKey val id: String,
    val recordType: String,
    val recordId: String,
    val fieldName: String,
    val originalValue: String?,
    val correctedValue: String,
    val correctedAtMillis: Long,
    val source: String = LedgerSource.DRIVER.name
)

@Entity(tableName = "platform_snapshots", indices = [Index(value = ["platform", "periodStartMillis"])])
data class PlatformSnapshotEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val kind: String,
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val grossCents: Long?,
    val tripCount: Int?,
    val onlineSeconds: Long?,
    val source: String,
    val confidence: String
)

@Entity(tableName = "daily_goal_revisions", indices = [Index(value = ["effectiveAtMillis"])])
data class DailyGoalRevisionEntity(
    @PrimaryKey val id: String,
    val effectiveAtMillis: Long,
    val dailyGrossGoalCents: Long,
    val source: String = LedgerSource.DRIVER.name
)

@Entity(
    tableName = "record_field_states",
    primaryKeys = ["recordType", "recordId", "fieldName"],
    indices = [Index(value = ["recordId"])]
)
data class RecordFieldStateEntity(
    val recordType: String,
    val recordId: String,
    val fieldName: String,
    val source: String,
    val confidence: String,
    val observedAtMillis: Long
)

@Entity(tableName = "daily_aggregates", primaryKeys = ["dayKey", "zoneId"])
data class DailyAggregateEntity(
    val dayKey: String,
    val zoneId: String,
    val rebuiltAtMillis: Long,
    val confirmedOffers: Int,
    val confirmedEarningsCents: Long,
    val expensesCents: Long,
    val goodOffers: Int,
    val warningOffers: Int,
    val badOffers: Int
)

data class OfferSummaryRow(
    val offerCount: Int,
    val greatCount: Int,
    val goodCount: Int,
    val warningCount: Int,
    val badCount: Int,
    val averageValuePerKm: Double?,
    val averageValuePerHour: Double?
)

data class PlatformOfferSummaryRow(
    val platform: String,
    val offerCount: Int,
    val goodCount: Int,
    val warningCount: Int,
    val badCount: Int,
    val averageValuePerKm: Double?,
    val averageValuePerHour: Double?
)

data class MoneySummaryRow(val totalCents: Long, val itemCount: Int)

data class ExpenseSummaryRow(
    val totalCents: Long,
    val operatingTotalCents: Long,
    val itemCount: Int
)
