package br.com.calcmot.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.calcmot.account.AccountRoute
import br.com.calcmot.ui.theme.MetricaTheme

/** DEV-only host, internal activity: no Core bootstrap or public production entry. */
class AccountDevActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName == "br.com.calcmot.identitydev")
        setContent { MetricaTheme { AccountRoute(onBack = { finish() }) } }
    }
}
