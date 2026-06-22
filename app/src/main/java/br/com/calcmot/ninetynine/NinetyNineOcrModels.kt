package br.com.calcmot.ninetynine

import br.com.calcmot.processor.ScreenBounds

data class NinetyNineOcrLine(
    val text: String,
    val bounds: ScreenBounds
)

data class NinetyNineOcrFrame(
    val lines: List<NinetyNineOcrLine>,
    val width: Int,
    val height: Int,
    val capturedAtMillis: Long = System.currentTimeMillis()
)

enum class NinetyNineExtractionRejection {
    EMPTY_OCR,
    INACTIVE_FRAME,
    NO_OFFER_MARKER,
    PARSER_REJECTED
}

sealed interface NinetyNineExtractionResult {
    data class Candidate(
        val value: br.com.calcmot.model.OfferCandidate,
        val sanitizedText: String
    ) : NinetyNineExtractionResult

    data class Rejected(
        val reason: NinetyNineExtractionRejection,
        val sanitizedText: String = ""
    ) : NinetyNineExtractionResult
}
