# CalcMot Admin PROD — publicação somente leitura

> Histórico de 27/10. Em 28/10 o proprietário autorizou operações reais, a política foi registrada e ambos Workers foram ativados. Estado vigente: [ativação operacional](S06-ADMIN-PROD-ACTIVATION.md). A restrição readonly abaixo foi substituída; os fatos históricos foram preservados.

Data: 27/10/2026. Este registro complementa o histórico em S06-ADMIN-PROD-CUTOVER-RESULTS.md; o rollback anterior não representa o ambiente atualmente publicado.

## Decisão vigente

O proprietário confirmou manter PROD somente leitura e tratar picos isolados de CPU como alerta. Para este release, substituiu expressamente os cortes automáticos de 8/10ms por rollback diante de falha real recorrente (incluindo1102), erro de autenticação/isolamento ou esgotamento de cota. O limite nominal da plataforma não mudou. Nenhuma autorização de mutação comercial ou grant foi concedida.

## Publicação efetiva

- Backend PR4 → growth, merge `00c0413ed6f12d70eed21c601206390a504879cb`; Angular PR7 → main, merge `c20c8ceed0b90538d01254c7abbfce0528c9c429`. Ambos reconfirmados como merged; nenhum merge repetido.
- Código atual do site preservado: `b4200894ee9e08bebfb0b3ca9d7f3681bb48dae5`, incluindo as mudanças posteriores do PR9.
- Cloudflare Builds: `CALCMOT_ADMIN_ENVIRONMENT=PROD` salvo. Main/build habilitados e comandos originais preservados: `npm run build` → `npx wrangler deploy`.
- Build `11aadd4c-ff7f-48d5-9d9f-f1898f7e86cd`; site ativo `3eb20601-6320-4d69-aa28-f19713f41cd8`, 100%; deployment `bc82645b-f6e3-427d-b6db-bbce60f93cf2`, criado 15:08:38 UTC.
- Somente as duas rotas existentes `/api/admin/v1` e `/api/admin/v1/*` foram atribuídas ao `calcmot-admin-gateway-prod`, preservando fail-closed. Sem DNS, wildcard global, Pages ou nova infraestrutura.
- Gateway PROD `1e18bfd2-c6a5-46c6-97c9-64d99ea0afed`; privado PROD `2913aabd-a653-433e-9863-d9ffa0103f64`, sem redeploy. Ambos com `ADMIN_MUTATIONS_ENABLED=false`; gateway sem D1, binding ADMIN apenas para privado PROD, ligado exclusivamente ao D1 PROD.

## Homologação remota

| Verificação | Evidência |
|---|---|
| `/admin` | 200, header PROD, no-store, noindex; CSR sem dados administrativos no HTML |
| Proprietário Google real | Login pelo SDK/navegador; dashboard, fila e contagens reais vazios; banner PROD/somente consulta |
| Contrato de sessão e consultas | Interface aceitou sessão compatível e respostas de fila/contagens; sem exportar trace HTTP autenticado nem extrair tokens |
| Conta comum Google real | Negada pelo backend; UI sem autorização e sem dados administrativos |
| Sessão anônima | 401 AUTH_REQUIRED/PROD |
| Token inválido sintético | 401 AUTH_REQUIRED/PROD |
| Origem indevida ou marcador ausente | 403 FORBIDDEN/PROD |
| Raiz exata da API | 404/PROD, conforme despacho de rotas |
| Acesso direto workers.dev | 404 sem resposta administrativa para gateway e privado; privado segue sem rota pública |
| Site público | Seis páginas 200; títulos/H1/descrições/canonical corretos; sitemap 200 sem `/admin` |
| Calculadora | R$18/10km/20min → R$1,80/km e R$54/h |

Não foram criados pedidos sintéticos em PROD para testar detalhes/evidências/auditoria. Esses serviços reutilizam a implementação S06 e sua cobertura já existente; o banco vazio não permite alegar um ensaio operacional de decisão/emissão nesta publicação. UID correto sem admin, tokens expirados/outro projeto e concorrência continuam cobertos pelos testes sintéticos anteriores, sem fabricar credenciais para repetir esses casos remotamente.

## Dados e custo

Precheck e inventário após consultas: nove migrations, quatro índices, quick_check OK, FK0, oito categorias de negócio zeradas, inclusive grants e políticas. Grant DEV original: um registro, fingerprint privado inalterado. Nenhuma mutação comercial, grant, exclusão ou mudança IAM/claims executada.

Cotas compartilhadas lidas em 27/10 antes da amostragem: Workers378/100.000 requests/dia; eventos452/200.000; build134/3.000min/mês; D1446/5milhões de linhas lidas,0/100mil escritas;475,14kB/5GB,2/10bancos. Em leitura posterior D1 passou a661linhas,0escritas. Delta compartilhado, não atribuído integralmente ao PROD. Sem cobrança/cartão/upgrade.

## Rollback preservado

Baseline frontend DEV: `77d7366a-cd8e-4a20-9201-294a9751206b`, mesmo código b420089. Gateway DEV `392e110d-1ab8-488a-8ee9-0ff9cba63551`; privado DEV `8524ed18-c710-45c6-ba14-f880780b945f`. Restaurar somente as duas rotas ao gateway DEV, salvar seleção de build DEV e repetir o build Git correspondente, preservando alterações posteriores do site. Nenhum banco/migration deve ser removido ou revertido. O rollback anterior foi realmente executado e validado, não apenas descrito.

## Limites da liberação

Painel publicado para consulta. Mutações e concessões comerciais continuam OFF. Bootstrap da política não executado: produtor privado de sessão recente sem USB ainda não validado; não há recibo real nem política persistida. O mecanismo existente não foi contornado, nem foi criado endpoint público de bootstrap. Essa pendência é separada das consultas administrativas e da verificação física Android para futura ativação comercial.

## Encerramento da verificação — 27/10/2026

PROD permanece publicado e somente leitura. Nova consulta HTTP final confirmou `/admin` 200 PROD/no-store/noindex, `/session` anônimo 401 PROD, calculadora e sitemap 200 e ausência de admin no sitemap. Dashboard autenticado permanece funcional, com contagens e fila vazias; a aba foi preservada para o proprietário. Não foi repetido logout nesta última passagem para não encerrar sua sessão; a validação anterior de logout permanece histórica.

CPU real observada no gráfico Cloudflare, janela 15:13–15:16 UTC: gateway P50 agregado exibido 1,65 ms, P99 4,96 ms; privado P50 2,07 ms, P99 7,53 ms. Buckets individuais chegaram a P99 8,409 ms no gateway e 10,398 ms no privado. Máximo por requisição não disponibilizado: não confundir com P99. Foram confirmadas 19 invocações por Worker na janela inicial, sem erros nem CPU exceeded reportados. A amostragem adicional espaçada não teve três períodos de dez leituras confirmados nas métricas; esse critério permanece incompleto, sem alegação de capacidade comprovada. Valores zero de buckets sem dados não são CPU zero.

Conforme decisão explícita do proprietário, picos isolados são alertas e não provocam rollback deste release; falha real recorrente, erro de autenticação/isolamento ou cota esgotada exigem rollback. Não há monitor recorrente novo configurado. Nenhuma mutação comercial foi habilitada e nenhuma política ou concessão foi criada para completar artificialmente a homologação.

Captura real final: `.tmp/screens/admin-prod-live/dashboard-prod-final-2026-10-27.png`. Status: consultas PROD operacionais; bootstrap privado da política e fechamento da amostragem completa permanecem pendentes, separados da disponibilidade do painel.
