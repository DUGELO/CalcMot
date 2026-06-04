package br.com.calcmot.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceEntryJsonTest {

    @Test
    fun `summary subtracts costs from earnings`() {
        val entries = listOf(
            FinanceEntry("1", FinanceEntryType.EARNING, 10000, "Corridas", 1000),
            FinanceEntry("2", FinanceEntryType.COST, 2500, "Combustivel", 2000)
        )

        val summary = entries.toFinanceSummary()

        assertEquals(10000L, summary.earningsCents)
        assertEquals(2500L, summary.costsCents)
        assertEquals(7500L, summary.netCents)
        assertEquals(2, summary.entryCount)
    }

    @Test
    fun `json round trip preserves entries`() {
        val entries = listOf(
            FinanceEntry("1", FinanceEntryType.EARNING, 12345, "Corrida", 1000),
            FinanceEntry("2", FinanceEntryType.COST, 6789, "Almoco", 2000)
        )

        val decoded = FinanceEntryJson.decode(FinanceEntryJson.encode(entries))

        assertEquals(entries, decoded)
    }

    @Test
    fun `empty and corrupted json return empty list`() {
        assertTrue(FinanceEntryJson.decode(null).isEmpty())
        assertTrue(FinanceEntryJson.decode("").isEmpty())
        assertTrue(FinanceEntryJson.decode("{not-json").isEmpty())
    }

    @Test
    fun `money parser accepts comma and point`() {
        assertEquals(12345L, FinanceFormatter.parseMoneyToCents("123,45"))
        assertEquals(12345L, FinanceFormatter.parseMoneyToCents("123.45"))
        assertEquals(123456L, FinanceFormatter.parseMoneyToCents("1.234,56"))
        assertEquals(null, FinanceFormatter.parseMoneyToCents("0"))
    }
}
