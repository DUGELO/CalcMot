package br.com.calcmot.debug

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.calcmot.ui.UiTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Runs on a real Android device against the DEBUG-only QA laboratory.
 * Screenshots are persisted for adb pull; no real shares, ratings or notifications sent.
 */
@RunWith(AndroidJUnit4::class)
class GrowthVisualE2ETest {
    @get:Rule val rule = createAndroidComposeRule<GrowthQaActivity>()

    private fun saveScreenshot(name: String) {
        rule.waitForIdle()
        val dir = File(rule.activity.getExternalFilesDir(null), "growth-qa-screenshots")
        assertTrue(dir.exists() || dir.mkdirs())
        val screenshot = rule.activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(
            screenshot.width.coerceAtLeast(1),
            screenshot.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        screenshot.draw(canvas)
        FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test
    fun screenAndShareTwoDaysWithDismiss() {
        rule.onNodeWithTag(GrowthQaTags.SCREEN).assertIsDisplayed()
        saveScreenshot("01-growth-lab.png")
        rule.onNodeWithTag(GrowthQaTags.STREAK_2).performClick()
        rule.onNodeWithTag(GrowthQaTags.SHARE_PREVIEW).performScrollTo().performClick()
        rule.onNodeWithTag(UiTestTags.GROWTH_SHARE_PROMPT).assertIsDisplayed()
        rule.onNodeWithText("2 dias seguidos").assertIsDisplayed()
        rule.onNodeWithTag(UiTestTags.GROWTH_SHARE_ART).assertIsDisplayed()
        saveScreenshot("02-share-2-days.png")
        rule.onNodeWithTag(UiTestTags.GROWTH_SHARE_PROMPT_DISMISS).performClick()
        rule.onNodeWithTag(GrowthQaTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun shareThirtyDaysAndCtaAreOnlySimulated() {
        rule.onNodeWithTag(GrowthQaTags.STREAK_30).performScrollTo().performClick()
        rule.onNodeWithTag(GrowthQaTags.SHARE_PREVIEW).performScrollTo().performClick()
        rule.onNodeWithText("30 dias seguidos").assertIsDisplayed()
        saveScreenshot("03-share-30-days.png")
        rule.onNodeWithTag(UiTestTags.GROWTH_SHARE_PROMPT_ACTION).performClick()
        rule.onNodeWithTag(GrowthQaTags.SHARE_FEEDBACK).assertIsDisplayed()
    }

    @Test
    fun manualPlayRatingPreviewAndNotificationPreview() {
        rule.onNodeWithTag(GrowthQaTags.STREAK_10).performScrollTo().performClick()
        rule.onNodeWithTag(GrowthQaTags.RATE_PREVIEW).performScrollTo().performClick()
        rule.onNodeWithTag(GrowthQaTags.RATE_DIALOG).assertIsDisplayed()
        saveScreenshot("04-rating-explanation.png")
        rule.onNodeWithText("Entendi").performClick()
        rule.onNodeWithTag(GrowthQaTags.NOTIFICATION_PREVIEW).performScrollTo().performClick()
        rule.onNodeWithTag(GrowthQaTags.NOTIFICATION_DIALOG).assertIsDisplayed()
        saveScreenshot("05-notification-preview.png")
    }
}
