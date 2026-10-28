package br.com.calcmot.telemetry

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Invoke write, force-stop ONLY identitydev, then invoke read with the same synthetic run ID. */
class CoreAnalysisRestartDeviceTest {
    @Test fun receiptSurvivesProcessRestart() {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("s10Phase")
        assumeTrue("Requires the documented two-process harness", phase == "write" || phase == "read")
        val runId = requireNotNull(args.getString("s10RunId"))
        require(runId.matches(Regex("[a-f0-9-]{36}")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "br.com.calcmot.identitydev")
        val directory = File(context.noBackupFilesDir, "s10-restart-$runId")
        val original = CoreAnalysisReceipt.atSuccess("synthetic-original", "synthetic-local-owner",
            1_791_547_200_000L, "America/Sao_Paulo", "99", "unknown")
        if (phase == "write") {
            assertFalse(directory.exists())
            val ledger = CoreAnalysisLedger(AndroidCoreAnalysisStore(directory))
            assertEquals(3, ledger.record(original, CoreAnalysisKind.REAL).size)
            assertEquals(original, ledger.snapshot().receipts.single())
        } else {
            try {
                assertTrue(directory.isDirectory)
                val ledger = CoreAnalysisLedger(AndroidCoreAnalysisStore(directory))
                assertEquals(original, ledger.snapshot().receipts.single())
                val replay = CoreAnalysisReceipt.atSuccess(original.analysisId, original.identityKey,
                    1_791_547_200_000L + 86_400_000L, "Asia/Tokyo", "99", "unknown")
                assertTrue(ledger.record(replay, CoreAnalysisKind.REAL).isEmpty())
                assertEquals(original.localDate, ledger.snapshot().days.single().localDate)
                assertEquals(1, ledger.snapshot().firstValues.size)
            } finally { directory.deleteRecursively() }
        }
    }
}
