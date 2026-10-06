package br.com.calcmot.finance.ledger

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialLedgerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOffer(value: OfferRecordEntity): Long

    @Query("SELECT * FROM offer_records WHERE platform = :platform AND fingerprint = :fingerprint AND lastSeenAtMillis >= :sinceMillis AND lastSeenAtMillis <= :observedAtMillis ORDER BY lastSeenAtMillis DESC LIMIT 1")
    suspend fun findRecentOffer(
        platform: String,
        fingerprint: String,
        sinceMillis: Long,
        observedAtMillis: Long
    ): OfferRecordEntity?

    @Query("SELECT * FROM offer_records WHERE platform = :platform AND source = :source AND lastSeenAtMillis >= :sinceMillis AND lastSeenAtMillis <= :observedAtMillis ORDER BY lastSeenAtMillis DESC LIMIT :limit")
    suspend fun findRecentPlatformOffers(
        platform: String,
        source: String,
        sinceMillis: Long,
        observedAtMillis: Long,
        limit: Int
    ): List<OfferRecordEntity>

    @Query("UPDATE offer_records SET lastSeenAtMillis = :seenAtMillis WHERE id = :id")
    suspend fun updateOfferLastSeen(id: String, seenAtMillis: Long)

    @Query("SELECT * FROM offer_records WHERE id = :id LIMIT 1")
    suspend fun offerById(id: String): OfferRecordEntity?

    @Update
    suspend fun updateOffer(value: OfferRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldStates(values: List<RecordFieldStateEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrip(value: TripRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEarning(value: EarningRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(value: ExpenseRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(value: DataCorrectionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSnapshot(value: PlatformSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyGoalRevision(value: DailyGoalRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyAggregate(value: DailyAggregateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(value: WorkSessionEntity)

    @Query("SELECT * FROM work_sessions WHERE endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1")
    suspend fun activeSession(): WorkSessionEntity?

    @Query("SELECT * FROM work_sessions WHERE id = :id LIMIT 1")
    suspend fun sessionById(id: String): WorkSessionEntity?

    @Query("SELECT * FROM work_sessions WHERE id IN (:ids) ORDER BY startedAtMillis")
    suspend fun sessionsByIds(ids: List<String>): List<WorkSessionEntity>

    @Update
    suspend fun updateSession(value: WorkSessionEntity)

    @Query("DELETE FROM work_sessions WHERE id IN (:ids)")
    suspend fun deleteSessions(ids: List<String>)

    @Query("UPDATE work_sessions SET lastActivityAtMillis = :atMillis WHERE id = :id")
    suspend fun touchSession(id: String, atMillis: Long)

    @Query("UPDATE work_sessions SET endedAtMillis = :endedAtMillis, lastActivityAtMillis = :endedAtMillis WHERE id = :id")
    suspend fun closeSession(id: String, endedAtMillis: Long)

    @Query("UPDATE offer_records SET sessionId = :targetId WHERE sessionId = :sourceId")
    suspend fun moveOffersToSession(sourceId: String, targetId: String)

    @Query("UPDATE trip_records SET sessionId = :targetId WHERE sessionId = :sourceId")
    suspend fun moveTripsToSession(sourceId: String, targetId: String)

    @Query("UPDATE earning_records SET sessionId = :targetId WHERE sessionId = :sourceId")
    suspend fun moveEarningsToSession(sourceId: String, targetId: String)

    @Query("UPDATE expense_records SET sessionId = :targetId WHERE sessionId = :sourceId")
    suspend fun moveExpensesToSession(sourceId: String, targetId: String)

    @Query("UPDATE offer_records SET sessionId = :targetId WHERE sessionId = :sourceId AND observedAtMillis >= :splitAtMillis")
    suspend fun splitOffersToSession(sourceId: String, targetId: String, splitAtMillis: Long)

    @Query("UPDATE trip_records SET sessionId = :targetId WHERE sessionId = :sourceId AND COALESCE(completedAtMillis, COALESCE(startedAtMillis, acceptedAtMillis)) >= :splitAtMillis")
    suspend fun splitTripsToSession(sourceId: String, targetId: String, splitAtMillis: Long)

    @Query("UPDATE earning_records SET sessionId = :targetId WHERE sessionId = :sourceId AND occurredAtMillis >= :splitAtMillis")
    suspend fun splitEarningsToSession(sourceId: String, targetId: String, splitAtMillis: Long)

    @Query("UPDATE expense_records SET sessionId = :targetId WHERE sessionId = :sourceId AND occurredAtMillis >= :splitAtMillis")
    suspend fun splitExpensesToSession(sourceId: String, targetId: String, splitAtMillis: Long)

    @Query("SELECT * FROM work_sessions WHERE startedAtMillis < :endMillis AND COALESCE(endedAtMillis, lastActivityAtMillis) >= :startMillis ORDER BY startedAtMillis DESC")
    fun observeSessions(startMillis: Long, endMillis: Long): Flow<List<WorkSessionEntity>>

    @Query("SELECT * FROM offer_records WHERE observedAtMillis >= :startMillis AND observedAtMillis < :endMillis ORDER BY observedAtMillis DESC LIMIT :limit")
    fun observeRecentOffers(startMillis: Long, endMillis: Long, limit: Int): Flow<List<OfferRecordEntity>>

    @Query("SELECT * FROM offer_records WHERE observedAtMillis >= :startMillis AND observedAtMillis < :endMillis ORDER BY observedAtMillis DESC")
    fun observeOffers(startMillis: Long, endMillis: Long): Flow<List<OfferRecordEntity>>

    @Query("""
        SELECT COUNT(*) AS offerCount,
               COALESCE(SUM(CASE WHEN classification = 'GREAT' THEN 1 ELSE 0 END), 0) AS greatCount,
               COALESCE(SUM(CASE WHEN classification = 'GOOD' THEN 1 ELSE 0 END), 0) AS goodCount,
               COALESCE(SUM(CASE WHEN classification = 'WARNING' THEN 1 ELSE 0 END), 0) AS warningCount,
               COALESCE(SUM(CASE WHEN classification = 'BAD' THEN 1 ELSE 0 END), 0) AS badCount,
               AVG(CAST(fareCents AS REAL) / 100.0 / ((pickupDistanceMeters + tripDistanceMeters) / 1000.0)) AS averageValuePerKm,
               AVG(CAST(fareCents AS REAL) / 100.0 / ((pickupTimeSeconds + tripTimeSeconds) / 3600.0)) AS averageValuePerHour
        FROM offer_records
        WHERE observedAtMillis >= :startMillis AND observedAtMillis < :endMillis
          AND confidence IN ('CONFIRMED', 'DRIVER_CORRECTED')
    """)
    fun observeOfferSummary(startMillis: Long, endMillis: Long): Flow<OfferSummaryRow>

    @Query("""
        SELECT platform AS platform,
               COUNT(*) AS offerCount,
               SUM(CASE WHEN classification IN ('GREAT', 'GOOD') THEN 1 ELSE 0 END) AS goodCount,
               SUM(CASE WHEN classification = 'WARNING' THEN 1 ELSE 0 END) AS warningCount,
               SUM(CASE WHEN classification = 'BAD' THEN 1 ELSE 0 END) AS badCount,
               AVG(CAST(fareCents AS REAL) / 100.0 / ((pickupDistanceMeters + tripDistanceMeters) / 1000.0)) AS averageValuePerKm,
               AVG(CAST(fareCents AS REAL) / 100.0 / ((pickupTimeSeconds + tripTimeSeconds) / 3600.0)) AS averageValuePerHour
        FROM offer_records
        WHERE observedAtMillis >= :startMillis AND observedAtMillis < :endMillis
          AND confidence IN ('CONFIRMED', 'DRIVER_CORRECTED')
        GROUP BY platform
    """)
    fun observePlatformOfferSummary(startMillis: Long, endMillis: Long): Flow<List<PlatformOfferSummaryRow>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) AS totalCents, COUNT(*) AS itemCount FROM earning_records WHERE occurredAtMillis >= :startMillis AND occurredAtMillis < :endMillis AND confidence IN ('CONFIRMED', 'DRIVER_PROVIDED', 'DRIVER_CORRECTED')")
    fun observeEarningSummary(startMillis: Long, endMillis: Long): Flow<MoneySummaryRow>

    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) AS totalCents,
               COALESCE(SUM(CASE WHEN affectsOperatingProfit = 1 THEN amountCents ELSE 0 END), 0) AS operatingTotalCents,
               COUNT(*) AS itemCount
        FROM expense_records
        WHERE occurredAtMillis >= :startMillis AND occurredAtMillis < :endMillis
          AND confidence IN ('CONFIRMED', 'DRIVER_PROVIDED', 'DRIVER_CORRECTED')
    """)
    fun observeExpenseSummary(startMillis: Long, endMillis: Long): Flow<ExpenseSummaryRow>

    @Query("SELECT * FROM earning_records WHERE occurredAtMillis >= :startMillis AND occurredAtMillis < :endMillis ORDER BY occurredAtMillis DESC")
    suspend fun earningsForPeriod(startMillis: Long, endMillis: Long): List<EarningRecordEntity>

    @Query("SELECT * FROM expense_records WHERE occurredAtMillis >= :startMillis AND occurredAtMillis < :endMillis ORDER BY occurredAtMillis DESC")
    suspend fun expensesForPeriod(startMillis: Long, endMillis: Long): List<ExpenseRecordEntity>

    @Query("SELECT * FROM offer_records WHERE observedAtMillis >= :startMillis AND observedAtMillis < :endMillis ORDER BY observedAtMillis DESC")
    suspend fun offersForPeriod(startMillis: Long, endMillis: Long): List<OfferRecordEntity>

    @Query("DELETE FROM record_field_states WHERE recordId IN (SELECT id FROM offer_records WHERE observedAtMillis < :cutoffMillis)")
    suspend fun deleteOldOfferFieldStates(cutoffMillis: Long)
    @Query("DELETE FROM data_corrections WHERE correctedAtMillis < :cutoffMillis") suspend fun deleteOldCorrections(cutoffMillis: Long)
    @Query("DELETE FROM offer_records WHERE observedAtMillis < :cutoffMillis") suspend fun deleteOldOffers(cutoffMillis: Long)
    @Query("DELETE FROM trip_records WHERE COALESCE(completedAtMillis, COALESCE(startedAtMillis, acceptedAtMillis)) < :cutoffMillis") suspend fun deleteOldTrips(cutoffMillis: Long)
    @Query("DELETE FROM earning_records WHERE occurredAtMillis < :cutoffMillis") suspend fun deleteOldEarnings(cutoffMillis: Long)
    @Query("DELETE FROM expense_records WHERE occurredAtMillis < :cutoffMillis") suspend fun deleteOldExpenses(cutoffMillis: Long)
    @Query("DELETE FROM platform_snapshots WHERE periodEndMillis < :cutoffMillis") suspend fun deleteOldSnapshots(cutoffMillis: Long)
    @Query("DELETE FROM work_sessions WHERE COALESCE(endedAtMillis, lastActivityAtMillis) < :cutoffMillis") suspend fun deleteOldSessions(cutoffMillis: Long)
    @Query("DELETE FROM daily_goal_revisions WHERE effectiveAtMillis < :cutoffMillis") suspend fun deleteOldGoalRevisions(cutoffMillis: Long)
    @Query("DELETE FROM daily_aggregates") suspend fun clearAggregates()

    @Query("DELETE FROM record_field_states") suspend fun clearFieldStates()
    @Query("DELETE FROM data_corrections") suspend fun clearCorrections()
    @Query("DELETE FROM offer_records") suspend fun clearOffers()
    @Query("DELETE FROM trip_records") suspend fun clearTrips()
    @Query("DELETE FROM earning_records") suspend fun clearEarnings()
    @Query("DELETE FROM expense_records") suspend fun clearExpenses()
    @Query("DELETE FROM platform_snapshots") suspend fun clearSnapshots()
    @Query("DELETE FROM work_sessions") suspend fun clearSessions()
    @Query("DELETE FROM daily_goal_revisions") suspend fun clearGoalRevisions()
}

@Database(
    entities = [
        OfferRecordEntity::class,
        TripRecordEntity::class,
        EarningRecordEntity::class,
        WorkSessionEntity::class,
        ExpenseRecordEntity::class,
        DataCorrectionEntity::class,
        PlatformSnapshotEntity::class,
        DailyGoalRevisionEntity::class,
        RecordFieldStateEntity::class,
        DailyAggregateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CalcMotFinanceDatabase : RoomDatabase() {
    abstract fun ledgerDao(): FinancialLedgerDao

    suspend fun deleteOlderThan(cutoffMillis: Long) = withTransaction {
        val dao = ledgerDao()
        dao.deleteOldOfferFieldStates(cutoffMillis)
        dao.deleteOldCorrections(cutoffMillis)
        dao.deleteOldOffers(cutoffMillis)
        dao.deleteOldTrips(cutoffMillis)
        dao.deleteOldEarnings(cutoffMillis)
        dao.deleteOldExpenses(cutoffMillis)
        dao.deleteOldSnapshots(cutoffMillis)
        dao.deleteOldSessions(cutoffMillis)
        dao.deleteOldGoalRevisions(cutoffMillis)
        dao.clearAggregates()
    }

    suspend fun deleteAllFinancialHistory() = withTransaction {
        val dao = ledgerDao()
        dao.clearFieldStates()
        dao.clearCorrections()
        dao.clearOffers()
        dao.clearTrips()
        dao.clearEarnings()
        dao.clearExpenses()
        dao.clearSnapshots()
        dao.clearSessions()
        dao.clearGoalRevisions()
        dao.clearAggregates()
    }

    companion object {
        const val DATABASE_NAME = "calcmot_finance.db"

        @Volatile private var instance: CalcMotFinanceDatabase? = null

        fun get(context: Context): CalcMotFinanceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CalcMotFinanceDatabase::class.java,
                DATABASE_NAME
            ).build().also { instance = it }
        }
    }
}
