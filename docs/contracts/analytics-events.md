# Analytics — dicionário canônico v1.0.0

Fonte: PRD §§8/16/16.1/16.2, [README](README.md) INV-07/14/16/17. Eventos financeiros não são prova independente: ledger usa evidência oficial server-side. Emitir o nome canônico **uma vez**, nunca o alias junto.

## Envelope, payload e código existente

`entitlement.schema.json#/$defs/AnalyticsEnvelope` é contrato de registro/validação: `name`, `emitter`, `occurredAt` UTC, `dedupeKey` privado e `params`. `dedupeKey`/Identity/CoreAnalysisReceipt não são payload GA4/Clarity. Cliente envia somente nome + params minimizados autorizados pelo dicionário; campos técnicos de ledger/correlação permanecem na origem protegida.

Params congelados permitidos: `app_family`, `android_api`, `app_version_bucket`, `reason` (código sanitizado, não mensagem livre), `classification`, `plan`, `source_kind`, `method`, `subscription_state`, `billing_phase`, `reward_state`, `milestone`, `delta_days`, `prompt_kind`. São opcionais e não autorizam coleta automática de todos. Valores/enums estão em `$defs/AnalyticsParams`; razões/códigos novos são definidos por story/contrato de erro, não OCR livre. Identificadores comerciais/campaign não são necessários em payload para determinar remuneração: joins protegidos/exports agregados ficam backend conforme AR-02.

Nunca params com uid/inviteeUid/sourceId/analysisId/dedupeKey/operationId/purchaseToken/tokenHash/evidenceHash/orderRef/paymentRef, contato, nome, telefone, endereço, coordenada, OCR/frame/screenshot ou conta pessoal identificável. Progresso creator/driver só agregado. Valor/receita/moeda/ordem ficam em PurchaseEvent/ledger, não são enviados automaticamente a analytics cliente. SDK financeiro/Clarity não pode emitir eventos canônicos BACKEND.

Inspeção local: `telemetry/AnalyticsEvents.kt` e `AnalyticsParams.kt` possuem allowlists; `FirebaseAnalyticsTracker.kt` usa `SafeTelemetryPolicy`, throttling e encaminha eventos também à `ClarityIntegration`. Isso **não** implementa dedupe lógico, vínculo UID ou autoridade financeira. S10/S14 devem estender apenas a allowlist necessária e manter sanitização/roteamento correto; não encaminhar registros financeiros/private envelopes por esse tracker. Os eventos diagnósticos já existentes são preservados sem refatorar pipeline ou criar uma segunda stack.

## Eventos canônicos novos/normalizados

| Evento | Emissor | Chave privada/semântica | Requisitos |
|---|---|---|---|
| first_open | CLIENT (SDK) | Primeiro open da identidade/coorte; evento SDK, não emitir uma cópia manual. Linking legítimo não cria novo usuário para métricas. | DATA-002 |
| setup_ready | CLIENT | Primeiro readiness confirmado por identidade, setup gratuito. | BILL-002/CORE-001/DATA-002 |
| core_demo_completed | CLIENT | Conclusão de simulação; separada de todos os Core real events. | CORE-001 |
| core_analysis_completed | CLIENT | identityKey + analysisId opaco do resultado lógico concluído; não cada frame. Recibo/dedupe local, sem backend de corrida individual. | DATA-001 |
| first_core_value | CLIENT | Uma vez por identidade consolidada; primeira análise real. | DATA-001/002 |
| core_active_day | CLIENT | identityKey + data local original; uma vez por dia, set persistente sem conteúdo de oferta. | DATA-001/002 |
| core_extraction_succeeded | CLIENT | Resultado lógico de extração compatível; não prova pagamento nem conta CAD sozinho. | CORE-002 |
| core_extraction_failed | CLIENT | Tentativa/motivo técnico sanitizado + app/API/bucket de versão; não sucesso fictício. | CORE-002 |
| trial_started_verified | BACKEND | Assinatura/início trial confirmado; dedupe de transição econômica, não tap de checkout. | BILL-002/004/005 |
| first_charge_verified | BACKEND | Primeira transação paga do novo titular, economicPurchaseKey; ACTIVE/token não bastam. | DATA-003/REF-002 |
| renewal_charge_verified | BACKEND | Cada renovação paga, economicPurchaseKey distinto; não derivar de calendário. | DATA-003 |
| first_renewal_verified | BACKEND | Somente a primeira renewal_charge paga de titular/cadeia; milestone creator usa uma vez. Não somar este evento a todas-renovações como outro pagamento. | DATA-003/AFF-001 |
| refund_confirmed | BACKEND | Reversão/refund/void oficial reconciliado e sua referência econômica; histórico preservado. | DATA-003 |
| subscription_state_changed | BACKEND | Assinatura + revisão/transição confirmada, não tipo RTDN isolado. | BILL-005/008 |
| subscription_grace_entered | BACKEND | Entrada GRACE confirmada/transição única. | BILL-008 |
| subscription_hold_entered | BACKEND | Entrada ON_HOLD efetiva confirmada. | BILL-008 |
| subscription_recovered | BACKEND | Recuperação confirmada da situação de pagamento/acesso; não callback de cliente. | BILL-008 |
| referral_prompt_eligible | CLIENT | Avaliação positiva no contexto seguro; dedupe pela avaliação/contexto, sem nova quota temporal inventada. Denominador distinto de shown. | GROW-001 |
| referral_prompt_shown | CLIENT | Instância de prompt realmente exibido, incrementa cap. | GROW-001 |
| referral_prompt_dismissed | CLIENT | Dispensa dessa instância, aplica cooldown/duas dispensas. | GROW-001 |
| referral_share_tap | CLIENT | Gesto voluntário de compartilhar; não envio. | REF-001 |
| share_sheet_opened | CLIENT | Sharesheet efetivamente aberto; cancelamento/sem destinatário não comprovam envio. | REF-001 |
| referral_link_clicked | BACKEND | Recepção do link no domínio; clique, não usuário único deduplicado sem identificador nem instalação. | ATTR-001 |
| attribution_locked | BACKEND | Uma origem locked por identidade; lock orgânico/unknown também é estado válido. | ATTR-002 |
| referred_first_core | BACKEND | Projeção de primeiro Core cliente + origem válida, uma vez; não ganha autoridade financeira. Transporte/suporte do join depende AR-02/S30/S35. | DATA-002 |
| referral_qualified | BACKEND | invitee + campaign + firstCharge válida/janela; creator/driver exclusivos. | REF-002 |
| reward_applied | BACKEND | rewardId/operação confirmada e delta real; timeout/EARNED não emite. | REF-003–005 |
| creator_first_paid_eligible | BACKEND | creator + invitee + FIRST_CHARGE; comissão elegível, não necessariamente settled. | AFF-001/OPS-003 |
| creator_first_renewal_eligible | BACKEND | creator + invitee + FIRST_RENEWAL, após ciclo real. | AFF-001/OPS-003 |
| review_flow_requested | CLIENT | Tentativa legítima do fluxo Play Review; sem nota/envio retornado. | GROW-003 |

São **30 nomes** no enum AnalyticsEnvelope. Emissores restritos pelo schema. A semântica/dedupe também é obrigatória; não logar evento por poder passar schema.

## Aliases de leitura do PRD

| Nome não canônico | Nome/condição canônica |
|---|---|
| paid_first_verified / first_paid_verified | first_charge_verified; não emitir ambos. |
| growth_prompt_shown | referral_prompt_shown se prompt referral; review continua review_flow_requested. |
| growth_prompt_eligible | referral_prompt_eligible no contexto referral. |
| hold_entered | subscription_hold_entered. |
| recovered | subscription_recovered quando confirmação backend. |
| creator_commission_eligible | creator_first_paid_eligible ou creator_first_renewal_eligible conforme etapa, nunca genérico duplicado. |
| FIRST_PAID_VERIFIED (máquina de Referral) | Estado de domínio; não um segundo evento client/server. |
| subscription_renewed em Analytics cliente | Não é prova financeira nem alias autorizado para renewal_charge_verified. |

`renewal_charge_verified` e `first_renewal_verified` são eventos **distintos**, todos versus primeiro; não são aliases. `refund_confirmed` é reversão, não exclusão retroativa de evento de pagamento. `review_submitted`, `share_sent`, `trial_started` inferido de instalação ou `reward_applied` inferido de clique não são eventos autorizados deste contrato.

## Métrica/dia e fronteira lógica (OQ-08 resolvida)

Uma análise identifica uma conclusão lógica real aceita pelo Core atual; reutilizar analysisId para retry, múltiplos frames, TTL/update do mesmo resultado. Nova oferta aceita/concluída recebe ID novo, mesmo com mesmos preços/distâncias. Não usar fingerprint bruto de OCR/corrida como identificador exportado, nem introduzir janela numérica para diferenciar ofertas. O seam deve ser verificável com regressão em S10. IDs locais são efêmeros/conjunto limitado conforme retention OQ-07; não construir histórico backend de ofertas.

Capturar instante UTC/data local/timezone do sucesso original. Preservar `(identityKey, YYYY-MM-DD)` de cada dia; evento repetido/relógio/timezone alterado não reinterpreta análise histórica. Mudança de timezone vale para conclusões futuras cujo dia seja determinável, não reenvio de antigos resultados. Linking legítimo mapeado por S04 une IDs/sets de datas e primeira abertura/primeiro valor por instante mais antigo, uma vez. Política de colisão/ownership permanece OQ-01; não fundir clientes pelo Analytics.

| Métrica | Regra congelada |
|---|---|
| CAD7 / CAD30 | Identidades distintas com análise lógica real em ≥2 datas locais na janela de 7/30 dias; dedupe de dias, exclui demo/frames. |
| CoreUsers7 / CoreUsers30 | ≥1 data na mesma janela; não confundir com CAD. |
| Activation24h/72h | first_open→setup_ready→first_core_value na mesma identidade/coorte nova elegível, prazo desde abertura; para taxa72h só coorte observável/maturada ≥72h. |
| Core D1/D7/D30 | Diferença calendárica de 1/7/30 dias entre data local original de first_open e data local original de Core. Métricas rolling/7-day-window e recorrência M0/M1 separadas. Não recalcular datas históricas após timezone/linking. |
| K_active | Novos atribuídos que chegaram à primeira análise / usuários elegíveis para indicação no período; janela 30d. |
| Trial→paid | Trials que maturaram para primeira cobrança; numerador com first_charge_verified oficial, não todos os trials recentes. |
| Primeira renovação / paid_M2/M3 | Respeitar ciclo da Play: mensal e trimestral separados, sem converter trimestre em M2 civil. |
| Receita / CAC | Ordens/repasse/custos conciliados; mensal equivalente distinto de caixa trimestral R$25; CAC cash e econômico separados. |

Coorte imatura/dado indisponível ≠0%. Não somar GA4 e Play como mesma população nem inferir conversão paga de retenção grátis. Não afirmar causalidade/significância com amostra pequena/versões e aquisição diferentes. Aliases `CAD30`/`activation72h` são métricas, não novos nomes de eventos.

No tracker existente `platform` pode alimentar app_family na projeção analítica canônica; não enviar os dois para fabricar duas observações. `offer_parsed`, `overlay_shown`, OCR success e throttling diagnóstico existentes não equivalem a análise lógica. Allowlist dos eventos existentes permanece inalterada em S01. Extensão/integração é S10, não feita aqui.
