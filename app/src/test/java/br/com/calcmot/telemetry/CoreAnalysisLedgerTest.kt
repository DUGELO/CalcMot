package br.com.calcmot.telemetry

import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class CoreAnalysisLedgerTest {
    private class Store : CoreAnalysisStore {
        var encoded = CoreAnalysisCodec.encode(CoreAnalysisState())
        var failWrite = false
        override fun read() = CoreAnalysisCodec.decode(encoded)
        override fun write(state: CoreAnalysisState) {
            if (failWrite) throw IOException("synthetic")
            encoded = CoreAnalysisCodec.encode(state)
        }
    }

    private fun receipt(id: String = "a", owner: String = "installation", day: String = "2026-10-09",
        family: String = "uber", zone: String = "UTC"): CoreAnalysisReceipt {
        val millis = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse("$day 12:00")!!.time
        return CoreAnalysisReceipt.atSuccess(id, owner, millis, zone, family, "unknown")
    }

    @Test fun `ten receipts retries and UI refreshes with one logical ID count once for both platforms`() {
        for (family in listOf("uber", "99")) {
            val ledger = CoreAnalysisLedger(Store())
            val events = (1..10).flatMap { ledger.record(receipt(family = family), CoreAnalysisKind.REAL) }
            assertEquals(listOf(AnalyticsEvents.CORE_ANALYSIS_COMPLETED, AnalyticsEvents.FIRST_CORE_VALUE,
                AnalyticsEvents.CORE_ACTIVE_DAY), events)
            assertEquals(1, ledger.snapshot().receipts.size)
        }
    }

    @Test fun `different logical IDs do not collapse even though no financial fields distinguish them`() {
        val ledger = CoreAnalysisLedger(Store())
        val events = listOf("a", "b").flatMap { ledger.record(receipt(it), CoreAnalysisKind.REAL) }
        assertEquals(2, events.count { it == AnalyticsEvents.CORE_ANALYSIS_COMPLETED })
        assertEquals(1, events.count { it == AnalyticsEvents.CORE_ACTIVE_DAY })
        assertEquals(1, events.count { it == AnalyticsEvents.FIRST_CORE_VALUE })
    }

    @Test fun `new dates add days but replay cannot rewrite the original date or timezone`() {
        val ledger = CoreAnalysisLedger(Store())
        val original = receipt()
        ledger.record(original, CoreAnalysisKind.REAL)
        assertTrue(ledger.record(receipt(day = "2026-10-10", zone = "Asia/Tokyo"), CoreAnalysisKind.REAL).isEmpty())
        ledger.record(receipt("b", day = "2026-10-10"), CoreAnalysisKind.REAL)
        assertEquals(original, ledger.snapshot().receipts.first())
        assertEquals(2, ledger.snapshot().days.size)
    }

    @Test fun `replay after process restart uses persisted JSON not a memory singleton`() {
        val directory = Files.createTempDirectory("core-ledger-test").toFile()
        try {
            val file = File(directory, "synthetic.json")
            fun store() = object : CoreAnalysisStore {
                override fun read() = if (file.exists()) CoreAnalysisCodec.decode(file.readText()) else CoreAnalysisState()
                override fun write(state: CoreAnalysisState) = file.writeText(CoreAnalysisCodec.encode(state))
            }
            CoreAnalysisLedger(store()).record(receipt(), CoreAnalysisKind.REAL)
            val restarted = CoreAnalysisLedger(store())
            assertTrue(restarted.record(receipt(day = "2026-10-11"), CoreAnalysisKind.REAL).isEmpty())
            assertEquals("2026-10-09", restarted.snapshot().days.single().localDate)
        } finally { directory.deleteRecursively() }
    }

    @Test fun `demo never writes receipts days or first value`() {
        val ledger = CoreAnalysisLedger(Store())
        assertTrue(ledger.record(receipt(), CoreAnalysisKind.DEMO).isEmpty())
        assertEquals(CoreAnalysisState(), ledger.snapshot())
    }

    @Test fun `late original success preserves the earliest first value without emitting it twice`() {
        val ledger = CoreAnalysisLedger(Store())
        ledger.record(receipt("later", day = "2026-10-10"), CoreAnalysisKind.REAL)
        val events = ledger.record(receipt("earlier", day = "2026-10-08"), CoreAnalysisKind.REAL)
        assertFalse(AnalyticsEvents.FIRST_CORE_VALUE in events)
        assertEquals("earlier", ledger.snapshot().firstValues.single().analysisId)
    }

    @Test fun `technical capacity fails closed without evicting already recorded IDs`() {
        val receipts = (1..CoreAnalysisState.MAX_ENTRIES).map { receipt("id-$it") }
        val store = Store().apply { write(CoreAnalysisState(receipts = receipts,
            days = listOf(CoreActiveDay("installation", "2026-10-09")), firstValues = listOf(receipts.first()))) }
        val ledger = CoreAnalysisLedger(store)
        assertThrows(IllegalArgumentException::class.java) {
            ledger.record(receipt("too-many"), CoreAnalysisKind.REAL)
        }
        assertEquals(CoreAnalysisState.MAX_ENTRIES, ledger.snapshot().receipts.size)
        assertTrue(ledger.record(receipts.first(), CoreAnalysisKind.REAL).isEmpty())
    }

    @Test fun `serialized receipt keys match the frozen CoreAnalysisReceipt schema`() {
        val ledger = CoreAnalysisLedger(Store())
        ledger.record(receipt(), CoreAnalysisKind.REAL)
        val schema = sequenceOf(File("../docs/contracts/entitlement.schema.json"),
            File("docs/contracts/entitlement.schema.json")).first { it.isFile }
        val definition = JsonParser.parseString(schema.readText()).asJsonObject
            .getAsJsonObject("\$defs").getAsJsonObject("CoreAnalysisReceipt")
        val encoded = JsonParser.parseString(CoreAnalysisCodec.encode(ledger.snapshot())).asJsonObject
            .getAsJsonArray("receipts")[0].asJsonObject
        assertFalse(definition["additionalProperties"].asBoolean)
        assertEquals(definition.getAsJsonArray("required").map { it.asString }.toSet(), encoded.keySet())
    }

    @Test fun `explicit association unions overlapping IDs and days and is idempotent`() {
        val store = Store()
        val ledger = CoreAnalysisLedger(store)
        ledger.record(receipt(owner = "anon"), CoreAnalysisKind.REAL)
        ledger.record(receipt(owner = "linked"), CoreAnalysisKind.REAL)
        ledger.consolidateVerifiedLink("anon", "linked")
        ledger.consolidateVerifiedLink("anon", "linked")
        assertEquals(1, ledger.snapshot().receipts.size)
        assertEquals(1, ledger.snapshot().days.size)
        assertEquals(1, ledger.snapshot().firstValues.size)
        assertTrue(CoreAnalysisLedger(store).record(receipt(owner = "anon"), CoreAnalysisKind.REAL).isEmpty())
    }

    @Test fun `association preserves earliest first value and creates no new first event`() {
        val ledger = CoreAnalysisLedger(Store())
        ledger.record(receipt("later", "linked", "2026-10-10"), CoreAnalysisKind.REAL)
        ledger.record(receipt("earlier", "anon", "2026-10-08"), CoreAnalysisKind.REAL)
        ledger.consolidateVerifiedLink("anon", "linked")
        assertEquals("earlier", ledger.snapshot().firstValues.single().analysisId)
        assertEquals(listOf(AnalyticsEvents.CORE_ANALYSIS_COMPLETED),
            ledger.record(receipt("third", "linked", "2026-10-10"), CoreAnalysisKind.REAL))
    }

    @Test fun `different identities stay isolated without an explicit association`() {
        val ledger = CoreAnalysisLedger(Store())
        ledger.record(receipt(owner = "A"), CoreAnalysisKind.REAL)
        ledger.record(receipt(owner = "B"), CoreAnalysisKind.REAL)
        assertEquals(2, ledger.snapshot().receipts.size)
        assertEquals(2, ledger.snapshot().firstValues.size)
    }

    @Test fun `failed atomic persistence does not commit or emit and can be retried`() {
        val store = Store()
        val ledger = CoreAnalysisLedger(store)
        store.failWrite = true
        assertThrows(IOException::class.java) { ledger.record(receipt(), CoreAnalysisKind.REAL) }
        assertEquals(CoreAnalysisState(), ledger.snapshot())
        store.failWrite = false
        assertEquals(3, ledger.record(receipt(), CoreAnalysisKind.REAL).size)
    }

    @Test fun `corrupt or unknown-version persisted data fails closed instead of resetting usage`() {
        for (json in listOf("null", "{}", "not-json", "{\"version\":2}")) {
            val store = Store().apply { encoded = json }
            assertThrows(Exception::class.java) { CoreAnalysisLedger(store).record(receipt(), CoreAnalysisKind.REAL) }
        }
    }

    @Test fun `inconsistent local date unrecognized timezone and unknown family are rejected`() {
        val ledger = CoreAnalysisLedger(Store())
        for (bad in listOf(receipt().copy(localDate = "2026-02-30"), receipt().copy(timeZoneId = "not/a-zone"),
            receipt().copy(appFamily = "unknown"), receipt().copy(occurredAt = "2026-10-09T12:00:00Z"))) {
            assertTrue(ledger.record(bad, CoreAnalysisKind.REAL).isEmpty())
        }
        assertTrue(ledger.snapshot().receipts.isEmpty())
    }

    @Test fun `concurrent attempts are serialized before persisting or returning event intents`() {
        val ledger = CoreAnalysisLedger(Store())
        val counts = java.util.concurrent.atomic.AtomicInteger()
        val threads = (1..10).map { Thread {
            counts.addAndGet(ledger.record(receipt(), CoreAnalysisKind.REAL).size)
        }.apply { start() } }
        threads.forEach { it.join() }
        assertEquals(3, counts.get())
        assertEquals(1, ledger.snapshot().receipts.size)
    }

    @Test fun `G18 and G19 run directly from the frozen S01 fixture file`() {
        val contract = sequenceOf(File("../docs/contracts/test-matrix.md"), File("docs/contracts/test-matrix.md"))
            .first { it.isFile }.readText()
        val json = contract.substringAfter("```json").substringBefore("```")
        val root = JsonParser.parseString(json)
        fun fixtures(element: com.google.gson.JsonElement): Sequence<com.google.gson.JsonObject> = sequence {
            if (element.isJsonArray) element.asJsonArray.forEach { yieldAll(fixtures(it)) }
            else if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has("id")) yield(obj)
                obj.entrySet().forEach { yieldAll(fixtures(it.value)) }
            }
        }
        val selected = fixtures(root).filter { it["id"].asString in listOf("G18", "G19") }.toList()
        assertEquals(2, selected.size)
        for (fixture in selected) {
            val ledger = CoreAnalysisLedger(Store())
            fixture.getAsJsonObject("aliases").entrySet().forEach {
                ledger.consolidateVerifiedLink(it.key, it.value.asString)
            }
            val events = fixture.getAsJsonArray("receipts").flatMap { item ->
                val entry = item.asJsonObject
                ledger.record(receipt(entry["analysisId"].asString, entry["identityKey"].asString,
                    entry["localDate"].asString), CoreAnalysisKind.valueOf(entry["kind"].asString))
            }
            assertEquals(fixture["expectedAnalyses"].asInt, ledger.snapshot().receipts.size)
            assertEquals(fixture["expectedDays"].asInt, ledger.snapshot().days.size)
            assertEquals(fixture["expectedFirstCoreValues"].asInt,
                events.count { it == AnalyticsEvents.FIRST_CORE_VALUE })
        }
    }
}
