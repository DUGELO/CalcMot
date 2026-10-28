# OQ-03 / OQ-10 — Política aprovada e fronteiras técnicas

Formalização: **2026-10-14**, por decisão expressa do proprietário nesta conversa. Substitui as recomendações comerciais ainda abertas da [proposta anterior](OQ-03-OQ-10-OFFLINE-SAFE-ENTRY-PROPOSAL.md). Não altera os sete contratos S01, PRD-fonte ou elegibilidade legado. **Aprovação de política não é ativação de cobrança/gates, deploy ou S09.**

## OQ-03 aprovada

| Fonte | Limite aprovado | Origem da recência |
|---|---|---|
| Play PAID_PERIOD | Até 72h, nunca além de `validUntil` | Última verificação autoritativa Play: `play.checkedAt`. |
| Play TRIAL | Até 24h, nunca além de `validUntil` | Última verificação autoritativa: `play.checkedAt`. |
| Promo ACTIVE | Até 24h, nunca além de `validUntil`, sem antecipar início | Confirmação confiável da **fonte promocional**, vinculada ao projeto/UID e ao mesmo período. |
| Legacy Core confirmado | Permanente, sem expiração comercial | Grant já autoritativo; rede, plano e boot não revogam direito. |
| Revisão assistida em análise | **Sem acesso provisório ativado** | Uma data de provisório em fixture/snapshot não ativa direito local. |

Reboot/perda de âncora monotônica exige revalidação para novos acessos temporários. Não substituir pelo relógio civil, nem acrescentar dias ao período comprado. UNKNOWN não é PAID_PERIOD nem confirmação de trial; estados Play que não concedem direito não são promovidos a ACTIVE. Fontes continuam independentes.

Limite local = menor entre expiração e recência autoritativa + janela da fonte/fase. Atualizar a UI ou retornar um GET recente não revalida uma assinatura Play, não renova trial e não prova pagamento.

### Código implementado

`OfflinePolicy.APPROVED_V1` (`OQ03-V1-72H-24H`) tem limites **simultâneos** de paid, trial e promo. `playAge(billingPhase)` escolhe a fase, sem dependência de configuração global para todos os assinantes. Adapter Android usa essa política para o read model, mas não está instanciado no Application publicado e nenhum gate comercial a consome.

Review passa a `DISABLED`, mesmo com `provisionalUntil` no snapshot. O servidor S01 permanece preservado; a aprovação atual impede ativação desse provisório no cliente. Legado permanece independente da política temporal.

### Semântica de promo — resolvida no consumidor, integração pendente

**Não usar** `checkedAt` raiz, hora civil, revisão nova, UID declarado, alteração de outra fonte ou simples hora do GET como prova de verificação promocional. A alternativa de inferir verificação a cada GET da proposta anterior **não foi ativada**.

`PromoVerifier` é um port privado necessário para receber uma confirmação de fonte confiável, separada da projeção S07. `PromoVerification` precisa corresponder ao owner do token, estado e início/fim daquela fonte e ter instante válido não futuro. Não cria promoção ou período: só acrescenta recência a uma promoção já presente no snapshot autenticado. Recibo de outro titular/período/data futura não renova acesso.

Timestamp aceito fica em metadado local HMAC `promoVerifiedAt`, **fora do objeto EntitlementCache/Snapshot congelado**. Cache versão 2 mantém leitura de versão 1; antigos arquivos não recebem uma verificação fictícia. Alteração do período/estado limpa a recência; GET posterior conserva apenas a confirmação antiga da mesma fonte e nunca a atualiza sozinho. Falha do verificador conserva somente a confirmação anterior ainda válida/finita.

**Bloqueio operacional específico:** S07 lê uma projeção privada, sem fonte promocional independente com verificador/protocolo de recência implementado. Não há adapter real autorizado para `PromoVerifier`, que permanece ausente no Android. Resultado de promo sem essa prova é `REVALIDATION_REQUIRED`, sem Pro novo; Core legado/atualmente gratuito permanecem. Implementar emissor/verificador real de promo agora anteciparia stories comerciais fora do escopo. Nenhum header ou endpoint novo foi inventado para alterar o contrato.

Uma fonte futura deverá entregar prova por canal autenticado/ambiente confiável e incluir titular, período e verificação; contrato/proveniência requer revisão própria. O port interno e fixtures **não** são assinatura de servidor nem mecanismo antifraude contra APK modificado/root. Não entregar timestamps de cliente a esse port como se fossem autoridade.

## OQ-10 aprovada

- Pendência comercial somente em interação voluntária com a interface principal.
- ON_RESUME, toggle desligado, IDLE e ausência de oferta isolados não qualificam uma entrada; não comprovam veículo parado.
- Exigir confirmação consistente de fim de sessão **e processamento concluído**.
- Dados ausentes/contraditórios adiam apresentação; análise/OCR/Accessibility/overlay nunca são interrompidos para cobrança.
- Adiamento de apresentação não prorroga acesso temporário vencido.
- **Gates comerciais OFF até validação específica.**

### Observação preparada sem alteração funcional do Core

`SafeEntryObservation` publica StateFlow de uma observação **inteira e imutável**: sessão, geração, pedido de término, geração confirmada de conclusão, trabalho em trânsito, overlay e saúde do produtor. Início desconhecido não é IDLE seguro. Qualificação exige geração corrente, término pedido/confirmado na mesma geração, zero trabalho em trânsito, overlay ausente e produtor saudável.

Troca de sessão, nova geração, invalidação/desconexão e regressão invalidam intenção capturada; gerações retiradas não voltam a qualificar por replay. A intenção também exige `VOLUNTARY_MAIN_ACTION` e foco. Isso é prontidão de observação, não autorização de UI: `commercialGatesEnabled=false` e não há comando de apresentação, ocultação de overlay, cancelamento ou rede.

**Bloqueio técnico de integração:** o runtime existente não fornece ack de drenagem/fim de sessão nem geração transacional em todo o trabalho assíncrono. Nenhum produtor real foi conectado ao observador. Produzir esse ack exige acompanhar início/fim de trabalho e lifetime do serviço/pipeline crítico; observar apenas Snapshot/IDLE/toggle não basta. Conforme a restrição desta execução, não adicionamos contadores/hooks ao pipeline nem inventamos sinais. Uma integração futura deve ser especificamente autorizada, observacional, validar concorrência com nova oferta e garantir que a decisão de apresentação não use estado já invalidado.

Enquanto isso, observador sem produtor permanece UNKNOWN e apresentação permanece OFF. Expiração de direitos temporários segue o timer do repositório, independentemente do observador; nenhuma prorrogação comercial, fim automático de condução ou timeout novo foi definido. Não é necessário reabrir as decisões comerciais aprovadas para registrar esses bloqueios técnicos.

## Validação desta execução

Alterações de comportamento justificam reexecução dos testes **somente de entitlement/entrada segura**: limites simultâneos pago/trial, ausência/recibo válido de promo, GET sem revalidação Play/promo, revisão desativada, reboot/perda de âncora, expiração, recuperação, identidade e concorrência durante verificação. Não repetir toda a suite de 299 nem testes backend sem mudança.

Samsung DEV conectado: executar os testes instrumentados atualizados, incluindo leitura de envelope v1/v2, integridade, boot persistido divergente, validade expirada e troca/recuperação de UID. **Boot divergente é fixture controlada em hardware real**, não reinicialização física do aparelho nem evidência de Play/Google real. Não tocar no pacote de produção, não criar direito comercial para testes.

Resultados finais, arquivos e status registrados no [relatório S08](S08-ENTITLEMENT-CACHE.md). S08 permanece PARTIAL até prova de integração segura/restore e sinal de término real; política comercial está aprovada, não pendente de aprovação. Fontes financeiras/promocionais reais, UI de recuperação S09 e ativação comercial permanecem nas respectivas stories, sem antecipação aqui.

**Validação final:** 47 testes JVM específicos PASS; 2 instrumentados no Samsung PASS. O boot no ensaio é divergência controlada de âncora persistida, sem reboot físico. Sem teste de fonte promocional real ou produtor de ack ausentes; não transformar fixtures em evidência de operação com usuário real.
