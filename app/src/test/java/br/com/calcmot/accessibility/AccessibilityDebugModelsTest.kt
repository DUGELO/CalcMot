package br.com.calcmot.accessibility

import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.TreeOfferInspection
import br.com.calcmot.processor.TreeRejectionReason
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityDebugModelsTest {

    @Test
    fun `classifies empty root and parser failure distinctly`() {
        assertEquals(
            AccessibilityFailureCategory.ROOT_NULL,
            AccessibilityFailureClassifier.classify(
                serviceActive = true,
                uberForeground = true,
                rootNull = true,
                windowCount = 1,
                nodeCount = 0,
                textNodeCount = 0,
                contentDescriptionNodeCount = 0,
                inspection = null,
                parserSucceeded = false
            )
        )

        assertEquals(
            AccessibilityFailureCategory.TREE_HAS_DATA_PARSER_FAILED,
            AccessibilityFailureClassifier.classify(
                serviceActive = true,
                uberForeground = true,
                rootNull = false,
                windowCount = 1,
                nodeCount = 30,
                textNodeCount = 4,
                contentDescriptionNodeCount = 12,
                inspection = inspection(TreeRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS),
                parserSucceeded = false
            )
        )
    }

    @Test
    fun `timing summary selects useful delay and exposes overlay state`() {
        val summary = TimingSweepSummary(
            generationId = 7,
            observations = listOf(
                observation(delayMs = 0, nodes = 0, desc = 0, candidate = false),
                observation(delayMs = 150, nodes = 80, desc = 30, candidate = false),
                observation(delayMs = 300, nodes = 100, desc = 40, candidate = true)
            )
        )

        assertEquals(300L, summary.bestDelayMs)
        assertEquals(300L, summary.firstValidCandidateDelayMs)

        val overlayState = summary.toOverlayState(
            serviceActive = true,
            uberForeground = true,
            lastEventType = "TYPE_WINDOW_CONTENT_CHANGED"
        )

        assertEquals(true, overlayState.serviceActive)
        assertEquals(true, overlayState.uberForeground)
        assertEquals(100, overlayState.nodesScanned)
        assertEquals(40, overlayState.contentDescriptionNodeCount)
        assertEquals(1, overlayState.candidateCount)
    }

    private fun observation(
        delayMs: Long,
        nodes: Int,
        desc: Int,
        candidate: Boolean
    ): TimingSweepObservation {
        return TimingSweepObservation(
            generationId = 7,
            delayMs = delayMs,
            scanId = "scan-$delayMs",
            rootNull = nodes == 0,
            windowCount = 1,
            snapshot = AccessibilityTreeSnapshot(
                sourceName = "scan-$delayMs",
                capturedAtMillis = delayMs,
                eventAtMillis = 0,
                screenWidth = 720,
                screenHeight = 1600,
                windowCount = 1,
                rootCount = 1,
                nodeCount = nodes,
                rootPackageName = "com.ubercab.driver",
                rootClassName = "root",
                lines = emptyList(),
                nodes = emptyList()
            ).copy(
                // The summary uses nodeCount directly when node snapshots are absent.
                // This mirrors root/window-null observations in field logs.
            ),
            inspection = inspection(if (candidate) null else TreeRejectionReason.NO_PRICE),
            candidateParsed = candidate,
            failureCategory = if (candidate) AccessibilityFailureCategory.UNKNOWN else AccessibilityFailureCategory.TREE_EMPTY
        ).let {
            if (nodes == 0) {
                it
            } else {
                it.copy(snapshot = it.snapshot?.copy(nodes = List(desc) { index ->
                    br.com.calcmot.processor.AccessibleNodeSnapshot(
                        snapshotId = index,
                        parentSnapshotId = null,
                        depth = 0,
                        windowId = 1,
                        packageName = "com.ubercab.driver",
                        className = "android.widget.TextView",
                        textRaw = if (index == 0) "R$ 7" else null,
                        textNormalized = null,
                        contentDescriptionRaw = "desc-$index",
                        contentDescriptionNormalized = null,
                        viewIdResourceName = null,
                        boundsInScreen = br.com.calcmot.processor.ScreenBounds(0, 0, 1, 1),
                        visibleToUser = true,
                        clickable = false,
                        enabled = true,
                        focused = false,
                        selected = false,
                        childCount = 0,
                        timestamp = 0
                    )
                }))
            }
        }
    }

    private fun inspection(reason: TreeRejectionReason?): TreeOfferInspection {
        return TreeOfferInspection(
            sourceName = "test",
            elapsedSinceEventMs = 0,
            nodeCount = 1,
            lineCount = 1,
            hasPrice = reason == null,
            hasActionButton = reason == null,
            timeDistanceBlockCount = if (reason == null) 2 else 0,
            isCompleteOffer = reason == null,
            offerText = if (reason == null) "R$ 7\n2 minutos (0.5 km)\nViagem de 4 minutos (1.1 km)" else null,
            rejectionReason = reason
        )
    }
}
