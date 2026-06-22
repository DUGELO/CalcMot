package br.com.calcmot.ninetynine

import br.com.calcmot.processor.ScreenBounds
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NinetyNineSanitizerTest {

    @Test
    fun `sanitizer corrects currency time distance and common digit confusions`() {
        val cases = mapOf(
            "RS 9:5I" to "R$ 9,51",
            "7 rnin (3,2 krn)" to "7 min (3,2km)",
            "Il min (6.Z km)" to "11 min (6,7km)",
            "3 mnin (O,45 km)" to "3 min (0,45km)",
            "5 min (I,9 km)" to "5 min (1,9km)",
            "8 min (&,4 km)" to "8 min (8,4km)",
            "A min (G,2 km)" to "4 min (6,2km)"
        )

        cases.forEach { (raw, expected) ->
            assertEquals(raw, expected, NinetyNineSanitizer.sanitizeLine(raw))
        }
    }

    @Test
    fun `sanitizer removes promotional and displacement values but keeps the offer fare`() {
        val sanitized = NinetyNineSanitizer.sanitize(
            listOf(
                "R$ 8,30",
                "Preço/km R$ 1,82",
                "Taxa de deslocamento R$ 1,39",
                "9 min (3,8 km)",
                "9 min (5,3 km)"
            )
        )

        assertTrue(sanitized.contains("R$ 8,30"))
        assertTrue(sanitized.none { it.contains("/km") })
        assertTrue(sanitized.none { it.contains("Taxa de deslocamento") })
    }

    @Test
    fun `sanitizer preserves main fare when OCR merges it with value per kilometer`() {
        assertEquals(
            listOf("R$ 13,30"),
            NinetyNineSanitizer.sanitize(
                listOf("R$13,30 Ox1,9 R$2,48/km")
            )
        )
    }

    @Test
    fun `sanitizer mirrors reference rating and missing distance corrections`() {
        assertEquals(
            "4,86 120 corridas Perfil Essencial",
            NinetyNineSanitizer.sanitizeLine("486 120 corridas Perfil Essencial")
        )
        assertEquals(
            "31 min (10,8km)",
            NinetyNineSanitizer.sanitizeLine("31 min (0,8 km)")
        )
    }

    @Test
    fun `ocr extractor parses all frozen 99 fixtures into the existing offer contract`() {
        val expected = mapOf(
            "offer-list-7.10.26-anonymized.txt" to "15.20|2.5|8|14.0|13",
            "offer-negocia-7.10.26-anonymized.txt" to "6.98|1.1|5|2.3|5",
            "offer-negocia-list-7.10.26-20260619-anonymized.txt" to "9.51|3.2|7|6.2|11",
            "offer-expanded-cash-8.30-7.10.26-20260619-anonymized.txt" to "8.30|3.8|9|5.3|9",
            "offer-expanded-app-7.00-7.10.26-20260619-anonymized.txt" to "7.00|0.5|3|2.7|5",
            "offer-expanded-cash-7.00-7.10.26-20260619-anonymized.txt" to "7.00|0.4|3|1.9|5",
            "ocr-noisy-negocia.txt" to "9.51|3.2|7|6.2|11",
            "ocr-noisy-expanded-premium.txt" to "8.30|3.8|9|5.3|9"
        )

        expected.forEach { (fixtureName, fingerprint) ->
            val lines = fixture("99/$fixtureName").lineSequence().filter(String::isNotBlank).toList()
            val frame = NinetyNineOcrFrame(
                lines = lines.mapIndexed { index, line ->
                    NinetyNineOcrLine(
                        text = line,
                        bounds = ScreenBounds(20, 100 + index * 50, 700, 140 + index * 50)
                    )
                },
                width = 720,
                height = 1516
            )

            val result = NinetyNineOfferExtractor.extract(frame)

            assertTrue("$fixtureName result=$result", result is NinetyNineExtractionResult.Candidate)
            val candidate = (result as NinetyNineExtractionResult.Candidate).value
            assertNotNull(candidate.toTripData())
            assertEquals(fingerprint, candidate.fingerprint)
        }
    }

    @Test
    fun `idle and expired frames never become candidates`() {
        val texts = listOf(
            listOf("Buscando", "Ganhos de hoje", "R$ 120,00"),
            listOf("Oferta expirou", "R$ 9,51", "7 min (3,2 km)", "11 min (6,2 km)")
        )

        texts.forEach { lines ->
            val result = NinetyNineOfferExtractor.extract(
                NinetyNineOcrFrame(
                    lines = lines.mapIndexed { index, text ->
                        NinetyNineOcrLine(text, ScreenBounds(0, index * 50, 700, index * 50 + 40))
                    },
                    width = 720,
                    height = 1516
                )
            )
            assertTrue(result is NinetyNineExtractionResult.Rejected)
        }
    }

    @Test
    fun `99 parser keeps nearest duration when address contains 24 h`() {
        val lines = listOf(
            "Escolha uma",
            "Pop Nova",
            "R$13,30 Ox1,9 R$2,48/km",
            "4,95 59 corridas",
            "(5 min 1,3 km) Distribuidora 24h Quadra",
            "(10 min 4,1 km) Chacara Do Pereira",
            "Escolher"
        )
        val result = NinetyNineOfferExtractor.extract(
            NinetyNineOcrFrame(
                lines = lines.mapIndexed { index, text ->
                    NinetyNineOcrLine(text, ScreenBounds(0, index * 50, 700, index * 50 + 40))
                },
                width = 720,
                height = 1516
            )
        )

        assertTrue(result is NinetyNineExtractionResult.Candidate)
        val candidate = (result as NinetyNineExtractionResult.Candidate).value
        assertEquals(13.30, candidate.price, 0.01)
        assertEquals(5, candidate.pickupTimeMin)
        assertEquals(10, candidate.tripTimeMin)
    }

    @Test
    fun `ocr extractor never mixes values from multiple visible cards`() {
        val lines = listOf(
            "Escolha uma",
            "Pop Nova",
            "R$ 14,20",
            "4,93 381 corridas",
            "(5 min 1 km) Origem A",
            "(20 min 11,3 km) Destino A",
            "Escolher",
            "Pop Nova",
            "R$ 11,40",
            "4,89 85 corridas Cartao verif.",
            "(8 min 2,2 km) Origem B",
            "(14 min 6,1 km) Destino B",
            "Escolher"
        )
        val result = NinetyNineOfferExtractor.extract(
            NinetyNineOcrFrame(
                lines = lines.mapIndexed { index, text ->
                    NinetyNineOcrLine(text, ScreenBounds(0, index * 50, 700, index * 50 + 40))
                },
                width = 720,
                height = 1516
            )
        )

        assertTrue(result is NinetyNineExtractionResult.Candidate)
        val candidate = (result as NinetyNineExtractionResult.Candidate).value
        assertEquals(14.20, candidate.price, 0.01)
        assertEquals(1.0, candidate.pickupDistanceKm, 0.01)
        assertEquals(5, candidate.pickupTimeMin)
        assertEquals(11.3, candidate.tripDistanceKm, 0.01)
        assertEquals(20, candidate.tripTimeMin)
    }

    private fun fixture(path: String): String {
        val resource = checkNotNull(javaClass.classLoader?.getResource(path)) {
            "Fixture not found: $path"
        }
        return File(resource.toURI()).readText()
    }
}
