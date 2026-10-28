# S06 / S09 / OQ-07 — resultado operacional DEV

Ensaio iniciado em **2026-10-16**, retomado em **2026-10-19**, branch `growth`, base local `226fe22f`. Esta nota prevalece sobre os diagnósticos anteriores de indisponibilidade de operador, intake e grant. Não autoriza produção.

## Resultado comprovado

O Android enviou uma declaração S01 com sessão Google real, recebeu protocolo persistido no D1 DEV, e o proprietário analisou o caso pelo console privado. Duas referências **AUXILIARY explicitamente sintéticas** fundamentaram uma avaliação **ASSISTED_ASSESSMENT de teste**, autorizada especificamente pelo proprietário. Não foram apresentadas como prova histórica.

Antes da emissão: decisão APPROVE, revisão 4, protocolo ainda UNDER_REVIEW; SQL confirmou **1 solicitação / 0 grants**. Depois da emissão autorizada: GRANTED, baseline `calcmot-core-3.5-vc12`, grant revision 1. Repetição da mesma emissão devolveu `idempotent=true`.

O GET autenticado retornou entitlement S01, revisão 6, `legacyCore=true`, `coreAllowed=true`, `proAllowed=false`. A V3 integrada mostrou o acesso permanente, com detalhes, sem nova chamada para solicitar recuperação. Logout/relogin e reinício do processo preservaram o direito. Em 19/10, **somente `br.com.calcmot.identitydev` foi desinstalado e reinstalado**: abriu sem sessão; login voluntário com a mesma conta recuperou a revisão 6 pelo servidor, sem cache da instalação anterior.

A segunda conta Google expressamente autorizada não herdou o grant: seis GETs reais retornaram 503 por ausência de fonte/base suficiente. Sua sessão foi recusada pelo console de operador. Um segundo protocolo real DEV foi aberto e rejeitado pelo proprietário, sem emissão.

Leitura D1 final em 19/10: **1 GRANTED, 1 REJECTED, 1 grant, 1 recibo de emissão, 0 históricos verificados**. Os identificadores de pessoas/protocolos e artefatos privados do ensaio ficam fora da documentação versionada, em `.tmp/s06-operational/`. Não apagar o grant do ensaio sem autorização própria nem reutilizá-lo como direito comercial de produção.

## Ambiente e autoridade

- Worker: `calcmot-api-dev`, versão `a7f64f6f-7dd0-4af7-acb8-e31d5ad64149`, domínio workers.dev exclusivamente DEV. O rótulo Cloudflare “Production” significa o deployment padrão **desse Worker DEV**.
- D1 existente: `calcmot-identity-dev`. Migrations 0002–0008 aplicadas após aprovação; nenhuma migration histórica editada. Flags de claim e projeção habilitadas exclusivamente na configuração DEV explícita. Banco medido: 212.992 bytes.
- Operador: UID conferido no Console Firebase e fixado fora do repositório; assinatura Google, issuer/audience/sub, `admin=true` booleano e `auth_time` de até 300 segundos. Refresh de token não substitui reautenticação.
- Bootstrap: autoridade **já existente** da conta proprietária no Cloud Shell, Identity Toolkit com quota project `calcmot`. Claim admin atribuída somente ao UID conferido, após autorização específica, preservando as demais claims. Inventário completo naquele momento: 2 usuários, proprietário admin, 0 outros admins. Não é garantia sobre futuras alterações administrativas.
- **A claim pertence ao projeto Firebase compartilhado**; essa alteração de usuário foi autorizada e executada. Não houve nova service account, chave privada, ADC local, custom role ou mudança IAM. A alternativa dedicada aprovada não precisou ser criada.
- Console local em loopback, binding D1 remoto autenticado via Wrangler, mesmo `LegacyReviewService` e atomicidade D1. Nenhum `/admin/*` ou endpoint de emissão foi publicado. Token/UID do operador em memória, não em arquivos/argumentos/Android.
- Wrangler usa a credencial OAuth existente no cofre Windows, com os escopos já aprovados. Não ampliar escopos para eliminar o aviso genérico da CLI.

## Implementação mínima

1. **OQ-07:** migration aditiva 0008, `LegacyPrivacyService`, fechamento definitivo explícito, dry-run, revisão/fingerprint, operação atômica e recibo sem target UID após conclusão. Exceções de imutabilidade limitadas à operação, projeto e titular; não há janela global de exclusão.
2. **Console:** binding remoto com alvo DEV fixo, dispatcher único para console stdin e ponte local. Owner/admin/recência verificados antes do acesso. Sem SQL manual para concessão.
3. **Android:** `LegacyClaimClient` + transporte HTTPS S01 apenas no build identityDev opt-in. Chave durável por projeto/UID; serialização, retry/retomada da mesma intenção, sem GET novo, sem campos de prova inventados. Produção mantém intake OFF.
4. **Identidade:** revision monotônica na fonte coordenada e stamp da operação de login. ViewModel usa `flatMapLatest`, não contagem de emissões como prova universal. Primeiro login legítimo é distinguido de resultado obsoleto; ABA observado/coordenado invalida respostas anteriores.
5. **UI:** V3 preservada. Apenas feedback específico de conflito/envio adiado adicionado; sem redesenho ou concessão visual.

## Testes e evidências

| Verificação | Resultado real |
|---|---|
| JVM identidade/conta/intake | 55 PASS na execução de 16/10: Presentation 11, ViewModel 26, client 3, tradução Auth 2, Coordinator 13 |
| Backend revisão/emissão | 31 testes de revisão + integração persistida de emissão PASS na rodada dirigida anterior desta missão |
| Privacidade local D1 | 7 PASS; dry-run, autorização, prazos, descarte com grant preservado, 12 meses, exclusão sintética integral, replay, rollback, plano obsoleto, fechamento/reabertura |
| Guarda do alvo remoto | 1 PASS; recusa troca de conta/banco/Worker ou introdução de bindings/rotas não permitidos |
| Operador Google real | Handoff aceito após reautenticação; rejeição de sessão ordinária comprovada fisicamente |
| Segunda conta sem grant | 6 GETs reais 503; 326, 325, 389, 283, 336 e 323 ms |
| Proprietário após relogin | 6 GETs reais 200, revisão 6; 369, 164, 184, 159, 178 e 177 ms |
| Retomada em 19/10 | 6 GETs reais 200; 1.076, 159, 168, 188, 189 e 155 ms |
| Após reinstalação DEV | 6 GETs reais 200; 396, 175, 179, 173, 175 e 186 ms; mesmo direito, sem Premium |
| API sem autenticação | GET entitlement e caminho admin retornaram 401 AUTH_REQUIRED; não há admin anônimo |
| Capturas | `granted-real-dev.png` e `granted-after-reinstall-real-dev.png`: screenshots reais Samsung SM-A075M, 720×1600, inspecionados; não são previews nem grants de produção |

Latências Android são round-trip, não CPU edge. **Dois testes instrumentados finais PASS em 19/10**, no Samsung: sessão real + parâmetros falsos UID/projeto continuam consultando o próprio grant; admin público retorna 403 até para o proprietário e token inválido retorna 401. Segundo teste restaura o cache real pós-reinstalação, injeta 503 e IOException no transporte de teste e comprova BACKEND_UNAVAILABLE/OFFLINE com `legacyCore=true`, Core atual preservado, sem Premium e arquivo cache idêntico. A falha foi controlada; não é prova de indisponibilidade real do provedor.

Build da instrumentação final PASS (1m29s); TypeScript e verificação sintática dos scripts PASS; `git diff --check` PASS e sete contratos S01 sem diferenças no Git. Não houve nova rodada das suítes gerais nem download de assets/fontes.

## CPU, cotas e limites

Painel oficial do Worker, 19/10, janela 24h observada: **16 invocações**, versão ativa `a7f64f6f`, **0 erros**, **0 Exceeded CPU Time Limits**, 0 estouros de memória e 0 desconexões. CPU P50 **1,837 ms**, P90 **10,310 ms**, P99 **15,441 ms**; memória P50 1,88 MB, P99 1,89 MB. Wall time P50 122,837 ms e P99 652,039 ms. Três subrequests Google 2xx foram exibidos; não confundir isso com contagem de leituras D1.

Em 16/10, duas janelas anteriores mostraram CPU P50 9,862 e 10,388 ms, com P90 9,862 e 19,229 ms. Não são amostras individuais nem benchmark isolado da assinatura JWT. O período atual confirma funcionamento Free no volume do piloto, **não uma garantia de toda requisição abaixo de 10 ms ou de capacidade em escala**. Não gerar carga para forçar cotas. Antes de rollout, medir cold/warm por operação e investigar cauda; não solucionar com upgrade automático.

Consultas SQL de verificação em 19/10: grants 0,3886 ms / 1 linha lida / 0 escritas; estados 0,2821 ms / 4 linhas; recibos 0,09 ms / 1 linha; histórico 0,0682 ms / 0 linhas. Essas contagens são do diagnóstico, não do total mensal do serviço. Painéis reconferidos em 19/10: **Cloudflare Workers Free — Current plan** e **Firebase Plano Spark**. Nenhum plano pago, cartão ou faturamento foi ativado. Nenhuma nova contratação.

## OQ-07 executável e limites de privacidade

Política aprovada **para o piloto DEV**, com controlador pessoa física e contato CalcMot existente: [política vigente](OQ-07-DEV-APPROVED-POLICY.md). Evidências 30 dias, suporte 90 dias, auditoria minimizada 12 meses após encerramento definitivo aplicável; logs sanitizados 30 dias. Não são prazos universais da LGPD.

`privacy-close` marca encerramento definitivo somente após tratar recurso; uma rejeição não inicia o prazo automaticamente. `privacy-plan` é leitura; `privacy-execute` separado exige titularidade, confirmação e fingerprint vigente. Descarte de evidências/auditoria não revoga grant permanente. Exclusão integral D1 retorna etapas externas pendentes; Firebase, suporte, cache e backups exigem o procedimento separado. **Nenhum dado/grant/conta real foi excluído.** Ensaios destrutivos usaram somente D1 local sintético.

A operação de replay informa conclusão do operationId; após retirar o target UID do recibo, não serve como comprovante identificável de exclusão de qualquer outro titular. Guardar a referência no atendimento apenas conforme sua finalidade/prazo, sem reconstruir retenção residual ilimitada.

Não há rotina autônoma de descarte: o proprietário executa o procedimento manual nos vencimentos. Não afirmar exclusão global, anonimização automática de UID, conformidade jurídica certificada ou recuperação automática após eliminar integralmente o vínculo. Identificação pública completa do controlador, fundamento por finalidade, transferências e retenções dos provedores continuam requisitos de rollout, não aprovação jurídica tácita deste piloto.

## Core e limites da demonstração

Nenhuma rede foi adicionada por oferta e nenhum gate comercial foi conectado ao Core. OCR, AccessibilityService, overlay, cálculos e manifest de produção foram preservados. O intake verifica monitoramento/pipeline antes do token e antes do envio, e adia a operação conhecida ativa. Isso é uma guarda de admissão, **não exclusão mútua com um pipeline que pode iniciar depois**.

O APK identityDev isola a conta e não demonstra performance de ofertas reais Uber/99. Critério comprovável desta entrega: chamada nasce apenas de intenção/retomada de conta; zero novo hook por oferta; trabalho conhecido ativo recusa envio; respostas de sessão antiga são descartadas; remoção de NavBackStackEntry continua encerrando observações segundo auditoria anterior preservada. Sem prometer zero competição de CPU/I/O enquanto a Conta está aberta. Sessão drenada/OQ-10 comercial permanece fora do ensaio e gates OFF.

Epoch cobre transições coordenadas e mudanças externas efetivamente observadas na fonte. Não detecta magicamente uma sequência externa jamais observada nem sincroniza coordenadores independentes. Não usar isso para afirmar segurança universal de qualquer mutação direta futura do Firebase.

## Status

- **OQ-07:** política do piloto aprovada; operações proporcionais implementadas e testadas em dados sintéticos. Rollout jurídico/operacional geral HOLD.
- **S06: DONE no escopo operacional DEV controlado desta missão.** Operador, revisão, complementação, rejeição, aprovação sem emissão, emissão única, auditoria, isolamento e recuperação real comprovados. Prontidão pública/produção HOLD; não usar DONE DEV como autorização comercial ou de produção.
- **S09: DONE no fluxo de conta/recuperação do piloto DEV desta missão; PARTIAL para o escopo geral de rollout.** V3 aprovada, login opcional, intake, consulta, isolamento, logout/relogin e reinstalação reais demonstrados. Não há mais bloqueio genérico de “nenhum operador / intake / grant demonstrado”. Segundo aparelho físico e medição do Core real simultâneo não foram executados; CPU de cauda e entrada comercial segura continuam limites explícitos. Produção/intake público não autorizados, gates OFF.

Não houve push, release, novo provedor, S10, Billing, cobrança, trial, Premium ou concessão comercial em produção. O grant DEV foi único e expressamente autorizado. Workspace anterior preservado.

## Arquivos desta missão

- Android novos: `account/AndroidLegacyRequests.kt`, `account/LegacyClaimClient.kt`, testes `LegacyClaimClientTest.kt` e `OperatorSessionHandoffDeviceTest.kt`.
- Android ajustados: `app/build.gradle.kts` (opt-in), `AccountRoute.kt`, `AccountViewModel.kt`, `AccountPresentation.kt`, `AccountScreen.kt` (wiring/feedback), `IdentitySession.kt`, `IdentityCoordinator.kt`, `GoogleIdentityClient.kt` e regressões de Coordinator/ViewModel. Não confundir o diff agregado do workspace com alterações somente desta missão: redesign e avatar anteriores foram preservados.
- Backend novos: `migrations/0008_legacy_privacy.sql`, `src/legacy-privacy.ts`, `scripts/legacy-dev-binding.mjs` e declaração `.d.mts`, `legacy-console-command.mjs`, `s06-dev-session.mjs`, `s06-dev-command.mjs`, testes `legacy-privacy.test.ts` e `legacy-dev-binding.test.ts`.
- Backend ajustado: `scripts/legacy-review-local.mjs`; `wrangler.dev.jsonc` é configuração local ignorada, sem token/owner, com flags DEV explícitas. Migrations 0002–0007 já existentes foram aplicadas, não reescritas.
- Documentação: este resultado, política aprovada OQ-07, runbook S06, relatórios S06/S09, BACKLOG, EXECUTION-PLAN e notas de superação das propostas antigas. Screenshots/logs de build e metadados do ensaio somente em `.tmp/s06-operational/` ignorado; sem email/perfil real adicionado ao repositório.

O console foi encerrado após o ensaio/interrupção; na retomada não havia listener em 8966/8967 nem reverse ADB. A credencial administrativa deve ser entregue novamente após reautenticação quando o proprietário retomar o procedimento. Isso é encerramento da sessão, não perda do grant.
