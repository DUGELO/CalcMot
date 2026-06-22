package br.com.calcmot.processor

import br.com.calcmot.DriverApp
import br.com.calcmot.DriverAppPackagePolicy

data class AccessibilityTreeSnapshot(
    val sourceName: String,
    val capturedAtMillis: Long,
    val eventAtMillis: Long,
    val screenWidth: Int,
    val screenHeight: Int,
    val windowCount: Int,
    val rootCount: Int,
    val nodeCount: Int,
    val rootPackageName: String?,
    val rootClassName: String?,
    val lines: List<AccessibleLine>,
    val driverApp: DriverApp = DriverAppPackagePolicy.driverAppForPackage(rootPackageName),
    val nodes: List<AccessibleNodeSnapshot> = emptyList(),
    val generationId: Long = 0L,
    val scanId: String = sourceName,
    val sourceKind: AccessibilitySnapshotSourceKind = AccessibilitySnapshotSourceKind.UNKNOWN,
    val delayMs: Long = elapsedSinceEventFallback(capturedAtMillis, eventAtMillis),
    val truncated: Boolean = false,
    val maxDepthReached: Int = nodes.maxOfOrNull { it.depth } ?: 0
) {
    val elapsedSinceEventMs: Long
        get() = (capturedAtMillis - eventAtMillis).coerceAtLeast(0L)

    companion object {
        private fun elapsedSinceEventFallback(capturedAtMillis: Long, eventAtMillis: Long): Long {
            return (capturedAtMillis - eventAtMillis).coerceAtLeast(0L)
        }
    }
}

data class AccessibleNodeSnapshot(
    val snapshotId: Int,
    val parentSnapshotId: Int?,
    val generationId: Long = 0L,
    val sourceKind: AccessibilitySnapshotSourceKind = AccessibilitySnapshotSourceKind.UNKNOWN,
    val path: String = "",
    val depth: Int,
    val indexInParent: Int = 0,
    val windowId: Int?,
    val packageName: String?,
    val className: String?,
    val textRaw: String?,
    val textNormalized: String?,
    val contentDescriptionRaw: String?,
    val contentDescriptionNormalized: String?,
    val stateDescriptionRaw: String? = null,
    val paneTitleRaw: String? = null,
    val hintTextRaw: String? = null,
    val tooltipTextRaw: String? = null,
    val extrasSummary: String? = null,
    val viewIdResourceName: String?,
    val boundsInScreen: ScreenBounds,
    val visibleToUser: Boolean,
    val clickable: Boolean,
    val enabled: Boolean,
    val focused: Boolean,
    val selected: Boolean,
    val childCount: Int,
    val timestamp: Long,
    val timestampMs: Long = timestamp
)

data class AccessibleLine(
    val text: String,
    val bounds: ScreenBounds,
    val packageName: String? = null,
    val className: String? = null,
    val viewId: String? = null,
    val depth: Int = 0,
    val source: AccessibleTextSource = AccessibleTextSource.TEXT,
    val visibleToUser: Boolean = true,
    val clickable: Boolean = false,
    val enabled: Boolean = true,
    val focused: Boolean = false,
    val selected: Boolean = false,
    val childCount: Int = 0,
    val nodeSnapshotId: Int? = null,
    val parentSnapshotId: Int? = null
)

enum class AccessibleTextSource {
    TEXT,
    CONTENT_DESCRIPTION,
    STATE_DESCRIPTION,
    PANE_TITLE,
    TOOLTIP_TEXT,
    HINT_TEXT,
    EXTRAS,
    VIEW_ID_RESOURCE_NAME,
    SIBLING_CONTEXT,
    PARENT_CONTEXT,
    WINDOW_SCAN,
    REFRESHED_TREE
}

enum class AccessibilitySnapshotSourceKind {
    EVENT_PAYLOAD,
    EVENT_SOURCE,
    ROOT_IN_ACTIVE_WINDOW,
    WINDOW_ROOT,
    COMBINED_WINDOWS,
    UNKNOWN
}

enum class OfferFieldType {
    FARE,
    PICKUP_TIME_DISTANCE,
    TRIP_TIME_DISTANCE,
    ACTION_BUTTON,
    SERVICE_CATEGORY,
    UNKNOWN
}

data class FieldCandidate(
    val fieldType: OfferFieldType,
    val rawValue: String,
    val normalizedValue: String,
    val source: AccessibleTextSource,
    val nodeSnapshotId: Int?,
    val viewIdResourceName: String?,
    val bounds: ScreenBounds,
    val confidence: Double,
    val evidence: String
)

data class KnownNodeMapping(
    val viewIdResourceName: String,
    val expectedFieldType: OfferFieldType,
    val confidenceBoost: Double,
    val notes: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long
)

data class ScreenBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerY: Int
        get() = (top + bottom) / 2

    val height: Int
        get() = bottom - top

    val width: Int
        get() = right - left
}
