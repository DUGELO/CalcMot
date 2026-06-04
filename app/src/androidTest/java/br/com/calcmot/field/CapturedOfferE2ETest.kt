package br.com.calcmot.field

import br.com.calcmot.model.TripData
import br.com.calcmot.overlay.IOverlayManager
import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.AccessibleLine
import br.com.calcmot.processor.AccessibleTextSource
import br.com.calcmot.processor.ScreenBounds
import br.com.calcmot.processor.OfferParser
import br.com.calcmot.processor.OfferStabilityGate
import br.com.calcmot.processor.OfferTreeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturedOfferE2ETest {

    @Test
    fun allCapturedOffersPassTheMockedAccessibilityTreePipeline() {
        assertEquals("Every user-provided screenshot must be represented", 39, capturedOffers.size)
        assertEquals(
            "Fixture names must be unique",
            capturedOffers.size,
            capturedOffers.map { it.source }.toSet().size
        )

        capturedOffers.forEach { fixture ->
            val validatedText = OfferTreeExtractor.extractOfferText(fixture.toSnapshot())
            assertNotNull("${fixture.source} should pass accessibility tree extraction", validatedText)

            val candidate = OfferParser.parse(validatedText!!)
            assertNotNull("${fixture.source} should parse into an offer candidate", candidate)

            val gate = OfferStabilityGate(requiredMatchingFrames = 2)
            val overlay = RecordingOverlayManager()
            assertNull("${fixture.source} should not show on first frame", gate.accept(candidate))

            val stableTrip = gate.accept(candidate)
            assertNotNull("${fixture.source} should show on second equal frame", stableTrip)
            overlay.showOverlay(stableTrip!!)

            fixture.assertTrip(stableTrip)
            assertTrue("${fixture.source} should call overlay", overlay.showOverlayCalled)
            fixture.assertTrip(overlay.lastTripData!!)
        }
    }

    @Test
    fun changingCapturedOffersRequiresTwoFreshFramesAndNeverReusesPreviousData() {
        val gate = OfferStabilityGate(requiredMatchingFrames = 2)
        val overlay = RecordingOverlayManager()
        var lastShown: TripData? = null

        capturedOffers.forEach { fixture ->
            val candidate = OfferParser.parse(
                OfferTreeExtractor.extractOfferText(fixture.toSnapshot())!!
            )!!

            val firstFrameResult = gate.accept(candidate)
            if (lastShown == null || !fixture.matches(lastShown!!)) {
                assertNull("${fixture.source} must not reuse previous offer on first different frame", firstFrameResult)
            }

            val stableTrip = gate.accept(candidate)
            assertNotNull("${fixture.source} must stabilize after second frame", stableTrip)
            fixture.assertTrip(stableTrip!!)

            overlay.showOverlay(stableTrip)
            fixture.assertTrip(overlay.lastTripData!!)
            lastShown = stableTrip

            assertNull("${fixture.source} invalid frame should reset stability", gate.accept(null))
            overlay.hideOverlay()
            assertTrue("${fixture.source} invalid frame should hide overlay", overlay.hideOverlayCalled)
            overlay.hideOverlayCalled = false
        }
    }

    @Test
    fun mockedMapNoiseAroundCapturedCardsDoesNotCreateAnOfferByItself() {
        capturedOffers.forEach { fixture ->
            val noiseOnlySnapshot = snapshot(
                listOf(
                    line("BR-070", top = 80, left = 120, right = 240),
                    line("Radar de Viagens", top = 180, left = 190, right = 480),
                    line("3", top = 230, left = 470, right = 500),
                    line("445", top = 330, left = 620, right = 690),
                    line("180", top = 430, left = 610, right = 680),
                    line(fixture.pickupTimeMin.toString(), top = 520, left = 300, right = 340),
                    line(fixture.tripDistanceKm.toString(), top = 590, left = 300, right = 390)
                )
            )

            assertNull("${fixture.source} map noise must be rejected", OfferTreeExtractor.extractOfferText(noiseOnlySnapshot))
        }
    }

    private class RecordingOverlayManager : IOverlayManager {
        var showOverlayCalled = false
        var hideOverlayCalled = false
        var removeOverlayCalled = false
        var lastTripData: TripData? = null

        override fun showOverlay(data: TripData) {
            showOverlayCalled = true
            lastTripData = data
        }

        override fun hideOverlay() {
            hideOverlayCalled = true
        }

        override fun removeOverlay() {
            removeOverlayCalled = true
        }
    }
}

private data class CapturedOfferFixture(
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
    private val totalDistanceKm: Double
        get() = pickupDistanceKm + tripDistanceKm

    private val totalTimeMin: Int
        get() = pickupTimeMin + tripTimeMin

    fun toSnapshot(): AccessibilityTreeSnapshot {
        val lines = mutableListOf<AccessibleLine>()
        lines += line(service, top = 70, left = 56, right = 190)
        badges.forEachIndexed { index, badge ->
            lines += line(badge, top = 74 + index * 42, left = 220, right = 560)
        }
        lines += line(fareText(), top = 130, bottom = 205, left = 56, right = 360)
        rating?.let {
            lines += line(String.format(java.util.Locale.US, "%.2f (100)", it).replace(".", ","), top = 220, right = 260)
        }
        lines += line("$pickupTimeMin minutos (${distanceText(pickupDistanceKm)} km) de distancia", top = 340, bottom = 380)
        lines += line("Endereco de origem", top = 390, bottom = 430)
        lines += line("Viagem de $tripTimeMin minutos (${distanceText(tripDistanceKm)} km)", top = 465, bottom = 505)
        lines += line("Endereco de destino", top = 515, bottom = 555)
        lines += line(action, top = 800, bottom = 880, left = 56, right = 664)

        return snapshot(lines)
    }

    fun assertTrip(actual: TripData) {
        assertEquals("$source fare", fare, actual.valor, 0.01)
        assertEquals("$source total distance", totalDistanceKm, actual.distanciaKm, 0.01)
        assertEquals("$source total time", totalTimeMin, actual.minutosTotais)
        assertEquals("$source R$/km", fare / totalDistanceKm, actual.valorPorKm, 0.01)
        assertEquals("$source R$/h", fare / (totalTimeMin / 60.0), actual.valorPorHora, 0.01)
    }

    fun matches(actual: TripData): Boolean {
        return kotlin.math.abs(actual.valor - fare) < 0.01 &&
            kotlin.math.abs(actual.distanciaKm - totalDistanceKm) < 0.01 &&
            actual.minutosTotais == totalTimeMin
    }

    private fun fareText(): String {
        return if (fare % 1.0 == 0.0) {
            "R$ ${fare.toInt()}"
        } else {
            String.format(java.util.Locale("pt", "BR"), "R$ %.2f", fare)
        }
    }
}

private fun distanceText(value: Double): String {
    return String.format(java.util.Locale.US, "%.1f", value)
}

private fun line(
    text: String,
    top: Int,
    bottom: Int = top + 40,
    left: Int = 56,
    right: Int = 640
): AccessibleLine {
    return AccessibleLine(
        text = text,
        bounds = ScreenBounds(left = left, top = top, right = right, bottom = bottom),
        packageName = "com.ubercab.driver",
        className = "android.view.View",
        depth = 4,
        source = AccessibleTextSource.CONTENT_DESCRIPTION,
        visibleToUser = true
    )
}

private fun snapshot(lines: List<AccessibleLine>): AccessibilityTreeSnapshot {
    return AccessibilityTreeSnapshot(
        sourceName = "mock-accessibility-tree",
        capturedAtMillis = 1_000L,
        eventAtMillis = 980L,
        screenWidth = FRAME_WIDTH,
        screenHeight = FRAME_HEIGHT,
        windowCount = 1,
        rootCount = 1,
        nodeCount = lines.size,
        rootPackageName = "com.ubercab.driver",
        rootClassName = "android.view.View",
        lines = lines
    )
}

private const val FRAME_WIDTH = 720
private const val FRAME_HEIGHT = 1000

private val capturedOffers = listOf(
    CapturedOfferFixture(
        source = "Screenshot_20260526_163025_Uber Driver.jpg",
        fare = 10.78,
        rating = 4.90,
        pickupTimeMin = 6,
        pickupDistanceKm = 1.7,
        tripTimeMin = 13,
        tripDistanceKm = 5.5,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163121_Uber Driver.jpg",
        fare = 10.29,
        rating = 4.97,
        pickupTimeMin = 3,
        pickupDistanceKm = 0.7,
        tripTimeMin = 14,
        tripDistanceKm = 5.5,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163135_Uber Driver.jpg",
        fare = 10.29,
        rating = 4.97,
        badges = listOf("Exclusivo"),
        pickupTimeMin = 3,
        pickupDistanceKm = 0.7,
        tripTimeMin = 14,
        tripDistanceKm = 5.5,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163308_Uber Driver.jpg",
        fare = 8.95,
        rating = 4.93,
        pickupTimeMin = 3,
        pickupDistanceKm = 0.9,
        tripTimeMin = 11,
        tripDistanceKm = 4.7,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163325_Uber Driver.jpg",
        fare = 8.95,
        rating = 4.93,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 3,
        pickupDistanceKm = 0.9,
        tripTimeMin = 11,
        tripDistanceKm = 4.7,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163433_Uber Driver.jpg",
        fare = 16.74,
        rating = 4.94,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.2,
        tripTimeMin = 19,
        tripDistanceKm = 11.6,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163443_Uber Driver.jpg",
        fare = 16.74,
        rating = 4.94,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.2,
        tripTimeMin = 19,
        tripDistanceKm = 11.6,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163455_Uber Driver.jpg",
        fare = 13.07,
        rating = 4.93,
        badges = listOf("Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.4,
        tripTimeMin = 13,
        tripDistanceKm = 9.5,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163520_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.96,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 2,
        pickupDistanceKm = 0.5,
        tripTimeMin = 5,
        tripDistanceKm = 1.6,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163555_Uber Driver.jpg",
        fare = 10.23,
        rating = 4.83,
        pickupTimeMin = 5,
        pickupDistanceKm = 1.7,
        tripTimeMin = 9,
        tripDistanceKm = 6.9,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163655_Uber Driver.jpg",
        fare = 7.02,
        rating = 4.92,
        badges = listOf("Verificado"),
        pickupTimeMin = 3,
        pickupDistanceKm = 1.2,
        tripTimeMin = 8,
        tripDistanceKm = 3.5,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163715_Uber Driver.jpg",
        fare = 10.23,
        rating = 4.83,
        pickupTimeMin = 5,
        pickupDistanceKm = 1.7,
        tripTimeMin = 9,
        tripDistanceKm = 6.9,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163731_Uber Driver.jpg",
        fare = 8.89,
        rating = 4.75,
        badges = listOf("Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.0,
        tripTimeMin = 11,
        tripDistanceKm = 3.9,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163804_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.88,
        badges = listOf("Exclusivo"),
        pickupTimeMin = 2,
        pickupDistanceKm = 0.5,
        tripTimeMin = 4,
        tripDistanceKm = 1.1,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163849_Uber Driver.jpg",
        fare = 8.71,
        rating = 4.90,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 2,
        pickupDistanceKm = 0.6,
        tripTimeMin = 12,
        tripDistanceKm = 4.7,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163931_CalcMot.jpg",
        fare = 17.45,
        rating = 4.90,
        badges = listOf("Exclusivo"),
        pickupTimeMin = 5,
        pickupDistanceKm = 1.8,
        tripTimeMin = 21,
        tripDistanceKm = 11.9,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_163945_Uber Driver.jpg",
        fare = 13.08,
        rating = 4.92,
        pickupTimeMin = 6,
        pickupDistanceKm = 1.9,
        tripTimeMin = 16,
        tripDistanceKm = 6.8,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164000_Uber Driver.jpg",
        fare = 21.67,
        rating = 4.96,
        badges = listOf("Verificado"),
        pickupTimeMin = 8,
        pickupDistanceKm = 2.4,
        tripTimeMin = 21,
        tripDistanceKm = 16.7,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164025_Uber Driver.jpg",
        service = "Priority",
        fare = 10.07,
        rating = 4.90,
        badges = listOf("Exclusivo", "Verificado", "+R$ 2,04 incluido para prioridade de"),
        pickupTimeMin = 2,
        pickupDistanceKm = 0.5,
        tripTimeMin = 12,
        tripDistanceKm = 4.7,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164120_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.92,
        badges = listOf("Verificado"),
        pickupTimeMin = 6,
        pickupDistanceKm = 1.8,
        tripTimeMin = 7,
        tripDistanceKm = 2.1,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164135_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.93,
        badges = listOf("Verificado"),
        pickupTimeMin = 5,
        pickupDistanceKm = 1.8,
        tripTimeMin = 6,
        tripDistanceKm = 1.8,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164152_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.93,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 2,
        pickupDistanceKm = 0.5,
        tripTimeMin = 4,
        tripDistanceKm = 1.1,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164215_Uber Driver.jpg",
        fare = 12.82,
        rating = 4.92,
        badges = listOf("Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.4,
        tripTimeMin = 14,
        tripDistanceKm = 7.7,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164232_Uber Driver.jpg",
        fare = 8.76,
        rating = 4.89,
        badges = listOf("Exclusivo", "Verificado", "Varias paradas"),
        pickupTimeMin = 5,
        pickupDistanceKm = 1.5,
        tripTimeMin = 13,
        tripDistanceKm = 3.1,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164250_Uber Driver.jpg",
        fare = 9.61,
        rating = 4.83,
        pickupTimeMin = 5,
        pickupDistanceKm = 1.7,
        tripTimeMin = 9,
        tripDistanceKm = 6.9,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164308_Uber Driver.jpg",
        fare = 9.61,
        rating = 4.83,
        badges = listOf("Exclusivo"),
        pickupTimeMin = 5,
        pickupDistanceKm = 1.7,
        tripTimeMin = 9,
        tripDistanceKm = 6.9,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164320_Uber Driver.jpg",
        fare = 10.40,
        rating = 4.87,
        pickupTimeMin = 7,
        pickupDistanceKm = 2.4,
        tripTimeMin = 11,
        tripDistanceKm = 4.3,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164330_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.92,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 7,
        pickupDistanceKm = 1.8,
        tripTimeMin = 7,
        tripDistanceKm = 2.1,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164345_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.72,
        badges = listOf("Verificado"),
        pickupTimeMin = 3,
        pickupDistanceKm = 0.7,
        tripTimeMin = 8,
        tripDistanceKm = 2.7,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164349_Uber Driver.jpg",
        fare = 8.82,
        rating = 4.87,
        badges = listOf("Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.5,
        tripTimeMin = 9,
        tripDistanceKm = 5.5,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164357_Uber Driver.jpg",
        fare = 12.35,
        rating = 4.90,
        pickupTimeMin = 5,
        pickupDistanceKm = 1.8,
        tripTimeMin = 12,
        tripDistanceKm = 8.5,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164404_Uber Driver.jpg",
        fare = 9.07,
        rating = 4.91,
        pickupTimeMin = 6,
        pickupDistanceKm = 2.0,
        tripTimeMin = 9,
        tripDistanceKm = 4.0,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164505_Uber Driver.jpg",
        fare = 7.46,
        rating = 4.97,
        pickupTimeMin = 3,
        pickupDistanceKm = 1.1,
        tripTimeMin = 9,
        tripDistanceKm = 3.7,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164830_Uber Driver.jpg",
        fare = 10.85,
        rating = 4.84,
        badges = listOf("Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.4,
        tripTimeMin = 13,
        tripDistanceKm = 5.5,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_164945_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.85,
        badges = listOf("Verificado"),
        pickupTimeMin = 5,
        pickupDistanceKm = 1.7,
        tripTimeMin = 7,
        tripDistanceKm = 2.9,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_165010_Uber Driver.jpg",
        fare = 9.22,
        rating = 4.80,
        pickupTimeMin = 4,
        pickupDistanceKm = 1.1,
        tripTimeMin = 11,
        tripDistanceKm = 4.7,
        action = "Selecionar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_165205_Uber Driver.jpg",
        fare = 9.96,
        rating = 4.90,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.2,
        tripTimeMin = 10,
        tripDistanceKm = 6.1,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_165257_Uber Driver.jpg",
        service = "Priority",
        fare = 29.71,
        rating = 4.90,
        badges = listOf("+R$ 4,43 incluido para prioridade de"),
        pickupTimeMin = 4,
        pickupDistanceKm = 1.5,
        tripTimeMin = 28,
        tripDistanceKm = 22.4,
        action = "Aceitar"
    ),
    CapturedOfferFixture(
        source = "Screenshot_20260526_165331_Uber Driver.jpg",
        fare = 7.00,
        rating = 4.92,
        badges = listOf("Exclusivo", "Verificado"),
        pickupTimeMin = 5,
        pickupDistanceKm = 1.7,
        tripTimeMin = 9,
        tripDistanceKm = 4.5,
        action = "Aceitar"
    )
)
