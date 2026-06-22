package br.com.calcmot.ninetynine

import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

class NinetyNineCaptureEngine(
    private val captureSource: NinetyNineCaptureSource,
    private val ocrEngine: NinetyNineOcrEngine = MlKitNinetyNineOcrEngine(),
    private val clockMillis: () -> Long = SystemClock::elapsedRealtime
) : AutoCloseable {
    private val busy = AtomicBoolean(false)
    private var lastCaptureAtMillis = Long.MIN_VALUE
    private var lastOcrAtMillis = Long.MIN_VALUE
    private var lastVisualSignature: Long? = null
    private var unchangedOcrIntervalMillis = ACTIVE_UNCHANGED_OCR_INTERVAL_MS

    suspend fun captureAndExtract(
        targetBounds: Rect,
        excludedScreenBounds: List<Rect>
    ): NinetyNineCaptureResult {
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

        lastCaptureAtMillis = now
        return try {
            val bitmap = captureSource.capture(targetBounds)
                ?: return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.CAPTURE_FAILED)
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
                val frame = ocrEngine.recognize(
                    bitmap = bitmap,
                    cropOriginX = targetBounds.left,
                    cropOriginY = targetBounds.top,
                    excludedScreenBounds = excludedScreenBounds
                ) ?: return NinetyNineCaptureResult.Skipped(NinetyNineCaptureSkipReason.OCR_FAILED)
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
                bitmap.recycle()
            }
        } finally {
            busy.set(false)
        }
    }

    override fun close() {
        ocrEngine.close()
    }

    private fun cooldownMillis(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MODERN_COOLDOWN_MS
        } else {
            LEGACY_COOLDOWN_MS
        }
    }

    private companion object {
        const val MODERN_COOLDOWN_MS = 500L
        const val LEGACY_COOLDOWN_MS = 1_000L
        const val ACTIVE_UNCHANGED_OCR_INTERVAL_MS = 3_000L
        const val IDLE_UNCHANGED_OCR_INTERVAL_MS = 6_000L
        const val MIN_TARGET_HEIGHT_PX = 120
    }
}

enum class NinetyNineCaptureSkipReason {
    INVALID_TARGET,
    COOLDOWN,
    BUSY,
    UNCHANGED_FRAME,
    CAPTURE_FAILED,
    OCR_FAILED
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
