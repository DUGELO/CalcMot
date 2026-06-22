package br.com.calcmot.ninetynine

import br.com.calcmot.processor.NinetyNineOfferParser
import br.com.calcmot.processor.TextNormalizer

object NinetyNineOfferExtractor {

    fun extract(frame: NinetyNineOcrFrame): NinetyNineExtractionResult {
        if (frame.lines.isEmpty()) {
            return NinetyNineExtractionResult.Rejected(NinetyNineExtractionRejection.EMPTY_OCR)
        }

        val orderedLines = frame.lines
            .sortedWith(compareBy<NinetyNineOcrLine> { it.bounds.top }.thenBy { it.bounds.left })
            .map(NinetyNineOcrLine::text)
        val sanitizedFrameLines = NinetyNineSanitizer.sanitize(orderedLines)
        if (sanitizedFrameLines.isEmpty()) {
            return NinetyNineExtractionResult.Rejected(NinetyNineExtractionRejection.EMPTY_OCR)
        }

        val sanitizedFrameText = sanitizedFrameLines.joinToString("\n")
        val searchText = TextNormalizer.searchKey(sanitizedFrameText)
        val inactiveMarker = NinetyNineRecognitionConfig.inactiveFrameMarkers
            .firstOrNull(searchText::contains)
        val frameHasOfferMarker = NinetyNineRecognitionConfig.offerMarkers.any(searchText::contains)
        if (inactiveMarker != null && !frameHasOfferMarker) {
            return NinetyNineExtractionResult.Rejected(
                NinetyNineExtractionRejection.INACTIVE_FRAME,
                sanitizedFrameText
            )
        }

        var sawOfferMarker = false
        actionDelimitedGroups(orderedLines)
            .plusElement(orderedLines)
            .distinct()
            .forEach { group ->
                val sanitizedLines = NinetyNineSanitizer.sanitize(group)
                val sanitizedText = sanitizedLines.joinToString("\n")
                val groupSearchText = TextNormalizer.searchKey(sanitizedText)
                val hasOfferMarker =
                    NinetyNineRecognitionConfig.offerMarkers.any(groupSearchText::contains)
                if (!hasOfferMarker) return@forEach
                sawOfferMarker = true
                val candidate = NinetyNineOfferParser.parse(sanitizedText) ?: return@forEach
                return NinetyNineExtractionResult.Candidate(candidate, sanitizedText)
            }

        if (!sawOfferMarker) {
            return NinetyNineExtractionResult.Rejected(
                NinetyNineExtractionRejection.NO_OFFER_MARKER,
                sanitizedFrameText
            )
        }
        return NinetyNineExtractionResult.Rejected(
            NinetyNineExtractionRejection.PARSER_REJECTED,
            sanitizedFrameText
        )
    }

    private fun actionDelimitedGroups(lines: List<String>): List<List<String>> {
        val groups = mutableListOf<List<String>>()
        var startIndex = 0
        lines.forEachIndexed { index, line ->
            val normalized = TextNormalizer.searchKey(line)
            val isAction = NinetyNineRecognitionConfig.actionMarkers.any(normalized::contains)
            if (!isAction) return@forEachIndexed
            if (index >= startIndex) {
                groups += lines.subList(startIndex, index + 1)
            }
            startIndex = index + 1
        }
        return groups
    }
}
