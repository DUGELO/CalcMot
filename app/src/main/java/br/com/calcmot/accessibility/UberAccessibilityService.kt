package br.com.calcmot.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.com.calcmot.AppDiagnostics
import br.com.calcmot.AppSettings
import br.com.calcmot.BuildConfig
import br.com.calcmot.DriverApp
import br.com.calcmot.DriverAppPackagePolicy
import br.com.calcmot.PackageDecision
import br.com.calcmot.model.OfferCaptureOutcome
import br.com.calcmot.model.OfferCaptureRejectionReason
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.TripData
import br.com.calcmot.model.isImmediateInvalidContext
import br.com.calcmot.model.isInvalidContext
import br.com.calcmot.ninetynine.AccessibilityScreenshotCaptureSource
import br.com.calcmot.ninetynine.NinetyNineCaptureEngine
import br.com.calcmot.ninetynine.NinetyNineCaptureResult
import br.com.calcmot.ninetynine.NinetyNineCaptureSkipReason
import br.com.calcmot.ninetynine.NinetyNineExtractionRejection
import br.com.calcmot.ninetynine.NinetyNineExtractionResult
import br.com.calcmot.ninetynine.NinetyNineRecognitionConfig
import br.com.calcmot.ninetynine.UnsupportedNinetyNineCaptureSource
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
import br.com.calcmot.processor.DriverOfferParser
import br.com.calcmot.processor.DriverOfferTreeExtractor
import br.com.calcmot.processor.FarePriceExtractor
import br.com.calcmot.processor.ScreenBounds
import br.com.calcmot.processor.TextNormalizer
import br.com.calcmot.processor.TreeOfferInspection
import br.com.calcmot.processor.TreeRejectionReason
import br.com.calcmot.processor.overlayFingerprint
import br.com.calcmot.telemetry.OverlayLatencyTrace
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
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

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
    @Volatile
    private var pendingCaptureAfterCurrent = false
    @Volatile
    private var pendingCaptureEventAtMillis = 0L
    @Volatile
    private var pendingCapturePackageName: String? = null
    @Volatile
    private var pendingCaptureEventType = 0
    @Volatile
    private var pendingCaptureWindowId = 0
    @Volatile
    private var lastWatchdogScanAtMillis = 0L
    @Volatile
    private var rawForegroundPackageName: String? = null
    @Volatile
    private var trustedForegroundPackageName: String? = null
    @Volatile
    private var trustedForegroundDecision: PackageDecision? = null
    @Volatile
    private var trustedForegroundDriverApp: DriverApp = DriverApp.UNKNOWN
    @Volatile
    private var trustedForegroundSeenAtElapsed: Long = 0L
    @Volatile
    private var latestVisualProbe: OverlayLatencyTrace.VisualProbe? = null
    private val latencyTracesByGeneration = ConcurrentHashMap<Long, OverlayLatencyTrace>()
    private var lastParsedOfferAudit: ParsedOfferAudit? = null
    private val postReconnectCandidateGuard = PostReconnectCandidateGuard()
    private val accessibilityTreeLab by lazy { AccessibilityTreeLab(this) }
    private val captureLearningLab by lazy { CaptureLearningLab(this) }
    private val ninetyNineDiagnostics by lazy { NinetyNineAccessibilityDiagnostics(this) }
    private val ninetyNineSemanticBridgeProbe by lazy {
        NinetyNineSemanticBridgeProbe(ninetyNineDiagnostics)
    }
    private var ninetyNineCaptureEngine: NinetyNineCaptureEngine? = null
    private val shellOfferHandler: (ShellOfferFrame) -> Unit = { frame ->
        if (BuildConfig.DEBUG) {
            serviceScope.launch { handleShellOfferFrame(frame) }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ensureServiceScopeActive()
        configureRuntimeAccessibilityInfo()
        val reconnectBaselineFingerprint = captureCoordinator.currentOverlayFingerprint()
        cancelCapturePipeline()
        cancelOverlayExpiry()
        captureCoordinator.reset()
        lastParsedOfferAudit = null
        postReconnectCandidateGuard.onServiceConnected(reconnectBaselineFingerprint)
        overlayManager?.removeOverlay()
        overlayManager = OverlayManager(this).also { manager ->
            manager.setOnUserDismissed {
                handleOverlayDismissedByUser()
            }
        }
        if (BuildConfig.DEBUG) {
            ShellOfferBridge.register(shellOfferHandler)
        }
        startContinuousAccessibilityPolling()
        if (AccessibilityDebugConfig.ENABLE_DEBUG_OVERLAY_HEARTBEAT) {
            showDebugHeartbeat(
                uberForeground = false,
                lastEventType = "service_connected",
                rootNull = true,
                windowCount = 0,
                nodeCount = 0,
                textNodeCount = 0,
                contentDescriptionNodeCount = 0,
                candidateCount = 0,
                failureCategory = AccessibilityFailureCategory.NO_ACCESSIBILITY_EVENTS,
                bestDelayMs = null
            )
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "Service connected")
        ninetyNineDiagnostics.recordServiceConfiguration(
            "eventTypes=${serviceInfo?.eventTypes} flags=${serviceInfo?.flags} " +
                "notificationTimeout=${serviceInfo?.notificationTimeout}"
        )
    }

    private fun configureRuntimeAccessibilityInfo() {
        if (runtimeAccessibilityInfoConfigured || runtimeAccessibilityInfoConfiguredOnce) return
        val currentInfo = serviceInfo ?: return
        currentInfo.eventTypes = DriverAccessibilityEventPolicy.baseEventTypes
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
        if (event == null) return

        val eventPackage = event.packageName?.toString()
        val packageDecision = DriverAppPackagePolicy.classify(eventPackage)
        val eventDriverApp = DriverAppPackagePolicy.driverAppForPackage(eventPackage)
        rawForegroundPackageName = DriverAppPackagePolicy.normalize(eventPackage)

        when (packageDecision) {
            PackageDecision.UNKNOWN -> {
                overlayManager?.setForegroundPackage(eventPackage)
                Log.w(
                    TAG,
                    "CALCMOT_UNKNOWN_PACKAGE_EVENT_IGNORED eventType=${event.eventType}"
                )
                return
            }

            PackageDecision.OWN_APP -> {
                overlayManager?.setForegroundPackage(eventPackage)
                Log.w(
                    TAG,
                    "CALCMOT_OWN_PACKAGE_EVENT_IGNORED " +
                        "package=${DriverAppPackagePolicy.describe(eventPackage)} eventType=${event.eventType}"
                )
                return
            }

            PackageDecision.TRANSIENT_SYSTEM -> {
                overlayManager?.setForegroundPackage(eventPackage)
                Log.w(
                    TAG,
                    "CALCMOT_TRANSIENT_SYSTEM_EVENT_IGNORED " +
                        "package=${DriverAppPackagePolicy.describe(eventPackage)} eventType=${event.eventType}"
                )
                return
            }

            PackageDecision.BLOCKED_USER_APP -> {
                if (event.isForegroundDefiningEvent()) {
                    if (trustedForegroundDriverApp == DriverApp.NINETY_NINE &&
                        runCatching(::hasVisibleNinetyNineRoot).getOrDefault(false)
                    ) {
                        Log.w(
                            TAG,
                            "CALCMOT_99_BACKGROUND_BLOCKED_EVENT_IGNORED " +
                                "package=${DriverAppPackagePolicy.describe(eventPackage)} eventType=${event.eventType}"
                        )
                        return
                    }
                    enterSafeIdleForBlockedUserApp(eventPackage, event.eventType)
                } else {
                    Log.w(
                        TAG,
                        "CALCMOT_BLOCKED_USER_EVENT_IGNORED_NON_FOREGROUND " +
                            "package=${DriverAppPackagePolicy.describe(eventPackage)} eventType=${event.eventType}"
                    )
                }
                return
            }

            PackageDecision.DRIVER_APP -> Unit
        }

        val activeFocusedDriverApp = activeFocusedDriverAppForGuard()
        if (activeFocusedDriverApp != DriverApp.UNKNOWN && activeFocusedDriverApp != eventDriverApp) {
            Log.w(
                TAG,
                "CALCMOT_BACKGROUND_DRIVER_EVENT_IGNORED " +
                    "package=${DriverAppPackagePolicy.describe(eventPackage)} " +
                    "activeDriver=${activeFocusedDriverApp.id} eventType=${event.eventType}"
            )
            return
        }

        switchDriverAppIfNeeded(eventDriverApp, eventPackage)
        configureRuntimeProfileForDriverApp(eventDriverApp)
        AppSettings.setLastDriverApp(this, eventDriverApp)
        updateTrustedForeground(packageName = eventPackage, decision = PackageDecision.DRIVER_APP)
        overlayManager?.setForegroundPackage(eventPackage)
        Log.w(
            TAG,
            "CALCMOT_PACKAGE_GUARD state=CALCMOT_ALLOWED_DRIVER_APP_ACTIVE " +
                "package=${DriverAppPackagePolicy.describe(eventPackage)} eventType=${event.eventType}"
        )
        if (eventDriverApp == DriverApp.NINETY_NINE) {
            ninetyNineDiagnostics.recordEvent(event, event.eventType.debugEventName())
        }
        if (!event.isRelevantDriverEvent(eventDriverApp)) return

        val eventCopy = AccessibilityEvent.obtain(event)
        AppDiagnostics.recordEvent(this, event.eventType)
        val eventAtMillis = System.currentTimeMillis()
        if (AccessibilityDebugConfig.ENABLE_DEBUG_OVERLAY_HEARTBEAT) {
            showDebugHeartbeat(
                uberForeground = true,
                lastEventType = event.eventType.debugEventName(),
                rootNull = false,
                windowCount = 0,
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
            cancelCapturePipeline(OverlayLatencyTrace.EndReason.MONITORING_DISABLED)
            cancelOverlayExpiry()
            serviceScope.launch {
                captureCoordinator.reset()
                mainScope.launch { overlayManager?.hideOverlay() }
                closeAllLatencyTraces(OverlayLatencyTrace.EndReason.MONITORING_DISABLED)
                eventCopy.recycle()
            }
            return
        }

        val captureSession = beginOrCoalesceCaptureSession(
            eventAtMillis = eventAtMillis,
            packageName = DriverAppPackagePolicy.describe(eventPackage),
            eventType = event.eventType,
            windowId = event.windowId
        )
        val generation = captureSession.generation
        if (eventDriverApp == DriverApp.NINETY_NINE) {
            ninetyNineDiagnostics.recordCaptureSession(
                generation = generation,
                shouldStartBurst = captureSession.shouldStartBurst,
                eventType = event.eventType,
                windowId = event.windowId,
                coalesced = !captureSession.shouldStartBurst
            )
        }
        captureSession.trace?.metric(
            name = "capture.session",
            durationMs = 0L,
            details = "generation=$generation shouldStartBurst=${captureSession.shouldStartBurst}"
        )
        if (captureSession.trace != null) {
            Log.w(
                TAG,
                "CALCMOT_ACCESSIBILITY_EVENT package=${DriverAppPackagePolicy.describe(eventPackage)} " +
                    "eventType=${event.eventType} windowId=${event.windowId} traceId=${captureSession.trace.traceId}"
            )
        }
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

    private fun enterSafeIdleForBlockedUserApp(packageName: String?, eventType: Int) {
        closeNinetyNineCaptureEngine()
        updateTrustedForeground(packageName = packageName, decision = PackageDecision.BLOCKED_USER_APP)
        Log.w(
            TAG,
            "CALCMOT_SAFE_MODE state=CALCMOT_PAUSED_BLOCKED_USER_APP " +
                "package=${DriverAppPackagePolicy.describe(packageName)} eventType=$eventType"
        )
        Log.w(
            TAG,
            "BANK_SAFE_MODE_ACTIVE package=${DriverAppPackagePolicy.describe(packageName)}"
        )
        Log.w(
            TAG,
            "OVERLAY_FORCE_HIDDEN_NON_ALLOWED_APP package=${DriverAppPackagePolicy.describe(packageName)}"
        )
        cancelCapturePipeline(OverlayLatencyTrace.EndReason.SAFE_MODE_BLOCKED_USER_APP)
        cancelOverlayExpiry()
        accessibilityPollingUntil = 0L
        clearPendingCapture()
        activeScanSessionGeneration = 0L
        activeScanSessionStartedAtMillis = 0L
        lastParsedOfferAudit = null
        serviceScope.launch {
            captureCoordinator.reset()
        }
        mainScope.launch {
            overlayManager?.setForegroundPackage(packageName)
        }
    }

    private fun updateTrustedForeground(packageName: String?, decision: PackageDecision) {
        trustedForegroundPackageName = DriverAppPackagePolicy.normalize(packageName)
        trustedForegroundDecision = decision
        trustedForegroundDriverApp = if (decision == PackageDecision.DRIVER_APP) {
            DriverAppPackagePolicy.driverAppForPackage(packageName)
        } else {
            DriverApp.UNKNOWN
        }
        trustedForegroundSeenAtElapsed = SystemClock.elapsedRealtime()
    }

    private fun switchDriverAppIfNeeded(driverApp: DriverApp, packageName: String?) {
        if (driverApp == DriverApp.UNKNOWN || driverApp == trustedForegroundDriverApp) return
        val previousDriverApp = trustedForegroundDriverApp
        if (previousDriverApp == DriverApp.NINETY_NINE && driverApp != DriverApp.NINETY_NINE) {
            closeNinetyNineCaptureEngine()
        }
        if (previousDriverApp != DriverApp.UNKNOWN) {
            cancelCapturePipeline(OverlayLatencyTrace.EndReason.TRACE_SUPERSEDED_BY_NEW_DRIVER_EVENT)
            cancelOverlayExpiry()
            captureCoordinator.switchDriverApp(driverApp)
            lastParsedOfferAudit = null
            accessibilityPollingUntil = 0L
            mainScope.launch {
                overlayManager?.removeOverlay()
                overlayManager?.setForegroundPackage(packageName)
            }
            Log.w(
                TAG,
                "CALCMOT_DRIVER_APP_SWITCH previous=${previousDriverApp.id} next=${driverApp.id} " +
                    "package=${DriverAppPackagePolicy.describe(packageName)}"
            )
            if (driverApp == DriverApp.NINETY_NINE || previousDriverApp == DriverApp.NINETY_NINE) {
                ninetyNineDiagnostics.recordDriverAppSwitch(
                    previous = previousDriverApp.id,
                    next = driverApp.id,
                    packageName = packageName
                )
            }
        } else {
            captureCoordinator.switchDriverApp(driverApp)
        }
        trustedForegroundDriverApp = driverApp
    }

    private fun configureRuntimeProfileForDriverApp(driverApp: DriverApp) {
        val currentInfo = serviceInfo ?: return
        val targetEventTypes = DriverAccessibilityEventPolicy.eventTypesFor(driverApp)
        val targetFlags = ninetyNineSemanticBridgeProbe.flagsFor(driverApp, currentInfo.flags)
        if (currentInfo.eventTypes == targetEventTypes && currentInfo.flags == targetFlags) return
        currentInfo.eventTypes = targetEventTypes
        currentInfo.flags = targetFlags
        serviceInfo = currentInfo
        if (driverApp == DriverApp.NINETY_NINE) {
            ninetyNineDiagnostics.recordServiceConfiguration(
                "event-profile=99 eventTypes=${currentInfo.eventTypes} flags=${currentInfo.flags}"
            )
        }
    }

    private fun beginOrCoalesceCaptureSession(
        eventAtMillis: Long,
        packageName: String,
        eventType: Int,
        windowId: Int
    ): CaptureSession {
        val now = System.currentTimeMillis()
        val canCoalesce = capturePipelineJob?.isActive == true &&
            activeScanSessionGeneration == captureGeneration &&
            now - activeScanSessionStartedAtMillis <= ACCESSIBILITY_SCAN_SESSION_COALESCE_MS

        if (canCoalesce) {
            accessibilityPollingEventAtMillis = eventAtMillis
            pendingCaptureAfterCurrent = true
            pendingCaptureEventAtMillis = eventAtMillis
            pendingCapturePackageName = packageName
            pendingCaptureEventType = eventType
            pendingCaptureWindowId = windowId
            val activeTrace = latencyTraceForGeneration(activeScanSessionGeneration)
            activeTrace?.metric(
                name = "capture.coalesced",
                durationMs = now - activeScanSessionStartedAtMillis,
                details = "activeTraceId=${activeTrace.traceId} targetGeneration=$activeScanSessionGeneration " +
                    "elapsedSinceSessionMs=${now - activeScanSessionStartedAtMillis} pendingAfterCurrent=true"
            )
            return CaptureSession(
                generation = activeScanSessionGeneration,
                shouldStartBurst = false,
                trace = activeTrace
            )
        }

        if (activeScanSessionGeneration == captureGeneration && activeScanSessionGeneration != 0L) {
            latencyTraceForGeneration(captureGeneration)
                ?.close(OverlayLatencyTrace.EndReason.TRACE_SUPERSEDED_BY_NEW_DRIVER_EVENT)
        }
        capturePipelineJob?.cancel()
        clearPendingCapture()
        captureGeneration += 1
        activeScanSessionGeneration = captureGeneration
        activeScanSessionStartedAtMillis = now
        val latencyTrace = OverlayLatencyTrace.start(
            packageName = packageName,
            eventType = eventType,
            windowId = windowId,
            visualProbe = consumeRecentVisualProbe()
        ).mark(OverlayLatencyTrace.Stage.T1_PACKAGE_ALLOWED_CONFIRMED)
        latencyTracesByGeneration[captureGeneration] = latencyTrace
        cleanupLatencyTraces()
        return CaptureSession(
            generation = captureGeneration,
            shouldStartBurst = true,
            trace = latencyTrace
        )
    }

    private fun cancelCapturePipeline(
        endReason: OverlayLatencyTrace.EndReason = OverlayLatencyTrace.EndReason.NO_CANDIDATE_AFTER_TREE
    ) {
        captureGeneration += 1
        activeScanSessionGeneration = 0L
        activeScanSessionStartedAtMillis = 0L
        clearPendingCapture()
        closeAllLatencyTraces(endReason)
        accessibilityPollingJob?.cancel()
        accessibilityPollingJob = null
        capturePipelineJob?.cancel()
        capturePipelineJob = null
    }

    private fun clearPendingCapture() {
        pendingCaptureAfterCurrent = false
        pendingCaptureEventAtMillis = 0L
        pendingCapturePackageName = null
        pendingCaptureEventType = 0
        pendingCaptureWindowId = 0
    }

    private fun startPendingCaptureAfterCurrentIfNeeded() {
        if (!pendingCaptureAfterCurrent) return
        val eventAtMillis = pendingCaptureEventAtMillis
        val packageName = pendingCapturePackageName
        val eventType = pendingCaptureEventType
        val windowId = pendingCaptureWindowId
        clearPendingCapture()

        if (packageName.isNullOrBlank() || !DriverAppPackagePolicy.isDriverPackage(packageName)) return
        if (!isCurrentForegroundPackageAllowed()) return
        if (!AppSettings.isMonitoringEnabled(this)) return

        val session = beginOrCoalesceCaptureSession(
            eventAtMillis = eventAtMillis,
            packageName = packageName,
            eventType = eventType,
            windowId = windowId
        )
        session.trace?.metric(
            name = "capture.pendingAfterCurrent",
            durationMs = 0L,
            details = "generation=${session.generation} shouldStartBurst=${session.shouldStartBurst}"
        )
        if (!session.shouldStartBurst) return

        capturePipelineJob = serviceScope.launch {
            runCapturePipeline(event = null, eventAtMillis = eventAtMillis, generation = session.generation)
        }
    }

    private fun closeAllLatencyTraces(reason: OverlayLatencyTrace.EndReason) {
        latencyTracesByGeneration.values.forEach { trace ->
            trace.close(reason)
        }
        latencyTracesByGeneration.clear()
    }

    private fun isCurrentGeneration(generation: Long): Boolean {
        return generation == captureGeneration
    }

    private fun latencyTraceForGeneration(generation: Long): OverlayLatencyTrace? {
        return latencyTracesByGeneration[generation]
    }

    private fun cleanupLatencyTraces() {
        val oldestGenerationToKeep = captureGeneration - 2
        latencyTracesByGeneration.keys.removeIf { it < oldestGenerationToKeep }
    }

    private fun consumeRecentVisualProbe(): OverlayLatencyTrace.VisualProbe? {
        val probe = latestVisualProbe ?: return null
        val ageMs = SystemClock.elapsedRealtime() - probe.elapsedRealtime
        return if (ageMs in 0..VISUAL_PROBE_MAX_AGE_MS) {
            latestVisualProbe = null
            probe
        } else {
            Log.w(TAG, "CALCMOT_LATENCY_VISUAL_PROBE_EXPIRED ageMs=$ageMs")
            latestVisualProbe = null
            null
        }
    }

    private suspend fun runCapturePipeline(
        event: AccessibilityEvent?,
        eventAtMillis: Long,
        generation: Long
    ) {
        coroutineScope {
            val trace = latencyTraceForGeneration(generation)
            if (trustedForegroundDriverApp == DriverApp.NINETY_NINE) {
                handleNinetyNineVisualCapture(label = "99-event", trace = trace)
                return@coroutineScope
            }
            if (event != null && handleEventPayloadCandidate(event, eventAtMillis, trace)) return@coroutineScope
            if (event != null &&
                trustedForegroundDriverApp == DriverApp.NINETY_NINE &&
                handleEventSourceCandidate(event.source, eventAtMillis)
            ) {
                return@coroutineScope
            }
            val handledByTree = handleAccessibilityCandidateBurst(eventAtMillis, generation)
            if (!handledByTree && isCurrentGeneration(generation)) {
                rejectCurrentFrame(
                    reason = "Accessibility tree did not expose a complete offer",
                    source = OfferCaptureSource.ACCESSIBILITY_TREE,
                    rejectionReason = OfferCaptureRejectionReason.NOT_CARD_LIKE
                )
                trace?.close(OverlayLatencyTrace.EndReason.NO_CANDIDATE_AFTER_TREE)
            }
        }
        if (isCurrentGeneration(generation)) {
            activeScanSessionGeneration = 0L
            activeScanSessionStartedAtMillis = 0L
            capturePipelineJob = null
            startPendingCaptureAfterCurrentIfNeeded()
        }
    }

    private suspend fun handleEventPayloadCandidate(
        event: AccessibilityEvent,
        eventAtMillis: Long,
        trace: OverlayLatencyTrace?
    ): Boolean {
        val snapshot = event.toAccessibilitySnapshot(eventAtMillis)
        if (snapshot.lines.isEmpty()) return false

        val scanResult = extractCandidateFromSnapshot(snapshot, trace)
        return scanResult != null && handleAccessibilityScanResult(
            scanResult = scanResult,
            label = "event-payload",
            trace = trace
        )
    }

    private suspend fun handleEventSourceCandidate(
        source: AccessibilityNodeInfo?,
        eventAtMillis: Long
    ): Boolean {
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
            val scanResult = extractCandidateFromSnapshot(snapshot, trace = null)
            if (scanResult != null) {
                return handleAccessibilityScanResult(
                    scanResult = scanResult,
                    label = "event-source",
                    trace = null
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
                    closeNinetyNineCaptureEngine()
                    captureCoordinator.reset()
                    cancelOverlayExpiry()
                    mainScope.launch { overlayManager?.hideOverlay() }
                    return@launch
                }
                if (!isCurrentForegroundPackageAllowed()) {
                    return@launch
                }

                val scanResult = extractCandidateFromAccessibilityTree(
                    eventAtMillis = accessibilityPollingEventAtMillis,
                    logRejectedTree = false
                )
                if (scanResult != null) {
                    handleAccessibilityScanResult(
                        scanResult = scanResult,
                        label = "accessibility-poll",
                        trace = latencyTraceForGeneration(captureGeneration)
                    )
                    if (scanResult is AccessibilityScanResult.InvalidContext) return@launch
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
                runCatching {
                    bootstrapNinetyNineForegroundFromActiveRoot()
                }.onFailure { error ->
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "CALCMOT_99_FOREGROUND_BOOTSTRAP_FAILED", error)
                    }
                }
                if (!isCurrentForegroundPackageAllowed()) {
                    continue
                }

                captureCoordinator.expireOverlayIfStale()
                    ?.let { applyCaptureDecision(it, "overlay-heartbeat-ttl") }

                if (trustedForegroundDriverApp == DriverApp.NINETY_NINE) {
                    runFocusedNinetyNineHeartbeatScanIfNeeded()
                    continue
                }
                runFocusedUberWatchdogScanIfNeeded()
            }
        }
    }

    private fun bootstrapNinetyNineForegroundFromActiveRoot() {
        if (trustedForegroundDriverApp == DriverApp.NINETY_NINE &&
            trustedForegroundDecision == PackageDecision.DRIVER_APP
        ) {
            return
        }

        val rootPackage = rootInActiveWindow
            ?.packageName
            ?.toString()
            ?.takeIf { DriverAppPackagePolicy.driverAppForPackage(it) == DriverApp.NINETY_NINE }
        val interactiveWindows = if (rootPackage == null) allInteractiveWindowsForScan() else emptyList()
        val activePackage = rootPackage
            ?: interactiveWindows
                .firstOrNull { window ->
                    (window.isActive || window.isFocused) &&
                        DriverApp.NINETY_NINE.ownsPackage(window.root?.packageName)
                }
                ?.root
                ?.packageName
                ?.toString()
            ?: return

        switchDriverAppIfNeeded(DriverApp.NINETY_NINE, activePackage)
        configureRuntimeProfileForDriverApp(DriverApp.NINETY_NINE)
        AppSettings.setLastDriverApp(this, DriverApp.NINETY_NINE)
        updateTrustedForeground(activePackage, PackageDecision.DRIVER_APP)
        overlayManager?.setForegroundPackage(activePackage)
        ninetyNineDiagnostics.recordServiceConfiguration(
            "foreground-bootstrap=active-root package=${DriverAppPackagePolicy.describe(activePackage)}"
        )
    }

    private suspend fun runFocusedNinetyNineHeartbeatScanIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastWatchdogScanAtMillis < NINETY_NINE_HEARTBEAT_SCAN_INTERVAL_MS) return
        if (capturePipelineJob?.isActive == true || accessibilityPollingJob?.isActive == true) return
        if (!isCurrentForegroundPackageAllowed()) return
        if (!hasUberRootAvailable()) return

        lastWatchdogScanAtMillis = now
        handleNinetyNineVisualCapture(label = "99-heartbeat", trace = null)
    }

    private suspend fun handleNinetyNineVisualCapture(
        label: String,
        trace: OverlayLatencyTrace?
    ): Boolean {
        if (trustedForegroundDriverApp != DriverApp.NINETY_NINE) return false
        if (!isCurrentForegroundPackageAllowed()) return false
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) return false
        val targetBounds = findNinetyNineCaptureBounds() ?: run {
            recordCaptureRejection(
                OfferCaptureSource.NINETY_NINE_OCR,
                OfferCaptureRejectionReason.INVALID_FRAME
            )
            return false
        }

        val startedAt = SystemClock.elapsedRealtime()
        val result = getOrCreateNinetyNineCaptureEngine().captureAndExtract(
            targetBounds = targetBounds,
            excludedScreenBounds = listOfNotNull(overlayManager?.visibleBounds)
        )
        trace?.metric(
            name = "99.visualCapture",
            durationMs = SystemClock.elapsedRealtime() - startedAt,
            details = "label=$label result=${result.javaClass.simpleName}"
        )
        return when (result) {
            is NinetyNineCaptureResult.Extracted -> {
                when (val extraction = result.result) {
                    is NinetyNineExtractionResult.Candidate -> {
                        recordCaptureSuccess(OfferCaptureSource.NINETY_NINE_OCR, extraction.value)
                        Log.w(
                            TAG,
                            "CALCMOT_99_OCR_CANDIDATE label=$label " +
                                "fingerprint=${extraction.value.fingerprint} trustedSingleFrame=true"
                        )
                        if (BuildConfig.DEBUG) {
                            ninetyNineDiagnostics.recordOcrResult(
                                status = "candidate",
                                detail = extraction.value.fingerprint,
                                rawText = extraction.sanitizedText
                            )
                        }
                        handleCandidate(
                            candidate = extraction.value,
                            source = OfferCaptureSource.NINETY_NINE_OCR,
                            label = label,
                            trustedSingleFrame = true,
                            trace = trace
                        )
                    }

                    is NinetyNineExtractionResult.Rejected -> {
                        val rejectionReason = extraction.reason.toCaptureRejectionReason()
                        recordCaptureRejection(OfferCaptureSource.NINETY_NINE_OCR, rejectionReason)
                        Log.w(
                            TAG,
                            "CALCMOT_99_OCR_REJECTED label=$label reason=${extraction.reason.name}"
                        )
                        if (BuildConfig.DEBUG) {
                            ninetyNineDiagnostics.recordOcrResult(
                                status = "rejected",
                                detail = extraction.reason.name,
                                rawText = extraction.sanitizedText
                            )
                        }
                        false
                    }
                }
            }

            is NinetyNineCaptureResult.Skipped -> {
                if (result.reason != NinetyNineCaptureSkipReason.COOLDOWN &&
                    result.reason != NinetyNineCaptureSkipReason.BUSY &&
                    result.reason != NinetyNineCaptureSkipReason.UNCHANGED_FRAME
                ) {
                    Log.w(
                        TAG,
                        "CALCMOT_99_OCR_SKIPPED label=$label reason=${result.reason.name}"
                    )
                    recordCaptureRejection(
                        OfferCaptureSource.NINETY_NINE_OCR,
                        OfferCaptureRejectionReason.INVALID_FRAME
                    )
                }
                if (BuildConfig.DEBUG &&
                    result.reason != NinetyNineCaptureSkipReason.COOLDOWN &&
                    result.reason != NinetyNineCaptureSkipReason.BUSY &&
                    result.reason != NinetyNineCaptureSkipReason.UNCHANGED_FRAME
                ) {
                    ninetyNineDiagnostics.recordOcrResult(
                        status = "skipped",
                        detail = result.reason.name,
                        rawText = null
                    )
                }
                false
            }
        }
    }

    private fun getOrCreateNinetyNineCaptureEngine(): NinetyNineCaptureEngine {
        return ninetyNineCaptureEngine ?: NinetyNineCaptureEngine(
            captureSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                AccessibilityScreenshotCaptureSource(this)
            } else {
                UnsupportedNinetyNineCaptureSource
            }
        ).also { ninetyNineCaptureEngine = it }
    }

    private fun closeNinetyNineCaptureEngine() {
        ninetyNineCaptureEngine?.close()
        ninetyNineCaptureEngine = null
    }

    private fun findNinetyNineCaptureBounds(): Rect? {
        val roots = buildList {
            activeWindowRootsForScan().forEach { add(it.root) }
            allInteractiveWindowsForScan().forEach { it.root?.let(::add) }
        }.distinctBy { root ->
            val bounds = Rect()
            runCatching { root.getBoundsInScreen(bounds) }
            "${root.windowId}|${root.packageName}|${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
        }

        var packageBounds: Rect? = null
        roots.forEach { root ->
            if (!DriverApp.NINETY_NINE.ownsPackage(root.packageName)) return@forEach
            val rootBounds = Rect().also { root.getBoundsInScreen(it) }
            if (rootBounds.width() > 0 && rootBounds.height() > 0) {
                packageBounds = rootBounds
            }
            if (root.containsNinetyNineTriggerView()) {
                return rootBounds.takeIf { it.width() > 0 && it.height() > 0 }
            }
        }
        return packageBounds
    }

    private fun AccessibilityNodeInfo.containsNinetyNineTriggerView(): Boolean {
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(this)
        var visited = 0
        while (queue.isNotEmpty() && visited < NINETY_NINE_TRIGGER_SCAN_NODE_LIMIT) {
            val node = queue.removeFirst()
            visited += 1
            if (node.viewIdResourceName in NinetyNineRecognitionConfig.triggerViewIds) return true
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }
        return false
    }

    private fun NinetyNineExtractionRejection.toCaptureRejectionReason(): OfferCaptureRejectionReason {
        return when (this) {
            NinetyNineExtractionRejection.EMPTY_OCR -> OfferCaptureRejectionReason.INVALID_FRAME
            NinetyNineExtractionRejection.INACTIVE_FRAME -> OfferCaptureRejectionReason.INVALID_CONTEXT_NO_REQUEST
            NinetyNineExtractionRejection.NO_OFFER_MARKER -> OfferCaptureRejectionReason.NOT_CARD_LIKE
            NinetyNineExtractionRejection.PARSER_REJECTED -> OfferCaptureRejectionReason.PARSER_REJECTED
        }
    }

    private suspend fun runFocusedUberWatchdogScanIfNeeded() {
        val now = System.currentTimeMillis()
        if (overlayManager?.isVisible != true) return
        if (now - lastWatchdogScanAtMillis < ACCESSIBILITY_WATCHDOG_SCAN_INTERVAL_MS) return
        if (capturePipelineJob?.isActive == true || accessibilityPollingJob?.isActive == true) return
        if (!isCurrentForegroundPackageAllowed()) return
        if (!hasUberRootAvailable()) return

        lastWatchdogScanAtMillis = now
        val scanResult = extractCandidateFromAccessibilityTree(
            eventAtMillis = now,
            logRejectedTree = false,
            generationId = captureGeneration,
            delayMs = 0L
        ) ?: return

        handleAccessibilityScanResult(
            scanResult = scanResult,
            label = "accessibility-watchdog",
            trace = latencyTraceForGeneration(captureGeneration)
        )
    }

    private fun hasUberRootAvailable(): Boolean {
        if (rootInActiveWindow?.containsDriverPackage(trustedForegroundDriverApp) == true) return true

        return allInteractiveWindowsForScan().any { windowSource ->
            windowSource.root?.containsDriverPackage(trustedForegroundDriverApp) == true
        }
    }

    private fun hasVisibleNinetyNineRoot(): Boolean {
        if (DriverApp.NINETY_NINE.ownsPackage(rootInActiveWindow?.packageName)) return true
        return allInteractiveWindowsForScan().any { window ->
            (window.isActive || window.isFocused) &&
                DriverApp.NINETY_NINE.ownsPackage(window.root?.packageName)
        }
    }

    private fun activeFocusedDriverAppForGuard(): DriverApp {
        driverAppFromRoot(rootInActiveWindow)?.let { return it }
        return allInteractiveWindowsForScan()
            .asSequence()
            .filter { it.isActive || it.isFocused }
            .mapNotNull { driverAppFromRoot(it.root) }
            .firstOrNull()
            ?: DriverApp.UNKNOWN
    }

    private fun driverAppFromRoot(root: AccessibilityNodeInfo?): DriverApp? {
        root ?: return null
        DriverAppPackagePolicy.driverAppForPackage(root.packageName)
            .takeIf { it != DriverApp.UNKNOWN }
            ?.let { return it }
        return DriverApp.supported.firstOrNull { driverApp ->
            root.containsDriverPackage(driverApp)
        }
    }

    private fun isCurrentForegroundPackageAllowed(): Boolean {
        return trustedForegroundDecision == PackageDecision.DRIVER_APP &&
            DriverAppPackagePolicy.isDriverPackage(trustedForegroundPackageName)
    }

    private fun canContinueDriverTreeScan(generation: Long): Boolean {
        return isCurrentGeneration(generation) && isCurrentForegroundPackageAllowed()
    }

    private suspend fun handleAccessibilityCandidateBurst(
        eventAtMillis: Long,
        generation: Long
    ): Boolean {
        var foundCandidate = false
        var previousDelay = 0L
        val trace = latencyTraceForGeneration(generation)
        val delays = DriverAccessibilityEventPolicy.burstDelaysFor(
            driverApp = trustedForegroundDriverApp,
            debugTimingSweepEnabled = AccessibilityDebugConfig.ENABLE_TIMING_SWEEP,
            debugTimingSweepDelaysMs = AccessibilityDebugConfig.TIMING_SWEEP_DELAYS_MS
        )

        delays.forEachIndexed { attempt, targetDelay ->
            if (!canContinueDriverTreeScan(generation)) return false
            val sleepStart = SystemClock.elapsedRealtime()
            delay(targetDelay - previousDelay)
            val actualSleepMs = SystemClock.elapsedRealtime() - sleepStart
            previousDelay = targetDelay
            trace?.metric(
                name = "burst.delay",
                durationMs = actualSleepMs,
                details = "generation=$generation attempt=${attempt + 1}/${delays.size} targetDelayMs=$targetDelay"
            )
            if (!canContinueDriverTreeScan(generation)) return false

            val scanResult = extractCandidateFromAccessibilityTree(
                eventAtMillis = eventAtMillis,
                logRejectedTree = attempt == delays.lastIndex,
                generationId = generation,
                delayMs = targetDelay
            )
            if (scanResult != null) {
                if (scanResult is AccessibilityScanResult.InvalidContext) {
                    val isWeakInvalidContext = !scanResult.reason.isImmediateInvalidContext()
                    if (isWeakInvalidContext && attempt == 0) {
                        if (BuildConfig.DEBUG) {
                            Log.i(
                                TAG,
                                "Weak invalid context observed; waiting one more burst attempt " +
                                    "reason=${scanResult.reason} generation=$generation delay=${targetDelay}ms"
                            )
                        }
                        return@forEachIndexed
                    }
                    handleAccessibilityScanResult(
                        scanResult = scanResult,
                        label = "accessibility-burst",
                        trace = trace
                    )
                    return true
                }

                val treeCandidate = (scanResult as AccessibilityScanResult.Candidate).value
                foundCandidate = true
                if (BuildConfig.DEBUG) {
                    Log.i(
                        TAG,
                        "Accessibility tree produced offer generation=$generation " +
                            "delay=${targetDelay}ms attempt=${attempt + 1}/${delays.size}"
                    )
                }
                if (handleAccessibilityScanResult(scanResult, "accessibility-burst", trace)) {
                    return true
                }
            }
        }

        return foundCandidate
    }

    private suspend fun extractCandidateFromAccessibilityTree(
        eventAtMillis: Long,
        logRejectedTree: Boolean = true,
        generationId: Long = captureGeneration,
        delayMs: Long = (System.currentTimeMillis() - eventAtMillis).coerceAtLeast(0L)
    ): AccessibilityScanResult? {
        if (!canContinueDriverTreeScan(generationId)) return null
        val trace = latencyTraceForGeneration(generationId)
        AppDiagnostics.recordStage(this, AppDiagnostics.Stage.TREE_SCAN_RUNNING)
        val calcMotOverlayRemovedForScan = removeOverlayWindowsBeforeScanIfNeeded()
        if (!canContinueDriverTreeScan(generationId)) return null
        trace?.mark(OverlayLatencyTrace.Stage.T2_ROOT_READ_START)
        Log.w(TAG, "CALCMOT_ROOT_READ stage=start generation=$generationId delay=${delayMs}ms")
        val rootReadStartedAt = SystemClock.elapsedRealtime()
        val rootNull = rootInActiveWindow == null
        val windowCount = runCatching { windows.size }.getOrDefault(0)
        val roots = accessibilityRootSources(trace)
        if (trustedForegroundDriverApp == DriverApp.NINETY_NINE) {
            ninetyNineDiagnostics.recordRootSelection(
                generation = generationId,
                delayMs = delayMs,
                rootNull = rootNull,
                windowCount = windowCount,
                roots = roots.map {
                    "${it.name} kind=${it.sourceKind} driver=${it.isDriverRoot} " +
                        "activeOrFocused=${it.isActiveOrFocused} package=${it.root.packageName} " +
                        "class=${it.root.className} children=${it.root.childCount}"
                }
            )
            ninetyNineSemanticBridgeProbe.requestSemantics(
                generation = generationId,
                delayMs = delayMs,
                roots = roots.map { it.name to it.root }
            )
        }
        if (!canContinueDriverTreeScan(generationId)) {
            Log.w(
                TAG,
                "CALCMOT_TREE_EXTRACT_ABORTED_NON_DRIVER_FOREGROUND generation=$generationId " +
                    "delay=${delayMs}ms"
            )
            return null
        }
        trace?.metric(
            name = "root.totalRead",
            durationMs = SystemClock.elapsedRealtime() - rootReadStartedAt,
            details = "generation=$generationId rootNull=$rootNull windowCount=$windowCount rootCount=${roots.size}"
        )
        trace?.mark(
            OverlayLatencyTrace.Stage.T3_ROOT_READ_END,
            reason = "rootCount=${roots.size} rootPackages=${roots.joinToString(separator = ",") { it.root.packageName?.toString().orEmpty() }}"
        )
        Log.w(
            TAG,
            "CALCMOT_ROOT_READ stage=end generation=$generationId rootNull=$rootNull " +
                "windowCount=$windowCount rootCount=${roots.size}"
        )
        if (roots.isEmpty()) {
            AppDiagnostics.recordStage(this, AppDiagnostics.Stage.CARD_UNCERTAIN)
            val emptySnapshot = emptyAccessibilitySnapshot(
                sourceName = "empty-roots",
                eventAtMillis = eventAtMillis,
                windowCount = windowCount,
                generationId = generationId,
                delayMs = delayMs
            )
            val emptyInspection = inspectOfferTree(emptySnapshot, trace)
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
            logNoCandidateAfterTreeDetails(
                trace = trace,
                generationId = generationId,
                rootNull = rootNull,
                windowCount = windowCount,
                roots = emptyList(),
                snapshotsToInspect = emptyList(),
                rejectedReasons = emptyList(),
                bestRejectionReason = failure.label,
                combinedWindowsRan = false
            )
            if (trustedForegroundDriverApp == DriverApp.NINETY_NINE) {
                ninetyNineDiagnostics.recordNoCandidate(
                    generation = generationId,
                    delayMs = delayMs,
                    bestReason = failure.label,
                    rejectedReasons = emptyList()
                )
            }
            return null
        }

        val rejectedReasons = mutableListOf<String>()
        var weakInvalidContextReason: OfferCaptureRejectionReason? = null
        trace?.mark(OverlayLatencyTrace.Stage.T4_TREE_EXTRACT_START)
        val snapshotsToInspect = mutableListOf<AccessibilityTreeSnapshot>()
        var combinedWindowsRan = false

        fun inspectSnapshot(snapshot: AccessibilityTreeSnapshot): AccessibilityScanResult? {
            AppDiagnostics.recordStage(this, AppDiagnostics.Stage.TREE_SNAPSHOT_CAPTURED)
            val inspectStartedAt = SystemClock.elapsedRealtime()
            val inspection = inspectOfferTree(snapshot, trace)
            trace?.metric(
                name = "tree.inspect",
                durationMs = SystemClock.elapsedRealtime() - inspectStartedAt,
                details = "source=${snapshot.sourceName} nodeCount=${snapshot.nodeCount} lineCount=${snapshot.lines.size}"
            )
            AppDiagnostics.recordTreeInspection(this, snapshot, inspection)
            accessibilityTreeLab.record(snapshot, inspection)
            captureLearningLab.recordTreeInspection(snapshot, inspection)

            val parseStartedAt = SystemClock.elapsedRealtime()
            val candidate = inspection.offerText?.let {
                DriverOfferParser.parse(snapshot.driverApp, it)
            }
            if (snapshot.driverApp == DriverApp.NINETY_NINE) {
                ninetyNineDiagnostics.recordSnapshot(snapshot, inspection, candidate)
            }
            trace?.metric(
                name = "candidate.parse",
                durationMs = SystemClock.elapsedRealtime() - parseStartedAt,
                details = "source=${snapshot.sourceName} complete=${inspection.isCompleteOffer} candidate=${candidate != null}"
            )
            if (BuildConfig.DEBUG) {
                Log.w(
                    TAG,
                    "CARD_PATH: source=${snapshot.sourceName} " +
                        "complete=${inspection.isCompleteOffer} " +
                        "candidate=${candidate != null} " +
                        "reason=${inspection.rejectionReason} " +
                        "delay=${delayMs}ms"
                )
            }
            inspection.strongInvalidContextCaptureReason()?.let { reason ->
                return AccessibilityScanResult.InvalidContext(reason)
            }
            inspection.weakInvalidContextCaptureReason()?.let { reason ->
                weakInvalidContextReason = weakInvalidContextReason ?: reason
            }
            if (candidate != null) {
                trace
                    ?.withCandidate(candidate)
                    ?.mark(OverlayLatencyTrace.Stage.T6_CANDIDATE_PARSED)
                    ?.mark(OverlayLatencyTrace.Stage.T7_CANDIDATE_COMPLETE)
                val trustedSingleFrame = inspection.canBypassStabilityGate()
                if (shouldIgnorePostReconnectCandidate(candidate, trustedSingleFrame, snapshot.sourceName)) {
                    rejectedReasons += "${snapshot.sourceName}:POST_RECONNECT_STALE_CANDIDATE"
                    return null
                }
                recordSuspectValueChangeIfNeeded(
                    candidate = candidate,
                    source = OfferCaptureSource.ACCESSIBILITY_TREE,
                    offerText = inspection.offerText,
                    snapshotName = snapshot.sourceName
                )
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
                return AccessibilityScanResult.Candidate(
                    AccessibilityCandidate(
                        candidate = candidate,
                        trustedSingleFrame = trustedSingleFrame
                    )
                )
            }
            recordTreeCaptureRejectionIfUseful(inspection)
            rejectedReasons += "${snapshot.sourceName}:${inspection.rejectionReason}"
            return null
        }

        roots.forEach { rootSource ->
            if (!canContinueDriverTreeScan(generationId)) return null
            val snapshotStartedAt = SystemClock.elapsedRealtime()
            val snapshot = rootSource.root.toAccessibilitySnapshot(
                sourceName = rootSource.name,
                eventAtMillis = eventAtMillis,
                windowCount = rootSource.windowCount,
                rootCount = roots.size,
                generationId = generationId,
                sourceKind = rootSource.sourceKind,
                trace = trace
            )
            trace?.metric(
                name = "tree.snapshot",
                durationMs = SystemClock.elapsedRealtime() - snapshotStartedAt,
                details = "source=${rootSource.name} nodeCount=${snapshot.nodeCount} lineCount=${snapshot.lines.size} truncated=${snapshot.truncated}"
            )
            if (!canContinueDriverTreeScan(generationId)) return null
            snapshotsToInspect += snapshot
            inspectSnapshot(snapshot)?.let { result ->
                trace?.mark(
                    OverlayLatencyTrace.Stage.T5_TREE_EXTRACT_END,
                    reason = "snapshotCount=${snapshotsToInspect.size} early=true"
                )
                Log.w(
                    TAG,
                    "CALCMOT_TREE_EXTRACT generation=$generationId delay=${delayMs}ms " +
                        "rootCount=${roots.size} snapshotCount=${snapshotsToInspect.size} early=true"
                )
                return result
            }
        }

        if (!canContinueDriverTreeScan(generationId)) return null
        snapshotsToInspect.toCombinedSnapshotOrNull(eventAtMillis)?.let { combinedSnapshot ->
            combinedWindowsRan = true
            snapshotsToInspect += combinedSnapshot
            if (!canContinueDriverTreeScan(generationId)) return null
            inspectSnapshot(combinedSnapshot)?.let { result ->
                trace?.mark(
                    OverlayLatencyTrace.Stage.T5_TREE_EXTRACT_END,
                    reason = "snapshotCount=${snapshotsToInspect.size} combined=true"
                )
                Log.w(
                    TAG,
                    "CALCMOT_TREE_EXTRACT generation=$generationId delay=${delayMs}ms " +
                        "rootCount=${roots.size} snapshotCount=${snapshotsToInspect.size} combined=true"
                )
                return result
            }
        }

        trace?.mark(
            OverlayLatencyTrace.Stage.T5_TREE_EXTRACT_END,
            reason = "snapshotCount=${snapshotsToInspect.size}"
        )
        Log.w(
            TAG,
            "CALCMOT_TREE_EXTRACT generation=$generationId delay=${delayMs}ms " +
                "rootCount=${roots.size} snapshotCount=${snapshotsToInspect.size}"
        )

        weakInvalidContextReason?.let { reason ->
            if (BuildConfig.DEBUG) {
                Log.i(
                    TAG,
                    "TREE_SCAN_STOP_WEAK_INVALID_CONTEXT: " +
                        "reason=$reason generation=$generationId delay=${delayMs}ms"
                )
            }
            return AccessibilityScanResult.InvalidContext(reason)
        }

        val currentBestSnapshot = snapshotsToInspect.maxWithOrNull(
            compareBy<AccessibilityTreeSnapshot> { it.nodes.count { node -> !node.contentDescriptionRaw.isNullOrBlank() } }
                .thenBy { it.nodes.count { node -> !node.textRaw.isNullOrBlank() } }
                .thenBy { it.nodeCount }
        )
        val currentBestInspection = currentBestSnapshot?.let { inspectOfferTree(it, trace) }
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
        logNoCandidateAfterTreeDetails(
            trace = trace,
            generationId = generationId,
            rootNull = rootNull,
            windowCount = windowCount,
            roots = roots,
            snapshotsToInspect = snapshotsToInspect,
            rejectedReasons = rejectedReasons,
            bestRejectionReason = currentFailure.label,
            combinedWindowsRan = combinedWindowsRan
        )
        if (trustedForegroundDriverApp == DriverApp.NINETY_NINE) {
            ninetyNineDiagnostics.recordNoCandidate(
                generation = generationId,
                delayMs = delayMs,
                bestReason = currentFailure.label,
                rejectedReasons = rejectedReasons
            )
        }
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

    private fun logNoCandidateAfterTreeDetails(
        trace: OverlayLatencyTrace?,
        generationId: Long,
        rootNull: Boolean,
        windowCount: Int,
        roots: List<AccessibilityRootSource>,
        snapshotsToInspect: List<AccessibilityTreeSnapshot>,
        rejectedReasons: List<String>,
        bestRejectionReason: String,
        combinedWindowsRan: Boolean
    ) {
        val topSources = snapshotsToInspect
            .take(5)
            .joinToString(separator = ",") { it.sourceName }
            .ifBlank { "none" }
        Log.w(
            TAG,
            "CALCMOT_NO_CANDIDATE_AFTER_TREE_DETAILS " +
                "traceId=${trace?.traceId ?: "none"} generation=$generationId " +
                "package=${trace?.packageNameForDiagnostics() ?: DriverAppPackagePolicy.describe(trustedForegroundPackageName)} " +
                "eventType=${trace?.eventTypeForDiagnostics() ?: -1} " +
                "windowId=${trace?.windowIdForDiagnostics() ?: -1} " +
                "rootCount=${roots.size} " +
                "driverRootCount=${roots.count { it.isDriverRoot }} " +
                "unverifiedRootCount=${roots.count { !it.isDriverRoot }} " +
                "rootInActiveWindowExists=${!rootNull} windowsExists=${windowCount > 0} " +
                "topSources=$topSources bestRejectionReason=$bestRejectionReason " +
                "combinedWindowsRan=$combinedWindowsRan " +
                "rejectedReasons=${rejectedReasons.take(MAX_ACCESSIBILITY_LOG_REASONS).joinToString(separator = ",").ifBlank { "none" }}"
        )
    }

    private suspend fun removeOverlayWindowsBeforeScanIfNeeded(): Boolean {
        if (!AccessibilityDebugConfig.ENABLE_ZERO_OVERLAY_DURING_SCAN) return false
        if (overlayManager?.isVisible == true) return false
        
        val removed = withContext(Dispatchers.Main.immediate) {
            overlayManager?.removeOverlayWindowsForScan() == true
        }

        if (removed && BuildConfig.DEBUG) {
            Log.i(TAG, "Removed CalcMot overlay windows before accessibility scan")
        }
        return removed
    }

    private fun accessibilityRootSources(trace: OverlayLatencyTrace? = null): List<AccessibilityRootSource> {
        val windowsSizeStartedAt = SystemClock.elapsedRealtime()
        val initialWindowCount = runCatching { windows.size }.getOrDefault(0)
        trace?.metric(
            name = "root.windowsSize",
            durationMs = SystemClock.elapsedRealtime() - windowsSizeStartedAt,
            details = "windowCount=$initialWindowCount"
        )
        val activeRootsStartedAt = SystemClock.elapsedRealtime()
        val activeRoots = activeWindowRootsForScan()
            .map { activeRoot ->
                val isDriverRoot = activeRoot.root.containsDriverPackage(trustedForegroundDriverApp)
                val packageHint = if (isDriverRoot) "driver" else "unverified"
                AccessibilityRootSource(
                    name = "$packageHint-${activeRoot.debugName}",
                    root = activeRoot.root,
                    windowCount = initialWindowCount,
                    sourceKind = AccessibilitySnapshotSourceKind.ROOT_IN_ACTIVE_WINDOW,
                    isDriverRoot = isDriverRoot,
                    isActiveOrFocused = true
                )
            }
        trace?.metric(
            name = "root.activeRoots",
            durationMs = SystemClock.elapsedRealtime() - activeRootsStartedAt,
            details = "count=${activeRoots.size} driverCount=${activeRoots.count { it.isDriverRoot }}"
        )

        val activeDriverRoots = activeRoots.filter { it.isDriverRoot }
        if (activeDriverRoots.isNotEmpty() && trustedForegroundDriverApp == DriverApp.UBER) {
            return activeDriverRoots.distinctAccessibilityRoots()
        }

        val allWindowsStartedAt = SystemClock.elapsedRealtime()
        val currentWindows = allInteractiveWindowsForScan(trace)
        trace?.metric(
            name = "root.allInteractiveWindows",
            durationMs = SystemClock.elapsedRealtime() - allWindowsStartedAt,
            details = "count=${currentWindows.size}"
        )
        val windowRoots = currentWindows
            .mapIndexedNotNull { index, windowSource ->
                val root = windowSource.root ?: return@mapIndexedNotNull null
                val isDriverRoot = root.containsDriverPackage(trustedForegroundDriverApp)
                val packageHint = if (isDriverRoot) "driver" else "unverified"
                AccessibilityRootSource(
                    name = "window-$index-$packageHint-${windowSource.debugName}",
                    root = root,
                    windowCount = currentWindows.size,
                    sourceKind = AccessibilitySnapshotSourceKind.WINDOW_ROOT,
                    isDriverRoot = isDriverRoot,
                    isActiveOrFocused = windowSource.isActive || windowSource.isFocused
                )
            }

        val driverWindowRoots = (
            windowRoots.filter { it.isDriverRoot && it.isActiveOrFocused } +
                windowRoots.filter { it.isDriverRoot && !it.isActiveOrFocused }
            ).distinctAccessibilityRoots()
        if (driverWindowRoots.isNotEmpty()) {
            return (activeDriverRoots + driverWindowRoots).distinctAccessibilityRoots()
        }

        return (activeRoots + windowRoots.filter { !it.isDriverRoot })
            .distinctAccessibilityRoots()
    }

    private fun List<AccessibilityRootSource>.distinctAccessibilityRoots(): List<AccessibilityRootSource> {
        return distinctBy {
            val bounds = Rect()
            runCatching { it.root.getBoundsInScreen(bounds) }
            "${it.sourceKind}|${it.root.packageName}|${it.root.className}|${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
        }
    }

    private fun allInteractiveWindowsForScan(trace: OverlayLatencyTrace? = null): List<AccessibilityWindowSource> {
        val currentDisplayStartedAt = SystemClock.elapsedRealtime()
        val fromCurrentDisplay = runCatching { windows }
            .getOrDefault(emptyList())
            .mapIndexed { index, window ->
                window.toAccessibilityWindowSource(displayLabel = "current", index = index)
            }
        trace?.metric(
            name = "root.windowsCurrentDisplay",
            durationMs = SystemClock.elapsedRealtime() - currentDisplayStartedAt,
            details = "count=${fromCurrentDisplay.size}"
        )

        val allDisplaysStartedAt = SystemClock.elapsedRealtime()
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
        trace?.metric(
            name = "root.windowsOnAllDisplays",
            durationMs = SystemClock.elapsedRealtime() - allDisplaysStartedAt,
            details = "count=${fromAllDisplays.size}"
        )

        return (fromAllDisplays + fromCurrentDisplay)
            .filter { it.root != null }
            .distinctBy {
                val root = it.root
                "${it.identityKey}|${root?.packageName}|${root?.className}"
            }
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

    private fun AccessibilityNodeInfo.containsDriverPackage(driverApp: DriverApp): Boolean {
        if (driverApp == DriverApp.UNKNOWN) return false
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(this)
        var visited = 0

        while (queue.isNotEmpty() && visited < PACKAGE_SCAN_NODE_LIMIT) {
            val node = queue.removeFirst()
            visited += 1
            if (driverApp.ownsPackage(node.packageName)) return true

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

    private fun inspectOfferTree(
        snapshot: AccessibilityTreeSnapshot,
        trace: OverlayLatencyTrace?
    ): TreeOfferInspection {
        return DriverOfferTreeExtractor.inspect(
            snapshot = snapshot,
            includeAuditFields = false
        ) { name, durationMs, details ->
                trace?.metric(
                    name = "tree.inspect.$name",
                    durationMs = durationMs,
                    details = "source=${snapshot.sourceName} $details"
                )
            }
    }

    private fun extractCandidateFromSnapshot(
        snapshot: AccessibilityTreeSnapshot,
        trace: OverlayLatencyTrace?
    ): AccessibilityScanResult? {
        trace?.mark(OverlayLatencyTrace.Stage.T4_TREE_EXTRACT_START)
        val inspectStartedAt = SystemClock.elapsedRealtime()
        val inspection = inspectOfferTree(snapshot, trace)
        trace?.metric(
            name = "tree.inspect",
            durationMs = SystemClock.elapsedRealtime() - inspectStartedAt,
            details = "source=${snapshot.sourceName} nodeCount=${snapshot.nodeCount} lineCount=${snapshot.lines.size}"
        )
        val parseStartedAt = SystemClock.elapsedRealtime()
        val candidate = inspection.offerText?.let {
            DriverOfferParser.parse(snapshot.driverApp, it)
        }
        if (snapshot.driverApp == DriverApp.NINETY_NINE) {
            ninetyNineDiagnostics.recordSnapshot(snapshot, inspection, candidate)
        }
        trace?.metric(
            name = "candidate.parse",
            durationMs = SystemClock.elapsedRealtime() - parseStartedAt,
            details = "source=${snapshot.sourceName} complete=${inspection.isCompleteOffer} candidate=${candidate != null}"
        )
        trace?.mark(
            OverlayLatencyTrace.Stage.T5_TREE_EXTRACT_END,
            reason = "snapshot=${snapshot.sourceName}"
        )

        if (BuildConfig.DEBUG) {
            Log.w(
                TAG,
                "CARD_PATH: source=${snapshot.sourceName} (snapshot) " +
                    "complete=${inspection.isCompleteOffer} " +
                    "candidate=${candidate != null} " +
                    "reason=${inspection.rejectionReason}"
            )
        }

        AppDiagnostics.recordTreeInspection(this, snapshot, inspection)
        accessibilityTreeLab.record(snapshot, inspection)
        captureLearningLab.recordTreeInspection(snapshot, inspection)

        (inspection.strongInvalidContextCaptureReason() ?: inspection.weakInvalidContextCaptureReason())?.let { reason ->
            return AccessibilityScanResult.InvalidContext(reason)
        }

        if (candidate != null) {
            trace
                ?.withCandidate(candidate)
                ?.mark(OverlayLatencyTrace.Stage.T6_CANDIDATE_PARSED)
                ?.mark(OverlayLatencyTrace.Stage.T7_CANDIDATE_COMPLETE)
            val typeLabel = if (inspection.isRadar) "ACTIONABLE_RADAR_CARD" else "ACTIONABLE_MAIN_CARD"
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "OFFER_DETECTED: type=$typeLabel fingerprint=${candidate.fingerprint}")
            }
            val trustedSingleFrame = inspection.canBypassStabilityGate()
            if (shouldIgnorePostReconnectCandidate(candidate, trustedSingleFrame, snapshot.sourceName)) {
                recordTreeCaptureRejectionIfUseful(inspection)
                return null
            }
            recordSuspectValueChangeIfNeeded(
                candidate = candidate,
                source = OfferCaptureSource.ACCESSIBILITY_TREE,
                offerText = inspection.offerText,
                snapshotName = snapshot.sourceName
            )
            recordCaptureSuccess(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)
        } else {
            recordTreeCaptureRejectionIfUseful(inspection)
        }
        if (candidate != null && BuildConfig.DEBUG) {
            Log.i(TAG, "Accessibility candidate parsed from ${snapshot.sourceName}: ${candidate.fingerprint}")
        }
        return candidate?.let {
            AccessibilityScanResult.Candidate(
                AccessibilityCandidate(
                    candidate = it,
                    trustedSingleFrame = inspection.canBypassStabilityGate()
                )
            )
        }
    }

    private fun TreeOfferInspection.canBypassStabilityGate(): Boolean {
        return trustedForegroundDriverApp == DriverApp.UBER && isCompleteOffer && hasActionButton
    }

    private fun shouldIgnorePostReconnectCandidate(
        candidate: OfferCandidate,
        trustedSingleFrame: Boolean,
        snapshotName: String
    ): Boolean {
        if (!trustedSingleFrame) return false
        val overlayFingerprint = candidate.overlayFingerprint()
        val shouldIgnore = postReconnectCandidateGuard.shouldIgnore(overlayFingerprint)
        if (shouldIgnore && BuildConfig.DEBUG) {
            Log.w(
                TAG,
                "POST_RECONNECT_STALE_CANDIDATE_IGNORED: " +
                    "snapshot=$snapshotName fingerprint=$overlayFingerprint raw=${candidate.fingerprint}"
            )
        }
        return shouldIgnore
    }

    private fun TreeOfferInspection.strongInvalidContextCaptureReason(): OfferCaptureRejectionReason? {
        val reason = rejectionReason?.toCaptureRejectionReason() ?: return null
        return reason.takeIf { it.isImmediateInvalidContext() }
    }

    private fun TreeOfferInspection.weakInvalidContextCaptureReason(): OfferCaptureRejectionReason? {
        val reason = rejectionReason?.toCaptureRejectionReason() ?: return null
        return reason.takeIf { it.isInvalidContext() && !it.isImmediateInvalidContext() }
    }

    private fun recordSuspectValueChangeIfNeeded(
        candidate: OfferCandidate,
        source: OfferCaptureSource,
        offerText: String?,
        snapshotName: String
    ) {
        val previous = lastParsedOfferAudit
        val now = System.currentTimeMillis()
        if (
            previous != null &&
            now - previous.seenAtMillis <= SUSPECT_VALUE_CHANGE_WINDOW_MS &&
            abs(previous.candidate.price - candidate.price) <= SUSPECT_VALUE_EPSILON &&
            abs(previous.candidate.pickupDistanceKm - candidate.pickupDistanceKm) <= SUSPECT_VALUE_EPSILON &&
            abs(previous.candidate.tripDistanceKm - candidate.tripDistanceKm) <= SUSPECT_VALUE_EPSILON &&
            (
                previous.candidate.pickupTimeMin != candidate.pickupTimeMin ||
                    previous.candidate.tripTimeMin != candidate.tripTimeMin
                )
        ) {
            Log.w(
                TAG,
                "SUSPECT_VALUE_CHANGE: " +
                    "oldFingerprint=${previous.candidate.fingerprint} " +
                    "newFingerprint=${candidate.fingerprint} " +
                    "source=$source " +
                    "oldSnapshot=${previous.snapshotName} " +
                    "newSnapshot=$snapshotName " +
                    "oldOfferText=${previous.offerText} " +
                    "newOfferText=$offerText"
            )
        }

        lastParsedOfferAudit = ParsedOfferAudit(
            candidate = candidate,
            offerText = offerText,
            snapshotName = snapshotName,
            seenAtMillis = now
        )
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
        val inspection = DriverOfferTreeExtractor.inspect(this)
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
        sourceKind: AccessibilitySnapshotSourceKind = AccessibilitySnapshotSourceKind.UNKNOWN,
        trace: OverlayLatencyTrace? = null
    ): AccessibilityTreeSnapshot {
        AppDiagnostics.recordStage(this@UberAccessibilityService, AppDiagnostics.Stage.TREE_REFRESH_REQUESTED)
        val refreshStartedAt = SystemClock.elapsedRealtime()
        runCatching { refresh() }
            .onSuccess {
                AppDiagnostics.recordStage(this@UberAccessibilityService, AppDiagnostics.Stage.TREE_REFRESHED)
            }
        trace?.metric(
            name = "tree.refresh",
            durationMs = SystemClock.elapsedRealtime() - refreshStartedAt,
            details = "source=$sourceName"
        )
        val lines = mutableListOf<AccessibleLine>()
        val nodes = mutableListOf<AccessibleNodeSnapshot>()
        val dfsStartedAt = SystemClock.elapsedRealtime()
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
        val dfsMs = SystemClock.elapsedRealtime() - dfsStartedAt
        val dfsSignals = walkResult.visited.toDfsSignalStats()
        trace?.metric(
            name = "tree.dfs",
            durationMs = dfsMs,
            details = "source=$sourceName visited=${walkResult.visited.size} nodesPerMs=${walkResult.visited.size.perMs(dfsMs)} truncated=${walkResult.truncated} duplicateNodeSkips=${walkResult.duplicateNodeSkips} maxDepthReached=${walkResult.maxDepthReached} visitedBeforeFirstFare=${dfsSignals.visitedBeforeFirstFare} visitedBeforeFirstCandidateSignal=${dfsSignals.visitedBeforeFirstCandidateSignal} visitedAfterCandidateSignal=${dfsSignals.visitedAfterCandidateSignal}"
        )
        val snapshotIdsByPath = mutableMapOf<String, Int>()
        val snapshotTimings = SnapshotPropertyTimings()
        val collectStartedAt = SystemClock.elapsedRealtime()
        walkResult.visited.forEach { visit ->
            collectAccessibleNodeSnapshot(
                node = visit.node,
                visit = visit,
                output = lines,
                nodes = nodes,
                parentSnapshotId = visit.path.parentPath()?.let(snapshotIdsByPath::get),
                generationId = generationId,
                sourceKind = sourceKind,
                timings = snapshotTimings
            ).also { snapshotIdsByPath[visit.path] = it }
        }
        val collectMs = SystemClock.elapsedRealtime() - collectStartedAt
        trace?.metric(
            name = "tree.collectNodes",
            durationMs = collectMs,
            details = "source=$sourceName nodeCount=${nodes.size} lineCount=${lines.size} nodesPerMs=${nodes.size.perMs(collectMs)}"
        )
        snapshotTimings.log(trace, sourceName)
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
        sourceKind: AccessibilitySnapshotSourceKind,
        timings: SnapshotPropertyTimings? = null
    ): Int {
        val bounds = Rect()
        val boundsStartedAt = System.nanoTime()
        node.getBoundsInScreen(bounds)
        timings?.let { it.boundsNanos += System.nanoTime() - boundsStartedAt }
        val screenBounds = ScreenBounds(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom
        )
        val nodeSnapshotId = System.identityHashCode(node)
        val textStartedAt = System.nanoTime()
        val textRaw = node.text?.toString()
        timings?.let { it.textNanos += System.nanoTime() - textStartedAt }
        val contentDescriptionStartedAt = System.nanoTime()
        val contentDescriptionRaw = node.contentDescription?.toString()
        timings?.let { it.contentDescriptionNanos += System.nanoTime() - contentDescriptionStartedAt }
        val stateGroupStartedAt = System.nanoTime()
        val stateDescriptionRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) node.stateDescription?.toString() else null
        val paneTitleRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) node.paneTitle?.toString() else null
        val tooltipTextRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) node.tooltipText?.toString() else null
        val hintTextRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString() else null
        timings?.let { it.statePaneTooltipHintNanos += System.nanoTime() - stateGroupStartedAt }
        val extrasStartedAt = System.nanoTime()
        val extrasSummary = node.extrasSummary()
        timings?.let { it.extrasNanos += System.nanoTime() - extrasStartedAt }
        val viewIdStartedAt = System.nanoTime()
        val viewIdResourceName = node.viewIdResourceName
        timings?.let { it.viewIdNanos += System.nanoTime() - viewIdStartedAt }

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
            viewIdResourceName = viewIdResourceName,
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
                parentSnapshotId = parentSnapshotId,
                viewIdResourceName = viewIdResourceName
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
        parentSnapshotId: Int?,
        viewIdResourceName: String?
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

    private fun List<br.com.calcmot.processor.DebugTreeVisit<AccessibilityNodeInfo>>.toDfsSignalStats(): DfsSignalStats {
        var firstFareIndex: Int? = null
        var firstCandidateSignalIndex: Int? = null
        var sawFare = false
        var sawTripBlock = false
        var sawAction = false

        forEachIndexed { index, visit ->
            val text = visit.node.quickSignalText()
            if (text.isBlank()) return@forEachIndexed
            val normalized = TextNormalizer.searchKey(text)
            if (!sawFare && FarePriceExtractor.containsPrimaryFare(text)) {
                sawFare = true
                firstFareIndex = index
            }
            if (!sawTripBlock && normalized.contains("km") && durationSignalRegex.containsMatchIn(normalized)) {
                sawTripBlock = true
            }
            if (!sawAction && (normalized.contains("aceitar") || normalized.contains("selecionar"))) {
                sawAction = true
            }
            if (firstCandidateSignalIndex == null && sawFare && sawTripBlock && sawAction) {
                firstCandidateSignalIndex = index
            }
        }

        val visitedCount = size
        val beforeFare = firstFareIndex?.plus(1) ?: -1
        val beforeCandidate = firstCandidateSignalIndex?.plus(1) ?: -1
        val afterCandidate = firstCandidateSignalIndex?.let { (visitedCount - it - 1).coerceAtLeast(0) } ?: -1
        return DfsSignalStats(
            visitedBeforeFirstFare = beforeFare,
            visitedBeforeFirstCandidateSignal = beforeCandidate,
            visitedAfterCandidateSignal = afterCandidate
        )
    }

    private fun AccessibilityNodeInfo.quickSignalText(): String {
        return listOfNotNull(
            text?.toString(),
            contentDescription?.toString()
        ).joinToString(separator = " ")
    }

    private fun SnapshotPropertyTimings.log(trace: OverlayLatencyTrace?, sourceName: String) {
        trace?.metric("tree.snapshot.bounds", boundsNanos.toMillis(), "source=$sourceName")
        trace?.metric("tree.snapshot.text", textNanos.toMillis(), "source=$sourceName")
        trace?.metric("tree.snapshot.contentDescription", contentDescriptionNanos.toMillis(), "source=$sourceName")
        trace?.metric("tree.snapshot.extras", extrasNanos.toMillis(), "source=$sourceName")
        trace?.metric("tree.snapshot.viewId", viewIdNanos.toMillis(), "source=$sourceName")
        trace?.metric(
            name = "tree.snapshot.statePaneTooltipHint",
            durationMs = statePaneTooltipHintNanos.toMillis(),
            details = "source=$sourceName"
        )
    }

    private fun Long.toMillis(): Long {
        return (this / 1_000_000L).coerceAtLeast(0L)
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

    private var noPriceThrottleCount = 0

    private fun recordTreeCaptureRejectionIfUseful(inspection: TreeOfferInspection) {
        if (inspection.lineCount <= 0) return
        if (!inspection.hasPrice && !inspection.hasActionButton && inspection.timeDistanceBlockCount == 0) return

        val reason = inspection.rejectionReason?.toCaptureRejectionReason()
            ?: OfferCaptureRejectionReason.UNKNOWN

        if (reason == OfferCaptureRejectionReason.NO_PRICE) {
            noPriceThrottleCount++
            if (noPriceThrottleCount % 10 != 1) return
        } else {
            noPriceThrottleCount = 0
        }

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
            TreeRejectionReason.INVALID_CONTEXT_STILL_THERE -> OfferCaptureRejectionReason.INVALID_CONTEXT_STILL_THERE
            TreeRejectionReason.INVALID_CONTEXT_REQUEST_UNAVAILABLE -> OfferCaptureRejectionReason.INVALID_CONTEXT_REQUEST_UNAVAILABLE
            TreeRejectionReason.INVALID_CONTEXT_NO_REQUEST -> OfferCaptureRejectionReason.INVALID_CONTEXT_NO_REQUEST
            TreeRejectionReason.INVALID_CONTEXT_OFFLINE -> OfferCaptureRejectionReason.INVALID_CONTEXT_OFFLINE
        }
    }

    private fun handleCandidate(
        candidate: OfferCandidate,
        source: OfferCaptureSource,
        label: String,
        trustedSingleFrame: Boolean = false,
        trace: OverlayLatencyTrace? = null
    ): Boolean {
        val stabilityStartedAt = SystemClock.elapsedRealtime()
        val decision = captureCoordinator.acceptCandidate(
            source = source,
            candidate = candidate,
            trustedSingleFrame = trustedSingleFrame,
            driverApp = trustedForegroundDriverApp
        )
        trace?.metric(
            name = "stability.evaluate",
            durationMs = SystemClock.elapsedRealtime() - stabilityStartedAt,
            details = "label=$label trustedSingleFrame=$trustedSingleFrame decision=${decision.javaClass.simpleName}"
        )
        Log.w(
            TAG,
            "CALCMOT_STABILITY_GATE label=$label source=$source trustedSingleFrame=$trustedSingleFrame " +
                "decision=${decision.javaClass.simpleName} fingerprint=${candidate.fingerprint}"
        )
        if (decision is CaptureDecision.ShowOverlay) {
            trace?.mark(OverlayLatencyTrace.Stage.T8_STABILITY_ACCEPTED)
        }
        if (trustedSingleFrame && decision is CaptureDecision.ShowOverlay && BuildConfig.DEBUG) {
            Log.w(
                TAG,
                "OVERLAY_BYPASS_ACTIVATED: source=$source fingerprint=${candidate.fingerprint}"
            )
        }
        return applyCaptureDecision(decision, label, trace)
    }

    private suspend fun handleAccessibilityScanResult(
        scanResult: AccessibilityScanResult,
        label: String,
        trace: OverlayLatencyTrace?
    ): Boolean {
        return when (scanResult) {
            is AccessibilityScanResult.Candidate -> {
                handleCandidate(
                    candidate = scanResult.value.candidate,
                    source = OfferCaptureSource.ACCESSIBILITY_TREE,
                    label = label,
                    trustedSingleFrame = scanResult.value.trustedSingleFrame,
                    trace = trace
                )
            }

            is AccessibilityScanResult.InvalidContext -> {
                if (scanResult.reason.isImmediateInvalidContext()) {
                    forceHideOverlayForInvalidContext(
                        reason = scanResult.reason,
                        label = label,
                        trace = trace
                    )
                } else {
                    applyCaptureDecision(
                        decision = captureCoordinator.rejectFrame(
                            source = OfferCaptureSource.ACCESSIBILITY_TREE,
                            reason = scanResult.reason
                        ),
                        label = label,
                        trace = trace
                    )
                }
            }
        }
    }

    private suspend fun forceHideOverlayForInvalidContext(
        reason: OfferCaptureRejectionReason,
        label: String,
        trace: OverlayLatencyTrace?
    ): Boolean {
        val decision = captureCoordinator.rejectFrame(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            reason = reason
        ) as CaptureDecision.HideOverlay

        cancelOverlayExpiry()
        accessibilityPollingUntil = 0L
        activeScanSessionGeneration = 0L
        activeScanSessionStartedAtMillis = 0L
        captureGeneration += 1
        lastParsedOfferAudit = null
        AppDiagnostics.recordStage(this, AppDiagnostics.Stage.FRAME_REJECTED)
        captureLearningLab.recordOverlayHidden(
            status = "overlay_force_hidden_invalid_context",
            source = decision.source,
            fingerprint = decision.overlayFingerprint,
            reason = decision.reason
        )

        if (BuildConfig.DEBUG) {
            Log.w(
                TAG,
                "OVERLAY_FORCE_HIDDEN_INVALID_CONTEXT: " +
                    "reason=$reason label=$label fingerprint=${decision.overlayFingerprint}"
            )
        }

        withContext(Dispatchers.Main.immediate) {
            overlayManager?.hideOverlay()
        }

        trace?.close(OverlayLatencyTrace.EndReason.INVALID_CONTEXT)
        return true
    }

    private fun handleShellOfferFrame(frame: ShellOfferFrame) {
        when (frame) {
            ShellOfferFrame.InvalidFrame -> handleShellInvalidFrame()
            is ShellOfferFrame.Candidate -> handleShellOfferCandidate(frame.candidate)
            is ShellOfferFrame.StableTrip -> handleShellStableTrip(frame.tripData)
            is ShellOfferFrame.VisualProbe -> handleShellVisualProbe(frame.probe)
        }
    }

    private fun handleShellVisualProbe(probe: OverlayLatencyTrace.VisualProbe) {
        latestVisualProbe = probe
        Log.w(
            TAG,
            "CALCMOT_LATENCY_VISUAL_PROBE_RECEIVED elapsed=${probe.elapsedRealtime} " +
                "label=${probe.label ?: "none"} source=${probe.source ?: "none"}"
        )
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
            decision = captureCoordinator.acceptStableTrip(
                source = OfferCaptureSource.UIAUTOMATOR_LAB,
                tripData = tripData,
                driverApp = trustedForegroundDriverApp.takeIf { it != DriverApp.UNKNOWN } ?: DriverApp.UBER
            ),
            label = "uiautomator-shell-bridge",
            trace = null
        )
    }

    private fun showOverlay(tripData: TripData, trace: OverlayLatencyTrace?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (isCurrentForegroundPackageAllowed()) {
                overlayManager?.setForegroundPackage(trustedForegroundPackageName)
            }
            overlayManager?.setLatencyTrace(trace)
            overlayManager?.showOverlay(tripData)
        } else {
            runBlocking(Dispatchers.Main.immediate) {
                if (isCurrentForegroundPackageAllowed()) {
                    overlayManager?.setForegroundPackage(trustedForegroundPackageName)
                }
                overlayManager?.setLatencyTrace(trace)
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

    private fun applyCaptureDecision(
        decision: CaptureDecision,
        label: String,
        trace: OverlayLatencyTrace? = null
    ): Boolean {
        return when (decision) {
            is CaptureDecision.WaitingForStability -> {
                AppDiagnostics.recordStage(this, AppDiagnostics.Stage.FIRST_FRAME)
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Offer candidate accepted as first frame [$label]: ${decision.candidate.fingerprint}")
                }
                trace?.close(OverlayLatencyTrace.EndReason.WAITING_FOR_STABILITY)
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
                trace?.close(OverlayLatencyTrace.EndReason.CARD_GONE)
                false
            }

            is CaptureDecision.ShowOverlay -> {
                AppDiagnostics.recordStage(this, AppDiagnostics.Stage.STABLE_OFFER)
                captureLearningLab.recordOverlay(decision.source, decision.tripData)
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "OVERLAY_NEW_FINGERPRINT_SHOWN: fingerprint=${decision.overlayFingerprint}")
                    Log.w(TAG, "Stable offer confirmed [$label]: ${decision.tripData}")
                }
                trace
                    ?.withTripData(decision.tripData)
                    ?.mark(OverlayLatencyTrace.Stage.T9_OVERLAY_REQUESTED)
                Log.w(TAG, "CALCMOT_OVERLAY_REQUEST fingerprint=${decision.overlayFingerprint}")
                showOverlay(decision.tripData, trace)
                scheduleOverlayExpiry(decision.overlayFingerprint)
                true
            }

            is CaptureDecision.RenewCurrentOverlay -> {
                if (BuildConfig.DEBUG) {
                    Log.w(
                        TAG,
                        "OVERLAY_TTL_RENEWED: fingerprint=${decision.overlayFingerprint}"
                    )
                    Log.i(
                        TAG,
                        "OVERLAY_ALREADY_VISIBLE_RENEWED: source=${decision.source} " +
                            "fingerprint=${decision.overlayFingerprint}"
                    )
                    Log.d(TAG, "OVERLAY_DUPLICATE_SKIPPED: fingerprint=${decision.overlayFingerprint}")
                }
                scheduleOverlayExpiry(decision.overlayFingerprint)
                trace?.close(OverlayLatencyTrace.EndReason.RENEWED_VISIBLE_OVERLAY)
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
                trace?.close(OverlayLatencyTrace.EndReason.CARD_GONE)
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
                    Log.w(
                        TAG,
                        "OVERLAY_KEEPALIVE_SAME_ACTIONABLE_CARD: reason=${decision.reason} streak=${decision.invalidFrameStreak}"
                    )
                    Log.d(
                        TAG,
                        "Frame rejected but waiting for confirmation " +
                        "(${decision.invalidFrameStreak}/${decision.requiredInvalidFramesToReset}) [$label]"
                    )
                }
                trace?.close(OverlayLatencyTrace.EndReason.INVALID_CONTEXT)
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
                if (BuildConfig.DEBUG) {
                    if (decision.reason.isImmediateInvalidContext()) {
                        Log.w(TAG, "OVERLAY_FORCE_HIDDEN_INVALID_CONTEXT: reason=${decision.reason} fingerprint=${decision.overlayFingerprint}")
                    } else if (label == "overlay-ttl") {
                        Log.w(TAG, "OVERLAY_EXPIRED_NO_ACTIONABLE_CARD: fingerprint=${decision.overlayFingerprint}")
                    }
                    Log.d(TAG, "Frame rejected [$label]: ${decision.reason}")
                }
                cancelOverlayExpiry()
                mainScope.launch {
                    if (label == "overlay-ttl" || label == "overlay-heartbeat-ttl") {
                        overlayManager?.expireOverlay(decision.overlayFingerprint)
                    } else {
                        overlayManager?.hideOverlay()
                    }
                }
                trace?.close(
                    if (label == "overlay-ttl" || label == "overlay-heartbeat-ttl") {
                        OverlayLatencyTrace.EndReason.CARD_GONE
                    } else {
                        OverlayLatencyTrace.EndReason.INVALID_CONTEXT
                    }
                )
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (BuildConfig.DEBUG) {
            ShellOfferBridge.unregister(shellOfferHandler)
        }
        continuousAccessibilityPollingJob?.cancel()
        accessibilityPollingJob?.cancel()
        capturePipelineJob?.cancel()
        overlayExpiryJob?.cancel()
        accessibilityTreeLab.close()
        captureLearningLab.close()
        ninetyNineDiagnostics.close()
        closeNinetyNineCaptureEngine()
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

    private fun AccessibilityEvent.isRelevantDriverEvent(driverApp: DriverApp): Boolean {
        return DriverAccessibilityEventPolicy.isRelevant(driverApp, eventType)
    }

    private fun AccessibilityEvent.isForegroundDefiningEvent(): Boolean {
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    private fun Int.debugEventName(): String {
        return when (this) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TYPE_VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "TYPE_VIEW_SCROLLED"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "TYPE_WINDOWS_CHANGED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "TYPE_VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> "TYPE_VIEW_ACCESSIBILITY_FOCUSED"
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> "TYPE_ANNOUNCEMENT"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "TYPE_NOTIFICATION_STATE_CHANGED"
            else -> "event_$this"
        }
    }

    private fun Int.perMs(durationMs: Long): String {
        if (durationMs <= 0L) return this.toString()
        return "%.2f".format(java.util.Locale.US, this.toDouble() / durationMs.toDouble())
    }

    private companion object {
        const val TAG = "UberReader"
        var runtimeAccessibilityInfoConfiguredOnce = false
        const val UBER_DRIVER_PACKAGE = "com.ubercab.driver"
        const val MAX_ACCESSIBILITY_LOG_REASONS = 40
        const val EVENT_SOURCE_PARENT_ATTEMPTS = 8
        const val ACCESSIBILITY_HEARTBEAT_INTERVAL_MS = 1_000L
        const val ACCESSIBILITY_WATCHDOG_SCAN_INTERVAL_MS = 2_000L
        const val NINETY_NINE_HEARTBEAT_SCAN_INTERVAL_MS = 750L
        const val ACCESSIBILITY_POLL_WINDOW_MS = 2_500L
        const val ACCESSIBILITY_POLL_INTERVAL_MS = 250L
        const val REQUIRED_INVALID_FRAMES_TO_RESET = 2
        const val OVERLAY_TTL_MS = 5_000L
        const val ACCESSIBILITY_SCAN_SESSION_COALESCE_MS = 250L
        const val MAX_EXTRAS_TEXT_VALUES = 24
        const val MAX_EXTRAS_VALUE_LENGTH = 160
        const val MAX_ACCESSIBILITY_TREE_DEPTH = 80
        const val MAX_ACCESSIBILITY_TREE_NODES = 2_000
        const val PACKAGE_SCAN_NODE_LIMIT = 5_000
        const val NINETY_NINE_TRIGGER_SCAN_NODE_LIMIT = 500
        const val SUSPECT_VALUE_CHANGE_WINDOW_MS = 15_000L
        const val SUSPECT_VALUE_EPSILON = 0.05
        const val VISUAL_PROBE_MAX_AGE_MS = 5_000L
        val durationSignalRegex = Regex("""\b[0-9]{1,3}\s*(h|hora|min|minuto)\b""")
    }

    private data class CaptureSession(
        val generation: Long,
        val shouldStartBurst: Boolean,
        val trace: OverlayLatencyTrace?
    )

    private data class AccessibilityCandidate(
        val candidate: OfferCandidate,
        val trustedSingleFrame: Boolean
    )

    private data class DfsSignalStats(
        val visitedBeforeFirstFare: Int,
        val visitedBeforeFirstCandidateSignal: Int,
        val visitedAfterCandidateSignal: Int
    )

    private data class SnapshotPropertyTimings(
        var boundsNanos: Long = 0L,
        var textNanos: Long = 0L,
        var contentDescriptionNanos: Long = 0L,
        var extrasNanos: Long = 0L,
        var viewIdNanos: Long = 0L,
        var statePaneTooltipHintNanos: Long = 0L
    )

    private sealed interface AccessibilityScanResult {
        data class Candidate(val value: AccessibilityCandidate) : AccessibilityScanResult
        data class InvalidContext(val reason: OfferCaptureRejectionReason) : AccessibilityScanResult
    }

    private data class ParsedOfferAudit(
        val candidate: OfferCandidate,
        val offerText: String?,
        val snapshotName: String,
        val seenAtMillis: Long
    )

    private data class AccessibilityRootSource(
        val name: String,
        val root: AccessibilityNodeInfo,
        val windowCount: Int,
        val sourceKind: AccessibilitySnapshotSourceKind,
        val isDriverRoot: Boolean,
        val isActiveOrFocused: Boolean
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
