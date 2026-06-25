package br.com.calcmot.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.OverlayPositionPreference
import br.com.calcmot.ui.theme.MetricaTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InternalScreensVisualTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun resetSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("calcmot_settings", 0).edit().clear().commit()
        AppSettings.setMonitoringEnabled(context, true)
        AppSettings.setOverlayPosition(context, OverlayPositionPreference.HIGH)
    }

    @Test
    fun capturesHomeWithoutPermission() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = false))
        composeRule.onNodeWithText("Falta ativar o cálculo automático").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.HOME_HERO_CARD).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.HOME_GOAL_CARD).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.HOME_PRIMARY_ACTION).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.MONITORING_SWITCH).assertCountEquals(0)
        assertNoCommonUserFacingForbiddenTerms()
        captureScreen("home_without_permission", forbidRedDominant = true)
    }

    @Test
    fun capturesHomeReady() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))
        composeRule.onNodeWithText("Calculador de ganhos").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.HOME_HERO_CARD).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.HOME_GOAL_CARD).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.OPEN_UBER_DRIVER_BUTTON).assertCountEquals(1)
        composeRule.onAllNodesWithTag(UiTestTags.OPEN_99_DRIVER_BUTTON).assertCountEquals(1)
        assertNoCommonUserFacingForbiddenTerms()
        captureScreen("home_ready")
    }

    @Test
    fun capturesHomePaused() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.setMonitoringEnabled(context, false)
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))
        composeRule.onNodeWithText("Ligar cálculo automático").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.MONITORING_SWITCH).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UiTestTags.HOME_PRIMARY_ACTION).assertCountEquals(1)
        captureScreen("home_paused")
    }

    @Test
    fun capturesGoals() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_FINANCE_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.FINANCE_SCREEN)
            .performScrollToNode(hasText("Semáforo"))
        composeRule.onNodeWithText("Semáforo").assertIsDisplayed()
        composeRule.onAllNodesWithText("Equilibrado").assertCountEquals(1)
        assertNoCommonUserFacingForbiddenTerms()
        captureScreen("goals")
    }

    @Test
    fun capturesSettings() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_SETTINGS_ITEM).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_SCREEN).assertIsDisplayed()
        composeRule.onAllNodesWithText("Você no controle").assertCountEquals(0)
        composeRule.onAllNodesWithText("Segurança no dia a dia").assertCountEquals(0)
        captureScreen("settings")
    }

    @Test
    fun capturesHelpAndPrivacy() {
        renderHome(permissionState = AppPermissionState(hasAccessibilityService = true))
        composeRule.onNodeWithTag(UiTestTags.DRAWER_MENU_BUTTON).performClick()
        composeRule.onNodeWithTag(UiTestTags.DRAWER_HELP_ITEM).performClick()
        composeRule.onAllNodesWithTag(UiTestTags.FAQ_ITEM).assertCountEquals(5)
        composeRule.onNodeWithText("O que significam as cores").assertIsDisplayed()
        assertNoCommonUserFacingForbiddenTerms()
        captureScreen("help")

        composeRule.onNodeWithTag(UiTestTags.HELP_PRIVACY_BUTTON).performScrollTo().performClick()
        composeRule.onNodeWithText("O CalcMot calcula ofertas visíveis", substring = true).assertIsDisplayed()
        captureScreen("privacy")
    }

    @Test
    fun capturesOnboarding() {
        composeRule.setContent {
            MetricaTheme {
                OnboardingScreen(
                    permissionState = AppPermissionState(hasAccessibilityService = false),
                    onPermissionsRefresh = {}
                )
            }
        }
        composeRule.onNodeWithText("Veja se a corrida compensa antes de aceitar.").assertIsDisplayed()
        composeRule.onAllNodesWithTag(UiTestTags.FINISH_ONBOARDING_BUTTON).assertCountEquals(0)
        assertNoCommonUserFacingForbiddenTerms()
        captureScreen("onboarding")
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

    private fun assertNoCommonUserFacingForbiddenTerms() {
        composeRule.onAllNodesWithText("ATENÇÃO", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Safe Mode", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("whitelist", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("pipeline", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("serviço rodando", substring = true).assertCountEquals(0)
    }

    private fun captureScreen(name: String, forbidRedDominant: Boolean = false) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        assertTrue("Screenshot $name width should be positive", bitmap.width > 0)
        assertTrue("Screenshot $name height should be positive", bitmap.height > 0)
        assertTrue("Screenshot $name should not be blank", bitmap.hasDifferentPixels())
        if (forbidRedDominant) {
            assertTrue("Screenshot $name should not be red-dominant", bitmap.redDominanceRatio() < 0.12f)
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = context.getExternalFilesDir("calcmot-screenshots")
            ?: File(context.filesDir, "calcmot-screenshots")
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        saveScreenshotToPictures(bitmap, name)
    }

    private fun saveScreenshotToPictures(bitmap: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/CalcMotScreenshots"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create MediaStore entry for $name")
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: error("Could not open MediaStore stream for $name")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }
}

private fun Bitmap.hasDifferentPixels(): Boolean {
    val first = getPixel(0, 0)
    val stepX = (width / 12).coerceAtLeast(1)
    val stepY = (height / 12).coerceAtLeast(1)
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            if (getPixel(x, y) != first) return true
            x += stepX
        }
        y += stepY
    }
    return false
}

private fun Bitmap.redDominanceRatio(): Float {
    val stepX = (width / 48).coerceAtLeast(1)
    val stepY = (height / 48).coerceAtLeast(1)
    var sampled = 0
    var redDominant = 0
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = getPixel(x, y)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            if (r > 150 && r > g + 50 && r > b + 50) redDominant++
            sampled++
            x += stepX
        }
        y += stepY
    }
    return redDominant.toFloat() / sampled.toFloat()
}
