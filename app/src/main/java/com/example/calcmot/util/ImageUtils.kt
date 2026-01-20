package com.example.calcmot.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix

object ImageUtils {

    /**
     * Aumenta a escala e binariza a imagem (Preto e Branco)
     * Isso ajuda o OCR a não confundir números com ruas do mapa.
     */
    fun preProcessBitmap(original: Bitmap): Bitmap {
        // 1. Escala (Upscale 2x)
        val matrix = Matrix()
        matrix.postScale(2f, 2f)
        val scaledBitmap = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

        val width = scaledBitmap.width
        val height = scaledBitmap.height
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Limiar de corte (Threshold)
        val threshold = 150

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            // Luminância
            val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()

            // Binarização: Se escuro -> PRETO, Se claro -> BRANCO
            pixels[i] = if (gray < threshold) Color.BLACK else Color.WHITE
        }

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return newBitmap
    }
}