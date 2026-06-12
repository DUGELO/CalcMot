package br.com.calcmot.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun readyHomeHasTwoMainCardsAndOnePrimaryAction() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.HOME_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Calculador de ganhos").assertIsDisplayed()
        composeRule.onNodeWithText("Pronto para calcular").assertIsDisplayed()
        composeRule.onNodeWithText("Abrir app de motorista").assertIsDisplayed()
        composeRule.onNodeWithText("Meta").assertIsDisplayed()
        composeRule.onNodeWithText("Editar meta").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.HOME_HERO_CARD).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.HOME_GOAL_CARD).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.HOME_PRIMARY_ACTION).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.MONITORING_SWITCH).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UiTestTags.SAFETY_SUMMARY_CARD).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UiTestTags.OVERLAY_PREVIEW).assertCountEquals(0)
        composeRule.onAllNodesWithText("Semáforo de lucro").assertCountEquals(0)
        composeRule.onAllNodesWithText("Atenção", substring = true).assertCountEquals(0)
    }

    @Test
    fun permissionHomeUsesCalmRequiredStateAndOnePrimaryAction() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = false))

        composeRule.onNodeWithText("Falta ativar o cálculo automático").assertIsDisplayed()
        composeRule.onNodeWithText("Ativar permissão").assertIsDisplayed()
        composeRule.onNodeWithText("Já ativei").assertIsDisplayed()
        composeRule.onNodeWithText("Fora dos apps de motorista, o CalcMot fica em espera.").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.HOME_PRIMARY_ACTION).assertCountEquals(1)
        composeRule.onAllNodesWithText("erro", substring = true, ignoreCase = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UiTestTags.MONITORING_SWITCH).assertCountEquals(0)
    }

    @Test
    fun pausedHomeHasOnlyOneActionToResume() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setMonitoringEnabled(context, false)

        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithText("Cálculo automático pausado").assertIsDisplayed()
        composeRule.onNodeWithText("Ligar cálculo automático").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.HOME_PRIMARY_ACTION).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.MONITORING_SWITCH).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UiTestTags.OPEN_DRIVER_APP_BUTTON).assertCountEquals(0)
    }

    @Test
    fun goalsUseSegmentedPresetsAndSaveReflectsOnHome() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithText("Editar meta").performClick()
        composeRule.onNodeWithTag(UiTestTags.FINANCE_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Minha meta").assertIsDisplayed()
        composeRule.onNodeWithText("Escolha um perfil").assertIsDisplayed()
        composeRule.onAllNodesWithText("Equilibrado").assertCountEquals(1)
        composeRule.onNodeWithTag(UiTestTags.GOAL_PRESET_DEMANDING).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRIVER_GOAL_KM_INPUT).assertTextContains("1,70")
        composeRule.onNodeWithTag(UiTestTags.DRIVER_GOAL_HOUR_INPUT).assertTextContains("42,00")
        composeRule.onNodeWithTag(UiTestTags.DRIVER_GOAL_SAVE_BUTTON).performClick()

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_HOME_ITEM).performClick()
        composeRule.onNodeWithText("R$ 1,70/km · R$ 42/h").assertIsDisplayed()
    }

    @Test
    fun settingsAreControlsNotExplanatoryCards() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setCustomOverlayPosition(context, OverlayCustomPosition(x = 10, y = 20))
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_SETTINGS_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Cálculo automático").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).assertIsOn()
        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).performClick()

        composeRule.runOnIdle {
            assertFalse(AppSettings.isMonitoringEnabled(context))
        }
        composeRule.onNodeWithTag(UiTestTags.MONITORING_SWITCH).assertIsOff()
        composeRule.onAllNodesWithText("Você no controle").assertCountEquals(0)
        composeRule.onAllNodesWithText("Segurança no dia a dia").assertCountEquals(0)

        composeRule.onNodeWithTag(UiTestTags.OVERLAY_POSITION_MEDIUM)
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(OverlayPositionPreference.MEDIUM, AppSettings.getOverlayPosition(context))
            assertNull(AppSettings.getCustomOverlayPosition(context))
        }
        composeRule.onNodeWithTag(UiTestTags.OVERLAY_POSITION_MEDIUM).assertIsSelected()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_PRIVACY_ROW).assertIsDisplayed()
    }

    @Test
    fun resultEntryFlowStillWorks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_RESULT_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.RESULT_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Resultado de hoje").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(UiTestTags.RESULT_SCREEN)
            .performScrollToNode(hasTestTag(UiTestTags.FINANCE_AMOUNT_INPUT))
        composeRule.onNodeWithTag(UiTestTags.FINANCE_AMOUNT_INPUT).performTextInput("123,45")
        composeRule.onNodeWithTag(UiTestTags.FINANCE_DESCRIPTION_INPUT).performTextInput("Pedágio")
        composeRule.onNodeWithTag(UiTestTags.FINANCE_ADD_BUTTON).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, FinanceRepository(context).getEntries().size)
        }
        composeRule.onNodeWithTag(UiTestTags.RESULT_SCREEN).performScrollToNode(hasText("Pedágio"))
        composeRule.onNodeWithText("Pedágio").assertIsDisplayed()
    }

    @Test
    fun helpAndPrivacyStayCompactAndTrustworthy() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_HELP_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.HELP_SCREEN).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.FAQ_ITEM).assertCountEquals(5)
        composeRule.onNodeWithText("O que significam as cores").assertIsDisplayed()

        composeRule.onNodeWithTag(UiTestTags.HELP_PRIVACY_BUTTON).performScrollTo().performClick()
        composeRule.onNodeWithTag(UiTestTags.PRIVACY_POLICY_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Privacidade").assertIsDisplayed()
        composeRule.onNodeWithText("O CalcMot calcula ofertas visíveis", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("100% invisível", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("garantido", substring = true, ignoreCase = true).assertCountEquals(0)
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
