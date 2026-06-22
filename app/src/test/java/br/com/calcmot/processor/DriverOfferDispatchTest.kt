package br.com.calcmot.processor

import br.com.calcmot.DriverApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class DriverOfferDispatchTest {

    @Test
    fun `uber dispatch is equivalent to production parser result`() {
        uberRegressionInputs.forEach { rawText ->
            assertEquals(
                OfferParser.parse(rawText),
                DriverOfferParser.parse(DriverApp.UBER, rawText)
            )
        }
    }

    @Test
    fun `99 parser accepts a complete actionable contract without using bonus as fare`() {
        val candidate = DriverOfferParser.parse(
            DriverApp.NINETY_NINE,
            """
                99Pop
                R$ 18,40
                +R$ 3,00 de bônus
                Embarque: 6 min 2,1 km
                Corrida: 18 min 8,4 km
                Aceitar corrida
            """.trimIndent()
        )

        assertNotNull(candidate)
        assertEquals(18.40, candidate!!.price, 0.01)
        assertEquals(2.1, candidate.pickupDistanceKm, 0.01)
        assertEquals(6, candidate.pickupTimeMin)
        assertEquals(8.4, candidate.tripDistanceKm, 0.01)
        assertEquals(18, candidate.tripTimeMin)
    }

    @Test
    fun `99 parser covers anonymized visual fixture captured from version 7 10 26`() {
        val rawText = fixture("99/offer-list-7.10.26-anonymized.txt")

        val candidate = DriverOfferParser.parse(DriverApp.NINETY_NINE, rawText)

        assertNotNull(candidate)
        assertEquals(15.20, candidate!!.price, 0.01)
        assertEquals(2.5, candidate.pickupDistanceKm, 0.01)
        assertEquals(8, candidate.pickupTimeMin)
        assertEquals(14.0, candidate.tripDistanceKm, 0.01)
        assertEquals(13, candidate.tripTimeMin)
    }

    @Test
    fun `99 parser selects offered fare from real negotiate card`() {
        val rawText = fixture("99/offer-negocia-7.10.26-anonymized.txt")

        assertEquals(
            listOf(6.98, 7.33, 7.54, 7.68),
            FarePriceExtractor.extractPrimaryFareCandidates(rawText)
        )
        assertEquals(5, DurationParser.parseMinutes("5min (1,1km)"))

        val candidate = DriverOfferParser.parse(DriverApp.NINETY_NINE, rawText)

        assertNotNull(candidate)
        assertEquals(6.98, candidate!!.price, 0.01)
        assertEquals(1.1, candidate.pickupDistanceKm, 0.01)
        assertEquals(5, candidate.pickupTimeMin)
        assertEquals(2.3, candidate.tripDistanceKm, 0.01)
        assertEquals(5, candidate.tripTimeMin)
        assertEquals(4.82, candidate.passengerRating!!, 0.01)
    }

    @Test
    fun `99 parser covers four real visual formats captured on 2026 06 19`() {
        val cases = listOf(
            RealNinetyNineCase(
                fixture = "99/offer-negocia-list-7.10.26-20260619-anonymized.txt",
                price = 9.51,
                pickupDistanceKm = 3.2,
                pickupTimeMin = 7,
                tripDistanceKm = 6.2,
                tripTimeMin = 11
            ),
            RealNinetyNineCase(
                fixture = "99/offer-expanded-cash-8.30-7.10.26-20260619-anonymized.txt",
                price = 8.30,
                pickupDistanceKm = 3.8,
                pickupTimeMin = 9,
                tripDistanceKm = 5.3,
                tripTimeMin = 9
            ),
            RealNinetyNineCase(
                fixture = "99/offer-expanded-app-7.00-7.10.26-20260619-anonymized.txt",
                price = 7.00,
                pickupDistanceKm = 0.45,
                pickupTimeMin = 3,
                tripDistanceKm = 2.7,
                tripTimeMin = 5
            ),
            RealNinetyNineCase(
                fixture = "99/offer-expanded-cash-7.00-7.10.26-20260619-anonymized.txt",
                price = 7.00,
                pickupDistanceKm = 0.41,
                pickupTimeMin = 3,
                tripDistanceKm = 1.9,
                tripTimeMin = 5
            )
        )

        cases.forEach { case ->
            val candidate = DriverOfferParser.parse(
                DriverApp.NINETY_NINE,
                fixture(case.fixture)
            )

            assertNotNull(case.fixture, candidate)
            assertEquals(case.price, candidate!!.price, 0.01)
            assertEquals(case.pickupDistanceKm, candidate.pickupDistanceKm, 0.001)
            assertEquals(case.pickupTimeMin, candidate.pickupTimeMin)
            assertEquals(case.tripDistanceKm, candidate.tripDistanceKm, 0.001)
            assertEquals(case.tripTimeMin, candidate.tripTimeMin)
        }
    }

    @Test
    fun `99 parser rejects idle earnings and incomplete screens`() {
        assertNull(
            DriverOfferParser.parse(
                DriverApp.NINETY_NINE,
                "Ganhos de hoje\nR$ 120,00\nBuscando"
            )
        )
        assertNull(
            DriverOfferParser.parse(
                DriverApp.NINETY_NINE,
                "R$ 18,40\nEmbarque: 6 min 2,1 km\nAceitar corrida"
            )
        )
    }

    @Test
    fun `99 tree extractor never consumes uber nodes`() {
        val snapshot = AccessibilityTreeSnapshot(
            sourceName = "mixed-tree",
            capturedAtMillis = 1_000,
            eventAtMillis = 900,
            screenWidth = 720,
            screenHeight = 1600,
            windowCount = 2,
            rootCount = 2,
            nodeCount = 7,
            rootPackageName = "com.app99.driver",
            rootClassName = "android.view.View",
            driverApp = DriverApp.NINETY_NINE,
            lines = listOf(
                line("R$ 18,40", "com.ubercab.driver", 200),
                line("6 min 2,1 km de distância", "com.ubercab.driver", 400),
                line("Viagem de 18 min 8,4 km", "com.ubercab.driver", 600),
                line("Aceitar", "com.ubercab.driver", 800)
            )
        )

        val inspection = DriverOfferTreeExtractor.inspect(snapshot)

        assertNull(inspection.offerText)
        assertEquals(TreeRejectionReason.EMPTY_TREE, inspection.rejectionReason)
    }

    private fun line(text: String, packageName: String, top: Int): AccessibleLine {
        return AccessibleLine(
            text = text,
            bounds = ScreenBounds(20, top, 700, top + 60),
            packageName = packageName
        )
    }

    private fun fixture(path: String): String {
        val resource = checkNotNull(javaClass.classLoader?.getResource(path)) {
            "Fixture not found: $path"
        }
        return File(resource.toURI()).readText()
    }

    private data class RealNinetyNineCase(
        val fixture: String,
        val price: Double,
        val pickupDistanceKm: Double,
        val pickupTimeMin: Int,
        val tripDistanceKm: Double,
        val tripTimeMin: Int
    )

    private val uberRegressionInputs = listOf(
        """
            UberX
            R$ 10,78
            6 minutos (1.7 km) de distancia
            Viagem de 13 minutos (5.5 km)
            Selecionar
        """.trimIndent(),
        """
            Priority
            R$ 29,71
            +R$ 4,43 incluido para prioridade de
            4 minutos (1.5 km) de distancia
            Viagem de 28 minutos (22.4 km)
            Aceitar
        """.trimIndent(),
        "Radar de Viagens\n3\n180\nBR-070"
    )
}
