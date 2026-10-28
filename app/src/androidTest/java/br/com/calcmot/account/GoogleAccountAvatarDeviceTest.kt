package br.com.calcmot.account

import android.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import br.com.calcmot.debug.AccountUiTestActivity
import br.com.calcmot.ui.design.theme.CalcMotTheme
import coil3.ColorImage
import coil3.ImageLoader
import coil3.request.ImageResult
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.decode.DataSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** Synthetic pixels only; never downloads a real profile or changes Firebase users. */
class GoogleAccountAvatarDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<AccountUiTestActivity>()
    private var loader: ImageLoader? = null
    private val calls = AtomicInteger()
    private fun imageLoader(successfulCalls: Int) = ImageLoader.Builder(rule.activity)
        .components { add(object : Interceptor {
            override suspend fun intercept(chain: Interceptor.Chain): ImageResult =
                if (calls.incrementAndGet() <= successfulCalls)
                    SuccessResult(image = ColorImage(Color.BLUE), request = chain.request, dataSource = DataSource.MEMORY)
                else ErrorResult(image = null, request = chain.request, throwable = IOException("Synthetic image failure"))
        }) }.build().also { loader = it }
    @After fun closeLoader() { loader?.shutdown() }

    @Test fun missingPhotoKeepsFallbackWithoutFetching() {
        val images = imageLoader(0)
        rule.setContent { CalcMotTheme { GoogleAccountAvatar(null, images) } }
        rule.onNodeWithTag("account_avatar_fallback").assertIsDisplayed()
        rule.onNodeWithTag("account_avatar_photo").assertDoesNotExist()
        rule.runOnIdle { assertEquals(0, calls.get()) }
    }
    @Test fun loadedPhotoDisappearsImmediatelyAtLogout() {
        val images = imageLoader(1)
        val photo = mutableStateOf<AccountProfilePhoto?>(AccountProfilePhoto("owner-a", "https://example.test/avatar"))
        rule.setContent { CalcMotTheme { GoogleAccountAvatar(photo.value, images) } }
        rule.waitUntil(5000) { rule.onAllNodesWithTag("account_avatar_photo").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("account_avatar_fallback").assertDoesNotExist()
        rule.runOnIdle { photo.value = null }
        rule.onNodeWithTag("account_avatar_fallback").assertIsDisplayed()
        rule.onNodeWithTag("account_avatar_photo").assertDoesNotExist()
    }
    @Test fun anotherOwnerWithSameUrlCannotRetainThePreviousBitmap() {
        val images = imageLoader(1)
        val url = "https://example.test/avatar"
        val photo = mutableStateOf<AccountProfilePhoto?>(AccountProfilePhoto("owner-a", url))
        rule.setContent { CalcMotTheme { GoogleAccountAvatar(photo.value, images) } }
        rule.waitUntil(5000) { rule.onAllNodesWithTag("account_avatar_photo").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { photo.value = AccountProfilePhoto("owner-b", url) }
        rule.waitUntil(5000) { calls.get() == 2 }
        rule.onNodeWithTag("account_avatar_fallback").assertIsDisplayed()
        rule.onNodeWithTag("account_avatar_photo").assertDoesNotExist()
    }
    @Test fun downloadFailureUsesFallbackWithoutBlockingTheScreen() {
        val images = imageLoader(0)
        rule.setContent { CalcMotTheme { GoogleAccountAvatar(AccountProfilePhoto("owner-a", "https://example.test/avatar"), images) } }
        rule.waitUntil(5000) { calls.get() == 1 }
        rule.onNodeWithTag("account_avatar_fallback").assertIsDisplayed()
        rule.onNodeWithTag("account_avatar_photo").assertDoesNotExist()
    }
}
