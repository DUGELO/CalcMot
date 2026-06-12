package br.com.calcmot.overlay

import android.graphics.Rect
import br.com.calcmot.accessibility.AccessibilityDebugOverlayState
import br.com.calcmot.model.TripData
import br.com.calcmot.telemetry.OverlayLatencyTrace

interface IOverlayManager {
    val isVisible: Boolean
        get() = false

    val visibleBounds: Rect?
        get() = null

    fun showOverlay(data: TripData)
    fun showDebugOverlay(state: AccessibilityDebugOverlayState) = Unit
    fun setForegroundPackage(packageName: String?) = Unit
    fun setLatencyTrace(trace: OverlayLatencyTrace?) = Unit
    fun hideDebugOverlay() = Unit
    fun hideOverlay()
    fun expireOverlay(fingerprint: String? = null) = hideOverlay()
    fun removeOverlay()
    fun removeOverlayWindowsForScan(): Boolean = false
    fun setOnUserDismissed(callback: (() -> Unit)?) = Unit
}
