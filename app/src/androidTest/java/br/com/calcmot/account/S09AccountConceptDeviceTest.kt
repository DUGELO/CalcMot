package br.com.calcmot.account

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import br.com.calcmot.debug.AccountUiTestActivity
import br.com.calcmot.entitlement.EntitlementState
import br.com.calcmot.entitlement.EntitlementStatus
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real production composition, synthetic states only. Earlier A/B/V3 prototypes are archived. */
class S09AccountConceptDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<AccountUiTestActivity>()
    @Test fun signed_out() = capture("signed_out", false)
    @Test fun signed_outLarge() = capture("signed_out", true)
    @Test fun signed_in() = capture("signed_in", false)
    @Test fun signed_inLarge() = capture("signed_in", true)
    @Test fun information() = capture("information", false)
    @Test fun informationLarge() = capture("information", true)
    @Test fun legacy() = capture("legacy", false)
    @Test fun legacyLarge() = capture("legacy", true)
    @Test fun awaiting() = capture("awaiting", false)
    @Test fun awaitingLarge() = capture("awaiting", true)
    @Test fun no_confirmed_grant() = capture("no_confirmed_grant", false)
    @Test fun no_confirmed_grantLarge() = capture("no_confirmed_grant", true)
    private fun capture(name: String, large: Boolean) {
        check(rule.activity.packageName == "br.com.calcmot.identitydev")
        val access = when (name) {
            "legacy" -> EntitlementState(status = EntitlementStatus.READY, revision = 1L, legacyCore = true, legacyState = "GRANTED")
            "awaiting" -> EntitlementState(status = EntitlementStatus.EMPTY)
            "no_confirmed_grant" -> EntitlementState(status = EntitlementStatus.READY, revision = 1L, legacyState = "UNKNOWN")
            else -> EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)
        }
        val state = if (name == "signed_out") AccountState() else AccountState(signedIn = true, linked = true,
            displayName = "Alex Santos", email = "alex@example.test", linkedProviders = setOf("google.com"), access = access)
        rule.activityRule.scenario.onActivity { it.enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.BLACK),
            SystemBarStyle.dark(android.graphics.Color.BLACK)) }
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.5f else 1f)) {
                br.com.calcmot.ui.theme.MetricaTheme {
                    AccountScreen(state, {}, {}, {}, {}, {}, {},
                        initialPage = if (name in setOf("signed_out", "signed_in")) AccountPage.ACCOUNT else AccountPage.ACCESS,
                        testLabel = "TESTE — DADOS SINTÉTICOS")
                }
            }
        }
        if (name == "signed_out") rule.onNodeWithTag("account_login").assertIsDisplayed()
        else rule.onNodeWithText("Conta Google conectada").assertIsDisplayed()
        if (name == "legacy") {
            rule.onNodeWithTag("account_recovery").assertDoesNotExist()
            rule.onNodeWithTag("account_legacy_details").assertExists()
        }
        if (name == "no_confirmed_grant") rule.onNodeWithText("Acesso nesta conta").assertExists()
        rule.waitForIdle()
        val dir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: rule.activity.getExternalFilesDir("s09-integrated")!!
        dir.mkdirs()
        val window = rule.activity.window
        val bitmap = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var result = PixelCopy.ERROR_UNKNOWN
        PixelCopy.request(window, bitmap, { result = it; latch.countDown() }, Handler(Looper.getMainLooper()))
        check(latch.await(3, TimeUnit.SECONDS) && result == PixelCopy.SUCCESS)
        File(dir, "s09-integrated-$name${if (large) "-font150" else ""}.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
