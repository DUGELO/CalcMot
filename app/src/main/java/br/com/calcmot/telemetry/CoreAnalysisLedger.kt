package br.com.calcmot.telemetry

/** One app-scoped owner, used only by the background recorder. No offer content is stored. */
internal class CoreAnalysisLedger(private val store: CoreAnalysisStore) {
    private var loaded: CoreAnalysisState? = null

    @Synchronized
    fun record(receipt: CoreAnalysisReceipt, kind: CoreAnalysisKind): List<String> {
        if (kind != CoreAnalysisKind.REAL || !receipt.isValid()) return emptyList()
        val state = state()
        val owner = canonical(receipt.identityKey, state.aliases)
        if (state.receipts.any { it.identityKey == owner && it.analysisId == receipt.analysisId }) {
            return emptyList() // Preserve the original date, including on timezone/clock replay.
        }
        val normalized = receipt.copy(identityKey = owner)
        val first = state.firstValues.none { it.identityKey == owner }
        val activeDay = state.days.none { it.identityKey == owner && it.localDate == receipt.localDate }
        val earlier = state.firstValues.firstOrNull { it.identityKey == owner }
            ?.takeIf { receipt.occurredAt < it.occurredAt }
        val next = state.copy(
            receipts = state.receipts + normalized,
            days = if (activeDay) state.days + CoreActiveDay(owner, receipt.localDate) else state.days,
            firstValues = when {
                first -> state.firstValues + normalized
                earlier != null -> state.firstValues.filterNot { it.identityKey == owner } + normalized
                else -> state.firstValues
            }
        )
        commit(next)
        // Marked persistently BEFORE dispatch: at-most-once SDK attempt, not exactly-once delivery.
        return buildList {
            add(AnalyticsEvents.CORE_ANALYSIS_COMPLETED)
            if (first) add(AnalyticsEvents.FIRST_CORE_VALUE)
            if (activeDay) add(AnalyticsEvents.CORE_ACTIVE_DAY)
        }
    }

    /** Only an explicit, successful S04 association may call this; never derive it from email. */
    @Synchronized
    fun consolidateVerifiedLink(fromIdentity: String, toIdentity: String) {
        require(fromIdentity.isNotBlank() && toIdentity.isNotBlank())
        require(fromIdentity.length <= 256 && toIdentity.length <= 256)
        val state = state()
        val from = canonical(fromIdentity, state.aliases)
        val to = canonical(toIdentity, state.aliases)
        if (from == to) return
        val aliases = state.aliases + (from to to)
        fun owner(key: String) = canonical(key, aliases)
        // Sort original instants before union; never recalculate dates in the current timezone.
        val receipts = state.receipts.sortedBy { it.occurredAt }
            .map { it.copy(identityKey = owner(it.identityKey)) }
            .distinctBy { it.identityKey to it.analysisId }
        val days = state.days.map { it.copy(identityKey = owner(it.identityKey)) }.distinct()
        val first = state.firstValues.sortedBy { it.occurredAt }
            .map { it.copy(identityKey = owner(it.identityKey)) }.distinctBy { it.identityKey }
        commit(state.copy(receipts = receipts, days = days, firstValues = first, aliases = aliases))
        // A link creates no usage event. Previously delivered SDK events cannot be retracted here.
    }

    @Synchronized
    fun snapshot(): CoreAnalysisState = state().let {
        it.copy(receipts = it.receipts.toList(), days = it.days.toList(),
            firstValues = it.firstValues.toList(), aliases = it.aliases.toMap())
    }

    private fun state(): CoreAnalysisState = loaded ?: store.read().also {
        it.validate()
        loaded = it
    }

    private fun commit(next: CoreAnalysisState) {
        next.validate()
        store.write(next) // Failed/corrupt persistence never silently resets deduplication.
        loaded = next
    }

    private fun canonical(identity: String, aliases: Map<String, String>): String {
        var current = identity
        val visited = mutableSetOf<String>()
        while (current in aliases) {
            check(visited.add(current)) { "Invalid local identity association" }
            current = aliases.getValue(current)
        }
        return current
    }
}

internal interface CoreAnalysisStore {
    fun read(): CoreAnalysisState
    fun write(state: CoreAnalysisState)
}

internal data class CoreActiveDay(val identityKey: String, val localDate: String)

internal data class CoreAnalysisState(
    val version: Int = 1,
    val receipts: List<CoreAnalysisReceipt> = emptyList(),
    val days: List<CoreActiveDay> = emptyList(),
    val firstValues: List<CoreAnalysisReceipt> = emptyList(),
    val aliases: Map<String, String> = emptyMap()
) {
    fun validate() {
        require(version == 1)
        // Technical bounds, not commercial retention. Exhaustion stops metrics, never the Core.
        require(receipts.size <= MAX_ENTRIES && days.size <= MAX_ENTRIES)
        require(firstValues.size <= MAX_IDENTITIES && aliases.size <= MAX_IDENTITIES)
        require(receipts.all { it.isValid() } && firstValues.all { it.isValid() })
        require(receipts.distinctBy { it.identityKey to it.analysisId }.size == receipts.size)
        require(days.distinct().size == days.size)
        require(firstValues.distinctBy { it.identityKey }.size == firstValues.size)
        val receiptDays = receipts.map { CoreActiveDay(it.identityKey, it.localDate) }.toSet()
        require(days.toSet() == receiptDays)
        require(firstValues.map { it.identityKey }.toSet() == receipts.map { it.identityKey }.toSet())
        require(firstValues.all { it in receipts })
        aliases.forEach { (start, target) ->
            require(start.isNotBlank() && target.isNotBlank() && start.length <= 256 && target.length <= 256)
            val visited = mutableSetOf<String>()
            var current = start
            while (current in aliases) {
                require(visited.add(current))
                current = aliases.getValue(current)
            }
        }
    }

    companion object {
        const val MAX_ENTRIES = 1024
        const val MAX_IDENTITIES = 64
    }
}
