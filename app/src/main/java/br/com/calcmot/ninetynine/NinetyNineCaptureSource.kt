package br.com.calcmot.ninetynine

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface NinetyNineCaptureSource {
    suspend fun capture(targetBounds: Rect): Bitmap?
}

@RequiresApi(Build.VERSION_CODES.R)
class AccessibilityScreenshotCaptureSource(
    private val service: AccessibilityService
) : NinetyNineCaptureSource {

    override suspend fun capture(targetBounds: Rect): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)

            fun complete(bitmap: Bitmap?) {
                if (!completed.compareAndSet(false, true) || !continuation.isActive) {
                    bitmap?.recycle()
                    return
                }
                runCatching { continuation.resume(bitmap) }
                    .onFailure { bitmap?.recycle() }
            }

            try {
                service.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    service.mainExecutor,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(
                            screenshot: AccessibilityService.ScreenshotResult
                        ) {
                            var sourceBitmap: Bitmap? = null
                            try {
                                val hardwareBuffer = screenshot.hardwareBuffer
                                sourceBitmap = try {
                                    Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                                        ?.copy(Bitmap.Config.ARGB_8888, false)
                                } finally {
                                    runCatching { hardwareBuffer.close() }
                                }
                                val cropped = sourceBitmap?.cropTo(targetBounds)
                                if (sourceBitmap != null && cropped !== sourceBitmap) {
                                    sourceBitmap.recycle()
                                }
                                sourceBitmap = null
                                complete(cropped)
                            } catch (error: Exception) {
                                sourceBitmap?.let {
                                    if (!it.isRecycled) it.recycle()
                                }
                                Log.w(TAG, "SCREENSHOT_RESULT_REJECTED type=${error.javaClass.simpleName}")
                                complete(null)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "SCREENSHOT_FAILED code=$errorCode")
                            complete(null)
                        }
                    }
                )
            } catch (error: Exception) {
                Log.w(TAG, "SCREENSHOT_REQUEST_REJECTED type=${error.javaClass.simpleName}")
                complete(null)
            }
        }

    private companion object {
        const val TAG = "CalcMot99Capture"
    }
}

object UnsupportedNinetyNineCaptureSource : NinetyNineCaptureSource {
    override suspend fun capture(targetBounds: Rect): Bitmap? {
        return null
    }
}

private fun Bitmap.cropTo(requestedBounds: Rect): Bitmap? {
    val bounds = Rect(
        requestedBounds.left.coerceIn(0, width),
        requestedBounds.top.coerceIn(0, height),
        requestedBounds.right.coerceIn(0, width),
        requestedBounds.bottom.coerceIn(0, height)
    )
    if (bounds.width() <= 0 || bounds.height() <= 0) return null
    if (bounds.left == 0 && bounds.top == 0 && bounds.right == width && bounds.bottom == height) {
        return this
    }
    return Bitmap.createBitmap(this, bounds.left, bounds.top, bounds.width(), bounds.height())
}
