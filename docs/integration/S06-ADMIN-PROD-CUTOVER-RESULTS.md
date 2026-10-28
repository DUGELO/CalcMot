# S06 Admin — ensaio PROD e rollback

## Resultado

HOLD de produção. O cutover somente leitura foi efetivamente exercitado em 26/10/2026, mas a CPU observada acionou o critério de interrupção aprovado. Não habilitar mutações nem retomar tráfego PROD com base apenas no login bem-sucedido. O rollback do frontend foi retomado em 27/10 após interrupção da execução.

## Integração preservada

- Backend [PR #4](https://github.com/DUGELO/CalcMot/pull/4) integrado exclusivamente em growth: `00c0413ed6f12d70eed21c601206390a504879cb`. Head autorizado `9f3ef39`; CI remoto `40014129716`, typecheck, 140 testes aprovados, um remoto mutante desativado e dois dry-runs. O merge não publicou Workers.
- Angular [PR #7](https://github.com/DUGELO/calcmot-site/pull/7) integrado em main: `c20c8ceed0b90538d01254c7abbfce0528c9c429`. Variáveis remotas de build ausentes antes do merge. Primeiro deploy DEV: `9010ea23-8bf4-40c1-9cf4-ca98719a0663`.
- O código do site avançou posteriormente para `b4200894ee9e08bebfb0b3ca9d7f3681bb48dae5` (PR #9), versão `b515b469-8266-4cd0-a902-c2a55a179a44`. O rollback deve preservar esse código, sem voltar silenciosamente ao artefato anterior às mudanças de 24/10.
- Pipeline preservado: main → Cloudflare Builds → `npm run build` → `npx wrangler deploy`. Seleção administrativa ocorre na variável de build `CALCMOT_ADMIN_ENVIRONMENT`, não em binding de banco no site.

## O que foi exercitado no PROD

As duas rotas existentes foram atribuídas ao gateway PROD, preservando os padrões exato `/api/admin/v1` e `/api/admin/v1/*` e fail-closed; nenhum wildcard global, DNS ou novo recurso foi criado. `/admin` e API passaram a identificar PROD, com no-store/noindex.

O proprietário concluiu o login Google legítimo pelo navegador. O dashboard real exibiu destino PROD, somente consulta, zero solicitações e zero contagens. Foram acionadas atualizações de fila e contagens pela interface; as duas últimas rodadas de cinco atualizações tiveram conclusão/estado vazio confirmados individualmente. A primeira rodada não possui contagem HTTP conclusiva independente. Não foram extraídos tokens ou usados dados sintéticos no D1 PROD. A interface confirma sucesso funcional; não foi exportado trace de rede autenticado, portanto não apresentar isso como captura independente de todos os status HTTP 200.

HTTP remoto diretamente conferido sem credenciais: `/admin` 200/PROD/no-store/noindex; `/api/admin/v1/session` 401/PROD; raiz exata da API 404/PROD conforme o despacho. As seis páginas públicas retornaram 200 e tiveram SEO conferido pelo agente Angular; sitemap sem `/admin`. Conta comum 403, logout e a matriz completa de negativos PROD não foram concluídos nesta janela antes do STOP.

## CPU: causa do STOP

Fonte: painel nativo Cloudflare do Worker `calcmot-admin-gateway-prod`, CPU Time, período 26/10/2026 17:26–17:27 BRT (20:26–20:27 UTC).

| Evidência | Valor |
|---|---:|
| Timestamp do ponto inicial | 1793046360000 |
| P50 do ponto | 1,998 ms |
| P99 do ponto | **11,619 ms** |
| P99 no resumo da seleção, que inclui dois buckets | 7,2 ms |
| Máximo real exportado | Não disponível |

O ponto P99 viola o corte de P99 ≥8 ms e excede 10 ms. O resumo de 7,2 ms não permite ignorar o bucket de 11,619 ms; não fazer média de percentis para aprovar. P99 não foi chamado de máximo. Não há prova de erro 1102 neste registro, nem atribuição conclusiva do custo a JWT, inicialização ou outro trecho.

Ao observar esse resultado, foram interrompidas novas consultas autenticadas e iniciada a restauração DEV. Não se completou a exportação de três janelas de ambos os Workers, erros/consumo D1 ou máximo; tais campos permanecem desconhecidos. A medição não satisfaz o gate de aprovação. A leitura anterior das cotas Workers em 26/10 informou 1.124/100.000 requests/dia e 132/3.000 minutos de build/mês; não representa consumo atual nem medição D1. Não houve contratação/upgrade.

## Dados, gates e política

Workers PROD preservados, sem redeploy nesta janela: privado `2913aabd-a653-433e-9863-d9ffa0103f64`; gateway `1e18bfd2-c6a5-46c6-97c9-64d99ea0afed`, ambos 100% e `ADMIN_MUTATIONS_ENABLED=false`. Gateway sem D1, ADMIN apenas para privado PROD. Bancos DEV/PROD distintos.

Inventário anterior ao ensaio e posterior ao rollback, reconfirmado em 27/10 às 14:40 UTC: nove migrations, quatro índices, quick_check OK, zero violações FK, zero claims/grants/políticas/evidências/eventos/emissões/histórico/snapshots. Grant DEV: um registro, fingerprint privado inalterado; nenhum fingerprint ou identificador pessoal publicado.

Bootstrap PROD não executado: `bootstrap-admin-prod-policy.mjs` permanece o único mecanismo privado preparado, mas o produtor legítimo de sessão recente sem USB ainda não foi validado. Não há recibo de registro. OQ-07 PROD aprovada/documentada não equivale a política persistida. Nenhum novo grant, claim admin, IAM ou exclusão foi executado.

## Rollback e próximo gate

As duas rotas foram restauradas ao gateway DEV em 26/10; HTTP em 27/10 confirmou API DEV. A variável remota de build foi salva como DEV e reconfirmada em 27/10, preservando main e os comandos originais. Foi acionado Retry build do build `1b761701-027f-4174-88ea-9573d6779c77`, código b420089, para publicar o mesmo código com ambiente DEV.

Rollback concluído em 27/10/2026: [build 91e8ea61](https://dash.cloudflare.com/45086fe60ce934025fcac45e65eb4b69/workers/services/view/calcmot-site/production/builds/91e8ea61-2061-4da6-9dab-6c321d6950f6), duração 1m59s; versão `77d7366a-cd8e-4a20-9201-294a9751206b`, tráfego 100%, deployment `a39b14d7-0833-461c-a80e-61f80f89f586`, criado às 14:41:38 UTC. Não foi substituído o pipeline por publicação manual permanente: foi repetido o build Git existente com a seleção DEV persistida. O código versionado já mantém default DEV; nenhuma alteração de fonte é necessária para que o próximo autodeploy preserve essa seleção.

Smoke remoto às 14:42 UTC: `/admin` 200/DEV/no-store/noindex; `/api/admin/v1/session` 401/DEV/no-store/noindex; raiz exata da API 404/DEV; calculadora e sitemap 200, sitemap sem `/admin`. Frontend e API novamente consistentes, sem fallback silencioso entre ambientes. Usar essa versão DEV e o commit b420089 como baseline frontend atualizado; gateway DEV permanece `392e110d-1ab8-488a-8ee9-0ff9cba63551` e privado DEV `8524ed18-c710-45c6-ba14-f880780b945f` conforme último inventário, a reconferir antes de outra janela.

Validação final independente pelo agente Angular em 27/10: seis páginas públicas 200 com SEO correto, sitemap sem administração, `/admin` 200/DEV/no-store/noindex, sessão anônima 401/DEV. Calculadora real: R$18 / 10km / 20min → R$1,80/km e R$54/h. Versões DEV reconfirmadas por leitura: gateway `392e110d-1ab8-488a-8ee9-0ff9cba63551`, privado `8524ed18-c710-45c6-ba14-f880780b945f`, ambos 100%. Captura real local sem perfil: `.tmp/screens/admin-rollback/admin-dev-2026-10-27.png`.

Não excluir D1 PROD nem desfazer migrations. Próximo gate: diagnosticar a origem do custo de CPU com evidência e correção testada, repetir a homologação completa com métricas de ambos os Workers, concluir os controles negativos e validar o produtor privado de sessão para o registro da política. Nenhuma liberação comercial decorre deste ensaio.

## Nova decisão do proprietário — 27/10/2026

O proprietário confirmou expressamente manter PROD somente leitura com picos isolados de CPU tratados como alertas. Para este release, isso substitui os cortes automáticos de P99 8ms/máximo 10ms. Rollback continua obrigatório por falha real recorrente (incluindo1102), erro de autenticação/isolamento ou cota esgotada. Não altera o limite Free da plataforma nem transforma as métricas antigas em aprovação de capacidade. Gates comerciais permanecemfalse. Novo cutover em execução; registrar abaixo o resultado final, sem apagar o ensaio e rollback anteriores.
