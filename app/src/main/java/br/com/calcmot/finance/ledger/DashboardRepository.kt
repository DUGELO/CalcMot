package br.com.calcmot.finance.ledger

import android.content.Context
import br.com.calcmot.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class DashboardOverview(
    val offerSummary: OfferSummaryRow,
    val platformSummaries: List<PlatformOfferSummaryRow>,
    val recentOffers: List<OfferRecordEntity>,
    val analysis: FinancialAnalysis,
    val earnings: MoneySummaryRow,
    val expenses: ExpenseSummaryRow,
    val sessions: List<WorkSessionEntity>,
    val dailyGrossGoalCents: Long
) {
    val hasAnyData: Boolean
        get() = offerSummary.offerCount > 0 || earnings.itemCount > 0 || expenses.itemCount > 0
    val hasUberData: Boolean get() = platformSummaries.any { it.platform == "uber" && it.offerCount > 0 }
    val hasNinetyNineData: Boolean get() = platformSummaries.any { it.platform == "99" && it.offerCount > 0 }
    val confirmedGrossCents: Long? get() = earnings.totalCents.takeIf { earnings.itemCount > 0 }
    val netCents: Long? get() = confirmedGrossCents?.minus(expenses.operatingTotalCents)
}

interface DashboardRepository {
    fun observeOverview(period: LedgerPeriod = LedgerPeriod.day()): Flow<DashboardOverview>
}

class LocalDashboardRepository(
    private val context: Context,
    private val ledger: FinancialLedgerRepository = FinanceLedgerProvider.repository(context)
) : DashboardRepository {
    override fun observeOverview(period: LedgerPeriod): Flow<DashboardOverview> {
        val offerData = combine(
            ledger.observeOfferSummary(period),
            ledger.observePlatformOfferSummary(period),
            ledger.observeOffers(period)
        ) { summary, platforms, offers -> Triple(summary, platforms, offers) }
        val financialData = combine(
            ledger.observeEarningSummary(period),
            ledger.observeExpenseSummary(period),
            ledger.observeSessions(period)
        ) { earnings, expenses, sessions -> Triple(earnings, expenses, sessions) }

        return combine(offerData, financialData) { offers, financial ->
            DashboardOverview(
                offerSummary = offers.first,
                platformSummaries = offers.second,
                recentOffers = offers.third.take(8),
                analysis = FinancialAnalytics.analyze(offers.third),
                earnings = financial.first,
                expenses = financial.second,
                sessions = financial.third,
                dailyGrossGoalCents = AppSettings.getDailyGrossGoalCents(context)
            )
        }
    }
}

object DashboardRepositoryProvider {
    @Volatile private var repository: DashboardRepository? = null

    fun get(context: Context): DashboardRepository = repository ?: synchronized(this) {
        repository ?: LocalDashboardRepository(context.applicationContext).also { repository = it }
    }
}
