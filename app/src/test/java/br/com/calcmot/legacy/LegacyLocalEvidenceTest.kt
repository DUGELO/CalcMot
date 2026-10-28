package br.com.calcmot.legacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LegacyLocalEvidenceTest {
    @Test fun `local update and restored onboarding are auxiliary and never a grant`() {
        assertTrue(LegacyLocalEvidence(firstInstallTimeMs = 100, lastUpdateTimeMs = 200,
            currentVersionCode = 11, storedOnboardingCompleted = null).hasAuxiliaryIndication)
        assertTrue(LegacyLocalEvidence(firstInstallTimeMs = null, lastUpdateTimeMs = null,
            currentVersionCode = null, storedOnboardingCompleted = true).hasAuxiliaryIndication)
        // A new install can have the same current version as an existing user.
        assertFalse(LegacyLocalEvidence(firstInstallTimeMs = 200, lastUpdateTimeMs = 200,
            currentVersionCode = 11, storedOnboardingCompleted = null).hasAuxiliaryIndication)
        assertFalse(LegacyLocalEvidence(firstInstallTimeMs = null, lastUpdateTimeMs = null,
            currentVersionCode = null, storedOnboardingCompleted = null).hasAuxiliaryIndication)
    }

    @Test fun `migration preparation cannot introduce an Auth or network dependency into Core`() {
        val source = listOf(File("src/main/java/br/com/calcmot"), File("app/src/main/java/br/com/calcmot"))
            .first { it.isDirectory }
        val evidenceFiles = File(source, "legacy").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(evidenceFiles.isNotEmpty())
        evidenceFiles.forEach { file ->
            val text = file.readText()
            listOf("com.google.firebase", "java.net", "okhttp", "br.com.calcmot.identity",
                "br.com.calcmot.accessibility", "br.com.calcmot.overlay", "br.com.calcmot.securityrecording")
                .forEach { assertFalse("${file.name}: $it", text.contains("import $it")) }
        }
        // Offline/Firebase failure/missing account or snapshot cannot gate existing
        // functionality: there is no consumer of this preparation in the current app.
        source.walkTopDown().filter { it.extension == "kt" && it.parentFile?.name != "legacy" }
            .forEach { assertFalse(it.name, it.readText().contains("br.com.calcmot.legacy")) }
    }
}
