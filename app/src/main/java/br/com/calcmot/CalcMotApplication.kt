package br.com.calcmot

import android.app.Application
import br.com.calcmot.telemetry.TelemetryProvider

class CalcMotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TelemetryProvider.initialize(this)
    }
}
