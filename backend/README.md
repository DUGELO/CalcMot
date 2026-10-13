# CalcMot API — S02 local

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

