package br.com.calcmot.overlay

import android.annotation.SuppressLint
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Build
import android.os.Looper
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
import br.com.calcmot.OverlayCustomPosition
import br.com.calcmot.accessibility.AccessibilityDebugOverlayState
import br.com.calcmot.model.FinancialImpactCalculator
import br.com.calcmot.model.OfferFinancialImpact
import br.com.calcmot.model.ProfitabilityCalculator
import br.com.calcmot.model.ProfitabilityResult
import br.com.calcmot.model.TripData
import br.com.calcmot.processor.overlayFingerprint
import br.com.calcmot.ui.design.overlay.CalcMotOverlayContainer
import br.com.calcmot.ui.design.overlay.FinancialImpactBlockDS
import br.com.calcmot.ui.design.overlay.MetricRow
import br.com.calcmot.ui.design.overlay.OfferQualityBadge
import br.com.calcmot.ui.design.overlay.OverlayDragHandle
import br.com.calcmot.ui.design.theme.CalcMotTheme
import android.util.Log
import kotlin.math.roundToInt
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

open class OverlayManager(private val context: Context) : IOverlayManager {

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
    private val debugOverlayState = mutableStateOf<AccessibilityDebugOverlayState?>(null)
    private val overlayStateMachine = OverlayStateMachine()
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
                userDismissedCallback?.invoke()
                return true
            }
        }
    )

    override val isVisible: Boolean
        get() = runOnMainBlocking {
            composeView?.visibility == View.VISIBLE && composeView?.parent != null
        }

    override val visibleBounds: Rect?
        get() {
            return runOnMainBlocking {
                val view = composeView ?: return@runOnMainBlocking null
                if (view.visibility != View.VISIBLE || view.parent == null || view.width <= 0 || view.height <= 0) {
                    return@runOnMainBlocking null
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
        runOnMainBlocking {
            showOverlayInternal(data, retryOnBadToken = true)
        }
    }

    override fun showDebugOverlay(state: AccessibilityDebugOverlayState) {
        runOnMainBlocking {
            showDebugOverlayInternal(state, retryOnBadToken = true, forceApplicationOverlay = false)
        }
    }

    private fun showDebugOverlayInternal(
        state: AccessibilityDebugOverlayState,
        retryOnBadToken: Boolean,
        forceApplicationOverlay: Boolean
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

            debugComposeView?.visibility = View.VISIBLE
            if (debugComposeView?.parent == null) {
                requireNotNull(debugWindowManager).addView(
                    debugComposeView,
                    getDebugLayoutParams(windowType)
                )
                if (BuildConfig.DEBUG) Log.i("OverlayManager", "Debug overlay shown")
            }
        } catch (e: WindowManager.BadTokenException) {
            resetDebugOverlayView()
            Log.w("OverlayManager", "OVERLAY_TOKEN_RECOVERING_DEBUG")
            if (retryOnBadToken) {
                retryAfterBadToken(
                    primaryWindowType = windowType,
                    allowApplicationOverlayFallback = true,
                    retryAccessibilityOverlay = {
                        showDebugOverlayInternal(
                            state,
                            retryOnBadToken = false,
                            forceApplicationOverlay = false
                        )
                    },
                    retryApplicationOverlay = {
                        showDebugOverlayInternal(
                            state,
                            retryOnBadToken = false,
                            forceApplicationOverlay = true
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("OverlayManager", "Erro showDebugOverlay", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayInternal(data: TripData, retryOnBadToken: Boolean) {
        showOverlayInternal(data, retryOnBadToken, forceApplicationOverlay = false)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayInternal(
        data: TripData,
        retryOnBadToken: Boolean,
        forceApplicationOverlay: Boolean
    ) {
        val windowType = getWindowType(forceApplicationOverlay)
        val newFingerprint = data.overlayFingerprint()
        val oldFingerprint = overlayStateMachine.currentFingerprint()
        val transition = overlayStateMachine.showRequested(newFingerprint)
        val requestedAt = System.currentTimeMillis()

        Log.w("OverlayManager", "OVERLAY_SHOW_REQUESTED fingerprint=$newFingerprint")

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

            if (composeView == null) {
                lifecycleOwner = CustomLifecycleOwner()
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                val windowContext = createOverlayWindowContext(windowType)
                overlayWindowContext = windowContext
                overlayWindowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                createComposeView(windowContext)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }

            composeView?.registerVisibleTelemetry(newFingerprint, requestedAt)
            composeView?.visibility = View.VISIBLE

            if (composeView?.parent == null) {
                val params = getLayoutParams(windowType)
                currentLayoutParams = params
                requireNotNull(overlayWindowManager).addView(composeView, params)
                Log.w("OverlayManager", "OVERLAY_VIEW_ADDED fingerprint=$newFingerprint")
                overlayStateMachine.markShown(newFingerprint)
                if (transition == OverlayTransition.AttachOrReplace && oldFingerprint != null) {
                    Log.w("OverlayManager", "OVERLAY_REPLACED_OLD_FINGERPRINT old=$oldFingerprint new=$newFingerprint")
                }
            } else if (transition == OverlayTransition.UpdateInPlace) {
                if (BuildConfig.DEBUG) {
                    Log.d("OverlayManager", "Overlay updated in-place: $newFingerprint")
                }
            } else if (transition == OverlayTransition.AttachOrReplace) {
                overlayStateMachine.markShown(newFingerprint)
            }
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_SHOWN)
            if (BuildConfig.DEBUG) {
                Log.i("OverlayManager", "Overlay shown: $data")
            }
        } catch (e: WindowManager.BadTokenException) {
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_ERROR)
            overlayStateMachine.markTokenRecovering(newFingerprint)
            resetOverlayView()
            Log.w("OverlayManager", "OVERLAY_TOKEN_RECOVERING fingerprint=$newFingerprint")
            if (retryOnBadToken) {
                retryAfterBadToken(
                    primaryWindowType = windowType,
                    allowApplicationOverlayFallback = true,
                    retryAccessibilityOverlay = {
                        showOverlayInternal(
                            data,
                            retryOnBadToken = false,
                            forceApplicationOverlay = false
                        )
                    },
                    retryApplicationOverlay = {
                        showOverlayInternal(
                            data,
                            retryOnBadToken = false,
                            forceApplicationOverlay = true
                        )
                    }
                )
            }
        } catch (e: Exception) {
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_ERROR)
            Log.e("OverlayManager", "Erro showOverlay: ", e)
        }
    }

    override fun hideOverlay() {
        runOnMainBlocking {
            resetOverlayView()
            overlayStateMachine.markHidden()
        }
    }

    override fun expireOverlay(fingerprint: String?) {
        runOnMainBlocking {
            resetOverlayView()
            overlayStateMachine.markExpired(fingerprint ?: overlayStateMachine.currentFingerprint())
        }
    }

    override fun hideDebugOverlay() {
        runOnMainBlocking {
            debugComposeView?.visibility = View.GONE
        }
    }

    override fun removeOverlay() {
        runOnMainBlocking {
            resetOverlayView()
            resetDebugOverlayView()
            overlayStateMachine.markHidden()
        }
    }

    override fun removeOverlayWindowsForScan(): Boolean {
        return runOnMainBlocking {
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
            ownerToDestroy?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
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
            ownerToDestroy?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
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

    private fun <T> runOnMainBlocking(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }

        val latch = CountDownLatch(1)
        val result = AtomicReference<Result<T>>()
        mainHandler.post {
            result.set(runCatching(block))
            latch.countDown()
        }
        latch.await()
        return result.get().getOrThrow()
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
                CalcMotTheme {
                    tripDataState.value?.let {
                        OverlayView(
                            tripData = it,
                            profitability = profitabilityState.value,
                            financialImpact = financialImpactState.value
                        )
                    }
                }
            }
        }
    }

    private fun ComposeView.registerVisibleTelemetry(fingerprint: String, requestedAt: Long) {
        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)
                val drawnAt = System.currentTimeMillis()
                Log.w("OverlayManager", "OVERLAY_FIRST_DRAWN fingerprint=$fingerprint")
                val latency = drawnAt - requestedAt
                Log.w("OverlayManager", "OVERLAY_VISIBLE_TO_USER fingerprint=$fingerprint visibleLatencyMs=$latency")
                return true
            }
        })
    }

    private fun createDebugComposeView(windowContext: Context) {
        debugComposeView = ComposeView(windowContext).apply {
            val owner = debugLifecycleOwner!!
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                CalcMotTheme {
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

    private fun retryAfterBadToken(
        primaryWindowType: Int,
        allowApplicationOverlayFallback: Boolean,
        retryAccessibilityOverlay: () -> Unit,
        retryApplicationOverlay: () -> Unit
    ) {
        if (
            BuildConfig.DEBUG &&
            allowApplicationOverlayFallback &&
            primaryWindowType == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY &&
            canUseApplicationOverlayFallback()
        ) {
            preferDebugApplicationOverlay = true
            Log.w("OverlayManager", "Retrying overlay immediately with application overlay fallback")
            mainHandler.post(retryApplicationOverlay)
        } else {
            mainHandler.postDelayed(retryAccessibilityOverlay, BAD_TOKEN_RETRY_DELAY_MS)
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
        const val BAD_TOKEN_RETRY_DELAY_MS = 150L
        const val USER_DISMISS_SUPPRESS_MILLIS = 10_000L
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
