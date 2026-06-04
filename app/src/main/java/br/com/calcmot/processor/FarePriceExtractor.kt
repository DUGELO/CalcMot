package br.com.calcmot.processor

object FarePriceExtractor {

    private val priceRegex = Regex("""R\$\s*([0-9]+(?:[.,][0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val bonusPriceKeywords = listOf("incluido", "prioridade", "bonus", "adicional")

    fun extractPrimaryFare(rawText: String): Double? {
        return extractPrimaryFareCandidates(rawText).firstOrNull()
    }

    fun extractPrimaryFareCandidates(rawText: String): List<Double> {
        return TextNormalizer.clean(rawText)
            .lineSequence()
            .flatMap { line -> findPrimaryFareMatches(line).asSequence() }
            .flatMap { match -> match.amountText.toFareCandidates().asSequence() }
            .distinct()
            .toList()
    }

    fun extractSinglePrimaryFare(rawText: String): Double? {
        val matches = TextNormalizer.clean(rawText)
            .lineSequence()
            .flatMap { line -> findPrimaryFareMatches(line).asSequence() }
            .toList()

        if (matches.size != 1) return null
        return matches.single().amountText.toDecimal()
    }

    fun containsPrimaryFare(text: String): Boolean {
        return findPrimaryFareMatches(TextNormalizer.clean(text)).isNotEmpty()
    }

    private fun findPrimaryFareMatches(line: String): List<FareMatch> {
        val matches = priceRegex.findAll(line).toList()
        return matches.mapIndexedNotNull { index, match ->
            val nextPriceStart = matches.getOrNull(index + 1)?.range?.first ?: line.length
            match.toPrimaryFareMatchOrNull(line, nextPriceStart)
        }
    }

    private fun MatchResult.toPrimaryFareMatchOrNull(line: String, nextPriceStart: Int): FareMatch? {
        val previousNonBlank = line.substring(0, range.first).lastOrNull { !it.isWhitespace() }
        if (previousNonBlank == '+') return null

        val localContext = TextNormalizer.searchKey(line.substring(range.first, nextPriceStart))
        val lineContext = TextNormalizer.searchKey(line)
        if (lineContext.contains("/km") ||
            lineContext.contains("/h") ||
            lineContext.contains("por km") ||
            lineContext.contains("por hora")
        ) {
            return null
        }
        if (bonusPriceKeywords.any { localContext.contains(it) }) return null

        return FareMatch(amountText = groupValues[1])
    }

    private fun String.toDecimal(): Double? {
        return replace(",", ".").toDoubleOrNull()
    }

    private fun String.toFareCandidates(): List<Double> {
        val decimal = toDecimal() ?: return emptyList()
        if (contains(",") || contains(".")) return listOf(decimal)
        if (length !in IMPLICIT_CENTS_LENGTH_RANGE) return listOf(decimal)

        val centsDecimal = (toLongOrNull() ?: return listOf(decimal)) / 100.0
        return listOf(decimal, centsDecimal).distinct()
    }

    private data class FareMatch(val amountText: String)

    private val IMPLICIT_CENTS_LENGTH_RANGE = 3..5
}
