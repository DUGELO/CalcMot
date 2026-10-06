package br.com.calcmot.finance.ledger

import android.content.Context
import androidx.room.withTransaction
import br.com.calcmot.AppSettings
import br.com.calcmot.finance.FinanceEntryJson
import br.com.calcmot.finance.FinanceEntryType
import br.com.calcmot.model.DriverGoal
import br.com.calcmot.model.FinancialImpactCalculator
import br.com.calcmot.model.GoalMode
import br.com.calcmot.model.OfferCandidate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs

data class LedgerPeriod(val startMillis: Long, val endMillis: Long) {
    companion object {
        fun day(date: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): LedgerPeriod {
            val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            return LedgerPeriod(start, end)
        }
    }
}

data class StableOfferObservation(
    val platform: String,
    val source: LedgerSource,
    val observedAtMillis: Long,
    val fingerprint: String,
    val fareCents: Long,
    val pickupDistanceMeters: Long,
    val pickupTimeSeconds: Int,
    val tripDistanceMeters: Long,
    val tripTimeSeconds: Int,
    val passengerRatingMilli: Int?,
    val classification: String,
    val classificationReason: String,
    val goalPerKmMicros: Long,
    val goalPerHourCents: Long,
    val goalMode: String,
    val category: String? = null,
    val bonusCents: Long? = null,
    val passengerRatingCount: Int? = null,
    val specialOfferType: String? = null
)

interface FinancialLedgerRepository {
    fun observeOfferSummary(period: LedgerPeriod): Flow<OfferSummaryRow>
    fun observePlatformOfferSummary(period: LedgerPeriod): Flow<List<PlatformOfferSummaryRow>>
    fun observeRecentOffers(period: LedgerPeriod, limit: Int = 8): Flow<List<OfferRecordEntity>>
    fun observeOffers(period: LedgerPeriod): Flow<List<OfferRecordEntity>>
    fun observeEarningSummary(period: LedgerPeriod): Flow<MoneySummaryRow>
    fun observeExpenseSummary(period: LedgerPeriod): Flow<ExpenseSummaryRow>
    fun observeSessions(period: LedgerPeriod): Flow<List<WorkSessionEntity>>

    suspend fun recordStableOffer(observation: StableOfferObservation): Boolean
    suspend fun getOffer(id: String): OfferRecordEntity?
    suspend fun correctOffer(id: String, correction: OfferCorrection): Boolean
    suspend fun markOfferRealization(id: String, state: OfferRealizationState): Boolean
    suspend fun getSession(id: String): WorkSessionEntity?
    suspend fun adjustSession(id: String, startedAtMillis: Long, endedAtMillis: Long?): Boolean
    suspend fun splitSession(id: String, splitAtMillis: Long): String?
    suspend fun mergeSessions(ids: Set<String>): String?
    suspend fun addManualEarning(amountCents: Long, description: String, occurredAtMillis: Long): String
    suspend fun addExpense(
        amountCents: Long,
        category: ExpenseCategory,
        description: String,
        affectsOperatingProfit: Boolean,
        occurredAtMillis: Long
    ): String
    suspend fun setDailyGrossGoal(amountCents: Long, effectiveAtMillis: Long = System.currentTimeMillis())
    suspend fun correctField(
        recordType: LedgerRecordType,
        recordId: String,
        fieldName: String,
        originalValue: String?,
        correctedValue: String
    )
    suspend fun exportSnapshot(period: LedgerPeriod): LedgerExportSnapshot
    suspend fun runRetention(nowMillis: Long = System.currentTimeMillis())
    suspend fun deleteAllFinancialHistory()
    suspend fun migrateLegacyEntries()
}

data class OfferCorrection(
    val fareCents: Long,
    val pickupDistanceMeters: Long,
    val pickupTimeSeconds: Int,
    val tripDistanceMeters: Long,
    val tripTimeSeconds: Int
)

data class LedgerExportSnapshot(
    val period: LedgerPeriod,
    val offers: List<OfferRecordEntity>,
    val earnings: List<EarningRecordEntity>,
    val expenses: List<ExpenseRecordEntity>
)

class RoomFinancialLedgerRepository(
    private val context: Context,
    private val database: CalcMotFinanceDatabase = CalcMotFinanceDatabase.get(context)
) : FinancialLedgerRepository {
    private val dao = database.ledgerDao()

    override fun observeOfferSummary(period: LedgerPeriod) = dao.observeOfferSummary(period.startMillis, period.endMillis)
    override fun observePlatformOfferSummary(period: LedgerPeriod) = dao.observePlatformOfferSummary(period.startMillis, period.endMillis)
    override fun observeRecentOffers(period: LedgerPeriod, limit: Int) = dao.observeRecentOffers(period.startMillis, period.endMillis, limit)
    override fun observeOffers(period: LedgerPeriod) = dao.observeOffers(period.startMillis, period.endMillis)
    override fun observeEarningSummary(period: LedgerPeriod) = dao.observeEarningSummary(period.startMillis, period.endMillis)
    override fun observeExpenseSummary(period: LedgerPeriod) = dao.observeExpenseSummary(period.startMillis, period.endMillis)
    override fun observeSessions(period: LedgerPeriod) = dao.observeSessions(period.startMillis, period.endMillis)

    override suspend fun recordStableOffer(observation: StableOfferObservation): Boolean = database.withTransaction {
        val exactMatch = dao.findRecentOffer(
            platform = observation.platform,
            fingerprint = observation.fingerprint,
            sinceMillis = observation.observedAtMillis - OFFER_EPISODE_WINDOW_MILLIS,
            observedAtMillis = observation.observedAtMillis
        )
        val recent = exactMatch ?: observation
            .takeIf { it.source == LedgerSource.UBER_ACCESSIBILITY }
            ?.let {
                dao.findRecentPlatformOffers(
                    platform = observation.platform,
                    source = observation.source.name,
                    sinceMillis = observation.observedAtMillis - OFFER_CONTINUITY_WINDOW_MILLIS,
                    observedAtMillis = observation.observedAtMillis,
                    limit = RECENT_OFFER_CANDIDATE_LIMIT
                ).firstOrNull { candidate -> candidate.matchesSameUberOfferEpisode(observation) }
            }
        if (recent != null) {
            dao.updateOfferLastSeen(recent.id, maxOf(recent.lastSeenAtMillis, observation.observedAtMillis))
            ensureSession(observation.observedAtMillis)
            return@withTransaction false
        }

        val session = ensureSession(observation.observedAtMillis)
        val id = UUID.randomUUID().toString()
        val episode = observation.observedAtMillis / OFFER_EPISODE_WINDOW_MILLIS
        val inserted = dao.insertOffer(
            OfferRecordEntity(
                id = id,
                sessionId = session.id,
                platform = observation.platform,
                source = observation.source.name,
                confidence = LedgerConfidence.CONFIRMED.name,
                observedAtMillis = observation.observedAtMillis,
                lastSeenAtMillis = observation.observedAtMillis,
                fingerprint = observation.fingerprint,
                dedupKey = "${observation.platform}|${observation.fingerprint}|$episode",
                fareCents = observation.fareCents,
                pickupDistanceMeters = observation.pickupDistanceMeters,
                pickupTimeSeconds = observation.pickupTimeSeconds,
                tripDistanceMeters = observation.tripDistanceMeters,
                tripTimeSeconds = observation.tripTimeSeconds,
                bonusCents = observation.bonusCents,
                category = observation.category,
                passengerRatingMilli = observation.passengerRatingMilli,
                passengerRatingCount = observation.passengerRatingCount,
                specialOfferType = observation.specialOfferType,
                realizationState = OfferRealizationState.UNKNOWN.name,
                classification = observation.classification,
                classificationReason = observation.classificationReason,
                goalPerKmMicros = observation.goalPerKmMicros,
                goalPerHourCents = observation.goalPerHourCents,
                goalMode = observation.goalMode
            )
        )
        if (inserted == -1L) return@withTransaction false

        dao.insertFieldStates(
            listOf("fare", "pickup_distance", "pickup_time", "trip_distance", "trip_time", "passenger_rating")
                .filterNot { it == "passenger_rating" && observation.passengerRatingMilli == null }
                .map { field ->
                    RecordFieldStateEntity(
                        recordType = LedgerRecordType.OFFER.name,
                        recordId = id,
                        fieldName = field,
                        source = observation.source.name,
                        confidence = LedgerConfidence.CONFIRMED.name,
                        observedAtMillis = observation.observedAtMillis
                    )
                }
        )
        true
    }

    override suspend fun getOffer(id: String): OfferRecordEntity? = dao.offerById(id)

    override suspend fun getSession(id: String): WorkSessionEntity? = dao.sessionById(id)

    override suspend fun adjustSession(
        id: String,
        startedAtMillis: Long,
        endedAtMillis: Long?
    ): Boolean = database.withTransaction {
        val current = dao.sessionById(id) ?: return@withTransaction false
        val effectiveEnd = endedAtMillis ?: current.endedAtMillis
        if (startedAtMillis <= 0L || (effectiveEnd != null && effectiveEnd <= startedAtMillis)) {
            return@withTransaction false
        }
        val updated = current.copy(
            startedAtMillis = startedAtMillis,
            lastActivityAtMillis = effectiveEnd ?: maxOf(current.lastActivityAtMillis, startedAtMillis),
            endedAtMillis = effectiveEnd,
            autoDetected = false,
            durationConfidence = LedgerConfidence.DRIVER_CORRECTED.name
        )
        dao.updateSession(updated)
        recordSessionCorrection(current, updated)
        true
    }

    override suspend fun splitSession(id: String, splitAtMillis: Long): String? = database.withTransaction {
        val current = dao.sessionById(id) ?: return@withTransaction null
        val currentEnd = current.endedAtMillis ?: current.lastActivityAtMillis
        if (splitAtMillis <= current.startedAtMillis || splitAtMillis >= currentEnd) {
            return@withTransaction null
        }
        val newId = UUID.randomUUID().toString()
        val second = current.copy(
            id = newId,
            startedAtMillis = splitAtMillis,
            autoDetected = false,
            durationConfidence = LedgerConfidence.DRIVER_CORRECTED.name
        )
        dao.updateSession(
            current.copy(
                lastActivityAtMillis = splitAtMillis,
                endedAtMillis = splitAtMillis,
                autoDetected = false,
                durationConfidence = LedgerConfidence.DRIVER_CORRECTED.name
            )
        )
        dao.insertSession(second)
        dao.splitOffersToSession(id, newId, splitAtMillis)
        dao.splitTripsToSession(id, newId, splitAtMillis)
        dao.splitEarningsToSession(id, newId, splitAtMillis)
        dao.splitExpensesToSession(id, newId, splitAtMillis)
        correctField(LedgerRecordType.SESSION, id, "split_at", null, splitAtMillis.toString())
        newId
    }

    override suspend fun mergeSessions(ids: Set<String>): String? = database.withTransaction {
        if (ids.size < 2) return@withTransaction null
        val sessions = dao.sessionsByIds(ids.toList())
        if (sessions.size != ids.size) return@withTransaction null
        val keeper = sessions.first()
        val otherIds = sessions.drop(1).map { it.id }
        val mergedEnd = if (sessions.any { it.endedAtMillis == null }) null
            else sessions.maxOf { requireNotNull(it.endedAtMillis) }
        val merged = keeper.copy(
            startedAtMillis = sessions.minOf { it.startedAtMillis },
            lastActivityAtMillis = sessions.maxOf { it.lastActivityAtMillis },
            endedAtMillis = mergedEnd,
            autoDetected = false,
            durationConfidence = LedgerConfidence.DRIVER_CORRECTED.name
        )
        otherIds.forEach { sourceId ->
            dao.moveOffersToSession(sourceId, keeper.id)
            dao.moveTripsToSession(sourceId, keeper.id)
            dao.moveEarningsToSession(sourceId, keeper.id)
            dao.moveExpensesToSession(sourceId, keeper.id)
        }
        dao.updateSession(merged)
        dao.deleteSessions(otherIds)
        correctField(
            LedgerRecordType.SESSION,
            keeper.id,
            "merged_sessions",
            null,
            sessions.joinToString(",") { it.id }
        )
        keeper.id
    }

    override suspend fun markOfferRealization(id: String, state: OfferRealizationState): Boolean = database.withTransaction {
        val current = dao.offerById(id) ?: return@withTransaction false
        dao.updateOffer(current.copy(realizationState = state.name))
        true
    }

    override suspend fun correctOffer(id: String, correction: OfferCorrection): Boolean = database.withTransaction {
        val current = dao.offerById(id) ?: return@withTransaction false
        if (correction.fareCents <= 0L || correction.pickupDistanceMeters < 0L ||
            correction.tripDistanceMeters <= 0L || correction.pickupTimeSeconds < 0 ||
            correction.tripTimeSeconds <= 0
        ) return@withTransaction false

        val candidate = OfferCandidate(
            price = correction.fareCents / 100.0,
            pickupDistanceKm = correction.pickupDistanceMeters / 1000.0,
            pickupTimeMin = correction.pickupTimeSeconds / 60,
            tripDistanceKm = correction.tripDistanceMeters / 1000.0,
            tripTimeMin = correction.tripTimeSeconds / 60,
            passengerRating = current.passengerRatingMilli?.div(1000.0)
        )
        val tripData = candidate.toTripData() ?: return@withTransaction false
        val goal = DriverGoal(
            minValuePerKm = current.goalPerKmMicros / 1_000_000.0,
            minValuePerHour = current.goalPerHourCents / 100.0,
            mode = runCatching { GoalMode.valueOf(current.goalMode) }.getOrDefault(GoalMode.BALANCED)
        )
        val impact = FinancialImpactCalculator.calculate(tripData, goal) ?: return@withTransaction false
        val correctedAt = System.currentTimeMillis()
        val changes = listOf(
            "fare" to (current.fareCents.toString() to correction.fareCents.toString()),
            "pickup_distance" to (current.pickupDistanceMeters.toString() to correction.pickupDistanceMeters.toString()),
            "pickup_time" to (current.pickupTimeSeconds.toString() to correction.pickupTimeSeconds.toString()),
            "trip_distance" to (current.tripDistanceMeters.toString() to correction.tripDistanceMeters.toString()),
            "trip_time" to (current.tripTimeSeconds.toString() to correction.tripTimeSeconds.toString())
        ).filter { (_, values) -> values.first != values.second }
        if (changes.isEmpty()) return@withTransaction true

        dao.updateOffer(
            current.copy(
                fareCents = correction.fareCents,
                pickupDistanceMeters = correction.pickupDistanceMeters,
                pickupTimeSeconds = correction.pickupTimeSeconds,
                tripDistanceMeters = correction.tripDistanceMeters,
                tripTimeSeconds = correction.tripTimeSeconds,
                classification = impact.classification.name,
                classificationReason = impact.subtext,
                confidence = LedgerConfidence.DRIVER_CORRECTED.name
            )
        )
        changes.forEach { (field, values) ->
            dao.insertCorrection(
                DataCorrectionEntity(
                    id = UUID.randomUUID().toString(),
                    recordType = LedgerRecordType.OFFER.name,
                    recordId = id,
                    fieldName = field,
                    originalValue = values.first,
                    correctedValue = values.second,
                    correctedAtMillis = correctedAt
                )
            )
        }
        dao.insertFieldStates(changes.map { (field, _) ->
            RecordFieldStateEntity(
                recordType = LedgerRecordType.OFFER.name,
                recordId = id,
                fieldName = field,
                source = LedgerSource.DRIVER.name,
                confidence = LedgerConfidence.DRIVER_CORRECTED.name,
                observedAtMillis = correctedAt
            )
        })
        true
    }

    override suspend fun addManualEarning(amountCents: Long, description: String, occurredAtMillis: Long): String {
        require(amountCents > 0L)
        val session = database.withTransaction { ensureSession(occurredAtMillis) }
        val id = UUID.randomUUID().toString()
        dao.insertEarning(
            EarningRecordEntity(
                id = id,
                tripId = null,
                sessionId = session.id,
                platform = null,
                amountCents = amountCents,
                kind = EarningKind.MANUAL.name,
                occurredAtMillis = occurredAtMillis,
                source = LedgerSource.DRIVER.name,
                confidence = LedgerConfidence.DRIVER_PROVIDED.name,
                note = description.trim().takeIf(String::isNotBlank),
                legacyId = null
            )
        )
        return id
    }

    override suspend fun addExpense(
        amountCents: Long,
        category: ExpenseCategory,
        description: String,
        affectsOperatingProfit: Boolean,
        occurredAtMillis: Long
    ): String {
        require(amountCents > 0L)
        val id = UUID.randomUUID().toString()
        dao.insertExpense(
            ExpenseRecordEntity(
                id = id,
                sessionId = dao.activeSession()?.id,
                amountCents = amountCents,
                category = category.name,
                occurredAtMillis = occurredAtMillis,
                description = description.trim().takeIf(String::isNotBlank),
                affectsOperatingProfit = affectsOperatingProfit,
                source = LedgerSource.DRIVER.name,
                confidence = LedgerConfidence.DRIVER_PROVIDED.name,
                legacyId = null
            )
        )
        return id
    }

    override suspend fun setDailyGrossGoal(amountCents: Long, effectiveAtMillis: Long) {
        val normalized = amountCents.coerceAtLeast(0L)
        AppSettings.setDailyGrossGoalCents(context, normalized)
        dao.insertDailyGoalRevision(
            DailyGoalRevisionEntity(UUID.randomUUID().toString(), effectiveAtMillis, normalized)
        )
    }

    override suspend fun correctField(
        recordType: LedgerRecordType,
        recordId: String,
        fieldName: String,
        originalValue: String?,
        correctedValue: String
    ) {
        require(recordId.isNotBlank() && fieldName.isNotBlank() && correctedValue.isNotBlank())
        dao.insertCorrection(
            DataCorrectionEntity(
                id = UUID.randomUUID().toString(),
                recordType = recordType.name,
                recordId = recordId,
                fieldName = fieldName,
                originalValue = originalValue,
                correctedValue = correctedValue,
                correctedAtMillis = System.currentTimeMillis()
            )
        )
        dao.insertFieldStates(
            listOf(
                RecordFieldStateEntity(
                    recordType = recordType.name,
                    recordId = recordId,
                    fieldName = fieldName,
                    source = LedgerSource.DRIVER.name,
                    confidence = LedgerConfidence.DRIVER_CORRECTED.name,
                    observedAtMillis = System.currentTimeMillis()
                )
            )
        )
    }

    override suspend fun exportSnapshot(period: LedgerPeriod) = LedgerExportSnapshot(
        period = period,
        offers = dao.offersForPeriod(period.startMillis, period.endMillis),
        earnings = dao.earningsForPeriod(period.startMillis, period.endMillis),
        expenses = dao.expensesForPeriod(period.startMillis, period.endMillis)
    )

    override suspend fun runRetention(nowMillis: Long) {
        val zone = ZoneId.systemDefault()
        val cutoff = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .minusMonths(6)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        database.deleteOlderThan(cutoff)
        dao.activeSession()?.takeIf { nowMillis - it.lastActivityAtMillis >= SESSION_IDLE_TIMEOUT_MILLIS }
            ?.let { dao.closeSession(it.id, it.lastActivityAtMillis) }
    }

    override suspend fun deleteAllFinancialHistory() {
        database.deleteAllFinancialHistory()
    }

    override suspend fun migrateLegacyEntries() {
        val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_LEGACY_MIGRATION_COMPLETE, false)) return
        val entries = FinanceEntryJson.decode(prefs.getString(KEY_LEGACY_ENTRIES, null))
        database.withTransaction {
            entries.forEach { entry ->
                when (entry.type) {
                    FinanceEntryType.EARNING -> dao.insertEarning(
                        EarningRecordEntity(
                            id = UUID.randomUUID().toString(),
                            tripId = null,
                            sessionId = null,
                            platform = null,
                            amountCents = entry.amountCents,
                            kind = EarningKind.MANUAL.name,
                            occurredAtMillis = entry.dateMillis,
                            source = LedgerSource.LEGACY_MANUAL.name,
                            confidence = LedgerConfidence.DRIVER_PROVIDED.name,
                            note = entry.description.takeIf(String::isNotBlank),
                            legacyId = entry.id
                        )
                    )
                    FinanceEntryType.COST -> dao.insertExpense(
                        ExpenseRecordEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = null,
                            amountCents = entry.amountCents,
                            category = ExpenseCategory.OTHER.name,
                            occurredAtMillis = entry.dateMillis,
                            description = entry.description.takeIf(String::isNotBlank),
                            affectsOperatingProfit = true,
                            source = LedgerSource.LEGACY_MANUAL.name,
                            confidence = LedgerConfidence.DRIVER_PROVIDED.name,
                            legacyId = entry.id
                        )
                    )
                }
            }
        }
        prefs.edit().remove(KEY_LEGACY_ENTRIES).putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true).commit()
    }

    private suspend fun ensureSession(atMillis: Long): WorkSessionEntity {
        val active = dao.activeSession()
        if (active != null && atMillis - active.lastActivityAtMillis < SESSION_IDLE_TIMEOUT_MILLIS) {
            dao.touchSession(active.id, maxOf(active.lastActivityAtMillis, atMillis))
            return active.copy(lastActivityAtMillis = maxOf(active.lastActivityAtMillis, atMillis))
        }
        if (active != null) dao.closeSession(active.id, active.lastActivityAtMillis)

        val goal = AppSettings.getDriverGoal(context)
        return WorkSessionEntity(
            id = UUID.randomUUID().toString(),
            startedAtMillis = atMillis,
            lastActivityAtMillis = atMillis,
            endedAtMillis = null,
            autoDetected = true,
            durationConfidence = LedgerConfidence.ESTIMATED.name,
            dailyGrossGoalCentsSnapshot = AppSettings.getDailyGrossGoalCents(context),
            goalPerKmMicrosSnapshot = (goal.minValuePerKm * 1_000_000).toLong(),
            goalPerHourCentsSnapshot = (goal.minValuePerHour * 100).toLong()
        ).also { dao.insertSession(it) }
    }

    private fun OfferRecordEntity.matchesSameUberOfferEpisode(
        observation: StableOfferObservation
    ): Boolean {
        if (fareCents != observation.fareCents) return false
        if (abs(pickupDistanceMeters - observation.pickupDistanceMeters) > OFFER_PICKUP_DISTANCE_TOLERANCE_METERS) return false
        if (abs(pickupTimeSeconds - observation.pickupTimeSeconds) > OFFER_PICKUP_TIME_TOLERANCE_SECONDS) return false
        if (abs(tripDistanceMeters - observation.tripDistanceMeters) > OFFER_TRIP_DISTANCE_TOLERANCE_METERS) return false
        if (abs(tripTimeSeconds - observation.tripTimeSeconds) > OFFER_TRIP_TIME_TOLERANCE_SECONDS) return false
        if (bonusCents != null && observation.bonusCents != null && bonusCents != observation.bonusCents) return false
        if (category != null && observation.category != null && category != observation.category) return false
        if (specialOfferType != null && observation.specialOfferType != null && specialOfferType != observation.specialOfferType) return false
        if (passengerRatingMilli != null && observation.passengerRatingMilli != null &&
            passengerRatingMilli != observation.passengerRatingMilli
        ) return false
        return true
    }

    private suspend fun recordSessionCorrection(
        original: WorkSessionEntity,
        corrected: WorkSessionEntity
    ) {
        val correctedAt = System.currentTimeMillis()
        listOf(
            "started_at" to (original.startedAtMillis to corrected.startedAtMillis),
            "ended_at" to ((original.endedAtMillis ?: -1L) to (corrected.endedAtMillis ?: -1L))
        ).filter { (_, values) -> values.first != values.second }
            .forEach { (field, values) ->
                dao.insertCorrection(
                    DataCorrectionEntity(
                        id = UUID.randomUUID().toString(),
                        recordType = LedgerRecordType.SESSION.name,
                        recordId = original.id,
                        fieldName = field,
                        originalValue = values.first.toString(),
                        correctedValue = values.second.toString(),
                        correctedAtMillis = correctedAt
                    )
                )
            }
    }

    companion object {
        const val SESSION_IDLE_TIMEOUT_MILLIS = 90L * 60L * 1000L
        const val OFFER_EPISODE_WINDOW_MILLIS = 2L * 60L * 1000L
        const val OFFER_CONTINUITY_WINDOW_MILLIS = 90L * 1000L
        private const val RECENT_OFFER_CANDIDATE_LIMIT = 8
        private const val OFFER_PICKUP_DISTANCE_TOLERANCE_METERS = 400L
        private const val OFFER_PICKUP_TIME_TOLERANCE_SECONDS = 120
        private const val OFFER_TRIP_DISTANCE_TOLERANCE_METERS = 200L
        private const val OFFER_TRIP_TIME_TOLERANCE_SECONDS = 60
        private const val LEGACY_PREFS_NAME = "calcmot_finance"
        private const val KEY_LEGACY_ENTRIES = "entries_json"
        private const val KEY_LEGACY_MIGRATION_COMPLETE = "room_migration_complete"
    }
}

object FinanceLedgerProvider {
    @Volatile private var repository: FinancialLedgerRepository? = null

    fun repository(context: Context): FinancialLedgerRepository = repository ?: synchronized(this) {
        repository ?: RoomFinancialLedgerRepository(context.applicationContext).also { repository = it }
    }
}
