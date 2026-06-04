package br.com.calcmot.processor

import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.TripData

class OfferStabilityGate(
    private val requiredMatchingFrames: Int = 2
) {
    private var lastFingerprint: String? = null
    private var matchingFrames: Int = 0

    fun accept(candidate: OfferCandidate?): TripData? {
        if (candidate == null) {
            reset()
            return null
        }

        val fingerprint = candidate.fingerprint
        if (fingerprint == lastFingerprint) {
            matchingFrames++
        } else {
            lastFingerprint = fingerprint
            matchingFrames = 1
        }

        return if (matchingFrames >= requiredMatchingFrames) {
            candidate.toTripData()
        } else {
            null
        }
    }

    fun reset() {
        lastFingerprint = null
        matchingFrames = 0
    }
}
