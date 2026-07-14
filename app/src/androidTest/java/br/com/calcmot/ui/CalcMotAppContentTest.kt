package br.com.calcmot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.OverlayPositionPreference
import br.com.calcmot.CalcMotAppContent
import br.com.calcmot.ui.theme.MetricaTheme
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class CalcMotAppContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permissionMissingShowsOnboarding() {
        composeRule.setContent {
            MetricaTheme {
                CalcMotAppContent(
                    permissionState = AppPermissionState(hasAccessibilityService = false),
                    onPermissionsRefresh = {}
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_SCREEN).assertIsDisplayed()
        composeRule.onAllNodesWithText("Decida corridas", substring = true).assertCountEquals(1)
    }

    @Test
    fun permissionGrantedShowsHome() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setMonitoringEnabled(context, true)
        AppSettings.setOverlayPosition(context, OverlayPositionPreference.HIGH)

        composeRule.setContent {
            MetricaTheme {
                CalcMotAppContent(
                    permissionState = AppPermissionState(hasAccessibilityService = true),
                    onPermissionsRefresh = {}
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.HOME_READY_SCREEN).assertIsDisplayed()
        composeRule.onAllNodesWithText("Pronto para calcular").assertCountEquals(2)
    }

    @Test
    fun returningUserWithoutPermissionSeesHomeRecoveryState() {
        composeRule.setContent {
            MetricaTheme {
                CalcMotAppContent(
                    permissionState = AppPermissionState(hasAccessibilityService = false),
                    onboardingCompleted = true,
                    onPermissionsRefresh = {}
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.HOME_PERMISSION_REQUIRED_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Permissão necessária").assertIsDisplayed()
    }
}
