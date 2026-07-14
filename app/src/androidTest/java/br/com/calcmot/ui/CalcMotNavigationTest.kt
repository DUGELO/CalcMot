package br.com.calcmot.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import android.view.View
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CalcMotNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setMonitoringEnabled(context, true)
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

        pressBack()

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

    private fun openSettings() {
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_SETTINGS_ITEM).performClick()
    }
}
