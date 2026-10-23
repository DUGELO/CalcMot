# CalcMot Admin — checklist único de readiness e janela somente leitura

23/10/2026. Esta revisão operacional prevalece sobre sequências anteriores que exigiam CPU autenticada antes de existir caminho de tráfego para medi-la. **Nenhuma transferência de rota, merge, ativação de mutações, IAM/claims, concessão ou exclusão foi executada nesta ordem.** O preparo de infraestrutura anterior foi aceito, não será repetido.

## Estado e versões que devem ser reconferidas na janela

| Componente | Alvo confirmado |
|---|---|
| DEV D1 | calcmot-identity-dev /4d0c0c71-c606-45a6-a037-43bccef8c05e |
| PROD D1 | calcmot-identity-prod /d0232771-b6e7-49a1-ae9f-05267fbdf3a1 |
| PROD privado | calcmot-admin-private-prod /2913aabd-a653-433e-9863-d9ffa0103f64 |
| PROD gateway | calcmot-admin-gateway-prod /1e18bfd2-c6a5-46c6-97c9-64d99ea0afed |
| Web baseline | calcmot-site /1940ff84-5711-4e20-8e2d-f99febca7978, informado no último corte; reconferir antes de usar para rollback |
| Estado público atual | /api/admin/v1 continua no gateway DEV1.2; Angular publicado continua DEV |
| PROD configuração | ADMIN_ENVIRONMENT=PROD e ADMIN_MUTATIONS_ENABLED=false nos dois Workers |

Leitura remota desta ordem confirmou DB PROD distinto, gateway sem D1 e binding ADMIN somente para o privado PROD, privado somente com D1 PROD e gates false. Schema0001–0009, quatro índices, integridadeOK, todas as contagens de negócio e políticas0. Fingerprint do grant DEV comparado em memória com o baseline e mantido igual;1grant. Não publicar fingerprints de registros pessoais no Git. Manifesto versionado contém somente hashes de fontes/configs/migrations e metadata não sensível; nunca secret/UID do operador.

## PRs e revisão

- Backend: PR próprio para growth, topic codex/admin-prod-readiness. O workspace principal continua growth. Como as fontes S06/S07 ainda estavam locais, o PR inclui a fundação backend necessária e o conteúdo S01 original sem modificações, para não depender de arquivos ausentes num checkout limpo. Nenhum arquivo Android modificado é incluído. A URL/head exatos constam no registro final e no PR.
- Angular: topic codex/admin-environments no repositório do site. Env default/staging DEV, production PROD somente consulta, contrato1.3 coordenado e compatibilidade explícita DEV1.2. Não escolher ambiente pelo hostname/storage/query ou seguir automaticamente o ambiente recebido.
- Conferir ambos os diffs, commits exatos, checks e artefatos. Os PRs não são autorização de merge. O comando remoto de produção permanece npm run build → npx wrangler deploy. Mudança do runner de testes, se necessária ao Chromium, não muda o comando/gatilho de deploy Cloudflare e deve estar explícita no diff Angular.

## GO/HOLD desta ordem

| Critério | Estado |
|---|---|
| Bancos/bindings isolados e sem importação DEV | GO |
| Mutações PROD bloqueadas em gateway e privado | GO; devem permanecer false durante toda a janela |
| Autenticação/isolamento/concorrência local e contrato1.3 | GO pelos testes; não confundir com autenticação remota PROD |
| Env Angular explícita e readonly | GO local; PR/checks/revisão remotos são gate de publicação |
| OQ-07 operacional | GO documental; proprietário confirmou os mesmos prazos para PROD23/10. Nenhuma exclusão autorizada |
| Registro inicial da política PROD | HOLD: sessão privada legítima e recente ainda não entregue por canal validado sem USB. Não há recibo de sucesso |
| CPU real JWT+D1 | HOLD de medição; coleta preparada, depende da janela autorizada de tráfego readonly |
| Transferência das rotas e merge | HOLD de autorização/revisão; explicitamente não autorizados nesta ordem |
| Uso comercial e grants reais | HOLD; fora desta janela, gates false |
| Android físico adicional | Pendência da ativação de direitos reais, não gate para GET/login administrativo |

**Distinguir gates:** política e Android não são necessários para owner GET/read-only no banco vazio. A janela limitada pode ser submetida à aprovação depois dos checks/revisão; sua permanência depende de autenticação, isolamento, métricas e regressões comprovadas durante a janela. Não chamar o ambiente comercial de operacional por ter obtido GET200.

## Registro privado da política: preparado, não aplicado

Mecanismo único: backend/scripts/bootstrap-admin-prod-policy.mjs → verifySession/Google JWKS → operatorReadiness → LegacyReviewService.authorizePolicy → D1 PROD. Confere UUID, projeto, UID owner configurado fora do Git, admin booleano, Google vinculado e auth_time≤300s antes de abrir o proxy D1. A confirmação do JSON especifica databaseId e policyVersion; não aceita grants ou ação genérica. Não abrir endpoint HTTP de bootstrap, emitir custom token, relaxar API key, remover guardas DEV, extrair token do browser ou pedir credencial pelo chat/clipboard/arquivo.

```powershell
# Offline e sem sessão:
node scripts/bootstrap-admin-prod-policy.mjs --check
# Futuro, após confirmação humana específica e produtor legítimo de stdin em memória:
# produtor autorizado em memória | node scripts/bootstrap-admin-prod-policy.mjs --apply
```

O script existente recebe uma sessão; ele não realiza login Google. Nesta auditoria não foi identificado um produtor PROD seguro já integrado, independente de USB. Esse é o bloqueio concreto: uma sessão do Console Firebase ou do Angular não é automaticamente stdin autorizado. Nenhuma nova autenticação foi inventada para contorná-lo.

Depois de sucesso efetivo, o script relê a política persistida e só então emite: operationId não sensível estável, policyVersion, policyRecordedAt, verifiedAt, coreBaseline e grantsWritten0. operationId é recibo derivado da metadata da política, não evento de uso histórico nem grant. Em falha após tentativa de escrita, applied=null significa resultado não confirmado: consultar/reexecutar idempotentemente, não afirmar rollback inexistente. Não preencher essa evidência com exemplo sintético.

```powershell
# Após registro real confirmado: espera1policy e0 em todas as tabelas de negócio.
node scripts/check-admin-prod-readiness.mjs --expect-policy
```

## Execução única do cutover readonly — somente após nova autorização

1. Registrar os dois PRs/head SHAs aprovados, versões atuais de site/gateway/privado, IDs e proprietário das duas rotas, estado Free/cotas e uma comparação atual do grant DEV. Guardar evidência privada mínima; não imprimir payload pessoal. Se qualquer base mudou, interromper e revisar, não usar snapshot antigo silenciosamente.
2. Reconferir no backend os gates false e isolamento. Não executar migrations ou provisionamento novamente. Gerar/revisar manifesto/configs se necessário; isso não publica nada. A configuração staged não tem routes. Não fazer deploy do arquivo gateway.cutover só para “testar”.
3. Fixar no PR Angular o artefato PROD readonly que o comando automático npm run build vai efetivamente produzir. Os comandos locais build:admin:prod não alteram por si só o comando padrão remoto. A seleção final precisa ser uma alteração versionada revisada na janela, sem mudar o pipeline. Os seis SSG públicos devem ficar idênticos e sem dados administrativos.
4. Congelar novas ações DEV no procedimento da janela, drenar requisições em andamento e avisar o operador. Não modificar gates/configs DEV nesta preparação. Nenhuma concessão, evidência ou decisão é parte do teste PROD.
5. Executar merges/publicação somente conforme aprovação coordenada. Durante eventual desencontro de frontend e API, o painel deve falhar fechado; o site público continua servindo normalmente. Não introduzir fallback automático de ambientes para mascarar a transição.
6. Reatribuir SOMENTE os IDs existentes das rotas https://calcmot.com.br/api/admin/v1 e https://calcmot.com.br/api/admin/v1/* ao gateway PROD, fail-closed. Não criar wildcard global, DNS, subdomínio, Pages ou binding D1 no gateway/site. Confirmar ownership real no painel/API, não pressupor que deploy reclama rotas alheias.
7. Validar sem mutações: anônimo401, usuário comum403, UID owner sem admin403, token inválido/expirado/outro projeto401 conforme testes autorizados, origem indevida403, owner Google Web GET /session200/environmentPROD/mutationsEnabledfalse e GET fila/contagens200. Não extrair tokens para montar testes; testes de claims inválidas podem permanecer locais se não houver credencial real autorizada correspondente. Diferenciar casos locais de testes remotos feitos.
8. Confirmar banco sem casos/grants; política pode ser0 ou1 conforme o registro privado explicitamente confirmado. Conferir no-store/noindex, /admin sem dados SSR/SSG, ausência de acesso público ao privado e site/SEO/canonical/sitemap. Executar amostragem abaixo, confirmar cotas e fechar registro GO/HOLD da janela.

## CPU e cotas — coleta preparada, nenhum valor fabricado

Usar métricas nativas Cloudflare por versão/script e período; não cron pago, novo Worker de logging ou captura de headers/tokens. Arquivo scripts/admin-cpu-query.graphql reproduz consulta read-only oficial. Usar o dashboard já autorizado ou autoridade Analytics já existente; não criar token nem ampliar OAuth/IAM para obtê-la nesta ordem. No data não é0ms. Ausência de autorização/dataset é HOLD da medição.

Durante a janela autorizada, registrar3períodos separados de até10min, com mínimo10 leituras autenticadas JWT+D1 em cada período por cadeia gateway→privado. Operador usa a UI para atualizar fila/contagens; todos os retornos devem ser200, ambientePROD e gatesfalse. Separar testes negativos do intervalo de performance. Registrar versão, UTC de início/fim, número de chamadas, erros/rejeições, linhas lidas D1 e quantis CPU dos DOIS Workers. A primeira janela é candidata a caminho frio, não prova de isolate/JWKS frio; repetir uma janela aquecida e outra espaçada sem forçar deploy.

Limite Free da conta confirmado anteriormente:10ms/request; reconferir na janela. Critério técnico conservador desta preparação: interromper diante de1102/resource-limit, CPU máximo observado≥10ms, P99≥8ms, erro não explicado, falha de JWT/Origin/isolamento, gate diferente de false ou cota insuficiente. A margem de8ms não é SLA comercial. Tempo SQL/wall-clock de navegador não é CPU de Worker. Não somar percentis como se fossem uma distribuição conjunta.

Se GraphQL exportar unidade diferente, confirmar no schema/dashboard e converter explicitamente para ms antes do relatório. Preservar unidade original e conversão na evidência privada. P99 não é máximo; registrar max=null quando não disponível, sem alegar máximo abaixo do teto. Trinta amostras por Worker são um ensaio mínimo, não prova estatística de cauda ou capacidade ilimitada. Banco vazio não demonstra desempenho de futura revisão com dados volumosos.

```powershell
node scripts/assess-admin-cpu.mjs CAMINHO_DE_METRICAS_SANITIZADAS.json
```

Entrada: source=CLOUDFLARE_PLATFORM_METRICS, environment=PROD, synthetic=false, windows com worker/start/end/authenticatedReads/jwtAndD1Verified/errors/resourceLimitErrors/cpuP50Ms/cpuP99Ms/cpuMaxMs. Só metadata e números, nunca request/token/perfil. O avaliador é offline: não coleta nem autentica por si só e não transforma declaração em prova criptográfica. Fixtures dos testes são sintéticas e não constituem CPU medida. HOLD se faltarem dados; PASS é apenas o gate CPU proposto, não liberação global do cutover.

Conferir consumo compartilhado da conta antes/depois:100mil requests/dia, D1 5milhões de linhas lidas e100mil escritas/dia, além do site e DEV. Não tratar120/min por isolate como limite distribuído. Não subir plano automaticamente se exceder cota.

Fontes: [consulta Workers Metrics](https://developers.cloudflare.com/analytics/graphql-api/tutorials/querying-workers-metrics/), [métricas e quantis](https://developers.cloudflare.com/workers/observability/metrics-and-analytics/). Nenhum token Analytics foi criado nesta preparação.

## Rollback imediato em qualquer falha

- Manter gatesfalse nos DOIS Workers. Interromper amostragem/operações; não mudar JWT/Origin/allowlist para “destravar”.
- Antes de dados reais: restaurar somente os dois IDs de rota ao gateway DEV saudável previamente conferido e o artefato Angular DEV correspondente. A transição deve falhar fechada, nunca misturar ambientes sem indicação. Preservar o pipeline: se for usado rollback de emergência do Worker do site, preparar revert versionado para impedir que o próximo autodeploy reintroduza o corte. Merge/revert continuam sujeitos à autorização.
- Preservar D1 PROD, DEV, grants, políticas e auditoria. Não apagar banco, importar fixtures ou desfazer migrations para rollback de aplicação. Se houver dados reais inesperados, manter indisponibilidade segura e preservar o banco para investigação; não voltar a DEV como se fosse recuperação desses registros.
- Registrar resultado e causa sem tokens/payloads pessoais. Reabrir a janela somente após revisão da falha. Uma falha administrativa não justifica alterar Core Android, OCR, overlay ou permissões.

## Condição final

**GO para revisão dos PRs e proposta de janela readonly. HOLD para executar o corte agora:** faltam revisão/autorizações de merge/rotas e medições/validações que só a janela permitirá. **HOLD separado para bootstrap real:** não existe evidência de sucesso nem produtor de sessão validado nesta ordem. A política de retenção PROD está aprovada, mas nenhuma exclusão ou concessão real está liberada.
