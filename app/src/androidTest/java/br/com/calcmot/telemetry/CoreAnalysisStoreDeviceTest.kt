package br.com.calcmot.telemetry

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

/** Synthetic, local, isolated-package only. No authentication, Analytics calls or driver-app input. */
class CoreAnalysisStoreDeviceTest {
    @Test fun atomicFilePersistsAcrossOwnersAndRejectsCorruption() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "br.com.calcmot.identitydev")
        val directory = File(context.noBackupFilesDir, "s10-test-${UUID.randomUUID()}")
        try {
            val receipt = CoreAnalysisReceipt.atSuccess("synthetic-result", "synthetic-owner",
                1_791_547_200_000L, "America/Sao_Paulo", "uber", "unknown")
            val ledger = CoreAnalysisLedger(AndroidCoreAnalysisStore(directory))
            assertEquals(3, ledger.record(receipt, CoreAnalysisKind.REAL).size)
            val restarted = CoreAnalysisLedger(AndroidCoreAnalysisStore(directory))
            assertTrue(restarted.record(receipt, CoreAnalysisKind.REAL).isEmpty())
            assertEquals(receipt, restarted.snapshot().receipts.single())
            assertEquals(3, restarted.record(receipt.copy(identityKey = "different-owner"), CoreAnalysisKind.REAL).size)
            val file = File(directory, "core-analysis-v1.json")
            val validJson = file.readText()
            file.writeText("broken synthetic record")
            assertThrows(Exception::class.java) { CoreAnalysisLedger(AndroidCoreAnalysisStore(directory)).snapshot() }
            // Restore only the test's own fixture and verify original dedupe remains intact.
            file.writeText(validJson)
            assertTrue(CoreAnalysisLedger(AndroidCoreAnalysisStore(directory))
                .record(receipt, CoreAnalysisKind.REAL).isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }
}
