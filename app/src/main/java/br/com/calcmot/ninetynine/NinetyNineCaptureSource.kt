package br.com.calcmot.ninetynine

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi
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
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(
                        screenshot: AccessibilityService.ScreenshotResult
                    ) {
                        val hardwareBuffer = screenshot.hardwareBuffer
                        val bitmap = try {
                            Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                                ?.copy(Bitmap.Config.ARGB_8888, false)
                        } finally {
                            hardwareBuffer.close()
                        }
                        val cropped = bitmap?.cropTo(targetBounds)
                        if (bitmap != null && cropped !== bitmap) bitmap.recycle()
                        if (continuation.isActive) {
                            continuation.resume(cropped)
                        } else {
                            cropped?.recycle()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            )
        }
}

class MediaProjectionCaptureSource : NinetyNineCaptureSource {
    override suspend fun capture(targetBounds: Rect): Bitmap? {
        return NinetyNineProjectionService.capture(targetBounds)
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
