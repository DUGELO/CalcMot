# S07 — Projeção segura de direitos

Data: **2026-10-14**. Branch `growth`; alterações existentes preservadas. **DONE no escopo autorizado de implementação e demonstração local.** Não é GO de operação com usuários ou deploy: S05/S06 continuam PARTIAL operacionalmente, OQ-07 permanece proposta não aprovada e CPU edge precisa de validação antes de publicar. Requisitos ENT-003/004, PRD §§9/11.1 e INV-01/02/03/04/14. Sem Billing, RTDN, paywall, S08/S09 ou Android.

## Componentes e decisão de persistência

- [Projetor](../../backend/src/entitlement.ts): combina fontes privadas em um snapshot S01 próprio, com uma consulta consistente por projeto/UID. Nenhum parâmetro cliente escolhe titular, direitos, períodos ou revisão.
- [Migration 0007](../../backend/migrations/0007_entitlement_source_versions.sql): adiciona apenas `entitlement_source_versions` e triggers de revisão. Backfill de revisão para fontes existentes, sem criar grants, políticas, compras, benefícios ou alterar migrations 0001–0006.
- [GET canônico](../../backend/src/index.ts): integração após JWT e proteção contra abuso existentes, mantendo envelope 200/401/503, no-store e X-Request-ID. POST/PUT/admin/internal continuam sem escritor de snapshots público.
- [Config local](../../backend/wrangler.jsonc): `ENTITLEMENT_PROJECTION_ENABLED=true` apenas no ambiente local. Configs DEV remotas/produção não foram alteradas; flag ausente preserva o leitor S02 anterior. É ativação técnica da integração após migration, não flag comercial/cobrança.

**GET permanece somente leitura**, conforme OpenAPI congelado. Não materializamos um novo JSON completo a cada consulta nem fazemos escrita escondida no GET. Os registros financeiros/promocionais privados já representados em `entitlement_snapshots` são uma entrada autoritativa; LegacyGrant/LegacyClaim são fontes independentes. A saída é o read model normalizado S01. Marcador persistente de versão muda na mesma transação que cada alteração das fontes, sem job/cron/fila ou novo serviço.

Não foram criados adaptadores de Play/promo, compras ou projetores financeiros fictícios. Essas stories futuras deverão alimentar fontes autoritativas e respeitar concorrência entre seus próprios escritores. A S07 **não escreve nem sobrescreve o JSON financeiro/promocional existente**.

## Regras efetivamente implementadas

| Situação | Resultado |
|---|---|
| Grant permanente S05 válido, inclusive avaliação assistida S06 | `legacy.state=GRANTED`, `legacyCore=true`, `coreAllowed=true`; nunca produz Pro sozinho. Grant não é reavaliado por idade da conta, versão atual, cutoff ou evidência já revisada. |
| Grant sem outra entrada de snapshot | Bootstrap só de leitura: Play/promo NONE, Pro falso/features vazias e cobrança desativada (`subscriptionRequired=false`, default atual). Não há novo grant ou assinatura. |
| Sem grant e sem entrada autoritativa suficiente | `503 RETRY_LATER`, retryable. Não fabricar legado, compra, período ou política de novos usuários. |
| Snapshot existente sem grant | Preserva `subscriptionRequired` privado e fontes; normaliza união/validade. Conta na ponte (`subscriptionRequired=false`) mantém Core; fixture de cobrança não é rollout real. |
| Play ACTIVE/CANCELED_WITH_ACCESS/GRACE com expiry futuro | Fonte Play permite Core/Pro. ACTIVE/TRIAL não vira prova de dinheiro ou recompensa. Demais estados não concedem novo direito Play. |
| Promo ativa | Permite Core/Pro somente em `startAt <= serverNow < validUntil`; futura/expirada não permite acesso. Não reativa assinatura. |
| Claim atual | UNDER_REVIEW/REJECTED e protocolo vêm da fonte; provisório só se houver data finita autoritativa já existente. Não cria sete dias. Grant permanente prevalece sobre claim e encerra provisório na view. |
| UNKNOWN Play/fase ou divergência temporal de acesso | `nextAction=RETRY`; preservar Legacy Core independente. Não inventar estado pago confirmado. |
| Fonte anteriormente presente desaparece / dado inválido / outage | 503 recuperável, sem snapshot falso de revogação ou perda do grant. Source marker guarda presença anterior para detectar ausência ambígua. |

`coreAllowed = !subscriptionRequired || legacyCore || playValid || promoValid || reviewValid` e `proAllowed = playValid || promoValid`. Features Pro vêm somente da entrada existente e são vazias quando Pro não está válido. A S07 não habilita Insights nem cria disponibilidade comercial. Datas/estado originais de Play/promo são preservados mesmo quando o período terminou; normalização não simula reconsulta à Play.

Grant é validado pelo schema congelado, UID, datas e concordância entre JSON/colunas; schema/UID/datas da claim também são conferidos. Snapshot de entrada inválido ou grant declarado sem fonte canônica falha recuperavelmente. Não apaga dados para reparar inconsistência.

## Revisão, idempotência e falhas

Marcador `(firebase_project_id, uid)` tem revisão monotônica, instante de alteração e três sinais de presença anterior. Triggers acompanham INSERT/UPDATE/DELETE de fontes; não fazem exclusões. Guardas impedem regressão da revisão, mudança de titular e perda dos sinais por UPDATE. Backfill sobe a revisão acima das entradas antigas. Buracos na sequência são permitidos: emissão S06 pode alterar grant e claim na mesma transação.

Uma única SELECT com quatro buscas por chave lê marcador, entrada existente, grant e claim numa visão consistente do D1. Não há read-modify-write do snapshot financeiro. Falha na emissão S06 reverte também o marcador; falha ao escrever revisão reverte a inserção do grant. Retry não duplica direito nem avança versão sem mudança real.

**Revisão representa versões das fontes.** Passagem do tempo não grava nem incrementa revisão: `serverNow` e os períodos normalizam a validade no instante de resposta. `checkedAt` corresponde à projeção/alteração privada, não a uma nova consulta Play; `play.checkedAt` original permanece. Futuro cache S08 deve respeitar datas mesmo sem nova revisão. Não introduzimos TTL, tolerância offline ou gate de acesso Android.

O Core atual não consome este endpoint no pipeline; erro de rede, falta de snapshot ou 503 não o bloqueiam. Só testes sintéticos simularam remoção de uma entrada para verificar perda de dados; nenhum dado pessoal real foi excluído.

## Demonstração e validações

- **112 testes backend PASS, 1 remoto opt-in SKIPPED.** Desses, 20 novos casos S07 executam D1 local real, JWTs sintéticos assinados e schemas S01; TypeScript PASS.
- Rodada final dos **20 S07 PASS**, incluindo Worker compilado executado em **workerd**, com binding D1 persistido e request HTTP pelo runtime (`dispatchFetch`). Não é somente mock de SQL ou chamada do handler em Node. Somente o entry de teste injeta chave pública sintética; Worker/console de produção mantêm JWKS Google fixo.
- Migração sobre grant/snapshot anteriores: grant preservado byte a byte, revisão reconciliada. Fontes assistida/verificável, reinstalação/nova sessão do mesmo UID, reinício do D1 e isolamento por projeto/UID comprovados.
- Matriz Play, promo futura/expirada e review finita; grant nunca vira Pro. Corrida de atualização financeira/promo com chegada de grant preserva todas as fontes. GETs concorrentes mantêm versão e não escrevem.
- Falha transacional na escrita do marcador e falha tardia na conclusão S06: sem grant/versão parcialmente confirmados; retry emite uma vez. Dados de fonte faltantes, schema inválido e indisponibilidade retornam envelope S01 sem detalhes privados.
- SQL EXPLAIN QUERY PLAN: **4 SEARCH por chave, 0 SCAN**. Contador do projetor: **1 prepare SELECT / GET, 0 writes**. O JWT existente continua sendo verificado antes dessa leitura.
- Wrangler aplicou migrations 0001–0007 exclusivamente em `.tmp/s07-wrangler-local`; repetição sem pendências. Fixtures ficam em banco de teste persistido separado, nunca em DEV remoto/produção.
- Bundle dry-run PASS: **290,30 KiB / gzip 41,82 KiB**; sem publicação. Sete contratos, migrations antigas e implementação S05/S06 preservados; Android sem mudança.

Resposta real do workerd local com grant sintético (campos abreviados aqui; teste valida o envelope completo):

```json
{
  "contractVersion": "1.0.0",
  "subscriptionRequired": false,
  "legacyCore": true,
  "coreAllowed": true,
  "proAllowed": false,
  "proFeatureSet": [],
  "legacy": {"state": "GRANTED"},
  "nextAction": "NONE"
}
```

É um recorte explicativo, não uma fixture completa do schema. Revisão permaneceu igual após reinício e nova sessão; nenhum UID, digest de evidência ou ator foi exposto na resposta. Evidência local minimizada: `.tmp/s07-evidence.json`.

## Reprodução local e limites de custo

Em `backend`, usando somente configuração local:

```powershell
$env:WRANGLER_SEND_METRICS='false'
node node_modules/wrangler/bin/wrangler.js d1 migrations apply DB --local --persist-to ../.tmp/s07-wrangler-local
node node_modules/vitest/vitest.mjs run tests/entitlement.integration.test.ts
node node_modules/wrangler/bin/wrangler.js dev --local --persist-to ../.tmp/s07-wrangler-local
```

Banco Wrangler limpo não recebe fixture automaticamente. Os testes criam seu próprio banco isolado e entry workerd com chave **somente de teste**; não usar esses tokens na API com JWKS Google nem adicionar bypass de assinatura. Não há credenciais, service account, novo pacote pago ou recurso remoto nesta execução.

Workers Free documenta CPU de 10 ms/request; D1 Free tem quotas de linhas lidas/escritas. Otimização estrutural aqui não prova margem no edge: **CPU real dessa versão não foi medida**. JWT/JWKS frio, validação JSON e tamanho real das fontes precisam de amostras DEV autorizadas antes de rollout; latência local não será apresentada como CPU Cloudflare. Cada alteração de fonte escreve um marcador adicional (duas em emissão grant+claim), custo que deve entrar no orçamento futuro. [Workers limits](https://developers.cloudflare.com/workers/platform/limits/), [D1 pricing](https://developers.cloudflare.com/d1/platform/pricing/).

## Dependências restantes e fronteira de DONE

- **Operação real:** S05/S06 ainda PARTIAL por operador/ensaio Google; não concedemos direito real para provar S07. Ativar flag remota exige migration e validação/autorização de ambiente própria.
- **Privacidade:** OQ-07 continua OPEN/proposta. Novo marcador contém UID/metadados pessoais e deve entrar no desenho S15 de retenção/exclusão; nenhuma retenção infinita ou exclusão foi aprovada/implementada.
- **Fontes futuras:** Billing/promo e controles de seus escritores seguem suas stories. Fixtures de assinatura não comprovam dinheiro real e não habilitam benefícios.
- **Android:** cache offline/entrada segura e UI são S08/S09, não iniciadas. A preservação do Core atual decorre de não ligá-lo a esta consulta, não de um fallback infinito inventado.
- **CPU/publicação:** medir edge antes de declarar prontidão operacional, sem fazer upgrade automático se ultrapassar quota.

**S07 DONE quanto ao escopo local pedido:** fontes conectadas ao endpoint, schema, revisão, falhas, concorrência e recuperação demonstrados em D1/workerd reais locais. **Prontidão de produção permanece HOLD** pelos itens acima. Não é fechamento de S05/S06/OQ-07 nem autorização para avançar outra story.

Arquivos novos: migration 0007, `src/entitlement.ts`, `tests/entitlement.integration.test.ts` e este relatório. Alterados: handler, configuração Wrangler local, README, BACKLOG, EXECUTION-PLAN e nota ADR-001. Sem mudança no código S05/S06, contratos, migrations antigas ou Android; sem deploy/push/serviço pago.
