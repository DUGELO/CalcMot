# Billing e entitlements — matriz v1.0.0

Companion da [S01/README](README.md), fonte PRD §§3/4/9/11/12/17. Contrato documental; APIs/Console/Library não foram reconferidos ou integrados nesta story. OQ-04/05 e AR-01 continuam bloqueando a prova financeira/integração real.

## Estados separados

`SubscriptionState` e `billingPhase` residem em [entitlement.schema.json](entitlement.schema.json). Não usar o enum de UI como estado armazenado da Play.

| Dimensão | Valores | Autoridade |
|---|---|---|
| SubscriptionState | NONE, PENDING, ACTIVE, CANCELED_WITH_ACCESS, GRACE, ON_HOLD, PAUSED, EXPIRED, REVOKED, UNKNOWN | Última consulta Play verificada pelo backend. NONE = ausência de assinatura conhecida; UNKNOWN = não mapeado/incerto, não sinônimo de expired. |
| billingPhase | NONE, TRIAL, PAID_PERIOD, UNKNOWN | Fase/oferta/período confirmado pelo backend. PAID_PERIOD não é prova financeira de cobrança recebida. |
| VerificationState | VERIFYING, VERIFIED, REJECTED | Operação autenticada de compra; pending Play retorna VERIFYING/reason PURCHASE_PENDING, sem ACK indevido ou grant definitivo. |
| Legacy | UNKNOWN, UNDER_REVIEW, GRANTED, REJECTED | Conta/evidência/política backend; GRANTED permanentemente preservado para Core. |
| Promo | NONE, ACTIVE, EXPIRED no snapshot; ACTIVE/EXPIRED/VOID no grant privado | ACTIVE representa concessão verificada; acesso só no intervalo startAt/validUntil. NONE significa sem concessão, não esconde prazo futuro confirmado. Sem autorrenovação. |
| Reward | EARNED, APPLYING, NEEDS_RECONCILIATION, APPLIED, VOID, EXPIRED | Ledger backend; não é estado de assinatura. |
| Payment event | TRIAL_STARTED, FIRST_CHARGE, RENEWAL_CHARGE, REFUND, VOID + paymentStatus/verificationState | Evidência financeira oficial conciliada; nunca callback/ACTIVE isolados. |

### Mapeamento de nomes do PRD

| Nome ilustrativo/raw | Contrato canônico | Observação |
|---|---|---|
| CANCELED com expiry futuro; canceled_but_valid; cancelled_but_valid | CANCELED_WITH_ACCESS | Depois do expiry, não há acesso Play por esse estado; nova consulta projeta EXPIRED. |
| IN_GRACE_PERIOD / grace | GRACE | Prazo/estado da Play; não conceder grace local. |
| ACTIVE em free trial / TRIAL_ACTIVE do desenho | state ACTIVE + billingPhase TRIAL | UI TRIAL_ACTIVE; firstPaid permanece ausente. |
| ACTIVE em período posterior / PAID_ACTIVE UI | state ACTIVE + billingPhase PAID_PERIOD | Acesso válido; ledger/payment event confirma dinheiro separadamente. |
| VALIDATING / PENDING_VERIFICATION | VerificationState VERIFYING | Não adicionar estados duplicados ao SubscriptionState. |
| HOLD / on_hold | ON_HOLD | Não conserva novo Core pago por tempo indeterminado. |
| refunded / VOID | Evento REFUND/VOID; direito segue consulta Play atual | Reversão financeira pode ou não implicar revogação total. Não mapear todo refund automaticamente para EXPIRED nem remover grant. |
| Play revogado | REVOKED | Bloqueia fonte Play, mas não legado/promo independente. |
| pendingLegacyReview boolean | legacy.state UNDER_REVIEW + review.provisionalUntil | Booleano local não concede acesso infinito. |

Esses são aliases de leitura dos exemplos. Não manter duas rotas/duas emissões/dobrar estado armazenado. A versão/API Google efetiva será mapeada em S19/S20 após consulta oficial; não inventar constante SDK ou endpoint para inferir firstPaid.

## Play → direito no instante autoritativo

Esta tabela descreve a fonte Play para **novo usuário com subscriptionRequired=true**, sem legacy/promo/review independente. A união completa está em INV-03 do README. A validade usa intervalo com fim exclusivo: `validUntil > now`; cancelamento não termina direito já vigente. Verificação de dinheiro não condiciona o acesso de trial confirmado.

| Estado confirmado | Condição | Core/Pro pela fonte Play | UI segura / financeiro |
|---|---|---|---|
| NONE | Sem assinatura | Não | PAID_ONLY/TRIAL_ELIGIBLE após catálogo/readiness/demo; nenhum trial automático. |
| PENDING | Pagamento ainda pendente | Não | CHECKOUT_PENDING/VERIFYING; PURCHASE_RECOVERY/restore; nenhuma primeira cobrança/comissão/reward. |
| ACTIVE + TRIAL | expiry futuro confirmado | Sim | TRIAL_ACTIVE; prazo/condições Play; trial_started_verified, não first_charge_verified. |
| ACTIVE + PAID_PERIOD | expiry futuro confirmado | Sim | PAID_ACTIVE (“assinatura vigente”); só evento financeiro oficial prova dinheiro. |
| ACTIVE + UNKNOWN | expiry futuro confirmado | Sim | PURCHASE_RECOVERY/gestão para esclarecer fase; não inventar trial/firstPaid. |
| CANCELED_WITH_ACCESS | expiry futuro confirmado | Sim até expiry | Informar fim confirmado; Gerenciar/Restaurar. Sem cobrança automática de trial cancelado quando Play confirma cancelamento efetivo. |
| GRACE | validity/expiry vigente conforme Play | Sim conforme prazo Play | Aviso de pagamento/gerenciamento fora de trabalho; backend pode emitir grace_entered. |
| ON_HOLD | Estado efetivo confirmado | Não | ON_HOLD, ação Play/restore fora do turno. Não promover trial/promo para esconder falha. |
| PAUSED | Pausa efetiva confirmada | Não | PAUSED/gestão segura; pausa agendada futura não equivale a pausa efetiva, seguir estado/período confirmado. |
| EXPIRED | Vencido confirmado | Não | EXPIRED/CHOOSE_PLAN somente em entrada segura, com restore/suporte. |
| REVOKED | Revogação confirmada | Não | EXPIRED ou PURCHASE_RECOVERY conforme motivo; fonte independente permanece. |
| UNKNOWN | Estado não mapeado/não confirmado | Não cria novo direito | Reconciliar; cache de direito anteriormente confirmado segue política finita OQ-03. Nunca interpretar automaticamente como compra/expiry. |

Estado/expiry inconsistentes não autorizam estender período: reconciliar. A UI pode continuar acesso de cache previamente válido durante falha transitória dentro da política aprovada; isso não muda o estado Play nem gera pagamento. Expiry no meio de monitoring não dispara overlay/paywall/interrupção; restrição na próxima entrada segura, sem fallback ilimitado.

## UI de readiness, compra e recuperação

Todos os estados estão em `$defs/BillingUiState`. Eles são apresentação do lifecycle, não fonte financeira.

| Estado UI | Entrada e ação verificável |
|---|---|
| SETUP_REQUIRED | Disclosure/consentimento e setup gratuito; nenhum checkout automático. |
| DEMO_READY | Simulação identificada disponível; nenhuma captura de app terceiro. |
| OFFER_LOADING | Consulta ProductDetails; retry/erro honesto se falhar. |
| TRIAL_ELIGIBLE | Oferta real 7d, preços/período/renovação/cancelamento/data ou termo Play; “Ativar meus 7 dias” por gesto. |
| PAID_ONLY | Play não oferece trial; checkout com preço real, sem prometer dias grátis. |
| CHECKOUT_PENDING | Compra Play pendente; não confundir com verificação concluída. |
| VERIFYING | Backend consultando/verificando; 202 não é entitlement definitivo ou indefinido. |
| TRIAL_ACTIVE | Trial confirmado na Play, expiry vigente; nunca chamado de pagante financeiro. |
| PAID_ACTIVE | Assinatura em período vigente confirmado; não é prova de receita liquidada por si. |
| CANCELED_WITH_ACCESS | Acesso até data confirmada; cancel não bloqueia imediatamente. |
| GRACE | Direitos conforme Play; gestão/retry seguro. |
| ON_HOLD / PAUSED | Situação efetiva; gerir/restaurar, respeitando fontes independentes. |
| EXPIRED | Sem fonte Play válida; só restringir quando não houver fonte independente e for seguro. |
| PURCHASE_RECOVERY | Compra paga não sincronizada, fase UNKNOWN, falha de restore ou conflito de posse: prioridade/suporte; não transferir token por declaração. |
| LEGACY_GRANDFATHERED | Rótulo “Acesso antigo preservado”; nunca paywall Core. |

Se fontes coexistirem, LEGACY_GRANDFATHERED para Core não esconde status de assinatura opcional/Pro. A escolha da apresentação simultânea segue UX S24/S25/OQ-12, sem alterar matriz de direito.

## Recompensas e fontes em coexistência

| Situação | Decisão server-side | Confirmação UI |
|---|---|---|
| Auto-renovável efetivamente elegível a defer | GET/etag/lock/operação/delta, enviar uma vez, confirmar expiry | “Próxima cobrança adiada até [data confirmada]”, somente APPLIED. |
| Trial/canceled/grace/hold/pausa/inelegível ou prova insuficiente | EARNED; não deferir/promover indiscriminadamente nem cobrar enquanto anuncia dias grátis | “Dias conquistados, aguardando aplicação”; WAIT_ELIGIBLE_STATE/CONTACT_SUPPORT. |
| Sem assinatura/renovação conflitante | Solicitação voluntária; promo com datas server-side/idempotência | “Pro de X a Y, sem renovação automática”. |
| Legado + Insights real validado | Promo Pro adicional | Ao expirar Pro, Core continua grátis. |
| Legado sem vantagem adicional | Rewards do segmento OFF, shared link orgânico disponível | Sem progresso/promessa de “Core grátis”. |
| Timeout após defer enviado | NEEDS_RECONCILIATION, GET/histórico/revisão; não retry cego | Aguardando confirmação, sem APPLIED inventado. |

Ineligibilidade temporária não apaga crédito. Do ledger para views: APPLIED/EXPIRED conserva appliedDays; campaign PAUSED conserva créditos/datas e suporte. Sem sobrepor promo a período pago conflitante ou gerar autorrenovação. Resgate/void tardio/janela continuam OQ-06; não assumir 90 dias.

## Gates que continuam obrigatórios

Biblioteca 9.1.0+ e ACK baseline três dias são requisitos do PRD a reconferir em S03/S18/S19, não resultados de S01. IDs `calcmot_pro`, `monthly_auto`, `quarterly_auto`, `trial_7d_new_user` continuam propostos até Console. Fonte de firstCharge/renewal/void e chave econômica dependem de prova S21; proration/replacement/defer de OQ-05. Offline/entrada segura dependem OQ-03/10. Cutoff somente após ponte/restore/claims e autorização; flags default OFF. Nenhuma decisão dessa matriz autoriza ligar produção.
