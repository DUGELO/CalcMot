package com.example.calcmot.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object TextRecognizerHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromImage(bitmap: Bitmap): String? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val extractedText = result.text
            Log.d("OCR", "Texto Bruto: $extractedText") // Descomente para debug
            extractedText
        } catch (e: Exception) {
            Log.e("OCR", "Erro no ML Kit", e)
            null
        }
    }
}