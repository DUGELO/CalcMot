package com.example.calcmot.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.example.calcmot.ocr.TextRecognizerHelper
import com.example.calcmot.overlay.IOverlayManager
import com.example.calcmot.overlay.OverlayManager
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.*
import kotlin.math.abs

class UberAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    internal var overlayManager: IOverlayManager? = null
    private var isProcessing = false
    private val debounceDelay = 1000L
    private var lastProcessedTime = 0L

    // Estado para FASE 4 - Detecção Semântica
    private var lastStableBoundingBox: Rect? = null
    private var stableFramesCount = 0
    private val STABILITY_THRESHOLD = 2 // N frames consecutivos
    private val BOUNDING_BOX_AREA_TOLERANCE = 0.20 // 20% de tolerância para a área do box
    private val BOUNDING_BOX_IOU_TOLERANCE = 0.8 // 80% de sobreposição

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
        if (isProcessing || (currentTime - lastProcessedTime) < debounceDelay) return

        isProcessing = true
        lastProcessedTime = currentTime

        CoroutineScope(Dispatchers.Main).launch {
            overlayManager?.hideOverlay()
            delay(50)
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
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
            })
        }
    }

    private fun processBitmap(fullBitmap: Bitmap) {
        serviceScope.launch {
            try {
                val softwareBitmap = fullBitmap.copy(Bitmap.Config.ARGB_8888, false)
                val height = softwareBitmap.height
                val width = softwareBitmap.width
                val cropHeight = (height * 0.45).toInt()
                val startY = height - cropHeight
                val croppedBitmap = Bitmap.createBitmap(softwareBitmap, 0, startY, width, cropHeight)

                val visionText = TextRecognizerHelper.extractTextElements(croppedBitmap)
                
                logOcrRawDump(visionText)

                val signatureElements = findSemanticSignature(visionText)

                if (signatureElements.isNotEmpty()) {
                    val unifiedBox = createUnifiedBoundingBox(signatureElements)
                    if (isSpatiallyStable(unifiedBox)) {
                        stableFramesCount++
                        lastStableBoundingBox = unifiedBox
                        Log.i("SemanticDetection", "CARD_SPATIALLY_STABLE. Frame count: $stableFramesCount.")

                        if (stableFramesCount >= STABILITY_THRESHOLD) {
                            Log.w("SemanticDetection", "CARD_STABLE_CONFIRMED. IS_OFFER_CARD: true, BOUNDING_BOX: $lastStableBoundingBox")
                        }
                    } else {
                        Log.d("SemanticDetection", "CARD_UNSTABLE. IS_OFFER_CARD: false. Box mudou de ${lastStableBoundingBox} para $unifiedBox. Resetando.")
                        resetStability()
                    }
                } else {
                    Log.d("SemanticDetection", "CARD_ANOMALY. IS_OFFER_CARD: false. Assinatura semântica não encontrada. Resetando.")
                    resetStability()
                }
            } catch (e: Exception) {
                Log.e("UberReader", "Erro na FASE 4: ${e.message}")
                resetStability()
            } finally {
                isProcessing = false
            }
        }
    }
    
    private fun resetStability() {
        if (lastStableBoundingBox != null) {
            Log.w("SemanticDetection", "Estabilidade resetada. O card não é mais detectado.")
        }
        stableFramesCount = 0
        lastStableBoundingBox = null
    }
    
    private fun logOcrRawDump(visionText: Text) {
        val rawText = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }.joinToString(separator = " | ") { it.text }
        Log.d("OCRDump", "Raw OCR Text: [$rawText]")
    }

    private fun findSemanticSignature(visionText: Text): List<Text.Element> {
        val signatureElements = mutableListOf<Text.Element>()
        var foundCurrency = false
        var foundButton = false
        val kmElements = mutableListOf<Text.Element>()
        val timeElements = mutableListOf<Text.Element>()
        
        val allElements = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }

        for (element in allElements) {
            when {
                element.text.contains("R$") -> {
                    if (!foundCurrency) {
                        signatureElements.add(element)
                        foundCurrency = true
                    }
                }
                (element.text.equals("Aceitar", ignoreCase = true) || element.text.equals("Selecionar", ignoreCase = true)) -> {
                    if (!foundButton) {
                        signatureElements.add(element)
                        foundButton = true
                    }
                }
                element.text.contains("km", ignoreCase = true) -> kmElements.add(element)
                element.text.contains("min", ignoreCase = true) -> timeElements.add(element)
            }
        }
        
        val distinctKm = kmElements.distinctBy { it.boundingBox }
        val distinctTime = timeElements.distinctBy { it.boundingBox }

        val signatureComplete = foundCurrency && foundButton && distinctKm.size >= 2 && distinctTime.size >= 2
        
        val logBuilder = StringBuilder()
        logBuilder.append("Assinatura Semântica: [")
        logBuilder.append("Moeda(R$): $foundCurrency, ")
        logBuilder.append("Botão(Aceitar/Selecionar): $foundButton, ")
        logBuilder.append("KMs: ${distinctKm.size}, ")
        logBuilder.append("MINs: ${distinctTime.size}")
        logBuilder.append("] -> Completa: $signatureComplete")
        Log.v("SemanticDetection_Debug", logBuilder.toString())

        if (signatureComplete) {
            signatureElements.addAll(distinctKm)
            signatureElements.addAll(distinctTime)
            return signatureElements.distinct()
        }

        return emptyList()
    }

    private fun isNear(box1: Rect?, box2: Rect?): Boolean {
        if (box1 == null || box2 == null) return false
        val inflatedBox1 = Rect(box1)
        inflatedBox1.inset(-30, -30)
        return Rect.intersects(inflatedBox1, box2)
    }

    private fun createUnifiedBoundingBox(elements: List<Text.Element>): Rect {
        val unifiedRect = Rect()
        elements.forEach { element ->
            element.boundingBox?.let { unifiedRect.union(it) }
        }
        return unifiedRect
    }

    private fun isSpatiallyStable(newBox: Rect): Boolean {
        val oldBox = lastStableBoundingBox ?: return true

        val oldArea = oldBox.width() * oldBox.height()
        val newArea = newBox.width() * newBox.height()

        if (oldArea == 0 || newArea == 0) return false

        val areaDifference = abs(newArea - oldArea) / oldArea.toFloat()

        val intersection = Rect(oldBox)
        if (!intersection.intersect(newBox)) return false
        val intersectionArea = intersection.width() * intersection.height()
        val unionArea = oldArea + newArea - intersectionArea
        val iou = if (unionArea > 0) intersectionArea / unionArea.toFloat() else 0f

        return areaDifference < BOUNDING_BOX_AREA_TOLERANCE && iou > BOUNDING_BOX_IOU_TOLERANCE
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.removeOverlay()
        serviceScope.cancel()
    }

    override fun onInterrupt() {}
}