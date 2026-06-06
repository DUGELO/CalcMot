package br.com.calcmot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.calcmot.AppPermissionState
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.screens.OnboardingScreen
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disclosureAppearsBeforeAccessibilityPermissionAction() {
        composeRule.setContent {
            CalcMotTheme {
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
        composeRule.onNodeWithText("Como o CalcMot usa acessibilidade").assertIsDisplayed()
        composeRule.onNodeWithText("O app não toca na tela", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertIsEnabled()
        composeRule.onNodeWithTag(UiTestTags.FINISH_ONBOARDING_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun activeAccessibilityEnablesCompletion() {
        composeRule.setContent {
            CalcMotTheme {
                OnboardingScreen(
                    permissionState = AppPermissionState(hasAccessibilityService = true),
                    onPermissionsRefresh = {}
                )
            }
        }

        composeRule.onNodeWithText("Ativo").assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(UiTestTags.FINISH_ONBOARDING_BUTTON).assertIsEnabled()
    }

    @Test
    fun privacyPolicyOpensInsideOnboarding() {
        composeRule.setContent {
            CalcMotTheme {
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
