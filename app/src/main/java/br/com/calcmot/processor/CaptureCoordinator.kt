package br.com.calcmot.processor

import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.OfferCaptureRejectionReason
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.model.TripData
import java.util.Locale

class CaptureCoordinator(
    requiredMatchingFrames: Int = 2,
    private val requiredInvalidFramesToReset: Int = 2,
    private val overlayTtlMillis: Long = DEFAULT_OVERLAY_TTL_MILLIS,
    private val userDismissSuppressMillis: Long = DEFAULT_USER_DISMISS_SUPPRESS_MILLIS,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val stabilityGate = OfferStabilityGate(requiredMatchingFrames = requiredMatchingFrames)
    private var currentOverlayFingerprint: String? = null
    private var lastValidCandidateAtMillis: Long? = null
    private var suppressedFingerprint: String? = null
    private var suppressedUntilMillis: Long = 0L
    private var invalidFrameStreak = 0

    fun acceptCandidate(
        source: OfferCaptureSource,
        candidate: OfferCandidate,
        trustedSingleFrame: Boolean = false
    ): CaptureDecision {
        val now = clockMillis()
        val nextOverlayFingerprint = candidate.overlayFingerprint()
        invalidFrameStreak = 0
        lastValidCandidateAtMillis = now

        if (isSuppressed(nextOverlayFingerprint, now)) {
            return CaptureDecision.SuppressedCandidate(
                source = source,
                candidate = candidate,
                overlayFingerprint = nextOverlayFingerprint
            )
        }
        clearExpiredOrDifferentSuppression(nextOverlayFingerprint, now)

        if (trustedSingleFrame) {
            val tripData = candidate.toTripData()
            if (tripData != null) {
                stabilityGate.accept(candidate)
                currentOverlayFingerprint = nextOverlayFingerprint
                return CaptureDecision.ShowOverlay(
                    source = source,
                    tripData = tripData,
                    overlayFingerprint = nextOverlayFingerprint
                )
            }
        }

        val tripData = stabilityGate.accept(candidate)
        if (tripData == null) {
            val shouldHideStaleOverlay = currentOverlayFingerprint != null &&
                currentOverlayFingerprint != nextOverlayFingerprint
            if (shouldHideStaleOverlay) {
                currentOverlayFingerprint = null
                lastValidCandidateAtMillis = null
                return CaptureDecision.HideStaleOverlay(
                    source = source,
                    candidate = candidate,
                    overlayFingerprint = nextOverlayFingerprint
                )
            }

            return CaptureDecision.WaitingForStability(
                source = source,
                candidate = candidate,
                overlayFingerprint = nextOverlayFingerprint
            )
        }

        currentOverlayFingerprint = nextOverlayFingerprint
        return CaptureDecision.ShowOverlay(
            source = source,
            tripData = tripData,
            overlayFingerprint = nextOverlayFingerprint
        )
    }

    fun acceptStableTrip(source: OfferCaptureSource, tripData: TripData): CaptureDecision.ShowOverlay {
        val now = clockMillis()
        val overlayFingerprint = tripData.overlayFingerprint()
        invalidFrameStreak = 0
        lastValidCandidateAtMillis = now
        currentOverlayFingerprint = overlayFingerprint
        return CaptureDecision.ShowOverlay(
            source = source,
            tripData = tripData,
            overlayFingerprint = overlayFingerprint
        )
    }

    fun rejectFrame(
        source: OfferCaptureSource,
        reason: OfferCaptureRejectionReason
    ): CaptureDecision {
        invalidFrameStreak++
        if (invalidFrameStreak < requiredInvalidFramesToReset) {
            return CaptureDecision.KeepCurrentOverlay(
                source = source,
                reason = reason,
                invalidFrameStreak = invalidFrameStreak,
                requiredInvalidFramesToReset = requiredInvalidFramesToReset
            )
        }

        resetState(clearSuppression = false)
        return CaptureDecision.HideOverlay(source = source, reason = reason)
    }

    fun expireOverlayIfStale(): CaptureDecision.HideOverlay? {
        val currentFingerprint = currentOverlayFingerprint ?: return null
        val lastSeenAt = lastValidCandidateAtMillis ?: return null
        val now = clockMillis()
        if (now - lastSeenAt < overlayTtlMillis) return null

        resetState(clearSuppression = false)
        return CaptureDecision.HideOverlay(
            source = null,
            reason = OfferCaptureRejectionReason.INVALID_FRAME,
            overlayFingerprint = currentFingerprint
        )
    }

    fun dismissCurrentOverlayByUser(): CaptureDecision.HideOverlay {
        val now = clockMillis()
        currentOverlayFingerprint?.let {
            suppressedFingerprint = it
            suppressedUntilMillis = now + userDismissSuppressMillis
        }
        resetState(clearSuppression = false)
        return CaptureDecision.HideOverlay(
            source = null,
            reason = OfferCaptureRejectionReason.INVALID_FRAME
        )
    }

    fun reset(): CaptureDecision.HideOverlay {
        resetState(clearSuppression = true)
        return CaptureDecision.HideOverlay(
            source = null,
            reason = OfferCaptureRejectionReason.INVALID_FRAME
        )
    }

    private fun resetState(clearSuppression: Boolean) {
        stabilityGate.reset()
        currentOverlayFingerprint = null
        invalidFrameStreak = 0
        lastValidCandidateAtMillis = null
        if (clearSuppression) {
            suppressedFingerprint = null
            suppressedUntilMillis = 0L
        }
    }

    private fun isSuppressed(fingerprint: String, now: Long): Boolean {
        return suppressedFingerprint == fingerprint && now < suppressedUntilMillis
    }

    private fun clearExpiredOrDifferentSuppression(fingerprint: String, now: Long) {
        if (suppressedFingerprint == null) return
        if (suppressedFingerprint != fingerprint || now >= suppressedUntilMillis) {
            suppressedFingerprint = null
            suppressedUntilMillis = 0L
        }
    }

    private companion object {
        const val DEFAULT_OVERLAY_TTL_MILLIS = 1_200L
        const val DEFAULT_USER_DISMISS_SUPPRESS_MILLIS = 10_000L
    }
}

sealed interface CaptureDecision {
    val source: OfferCaptureSource?

    data class WaitingForStability(
        override val source: OfferCaptureSource,
        val candidate: OfferCandidate,
        val overlayFingerprint: String
    ) : CaptureDecision

    data class HideStaleOverlay(
        override val source: OfferCaptureSource,
        val candidate: OfferCandidate,
        val overlayFingerprint: String
    ) : CaptureDecision

    data class ShowOverlay(
        override val source: OfferCaptureSource,
        val tripData: TripData,
        val overlayFingerprint: String
    ) : CaptureDecision

    data class SuppressedCandidate(
        override val source: OfferCaptureSource,
        val candidate: OfferCandidate,
        val overlayFingerprint: String
    ) : CaptureDecision

    data class KeepCurrentOverlay(
        override val source: OfferCaptureSource,
        val reason: OfferCaptureRejectionReason,
        val invalidFrameStreak: Int,
        val requiredInvalidFramesToReset: Int
    ) : CaptureDecision

    data class HideOverlay(
        override val source: OfferCaptureSource?,
        val reason: OfferCaptureRejectionReason,
        val overlayFingerprint: String? = null
    ) : CaptureDecision
}

fun OfferCandidate.overlayFingerprint(): String {
    return "%.2f|%.1f|%d".format(
        Locale.US,
        price,
        totalDistanceKm,
        totalTimeMin
    )
}

fun TripData.overlayFingerprint(): String {
    return "%.2f|%.1f|%d".format(
        Locale.US,
        valor,
        distanciaKm,
        minutosTotais
    )
}
