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
import br.com.calcmot.model.ProfitabilityCalculator
import br.com.calcmot.model.ProfitabilityResult
import br.com.calcmot.model.TripData
import br.com.calcmot.processor.overlayFingerprint
import br.com.calcmot.ui.theme.MetricaTheme
import android.util.Log
import kotlin.math.roundToInt

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
                composeView?.visibility = View.GONE
                overlayStateMachine.markDismissed(
                    fingerprint = fingerprint,
                    suppressMillis = USER_DISMISS_SUPPRESS_MILLIS
                )
                userDismissedCallback?.invoke()
                return true
            }
        }
    )

    override val isVisible: Boolean
        get() = composeView?.visibility == View.VISIBLE && composeView?.parent != null

    override val visibleBounds: Rect?
        get() {
            val view = composeView ?: return null
            if (!isVisible || view.width <= 0 || view.height <= 0) return null
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            return Rect(
                location[0],
                location[1],
                location[0] + view.width,
                location[1] + view.height
            )
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun showOverlay(data: TripData) {
        showOverlayInternal(data, retryOnBadToken = true)
    }

    override fun showDebugOverlay(state: AccessibilityDebugOverlayState) {
        showDebugOverlayInternal(state, retryOnBadToken = true, forceApplicationOverlay = false)
    }

    private fun showDebugOverlayInternal(
        state: AccessibilityDebugOverlayState,
        retryOnBadToken: Boolean,
        forceApplicationOverlay: Boolean
    ) {
        val windowType = getWindowType(forceApplicationOverlay || preferDebugApplicationOverlay)
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
            Log.e("OverlayManager", "Erro showDebugOverlay: token invalido", e)
            if (retryOnBadToken) {
                retryAfterBadToken(
                    primaryWindowType = windowType,
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
        val windowType = getWindowType(forceApplicationOverlay || preferDebugApplicationOverlay)
        val fingerprint = data.overlayFingerprint()
        val transition = overlayStateMachine.showRequested(fingerprint)
        try {
            tripDataState.value = data
            profitabilityState.value = ProfitabilityCalculator.calculate(
                tripData = data,
                settings = AppSettings.getProfitabilitySettings(context)
            )

            if (composeView == null) {
                lifecycleOwner = CustomLifecycleOwner()
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                val windowContext = createOverlayWindowContext(windowType)
                overlayWindowContext = windowContext
                overlayWindowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                createComposeView(windowContext)
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }

            composeView?.visibility = View.VISIBLE

            if (composeView?.parent == null) {
                val params = getLayoutParams(windowType)
                currentLayoutParams = params
                requireNotNull(overlayWindowManager).addView(composeView, params)
                overlayStateMachine.markShown(fingerprint)
            } else if (transition == OverlayTransition.UpdateInPlace) {
                if (BuildConfig.DEBUG) {
                    Log.d("OverlayManager", "Overlay updated in-place: $fingerprint")
                }
            }
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_SHOWN)
            if (BuildConfig.DEBUG) {
                Log.i("OverlayManager", "Overlay shown: $data")
            }
        } catch (e: WindowManager.BadTokenException) {
            AppDiagnostics.recordStage(context, AppDiagnostics.Stage.OVERLAY_ERROR)
            overlayStateMachine.markTokenRecovering(fingerprint)
            resetOverlayView()
            Log.e("OverlayManager", "Erro showOverlay: token invalido", e)
            if (retryOnBadToken) {
                retryAfterBadToken(
                    primaryWindowType = windowType,
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
        composeView?.visibility = View.GONE
        overlayStateMachine.markHidden()
    }

    override fun expireOverlay(fingerprint: String?) {
        composeView?.visibility = View.GONE
        overlayStateMachine.markExpired(fingerprint ?: overlayStateMachine.currentFingerprint())
    }

    override fun hideDebugOverlay() {
        debugComposeView?.visibility = View.GONE
    }

    override fun removeOverlay() {
        resetOverlayView()
        resetDebugOverlayView()
        overlayStateMachine.markHidden()
    }

    override fun removeOverlayWindowsForScan(): Boolean {
        val hadOverlayWindow = composeView?.parent != null || debugComposeView?.parent != null
        if (hadOverlayWindow) {
            removeOverlay()
        }
        return hadOverlayWindow
    }

    override fun setOnUserDismissed(callback: (() -> Unit)?) {
        userDismissedCallback = callback
    }

    private fun resetOverlayView() {
        composeView?.let {
            if (it.parent != null) {
                runCatching { (overlayWindowManager ?: baseWindowManager).removeView(it) }
            }
            lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        composeView = null
        lifecycleOwner = null
        overlayWindowContext = null
        overlayWindowManager = null
        currentLayoutParams = null
    }

    private fun resetDebugOverlayView() {
        debugComposeView?.let {
            if (it.parent != null) {
                runCatching { (debugWindowManager ?: baseWindowManager).removeView(it) }
            }
            debugLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        debugComposeView = null
        debugLifecycleOwner = null
        debugWindowContext = null
        debugWindowManager = null
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
                            profitability = profitabilityState.value
                        )
                    }
                }
            }
        }
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
        retryAccessibilityOverlay: () -> Unit,
        retryApplicationOverlay: () -> Unit
    ) {
        if (
            BuildConfig.DEBUG &&
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
        if (context is AccessibilityService && !forceApplicationOverlay) {
            return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
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
