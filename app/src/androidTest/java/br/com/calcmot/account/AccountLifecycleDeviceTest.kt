package br.com.calcmot.account

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import br.com.calcmot.debug.AccountUiTestActivity
import br.com.calcmot.entitlement.*
import br.com.calcmot.identity.*
import br.com.calcmot.ui.theme.MetricaTheme
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Actual AccountRoute/NavBackStackEntry ownership. Only transport/token test doubles;
 * no Firebase user mutation, real image download, Worker request or Core bootstrap. */
@OptIn(coil3.annotation.DelicateCoilApi::class)
class AccountLifecycleDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<AccountUiTestActivity>()
    private var previousImages: ImageLoader? = null
    private var images: ImageLoader? = null
    private var tracking: ObservedApplication? = null
    private val imageCalls = AtomicInteger()
    private val activeImages = AtomicInteger()
    private val canceledImages = AtomicInteger()

    @After fun cleanup() {
        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        tracking?.dispose()
        previousImages?.let { SingletonImageLoader.setUnsafe(it) }
        images?.shutdown()
    }

    private fun observeImages() {
        previousImages = SingletonImageLoader.get(rule.activity)
        images = ImageLoader.Builder(rule.activity).components {
            add(object : Interceptor {
                override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                    imageCalls.incrementAndGet(); activeImages.incrementAndGet()
                    try { awaitCancellation() }
                    finally { activeImages.decrementAndGet(); canceledImages.incrementAndGet() }
                }
            })
        }.build().also { SingletonImageLoader.setUnsafe(it) }
    }

    @Test fun removingActualAccountEntryClosesAdapterAndObserversButBackgroundDoesNot() {
        check(rule.activity.packageName == "br.com.calcmot.identitydev")
        observeImages()
        val calls = AtomicInteger()
        lateinit var adapter: AndroidEntitlements
        val originalApp = rule.activity.application
        val app = ObservedApplication(originalApp) { callback ->
            adapter = callback.javaClass.declaredFields.first {
                it.type == AndroidEntitlements::class.java
            }.apply { isAccessible = true }.get(callback) as AndroidEntitlements
            // Installed during registration, before AccountRoute's open effect can query.
            field(adapter.repository, "auth").set(adapter.repository, object : IdentityAuthPort {
                override fun currentSession() = IdentitySession("lifecycle-synthetic", setOf("google.com"))
                override suspend fun signIn(googleIdToken: String): IdentitySession = error("Not exercised")
                override suspend fun link(uid: String, googleIdToken: String): IdentitySession = error("Not exercised")
                override suspend fun reauthenticate(uid: String, googleIdToken: String): IdentitySession = error("Not exercised")
                override suspend fun firebaseIdToken(forceRefresh: Boolean) = "synthetic-local-token"
            })
            field(adapter.repository, "remote").set(adapter.repository, object : EntitlementRemote {
                override suspend fun get(token: String): EntitlementHttpResponse {
                    calls.incrementAndGet(); return EntitlementHttpResponse(503, "")
                }
            })
        }.also { tracking = it }
        val context = object : ContextWrapper(rule.activity) {
            override fun getApplicationContext(): Context = app
        }
        val nav = TestNavHostController(rule.activity).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        rule.setContent { CompositionLocalProvider(LocalContext provides context) {
            MetricaTheme { NavHost(nav, startDestination = "home") {
                composable("home") { Text("Lifecycle test home") }
                composable("account") { AccountRoute { nav.popBackStack() } }
                composable("cover") { Text("Account still in the back stack") }
            } }
        } }
        rule.runOnIdle { nav.navigate("account") }
        rule.waitUntil(5000) { app.registered.get() == 1 }
        val entry = nav.getBackStackEntry("account")
        val neverCreate = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = error("Route must own this instance")
        }
        val model = ViewModelProvider(entry, neverCreate)[AccountViewModel::class.java]
        val identity: IdentityCoordinator = read(model, "identity")
        val identityFlow: MutableStateFlow<*> = read(identity, "mutableSession")
        val rightsFlow: MutableStateFlow<*> = read(adapter.repository, "mutable")
        val adapterScope: CoroutineScope = read(adapter, "scope")
        val timerScope: CoroutineScope = read(adapter.repository, "timerScope")
        rule.waitUntil(5000) { identityFlow.subscriptionCount.value > 0 && rightsFlow.subscriptionCount.value > 0 }

        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        assertTrue(model.viewModelScope.coroutineContext[Job]!!.isActive)
        assertEquals(0, app.unregistered.get())
        val beforeResume = calls.get()
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitUntil(5000) { calls.get() > beforeResume }
        assertTrue(adapterScope.coroutineContext[Job]!!.isActive)

        rule.runOnIdle { nav.navigate("cover") }
        rule.waitForIdle()
        assertSame(model, ViewModelProvider(entry, neverCreate)[AccountViewModel::class.java])
        assertEquals(0, app.unregistered.get()) // Entry retained is not entry removed.
        rule.runOnIdle { nav.popBackStack() }
        rule.waitForIdle()
        rule.runOnIdle { nav.popBackStack() }
        rule.waitUntil(5000) { entry.lifecycle.currentState == Lifecycle.State.DESTROYED && app.unregistered.get() == 1 }
        rule.waitUntil(5000) { identityFlow.subscriptionCount.value == 0 && rightsFlow.subscriptionCount.value == 0 }
        assertFalse(model.viewModelScope.coroutineContext[Job]!!.isActive)
        assertFalse(adapterScope.coroutineContext[Job]!!.isActive)
        assertFalse(timerScope.coroutineContext[Job]!!.isActive)

        val stoppedCalls = calls.get(); val stoppedResumes = app.resumes.get(); val stoppedImages = imageCalls.get()
        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        assertEquals(stoppedResumes, app.resumes.get())
        assertEquals(stoppedCalls, calls.get())
        assertEquals(stoppedImages, imageCalls.get())
        evidence("route-lifecycle", mapOf("registered" to app.registered.get(), "unregistered" to app.unregistered.get(),
            "queriesBeforeFinalResume" to stoppedCalls, "queriesAfterFinalResume" to calls.get(),
            "callbacksBeforeFinalResume" to stoppedResumes, "callbacksAfterFinalResume" to app.resumes.get(),
            "identitySubscriptionsAfterPop" to identityFlow.subscriptionCount.value,
            "rightsSubscriptionsAfterPop" to rightsFlow.subscriptionCount.value,
            "viewModelActiveAfterPop" to model.viewModelScope.coroutineContext[Job]!!.isActive,
            "adapterActiveAfterPop" to adapterScope.coroutineContext[Job]!!.isActive,
            "timerActiveAfterPop" to timerScope.coroutineContext[Job]!!.isActive))
    }

    @Test fun removingPhotoEntryCancelsItsRequestAndDoesNotReloadOnResume() {
        observeImages()
        val nav = TestNavHostController(rule.activity).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
        val state = AccountState(signedIn = true, linked = true, displayName = "Synthetic driver",
            linkedProviders = setOf("google.com"), profilePhoto = AccountProfilePhoto("synthetic-photo-owner", "https://example.test/avatar"))
        rule.setContent { MetricaTheme { NavHost(nav, startDestination = "home") {
            composable("home") { Text("Photo lifecycle test home") }
            composable("account") { AccountScreen(state, { nav.popBackStack() }, {}, {}, {}, {}, {}) }
        } } }
        rule.runOnIdle { nav.navigate("account") }
        val entry = nav.getBackStackEntry("account")
        rule.waitUntil(5000) { activeImages.get() == 1 }
        rule.runOnIdle { nav.popBackStack() }
        rule.waitUntil(5000) { entry.lifecycle.currentState == Lifecycle.State.DESTROYED && activeImages.get() == 0 }
        assertEquals(1, canceledImages.get())
        val beforeResume = imageCalls.get()
        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        assertEquals(beforeResume, imageCalls.get())
        evidence("photo-lifecycle", mapOf("requestsBeforeResume" to beforeResume, "requestsAfterResume" to imageCalls.get(),
            "activeAfterPop" to activeImages.get(), "canceledRequests" to canceledImages.get()))
    }

    private fun evidence(name: String, values: Map<String, Any>) {
        val root = androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { java.io.File(it) } ?: rule.activity.getExternalFilesDir("s09-lifecycle")!!
        root.mkdirs()
        java.io.File(root, "$name.json").writeText(org.json.JSONObject(values).toString(2))
    }

    private fun field(owner: Any, name: String) = owner.javaClass.getDeclaredField(name).apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST") private fun <T> read(owner: Any, name: String): T = field(owner, name).get(owner) as T

    /** Observes actual framework callbacks without reading Android hidden fields. */
    private class ObservedApplication(private val actual: Application,
        private val registeredHook: (ActivityLifecycleCallbacks) -> Unit) : Application() {
        val registered = AtomicInteger(); val unregistered = AtomicInteger(); val resumes = AtomicInteger()
        private val forwarding = mutableMapOf<ActivityLifecycleCallbacks, ActivityLifecycleCallbacks>()
        init { attachBaseContext(actual) }
        override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
            registeredHook(callback)
            val delegate = object : ActivityLifecycleCallbacks by callback {
                override fun onActivityResumed(activity: Activity) { resumes.incrementAndGet(); callback.onActivityResumed(activity) }
            }
            forwarding[callback] = delegate; registered.incrementAndGet()
            actual.registerActivityLifecycleCallbacks(delegate)
        }
        override fun unregisterActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {
            forwarding.remove(callback)?.let { actual.unregisterActivityLifecycleCallbacks(it); unregistered.incrementAndGet() }
        }
        fun dispose() { forwarding.values.forEach { actual.unregisterActivityLifecycleCallbacks(it) }; forwarding.clear() }
    }
}
