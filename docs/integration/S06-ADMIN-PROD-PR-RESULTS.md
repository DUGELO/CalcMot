# Admin PROD — entrega dos PRs e resultado final23/10/2026

## PRs sem merge

- Backend [DUGELO/CalcMot#4](https://github.com/DUGELO/CalcMot/pull/4), branch codex/admin-prod-readiness → growth. Código e testes verificados no commit09d83bbd9fc032790a21abedb4c467294b5c28ba; o commit posterior deste relatório altera somente evidências/documentação. PR em rascunho enquanto os HOLDs de ativação permanecem.
- Angular [DUGELO/calcmot-site#7](https://github.com/DUGELO/calcmot-site/pull/7), codex/admin-environments → main, head ff80f6b0d1ab2869b799ca0487b6365989b02fe9,28arquivos. Checks Site CI/Lighthouse/preview aprovados, inclusive testes públicos mobile/GA4. Sem conflitos reportados, sem merge.

A fundação backend S06/S07 e S01 ainda não estava na base remota growth. O PR backend a versiona para permitir revisão/testes reproduzíveis, além da preparação PROD; não é uma reimplementação nem inclui o Android. Todos os blobs de docs/contracts são idênticos ao baseline local226fe22f, sem edição de S01. A linha vazia antiga no EOF da migration0008 foi preservada, única exceção documentada de whitespace; nenhuma migration aplicada foi reescrita. O workspace principal permaneceu em growth/226fe22f e suas alterações Android foram preservadas. O PR usa índice separado, não um git add global do workspace.

## Validação do commit isolado

- Extração do Git apenas do backend/contratos e arquivo Android baseline lido por teste; sem node_modules, .tmp, configs locais ou alterações Android do workspace.
- npm ci pelo lockfile e TypeScript PASS.
- Suíte completa:13arquivosPASS,140testesPASS,1teste de escrita remota DEV intencionalmente desativado por CALCMOT_REMOTE_DEV_TEST=0. Nenhuma fixture remota criada.
- Os dois testes que gravam evidência S06/S07 dependiam da existência prévia de ../.tmp. Corrigido exclusivamente o setup de diretório em cada suíte; repetição integral passou. Nenhuma regra de concessão/cache/Core alterada.
- Dois dry-runs Wrangler4.149.0 PASS: gateway44,01KiB, privado156,46KiB. São bundles locais, não um novo deploy ou medida CPU.
- Manifesto versionado:29inputs e9migrations, SHA256 com normalização UTF8/LF, zero divergências contra o checkout limpo após gerar validadores. Não contém UID, token, secret ou snapshot de dados privados.
- Atualização pré-cutover: backend agora possui CI remoto reproduzível, descrito abaixo. Os resultados locais anteriores continuam preservados como evidência separada.

## Leitura remota, sem mudanças

Versões realmente ativas reconferidas por deployments/list + versions/view: privado2913aabd-a653-433e-9863-d9ffa0103f64 e gateway1e18bfd2-c6a5-46c6-97c9-64d99ea0afed, ambos100%, environmentPROD, ADMIN_MUTATIONS_ENABLED=false e allowlist por secret presente. O gateway tem Service Binding ADMIN para o privado PROD e nenhum D1; o privado tem somente o D1 PRODd0232771-b6e7-49a1-ae9f-05267fbdf3a1. D1 list confirmou os nomes e UUIDs distintos DEV/PROD.

Inventário PROD: schema0001–0009, quatro índices, quick_checkOK, foreign_key_check vazio, zero claims/grants/policies/events/issuances/evidence/history/snapshots. Comparação do fingerprint do grant DEV em memória:igual,1grant. Quatro páginas públicas consultadas200, sem header administrativo. Nenhuma nova rota, migration, recurso, claim/IAM, exclusão, grant ou publicação foi executada nesta ordem.

## Política PROD e OQ-07

OQ-07 estendida expressamente pelo proprietário a PROD23/10: evidências30dias, suporte90dias, auditoria mínima12meses, logs sanitizados30dias, descarte separado do grant e exclusão integral por pedido verificado. Procedimento: [OQ-07-PROD-OPERATIONS.md](../product/OQ-07-PROD-OPERATIONS.md). Isso fecha a decisão operacional de prazos; não autoriza execução destrutiva agora nem certifica todo o produto perante LGPD.

Registro da política de elegibilidade: ferramenta privada já implementada preservada, agora com recibo não sensível somente após reler a política persistida (operationId estável, policyVersion, policyRecordedAt, verifiedAt, baseline). Falha após tentativa de escrita reporta resultado não confirmado, sem prometer rollback inexistente. --check offline passou; --apply NÃO executado. Nenhum recibo de sucesso ou data de registro real foi inventado; o banco continua com0políticas.

Bloqueio específico: falta um produtor de sessão Google/Firebase recente que entregue stdin ao mecanismo privado, já validado sem USB. A sessão do Angular/Console não foi extraída; não foram solicitados tokens no chat, clipboard ou arquivos, relaxadas restrições da chave Web ou criadas autenticações/endpoints alternativos. A preparação está pronta para receber a autoridade legítima, mas não é correto declarar o bootstrap operacional concluído sem esse canal e confirmação humana de execução.

## GO/HOLD para próxima decisão

| Gate | Resultado |
|---|---|
| Revisar PRs, artefatos e manifesto | GO; links e resultados acima |
| Separação DEV/PROD e gates false | GO reconfirmado |
| OQ-07 documental para operação | GO aprovado; sem exclusão executada |
| Registro inicial de política real | HOLD por canal de sessão recente privado não validado; sem grant |
| Coleta de CPU real JWT+D1 | Preparada; HOLD de medição até janela readonly autorizada, sem números fabricados |
| Merge e duas rotas públicas | HOLD de autorização/revisão, não executados |
| Manter PROD após janela | Depende de login/ownerGET200, negativos, banco/ambiente, CPU/cotas e público/SEO durante a janela |
| Concessão comercial/mutações/Android real | HOLD separado; não faz parte de readonly/admin GET |

Procedimento único: [S06-ADMIN-PROD-READINESS.md](S06-ADMIN-PROD-READINESS.md). O objetivo da janela é comprovar autenticação/CPU que não podem ser afirmadas antes de haver tráfego autorizado. Bootstrap e aparelho não são dependências técnicas de GET no banco vazio; não usar isso para liberar emissões. Se falhar, manter gatesfalse, restaurar par frontend/rotas conforme baseline autorizado e preservar ambos os bancos e grants.

## Integração pré-cutover — 23/10/2026

Check remoto implementado em .github/workflows/admin-backend-readiness.yml, apenas pull requests destinados a growth, sem gatilho de publicação, credenciais remotas ou permissões de escrita. Ubuntu24.04/Node24, npm ci, typecheck, suíte sintética e dois wrangler deploy --dry-run. CALCMOT_REMOTE_DEV_TEST=0 obrigatório.

Execução [40014129716](https://github.com/DUGELO/CalcMot/actions/runs/40014129716), commit1bbe2c2: PASS;140testes aprovados/1remoto mutante ignorado, typecheck e ambos dry-runs aprovados. Privado156.46KiB/gzip31.70; gateway44.01KiB/gzip12.31. Nenhum Worker publicado. O follow-up documental não altera runtime nem migrations.

Leitura remota reconfirmou versões PROD privado2913aabd-a653-433e-9863-d9ffa0103f64 e gateway1e18bfd2-c6a5-46c6-97c9-64d99ea0afed,100%,gatesfalse, gateway sem D1/binding ADMIN para privadoPROD; bancos distintos. PROD quick_checkOK/FK0/schema9/índices4 e oito categorias de negócio zeradas, inclusivepolíticas/grants. GrantDEV1/fingerprintinalterado; quatro páginas públicas200.

Angular confirmou PR7/headff80f6b/checksSiteCI,Lighthouse,Cloudflareaprovados; default do códigoDEV e comandos npm run build → npx wrangler deploy preservados. A variável remota CALCMOT_ADMIN_ENVIRONMENT não foi reconfirmada nesta leitura: conferir seu valor/ausência antes da janela; não foi alterada para PROD.

GO para revisão final dos PRs. HOLD de merge/rotas/publicação até autorização específica. Janela readonly preparada conforme READINESS: fixar os SHAs/versões e rollback pareado; reconfirmar configuração remota do build; coordenar seleçãoPROD e somente duas rotas exatas; manter gatesfalse; testar ownerGET200/PROD, anônimo401/comum403, banco vazio/público/SEO e CPU/cotas. CPU real ainda não medida nesta ordem: interromper se houver erro de autenticação/isolamento/roteamento, limite Free, P99>=8ms ou máximo observado>=10ms. Ausência de métricas significa HOLD, não zeroCPU. PolíticaPROD e concessões permanecem bloqueadas; USB/ADB não é requisito para GETreadonly. Nenhum merge, deploy, rota, IAM/claim, exclusão ou grant executado.
