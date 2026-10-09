package br.com.calcmot.overlay

import android.annotation.SuppressLint
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.ViewTreeObserver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import br.com.calcmot.AppSettings
import br.com.calcmot.AppDiagnostics
import br.com.calcmot.BuildConfig
import br.com.calcmot.DriverAppPackagePolicy
import br.com.calcmot.DriverApp
import br.com.calcmot.OverlayCustomPosition
import br.com.calcmot.PackageDecision
import br.com.calcmot.accessibility.AccessibilityDebugOverlayState
import br.com.calcmot.model.FinancialImpactCalculator
import br.com.calcmot.model.OfferFinancialImpact
import br.com.calcmot.model.ProfitabilityCalculator
import br.com.calcmot.model.ProfitabilityResult
import br.com.calcmot.model.TripData
import br.com.calcmot.processor.overlayFingerprint
import br.com.calcmot.telemetry.OverlayLatencyTrace
import br.com.calcmot.telemetry.AnalyticsBuckets
import br.com.calcmot.telemetry.AnalyticsEvents
import br.com.calcmot.telemetry.AnalyticsParams
import br.com.calcmot.telemetry.AnalyticsValues
import br.com.calcmot.telemetry.TelemetryProvider
import br.com.calcmot.ui.theme.MetricaTheme
import android.util.Log
import kotlin.math.roundToInt
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import br.com.calcmot.ReadingPipelineRuntime

open class OverlayManager(private val context: Context) : IOverlayManager {

    private val operationGeneration = AtomicLong(0L)
    private val closed = AtomicBoolean(false)
    private val windowOwnership = OverlayWindowOwnership()
    private val baseWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var debugComposeView: ComposeView? = null
    private var overlayWindowContext: Context? = null
    private var debugWindowContext: Context? = null
    private var overlayWindowManager: WindowManager? = null
    private var debugWindowManager: WindowManager? = null
    private val tripDataState = mutableStateOf<TripData?>(null)
    private val profitabilityState = mutableStateOf<ProfitabilityResult?>(null)
    private val financialImpactState = mutableStateOf<OfferFinancialImpact?>(null)
    private val overlayThemeState = mutableStateOf(AppSettings.getOverlayTheme(context))
    private val debugOverlayState = mutableStateOf<AccessibilityDebugOverlayState?>(null)
    private val overlayStateMachine = OverlayStateMachine()
    private var foregroundPackageName: String? = null
    private var trustedDriverPackageName: String? = null
    private var trustedDriverPackageSeenAtElapsed: Long = 0L
    private var criticalBlockPackageName: String? = null
    private var pendingLatencyTrace: OverlayLatencyTrace? = null
    private var lifecycleOwner: CustomLifecycleOwner? = null
    private var debugLifecycleOwner: CustomLifecycleOwner? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentLayoutParams: WindowManager.LayoutParams? = null
    private var dragStartRawX = 0f
    private var dragStartRawY = 0f
    private var dragStartX = 0
    private var dragStartY = 0
    private var isDragging = false
    private var userDismissedCallback: (() -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val fingerprint = overlayStateMachine.currentFingerprint()
                overlayStateMachine.markDismissed(
                    fingerprint = fingerprint,
                    suppressMillis = USER_DISMISS_SUPPRESS_MILLIS
                )
                resetOverlayView()
                runCatching { userDismissedCallback?.invoke() }
                    .onFailure { error ->
                        Log.e(TAG, "OVERLAY_DISMISS_CALLBACK_FAILURE", error)
                    }
                return true
            }
        }
    )

    override val isVisible: Boolean
        get() = runOverlayBoundary("isVisible", false) {
            composeView?.visibility == View.VISIBLE && composeView?.parent != null
        }

    override val visibleBounds: Rect?
        get() {
            return runOverlayBoundary("visibleBounds", null) {
                val view = composeView ?: return@runOverlayBoundary null
                if (view.visibility != View.VISIBLE || view.parent == null || view.width <= 0 || view.height <= 0) {
                    return@runOverlayBoundary null
                }
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                Rect(
                    location[0],
                    location[1],
                    location[0] + view.width,
                    location[1] + view.height
                )
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun showOverlay(data: TripData) {
        runOverlayBoundary("showOverlay", Unit) { call ->
            if (!AppSettings.isMonitoringEnabled(context)) return@runOverlayBoundary
            if (!isOverlayAllowed()) {
                blockOverlayOutsideDriverApp("showOverlay")
                return@runOverlayBoundary
            }
            showOverlayInternal(data, retryOnBadToken = true, call = call)
        }
    }

    override fun showDebugOverlay(state: AccessibilityDebugOverlayState) {
        runOverlayBoundary("showDebugOverlay", Unit) { call ->
            if (!isOverlayAllowed()) {
                blockOverlayOutsideDriverApp("showDebugOverlay")
                return@runOverlayBoundary
            }
            showDebugOverlayInternal(state, retryOnBadToken = true, forceApplicationOverlay = false, call = call)
        }
    }

    override fun setForegroundPackage(packageName: String?) {
        runOverlayBoundary("setForegroundPackage", Unit) {
            val decision = DriverAppPackagePolicy.classify(packageName)
            when (decision) {
                PackageDecision.DRIVER_APP -> {
                    foregroundPackageName = DriverAppPackagePolicy.normalize(packageName)
                    trustedDriverPackageName = foregroundPackageName
                    trustedDriverPackageSeenAtElapsed = SystemClock.elapsedRealtime()
                    criticalBlockPackageName = null
                    Log.w(
                        "OverlayManager",
                        "OVERLAY_ALLOWED_DRIVER_APP package=${DriverAppPackagePolicy.describe(packageName)}"
                    )
                }

                PackageDecision.ALLOWED_USER_APP -> {
                    foregroundPackageName = DriverAppPackagePolicy.normalize(packageName)
                    criticalBlockPackageName = null
                    Log.w(
                        "OverlayManager",
                        "OVERLAY_ALLOWED_COMMON_APP package=" +
                            DriverAppPackagePolicy.describe(packageName)
                    )
                }

                PackageDecision.BLOCKED_USER_APP -> {
                    foregroundPackageName = DriverAppPackagePolicy.normalize(packageName)
                    trustedDriverPackageName = null
                    criticalBlockPackageName = foregroundPackageName
                    blockOverlayForBlockedUserApp("foregroundChanged")
                }

                PackageDecision.OWN_APP -> {
                    Log.w(
                        "OverlayManager",
                        "OVERLAY_FOREGROUND_OWN_IGNORED package=${DriverAppPackagePolicy.describe(packageName)}"
                    )
                }

                PackageDecision.TRANSIENT_SYSTEM -> {
                    Log.w(
                        "OverlayManager",
                        "OVERLAY_FOREGROUND_TRANSIENT_IGNORED " +
                            "package=${DriverAppPackagePolicy.describe(packageName)}"
                    )
                }

                PackageDecision.UNKNOWN -> {
                    Log.w("OverlayManager", "OVERLAY_FOREGROUND_UNKNOWN_IGNORED")
                }
            }
        }
    }

    override fun setLatencyTrace(trace: OverlayLatencyTrace?) {
        runOverlayBoundary("setLatencyTrace", Unit) {
            pendingLatencyTrace = trace
        }
    }

    private fun showDebugOverlayInternal(
        state: AccessibilityDebugOverlayState,
        retryOnBadToken: Boolean,
        forceApplicationOverlay: Boolean,
        call: OverlayFailureContext
    ) {
        val windowType = getWindowType(forceApplicationOverlay)
        try {
            debugOverlayState.value = state

            if (debugComposeView == null) {
                debugLifecycleOwner = CustomLifecycleOwner()
                debugLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                val windowContext = createOverlayWindowContext(windowType)
                debugWindowContext = windowContext
                debugWindowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                createDebugComposeView(windowContext)
                debugLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }

            if (!isRequestCurrent(call)) { resetDebugOverlayView(); return }
            if (debugComposeView?.parent == null) debugComposeView?.visibility = View.GONE
            if (debugComposeView?.parent == null) {
                requireNotNull(debugWindowManager).addView(
                    debugComposeView,
                    getDebugLayoutParams(windowType)
                )
                if (BuildConfig.DEBUG) Log.i("OverlayManager", "Debug overlay shown")
            }
            if (!isRequestCurrent(call)) { resetDebugOverlayView(); return }
            debugComposeView?.visibility = View.VISIBLE
        } catch (e: WindowManager.BadTokenException) {
            resetDebugOverlayView()
            Log.w("OverlayManager", "OVERLAY_TOKEN_RECOVERING_DEBUG")
            if (retryOnBadToken) {
                retryAfterBadToken(
                    call = call,
                    primaryWindowType = windowType,
                    allowApplicationOverlayFallback = true,
                    retryAccessibilityOverlay = {
                        showDebugOverlayInternal(
                            state,
                            retryOnBadToken = false,
                            forceApplicationOverlay = false,
                            call = call
                        )
                    },
                    retryApplicationOverlay = {
                        showDebugOverlayInternal(
                            state,
                            retryOnBadToken = false,
                            forceApplicationOverlay = true,
                            call = call
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("OverlayManager", "Erro showDebugOverlay", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayInternal(data: TripData, retryOnBadToken: Boolean, call: OverlayFailureContext) {
        showOverlayInternal(data, retryOnBadToken, forceApplicationOverlay = false, call = call)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayInternal(
        data: TripData,
        retryOnBadToken: Boolean,
        forceApplicationOverlay: Boolean,
        call: OverlayFailureContext
    ) {
        val windowType = getWindowType(forceApplicationOverlay)
        val newFingerprint = data.overlayFingerprint()
        val oldFingerprint = overlayStateMachine.currentFingerprint()
        val transition = overlayStateMachine.showRequested(newFingerprint)
        val requestedAt = System.currentTimeMillis()
        val requestedAtElapsed = android.os.SystemClock.elapsedRealtime()
        val latencyTrace = pendingLatencyTrace?.withTripData(data)

        ReadingPipelineRuntime.markOverlayStatus(ReadingPipelineRuntime.OverlayStatus.REQUESTED)
        Log.w("OverlayManager", "OVERLAY_SHOW_REQUESTED fingerprint=$newFingerprint")
        Log.w("OverlayManager", "CALCMOT_OVERLAY_REQUEST fingerprint=$newFingerprint")
        latencyTrace?.mark(OverlayLatencyTrace.Stage.T10_OVERLAY_ADD_OR_UPDATE_START)

        val hasAttachedOverlay = composeView?.parent != null
        if (
            transition == OverlayTransition.AttachOrReplace &&
            !hasAttachedOverlay &&
            (composeView != null || debugComposeView != null)
        ) {
            Log.w("OverlayManager", "OVERLAY_STALE_VIEW_REMOVED_BEFORE_NEW old=$oldFingerprint new=$newFingerprint")
            resetOverlayView()
            resetDebugOverlayView()
        } else if (transition == OverlayTransition.AttachOrReplace && hasAttachedOverlay) {
            Log.w("OverlayManager", "OVERLAY_REPLACED_IN_PLACE old=$oldFingerprint new=$newFingerprint")
            resetDebugOverlayView()
        }

        try {
            val calculationStartedAt = android.os.SystemClock.elapsedRealtime()
            overlayThemeState.value = AppSettings.getOverlayTheme(context)
            tripDataState.value = data
            profitabilityState.value = ProfitabilityCalculator.calculate(
                tripData = data,
                settings = AppSettings.getProfitabilitySettings(context)
            )
            financialImpactState.value = if (AppSettings.isFinancialImpactEnabled(context)) {
                FinancialImpactCalculator.calculate(
                    tripData = data,
                    goal = AppSettings.getDriverGoal(context)
                )
            } else {
                null
            }
            latencyTrace?.metric(
                name = "overlay.localCalculation",
                durationMs = android.os.SystemClock.elapsedRealtime() - calculationStartedAt,
                details = "financialImpact=${financialImpactState.value != null}"
            )

            if (composeView == null) {
                val createViewStartedAt = android.os.SystemClock.elapsedRealtime()
                lifecycleOwner = CustomLifecycleOwner()
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                val windowContext = createOverlayWindowContext(windowType)
                overlayWindowContext = windowContext
                overlayWindowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                createComposeView(windowContext)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                latencyTrace?.metric(
                    name = "overlay.createComposeView",
                    durationMs = android.os.SystemClock.elapsedRealtime() - createViewStartedAt,
                    details = "windowType=$windowType"
                )
            }

            if (!isRequestCurrent(call) || !AppSettings.isMonitoringEnabled(context)) {
                resetOverlayView()
                overlayStateMachine.markHidden()
                return
            }
            windowOwnership.attach(call.pending)
            composeView?.registerVisibleTelemetry(
                fingerprint = newFingerprint,
                requestedAt = requestedAt,
                requestedAtElapsed = requestedAtElapsed,
                trace = latencyTrace,
                call = call
            )
            // Attach hidden; a late native addView must not reveal an expired request.
            if (composeView?.parent == null) composeView?.visibility = View.GONE

            if (composeView?.parent == null) {
                val params = getLayoutParams(windowType)
                currentLayoutParams = params
                val addViewStartedAt = android.os.SystemClock.elapsedRealtime()
                requireNotNull(overlayWindowManager).addView(composeView, params)
                latencyTrace?.metric(
                    name = "overlay.addView",
                    durationMs = android.os.SystemClock.elapsedRealtime() - addViewStartedAt,
                    details = "transition=$transition"
                )
                Log.w("OverlayManager", "OVERLAY_VIEW_ADDED fingerprint=$newFingerprint")
                overlayStateMachine.markShown(newFingerprint)
                if (transition == OverlayTransition.AttachOrReplace && oldFingerprint != null) {
                    Log.w("OverlayManager", "OVERLAY_REPLACED_OLD_FINGERPRINT old=$oldFingerprint new=$newFingerprint")
                }
            } else if (transition == OverlayTransition.UpdateInPlace) {
                latencyTrace?.metric(
                    name = "overlay.updateInPlace",
                    durationMs = 0L,
                    details = "transition=$transition"
                )
                if (BuildConfig.DEBUG) {
                    Log.d("OverlayManager", "Overlay updated in-place: $newFingerprint")
                }
            } else if (transition == OverlayTransition.AttachOrReplace) {
                overlayStateMachine.markShown(newFingerprint)
            }
            if (!isRequestCurrent(call) || !AppSettings.isMonitoringEnabled(context)) {
                resetOverlayView()
                overlayStateMachine.markHidden()
                return
            }
            composeView?.visibility = View.VISIBLE
            latencyTrace?.mark(OverlayLatencyTrace.Stage.T11_OVERLAY_ADD_OR_UPDATE_END)
            Log.w("OverlayManager", "CALCMOT_OVERLAY_WINDOW fingerprint=$newFingerprint")
            ReadingPipelineRuntime.markOverlayStatus(ReadingPipelineRuntime.OverlayStatus.WINDOW_ADDED)
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_SHOWN)
            TelemetryProvider.analytics.track(
                AnalyticsEvents.OVERLAY_SHOWN,
                mapOf(
                    AnalyticsParams.PLATFORM to currentTelemetryPlatform(),
                    AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_OVERLAY,
                    AnalyticsParams.CLASSIFICATION to (
                        profitabilityState.value?.quality?.name?.lowercase() ?: "unknown"
                    )
                ) + AnalyticsBuckets.from(data)
            )
            if (BuildConfig.DEBUG) {
                Log.i("OverlayManager", "Overlay shown: $data")
            }
        } catch (e: WindowManager.BadTokenException) {
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_ERROR)
            trackOverlayFailure(AnalyticsValues.REASON_BAD_TOKEN, e, reportCrash = !retryOnBadToken)
            overlayStateMachine.markTokenRecovering(newFingerprint)
            resetOverlayView()
            Log.w("OverlayManager", "OVERLAY_TOKEN_RECOVERING fingerprint=$newFingerprint")
            if (retryOnBadToken) {
                retryAfterBadToken(
                    call = call,
                    primaryWindowType = windowType,
                    allowApplicationOverlayFallback = true,
                    retryAccessibilityOverlay = {
                        showOverlayInternal(
                            data,
                            retryOnBadToken = false,
                            forceApplicationOverlay = false,
                            call = call
                        )
                    },
                    retryApplicationOverlay = {
                        showOverlayInternal(
                            data,
                            retryOnBadToken = false,
                            forceApplicationOverlay = true,
                            call = call
                        )
                    }
                )
            } else {
                latencyTrace?.close(OverlayLatencyTrace.EndReason.BAD_TOKEN)
            }
        } catch (e: Exception) {
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_ERROR)
            trackOverlayFailure(AnalyticsValues.REASON_EXCEPTION, e, reportCrash = true)
            latencyTrace?.close(OverlayLatencyTrace.EndReason.CARD_GONE)
            resetOverlayView()
            overlayStateMachine.markHidden()
            ReadingPipelineRuntime.markOverlayStatus(ReadingPipelineRuntime.OverlayStatus.FAILED)
            Log.e("OverlayManager", "Erro showOverlay: ", e)
        }
    }

    private fun isOverlayAllowed(): Boolean {
        if (criticalBlockPackageName != null) return false
        val foregroundDecision = DriverAppPackagePolicy.classify(foregroundPackageName)
        if (foregroundDecision != PackageDecision.DRIVER_APP &&
            foregroundDecision != PackageDecision.ALLOWED_USER_APP
        ) {
            return false
        }
        val trustedPackage = trustedDriverPackageName ?: return false
        if (!DriverAppPackagePolicy.isDriverPackage(trustedPackage)) return false
        val elapsedSinceTrustedDriver = SystemClock.elapsedRealtime() - trustedDriverPackageSeenAtElapsed
        return elapsedSinceTrustedDriver in 0..TRUSTED_DRIVER_GRACE_MS
    }

    private fun blockOverlayOutsideDriverApp(reason: String) {
        val packageName = DriverAppPackagePolicy.describe(foregroundPackageName)
        Log.w(
            "OverlayManager",
            "OVERLAY_REQUEST_DROPPED_NO_TRUSTED_DRIVER package=$packageName reason=$reason"
        )
        pendingLatencyTrace?.close(OverlayLatencyTrace.EndReason.CARD_GONE)
        pendingLatencyTrace = null
    }

    private fun blockOverlayForBlockedUserApp(reason: String) {
        val packageName = DriverAppPackagePolicy.describe(criticalBlockPackageName ?: foregroundPackageName)
        Log.w(
            "OverlayManager",
            "OVERLAY_BLOCKED_USER_APP package=$packageName reason=$reason"
        )
        resetOverlayView()
        resetDebugOverlayView()
        overlayStateMachine.markHidden()
        pendingLatencyTrace?.close(OverlayLatencyTrace.EndReason.SAFE_MODE_BLOCKED_USER_APP)
        pendingLatencyTrace = null
    }

    override fun hideOverlay() {
        runOverlayBoundary("hideOverlay", Unit) {
            resetOverlayView()
            overlayStateMachine.markHidden()
        }
    }

    override fun expireOverlay(fingerprint: String?) {
        runOverlayBoundary("expireOverlay", Unit) {
            resetOverlayView()
            overlayStateMachine.markExpired(fingerprint ?: overlayStateMachine.currentFingerprint())
        }
    }

    override fun hideDebugOverlay() {
        runOverlayBoundary("hideDebugOverlay", Unit) {
            debugComposeView?.visibility = View.GONE
        }
    }

    override fun close() {
        closed.set(true)
        removeOverlay()
        userDismissedCallback = null
    }

    override fun removeOverlay() {
        runOverlayBoundary("removeOverlay", Unit) {
            resetOverlayView()
            resetDebugOverlayView()
            overlayStateMachine.markHidden()
        }
    }

    override fun removeOverlayWindowsForScan(): Boolean {
        return runOverlayBoundary("removeOverlayWindowsForScan", false) {
            val hadOverlayWindow = composeView?.parent != null || debugComposeView?.parent != null
            if (hadOverlayWindow) {
                resetOverlayView()
                resetDebugOverlayView()
                overlayStateMachine.markHidden()
            }
            hadOverlayWindow
        }
    }

    override fun setOnUserDismissed(callback: (() -> Unit)?) {
        userDismissedCallback = callback
    }

    private fun resetOverlayView() {
        windowOwnership.clear()
        ReadingPipelineRuntime.markOverlayStatus(ReadingPipelineRuntime.OverlayStatus.ABSENT)
        val viewToRemove = composeView
        val ownerToDestroy = lifecycleOwner
        val manager = overlayWindowManager ?: baseWindowManager

        composeView = null
        lifecycleOwner = null
        overlayWindowContext = null
        overlayWindowManager = null
        currentLayoutParams = null
        financialImpactState.value = null

        if (viewToRemove != null || ownerToDestroy != null) {
            viewToRemove?.let {
                if (it.parent != null) {
                    removeWindowView(manager, it, "overlay")
                }
            }
            destroyLifecycleOwner(ownerToDestroy, "overlay")
        }
    }

    private fun resetDebugOverlayView() {
        val viewToRemove = debugComposeView
        val ownerToDestroy = debugLifecycleOwner
        val manager = debugWindowManager ?: baseWindowManager

        debugComposeView = null
        debugLifecycleOwner = null
        debugWindowContext = null
        debugWindowManager = null

        if (viewToRemove != null || ownerToDestroy != null) {
            viewToRemove?.let {
                if (it.parent != null) {
                    removeWindowView(manager, it, "debug-overlay")
                }
            }
            destroyLifecycleOwner(ownerToDestroy, "debug-overlay")
        }
    }

    private fun destroyLifecycleOwner(owner: CustomLifecycleOwner?, label: String) {
        runCatching {
            owner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }.onFailure { error ->
            Log.e(TAG, "OVERLAY_LIFECYCLE_DESTROY_FAILURE label=$label", error)
        }
    }

    private fun removeWindowView(manager: WindowManager, view: View, label: String) {
        runCatching {
            manager.removeViewImmediate(view)
        }.onFailure { error ->
            Log.w("OverlayManager", "Failed to remove $label window immediately", error)
            runCatching {
                manager.removeView(view)
            }.onFailure { fallbackError ->
                Log.e("OverlayManager", "Failed to remove $label window", fallbackError)
            }
        }
    }

    private class ObsoleteOverlayOperation : IllegalStateException("Overlay operation superseded")

    private data class OverlayFailureContext(
        val operation: String,
        val callerThread: String,
        val generation: Long,
        val pending: PendingOverlayOperation,
        var failureKind: String = "operation_exception"
    )

    private fun <T> runOnMainBlocking(failureContext: OverlayFailureContext, generation: Long, block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (!failureContext.pending.tryStart(SystemClock.elapsedRealtime(), true)) throw ObsoleteOverlayOperation()
            return block()
        }

        val latch = CountDownLatch(1)
        val result = AtomicReference<Result<T>>()
        val pending = failureContext.pending
        val runnable = Runnable {
            val contextCurrent = when {
                isCleanup(failureContext.operation) -> operationGeneration.get() == generation
                failureContext.operation in setOf("show_overlay", "show_debug_overlay") -> !closed.get() && operationGeneration.get() == generation
                else -> !closed.get()
            }
            if (!contextCurrent) {
                result.set(Result.failure(ObsoleteOverlayOperation()))
            } else if (!pending.tryStart(SystemClock.elapsedRealtime(), true) && !isCleanup(failureContext.operation)) {
                failureContext.failureKind = "main_wait_timeout"
                result.set(Result.failure(IllegalStateException("Overlay deadline expired before execution")))
            } else {
                result.set(runCatching(block))
            }
            latch.countDown()
        }
        val posted = mainHandler.post(runnable)
        check(posted) {
            failureContext.failureKind = "main_post_rejected"
            "Main looper rejected overlay operation"
        }
        val completed = try {
            latch.await(MAIN_THREAD_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (error: InterruptedException) {
            pending.cancel()
            if (!isCleanup(failureContext.operation)) mainHandler.removeCallbacks(runnable)
            failureContext.failureKind = "main_wait_interrupted"
            Thread.currentThread().interrupt()
            throw error
        }
        check(completed) {
            pending.cancel()
            if (!isCleanup(failureContext.operation)) mainHandler.removeCallbacks(runnable)
            failureContext.failureKind = "main_wait_timeout"
            "Timed out waiting for overlay operation on main thread"
        }
        return checkNotNull(result.get()) {
            failureContext.failureKind = "main_result_missing"
            "Overlay operation completed without a result"
        }.getOrThrow()
    }

    private fun <T> runOverlayBoundary(
        operation: String,
        fallback: T,
        block: (OverlayFailureContext) -> T
    ): T {
        if (closed.get() && operation != "removeOverlay") return fallback
        val generation = if (operation in setOf(
                "showOverlay", "hideOverlay", "expireOverlay", "removeOverlay",
                "removeOverlayWindowsForScan"
            )) operationGeneration.incrementAndGet() else operationGeneration.get()
        val failureContext = OverlayFailureContext(
            operation = when (operation) {
                "isVisible" -> "is_visible"
                "visibleBounds" -> "visible_bounds"
                "showOverlay" -> "show_overlay"
                "showDebugOverlay" -> "show_debug_overlay"
                "setForegroundPackage" -> "set_foreground_package"
                "setLatencyTrace" -> "set_latency_trace"
                "hideOverlay" -> "hide_overlay"
                "expireOverlay" -> "expire_overlay"
                "hideDebugOverlay" -> "hide_debug_overlay"
                "removeOverlay" -> "remove_overlay"
                "removeOverlayWindowsForScan" -> "remove_overlay_windows_for_scan"
                else -> "unknown"
            },
            callerThread = if (Looper.myLooper() == Looper.getMainLooper()) "main" else "background",
            generation = generation,
            pending = PendingOverlayOperation(SystemClock.elapsedRealtime() + MAIN_THREAD_OPERATION_TIMEOUT_MS)
        )
        return runCatching {
            runOnMainBlocking(failureContext, generation) { block(failureContext) }
        }.getOrElse { error ->
            if (error is ObsoleteOverlayOperation) return@getOrElse fallback
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_ERROR)
            ReadingPipelineRuntime.markOverlayStatus(ReadingPipelineRuntime.OverlayStatus.FAILED)
            trackOverlayFailure(
                AnalyticsValues.REASON_BOUNDARY_FAILURE,
                error,
                reportCrash = true,
                operation = failureContext.operation,
                failureKind = failureContext.failureKind,
                callerThread = failureContext.callerThread
            )
            Log.e(
                TAG,
                "OVERLAY_BOUNDARY_FAILURE overlay_operation=${failureContext.operation} " +
                    "overlay_failure_kind=${failureContext.failureKind} " +
                    "overlay_caller_thread=${failureContext.callerThread}",
                error
            )
            fallback
        }
    }

    private fun trackOverlayFailure(
        reason: String,
        error: Throwable,
        reportCrash: Boolean,
        operation: String = "unknown",
        failureKind: String = "not_applicable",
        callerThread: String = "unknown"
    ) {
        val params = mapOf(
            AnalyticsParams.PLATFORM to currentTelemetryPlatform(),
            AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_OVERLAY,
            AnalyticsParams.REASON to reason,
            AnalyticsParams.PIPELINE_STATE to "failed",
            AnalyticsParams.OVERLAY_OPERATION to operation,
            AnalyticsParams.OVERLAY_FAILURE_KIND to failureKind,
            AnalyticsParams.OVERLAY_CALLER_THREAD to callerThread
        )
        TelemetryProvider.analytics.track(AnalyticsEvents.OVERLAY_FAILED, params)
        if (reportCrash) {
            TelemetryProvider.crashReporter.recordNonFatal(error, reason, params)
        }
    }

    private fun currentTelemetryPlatform(): String {
        return DriverApp.fromPackage(foregroundPackageName).id
    }

    private fun createComposeView(windowContext: Context) {
        composeView = ComposeView(windowContext).apply {
            val owner = lifecycleOwner!!
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setOnTouchListener { _, event ->
                handleTouch(event)
            }

            setContent {
                MetricaTheme {
                    tripDataState.value?.let {
                        OverlayView(
                            tripData = it,
                            profitability = profitabilityState.value,
                            financialImpact = financialImpactState.value,
                            theme = overlayThemeState.value
                        )
                    }
                }
            }
        }
    }

    private fun ComposeView.registerVisibleTelemetry(
        fingerprint: String,
        requestedAt: Long,
        requestedAtElapsed: Long,
        trace: OverlayLatencyTrace?,
        call: OverlayFailureContext
    ) {
        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                if (!windowOwnership.owns(call.pending) || composeView !== this@registerVisibleTelemetry ||
                    overlayStateMachine.currentFingerprint() != fingerprint) return true
                if (!isRequestCurrent(call) || !AppSettings.isMonitoringEnabled(context)) {
                    visibility = View.GONE
                    mainHandler.post {
                        if (windowOwnership.owns(call.pending) && operationGeneration.get() == call.generation &&
                            composeView === this@registerVisibleTelemetry &&
                            overlayStateMachine.currentFingerprint() == fingerprint) {
                            resetOverlayView()
                            overlayStateMachine.markHidden()
                        }
                    }
                    return false
                }
                ReadingPipelineRuntime.markOverlayStatus(ReadingPipelineRuntime.OverlayStatus.PREDRAW_CONFIRMED)
                val drawnAt = System.currentTimeMillis()
                val drawnAtElapsed = android.os.SystemClock.elapsedRealtime()
                Log.w("OverlayManager", "OVERLAY_FIRST_DRAWN fingerprint=$fingerprint")
                Log.w(
                    "OverlayManager",
                    "CALCMOT_OVERLAY_DRAW fingerprint=$fingerprint firstDrawMs=${drawnAtElapsed - requestedAtElapsed}"
                )
                trace?.mark(OverlayLatencyTrace.Stage.T12_OVERLAY_FIRST_DRAW)
                val latency = drawnAt - requestedAt
                Log.w("OverlayManager", "OVERLAY_VISIBLE_TO_USER fingerprint=$fingerprint visibleLatencyMs=$latency")
                trace?.mark(OverlayLatencyTrace.Stage.T13_OVERLAY_VISIBLE_TO_USER)
                return true
            }
        })
        mainHandler.postDelayed(
            { trace?.close(OverlayLatencyTrace.EndReason.PREDRAW_TIMEOUT) },
            OVERLAY_PREDRAW_TIMEOUT_MS
        )
    }

    private fun createDebugComposeView(windowContext: Context) {
        debugComposeView = ComposeView(windowContext).apply {
            val owner = debugLifecycleOwner!!
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                MetricaTheme {
                    debugOverlayState.value?.let {
                        DebugOverlayView(state = it)
                    }
                }
            }
        }
    }

    private fun getLayoutParams(windowType: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            val customPosition = AppSettings.getCustomOverlayPosition(context)
            if (customPosition != null) {
                gravity = Gravity.TOP or Gravity.START
                x = customPosition.x
                y = customPosition.y
            } else {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(AppSettings.getOverlayPosition(context).offsetDp)
            }
        }
    }

    private fun getDebugLayoutParams(windowType: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(8)
            y = dpToPx(40)
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        val params = currentLayoutParams ?: return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartRawX = event.rawX
                dragStartRawY = event.rawY
                val location = IntArray(2)
                composeView?.getLocationOnScreen(location)
                dragStartX = location[0]
                dragStartY = location[1]
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - dragStartRawX
                val dy = event.rawY - dragStartRawY
                if (!isDragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                    isDragging = true
                }
                if (isDragging) {
                    params.gravity = Gravity.TOP or Gravity.START
                    params.x = (dragStartX + dx).roundToInt().coerceAtLeast(0)
                    params.y = (dragStartY + dy).roundToInt().coerceAtLeast(0)
                    runCatching { (overlayWindowManager ?: baseWindowManager).updateViewLayout(composeView, params) }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    AppSettings.setCustomOverlayPosition(
                        context,
                        OverlayCustomPosition(x = params.x, y = params.y)
                    )
                }
                isDragging = false
            }
        }

        return true
    }

    private fun dpToPx(valueDp: Int): Int {
        return (valueDp * context.resources.displayMetrics.density).roundToInt()
    }

    private fun createOverlayWindowContext(windowType: Int): Context {
        if (context is AccessibilityService && windowType == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) {
            return context
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            val display = baseWindowManager.defaultDisplay
            context.createDisplayContext(display).createWindowContext(windowType, null)
        } else {
            context
        }
    }

    private fun isCleanup(operation: String): Boolean = operation in setOf(
        "hide_overlay", "expire_overlay", "remove_overlay", "remove_overlay_windows_for_scan"
    )

    private fun isRequestCurrent(call: OverlayFailureContext): Boolean =
        call.pending.isValid(SystemClock.elapsedRealtime(), !closed.get() && operationGeneration.get() == call.generation)

    private fun retryAfterBadToken(
        call: OverlayFailureContext,
        primaryWindowType: Int,
        allowApplicationOverlayFallback: Boolean,
        retryAccessibilityOverlay: () -> Unit,
        retryApplicationOverlay: () -> Unit
    ) {
        fun retryIfCurrent(retry: () -> Unit) {
            if (isRequestCurrent(call) &&
                AppSettings.isMonitoringEnabled(context) && isOverlayAllowed()) {
                retry()
            } else {
                Log.i(TAG, "OVERLAY_OBSOLETE_RETRY_DROPPED")
            }
        }
        if (
            BuildConfig.DEBUG &&
            allowApplicationOverlayFallback &&
            primaryWindowType == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY &&
            canUseApplicationOverlayFallback()
        ) {
            preferDebugApplicationOverlay = true
            Log.w("OverlayManager", "Retrying overlay immediately with application overlay fallback")
            mainHandler.post { retryIfCurrent(retryApplicationOverlay) }
        } else {
            mainHandler.postDelayed({ retryIfCurrent(retryAccessibilityOverlay) }, BAD_TOKEN_RETRY_DELAY_MS)
        }
    }

    private fun canUseApplicationOverlayFallback(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    private fun getWindowType(forceApplicationOverlay: Boolean): Int {
        if (!forceApplicationOverlay && context is AccessibilityService) {
            return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

        if (forceApplicationOverlay || canUseApplicationOverlayFallback()) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
        }

        if (context is AccessibilityService) {
            return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

        return WindowManager.LayoutParams.TYPE_APPLICATION
    }

    private companion object {
        const val TAG = "OverlayManager"
        const val BAD_TOKEN_RETRY_DELAY_MS = 150L
        const val MAIN_THREAD_OPERATION_TIMEOUT_MS = 2_000L
        const val OVERLAY_PREDRAW_TIMEOUT_MS = 1_500L
        const val USER_DISMISS_SUPPRESS_MILLIS = 10_000L
        const val TRUSTED_DRIVER_GRACE_MS = 5_000L
        var preferDebugApplicationOverlay = false
    }
}

private class CustomLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val _viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
