package com.example.calcmot.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.example.calcmot.ocr.TextRecognizerHelper
import com.example.calcmot.overlay.IOverlayManager
import com.example.calcmot.overlay.OverlayManager
import com.example.calcmot.processor.TextProcessor
import com.example.calcmot.util.ImageUtils
import kotlinx.coroutines.*

class UberAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    internal var overlayManager: IOverlayManager? = null // Depende da interface
    private var isProcessing = false
    private val debounceDelay = 1000L
    private var lastProcessedTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (overlayManager == null) {
            overlayManager = OverlayManager(this)
        }
        Log.d("UberReader", "✅ Serviço Conectado")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName != "com.ubercab.driver") return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val currentTime = System.currentTimeMillis()
        if (isProcessing || (currentTime - lastProcessedTime) < debounceDelay) {
            return
        }

        isProcessing = true
        lastProcessedTime = currentTime

        CoroutineScope(Dispatchers.Main).launch {
            overlayManager?.hideOverlay()
            delay(50)

            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                        if (bitmap != null) {
                            processBitmap(bitmap)
                        } else {
                            isProcessing = false
                        }
                        screenshot.hardwareBuffer.close()
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e("UberReader", "Falha Screenshot: $errorCode")
                        isProcessing = false
                    }
                }
            )
        }
    }

    internal fun processBitmap(fullBitmap: Bitmap) {
        serviceScope.launch {
            try {
                val softwareBitmap = fullBitmap.copy(Bitmap.Config.ARGB_8888, false)

                val height = softwareBitmap.height
                val width = softwareBitmap.width
                val cropHeight = (height * 0.45).toInt()
                val startY = height - cropHeight
                val croppedBitmap = Bitmap.createBitmap(softwareBitmap, 0, startY, width, cropHeight)

                val cleanBitmap = ImageUtils.preProcessBitmap(croppedBitmap)

                val text = TextRecognizerHelper.extractTextFromImage(cleanBitmap)

                if (!text.isNullOrBlank()) {
                    val tripData = TextProcessor.processText(text)

                    withContext(Dispatchers.Main) {
                        if (tripData != null) {
                            overlayManager?.showOverlay(tripData)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UberReader", "Erro processamento: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.removeOverlay()
        serviceScope.cancel()
    }

    override fun onInterrupt() {}
}