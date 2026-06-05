package br.com.calcmot.model

enum class OfferCaptureSource(val id: String) {
    UIAUTOMATOR_LAB("uiautomator_lab"),
    ACCESSIBILITY_TREE("accessibility_tree");

    companion object {
        fun fromId(id: String?): OfferCaptureSource? {
            return entries.firstOrNull { it.id == id }
        }
    }
}

enum class OfferCaptureRejectionReason(val id: String) {
    INVALID_FRAME("invalid_frame"),
    NO_PRICE("no_price"),
    MULTIPLE_PRIMARY_PRICES("multiple_primary_prices"),
    NO_ACTION_BUTTON("no_action_button"),
    INCOMPLETE_TIME_DISTANCE_BLOCKS("incomplete_time_distance_blocks"),
    INVALID_VERTICAL_ORDER("invalid_vertical_order"),
    NOT_CARD_LIKE("not_card_like"),
    PARSER_REJECTED("parser_rejected"),
    INVALID_CONTEXT_STILL_THERE("invalid_context_still_there"),
    INVALID_CONTEXT_REQUEST_UNAVAILABLE("invalid_context_request_unavailable"),
    INVALID_CONTEXT_NO_REQUEST("invalid_context_no_request"),
    INVALID_CONTEXT_OFFLINE("invalid_context_offline"),
    UNKNOWN("unknown");

    companion object {
        fun fromId(id: String?): OfferCaptureRejectionReason? {
            return entries.firstOrNull { it.id == id }
        }
    }
}

fun OfferCaptureRejectionReason.isInvalidContext(): Boolean {
    return this == OfferCaptureRejectionReason.INVALID_CONTEXT_STILL_THERE ||
        this == OfferCaptureRejectionReason.INVALID_CONTEXT_REQUEST_UNAVAILABLE ||
        this == OfferCaptureRejectionReason.INVALID_CONTEXT_NO_REQUEST ||
        this == OfferCaptureRejectionReason.INVALID_CONTEXT_OFFLINE
}

fun OfferCaptureRejectionReason.isImmediateInvalidContext(): Boolean {
    return this == OfferCaptureRejectionReason.INVALID_CONTEXT_STILL_THERE ||
        this == OfferCaptureRejectionReason.INVALID_CONTEXT_REQUEST_UNAVAILABLE
}

data class OfferCaptureOutcome(
    val source: OfferCaptureSource,
    val fingerprint: String?,
    val candidate: OfferCandidate?,
    val tripData: TripData?,
    val rejectionReason: OfferCaptureRejectionReason?,
    val capturedAtMillis: Long = System.currentTimeMillis()
) {
    val isComplete: Boolean
        get() = candidate != null || tripData != null

    companion object {
        fun accepted(
            source: OfferCaptureSource,
            candidate: OfferCandidate,
            capturedAtMillis: Long = System.currentTimeMillis()
        ): OfferCaptureOutcome {
            return OfferCaptureOutcome(
                source = source,
                fingerprint = candidate.fingerprint,
                candidate = candidate,
                tripData = null,
                rejectionReason = null,
                capturedAtMillis = capturedAtMillis
            )
        }

        fun accepted(
            source: OfferCaptureSource,
            tripData: TripData,
            capturedAtMillis: Long = System.currentTimeMillis()
        ): OfferCaptureOutcome {
            return OfferCaptureOutcome(
                source = source,
                fingerprint = null,
                candidate = null,
                tripData = tripData,
                rejectionReason = null,
                capturedAtMillis = capturedAtMillis
            )
        }

        fun rejected(
            source: OfferCaptureSource,
            reason: OfferCaptureRejectionReason,
            capturedAtMillis: Long = System.currentTimeMillis()
        ): OfferCaptureOutcome {
            return OfferCaptureOutcome(
                source = source,
                fingerprint = null,
                candidate = null,
                tripData = null,
                rejectionReason = reason,
                capturedAtMillis = capturedAtMillis
            )
        }
    }
}
