package br.com.calcmot.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.OverlayCustomPosition
import br.com.calcmot.OverlayPositionPreference
import br.com.calcmot.finance.FinanceRepository
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun resetSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("calcmot_settings", 0).edit().clear().commit()
        AppSettings.setMonitoringEnabled(context, true)
        AppSettings.setOverlayPosition(context, OverlayPositionPreference.HIGH)
        context.getSharedPreferences("calcmot_finance", 0).edit().clear().commit()
    }

    @After
    fun restoreMonitoringForManualFieldTests() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setMonitoringEnabled(context, true)
    }

    @Test
    fun readyStateShowsSimpleHomeAndOverlayPreview() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.HOME_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Em espera").assertIsDisplayed()
        composeRule.onAllNodesWithText("Roots vistos").assertCountEquals(0)
        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).assertIsOn()
        composeRule.onNodeWithTag(UiTestTags.OPEN_DRIVER_APP_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Abrir Uber Driver").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.SAFETY_SUMMARY_CARD)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Ver segurança").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OVERLAY_PREVIEW)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("BOA").assertIsDisplayed()
        composeRule.onNodeWithText("Dentro da meta").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 2,50/km").assertIsDisplayed()
        composeRule.onAllNodesWithText("por km").assertCountEquals(0)
        composeRule.onNodeWithText("R$ 41,86/h · 43 min").assertIsDisplayed()
        composeRule.onAllNodesWithText("por hora").assertCountEquals(0)
        composeRule.onAllNodesWithText("tempo total").assertCountEquals(0)
    }

    @Test
    fun drawerNavigatesToFinance() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_FINANCE_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.FINANCE_SCREEN).assertIsDisplayed()
    }

    @Test
    fun drawerNavigatesToResult() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_RESULT_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.RESULT_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Resultado de hoje").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum ganho ou custo anotado hoje.").assertIsDisplayed()
    }

    @Test
    fun financePrioritizesGoalAndKeepsAdvancedCostsCollapsed() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_FINANCE_ITEM).performClick()
        composeRule.onNodeWithText("Mostrar impacto na meta").assertIsDisplayed()
        composeRule.onNodeWithText("Escolha uma meta rápida").assertIsDisplayed()
        composeRule.onNodeWithText("Começando").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 1,50/km • R$ 35,00/h").assertIsDisplayed()
        composeRule.onNodeWithText("Exigente").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.FINANCE_AMOUNT_INPUT).assertCountEquals(0)

        composeRule.onNodeWithTag(UiTestTags.FINANCE_SCREEN)
            .performScrollToNode(hasTestTag(UiTestTags.PROFIT_ADVANCED_TOGGLE))
        composeRule.onAllNodesWithTag(UiTestTags.PROFIT_EFFICIENCY_INPUT).assertCountEquals(0)
        composeRule.onNodeWithTag(UiTestTags.PROFIT_ADVANCED_TOGGLE).performClick()
        composeRule.onNodeWithTag(UiTestTags.PROFIT_EFFICIENCY_INPUT).assertIsDisplayed()
    }

    @Test
    fun drawerNavigatesToHelpAndPrivacy() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_HELP_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.HELP_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Segurança e privacidade").assertIsDisplayed()

        composeRule.onNodeWithTag(UiTestTags.HELP_PRIVACY_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.PRIVACY_POLICY_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Sua segurança primeiro").assertIsDisplayed()
        composeRule.onNodeWithText("3 garantias importantes").assertIsDisplayed()
        composeRule.onNodeWithText("O que o CalcMot não faz").assertIsDisplayed()
        composeRule.onNodeWithText("eduardoangelo20001@gmail.com", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun drawerNavigatesToSecurity() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_SECURITY_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.SECURITY_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Onde o CalcMot atua").assertIsDisplayed()
        composeRule.onNodeWithText("O que ele não faz")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun securityScreenPausesMonitoring() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithText("Ver segurança")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(UiTestTags.SECURITY_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).performClick()

        composeRule.runOnIdle {
            assertFalse(AppSettings.isMonitoringEnabled(context))
        }
        composeRule.onNodeWithText("CalcMot pausado por você").assertIsDisplayed()
    }

    @Test
    fun drawerNavigatesToDiagnostics() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_DIAGNOSTICS_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.DIAGNOSTICS_SCREEN).assertIsDisplayed()
    }

    @Test
    fun financeCreatesAndDeletesEntry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_RESULT_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.RESULT_SCREEN)
            .performScrollToNode(hasTestTag(UiTestTags.FINANCE_AMOUNT_INPUT))
        composeRule.onNodeWithTag(UiTestTags.FINANCE_AMOUNT_INPUT).performTextInput("123,45")
        composeRule.onNodeWithTag(UiTestTags.FINANCE_DESCRIPTION_INPUT).performTextInput("Pedágio")
        composeRule.onNodeWithTag(UiTestTags.FINANCE_ADD_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, FinanceRepository(context).getEntries().size)
        }

        composeRule.onNodeWithTag(UiTestTags.RESULT_SCREEN)
            .performScrollToNode(hasText("Pedágio"))
        composeRule.onNodeWithText("Pedágio").assertIsDisplayed()

        composeRule.onNodeWithTag(UiTestTags.RESULT_SCREEN)
            .performScrollToNode(hasText("Excluir"))
        composeRule.onNodeWithText("Excluir").performClick()
        composeRule.runOnIdle {
            assertEquals(0, FinanceRepository(context).getEntries().size)
        }
        composeRule.onNodeWithText("Nenhum ganho ou custo anotado hoje.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun monitoringTogglePersistsPausedState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).performClick()

        composeRule.runOnIdle {
            assertFalse(AppSettings.isMonitoringEnabled(context))
        }
        composeRule.onNodeWithText("Pausado").assertIsDisplayed()
        composeRule.onNodeWithText("Ligar CalcMot").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.OPEN_DRIVER_APP_BUTTON).assertCountEquals(0)
        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).assertIsOff()
    }

    @Test
    fun pendingPermissionDisablesMonitoringAndShowsAction() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = false))

        composeRule.onNodeWithText("Falta permissão").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertIsDisplayed()
    }

    @Test
    fun overlayPositionSelectionPersistsAndClearsCustomPosition() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setCustomOverlayPosition(context, OverlayCustomPosition(x = 10, y = 20))
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.OVERLAY_POSITION_MEDIUM)
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(OverlayPositionPreference.MEDIUM, AppSettings.getOverlayPosition(context))
            assertNull(AppSettings.getCustomOverlayPosition(context))
        }
        composeRule.onNodeWithTag(UiTestTags.OVERLAY_POSITION_MEDIUM).assertIsSelected()

        composeRule.onNodeWithTag(UiTestTags.OVERLAY_POSITION_LOW)
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(OverlayPositionPreference.LOW, AppSettings.getOverlayPosition(context))
        }
        composeRule.onNodeWithTag(UiTestTags.OVERLAY_POSITION_LOW).assertIsSelected()
    }

    private fun renderHome(permissionState: AppPermissionState) {
        composeRule.setContent {
            MetricaTheme {
                HomeScreen(
                    permissionState = permissionState,
                    onPermissionsRefresh = {}
                )
            }
        }
    }
}
