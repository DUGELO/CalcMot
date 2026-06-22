package br.com.calcmot.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import br.com.calcmot.DriverApp

class NinetyNineSemanticBridgeProbe(
    diagnostics: NinetyNineAccessibilityDiagnostics
) {
    fun flagsFor(driverApp: DriverApp, currentFlags: Int): Int = currentFlags

    fun requestSemantics(
        generation: Long,
        delayMs: Long,
        roots: List<Pair<String, AccessibilityNodeInfo>>
    ) = Unit
}
