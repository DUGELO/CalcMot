package br.com.calcmot.processor

import br.com.calcmot.DriverApp
import br.com.calcmot.DriverAppPackagePolicy

object DriverOfferTreeExtractor {

    fun inspect(
        snapshot: AccessibilityTreeSnapshot,
        includeAuditFields: Boolean = true,
        metric: ((name: String, durationMs: Long, details: String) -> Unit)? = null
    ): TreeOfferInspection {
        return when (snapshot.driverApp) {
            DriverApp.UBER -> OfferTreeExtractor.inspect(snapshot, includeAuditFields, metric)
            DriverApp.NINETY_NINE -> NinetyNineOfferTreeExtractor.inspect(snapshot)
            DriverApp.UNKNOWN -> TreeOfferInspection(
                sourceName = snapshot.sourceName,
                elapsedSinceEventMs = snapshot.elapsedSinceEventMs,
                nodeCount = snapshot.nodeCount,
                lineCount = snapshot.lines.size,
                hasPrice = false,
                hasActionButton = false,
                timeDistanceBlockCount = 0,
                isCompleteOffer = false,
                offerText = null,
                rejectionReason = TreeRejectionReason.EMPTY_TREE
            )
        }
    }
}

object NinetyNineOfferTreeExtractor {

    fun inspect(snapshot: AccessibilityTreeSnapshot): TreeOfferInspection {
        val lines = snapshot.lines
            .asSequence()
            .filter { it.visibleToUser }
            .filter { it.source != AccessibleTextSource.VIEW_ID_RESOURCE_NAME }
            .filter {
                val packageName = it.packageName ?: snapshot.rootPackageName
                DriverAppPackagePolicy.driverAppForPackage(packageName) == DriverApp.NINETY_NINE
            }
            .mapNotNull { line ->
                TextNormalizer.clean(line.text).takeIf(String::isNotBlank)
            }
            .distinct()
            .toList()
        val allText = lines.joinToString("\n")
        val normalized = TextNormalizer.searchKey(allText)
        val invalidContext = detectInvalidContext(normalized)
        val hasPrice = FarePriceExtractor.containsPrimaryFare(allText)
        val hasAction = DriverApp.NINETY_NINE.actionLabels.any(normalized::contains)
        val blockCount = timeDistanceRegex.findAll(normalized).count()

        if (invalidContext?.isStrong == true) {
            return inspection(
                snapshot = snapshot,
                lineCount = lines.size,
                hasPrice = hasPrice,
                hasAction = hasAction,
                blockCount = blockCount,
                rejectionReason = invalidContext.reason
            )
        }

        val candidate = DriverOfferParser.parse(DriverApp.NINETY_NINE, allText)
        return if (candidate != null) {
            inspection(
                snapshot = snapshot,
                lineCount = lines.size,
                hasPrice = true,
                hasAction = hasAction,
                blockCount = blockCount,
                offerText = allText
            )
        } else {
            inspection(
                snapshot = snapshot,
                lineCount = lines.size,
                hasPrice = hasPrice,
                hasAction = hasAction,
                blockCount = blockCount,
                rejectionReason = invalidContext?.reason ?: when {
                    lines.isEmpty() -> TreeRejectionReason.EMPTY_TREE
                    !hasPrice -> TreeRejectionReason.NO_PRICE
                    blockCount < 2 -> TreeRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS
                    else -> TreeRejectionReason.PARSER_REJECTED
                }
            )
        }
    }

    private fun inspection(
        snapshot: AccessibilityTreeSnapshot,
        lineCount: Int,
        hasPrice: Boolean,
        hasAction: Boolean,
        blockCount: Int,
        offerText: String? = null,
        rejectionReason: TreeRejectionReason? = null
    ): TreeOfferInspection {
        return TreeOfferInspection(
            sourceName = snapshot.sourceName,
            elapsedSinceEventMs = snapshot.elapsedSinceEventMs,
            nodeCount = snapshot.nodeCount,
            lineCount = lineCount,
            hasPrice = hasPrice,
            hasActionButton = hasAction,
            timeDistanceBlockCount = blockCount,
            isCompleteOffer = offerText != null,
            offerText = offerText,
            rejectionReason = rejectionReason
        )
    }

    private fun detectInvalidContext(text: String): InvalidContext? {
        return when {
            strongInvalidMarkers.any(text::contains) -> {
                InvalidContext(TreeRejectionReason.INVALID_CONTEXT_REQUEST_UNAVAILABLE, isStrong = true)
            }
            text.contains("voce esta offline") || text.contains("conecte-se para aceitar corridas") -> {
                InvalidContext(TreeRejectionReason.INVALID_CONTEXT_OFFLINE, isStrong = false)
            }
            text.contains("buscando") || text.contains("procurando chamadas") -> {
                InvalidContext(TreeRejectionReason.INVALID_CONTEXT_NO_REQUEST, isStrong = false)
            }
            else -> null
        }
    }

    private data class InvalidContext(
        val reason: TreeRejectionReason,
        val isStrong: Boolean
    )

    private val strongInvalidMarkers = setOf(
        "corrida nao esta mais disponivel",
        "chamada nao esta mais disponivel",
        "outro motorista aceitou",
        "oferta expirada",
        "solicitacao expirada"
    )
    private val timeDistanceRegex =
        Regex("""\b[0-9]{1,3}\s*(?:h|hora(?:s)?|min|minuto(?:s)?)\b.{0,80}\b[0-9]+(?:[.,][0-9]+)?\s*km\b""")
}
