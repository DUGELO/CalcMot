package br.com.calcmot.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import br.com.calcmot.R
import br.com.calcmot.entitlement.EntitlementStatus
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing as Space

/** UI composition slot; Google remains the only provider wired by AccountRoute. */
internal typealias AccountSignInContent = @Composable (Boolean, () -> Unit) -> Unit

@Composable internal fun AccountEntry(state: AccountState, signIn: AccountSignInContent,
    onLogin: () -> Unit, onContinue: () -> Unit, onSupport: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.Md)) {
        Image(painterResource(R.drawable.calcmot_logo_hero), null, Modifier.size(72.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.Xs)) {
            Text("Minha conta", style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { heading() })
            AccountWordmark(large = true)
        }
        Text("Entre com Google para acessar sua conta no CalcMot.", style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp))
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.Xs)) {
        signIn(!state.busy && state.authAvailable, onLogin)
        AccountQuietAction("Continuar sem conta", onContinue, "account_continue")
    }
    AccountQuietAction("Ajuda e suporte", onSupport, "account_support")
}

@Composable internal fun AccountProfile(state: AccountState) {
    Column(Modifier.accountPrivate(), verticalArrangement = Arrangement.spacedBy(Space.Lg)) {
        AccountWordmark()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Lg)) {
            GoogleAccountAvatar(state.profilePhoto)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.Xs)) {
                Text(state.displayName?.takeIf(String::isNotBlank) ?: if ("google.com" in state.linkedProviders) "Sua conta Google" else "Sua conta",
                    style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                state.email?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        AccountConnectionStatus(state)
    }
}

@Composable internal fun AccountConnectionStatus(state: AccountState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Sm)) {
        Icon(Icons.Outlined.AccountCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(accountSessionLabel(state.linkedProviders),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun AccountActions(actions: List<AccountAction>, onAction: (AccountActionId) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.Xl)) {
        actions.groupBy { it.section }.forEach { (section, entries) ->
            Column(verticalArrangement = Arrangement.spacedBy(if (section == AccountActionSection.SESSION) Space.Md else 0.dp)) {
                if (section == AccountActionSection.SESSION) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                entries.forEachIndexed { index, action ->
                    val (icon, tag) = when (action.id) {
                        AccountActionId.ACCESS -> Icons.Outlined.PersonOutline to "account_details"
                        AccountActionId.SUPPORT -> Icons.AutoMirrored.Outlined.HelpOutline to "account_support"
                        AccountActionId.SIGN_OUT -> Icons.AutoMirrored.Outlined.Logout to "account_logout"
                    }
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Space.Lg), color = MaterialTheme.colorScheme.outline)
                    AccountTaskRow(icon, action.title, action.description, { onAction(action.id) }, tag,
                        action.enabled, chevron = action.id != AccountActionId.SIGN_OUT)
                }
            }
        }
    }
}

@Composable internal fun AccountTaskRow(icon: ImageVector, title: String, description: String?,
    onClick: () -> Unit, tag: String, enabled: Boolean = true, chevron: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = Space.Lg, vertical = Space.Md),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.Xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun AccountAccess(state: AccountState, signIn: AccountSignInContent, onLogin: () -> Unit,
    onRefresh: () -> Unit, onRecovery: () -> Unit, onSupport: () -> Unit,
    detailsExpanded: Boolean, onToggleDetails: () -> Unit) {
    val access = accessPresentation(state)
    if (state.signedIn) AccountConnectionStatus(state)
    Column(verticalArrangement = Arrangement.spacedBy(Space.Md)) {
        Text("Seu acesso no CalcMot", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Surface(shape = RoundedCornerShape(CalcMotShape.Md), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(Space.Lg), verticalArrangement = Arrangement.spacedBy(Space.Md)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Md)) {
                    Icon(when (access.kind) {
                        AccessKind.LEGACY -> Icons.Outlined.VerifiedUser
                        AccessKind.AWAITING -> Icons.Outlined.HourglassEmpty
                        AccessKind.NO_CONFIRMED_GRANT -> Icons.Outlined.Info
                        AccessKind.REVIEW -> Icons.Outlined.PendingActions
                        else -> Icons.Outlined.Info
                    }, null, Modifier.size(24.dp), tint = if (access.kind == AccessKind.LEGACY)
                        CalcMotColors.Success else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(access.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
                Text(if (access.kind == AccessKind.LEGACY) "As funcionalidades gratuitas até a versão 3.5 estão preservadas." else access.description,
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (access.kind == AccessKind.LEGACY) {
                    Text("Este direito não depende de assinatura.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onToggleDetails, modifier = Modifier.heightIn(min = 48.dp).testTag("account_legacy_details")) {
                        Text(if (detailsExpanded) "Ocultar detalhes" else "Detalhes do acesso gratuito")
                    }
                    if (detailsExpanded) LegacyRightDetails()
                }
                if (!state.signedIn || state.access.status in setOf(EntitlementStatus.UNAUTHORIZED, EntitlementStatus.SESSION_EXPIRED))
                    signIn(!state.busy && state.authAvailable, onLogin)
                else TextButton(onClick = onRefresh, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 0.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("account_refresh")) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(20.dp)); Spacer(Modifier.width(Space.Sm))
                    Text(when (access.kind) {
                        AccessKind.UNAVAILABLE -> "Tentar novamente"
                        AccessKind.AWAITING -> "Consultar acesso"
                        else -> "Atualizar status"
                    })
                }
            }
        }
    }
    AccountTaskRow(Icons.AutoMirrored.Outlined.HelpOutline, "Ajuda e suporte", null, onSupport, "account_support")
    if (access.kind != AccessKind.LEGACY) AccountQuietAction(
        if (state.request != null || state.access.claimId != null) "Ver minha solicitação" else "Já usava o CalcMot antes? Saiba mais",
        onRecovery, "account_recovery", small = true)
}

@Composable internal fun LegacyRightDetails() {
    Text("O direito permanente preserva as funcionalidades que eram gratuitas até a versão 3.5. Recursos posteriores não estão incluídos automaticamente.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable internal fun AccountQuietAction(text: String, onClick: () -> Unit, tag: String, small: Boolean = false) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Text(text, style = if (small) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelLarge)
    }
}

@Composable private fun AccountWordmark(large: Boolean = false) {
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color = CalcMotColors.TextPrimary)) { append("Calc") }
        withStyle(SpanStyle(color = CalcMotColors.Success)) { append("Mot") }
    }, style = if (large) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge,
        fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black)
}
