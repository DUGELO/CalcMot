package br.com.calcmot.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import br.com.calcmot.BuildConfig
import br.com.calcmot.identity.*
import br.com.calcmot.ui.theme.MetricaTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

/** Development source set only. No token in intents, UI, clipboard, disk or logs. */
class IdentityProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(BuildConfig.DEBUG && packageName == "br.com.calcmot.identitydev")
        val client = GoogleIdentityClient.create(this)
        setContent {
            MetricaTheme {
                val scope = rememberCoroutineScope()
                val session by client.identity.session.collectAsState()
                var status by remember { mutableStateOf("Pronto. Login opcional; nenhum benefício é concedido.") }
                var busy by remember { mutableStateOf(false) }
                fun runOperation(operation: suspend () -> String) {
                    busy = true
                    scope.launch {
                        try { status = operation() }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (error: IdentityOperationException) { status = error.reason.name }
                        catch (_: Exception) { status = "UNAVAILABLE" }
                        finally { busy = false; client.identity.refreshSession() }
                    }
                }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("S02/S04 — teste DEV", style = MaterialTheme.typography.headlineSmall)
                        Text("Pacote isolado. Não é a tela de conta do produto.")
                        Text(if (session == null) "Sem sessão" else
                            "Sessão: ${if (session!!.identityLinked) "vinculada" else "não vinculada"}; prova local ${proof(session!!.uid)}")
                        Text(status)
                        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            runOperation {
                                when (val result = client.signIn(this@IdentityProbeActivity)) {
                                    is IdentityResult.Success -> "LOGIN_OK"
                                    is IdentityResult.Failure -> result.reason.name
                                }
                            }
                        }) { Text("Entrar / reautenticar Google") }
                        OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            runOperation { client.identity.firebaseIdToken(true); "REFRESH_OK (token não exibido)" }
                        }) { Text("Renovar sessão") }
                        Text("Worker verificado no build: ${BuildConfig.IDENTITY_DEV_WORKER_URL.ifBlank { "não configurado" }}")
                        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            runOperation {
                                val endpoint = devEndpoint(BuildConfig.IDENTITY_DEV_WORKER_URL)
                                    ?: return@runOperation "CONFIGURATION: apenas Worker DEV autorizado"
                                val firebaseToken = client.identity.firebaseIdToken()
                                withContext(Dispatchers.IO) { query(endpoint, firebaseToken) }
                            }
                        }) { Text("Consultar entitlement próprio") }
                        OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            runOperation {
                                FirebaseAuth.getInstance().signOut()
                                CredentialManager.create(applicationContext)
                                    .clearCredentialState(ClearCredentialStateRequest())
                                "SIGNED_OUT_DEV_ONLY"
                            }
                        }) { Text("Sair somente da sessão DEV") }
                        Text("Cancelamento: abra Google e feche a seleção. Colisão: com sessão A, selecione conta B; deve recusar a troca. Compare a prova local antes/depois. Reinstalação deve remover somente este pacote DEV.")
                    }
                }
            }
        }
    }

    private fun devEndpoint(value: String): URL? = runCatching {
        val root = URL(value.trim())
        require(root.protocol == "https" && root.port == -1 && root.userInfo == null &&
            root.host.matches(Regex("calcmot-api-dev\\.[a-z0-9-]+\\.workers\\.dev")) &&
            root.path in listOf("", "/") && root.query == null && root.ref == null)
        URL(root, "/v1/me/entitlement")
    }.getOrNull()

    private fun query(url: URL, token: String): String {
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000; connection.readTimeout = 10000
            connection.setRequestProperty("Authorization", "Bearer $token")
            val http = connection.responseCode
            val stream = if (http < 400) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { reader ->
                val buffer = CharArray(8192)
                val count = reader.read(buffer)
                if (count < 0) "{}" else String(buffer, 0, count)
            } ?: "{}"
            val json = runCatching {JSONObject(body)}.getOrNull()
            val code = json?.optString("code")?.takeIf {it in setOf("AUTH_REQUIRED", "RETRY_LATER", "FORBIDDEN", "NOT_FOUND")}
            val requestId = connection.getHeaderField("X-Request-ID")
                ?.takeIf {it.matches(Regex("[a-fA-F0-9-]{36}"))} ?: "indisponível"
            return "HTTP $http ${code ?: ""}; requestId $requestId. Sem snapshot, 503 é esperado; confirme auth/D1 pela evidência DEV."
        } finally { connection.disconnect() }
    }

    private fun proof(uid: String): String = MessageDigest.getInstance("SHA-256")
        .digest(uid.toByteArray()).take(8).joinToString("") {"%02x".format(it.toInt() and 255)}
}

