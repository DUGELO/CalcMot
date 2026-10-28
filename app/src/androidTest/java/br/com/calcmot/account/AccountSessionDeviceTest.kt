package br.com.calcmot.account

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import br.com.calcmot.debug.AccountUiTestActivity
import br.com.calcmot.entitlement.EntitlementState
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Rule
import org.junit.Test

/** Session UI boundaries; no Firebase, real grants or network fixtures. */
class AccountSessionDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<AccountUiTestActivity>()

    @Test fun changingOwnerClosesThePreviousLogoutDialog() {
        val state = mutableStateOf(AccountState(signedIn = true, displayName = "Synthetic A", sessionGeneration = 1))
        rule.setContent { MetricaTheme { AccountScreen(state.value, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithTag("account_logout").performScrollTo().performClick()
        rule.onNodeWithText("Sair desta conta?").assertIsDisplayed()
        rule.runOnIdle { state.value = AccountState(signedIn = true, displayName = "Synthetic B", sessionGeneration = 2) }
        rule.onNodeWithText("Sair desta conta?").assertDoesNotExist()
        rule.onNodeWithText("Synthetic A").assertDoesNotExist()
        rule.onNodeWithText("Synthetic B").assertIsDisplayed()
    }

    @Test fun changingOwnerResetsRecoveryNavigationAndProtocol() {
        val state = mutableStateOf(AccountState(signedIn = true, sessionGeneration = 1,
            request = LegacyRequestView("SYNTHETIC-A-PROTOCOL", "UNDER_REVIEW")))
        rule.setContent { MetricaTheme { AccountScreen(state.value, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithTag("account_details").performScrollTo().performClick()
        rule.onNodeWithTag("account_recovery").performScrollTo().performClick()
        rule.onNodeWithTag("account_protocol").assertExists()
        rule.runOnIdle { state.value = AccountState(signedIn = true, sessionGeneration = 2) }
        rule.onNodeWithTag("account_protocol").assertDoesNotExist()
        rule.onNodeWithTag("account_details").assertIsDisplayed()
        rule.onNodeWithTag("account_recovery").assertDoesNotExist()
    }

    @Test fun confirmedPermanentRightOffersDetailsInsteadOfAnotherRequest() {
        rule.setContent { MetricaTheme { AccountScreen(AccountState(signedIn = true,
            access = EntitlementState(legacyCore = true)), {}, {}, {}, {}, {}, {}, initialPage = AccountPage.ACCESS) } }
        rule.onNodeWithTag("account_recovery").assertDoesNotExist()
        rule.onNodeWithTag("account_request").assertDoesNotExist()
        rule.onNodeWithTag("account_legacy_details").performScrollTo().performClick()
        rule.onNodeWithText("O direito permanente preserva as funcionalidades que eram gratuitas até a versão 3.5. Recursos posteriores não estão incluídos automaticamente.")
            .performScrollTo().assertIsDisplayed()
    }
}
