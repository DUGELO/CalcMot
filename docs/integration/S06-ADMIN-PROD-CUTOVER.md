# CalcMot Admin — preparo e cutover PROD

**Revisão operacional23/10:** seguir o checklist único [S06-ADMIN-PROD-READINESS.md](S06-ADMIN-PROD-READINESS.md). A janela controlada somente leitura mede autenticação/CPU durante o corte aprovado; não exigir métricas de um endpoint ainda sem tráfego como precondição impossível. Gates e concessões permanecem OFF. OQ-07 foi estendida expressamente a PROD; ver OQ-07-PROD-OPERATIONS.md. O histórico abaixo permanece como registro da preparação.

2026-10-22. Branch growth. Preparação autorizada pelo proprietário, seguida de “autorizo uso real” e “faça o que tem que ser feito”. Esta etapa prepara recursos isolados; não ativa concessões comerciais, IAM/claims, tráfego PROD ou cobrança. DEV e seus grants não são importados.

## Estado efetivamente executado — atualizado23/10/2026

- D1 PROD criado: `calcmot-identity-prod`, UUID `d0232771-b6e7-49a1-ae9f-05267fbdf3a1`.
- Worker privado PROD ativo sem alvos públicos: versão `2913aabd-a653-433e-9863-d9ffa0103f64` após secret.
- Gateway PROD ativo sem alvos públicos: versão `1e18bfd2-c6a5-46c6-97c9-64d99ea0afed` após secret.
- Schema0001–0009, quatro índices, quick_check=ok, foreign_key_check vazio. Zero claims, grants, policies, events, issuances, evidence, historical_use e snapshots; nenhum dump DEV importado.
- UID do proprietário conferido na linha legítima do Firebase e configurado como secret nos dois Workers; entrada temporária eliminada. Nenhuma claim/IAM criada ou alterada.
- Tráfego oficial ainda DEV: GET anônimo /api/admin/v1/session retornou401, X-CalcMot-Environment DEV e no-store na retomada23/10. Não houve cutover.
- Registro detalhado: [S06-ADMIN-PROD-PREPARATION-RESULTS.md](S06-ADMIN-PROD-PREPARATION-RESULTS.md).

## Recursos e execução em duas etapas

Conta Cloudflare existente: 45086fe60ce934025fcac45e65eb4b69. Plano Free conferido no painel nesta execução: US$0, 100 mil requests/dia, até 10ms CPU/request; D1 Free com até 10 bancos, 5GB total, 5 milhões de linhas lidas/dia e 100 mil escritas/dia. Antes do preparo existia somente calcmot-identity-dev. Nenhum upgrade, cartão ou billing é necessário para os recursos propostos. As cotas são compartilhadas; isolamento de banco não reserva cotas para PROD.

**Preparo sem tráfego:** novo D1 calcmot-identity-prod vazio; calcmot-admin-private-prod com DB somente PROD; calcmot-admin-gateway-prod somente Service Binding ADMIN para o privado. Workers sem routes, workers.dev ou preview; ADMIN_ENVIRONMENT=PROD e ADMIN_MUTATIONS_ENABLED=false nos dois. O gateway não recebe D1. Firebase calcmot e operador/claim já autorizados permanecem; nenhuma identidade ou permissão nova. Não publicar outro site ou alterar DNS/calcmot-site.

**Cutover separado:** apenas as duas rotas atuais /api/admin/v1 e /api/admin/v1/* podem migrar do gateway DEV para PROD depois dos critérios abaixo. /admin permanece o Angular existente. Não executar esse corte durante a preparação. Operações comerciais continuam OFF. Arquivos gerados de cutover existem para revisão, não representam autorização automática de deploy.

## Rollback antes de tráfego

Como os novos Workers não recebem tráfego, uma falha não altera o site ou DEV. Interromper e manter os recursos sem rotas; não tocar nos recursos DEV. Remoção dos Workers novos e do D1 novo é opcional e somente após inventário comprovar ausência de dados reais e autorização para exclusão. Não apagar banco com dados para “reverter migration”.

## Preparação reproduzível

Executar na pasta backend. A CLI já instalada é Wrangler4.149.0. Não executar comandos de upgrade.

```powershell
npm run typecheck
npx vitest run tests/admin-web.test.ts tests/admin-prod-release.test.ts --maxWorkers=1 --testTimeout=30000 --hookTimeout=90000
node scripts/validate-prod-migrations-local.mjs
node scripts/prepare-admin-prod.mjs
```

O último comando é offline: gera manifest com SHA256 das nove migrations; não emite configuração pronta sem UUID PROD distinto do DEV. validate-prod-migrations-local sempre usa --local, banco sintético exclusivo e duas aplicações; nunca copia DEV.

Após conferir Free, nomes e autorização de preparo:

```powershell
npx wrangler d1 list --json
npx wrangler d1 create calcmot-identity-prod
# Substituir pelo UUID real retornado, nunca pelo UUID DEV ou por exemplo.
node scripts/prepare-admin-prod.mjs --database-id UUID_REAL_PROD
npx wrangler d1 migrations list DB --remote --config .tmp/admin-prod-release/private.jsonc
npx wrangler d1 migrations apply DB --remote --config .tmp/admin-prod-release/private.jsonc
npx wrangler d1 execute DB --remote --config .tmp/admin-prod-release/private.jsonc --file scripts/admin-prod-readiness.sql --json
node scripts/check-admin-prod-readiness.mjs
npx wrangler deploy --config .tmp/admin-prod-release/private.jsonc --dry-run
npx wrangler deploy --config .tmp/admin-prod-release/gateway.staged.jsonc --dry-run
npx wrangler deploy --config .tmp/admin-prod-release/private.jsonc
npx wrangler deploy --config .tmp/admin-prod-release/gateway.staged.jsonc
```

Esperado antes do primeiro caso: ledger0001–0009, quatro índices legacy_admin_*, quick_check=ok, foreign_key_check vazio, zero claims/grants/policies/evidências/decisões/issuances. Não executar INSERT de fixture, importar dump DEV ou copiar fonte de grant.

As migrations não criam administrador nem política. A ausência de LEGACY_OWNER_UID é fail-closed503. Configurar o UID legítimo conferido como secret nos novos Workers por entrada protegida, fora do Git, sem argumentos/logs; não inferir UID pelo email ou criar claim.

```powershell
npx wrangler secret put LEGACY_OWNER_UID --config .tmp/admin-prod-release/private.jsonc
npx wrangler secret put LEGACY_OWNER_UID --config .tmp/admin-prod-release/gateway.staged.jsonc
```

Nenhum token é necessário para aplicar schema vazio. Autenticação operacional e registro da política exigem sessão Firebase legítima do operador, não credencial Cloudflare nem UID digitado isoladamente.

## Contrato e frontend

Contrato administrativo1.3.0 coordenado com o agente Angular; S01 inalterado. /session retorna environment DEV|PROD, mutationsEnabled e recência. Todas as respostas identificam X-CalcMot-Environment. POSTs PROD exigem X-CalcMot-Expected-Environment: PROD, além das demais guardas; aba DEV antiga é recusada antes de D1. O gateway sobrescreve metadata interna do alvo e o privado verifica ambiente antes de qualquer leitura/escrita. Alterar apenas um dos Workers não libera mutações no outro.

Frontend: environment.ts, environment.staging.ts e environment.production.ts, conforme solicitação do proprietário e convenção Angular; arquivos contêm somente configuração pública. PROD não pode usar DEV por fallback. API relativa /api/admin/v1 no origin oficial; Firebase Auth permanece projeto calcmot. Reutilizar o app Web existente é tecnicamente suficiente; app Web separado não isola direitos/UIDs do mesmo projeto. Nenhum novo app/claim/IAM é presumido ou criado. Seleção de env PROD e publicação do Angular são coordenadas, mantendo o mecanismo automático de main. Não modificar workflow/pipeline para fazer a troca.

O mesmo caminho não serve DEV e PROD simultaneamente. Depois do corte, os recursos DEV continuam preservados, mas as duas rotas oficiais passam a PROD. Não reaplicar a configuração DEV antiga com routes, pois ela poderia reclamar essas rotas. Não inventar alias/subdomínio DEV; acesso de manutenção DEV permanece pelos canais já autorizados.

## Gates antes do cutover

1. Registrar versões e IDs das duas rotas DEV e versão atual calcmot-site; inventário/fingerprint do grant DEV em memória. Não imprimir registros pessoais.
2. Conferir UUID PROD distinto, manifesto/schema e zero dados sintéticos. Fixar release/bundle/hash testados. Os exemplos não substituem o UUID real.
3. Confirmar operador real allowlisted e admin=true com Firebase ID Token verificado, sem novas claims. Negativos401/403 e POST com auth_time>300s recusado; GET válido continua permitido.
4. Registrar a política comercial já aprovada usando LegacyReviewService.authorizePolicy com operador legítimo; não copiar approved_by nem SQL de política do DEV. A API web atual não expõe bootstrap de política. O console remoto existente aceita DEV apenas: não apontá-lo a PROD removendo sua guarda. O novo script privado bootstrap-admin-prod-policy.mjs prepara exclusivamente essa operação, sem endpoint público/grant: --check é offline; --apply exige JSON por stdin de produtor confiável em memória, token Firebase legítimo, admin/owner verificado, Google vinculado e reautenticação300s, confirmação de UUID PROD e policyVersion. LEGACY_OWNER_UID vem da configuração protegida do operador, nunca de email. Não pedir token por chat/clipboard/log/arquivo. Ainda falta validar esse canal com sessão real: nenhuma política foi inserida nesta etapa. POLICY_REQUIRED422 continua correto enquanto faltar o registro. Não liberar concessões antes dessa validação.
5. Validar em DEV o ciclo positivo autorizado, aprovação sem emissão, emissão idempotente e recuperação no Android mesmo UID, sem conceder Premium. O grant DEV anterior não é dado real PROD. Sem aparelho nesta execução, uma nova validação física não pode ser declarada.
6. Definir operacionalmente OQ-07 para uso real: a aprovação anterior dos prazos é restrita ao piloto DEV; “uso real” não é apresentado como parecer jurídico. Confirmar contato público, finalidades/fundamentos e execução de descarte/atendimento. Nenhum descarte real automático nesta release.
7. CPU edge representativa e cotas atuais: coletar múltiplas amostras autenticadas, inclusive D1, e p50/p95/max por Worker; nenhum resultado local substitui medida edge. Não ativar se exceder limite Free ou faltarem dados de capacidade relevantes. Não habilitar plano pago.
8. Frontend deve estar compatível com1.3, ambiente esperado e readonly. A versão Angular atual1.2 não é promovida por simples troca de binding. Ensaiar plano com configurações locais antes da janela.

## Corte em janela controlada — NÃO executado nesta etapa

1. Desabilitar novas mutações DEV durante a janela mediante alteração explícita e aprovada; aguardar requisições em andamento. Não migrar casos ou grants DEV.
2. Publicar pelo fluxo normal da main a seleção de configuração PROD coordenada com o frontend; uma divergência temporária bloqueia admin, não o site público. Não enfraquecer a validação para esconder o erro.
3. Transferir exclusivamente as duas rotas API do gateway DEV ao gateway PROD via edição dos IDs existentes, fail-closed. Config gerada gateway.cutover.jsonc contém somente esses padrões. Wrangler deploy de um Worker novo não deve ser presumido capaz de reclamar rotas de outro Worker: conferir ownership/IDs e usar o painel/API de rotas existente. Sem wildcard global e sem DNS novo.
4. Confirmar /, seis páginas públicas e SSR/SEO; /admin shell no-store/noindex; API anônima401, usuário comum403, origem indevida403, dono GET200/environmentPROD/contagens vazias. Serviço privado sem URL e fetch externo403.
5. Manter PROD readonly até validação. Habilitar mutações somente com decisão explícita, política registrada e ensaio concluído, em ambos os Workers. Idempotência/revisão/recência permanecem obrigatórias; não criar grants para “smoke test”.

## Rollback após o corte

Em erro, bloquear mutações nos dois Workers e manter a administração indisponível com segurança; não redirecionar silenciosamente usuários PROD para D1 DEV. Reverter primeiro o código para versão PROD compatível com o mesmo banco e gates OFF; não reverter schema destrutivamente nem apagar auditoria/grants. Se não houver nenhum dado real e o dono decidir voltar a homologação, reatribuir somente os dois IDs de rota ao gateway DEV e restaurar a configuração DEV do frontend pelo fluxo normal, identificada como DEV. Depois de dados reais, retorno a DEV é ambiente de teste separado, nunca recuperação dos dados PROD.

Não executar rollback de grants ou restaurar snapshot anterior por padrão: isso pode extinguir direito já emitido. Restauração de D1 exige incidente autorizado, verificação do ponto de recuperação, ambiente isolado e reaplicação de decisões de privacidade. Preservar evidência mínima da ocorrência.

## Observabilidade e custo

Sem logs de JWT, UID, perfil ou conteúdo de evidência; observability=false nos exemplos para não coletar invocation URLs de protocolos. Auditoria S06 transacional permanece no D1. Usar requestId dos erros, status, métricas agregadas de plataforma, consumo D1 e fingerprint/inventários sem payload pessoal. Operador verifica erros/cotas diariamente no piloto; após1102/429/503, parar novas operações e investigar, nunca repetir emissão com nova chave.

Os limites120requests/min e10mutações/min são proteção por isolate, não teto global nem reserva de cota. Free não garante disponibilidade irrestrita; site e demais Workers compartilham capacidade. Fontes oficiais: [Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/), [D1 pricing](https://developers.cloudflare.com/d1/platform/pricing/), [Firebase Spark](https://firebase.google.com/docs/projects/billing/firebase-pricing-plans). Nenhum serviço pago adicional é parte deste plano.

O frontend não recebe D1, allowlist UID, token de serviço ou credencial administrativa. Não alterar OCR, AccessibilityService, overlay, cálculos, contratos S01 ou versão publicada do Android. A configuração calcmot-api-prod é preparação adicional SEM rotas e com intake/leitura OFF: ativação e URL Android são release separada, não devem ser presumidas por ativar /admin.
