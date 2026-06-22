package br.com.calcmot.processor

import br.com.calcmot.DriverApp
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UberPipelineBaselineTest {

    @Test
    fun `uber serialized pipeline baseline remains byte identical`() {
        val fixtureRoots = listOf(
            "uiautomator-session-20260601",
            "uiautomator-session-20260601-all"
        )
        val serialized = fixtureRoots.flatMap { root ->
            listResourceDirectory(root).map { fixtureName ->
                serializeFixture(root, fixtureName)
            }
        }.joinToString("\n")

        assertEquals(469, serialized.lineSequence().count())
        assertEquals(EXPECTED_SHA_256, serialized.sha256())
    }

    private fun serializeFixture(root: String, fixtureName: String): String {
        val xml = readResource("$root/$fixtureName")
        val snapshot = UiAutomatorDumpParser.parse(xml, sourceName = fixtureName)
        val legacyInspection = OfferTreeExtractor.inspect(snapshot)
        val dispatchedInspection = DriverOfferTreeExtractor.inspect(
            snapshot.copy(driverApp = DriverApp.UBER)
        )
        assertEquals("$root/$fixtureName inspection", legacyInspection, dispatchedInspection)

        val legacyCandidate = legacyInspection.offerText?.let(OfferParser::parse)
        val dispatchedCandidate = dispatchedInspection.offerText?.let {
            DriverOfferParser.parse(DriverApp.UBER, it)
        }
        assertEquals("$root/$fixtureName candidate", legacyCandidate, dispatchedCandidate)
        val trip = legacyCandidate?.toTripData()

        return listOf(
            root,
            fixtureName,
            legacyInspection.rejectionReason?.name ?: "accepted",
            legacyInspection.hasPrice,
            legacyInspection.hasActionButton,
            legacyInspection.timeDistanceBlockCount,
            legacyCandidate?.fingerprint ?: "none",
            legacyCandidate?.overlayFingerprint() ?: "none",
            trip?.let {
                String.format(
                    Locale.US,
                    "%.2f|%.3f|%d|%.6f|%.6f|%s",
                    it.valor,
                    it.distanciaKm,
                    it.minutosTotais,
                    it.valorPorKm,
                    it.valorPorHora,
                    it.nota?.let { note -> String.format(Locale.US, "%.2f", note) } ?: "none"
                )
            } ?: "none"
        ).joinToString("\t")
    }

    private fun listResourceDirectory(path: String): List<String> {
        val url = checkNotNull(javaClass.classLoader?.getResource(path)) {
            "Missing test resource directory: $path"
        }
        return File(url.toURI())
            .listFiles()
            .orEmpty()
            .map(File::getName)
            .filter { it.endsWith(".xml") }
            .sorted()
    }

    private fun readResource(path: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "Missing test resource: $path"
        }
        return stream.bufferedReader().use { it.readText() }
    }

    private fun String.sha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val EXPECTED_SHA_256 = "0b572aa50f4c47a8ec96629ab32d547ce610cac5a27e3e9b0c49ec3157445125"
    }
}
