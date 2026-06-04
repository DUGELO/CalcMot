package br.com.calcmot.processor

object DurationParser {

    private val compactHourMinuteRegex = Regex("""\b([0-9]{1,2})h\s*([0-9]{1,2})\b""")
    private val hourRegex = Regex("""\b([0-9]{1,2})\s*(?:h|hora(?:s)?)\b(?:\s*e?\s*([0-9]{1,2})\s*(?:min|minuto(?:s)?))?""")
    private val minuteRegex = Regex("""\b([0-9]{1,3})\s*(?:min|minuto(?:s)?)\b""")

    fun parseMinutes(text: String): Int? {
        val normalized = normalize(text)

        compactHourMinuteRegex.find(normalized)?.let { match ->
            val hours = match.groupValues[1].toIntOrNull() ?: return null
            val minutes = match.groupValues[2].toIntOrNull() ?: return null
            return toTotalMinutes(hours, minutes)
        }

        hourRegex.find(normalized)?.let { match ->
            val hours = match.groupValues[1].toIntOrNull() ?: return null
            val minutes = match.groupValues.getOrNull(2)
                ?.takeIf { it.isNotBlank() }
                ?.toIntOrNull()
                ?: 0
            return toTotalMinutes(hours, minutes)
        }

        minuteRegex.find(normalized)?.let { match ->
            val minutes = match.groupValues[1].toIntOrNull() ?: return null
            return minutes.takeIf { it in 1..MAX_TOTAL_MINUTES }
        }

        return null
    }

    private fun toTotalMinutes(hours: Int, minutes: Int): Int? {
        if (hours !in 1..MAX_HOURS) return null
        if (minutes !in 0..59) return null
        val total = hours * 60 + minutes
        return total.takeIf { it in 1..MAX_TOTAL_MINUTES }
    }

    private fun normalize(value: String): String {
        return TextNormalizer.searchKey(value)
    }

    private const val MAX_HOURS = 12
    private const val MAX_TOTAL_MINUTES = 12 * 60
}
