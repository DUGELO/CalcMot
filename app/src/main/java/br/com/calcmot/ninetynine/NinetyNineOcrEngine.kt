package br.com.calcmot.ninetynine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import br.com.calcmot.processor.ScreenBounds
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface NinetyNineOcrEngine : AutoCloseable {
    suspend fun recognize(
        bitmap: Bitmap,
        cropOriginX: Int,
        cropOriginY: Int,
        excludedScreenBounds: List<Rect>
    ): NinetyNineOcrFrame?
}

class MlKitNinetyNineOcrEngine : NinetyNineOcrEngine {
    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(
        bitmap: Bitmap,
        cropOriginX: Int,
        cropOriginY: Int,
        excludedScreenBounds: List<Rect>
    ): NinetyNineOcrFrame? {
        val primaryText = process(bitmap) ?: return null
        val primaryFrame = primaryText.toFrame(
            width = bitmap.width,
            height = bitmap.height,
            cropOriginX = cropOriginX,
            cropOriginY = cropOriginY,
            excludedScreenBounds = excludedScreenBounds
        )
        if (primaryFrame.lines.any { it.text.containsCurrencySignal() }) {
            return primaryFrame
        }

        val invertedBitmap = bitmap.invertedCopy()
        val secondaryFrame = try {
            process(invertedBitmap)?.toFrame(
                width = bitmap.width,
                height = bitmap.height,
                cropOriginX = cropOriginX,
                cropOriginY = cropOriginY,
                excludedScreenBounds = excludedScreenBounds
            )
        } finally {
            invertedBitmap.recycle()
        } ?: return primaryFrame

        return primaryFrame.copy(
            lines = (primaryFrame.lines + secondaryFrame.lines)
                .distinctBy {
                    "${it.text}|${it.bounds.left}|${it.bounds.top}|${it.bounds.right}|${it.bounds.bottom}"
                }
                .sortedWith(compareBy<NinetyNineOcrLine> { it.bounds.top }.thenBy { it.bounds.left })
        )
    }

    private suspend fun process(bitmap: Bitmap): Text? =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { text ->
                if (continuation.isActive) {
                    continuation.resume(text)
                }
            }
            .addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
            .addOnCanceledListener {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    override fun close() {
        recognizer.close()
    }

    private fun Text.toFrame(
        width: Int,
        height: Int,
        cropOriginX: Int,
        cropOriginY: Int,
        excludedScreenBounds: List<Rect>
    ): NinetyNineOcrFrame {
        val topNoiseLimit = (height * TOP_NOISE_RATIO).toInt()
        val lines = textBlocks
            .sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }
            .flatMap(Text.TextBlock::getLines)
            .mapNotNull { line ->
                val bounds = line.boundingBox ?: return@mapNotNull null
                val rawText = line.text.trim()
                if (rawText.isBlank()) return@mapNotNull null
                if (bounds.top < topNoiseLimit && !rawText.contains("R$", ignoreCase = true)) {
                    return@mapNotNull null
                }

                val screenBounds = Rect(
                    bounds.left + cropOriginX,
                    bounds.top + cropOriginY,
                    bounds.right + cropOriginX,
                    bounds.bottom + cropOriginY
                )
                if (excludedScreenBounds.any { Rect.intersects(it, screenBounds) }) {
                    return@mapNotNull null
                }
                NinetyNineOcrLine(
                    text = rawText,
                    bounds = ScreenBounds(
                        left = bounds.left,
                        top = bounds.top,
                        right = bounds.right,
                        bottom = bounds.bottom
                    )
                )
            }
        return NinetyNineOcrFrame(lines = lines, width = width, height = height)
    }

    private companion object {
        const val TOP_NOISE_RATIO = 0.075
    }
}

private fun String.containsCurrencySignal(): Boolean {
    return contains("R$", ignoreCase = true) ||
        contains(Regex("""(?i)\bR[S5]\s*[0-9OQIlLAZG&]"""))
}

private fun Bitmap.invertedCopy(): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val matrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(matrix)
    }
    Canvas(output).drawBitmap(this, 0f, 0f, paint)
    return output
}
