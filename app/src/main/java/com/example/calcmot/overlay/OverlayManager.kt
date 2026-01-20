package com.example.calcmot.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.calcmot.model.TripData
import com.example.calcmot.ui.theme.MetricaTheme
import android.util.Log

// Agora implementa a interface IOverlayManager
open class OverlayManager(private val context: Context) : IOverlayManager {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private val tripDataState = mutableStateOf<TripData?>(null)
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun showOverlay(data: TripData) {
        try {
            tripDataState.value = data

            if (composeView == null) {
                lifecycleOwner = OverlayLifecycleOwner()
                lifecycleOwner?.onCreate()
                createComposeView()
                lifecycleOwner?.onResume()
            }

            composeView?.visibility = View.VISIBLE

            if (composeView?.parent == null) {
                val params = getLayoutParams()
                windowManager.addView(composeView, params)
            }
        } catch (e: Exception) {
            Log.e("OverlayManager", "Erro showOverlay: ", e)
        }
    }

    override fun hideOverlay() {
        composeView?.visibility = View.GONE
    }

    override fun removeOverlay() {
        composeView?.let {
            if (it.parent != null) windowManager.removeView(it)
            lifecycleOwner?.onDestroy()
        }
        composeView = null
        lifecycleOwner = null
    }

    private fun createComposeView() {
        composeView = ComposeView(context).apply {
            val lifecycle = lifecycleOwner!!
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)

            setContent {
                MetricaTheme {
                    tripDataState.value?.let {
                        OverlayView(tripData = it)
                    }
                }
            }
        }
    }

    private fun getLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 250
        }
    }
}

private class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}