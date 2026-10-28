package br.com.calcmot.telemetry

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoreAnalysisRecorderTest {
    private class Store : CoreAnalysisStore {
        var state = CoreAnalysisState()
        var reads = 0
        var fail = false
        override fun read(): CoreAnalysisState { reads++; return state }
        override fun write(state: CoreAnalysisState) {
            if (fail) throw IOException("synthetic persistence failure")
            this.state = state
        }
    }
    private fun receipt(id: String = "opaque") = CoreAnalysisReceipt.atSuccess(
        id, "private-local-owner", 1_791_547_200_000L, "America/Sao_Paulo", "99", "good")

    @Test fun `enqueue never reads disk or invokes analytics on the caller`() = runTest {
        val store = Store()
        val events = mutableListOf<String>()
        val recorder = CoreAnalysisRecorder(this, CoreAnalysisLedger(store), object : AnalyticsTracker {
            override fun track(event: String, params: Map<String, String>) { events += event }
        }, StandardTestDispatcher(testScheduler))
        repeat(10) { assertTrue(recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, true)) }
        assertEquals(0, store.reads)
        assertTrue(events.isEmpty())
        runCurrent()
        assertEquals(3, events.size)
        assertEquals(1, store.state.receipts.size)
        recorder.close()
    }

    @Test fun `analytics failure cannot escape the handoff or reset persistent deduplication`() = runTest {
        val store = Store()
        val recorder = CoreAnalysisRecorder(this, CoreAnalysisLedger(store), object : AnalyticsTracker {
            override fun track(event: String, params: Map<String, String>) { throw IOException("SDK unavailable") }
        }, StandardTestDispatcher(testScheduler))
        assertTrue(recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, true))
        runCurrent()
        assertEquals(3, recorder.failedOperations)
        assertTrue(recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, true))
        runCurrent()
        assertEquals(3, recorder.failedOperations)
        assertEquals(1, store.state.receipts.size)
        recorder.close()
    }

    @Test fun `failed storage sends nothing and leaves the consumer alive for retry`() = runTest {
        val store = Store().apply { fail = true }
        val events = mutableListOf<String>()
        val recorder = CoreAnalysisRecorder(this, CoreAnalysisLedger(store), object : AnalyticsTracker {
            override fun track(event: String, params: Map<String, String>) { events += event }
        }, StandardTestDispatcher(testScheduler))
        recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, true)
        runCurrent()
        assertTrue(events.isEmpty())
        assertEquals(1, recorder.failedOperations)
        store.fail = false
        recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, true)
        runCurrent()
        assertEquals(3, events.size)
        recorder.close()
    }

    @Test fun `demo unknown clock queue pressure and shutdown fail without blocking`() = runTest {
        val store = Store()
        val recorder = CoreAnalysisRecorder(this, CoreAnalysisLedger(store), NoOpAnalyticsTracker,
            StandardTestDispatcher(testScheduler))
        assertFalse(recorder.tryRecord(receipt(), CoreAnalysisKind.DEMO, true))
        assertFalse(recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, false))
        repeat(64) { assertTrue(recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, true)) }
        assertFalse(recorder.tryRecord(receipt("queue-full"), CoreAnalysisKind.REAL, true))
        assertEquals(0, store.reads)
        runCurrent()
        assertEquals(1, store.state.receipts.size)
        recorder.close()
        assertFalse(recorder.tryRecord(receipt("closed"), CoreAnalysisKind.REAL, true))
    }

    @Test fun `all three canonical SDK payloads contain only family and classification`() = runTest {
        val payloads = mutableListOf<Map<String, String>>()
        val recorder = CoreAnalysisRecorder(this, CoreAnalysisLedger(Store()), object : AnalyticsTracker {
            override fun track(event: String, params: Map<String, String>) { payloads += params }
        }, StandardTestDispatcher(testScheduler))
        recorder.tryRecord(receipt(), CoreAnalysisKind.REAL, true)
        runCurrent()
        assertEquals(3, payloads.size)
        assertTrue(payloads.all { it == mapOf("app_family" to "99", "classification" to "good") })
        recorder.close()
    }

    @Test fun `canonical sanitization strips even diagnostic allowlisted data and SDK first open is not manual`() {
        val input = mapOf("app_family" to "uber", "classification" to "good", "uid" to "private",
            "analysisId" to "private", "dedupeKey" to "private", "ocr" to "private",
            "price_bucket" to "3_to_7", "device_model" to "Example", "source" to "home",
            "app_version" to "3.5", "address" to "private", "distance" to "private")
        AnalyticsEvents.coreEvents.forEach {
            assertEquals(mapOf("app_family" to "uber", "classification" to "good"),
                SafeTelemetryPolicy.sanitizeEvent(it, input)!!.params)
        }
        assertNull(SafeTelemetryPolicy.sanitizeEvent("first_open", emptyMap()))
    }
}
