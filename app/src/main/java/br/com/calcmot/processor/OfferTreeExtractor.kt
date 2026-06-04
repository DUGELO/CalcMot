package br.com.calcmot.processor

object OfferTreeExtractor {

    fun extractOfferText(snapshot: AccessibilityTreeSnapshot): String? {
        return inspect(snapshot).offerText
    }

    fun inspect(snapshot: AccessibilityTreeSnapshot): TreeOfferInspection {
        val lines = snapshot.semanticLines()

        if (lines.isEmpty()) {
            return snapshot.inspection(lines, rejectionReason = TreeRejectionReason.EMPTY_TREE)
        }

        val buttonLine = lines.lastOrNull { it.looksLikeActionButton() }
        val tripBlocks = extractTripBlocks(lines, snapshot.screenHeight)
        val priceLines = lines.filter { it.looksLikeOfferFareLine(snapshot.screenHeight) }

        if (priceLines.isEmpty()) {
            return snapshot.inspection(
                lines = lines,
                hasActionButton = buttonLine != null,
                timeDistanceBlockCount = tripBlocks.size,
                rejectionReason = TreeRejectionReason.NO_PRICE
            )
        }

        val priceLine = selectPrimaryPriceLine(priceLines, tripBlocks)
        if (priceLine == null) {
            return snapshot.inspection(
                lines = lines,
                hasPrice = true,
                hasActionButton = buttonLine != null,
                timeDistanceBlockCount = tripBlocks.size,
                rejectionReason = TreeRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS
            )
        }

        if (buttonLine != null && priceLine.bounds.centerY >= buttonLine.bounds.centerY) {
            return snapshot.inspection(
                lines = lines,
                hasPrice = true,
                hasActionButton = true,
                timeDistanceBlockCount = tripBlocks.size,
                rejectionReason = TreeRejectionReason.PRICE_AFTER_BUTTON
            )
        }

        val cardBottom = buttonLine?.bounds?.centerY ?: snapshot.screenHeight
        val cardTripBlocks = tripBlocks
            .filter {
                it.bounds.centerY > priceLine.bounds.centerY &&
                    it.bounds.centerY < cardBottom
            }
            .take(REQUIRED_TRIP_BLOCKS)

        val cardLines = lines.filter {
            it.bounds.centerY >= priceLine.bounds.centerY &&
                it.bounds.centerY <= (buttonLine?.bounds?.centerY ?: cardTripBlocks.lastOrNull()?.bounds?.bottom ?: cardBottom)
        }
        if (cardLines.size < MIN_CARD_LINES_WITHOUT_BUTTON || mapNoiseDominates(cardLines)) {
            return snapshot.inspection(
                lines = lines,
                hasPrice = true,
                hasActionButton = buttonLine != null,
                timeDistanceBlockCount = tripBlocks.size,
                rejectionReason = TreeRejectionReason.NOT_CARD_LIKE
            )
        }

        if (cardTripBlocks.size != REQUIRED_TRIP_BLOCKS) {
            return snapshot.inspection(
                lines = lines,
                hasPrice = true,
                hasActionButton = buttonLine != null,
                timeDistanceBlockCount = cardTripBlocks.size,
                rejectionReason = TreeRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS
            )
        }

        val orderedBlocks = cardTripBlocks.sortedBy { it.bounds.centerY }
        if (!isVerticalOrderValid(priceLine, orderedBlocks[0], orderedBlocks[1], buttonLine)) {
            return snapshot.inspection(
                lines = lines,
                hasPrice = true,
                hasActionButton = buttonLine != null,
                timeDistanceBlockCount = cardTripBlocks.size,
                rejectionReason = TreeRejectionReason.INVALID_VERTICAL_ORDER
            )
        }

        val offerText = buildOfferText(priceLine, orderedBlocks)
        if (OfferParser.parse(offerText) == null) {
            return snapshot.inspection(
                lines = lines,
                hasPrice = true,
                hasActionButton = buttonLine != null,
                timeDistanceBlockCount = cardTripBlocks.size,
                rejectionReason = TreeRejectionReason.PARSER_REJECTED
            )
        }

        return snapshot.inspection(
            lines = lines,
            hasPrice = true,
            hasActionButton = buttonLine != null,
            timeDistanceBlockCount = cardTripBlocks.size,
            offerText = offerText
        )
    }

    private fun AccessibilityTreeSnapshot.inspection(
        lines: List<AccessibleLine>,
        hasPrice: Boolean = false,
        hasActionButton: Boolean = false,
        timeDistanceBlockCount: Int = 0,
        offerText: String? = null,
        rejectionReason: TreeRejectionReason? = null
    ): TreeOfferInspection {
        val fieldCandidates = extractFieldCandidates(lines, screenHeight)
        return TreeOfferInspection(
            sourceName = sourceName,
            elapsedSinceEventMs = elapsedSinceEventMs,
            nodeCount = nodeCount,
            lineCount = lines.size,
            hasPrice = hasPrice,
            hasActionButton = hasActionButton,
            timeDistanceBlockCount = timeDistanceBlockCount,
            isCompleteOffer = offerText != null,
            offerText = offerText,
            rejectionReason = rejectionReason,
            fieldCandidates = fieldCandidates,
            knownNodeMappings = fieldCandidates.discoverKnownNodeMappings(capturedAtMillis)
        )
    }

    private fun AccessibilityTreeSnapshot.semanticLines(): List<AccessibleLine> {
        val baseLines = lines
            .filter { it.visibleToUser }
            .flatMap { line -> line.semanticFragments() }
            .mapNotNull { line ->
                val cleanText = TextNormalizer.clean(line.text)
                if (cleanText.isBlank() || line.bounds.width <= 0 || line.bounds.height <= 0) {
                    null
                } else {
                    line.copy(text = cleanText)
                }
            }
            .sortedWith(
                compareByDescending<AccessibleLine> { it.source.semanticPriority() }
                    .thenBy { it.bounds.top }
                    .thenBy { it.bounds.left }
            )
            .distinctBy { "${normalize(it.text)}|${it.bounds.left}|${it.bounds.top}|${it.bounds.right}|${it.bounds.bottom}" }
            .sortedWith(compareBy<AccessibleLine> { it.bounds.top }.thenBy { it.bounds.left })

        val groupedRows = baseLines
            .groupInlineFragments(screenWidth)
            .filter { it.text.isNotBlank() }

        return (baseLines + groupedRows)
            .distinctBy { "${normalize(it.text)}|${it.bounds.left}|${it.bounds.top}|${it.bounds.right}|${it.bounds.bottom}" }
            .sortedWith(compareBy<AccessibleLine> { it.bounds.top }.thenBy { it.bounds.left })
    }

    private fun AccessibleLine.semanticFragments(): List<AccessibleLine> {
        val cleanText = TextNormalizer.clean(text)
        if (source == AccessibleTextSource.VIEW_ID_RESOURCE_NAME) {
            return listOf(copy(text = cleanText))
        }

        val expanded = cleanText
            .replace(primaryFareSplitRegex, "\n$1\n")
            .replace(tripSplitRegex, "\n$1")
            .replace(actionSplitRegex, "\n$1\n")
        val fragments = expanded
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        if (fragments.size <= 1) return listOf(copy(text = cleanText))

        val fragmentHeight = maxOf(1, bounds.height / fragments.size)
        return fragments.mapIndexed { index, fragment ->
            val top = bounds.top + index * fragmentHeight
            copy(
                text = fragment,
                bounds = ScreenBounds(
                    left = bounds.left,
                    top = top,
                    right = bounds.right,
                    bottom = if (index == fragments.lastIndex) bounds.bottom else top + fragmentHeight
                )
            )
        }
    }

    private fun List<AccessibleLine>.groupInlineFragments(screenWidth: Int): List<AccessibleLine> {
        if (size < 2) return emptyList()

        val groups = mutableListOf<MutableList<AccessibleLine>>()
        forEach { line ->
            val group = groups.firstOrNull { existing ->
                existing.any { it.isSameVisualRow(line) }
            }
            if (group == null) {
                groups += mutableListOf(line)
            } else {
                group += line
            }
        }

        return groups
            .filter { it.size > 1 }
            .mapNotNull { group ->
                val ordered = group.sortedBy { it.bounds.left }
                val joinedText = ordered.joinToString(separator = " ") { it.text }
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                if (joinedText.isBlank()) return@mapNotNull null

                val bounds = ordered.unionBounds()
                val base = ordered.first()
                base.copy(
                    text = joinedText,
                    bounds = ScreenBounds(
                        left = bounds.left,
                        top = bounds.top,
                        right = if (screenWidth > 0 && bounds.right > screenWidth) screenWidth else bounds.right,
                        bottom = bounds.bottom
                    )
                )
            }
    }

    private fun AccessibleLine.isSameVisualRow(other: AccessibleLine): Boolean {
        val centerDelta = kotlin.math.abs(bounds.centerY - other.bounds.centerY)
        val verticalOverlap = minOf(bounds.bottom, other.bounds.bottom) - maxOf(bounds.top, other.bounds.top)
        val minHeight = minOf(bounds.height, other.bounds.height).coerceAtLeast(1)
        return centerDelta <= SAME_ROW_CENTER_TOLERANCE_PX ||
            verticalOverlap >= (minHeight * SAME_ROW_OVERLAP_RATIO).toInt()
    }

    private fun extractTripBlocks(lines: List<AccessibleLine>, screenHeight: Int): List<TreeTripBlock> {
        val candidateBlocks = mutableListOf<TreeTripBlock>()

        for (startIndex in lines.indices) {
            for (windowSize in 1..MAX_BLOCK_WINDOW_SIZE) {
                val endIndex = startIndex + windowSize - 1
                if (endIndex !in lines.indices) break

                val window = lines.subList(startIndex, endIndex + 1)
                if (windowSize > 1 && !window.hasCloseVerticalSpacing(screenHeight)) continue

                val text = window.joinToString(separator = " ") { it.text }
                if (!text.looksLikeTimeDistanceBlock()) continue
                if (windowSize > 1 && !window.hasReasonableBlockSpan(screenHeight)) continue

                candidateBlocks += TreeTripBlock(
                    startIndex = startIndex,
                    endIndex = endIndex,
                    text = text,
                    bounds = window.unionBounds()
                )
                break
            }
        }

        return candidateBlocks
            .sortedWith(compareBy<TreeTripBlock> { it.startIndex }.thenBy { it.endIndex - it.startIndex })
            .fold(mutableListOf()) { accepted, block ->
                if (accepted.isEmpty() || block.startIndex > accepted.last().endIndex) {
                    accepted += block
                }
                accepted
            }
    }

    private fun List<AccessibleLine>.hasCloseVerticalSpacing(screenHeight: Int): Boolean {
        val fallbackGap = MIN_GROUP_GAP_PX * 3
        val maxGap = if (screenHeight > 0) {
            maxOf(MIN_GROUP_GAP_PX, (screenHeight * MAX_GROUP_GAP_RATIO).toInt())
        } else {
            fallbackGap
        }
        return zipWithNext().all { (first, second) ->
            second.bounds.top - first.bounds.bottom <= maxGap
        }
    }

    private fun List<AccessibleLine>.hasReasonableBlockSpan(screenHeight: Int): Boolean {
        val maxSpan = if (screenHeight > 0) {
            (screenHeight * MAX_BLOCK_SPAN_RATIO).toInt()
        } else {
            MAX_BLOCK_SPAN_FALLBACK_PX
        }
        return unionBounds().height <= maxSpan
    }

    private fun List<AccessibleLine>.unionBounds(): ScreenBounds {
        return ScreenBounds(
            left = minOf { it.bounds.left },
            top = minOf { it.bounds.top },
            right = maxOf { it.bounds.right },
            bottom = maxOf { it.bounds.bottom }
        )
    }

    private fun String.looksLikeTimeDistanceBlock(): Boolean {
        val normalized = normalize(this)
        val durationMatch = durationMarkerRegex.find(normalized) ?: return false
        val distanceMatch = distanceRegex.find(normalized) ?: return false
        return DurationParser.parseMinutes(this) != null &&
            distanceMatch.range.first > durationMatch.range.first
    }

    private fun isVerticalOrderValid(
        priceLine: AccessibleLine,
        firstTripBlock: TreeTripBlock,
        secondTripBlock: TreeTripBlock,
        buttonLine: AccessibleLine?
    ): Boolean {
        val basicOrderValid = priceLine.bounds.centerY < firstTripBlock.bounds.centerY &&
            firstTripBlock.bounds.centerY < secondTripBlock.bounds.centerY &&
            (buttonLine == null || secondTripBlock.bounds.centerY < buttonLine.bounds.centerY)
        return basicOrderValid
    }

    private fun selectPrimaryPriceLine(
        priceLines: List<AccessibleLine>,
        tripBlocks: List<TreeTripBlock>
    ): AccessibleLine? {
        return priceLines.firstOrNull { priceLine ->
            tripBlocks.count { block -> block.bounds.centerY > priceLine.bounds.centerY } >= REQUIRED_TRIP_BLOCKS
        }
    }

    private fun buildOfferText(
        priceLine: AccessibleLine,
        orderedBlocks: List<TreeTripBlock>
    ): String {
        return buildList {
            add(priceLine.text)
            orderedBlocks.take(REQUIRED_TRIP_BLOCKS).forEach { add(it.text) }
        }.joinToString(separator = "\n")
    }

    private fun mapNoiseDominates(lines: List<AccessibleLine>): Boolean {
        val semanticLines = lines.count {
            it.looksLikeActionButton() ||
                FarePriceExtractor.containsPrimaryFare(it.text) ||
                it.text.looksLikeTimeDistanceBlock()
        }
        val roadNumberLines = lines.count { roadNumberRegex.matches(normalize(it.text)) }
        return roadNumberLines > semanticLines
    }

    private fun AccessibleLine.looksLikeActionButton(): Boolean {
        val text = normalize(text)
        return text.contains("aceitar") || text.contains("selecionar")
    }

    private fun AccessibleLine.looksLikeOfferFareLine(screenHeight: Int): Boolean {
        if (!FarePriceExtractor.containsPrimaryFare(text)) return false
        if (source == AccessibleTextSource.VIEW_ID_RESOURCE_NAME) return false
        if (packageName != null && packageName != UBER_DRIVER_PACKAGE) return false

        val id = viewId.orEmpty().lowercase()
        if (id.contains("earnings_tracker")) return false

        val normalized = normalize(text)
        if (normalized.contains(":id/") || normalized.contains(".id/")) return false
        val isTopZeroCounter = normalized == "r$ 0,00" &&
            screenHeight > 0 &&
            bounds.centerY < (screenHeight * TOP_COUNTER_MAX_CENTER_RATIO).toInt()
        return !isTopZeroCounter
    }

    private fun AccessibleLine.looksLikeServiceLabel(): Boolean {
        val text = normalize(text)
        return text.contains("uberx") ||
            text.contains("priority") ||
            text.contains("comfort") ||
            text.contains("black")
    }

    private fun extractFieldCandidates(lines: List<AccessibleLine>, screenHeight: Int): List<FieldCandidate> {
        return lines.mapNotNull { line ->
            val normalized = normalize(line.text)
            val fieldType = when {
                line.looksLikeOfferFareLine(screenHeight) -> OfferFieldType.FARE
                line.looksLikeActionButton() -> OfferFieldType.ACTION_BUTTON
                line.text.looksLikeTimeDistanceBlock() && normalized.contains("viagem") -> {
                    OfferFieldType.TRIP_TIME_DISTANCE
                }
                line.text.looksLikeTimeDistanceBlock() -> OfferFieldType.PICKUP_TIME_DISTANCE
                line.looksLikeServiceLabel() -> OfferFieldType.SERVICE_CATEGORY
                else -> return@mapNotNull null
            }

            FieldCandidate(
                fieldType = fieldType,
                rawValue = line.text,
                normalizedValue = normalized,
                source = line.source,
                nodeSnapshotId = line.nodeSnapshotId,
                viewIdResourceName = line.viewId,
                bounds = line.bounds,
                confidence = line.confidenceFor(fieldType),
                evidence = buildEvidence(line, fieldType)
            )
        }
    }

    private fun List<FieldCandidate>.discoverKnownNodeMappings(
        capturedAtMillis: Long
    ): List<KnownNodeMapping> {
        return mapNotNull { candidate ->
            val lineViewId = candidate.viewIdResourceName
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            KnownNodeMapping(
                viewIdResourceName = lineViewId,
                expectedFieldType = candidate.fieldType,
                confidenceBoost = VIEW_ID_CONFIDENCE_BOOST,
                notes = "Observed near ${candidate.fieldType.name.lowercase()} evidence",
                firstSeenAt = capturedAtMillis,
                lastSeenAt = capturedAtMillis
            )
        }.distinctBy { "${it.viewIdResourceName}|${it.expectedFieldType}" }
    }

    private fun AccessibleLine.confidenceFor(fieldType: OfferFieldType): Double {
        val sourceBoost = when (source) {
            AccessibleTextSource.CONTENT_DESCRIPTION -> 0.22
            AccessibleTextSource.STATE_DESCRIPTION,
            AccessibleTextSource.PANE_TITLE,
            AccessibleTextSource.TOOLTIP_TEXT,
            AccessibleTextSource.HINT_TEXT -> 0.16
            AccessibleTextSource.EXTRAS -> 0.12
            AccessibleTextSource.TEXT -> 0.10
            AccessibleTextSource.VIEW_ID_RESOURCE_NAME -> 0.05
            AccessibleTextSource.SIBLING_CONTEXT,
            AccessibleTextSource.PARENT_CONTEXT,
            AccessibleTextSource.WINDOW_SCAN,
            AccessibleTextSource.REFRESHED_TREE -> 0.08
        }
        val viewIdBoost = if (viewId.isNullOrBlank()) 0.0 else VIEW_ID_CONFIDENCE_BOOST
        val actionBoost = if (fieldType == OfferFieldType.ACTION_BUTTON && clickable) 0.12 else 0.0
        return (BASE_FIELD_CONFIDENCE + sourceBoost + viewIdBoost + actionBoost).coerceAtMost(0.99)
    }

    private fun buildEvidence(line: AccessibleLine, fieldType: OfferFieldType): String {
        return "field=$fieldType;source=${line.source};viewId=${line.viewId};" +
            "class=${line.className};depth=${line.depth};clickable=${line.clickable}"
    }

    private fun AccessibleTextSource.semanticPriority(): Int {
        return when (this) {
            AccessibleTextSource.CONTENT_DESCRIPTION -> 100
            AccessibleTextSource.STATE_DESCRIPTION -> 95
            AccessibleTextSource.PANE_TITLE -> 90
            AccessibleTextSource.TOOLTIP_TEXT -> 85
            AccessibleTextSource.HINT_TEXT -> 80
            AccessibleTextSource.EXTRAS -> 75
            AccessibleTextSource.TEXT -> 70
            AccessibleTextSource.SIBLING_CONTEXT -> 60
            AccessibleTextSource.PARENT_CONTEXT -> 55
            AccessibleTextSource.WINDOW_SCAN -> 50
            AccessibleTextSource.REFRESHED_TREE -> 45
            AccessibleTextSource.VIEW_ID_RESOURCE_NAME -> 20
        }
    }

    private fun normalize(value: String): String {
        return TextNormalizer.searchKey(value)
    }

    private data class TreeTripBlock(
        val startIndex: Int,
        val endIndex: Int,
        val text: String,
        val bounds: ScreenBounds
    )

    private val distanceRegex = Regex("""\b[0-9]+(?:[.,][0-9]+)?\s*km\b""")
    private val durationMarkerRegex = Regex("""\b[0-9]{1,3}\s*(?:h|hora(?:s)?|min|minuto(?:s)?)\b|[0-9]{1,2}h\s*[0-9]{1,2}""")
    private val roadNumberRegex = Regex("""^\d{2,4}$""")
    private val primaryFareSplitRegex = Regex("""(?i)(\+?\s*R\$\s*[0-9]+(?:[.,][0-9]{1,2})?)""")
    private val tripSplitRegex = Regex("""(?i)\b(Viagem\s+de\b)""")
    private val actionSplitRegex = Regex("""(?i)\b(Aceitar|Selecionar)\b""")
    private const val MIN_CARD_LINES_WITHOUT_BUTTON = 3
    private const val REQUIRED_TRIP_BLOCKS = 2
    private const val MAX_BLOCK_WINDOW_SIZE = 8
    private const val MIN_GROUP_GAP_PX = 24
    private const val MAX_GROUP_GAP_RATIO = 0.11
    private const val MAX_BLOCK_SPAN_RATIO = 0.22
    private const val MAX_BLOCK_SPAN_FALLBACK_PX = 320
    private const val SAME_ROW_CENTER_TOLERANCE_PX = 18
    private const val SAME_ROW_OVERLAP_RATIO = 0.55
    private const val BASE_FIELD_CONFIDENCE = 0.55
    private const val VIEW_ID_CONFIDENCE_BOOST = 0.10
    private const val UBER_DRIVER_PACKAGE = "com.ubercab.driver"
    private const val TOP_COUNTER_MAX_CENTER_RATIO = 0.18
}

data class TreeOfferInspection(
    val sourceName: String,
    val elapsedSinceEventMs: Long,
    val nodeCount: Int,
    val lineCount: Int,
    val hasPrice: Boolean,
    val hasActionButton: Boolean,
    val timeDistanceBlockCount: Int,
    val isCompleteOffer: Boolean,
    val offerText: String?,
    val rejectionReason: TreeRejectionReason?,
    val fieldCandidates: List<FieldCandidate> = emptyList(),
    val knownNodeMappings: List<KnownNodeMapping> = emptyList()
)

enum class TreeRejectionReason {
    EMPTY_TREE,
    NO_PRICE,
    MULTIPLE_PRIMARY_PRICES,
    NO_ACTION_BUTTON,
    PRICE_AFTER_BUTTON,
    NOT_CARD_LIKE,
    INCOMPLETE_TIME_DISTANCE_BLOCKS,
    INVALID_VERTICAL_ORDER,
    PARSER_REJECTED
}
