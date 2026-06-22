package br.com.calcmot.ninetynine

import br.com.calcmot.processor.TextNormalizer
import java.util.Locale

object NinetyNineSanitizer {

    fun sanitize(lines: List<String>): List<String> {
        return lines
            .asSequence()
            .map(::sanitizeLine)
            .flatMap(::splitCompositePriceLine)
            .filter(String::isNotBlank)
            .filterNot(::shouldIgnoreOcrLine)
            .distinct()
            .toList()
    }

    private fun splitCompositePriceLine(line: String): Sequence<String> {
        val normalized = TextNormalizer.searchKey(line)
        if (!normalized.contains("/km") && !normalized.contains("por km")) {
            return sequenceOf(line)
        }
        val primaryFare = currencyRegex.find(line)?.value ?: return sequenceOf(line)
        return sequenceOf(primaryFare)
    }

    fun sanitizeLine(rawLine: String): String {
        var line = TextNormalizer.clean(rawLine)
            .replace(Regex("""(?i)\bR[S5]\s*""")) { "R$ " }
            .replace(Regex("""(?i)\b(?:mnin|rnin|rmin|rnin|mim)\b"""), "min")
            .replace(Regex("""(?i)\bZ\s*min\b"""), "7 min")
            .replace(Regex("""(?i)\b([0-9OQIlLAZG&]+)\s*mins?\b""")) {
                "${sanitizeIntegerToken(it.groupValues[1])} min"
            }
            .replace(Regex("""(?i)\b([0-9OQIlLAZG&]+)\s*minutos?\b""")) {
                "${sanitizeIntegerToken(it.groupValues[1])} min"
            }
            .replace(Regex("""(?i)\b([0-9OQIlLAZG&]+)\s*h(?:oras?)?\b""")) {
                "${sanitizeIntegerToken(it.groupValues[1])} h"
            }

        line = applyJarTypeCorrections(line)
        line = currencyRegex.replace(line) { match ->
            val amount = sanitizeDecimalToken(match.groupValues[1])
            "R$ $amount"
        }
        line = distanceRegex.replace(line) { match ->
            val amount = sanitizeDecimalToken(match.groupValues[1])
            val unit = normalizeDistanceUnit(match.groupValues[2])
            "$amount $unit"
        }
        line = compactTimeDistanceRegex.replace(line) { match ->
            val minutes = sanitizeIntegerToken(match.groupValues[1])
            val distance = sanitizeDecimalToken(match.groupValues[2])
            val unit = normalizeDistanceUnit(match.groupValues[3])
            "$minutes min ($distance $unit)"
        }

        return applyMissingLeadingDistanceDigit(
            line
            .replace(Regex("""\s+"""), " ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(" km)", "km)")
            .replace(" m)", "m)")
            .trim()
        )
    }

    fun shouldIgnoreOcrLine(rawLine: String): Boolean {
        val line = TextNormalizer.searchKey(rawLine)
        if (line.length < 2) return true
        if (line == "eor") return true
        if (ignoredMarkers.any(line::contains)) return true
        val fare = currencyRegex.find(rawLine)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::sanitizeDecimalToken)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
        return fare != null && fare < MINIMUM_REFERENCE_FARE
    }

    private fun applyJarTypeCorrections(value: String): String {
        val normalized = TextNormalizer.searchKey(value)
        return when {
            noteTriggers.any(normalized::contains) -> sanitizeRatingLine(value)
            normalized.contains("km") || normalized.endsWith("m)") -> sanitizeMetricCharacters(value)
            value.firstOrNull()?.isDigit() == true -> sanitizeNumericLine(value)
            normalized.contains("min") -> sanitizeMetricCharacters(value)
            else -> value
        }
    }

    private fun sanitizeRatingLine(value: String): String {
        val token = ratingTokenRegex.find(value)?.value ?: return value
        val numeric = sanitizeNumericCharacters(token)
            .replace('.', ',')
            .replace(':', ',')
            .replace('-', ' ')
            .filter { it.isDigit() || it == ',' }
        val rating = when {
            numeric.length == 3 && ',' !in numeric -> "${numeric[0]},${numeric.substring(1)}"
            else -> numeric
        }
        return value.replaceRange(
            ratingTokenRegex.find(value)!!.range,
            rating
        )
    }

    private fun sanitizeMetricCharacters(value: String): String {
        return value
            .replace('.', ',')
            .replace(':', ',')
            .replace('L', '1')
            .replace('I', '1')
            .replace('A', '4')
            .replace('O', '0')
            .replace('G', '6')
            .replace('&', '8')
            .replace('Z', '7')
            .replace(" km)", "km)")
            .replace(" m)", "m)")
    }

    private fun sanitizeNumericLine(value: String): String {
        return value
            .replace('.', ',')
            .replace('l', '1')
            .replace('L', '1')
            .replace('I', '1')
            .replace('A', '4')
            .replace('O', '0')
            .replace('G', '6')
            .replace('&', '8')
            .replace('Z', '7')
    }

    private fun applyMissingLeadingDistanceDigit(value: String): String {
        val match = missingLeadingDistanceRegex.find(value) ?: return value
        val minutes = match.groupValues[1].toIntOrNull() ?: return value
        val integerPart = match.groupValues[2]
        val fractionPart = match.groupValues[3]
        val distance = "$integerPart.$fractionPart".toDoubleOrNull() ?: return value
        if (integerPart.length != 1 || distance >= 1.0 || minutes / distance <= 25.0) return value

        val correctedDistance = "1$integerPart,$fractionPart"
        return value.replaceRange(
            match.groups[2]!!.range.first..match.groups[3]!!.range.last,
            correctedDistance
        )
    }

    private fun sanitizeIntegerToken(value: String): String {
        return sanitizeNumericCharacters(value)
            .filter(Char::isDigit)
            .ifBlank { value }
    }

    private fun sanitizeDecimalToken(value: String): String {
        val normalized = sanitizeNumericCharacters(value)
            .replace(':', ',')
            .replace(';', ',')
            .replace(Regex("""[.](?=\d{1,2}$)"""), ",")
            .replace(Regex("""\s+"""), "")
        val separatorIndex = normalized.indexOfLast { it == ',' || it == '.' }
        if (separatorIndex < 0) return normalized.filter(Char::isDigit)

        val integer = normalized.substring(0, separatorIndex).filter(Char::isDigit)
        val fraction = normalized.substring(separatorIndex + 1).filter(Char::isDigit)
        return when {
            integer.isBlank() -> "0,${fraction.take(2)}"
            fraction.isBlank() -> integer
            else -> "$integer,${fraction.take(2)}"
        }
    }

    private fun sanitizeNumericCharacters(value: String): String {
        return value.uppercase(Locale.ROOT)
            .map { character ->
                when (character) {
                    'O', 'Q' -> '0'
                    'I', 'L' -> '1'
                    'Z' -> '7'
                    'A' -> '4'
                    'G' -> '6'
                    '&' -> '8'
                    else -> character
                }
            }
            .joinToString("")
    }

    private fun normalizeDistanceUnit(value: String): String {
        val normalized = value.lowercase(Locale.ROOT)
        return if (normalized.startsWith("k")) "km" else "m"
    }

    private val currencyRegex =
        Regex("""(?i)R\$\s*([0-9OQIlLAZG&]+(?:[.,:;][0-9OQIlLAZG&]{1,2})?)""")
    private val distanceRegex =
        Regex("""(?i)\b([0-9OQIlLAZG&]+(?:[.,:;][0-9OQIlLAZG&]{1,3})?)\s*(k[mn]|krn|km|rn|m)\b""")
    private val compactTimeDistanceRegex =
        Regex("""(?i)\b([0-9OQIlLAZG&]+)\s*(?:min|mnin|rnin)\s*\(\s*([0-9OQIlLAZG&]+(?:[.,:;][0-9OQIlLAZG&]{1,3})?)\s*(k[mn]|krn|km|rn|m)\s*\)""")
    private val ignoredMarkers = setOf(
        "conclua",
        "ganhe",
        "/km",
        "preco",
        "preco por km",
        "preco/km",
        "inclu",
        "tarifa",
        "taxa",
        "dinamica",
        "inclusa",
        "deslo"
    )
    private val noteTriggers = setOf(
        "corr",
        "perfil",
        "premium",
        "cpf",
        "cartao",
        "verif.",
        "essencial",
        "passage"
    )
    private val ratingTokenRegex =
        Regex("""(?i)[0-5OQIlLAZG&]?\s*[0-9OQIlLAZG&]{1,3}(?:[.,:][0-9OQIlLAZG&]{1,2})?""")
    private val missingLeadingDistanceRegex =
        Regex("""(?i)\b(\d+)\s*min\s*\(\s*(\d),(\d)\s*km\s*\)""")
    private const val MINIMUM_REFERENCE_FARE = 4.0
}
