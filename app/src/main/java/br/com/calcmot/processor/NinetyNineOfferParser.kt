package br.com.calcmot.processor

import br.com.calcmot.DriverApp
import br.com.calcmot.model.OfferCandidate

object NinetyNineOfferParser {

    fun parse(rawText: String): OfferCandidate? {
        val lines = rawText
            .lineSequence()
            .map(TextNormalizer::clean)
            .filter(String::isNotBlank)
            .toList()
        if (lines.isEmpty()) return null

        val normalizedText = normalize(lines.joinToString(" "))
        val hasAction = DriverApp.NINETY_NINE.actionLabels.any(normalizedText::contains)
        val hasActionableCardEvidence =
            normalizedText.contains("negocia") ||
                (
                    normalizedText.contains("corridas") &&
                        actionableProfileMarkers.any(normalizedText::contains)
                    )
        val blocks = extractTimeDistanceBlocks(lines)
        if (blocks.size != REQUIRED_TRIP_BLOCKS) return null

        val pickup = blocks.singleOrNull { it.role == BlockRole.PICKUP }
        val trip = blocks.singleOrNull { it.role == BlockRole.TRIP }
        val ordered = blocks.sortedBy { it.startIndex }
        val (resolvedPickup, resolvedTrip) =
            if (pickup != null && trip != null && pickup != trip) {
                pickup to trip
            } else {
                ordered.first() to ordered.last()
            }

        if (resolvedPickup == resolvedTrip) return null
        if (!hasAction && !hasActionableCardEvidence && (pickup == null || trip == null)) return null
        if (!hasAction && containsNonOfferFinancialContext(normalizedText)) return null

        val rating = lines.firstNotNullOfOrNull(::extractRating)
        return FarePriceExtractor.extractPrimaryFareCandidates(rawText)
            .firstNotNullOfOrNull { price ->
                OfferCandidate(
                    price = price,
                    pickupDistanceKm = resolvedPickup.distanceKm,
                    pickupTimeMin = resolvedPickup.timeMin,
                    tripDistanceKm = resolvedTrip.distanceKm,
                    tripTimeMin = resolvedTrip.timeMin,
                    passengerRating = rating
                ).takeIf { it.toTripData() != null }
            }
    }

    private fun extractTimeDistanceBlocks(lines: List<String>): List<TimeDistanceBlock> {
        val candidates = mutableListOf<TimeDistanceBlock>()
        for (startIndex in lines.indices) {
            for (windowSize in 1..MAX_BLOCK_WINDOW_SIZE) {
                val endIndex = startIndex + windowSize - 1
                if (endIndex !in lines.indices) break
                val text = lines.subList(startIndex, endIndex + 1).joinToString(" ")
                val normalizedBlock = normalize(text)
                val distanceMatch = distanceRegex.find(normalizedBlock) ?: continue
                val timeMin = extractMinutesBeforeDistance(
                    normalizedBlock.substring(0, distanceMatch.range.first)
                ) ?: continue
                val distanceValue = distanceMatch.groupValues
                    .getOrNull(1)
                    ?.replace(",", ".")
                    ?.toDoubleOrNull()
                    ?: continue
                val distanceKm = if (distanceMatch.groupValues.getOrNull(2) == "m") {
                    distanceValue / METERS_PER_KILOMETER
                } else {
                    distanceValue
                }
                if (timeMin <= 0 || distanceKm <= 0.0) continue
                candidates += TimeDistanceBlock(
                    startIndex = startIndex,
                    endIndex = endIndex,
                    timeMin = timeMin,
                    distanceKm = distanceKm,
                    role = detectRole(text)
                )
                break
            }
        }

        return candidates
            .sortedBy { it.startIndex }
            .fold(mutableListOf()) { accepted, block ->
                if (accepted.isEmpty() || block.startIndex > accepted.last().endIndex) {
                    accepted += block
                }
                accepted
            }
    }

    private fun extractMinutesBeforeDistance(prefix: String): Int? {
        compactHourMinuteRegex.findAll(prefix).lastOrNull()?.let { match ->
            val hours = match.groupValues[1].toIntOrNull() ?: return@let
            val minutes = match.groupValues[2].toIntOrNull() ?: return@let
            return (hours * 60 + minutes).takeIf { it in 1..MAX_TRIP_TIME_MINUTES }
        }
        minuteOnlyRegex.findAll(prefix).lastOrNull()?.let { match ->
            return match.groupValues[1]
                .toIntOrNull()
                ?.takeIf { it in 1..MAX_TRIP_TIME_MINUTES }
        }
        hourOnlyRegex.findAll(prefix).lastOrNull()?.let { match ->
            val hours = match.groupValues[1].toIntOrNull() ?: return@let
            return (hours * 60).takeIf { it in 1..MAX_TRIP_TIME_MINUTES }
        }
        return null
    }

    private fun detectRole(text: String): BlockRole {
        val normalized = normalize(text)
        return when {
            pickupMarkers.any { normalized.containsWholeMarker(it) } -> BlockRole.PICKUP
            tripMarkers.any { normalized.containsWholeMarker(it) } -> BlockRole.TRIP
            else -> BlockRole.UNKNOWN
        }
    }

    private fun String.containsWholeMarker(marker: String): Boolean {
        return Regex("""\b${Regex.escape(marker)}\b""").containsMatchIn(this)
    }

    private fun extractRating(line: String): Double? {
        val normalized = normalize(line)
        if (normalized.contains("km") || normalized.contains("min") || normalized.contains("r$")) return null
        return ratingRegex.find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
    }

    private fun containsNonOfferFinancialContext(text: String): Boolean {
        return nonOfferFinancialMarkers.any(text::contains)
    }

    private fun normalize(value: String): String = TextNormalizer.searchKey(value)

    private data class TimeDistanceBlock(
        val startIndex: Int,
        val endIndex: Int,
        val timeMin: Int,
        val distanceKm: Double,
        val role: BlockRole
    )

    private enum class BlockRole {
        PICKUP,
        TRIP,
        UNKNOWN
    }

    private val pickupMarkers = setOf(
        "embarque",
        "origem",
        "ate o passageiro",
        "buscar passageiro",
        "distancia ate o passageiro",
        "chegar ao passageiro"
    )
    private val tripMarkers = setOf(
        "corrida",
        "viagem",
        "destino",
        "duracao estimada",
        "distancia estimada"
    )
    private val nonOfferFinancialMarkers = setOf(
        "ganhos",
        "saldo",
        "meta diaria",
        "historico",
        "extrato"
    )
    private val actionableProfileMarkers = setOf(
        "perfil",
        "cartao",
        "pgto no app",
        "pagamento no app",
        "dinheiro"
    )
    private val distanceRegex = Regex("""\b([0-9]+(?:[.,][0-9]+)?)\s*(km|m)\b""")
    private val compactHourMinuteRegex = Regex("""\b([0-9]{1,2})h\s*([0-9]{1,2})\b""")
    private val minuteOnlyRegex = Regex("""\b([0-9]{1,3})\s*(?:min|minuto(?:s)?)\b""")
    private val hourOnlyRegex = Regex("""\b([0-9]{1,2})\s*(?:h|hora(?:s)?)\b""")
    private val ratingRegex = Regex("""(?:^|\s)([345][.,][0-9]{1,2})(?:\s|$)""")
    private const val MAX_BLOCK_WINDOW_SIZE = 5
    private const val REQUIRED_TRIP_BLOCKS = 2
    private const val METERS_PER_KILOMETER = 1_000.0
    private const val MAX_TRIP_TIME_MINUTES = 12 * 60
}
