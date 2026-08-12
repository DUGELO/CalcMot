package br.com.calcmot.ninetynine

import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import br.com.calcmot.telemetry.AnalyticsEvents
import br.com.calcmot.telemetry.AnalyticsParams
import br.com.calcmot.telemetry.AnalyticsValues
import br.com.calcmot.telemetry.TelemetryProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class NinetyNineCaptureEngine(
    private val captureSource: NinetyNineCaptureSource,
    private val ocrEngine: NinetyNineOcrEngine = MlKitNinetyNineOcrEngine(),
    private val stateListener: (NinetyNineEngineState) -> Unit = {},
    private val clockMillis: () -> Long = SystemClock::elapsedRealtime
) : AutoCloseable {
    private val busy = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val closeRequested = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var lastCaptureAtMillis = Long.MIN_VALUE
    private var lastOcrAtMillis = Long.MIN_VALUE
    private var lastVisualSignature: Long? = null
    private var unchangedOcrIntervalMillis = ACTIVE_UNCHANGED_OCR_INTERVAL_MS

    suspend fun captureAndExtract(
        targetBounds: Rect,
        excludedScreenBounds: List<Rect>
    ): NinetyNineCaptureResult {
        if (paused.get()) {
            return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.PAUSED)
        }
        if (closeRequested.get()) {
            return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.CLOSED)
        }
        if (targetBounds.height() <= MIN_TARGET_HEIGHT_PX) {
            return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.INVALID_TARGET)
        }
        val now = clockMillis()
        if (lastCaptureAtMillis != Long.MIN_VALUE && now - lastCaptureAtMillis < cooldownMillis()) {
            return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.COOLDOWN)
        }
        if (!busy.compareAndSet(false, true)) {
            return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.BUSY)
        }
        if (paused.get() || closeRequested.get()) {
            busy.set(false)
            if (closeRequested.get()) closeOcrOnce()
            return NinetyNineCaptureResult.Skipped(
                if (closeRequested.get()) {
                    NinetyNineCaptureSkipReason.CLOSED
                } else {
                    NinetyNineCaptureSkipReason.PAUSED
                }
            )
        }

        lastCaptureAtMillis = now
        return try {
            stateListener(NinetyNineEngineState.CAPTURING)
            val bitmap = try {
                withTimeout(CAPTURE_TIMEOUT_MS) { captureSource.capture(targetBounds) }
            } catch (_: TimeoutCancellationException) {
                Log.w(TAG, "CAPTURE_TIMEOUT")
                return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.CAPTURE_TIMEOUT)
            }
                ?: return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.CAPTURE_FAILED)
            var recycleImmediately = true
            try {
                val visualSignature = bitmap.visualSignature()
                if (visualSignature == lastVisualSignature &&
                    lastOcrAtMillis != Long.MIN_VALUE &&
                    now - lastOcrAtMillis < unchangedOcrIntervalMillis
                ) {
                    return NinetyNineCaptureResult.Skipped(
                        NinetyNineCaptureSkipReason.UNCHANGED_FRAME
                    )
                }
                lastVisualSignature = visualSignature
                lastOcrAtMillis = now
                stateListener(NinetyNineEngineState.OCR)
                trackOcr(AnalyticsEvents.NINETY_NINE_OCR_STARTED)
                val frame = try {
                    withTimeout(OCR_TIMEOUT_MS) {
                        ocrEngine.recognize(
                            bitmap = bitmap,
                            cropOriginX = targetBounds.left,
                            cropOriginY = targetBounds.top,
                            excludedScreenBounds = excludedScreenBounds
                        )
                    }
                } catch (_: TimeoutCancellationException) {
                    // ML Kit tasks are not cancellable. Keep the bitmap alive briefly so a late
                    // native callback cannot read recycled memory, then abandon this frame.
                    recycleImmediately = false
                    recycleLater(bitmap)
                    Log.w(TAG, "OCR_TIMEOUT")
                    trackOcrFailure(NinetyNineCaptureSkipReason.OCR_TIMEOUT)
                    return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.OCR_TIMEOUT)
                } catch (cancelled: CancellationException) {
                    recycleImmediately = false
                    recycleLater(bitmap)
                    throw cancelled
                }
                if (frame == null) {
                    trackOcrFailure(NinetyNineCaptureSkipReason.OCR_FAILED)
                    return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.OCR_FAILED)
                }
                trackOcr(AnalyticsEvents.NINETY_NINE_OCR_SUCCESS)
                val extraction = NinetyNineOfferExtractor.extract(frame)
                unchangedOcrIntervalMillis = when (extraction) {
                    is NinetyNineExtractionResult.Candidate -> ACTIVE_UNCHANGED_OCR_INTERVAL_MS
                    is NinetyNineExtractionResult.Rejected -> {
                        if (extraction.reason == NinetyNineExtractionRejection.INACTIVE_FRAME ||
                            extraction.reason == NinetyNineExtractionRejection.NO_OFFER_MARKER
                        ) {
                            IDLE_UNCHANGED_OCR_INTERVAL_MS
                        } else {
                            ACTIVE_UNCHANGED_OCR_INTERVAL_MS
                        }
                    }
                }
                NinetyNineCaptureResult.Extracted(extraction)
            } finally {
                if (recycleImmediately && !bitmap.isRecycled) bitmap.recycle()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "CAPTURE_FAILURE_CONTAINED type=${error.javaClass.simpleName}")
            trackOcrFailure(NinetyNineCaptureSkipReason.INTERNAL_ERROR)
            TelemetryProvider.crashReporter.recordNonFatal(
                error = error,
                reason = "internal_error",
                params = mapOf(
                    AnalyticsParams.PLATFORM to AnalyticsValues.PLATFORM_NINETY_NINE,
                    AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_NINETY_NINE_OCR,
                    AnalyticsParams.PIPELINE_STATE to "ocr"
                )
            )
            NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.INTERNAL_ERROR)
        } finally {
            busy.set(false)
            stateListener(NinetyNineEngineState.IDLE)
            if (closeRequested.get()) closeOcrOnce()
        }
    }

    override fun close() {
        paused.set(true)
        closeRequested.set(true)
        if (!busy.get()) closeOcrOnce()
    }

    fun pause() {
        paused.set(true)
    }

    fun resume() {
        if (!closeRequested.get()) paused.set(false)
    }

    fun resetTransientState(): Boolean {
        if (busy.get()) return false
        lastCaptureAtMillis = Long.MIN_VALUE
        lastOcrAtMillis = Long.MIN_VALUE
        lastVisualSignature = null
        unchangedOcrIntervalMillis = ACTIVE_UNCHANGED_OCR_INTERVAL_MS
        return true
    }

    private fun recycleLater(bitmap: android.graphics.Bitmap) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!bitmap.isRecycled) bitmap.recycle()
        }, LATE_OCR_BITMAP_GRACE_MS)
    }

    private fun closeOcrOnce() {
        if (closed.compareAndSet(false, true)) {
            runCatching { ocrEngine.close() }
        }
    }

    private fun cooldownMillis(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MODERN_COOLDOWN_MS
        } else {
            LEGACY_COOLDOWN_MS
        }
    }

    private fun trackOcr(event: String) {
        TelemetryProvider.analytics.track(
            event,
            mapOf(
                AnalyticsParams.PLATFORM to AnalyticsValues.PLATFORM_NINETY_NINE,
                AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_NINETY_NINE_OCR,
                AnalyticsParams.PIPELINE_STATE to "ocr"
            )
        )
    }

    private fun trackOcrFailure(reason: NinetyNineCaptureSkipReason) {
        TelemetryProvider.analytics.track(
            AnalyticsEvents.NINETY_NINE_OCR_FAILED,
            mapOf(
                AnalyticsParams.PLATFORM to AnalyticsValues.PLATFORM_NINETY_NINE,
                AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_NINETY_NINE_OCR,
                AnalyticsParams.PIPELINE_STATE to "failed",
                AnalyticsParams.REASON to reason.name.lowercase()
            )
        )
    }

    private companion object {
        const val MODERN_COOLDOWN_MS = 500L
        const val LEGACY_COOLDOWN_MS = 1_000L
        const val ACTIVE_UNCHANGED_OCR_INTERVAL_MS = 3_000L
        const val IDLE_UNCHANGED_OCR_INTERVAL_MS = 6_000L
        const val MIN_TARGET_HEIGHT_PX = 120
        const val CAPTURE_TIMEOUT_MS = 8_000L
        const val OCR_TIMEOUT_MS = 15_000L
        const val LATE_OCR_BITMAP_GRACE_MS = 30_000L
        const val TAG = "CalcMot99Capture"
    }
}

enum class NinetyNineCaptureSkipReason {
    INVALID_TARGET,
    COOLDOWN,
    BUSY,
    PAUSED,
    UNCHANGED_FRAME,
    CAPTURE_FAILED,
    CAPTURE_TIMEOUT,
    OCR_FAILED,
    OCR_TIMEOUT,
    INTERNAL_ERROR,
    CLOSED
}

enum class NinetyNineEngineState {
    IDLE,
    CAPTURING,
    OCR
}

sealed interface NinetyNineCaptureResult {
    data class Extracted(val result: NinetyNineExtractionResult) : NinetyNineCaptureResult
    data class Skipped(val reason: NinetyNineCaptureSkipReason) : NinetyNineCaptureResult
}

private fun android.graphics.Bitmap.visualSignature(): Long {
    val sampledWidth = (width * SIGNATURE_WIDTH_RATIO).toInt().coerceAtLeast(1)
    var hash = FNV_OFFSET_BASIS
    for (row in 0 until SIGNATURE_ROWS) {
        val y = ((row + 0.5) * height / SIGNATURE_ROWS).toInt().coerceIn(0, height - 1)
        for (column in 0 until SIGNATURE_COLUMNS) {
            val x = ((column + 0.5) * sampledWidth / SIGNATURE_COLUMNS)
                .toInt()
                .coerceIn(0, sampledWidth - 1)
            val color = getPixel(x, y)
            val luminance = (
                android.graphics.Color.red(color) * 3 +
                    android.graphics.Color.green(color) * 6 +
                    android.graphics.Color.blue(color)
                ) / 10
            hash = (hash xor (luminance / LUMINANCE_BUCKET_SIZE).toLong()) * FNV_PRIME
        }
    }
    return hash
}

private const val SIGNATURE_COLUMNS = 12
private const val SIGNATURE_ROWS = 18
private const val SIGNATURE_WIDTH_RATIO = 0.65
private const val LUMINANCE_BUCKET_SIZE = 16
private const val FNV_OFFSET_BASIS = -3750763034362895579L
private const val FNV_PRIME = 1099511628211L
