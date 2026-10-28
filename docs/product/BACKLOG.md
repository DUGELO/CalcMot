# Backlog de engenharia — CalcMot Growth & Monetization V1

Data: 2026-10-09. Branch examinada: `growth`, HEAD `9d78bd76b3a373f139a8360a6e527fa35e7ec0fa`.

Fonte de verdade: `C:\Users\426174\Downloads\CalcMot_PRD_v5_Final.md`, v5.0, corte de evidências 08/10/2026, lido integralmente (926 linhas). SHA-256: `7CD6D94723F381B69B71413EEEFFA4287DB0A4CE032DD28017031A5C3FEFE221`.

Este backlog contém **12 epics e 52 stories**. A decomposição original foi documental, sem implementação. S01 foi executada como contratos documentais e S03 encerrada como auditoria com prontidão HOLD: [relatório](S03-PLATFORM-AUDIT.md). Em 2026-10-10, **S02/S04 receberam implementação local parcial**, por autorização específica: [resultado e pendências](S02-S04-INTEGRATION.md), [arquitetura gratuita aprovada](ADR-001-FREE-IDENTITY-BACKEND.md). Nenhuma ativação externa/deploy; S05–S52 não foram iniciadas. Os exemplos/comandos do PRD não autorizam avanço automático. Plano: [EXECUTION-PLAN.md](EXECUTION-PLAN.md).

## Checagem de implementabilidade

Há lacunas localizadas; elas não impedem criar o backlog nem preparar incrementos sem cobrança. Cada item abaixo identifica a decisão/evidência necessária e as stories afetadas. Não atribuir valores comerciais, prazos ou regras de identidade silenciosamente. As perguntas não são novos requisitos.

| ID / tipo | Problema real e decisão/evidência pendente | Impacto |
|---|---|---|
| OQ-01 `OPEN_QUESTION` — parcialmente respondida | Em 10/10 o usuário aprovou Google Sign-In/Firebase Auth/Credential Manager. S04 conserva UID via linking/reautenticação e recusa colisões sem merge/transferência por email. Permanece definir suporte/ownership para recuperação sem acesso ao Google ou perda de conta não vinculada; OAuth/device ainda requerem prova real. PRD §§3.2/9.1; ADR-001. | S04/S09; provedor não deve ser perguntado novamente, recuperação excepcional permanece pendente. |
| OQ-02 `RESOLVED` comercialmente / operação `OPEN` | Proprietário aprovou: quem utilizou até 3.5/versionCode 12 inclusive preserva permanentemente o gratuito dessa versão; novos da 3.6+ não recebem legado automático. Google só vincula/recupera. Cadastro na ponte, versão local, UID e timestamp não provam história. [Política, inventário e pendências operacionais](OQ-02-LEGACY-POLICY.md). Não substituir a regra por cutoff de calendário ou ampliar a todos os cadastrados. | S05/S06/S16: fontes/revisão/ownership/provisório e migração real ainda precisam de fechamento operacional, sem reabrir elegibilidade. |
| OQ-03 `APPROVED_POLICY` | Pago 72h desde Play verificado; trial 24h; promo 24h desde fonte confiável, sempre limitados a expiry; perda de âncora exige revalidação; Legacy Core permanente; sem provisório de review. [Decisão](OQ-03-OQ-10-APPROVED-POLICY.md). | S08/S25/S27; integração real da fonte promo e validação operacional pendentes, não decisão comercial. |
| OQ-04 `OPEN_QUESTION` | Qual API/ordem/relatório oficial acessível à conta comprova primeira cobrança efetivamente paga/não anulada, renovação e void? Qual atraso e critério de liquidação? `ACTIVE`, token e RTDN não bastam. §§10.1/11.5. | S21 é a prova necessária; S22/S39/S46 e ativação financeira ficam bloqueadas sem ela. |
| OQ-05 `OPEN_QUESTION` | Confirmar IDs Play propostos, disponibilidade real de ofertas, modalidades elegíveis a defer e regra comercial de troca mensal↔trimestral/replacement/proration. Não presumir elegibilidade ou endpoint vigente. §§4/11.1/11.5. | S18/S19/S26/S41 e release C/D. |
| OQ-06 `OPEN_QUESTION` | Definir janela refund/void para qualificação, reversões tardias, critérios de fraude/revisão/rate limits, validade/rotação de códigos, resgate e crédito não aplicado. Os 90 dias de resgate são proposta, não decisão. Prazo de payout e controles de lote dependem de termos/contrato. §§5/6/10/11.5/18. | S28/S39/S42/S46/S47; preparar motor com parâmetros não habilita campanha. |
| OQ-07 `OPEN_QUESTION` | Quais retenções/exclusões por categoria, tratamento dos registros financeiros e grants duráveis na exclusão de conta, consentimentos e owner de suporte/incidente? Não escolher prazos legais ou apagar ledger indiscriminadamente. §§10/13/17. | S14/S15 e extensão da política nas stories financeiras/gates. |
| OQ-08 `RESOLVED` | Resolvida pela S01 em [docs/contracts/README.md](../contracts/README.md), D01–D12: rota `/v1/me/entitlement`, `api.openapi.yaml`, estados/aliases/eventos, análise lógica/data local/dedupe de linking legítimo e intenção autenticada de resgate. | Contratos v1.0.0 congelados; policy de ownership, ambiente e parâmetros comerciais de outras OQs permanecem abertos. |
| OQ-09 `OPEN_QUESTION` | Qual baseline, tolerância para piora “material/relevante” de p95/crash/ANR e cobertura viável de versões anteriores/aparelhos? Limiares de HOLD e amostra/owners de GO não estão fechados; ≥20% relativo é sinal proposto de investigação. §§8.1/13/16.2/17. | S12/S27; não fabricar limites ou cobertura verde. |
| OQ-10 `APPROVED_POLICY` | Ação voluntária na Main + fim de sessão/processamento confirmado; ON_RESUME/IDLE/toggle/ausência de oferta não bastam; desconhecido/contraditório adia UI sem prorrogar direito; gates OFF até validação. [Decisão](OQ-03-OQ-10-APPROVED-POLICY.md). | S08/S24/S31; observador preparado, produtor de ack e integração ao runtime pendentes. |
| OQ-11 `OPEN_QUESTION` | Como validar intenção/utilidade de Pro Insights e qual recorte semanal/distribuição de indicadores? Só o painel descrito no §5.3; sem inventar outras features ou métricas de economia. | S37/S38 e benefício ao legado em S44; não bloqueia assinatura de novos. |
| OQ-12 `OPEN_QUESTION` | Conteúdo exato do cartão e ocasião/cadência de In-App Review não estão fechados. O protótipo de paywall cobre mensal antigo, não as variantes trial/trimestral/erro: delta visual/copy precisa ser revisado antes de wiring. §§4.2/7/13/14. | S24/S50/S51; manter os comportamentos obrigatórios do PRD. |
| AR-01 `ASSUMPTION_REQUIRED` — parcialmente evidenciada | S03 comprovou Firebase `calcmot`/Spark e assinatura Play. Em 10/10 o usuário aprovou Workers Free/TypeScript+D1; fontes agora em `backend/`, Auth Android em `identity/`, sem deploy. Ambientes reais, acesso Cloudflare/bindings, OAuth/SHA-1 Play, hosting e acessos Play/PubSub continuam pendentes conforme S03/ADR-001. Não retornar a Functions/Run pagos como default. | S02/S04 e integrações/gates externos; configuração local não comprova produção pronta. |
| AR-02 `ASSUMPTION_REQUIRED` | Confirmar acesso e meio de relatório semanal (GA4/exports/consulta e dados Play/custos). Existe pasta de métricas, mas disponibilidade do export/BigQuery não foi validada. iPhone/waitlist pertence a esquema separado não especificado. | S34–S36/S48/S52; não construir plataforma de BI ou funil iOS nesta V1. |

**Como avançar:** registrar respostas e evidências no contrato correspondente da S01 e na story de domínio. Uma lacuna bloqueia sua decisão/ativação, não todas as outras stories. Sandbox, fixture e dry-run não substituem gate de produção. O caminho de prova financeira deve ser investigado cedo; não deixar OQ-04 para depois do checkout.

## Contexto local e referência de paywall

O projeto inclui apenas `:app` em `settings.gradle.kts`. Possui Compose/Material 3, DS em `ui/design/`, Firebase Analytics/Crashlytics em `telemetry/`, Clarity em `analytics/`, runtime em `ReadingPipelineRuntime.kt`, testes de produção e gravação em `securityrecording/`. `app/build.gradle.kts` declara `br.com.calcmot`, minSdk 24 e targetSdk 36; isso não prova conformidade atual do Console. Não reduzir suporte existente por causa do mínimo Android 10+ citado no PRD.

`PremiumScreen.kt` contém preço estático R$9,90 e benefícios antigos. Sua existência não comprova Billing nem rota comercial operacional. Novas verticais ficam em **pacotes no :app**, conforme §9, sem modularização Gradle/refatoração geral. Reutilizar `telemetry/` e `analytics/` existentes; não criar uma segunda stack analítica apenas pelo nome `data/analytics/` ilustrado no PRD.

Não há convenção central de backlog. Mantém-se a convenção dos planos em `docs/camera/`: IDs explícitos, campos por história, dependências, critérios e evidências. Usa-se `docs/product/` conforme solicitado; o planejamento da câmera é de outra feature e não define o escopo desta.

Protótipo localizado e aberto: [03-premium.png](../design/references/calcmot-prototypes-2026/03-premium.png), documentado no [índice visual](../design/references/calcmot-prototypes-2026/README.md). Há cópia em `.tmp/epic1-final-build/` e preview em `.tmp/screens/`; não são especificações independentes.

Leitura visual: imagem 863×1822, retrato; topbar com voltar/“Premium”, hero de marca, título branco/verde e explicação centralizada; um card grande escuro com borda e acento verde, preço mensal e quatro linhas de benefícios; CTA azul largo, “Agora não”, proteção viária no rodapé; fundo preto azulado, raios generosos, status/navigation bars escuras. **Preservar identidade e hierarquia em Compose nativo.** O R$9,90, plano único e promessa de continuar Core não prevalecem sobre v5: preços via Play, mensal/trimestral, trial voluntário, disclosure e Core legado preservado. S24 documenta o delta, sem vender ao legado o Core que já possui. Sem screenshot inteira clicável, cards dentro de cards ou benefícios Pro fictícios.

## Capabilities → epics

| Capability | Resultado do PRD | Epics | Requisitos principais |
|---|---|---|---|
| C01 — Contratos e integrações mínimas (fundação) | Fronteiras versionadas, operações autenticadas e publicação viável | E01 | §§8–11/15; OPS-002, NFR-004/006/007 |
| C02 — Identidade e Core preservado | Direito antigo recuperável e acesso local seguro | E02/E04 | ENT-001–004, OPS-001, PRIV-001 |
| C03 — Core mensurável e compatível | Uso lógico confiável, falha segura e coortes | E03/E07 | DATA-001/002, CORE-002, NFR-001–003/009 |
| C04 — Experimentar e assinar voluntariamente | Setup/demo grátis, Play verificado, recuperação e piloto | E05 | CORE-001, BILL-001–008, DATA-003 |
| C05 — Distribuição orgânica consentida | Origem atribuível, link e compartilhamento seguro | E06 | ATTR-001/002, REF-001, GROW-001 |
| C06 — Benefício adicional real (condicional) | Insights semanais sem retirar Core legado | E08 | §5.3; REF-005 quando disponível |
| C07 — Recompensa conferível | Primeiro pagante real, 7/30/60 total, defer/promo sem duplicação | E09 | REF-002–005, NFR-004/006 |
| C08 — Creator remunerado por compra | R$3 + R$1 conciliados, custo/coortes sem PII | E10/E07 | AFF-001, OPS-003, DATA-002/003 |
| C09 — Divulgação/review honestos | Cartão local e review independente de incentivo | E11 | GROW-002/003 |
| C10 — Aquisição pela loja | Uma hipótese visual de ASO por vez | E12 | ASO-001 |

## Regras de execução comuns

- **Tipos:** fundação, produto ou gate operacional; gates são evidências/aprovações externas e podem exceder uma sessão. Cada story técnica tem um incremento isoladamente demonstrável e seus próprios testes. Não criar infraestrutura genérica para implementar este backlog.
- **Arquivos:** `APP` = `app/src/main/java/br/com/calcmot/`; `UI` = `APP/ui/`; `TEL` = `APP/telemetry/`; testes Android = `app/src/test/` e `app/src/androidTest/`. `BACKEND` e `WEB` são destinos a confirmar em AR-01, inexistentes no checkout; não nomes de módulos ou arquivos já encontrados. Pacotes novos são explicitamente marcados como previstos. Registrar os caminhos efetivos antes de codificar.
- **UI:** reutilizar DS nativo, toques ≥48dp, TalkBack, telas pequenas/altas, font scale e landscape quando aplicável. Preview e screenshot de device/emulador do componente real; guardar comparação em `.tmp/screens/`. Ao usar imagem, observá-la antes e validar delta aprovado. Não presumir infraestrutura de screenshot automatizado por existir teste de UI.
- **Integração:** lint, compilação, unitários e instrumentados pertinentes por story; fixtures sintéticas para lifecycle/eventos, sem framework de simulação novo. Em build de entrega, `assembleRelease` e `bundleRelease`. Lógica legacy/cobrança/recompensa exige dupla revisão prevista no PRD. Não habilitar flags de produção por fechar story técnica.
- **Contratos após lançamento:** IDs de produtos/eventos, schemas de ledger, regras de pagamento e política/cutoff são versionados. Mudança exige revisão/migração apropriada; nunca editar retroativamente o registro de direito ou origem remunerada.
- **Fronteiras:** não reformular OCR/Accessibility/parser/semáforo/overlay, cálculos, câmera, manifest ou permissões. S10/S11 apenas observam seams existentes; mudanças protegidas indispensáveis devem ser explicitadas/revisadas antes da futura implementação, conforme AGENTS.md. App Links justificam apenas intent-filter de S30. Não mudar versionCode/versionName sem pedido.
- **Rollout:** A ponte gratuita; B orgânico sem incentivo; C assinatura só para novos pós-cutoff; D rewards/creators após dinheiro provado. C independe de Insights. Legado tem Core permanente; só recebe benefício Pro após E08 validado. R$10/R$25, trial 7d, rewards e CPA são hipóteses de piloto, não aprovações comerciais automáticas.

## E01 — Contratos de acesso, integrações autenticadas e publicação Play

- **ID / nome:** E01 — Contratos de acesso, integrações autenticadas e publicação Play.
- **Objetivo:** congelar as fronteiras necessárias antes de alterações concorrentes e eliminar impedimentos de integração/publicação.
- **Valor entregue:** domínios implementáveis com uma única semântica e operações sem autoridade financeira do cliente.
- **Escopo:** contratos mínimos do §15.1, API/Firebase/RBAC/segredos/rules e auditoria Play.
- **Fora de escopo:** plataforma interna, microserviços, migração Gradle, concessão de compra/reward ou ativação comercial.
- **Dependências:** respostas contratuais OQ-08; ambientes/acessos AR-01. Não depende de Billing funcional.
- **Critérios de fechamento:** contratos versionados e usados pelos domínios; API autenticada demonstrada; negativos de rules/RBAC passam; checklist Play com evidências ou gate explicitamente em HOLD.

```text
E01
├── S01 — Congelar contratos e invariantes da V1
├── S02 — Disponibilizar API autenticada e proteção dos registros
└── S03 — Auditar condições de publicação no Play Console
```

### S01 — Congelar contratos e invariantes da V1

- **Status:** concluída documentalmente em 2026-10-09; sete arquivos em [docs/contracts/](../contracts/README.md). Validações e limites em [test-matrix.md](../contracts/test-matrix.md). Nenhum código de produção/serviço externo habilitado; sem avanço para S02.

- **Tipo / requisito:** fundação; §§9–12/15.1/16, NFR-004/006/007; origem ST-01 e contratos do §15.
- **Objetivo:** permitir implementação independente sem divergência de direitos, eventos ou dinheiro.
- **Descrição técnica:** especificar os contratos mínimos pedidos no PRD: identidade, LegacyClaim, snapshot/fontes/revisões, matriz Play, atribuição, qualificação, ledgers, eventos e API. Resolver/documentar OQ-08; deixar parâmetros comerciais pendentes explícitos. Descrever erros, idempotência, UTC, emissão autorizada e fixtures BDD por domínio.
- **Critérios de aceite:** contratos em `docs/contracts/README.md`, `entitlement.schema.json`, `referral.schema.json`, `analytics-events.md`, `api.openapi.yaml`, `billing-state-matrix.md`, `test-matrix.md`; ledger/commission descritos nesses contratos sem artefatos redundantes; nomes únicos, fontes e aliases definidos; nenhuma rota interna é chamada pelo cliente; todos os estados/requisitos do PRD são rastreáveis.
- **Dependências:** nenhuma story; OQ-08 para congelamento efetivo.
- **Arquivos/módulos prováveis:** `docs/contracts/` (novo).
- **Testes necessários:** validação de schema/OpenAPI e fixtures de legado, trial/paid, 1/3/5, void e canal exclusivo; checagem de consistência documental.
- **Riscos/observações:** não converter pseudo-Kotlin em código nem usar booleano `isPremium` como contrato.

### S02 — Disponibilizar API autenticada e proteção dos registros

- **Status:** PARTIAL em 2026-10-13. JWT Google real no edge; isolamento A/B no D1 remoto com fixtures autorizadas via handler local/chaves efêmeras de teste, todos os direitos negados e limpeza comprovada. Controles em memória implantados exclusivamente DEV. Novas 2 invocações: P50 1,943ms / P90 14,163ms, sem erros; margem CPU ainda em validação. Pendentes parâmetros/budget/owner operacionais aprovados para ambiente aberto. Contratos wire preservados; [evidências e limites](S02-S04-INTEGRATION.md).

- **Tipo / requisito:** fundação; §§9–11, PRIV-001, NFR-004/006/007; suporte a ST-03/07/10/24.
- **Objetivo:** estabelecer somente o transporte seguro necessário aos domínios da V1.
- **Descrição técnica:** no ambiente AR-01, validar ID token Firebase, extrair UID da sessão, aplicar RBAC admin/creator, acesso apenas aos dados próprios, regras Firestore e rate limits acordados. Proteger segredos Play server-side; App Check quando aplicável, sem substituir autenticação. Disponibilizar erro/correlation ID comuns, sem criar handlers financeiros fictícios.
- **Critérios de aceite:** token ausente/inválido e UID forjado são rejeitados; cliente não escreve grants/entitlements/subscriptions/purchase_events/ledgers/attributions; admin sem papel não concede direito; logs não contêm token/PII; novas coleções recebem restrição nas stories que as introduzem.
- **Dependências:** S01; AR-01 e limites de OQ-06 aplicáveis.
- **Arquivos/módulos prováveis:** BACKEND, regras/índices Firestore e configuração de segredo (destinos a confirmar).
- **Testes necessários:** integração/emulador de autenticação/rules; leitura cruzada, escrita SDK, papel indevido e rate limit.

### S03 — Auditar condições de publicação no Play Console

- **Status:** auditoria documental encerrada em 2026-10-09; prontidão de monetização **HOLD**, sem STOP identificado. Evidências de Console, limites de leitura, decisões pendentes e condições para S02/S04 em [S03-PLATFORM-AUDIT.md](S03-PLATFORM-AUDIT.md). Não equivale a aprovação de publicação.

- **Tipo / requisito:** gate operacional; OPS-002, BILL-007, CORE-002; ST-29, §§8.1/17.
- **Objetivo:** detectar bloqueadores de plataforma antes de preparar lançamento comercial.
- **Descrição técnica:** conferir developer/app verification, `br.com.calcmot`, target exigido na data de release, App Signing, catálogo/acessos backend e disclosure/consentimento Accessibility conforme uso real. Verificar versão oficial Billing vigente no build; 9.1.0+ é baseline do PRD sujeito a reconferência.
- **Critérios de aceite:** evidência datada por item, owner e GO/HOLD/STOP registrados; `isAccessibilityTool` não é declarado sem finalidade correspondente; nenhum bypass é proposto; conformidade do Console não é inferida de targetSdk local.
- **Dependências:** nenhuma story; AR-01 e acesso humano ao Console/documentação oficial.
- **Arquivos/módulos prováveis:** `docs/play-store-submission.md`, `docs/product/` (evidências futuras), build Gradle apenas se revisão futura justificar.
- **Testes necessários:** auditoria verificável de configuração/política; comparar package/build/certificado com Console. Não modificar produção nesta entrega.

## E02 — Identidade recuperável e Core legado preservado

- **ID / nome:** E02 — Identidade recuperável e Core legado preservado.
- **Objetivo:** transformar elegibilidade antiga em direito durável, recuperável e seguro offline.
- **Valor entregue:** o motorista antigo continua usando o Core, inclusive após reinstalação ou atualização tardia.
- **Escopo:** Auth/linking, grants, claims/admin, projeção de fontes, cache e recuperação UI.
- **Fora de escopo:** cobrar legados pelo Core, identificação perfeita sem evidência, autenticação síncrona por oferta, benefícios Pro inventados.
- **Dependências:** S01/S02; OQ-01–03/07/10 e AR-01 conforme story.
- **Critérios de fechamento:** restore e late-claim passam; grant confirmado não é revogado por preço/flag/plano; cache não causa I/O no Core; provisório e fallback têm prazos auditáveis; recovery UI funciona.

```text
E02
├── S04 — Vincular identidade a login recuperável
├── S05 — Registrar grant legado auditável
├── S06 — Revisar claims tardios e operar suporte
├── S07 — Projetar direitos por fonte no servidor
├── S08 — Consumir snapshot local com política offline
└── S09 — Apresentar vínculo e recuperação de acesso
```

### S04 — Vincular identidade a login recuperável

- **Status:** PARTIAL em 2026-10-13. Google Auth Spark/OAuth DEV e testes físicos anteriores comprovados. SHA-1 Play autorizado cadastrado no Firebase principal; JSON candidato oficial e default_web_client_id validados separadamente, original preservado. Restam testes de distribuição Play, segundo aparelho, expiração natural/linking excepcional e política de recuperação residual. Nenhum fluxo S09/release. [Evidências](S02-S04-INTEGRATION.md).

- **Tipo / requisito:** fundação; ENT-002, §§3.2/9.1; ST-02.
- **Objetivo:** preservar identidade/direito entre sessões e aparelhos sem exigir login para Core gratuito da ponte.
- **Descrição técnica:** integrar Firebase Auth e linking do provedor aprovado; expor UID/estado de vínculo, recuperar sessão e tratar colisão segundo OQ-01. Conta anônima pode apoiar início, mas não é única via de preservação definitiva. Falha de login não bloqueia a ponte.
- **Critérios de aceite:** login/relogin/troca de aparelho recuperam o mesmo titular; linking conserva direito e continuidade conforme contrato; conta já existente não sobrescreve grant/compras; explicação distingue identidade anônima de recuperável.
- **Dependências:** S01; OQ-01 e AR-01.
- **Arquivos/módulos prováveis:** `APP/identity/` (novo), `CalcMotApplication.kt`, `app/build.gradle.kts` para dependência Auth.
- **Testes necessários:** Auth/linking com emulador/tester; sessão perdida, login cancelado, provedor falho e colisão; reinstalação/aparelho secundário.

### S05 — Registrar grant legado auditável

- **Status:** PARTIAL; integração atualizada em 2026-10-14, base `growth/226fe22f`. Elegibilidade aprovada por uso histórico <=3.5/12; inventário identificado no commit 3.5. Fonte imutável pela via histórica S05 ou pela decisão humana autorizada S06, sem cadastro automático na ponte. Migration 0006 preserva fontes anteriores e valida a trilha distinta de revisão; nenhum endpoint/grant real/snapshot. Pendem operador/ensaio real e operação, não decisão comercial. [Implementação](S05-LEGACY-FOUNDATION.md), [política aprovada](OQ-02-LEGACY-POLICY.md), [emissão S06](S06-LEGACY-RECOVERY.md).

- **Tipo / requisito:** fundação; ENT-001, §§3/10; ST-03.
- **Objetivo:** tornar permanente o Core comprovadamente anterior à mudança.
- **Descrição técnica:** registrar grant por UID autenticado com `legacyGrantedAt`, evidência, `legacyCoreVersionBaseline`, `migrationVersion`, policy/cutoff/revisão/ator; conceder apenas segundo OQ-02, sem booleano local regravável. Preparar configuração de cutoff ainda não congelada; não inventar data.
- **Critérios de aceite:** mesma migração repetida não duplica grant; reinstalação recupera registro; pós-cutoff não ganha legado por autodeclaração; rollback/preço/plano não apagam direito; evidência local é auxiliar, não prova absoluta; inventário de Core existente preservado.
- **Dependências:** S02/S04; OQ-02 para regras de concessão real.
- **Arquivos/módulos prováveis:** BACKEND legacy/users/grants; `APP/identity/` e `entitlement/` (previstos).
- **Testes necessários:** concessão idempotente, versões históricas 1/11/12/13+, evidência insuficiente, cadastro/versão atual sem prova histórica, replay, recuperação e rollback; rules de escrita. UTC de migração não redefine a elegibilidade por versão aprovada.

### S06 — Revisar claims tardios e operar suporte

- **Status vigente — 2026-10-19: DONE no escopo operacional DEV controlado; produção HOLD.** Operador Firebase real, console privado remoto, revisão assistida e emissão única especificamente autorizada demonstrados até V3 e reinstalação DEV; segundo titular/caso negativo isolados. OQ-07 aprovada para o piloto, migration 0008 aplicada com aprovação, descarte somente em D1 local sintético. [Evidências, CPU e limites de rollout](S06-S09-DEV-OPERATIONAL-RESULTS.md). Os estados de preparação abaixo são histórico preservado, não pendência ainda sem diagnóstico.

- **Status:** PARTIAL em 2026-10-14. Aprovação humana autorizada integrada à emissão atômica de LegacyGrant S05, com trilha ASSISTED_ASSESSMENT distinta de histórico verificável, revisão vigente, idempotência, concorrência/rollback e recuperação persistida. 85 testes backend PASS (32 S06), 1 remoto SKIPPED, typecheck e bundle dry-run PASS; console exclusivamente local, sem grant real/deploy. Restam autoridade Firebase real do proprietário, ensaio operacional autorizado e OQ-07; emissão assistida deixou de ser impedimento técnico. [Relatório e procedimento](S06-LEGACY-RECOVERY.md).
- **Fechamento operacional em preparação:** leitura Firebase/IAM e diagnóstico local concluídos; sem CLI/ADC/Admin SDK nos locais examinados. Conta de serviço existente sem chave, com privilégios amplos; nenhum admin foi concedido. 8 testes específicos de preflight/integração PASS, sem reexecutar a suite completa. [Runbook sem chave](S06-OPERATOR-RUNBOOK.md); [OQ-07 proposta, não aprovada](OQ-07-DATA-POLICY-PROPOSAL.md). Guardas atuais ainda impedem expurgo legítimo: solução autorizada futura S15 é necessária antes de coleta real, sem revogar Core. PARTIAL mantido.

- **Tipo / requisito:** produto; ENT-004/002, §§3.2/10/11; ST-03/25.
- **Objetivo:** evitar falso bloqueio de usuário antigo não migrado.
- **Descrição técnica:** implementar claim autenticado com protocolo/evidências limitadas e estados pendente/granted/rejected; provisório autoritativo com prazo; operação admin mínima para revisar/conceder com justificativa, reviewer e auditoria. Usar fluxo/painel de suporte mínimo existente ou endpoint protegido, sem suíte administrativa nova.
- **Critérios de aceite:** indício legítimo permite acesso provisório nos termos; ausência de UID recuperável tem recuperação assistida; prazo expira conforme política, sem booleano infinito; rejeição/concessão são auditadas; nova instalação não recebe legado por declaração; nenhuma PII em Analytics.
- **Decisão operacional aprovada em 2026-10-14:** proprietário é o único aprovador; avaliação assistida contextualizada e com titularidade confirmada pode fundamentar fonte permanente, sem fabricar histórico, Premium ou transferências. **Provisório de sete dias não será ativado nesta entrega**, conforme instrução expressa; campo permanece nulo. Critérios históricos acima não autorizam ativação nem retenção indefinida. Fonte S06 não é projeção S07.
- **Dependências:** S05; OQ-02/OQ-07.
- **Arquivos/módulos prováveis:** BACKEND legacy_claims/admin; contratos e futura documentação de suporte.
- **Testes necessários:** claim repetido, provedor perdido, atualização meses depois, expiração, decisão admin não autorizada e trilha de grant.

### S07 — Projetar direitos por fonte no servidor

- **Status:** DONE no escopo local autorizado em 2026-10-14. Projetor read-only S01, migration aditiva 0007 com revisão atômica das fontes, preservação de Play/promo e Legacy Core separado de Pro. 112 testes backend PASS (20 S07), 1 remoto SKIPPED; request HTTP do Worker compilado em workerd + D1 persistido e recuperação após reinício comprovados. Flag apenas local; operação/CPU edge/OQ-07 seguem HOLD, sem deploy ou grant real. [Evidências e limites](S07-ENTITLEMENT-PROJECTION.md). Dependências operacionais PARTIAL de S05/S06 não foram reclassificadas.

- **Tipo / requisito:** fundação; ENT-003/004, §§9/11.1; ST-04.
- **Objetivo:** disponibilizar snapshot autoritativo, sem reduzir direito a uma flag premium.
- **Descrição técnica:** normalizar LegacyCore, PlayValid, PromoPro e ProvisionalReview com UTC, `serverNow`, revisão e validade separadas. Expor GET próprio do contrato S01. Billing/promo alimentam suas fontes nas stories correspondentes; usar fixtures agora, sem fabricar compra.
- **Critérios de aceite:** legado prevalece para Core; cancelado ainda válido/grace respeitam expiry; hold/paused/pending/expired/revoked não concedem novo Core sem fonte independente; provisório tem prazo; fonte UNKNOWN gera reconciliação; promo expirada não reativa Play expirado.
- **Dependências:** S05/S06.
- **Arquivos/módulos prováveis:** BACKEND entitlement/projector; `docs/contracts/`.
- **Testes necessários:** matriz de união/prioridade de fontes, validade/revisão, acesso de outro UID e estado desconhecido.

### S08 — Consumir snapshot local com política offline

- **Política aprovada / infraestrutura atual:** [OQ-03/OQ-10](OQ-03-OQ-10-APPROVED-POLICY.md) formalizadas; código distingue paid/trial simultaneamente, exige verificação própria de promo e desativa review. Cache v2 lê v1; observador por geração preparado, gates OFF. S08 PARTIAL por fonte promocional confiável e ack/validação de sessão não integrados; registros abaixo são histórico das entregas anteriores, não aprovação comercial ainda pendente.
- **Validação corrente:** 47 testes específicos JVM e 2 Samsung DEV PASS; 7 contratos/implementações anteriores do Core preservados. Boot/recuperação de UID são cases controladas com fonte/identidade sintéticas, não revalidação financeira ou reboot físico. [Relatório atualizado](S08-ENTITLEMENT-CACHE.md).
- **Continuação de validação:** Samsung SM-A075M/API 35: 1 teste instrumentado de cache/Keystore PASS; 8 testes novos de proposta OQ-03/OQ-10 PASS, sem repetir a suíte. [Propostas e lacunas](OQ-03-OQ-10-OFFLINE-SAFE-ENTRY-PROPOSAL.md) não aprovadas/ativadas; S08 permanece PARTIAL. Fonte de recência promo, distinção pago/trial e confirmação de fim de sessão ainda impedem ativação segura.
- **Status 2026-10-14: PARTIAL.** Repositório Android/cache por projeto/UID, parser S01, persistência atômica/Keystore, refresh fora do Core e avaliação temporal em RAM implementados. 299 testes PASS em debug e identityDev (30 S08), instrumentação DEV compilada e fixture/cache validados pelo schema S01. Sem política comercial instalada, gate no Core ou ativação de ambiente. OQ-03/OQ-10 e validação física permanecem pendentes; [escopo e evidências](S08-ENTITLEMENT-CACHE.md). Não inicia S09.
- **Tipo / requisito:** fundação; ENT-003/004, NFR-002/005/008, §§11.3–11.4; ST-04/09.
- **Objetivo:** proteger uso legítimo e impedir consulta de rede no processamento de oferta.
- **Descrição técnica:** `EntitlementRepository` em pacote previsto publica StateFlow cacheado; persistir snapshot gerado pelo servidor com revisão/serverNow/validUntil/âncora monotônica. Refresh em abertura/retorno/jobs adequados; Core lê RAM. Separar grant durável, compra em verificação, provisório e vencimento confirmado; aplicar alteração restritiva na entrada segura, com limite aprovado.
- **Critérios de aceite:** análise offline não faz request de growth/billing/auth; legado confirmado continua disponível; relógio alterado/reboot não estendem prazo eternamente; monitoramento não é interrompido por paywall; tolerância e review expiram conforme OQ-03; restore possível na entrada segura.
- **Dependências:** S07; OQ-03/OQ-10.
- **Arquivos/módulos prováveis:** `APP/entitlement/` (novo), bootstrap/app lifecycle; consumo em runtime/UI sem reescrever Core.
- **Testes necessários:** matriz de cache/expiração, relógio injetado, perda de rede/reboot/dados, fontes combinadas e verificação de zero I/O no caminho da oferta.

### S09 — Apresentar vínculo e recuperação de acesso

- **Status vigente — 2026-10-19: fluxo de conta/recuperação DONE no piloto DEV; S09 geral PARTIAL / rollout HOLD.** V3 aprovada preservada, intake opt-in S01, sessão real, operador, grant autorizado, 200 sem Premium, logout/relogin e reinstalação DEV comprovados; outra conta não herda cache/grant. Dois testes físicos finais cobrem autorização pública e cache real com falhas controladas; 55 JVM pertinentes PASS. [Resultados completos](S06-S09-DEV-OPERATIONAL-RESULTS.md). Segundo aparelho e simultaneidade com Core real não medidos; cauda CPU e privacidade de rollout explícitas. Sem produção, S10 ou novas funcionalidades. Notas antigas de visual não aprovado/operação indisponível abaixo são histórico superado.

**Diagnósticos finais — 2026-10-15:** primeiro login suspenso corrigido após reprodução; 48 testes JVM PASS e 2 testes físicos de NavBackStackEntry/lifecycle PASS. Remoção real da entrada encerra ViewModel/adaptador e não inicia novas consultas/fotos ao retomar a Activity. Epoch na fonte e I/O já em andamento continuam limites explícitos. [Evidências e diff mínimo](S09-FIRST-LOGIN-LIFECYCLE.md). S09 PARTIAL; V3 preservada.



**Auditoria final S09 — 2026-10-15:** revisão dirigida e integração real DEV em [S09-FINAL-AUDIT.md](S09-FINAL-AUDIT.md). Pequena correção autorizada de sucesso tardio e `flatMapLatest` (switchMap do Flow); 44 testes JVM PASS, sem redesign. S09 permanece PARTIAL; nenhuma S10 iniciada. Esta nota prevalece sobre os estados históricos abaixo.



**Atualização vigente — 2026-10-15:** V3 ajustada aprovada pelo proprietário como referência e integrada à composição funcional existente, sem dependência dos protótipos. Conta prioriza identidade; Meu acesso distingue consulta indisponível, estado desconhecido e resposta válida sem grant com linguagem neutra. Direito permanente confirmado oferece detalhes e não nova recuperação. Menu tipado, sessão/feedback isolados por titular e geração. **S09 PARTIAL** por operação S06/OQ-07 e limites registrados. [Auditoria, arquitetura e evidências correntes](S09-V3-INTEGRATION.md). As notas de conceitos/aprovação pendente abaixo são histórico anterior a esta autorização, não o status vigente.



- **V3 — ajustes finais, 2026-10-15:** direção preferida pelo proprietário, ainda sem aprovação de integração. Textos refinados, autenticação neutra, estados permanente/não verificado/sem grant confirmado separados da indisponibilidade. 12/12 testes físicos V3 PASS e 12 capturas normal/1,5× inspecionadas. [Evidências](S09-ACCOUNT-CONCEPTS.md). Apenas protótipo/documentação; S09 PARTIAL e visual aguardando aprovação. Nenhuma mudança funcional, grant real ou próxima story.

- **V3 híbrida — avaliação pendente:** três protótipos Compose no Samsung DEV; seis testes dirigidos (normal/1,5×) PASS, capturas inspecionadas/comparadas com A/B. Entrada sem card, perfil compacto com identidade CalcMot, saída separada e consulta neutra com retry secundário. [Resultados V3](S09-ACCOUNT-CONCEPTS.md). **PARTIAL / visual não aprovada**, sem integrar ao fluxo funcional ou alterar contratos/Core. Aprovação explícita necessária antes da integração.

- **2026-10-15 — protótipos A/B para escolha:** seis telas Compose renderizadas no Samsung DEV, mais seis em fonte 1,5; **12/12 testes específicos PASS** e inspeção visual final, com correção apenas no teste de alinhamento/barras. A privilegia identidade central/atalhos; B, tarefas em lista. [Evidências e comparação](S09-ACCOUNT-CONCEPTS.md). Sem alterar S09 funcional, Auth, Core ou contratos. **PARTIAL; visual NÃO APROVADA**, aguardando escolha explícita; nenhuma integração automática.

- **Direção vigente: PARTIAL, visualmente NÃO APROVADA.** Correção explícita do proprietário: Google Sign-In opcional protagonista; perfil/gestão após conexão; acesso complementar; recuperação terciária somente nas informações de conta/acesso. [Conceitos A/B](S09-ACCOUNT-CONCEPTS.md) isolados de teste para aprovação antes de nova implementação funcional. Resultados abaixo são evidências históricas, não aprovação do desenho anterior. Operação S06/OQ-07 continua pendente; gates OFF.

- **Status 2026-10-14: PARTIAL.** Conta/Meu acesso/Recuperação refinadas sem alterar arquitetura, Auth, navegação ou S01–S08. **22 JVM PASS na etapa anterior e 15/15 instrumentados Samsung PASS nesta retomada**, incluindo retry/suporte, navegação, fonte 1,5 e ausência de avisos duplicados. Capturas físicas com fixtures identificadas inspecionadas e comparadas com a composição anterior. Login real, consulta/retry 503, suporte sem envio e retorno entre páginas conferidos; três capturas reais refinadas inspecionadas. Intake operacional, restore de grant real e OQ-07 continuam pendentes. [Relatório, capturas e limites](S09-IDENTITY-RECOVERY-UI.md), [referências existentes](S09-VISUAL-REFERENCES.md). Instalação apenas no pacote DEV; sem gate, deploy, push ou avanço S10.
- **Tipo / requisito:** produto; ENT-001/002/004, NFR-010, §§3/7; ST-02/25.
- **Objetivo:** tornar preservação e suporte compreensíveis ao motorista.
- **Descrição técnica:** convite de vínculo sem bloquear uso gratuito; estado “Acesso antigo preservado”; recuperação/relogin, protocolo/revisão provisória, retry/suporte e limites de conta anônima. Reutilizar navegação/DS; nunca exibir paywall Core para grant válido.
- **Critérios de aceite:** copy público do legado não promete gratuidade de recursos futuros; reinstalação/aparelho novo e late-claim têm ações reais; review mostra estado/prazo sem PII de terceiros; erro de login na ponte permite continuar Core; estados acessíveis conforme DoD UI.
- **Dependências:** S04/S06/S08; OQ-01/OQ-02.
- **Arquivos/módulos prováveis:** `UI/CalcMotNavigation.kt`, `HomeRouteScreen.kt`, telas de conta/recuperação novas e `APP/identity/`.
- **Testes necessários:** Compose UI de estados/ações, restore E2E e screenshot de tela pequena/fonte ampliada/TalkBack.

## E03 — Uso canônico e compatibilidade do Core

- **ID / nome:** E03 — Uso canônico e compatibilidade do Core.
- **Objetivo:** medir valor real e detectar incompatibilidade sem alterar a extração de produção.
- **Valor entregue:** CAD/ativação confiáveis e proteção contra lançar cobrança sobre um Core regressivo.
- **Escopo:** eventos lógicos deduplicados, falhas sanitizadas, baseline/matriz Android × Uber/99.
- **Fora de escopo:** novo parser, bypass de conteúdo protegido, ANR refactor, screenshot/OCR bruto para growth.
- **Dependências:** S01/S03/S04; definição de análise/dia OQ-08, baseline OQ-09.
- **Critérios de fechamento:** duplicação por frames não infla uso; demo não conta como oferta real; incompatibilidade é explícita e limitada ao recurso afetado; matriz registra versões/resultados e baseline sem alegar cobertura ausente.

```text
E03
├── S10 — Registrar análise lógica e dia ativo sem duplicação
├── S11 — Informar compatibilidade e falhas sanitizadas
└── S12 — Qualificar Core na matriz e medir baseline
```

### S10 — Registrar análise lógica e dia ativo sem duplicação

- **Status 2026-10-28: PARTIAL, emissão não ativada.** Consumidor local persistente, união explícita, fila assíncrona e envelope canônico implementados; a origem atual não distingue ID lógico de renovação TTL/ofertas iguais. Sem heurística nova nem mudança do Core. Associação S04, confiança temporal e retenção específica continuam bloqueios de ativação. [Implementação, diagnósticos e validações](S10-CORE-USAGE.md). Não iniciar outras stories por consequência desta entrega.

- **Tipo / requisito:** fundação; DATA-001, NFR-002/007, §§2/16; ST-01.
- **Objetivo:** distinguir uso real de frames/parse repetidos e sustentar CAD/ativação.
- **Descrição técnica:** observar conclusão lógica existente, com `analysisId` efêmero e dedupe local; persistir conjunto de dias/primeiro valor sem conteúdo de oferta. Usar identidade S04 e regra de timezone/linking S01; estender allowlist/sanitização existente. Emitir `core_analysis_completed`, primeiro valor e dia ativo conforme dicionário congelado, não derivar de `offer_parsed` bruto.
- **Critérios de aceite:** mesma análise repetida gera um sucesso; várias análises no dia geram um `core_active_day`; dois dias distintos contam para CAD; primeira análise é única por identidade; demo não altera Core/CAD; payload não contém token/OCR/endereço/UID de terceiros; persistência/envio fora do caminho crítico.
- **Dependências:** S01/S04; OQ-08.
- **Arquivos/módulos prováveis:** TEL eventos/params/policy/tracker, seam de `ReadingPipelineRuntime.kt`; ponto de conclusão em serviço apenas se explicitamente revisado; agregador local novo.
- **Testes necessários:** repetição/frame, ofertas distintas com valores iguais, virada de dia/timezone, restart/linking, demo e regressão Uber/99 ao conectar seam.

### S11 — Informar compatibilidade e falhas sanitizadas

- **Tipo / requisito:** produto; CORE-002, DATA-001, NFR-002/007/009; ST-31, §8.1.
- **Objetivo:** tornar leitura indisponível observável sem fingir análise válida.
- **Descrição técnica:** observar resultados já existentes e classificar succeeded/failed(reason, appFamily, API/versionBucket) sem conteúdo; apresentar incompatibilidade/ausência de capacidade de captura e desabilitar somente função atingida. Reutilizar diagnostics/runtime, sem mudança de algoritmo ou contorno de proteção.
- **Critérios de aceite:** falha de ROI/permissão/API/conteúdo indisponível não gera sucesso fictício; resto do app permanece utilizável; dados reais de motorista não são enviados; não assumir que Uber/99 usam `accessibilityDataSensitive`; estado Android antigo sem captura compatível é honesto.
- **Dependências:** S10; revisão explícita de qualquer seam protegido.
- **Arquivos/módulos prováveis:** TEL, `ReadingPipelineRuntime.kt`, `UI/DiagnosticsScreen.kt`, `ReadingDiagnosticsScreen.kt`; observação de adapters existentes.
- **Testes necessários:** motivos sanitizados/allowlist, incompatibilidade isolada, UI e regressões atuais de Uber/99.

### S12 — Qualificar Core na matriz e medir baseline

- **Tipo / requisito:** gate operacional; CORE-002, NFR-001/003/009, §§8.1/17; ST-31.
- **Objetivo:** produzir baseline e evidência real de compatibilidade antes da ponte/cobrança.
- **Descrição técnica:** executar matriz Android 13/14/15/16, Uber/99 atuais e uma anterior suportada quando reproduzível; acrescentar 10/11/12 conforme suporte/base, sem reduzir minSdk existente. Cobrir leitura/semáforo/overlay, liga/desliga, foreground/background/suspensão, pós-2h, ROI, permissão e ANR; medir sucesso/p50/p95/crash-free contra build comparável.
- **Critérios de aceite:** versões, aparelho/API, build, resultados e lacunas registrados; limites OQ-09 aprovados; nenhum P0/P1 aberto no conjunto requerido para release; sem bypass/dados reais em telemetry; cobertura impossível fica HOLD, não PASS.
- **Dependências:** S03/S11; OQ-09, aparelhos/testers.
- **Arquivos/módulos prováveis:** `docs/quality/`, suites de regressão existentes, scripts `run-production-e2e-qa.ps1`/`analyze-calcmot-latency.ps1` se necessários.
- **Testes necessários:** execução das suites e matriz física/emulada definida acima; não criar epic/framework de testes.

## E04 — Versão ponte gratuita, privacidade e reversão

- **ID / nome:** E04 — Versão ponte gratuita, privacidade e reversão.
- **Objetivo:** entregar migração observável antes de qualquer exigência de pagamento.
- **Valor entregue:** base preservada, controles remotos e comunicação/recuperação prontas.
- **Escopo:** flags/buckets, preferências/dados/exclusão e gate da ponte/cutoff posterior.
- **Fora de escopo:** cobrança, push comercial, reclassificar antigo como novo, cancelar assinaturas por rollback.
- **Dependências:** E01/E02/E03 nas stories listadas; OQ-02/07 e AR-01.
- **Critérios de fechamento:** release A sem paywall; restore/claims passam; rollback preserva grants/reconciliação; comunicação/data safety/exclusão aprovadas; cutoff só congelado depois da janela e evidências exigidas.

```text
E04
├── S13 — Configurar flags, bucket estável e rollback
├── S14 — Publicar transparência e preferências de marketing
├── S15 — Operar exclusão dos dados introduzidos na ponte
└── S16 — Entregar ponte e registrar gate de cutoff
```

### S13 — Configurar flags, bucket estável e rollback

- **Tipo / requisito:** fundação; OPS-001, NFR-008, §§17/12; ST-10.
- **Objetivo:** desligar novas iniciativas preservando direitos adquiridos e suporte.
- **Descrição técnica:** integrar Remote Config e assignment estável server-side dos novos elegíveis; registrar versão/ator de rollout. Implementar flags exatamente do §17, cobrança/trial/rewards/creators/share/review/marketing/Integrity OFF, claims ON; separar caminhos de novos efeitos de reconciliação existente.
- **Critérios de aceite:** assignment não muda ao reabrir app; flag não transforma pré-cutoff em novo; cobrança OFF deixa Core acessível; desligar rewards impede novas concessões, conserva promessas/créditos/aplicações anteriores; RTDN/ACK/restore/suporte continuam; grants não são revogados; client não altera bucket.
- **Dependências:** S02/S07/S08; AR-01.
- **Arquivos/módulos prováveis:** BACKEND feature_rollouts, `APP/entitlement/`, configuração/adapter Remote Config novo.
- **Testes necessários:** defaults sem rede, mudança de flags, bucket persistente, rollback com fontes legacy/paid/promo/provisório; integração das novas features revalida flags nas próprias stories.

### S14 — Publicar transparência e preferências de marketing

- **Tipo / requisito:** produto; PRIV-001, GROW-001, NFR-007/010, §§3/7/10; ST-24.
- **Objetivo:** oferecer explicação e controle dos dados/solicitações da V1.
- **Descrição técnica:** atualizar política/disclosures/consentVersion e preferências persistentes de marketing/opt-out conforme coleta realmente introduzida. Preservar consentimento Accessibility destacado/afirmativo; revisar integração Clarity existente para novos estados, sem tokens/dados pessoais ou financeiros sensíveis. Push growth OFF e sem pedir POST_NOTIFICATIONS no primeiro launch para marketing.
- **Critérios de aceite:** política/Data Safety refletem dados/retention aprovados; opt-out persiste e é consultável; Core não exige consentimento comercial/notificação; sem contatos/screenshot de motorista/OCR bruto; confirmação não é pré-selecionada para marketing; coleta nova é minimizada.
- **Dependências:** S01/S04; OQ-07.
- **Arquivos/módulos prováveis:** `docs/privacy-policy.md`, `docs/play-store-submission.md`, `UI/PrivacyPolicyScreen.kt`, `AppLegal.kt`, preferências e allowlists existentes.
- **Testes necessários:** UI/restore de preferências, inspeção de payloads e estados sensíveis das integrações existentes; evidência de revisão de política.

### S15 — Operar exclusão dos dados introduzidos na ponte

- **Tipo / requisito:** produto; PRIV-001, NFR-007, §10; ST-24.
- **Objetivo:** disponibilizar exclusão/solicitação verificável conforme política aprovada.
- **Descrição técnica:** implementar fluxo autenticado de exclusão para identidade/claims/projeções/telemetria da ponte, respeitando retenção justificada e tratamento de grant em OQ-07. Distinguir exclusão de conta de cancelamento Play. Domínios futuros acrescentam seus próprios dados/testes a esse fluxo, sem criar framework de retenção genérico.
- **Critérios de aceite:** somente titular solicita; operação é repetível, apresenta resultado/prazo e suporte; dados de outro UID não são tocados; indisponibilidade não reporta sucesso falso; registros a reter têm motivo/prazo e acesso restrito; não apaga direito/ledger silenciosamente.
- **Dependências:** S02/S05/S06/S07/S14; OQ-07.
- **Arquivos/módulos prováveis:** BACKEND handlers de conta/dados, UI de privacidade/conta e documentação de exclusão.
- **Testes necessários:** titular/terceiro, retry/interrupção, exclusão completa versus retenção justificada, restauração/claims conforme regra; UI de protocolo.

### S16 — Entregar ponte e registrar gate de cutoff

- **Tipo / requisito:** gate operacional; ENT-001–004, OPS-001/002, PRIV-001, §§3.2/14.1/17/19; ST-05/25.
- **Objetivo:** fechar o primeiro incremento utilizável com Core gratuito, direito recuperável e métricas reais.
- **Descrição técnica:** integrar versão ponte/comunicação, convidar vínculo sem bloquear Core, validar restore/late-claim/offline/flags/privacidade e publicar evidências para release A. Após janela suficiente observada e autorização, congelar cutoff UTC/policy/core baseline no servidor; não supor duração nem fazer publicação nesta tarefa.
- **Critérios de aceite:** 100% dos legados identificados de teste recuperam Core; perda de sessão/aparelho/reinstalação sem vínculo tem suporte real; nenhum checkout/paywall/reward; matriz e build de entrega passam; lacunas de recuperação ampliam ponte; cutoff não antecede evidência/aprovação nem reclassifica usuários antigos.
- **Dependências:** S03/S09/S10/S12/S13/S14/S15; OQ-02 e gate humano de release/cutoff.
- **Arquivos/módulos prováveis:** integração de bootstrap/navegação, configuração, `docs/product/` e `docs/quality/` (evidências futuras).
- **Testes necessários:** E2E da ponte e recuperação; `assembleRelease`/`bundleRelease`; smoke de Core e rollback. Reusar testes de cada story, não transferi-los para este gate.

## E05 — Demo, assinatura Play e recuperação de compra

- **ID / nome:** E05 — Demo, assinatura Play e recuperação de compra.
- **Objetivo:** demonstrar valor antes do trial e verificar o lifecycle completo antes de cobrar novos usuários.
- **Valor entregue:** escolha voluntária e recuperável de mensal/trimestral com preços e direitos reais da Play.
- **Escopo:** demo gratuita, catálogo/Billing 9.1.0+, verify/ACK/RTDN, prova financeira, reconciliação, checkout, restore, status e troca de plano; gate de piloto.
- **Fora de escopo:** paywall de legado, trial local/automático, plano anual, comissão/reward, UI comercial durante monitoramento, expandir Insights como pré-requisito.
- **Dependências:** contratos E01, E02/E04 nos pontos indicados; OQ-03–05/07/09/10/12. Release C exige release A/cutoff aprovado, não release B ou Insights.
- **Critérios de fechamento:** matriz Play de trial/paid/pending/cancel/grace/hold/paused/expired/void/restore/troca passa; pagamentos são conciliados, ACK dentro do prazo e compra sem entitlement gera incidente; gate C autoriza apenas piloto de novos elegíveis e rollout reversível.

```text
E05
├── S17 — Demonstrar oferta sintética após readiness
├── S18 — Consultar catálogo e ofertas elegíveis da Play
├── S19 — Verificar compra, vincular titular e reconhecer na Play
├── S20 — Processar RTDN e projetar lifecycle confirmado
├── S21 — Comprovar fonte oficial de cobrança e void
├── S22 — Registrar primeira cobrança e renovação por identidade econômica
├── S23 — Reconciliar compras, estados e incidentes financeiros
├── S24 — Integrar paywall e checkout voluntário ao protótipo
├── S25 — Restaurar compra e apresentar status/gerenciamento
├── S26 — Trocar mensal e trimestral sem duplicar direitos
└── S27 — Homologar piloto de cobrança com gates e rollback
```

### S17 — Demonstrar oferta sintética após readiness

- **Tipo / requisito:** produto; CORE-001, BILL-002, NFR-010, §4.2; ST-30.
- **Objetivo:** mostrar R$/km, R$/h e semáforo antes de qualquer aceitação de trial.
- **Descrição técnica:** usar setup/diagnóstico gratuito e dados locais sintéticos explicitamente rotulados; renderizar indicadores com cálculo existente, sem alterar fórmulas ou acionar OCR/Accessibility sobre terceiros. Registrar `core_demo_completed` separado do uso real.
- **Critérios de aceite:** setup incompleto permite configurar grátis; demo disponível após readiness mostra “oferta de exemplo”/simulação; não inicia checkout/relógio trial nem serviço de terceiros; não emite primeira oferta/Core day/CAD; continuar é ação voluntária e não vende Insights inexistente.
- **Dependências:** S08/S10/S14.
- **Arquivos/módulos prováveis:** `UI/OnboardingScreen.kt`, `HomeRouteScreen.kt`, `DiagnosticsScreen.kt`, tela demo nova, `APP/model/Profitability.kt` apenas reutilizado; TEL.
- **Testes necessários:** Compose readiness/demo, indicadores com fixture sintética e sem efeitos de captura/compra; evento separado; screenshot/TalkBack/fonte ampliada.

### S18 — Consultar catálogo e ofertas elegíveis da Play

- **Tipo / requisito:** fundação; BILL-001/003/007, §4/11.5; ST-06/29.
- **Objetivo:** fornecer catálogo real para preço/termo/offerToken sem strings comerciais fixas.
- **Descrição técnica:** implementar BillingGateway/BillingClient com biblioteca 9.1.0 ou release oficial mais recente compatível na data do build; ProductDetails/base plans/offers da loja e configuração validada. Ambos planos dão o mesmo acesso; somente oferta elegível autoriza trial 7d. Tratar loading/desconexão/erro/indisponível com retry.
- **Critérios de aceite:** preços formatados/região e períodos vêm da Play; monthly_auto/quarterly_auto/trial_7d_new_user só após confirmação; sem oferta trial → paid-only; sem produto → erro honesto, sem fallback de R$9,90/R$10 rígido; catálogo não inicia compra/trial.
- **Dependências:** S01/S03/S04; OQ-05/AR-01.
- **Arquivos/módulos prováveis:** `APP/billing/` (novo), `app/build.gradle.kts`/catálogo de versões se usado, configuração Play.
- **Testes necessários:** gateway com fixtures e testers Play; trial elegível/inelegível, catálogo vazio, moeda/região, reconexão e versão/API oficial.

### S19 — Verificar compra, vincular titular e reconhecer na Play

- **Tipo / requisito:** fundação; BILL-004/005, NFR-004/006/007, §11.1; ST-07/26.
- **Objetivo:** conceder direito confirmado sem permitir roubo/replay de compra pelo cliente.
- **Descrição técnica:** endpoint autenticado recebe token, consulta subscriptionsv2.get, valida package/produto/plano, estado, titular/obfuscated account ID e linked tokens; protege token completo no servidor, grava hashes/binding/revisão e ACK compra elegível inclusive trial no prazo aplicável (baseline PRD: três dias). Preserva fonte/lock definido antes do checkout por S28/S24, inclusive organic/unknown; não retroatribui origem remunerada ao verificar compra. Verificador pode ser implementado/testado isoladamente por contrato, sem Hub.
- **Critérios de aceite:** callback não concede entitlement definitivo; token de outra identidade/produto/package é rejeitado; retry não duplica compra/grant; pending retorna verificação, não paid; ACK/revisão confirmados; token nunca em GA4/log/APK; verificação não substitui fonte prévia nem aceita cupom tardio.
- **Dependências:** S02/S04/S07; OQ-05. Pode ser implementada em paralelo a S18 com IDs/contratos congelados.
- **Arquivos/módulos prováveis:** BACKEND PlayPurchaseVerifier/subscriptions/entitlement, segredos/rules.
- **Testes necessários:** compra/trial ACK com Play tester; token cruzado, replay, troca de token, pending, falha parcial e bind/revisão idempotentes.

### S20 — Processar RTDN e projetar lifecycle confirmado

- **Tipo / requisito:** fundação; BILL-005/006/008, ENT-003, NFR-004, §§11.1/12; ST-08/26.
- **Objetivo:** sincronizar mudanças da Play sem confiar na ordem/tipo da notificação.
- **Descrição técnica:** entrada Pub/Sub autenticada, receipt/dedupe de messageId e objeto econômico; sempre reconsultar Play e alimentar projector S07 por revisão confirmada. Mapear trial/active, canceled-with-access, grace, hold, paused, pending, expired/revoked/unknown; proteger grants/promo independentes.
- **Critérios de aceite:** duplicadas/desordenadas/atrasadas não voltam a estado antigo ou repetem efeito; canceled futuro conserva acesso; hold não concede indefinidamente; UNKNOWN reconcilia; estado ACTIVE de trial não gera receita; notificação de defer não cria novo prêmio.
- **Dependências:** S19.
- **Arquivos/módulos prováveis:** BACKEND RTDN/SubscriptionProjector/event_receipts; matriz de estado.
- **Testes necessários:** matriz completa, reordenação/replay, consulta Play falha, revogação/refund e união com legado/promo; tester Play para eventos suportados.

### S21 — Comprovar fonte oficial de cobrança e void

- **Tipo / requisito:** fundação de evidência; DATA-003, §11.5; ST-33.
- **Objetivo:** resolver a principal incerteza financeira antes de construir qualificação/payout.
- **Descrição técnica:** demonstrar, com acesso oficial disponível à conta, como reconhecer transação real, primeira cobrança, primeira renovação e anulação/liquidação; documentar origem, campos, latência, ligação ordem↔token replacement↔UID, moeda e limitações. Produzir fixtures sanitizadas e contrato de adaptador, sem inventar endpoint.
- **Critérios de aceite:** evidência distingue trial ACTIVE de dinheiro; origem pode ser consultada/conciliada pelo backend; exemplos reais de configuração/teste oficial e limites do sandbox registrados; chave econômica e critério de pagamento são inequívocos; falta de prova mantém firstPaid/payout bloqueados.
- **Dependências:** S19; OQ-04 e AR-01.
- **Arquivos/módulos prováveis:** `docs/contracts/` financeiro e fixtures BACKEND; não novo simulador/framework.
- **Testes necessários:** reproduzir consulta/relatório oficial e comparar trial, cobrança, renovação e void; sanear dados/token antes de anexar evidência.
- **Riscos/observações:** teste de licença não prova dinheiro recebido em produção; observação real conciliada permanece gate posterior.

### S22 — Registrar primeira cobrança e renovação por identidade econômica

- **Tipo / requisito:** fundação; DATA-003, BILL-005, NFR-004/006/007, §§10.1/11.5/16; ST-26/33.
- **Objetivo:** fornecer eventos financeiros autorizados, únicos e auditáveis.
- **Descrição técnica:** integrar fonte comprovada S21; manter purchase_events/order refs, categorias trial/first_charge/renewal/refund/void e receipts com dedupe econômico inclusive linked token replacement. Atualizar firstPaidAt/firstRenewalAt somente com evidência; normalizar aliases S01 e preservar moeda/valor/plano no servidor.
- **Critérios de aceite:** trial/token isolado não define firstPaid; mesma transação via RTDN/job/retry conta uma vez; token substituído não cria novo pagante; mensal e trimestral renovam segundo ciclo real, não calendário M2; refund reconcilia registro; emitters financeiros são apenas servidor, sem segredos no GA4.
- **Dependências:** S20/S21.
- **Arquivos/módulos prováveis:** BACKEND purchase_events/subscriptions/adaptador financeiro; contratos/rules/retention próprios da S15.
- **Testes necessários:** fixtures financeiras, replay/concorrência, replacement, monthly/quarterly/trial, void e cruzamento de titulares; inspeção de logs.

### S23 — Reconciliar compras, estados e incidentes financeiros

- **Tipo / requisito:** fundação operacional; BILL-005/008, DATA-003, NFR-004/006, §§11/17; ST-08/26/32.
- **Objetivo:** recuperar eventos perdidos/falhas parciais e pagante cobrado sem direito.
- **Descrição técnica:** job autenticado reconcilia compra/ACK/estado sem RTDN, verificações pendentes e void usando fonte oficial; GET atualizado e projeção/revisão idempotentes. Emitir ticket/alerta com owner para cobrança confirmada sem entitlement e divergências, sem criar stack de observabilidade geral. Continuar com flags comerciais OFF.
- **Critérios de aceite:** compra cobrada sem acesso é incidente P0 reparável, não erro silencioso; RTDN perdida e falha após ACK recuperam; não duplica evento financeiro; `202 VERIFYING` não concede ilimitadamente; identidade/legacy review, atraso de compra e vencimento confirmado têm tratamento separado; rollback não desliga job.
- **Dependências:** S07/S20/S22; OQ-07 para owners/retention.
- **Arquivos/módulos prováveis:** BACKEND reconcile/scheduler, suporte/incidentes e receipts.
- **Testes necessários:** mensagem perdida, timeout, gravação/ACK parcial, cron repetido, refund e flags OFF; alerta e reparação auditada.

### S24 — Integrar paywall e checkout voluntário ao protótipo

- **Tipo / requisito:** produto; BILL-001–004, CORE-001, GROW-001, NFR-010, §§4.2/7; ST-06/09.
- **Objetivo:** oferecer trial/plano com consentimento, clareza e identidade visual CalcMot.
- **Descrição técnica:** adaptar `PremiumScreen.kt` ao [protótipo 03](../design/references/calcmot-prototypes-2026/03-premium.png), preservando hero/card/CTA/rodapé e revendo delta trial/trimestral/estados com OQ-12. Conectar catálogo/checkout/verify após setup+demo; informar preço/período, duração/data/termo real, renovação, cancelamento e forma de pagamento possível. Bind obfuscated account ID; antes de abrir checkout, obter lock server-side S28 da primeira fonte válida ou organic/unknown, sem exigir link/Hub. Respeitar flags/segurança.
- **Critérios de aceite:** “Ativar meus 7 dias” apenas com offer Play elegível; paid-only não promete trial; nenhuma contagem por instalação/demo; preço antigo estático removido do fluxo; fonte está travada antes de iniciar trial/compra; falha do lock tem retry, não reatribuição retroativa; checkout por toque, pending/VERIFYING/retry reais; legado nunca vê paywall Core; nenhum diálogo durante monitoramento/OCR/overlay/oferta recente/condução incerta; não interrompe serviço ativo.
- **Dependências:** S08/S13/S14/S17/S18/S19/S28; OQ-10/OQ-12. Exige apenas backend de lock, não release B/Hub ou E08.
- **Arquivos/módulos prováveis:** `UI/PremiumScreen.kt`, `CalcMotNavigation.kt`, `APP/billing/`, `entitlement/`, TEL/Clarity apenas sanitizados, referência/delta visual.
- **Testes necessários:** UI dos estados setup/demo/loading/eligible/paid-only/pending/verifying/erro; teste Play voluntário; segurança e lock; preview/screenshot comparado em dimensões/fontes do DoD.

### S25 — Restaurar compra e apresentar status/gerenciamento

- **Tipo / requisito:** produto; BILL-006/008, ENT-003, NFR-005/010, §§4.2/11; ST-09/32.
- **Objetivo:** recuperar compra paga e explicar validade sem falso bloqueio ou cancelamento.
- **Descrição técnica:** consulta/reenvio de compras no retorno/restore, mesma identidade/binding server-side; status TRIAL_ACTIVE/PAID_ACTIVE/CANCELED_WITH_ACCESS/GRACE/ON_HOLD/EXPIRED/PURCHASE_RECOVERY/LEGACY e datas confirmadas. Ações “Restaurar assinatura”, “Gerenciar assinatura na Google Play”, retry/suporte/dunning na entrada segura.
- **Critérios de aceite:** canceled válido mantém prazo; grace segue Play; hold/paused/expired só restringem novo sem outra fonte em entrada segura; pagante recém-cobrado em verificação tem recuperação prioritária; outro UID não toma token; cancelamento não afeta Legacy Core; offline não produz trial falso.
- **Dependências:** S20/S23/S24; OQ-03/OQ-10.
- **Arquivos/módulos prováveis:** `APP/billing/`, `entitlement/`, telas de assinatura/recuperação e navegação UI.
- **Testes necessários:** restore/reinstall/sessão perdida, token errado, grace→recovered/hold, cancel válido/expiry, falha de rede e UI de datas/TalkBack/screenshot.

### S26 — Trocar mensal e trimestral sem duplicar direitos

- **Tipo / requisito:** produto; BILL-003/006, §§11.1/14 BDD 6; ST-09/26.
- **Objetivo:** manter lifecycle e contabilização corretos ao trocar plano.
- **Descrição técnica:** integrar replacement/proration aprovados de OQ-05 no checkout Play; refletir plano/período/termo efetivo, linked tokens e revisão backend, usando mesmos direitos Core/Pro. Não criar cupom reatribuidor nem novo primeiro pagamento pela troca.
- **Critérios de aceite:** mensal↔trimestral tem condições reais antes da aceitação; acesso não duplica nem desaparece no período confirmado; token substituído conserva titular/origem; trial/firstPaid/renewal não são contados duas vezes; rótulo trimestral não afirma renovação em segundo mês.
- **Dependências:** S25; OQ-05.
- **Arquivos/módulos prováveis:** `APP/billing/`, UI de assinatura, BACKEND binding/projector/financial adapter.
- **Testes necessários:** replacement aceito/pending/cancelado/falho com tester e fixtures; financeiro/token chain/revisões concorrentes; UI de proration e condições.

### S27 — Homologar piloto de cobrança com gates e rollback

- **Tipo / requisito:** gate operacional; BILL-001–008, ENT-001–004, OPS-001/002, PRIV-001, NFR-001–009 relevantes, §§17/19; ST-22/24.
- **Objetivo:** tornar release C revisável sem confundir código pronto com cobrança autorizada.
- **Descrição técnica:** consolidar testes já pertencentes às stories; provar ciclo Play, financeiro/ACK/RTDN/restore/plan change/offline e baseline atualizado do Core. Revisar termos de assinatura/Data Safety/support. Executar internal/closed e submeter gate de 5% sticky de novos pós-cutoff; 25/50/100 só por evidência madura e autorização.
- **Critérios de aceite:** nenhum P0/P1 no gate fechado; compra/entitlement/legado corretos; métricas S36 disponíveis; cobrança incorreta/false-lock/Core P0/violação de privacidade/marketing durante oferta → STOP; pouca amostra/deterioração incerta → HOLD em 5%; rollback conserva compras/grants/promos, RTDN/ACK/reembolso/créditos. Não precisa de Insights ou rewards.
- **Dependências:** S16/S22/S23/S24/S25/S26/S36; cutoff aprovado, OQ-09, evidência Play/comercial humana.
- **Arquivos/módulos prováveis:** `docs/product/` gates/termos, `docs/quality/`, configurações de rollout e build de entrega.
- **Testes necessários:** E2E integrado/tipo de compra/offline/segurança, lifecycle/financeiro negativos, smoke Core atualizado, `assembleRelease`/`bundleRelease`, ensaio de rollback/alerta P0.

## E06 — Indicação orgânica atribuível e segura

- **ID / nome:** E06 — Indicação orgânica atribuível e segura.
- **Objetivo:** entregar distribuição consentida sem depender de assinatura ou incentivo.
- **Valor entregue:** link/código funcionam com/sem app e origem da primeira ativação é medível.
- **Escopo:** código/first-touch/lock, domínio/landing/App Links/Install Referrer, elegibilidade/caps, Sharesheet e Hub sem prêmio, gate B.
- **Fora de escopo:** contatos/envio automático/WhatsApp tracking, Dynamic Links, benefício financeiro, backend de dados de corridas, push de marketing.
- **Dependências:** E01/E02/telemetria/flags/privacidade conforme stories; AR-01 para domínio e OQ-06/10. Pode ser preparada junto de Billing.
- **Critérios de fechamento:** app/no-app/referrer e firstCore atribuídos; hijack/self-referral rejeitados; opt-out/caps/safety passam; release B não promete dias gratuitos nem monetização.

```text
E06
├── S28 — Emitir códigos e registrar atribuição first-touch/locked
├── S29 — Servir link, landing e assetlinks no domínio canônico
├── S30 — Receber App Link e Install Referrer no Android
├── S31 — Aplicar elegibilidade, caps e silêncio em condução
├── S32 — Compartilhar pelo Sharesheet e consultar Hub orgânico
└── S33 — Homologar distribuição sem incentivo
```

### S28 — Emitir códigos e registrar atribuição first-touch/locked

- **Tipo / requisito:** fundação; ATTR-002, REF-001, NFR-004/007, §§5/9.2/12; ST-12.
- **Objetivo:** fixar uma origem válida por indicado sem permitir autoindicação ou captura tardia.
- **Descrição técnica:** gerar código opaco aleatório/exclusivo por referrer elegível; servidor valida claim/source/campaign/método e primeira origem válida antes de trial/compra; estados provisional/locked/rejected. Registrar organic/unknown quando sem origem, nunca usar cupom tardio para substituir. Parametrizar rotação/limites conforme OQ-06.
- **Critérios de aceite:** mesmo UID indicador/indicado é rejeitado; lock é único/imutável; App Link/código depois de trial/compra só informam, não alteram canal; concorrência não troca first-touch; somente backend escreve; códigos não revelam PII; fonte driver/creator é exclusiva.
- **Dependências:** S02/S04; S01 contém contrato de interação com Billing, OQ-06.
- **Arquivos/módulos prováveis:** BACKEND attributions/referrers, contratos/rules; futuros pacotes `APP/growth/`.
- **Testes necessários:** código inválido/expirado, self-referral, duas origens/concorrência, lock antes de checkout, tentativa tardia e flags creator OFF.

### S29 — Servir link, landing e assetlinks no domínio canônico

- **Tipo / requisito:** produto; ATTR-001, REF-001, §9.2; ST-11.
- **Objetivo:** manter o código no percurso sem app e verificar associação com Android.
- **Descrição técnica:** `/r/{code}` em `https://calcmot.com.br` valida origem/campanha, registra clique server-side e leva à Play com referrer URL-encoded. Publicar `/.well-known/assetlinks.json` com SHA-256 Play App Signing de `br.com.calcmot`. Landing mínima, sem coleta silenciosa de identificadores/contatos.
- **Critérios de aceite:** código/campaign sobrevivem encoding/redirecionamento; fingerprint correto do certificado, não upload key suposta; inválido não cria atribuição; clique não é tratado como instalação/usuário único/envio; nada de Firebase Dynamic Links; iPhone não gera comissão e funil/waitlist separado não é construído sem especificação.
- **Dependências:** S28; AR-01/AR-02 para hosting/certificado/escopo separado.
- **Arquivos/módulos prováveis:** WEB/hosting do domínio (a confirmar), assetlinks, backend de clique mínimo.
- **Testes necessários:** HTTPS/path/encoding/código inválido, associação de certificado/assinatura release e redirect sem app; payload/log sanitizado.

### S30 — Receber App Link e Install Referrer no Android

- **Tipo / requisito:** produto; ATTR-001/002, §§9.2/12; ST-11/12.
- **Objetivo:** atribuir origem válida no Android com app instalado ou primeira abertura após Play.
- **Descrição técnica:** intent-filter HTTPS/host/path específico e handling em lifecycle/navigation; ler Install Referrer no first launch, validar no backend e ligar ativação canônica à origem. Binding de UID/linking segundo contrato; após lock/checkout somente conteúdo informativo.
- **Critérios de aceite:** app instalado abre `/r/code`; instalação via Play preserva referrer; repetição/session restore não reatribui; código inválido/tardio não muda fonte; origem não depende de leitura de contatos; permissões existentes preservadas, sem novo service para crescer.
- **Dependências:** S28/S29; certificado/ambiente AR-01.
- **Arquivos/módulos prováveis:** `app/src/main/AndroidManifest.xml` apenas App Link, `MainActivity.kt`, `UI/CalcMotNavigation.kt`, `APP/growth/` e dependência Install Referrer.
- **Testes necessários:** adb/tester Play com app/no-app, assinatura release, reinstall/primeira abertura, duas entradas/late-link e regressão de navegação.

### S31 — Aplicar elegibilidade, caps e silêncio em condução

- **Tipo / requisito:** produto; GROW-001, NFR-007/010, §§7/17; ST-35.
- **Objetivo:** limitar solicitações de indicação sem distrair o motorista.
- **Descrição técnica:** GrowthEligibilityPolicy lê dias reais S10, preferências e estado de segurança disponível; prompt só foreground seguro após ≥2 dias, monitoring/OCR/overlay OFF, sem oferta recente/sinal de direção. Persistir histórico: ≤2/30d, cooldown 14d após dispensa; duas dispensas →60d; opt-out durável. Não adicionar sensores/permissões para preencher OQ-10.
- **Critérios de aceite:** qualquer contexto operacional/incerto bloqueia prompt/share/paywall automático; botão voluntário usa gate de segurança sem exigir cap de prompt para abrir Hub; caps sobrevivem restart; duas dispensas silenciam 60d; eligible/shown/dismiss são eventos distintos; push marketing OFF; POST_NOTIFICATIONS não condiciona Core.
- **Dependências:** S08/S10/S14; OQ-10.
- **Arquivos/módulos prováveis:** `APP/growth/` (novo), runtime somente leitura, preferências/TEL e UI segura.
- **Testes necessários:** relógio injetado/limites 14/30/60d, duas dispensas, opt-out/restart, foreground/monitoring/overlay/OCR/recent/unknown, eligibility versus exposição.

### S32 — Compartilhar pelo Sharesheet e consultar Hub orgânico

- **Tipo / requisito:** produto; REF-001, GROW-001, ATTR-001, NFR-010, §§7/14.1; ST-13.
- **Objetivo:** tornar indicação utilizável antes de pagamentos e recompensas.
- **Descrição técnica:** ação voluntária “Indicar CalcMot” no dashboard, Hub com código/link e Sharesheet ACTION_SEND por toque; safety/caps S31 e flags. Mostrar link/ativação agregados quando disponíveis, sem progresso de dias de recompensa na release B. Sharesheet não comprova envio.
- **Critérios de aceite:** URL/código canônicos; sem contatos ou envio automático; Hub acessível a legado/novo elegível sem Pro; prompt só na entrada segura; evento share_sheet_opened não é share_sent; não promete Core/Pro grátis; nenhum dado do indicado é exposto; falha de link/API tem retry honesto.
- **Dependências:** S13/S28/S30/S31.
- **Arquivos/módulos prováveis:** `UI/CalcMotNavigation.kt`, `HomeRouteScreen.kt`, Hub novo, `APP/growth/`, TEL.
- **Testes necessários:** Compose code/link/copy/CTA, intent Sharesheet por gesto/cancelamento, safety/caps/flags, PII e screenshot/TalkBack.

### S33 — Homologar distribuição sem incentivo

- **Tipo / requisito:** gate operacional; ATTR-001/002, REF-001, GROW-001, PRIV-001, §§14.1/17.1; ST-13/19.
- **Objetivo:** fechar release B útil e segura sem aguardar cobrança.
- **Descrição técnica:** validar jornada link→app/Play→claim→primeiro valor e dashboard básico de atribuição; revisar termos mínimos da indicação sem prêmio e rollback de CTA. Integração com compras, se presente, respeita origem congelada.
- **Critérios de aceite:** app/no-app e primeiro Core atribuído observáveis; sem contatos/marketing em overlay; origem tardia imutável; sem promessa de recompensa/assinatura; poucos cliques/ativações são HOLD/aprendizado, não conversão paga; flags desligam CTA sem apagar atribuição.
- **Dependências:** S16/S32/S35; não depende de S27 ou E09.
- **Arquivos/módulos prováveis:** evidências em `docs/product/`/`docs/quality/`, configuração de flags e listing/link existentes.
- **Testes necessários:** E2E app/no-app/segurança/opt-out e rollback; build de entrega; checagem dos relatórios reais por origem.

## E07 — Coortes de ativação, aquisição e assinatura

- **ID / nome:** E07 — Coortes de ativação, aquisição e assinatura.
- **Objetivo:** medir valor/recorrência/dinheiro por população comparável antes de escalar.
- **Valor entregue:** relatórios semanais com denominadores, maturação e fontes inequívocas.
- **Escopo:** Core/ativação, funnel orgânico e assinaturas/financeiro; evolução incremental por disponibilidade de dados.
- **Fora de escopo:** plataforma BI nova, muitos A/Bs, LTV ou causalidade inventados, GA4 como comprovação de receita.
- **Dependências:** S10, trilhas de atribuição e financeiro para respectivos relatórios; AR-02.
- **Critérios de fechamento:** fórmulas/identidades/dias corretos; demo/frames excluídos; imaturos indisponíveis, não zero; mensal/trimestral/legacy/promo separados; fontes reproduzíveis e sem PII de terceiros.

```text
E07
├── S34 — Publicar coortes semanais de ativação e Core
├── S35 — Publicar funil de indicação orgânica por canal
└── S36 — Publicar coortes Play e proteção de receita
```

### S34 — Publicar coortes semanais de ativação e Core

- **Tipo / requisito:** produto analítico; DATA-002, §§8/16; ST-19.
- **Objetivo:** acompanhar ativação e recorrência antes da monetização.
- **Descrição técnica:** consulta/export/relatório semanal mínimo no meio AR-02, reaproveitando consolidação existente quando aplicável. FirstOpen→setupReady→firstCore em 24/72h, mediana até valor; CAD7/CAD30 ≥2 dias, CoreUsers ≥1, D1/D7/D30 exatos e recorrência mensal separados por origem/versão.
- **Critérios de aceite:** mesmos UIDs/coortes/timezone do contrato; ativação72h usa novos maturados ≥72h; D30 imaturo = indisponível; dados parciais rotulados; nenhuma soma GA4+Play como população única; demo/offer_parsed repetido excluídos; não chamar M0/M1 de D30 ou grátis de pago.
- **Dependências:** S10; AR-02.
- **Arquivos/módulos prováveis:** `CalcMot-Metrics-2026-08-to-current/` consolidação/relatórios e consultas previstas, sem modificar exports fonte.
- **Testes necessários:** coorte sintética com duplicação, timezone, primeira sessão, dias exatos/janela rolling/maturação; reconciliar totais com amostra sanitizada.

### S35 — Publicar funil de indicação orgânica por canal

- **Tipo / requisito:** produto analítico; DATA-002, ATTR-001/002, §§8/16; ST-19.
- **Objetivo:** distinguir compartilhamento, clique, atribuição e ativação efetiva.
- **Descrição técnica:** relatório elegíveis→CTA/prompt shown→Sharesheet→click→instalação atribuída→referred_first_core, organic/driver/creator conforme disponíveis. K_active = novos atribuídos com primeira análise/elegíveis no período; explicitar ausência de ID único no clique. Acrescentar paid/qualified/applied apenas com fontes financeiras de stories posteriores.
- **Critérios de aceite:** abrir Sharesheet não é envio; clique não é usuário único/install; lock conserva canal; denominadores/janelas/métodos explícitos; dados financeiros ausentes = indisponíveis; relatório não depende de reward para funcionar e não expõe indicado.
- **Dependências:** S28/S30/S32/S34; AR-02.
- **Arquivos/módulos prováveis:** relatórios/consultas de métricas e export agregado BACKEND.
- **Testes necessários:** fixture de cada etapa, duplicação/lock/origem desconhecida, ausência de maturação/dinheiro e cálculo de K_active.

### S36 — Publicar coortes Play e proteção de receita

- **Tipo / requisito:** produto analítico; DATA-002/003, BILL-008, §§8/16/17; ST-19/26.
- **Objetivo:** fundamentar o piloto com receita real e sinais de dano ao Core/legado.
- **Descrição técnica:** trials maduros→first_charge, cancel/expiry/refund, mix de planos e primeira renovação/M2/M3 quando observável; separar fonte Play/ordem de analytics e paid/promo/legacy. Mostrar erros de purchase/ACK, compra sem acesso, false-lock, perda de cache, reclamações, opt-out e Core/ANR/latência disponíveis.
- **Critérios de aceite:** trial ACTIVE não entra em receita; trimestral R$25 não é MRR R$25; renovação trimestral só após ciclo real; coortes imaturas são indisponíveis; antes/depois controla versão/origem/tempo e não promete significância; toda anomalia financeira tem owner/ticket; dados pessoais/tokens não entram no relatório.
- **Dependências:** S20/S22/S34; AR-02/OQ-07.
- **Arquivos/módulos prováveis:** relatórios/consultas métricas e BACKEND export agregado financeiro/operacional.
- **Testes necessários:** trial versus paid, monthly/quarterly, refund, falta de maturação, paid/promo e denominadores; reproduzir relatório semanal.

## E08 — Pro Insights adicional validado para legados

- **ID / nome:** E08 — Pro Insights adicional validado para legados.
- **Objetivo:** oferecer somente a vantagem nova descrita no PRD, caso sua utilidade seja comprovada.
- **Valor entregue:** painel semanal de uso/indicadores, distinto do Core que já era gratuito.
- **Escopo:** validação de interesse/protótipo e painel de dias/análises/distribuição com dados locais canônicos.
- **Fora de escopo:** remover histórico/ajustes gratuitos, backend de corridas, economia/lucro real alegados, localização, features premium adicionais.
- **Dependências:** S10/S08; gate OQ-11. Monetização de novos não depende deste epic.
- **Critérios de fechamento:** benefício implementado e testado somente após evidência de utilidade; Core antigo intacto; acesso Pro verificado; se gate falhar, epic permanece HOLD e rewards de legados OFF, compartilhamento voluntário continua.

```text
E08
├── S37 — Validar utilidade do painel semanal com legados
└── S38 — Entregar painel semanal com dados canônicos locais
```

### S37 — Validar utilidade do painel semanal com legados

- **Tipo / requisito:** gate de produto; §5.3/18; ST-17.
- **Objetivo:** impedir promessa de Pro fictício apenas para ativar campanha.
- **Descrição técnica:** preparar protótipo limitado a dias de cálculo, análises canônicas e distribuição de indicadores; testar intenção explícita de uso com legados e registrar evidência/decisão segundo OQ-11. Não transformar pesquisa em oferta paga ou recompensa já disponível.
- **Critérios de aceite:** protótipo distingue estimativa de lucro real e não retira Core/histórico; critérios de interesse e resultado rastreáveis; owner aprova GO/HOLD; sem validação/benefício funcional, reward legado permanece OFF. Sem quota/taxa inventada de aprovação.
- **Dependências:** S10; OQ-11 para critério de decisão e participantes.
- **Arquivos/módulos prováveis:** `docs/design/references/` do fluxo e evidências `docs/product/` futuras.
- **Testes necessários:** leitura/revisão visual do protótipo e pesquisa de interesse; critérios verificáveis aprovados, sem teste de código artificial.

### S38 — Entregar painel semanal com dados canônicos locais

- **Tipo / requisito:** produto condicional; §5.3, REF-005, NFR-007/010; ST-17.
- **Objetivo:** disponibilizar benefício Pro real e leve sem enviar corridas ao backend.
- **Descrição técnica:** agregar localmente análises/dias/classificações deduplicadas S10 no recorte aprovado; UI semanal nativa com acesso por Play/promo verificado S08. Dados insuficientes têm estado vazio honesto. Não reutilizar banco de câmera nem adicionar histórico de OCR/endereço.
- **Critérios de aceite:** painel mostra apenas três dimensões do PRD; quantidade/distribuição batem com eventos canônicos e não frames/demo; nada de “R$ economizados” ou lucro real; Core/histórico/ajustes existentes continuam grátis ao legado; Pro expira sem retirar Core; sharing de link permanece disponível sem Pro.
- **Dependências:** S08/S10/S37 com GO explícito; OQ-11.
- **Arquivos/módulos prováveis:** `APP/growth/` ou pacote Pro previsto, agregados locais existentes/novos mínimos, UI/DS/navegação e entitlement.
- **Testes necessários:** agregação/dedupe/semana/empty, fontes paid/promo/legacy, expiry; UI/screenshot/TalkBack; inspeção de ausência de dados individuais no backend.

## E09 — Referral com recompensa acumulada e aplicação confirmada

- **ID / nome:** E09 — Referral com recompensa acumulada e aplicação confirmada.
- **Objetivo:** premiar aquisição paga válida uma vez, com efeito real e transparente.
- **Valor entregue:** indicador acompanha 7/30/60 dias totais e datas de defer/promo confirmadas.
- **Escopo:** qualificação pós-pagamento/janela de void, ledger/marcos, defer seguro, promoção/resgate e progresso UI; gate reward.
- **Fora de escopo:** prêmio por trial/click/install, 97 dias, reinício automático de campanha, comissão simultânea, defer indiscriminado, renovação automática por promoção.
- **Dependências:** financeiro verificado e reconciliado E05, origem E06; OQ-05/06; benefício legado depende ainda de E08 GO/funcional.
- **Critérios de fechamento:** distintos novos pagantes qualificam uma vez; máximo 60d por campanha; timeout de defer não duplica aplicação; crédito não aplicado não se perde/mascara cobrança; UI só confirma após efeito; gate D reward mantém legado sem benefício adicional fora da campanha incentivada.

```text
E09
├── S39 — Qualificar indicado somente após pagamento válido
├── S40 — Conceder marcos no ledger de 7/30/60 totais
├── S41 — Adiar renovação elegível e reconciliar defer
├── S42 — Resgatar promoção sem assinatura/cobrança conflitante
├── S43 — Apresentar progresso, crédito e datas efetivas
└── S44 — Homologar piloto de reward com evidência financeira
```

### S39 — Qualificar indicado somente após pagamento válido

- **Tipo / requisito:** produto; REF-002, DATA-003, NFR-004/006, §§5.1/11.2/12; ST-14.
- **Objetivo:** contar novos pagantes distintos, não pagamentos repetidos ou trials.
- **Descrição técnica:** ReferralQualificationService usa first-charge verificado S22, titular novo/origem locked e janela refund/void OQ-06; transação única por invitee/campaign/identidade econômica. Antifraude mínimo aprovado, revisão para casos suspeitos, sem fingerprinting/contatos. Origem creator encaminha comissão, não reward driver.
- **Critérios de aceite:** trial/install/click/renovação/replay não pontuam; estorno na janela desqualifica; mesma conta/new payer qualifica uma vez; self-referral/fonte inválida/provisório não regularizado não geram prêmio; canais exclusivos; reversão tardia segue termos/revisão, sem retirar dias usados silenciosamente.
- **Dependências:** S22/S23/S28; OQ-06 e comprovação de firstPaid em produção antes de ativar campanha.
- **Arquivos/módulos prováveis:** BACKEND referrals/qualification/purchase bindings, rules/retention da própria entidade; relatório S35 incrementado com estágio financeiro.
- **Testes necessários:** trial ACTIVE, distintos/replay/linked tokens, void antes/depois da janela, self-referral/revisão e concorrência driver/creator; trilha de decisão auditável.

### S40 — Conceder marcos no ledger de 7/30/60 totais

- **Tipo / requisito:** produto; REF-003, NFR-004/006, §§5.1/11.2.1; ST-15.
- **Objetivo:** registrar direito conquistado sem inflar dias nem chamar Play dentro de transação.
- **Descrição técnica:** transação de campanha calcula cardinalidade de qualificados únicos e chave `campaignId:inviterUid:milestone`; target 1→7/3→30/5→60, delta 7/23/30 pela trilha acumulada. Separar earned/applied, reservar operação e estado do ledger; API agregada própria sem autoridade de concessão cliente.
- **Critérios de aceite:** contagens 0/1/2/3/4/5/6 dão 0/7/7/30/30/60/60 totais; dias usados não reiniciam marco; replay/concorrência não duplica direito; efeito externo fora de transação; `APPLIED` só após confirmação; cliente não escreve/qualifica; credit pending não é “ganhou grátis”.
- **Dependências:** S39.
- **Arquivos/módulos prováveis:** BACKEND reward_ledger/referrals/GET referrals; contracts/rules e relatório S35.
- **Testes necessários:** toda progressão e out-of-order, marcos após consumo, transações concorrentes/rollback, chave única e canal remunerado exclusivo.

### S41 — Adiar renovação elegível e reconciliar defer

- **Tipo / requisito:** produto; REF-004, NFR-004/006, §§11.2/11.5; ST-16.
- **Objetivo:** aplicar dias conquistados adiando a próxima cobrança somente quando permitido.
- **Descrição técnica:** lock lógico por tokenHash; GET Play/eligibilidade/etag/preExpiry, reservar operationId/APPLYING e enviar subscriptionsv2.defer uma vez por operação com deltaDuration 604800/1987200/2592000s conforme marco. Confirmar itemExpiryTimeDetails/GET; timeout após envio → NEEDS_RECONCILIATION, revisar efeito antes de qualquer nova tentativa. Usar validateOnly para prova de elegibilidade, não como aplicação.
- **Critérios de aceite:** somente assinatura/linha elegível usa defer; novo expiry real confirmado antes de APPLIED; etag conflitante volta à decisão sem duplicar; resposta perdida não causa reenvio cego; estado inconclusivo vai à revisão; RTDN deferred não gera dias; flags OFF conservam reconciliação da operação enviada.
- **Dependências:** S23/S40; OQ-05 e acesso API oficial.
- **Arquivos/módulos prováveis:** BACKEND RewardService/Play adapter/reconcile, ledger e documentação de incidente.
- **Testes necessários:** dry-run/tester elegível, conflito etag, concorrência, envio aceito/resposta perdida, mudança de expiry por outra causa e revisão manual; nenhuma chamada Play na transação Firestore.

### S42 — Resgatar promoção sem assinatura/cobrança conflitante

- **Tipo / requisito:** produto; REF-005, ENT-003, NFR-004/006/010, §§5.2/5.4/11.5; ST-16/27.
- **Objetivo:** aplicar crédito sem token elegível de modo voluntário, datado e sem cobrar em paralelo.
- **Descrição técnica:** servidor avalia fontes/período pago/renovação e mantém EARNED_UNAPPLIED quando não seguro; no resgate válido concede PROMO_PRO start/end por rewardId e projeta snapshot. Cliente solicita intenção autenticada pelo contrato S01; nunca chama endpoint interno ou concede direito. Prazo de resgate aprovado OQ-06, proposta de 90d não fixa.
- **Critérios de aceite:** sem assinatura pode resgatar promo sem criar checkout/autorrenovação; cancel/grace/hold/trial/pausa não dispara defer ou promo conflitante; dias conquistados não somem por condição temporária; datas/modo aparecem antes de aceitar; promo expira sem afetar grant; legado só recebe Insights se S38 funcional, caso contrário rewards desse segmento OFF.
- **Dependências:** S08/S23/S40; OQ-06/OQ-08; S38 apenas para modalidade legado.
- **Arquivos/módulos prováveis:** BACKEND promo_entitlements/reward/entitlement; UI de resgate e `APP/entitlement/`/`growth/`; rules/retention incrementais.
- **Testes necessários:** sem token, paid ainda válido/recorrente, canceled/grace/hold/trial, resgate repetido/expiração, legacy com/sem Insights; UI de datas/cobrança e snapshot.

### S43 — Apresentar progresso, crédito e datas efetivas

- **Tipo / requisito:** produto; REF-002–005, GROW-001, NFR-010, §§5/7; ST-13/27.
- **Objetivo:** mostrar o que foi conquistado e o que realmente mudou no direito/cobrança.
- **Descrição técnica:** evoluir Hub orgânico com 0–5, status de qualificação anônimo, earned/applied/pending/void/review e próximo marco. Exibir “próxima cobrança adiada até [Play]” ou “Pro de X até Y, sem renovação automática”; ação de resgate/restore/suporte nos casos previstos. Filtro de segmento evita promessa Pro ao legado sem S38.
- **Critérios de aceite:** trial indicado aparece pendente, não qualified; nunca soma 7+30+60; UI não chama “ganhou grátis” antes de APPLIED; dias em crédito e datas confirmadas separados; sem PII/UID do indicado; estados inconclusivos têm recuperação; segurança/flags do Hub preservadas.
- **Dependências:** S32/S40/S41/S42; S38 para exibir benefício legado.
- **Arquivos/módulos prováveis:** Hub UI, `APP/growth/`/`billing/`/`entitlement/`, endpoint agregado BACKEND e TEL seguro.
- **Testes necessários:** UI de cada count/state e modalidade, delayed/void/review, caps/opt-out, payload anônimo; screenshot/TalkBack/fonte ampliada.

### S44 — Homologar piloto de reward com evidência financeira

- **Tipo / requisito:** gate operacional; REF-002–005, PRIV-001, OPS-001, NFR-004/006, §§17/18; ST-23/24.
- **Objetivo:** liberar incentivo somente após firstPaid real e aplicação confiável.
- **Descrição técnica:** revisar termos/janela/antifraude/resgate, custos de campanha e incidentes; evidência de firstPaid conciliado em produção, teto/marcos/exclusividade, defer com falha/reconciliação e promo sem cobrança. Submeter GO/HOLD/STOP do reward, mantendo reconciliação e direitos prometidos no rollback.
- **Critérios de aceite:** nenhum reward duplicável, double benefit ou firstPaid fictício; orçamento/CAC cash/econ acompanhados sem confundir oportunidade com caixa; termos e owner claros; compra sem entitlement/duplicata/privacidade/UI durante overlay →STOP; legado sem S38 permanece sem prêmio; nenhum botão/flag ativa piloto automaticamente ao fechar testes.
- **Dependências:** S27/S35/S39/S40/S41/S42/S43; OQ-06 e aprovação comercial. S38 com GO é condição adicional somente para legado.
- **Arquivos/módulos prováveis:** termos/gates `docs/product/`, evidências QA e configuração da campanha/segmento.
- **Testes necessários:** E2E qualificação→1/3/5→defer/promo com void/timeouts/rollback/canal; build de entrega; aproveitar testes das stories, sem concentrá-los aqui.

## E10 — Creator CPA conciliado e custo por canal

- **ID / nome:** E10 — Creator CPA conciliado e custo por canal.
- **Objetivo:** executar piloto de creator remunerado por compra/renovação real, com contribuição observada.
- **Valor entregue:** parceiro tem resumo sem PII, comissão auditável e liquidação operacional por lote.
- **Escopo:** registro/código creator, ledger R$3+R$1, export/conciliação de payout, scorecard/CAC/contribuição e gate de três creators.
- **Fora de escopo:** fee fixo, pagar lead/trial/iPhone, comissão e dias pela mesma compra, payout por taxa líquida fixa, marketplace/processor de pagamentos novo, escala automática.
- **Dependências:** atribuição S28, financeiro E05, qualificação/marcos mínimos S39/S40; OQ-06/07/AR-02.
- **Critérios de fechamento:** primeira compra e primeira renovação liquidam uma vez; ciclos distintos mensais/trimestrais; nenhum dado pessoal de indicado; reais custos/repasse/refund sustentam GO, não comissão isolada; payout auditado/rollback continuam após flags OFF.

```text
E10
├── S45 — Cadastrar creator e expor resumo agregado
├── S46 — Registrar comissão exclusiva de primeira compra/renovação
├── S47 — Exportar e conciliar lotes de comissão
├── S48 — Publicar scorecard e economia real por canal
└── S49 — Homologar piloto de três creators
```

### S45 — Cadastrar creator e expor resumo agregado

- **Tipo / requisito:** produto; AFF-001, OPS-003, NFR-007, §§6/10/11; ST-18.
- **Objetivo:** atribuir creator de forma exclusiva e permitir acompanhar etapas sem acesso ao motorista.
- **Descrição técnica:** registro admin de creator ativo/slug/rates/código via fonte S28, campanha/flag affiliate; instruções de payout ficam em referência protegida separada. Endpoint próprio com clicks/activated/paid/eligible/payout agregados disponíveis, sem UI administrativa ampla.
- **Critérios de aceite:** creator vê apenas próprio agregado, sem UID/nome/telefone/cidade/endereço; rates congelados da V1 não são editáveis pelo cliente; code bloqueado/flag OFF não cria novo canal remunerado; fonte travada não muda por código tardio; etapa financeira ainda ausente não é inventada.
- **Dependências:** S28; OQ-06/OQ-07 para contrato/acesso protegido.
- **Arquivos/módulos prováveis:** BACKEND creators/referrers/summary/RBAC e contratos/termos.
- **Testes necessários:** papéis admin/creator/driver, acesso cruzado/PII, code/source/lock, resumo agregado com estágio ausente e flag.

### S46 — Registrar comissão exclusiva de primeira compra/renovação

- **Tipo / requisito:** produto; AFF-001, DATA-003, NFR-004/006, §§6/11.2.1; ST-18.
- **Objetivo:** criar somente R$3 na primeira cobrança válida e R$1 na primeira renovação paga.
- **Descrição técnica:** usar eventos financeiros S22/qualificação S39 e origem creator locked; ledger em centavos/moeda/estado/referência com chave econômica e creator+invitee+stage. Reservar canal exclusivo antes de reward/commission; janela/void/review conforme contrato, sem pagar por ACTIVE/trial ou calendário M2.
- **Critérios de aceite:** primeira compra gera 300 centavos e primeira renovação 100 uma vez cada; total V1 por indicado máximo R$4; trial/lead/iPhone/refund ineligível não geram comissão; token replacement/webhook duplicado não duplicam; trimestral só remunera renovação após ciclo real; mesma compra não financia driver reward; void tardio auditado segundo termos.
- **Dependências:** S22/S23/S39/S40/S45; OQ-06.
- **Arquivos/módulos prováveis:** BACKEND commission_ledger/qualification/financial events, rules/retention próprias; export agregado creator.
- **Testes necessários:** firstPaid/firstRenewal monthly/quarterly, replay/concorrência/replacement, void e corrida driver reward versus creator; regras e logs.

### S47 — Exportar e conciliar lotes de comissão

- **Tipo / requisito:** produto operacional; AFF-001, OPS-003, NFR-004/006/007, §§6.1/11; ST-18.
- **Objetivo:** tornar payout comprovável sem criar infraestrutura financeira não prevista.
- **Descrição técnica:** selecionar somente comissões elegíveis, gerar lote/export protegido, conciliar retorno/prova de pagamento por operador autorizado e marcar settledAt/paidAt idempotente. Registrar prazo/owner do contrato; void/review não liquidados. Nunca calcular repasse por percentual fixo de bruto; não adicionar gateway bancário automático.
- **Critérios de aceite:** repetir export/settle não paga outra vez; valor/ref/moeda e total batem com ledger; lote parcial/falho permanece reconciliável; aprovação/RBAC e auditoria preservados; instrução bancária separada/protegida; reward flag OFF não apaga comissão pendente/paga; exclusão/retention segundo política.
- **Dependências:** S15/S46; OQ-06/OQ-07 e operação externa de pagamento.
- **Arquivos/módulos prováveis:** BACKEND CreatorSettlement/admin/export e runbook/contrato operacional.
- **Testes necessários:** lote repetido/parcial, settle duplicado, void/review, RBAC, reconciliação e dados sensíveis no export/log.

### S48 — Publicar scorecard e economia real por canal

- **Tipo / requisito:** produto analítico; OPS-003, DATA-002, §§1.2/6/16/18; ST-28/36.
- **Objetivo:** medir CAC/contribuição e qualidade sem inferir rentabilidade da comissão isolada.
- **Descrição técnica:** relatório por creator/driver/organic, plano/coorte: firstPaid, refund, firstRenewal, CAD30 e custo; incorporar repasse/ordens reais, tarifas, tributos e custos incrementais disponíveis. Separar CAC_cash_total de CAC_econ_total/custo de oportunidade justificável; payback só com dados observáveis, valores ausentes sinalizados.
- **Critérios de aceite:** R$3+R$1 não é prova de CAC<4; R$8/80% e oportunidade de reward são sensibilidades rotuladas, nunca lucro/payout observado; trimestral segregado/competência real; não calcular LTV observado sem churn pago; nenhum PII; qualidade/renovação madura ou indisponível, sem falsa causalidade.
- **Dependências:** S34/S35/S36/S39/S46/S47; AR-02 e acesso a custos/repasse.
- **Arquivos/módulos prováveis:** relatórios/consultas/exports existentes de métricas e resumo BACKEND.
- **Testes necessários:** custos/plans/currencies/void, receita versus competência, denominador zero/imaturidade, oportunidade versus caixa e reprodução do scorecard.

### S49 — Homologar piloto de três creators

- **Tipo / requisito:** gate operacional; AFF-001, OPS-003, PRIV-001, OPS-001, §§17/18; ST-23/24/36.
- **Objetivo:** autorizar piloto pequeno somente com contabilidade e contribuição observáveis.
- **Descrição técnica:** conferir contratos de três creators, prazo de payout/publicidade identificada, RBAC, primeira cobrança real/renovação e lote conciliado. Submeter custo total/firstPaid/refund/firstRenewal/qualidade e canal exclusivo ao gate; creator pode ser homologado sem esperar abertura da campanha driver reward, mas exclusividade integrada deve passar.
- **Critérios de aceite:** nenhuma comissão sem dinheiro ou por lead/trial/refund; dados do motorista não são entregues; teto CAC é alvo a reavaliar, escala exige contribuição observada/autorização; trimestral imaturo não simula renovação; flags OFF suspendem novas comissões/CTAs mantendo liquidação de obrigações anteriores; violação → STOP.
- **Dependências:** S13/S27/S46/S47/S48; OQ-06/07 e autorização comercial; não depende de S44 ou S38.
- **Arquivos/módulos prováveis:** termos/gate/runbook `docs/product/` e configuração creator.
- **Testes necessários:** E2E compra→comissão→lote/void/rollback, competição de canais e access control; evidência de repasse/custos e revisão comercial.

## E11 — Cartão compartilhável e review sem incentivo

- **ID / nome:** E11 — Cartão compartilhável e review sem incentivo.
- **Objetivo:** acrescentar duas ações de divulgação honestas, independentes de remuneração.
- **Valor entregue:** cartão local verdadeiro e solicitação de review responsável.
- **Escopo:** imagem local sanitizada com link e integração In-App Review em contexto seguro.
- **Fora de escopo:** screenshot real de motorista, economia/viagens inventadas, recompensa por review, pré-pergunta de satisfação, push marketing.
- **Dependências:** S10/S13/S31/S32, Insights somente para cartão baseado no painel; OQ-12.
- **Critérios de fechamento:** share só por gesto/segurança e sem dados sensíveis; review separado de referral, sem review_submitted fictício; flags/cadência e QA UI verificados.

```text
E11
├── S50 — Compartilhar cartão local verdadeiro com link
└── S51 — Solicitar In-App Review em contexto seguro
```

### S50 — Compartilhar cartão local verdadeiro com link

- **Tipo / requisito:** produto P2; GROW-002, NFR-007/010, §13; ST-20.
- **Objetivo:** permitir divulgação visual baseada apenas no valor realmente disponível.
- **Descrição técnica:** gerar cartão local leve com conteúdo aprovado OQ-12 e link opaco, reutilizando agregados S38 quando citados. Compartilhar por gesto via fluxo Android já seguro; somente imagem do cartão, não captura de app/overlay/terceiros. Sem serviço de imagem/Figma/runtime remoto obrigatório.
- **Critérios de aceite:** texto/contagem correspondem a dados disponíveis; não promete economia, corridas aceitas ou ganhos; sem nome/telefone/endereço/OCR/coordenadas; link atribuído correto; zero envio automático; safety/opt-out/flag aplicáveis respeitados; Sharesheet não prova envio.
- **Dependências:** S13/S32/S38; OQ-12. Gate de Pro falho mantém esta story HOLD se cartão depender do painel, conforme ST-20.
- **Arquivos/módulos prováveis:** `APP/growth/`, UI de share/card e mecanismo local de URI compartilhável a confirmar; não alterar provider/permissão sem necessidade explícita.
- **Testes necessários:** conteúdo/PII/link/URI, share cancelado/arquivo temporário, segurança, preview/screenshot e limites de dados vazios.

### S51 — Solicitar In-App Review em contexto seguro

- **Tipo / requisito:** produto P2; GROW-003, NFR-010, §§7/16; ST-21.
- **Objetivo:** solicitar avaliação sem manipular satisfação nem misturar recompensa.
- **Descrição técnica:** integrar fluxo Play Review após elegibilidade/cadência aprovada OQ-12 e safety gate; flag separada, nenhum reward/conexão com milestones ou pré-pergunta. Registrar review_flow_requested como tentativa de UX, sem deduzir nota/envio de callback.
- **Critérios de aceite:** nenhum prompt no monitoramento/overlay/OCR/direção incerta; não é condição de acesso/promo; não promete benefício nem filtra usuários insatisfeitos; cancelamento/não exibição não afeta Core; não existe review_submitted inventado; cadência/flag persistentes conforme contrato.
- **Dependências:** S10/S13/S31; OQ-12.
- **Arquivos/módulos prováveis:** `APP/growth/` adapter review, UI segura/TEL e dependência oficial Play Review quando necessária.
- **Testes necessários:** elegibilidade/segurança/flags, fluxo não exibido/erro/cancelamento com tester; payloads e independência de rewards.

## E12 — Primeiro experimento de aquisição na Play Store

- **ID / nome:** E12 — Primeiro experimento de aquisição na Play Store.
- **Objetivo:** testar uma mensagem visual de valor com aquisição qualificada observável.
- **Valor entregue:** aprendizado de listing sem aguardar Billing.
- **Escopo:** uma screenshot variante/hipótese, comparação e registro de aquisição/ativação.
- **Fora de escopo:** várias mudanças simultâneas, anúncio de afiliação oficial, downloads como retenção, escala paid media, redesign do app.
- **Dependências:** acesso Console/relatórios; nenhuma story de assinatura. Ativação canônica depende de S10/S34 estar disponível, sem impedir preparação/abertura do experimento.
- **Critérios de fechamento:** teste ≥7 dias, uma variável, fonte/população/maturação explícitas; aquisição qualificada medida ou conclusão em HOLD por falta de dados; nenhum vencedor estatístico declarado sem evidência.

```text
E12
└── S52 — Executar ASO com uma screenshot e hipótese única
```

### S52 — Executar ASO com uma screenshot e hipótese única

- **Tipo / requisito:** produto/gate de experimento; ASO-001, §§17/18; ST-34.
- **Objetivo:** aprender se a mensagem de rentabilidade estimada melhora aquisição qualificada.
- **Descrição técnica:** usar UI real para variante “A corrida compensa? Confira R$/km, R$/h e semáforo”; Store Listing Experiment de uma screenshot por vez durante ≥7d. Registrar baseline/visitas/conversão e qualidade de ativação quando disponível; evitar alterar preço/trial simultaneamente no período interpretável.
- **Critérios de aceite:** só uma hipótese visual; sem ganhos garantidos ou afiliação oficial Uber/99; visitantes/downloads não viram retenção; relatório informa datas, população, Core activation e maturação; sem S10/S34 ou atribuição comparável, qualidade fica indisponível/HOLD, não sucesso presumido; não depende de Billing.
- **Dependências:** nenhuma story para preparar teste; AR-01/AR-02 para acesso; S10/S34 para concluir a leitura canônica de aquisição qualificada.
- **Arquivos/módulos prováveis:** assets/listing existentes em `docs/design/`/`docs/play-store-submission.md`, evidência de experimento `docs/product/`; Console externo.
- **Testes necessários:** revisar copy/UI real/legibilidade e configuração de única variante; reproduzir comparação e monitorar ≥7d. Publicação/ativação requer gate externo, não executada nesta tarefa.

## Rastreabilidade dos requisitos funcionais

Cada linha remete às stories e seus testes/critérios acima. Requisitos da tabela §13 são preservados; detalhes dos demais parágrafos aparecem nas descrições/observações e gates.

| Requisito | Stories que o satisfazem |
|---|---|
| ENT-001 | S05/S07/S09/S16 |
| ENT-002 | S04/S06/S09/S16 |
| ENT-003 | S07/S08/S20/S25 |
| ENT-004 | S06/S07/S08/S09 |
| BILL-001 | S18/S24 |
| BILL-002 | S17/S18/S24/S27 |
| BILL-003 | S18/S24/S26 |
| BILL-004 | S19/S24 |
| BILL-005 | S19/S20/S22/S23 |
| BILL-006 | S20/S25/S26 |
| BILL-007 | S03/S18 |
| BILL-008 | S20/S23/S25/S36 |
| ATTR-001 | S29/S30/S33 |
| ATTR-002 | S28/S30/S33 |
| REF-001 | S28/S29/S32 |
| REF-002 | S39/S43/S44 |
| REF-003 | S40/S43/S44 |
| REF-004 | S41/S43/S44 |
| REF-005 | S38/S42/S43/S44 (S38 condicional ao gate Pro) |
| AFF-001 | S45/S46/S47/S49 |
| GROW-001 | S14/S24/S31/S32/S43 |
| GROW-002 | S50 |
| GROW-003 | S51 |
| DATA-001 | S10/S11 |
| DATA-002 | S34/S35/S36/S48 |
| DATA-003 | S21/S22/S23/S36/S39/S46 |
| OPS-001 | S13/S16/S27/S44/S49 |
| OPS-002 | S03/S16/S27 |
| OPS-003 | S45/S47/S48/S49 |
| PRIV-001 | S02/S14/S15 e revisão/retention das entidades em S22/S28/S39–S47; gates S16/S27/S33/S44/S49 |
| CORE-001 | S17/S24 |
| CORE-002 | S03/S11/S12 e baseline atualizado S27 |
| ASO-001 | S52 |

## Rastreabilidade dos não funcionais e restrições

| Requisito | Implementação/validação específica |
|---|---|
| NFR-001 — p95 sem piora material | baseline/limites S12; comparação ao integrar S10/S11 e gate S27; OQ-09 |
| NFR-002 — nenhuma chamada externa por oferta | S08; observação assíncrona S10/S11; teste de integração S24/S27, nunca consulta Billing no Core |
| NFR-003 — crash/ANR sem regressão | S12/S27 e regressão pertinente nos seams S08/S10/S11; não épico de ANR/refactor |
| NFR-004 — idempotência financeira | S02/S19/S20/S22/S23/S28/S39–S42/S46/S47 com chaves/transações/efeitos externos confirmados |
| NFR-005 — cache em falha transitória | S08/S25; teste offline e recuperação S16/S27, OQ-03 |
| NFR-006 — reconstrução de cobrança/reward | receipts/bindings/ordens S19–S23, ledgers/operação S39–S42/S46/S47, incidentes S23 |
| NFR-007 — minimização | S02/S10/S11/S14/S15, rules/retention/payload próprios em stories de atribuição/financeiro/rewards/creators/relatórios/cartão |
| NFR-008 — rollout sem atualização | S13/S27/S44/S49; defaults OFF e reconciliação não interrompida |
| NFR-009 — Android 10+ já suportado | S11/S12/S18/S30; matriz/compatibilidade de libs, sem reduzir suporte minSdk 24 existente |
| NFR-010 — acessibilidade UI | DoD UI e testes em S09/S14/S15/S17/S24–S26/S31/S32/S38/S42/S43/S50/S51; screenshot real/preview, fonte ampliada/TalkBack |

**Restrições negativas preservadas:** sem alteração funcional dos pipelines; sem aceitar/rejeitar corrida automaticamente; sem contatos, dados brutos de oferta/screenshot de motorista, fingerprinting antifraude, rede social ou marketing em condução; sem plano anual, trial 14d, reajuste ou iOS pago; sem benefício Pro inexistente, paywall retroativo ou certeza técnica de restore sem identidade. Sem notificações comerciais na V1; futuro opt-in ≤1/30d é contexto futuro, não story de push. Sem LTV/conversão/causalidade fabricados. A conta Google indicada pelo usuário pode ser usada se design externo se tornar necessário; esta entrega usa somente referência local, sem conectar/publish/design externo.

## Reconciliação com as 36 stories de origem do PRD

Não implementar IDs ST diretamente junto dos S: esta tabela explica a decomposição e evita duplicidade. Splits separam incrementos/API/UI/efeito externo/prova; gates ST-22/23 não absorvem testes pertencentes às stories.

| Origem PRD | Backlog executável |
|---|---|
| ST-01 | S01/S10/S34 |
| ST-02 | S04/S09 |
| ST-03 | S05/S06/S07 |
| ST-04 | S07/S08 |
| ST-05 | S16 |
| ST-06 | S18/S24 |
| ST-07 | S19 |
| ST-08 | S20/S23 |
| ST-09 | S08/S24/S25/S26 |
| ST-10 | S13 |
| ST-11 | S29/S30 |
| ST-12 | S28/S30 |
| ST-13 | S31/S32/S43 |
| ST-14 | S39 |
| ST-15 | S40 |
| ST-16 | S41/S42 |
| ST-17 | S37/S38 |
| ST-18 | S45/S46/S47 |
| ST-19 | S34/S35/S36 |
| ST-20 | S50 |
| ST-21 | S51 |
| ST-22 | S27 |
| ST-23 | S44/S49 |
| ST-24 | S14/S15/S16/S27/S33/S44/S49 |
| ST-25 | S06/S09/S16 |
| ST-26 | S19/S20/S22/S23/S26 (fixtures/testes, sem simulador interno novo) |
| ST-27 | S42/S43 |
| ST-28 | S48 |
| ST-29 | S03/S18 |
| ST-30 | S17 |
| ST-31 | S11/S12 |
| ST-32 | S23/S25 |
| ST-33 | S21/S22 |
| ST-34 | S52 |
| ST-35 | S31 |
| ST-36 | S48/S49 |

## Fechamento documental desta entrega

Foram criados apenas este backlog e [o plano de execução](EXECUTION-PLAN.md). Caminhos novos de código são propostas de localização, não implementação. Perguntas/assunções permanecem abertas até resposta/evidência; o plano indica quais podem avançar e quais gates bloqueiam. Nenhum build, teste Android, Console, deploy, alteração de flag, assinatura, conta Google ou design externo foi executado. Verificação desta entrega: cobertura de IDs/campos/requisitos, grafo acíclico, fases topológicas, paralelismos e referências locais; resultados no plano.

## Atualização de execução S02/S04 — 2026-10-13

**S02/S04 permanecem PARTIAL**; [relatório corrente](S02-S04-INTEGRATION.md) distingue o fechamento objetivo das evidências anteriores. Nesta continuação: um teste de isolamento em D1 remoto com duas fixtures sem direitos/limpeza e cinco testes novos de controles aprovados; typecheck aprovado. Controles em memória implantados somente DEV; CPU N=2 P50 1,943ms/P90 14,163ms, zero erros, margem ainda HOLD. OAuth principal SHA-1 Play cadastrado com autorização, JSON e recurso gerado validados separadamente; arquivo original intacto. Os 22 testes backend/267 Android e fluxos físicos anteriores não foram repetidos como novas evidências. Sem API de produção, push, release ou avanço S05/S09. OQ-01 residual: recuperação excepcional; OQ-02 continua necessária a grants S05. A arquitetura permite desenvolvimento isolado de S05, sem autorizar concessão/rollout nem tornar Core dependente da API.
