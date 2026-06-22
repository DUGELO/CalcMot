package br.com.calcmot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseReadinessTest {

    @Test
    fun `manifest requests only the foreground permission required by legacy 99 projection`() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertFalse(manifest.contains("android.permission.SYSTEM_ALERT_WINDOW"))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"))
        assertTrue(manifest.contains("android:foregroundServiceType=\"mediaProjection\""))
    }

    @Test
    fun `release manifest does not export debug receiver`() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertFalse(manifest.contains("DebugReceiver"))
        assertFalse(manifest.contains("ACTION_PROCESS_TEXT"))
    }

    @Test
    fun `manifest declares package visibility for every supported driver app`() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        DriverApp.supported.flatMap { it.packageNames }.forEach { packageName ->
            assertTrue("Manifest must query $packageName", manifest.contains("<package android:name=\"$packageName\""))
        }
    }

    @Test
    fun `visual capture runtime is isolated to the 99 engine and never enters uber parsing`() {
        val ninetyNineSources = projectFile("app/src/main/java/br/com/calcmot/ninetynine").walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText(Charsets.UTF_8) }
        val uberParserSources = listOf(
            "app/src/main/java/br/com/calcmot/processor/OfferParser.kt",
            "app/src/main/java/br/com/calcmot/processor/OfferTreeExtractor.kt"
        ).joinToString(separator = "\n") { projectFile(it).readText(Charsets.UTF_8) }
        val buildFile = projectFile("app/build.gradle.kts").readText()
        val accessibilityConfig = projectFile("app/src/main/res/xml/accessibility_service_config.xml").readText()
        val accessibilityConfigV30 =
            projectFile("app/src/main/res/xml-v30/accessibility_service_config.xml").readText()
        val service = projectFile(
            "app/src/main/java/br/com/calcmot/accessibility/UberAccessibilityService.kt"
        ).readText()
        val overlay = projectFile("app/src/main/java/br/com/calcmot/overlay/OverlayManager.kt")
            .readText()
        val overlayStateMachine = projectFile("app/src/main/java/br/com/calcmot/overlay/OverlayStateMachine.kt")
            .readText()
        val requiredRuntimeTokens = listOf(
            "TextRecognizer",
            "TextRecognition",
            "InputImage",
            "MediaProjection",
            "takeScreenshot"
        )

        requiredRuntimeTokens.forEach { token ->
            assertTrue("$token must exist in the isolated 99 source", ninetyNineSources.contains(token))
            assertFalse("$token must never enter Uber parsers", uberParserSources.contains(token))
        }
        assertTrue(buildFile.contains("com.google.mlkit:text-recognition:16.0.1"))
        assertFalse(accessibilityConfig.contains("canTakeScreenshot"))
        assertTrue(accessibilityConfigV30.contains("android:canTakeScreenshot=\"true\""))
        assertTrue(service.contains("trustedForegroundDriverApp == DriverApp.NINETY_NINE"))
        assertTrue(service.contains("handleNinetyNineVisualCapture"))
        assertTrue(service.contains("OfferCaptureSource.NINETY_NINE_OCR"))
        assertTrue(service.contains("PowerManager"))
        assertTrue(service.contains("hasVisibleNinetyNineRoot"))
        assertTrue(ninetyNineSources.contains("AtomicBoolean"))
        assertTrue(ninetyNineSources.contains("ACTIVE_UNCHANGED_OCR_INTERVAL_MS = 3_000L"))
        assertTrue(ninetyNineSources.contains("IDLE_UNCHANGED_OCR_INTERVAL_MS = 6_000L"))
        assertTrue(ninetyNineSources.contains("NinetyNineProjectionService"))
        assertTrue(overlay.contains("BuildConfig.DEBUG"))
        assertTrue(overlayStateMachine.contains("OverlayUiState"))
    }

    @Test
    fun `accessibility tool declaration is enabled for no ocr tree capture`() {
        val mainConfig = projectFile("app/src/main/res/xml/accessibility_service_config.xml").readText()
        val debugConfig = projectFile("app/src/debug/res/xml/accessibility_service_config.xml").readText()
        val service = projectFile("app/src/main/java/br/com/calcmot/accessibility/UberAccessibilityService.kt")
            .readText()
        val offerTreeExtractor = projectFile("app/src/main/java/br/com/calcmot/processor/OfferTreeExtractor.kt")
            .readText()
        val debugToggles = projectFile("app/src/main/java/br/com/calcmot/accessibility/AccessibilityDebugConfig.kt")
            .readText()
        val eventPolicy = projectFile(
            "app/src/main/java/br/com/calcmot/accessibility/DriverAccessibilityEventPolicy.kt"
        ).readText()

        assertTrue(mainConfig.contains("android:isAccessibilityTool=\"true\""))
        assertTrue(debugConfig.contains("android:isAccessibilityTool=\"true\""))
        assertTrue(service.contains("configureRuntimeAccessibilityInfo"))
        assertTrue(service.contains("FLAG_RETRIEVE_INTERACTIVE_WINDOWS"))
        assertTrue(service.contains("FLAG_INCLUDE_NOT_IMPORTANT_VIEWS"))
        assertTrue(service.contains("removeOverlayWindowsForScan"))
        assertTrue(service.contains("overlayManager?.isVisible == true"))
        assertTrue(service.contains("ACCESSIBILITY_SCAN_SESSION_COALESCE_MS"))
        assertTrue(eventPolicy.contains("0L, 80L, 160L, 300L, 500L, 750L, 1_000L"))
        assertTrue(service.contains("ACCESSIBILITY_HEARTBEAT_INTERVAL_MS = 1_000L"))
        assertTrue(service.contains("ACCESSIBILITY_SCAN_SESSION_COALESCE_MS = 250L"))
        assertTrue(service.contains("pendingCaptureAfterCurrent"))
        assertTrue(service.contains("capture.pendingAfterCurrent"))
        assertTrue(service.contains("enterSafeIdleForBlockedUserApp"))
        assertTrue(service.contains("PackageDecision.BLOCKED_USER_APP"))
        assertTrue(service.contains("CALCMOT_ACCESSIBILITY_EVENT"))
        assertTrue(service.contains("CALCMOT_PACKAGE_GUARD"))
        assertTrue(service.contains("CALCMOT_OWN_PACKAGE_EVENT_IGNORED"))
        assertTrue(service.contains("CALCMOT_TRANSIENT_SYSTEM_EVENT_IGNORED"))
        assertTrue(service.contains("CALCMOT_UNKNOWN_PACKAGE_EVENT_IGNORED"))
        assertTrue(service.contains("CALCMOT_ROOT_READ"))
        assertTrue(service.contains("OverlayLatencyTrace"))
        assertTrue(service.contains("latencyTracesByGeneration"))
        assertTrue(service.contains("consumeRecentVisualProbe"))
        assertTrue(service.contains("closeAllLatencyTraces"))
        assertTrue(service.contains("rawForegroundPackageName"))
        assertTrue(service.contains("trustedForegroundPackageName"))
        assertTrue(service.contains("CALCMOT_BLOCKED_USER_EVENT_IGNORED_NON_FOREGROUND"))
        assertTrue(service.contains("SAFE_MODE_BLOCKED_USER_APP"))
        assertTrue(service.contains("RENEWED_VISIBLE_OVERLAY"))
        assertTrue(service.contains("CALCMOT_NO_CANDIDATE_AFTER_TREE_DETAILS"))
        assertTrue(offerTreeExtractor.contains("\"semanticLines\""))
        assertTrue(offerTreeExtractor.contains("\"fieldCandidates\""))
        assertTrue(offerTreeExtractor.contains("\"knownNodeMappings\""))
        assertTrue(service.contains("visitedBeforeFirstCandidateSignal"))
        assertFalse(service.contains("activeLatencyTrace"))
        assertTrue(service.contains("activeDriverRoots.isNotEmpty()"))
        assertTrue(
            service.contains(
                "activeDriverRoots.isNotEmpty() && trustedForegroundDriverApp == DriverApp.UBER"
            )
        )
        assertTrue(
            service.contains(
                "return (activeDriverRoots + driverWindowRoots).distinctAccessibilityRoots()"
            )
        )
        assertTrue(service.contains("trustedForegroundDriverApp == DriverApp.UBER && isCompleteOffer && hasActionButton"))
        assertTrue(service.contains("switchDriverAppIfNeeded"))
        assertTrue(service.contains("DriverOfferParser.parse"))
        assertTrue(service.contains("DriverOfferTreeExtractor.inspect"))
        assertTrue(service.contains("configureRuntimeProfileForDriverApp"))
        assertTrue(service.contains("DriverAccessibilityEventPolicy.eventTypesFor(driverApp)"))
        assertTrue(service.contains("ninetyNineSemanticBridgeProbe.requestSemantics"))
        assertTrue(service.contains("trustedForegroundDriverApp == DriverApp.NINETY_NINE"))
        assertTrue(service.contains("bootstrapNinetyNineForegroundFromActiveRoot"))
        assertTrue(service.contains("runFocusedNinetyNineHeartbeatScanIfNeeded"))
        assertTrue(service.contains("label = \"99-heartbeat\""))
        assertTrue(
            service.contains(
                "if (trustedForegroundDriverApp == DriverApp.NINETY_NINE) {\n" +
                    "                    runFocusedNinetyNineHeartbeatScanIfNeeded()\n" +
                    "                    continue\n" +
                    "                }\n" +
                    "                runFocusedUberWatchdogScanIfNeeded()"
            )
        )
        assertFalse(service.contains("accessibility-continuous-poll"))
        assertTrue(service.contains("if (BuildConfig.DEBUG) {\n            ShellOfferBridge.register(shellOfferHandler)"))
        assertTrue(service.contains("if (BuildConfig.DEBUG) {\n            ShellOfferBridge.unregister(shellOfferHandler)"))
        assertTrue(debugToggles.contains("ENABLE_ZERO_OVERLAY_DURING_SCAN"))
        assertTrue(debugToggles.contains("const val ENABLE_ZERO_OVERLAY_DURING_SCAN: Boolean = true"))
    }

    @Test
    fun `overlay token recovery does not dump bad token stack to logcat`() {
        val overlay = projectFile("app/src/main/java/br/com/calcmot/overlay/OverlayManager.kt")
            .readText()

        assertTrue(overlay.contains("OVERLAY_TOKEN_RECOVERING fingerprint="))
        assertTrue(overlay.contains("OVERLAY_REPLACED_IN_PLACE"))
        assertTrue(overlay.contains("OVERLAY_REQUEST_DROPPED_NO_TRUSTED_DRIVER"))
        assertTrue(overlay.contains("OVERLAY_BLOCKED_USER_APP"))
        assertFalse(overlay.contains("OVERLAY_BLOCKED_NON_ALLOWED_APP"))
        assertTrue(overlay.contains("OVERLAY_FOREGROUND_TRANSIENT_IGNORED"))
        assertTrue(overlay.contains("OVERLAY_FOREGROUND_OWN_IGNORED"))
        assertTrue(overlay.contains("TRUSTED_DRIVER_GRACE_MS = 2_000L"))
        assertTrue(overlay.contains("allowApplicationOverlayFallback = true"))
        assertTrue(overlay.contains("context is AccessibilityService"))
        assertFalse(overlay.contains("Erro showOverlay: token invalido\", e"))
        assertFalse(overlay.contains("Erro showDebugOverlay: token invalido\", e"))
    }

    @Test
    fun `accessibility tree lab stores raw snapshots only in debug source set`() {
        val debugLab = projectFile("app/src/debug/java/br/com/calcmot/accessibility/AccessibilityTreeLab.kt")
            .readText()
        val releaseLab = projectFile("app/src/release/java/br/com/calcmot/accessibility/AccessibilityTreeLab.kt")
            .readText()

        assertTrue(debugLab.contains("accessibility-lab/session-"))
        assertTrue(debugLab.contains("snapshot.driverApp == DriverApp.NINETY_NINE"))
        assertTrue(debugLab.contains("writeText"))
        assertFalse(releaseLab.contains("accessibility-lab/session-"))
        assertFalse(releaseLab.contains("writeText"))
        assertFalse(releaseLab.contains("\"lines\""))
    }

    @Test
    fun `99 verbose diagnostics are debug only and release implementation is empty`() {
        val debugDiagnostics = projectFile(
            "app/src/debug/java/br/com/calcmot/accessibility/NinetyNineAccessibilityDiagnostics.kt"
        ).readText()
        val releaseDiagnostics = projectFile(
            "app/src/release/java/br/com/calcmot/accessibility/NinetyNineAccessibilityDiagnostics.kt"
        ).readText()

        assertTrue(debugDiagnostics.contains("99-accessibility/session-"))
        assertTrue(debugDiagnostics.contains("CALCMOT_99_"))
        assertTrue(debugDiagnostics.contains("SEMANTIC_BRIDGE"))
        assertTrue(debugDiagnostics.contains("timeline.ndjson"))
        assertFalse(releaseDiagnostics.contains("99-accessibility/session-"))
        assertFalse(releaseDiagnostics.contains("CALCMOT_99_"))
        assertFalse(releaseDiagnostics.contains("timeline.ndjson"))
    }

    @Test
    fun `99 touch exploration probe exists only in debug source set`() {
        val debugConfig = projectFile("app/src/debug/res/xml/accessibility_service_config.xml").readText()
        val mainConfig = projectFile("app/src/main/res/xml/accessibility_service_config.xml").readText()
        val debugProbe = projectFile(
            "app/src/debug/java/br/com/calcmot/accessibility/NinetyNineSemanticBridgeProbe.kt"
        ).readText()
        val releaseProbe = projectFile(
            "app/src/release/java/br/com/calcmot/accessibility/NinetyNineSemanticBridgeProbe.kt"
        ).readText()

        assertTrue(debugConfig.contains("android:canRequestTouchExplorationMode=\"true\""))
        assertFalse(mainConfig.contains("canRequestTouchExplorationMode"))
        assertTrue(debugProbe.contains("FLAG_REQUEST_TOUCH_EXPLORATION_MODE.inv()"))
        assertTrue(debugProbe.contains("ACTION_ACCESSIBILITY_FOCUS"))
        assertTrue(debugProbe.contains("main_flutter_flutter_root"))
        assertTrue(debugProbe.contains("flutter_deal_gesture_container"))
        assertFalse(releaseProbe.contains("FLAG_REQUEST_TOUCH_EXPLORATION_MODE"))
        assertFalse(releaseProbe.contains("ACTION_ACCESSIBILITY_FOCUS"))
    }

    @Test
    fun `direct uiautomator probe is debug only`() {
        val debugManifest = projectFile("app/src/debug/AndroidManifest.xml").readText()
        val debugProbe = projectFile("app/src/debug/java/br/com/calcmot/debug/UiAutomatorProbeReceiver.kt")
            .readText()
        val debugBridge = projectFile("app/src/debug/java/br/com/calcmot/debug/UiAutomatorOfferReceiver.kt")
            .readText()
        val mainSources = projectFile("app/src/main").walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText(Charsets.UTF_8) }
        val releaseSources = projectFile("app/src/release").walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText(Charsets.UTF_8) }

        assertTrue(debugManifest.contains("br.com.calcmot.DEBUG_UIAUTOMATOR_PROBE"))
        assertTrue(debugManifest.contains("br.com.calcmot.DEBUG_UIAUTOMATOR_OFFER"))
        assertTrue(debugManifest.contains("br.com.calcmot.DEBUG_LATENCY_VISUAL_PROBE"))
        assertTrue(debugProbe.contains("/system/bin/uiautomator"))
        assertTrue(debugBridge.contains("DEBUG_UIAUTOMATOR_OFFER"))
        assertFalse(mainSources.contains("/system/bin/uiautomator"))
        assertFalse(mainSources.contains("DEBUG_UIAUTOMATOR_PROBE"))
        assertFalse(mainSources.contains("DEBUG_UIAUTOMATOR_OFFER"))
        assertFalse(mainSources.contains("shizuku"))
        assertFalse(mainSources.contains("moe.shizuku"))
        assertFalse(releaseSources.contains("/system/bin/uiautomator"))
        assertFalse(releaseSources.contains("DEBUG_UIAUTOMATOR_PROBE"))
        assertFalse(releaseSources.contains("DEBUG_UIAUTOMATOR_OFFER"))
        assertFalse(releaseSources.contains("shizuku"))
        assertFalse(releaseSources.contains("moe.shizuku"))
    }

    @Test
    fun `uiautomator oracle scripts generate benchmark report fields`() {
        val bridgeScript = projectFile("scripts/run-uiautomator-bridge.ps1").readText()
        val captureScript = projectFile("scripts/capture-accessibility-lab.ps1").readText()
        val labDoc = projectFile("docs/uiautomator-oracle-lab.md").readText()

        val sharedFields = listOf(
            "uiautomator_complete_cards",
            "internal_tree_complete_cards",
            "overlay_shown",
            "missed_cards",
            "average_latency_ms",
            "beta_overlay_coverage_percent",
            "beta_ready_min_85"
        )
        val resourceFields = listOf(
            "resource_sample_count",
            "resource_cpu_avg_percent",
            "resource_cpu_max_percent",
            "resource_pss_avg_kb",
            "resource_pss_max_kb",
            "resource_rss_avg_kb",
            "resource_rss_max_kb"
        )
        val oracleOnlyFields = listOf(
            "accessibility_service_enabled",
            "ocr_enabled",
            "production_overlay_shown",
            "correct_overlay_cards",
            "false_positive_count",
            "wrong_value_count",
            "coverage_correct_percent"
        )

        sharedFields.forEach { field ->
            assertTrue("bridge report should include $field", bridgeScript.contains(field))
            assertTrue("capture report should include $field", captureScript.contains(field))
            assertTrue("lab doc should explain $field", labDoc.contains(field))
        }
        resourceFields.forEach { field ->
            assertTrue("bridge report should include $field", bridgeScript.contains(field))
        }
        assertTrue(bridgeScript.contains("Get-CalcMotResourceSample"))
        assertTrue(bridgeScript.contains("ResourceSampleIntervalSeconds"))
        oracleOnlyFields.forEach { field ->
            assertTrue("bridge report should include $field", bridgeScript.contains(field))
            assertTrue("lab doc should explain $field", labDoc.contains(field))
        }
        assertTrue(bridgeScript.contains("exec-out uiautomator dump --compressed /dev/tty"))
        assertTrue(bridgeScript.contains("UiAutomatorDumpTimeoutSeconds"))
        assertTrue(bridgeScript.contains("Wait-Job"))
        assertTrue(bridgeScript.contains("OracleOnly"))
        assertTrue(bridgeScript.contains("-not \$OracleOnly"))
        assertTrue(bridgeScript.contains("production-events.json"))
        assertTrue(bridgeScript.contains("learning-backlog.json"))
        assertTrue(bridgeScript.contains("oracle-fixtures.json"))
        assertTrue(bridgeScript.contains("CaptureScreenshots"))
        assertTrue(bridgeScript.contains("capture_complete_accessibility_tree"))
        assertFalse(bridgeScript.contains("capture_complete_ocr_crop"))
        assertFalse(bridgeScript.contains("ocr_complete_cards"))
        assertTrue(labDoc.contains("-OracleOnly"))
        assertTrue(labDoc.contains("learning-backlog.json"))
        assertTrue(labDoc.contains("false_positive_count == 0"))

        val latencyAnalyzer = projectFile("scripts/analyze-calcmot-latency.ps1").readText()
        assertTrue(latencyAnalyzer.contains("CALCMOT_LATENCY_TRACE_SUMMARY"))
        assertTrue(latencyAnalyzer.contains("CALCMOT_LATENCY_METRIC"))
        assertTrue(latencyAnalyzer.contains("CALCMOT_LATENCY_TRACE_END"))
        assertTrue(latencyAnalyzer.contains("Top 10 Incomplete Causes"))
        assertTrue(latencyAnalyzer.contains("visible traces"))
        assertTrue(latencyAnalyzer.contains("renewed visible overlay"))
        assertTrue(latencyAnalyzer.contains("true closed non-visible traces"))
        assertTrue(latencyAnalyzer.contains("true incomplete/unclosed traces"))
        assertTrue(latencyAnalyzer.contains("Renewed/Dedup"))
        assertTrue(latencyAnalyzer.contains("RENEWED_VISIBLE_OVERLAY"))
        assertTrue(latencyAnalyzer.contains("CANDIDATE_RENEWED_VISIBLE_OVERLAY"))
        assertTrue(latencyAnalyzer.contains("candidate_found_rate"))
        assertTrue(latencyAnalyzer.contains("no_candidate_rate"))
        assertTrue(latencyAnalyzer.contains("safe_mode_during_driver_trace_count"))
        assertTrue(latencyAnalyzer.contains("overlay_removed_by_transient_count"))
        assertTrue(latencyAnalyzer.contains("audit_valid_for_product_latency"))
        assertTrue(latencyAnalyzer.contains("Top 10 Metric Sources"))
    }

    @Test
    fun `release source does not contain uiautomator runtime dependencies`() {
        val mainSources = projectFile("app/src/main").walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText(Charsets.UTF_8) }
        val releaseSources = projectFile("app/src/release").walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText(Charsets.UTF_8) }

        assertFalse(mainSources.contains("/dev/tty"))
        assertFalse(mainSources.contains("uiautomator dump"))
        assertFalse(releaseSources.contains("/dev/tty"))
        assertFalse(releaseSources.contains("uiautomator dump"))
    }

    @Test
    fun `app identity is ready for Play Store`() {
        val buildFile = projectFile("app/build.gradle.kts").readText()

        assertTrue(buildFile.contains("namespace = \"br.com.calcmot\""))
        assertTrue(buildFile.contains("applicationId = \"br.com.calcmot\""))
        assertFalse(buildFile.contains(legacyPackageName))
    }

    @Test
    fun `public UI strings do not contain encoding artifacts`() {
        val files = listOf(
            "app/src/main/java/br/com/calcmot/AppSettings.kt",
            "app/src/main/java/br/com/calcmot/ui/HomeScreen.kt",
            "app/src/main/java/br/com/calcmot/ui/FinanceScreen.kt",
            "app/src/main/java/br/com/calcmot/ui/OnboardingScreen.kt",
            "app/src/main/java/br/com/calcmot/ui/PrivacyPolicyScreen.kt",
            "app/src/main/java/br/com/calcmot/overlay/OverlayView.kt",
            "app/src/main/java/br/com/calcmot/ui/theme/Theme.kt",
            "app/src/main/java/br/com/calcmot/ui/theme/Color.kt",
            "app/src/main/res/values/strings.xml"
        )

        files.forEach { path ->
            val text = projectFile(path).readText(Charsets.UTF_8)
            assertFalse(path, mojibakeRegex.containsMatchIn(text))
        }
    }

    private val mojibakeRegex = Regex("[\\u00C2\\u00C3][\\u00A0-\\u00BF]")
    private val legacyPackageName = listOf("com", "example", "calcmot").joinToString(".")

    private fun projectFile(path: String): File {
        val fromRoot = File(path)
        if (fromRoot.exists()) return fromRoot

        val fromModule = File("../$path")
        if (fromModule.exists()) return fromModule

        error("File not found: $path")
    }
}
