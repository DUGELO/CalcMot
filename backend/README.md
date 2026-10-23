# CalcMot API — S02 local

**Admin PROD — preparo23/10/2026:** [resultados reais](../docs/integration/S06-ADMIN-PROD-PREPARATION-RESULTS.md) e [comandos/cutover/rollback](../docs/integration/S06-ADMIN-PROD-CUTOVER.md). D1 e Workers PROD isolados preparados, sem rotas públicas e com mutações OFF. DEV remoto continua1.2; release administrativa1.3 preparada e coordenada com envs Angular, sem alterar S01. Não publicar gateway.cutover nem habilitar operações comerciais por executar prepare-admin-prod. O script privado bootstrap-admin-prod-policy só registra política com autoridade legítima; não cria grants e não foi aplicado nesta preparação.

**Admin Web privado — 2026-10-19:** [contrato de integração1.2](../docs/integration/S06-ADMIN-WEB-CONTRACT.md), [backend local implementado/testado](../docs/integration/S06-ADMIN-WEB-BACKEND.md) e [registro de autorização remota](../docs/integration/S06-ADMIN-WEB-REMOTE-APPROVAL.md). Gateway com Firebase proprietário/admin → RPC → Worker privado HTTP403 → S06 existente; sem Angular/Android ou exposição administrativa na API pública. 0009 contém somente índices e foi aplicada ao D1 DEV autorizado. [Validação remota](../docs/integration/S06-ADMIN-WEB-REMOTE-VALIDATION.md): owner200, comum403, recência validada; UI Angular ainda não publicada.

**Operação atual, 19/10:** [ensaio S06/S09 DEV comprovado](../docs/product/S06-S09-DEV-OPERATIONAL-RESULTS.md). Migrations 0002–0008 e deploy exclusivamente DEV foram autorizados/executados; operador Google real, emissão sintética única, isolamento e recuperação Android após reinstalação demonstrados. [Console privado e instruções](../docs/product/S06-OPERATOR-RUNBOOK.md), [OQ-07 aprovada para o piloto](../docs/product/OQ-07-DEV-APPROVED-POLICY.md). Notas antigas abaixo sobre ausência de operador/retention/deploy são histórico. Produção permanece HOLD; não executar concessões comerciais nem exclusões reais com base neste ensaio.

Workers TypeScript + D1 local; contrato S01 v1.0.0, Firebase Auth Spark. A entrega inicial foi local. Em 13/10 o D1 DEV gratuito foi criado/migrado; deploy exclusivamente DEV foi autorizado e a validação real está registrada no relatório de integração. Decisão: [ADR-001](../docs/product/ADR-001-FREE-IDENTITY-BACKEND.md).

Em `backend/`, com Node compatível com Wrangler instalado:

```powershell
$env:npm_config_cache="$PWD/.npm-cache"
$env:WRANGLER_SEND_METRICS="false"
$env:WRANGLER_LOG_PATH="$PWD/.wrangler/logs"
npm ci
npm run typecheck
npm test
npm run migrate:local
npm run dev
```

O dev bind é `127.0.0.1:8787`. `database_id` de zeros no arquivo é **marcador exclusivamente local**, não recurso real. `workers_dev=false`, sem routes/account ID e sem script de deploy real. `npm run check:bundle` faz somente `deploy --dry-run`. Os comandos acima continuam locais. Para o ambiente DEV autorizado, usar exclusivamente o arquivo `wrangler.dev.jsonc` verificado, nunca este placeholder. Conferir o relatório antes de executar comandos remotos.

`GET /v1/me/entitlement` requer **Firebase ID token**, não Google ID token nem custom token. A rota lê unicamente `(projeto configurado, sub validado)`. Sem registro, retorna 503; não cria benefício por primeiro login. Campos query desconhecidos, inclusive UID forjado, são ignorados e nunca alteram o titular; GET mantém os status congelados 200/401/503. Escrituras e operações privilegiadas falham fechadas. Headers de correlação são opacos, respostas `no-store`; erros não contêm mensagens internas, UIDs ou tokens. Nenhum token é logado pelo handler. Não persistir Authorization em logging externo futuro.

Migrations versionadas criam apenas a view materializada de snapshots. SQL restringe UID/revisão, JSON válido, contrato 1.0.0, revisão crescente e titular imutável. Validador completo é gerado do schema S01 **no build**, sem runtime eval; sem alteração do schema. Nenhum ledger/compra/pagamento/grant foi criado. Snapshot é produzido pela S07 futura. Em 13/10 o usuário autorizou duas fixtures de snapshot com todos os direitos negados exclusivamente no D1 DEV; foram removidas ao terminar e a contagem original zero foi comprovada. Nunca aplicar fixtures em produção.

Testes usam chaves RS256 efêmeras e resolvedor injetado no harness, com D1 real local via Miniflare. O Worker publicado não oferece switch de emulator/unsigned JWT, endpoint de chaves configurável ou credenciais de teste. O Firebase Auth Emulator emite tokens unsigned que esse Worker deliberadamente rejeita; não enfraquecer a verificação para integrá-lo.

`FIREBASE_PROJECT_ID` é configuração não secreta por ambiente; `.dev.vars*`/`.wrangler/`/`node_modules` estão ignorados. Um projeto diverso requer configuração de ambiente deliberada e testes de issuer/audience. Nenhuma chave Admin, OAuth secret ou token Cloudflare está no repositório. A configuração real DEV já preparada fica em `wrangler.dev.jsonc`, ignorada, com UUID confirmado do D1 Free. O OAuth oficial do Wrangler é criptografado com chave no Windows Credential Manager, fora do repositório. Não usar o placeholder local para deploy.

Limites e pendências estão no ADR/relatório. A ausência de endpoint de escrita não substitui controle operacional do binding D1.

## Proteção operacional gratuita

`ABUSE_PROTECTION=enforced` exige valores inteiros positivos para `ABUSE_REQUESTS_PER_MINUTE` e `ABUSE_READS_PER_UID_PER_MINUTE`; configuração ativada inválida falha fechada. DEV usa 120 tentativas/minuto/isolate antes do JWT e 30 leituras/minuto/UID validado/isolate antes do D1. Máximo 512 titulares na memória; sem eviction que permita contornar o contador, sem KV/D1 writes ou novos serviços. Sem configuração explícita, o harness local histórico permanece funcional; não promover esse modo para ambiente aberto. Os números DEV não aprovam política de produção.

Exaustão/indisponibilidade devolve `503 RETRY_LATER` conforme o GET congelado (não introduz 429). `ENTITLEMENT_READ_ENABLED=false` interrompe a API read-only sem mudar grants, Auth ou Core. Contadores são por instância e janela UTC de minuto; reinícios/novas instâncias e fronteiras de janela limitam sua eficácia. **Não são rate limit distribuído nem orçamento diário global.** Não cachear tokens/snapshots para contornar CPU. Proposta operacional e consumo real de CPU no relatório.

## Teste opt-in no D1 DEV real

Somente com autorização específica de fixtures DEV e configuração local verificada:

```powershell
$env:CLOUDFLARE_AUTH_USE_KEYRING='true'
$env:WRANGLER_SEND_METRICS='false'
$env:CALCMOT_REMOTE_DEV_TEST='1'
./node_modules/.bin/vitest.cmd run tests/dev-d1-isolation.test.ts
Remove-Item Env:CALCMOT_REMOTE_DEV_TEST
```

O teste trava nome/conta/UUID DEV, gera dois UIDs sintéticos únicos e JWTs RS256 efêmeros em memória. Executa o handler local com resolvedor de teste e SQL no D1 remoto via Wrangler oficial. Não altera chaves/issuer da API pública nem simula Google real. A/B recebem revisions diferentes, todos os direitos negados; UID na query não seleciona terceiro. Cobre 200/401/403/503; `finally` apaga somente os dois registros dessa execução e verifica contagem original/zero fixtures. `npm test` normal mantém esse teste remoto desabilitado.

# S05 — fonte legado local, ainda sem ativação

**Continuação S07:** [projetor e validação local](../docs/product/S07-ENTITLEMENT-PROJECTION.md)
conectam fontes S05/S06 ao GET sem reimplementar concessão. Notas anteriores de
503 sem snapshot descrevem o leitor S02/flag ausente; com a integração local
habilitada, grant válido já produz 200 mesmo sem entrada financeira existente.

`src/legacy.ts` armazena somente a fonte `LegacyGrant` do contrato congelado.
S06 recupera fontes e possui emissor privado local de revisão autorizada, sem
endpoint público de concessão nem escrita de snapshots (S07). Nada foi ativado remotamente.
Elegibilidade comercial OQ-02 aprovada: uso histórico até 3.5/versionCode 12.
Política operacional e prova histórica verificada devem existir por processo
confiável; cadastro na migração e versão atual não concedem. `observeLocal`
sempre grava AUXILIARY. O contexto de sessão deve vir de `verifySession`, nunca
de JSON cliente. Recuperação é só intenção pendente; não concede/mescla UIDs.

Migration 0002 já existente foi preservada; 0003 é aditiva, com autoridade,
unicidade de prova e imutabilidade. Não aplicar em DEV remoto ou produção nesta
story. Auditar conflitos antes de migrar bancos com dados; falha de índice não
autoriza apagar/regravar grants. `npm test` executa fixtures S05 somente locais.
Relatório e decisões abertas: [S05](../docs/product/S05-LEGACY-FOUNDATION.md).

Migration 0004 é aditiva: regra <=12, baseline `calcmot-core-3.5-vc12` e histórico
privado imutável. Policies antigas sem regra não emitem novos grants; grants
anteriores não são revogados. Sem timestamp alternativo de elegibilidade:
`policyCutoffUtc` nulo nesta preparação. Nenhum dado real ou endpoint novo.
[Política aprovada e inventário](../docs/product/OQ-02-LEGACY-POLICY.md).

## S06 — revisão local e recuperação

POST `/v1/legacy/claim` usa JWT validado, Idempotency-Key e payload S01; somente
`wrangler.jsonc` local habilita `LEGACY_CLAIMS_ENABLED=true`. Sem flag, 503.
Não há rota administrativa pública. `npm run review:local` recebe JSON em stdin
e opera somente `.tmp/s06-issuance-local/v3/d1`, mesmo UUID/banco do Wrangler com
`--persist-to ../.tmp/s06-issuance-local`. Aplicar migrations 0001–0006 antes; parar o dev
server para o console acessar esse SQLite. Sessão do proprietário requer UID
explicitamente configurado, role admin booleana e reautenticação até cinco minutos.
Nenhuma comparação de email concede privilégio.

Avaliação humana combinada é ASSISTED_ASSESSMENT e não altera o histórico
verificado. APPROVE registra decisão e mantém UNDER_REVIEW. O comando privado
`policy` registra explicitamente a regra fixa aprovada pelo proprietário, antes
da avaliação; `issue` recebe protocolo, revisão da aprovação, key e
`input={approvalEventId}`. Migration 0006 exige decisão vigente/ownership/política
e grava recibo auditável + fonte permanente + GRANTED numa transação. Falha
reverte tudo; retries não duplicam e grants anteriores são preservados. Sem
Premium, provisório ou snapshot. Rejeição/recurso permanecem auditados.

`tests/legacy-issuance.integration.test.ts` demonstra intake autenticado, análise,
aprovação, emissão e recuperação após reiniciar D1, com identidades sintéticas
assinadas e isolamento. Não comprova admin Google real. UID/role legítimos do
proprietário e OQ-07 ainda bloqueiam operação real; não configurar privilégios
de produção nem reutilizar fixtures como direitos reais. Procedimento e comandos
protegidos: [S06](../docs/product/S06-LEGACY-RECOVERY.md). **PARTIAL**.

### Diagnóstico operacional S06, somente leitura

`node scripts/s06-operator-preflight.mjs --configuration` informa apenas presença
de configuração. Com sessão Google real e UID do proprietário fora do repo,
stdin `{token,expected:'owner'|'ordinary'}` valida o novo preflight sem abrir D1.
Não concede role nem substitui autorização do console. `s06-local-inventory.mjs`
lê somente metadados/contagens do banco local já existente. `s06-admin-inventory.mjs`
é opcional: requer runtime Firebase Admin SDK/ADC já autorizado, usa apenas listUsers
e emite contagens; nenhum SDK/CLI/ADC foi instalado/criado por esta execução.

Fluxo, diagnóstico sem chaves e permissões mínimas: [runbook](../docs/product/S06-OPERATOR-RUNBOOK.md).
[Política OQ-07](../docs/product/OQ-07-DATA-POLICY-PROPOSAL.md) é proposta não aprovada:
nenhuma retenção/purge foi ligada e guardas de grants continuam preservadas.

## S07 — projeção somente leitura

Migration 0007 adiciona versão das fontes, atualizada atomicamente com grant,
claim e snapshot existente. Uma SELECT própria normaliza o read model S01;
nenhum GET grava, sobrescreve outras fontes ou transforma Core legado em Pro.
`ENTITLEMENT_PROJECTION_ENABLED=true` foi definida somente em wrangler.jsonc local.
Aplicar todas as migrations antes de iniciar o ambiente; não ativar configuração
remota sem validação/autorização próprias. O teste `entitlement.integration.test.ts`
exercita D1 persistido e Worker compilado em workerd com chave pública sintética
exclusiva do teste. Não habilitar teste de JWT no Worker de produção.

Sem fonte suficiente/inconsistência: 503 RETRY_LATER; Core Android não consome essa
consulta. S07 DONE local, prontidão operacional/CPU edge/OQ-07 HOLD. Ver relatório
para semântica da revisão temporal, custo e dependências restantes.

