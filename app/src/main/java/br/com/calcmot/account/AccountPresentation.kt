package br.com.calcmot.account

import br.com.calcmot.entitlement.EntitlementStatus

internal enum class AccessKind { LEGACY, TEMPORARY, REVIEW, NO_CONFIRMED_GRANT, UNAVAILABLE, AWAITING, SIGNED_OUT }
internal data class AccessPresentation(val kind: AccessKind, val title: String, val description: String)

/** Presentation of S08's evaluated state. Never computes or grants entitlement. */
internal fun accessPresentation(state: AccountState): AccessPresentation = when {
    !state.signedIn -> AccessPresentation(AccessKind.SIGNED_OUT, "Entre para consultar", "O CalcMot continua disponível sem login.")
    state.access.legacyCore -> AccessPresentation(AccessKind.LEGACY, "Acesso gratuito permanente", "Seu direito ao Core legado está confirmado.")
    state.access.proAllowed -> AccessPresentation(AccessKind.TEMPORARY, "Acesso adicional confirmado", "Recursos adicionais disponíveis no período validado.")
    state.access.status in setOf(EntitlementStatus.OFFLINE, EntitlementStatus.BACKEND_UNAVAILABLE,
        EntitlementStatus.UNAUTHORIZED, EntitlementStatus.SESSION_EXPIRED, EntitlementStatus.INCONSISTENT,
        EntitlementStatus.STORAGE_UNAVAILABLE) -> AccessPresentation(AccessKind.UNAVAILABLE,
        "Não conseguimos consultar agora", when (state.access.status) {
            EntitlementStatus.OFFLINE -> "Tente novamente quando houver conexão. O Core atual continua disponível."
            EntitlementStatus.UNAUTHORIZED, EntitlementStatus.SESSION_EXPIRED -> "Confirme sua conta Google para consultar o acesso."
            else -> if ("google.com" in state.linkedProviders) "Sua conta Google continua conectada. Tente novamente mais tarde."
                else "Sua conta continua conectada. Tente novamente mais tarde."
        })
    (state.request?.state ?: state.access.legacyState) == "UNDER_REVIEW" -> AccessPresentation(AccessKind.REVIEW,
        "Solicitação em análise", "Seu pedido está sendo revisado.")
    state.access.status == EntitlementStatus.READY && state.access.revision != null -> AccessPresentation(
        AccessKind.NO_CONFIRMED_GRANT, "Acesso nesta conta", "Não há um direito antigo confirmado nesta conta. Você pode continuar usando as funções gratuitas atuais.")
    else -> AccessPresentation(AccessKind.AWAITING, "Acesso ainda não verificado", "Consulte seu acesso para obter uma confirmação.")
}

internal enum class AccountActionId { ACCESS, SUPPORT, SIGN_OUT }
internal enum class AccountActionSection { INFORMATION, SESSION }
internal data class AccountAction(val id: AccountActionId, val title: String,
    val description: String? = null, val section: AccountActionSection = AccountActionSection.INFORMATION,
    val enabled: Boolean = true)

/** Ordered account tasks only. Add/reorder entries here; no provider or entitlement decisions. */
internal fun accountActions(state: AccountState): List<AccountAction> = if (!state.signedIn) emptyList() else listOf(
    AccountAction(AccountActionId.ACCESS, "Consultar meu acesso"),
    AccountAction(AccountActionId.SUPPORT, "Ajuda e suporte"),
    AccountAction(AccountActionId.SIGN_OUT, "Sair da conta", "Para usar outra conta Google",
        AccountActionSection.SESSION, enabled = !state.busy))

internal fun accountSessionLabel(providers: Set<String>): String = when {
    "google.com" in providers -> "Conta Google conectada"
    providers.isNotEmpty() -> "Conta conectada"
    else -> "Identidade ainda não vinculada"
}
