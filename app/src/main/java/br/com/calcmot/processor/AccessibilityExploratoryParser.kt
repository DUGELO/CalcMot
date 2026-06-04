package br.com.calcmot.processor

data class ExploratoryFieldHit(
    val type: ExploratoryFieldType,
    val rawValue: String,
    val source: AccessibleTextSource,
    val nodeSnapshotId: Int?,
    val viewIdResourceName: String?,
    val bounds: ScreenBounds
)

enum class ExploratoryFieldType {
    PRICE,
    DISTANCE,
    DURATION,
    PLACE_CONTEXT,
    SERVICE_TYPE,
    DYNAMIC_OR_MULTIPLIER
}

object AccessibilityExploratoryParser {
    fun inspect(snapshot: AccessibilityTreeSnapshot): List<ExploratoryFieldHit> {
        return snapshot.lines.flatMap { line ->
            val hits = mutableListOf<ExploratoryFieldHit>()
            val text = TextNormalizer.clean(line.text)

            priceRegex.findAll(text).forEach { match ->
                hits += line.hit(ExploratoryFieldType.PRICE, match.value)
            }
            distanceRegex.findAll(text).forEach { match ->
                hits += line.hit(ExploratoryFieldType.DISTANCE, match.value)
            }
            durationRegex.findAll(text).forEach { match ->
                hits += line.hit(ExploratoryFieldType.DURATION, match.value)
            }
            if (serviceRegex.containsMatchIn(text)) {
                hits += line.hit(ExploratoryFieldType.SERVICE_TYPE, text)
            }
            if (dynamicRegex.containsMatchIn(text)) {
                hits += line.hit(ExploratoryFieldType.DYNAMIC_OR_MULTIPLIER, text)
            }
            if (placeContextRegex.containsMatchIn(text)) {
                hits += line.hit(ExploratoryFieldType.PLACE_CONTEXT, text)
            }

            hits
        }.distinctBy {
            "${it.type}|${TextNormalizer.searchKey(it.rawValue)}|${it.bounds.left}|${it.bounds.top}|${it.bounds.right}|${it.bounds.bottom}"
        }
    }

    private fun AccessibleLine.hit(type: ExploratoryFieldType, rawValue: String): ExploratoryFieldHit {
        return ExploratoryFieldHit(
            type = type,
            rawValue = rawValue,
            source = source,
            nodeSnapshotId = nodeSnapshotId,
            viewIdResourceName = viewId,
            bounds = bounds
        )
    }

    private val priceRegex = Regex("""(?i)(?:R\$\s*)?\b\d{1,3}(?:[.,]\d{2})\s*(?:reais|BRL)?\b|R\$\s*\d{1,3}\b""")
    private val distanceRegex = Regex("""(?i)\b\d+(?:[.,]\d+)?\s*(?:km|quil[oô]metros?|m|metros?)\b""")
    private val durationRegex = Regex("""(?i)\b\d{1,2}\s*h(?:ora(?:s)?)?(?:\s*e?\s*\d{1,2}\s*min(?:uto(?:s)?)?)?\b|\b\d{1,3}\s*min(?:uto(?:s)?)?\b""")
    private val serviceRegex = Regex("""(?i)\b(uberx|priority|comfort|black|flash|moto|exclusivo)\b""")
    private val dynamicRegex = Regex("""(?i)\b(din[aâ]mica|multiplicador|priority|\+\s*R\$)\b""")
    private val placeContextRegex = Regex("""(?i)\b(rua|avenida|av\.|r\.|quadra|q\.|jardim|setor|bairro|brasil|go|df)\b""")
}
