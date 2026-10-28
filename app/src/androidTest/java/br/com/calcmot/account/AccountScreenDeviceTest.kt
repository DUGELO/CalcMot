package br.com.calcmot.account

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import br.com.calcmot.debug.AccountUiTestActivity
import br.com.calcmot.entitlement.*
import br.com.calcmot.ui.design.theme.CalcMotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic render states only, DEV package. No fabricated rights enter Firebase or D1. */
class AccountScreenDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<AccountUiTestActivity>()
    private fun render(state: AccountState, small: Boolean = false, onBack: () -> Unit = {},
        onLogin: () -> Unit = {}, onLogout: () -> Unit = {}, onRequest: () -> Unit = {},
        onRefresh: () -> Unit = {}, onSupport: () -> Unit = {},
        initialPage: AccountPage = AccountPage.ACCOUNT) {
        check(rule.activity.packageName == "br.com.calcmot.identitydev")
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (small) 1.5f else 1f)) {
                br.com.calcmot.ui.theme.MetricaTheme {
                    Box(if (small) Modifier.requiredSize(320.dp, 640.dp) else Modifier) {
                        AccountScreen(state, onBack, onLogin, onRefresh, onRequest, onLogout, onSupport, initialPage,
                            testLabel = "TESTE — DADOS SINTÉTICOS")
                    }
                }
            }
        }
    }
    private fun screenshot(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val args = InstrumentationRegistry.getArguments()
        val dir = args.getString("additionalTestOutputDir")?.let(::File)
            ?: rule.activity.getExternalFilesDir("s09-screens")!!
        dir.mkdirs()
        val window = rule.activity.window
        val image = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
        val complete = CountDownLatch(1)
        var result = PixelCopy.ERROR_UNKNOWN
        PixelCopy.request(window, image, { result = it; complete.countDown() }, Handler(Looper.getMainLooper()))
        check(complete.await(3, TimeUnit.SECONDS) && result == PixelCopy.SUCCESS) { "Window capture unavailable" }
        File(dir, "$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
    @Test fun optionalLoginAndContinueNeverRequireAccount() {
        var continued = 0; var login = 0
        render(AccountState(), onBack = { continued++ }, onLogin = { login++ })
        rule.onNodeWithText("Minha conta").assertIsDisplayed()
        screenshot("s09-signed-out")
        rule.onNodeWithTag("account_continue").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, continued); assertEquals(0, login) }
        rule.onNodeWithTag("account_login").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, login) }
    }
    @Test fun backend503NeverShowsLegacyGrantAndAllowsReturn() {
        render(AccountState(signedIn = true, linked = true,
            access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)), initialPage = AccountPage.ACCESS)
        rule.onNodeWithText("Acesso gratuito permanente").assertDoesNotExist()
        rule.onNodeWithText("Não conseguimos consultar agora").assertIsDisplayed()
        rule.onNodeWithText("Não encontramos um direito legado confirmado nesta conta.").assertDoesNotExist()
        screenshot("s09-backend-unavailable")
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithTag("account_continue").performScrollTo().assertIsEnabled()
    }
    @Test fun confirmedLegacyUsesLimitedCopyAndReviewProtocolDoesNotGrantPro() {
        render(AccountState(signedIn = true, linked = true, access = EntitlementState(legacyCore = true),
            request = LegacyRequestView("S09-SYNTHETIC-PROTOCOL", "UNDER_REVIEW")), initialPage = AccountPage.ACCESS)
        rule.onNodeWithText("Acesso gratuito permanente").assertExists()
        rule.onNodeWithText("Este direito não depende de assinatura.").assertIsDisplayed()
        rule.onNodeWithText("Direitos adicionais confirmados para o período vigente.").assertDoesNotExist()
        screenshot("s09-legacy-synthetic")
    }
    @Test fun logoutNeedsConfirmationAndClaimRequiresExplicitRecoveryAction() {
        var logout = 0; var claim = 0
        render(AccountState(signedIn = true, linked = true, recoveryAvailable = true), onLogout = { logout++ }, onRequest = { claim++ })
        rule.onNodeWithTag("account_logout").performScrollTo().performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.runOnIdle { assertEquals(0, logout) }
        rule.onNodeWithTag("account_logout").performScrollTo().performClick()
        rule.onNodeWithText("Sair", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("account_details").performScrollTo().performClick()
        rule.onNodeWithTag("account_recovery").performScrollTo().performClick()
        rule.onNodeWithText("Solicitar análise").performClick()
        rule.runOnIdle { assertEquals(1, logout); assertEquals(1, claim) }
    }
    @Test fun smallScreenLargeFontKeepsActionsReachableAndAccessible() {
        render(AccountState(signedIn = true, linked = true, notice = AccountNotice.COLLISION), small = true)
        rule.onNodeWithTag("account_details").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").assertExists()
        screenshot("s09-large-font-small-screen")
        rule.onNodeWithTag("account_continue").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("account_logout").performScrollTo().assertIsEnabled()
    }
    @Test fun pendingReviewShowsCopyableProtocolWithoutInventingGrant() {
        render(AccountState(signedIn = true, linked = true,
            request = LegacyRequestView("S09-SYNTHETIC-PROTOCOL", "UNDER_REVIEW")), initialPage = AccountPage.RECOVERY)
        rule.onNodeWithText("Solicitação em análise").assertIsDisplayed()
        rule.onNodeWithTag("account_protocol").assertTextEquals("Protocolo: S09-SYNTHETIC-PROTOCOL")
        screenshot("s09-review-synthetic")
        rule.onNodeWithTag("account_copy_protocol").performScrollTo().performClick()
        rule.onNodeWithText("Protocolo copiado").assertExists()
        rule.onNodeWithText("Direito concedido").assertDoesNotExist()
    }
    @Test fun approvedClaimAwaitsConfirmedGrant() {
        render(AccountState(signedIn = true, linked = true,
            request = LegacyRequestView("S09-SYNTHETIC-PROTOCOL", "GRANTED")), initialPage = AccountPage.RECOVERY)
        rule.onNodeWithText("Aguardando confirmação do acesso").assertIsDisplayed()
        rule.onNodeWithText("Direito concedido").assertDoesNotExist()
        screenshot("s09-awaiting-grant-synthetic")
    }
    @Test fun accountDetailsRecoveryAndBackUseOneExistingRoute() {
        var returned = 0
        render(AccountState(signedIn = true, linked = true, displayName = "Motorista de teste", email = "motorista@example.test"), onBack = { returned++ })
        screenshot("s09-connected-synthetic")
        rule.onNodeWithTag("account_details").performScrollTo().performClick()
        rule.onNodeWithText("Acesso ainda não verificado").assertIsDisplayed()
        rule.onNodeWithTag("account_recovery").performScrollTo().performClick()
        rule.onNodeWithTag("account_intake_unavailable").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Falar com suporte").assertExists()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithTag("account_continue").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, returned) }
    }
    @Test fun settingsNavigationAndAccountReturnPreserveMonitoringPreference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val before = br.com.calcmot.AppSettings.isMonitoringEnabled(context)
        br.com.calcmot.AppSettings.setMonitoringEnabled(context, true) // DEV preference only; no service is enabled.
        val navigation = androidx.navigation.testing.TestNavHostController(context).apply {
            navigatorProvider.addNavigator(androidx.navigation.compose.ComposeNavigator())
        }
        try {
            rule.setContent { CalcMotTheme {
                br.com.calcmot.ui.CalcMotNavHost(br.com.calcmot.AppPermissionState(hasAccessibilityService = true),
                    true, {}, {}, navController = navigation)
            } }
            rule.runOnIdle { navigation.navigate("settings") }
            rule.onNodeWithTag("settings_account").performScrollTo().performClick()
            rule.onNodeWithTag("account_screen").assertExists()
            rule.onNodeWithContentDescription("Voltar").performClick()
            rule.runOnIdle {
                assertEquals("settings", navigation.currentDestination?.route)
                assertTrue(br.com.calcmot.AppSettings.isMonitoringEnabled(context))
            }
        } finally { br.com.calcmot.AppSettings.setMonitoringEnabled(context, before) }
    }
    @Test fun recoveryOpenedFromDetailsReturnsToDetailsBeforeAccount() {
        render(AccountState(signedIn = true, linked = true))
        rule.onNodeWithTag("account_details").performScrollTo().performClick()
        rule.onNodeWithTag("account_recovery").performScrollTo().performClick()
        rule.onNodeWithTag("account_intake_unavailable").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithText("Acesso ainda não verificado").assertIsDisplayed()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithTag("account_details").assertExists()
    }
    @Test fun unavailableAccountSummaryDoesNotRepeatTheErrorBelowActions() {
        var refreshed = 0
        render(AccountState(signedIn = true, linked = true, displayName = "Motorista de teste",
            email = "motorista@example.test", access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)),
            onRefresh = { refreshed++ })
        rule.onAllNodesWithText("Não conseguimos consultar agora").assertCountEquals(0)
        rule.onNodeWithTag("account_message").assertDoesNotExist()
        screenshot("s09-refined-account-synthetic")
        rule.onNodeWithTag("account_details").performScrollTo().performClick()
        rule.onNodeWithTag("account_refresh").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithTag("account_continue").performScrollTo().assertIsEnabled()
        rule.runOnIdle { assertEquals(1, refreshed) }
    }
    @Test fun recoverySeparatesEligibilityProcessAndSupportWithoutDuplicatingCore() {
        var supported = 0
        render(AccountState(signedIn = true, linked = true,
            access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)),
            onSupport = { supported++ }, initialPage = AccountPage.RECOVERY)
        rule.onNodeWithText("Quem pode solicitar").assertIsDisplayed()
        rule.onNodeWithText("Como funciona").assertIsDisplayed()
        rule.onNodeWithText("Próximo passo").assertIsDisplayed()
        rule.onNodeWithText("A análise é individual e vinculada à sua conta Google. O login não comprova uso antigo nem garante o direito.").assertExists()
        rule.onAllNodesWithText("Você pode continuar usando o Core atual.").assertCountEquals(1)
        rule.onNodeWithTag("account_message").assertDoesNotExist()
        rule.onNodeWithTag("account_request").assertDoesNotExist()
        screenshot("s09-refined-recovery-synthetic")
        rule.onNodeWithTag("account_support").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, supported) }
    }
    @Test fun compactAccessRetainsRetryAndDoesNotTurn503IntoMissingRights() {
        var refreshed = 0
        render(AccountState(signedIn = true, linked = true,
            access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)),
            onRefresh = { refreshed++ }, initialPage = AccountPage.ACCESS)
        rule.onNodeWithText("Não conseguimos consultar agora").assertIsDisplayed()
        rule.onNodeWithText("Não encontramos um direito legado confirmado nesta conta.").assertDoesNotExist()
        screenshot("s09-refined-access-synthetic")
        rule.onNodeWithTag("account_refresh").performClick()
        rule.onNodeWithTag("account_recovery").performScrollTo().performClick()
        rule.onNodeWithText("Próximo passo").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, refreshed) }
    }
    @Test fun refinedAccessAtLargeFontKeepsRetryAndRecoveryReachable() {
        render(AccountState(signedIn = true, linked = true,
            access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)),
            small = true, initialPage = AccountPage.ACCESS)
        rule.onNodeWithTag("account_refresh").performScrollTo().assertIsDisplayed()
        screenshot("s09-refined-access-large-font")
        rule.onNodeWithTag("account_recovery").performScrollTo().assertIsDisplayed()
    }
    @Test fun refinedRecoveryAtLargeFontKeepsSupportAndCoreReminderReachable() {
        render(AccountState(signedIn = true, linked = true, notice = AccountNotice.OFFLINE),
            small = true, initialPage = AccountPage.RECOVERY)
        rule.onNodeWithTag("account_support").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("account_core_reminder").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Você pode continuar usando o Core atual.").assertCountEquals(1)
        screenshot("s09-refined-recovery-large-font")
    }
}
