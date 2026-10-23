# CalcMot Growth & Monetization V1 — contratos congelados

Story executada: **S01 exclusivamente**. Versão documental dos contratos: **1.0.0**, congelada em 2026-10-09. Nenhum handler, SDK, regra Firebase, serviço externo ou feature flag foi implementado/habilitado. O congelamento define linguagem e invariantes; não aprova parâmetros comerciais pendentes nem comprova ambiente/API Play.

Fontes: [BACKLOG.md](../product/BACKLOG.md), [EXECUTION-PLAN.md](../product/EXECUTION-PLAN.md), `C:\Users\426174\Downloads\CalcMot_PRD_v5_Final.md` (v5, SHA-256 `7CD6D94723F381B69B71413EEEFFA4287DB0A4CE032DD28017031A5C3FEFE221`), `AGENTS.md`. PRD §§3–5/9–12/15–17 prevalecem sobre exemplos ilustrativos. A S01 não depende de outra story; resolve OQ-08.

## Artefatos e fronteiras

| Arquivo | Contrato |
|---|---|
| [entitlement.schema.json](entitlement.schema.json) | JSON Schema 2020-12: raiz `EntitlementSnapshot`; `$defs` para Identity, LegacyGrant/Claim, cache, Play/projeção/compra, estados UI, recibo lógico e envelope analítico. |
| [referral.schema.json](referral.schema.json) | JSON Schema 2020-12: raiz `ReferralSummary` própria/agregada; `$defs` para atribuição, qualificação, ledger, promo, view própria de reward, comissão e resumo creator. |
| [api.openapi.yaml](api.openapi.yaml) | OpenAPI 3.1.0, rotas e envelopes mínimos; reutiliza schemas por referências locais. Sem URL de backend inventada. |
| [billing-state-matrix.md](billing-state-matrix.md) | Separação Play/verification/UI/payment, fontes e regras de projeção/segurança. |
| [analytics-events.md](analytics-events.md) | Nomes, emissor, dedupe, parâmetros e regras de coorte; compatibilidade com telemetria existente. |
| [test-matrix.md](test-matrix.md) | Fixtures sintéticas Given/When/Then, positivos/negativos estruturais e regras semânticas. Não é implementação de policy engine. |

Schemas validam **estrutura**, enums e algumas implicações locais. Comparação de instantes, identidade, provas Play, rate limit, cardinalidade e consistência entre registros são invariantes semânticas explícitas abaixo/test-matrix. Passar schema não autoriza concessão de direito, qualificação ou pagamento.

Somente `:app` existe. Futuras implementações usam pacotes `identity/`, `entitlement/`, `billing/`, `growth/` quando necessários; não criar módulos Gradle/frameworks. Reutilizar `telemetry/`/`analytics/`. Os nomes de coleções/arquivos de backend permanecem lógicos até AR-01; DTO não é layout Firestore obrigatório.

## Decisões de OQ-08

| ID | Decisão congelada | Origem/racional |
|---|---|---|
| D01 | **`GET /v1/me/entitlement`** é a única rota canônica; **`api.openapi.yaml`** é o único OpenAPI. `/v1/entitlements/me` e `openapi.yaml` são nomes de exemplos antigos, não rotas/arquivos a criar em paralelo. | PRD §11.0 usa rota v5; §15.1 especifica arquivo. Nenhuma API prévia encontrada para migrar. |
| D02 | `LegacyClaim.state = UNDER_REVIEW / GRANTED / REJECTED`; `PENDING` no exemplo de claim é alias documental de UNDER_REVIEW. `LegacySnapshot.state` acrescenta UNKNOWN quando não há decisão. | Preserva §3/§11 e evita confundir com pending Play. |
| D03 | `SubscriptionState` usa NONE/PENDING/ACTIVE/CANCELED_WITH_ACCESS/GRACE/ON_HOLD/PAUSED/EXPIRED/REVOKED/UNKNOWN. `billingPhase = NONE/TRIAL/PAID_PERIOD/UNKNOWN`; verification usa VERIFYING/VERIFIED/REJECTED. UI tem enum próprio. | Estado confirmado, fase/termo e operação de verificação são coisas diferentes; ACTIVE nunca comprova dinheiro. Matriz detalhada no companion. |
| D04 | Wire snapshot tem `play.validUntil`, `promo.validUntil`, `review.provisionalUntil`; `revision`, `serverNow`, `checkedAt`, `subscriptionRequired`, `legacyCore`, `coreAllowed`, `proAllowed`, `proFeatureSet`. | Une exemplos §§9/11 sem criar vários campos expiry concorrentes. `playAccessUntil`/`promoProUntil` do pseudo-Kotlin podem ser accessors, não campos novos wire. `checkedAt` é confirmação/projeção backend, não última tentativa de rede cliente. |
| D05 | Grant usa `legacyGrantedAt`, `legacyEvidenceType`, `legacyCoreVersionBaseline`, `migrationVersion`, `grantRevision`, policy/evidence/actor; `policyCutoffUtc` pode ser null em política da ponte antes do congelamento. Grant confirmado não é reescrito ao definir cutoff depois. | §3.2 exige ponte antes do cutoff; não escolher data nem tornar schema incapaz de representar esse intervalo. Política exata segue OQ-02. |
| D06 | Reward ledger: EARNED/APPLYING/NEEDS_RECONCILIATION/APPLIED/VOID/EXPIRED. Crédito temporariamente inaplicável continua EARNED; motivo/nextAction explica situação. ELIGIBLE/EARNED_NOT_APPLIED/EARNED_UNAPPLIED são aliases de EARNED. | Normaliza §§5/11/12 sem inventar direito antes da qualificação. PENDING antes de qualificar pertence à indicação; RETRY só volta a EARNED se houver prova de nenhum efeito enviado. |
| D07 | Comissão é registro por `milestone = FIRST_CHARGE / FIRST_RENEWAL` e `state = PENDING / ELIGIBLE / SETTLED / VOID / REVIEW`; valores BRL 300/100 em `amountMinor`. | Separa os estados compostos FIRST_PAID_ELIGIBLE/SETTLED_R3/SETTLED_R1 do §12 em etapa + estado, com chaves idempotentes. Não equivale a CAC total. |
| D08 | `sourceType = DRIVER / CREATOR / ORGANIC / UNKNOWN`; primeira origem válida fica LOCKED antes do checkout, inclusive organic/unknown quando não há código. POST attribution/claim aceita intenção CLAIM ou LOCK; UID/fonte real são determinados pelo servidor. | §§5.1/9.2, backlog S24/S28. Não exige Hub para checkout, não permite troca de origem por token/cupom tardio. |
| D09 | `POST /v1/rewards/{rewardId}/redeem` é somente intenção autenticada de resgate; corpo vazio, sem uid/dias/datas/token/amount. Backend decide modalidade/prazo e retorna view própria. `/internal/reward/apply` é exclusivo de identidade de serviço. | Completa apenas a fronteira de resgate já exigida por §§5.4/11.5 e OQ-08. Não cria benefício, prazo de 90d aprovado ou novo mecanismo de cobrança. |
| D10 | Eventos financeiros canônicos: first_charge_verified; renewal_charge_verified para **cada** renovação; first_renewal_verified para a **primeira** delas. Aliases de primeiro pagamento são normalizados, não emitidos duas vezes. | §11.1/16.2; primeira renovação é subconjunto, não sinônimo de todas. Ledger deduplica por compra/etapa, não contagem de eventos. |
| D11 | Uma análise lógica é um resultado novo aceito/concluído no lifecycle já existente do Core, com analysisId opaco atribuído uma vez àquele resultado. Frames, retry, refresh/TTL e refinamentos do mesmo resultado reutilizam ID. Uma nova oferta aceita é outra análise, mesmo com valores financeiros iguais. | §16.1; fingerprint bruto/de conteúdo não é ID analítico nem prova inequívoca sozinho. S10 escolhe o seam existente verificável, sem nova heurística de parser/time-window. |
| D12 | O evento preserva data local `YYYY-MM-DD`, timezone IANA e instante UTC **do sucesso original**; chaves de dias já registrados não são recalculadas por mudança de timezone/relógio/linking. Na vinculação legítima fornecida por S04, unir sets de datas e IDs e usar primeiro valor/first_open mais antigo uma vez. | §16 pede dias locais estáveis/mesma coorte. Não fixar UTC ou America/Sao_Paulo para todo usuário. Não transmitir recibo individual de oferta para backend de growth; métricas usam dias/agregados e sinal mínimo de primeiro valor. |

Não foi criado ADR separado: são padronizações dos contratos ilustrativos já pedidos pelo PRD; a tabela acima registra alternativa, escolha e efeito. Arquitetura/backend/provedor Auth/política comercial não foram escolhidos. Alteração incompatível dos contratos requer revisão/versionamento e migração dos registros já emitidos, nunca edição retroativa silenciosa.

## Invariantes comuns

### INV-01 — Identidade e autoridade

Firebase UID vem da sessão validada. Clientes não escolhem uid/sourceId/amount/grant/effect. `Identity` é estado privado, não request de concessão. Provider/linking/colisão e mapeamento definitivo de identidades permanecem OQ-01. União métrica só ocorre após mapeamento legítimo, não funde grants/compras de terceiros por declaração. Restaurar token exige titular/package/produto/histórico confirmado.

### INV-02 — Legado

Core existente do legado confirmado é gratuito sem prazo. Registros confirmados são auditáveis e imutáveis; preço/plano/desinstalação/flag não revogam grant. Evidências locais são auxiliares; não prometer histórico perfeito de instalação Play. Claim tardio plausível tem protocolo/provisório limitado conforme backend/política OQ-02, sem bloqueio abrupto em condução. Fraude comprovada é caso excepcional de suporte/auditoria/termos, não revogação automática comercial.

### INV-03 — União de direitos

Em `serverNow`, `playValid` exige state ACTIVE/CANCELED_WITH_ACCESS/GRACE e validUntil futuro confirmado. `promoValid` exige ACTIVE e `startAt ≤ now < validUntil`. `reviewValid` exige UNDER_REVIEW e provisionalUntil futuro concedido pelo servidor. `legacyCore` equivale a legacy.state GRANTED.

`coreAllowed = !subscriptionRequired || legacyCore || playValid || promoValid || reviewValid`.

Promo ACTIVE é concessão verificada com período registrado, não promessa de que o período já começou: startAt futuro mantém acesso por promo falso até o início. O snapshot conserva as datas confirmadas para avaliar a validade; não é necessário um estado SCHEDULED adicional nesta V1.

`proAllowed = playValid || promoValid`; `proFeatureSet` lista somente features reais disponíveis àquela conta. No schema v1 o único feature set adicional é PRO_INSIGHTS_WEEKLY, condicionado à S37/S38. Pro válido pode não ter Insights lançado: não anunciar feature inexistente. Review/ponte/legado por si só não concedem Pro. Union/prioridade não apaga fontes independentes. `subscriptionRequired` é decisão server-side de flags + segmento/cutoff/bucket, não preferência cliente.

### INV-04 — Offline e segurança

Snapshot gerado pelo servidor/revisão/validade e âncora elapsedRealtime são cacheados; oferta lê RAM, sem I/O extra de Auth/Billing/growth no Accessibility/OCR/parser/overlay. Cache não modifica fonte/período comprado. Legacy confirmado é durável; review/paid/promo/fallback têm limite aprovado. Tolerância/reboot/desvio do relógio seguem OQ-03, não um número inventado. Aplicar restrição comercial em entrada segura, nunca durante processamento/turno; segurança não permite fallback infinito. Definição de entrada segura/recent offer depende OQ-10; incerteza suprime UI comercial.

### INV-05 — Trial e catálogo

Setup/disclosure/readiness/demo são gratuitos. Demo é sintética, claramente identificada, não usa terceiros e não gera Core day/first value. Trial 7d é escolha voluntária após setup/demo e oferta Play elegível; início/expiry não vêm de instalação/relógio local. ProductDetails/offerToken/preço/período da região são autoridade de UX; R$10/mês e R$25/trimestre são hipóteses BR a configurar/confirmar, não strings fixas. Não prometer “sem cartão”. Mesmo acesso nos dois planos; cancelamento conserva período válido.

### INV-06 — Compra e prova financeira

Billing callback/RTDN são sinais para reconsulta Play; cliente não concede assinatura definitiva. Verificar/bind/ACK (inclusive trial) no prazo vigente; baseline PRD é três dias, a reconferir por S03/S19. UNKNOWN/PENDING/VERIFYING não significam pagamento. Projeção Play pode conceder direito de trial/assinatura válida **sem** comprovar firstPaid; somente evidência financeira oficial de S21, conciliada por S22, comprova cobrança real/não anulada. `PAID_PERIOD`/UI PAID_ACTIVE descrevem fase/acesso, não caixa recebido.

### INV-07 — Registros financeiros e idempotência

`economicPurchaseKey` representa transação cobrada independente de messageId/callback/token replacement. `PurchaseEvent` registra observação verificada da transação/categoria; refund/void é registro de reversão relacionado, não desaparecimento do histórico da cobrança. `paymentStatus=PAID` significa prova naquele evento, não garantia de ausência de void posterior. Qualificação consulta a reversão atual/janela antes de conceder. Dedupe mínimo: objeto econômico + categoria/identidade do evento financeiro oficial; a fonte e forma exata da chave são bloqueadas por OQ-04/S21, não inferidas de ACTIVE. `firstRenewal=true` só na primeira RENEWAL_CHARGE paga. Uma compra validada cobrada sem entitlement exige incidente P0/owner/reparação, não silêncio.

### INV-08 — Atribuição e canal

Um indicado tem uma origem premiável: DRIVER ou CREATOR, ou nenhuma remuneração ORGANIC/UNKNOWN. Primeira fonte válida prevalece; lock é atômico antes do checkout. CLAIM não substitui fonte válida já existente. LOCK repetido com mesma intenção devolve original; tentativa de trocar fonte após lock retorna ALREADY_LOCKED. App Link tardio só informa. Código opaco não expõe PII; domínio canônico `https://calcmot.com.br/r/{code}`. Self-referral por mesma identidade é rejeitado; antifraude além do mínimo depende OQ-06, sem fingerprinting/agenda.

### INV-09 — Qualificação

Só novo pagante distinto, first-charge efetivamente paga/confirmada, janela refund/void concluída sem reversão, identidade regularizada e fonte válida. Chave lógica inclui invitee/campaign/compra econômica e impede segunda qualificação por renovação ou token substituído. Trial/click/download/demo nunca qualificam. firstCore/TRIAL são etapas de observação, não requisitos adicionais de uso mínimo inventados. Estorno antes de QUALIFIED anula pendente; depois segue termos/revisão OQ-06, sem retirada silenciosa de dias consumidos. Canal CREATOR pode passar pela validação, mas não cria reward DRIVER.

### INV-10 — Contagem e ledger

Campanha V1 tem uma trilha 0→7→30→60 **totais**, por indicador/campanha, a partir de 1/3/5 qualificados únicos. Contagens 2/4 conservam tier; 6+ não concedem mais. Materializar milestones cruzados em ordem ascendente, uma linha por `campaignId:inviterUid:milestone`: deltas 7/23/30. Assim, se cinco pagamentos chegam juntos, reservar linhas 1/3/5 uma vez; nunca criar 60 extra sobre 30. `earnedDays` é a soma de deltas concedidos/reservados, `appliedDays` soma deltas confirmados, inclusive já consumidos/futuros. Não reiniciar pelos dias ainda restantes. Void tardio/efeito já aplicado exige política/manual; não reduzir saldo histórico silenciosamente.

### INV-11 — Efeito externo de reward

Ledger EARNED reserva direito em transação; chamada Play fica fora dela. Lock lógico por tokenHash, operationId determinístico do reward, GET/etag/preExpiry + delta, defer uma vez por operação. Confirmar expiry real antes de APPLIED. Timeout **após envio** → NEEDS_RECONCILIATION, reconsultar e verificar histórico; não reenviar/reverter cegamente. Etag conflitante volta à decisão de elegibilidade. Falta de prova do efeito pede revisão manual. `validateOnly` é dry-run. RTDN SUBSCRIPTION_DEFERRED não concede reward. EXPIRED não zera appliedDays nem reabre marco.

No ledger `expectedExpiry` é o **preExpiry** lido antes do envio; `etag` é a versão dessa leitura; `observedExpiry` é o prazo efetivo confirmado após o efeito. `appliedExpiry` do exemplo antigo é alias de observedExpiry. Delta desejado deriva de deltaDays (7/23/30 →604800/1987200/2592000 segundos), sem segundo contador independente. Em reconciliação não atribuir toda mudança de expiry a esta operação se existirem outras causas; manter revisão quando a prova for insuficiente.

### INV-12 — Promo/resgate

Sem assinatura elegível, cliente pode solicitar resgate; backend verifica dono/modalidade/período pago/termos, datas e atomicidade por sourceRewardId. Promo não cria compra/autorrenovação; `autoRenewal=false`. Não sobrepor dias a cobrança recorrente que continua ativa: canceled/grace/hold/trial/pausa/ineligível conservam EARNED/WAIT_ELIGIBLE_STATE se não houver operação segura. Promo futura não libera acesso antes de startAt. Legado só recebe reward Pro com Insights real validado; caso contrário permanece indicação sem incentivo, com Core gratuito. Prazo de resgate/ativação OQ-06 aberto; proposta 90d não é default contratado.

### INV-13 — Comissão e liquidação

Uma linha por `creatorId:inviteeUid:milestone`, protegida também pelo economicPurchaseKey/campanha/canal: FIRST_CHARGE=300 centavos BRL; FIRST_RENEWAL=100, total V1 R$4 por indicado. Renovação é pagamento após ciclo real mensal/trimestral, não M2 civil. PENDING não é comissão paga; ELIGIBLE tem prova/janela; SETTLED exige lote/prova de liquidação/settledAt. VOID pré-settlement não paga; reversão tardia vai a revisão/termos, preservando histórico de lote/data se já pago. Não pagar o mesmo purchase como reward e comissão. Não automatizar payout por 80% ou R$8 de repasse presumidos; CAC cash/econ e contribuição são separados, teto <R$4 é alvo de piloto.

### INV-14 — Views e dados

UID/evidenceHash/order refs/purchase hashes/operationId são registros privados backend/local conforme contrato, não Analytics params nem views de terceiros. `ReferralSummary`/`RewardView` expõem só código próprio, counts/estágios/dias/datas; CreatorSummary contém agregado do próprio creator, sem UID/nome/telefone/cidade de indicado. Token completo só server-side protegido; transporte de compra não é analytics. Regras cliente negam escrita em grants/entitlement/financeiro/attribution/ledgers. Admin exige RBAC/auditoria, endpoints internal exigem identidade de serviço, não Firebase token cliente. Rate limits/retention/deleção/admin owners ainda OQ-06/07/AR-01.

### INV-15 — Flags/rollback

Defaults do PRD §17: cobrança/trial/trimestral/referral Hub/rewards/affiliate/share prompt/card/notificações/review/Integrity OFF; claims ON. Buckets de novos estáveis no servidor; nunca reclassificar pré-cutoff como novo. Desligar novas iniciativas não cancela assinatura automaticamente, não apaga grants/créditos/promos/comissões prometidos; RTDN/ACK/reconcile/suporte/void/obrigações continuam. `ReferralSummary.programMode=ORGANIC_ONLY` só quando não há programa/direitos remunerados naquele segmento; `REWARDS_PAUSED` representa campanha pausada com créditos/efeitos anteriores ainda visíveis. Não esconder promises com um summary vazio no rollback.

### INV-16 — Growth seguro

Prompt referral proativo após ≥2 dias reais, foreground seguro, monitoring/OCR/overlay OFF, sem oferta recente/direção; ≤2/30d, 14d após dispensa, duas dispensas →60d, opt-out persistente. Botão voluntário não exige o cap de prompt, mas share/checkout continuam sujeitos à segurança. Sharesheet ACTION_SEND por toque não comprova envio. Push marketing OFF, POST_NOTIFICATIONS não condiciona Core. Review sem incentivo/pré-pergunta de satisfação e sem review_submitted. Cartão não contém PII/screenshot de motorista/economia inventada.

### INV-17 — Métricas

CoreAnalysisReceipt é **contrato local de dedupe**, não rota de upload de corrida. Não guarda conteúdo/fingerprint de oferta no backend de growth. `core_active_day` deduplica `(identityKey, localDate)`; first_core_value uma vez; vínculo legítimo une datas/IDs sem duplicar primeira análise nem reinterpretar datas históricas. Horário de dispositivo não autoriza direitos; fallback e qualidade temporal têm tratamento OQ-03/S08. Dados cujo dia não puder ser determinado com confiança não inventam novo dia. CAD7/30 ≥2 dias, CoreUsers ≥1, D7 dia exato; demo não conta. Activation72h usa novos e coorte maturada; trial_to_paid só trials maduros; mensal/trimestral/legacy/promo separados. Cliques sem identidade única são cliques, não usuários. Não emitir financial event via cliente/Clarity.

### INV-18 — Versionamento e execução

Mudanças futuras em enum/campo/semântica exigem revisão e atualização conjunta dos companions/fixtures. IDs comerciais propostos só viram configuração real após OQ-05/Console; cutoff após ponte, evidência e aprovação. Fonte/data de decisão permanecem auditadas. No :app atual não existe API/ledger para compatibilizar; aliases aqui são documentação de leitura do PRD, não handlers legados a manter. S01 não habilita execução/flags automaticamente.

## Bloqueios que não foram decididos pela S01

| Item | Continua aberto | Bloqueio específico |
|---|---|---|
| OQ-01 | provider, colisão/linking/ownership | S04/S09; regra métrica de união usa somente mapa legítimo quando fornecido. |
| OQ-02 | prova/elegibilidade, baseline Core, provisório/owner, janela/cutoff | S05/S06/S16 e produção comercial; campos congelados não escolhem dados. |
| OQ-03 | tolerância, reboot, relógio, política finita | S08/S25/S27; sem valor default. |
| OQ-04 | origem oficial de primeira cobrança/ordens/void/latência e chave econômica comprovável | S21/S22/S39/S46; schemas não provam dinheiro. |
| OQ-05 | catálogo/ofertas, replacement/proration, defer real | S18/S19/S26/S41; IDs propostos não foram conferidos. |
| OQ-06 | refund window, antifraude/rate limits/rotação, resgate e payout | S28/S39/S42/S46/S47; sem usar 90d ou janela arbitrária. |
| OQ-07 | retention/deleção e owners/suporte | S14/S15/financeiro/gates; grants/ledger não são apagados por decisão local. |
| OQ-09 | baseline/limites/amostra/matriz e GO externo | S12/S27; ≥20% relativo continua sinal proposto de HOLD, não auto-gate estatístico. |
| OQ-10 | entrada segura/recent offer/sinais disponíveis | S08/S24/S31; estado incerto exige silêncio, sem sensores novos. |
| OQ-11 | utilidade/semana/distribuição Pro Insights | S37/S38 e reward legado; não bloqueia Billing de novos. |
| OQ-12 | variantes de paywall/card/review cadência | S24/S50/S51; referência local não decide estados comerciais faltantes. |
| AR-01 | projeto/backend/runtime/credenciais/Console/domínio/certificado | S02/S03/S04 e integrações; nenhuma escolha/habilitação externa feita. |
| AR-02 | fontes/meio de analytics/relatório e escopo iPhone separado | S34–S36/S48/S52; não criar BI/waitlist antecipados. |

**OQ-08 resolvida em D01–D12.** Próximas stories têm fronteiras documentadas, mas não estão universalmente prontas: S02 exige AR-01; S04 exige OQ-01/AR-01; S10 pode preparar dedupe/fixtures após identidade S04; catálogo S18 depende S03/S04/OQ-05; S28 depende S02/S04/OQ-06. Stories posteriores consomem esses contratos na ordem do plano. Nenhuma foi iniciada.

## Rastreabilidade dos requisitos do PRD

O contrato ou bloqueio abaixo preserva cada requisito; sua implementação/validação real pertence às stories apontadas no backlog. Não afirmar fechamento de requisito de produto por concluir a S01.

| Requisitos | Contrato/invariante ou dependência explicitamente preservada |
|---|---|
| ENT-001, ENT-002, ENT-004 | Identity/LegacyGrant/LegacyClaim; INV-01/02; OQ-01/02 e recuperação S04–S09. |
| ENT-003 | EntitlementSnapshot/Cache, SubscriptionState; INV-03/04; OQ-03/10. |
| BILL-001, BILL-002, BILL-003, BILL-007 | Matriz UI/readiness/catálogo/trial; INV-05/06; ProductDetails e SDK/IDs reais pendentes S03/S18. |
| BILL-004, BILL-005, BILL-006, BILL-008 | VerificationResult/SubscriptionProjection/PurchaseEvent, OpenAPI; INV-06/07; lifecycle/ACK/restore/change/refunds S19–S27. |
| ATTR-001, ATTR-002, REF-001 | Attribution/AttributionView, claim/lock, link canônico/gesto; INV-08/16 e S28–S33. |
| REF-002, REF-003 | ReferralQualification/RewardLedger/Summary; INV-09/10, marcos/void/dedupe/janela OQ-06. |
| REF-004, REF-005 | Ledger/RewardView/PromoGrant, intenção redeem/internal apply; INV-11/12, elegibilidade OQ-05/termos OQ-06/Pro legado OQ-11. |
| AFF-001, OPS-003 | CommissionLedger/CreatorSummary, settle service; INV-13/14 e contratos/custos reais OQ-04/06/AR-02. |
| GROW-001, GROW-002, GROW-003 | Política/caps/silêncio/gesto/eventos/review separados; INV-16 e OQ-10/12. Nenhum cartão/review implementado. |
| DATA-001, DATA-002, DATA-003 | CoreAnalysisReceipt/AnalyticsEnvelope/dicionário/coortes/PurchaseEvent; INV-07/17 e prova/exports S21/AR-02. |
| OPS-001 | subscriptionRequired/sources/REWARDS_PAUSED; INV-15/18; flags/buckets/rollback S13. |
| OPS-002, CORE-002 | Compatibilidade/falha sanitizada/sem bypass, gate Console/versionamento; INV-04/18 e S03/S11/S12/OQ-09. |
| PRIV-001 | Views sem terceiros/closed request schemas, authority/retention; INV-01/14 e OQ-07/AR-01. |
| CORE-001 | Demo event/estados UI/fluxo trial; INV-05/17. |
| ASO-001 | Uma screenshot/hipótese ≥7d e aquisição qualificada sem promessa falsa; dicionário/matriz/teste futuro S52, não nova infraestrutura. |
| NFR-001, NFR-003 | Baseline/tolerância e crash/ANR permanecem OQ-09; regressão pertinente S12/S27, sem refactor do Core. |
| NFR-002, NFR-005 | Snapshot RAM/fallback finito/direitos independentes; INV-03/04, OQ-03 e testes futuros S08. |
| NFR-004, NFR-006 | Unique keys/caller binding/ledger/reserva/efeito externo/receipts; INV-07–13 e fixtures. |
| NFR-007 | Minimização/sanitização/views/retention; INV-14/17 e OQ-07. |
| NFR-008 | Defaults OFF/obrigações continuam/bucket estável; INV-15. |
| NFR-009 | Compatibilidade Android existente preservada; matriz/runtime/libs S03/S12/S18, sem reduzir minSdk 24. |
| NFR-010 | UI states e critérios de acessibilidade previstos no backlog; screenshots/TalkBack nas stories de UI, não em S01 documental. |
