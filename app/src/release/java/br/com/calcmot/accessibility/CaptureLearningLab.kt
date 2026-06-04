package br.com.calcmot.accessibility

import android.content.Context
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.OfferCaptureRejectionReason
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.model.TripData
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.TreeOfferInspection

class CaptureLearningLab(context: Context) {
    fun recordCandidate(source: OfferCaptureSource, candidate: OfferCandidate) = Unit

    fun recordOverlay(source: OfferCaptureSource?, tripData: TripData) = Unit

    fun recordOverlayHidden(
        status: String,
        source: OfferCaptureSource?,
        fingerprint: String?,
        reason: OfferCaptureRejectionReason?
    ) = Unit

    fun recordRejected(source: OfferCaptureSource, reason: OfferCaptureRejectionReason) = Unit

    fun recordTreeInspection(
        snapshot: AccessibilityTreeSnapshot,
        inspection: TreeOfferInspection
    ) = Unit

    fun close() = Unit
}
