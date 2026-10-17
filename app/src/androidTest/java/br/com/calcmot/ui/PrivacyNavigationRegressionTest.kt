package br.com.calcmot.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.platform.app.InstrumentationRegistry
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Reproduces Privacy flashing back to Home after enabling Accessibility
 * during a session initially started at ONBOARDING.
 */
class PrivacyNavigationRegressionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun openingPrivacyFromDrawerAfterOnboardingDoesNotRedirectToHome() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setMonitoringEnabled(context, true)
        val accessibilityEnabled = mutableStateOf(false)
        val onboardingCompleted = mutableStateOf(false)
        val navController = TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }

        composeRule.setContent {
            MetricaTheme {
                CalcMotNavHost(
                    permissionState = AppPermissionState(accessibilityEnabled.value),
                    onboardingCompleted = onboardingCompleted.value,
                    onPermissionsRefresh = {},
                    onOnboardingCompleted = { onboardingCompleted.value = true },
                    navController = navController
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.ONBOARDING_SCREEN).assertIsDisplayed()
        composeRule.runOnIdle { accessibilityEnabled.value = true }
        composeRule.onNodeWithTag(UiTestTags.HOME_READY_SCREEN).assertIsDisplayed()

        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_PRIVACY_ITEM).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(UiTestTags.PRIVACY_POLICY_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.PRIVACY, navController.currentDestination?.route)

        composeRule.onNodeWithTag(UiTestTags.PRIVACY_POLICY_BACK_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.HOME_READY_SCREEN).assertIsDisplayed()
        assertEquals(CalcMotRoute.HOME, navController.currentDestination?.route)
    }
}
