package br.com.calcmot.processor

import br.com.calcmot.model.OfferCandidate

object OfferParser {

    private val distanceRegex = Regex("""\b([0-9]+(?:[.,][0-9]+)?)\s*km\b""")
    private val ratingRegex = Regex("""(?:^|\s)([345][.,][0-9]{1,2})(?:\s*\([0-9]+\))?""")

    fun parse(rawText: String): OfferCandidate? {
        val lines = rawText
            .lineSequence()
            .map { TextNormalizer.clean(it) }
            .filter { it.isNotEmpty() }
            .toList()
            .withoutOverlayNoise()

        if (lines.isEmpty()) return null

        val blocks = extractTimeDistanceBlocks(lines)
        if (blocks.size != REQUIRED_TRIP_BLOCKS) return null

        val orderedBlocks = blocks.sortedBy { it.startIndex }
        val tripBlockIndex = orderedBlocks
            .mapIndexedNotNull { index, block -> index.takeIf { normalize(block.text).contains("viagem") } }
            .singleOrNull()

        val pickup: TimeDistance
        val trip: TimeDistance
        if (tripBlockIndex == null) {
            pickup = orderedBlocks[0].timeDistance
            trip = orderedBlocks[1].timeDistance
        } else {
            if (tripBlockIndex == 0) return null
            pickup = orderedBlocks[1 - tripBlockIndex].timeDistance
            trip = orderedBlocks[tripBlockIndex].timeDistance
        }

        val rating = extractRating(lines)

        return extractPriceCandidates(rawText).firstNotNullOfOrNull { price ->
            OfferCandidate(
                price = price,
                pickupDistanceKm = pickup.distanceKm,
                pickupTimeMin = pickup.timeMin,
                tripDistanceKm = trip.distanceKm,
                tripTimeMin = trip.timeMin,
                passengerRating = rating
            ).takeIf { it.toTripData() != null }
        }
    }

    private fun extractPriceCandidates(rawText: String): List<Double> {
        return FarePriceExtractor.extractPrimaryFareCandidates(rawText)
    }

    private fun List<String>.withoutOverlayNoise(): List<String> {
        val filtered = mutableListOf<String>()
        var previousWasOverlayMetric = false

        for (line in this) {
            val normalized = normalize(line)
            val isOverlayQuality = normalized == "boa" ||
                normalized == "media" ||
                normalized == "ruim"
            val isOverlayMetric = normalized.contains("/km") ||
                normalized.contains("/h") ||
                normalized.contains("por km") ||
                normalized.contains("por hora")
            val isStandaloneDurationAfterMetric = previousWasOverlayMetric &&
                DurationParser.parseMinutes(line) != null &&
                !normalized.contains("km") &&
                !normalized.contains("viagem") &&
                !normalized.contains("distancia")

            if (isOverlayQuality || isOverlayMetric || isStandaloneDurationAfterMetric) {
                previousWasOverlayMetric = true
                continue
            }

            previousWasOverlayMetric = false
            filtered += line
        }

        return filtered
    }

    private fun extractTimeDistanceBlocks(lines: List<String>): List<TripBlock> {
        val candidateBlocks = mutableListOf<TripBlock>()

        for (startIndex in lines.indices) {
            for (windowSize in 1..MAX_BLOCK_WINDOW_SIZE) {
                val endIndex = startIndex + windowSize - 1
                if (endIndex !in lines.indices) break

                val segment = lines.subList(startIndex, endIndex + 1).joinToString(separator = " ")
                val timeDistance = extractTimeDistance(segment) ?: continue
                candidateBlocks += TripBlock(
                    text = segment,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    timeDistance = timeDistance
                )
                break
            }
        }

        return candidateBlocks
            .sortedWith(compareBy<TripBlock> { it.startIndex }.thenBy { it.endIndex - it.startIndex })
            .fold(mutableListOf()) { accepted, block ->
                if (accepted.isEmpty() || block.startIndex > accepted.last().endIndex) {
                    accepted += block
                }
                accepted
            }
    }

    private fun extractTimeDistance(text: String): TimeDistance? {
        val timeMin = DurationParser.parseMinutes(text) ?: return null
        val distanceKm = distanceRegex.find(normalize(text))
            ?.groupValues
            ?.getOrNull(1)
            ?.toDecimal()
            ?: return null

        if (timeMin <= 0 || distanceKm <= 0.0) return null
        return TimeDistance(timeMin = timeMin, distanceKm = distanceKm)
    }

    private fun extractRating(lines: List<String>): Double? {
        return lines.firstNotNullOfOrNull { line ->
            val normalized = normalize(line)
            if (!normalized.contains("verificado") &&
                !normalized.contains("viagem") &&
                !normalized.contains("distancia") &&
                !normalized.contains("km") &&
                !normalized.contains("min") &&
                !normalized.contains("hora")
            ) {
                ratingRegex.find(line)?.groupValues?.getOrNull(1)?.toDecimal()
            } else {
                null
            }
        }
    }

    private fun normalize(value: String): String {
        return TextNormalizer.searchKey(value)
    }

    private fun String.toDecimal(): Double? {
        return replace(",", ".").toDoubleOrNull()
    }

    private data class TripBlock(
        val text: String,
        val startIndex: Int,
        val endIndex: Int,
        val timeDistance: TimeDistance
    )

    private data class TimeDistance(
        val timeMin: Int,
        val distanceKm: Double
    )

    private const val MAX_BLOCK_WINDOW_SIZE = 6
    private const val REQUIRED_TRIP_BLOCKS = 2
}
