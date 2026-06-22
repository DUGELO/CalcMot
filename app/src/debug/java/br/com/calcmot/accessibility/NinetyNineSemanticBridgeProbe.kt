package br.com.calcmot.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import br.com.calcmot.DriverApp

class NinetyNineSemanticBridgeProbe(
    private val diagnostics: NinetyNineAccessibilityDiagnostics
) {
    private var lastProbedGeneration = Long.MIN_VALUE

    fun flagsFor(driverApp: DriverApp, currentFlags: Int): Int {
        return currentFlags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE.inv()
    }

    fun requestSemantics(
        generation: Long,
        delayMs: Long,
        roots: List<Pair<String, AccessibilityNodeInfo>>
    ) {
        if (generation == lastProbedGeneration || delayMs > MAX_INITIAL_PROBE_DELAY_MS) return
        lastProbedGeneration = generation

        val results = targetViewIds.flatMap { targetViewId ->
            roots.flatMap { (sourceName, root) ->
                val matches = runCatching {
                    root.findAccessibilityNodeInfosByViewId(targetViewId)
                }.getOrDefault(emptyList())
                matches.map { node ->
                    val beforeChildren = node.childCount
                    val beforeText = node.text?.toString()
                    val beforeDescription = node.contentDescription?.toString()
                    val focused = runCatching {
                        node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                    }.getOrDefault(false)
                    val refreshed = runCatching { node.refresh() }.getOrDefault(false)
                    val afterChildren = node.childCount
                    "$sourceName id=$targetViewId class=${node.className} window=${node.windowId} " +
                        "visible=${node.isVisibleToUser} important=${node.isImportantForAccessibility} " +
                        "focusable=${node.isFocusable} accessibilityFocused=${node.isAccessibilityFocused} " +
                        "beforeChildren=$beforeChildren afterChildren=$afterChildren " +
                        "beforeText=${beforeText.orEmpty()} beforeDescription=${beforeDescription.orEmpty()} " +
                        "focusAction=$focused refreshed=$refreshed actions=${node.actionList.joinToString("|") { action ->
                            "${action.id}:${action.label?.toString().orEmpty()}"
                        }}"
                }
            }
        }

        diagnostics.recordSemanticBridgeProbe(
            generation = generation,
            delayMs = delayMs,
            touchExplorationRequested = false,
            results = results.ifEmpty { listOf("no-target-nodes") }
        )
    }

    private companion object {
        const val MAX_INITIAL_PROBE_DELAY_MS = 160L
        val targetViewIds = listOf(
            "com.app99.driver:id/main_flutter_flutter_root",
            "com.app99.driver:id/flutter_deal_gesture_container",
            "com.app99.driver:id/broad_order_container"
        )
    }
}
