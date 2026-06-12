package br.com.calcmot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.calcmot.AppPermissionState
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disclosureAppearsBeforeAccessibilityPermissionAction() {
        composeRule.setContent {
            MetricaTheme {
                OnboardingScreen(
                    permissionState = AppPermissionState(hasAccessibilityService = false),
                    onPermissionsRefresh = {}
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.ACCESSIBILITY_DISCLOSURE)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Veja se a corrida compensa antes de aceitar.").assertIsDisplayed()
        composeRule.onNodeWithText("Calcula R$/km e R$/h.", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("fora dos apps de motorista", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertCountEquals(1)
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertIsEnabled()
        composeRule.onAllNodesWithTag(UiTestTags.FINISH_ONBOARDING_BUTTON).assertCountEquals(0)
        composeRule.onNodeWithText("Depois de ativar a permissão", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun activeAccessibilityEnablesCompletion() {
        composeRule.setContent {
            MetricaTheme {
                OnboardingScreen(
                    permissionState = AppPermissionState(hasAccessibilityService = true),
                    onPermissionsRefresh = {}
                )
            }
        }

        composeRule.onNodeWithText("Permissão ativa", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.FINISH_ONBOARDING_BUTTON).assertIsEnabled()
    }

    @Test
    fun privacyPolicyOpensInsideOnboarding() {
        composeRule.setContent {
            MetricaTheme {
                OnboardingScreen(
                    permissionState = AppPermissionState(hasAccessibilityService = false),
                    onPermissionsRefresh = {}
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.PRIVACY_LINK)
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag(UiTestTags.PRIVACY_POLICY_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("eduardoangelo20001@gmail.com", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(UiTestTags.PRIVACY_POLICY_BACK_BUTTON)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_SCREEN).assertIsDisplayed()
    }
}
