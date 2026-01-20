package com.example.calcmot.processor

import android.util.Log
import com.example.calcmot.model.TripData
import java.util.regex.Pattern
import kotlin.math.roundToInt

object TextProcessor {

    private const val TAG = "UberReader"

    fun processText(rawText: String): TripData? {
        try {
            // 1. SANITIZAÇÃO
            val text = rawText.uppercase()
                .replace("\n", " ")
                .replace("R$", " R$ ")
                .replace(",", ".")
                .replace(Regex("\\s+"), " ")
                .trim()

            // 2. EXTRAÇÃO DE VALOR
            val valorPattern = Pattern.compile("""R\$\s?(\d+(?:[.]\d{1,2})?)""")
            val valorMatcher = valorPattern.matcher(text)
            var valor = 0.0
            while (valorMatcher.find()) {
                valor = maxOf(valor, valorMatcher.group(1)?.toDoubleOrNull() ?: 0.0)
            }

            // 3. EXTRAÇÃO DE NOTA
            val notaPattern = Pattern.compile("""(?<!R\$\s?)(?<!\d)([345][.]\d{2})(?!\d)""")
            val notaMatcher = notaPattern.matcher(text)
            var nota = 0.0
            if (notaMatcher.find()) {
                nota = notaMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            }

            // 4. EXTRAÇÃO DE TEMPO E DISTÂNCIA
            var somaMinutos = 0.0
            var somaKm = 0.0
            val pattern = Pattern.compile("""(\d+)\s*MINUTOS?\s*\(([\d.]+)\s*KM\)""")
            val matcher = pattern.matcher(text)
            var matchesFound = 0
            while (matcher.find()) {
                somaMinutos += matcher.group(1)?.toDoubleOrNull() ?: 0.0
                somaKm += matcher.group(2)?.toDoubleOrNull() ?: 0.0
                matchesFound++
            }

            // 5. VALIDAÇÃO RIGOROSA (Conforme sua regra final)
            val isDataComplete = valor > 0.0 &&
                                 nota > 0.0 &&
                                 somaKm > 0.0 &&
                                 somaMinutos > 0.0 &&
                                 matchesFound >= 1 // Garante pelo menos UMA leitura completa de tempo/distância

            if (isDataComplete) {
                return TripData(
                    valor = valor,
                    distanciaKm = somaKm,
                    minutosTotais = somaMinutos.roundToInt(),
                    valorPorKm = valor / somaKm,
                    valorPorHora = if (somaMinutos > 0) valor / (somaMinutos / 60.0) else 0.0,
                    nota = nota
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no processamento de texto: ${e.message}")
        }

        // Se a validação falhar, não retorna nada.
        return null
    }
}