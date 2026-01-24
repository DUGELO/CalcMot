package com.example.calcmot.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
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
import com.example.calcmot.model.TripData
import com.example.calcmot.ui.theme.MetricaTheme
import android.util.Log

open class OverlayManager(private val context: Context) : IOverlayManager {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private val tripDataState = mutableStateOf<TripData?>(null)
    private var lifecycleOwner: CustomLifecycleOwner? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun showOverlay(data: TripData) {
        try {
            tripDataState.value = data

            if (composeView == null) {
                lifecycleOwner = CustomLifecycleOwner()
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                createComposeView()
                lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
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
            lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        composeView = null
        lifecycleOwner = null
    }

    private fun createComposeView() {
        composeView = ComposeView(context).apply {
            val owner = lifecycleOwner!!
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

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