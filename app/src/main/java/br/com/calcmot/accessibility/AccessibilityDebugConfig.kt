package br.com.calcmot.accessibility

import br.com.calcmot.BuildConfig

object AccessibilityDebugConfig {
    val ENABLE_ACCESSIBILITY_DEEP_DEBUG: Boolean = BuildConfig.DEBUG
    val ENABLE_TREE_DUMP: Boolean = BuildConfig.DEBUG
    val ENABLE_TIMING_SWEEP: Boolean = BuildConfig.DEBUG
    val ENABLE_DEBUG_OVERLAY_HEARTBEAT: Boolean = false
    const val ENABLE_ZERO_OVERLAY_DURING_SCAN: Boolean = true
    val ENABLE_VIEW_ID_DISCOVERY: Boolean = BuildConfig.DEBUG

    const val MAX_DEBUG_TREE_DEPTH = 80
    const val MAX_DEBUG_TREE_NODES = 5_000

    val TIMING_SWEEP_DELAYS_MS = longArrayOf(
        0L,
        80L,
        160L,
        300L,
        500L,
        750L,
        1_000L
    )
}
