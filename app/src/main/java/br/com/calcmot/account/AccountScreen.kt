package br.com.calcmot.account

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.R
import br.com.calcmot.entitlement.*
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing as Spacing
import kotlinx.coroutines.launch
import com.microsoft.clarity.modifiers.clarityMask

internal enum class AccountPage { ACCOUNT, ACCESS, RECOVERY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(state: AccountState, onBack: () -> Unit, onLogin: () -> Unit,
    onRefresh: () -> Unit, onRequest: () -> Unit, onLogout: () -> Unit, onSupport: () -> Unit,
    initialPage: AccountPage = AccountPage.ACCOUNT, testLabel: String? = null,
    signInContent: AccountSignInContent = { enabled, action -> GoogleAccountButton(enabled, action) }) {
    var pages by rememberSaveable(state.sessionGeneration) { mutableStateOf(listOf(initialPage.name)) }
    val page = AccountPage.valueOf(pages.last())
    var confirmLogout by remember(state.sessionGeneration) { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable(state.sessionGeneration) { mutableStateOf(false) }
    val snackbar = remember(state.sessionGeneration) { SnackbarHostState() }
    val scroll = key(state.sessionGeneration, page) { rememberScrollState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val access = accessPresentation(state)
    val claim = if (!state.signedIn) null else state.request ?: state.access.claimId?.let { LegacyRequestView(it, state.access.legacyState) }
    fun open(destination: AccountPage) { pages = pages + destination.name }
    fun back() {
        if (pages.size > 1) pages = pages.dropLast(1)
        else if (page != AccountPage.ACCOUNT) pages = listOf(AccountPage.ACCOUNT.name)
        else onBack()
    }
    BackHandler(page != AccountPage.ACCOUNT) { back() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text(when (page) {
            AccountPage.ACCOUNT -> "Conta"
            AccountPage.ACCESS -> "Meu acesso"
            AccountPage.RECOVERY -> if (claim == null) "Recuperação" else "Sua solicitação"
        }, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(scroll)
            .padding(horizontal = Spacing.ScreenHorizontal).padding(top = if (page == AccountPage.RECOVERY) Spacing.Lg else Spacing.Xl, bottom = Spacing.Xl).testTag("account_screen"),
            verticalArrangement = Arrangement.spacedBy(if (page == AccountPage.RECOVERY) Spacing.Lg else Spacing.Xl)) {
            if (state.busy || state.access.status == EntitlementStatus.REFRESHING)
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = "Consultando sua conta" })
            when (page) {
                AccountPage.ACCOUNT -> {
                    if (!state.signedIn) AccountEntry(state, signInContent, onLogin, onBack, onSupport)
                    else {
                        AccountProfile(state)
                        AccountActions(accountActions(state)) { action -> when (action) {
                            AccountActionId.ACCESS -> open(AccountPage.ACCESS)
                            AccountActionId.SUPPORT -> onSupport()
                            AccountActionId.SIGN_OUT -> confirmLogout = true
                        } }
                        AccountQuietAction("Voltar ao CalcMot", onBack, "account_continue")
                    }
                    // Account authentication and access verification are separate concerns.
                    if (state.notice != AccountNotice.NONE && (!state.signedIn || state.notice != AccountNotice.CONFIGURATION))
                        AccountFeedback(state, includeCoreReminder = false)
                }
                AccountPage.ACCESS -> {
                    AccountAccess(state, signInContent, onLogin, onRefresh, { open(AccountPage.RECOVERY) },
                        onSupport, detailsExpanded, { detailsExpanded = !detailsExpanded })
                    if (state.notice != AccountNotice.NONE) AccountFeedback(state, includeCoreReminder = false)
                }
                AccountPage.RECOVERY -> {
                    if (access.kind == AccessKind.LEGACY) {
                        Text("Acesso gratuito permanente", style = MaterialTheme.typography.headlineSmall)
                        LegacyRightDetails()
                        AccountQuietAction("Ajuda e suporte", onSupport, "account_support")
                    } else if (claim == null) {
                        Text("Recuperar acesso gratuito", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        RecoveryBlock("Quem pode solicitar") {
                            Text("Quem usou o CalcMot até a versão 3.5 pode pedir o reconhecimento do acesso gratuito permanente.", style = MaterialTheme.typography.bodyMedium)
                        }
                        RecoveryBlock("Como funciona") {
                            Text("A análise é individual e vinculada à sua conta Google. O login não comprova uso antigo nem garante o direito.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        RecoveryBlock("Próximo passo") {
                            when {
                                !state.linked -> {
                                    Text("Entre com Google para vincular seu pedido.", style = MaterialTheme.typography.bodyMedium)
                                    signInContent(!state.busy && state.authAvailable, onLogin)
                                }
                                state.recoveryAvailable -> {
                                    Text("O pedido será analisado antes da confirmação do direito.", style = MaterialTheme.typography.bodyMedium)
                                    Button(onClick = onRequest, enabled = !state.busy,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("account_request")) { Text("Solicitar análise") }
                                }
                                else -> {
                                    Text("O envio pelo app ainda não está disponível.", style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.testTag("account_intake_unavailable"))
                                    Button(onClick = onSupport, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("account_support")) { Text("Falar com suporte") }
                                }
                            }
                        }
                    } else {
                        Text(reviewTitle(claim, state), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(when {
                            state.access.legacyCore -> "Seu direito está confirmado. Você pode continuar usando o Core legado."
                            claim.state == "UNDER_REVIEW" -> "Seu pedido está sendo revisado. Nenhum acesso provisório foi ativado."
                            claim.state == "GRANTED" -> "A análise foi concluída. Atualize o status para confirmar a emissão e recuperar o acesso nesta conta."
                            claim.state == "REJECTED" -> "Entre em contato com o suporte se precisar pedir uma revisão do caso."
                            else -> "Atualize o status para consultar o andamento disponível."
                        }, style = MaterialTheme.typography.bodyLarge)
                        SelectionContainer { Text("Protocolo: ${claim.claimId}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.accountPrivate().testTag("account_protocol")) }
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(claim.claimId)); scope.launch { snackbar.showSnackbar("Protocolo copiado") }
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("account_copy_protocol")) {
                            Icon(Icons.Outlined.ContentCopy, null, Modifier.size(20.dp)); Spacer(Modifier.width(Spacing.Sm)); Text("Copiar protocolo")
                        }
                        Button(onClick = onRefresh, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("account_refresh")) { Text("Consultar andamento") }
                        TextButton(onClick = onSupport, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ajuda com minha solicitação") }
                    }
                    // Without a protocol, a previous access query is not a failed recovery request.
                    if (claim != null || state.notice != AccountNotice.NONE)
                        AccountFeedback(state, includeCoreReminder = false)
                    Text("Você pode continuar usando o Core atual.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("account_core_reminder"))
                }
            }
            if (testLabel != null) Text(testLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, title = { Text("Sair desta conta?") },
        text = { Text("Para usar outra conta, saia primeiro. Seus direitos continuam vinculados à identidade atual. Sair não exclui sua conta nem bloqueia o Core atual.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; pages = listOf(AccountPage.ACCOUNT.name); onLogout() }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } })
}

/** Only account profile/protocol. No SDK defaults or production pipeline change. */
internal fun Modifier.accountPrivate(): Modifier = if (Build.VERSION.SDK_INT >= 29) clarityMask() else this

@Composable private fun RecoveryBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable private fun GoogleAccountButton(enabled: Boolean, onClick: () -> Unit) {
    // Official unmodified neutral icon asset and Google Sans, scoped to Google's brand button.
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(CalcMotShape.Xl),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F2), contentColor = Color(0xFF1F1F1F)),
        contentPadding = PaddingValues(start = 2.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("account_login")) {
        Image(painterResource(R.drawable.google_signin_neutral_icon), null, Modifier.size(40.dp))
        Text("Continuar com Google", fontFamily = FontFamily(Font(R.font.google_sans_medium, FontWeight.Medium)),
            fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

internal fun reviewTitle(claim: LegacyRequestView, state: AccountState): String = when {
    state.access.legacyCore && state.signedIn -> "Direito concedido"
    claim.state == "UNDER_REVIEW" -> "Solicitação em análise"
    claim.state == "GRANTED" -> "Aguardando confirmação do acesso"
    claim.state == "REJECTED" -> "Solicitação não aprovada"
    else -> "Andamento a confirmar"
}

@Composable private fun AccountFeedback(state: AccountState, includeCoreReminder: Boolean = true) {
    accountMessage(state, includeCoreReminder)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("account_message").semantics { liveRegion = LiveRegionMode.Polite }) }
}

internal fun accountMessage(state: AccountState, includeCoreReminder: Boolean = true): String? = when (state.notice) {
    AccountNotice.CANCELED -> if (includeCoreReminder) "Login cancelado. Você pode continuar usando o CalcMot." else "Login cancelado."
    AccountNotice.OFFLINE -> "Sem conexão. Tente novamente quando a internet voltar."
    AccountNotice.SESSION_EXPIRED, AccountNotice.UNAUTHORIZED -> "Confirme sua conta Google para consultar novamente."
    AccountNotice.COLLISION -> "Essa conta Google não corresponde à sessão atual. Nenhum direito foi transferido. Saia para usar outra conta."
    AccountNotice.CONFIGURATION -> "A consulta de acesso ainda não está disponível nesta versão." + coreReminder(includeCoreReminder)
    AccountNotice.UNAVAILABLE -> "Não foi possível concluir agora. Tente novamente ou procure o suporte."
    AccountNotice.LOGGED_OUT -> "Você saiu desta conta." + coreReminder(includeCoreReminder)
    AccountNotice.REVIEW_UNAVAILABLE -> "A solicitação de análise ainda não está disponível no aplicativo. Fale com o suporte para apresentar seu caso."
    AccountNotice.REVIEW_RECEIVED -> "O estado da solicitação foi recebido. A confirmação do acesso é consultada separadamente."
    AccountNotice.REVIEW_CONFLICT -> "Não foi possível retomar este pedido. Fale com o suporte e informe seu protocolo, se disponível."
    AccountNotice.REVIEW_DEFERRED -> "O envio foi adiado enquanto o monitoramento ou a leitura está ativo. Conclua essa atividade e tente novamente."
    AccountNotice.NONE -> when (state.access.status) {
        EntitlementStatus.OFFLINE -> if (includeCoreReminder) "Sem conexão. Direitos antigos já confirmados permanecem preservados." else "Sem conexão. Tente novamente quando a internet voltar."
        EntitlementStatus.UNAUTHORIZED, EntitlementStatus.SESSION_EXPIRED -> if (state.signedIn) "Confirme sua conta Google para consultar novamente." else null
        EntitlementStatus.BACKEND_UNAVAILABLE -> "O serviço de acesso está indisponível. Tente novamente mais tarde." + coreReminder(includeCoreReminder)
        EntitlementStatus.INCONSISTENT, EntitlementStatus.STORAGE_UNAVAILABLE -> "Não foi possível confirmar os dados de acesso. Tente consultar novamente." + coreReminder(includeCoreReminder)
        else -> null
    }
}

private fun coreReminder(include: Boolean): String = if (include) " O Core atual continua disponível." else ""

@Preview(name = "Conta sem login", showBackground = true, widthDp = 360, heightDp = 800)
@Composable private fun AccountPreview() = CalcMotTheme { AccountScreen(AccountState(), {}, {}, {}, {}, {}, {}) }
@Preview(name = "Conta fonte ampliada", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable private fun AccountLargeFontPreview() = CalcMotTheme { AccountScreen(AccountState(signedIn = true, linked = true), {}, {}, {}, {}, {}, {}) }

@Preview(name = "Conta — indisponível", showBackground = true, widthDp = 360, heightDp = 800)
@Composable private fun RefinedAccountPreview() = CalcMotTheme {
    AccountScreen(AccountState(signedIn = true, linked = true, displayName = "Motorista de teste",
        email = "motorista@example.test", access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)),
        {}, {}, {}, {}, {}, {}, testLabel = "PREVIEW — DADOS SINTÉTICOS")
}
@Preview(name = "Meu acesso — indisponível", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Meu acesso — fonte ampliada", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable private fun RefinedAccessPreview() = CalcMotTheme {
    AccountScreen(AccountState(signedIn = true, linked = true, access = EntitlementState(status = EntitlementStatus.BACKEND_UNAVAILABLE)),
        {}, {}, {}, {}, {}, {}, initialPage = AccountPage.ACCESS, testLabel = "PREVIEW — DADOS SINTÉTICOS")
}
@Preview(name = "Recuperação — suporte", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Recuperação — fonte ampliada", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable private fun RefinedRecoveryPreview() = CalcMotTheme {
    AccountScreen(AccountState(signedIn = true, linked = true), {}, {}, {}, {}, {}, {},
        initialPage = AccountPage.RECOVERY, testLabel = "PREVIEW — DADOS SINTÉTICOS")
}
