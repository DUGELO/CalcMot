package br.com.calcmot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseReadinessTest {

    @Test
    fun `manifest does not request unused overlay or foreground permissions`() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertFalse(manifest.contains("android.permission.SYSTEM_ALERT_WINDOW"))
        assertFalse(manifest.contains("android.permission.FOREGROUND_SERVICE"))
    }

    @Test
    fun `release manifest does not export debug receiver`() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertFalse(manifest.contains("DebugReceiver"))
        assertFalse(manifest.contains("ACTION_PROCESS_TEXT"))
    }

    @Test
    fun `visual capture runtime is fully removed from production source`() {
        val mainSources = projectFile("app/src/main").walkTopDown()
            .filter { it.isFile }
            .joinToString(separator = "\n") { it.readText(Charsets.UTF_8) }
        val buildFile = projectFile("app/build.gradle.kts").readText()
        val accessibilityConfig = projectFile("app/src/main/res/xml/accessibility_service_config.xml").readText()
        val overlay = projectFile("app/src/main/java/br/com/calcmot/overlay/OverlayManager.kt")
            .readText()
        val overlayStateMachine = projectFile("app/src/main/java/br/com/calcmot/overlay/OverlayStateMachine.kt")
            .readText()
        val forbiddenRuntimeTokens = listOf(
            "TextRecognizer",
            "TextRecognition",
            "InputImage",
            "OfferFrameValidator",
            "OcrFrame",
            "OcrLine",
            "OCR_CROP",
            "SCREENSHOT_FAILED",
            "OCRDump",
            "OfferCropCalculator",
            "CropRect",
            "ocr-full",
            "full-fallback",
            "MediaProjection",
            "takeScreenshot",
            "canTakeScreenshot"
        )
        val forbiddenDependencyTokens = listOf(
            "mlkit",
            "text-recognition",
            "kotlinx-coroutines-play-services"
        )

        forbiddenRuntimeTokens.forEach { token ->
            assertFalse("$token must not exist in main source", mainSources.contains(token))
        }
        forbiddenDependencyTokens.forEach { token ->
            assertFalse("$token must not exist in Gradle dependencies", buildFile.contains(token))
        }
        assertFalse(accessibilityConfig.contains("canTakeScreenshot"))
        assertTrue(overlay.contains("BuildConfig.DEBUG"))
        assertTrue(overlayStateMachine.contains("OverlayUiState"))
    }

    @Test
    fun `accessibility tool declaration is enabled for no ocr tree capture`() {
        val mainConfig = projectFile("app/src/main/res/xml/accessibility_service_config.xml").readText()
        val debugConfig = projectFile("app/src/debug/res/xml/accessibility_service_config.xml").readText()
        val service = projectFile("app/src/main/java/br/com/calcmot/accessibility/UberAccessibilityService.kt")
            .readText()
        val debugToggles = projectFile("app/src/main/java/br/com/calcmot/accessibility/AccessibilityDebugConfig.kt")
            .readText()

        assertTrue(mainConfig.contains("android:isAccessibilityTool=\"true\""))
        assertTrue(debugConfig.contains("android:isAccessibilityTool=\"true\""))
        assertTrue(service.contains("configureRuntimeAccessibilityInfo"))
        assertTrue(service.contains("FLAG_RETRIEVE_INTERACTIVE_WINDOWS"))
        assertTrue(service.contains("FLAG_INCLUDE_NOT_IMPORTANT_VIEWS"))
        assertTrue(service.contains("removeOverlayWindowsForScan"))
        assertTrue(service.contains("overlayManager?.isVisible == true"))
        assertTrue(service.contains("ACCESSIBILITY_SCAN_SESSION_COALESCE_MS"))
        assertTrue(service.contains("120L, 240L, 420L, 700L, 1_000L, 1_500L, 2_200L"))
        assertTrue(service.contains("ACCESSIBILITY_HEARTBEAT_INTERVAL_MS = 1_000L"))
        assertTrue(service.contains("private fun TreeOfferInspection.canBypassStabilityGate(): Boolean {\n        return isCompleteOffer && hasActionButton\n    }"))
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
        assertTrue(debugLab.contains("writeText"))
        assertFalse(releaseLab.contains("accessibility-lab/session-"))
        assertFalse(releaseLab.contains("writeText"))
        assertFalse(releaseLab.contains("\"lines\""))
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
