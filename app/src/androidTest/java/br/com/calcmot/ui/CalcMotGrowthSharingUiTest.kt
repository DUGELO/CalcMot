package br.com.calcmot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.calcmot.ui.theme.MetricaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Isolated Compose checks: tests never open the real Android chooser or send a message. */
class CalcMotGrowthSharingUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun readyHomeNeverSharesUntilVoluntaryTap() {
        var taps = 0
        rule.setContent {
            MetricaTheme {
                HomeReadyScreen(
                    onMenu = {},
                    onOpenUber = {},
                    onOpen99 = {},
                    onOpenGoal = {},
                    onOpenSettings = {},
                    onOpenHelp = {},
                    onShare = { taps++ }
                )
            }
        }
        rule.runOnIdle { assertEquals(0, taps) }
        rule.onNodeWithTag(UiTestTags.HOME_SHARE_BUTTON)
            .performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun pausedHomeAllowsVoluntaryShareWithoutResumingMonitoring() {
        var shares = 0
        var resumed = 0
        rule.setContent {
            MetricaTheme {
                HomePausedScreen(
                    onMenu = {},
                    onResume = { resumed++ },
                    onOpenSettings = {},
                    onShare = { shares++ }
                )
            }
        }
        rule.onNodeWithTag(UiTestTags.HOME_SHARE_BUTTON)
            .performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(1, shares)
            assertEquals(0, resumed)
        }
    }

    @Test
    fun shareSheetShowsDynamicStreakAndAllowsDismissal() {
        var shares = 0
        var dismissals = 0
        rule.setContent {
            MetricaTheme {
                GrowthShareBottomSheet(
                    streakDays = 8,
                    onShare = { shares++ },
                    onDismiss = { dismissals++ }
                )
            }
        }
        rule.onNodeWithTag(UiTestTags.GROWTH_SHARE_STREAK).assertIsDisplayed()
        rule.onNodeWithTag(UiTestTags.GROWTH_SHARE_PROMPT_DISMISS).performClick()
        rule.runOnIdle {
            assertEquals(0, shares)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun shareSheetPrimaryActionRequiresTap() {
        var shares = 0
        rule.setContent {
            MetricaTheme {
                GrowthShareBottomSheet(streakDays = 2, onShare = { shares++ }, onDismiss = {})
            }
        }
        rule.runOnIdle { assertEquals(0, shares) }
        rule.onNodeWithTag(UiTestTags.GROWTH_SHARE_PROMPT_ACTION).performClick()
        rule.runOnIdle { assertEquals(1, shares) }
    }

    @Test
    fun ratingActionIsInFooterAboveDiagnostics() {
        rule.setContent {
            MetricaTheme {
                AppDrawer(
                    status = HomeStatus.READY,
                    onHome = {}, onGoal = {}, onSettings = {}, onHelp = {},
                    onPrivacy = {}, onFeedback = {},
                    onRate = {}, diagnosticsEnabled = true,
                    onDiagnostics = {}
                )
            }
        }
        val rating = rule.onNodeWithTag(UiTestTags.DRAWER_RATE_ITEM)
            .performScrollTo().assertIsDisplayed()
        val diagnostics = rule.onNodeWithTag(UiTestTags.DRAWER_DIAGNOSTICS_ITEM)
            .performScrollTo().assertIsDisplayed()
        val ratingBounds = rating.fetchSemanticsNode().boundsInRoot
        val diagnosticBounds = diagnostics.fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(ratingBounds.top < diagnosticBounds.top)
    }

    @Test
    fun drawerAllowsManualPlayStoreRating() {
        var rateTaps = 0
        rule.setContent {
            MetricaTheme {
                AppDrawer(
                    status = HomeStatus.READY,
                    onHome = {}, onGoal = {}, onSettings = {}, onHelp = {},
                    onPrivacy = {}, onFeedback = {},
                    onRate = { rateTaps++ }
                )
            }
        }
        rule.onNodeWithTag(UiTestTags.DRAWER_RATE_ITEM).performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, rateTaps) }
    }

    @Test
    fun drawerHasAnExplicitShareAction() {
        var taps = 0
        rule.setContent {
            MetricaTheme {
                AppDrawer(
                    status = HomeStatus.READY,
                    onHome = {},
                    onGoal = {},
                    onSettings = {},
                    onHelp = {},
                    onPrivacy = {},
                    onFeedback = {},
                    onShare = { taps++ }
                )
            }
        }
        rule.onNodeWithTag(UiTestTags.DRAWER_SHARE_ITEM)
            .performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, taps) }
    }
}
