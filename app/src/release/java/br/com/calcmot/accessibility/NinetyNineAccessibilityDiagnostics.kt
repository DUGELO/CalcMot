package br.com.calcmot.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.TreeOfferInspection

class NinetyNineAccessibilityDiagnostics(context: Context) {
    fun recordServiceConfiguration(details: String) = Unit

    fun recordDriverAppSwitch(previous: String, next: String, packageName: String?) = Unit

    fun recordEvent(event: AccessibilityEvent, eventName: String) = Unit

    fun recordCaptureSession(
        generation: Long,
        shouldStartBurst: Boolean,
        eventType: Int,
        windowId: Int,
        coalesced: Boolean
    ) = Unit

    fun recordRootSelection(
        generation: Long,
        delayMs: Long,
        rootNull: Boolean,
        windowCount: Int,
        roots: List<String>
    ) = Unit

    fun recordSemanticBridgeProbe(
        generation: Long,
        delayMs: Long,
        touchExplorationRequested: Boolean,
        results: List<String>
    ) = Unit

    fun recordSnapshot(
        snapshot: AccessibilityTreeSnapshot,
        inspection: TreeOfferInspection,
        candidate: OfferCandidate?
    ) = Unit

    fun recordNoCandidate(
        generation: Long,
        delayMs: Long,
        bestReason: String,
        rejectedReasons: List<String>
    ) = Unit

    fun recordOcrResult(status: String, detail: String, rawText: String?) = Unit

    fun close() = Unit
}
