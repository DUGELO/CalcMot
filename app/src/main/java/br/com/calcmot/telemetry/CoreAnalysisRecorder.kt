package br.com.calcmot.telemetry

import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Non-blocking handoff only. Deliberately not installed in Core until its logical result owner
 * can provide a stable ID (D11), a trustworthy original time and an S04 association (D12).
 */
internal class CoreAnalysisRecorder(
    scope: CoroutineScope,
    private val ledger: CoreAnalysisLedger,
    private val analytics: AnalyticsTracker,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Closeable {
    private sealed interface Command {
        data class Complete(val receipt: CoreAnalysisReceipt, val kind: CoreAnalysisKind) : Command
        data class Link(val from: String, val to: String) : Command
    }

    private val queue = Channel<Command>(capacity = 64)
    private val failures = AtomicInteger()
    val failedOperations: Int get() = failures.get()
    private val worker = scope.launch(dispatcher) {
        for (command in queue) {
            try {
                when (command) {
                    is Command.Complete -> {
                        val events = ledger.record(command.receipt, command.kind)
                        val params = mapOf(AnalyticsParams.APP_FAMILY to command.receipt.appFamily,
                            AnalyticsParams.CLASSIFICATION to command.receipt.classification)
                        for (event in events) {
                            try { analytics.track(event, params) }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { failures.incrementAndGet() }
                        }
                    }
                    is Command.Link -> ledger.consolidateVerifiedLink(command.from, command.to)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { failures.incrementAndGet() } // No payload/error text in logs.
        }
    }

    fun tryRecord(receipt: CoreAnalysisReceipt, kind: CoreAnalysisKind, originalTimeTrusted: Boolean): Boolean {
        if (kind != CoreAnalysisKind.REAL || !originalTimeTrusted) return false
        return queue.trySend(Command.Complete(receipt, kind)).isSuccess
    }

    /** Caller must supply an association confirmed by S04, not a session change or an email. */
    fun tryConsolidateVerifiedLink(from: String, to: String): Boolean =
        queue.trySend(Command.Link(from, to)).isSuccess

    override fun close() {
        queue.close()
        worker.cancel()
    }
}
