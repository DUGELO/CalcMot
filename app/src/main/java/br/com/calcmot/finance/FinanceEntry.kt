package br.com.calcmot.finance

data class FinanceEntry(
    val id: String,
    val type: FinanceEntryType,
    val amountCents: Long,
    val description: String,
    val dateMillis: Long
)

enum class FinanceEntryType(val label: String) {
    EARNING("Ganho"),
    COST("Custo")
}

data class FinanceSummary(
    val earningsCents: Long,
    val costsCents: Long,
    val netCents: Long,
    val entryCount: Int
)

fun List<FinanceEntry>.toFinanceSummary(): FinanceSummary {
    val earnings = filter { it.type == FinanceEntryType.EARNING }.sumOf { it.amountCents }
    val costs = filter { it.type == FinanceEntryType.COST }.sumOf { it.amountCents }
    return FinanceSummary(
        earningsCents = earnings,
        costsCents = costs,
        netCents = earnings - costs,
        entryCount = size
    )
}
