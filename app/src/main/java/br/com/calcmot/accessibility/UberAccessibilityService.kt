package br.com.calcmot.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.com.calcmot.AppDiagnostics
import br.com.calcmot.AppSettings
import br.com.calcmot.BuildConfig
import br.com.calcmot.model.OfferCaptureOutcome
import br.com.calcmot.model.OfferCaptureRejectionReason
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.TripData
import br.com.calcmot.overlay.IOverlayManager
import br.com.calcmot.overlay.OverlayManager
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.AccessibilitySnapshotSourceKind
import br.com.calcmot.processor.AccessibleLine
import br.com.calcmot.processor.AccessibleNodeSnapshot
import br.com.calcmot.processor.AccessibleTextSource
import br.com.calcmot.processor.CaptureCoordinator
import br.com.calcmot.processor.CaptureDecision
import br.com.calcmot.processor.DebugTreeWalker
import br.com.calcmot.processor.OfferParser
import br.com.calcmot.processor.OfferTreeExtractor
import br.com.calcmot.processor.ScreenBounds
import br.com.calcmot.processor.TextNormalizer
import br.com.calcmot.processor.TreeOfferInspection
import br.com.calcmot.processor.TreeRejectionReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class UberAccessibilityService : AccessibilityService() {

    private var captureDispatcher = newCaptureDispatcher()
    private var serviceScope = CoroutineScope(SupervisorJob() + captureDispatcher)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun newCaptureDispatcher(): ExecutorCoroutineDispatcher {
        return Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "CalcMot-Capture").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1
            }
        }.asCoroutineDispatcher()
    }

    private fun ensureServiceScopeActive() {
        if (serviceScope.isActive) return
        captureDispatcher.close()
        captureDispatcher = newCaptureDispatcher()
        serviceScope = CoroutineScope(SupervisorJob() + captureDispatcher)
    }

    internal var overlayManager: IOverlayManager? = null
    private val captureCoordinator = CaptureCoordinator(
        requiredMatchingFrames = 2,
        requiredInvalidFramesToReset = REQUIRED_INVALID_FRAMES_TO_RESET
    )
    private var continuousAccessibilityPollingJob: Job? = null
    private var accessibilityPollingJob: Job? = null
    private var capturePipelineJob: Job? = null
    private var overlayExpiryJob: Job? = null
    private var runtimeAccessibilityInfoConfigured = false
    @Volatile
    private var accessibilityPollingUntil = 0L
    @Volatile
    private var accessibilityPollingEventAtMillis = 0L
    @Volatile
    private var captureGeneration = 0L
    @Volatile
    private var activeScanSessionGeneration = 0L
    @Volatile
    private var activeScanSessionStartedAtMillis = 0L
    private val accessibilityTreeLab by lazy { AccessibilityTreeLab(this) }
    private val captureLearningLab by lazy { CaptureLearningLab(this) }
    private val shellOfferHandler: (ShellOfferFrame) -> Unit = { frame ->
        serviceScope.launch { handleShellOfferFrame(frame) }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ensureServiceScopeActive()
        configureRuntimeAccessibilityInfo()
        overlayManager?.removeOverlay()
        overlayManager = OverlayManager(this).also { manager ->
            manager.setOnUserDismissed {
                handleOverlayDismissedByUser()
            }
        }
        ShellOfferBridge.register(shellOfferHandler)
        startContinuousAccessibilityPolling()
        if (AccessibilityDebugConfig.ENABLE_DEBUG_OVERLAY_HEARTBEAT) {
            showDebugHeartbeat(
                uberForeground = false,
                lastEventType = "service_connected",
                rootNull = rootInActiveWindow == null,
                windowCount = runCatching { windows.size }.getOrDefault(0),
                nodeCount = 0,
                textNodeCount = 0,
                contentDescriptionNodeCount = 0,
                candidateCount = 0,
                failureCategory = AccessibilityFailureCategory.NO_ACCESSIBILITY_EVENTS,
                bestDelayMs = null
            )
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "Service connected")
    }

    private fun configureRuntimeAccessibilityInfo() {
        if (runtimeAccessibilityInfoConfigured || runtimeAccessibilityInfoConfiguredOnce) return
        val currentInfo = serviceInfo ?: return
        currentInfo.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        currentInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        currentInfo.notificationTimeout = 0L
        currentInfo.flags = currentInfo.flags or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = currentInfo
        runtimeAccessibilityInfoConfigured = true
        runtimeAccessibilityInfoConfiguredOnce = true

        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "Runtime AccessibilityServiceInfo applied flags=${currentInfo.flags} " +
                    "eventTypes=${currentInfo.eventTypes}"
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName != UBER_DRIVER_PACKAGE) return
        if (!event.isRelevantUberEvent()) return

        val eventCopy = AccessibilityEvent.obtain(event)
        AppDiagnostics.recordEvent(this, event.eventType)
        val eventAtMillis = System.currentTimeMillis()
        if (AccessibilityDebugConfig.ENABLE_DEBUG_OVERLAY_HEARTBEAT) {
            showDebugHeartbeat(
                uberForeground = true,
                lastEventType = event.eventType.debugEventName(),
                rootNull = rootInActiveWindow == null,
                windowCount = runCatching { windows.size }.getOrDefault(0),
                nodeCount = 0,
                textNodeCount = 0,
                contentDescriptionNodeCount = 0,
                candidateCount = 0,
                failureCategory = AccessibilityFailureCategory.UNKNOWN,
                bestDelayMs = null
            )
        }

        if (!AppSettings.isMonitoringEnabled(this)) {
            AppDiagnostics.recordStage(this, AppDiagnostics.Stage.MONITORING_DISABLED)
            cancelCapturePipeline()
            cancelOverlayExpiry()
            serviceScope.launch {
                captureCoordinator.reset()
                mainScope.launch { overlayManager?.hideOverlay() }
                eventCopy.recycle()
            }
            return
        }

        val captureSession = beginOrCoalesceCaptureSession(eventAtMillis)
        val generation = captureSession.generation
        AppDiagnostics.recordStage(this, AppDiagnostics.Stage.TREE_SCAN_SCHEDULED)
        if (!captureSession.shouldStartBurst) {
            eventCopy.recycle()
            return
        }

        capturePipelineJob = serviceScope.launch {
            try {
                runCapturePipeline(eventCopy, eventAtMillis, generation)
            } finally {
                eventCopy.recycle()
            }
        }
    }

    private fun beginOrCoalesceCaptureSession(eventAtMillis: Long): CaptureSession {
        val now = System.currentTimeMillis()
        val canCoalesce = capturePipelineJob?.isActive == true &&
            activeScanSessionGeneration == captureGeneration &&
            now - activeScanSessionStartedAtMillis <= ACCESSIBILITY_SCAN_SESSION_COALESCE_MS

        if (canCoalesce) {
            accessibilityPollingEventAtMillis = eventAtMillis
            return CaptureSession(
                generation = activeScanSessionGeneration,
                shouldStartBurst = false
            )
        }

        capturePipelineJob?.cancel()
        captureGeneration += 1
        activeScanSessionGeneration = captureGeneration
        activeScanSessionStartedAtMillis = now
        return CaptureSession(
            generation = captureGeneration,
            shouldStartBurst = true
        )
    }

    private fun cancelCapturePipeline() {
        captureGeneration += 1
        activeScanSessionGeneration = 0L
        activeScanSessionStartedAtMillis = 0L
        accessibilityPollingJob?.cancel()
        accessibilityPollingJob = null
        capturePipelineJob?.cancel()
        capturePipelineJob = null
    }

    private fun isCurrentGeneration(generation: Long): Boolean {
        return generation == captureGeneration
    }

    private suspend fun runCapturePipeline(
        event: AccessibilityEvent,
        eventAtMillis: Long,
        generation: Long
    ) {
        coroutineScope {
            if (handleEventPayloadCandidate(event, eventAtMillis)) return@coroutineScope
            val handledByTree = handleAccessibilityCandidateBurst(eventAtMillis, generation)
            if (!handledByTree && isCurrentGeneration(generation)) {
                rejectCurrentFrame(
                    reason = "Accessibility tree did not expose a complete offer",
                    source = OfferCaptureSource.ACCESSIBILITY_TREE,
                    rejectionReason = OfferCaptureRejectionReason.NOT_CARD_LIKE
                )
            }
        }
        if (isCurrentGeneration(generation)) {
            activeScanSessionGeneration = 0L
            activeScanSessionStartedAtMillis = 0L
        }
    }

    private fun handleEventPayloadCandidate(event: AccessibilityEvent, eventAtMillis: Long): Boolean {
        val snapshot = event.toAccessibilitySnapshot(eventAtMillis)
        if (snapshot.lines.isEmpty()) return false

        val treeCandidate = extractCandidateFromSnapshot(snapshot)
        return treeCandidate != null && handleCandidate(
            candidate = treeCandidate.candidate,
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            label = "event-payload",
            trustedSingleFrame = treeCandidate.trustedSingleFrame
        )
    }

    private fun handleEventSourceCandidate(source: AccessibilityNodeInfo?, eventAtMillis: Long): Boolean {
        var node = source ?: return false
        repeat(EVENT_SOURCE_PARENT_ATTEMPTS) { parentIndex ->
            val snapshot = node.toAccessibilitySnapshot(
                sourceName = "event-source-parent-$parentIndex",
                eventAtMillis = eventAtMillis,
                windowCount = windows.size,
                rootCount = 1,
                generationId = captureGeneration,
                sourceKind = AccessibilitySnapshotSourceKind.EVENT_SOURCE
            )
            val treeCandidate = extractCandidateFromSnapshot(snapshot)
            if (treeCandidate != null) {
                return handleCandidate(
                    candidate = treeCandidate.candidate,
                    source = OfferCaptureSource.ACCESSIBILITY_TREE,
                    label = "event-source",
                    trustedSingleFrame = treeCandidate.trustedSingleFrame
                )
            }
            node = node.parent ?: return false
        }
        return false
    }

    private fun extendAccessibilityPolling(eventAtMillis: Long) {
        accessibilityPollingUntil = System.currentTimeMillis() + ACCESSIBILITY_POLL_WINDOW_MS
        accessibilityPollingEventAtMillis = eventAtMillis
        if (accessibilityPollingJob?.isActive == true) return

        accessibilityPollingJob = serviceScope.launch {
            while (System.currentTimeMillis() <= accessibilityPollingUntil) {
                if (!AppSettings.isMonitoringEnabled(this@UberAccessibilityService)) {
                    captureCoordinator.reset()
                    cancelOverlayExpiry()
                    mainScope.launch { overlayManager?.hideOverlay() }
                    return@launch
                }

                val treeCandidate = extractCandidateFromAccessibilityTree(
                    eventAtMillis = accessibilityPollingEventAtMillis,
                    logRejectedTree = false
                )
                if (treeCandidate != null) {
                    handleCandidate(
                        candidate = treeCandidate.candidate,
                        source = OfferCaptureSource.ACCESSIBILITY_TREE,
                        label = "accessibility-poll",
                        trustedSingleFrame = treeCandidate.trustedSingleFrame
                    )
                }

                delay(ACCESSIBILITY_POLL_INTERVAL_MS)
            }
        }
    }

    private fun startContinuousAccessibilityPolling() {
        if (continuousAccessibilityPollingJob?.isActive == true) return

        continuousAccessibilityPollingJob = serviceScope.launch {
            while (true) {
                delay(ACCESSIBILITY_HEARTBEAT_INTERVAL_MS)

                if (!AppSettings.isMonitoringEnabled(this@UberAccessibilityService)) {
                    captureCoordinator.reset()
                    cancelOverlayExpiry()
                    mainScope.launch { overlayManager?.hideOverlay() }
                    continue
                }

                captureCoordinator.expireOverlayIfStale()
                    ?.let { applyCaptureDecision(it, "overlay-heartbeat-ttl") }
            }
        }
    }

    private fun hasUberRootAvailable(): Boolean {
        if (rootInActiveWindow?.containsPackageName(UBER_DRIVER_PACKAGE) == true) return true

        return allInteractiveWindowsForScan().any { windowSource ->
            windowSource.root?.containsPackageName(UBER_DRIVER_PACKAGE) == true
        }
    }

    private suspend fun handleAccessibilityCandidateBurst(
        eventAtMillis: Long,
        generation: Long
    ): Boolean {
        var foundCandidate = false
        var previousDelay = 0L
        val delays = if (AccessibilityDebugConfig.ENABLE_TIMING_SWEEP) {
            AccessibilityDebugConfig.TIMING_SWEEP_DELAYS_MS
        } else {
            ACCESSIBILITY_BURST_DELAYS_MS
        }

        delays.forEachIndexed { attempt, targetDelay ->
            if (!isCurrentGeneration(generation)) return false
            delay(targetDelay - previousDelay)
            previousDelay = targetDelay

            val treeCandidate = extractCandidateFromAccessibilityTree(
                eventAtMillis = eventAtMillis,
                logRejectedTree = attempt == delays.lastIndex,
                generationId = generation,
                delayMs = targetDelay
            )
            if (treeCandidate != null) {
                foundCandidate = true
                if (BuildConfig.DEBUG) {
                    Log.i(
                        TAG,
                        "Accessibility tree produced offer generation=$generation " +
                            "delay=${targetDelay}ms attempt=${attempt + 1}/${delays.size}"
                    )
                }
                if (handleCandidate(
                        candidate = treeCandidate.candidate,
                        source = OfferCaptureSource.ACCESSIBILITY_TREE,
                        label = "accessibility-burst",
                        trustedSingleFrame = treeCandidate.trustedSingleFrame
                    )
                ) {
                    return true
                }
            }
        }

        return foundCandidate
    }

    private fun extractCandidateFromAccessibilityTree(
        eventAtMillis: Long,
        logRejectedTree: Boolean = true,
        generationId: Long = captureGeneration,
        delayMs: Long = (System.currentTimeMillis() - eventAtMillis).coerceAtLeast(0L)
    ): AccessibilityCandidate? {
        AppDiagnostics.recordStage(this, AppDiagnostics.Stage.TREE_SCAN_RUNNING)
        val calcMotOverlayRemovedForScan = removeOverlayWindowsBeforeScanIfNeeded()
        val rootNull = rootInActiveWindow == null
        val windowCount = runCatching { windows.size }.getOrDefault(0)
        val roots = accessibilityRootSources()
        if (roots.isEmpty()) {
            AppDiagnostics.recordStage(this, AppDiagnostics.Stage.CARD_UNCERTAIN)
            val emptySnapshot = emptyAccessibilitySnapshot(
                sourceName = "empty-roots",
                eventAtMillis = eventAtMillis,
                windowCount = windowCount,
                generationId = generationId,
                delayMs = delayMs
            )
            val emptyInspection = OfferTreeExtractor.inspect(emptySnapshot)
            AppDiagnostics.recordTreeInspection(this, emptySnapshot, emptyInspection)
            accessibilityTreeLab.record(emptySnapshot, emptyInspection)
            captureLearningLab.recordTreeInspection(emptySnapshot, emptyInspection)
            val failure = AccessibilityFailureClassifier.classify(
                serviceActive = true,
                uberForeground = true,
                rootNull = rootNull,
                windowCount = windowCount,
                nodeCount = 0,
                textNodeCount = 0,
                contentDescriptionNodeCount = 0,
                inspection = null,
                parserSucceeded = false
            )
            showDebugHeartbeat(
                uberForeground = true,
                lastEventType = "scan_delay_${delayMs}ms",
                rootNull = rootNull,
                windowCount = windowCount,
                nodeCount = 0,
                textNodeCount = 0,
                contentDescriptionNodeCount = 0,
                candidateCount = 0,
                failureCategory = failure,
                bestDelayMs = delayMs
            )
            if (BuildConfig.DEBUG) {
                Log.w(
                    TAG,
                    "Accessibility roots empty generation=$generationId delay=${delayMs}ms " +
                        "rootNull=$rootNull windows=$windowCount overlayRemovedForScan=$calcMotOverlayRemovedForScan. " +
                        "Check service enabled, Android restricted settings, " +
                        "Uber foreground package, timing, and rootInActiveWindow/window availability."
                )
            }
            return null
        }

        val rejectedReasons = mutableListOf<String>()
        val snapshots = roots.map { rootSource ->
            rootSource.root.toAccessibilitySnapshot(
                sourceName = rootSource.name,
                eventAtMillis = eventAtMillis,
                windowCount = rootSource.windowCount,
                rootCount = roots.size,
                generationId = generationId,
                sourceKind = rootSource.sourceKind
            )
        }
        val snapshotsToInspect = snapshots + listOfNotNull(snapshots.toCombinedSnapshotOrNull(eventAtMillis))

        snapshotsToInspect.forEach { snapshot ->
            AppDiagnostics.recordStage(this, AppDiagnostics.Stage.TREE_SNAPSHOT_CAPTURED)
            val inspection = OfferTreeExtractor.inspect(snapshot)
            AppDiagnostics.recordTreeInspection(this, snapshot, inspection)
            accessibilityTreeLab.record(snapshot, inspection)
            captureLearningLab.recordTreeInspection(snapshot, inspection)

            val candidate = inspection.offerText?.let(OfferParser::parse)
            if (candidate != null) {
                showDebugHeartbeat(
                    uberForeground = true,
                    lastEventType = "scan_delay_${delayMs}ms",
                    rootNull = rootNull,
                    windowCount = snapshot.windowCount,
                    nodeCount = snapshot.nodeCount,
                    textNodeCount = snapshot.nodes.count { !it.textRaw.isNullOrBlank() },
                    contentDescriptionNodeCount = snapshot.nodes.count { !it.contentDescriptionRaw.isNullOrBlank() },
                    candidateCount = 1,
                    failureCategory = AccessibilityFailureCategory.UNKNOWN,
                    bestDelayMs = delayMs
                )
                recordCaptureSuccess(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)
                return AccessibilityCandidate(
                    candidate = candidate,
                    trustedSingleFrame = inspection.hasActionButton
                )
            }
            recordTreeCaptureRejectionIfUseful(inspection)
            rejectedReasons += "${snapshot.sourceName}:${inspection.rejectionReason}"
        }

        val currentBestSnapshot = snapshotsToInspect.maxWithOrNull(
            compareBy<AccessibilityTreeSnapshot> { it.nodes.count { node -> !node.contentDescriptionRaw.isNullOrBlank() } }
                .thenBy { it.nodes.count { node -> !node.textRaw.isNullOrBlank() } }
                .thenBy { it.nodeCount }
        )
        val currentBestInspection = currentBestSnapshot?.let(OfferTreeExtractor::inspect)
        val currentFailure = AccessibilityFailureClassifier.classify(
            serviceActive = true,
            uberForeground = true,
            rootNull = rootNull,
            windowCount = windowCount,
            nodeCount = currentBestSnapshot?.nodeCount ?: 0,
            textNodeCount = currentBestSnapshot?.nodes?.count { !it.textRaw.isNullOrBlank() } ?: 0,
            contentDescriptionNodeCount = currentBestSnapshot?.nodes?.count { !it.contentDescriptionRaw.isNullOrBlank() } ?: 0,
            inspection = currentBestInspection,
            parserSucceeded = false
        )
        showDebugHeartbeat(
            uberForeground = true,
            lastEventType = "scan_delay_${delayMs}ms",
            rootNull = rootNull,
            windowCount = windowCount,
            nodeCount = currentBestSnapshot?.nodeCount ?: 0,
            textNodeCount = currentBestSnapshot?.nodes?.count { !it.textRaw.isNullOrBlank() } ?: 0,
            contentDescriptionNodeCount = currentBestSnapshot?.nodes?.count { !it.contentDescriptionRaw.isNullOrBlank() } ?: 0,
            candidateCount = 0,
            failureCategory = currentFailure,
            bestDelayMs = delayMs
        )

        if (logRejectedTree) {
            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG,
                    "Accessibility tree rejected generation=$generationId delay=${delayMs}ms " +
                        "failure=${currentFailure.label} overlayRemovedForScan=$calcMotOverlayRemovedForScan " +
                        "across ${roots.size} roots: " +
                        "${rejectedReasons.take(MAX_ACCESSIBILITY_LOG_REASONS)}; " +
                        snapshotsToInspect.joinToString(separator = " | ") { it.debugSummary() }
                )
            }
        }

        return null
    }

    private fun removeOverlayWindowsBeforeScanIfNeeded(): Boolean {
        if (!AccessibilityDebugConfig.ENABLE_ZERO_OVERLAY_DURING_SCAN) return false
        if (overlayManager?.isVisible == true) return false
        val removed = overlayManager?.removeOverlayWindowsForScan() == true
        if (removed && BuildConfig.DEBUG) {
            Log.i(TAG, "Removed CalcMot overlay windows before accessibility scan")
        }
        return removed
    }

    private fun accessibilityRootSources(): List<AccessibilityRootSource> {
        val currentWindows = allInteractiveWindowsForScan()
        val windowRoots = currentWindows
            .filter { it.isActive || it.isFocused }
            .mapIndexedNotNull { index, windowSource ->
                val root = windowSource.root ?: return@mapIndexedNotNull null
                val packageHint = if (root.packageName?.toString() == UBER_DRIVER_PACKAGE) {
                    "uber"
                } else {
                    "unverified"
                }
                AccessibilityRootSource(
                    name = "window-$index-$packageHint-${windowSource.debugName}",
                    root = root,
                    windowCount = currentWindows.size,
                    sourceKind = AccessibilitySnapshotSourceKind.WINDOW_ROOT
                )
            }

        val activeRoots = activeWindowRootsForScan()
            .map { activeRoot ->
                val packageHint = if (activeRoot.root.packageName?.toString() == UBER_DRIVER_PACKAGE) {
                    "uber"
                } else {
                    "unverified"
                }
                AccessibilityRootSource(
                    name = "$packageHint-${activeRoot.debugName}",
                    root = activeRoot.root,
                    windowCount = currentWindows.size,
                    sourceKind = AccessibilitySnapshotSourceKind.ROOT_IN_ACTIVE_WINDOW
                )
            }

        return (windowRoots + activeRoots)
            .distinctBy {
                val bounds = Rect()
                runCatching { it.root.getBoundsInScreen(bounds) }
                "${it.sourceKind}|${it.root.packageName}|${it.root.className}|${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}|${it.name}"
            }
    }

    private fun allInteractiveWindowsForScan(): List<AccessibilityWindowSource> {
        val fromCurrentDisplay = runCatching { windows }
            .getOrDefault(emptyList())
            .mapIndexed { index, window ->
                window.toAccessibilityWindowSource(displayLabel = "current", index = index)
            }

        val fromAllDisplays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val sparseWindows = windowsOnAllDisplays
                buildList {
                    for (displayIndex in 0 until sparseWindows.size()) {
                        val displayId = sparseWindows.keyAt(displayIndex)
                        val displayWindows = sparseWindows.valueAt(displayIndex).orEmpty()
                        displayWindows.forEachIndexed { index, window ->
                            add(window.toAccessibilityWindowSource(displayLabel = "display-$displayId", index = index))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        return (fromAllDisplays + fromCurrentDisplay)
            .filter { it.root != null }
            .distinctBy { it.identityKey }
            .sortedWith(compareByDescending<AccessibilityWindowSource> { it.layer }.thenBy { it.debugName })
    }

    private fun activeWindowRootsForScan(): List<AccessibilityActiveRootSource> {
        val defaultRoot = rootInActiveWindow?.let {
            AccessibilityActiveRootSource(debugName = "root-in-active-window", root = it)
        }

        val prefetchedRoot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                getRootInActiveWindow(
                    AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_DEPTH_FIRST or
                        AccessibilityNodeInfo.FLAG_PREFETCH_SIBLINGS or
                        AccessibilityNodeInfo.FLAG_PREFETCH_ANCESTORS
                )
            }.getOrNull()?.let {
                AccessibilityActiveRootSource(debugName = "root-in-active-window-prefetch", root = it)
            }
        } else {
            null
        }

        return listOfNotNull(defaultRoot, prefetchedRoot)
    }

    private fun AccessibilityWindowInfo.toAccessibilityWindowSource(
        displayLabel: String,
        index: Int
    ): AccessibilityWindowSource {
        val bounds = Rect()
        runCatching { getBoundsInScreen(bounds) }
        val prefetchedRoot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                getRoot(
                    AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_DEPTH_FIRST or
                        AccessibilityNodeInfo.FLAG_PREFETCH_SIBLINGS or
                        AccessibilityNodeInfo.FLAG_PREFETCH_ANCESTORS
                )
            }.getOrNull()
        } else {
            null
        }
        val resolvedRoot = prefetchedRoot ?: root
        val resolvedDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { displayId }.getOrDefault(-1)
        } else {
            -1
        }

        return AccessibilityWindowSource(
            debugName = "$displayLabel-$index-type-$type-layer-$layer-active-$isActive-focused-$isFocused-" +
                "display-$resolvedDisplayId-bounds-${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}",
            identityKey = "$displayLabel|$index|$type|$layer|$resolvedDisplayId|" +
                "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}",
            layer = layer,
            isActive = isActive,
            isFocused = isFocused,
            root = resolvedRoot
        )
    }

    private fun AccessibilityNodeInfo.containsPackageName(packageName: String): Boolean {
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(this)
        var visited = 0

        while (queue.isNotEmpty() && visited < PACKAGE_SCAN_NODE_LIMIT) {
            val node = queue.removeFirst()
            visited += 1
            if (node.packageName?.toString() == packageName) return true

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }

        return false
    }

    private fun List<AccessibilityTreeSnapshot>.toCombinedSnapshotOrNull(
        eventAtMillis: Long
    ): AccessibilityTreeSnapshot? {
        if (size < 2) return null

        val combinedLines = flatMap { it.lines }
            .distinctBy { "${it.text.trim()}|${it.bounds.left}|${it.bounds.top}|${it.bounds.right}|${it.bounds.bottom}" }
        if (combinedLines.isEmpty()) return null
        val combinedNodes = flatMap { it.nodes }
            .distinctBy { "${it.snapshotId}|${it.boundsInScreen.left}|${it.boundsInScreen.top}|${it.boundsInScreen.right}|${it.boundsInScreen.bottom}" }

        return AccessibilityTreeSnapshot(
            sourceName = "combined-windows",
            capturedAtMillis = System.currentTimeMillis(),
            eventAtMillis = eventAtMillis,
            screenWidth = maxOf { it.screenWidth },
            screenHeight = maxOf { it.screenHeight },
            windowCount = maxOf { it.windowCount },
            rootCount = size,
            nodeCount = sumOf { it.nodeCount },
            rootPackageName = firstNotNullOfOrNull { it.rootPackageName },
            rootClassName = firstNotNullOfOrNull { it.rootClassName },
            lines = combinedLines,
            nodes = combinedNodes,
            generationId = captureGeneration,
            scanId = "combined-windows-gen-$captureGeneration",
            sourceKind = AccessibilitySnapshotSourceKind.COMBINED_WINDOWS,
            truncated = any { it.truncated },
            maxDepthReached = maxOf { it.maxDepthReached }
        )
    }

    private fun extractCandidateFromSnapshot(snapshot: AccessibilityTreeSnapshot): AccessibilityCandidate? {
        val inspection = OfferTreeExtractor.inspect(snapshot)
        AppDiagnostics.recordTreeInspection(this, snapshot, inspection)
        accessibilityTreeLab.record(snapshot, inspection)
        captureLearningLab.recordTreeInspection(snapshot, inspection)

        val candidate = inspection.offerText?.let(OfferParser::parse)
        if (candidate != null) {
            recordCaptureSuccess(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)
        } else {
            recordTreeCaptureRejectionIfUseful(inspection)
        }
        if (candidate != null && BuildConfig.DEBUG) {
            Log.i(TAG, "Accessibility candidate parsed from ${snapshot.sourceName}: ${candidate.fingerprint}")
        }
        return candidate?.let {
            AccessibilityCandidate(
                candidate = it,
                trustedSingleFrame = inspection.hasActionButton
            )
        }
    }

    private fun emptyAccessibilitySnapshot(
        sourceName: String,
        eventAtMillis: Long,
        windowCount: Int,
        generationId: Long,
        delayMs: Long
    ): AccessibilityTreeSnapshot {
        val displayMetrics = resources.displayMetrics
        return AccessibilityTreeSnapshot(
            sourceName = sourceName,
            capturedAtMillis = eventAtMillis + delayMs,
            eventAtMillis = eventAtMillis,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            windowCount = windowCount,
            rootCount = 0,
            nodeCount = 0,
            rootPackageName = null,
            rootClassName = null,
            lines = emptyList(),
            nodes = emptyList(),
            generationId = generationId,
            scanId = "$sourceName-gen-$generationId-delay-$delayMs",
            sourceKind = AccessibilitySnapshotSourceKind.UNKNOWN,
            delayMs = delayMs,
            truncated = false,
            maxDepthReached = 0
        )
    }

    private fun AccessibilityTreeSnapshot.debugSummary(): String {
        val textNodeCount = nodes.count { !it.textRaw.isNullOrBlank() }
        val descriptionNodeCount = nodes.count { !it.contentDescriptionRaw.isNullOrBlank() }
        val viewIdCount = nodes.count { !it.viewIdResourceName.isNullOrBlank() }
        val inspection = OfferTreeExtractor.inspect(this)
        return "${sourceName} nodes=$nodeCount textNodes=$textNodeCount " +
            "descNodes=$descriptionNodeCount viewIds=$viewIdCount " +
            "fields=${inspection.fieldCandidates.size} mappings=${inspection.knownNodeMappings.size} " +
            "reason=${inspection.rejectionReason}"
    }

    private fun AccessibilityEvent.toAccessibilitySnapshot(eventAtMillis: Long): AccessibilityTreeSnapshot {
        val lines = mutableListOf<AccessibleLine>()
        val rawTexts = mutableListOf<String>()
        text
            ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
            ?.let(rawTexts::addAll)
        contentDescription
            ?.toString()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(rawTexts::add)

        rawTexts
            .flatMap { rawText ->
                rawText
                    .replace(" | ", "\n")
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
            }
            .distinct()
            .forEachIndexed { index, lineText ->
                val top = 80 + index * 64
                lines += AccessibleLine(
                    text = lineText,
                    bounds = ScreenBounds(
                        left = 0,
                        top = top,
                        right = resources.displayMetrics.widthPixels,
                        bottom = top + 48
                    ),
                    packageName = packageName?.toString(),
                    className = className?.toString(),
                    viewId = null,
                    depth = 0,
                    source = AccessibleTextSource.TEXT,
                    visibleToUser = true
                )
            }

        val displayMetrics = resources.displayMetrics
        return AccessibilityTreeSnapshot(
            sourceName = "event-payload",
            capturedAtMillis = System.currentTimeMillis(),
            eventAtMillis = eventAtMillis,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = maxOf(displayMetrics.heightPixels, 200 + lines.size * 64),
            windowCount = windows.size,
            rootCount = 1,
            nodeCount = lines.size,
            rootPackageName = packageName?.toString(),
            rootClassName = className?.toString(),
            lines = lines,
            generationId = captureGeneration,
            scanId = "event-payload-gen-$captureGeneration",
            sourceKind = AccessibilitySnapshotSourceKind.EVENT_PAYLOAD
        )
    }

    private fun AccessibilityNodeInfo.toAccessibilitySnapshot(
        sourceName: String,
        eventAtMillis: Long,
        windowCount: Int,
        rootCount: Int,
        generationId: Long = captureGeneration,
        sourceKind: AccessibilitySnapshotSourceKind = AccessibilitySnapshotSourceKind.UNKNOWN
    ): AccessibilityTreeSnapshot {
        AppDiagnostics.recordStage(this@UberAccessibilityService, AppDiagnostics.Stage.TREE_REFRESH_REQUESTED)
        runCatching { refresh() }
            .onSuccess {
                AppDiagnostics.recordStage(this@UberAccessibilityService, AppDiagnostics.Stage.TREE_REFRESHED)
            }
        val lines = mutableListOf<AccessibleLine>()
        val nodes = mutableListOf<AccessibleNodeSnapshot>()
        val walkResult = DebugTreeWalker.walkDepthFirst(
            root = this,
            maxDepth = if (AccessibilityDebugConfig.ENABLE_ACCESSIBILITY_DEEP_DEBUG) {
                AccessibilityDebugConfig.MAX_DEBUG_TREE_DEPTH
            } else {
                MAX_ACCESSIBILITY_TREE_DEPTH
            },
            maxNodes = if (AccessibilityDebugConfig.ENABLE_ACCESSIBILITY_DEEP_DEBUG) {
                AccessibilityDebugConfig.MAX_DEBUG_TREE_NODES
            } else {
                MAX_ACCESSIBILITY_TREE_NODES
            },
            identity = { node ->
                val bounds = Rect()
                runCatching { node.getBoundsInScreen(bounds) }
                "${System.identityHashCode(node)}|${node.windowId}|${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
            },
            childCount = { node -> node.childCount },
            childAt = { node, index -> node.getChild(index) }
        )
        val snapshotIdsByPath = mutableMapOf<String, Int>()
        walkResult.visited.forEach { visit ->
            collectAccessibleNodeSnapshot(
                node = visit.node,
                visit = visit,
                output = lines,
                nodes = nodes,
                parentSnapshotId = visit.path.parentPath()?.let(snapshotIdsByPath::get),
                generationId = generationId,
                sourceKind = sourceKind
            ).also { snapshotIdsByPath[visit.path] = it }
        }
        val displayMetrics = resources.displayMetrics

        return AccessibilityTreeSnapshot(
            sourceName = sourceName,
            capturedAtMillis = System.currentTimeMillis(),
            eventAtMillis = eventAtMillis,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            windowCount = windowCount,
            rootCount = rootCount,
            nodeCount = nodes.size,
            rootPackageName = packageName?.toString(),
            rootClassName = className?.toString(),
            lines = lines,
            nodes = nodes,
            generationId = generationId,
            scanId = "$sourceName-gen-$generationId-delay-${(System.currentTimeMillis() - eventAtMillis).coerceAtLeast(0L)}",
            sourceKind = sourceKind,
            truncated = walkResult.truncated,
            maxDepthReached = walkResult.maxDepthReached
        )
    }

    private fun collectAccessibleNodeSnapshot(
        node: AccessibilityNodeInfo,
        visit: br.com.calcmot.processor.DebugTreeVisit<AccessibilityNodeInfo>,
        output: MutableList<AccessibleLine>,
        nodes: MutableList<AccessibleNodeSnapshot>,
        parentSnapshotId: Int?,
        generationId: Long,
        sourceKind: AccessibilitySnapshotSourceKind
    ): Int {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val screenBounds = ScreenBounds(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom
        )
        val nodeSnapshotId = System.identityHashCode(node)
        val textRaw = node.text?.toString()
        val contentDescriptionRaw = node.contentDescription?.toString()
        val stateDescriptionRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            node.stateDescription?.toString()
        } else {
            null
        }
        val paneTitleRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            node.paneTitle?.toString()
        } else {
            null
        }
        val tooltipTextRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            node.tooltipText?.toString()
        } else {
            null
        }
        val hintTextRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()
        } else {
            null
        }
        val extrasSummary = node.extrasSummary()

        nodes += AccessibleNodeSnapshot(
            snapshotId = nodeSnapshotId,
            parentSnapshotId = parentSnapshotId,
            generationId = generationId,
            sourceKind = sourceKind,
            path = visit.path,
            depth = visit.depth,
            indexInParent = visit.indexInParent,
            windowId = node.windowId,
            packageName = node.packageName?.toString(),
            className = node.className?.toString(),
            textRaw = textRaw,
            textNormalized = textRaw?.let(TextNormalizer::clean),
            contentDescriptionRaw = contentDescriptionRaw,
            contentDescriptionNormalized = contentDescriptionRaw?.let(TextNormalizer::clean),
            stateDescriptionRaw = stateDescriptionRaw,
            paneTitleRaw = paneTitleRaw,
            hintTextRaw = hintTextRaw,
            tooltipTextRaw = tooltipTextRaw,
            extrasSummary = extrasSummary,
            viewIdResourceName = node.viewIdResourceName,
            boundsInScreen = screenBounds,
            visibleToUser = node.isVisibleToUser,
            clickable = node.isClickable,
            enabled = node.isEnabled,
            focused = node.isFocused,
            selected = node.isSelected,
            childCount = node.childCount,
            timestamp = System.currentTimeMillis()
        )

        node.accessibilityTextValues().forEach { (source, value) ->
            output += node.toAccessibleLine(
                text = value,
                source = source,
                depth = visit.depth,
                bounds = screenBounds,
                nodeSnapshotId = nodeSnapshotId,
                parentSnapshotId = parentSnapshotId
            )
        }

        return nodeSnapshotId
    }

    private fun AccessibilityNodeInfo.toAccessibleLine(
        text: String,
        source: AccessibleTextSource,
        depth: Int,
        bounds: ScreenBounds,
        nodeSnapshotId: Int?,
        parentSnapshotId: Int?
    ): AccessibleLine {
        return AccessibleLine(
            text = text,
            bounds = bounds,
            packageName = packageName?.toString(),
            className = className?.toString(),
            viewId = viewIdResourceName,
            depth = depth,
            source = source,
            visibleToUser = isVisibleToUser,
            clickable = isClickable,
            enabled = isEnabled,
            focused = isFocused,
            selected = isSelected,
            childCount = childCount,
            nodeSnapshotId = nodeSnapshotId,
            parentSnapshotId = parentSnapshotId
        )
    }

    private fun AccessibilityNodeInfo.accessibilityTextValues(): List<Pair<AccessibleTextSource, String>> {
        val values = mutableListOf<Pair<AccessibleTextSource, String>>()
        val seen = linkedSetOf<String>()

        fun add(source: AccessibleTextSource, rawValue: CharSequence?) {
            val value = rawValue
                ?.toString()
                ?.replace('\u00A0', ' ')
                ?.replace(Regex("""\s+"""), " ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return
            if (seen.add(value)) {
                values += source to value
            }
        }

        add(AccessibleTextSource.TEXT, text)
        add(AccessibleTextSource.CONTENT_DESCRIPTION, contentDescription)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(AccessibleTextSource.STATE_DESCRIPTION, stateDescription)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(AccessibleTextSource.PANE_TITLE, paneTitle)
            add(AccessibleTextSource.TOOLTIP_TEXT, tooltipText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(AccessibleTextSource.HINT_TEXT, hintText)
        }

        add(AccessibleTextSource.VIEW_ID_RESOURCE_NAME, viewIdResourceName)
        extras
            ?.keySet()
            ?.asSequence()
            ?.take(MAX_EXTRAS_TEXT_VALUES)
            ?.forEach { key ->
                when (val value = extras.get(key)) {
                    is CharSequence -> add(AccessibleTextSource.EXTRAS, value)
                    is Array<*> -> value.forEach { item ->
                        if (item is CharSequence) add(AccessibleTextSource.EXTRAS, item)
                    }
                    is Iterable<*> -> value.forEach { item ->
                        if (item is CharSequence) add(AccessibleTextSource.EXTRAS, item)
                    }
                }
            }

        return values
    }

    private fun AccessibilityNodeInfo.extrasSummary(): String? {
        val extras = extras ?: return null
        val values = extras.keySet()
            .asSequence()
            .take(MAX_EXTRAS_TEXT_VALUES)
            .mapNotNull { key ->
                val value = extras.get(key) ?: return@mapNotNull null
                "$key=${value.toString().take(MAX_EXTRAS_VALUE_LENGTH)}"
            }
            .toList()
        return values
            .joinToString(separator = " | ")
            .takeIf { it.isNotBlank() }
    }

    private fun String.parentPath(): String? {
        val lastSlash = lastIndexOf('/')
        return if (lastSlash <= 0) null else substring(0, lastSlash)
    }

    private fun recordCaptureSuccess(source: OfferCaptureSource, candidate: OfferCandidate) {
        captureLearningLab.recordCandidate(source, candidate)
        AppDiagnostics.recordCaptureOutcome(
            this,
            OfferCaptureOutcome.accepted(source = source, candidate = candidate)
        )
    }

    private fun recordCaptureSuccess(source: OfferCaptureSource, tripData: TripData) {
        captureLearningLab.recordOverlay(source, tripData)
        AppDiagnostics.recordCaptureOutcome(
            this,
            OfferCaptureOutcome.accepted(source = source, tripData = tripData)
        )
    }

    private fun recordCaptureRejection(
        source: OfferCaptureSource,
        reason: OfferCaptureRejectionReason
    ) {
        captureLearningLab.recordRejected(source, reason)
        AppDiagnostics.recordCaptureOutcome(
            this,
            OfferCaptureOutcome.rejected(source = source, reason = reason)
        )
    }

    private fun recordTreeCaptureRejectionIfUseful(inspection: TreeOfferInspection) {
        if (inspection.lineCount <= 0) return
        if (!inspection.hasPrice && !inspection.hasActionButton && inspection.timeDistanceBlockCount == 0) return

        val reason = inspection.rejectionReason?.toCaptureRejectionReason()
            ?: OfferCaptureRejectionReason.UNKNOWN
        recordCaptureRejection(OfferCaptureSource.ACCESSIBILITY_TREE, reason)
    }

    private fun TreeRejectionReason.toCaptureRejectionReason(): OfferCaptureRejectionReason {
        return when (this) {
            TreeRejectionReason.EMPTY_TREE -> OfferCaptureRejectionReason.INVALID_FRAME
            TreeRejectionReason.NO_PRICE -> OfferCaptureRejectionReason.NO_PRICE
            TreeRejectionReason.MULTIPLE_PRIMARY_PRICES -> OfferCaptureRejectionReason.MULTIPLE_PRIMARY_PRICES
            TreeRejectionReason.NO_ACTION_BUTTON -> OfferCaptureRejectionReason.NO_ACTION_BUTTON
            TreeRejectionReason.PRICE_AFTER_BUTTON,
            TreeRejectionReason.INVALID_VERTICAL_ORDER -> OfferCaptureRejectionReason.INVALID_VERTICAL_ORDER
            TreeRejectionReason.NOT_CARD_LIKE -> OfferCaptureRejectionReason.NOT_CARD_LIKE
            TreeRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS -> {
                OfferCaptureRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS
            }
            TreeRejectionReason.PARSER_REJECTED -> OfferCaptureRejectionReason.PARSER_REJECTED
        }
    }

    private fun handleCandidate(
        candidate: OfferCandidate,
        source: OfferCaptureSource,
        label: String,
        trustedSingleFrame: Boolean = false
    ): Boolean {
        val decision = captureCoordinator.acceptCandidate(
            source = source,
            candidate = candidate,
            trustedSingleFrame = trustedSingleFrame
        )
        return applyCaptureDecision(decision, label)
    }

    private fun handleShellOfferFrame(frame: ShellOfferFrame) {
        when (frame) {
            ShellOfferFrame.InvalidFrame -> handleShellInvalidFrame()
            is ShellOfferFrame.Candidate -> handleShellOfferCandidate(frame.candidate)
            is ShellOfferFrame.StableTrip -> handleShellStableTrip(frame.tripData)
        }
    }

    private fun handleShellInvalidFrame() {
        recordCaptureRejection(
            source = OfferCaptureSource.UIAUTOMATOR_LAB,
            reason = OfferCaptureRejectionReason.INVALID_FRAME
        )
        rejectCurrentFrame(
            reason = "UIAutomator shell frame rejected",
            source = OfferCaptureSource.UIAUTOMATOR_LAB,
            rejectionReason = OfferCaptureRejectionReason.INVALID_FRAME
        )
    }

    private fun handleShellOfferCandidate(candidate: OfferCandidate) {
        if (!AppSettings.isMonitoringEnabled(this)) {
            AppDiagnostics.recordStage(this, AppDiagnostics.Stage.MONITORING_DISABLED)
            captureCoordinator.reset()
            mainScope.launch { overlayManager?.hideOverlay() }
            return
        }

        recordCaptureSuccess(OfferCaptureSource.UIAUTOMATOR_LAB, candidate)
        handleCandidate(
            candidate = candidate,
            source = OfferCaptureSource.UIAUTOMATOR_LAB,
            label = "uiautomator-shell"
        )
    }

    private fun handleShellStableTrip(tripData: TripData) {
        if (!AppSettings.isMonitoringEnabled(this)) {
            AppDiagnostics.recordStage(this, AppDiagnostics.Stage.MONITORING_DISABLED)
            captureCoordinator.reset()
            mainScope.launch { overlayManager?.hideOverlay() }
            return
        }

        recordCaptureSuccess(OfferCaptureSource.UIAUTOMATOR_LAB, tripData)
        applyCaptureDecision(
            decision = captureCoordinator.acceptStableTrip(OfferCaptureSource.UIAUTOMATOR_LAB, tripData),
            label = "uiautomator-shell-bridge"
        )
    }

    private fun showOverlay(tripData: TripData) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            overlayManager?.showOverlay(tripData)
        } else {
            runBlocking(Dispatchers.Main.immediate) {
                overlayManager?.showOverlay(tripData)
            }
        }
    }

    private fun handleOverlayDismissedByUser() {
        serviceScope.launch {
            val decision = captureCoordinator.dismissCurrentOverlayByUser()
            applyCaptureDecision(decision, "overlay-double-tap")
        }
    }

    private fun scheduleOverlayExpiry(expectedFingerprint: String) {
        overlayExpiryJob?.cancel()
        overlayExpiryJob = serviceScope.launch {
            delay(OVERLAY_TTL_MS)
            val decision = captureCoordinator.expireOverlayIfStale() ?: return@launch
            if (decision.overlayFingerprint == null || decision.overlayFingerprint == expectedFingerprint) {
                applyCaptureDecision(decision, "overlay-ttl")
            }
        }
    }

    private fun cancelOverlayExpiry() {
        overlayExpiryJob?.cancel()
        overlayExpiryJob = null
    }

    private fun rejectCurrentFrame(
        reason: String,
        source: OfferCaptureSource = OfferCaptureSource.ACCESSIBILITY_TREE,
        rejectionReason: OfferCaptureRejectionReason = OfferCaptureRejectionReason.INVALID_FRAME
    ) {
        val decision = captureCoordinator.rejectFrame(source, rejectionReason)
        applyCaptureDecision(decision, reason)
    }

    private fun applyCaptureDecision(decision: CaptureDecision, label: String): Boolean {
        return when (decision) {
            is CaptureDecision.WaitingForStability -> {
                AppDiagnostics.recordStage(this, AppDiagnostics.Stage.FIRST_FRAME)
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Offer candidate accepted as first frame [$label]: ${decision.candidate.fingerprint}")
                }
                false
            }

            is CaptureDecision.HideStaleOverlay -> {
                AppDiagnostics.recordStage(this, AppDiagnostics.Stage.FIRST_FRAME)
                captureLearningLab.recordOverlayHidden(
                    status = "overlay_hidden_card_changed",
                    source = decision.source,
                    fingerprint = decision.overlayFingerprint,
                    reason = null
                )
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "New offer candidate replaced visible overlay [$label]: ${decision.candidate.fingerprint}")
                }
                cancelOverlayExpiry()
                mainScope.launch { overlayManager?.hideOverlay() }
                false
            }

            is CaptureDecision.ShowOverlay -> {
                AppDiagnostics.recordStage(this, AppDiagnostics.Stage.STABLE_OFFER)
                captureLearningLab.recordOverlay(decision.source, decision.tripData)
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Stable offer confirmed [$label]: ${decision.tripData}")
                }
                showOverlay(decision.tripData)
                scheduleOverlayExpiry(decision.overlayFingerprint)
                true
            }

            is CaptureDecision.SuppressedCandidate -> {
                captureLearningLab.recordOverlayHidden(
                    status = "overlay_suppressed",
                    source = decision.source,
                    fingerprint = decision.overlayFingerprint,
                    reason = null
                )
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Offer suppressed after user dismiss [$label]: ${decision.overlayFingerprint}")
                }
                false
            }

            is CaptureDecision.KeepCurrentOverlay -> {
                AppDiagnostics.recordStage(this, AppDiagnostics.Stage.FRAME_REJECTED)
                captureLearningLab.recordOverlayHidden(
                    status = "overlay_kept_invalid_frame",
                    source = decision.source,
                    fingerprint = null,
                    reason = decision.reason
                )
                if (BuildConfig.DEBUG) {
                    Log.d(
                        TAG,
                        "Frame rejected but waiting for confirmation " +
                        "(${decision.invalidFrameStreak}/${decision.requiredInvalidFramesToReset}) [$label]"
                    )
                }
                false
            }

            is CaptureDecision.HideOverlay -> {
                AppDiagnostics.recordStage(this, AppDiagnostics.Stage.FRAME_REJECTED)
                captureLearningLab.recordOverlayHidden(
                    status = when (label) {
                        "overlay-ttl", "overlay-heartbeat-ttl" -> "overlay_expired"
                        "overlay-double-tap" -> "overlay_hidden_user"
                        else -> "overlay_hidden_invalid_frame"
                    },
                    source = decision.source,
                    fingerprint = decision.overlayFingerprint,
                    reason = decision.reason
                )
                if (BuildConfig.DEBUG) Log.d(TAG, "Frame rejected [$label]: ${decision.reason}")
                cancelOverlayExpiry()
                mainScope.launch {
                    if (label == "overlay-ttl" || label == "overlay-heartbeat-ttl") {
                        overlayManager?.expireOverlay(decision.overlayFingerprint)
                    } else {
                        overlayManager?.hideOverlay()
                    }
                }
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShellOfferBridge.unregister(shellOfferHandler)
        continuousAccessibilityPollingJob?.cancel()
        accessibilityPollingJob?.cancel()
        capturePipelineJob?.cancel()
        overlayExpiryJob?.cancel()
        accessibilityTreeLab.close()
        captureLearningLab.close()
        overlayManager?.removeOverlay()
        serviceScope.cancel()
        mainScope.cancel()
        captureDispatcher.close()
    }

    override fun onInterrupt() {}

    private fun showDebugHeartbeat(
        uberForeground: Boolean,
        lastEventType: String,
        rootNull: Boolean,
        windowCount: Int,
        nodeCount: Int,
        textNodeCount: Int,
        contentDescriptionNodeCount: Int,
        candidateCount: Int,
        failureCategory: AccessibilityFailureCategory,
        bestDelayMs: Long?
    ) {
        if (!AccessibilityDebugConfig.ENABLE_DEBUG_OVERLAY_HEARTBEAT) return
        mainScope.launch {
            overlayManager?.showDebugOverlay(
                AccessibilityDebugOverlayState(
                    serviceActive = true,
                    uberForeground = uberForeground,
                    lastEventType = lastEventType,
                    rootStatus = if (rootNull) "null" else "ok",
                    windowCount = windowCount,
                    nodesScanned = nodeCount,
                    textNodeCount = textNodeCount,
                    contentDescriptionNodeCount = contentDescriptionNodeCount,
                    candidateCount = candidateCount,
                    lastFailureReason = failureCategory,
                    bestDelayMs = bestDelayMs
                )
            )
        }
    }

    private fun AccessibilityEvent.isRelevantUberEvent(): Boolean {
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
            else -> false
        }
    }

    private fun Int.debugEventName(): String {
        return when (this) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TYPE_VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "TYPE_VIEW_SCROLLED"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "TYPE_WINDOWS_CHANGED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "TYPE_VIEW_FOCUSED"
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> "TYPE_ANNOUNCEMENT"
            else -> "event_$this"
        }
    }

    private companion object {
        const val TAG = "UberReader"
        var runtimeAccessibilityInfoConfiguredOnce = false
        const val UBER_DRIVER_PACKAGE = "com.ubercab.driver"
        const val MAX_ACCESSIBILITY_LOG_REASONS = 40
        const val EVENT_SOURCE_PARENT_ATTEMPTS = 8
        const val ACCESSIBILITY_HEARTBEAT_INTERVAL_MS = 1_000L
        const val ACCESSIBILITY_POLL_WINDOW_MS = 2_500L
        const val ACCESSIBILITY_POLL_INTERVAL_MS = 250L
        const val REQUIRED_INVALID_FRAMES_TO_RESET = 2
        const val OVERLAY_TTL_MS = 1_200L
        const val ACCESSIBILITY_SCAN_SESSION_COALESCE_MS = 2_500L
        const val MAX_EXTRAS_TEXT_VALUES = 24
        const val MAX_EXTRAS_VALUE_LENGTH = 160
        const val MAX_ACCESSIBILITY_TREE_DEPTH = 80
        const val MAX_ACCESSIBILITY_TREE_NODES = 2_000
        const val PACKAGE_SCAN_NODE_LIMIT = 5_000
        val ACCESSIBILITY_BURST_DELAYS_MS = longArrayOf(120L, 240L, 420L, 700L, 1_000L, 1_500L, 2_200L)
    }

    private data class CaptureSession(
        val generation: Long,
        val shouldStartBurst: Boolean
    )

    private data class AccessibilityCandidate(
        val candidate: OfferCandidate,
        val trustedSingleFrame: Boolean
    )

    private data class AccessibilityRootSource(
        val name: String,
        val root: AccessibilityNodeInfo,
        val windowCount: Int,
        val sourceKind: AccessibilitySnapshotSourceKind
    )

    private data class AccessibilityWindowSource(
        val debugName: String,
        val identityKey: String,
        val layer: Int,
        val isActive: Boolean,
        val isFocused: Boolean,
        val root: AccessibilityNodeInfo?
    )

    private data class AccessibilityActiveRootSource(
        val debugName: String,
        val root: AccessibilityNodeInfo
    )
}
