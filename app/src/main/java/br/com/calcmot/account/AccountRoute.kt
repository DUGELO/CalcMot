package br.com.calcmot.account

import android.app.Application
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.calcmot.BuildConfig
import br.com.calcmot.entitlement.AndroidEntitlements
import br.com.calcmot.identity.GoogleIdentityClient
import br.com.calcmot.ui.CALCMOT_SUPPORT_EMAIL
import br.com.calcmot.ui.theme.findActivity
import kotlinx.coroutines.launch

/** Created only by voluntary account navigation, never by Application or offer services. */
@Composable internal fun AccountRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var supportUnavailable by remember { mutableStateOf(false) }
    val openSupport: () -> Unit = {
        try { uri.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
        catch (_: Exception) { supportUnavailable = true }
    }
    if (supportUnavailable) AlertDialog(onDismissRequest = { supportUnavailable = false },
        title = { Text("Contato de suporte") },
        text = { SelectionContainer { Text("Não foi possível abrir um aplicativo de email. Você pode entrar em contato por $CALCMOT_SUPPORT_EMAIL.") } },
        confirmButton = { TextButton(onClick = { supportUnavailable = false }) { Text("Entendi") } })
    if (activity == null) { Text("Abra a conta no aplicativo CalcMot."); return }
    val client = remember(activity) { runCatching { GoogleIdentityClient.create(activity) }.getOrNull() }
    if (client == null) {
        AccountScreen(AccountState(notice = AccountNotice.CONFIGURATION, authAvailable = false), onBack, {}, {}, {}, {}, openSupport)
        return
    }
    val application = context.applicationContext as Application
    val factory = remember(client) { object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == AccountViewModel::class.java)
            val access = if (BuildConfig.ACCOUNT_API_BASE_URL.isBlank()) null else
                AndroidEntitlements.createDefault(application, BuildConfig.ACCOUNT_API_BASE_URL)
            return AccountViewModel(client.identity, { client.signOut() }, access?.repository,
                createLegacyRequests(application, client.identity), close = { access?.close() }) as T
        }
    } }
    val model: AccountViewModel = viewModel(factory = factory)
    LaunchedEffect(model) { model.open() }
    val state by model.state.collectAsStateWithLifecycle()
    AccountScreen(state, onBack,
        { scope.launch { model.login { client.signIn(activity) } } }, { scope.launch { model.refresh() } },
        { scope.launch { model.requestRecognition() } }, { scope.launch { model.logout() } },
        openSupport)
}
