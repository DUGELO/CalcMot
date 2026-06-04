package br.com.calcmot.finance

import android.content.Context
import java.util.UUID

class FinanceRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEntries(): List<FinanceEntry> {
        return FinanceEntryJson.decode(prefs.getString(KEY_ENTRIES, null))
            .sortedByDescending { it.dateMillis }
    }

    fun addEntry(
        type: FinanceEntryType,
        amountCents: Long,
        description: String,
        dateMillis: Long = System.currentTimeMillis()
    ): List<FinanceEntry> {
        val entry = FinanceEntry(
            id = UUID.randomUUID().toString(),
            type = type,
            amountCents = amountCents,
            description = description.trim(),
            dateMillis = dateMillis
        )
        val updated = (getEntries() + entry).sortedByDescending { it.dateMillis }
        save(updated)
        return updated
    }

    fun deleteEntry(id: String): List<FinanceEntry> {
        val updated = getEntries().filterNot { it.id == id }
        save(updated)
        return updated
    }

    private fun save(entries: List<FinanceEntry>) {
        prefs.edit()
            .putString(KEY_ENTRIES, FinanceEntryJson.encode(entries))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "calcmot_finance"
        const val KEY_ENTRIES = "entries_json"
    }
}

object FinanceEntryJson {
    fun encode(entries: List<FinanceEntry>): String {
        return entries.joinToString(prefix = "[", postfix = "]") { entry ->
            "{" +
                "\"id\":\"${entry.id.escapeJson()}\"," +
                "\"type\":\"${entry.type.name}\"," +
                "\"amountCents\":${entry.amountCents}," +
                "\"description\":\"${entry.description.escapeJson()}\"," +
                "\"dateMillis\":${entry.dateMillis}" +
                "}"
        }
    }

    fun decode(rawJson: String?): List<FinanceEntry> {
        if (rawJson.isNullOrBlank()) return emptyList()

        return runCatching {
            if (!rawJson.trim().startsWith("[")) return@runCatching emptyList()
            objectRegex.findAll(rawJson).mapNotNull { match ->
                val objectBody = match.groupValues[1]
                val id = objectBody.stringField("id").trim()
                val type = objectBody.stringField("type")
                    .let { runCatching { FinanceEntryType.valueOf(it) }.getOrNull() }
                val amountCents = objectBody.longField("amountCents")
                val dateMillis = objectBody.longField("dateMillis")

                if (id.isBlank() || type == null || amountCents <= 0L) {
                    null
                } else {
                    FinanceEntry(
                        id = id,
                        type = type,
                        amountCents = amountCents,
                        description = objectBody.stringField("description").trim(),
                        dateMillis = dateMillis
                    )
                }
            }
                .toList()
        }.getOrDefault(emptyList())
    }

    private fun String.stringField(name: String): String {
        val regex = Regex(""""$name"\s*:\s*"((?:\\.|[^"])*)"""")
        return regex.find(this)?.groupValues?.get(1)?.unescapeJson().orEmpty()
    }

    private fun String.longField(name: String): Long {
        val regex = Regex(""""$name"\s*:\s*(-?\d+)""")
        return regex.find(this)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private fun String.escapeJson(): String {
        return buildString(length) {
            this@escapeJson.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

    private fun String.unescapeJson(): String {
        return replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private val objectRegex = Regex("""\{([^{}]*)\}""")
}
