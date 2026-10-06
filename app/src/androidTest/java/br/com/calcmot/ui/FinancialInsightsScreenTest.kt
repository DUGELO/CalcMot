package br.com.calcmot.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import br.com.calcmot.finance.ledger.FinancialAnalysis
import br.com.calcmot.finance.ledger.OfferSummaryRow
import br.com.calcmot.finance.ledger.PlatformOfferSummaryRow
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Rule
import org.junit.Test

class FinancialInsightsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displayedOffersAreNeverPresentedAsPotentialEarnings() {
        val state = OpenDashboardUiState(
            offerSummary = OfferSummaryRow(
                offerCount = 12,
                greatCount = 1,
                goodCount = 3,
                warningCount = 2,
                badCount = 6,
                averageValuePerKm = 1.42,
                averageValuePerHour = 28.70
            ),
            platformSummaries = listOf(
                PlatformOfferSummaryRow("uber", 12, 4, 2, 6, 1.42, 28.70)
            ),
            analysis = FinancialAnalysis(emptyList(), null, null, null, null),
            historyEnabled = true
        )

        composeRule.setContent {
            MetricaTheme {
                FinancialInsightsScreen(state = state, onBack = {}, onOpenDailyGoal = {})
            }
        }

        composeRule.onNodeWithText("Ofertas analisadas")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Ofertas exibidas não são ganhos. O resumo financeiro usa apenas ganhos confirmados ou informados por você."
        ).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Potencial analisado").assertCountEquals(0)
    }
}
