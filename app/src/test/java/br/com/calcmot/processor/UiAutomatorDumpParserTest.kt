package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiAutomatorDumpParserTest {

    @Test
    fun `selected unique real uiautomator session cards pass tree extraction`() {
        val resourceRoot = "uiautomator-session-20260601"
        val fixtureNames = realSessionFixtureNames()

        assertEquals(39, fixtureNames.size)

        fixtureNames.forEach { fixtureName ->
            val xml = readResource("$resourceRoot/$fixtureName")
            val snapshot = UiAutomatorDumpParser.parse(xml, sourceName = fixtureName)
            val inspection = OfferTreeExtractor.inspect(snapshot)

            assertTrue("$fixtureName should be complete", inspection.isCompleteOffer)
            assertTrue("$fixtureName should include button evidence", inspection.hasActionButton)

            val candidate = OfferParser.parse(inspection.offerText!!)
            assertNotNull("$fixtureName should parse", candidate)
            assertNotNull("$fixtureName should produce sane trip data", candidate!!.toTripData())
        }
    }

    @Test
    fun `full real uiautomator session corpus exposes stable metrics`() {
        val resourceRoot = "uiautomator-session-20260601-all"
        val fixtureNames = listResourceDirectory(resourceRoot)

        assertEquals(430, fixtureNames.size)

        val inspections = fixtureNames.map { fixtureName ->
            val xml = readResource("$resourceRoot/$fixtureName")
            val snapshot = UiAutomatorDumpParser.parse(xml, sourceName = fixtureName)
            val inspection = OfferTreeExtractor.inspect(snapshot)
            val normalizedLines = snapshot.lines.map { TextNormalizer.searchKey(it.text) }
            SessionFrameMetric(
                inspection = inspection,
                fingerprint = inspection.offerText
                    ?.let(OfferParser::parse)
                    ?.fingerprint,
                hasPrice = snapshot.lines.any { FarePriceExtractor.containsPrimaryFare(it.text) },
                hasPickup = normalizedLines.any { it.matches(pickupLineRegex) },
                hasTrip = normalizedLines.any { it.matches(tripLineRegex) },
                hasButton = normalizedLines.any { it == "aceitar" || it == "selecionar" }
            )
        }

        assertEquals(133, inspections.count { it.inspection.isCompleteOffer })
        assertEquals(40, inspections.mapNotNull { it.fingerprint }.toSet().size)
        assertEquals(133, inspections.count { it.hasPrice })
        assertEquals(133, inspections.count { it.hasPickup })
        assertEquals(131, inspections.count { it.hasTrip })
        assertEquals(145, inspections.count { it.hasButton })
        assertEquals(0, inspections.count { it.inspection.isCompleteOffer && it.fingerprint == null })
    }

    @Test
    fun `real uia only cards from lab session are fixed regressions`() {
        val resourceRoot = "uiautomator-session-20260603-uia-only"
        val expectedFingerprints = mapOf(
            "20260603-165545-736.xml" to "44.23|25.1|55",
            "20260603-165436-230.xml" to "32.07|20.2|36",
            "20260603-165654-084.xml" to "23.45|10.5|27",
            "20260603-165648-296.xml" to "58.71|46.4|83",
            "20260603-165605-871.xml" to "61.13|42.6|70",
            "20260603-165454-787.xml" to "45.83|36.7|63",
            "20260603-165630-065.xml" to "15.16|10.4|22"
        )

        assertEquals(7, listResourceDirectory(resourceRoot).size)

        expectedFingerprints.forEach { (fixtureName, expectedFingerprint) ->
            val xml = readResource("$resourceRoot/$fixtureName")
            val snapshot = UiAutomatorDumpParser.parse(xml, sourceName = fixtureName)
            val inspection = OfferTreeExtractor.inspect(snapshot)

            assertTrue("$fixtureName should be complete", inspection.isCompleteOffer)
            assertTrue("$fixtureName should expose action button evidence", inspection.hasActionButton)

            val candidate = OfferParser.parse(inspection.offerText!!)
            assertNotNull("$fixtureName should parse", candidate)
            assertEquals("$fixtureName fingerprint", expectedFingerprint, candidate!!.overlayFingerprint())
            assertNotNull("$fixtureName should produce sane trip data", candidate.toTripData())
        }
    }

    @Test
    fun `recent production oracle cards remain parseable regressions`() {
        val resourceRoot = "uiautomator-session-20260603-recent"
        val expectedFingerprints = mapOf(
            "20260603-221654-122.xml" to "12.59|9.2|15",
            "20260603-221748-860.xml" to "10.29|6.6|16",
            "20260603-221753-897.xml" to "17.52|10.5|28"
        )

        assertEquals(3, listResourceDirectory(resourceRoot).size)

        expectedFingerprints.forEach { (fixtureName, expectedFingerprint) ->
            val xml = readResource("$resourceRoot/$fixtureName")
            val snapshot = UiAutomatorDumpParser.parse(xml, sourceName = fixtureName)
            val inspection = OfferTreeExtractor.inspect(snapshot)

            assertTrue("$fixtureName should be complete", inspection.isCompleteOffer)

            val candidate = OfferParser.parse(inspection.offerText!!)
            assertNotNull("$fixtureName should parse", candidate)
            assertEquals("$fixtureName fingerprint", expectedFingerprint, candidate!!.overlayFingerprint())
            assertNotNull("$fixtureName should produce sane trip data", candidate.toTripData())
        }
    }

    @Test
    fun `latest no ocr oracle cards remain parseable regressions`() {
        val resourceRoot = "uiautomator-session-20260604-recent"
        val expectedFingerprints = mapOf(
            "20260604-171931-355.xml" to "10.05|7.5|20",
            "20260604-172301-414.xml" to "9.89|7.3|20"
        )

        assertEquals(2, listResourceDirectory(resourceRoot).size)

        expectedFingerprints.forEach { (fixtureName, expectedFingerprint) ->
            val xml = readResource("$resourceRoot/$fixtureName")
            val snapshot = UiAutomatorDumpParser.parse(xml, sourceName = fixtureName)
            val inspection = OfferTreeExtractor.inspect(snapshot)

            assertTrue("$fixtureName should be complete", inspection.isCompleteOffer)

            val candidate = OfferParser.parse(inspection.offerText!!)
            assertNotNull("$fixtureName should parse", candidate)
            assertEquals("$fixtureName fingerprint", expectedFingerprint, candidate!!.overlayFingerprint())
            assertNotNull("$fixtureName should produce sane trip data", candidate.toTripData())
        }
    }

    @Test
    fun `uiautomator dump with complete offer becomes trip data`() {
        val snapshot = UiAutomatorDumpParser.parse(completeOfferDump, sourceName = "uiautomator-card")
        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)
        assertNotNull(inspection.offerText)

        val candidate = OfferParser.parse(inspection.offerText!!)
        assertNotNull(candidate)
        assertEquals(7.0, candidate!!.price, 0.01)
        assertEquals(0.2, candidate.pickupDistanceKm, 0.01)
        assertEquals(2, candidate.pickupTimeMin)
        assertEquals(1.8, candidate.tripDistanceKm, 0.01)
        assertEquals(5, candidate.tripTimeMin)

        val trip = candidate.toTripData()
        assertNotNull(trip)
        assertEquals(2.0, trip!!.distanciaKm, 0.01)
        assertEquals(7, trip.minutosTotais)
    }

    @Test
    fun `uiautomator dump ignores priority bonus as primary fare`() {
        val snapshot = UiAutomatorDumpParser.parse(priorityOfferDump, sourceName = "uiautomator-priority")
        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)

        val candidate = OfferParser.parse(inspection.offerText!!)
        assertNotNull(candidate)
        assertEquals(9.33, candidate!!.price, 0.01)
        assertEquals(2.1, candidate.pickupDistanceKm, 0.01)
        assertEquals(8, candidate.pickupTimeMin)
        assertEquals(3.9, candidate.tripDistanceKm, 0.01)
        assertEquals(11, candidate.tripTimeMin)
    }

    @Test
    fun `uiautomator dump supports long trip with hours`() {
        val snapshot = UiAutomatorDumpParser.parse(longTripDump, sourceName = "uiautomator-long-trip")
        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)

        val candidate = OfferParser.parse(inspection.offerText!!)
        assertNotNull(candidate)
        assertEquals(29.71, candidate!!.price, 0.01)
        assertEquals(1.5, candidate.pickupDistanceKm, 0.01)
        assertEquals(4, candidate.pickupTimeMin)
        assertEquals(22.4, candidate.tripDistanceKm, 0.01)
        assertEquals(70, candidate.tripTimeMin)
    }

    @Test
    fun `uiautomator dump with content description is included as accessible line`() {
        val snapshot = UiAutomatorDumpParser.parse(contentDescriptionDump)

        assertTrue(snapshot.lines.any { it.text == "Selecionar" && it.source == AccessibleTextSource.CONTENT_DESCRIPTION })
    }

    @Test
    fun `uiautomator dump with map noise is rejected`() {
        val snapshot = UiAutomatorDumpParser.parse(mapOnlyDump, sourceName = "uiautomator-map")
        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertEquals(TreeRejectionReason.NO_PRICE, inspection.rejectionReason)
    }
}

private fun realSessionFixtureNames(): List<String> {
    return (1..39).mapNotNull { index ->
        listResourceDirectory("uiautomator-session-20260601")
            .filter { name -> name.startsWith("%02d-".format(index)) }
            .singleOrNull()
    }
}

private fun listResourceDirectory(path: String): List<String> {
    val url = UiAutomatorDumpParserTest::class.java.classLoader
        ?.getResource(path)
        ?: error("Missing test resource directory: $path")
    return java.io.File(url.toURI())
        .listFiles()
        ?.map { it.name }
        ?.filter { it.endsWith(".xml") }
        ?.sorted()
        ?: emptyList()
}

private fun readResource(path: String): String {
    val stream = UiAutomatorDumpParserTest::class.java.classLoader
        ?.getResourceAsStream(path)
        ?: error("Missing test resource: $path")
    return stream.bufferedReader().use { it.readText() }
}

private data class SessionFrameMetric(
    val inspection: TreeOfferInspection,
    val fingerprint: String?,
    val hasPrice: Boolean,
    val hasPickup: Boolean,
    val hasTrip: Boolean,
    val hasButton: Boolean
)

private val pickupLineRegex = Regex(""".*\b[0-9]+\s*minutos?\s*\([0-9]+(?:[.,][0-9]+)?\s*km\)\s*de dist.*""")
private val tripLineRegex = Regex(""".*viagem\s+de\s+(?:[0-9]+\s*minutos?|[0-9]+\s*hora).*?\([0-9]+(?:[.,][0-9]+)?\s*km\).*""")

private val completeOfferDump = """
    <hierarchy rotation="0">
      <node index="0" text="" class="android.widget.FrameLayout" package="com.ubercab.driver" bounds="[0,0][720,1600]">
        <node index="0" text="UberX" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,830][208,888]" />
        <node index="1" text="Exclusivo" class="android.widget.TextView" package="com.ubercab.driver" bounds="[226,830][373,888]" />
        <node index="2" text="R$&#160;7" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,920][250,1010]" />
        <node index="3" text="Verificado" class="android.widget.TextView" package="com.ubercab.driver" bounds="[284,1012][480,1068]" />
        <node index="4" text="2 minutos (0.2&#160;km) de distância" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1110][590,1160]" />
        <node index="5" text="Rua Um, Águas Lindas de Goiás" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1170][640,1225]" />
        <node index="6" text="Viagem de 5 minutos (1.8&#160;km)" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1240][600,1290]" />
        <node index="7" text="Rua Dois, Águas Lindas de Goiás" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1300][640,1360]" />
        <node index="8" text="Aceitar" class="android.widget.Button" package="com.ubercab.driver" bounds="[56,1400][664,1510]" />
      </node>
    </hierarchy>
""".trimIndent()

private val priorityOfferDump = """
    <hierarchy rotation="0">
      <node index="0" text="" class="android.widget.FrameLayout" package="com.ubercab.driver" bounds="[0,0][720,1600]">
        <node index="0" text="Priority" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,760][220,820]" />
        <node index="1" text="Exclusivo" class="android.widget.TextView" package="com.ubercab.driver" bounds="[238,760][390,820]" />
        <node index="2" text="R$&#160;9,33" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,846][310,940]" />
        <node index="3" text="+R$&#160;2,04 incluído para prioridade de" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,1030][620,1090]" />
        <node index="4" text="8 minutos (2.1&#160;km) de distância" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1120][618,1170]" />
        <node index="5" text="Av. Principal" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1180][618,1235]" />
        <node index="6" text="Viagem de 11 minutos (3.9&#160;km)" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1250][618,1300]" />
        <node index="7" text="Jardim Brasilia" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1310][618,1365]" />
        <node index="8" text="Selecionar" class="android.widget.Button" package="com.ubercab.driver" bounds="[56,1420][664,1530]" />
      </node>
    </hierarchy>
""".trimIndent()

private val contentDescriptionDump = """
    <hierarchy rotation="0">
      <node index="0" text="" class="android.widget.FrameLayout" package="com.ubercab.driver" bounds="[0,0][720,1600]">
        <node index="0" text="" content-desc="Selecionar" class="android.view.View" package="com.ubercab.driver" bounds="[56,1420][664,1530]" />
      </node>
    </hierarchy>
""".trimIndent()

private val mapOnlyDump = """
    <hierarchy rotation="0">
      <node index="0" text="" class="android.widget.FrameLayout" package="com.ubercab.driver" bounds="[0,0][720,1600]">
        <node index="0" text="BR-070" class="android.widget.TextView" package="com.ubercab.driver" bounds="[100,180][230,220]" />
        <node index="1" text="Radar de Viagens" class="android.widget.TextView" package="com.ubercab.driver" bounds="[210,720][460,780]" />
        <node index="2" text="3" class="android.widget.TextView" package="com.ubercab.driver" bounds="[470,720][520,780]" />
        <node index="3" text="180" class="android.widget.TextView" package="com.ubercab.driver" bounds="[610,780][680,830]" />
      </node>
    </hierarchy>
""".trimIndent()

private val longTripDump = """
    <hierarchy rotation="0">
      <node index="0" text="" class="android.widget.FrameLayout" package="com.ubercab.driver" bounds="[0,0][720,1600]">
        <node index="0" text="Priority" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,720][220,780]" />
        <node index="1" text="Exclusivo" class="android.widget.TextView" package="com.ubercab.driver" bounds="[238,720][390,780]" />
        <node index="2" text="R$&#160;29,71" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,810][340,906]" />
        <node index="3" text="+R$&#160;4,43 incluÃ­do para prioridade de" class="android.widget.TextView" package="com.ubercab.driver" bounds="[56,992][650,1050]" />
        <node index="4" text="4 minutos (1.5&#160;km) de distÃ¢ncia" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1075][640,1130]" />
        <node index="5" text="Av. Vinte e Um de Abril, Aguas Lindas de Goias" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1140][660,1190]" />
        <node index="6" text="Viagem de 1 hora e 10 minutos (22.4&#160;km)" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1210][670,1265]" />
        <node index="7" text="Ceilandia, Brasilia - DF, Brasil" class="android.widget.TextView" package="com.ubercab.driver" bounds="[112,1275][660,1335]" />
        <node index="8" text="Aceitar" class="android.widget.Button" package="com.ubercab.driver" bounds="[56,1400][664,1510]" />
      </node>
    </hierarchy>
""".trimIndent()
