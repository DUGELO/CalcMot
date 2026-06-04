package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityExploratoryParserTest {

    @Test
    fun `finds values in long content description`() {
        val snapshot = snapshot(
            line = AccessibleLine(
                text = "UberX exclusivo R$ 21,67, 8 minutos (2.4 km) de distância, viagem de 21 minutos (16.7 km), destino Ceilândia Brasília DF",
                bounds = bounds(),
                source = AccessibleTextSource.CONTENT_DESCRIPTION,
                nodeSnapshotId = 10,
                viewId = "com.ubercab.driver:id/offer_card"
            )
        )

        val hits = AccessibilityExploratoryParser.inspect(snapshot)

        assertTrue(hits.any { it.type == ExploratoryFieldType.PRICE && it.rawValue.contains("21,67") })
        assertTrue(hits.any { it.type == ExploratoryFieldType.DISTANCE && it.rawValue.contains("2.4 km") })
        assertTrue(hits.any { it.type == ExploratoryFieldType.DURATION && it.rawValue.contains("8 minutos") })
        assertTrue(hits.any { it.type == ExploratoryFieldType.SERVICE_TYPE })
        assertTrue(hits.any { it.type == ExploratoryFieldType.PLACE_CONTEXT })
    }

    @Test
    fun `content description remains first class even when text is null`() {
        val snapshot = AccessibilityTreeSnapshot(
            sourceName = "test",
            capturedAtMillis = 100,
            eventAtMillis = 0,
            screenWidth = 720,
            screenHeight = 1600,
            windowCount = 1,
            rootCount = 1,
            nodeCount = 1,
            rootPackageName = "com.ubercab.driver",
            rootClassName = "android.view.View",
            lines = listOf(
                AccessibleLine(
                    text = "R$ 12,34",
                    bounds = bounds(),
                    source = AccessibleTextSource.CONTENT_DESCRIPTION,
                    nodeSnapshotId = 1
                )
            ),
            nodes = listOf(
                AccessibleNodeSnapshot(
                    snapshotId = 1,
                    parentSnapshotId = null,
                    depth = 0,
                    windowId = 1,
                    packageName = "com.ubercab.driver",
                    className = "android.widget.TextView",
                    textRaw = null,
                    textNormalized = null,
                    contentDescriptionRaw = "R$ 12,34",
                    contentDescriptionNormalized = "R$ 12,34",
                    viewIdResourceName = null,
                    boundsInScreen = bounds(),
                    visibleToUser = true,
                    clickable = false,
                    enabled = true,
                    focused = false,
                    selected = false,
                    childCount = 0,
                    timestamp = 100
                )
            )
        )

        val hits = AccessibilityExploratoryParser.inspect(snapshot)

        assertEquals(1, hits.count { it.type == ExploratoryFieldType.PRICE })
        assertEquals(AccessibleTextSource.CONTENT_DESCRIPTION, hits.first().source)
    }

    private fun snapshot(line: AccessibleLine): AccessibilityTreeSnapshot {
        return AccessibilityTreeSnapshot(
            sourceName = "test",
            capturedAtMillis = 100,
            eventAtMillis = 0,
            screenWidth = 720,
            screenHeight = 1600,
            windowCount = 1,
            rootCount = 1,
            nodeCount = 1,
            rootPackageName = "com.ubercab.driver",
            rootClassName = "android.view.View",
            lines = listOf(line)
        )
    }

    private fun bounds(): ScreenBounds = ScreenBounds(left = 10, top = 20, right = 300, bottom = 60)
}
