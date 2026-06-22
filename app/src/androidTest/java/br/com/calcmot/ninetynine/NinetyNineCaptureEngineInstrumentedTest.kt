package br.com.calcmot.ninetynine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.calcmot.processor.ScreenBounds
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NinetyNineCaptureEngineInstrumentedTest {

    @Test
    fun captureEngineKeepsOnlyOneFrameInFlightAndClosesRecognizer() = runBlocking {
        val sourceCalls = AtomicInteger(0)
        val source = object : NinetyNineCaptureSource {
            override suspend fun capture(targetBounds: Rect): Bitmap {
                sourceCalls.incrementAndGet()
                return Bitmap.createBitmap(
                    targetBounds.width(),
                    targetBounds.height(),
                    Bitmap.Config.ARGB_8888
                )
            }
        }
        val ocrStarted = CompletableDeferred<Unit>()
        val releaseOcr = CompletableDeferred<Unit>()
        var closed = false
        val ocr = object : NinetyNineOcrEngine {
            override suspend fun recognize(
                bitmap: Bitmap,
                cropOriginX: Int,
                cropOriginY: Int,
                excludedScreenBounds: List<Rect>
            ): NinetyNineOcrFrame {
                ocrStarted.complete(Unit)
                releaseOcr.await()
                return offerFrame()
            }

            override fun close() {
                closed = true
            }
        }
        var now = 10_000L
        val engine = NinetyNineCaptureEngine(source, ocr) { now }
        val target = Rect(0, 0, 720, 1516)

        val first = async { engine.captureAndExtract(target, emptyList()) }
        ocrStarted.await()
        val overlapping = engine.captureAndExtract(target, emptyList())
        releaseOcr.complete(Unit)
        val completed = first.await()
        engine.close()

        assertEquals(1, sourceCalls.get())
        assertTrue(overlapping is NinetyNineCaptureResult.Skipped)
        assertTrue(completed is NinetyNineCaptureResult.Extracted)
        assertTrue(closed)
    }

    @Test
    fun captureEngineAppliesModernCooldownWithoutAllocatingAnotherBitmap() = runBlocking {
        val sourceCalls = AtomicInteger(0)
        val source = object : NinetyNineCaptureSource {
            override suspend fun capture(targetBounds: Rect): Bitmap {
                sourceCalls.incrementAndGet()
                return Bitmap.createBitmap(
                    targetBounds.width(),
                    targetBounds.height(),
                    Bitmap.Config.ARGB_8888
                )
            }
        }
        val ocr = object : NinetyNineOcrEngine {
            override suspend fun recognize(
                bitmap: Bitmap,
                cropOriginX: Int,
                cropOriginY: Int,
                excludedScreenBounds: List<Rect>
            ): NinetyNineOcrFrame = offerFrame()

            override fun close() = Unit
        }
        var now = 20_000L
        val engine = NinetyNineCaptureEngine(source, ocr) { now }
        val target = Rect(0, 0, 720, 1516)

        val first = engine.captureAndExtract(target, emptyList())
        now += 100L
        val throttled = engine.captureAndExtract(target, emptyList())
        now += 500L
        val unchanged = engine.captureAndExtract(target, emptyList())

        assertTrue(first is NinetyNineCaptureResult.Extracted)
        assertEquals(
            NinetyNineCaptureSkipReason.COOLDOWN,
            (throttled as NinetyNineCaptureResult.Skipped).reason
        )
        assertEquals(
            NinetyNineCaptureSkipReason.UNCHANGED_FRAME,
            (unchanged as NinetyNineCaptureResult.Skipped).reason
        )
        assertEquals(2, sourceCalls.get())
        engine.close()
    }

    @Test
    fun bundledMlKitReadsAnAnonymizedReal99Card() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = context.filesDir.resolve("99-offer.png")
        assumeTrue("External real-card fixture was not staged", fixture.exists())
        val bitmap = BitmapFactory.decodeFile(fixture.absolutePath)
        assertNotNull(bitmap)
        val ocr = MlKitNinetyNineOcrEngine()

        val frame = try {
            ocr.recognize(bitmap, 0, 0, emptyList())
        } finally {
            bitmap.recycle()
            ocr.close()
        }
        assertNotNull(frame)
        val result = NinetyNineOfferExtractor.extract(frame!!)
        assertTrue("result=$result", result is NinetyNineExtractionResult.Candidate)
        val candidate = (result as NinetyNineExtractionResult.Candidate).value
        assertEquals(15.20, candidate.price, 0.01)
        assertEquals(2.5, candidate.pickupDistanceKm, 0.01)
        assertEquals(8, candidate.pickupTimeMin)
        assertEquals(14.0, candidate.tripDistanceKm, 0.01)
        assertEquals(13, candidate.tripTimeMin)
    }

    @Test
    fun bundledMlKitReadsARealDarkTheme99CardWithInvertedFallback() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = context.filesDir.resolve("99-dark-offer.png")
        assumeTrue("External dark-card fixture was not staged", fixture.exists())
        val bitmap = BitmapFactory.decodeFile(fixture.absolutePath)
        assertNotNull(bitmap)
        val ocr = MlKitNinetyNineOcrEngine()

        val frame = try {
            ocr.recognize(bitmap, 0, 0, emptyList())
        } finally {
            bitmap.recycle()
            ocr.close()
        }
        assertNotNull(frame)
        val result = NinetyNineOfferExtractor.extract(frame!!)
        assertTrue("result=$result lines=${frame.lines.map { it.text }}", result is NinetyNineExtractionResult.Candidate)
        val candidate = (result as NinetyNineExtractionResult.Candidate).value
        assertEquals(13.30, candidate.price, 0.01)
        assertEquals(1.3, candidate.pickupDistanceKm, 0.01)
        assertEquals(5, candidate.pickupTimeMin)
        assertEquals(4.1, candidate.tripDistanceKm, 0.01)
        assertEquals(10, candidate.tripTimeMin)
    }

    @Test
    fun bundledMlKitSegmentsARealMultiCard99Screen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = context.filesDir.resolve("99-multi-card.png")
        assumeTrue("External multi-card fixture was not staged", fixture.exists())
        val bitmap = BitmapFactory.decodeFile(fixture.absolutePath)
        assertNotNull(bitmap)
        val ocr = MlKitNinetyNineOcrEngine()

        val frame = try {
            ocr.recognize(bitmap, 0, 0, emptyList())
        } finally {
            bitmap.recycle()
            ocr.close()
        }
        assertNotNull(frame)
        val result = NinetyNineOfferExtractor.extract(frame!!)
        assertTrue("result=$result lines=${frame.lines.map { it.text }}", result is NinetyNineExtractionResult.Candidate)
        val candidate = (result as NinetyNineExtractionResult.Candidate).value
        assertEquals(8.20, candidate.price, 0.01)
        assertEquals(2.2, candidate.pickupDistanceKm, 0.01)
        assertEquals(7, candidate.pickupTimeMin)
        assertEquals(3.8, candidate.tripDistanceKm, 0.01)
        assertEquals(8, candidate.tripTimeMin)
    }

    private fun offerFrame(): NinetyNineOcrFrame {
        val text = listOf(
            "99Pop",
            "R$ 9,51",
            "7 min (3,2 km)",
            "11 min (6,2 km)",
            "Aceitar"
        )
        return NinetyNineOcrFrame(
            lines = text.mapIndexed { index, line ->
                NinetyNineOcrLine(
                    text = line,
                    bounds = ScreenBounds(20, 100 + index * 60, 700, 150 + index * 60)
                )
            },
            width = 720,
            height = 1516
        )
    }
}
