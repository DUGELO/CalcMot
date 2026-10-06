package br.com.calcmot

import android.app.Application
import br.com.calcmot.finance.ledger.FinanceLedgerProvider
import br.com.calcmot.finance.ledger.LedgerMaintenanceWorker
import br.com.calcmot.telemetry.TelemetryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalcMotApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        TelemetryProvider.initialize(this)
        val repository = FinanceLedgerProvider.repository(this)
        applicationScope.launch {
            runCatching { repository.migrateLegacyEntries() }
            runCatching { repository.runRetention() }
        }
        LedgerMaintenanceWorker.schedule(this)
    }
}
