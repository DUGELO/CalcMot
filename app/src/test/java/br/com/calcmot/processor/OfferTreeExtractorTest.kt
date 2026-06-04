package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class OfferTreeExtractorTest {

    @Test
    fun `all captured cards pass accessibility tree extraction`() {
        assertEquals("Every user-provided screenshot must be represented", 39, capturedTreeOffers.size)
        assertEquals(
            "Fixture names must be unique",
            capturedTreeOffers.size,
            capturedTreeOffers.map { it.source }.toSet().size
        )

        capturedTreeOffers.forEach { fixture ->
            val inspection = OfferTreeExtractor.inspect(fixture.toSnapshot())

            assertTrue("${fixture.source} should be complete in accessibility tree", inspection.isCompleteOffer)
            assertNotNull("${fixture.source} should expose offer text", inspection.offerText)

            val candidate = OfferParser.parse(inspection.offerText!!)
            assertNotNull("${fixture.source} should parse", candidate)

            val tripData = candidate!!.toTripData()
            assertNotNull("${fixture.source} should produce sane trip data", tripData)
            fixture.assertCandidate(candidate)
        }
    }

    @Test
    fun `tree pipeline requires two equal frames and never reuses previous offer`() {
        val gate = OfferStabilityGate(requiredMatchingFrames = 2)
        var lastFingerprint: String? = null

        capturedTreeOffers.take(12).forEach { fixture ->
            val candidate = OfferParser.parse(OfferTreeExtractor.extractOfferText(fixture.toSnapshot())!!)!!

            val firstFrame = gate.accept(candidate)
            if (lastFingerprint != candidate.fingerprint) {
                assertNull("${fixture.source} should wait for a second equal frame", firstFrame)
            }

            val stableTrip = gate.accept(candidate)
            assertNotNull("${fixture.source} should stabilize on the second equal frame", stableTrip)
            fixture.assertTrip(stableTrip!!)
            lastFingerprint = candidate.fingerprint

            assertNull("${fixture.source} invalid frame should reset stability", gate.accept(null))
        }
    }

    @Test
    fun `incomplete tree fails observably without fallback data`() {
        val fixture = capturedTreeOffers.first()
        val incompleteTree = snapshot(
            line(fixture.fareText(), top = 210, bottom = 300, left = 56, right = 390),
            line(
                "${fixture.pickupTimeMin} minutos (${distanceText(fixture.pickupDistanceKm)} km) de distancia",
                top = 670
            )
        )

        val inspection = OfferTreeExtractor.inspect(incompleteTree)

        assertNull(inspection.offerText)
        assertEquals(TreeRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS, inspection.rejectionReason)
    }

    @Test
    fun `map noise without card is rejected`() {
        val snapshot = snapshot(
            line("BR-070", top = 180),
            line("Radar de Viagens", top = 450),
            line("3", top = 510, left = 480, right = 530),
            line("445", top = 620, left = 600, right = 670),
            line("180", top = 760, left = 610, right = 680),
            line("7", top = 840, left = 350, right = 390),
            line("4.7", top = 920, left = 350, right = 430)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertEquals(TreeRejectionReason.NO_PRICE, inspection.rejectionReason)
    }

    @Test
    fun `accessibility tree accepts complete offer even when action button is missing`() {
        val snapshot = snapshot(
            line("R$ 10,85", top = 180),
            line("4 minutos (1.4 km) de distancia", top = 650),
            line("Viagem de 13 minutos (5.5 km)", top = 800)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)
        assertFalse(inspection.hasActionButton)

        val candidate = OfferParser.parse(inspection.offerText!!)
        assertNotNull(candidate)
        assertEquals(10.85, candidate!!.price, 0.01)
        assertEquals(6.9, candidate.totalDistanceKm, 0.01)
        assertEquals(17, candidate.totalTimeMin)
    }

    @Test
    fun `fragmented accessibility tree groups time and distance into trip blocks`() {
        val snapshot = snapshot(
            line("UberX", top = 120, left = 56, right = 208),
            line("R$", top = 205, bottom = 300, left = 56, right = 135),
            line("10,85", top = 205, bottom = 300, left = 136, right = 330),
            line("4 minutos", top = 650, left = 112, right = 260),
            line("(1.4 km) de distancia", top = 650, left = 262, right = 610),
            line("Viagem de 13 minutos", top = 800, left = 112, right = 420),
            line("(5.5 km)", top = 800, left = 422, right = 570),
            line("Aceitar", top = 1320, bottom = 1410, left = 56, right = 664)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)
        assertTrue(inspection.hasActionButton)

        val candidate = OfferParser.parse(inspection.offerText!!)
        assertNotNull(candidate)
        assertEquals(10.85, candidate!!.price, 0.01)
        assertEquals(6.9, candidate.totalDistanceKm, 0.01)
        assertEquals(17, candidate.totalTimeMin)
    }

    @Test
    fun `view id rows are not selected as fare when real text nodes are present`() {
        val snapshot = snapshot(
            line(
                "com.ubercab.driver:id/action_bar_root android:id/content R$ 11,78 " +
                    "5 minutos (1.4 km) de distancia Viagem de 9 minutos (6.3 km)",
                top = 205,
                bottom = 300,
                left = 0,
                right = 720,
                source = AccessibleTextSource.VIEW_ID_RESOURCE_NAME,
                viewId = "com.ubercab.driver:id/action_bar_root"
            ),
            line("UberX", top = 120, left = 56, right = 208),
            line("R$ 11,78", top = 205, bottom = 300, left = 56, right = 347),
            line("5 minutos (1.4 km) de distancia", top = 1082, left = 112, right = 515),
            line("Viagem de 9 minutos (6.3 km)", top = 1166, left = 112, right = 503),
            line("Selecionar", top = 1313, bottom = 1411, left = 56, right = 664)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)
        val candidate = OfferParser.parse(inspection.offerText!!)
        assertNotNull(candidate)
        assertEquals(11.78, candidate!!.price, 0.01)
        assertEquals(7.7, candidate.totalDistanceKm, 0.01)
        assertEquals(14, candidate.totalTimeMin)
    }

    @Test
    fun `price only accessibility evidence is rejected as incomplete`() {
        val snapshot = snapshot(
            line("R$ 24,62", top = 896, bottom = 994, left = 56, right = 358)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertEquals(TreeRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS, inspection.rejectionReason)
    }

    @Test
    fun `action button without price is rejected`() {
        val snapshot = snapshot(
            line("UberX", top = 120),
            line("4 minutos (1.4 km) de distancia", top = 650),
            line("Viagem de 13 minutos (5.5 km)", top = 800),
            line("Aceitar", top = 1320, bottom = 1410)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertEquals(TreeRejectionReason.NO_PRICE, inspection.rejectionReason)
    }

    @Test
    fun `price without two time distance blocks is rejected`() {
        val snapshot = snapshot(
            line("UberX", top = 120),
            line("R$ 10,85", top = 180),
            line("4 minutos (1.4 km) de distancia", top = 650),
            line("Aceitar", top = 1320, bottom = 1410)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertNull(inspection.offerText)
    }

    @Test
    fun `multiple primary prices choose the clear fare above trip blocks`() {
        val snapshot = snapshot(
            line("R$ 10,85", top = 180),
            line("R$ 10,85", top = 205, left = 58, right = 390),
            line("+R$ 2,04 incluido para prioridade de", top = 310),
            line("4 minutos (1.4 km) de distancia", top = 650),
            line("Viagem de 13 minutos (5.5 km)", top = 800),
            line("Aceitar", top = 1320, bottom = 1410)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)
        val candidate = OfferParser.parse(inspection.offerText!!)
        assertNotNull(candidate)
        assertEquals(10.85, candidate!!.price, 0.01)
    }

    @Test
    fun `content description is a first class source even when text nodes are absent`() {
        val snapshot = snapshot(
            line("UberX", top = 120, left = 56, right = 210, source = AccessibleTextSource.CONTENT_DESCRIPTION),
            line("R$ 10,85", top = 210, bottom = 300, source = AccessibleTextSource.CONTENT_DESCRIPTION),
            line("4 minutos (1.4 km) de distancia", top = 670, source = AccessibleTextSource.CONTENT_DESCRIPTION),
            line("Viagem de 13 minutos (5.5 km)", top = 820, source = AccessibleTextSource.CONTENT_DESCRIPTION)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)
        val candidate = OfferParser.parse(inspection.offerText!!)

        assertTrue(inspection.isCompleteOffer)
        assertNotNull(candidate)
        assertEquals(10.85, candidate!!.price, 0.01)
        assertEquals(6.9, candidate.totalDistanceKm, 0.01)
        assertEquals(17, candidate.totalTimeMin)
    }

    @Test
    fun `long content description is split into semantic offer fields`() {
        val snapshot = snapshot(
            line(
                "UberX R$ 18,50 3 minutos (1.2 km) de distancia Viagem de 12 minutos (5.4 km) Aceitar",
                top = 120,
                bottom = 920,
                source = AccessibleTextSource.CONTENT_DESCRIPTION
            )
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)
        val candidate = OfferParser.parse(inspection.offerText!!)

        assertTrue(inspection.isCompleteOffer)
        assertEquals(5, inspection.fieldCandidates.size)
        assertNotNull(candidate)
        assertEquals(18.50, candidate!!.price, 0.01)
        assertEquals(6.6, candidate.totalDistanceKm, 0.01)
        assertEquals(15, candidate.totalTimeMin)
    }

    @Test
    fun `long text node is split into semantic offer fields`() {
        val snapshot = snapshot(
            line(
                "Priority Exclusivo R$ 44,23 +R$ 5,75 incluido " +
                    "2 minutos (0.6 km) de distancia Cln Q. 201 " +
                    "Viagem de 53 minutos (24.5 km) Sobradinho Selecionar",
                top = 700,
                bottom = 1480,
                source = AccessibleTextSource.TEXT
            )
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)
        val candidate = OfferParser.parse(inspection.offerText!!)

        assertTrue(inspection.isCompleteOffer)
        assertTrue(inspection.hasActionButton)
        assertNotNull(candidate)
        assertEquals(44.23, candidate!!.price, 0.01)
        assertEquals(25.1, candidate.totalDistanceKm, 0.01)
        assertEquals(55, candidate.totalTimeMin)
    }

    @Test
    fun `semantic view ids are promoted to known node mappings`() {
        val snapshot = snapshot(
            line("R$ 10,85", top = 210, bottom = 300, viewId = "com.ubercab.driver:id/fare_label"),
            line("4 minutos (1.4 km) de distancia", top = 670, viewId = "com.ubercab.driver:id/pickup_eta"),
            line("Viagem de 13 minutos (5.5 km)", top = 820, viewId = "com.ubercab.driver:id/trip_duration")
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertTrue(inspection.isCompleteOffer)
        assertTrue(
            inspection.knownNodeMappings.any {
                it.viewIdResourceName == "com.ubercab.driver:id/fare_label" &&
                    it.expectedFieldType == OfferFieldType.FARE
            }
        )
        assertTrue(
            inspection.fieldCandidates.any {
                it.source == AccessibleTextSource.TEXT &&
                    it.viewIdResourceName == "com.ubercab.driver:id/pickup_eta"
            }
        )
    }

    @Test
    fun `fields outside visual order are rejected`() {
        val snapshot = snapshot(
            line("4 minutos (1.4 km) de distancia", top = 130),
            line("R$ 10,85", top = 260),
            line("Viagem de 13 minutos (5.5 km)", top = 800),
            line("Selecionar", top = 1320, bottom = 1410)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertNull(inspection.offerText)
    }

    @Test
    fun `bonus only price is rejected`() {
        val snapshot = snapshot(
            line("Priority", top = 120),
            line("+R$ 2,04 incluido para prioridade de", top = 310),
            line("2 minutos (0.5 km) de distancia", top = 680),
            line("Viagem de 12 minutos (4.7 km)", top = 830),
            line("Aceitar", top = 1320, bottom = 1410)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertEquals(TreeRejectionReason.NO_PRICE, inspection.rejectionReason)
    }

    @Test
    fun `top earnings counter is not treated as offer fare`() {
        val snapshot = snapshot(
            line(
                "R$ 0,00",
                top = 69,
                bottom = 131,
                left = 274,
                right = 446,
                viewId = "com.ubercab.driver:id/ub__earnings_tracker_counter_content_status"
            ),
            line("4 minutos (1.4 km) de distancia", top = 670),
            line("Viagem de 13 minutos (5.5 km)", top = 820),
            line("Aceitar", top = 1320, bottom = 1410)
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertFalse(inspection.hasPrice)
        assertEquals(TreeRejectionReason.NO_PRICE, inspection.rejectionReason)
        assertTrue(inspection.fieldCandidates.none { it.fieldType == OfferFieldType.FARE })
    }

    @Test
    fun `calcMot preview and overlay text are ignored by tree extractor`() {
        val snapshot = snapshot(
            line("R$ 2,50/km", top = 1396, left = 215, right = 507, packageName = "br.com.calcmot"),
            line("R$ 41,86/h", top = 1473, left = 180, right = 387, packageName = "br.com.calcmot"),
            line("43 min", top = 1473, left = 412, right = 541, packageName = "br.com.calcmot"),
            line("BOA", top = 1352, left = 333, right = 389, packageName = "br.com.calcmot")
        )

        val inspection = OfferTreeExtractor.inspect(snapshot)

        assertFalse(inspection.isCompleteOffer)
        assertEquals(TreeRejectionReason.NO_PRICE, inspection.rejectionReason)
        assertTrue(inspection.fieldCandidates.none { it.fieldType == OfferFieldType.FARE })
    }
}

private data class CapturedTreeOfferFixture(
    val source: String,
    val service: String = "UberX",
    val fare: Double,
    val rating: Double? = null,
    val badges: List<String> = emptyList(),
    val pickupTimeMin: Int,
    val pickupDistanceKm: Double,
    val tripTimeMin: Int,
    val tripDistanceKm: Double,
    val action: String
) {
    fun toSnapshot(includeButton: Boolean = true): AccessibilityTreeSnapshot {
        val lines = mutableListOf<AccessibleLine>()
        lines += line(service, top = 120, left = 56, right = 210)
        badges.forEachIndexed { index, badge ->
            lines += line(badge, top = 125 + index * 58, left = 230, right = 650)
        }
        lines += line(fareText(), top = 210, bottom = 300, left = 56, right = 390)
        rating?.let {
            lines += line(String.format(Locale.US, "%.2f (100)", it).replace(".", ","), top = 330, right = 280)
        }
        lines += line("$pickupTimeMin minutos (${distanceText(pickupDistanceKm)} km) de distancia", top = 670)
        lines += line("Endereco de origem", top = 735)
        lines += line("Viagem de $tripTimeMin minutos (${distanceText(tripDistanceKm)} km)", top = 820)
        lines += line("Endereco de destino", top = 885)
        if (includeButton) {
            lines += line(action, top = 1320, bottom = 1420, left = 56, right = 664)
        }
        return snapshot(*lines.toTypedArray())
    }

    fun rawText(): String {
        return listOfNotNull(
            service,
            fareText(),
            rating?.let { String.format(Locale.US, "%.2f (100)", it).replace(".", ",") },
            badges.joinToString(" ").takeIf { it.isNotBlank() },
            "$pickupTimeMin minutos (${distanceText(pickupDistanceKm)} km) de distancia",
            "Endereco de origem",
            "Viagem de $tripTimeMin minutos (${distanceText(tripDistanceKm)} km)",
            "Endereco de destino",
            action
        ).joinToString("\n")
    }

    fun assertCandidate(candidate: br.com.calcmot.model.OfferCandidate) {
        assertEquals("$source fare", fare, candidate.price, 0.01)
        assertEquals("$source pickup distance", pickupDistanceKm, candidate.pickupDistanceKm, 0.01)
        assertEquals("$source pickup time", pickupTimeMin, candidate.pickupTimeMin)
        assertEquals("$source trip distance", tripDistanceKm, candidate.tripDistanceKm, 0.01)
        assertEquals("$source trip time", tripTimeMin, candidate.tripTimeMin)
    }

    fun assertTrip(tripData: br.com.calcmot.model.TripData) {
        assertEquals("$source fare", fare, tripData.valor, 0.01)
        assertEquals("$source total distance", pickupDistanceKm + tripDistanceKm, tripData.distanciaKm, 0.01)
        assertEquals("$source total time", pickupTimeMin + tripTimeMin, tripData.minutosTotais)
    }

    fun fareText(): String {
        return if (fare % 1.0 == 0.0) {
            "R$ ${fare.toInt()}"
        } else {
            String.format(Locale("pt", "BR"), "R$ %.2f", fare)
        }
    }
}

private fun distanceText(value: Double): String {
    return String.format(Locale.US, "%.1f", value)
}

private fun snapshot(vararg lines: AccessibleLine): AccessibilityTreeSnapshot {
    return AccessibilityTreeSnapshot(
        sourceName = "test-tree",
        capturedAtMillis = 1_000L,
        eventAtMillis = 900L,
        screenWidth = 720,
        screenHeight = 1600,
        windowCount = 1,
        rootCount = 1,
        nodeCount = lines.size + 1,
        rootPackageName = "com.ubercab.driver",
        rootClassName = "android.view.View",
        lines = lines.toList()
    )
}

private fun line(
    text: String,
    top: Int,
    bottom: Int = top + 42,
    left: Int = 56,
    right: Int = 640,
    depth: Int = 3,
    source: AccessibleTextSource = AccessibleTextSource.TEXT,
    viewId: String? = null,
    packageName: String? = "com.ubercab.driver"
): AccessibleLine {
    return AccessibleLine(
        text = text,
        bounds = ScreenBounds(left = left, top = top, right = right, bottom = bottom),
        packageName = packageName,
        className = "android.widget.TextView",
        viewId = viewId,
        depth = depth,
        source = source,
        visibleToUser = true
    )
}

private val capturedTreeOffers = listOf(
    CapturedTreeOfferFixture("Screenshot_20260526_163025_Uber Driver.jpg", fare = 10.78, rating = 4.90, pickupTimeMin = 6, pickupDistanceKm = 1.7, tripTimeMin = 13, tripDistanceKm = 5.5, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163121_Uber Driver.jpg", fare = 10.29, rating = 4.97, pickupTimeMin = 3, pickupDistanceKm = 0.7, tripTimeMin = 14, tripDistanceKm = 5.5, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163135_Uber Driver.jpg", fare = 10.29, rating = 4.97, badges = listOf("Exclusivo"), pickupTimeMin = 3, pickupDistanceKm = 0.7, tripTimeMin = 14, tripDistanceKm = 5.5, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163308_Uber Driver.jpg", fare = 8.95, rating = 4.93, pickupTimeMin = 3, pickupDistanceKm = 0.9, tripTimeMin = 11, tripDistanceKm = 4.7, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163325_Uber Driver.jpg", fare = 8.95, rating = 4.93, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 3, pickupDistanceKm = 0.9, tripTimeMin = 11, tripDistanceKm = 4.7, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163433_Uber Driver.jpg", fare = 16.74, rating = 4.94, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.2, tripTimeMin = 19, tripDistanceKm = 11.6, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163443_Uber Driver.jpg", fare = 16.74, rating = 4.94, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.2, tripTimeMin = 19, tripDistanceKm = 11.6, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163455_Uber Driver.jpg", fare = 13.07, rating = 4.93, badges = listOf("Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.4, tripTimeMin = 13, tripDistanceKm = 9.5, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163520_Uber Driver.jpg", fare = 7.00, rating = 4.96, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 2, pickupDistanceKm = 0.5, tripTimeMin = 5, tripDistanceKm = 1.6, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163555_Uber Driver.jpg", fare = 10.23, rating = 4.83, pickupTimeMin = 5, pickupDistanceKm = 1.7, tripTimeMin = 9, tripDistanceKm = 6.9, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163655_Uber Driver.jpg", fare = 7.02, rating = 4.92, badges = listOf("Verificado"), pickupTimeMin = 3, pickupDistanceKm = 1.2, tripTimeMin = 8, tripDistanceKm = 3.5, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163715_Uber Driver.jpg", fare = 10.23, rating = 4.83, pickupTimeMin = 5, pickupDistanceKm = 1.7, tripTimeMin = 9, tripDistanceKm = 6.9, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163731_Uber Driver.jpg", fare = 8.89, rating = 4.75, badges = listOf("Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.0, tripTimeMin = 11, tripDistanceKm = 3.9, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163804_Uber Driver.jpg", fare = 7.00, rating = 4.88, badges = listOf("Exclusivo"), pickupTimeMin = 2, pickupDistanceKm = 0.5, tripTimeMin = 4, tripDistanceKm = 1.1, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163849_Uber Driver.jpg", fare = 8.71, rating = 4.90, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 2, pickupDistanceKm = 0.6, tripTimeMin = 12, tripDistanceKm = 4.7, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163931_CalcMot.jpg", fare = 17.45, rating = 4.90, badges = listOf("Exclusivo"), pickupTimeMin = 5, pickupDistanceKm = 1.8, tripTimeMin = 21, tripDistanceKm = 11.9, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_163945_Uber Driver.jpg", fare = 13.08, rating = 4.92, pickupTimeMin = 6, pickupDistanceKm = 1.9, tripTimeMin = 16, tripDistanceKm = 6.8, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164000_Uber Driver.jpg", fare = 21.67, rating = 4.96, badges = listOf("Verificado"), pickupTimeMin = 8, pickupDistanceKm = 2.4, tripTimeMin = 21, tripDistanceKm = 16.7, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164025_Uber Driver.jpg", service = "Priority", fare = 10.07, rating = 4.90, badges = listOf("Exclusivo", "Verificado", "+R$ 2,04 incluido para prioridade de"), pickupTimeMin = 2, pickupDistanceKm = 0.5, tripTimeMin = 12, tripDistanceKm = 4.7, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164120_Uber Driver.jpg", fare = 7.00, rating = 4.92, badges = listOf("Verificado"), pickupTimeMin = 6, pickupDistanceKm = 1.8, tripTimeMin = 7, tripDistanceKm = 2.1, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164135_Uber Driver.jpg", fare = 7.00, rating = 4.93, badges = listOf("Verificado"), pickupTimeMin = 5, pickupDistanceKm = 1.8, tripTimeMin = 6, tripDistanceKm = 1.8, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164152_Uber Driver.jpg", fare = 7.00, rating = 4.93, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 2, pickupDistanceKm = 0.5, tripTimeMin = 4, tripDistanceKm = 1.1, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164215_Uber Driver.jpg", fare = 12.82, rating = 4.92, badges = listOf("Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.4, tripTimeMin = 14, tripDistanceKm = 7.7, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164232_Uber Driver.jpg", fare = 8.76, rating = 4.89, badges = listOf("Exclusivo", "Verificado", "Varias paradas"), pickupTimeMin = 5, pickupDistanceKm = 1.5, tripTimeMin = 13, tripDistanceKm = 3.1, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164250_Uber Driver.jpg", fare = 9.61, rating = 4.83, pickupTimeMin = 5, pickupDistanceKm = 1.7, tripTimeMin = 9, tripDistanceKm = 6.9, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164308_Uber Driver.jpg", fare = 9.61, rating = 4.83, badges = listOf("Exclusivo"), pickupTimeMin = 5, pickupDistanceKm = 1.7, tripTimeMin = 9, tripDistanceKm = 6.9, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164320_Uber Driver.jpg", fare = 10.40, rating = 4.87, pickupTimeMin = 7, pickupDistanceKm = 2.4, tripTimeMin = 11, tripDistanceKm = 4.3, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164330_Uber Driver.jpg", fare = 7.00, rating = 4.92, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 7, pickupDistanceKm = 1.8, tripTimeMin = 7, tripDistanceKm = 2.1, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164345_Uber Driver.jpg", fare = 7.00, rating = 4.72, badges = listOf("Verificado"), pickupTimeMin = 3, pickupDistanceKm = 0.7, tripTimeMin = 8, tripDistanceKm = 2.7, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164349_Uber Driver.jpg", fare = 8.82, rating = 4.87, badges = listOf("Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.5, tripTimeMin = 9, tripDistanceKm = 5.5, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164357_Uber Driver.jpg", fare = 12.35, rating = 4.90, pickupTimeMin = 5, pickupDistanceKm = 1.8, tripTimeMin = 12, tripDistanceKm = 8.5, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164404_Uber Driver.jpg", fare = 9.07, rating = 4.91, pickupTimeMin = 6, pickupDistanceKm = 2.0, tripTimeMin = 9, tripDistanceKm = 4.0, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164505_Uber Driver.jpg", fare = 7.46, rating = 4.97, pickupTimeMin = 3, pickupDistanceKm = 1.1, tripTimeMin = 9, tripDistanceKm = 3.7, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164830_Uber Driver.jpg", fare = 10.85, rating = 4.84, badges = listOf("Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.4, tripTimeMin = 13, tripDistanceKm = 5.5, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_164945_Uber Driver.jpg", fare = 7.00, rating = 4.85, badges = listOf("Verificado"), pickupTimeMin = 5, pickupDistanceKm = 1.7, tripTimeMin = 7, tripDistanceKm = 2.9, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_165010_Uber Driver.jpg", fare = 9.22, rating = 4.80, pickupTimeMin = 4, pickupDistanceKm = 1.1, tripTimeMin = 11, tripDistanceKm = 4.7, action = "Selecionar"),
    CapturedTreeOfferFixture("Screenshot_20260526_165205_Uber Driver.jpg", fare = 9.96, rating = 4.90, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 4, pickupDistanceKm = 1.2, tripTimeMin = 10, tripDistanceKm = 6.1, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_165257_Uber Driver.jpg", service = "Priority", fare = 29.71, rating = 4.90, badges = listOf("+R$ 4,43 incluido para prioridade de"), pickupTimeMin = 4, pickupDistanceKm = 1.5, tripTimeMin = 28, tripDistanceKm = 22.4, action = "Aceitar"),
    CapturedTreeOfferFixture("Screenshot_20260526_165331_Uber Driver.jpg", fare = 7.00, rating = 4.92, badges = listOf("Exclusivo", "Verificado"), pickupTimeMin = 5, pickupDistanceKm = 1.7, tripTimeMin = 9, tripDistanceKm = 4.5, action = "Aceitar")
)
