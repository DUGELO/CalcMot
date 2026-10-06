package br.com.calcmot.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.platform.app.InstrumentationRegistry
import android.view.View
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.OverlayThemePreference
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CalcMotNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController
    private var originalMonitoringEnabled = true
    private var originalOverlayTheme = OverlayThemePreference.CLASSIC
    private var originalFinancialDisclosureDecided = false
    private var originalFinancialConsent = false
    private var originalFinancialHistoryEnabled = false

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        originalMonitoringEnabled = AppSettings.isMonitoringEnabled(context)
        originalOverlayTheme = AppSettings.getOverlayTheme(context)
        originalFinancialDisclosureDecided = AppSettings.hasFinancialHistoryDisclosureDecision(context)
        originalFinancialConsent = AppSettings.hasFinancialHistoryConsent(context)
        originalFinancialHistoryEnabled = AppSettings.isFinancialHistoryEnabled(context)
        AppSettings.setMonitoringEnabled(context, true)
        AppSettings.setOverlayTheme(context, OverlayThemePreference.CLASSIC)
        AppSettings.setFinancialHistoryConsent(context, false)
        navController = TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        composeRule.setContent {
            MetricaTheme {
                CalcMotNavHost(
                    permissionState = AppPermissionState(hasAccessibilityService = true),
                    onboardingCompleted = true,
                    onPermissionsRefresh = {},
                    onOnboardingCompleted = {},
                    navController = navController
                )
            }
        }
    }

    @After
    fun restoreAppSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setMonitoringEnabled(context, originalMonitoringEnabled)
        AppSettings.setOverlayTheme(context, originalOverlayTheme)
        if (originalFinancialDisclosureDecided) {
            AppSettings.setFinancialHistoryConsent(context, originalFinancialConsent)
            AppSettings.setFinancialHistoryEnabled(context, originalFinancialHistoryEnabled)
        } else {
            context.getSharedPreferences("calcmot_finance_settings", 0)
                .edit()
                .remove("financial_history_disclosure_decided_v1")
                .remove("financial_history_consented_v1")
                .remove("financial_history_enabled")
                .commit()
        }
    }

    @Test
    fun internalScreenUsesUpNavigationAndHasNoDrawer() {
        openSettings()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_SCREEN).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.DRAWER_MENU_BUTTON).assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Voltar").performClick()

        composeRule.onNodeWithTag(UiTestTags.HOME_READY_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.HOME, navController.currentDestination?.route)
    }

    @Test
    fun systemBackMatchesTopBarBack() {
        openSettings()

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithTag(UiTestTags.HOME_READY_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.HOME, navController.currentDestination?.route)
    }

    @Test
    fun privacyReturnsToItsActualOrigin() {
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_HELP_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.HELP_PRIVACY_BUTTON).performScrollTo().performClick()
        composeRule.onNodeWithTag(UiTestTags.PRIVACY_POLICY_SCREEN).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Voltar").performClick()

        composeRule.onNodeWithTag(UiTestTags.HELP_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.HELP, navController.currentDestination?.route)
    }

    @Test
    fun darkThemeKeepsSystemBarIconsLight() {
        composeRule.runOnIdle {
            val flags = composeRule.activity.window.decorView.systemUiVisibility
            assertEquals(0, flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            assertEquals(0, flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }
    }

    @Test
    fun overlayThemeSelectionPersistsAndReturnsToSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        openSettings()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_OVERLAY_THEME_ROW)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(UiTestTags.OVERLAY_THEME_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OVERLAY_THEME_SOLID).performClick()
        composeRule.onNodeWithTag(UiTestTags.OVERLAY_THEME_SAVE_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.SETTINGS_SCREEN).assertIsDisplayed()
        assertEquals(OverlayThemePreference.SOLID, AppSettings.getOverlayTheme(context))
        assertEquals(CalcMotRoute.SETTINGS, navController.currentDestination?.route)
    }

    @Test
    fun dashboardFinanceOpensFromDrawerAndReturnsToHome() {
        openDashboardFinance()

        composeRule.onNodeWithTag(UiTestTags.OPEN_DASHBOARD_FINANCE_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.OPEN_DASHBOARD_FINANCE, navController.currentDestination?.route)
        composeRule.onNodeWithContentDescription("Voltar").performClick()

        composeRule.onNodeWithTag(UiTestTags.HOME_READY_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.HOME, navController.currentDestination?.route)
    }

    @Test
    fun dashboardFinanceUsesShortSectionNavigation() {
        openDashboardFinance()

        composeRule.onNodeWithTag(UiTestTags.FINANCIAL_ACTIVITY_ACTION)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(UiTestTags.FINANCIAL_ACTIVITY_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.FINANCIAL_ACTIVITY, navController.currentDestination?.route)

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.onNodeWithTag(UiTestTags.OPEN_DASHBOARD_FINANCE_SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(UiTestTags.FINANCIAL_SOURCES_ACTION)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(UiTestTags.FINANCIAL_SOURCES_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.FINANCIAL_SOURCES, navController.currentDestination?.route)
    }

    @Test
    fun dataQualityReturnsToItsFinancialOrigin() {
        openDashboardFinance()
        composeRule.onNodeWithTag(UiTestTags.FINANCIAL_SOURCES_ACTION)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Confiança da leitura")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag(UiTestTags.FINANCIAL_DATA_QUALITY_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").performClick()

        composeRule.onNodeWithTag(UiTestTags.FINANCIAL_SOURCES_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.FINANCIAL_SOURCES, navController.currentDestination?.route)
    }

    @Test
    fun securityRecordingHubOpensThroughToolsWithoutStartingARecording() {
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_TOOLS_ITEM).performClick()

        composeRule.onNodeWithTag(UiTestTags.SECURITY_TOOLS_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.SECURITY_TOOLS, navController.currentDestination?.route)
        composeRule.onNodeWithTag(UiTestTags.SECURITY_CAMERA_CARD).performClick()

        composeRule.onNodeWithTag(UiTestTags.SECURITY_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Configuração pendente").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SECURITY_CONFIGURE_ACTION).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SECURITY_RECORDINGS_ACTION).assertIsDisplayed()
        assertEquals(CalcMotRoute.SECURITY_HUB, navController.currentDestination?.route)
    }

    private fun openSettings() {
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_SETTINGS_ITEM).performClick()
    }

    private fun openDashboardFinance() {
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_DASHBOARD_FINANCE_ITEM).performClick()
    }
}
