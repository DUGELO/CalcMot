package br.com.calcmot.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
        composeRule.onNodeWithTag(UiTestTags.ACCESSIBILITY_DISCLOSURE).assertIsDisplayed()
        composeRule.onNodeWithText("CalcMot").assertIsDisplayed()
        composeRule.onNodeWithText("Semáforo de lucro para motoristas").assertIsDisplayed()
        composeRule.onNodeWithText("Decida corridas com", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Veja rapidamente se a corrida", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Calcule R$/km e R$/h").assertIsDisplayed()
        composeRule.onNodeWithText("Veja Boa, Média ou Ruim").assertIsDisplayed()
        composeRule.onNodeWithText("Tenha resposta rápida", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UiTestTags.FINISH_ONBOARDING_BUTTON).assertCountEquals(0)

        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_NEXT_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(UiTestTags.ACCESSIBILITY_PERMISSION_ITEM).assertIsDisplayed()
        composeRule.onNodeWithText("Permissão de acessibilidade").assertIsDisplayed()
        composeRule.onNodeWithText("Como o CalcMot usa essa permissão").assertIsDisplayed()
        composeRule.onNodeWithText("Identifica valor, distância e tempo", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_NEXT_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertCountEquals(0)

        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_NEXT_BUTTON).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(UiTestTags.ACCESSIBILITY_CONFIRMATION_PAGE).assertIsDisplayed()
        composeRule.onNodeWithText("O que o CalcMot não faz").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Não aceita ou recusa corridas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON).assertIsEnabled()
        composeRule.onNodeWithText("Entendo e quero ativar").assertIsDisplayed()
        composeRule.onNodeWithText("Ver política de privacidade").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.ONBOARDING_BACK_BUTTON).assertCountEquals(0)
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

        composeRule.onNodeWithTag(UiTestTags.ACCESSIBILITY_CONFIRMATION_PAGE).assertIsDisplayed()
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

        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_NEXT_BUTTON)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_NEXT_BUTTON)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(UiTestTags.PRIVACY_LINK)
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
