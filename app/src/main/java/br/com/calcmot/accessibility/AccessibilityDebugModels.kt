package br.com.calcmot.accessibility

import br.com.calcmot.processor.AccessibilityTreeSnapshot
import br.com.calcmot.processor.TreeOfferInspection

data class AccessibilityDebugOverlayState(
    val serviceActive: Boolean,
    val uberForeground: Boolean,
    val lastEventType: String,
    val rootStatus: String,
    val windowCount: Int,
    val nodesScanned: Int,
    val textNodeCount: Int,
    val contentDescriptionNodeCount: Int,
    val candidateCount: Int,
    val lastFailureReason: AccessibilityFailureCategory,
    val bestDelayMs: Long?,
    val updatedAtMs: Long = System.currentTimeMillis()
)

enum class AccessibilityFailureCategory(
    val label: String,
    val suggestedAction: String
) {
    SERVICE_NOT_RUNNING(
        "SERVICE_NOT_RUNNING",
        "Ative o serviço de acessibilidade do CalcMot."
    ),
    RESTRICTED_SETTINGS_NOT_ALLOWED(
        "RESTRICTED_SETTINGS_NOT_ALLOWED",
        "Libere configurações restritas do Android para o CalcMot, se o sistema bloquear a permissão."
    ),
    ACCESSIBILITY_PERMISSION_MISSING(
        "ACCESSIBILITY_PERMISSION_MISSING",
        "Abra as configurações e ative a acessibilidade do CalcMot."
    ),
    OVERLAY_PERMISSION_MISSING(
        "OVERLAY_PERMISSION_MISSING",
        "Verifique permissão de sobreposição apenas para modo de teste fora do AccessibilityService."
    ),
    UBER_NOT_FOREGROUND(
        "UBER_NOT_FOREGROUND",
        "Abra o app de motorista e mantenha a tela da oferta em primeiro plano."
    ),
    NO_ACCESSIBILITY_EVENTS(
        "NO_ACCESSIBILITY_EVENTS",
        "Aguarde um evento da Uber ou reative o serviço de acessibilidade."
    ),
    ROOT_NULL(
        "ROOT_NULL",
        "rootInActiveWindow veio nulo; verificar timing, foreground e restrições do Android."
    ),
    WINDOWS_EMPTY(
        "WINDOWS_EMPTY",
        "getWindows não trouxe janelas úteis; verificar flags do serviço e tela em primeiro plano."
    ),
    TREE_EMPTY(
        "TREE_EMPTY",
        "A árvore foi lida, mas não há nós; testar delays maiores e refresh."
    ),
    TREE_HAS_NODES_NO_TEXT_NO_DESC(
        "TREE_HAS_NODES_NO_TEXT_NO_DESC",
        "A árvore tem nós, mas sem texto/contentDescription; investigar fontes semânticas alternativas."
    ),
    TREE_HAS_DATA_PARSER_FAILED(
        "TREE_HAS_DATA_PARSER_FAILED",
        "A árvore tem dados, mas o parser não reconheceu card; gerar fixture a partir do dump."
    ),
    PARSER_OK_STATE_REJECTED(
        "PARSER_OK_STATE_REJECTED",
        "O parser reconheceu candidato, mas a máquina de estado descartou; revisar estabilidade."
    ),
    STATE_OK_OVERLAY_FAILED(
        "STATE_OK_OVERLAY_FAILED",
        "O estado validou, mas o overlay falhou; revisar WindowManager e tokens."
    ),
    OVERLAY_RENDERED_OFFSCREEN(
        "OVERLAY_RENDERED_OFFSCREEN",
        "Overlay renderizado fora da tela; resetar posição customizada."
    ),
    CARD_EXPIRED_BEFORE_SCAN(
        "CARD_EXPIRED_BEFORE_SCAN",
        "O card sumiu antes do melhor delay; reduzir latência da leitura."
    ),
    UNKNOWN(
        "UNKNOWN",
        "Coletar novo dump com timing sweep."
    )
}

data class TimingSweepObservation(
    val generationId: Long,
    val delayMs: Long,
    val scanId: String,
    val rootNull: Boolean,
    val windowCount: Int,
    val snapshot: AccessibilityTreeSnapshot?,
    val inspection: TreeOfferInspection?,
    val candidateParsed: Boolean,
    val refreshDeltaNodes: Int = 0,
    val failureCategory: AccessibilityFailureCategory
) {
    val nodeCount: Int = snapshot?.nodeCount ?: 0
    val textNodeCount: Int = snapshot?.nodes?.count { !it.textRaw.isNullOrBlank() } ?: 0
    val contentDescriptionNodeCount: Int = snapshot?.nodes?.count { !it.contentDescriptionRaw.isNullOrBlank() } ?: 0
    val fieldCandidateCount: Int = inspection?.fieldCandidates?.size ?: 0
}

data class TimingSweepSummary(
    val generationId: Long,
    val observations: List<TimingSweepObservation>
) {
    val bestDelayMs: Long?
        get() = observations
            .sortedWith(
                compareByDescending<TimingSweepObservation> { it.candidateParsed }
                    .thenByDescending { it.contentDescriptionNodeCount }
                    .thenByDescending { it.textNodeCount }
                    .thenByDescending { it.nodeCount }
                    .thenBy { it.delayMs }
            )
            .firstOrNull()
            ?.delayMs

    val firstValidCandidateDelayMs: Long?
        get() = observations.firstOrNull { it.candidateParsed }?.delayMs

    val finalFailureCategory: AccessibilityFailureCategory
        get() = when {
            observations.isEmpty() -> AccessibilityFailureCategory.NO_ACCESSIBILITY_EVENTS
            observations.any { it.candidateParsed } -> AccessibilityFailureCategory.UNKNOWN
            observations.all { it.rootNull } -> AccessibilityFailureCategory.ROOT_NULL
            observations.all { it.windowCount == 0 } -> AccessibilityFailureCategory.WINDOWS_EMPTY
            observations.all { it.nodeCount == 0 } -> AccessibilityFailureCategory.TREE_EMPTY
            observations.any { it.nodeCount > 0 && it.textNodeCount == 0 && it.contentDescriptionNodeCount == 0 } -> {
                AccessibilityFailureCategory.TREE_HAS_NODES_NO_TEXT_NO_DESC
            }
            observations.any { it.fieldCandidateCount > 0 || (it.textNodeCount + it.contentDescriptionNodeCount) > 0 } -> {
                AccessibilityFailureCategory.TREE_HAS_DATA_PARSER_FAILED
            }
            else -> AccessibilityFailureCategory.UNKNOWN
        }

    fun toOverlayState(
        serviceActive: Boolean,
        uberForeground: Boolean,
        lastEventType: String
    ): AccessibilityDebugOverlayState {
        val best = observations.lastOrNull { it.delayMs == bestDelayMs } ?: observations.lastOrNull()
        return AccessibilityDebugOverlayState(
            serviceActive = serviceActive,
            uberForeground = uberForeground,
            lastEventType = lastEventType,
            rootStatus = if (best?.rootNull == true) "null" else "ok",
            windowCount = best?.windowCount ?: 0,
            nodesScanned = best?.nodeCount ?: 0,
            textNodeCount = best?.textNodeCount ?: 0,
            contentDescriptionNodeCount = best?.contentDescriptionNodeCount ?: 0,
            candidateCount = observations.count { it.candidateParsed },
            lastFailureReason = if (observations.any { it.candidateParsed }) {
                AccessibilityFailureCategory.UNKNOWN
            } else {
                finalFailureCategory
            },
            bestDelayMs = bestDelayMs
        )
    }
}

object AccessibilityFailureClassifier {
    fun classify(
        serviceActive: Boolean,
        uberForeground: Boolean,
        rootNull: Boolean,
        windowCount: Int,
        nodeCount: Int,
        textNodeCount: Int,
        contentDescriptionNodeCount: Int,
        inspection: TreeOfferInspection?,
        parserSucceeded: Boolean,
        stateRejected: Boolean = false,
        overlayFailed: Boolean = false
    ): AccessibilityFailureCategory {
        return when {
            !serviceActive -> AccessibilityFailureCategory.SERVICE_NOT_RUNNING
            !uberForeground -> AccessibilityFailureCategory.UBER_NOT_FOREGROUND
            overlayFailed -> AccessibilityFailureCategory.STATE_OK_OVERLAY_FAILED
            stateRejected && parserSucceeded -> AccessibilityFailureCategory.PARSER_OK_STATE_REJECTED
            rootNull -> AccessibilityFailureCategory.ROOT_NULL
            windowCount <= 0 -> AccessibilityFailureCategory.WINDOWS_EMPTY
            nodeCount <= 0 -> AccessibilityFailureCategory.TREE_EMPTY
            textNodeCount == 0 && contentDescriptionNodeCount == 0 -> {
                AccessibilityFailureCategory.TREE_HAS_NODES_NO_TEXT_NO_DESC
            }
            !parserSucceeded && inspection != null -> AccessibilityFailureCategory.TREE_HAS_DATA_PARSER_FAILED
            else -> AccessibilityFailureCategory.UNKNOWN
        }
    }
}
