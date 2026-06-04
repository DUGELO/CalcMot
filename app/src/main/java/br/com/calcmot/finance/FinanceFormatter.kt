package br.com.calcmot.finance

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

object FinanceFormatter {
    private val brazilianLocale = Locale.forLanguageTag("pt-BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(brazilianLocale)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", brazilianLocale)

    fun formatMoney(amountCents: Long): String {
        return currencyFormat.format(amountCents / 100.0)
    }

    fun formatSignedMoney(amountCents: Long): String {
        val prefix = if (amountCents < 0) "-" else ""
        return prefix + formatMoney(amountCents.absoluteValue)
    }

    fun formatDate(dateMillis: Long): String {
        return dateFormat.format(Date(dateMillis))
    }

    fun parseMoneyToCents(rawValue: String): Long? {
        val cleaned = rawValue
            .trim()
            .replace("R$", "")
            .replace(" ", "")
        val normalized = when {
            cleaned.contains(",") -> cleaned.replace(".", "").replace(",", ".")
            cleaned.count { it == '.' } == 1 && cleaned.substringAfter('.').length in 1..2 -> cleaned
            else -> cleaned.replace(".", "")
        }

        if (normalized.isBlank()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        val cents = Math.round(value * 100.0)
        return cents.takeIf { it > 0L }
    }
}
