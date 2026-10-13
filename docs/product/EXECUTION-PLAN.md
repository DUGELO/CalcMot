# Execution Plan — CalcMot Growth & Monetization V1

Data: 2026-10-09. Fonte: PRD v5 integral, identificado por hash no [BACKLOG.md](BACKLOG.md). Checkout analisado: `growth` em `9d78bd76b3a373f139a8360a6e527fa35e7ec0fa`. Escopo desta entrega: planejamento; **nenhuma implementação ou ativação externa**.

**12 epics, 52 stories.** Em 2026-10-09, S01 foi concluída documentalmente: [contratos v1.0.0](../contracts/README.md), OQ-08 resolvida. S03 foi encerrada como [auditoria de plataforma](S03-PLATFORM-AUDIT.md), com prontidão de monetização HOLD e pendências específicas; não é aprovação de release. Nenhum código de produção ou serviço habilitado. S02 e S04–S52 continuam não iniciadas. As fases abaixo são ondas topológicas de trabalho, não prazos nem barreiras globais. Uma story pode iniciar assim que suas dependências reais e decisões necessárias estiverem prontas. `||` indica dependências satisfeitas e possibilidade de trabalho por agentes diferentes, com contratos e propriedade de arquivos respeitados. Nenhum agente adicional foi iniciado.

## Antes da execução

Atualização 2026-10-10: S02/S04 possuem implementação local **PARTIAL**, restrita ao pedido específico do usuário. [Resultados/configuração pendente](S02-S04-INTEGRATION.md) e [ADR-001](ADR-001-FREE-IDENTITY-BACKEND.md). A declaração histórica de stories não iniciadas acima refere-se a 09/10. Nenhum deploy/upgrade, nenhuma autorização para avançar às próximas fases.

Ler a checagem de implementabilidade do backlog. S01 congela os contratos mínimos pedidos pelo PRD; não resolve política comercial silenciosamente. AR-01 confirma ambientes/linguagem/local do backend, Console/domínio/segredos. AR-02 confirma as fontes/meio de relatório. OQ-01–12 identificam as decisões que faltam; não aceitar enum/valor local como evidência de uma decisão externa.

Preparar desde o começo acesso e prova financeira OQ-04. S21 resolve a capacidade real de distinguir trial de transação paga; ausência de prova bloqueia S22 e gates financeiros. Não esperar checkout acabado para descobrir que `ACTIVE` não comprova dinheiro.

S03 é auditoria de publicação, não mudança automática de permissões/target/versionCode. S52 pode preparar a hipótese/screenshot/acesso desde o começo; está posicionada na fase 5 para que a leitura de ativação S10/S34 possa ser incorporada. Não depende de assinatura. Seu período mínimo de observação continua sendo ≥7 dias e seu fechamento não é imediato.

## Execution Plan

```yaml
Fase 1: "S01 || S03"

Fase 2: "S02 || S04"

Fase 3: "S05 || S10 || S14 || S18 || S28"

Fase 4: "S06 || S11 || S29 || S34 || S37 || S45"

Fase 5: "S07 || S12 || S30 || S52"

Fase 6: "S08 || S15 || S19"

Fase 7: "S09 || S13 || S17 || S20 || S21 || S31 || S38"

Fase 8: "S16 || S22 || S24 || S32 || S51"

Fase 9: "S23 || S35 || S36 || S50"

Fase 10: "S25 || S33 || S39"

Fase 11: "S26 || S40"

Fase 12: "S27 || S41 || S42 || S46"

Fase 13: "S43 || S47"

Fase 14: "S44 || S48"

Fase 15: "S49"
```

**Condições de execução que o YAML não elimina:** S37 é pesquisa/gate de utilidade; S38 só implementa após GO. Sem esse GO, S38/S50 e recompensa de legado ficam HOLD, enquanto ponte, orgânico, Billing e incentivo de novos podem avançar. S16 fecha a release A; janela real/cutoff aprovado permanecem condição para cobrança em S27. S39–S47 podem ser implementadas com fixtures/flags OFF antes de produção; S44/S49 só ativam piloto depois de primeira cobrança real conciliada e aprovação dos gates. S52 precisa da janela observada; não equivale a um patch de um dia. Não forçar 25/50/100% por calendário.

## Grafo de dependências por story

Esta tabela é a lista de pré-requisitos técnicos; requisitos de ambiente, respostas abertas e aprovações constam na story e nos gates abaixo. Dependência transitiva não precisa ser repetida. S52 tem dependência de leitura/fechamento de ativação, não de preparação do experimento.

| Story | Epic | Pré-requisitos técnicos |
|---|---|---|
| S01 | E01 | — |
| S02 | E01 | S01 |
| S03 | E01 | — |
| S04 | E02 | S01 |
| S05 | E02 | S02, S04 |
| S06 | E02 | S05 |
| S07 | E02 | S05, S06 |
| S08 | E02 | S07 |
| S09 | E02 | S04, S06, S08 |
| S10 | E03 | S01, S04 |
| S11 | E03 | S10 |
| S12 | E03 | S03, S11 |
| S13 | E04 | S02, S07, S08 |
| S14 | E04 | S01, S04 |
| S15 | E04 | S02, S05, S06, S07, S14 |
| S16 | E04 | S03, S09, S10, S12, S13, S14, S15 |
| S17 | E05 | S08, S10, S14 |
| S18 | E05 | S01, S03, S04 |
| S19 | E05 | S02, S04, S07 |
| S20 | E05 | S19 |
| S21 | E05 | S19 |
| S22 | E05 | S20, S21 |
| S23 | E05 | S07, S20, S22 |
| S24 | E05 | S08, S13, S14, S17, S18, S19, S28 |
| S25 | E05 | S20, S23, S24 |
| S26 | E05 | S25 |
| S27 | E05 | S16, S22, S23, S24, S25, S26, S36 |
| S28 | E06 | S02, S04 |
| S29 | E06 | S28 |
| S30 | E06 | S28, S29 |
| S31 | E06 | S08, S10, S14 |
| S32 | E06 | S13, S28, S30, S31 |
| S33 | E06 | S16, S32, S35 |
| S34 | E07 | S10 |
| S35 | E07 | S28, S30, S32, S34 |
| S36 | E07 | S20, S22, S34 |
| S37 | E08 | S10 |
| S38 | E08 | S08, S10, S37 com GO |
| S39 | E09 | S22, S23, S28 |
| S40 | E09 | S39 |
| S41 | E09 | S23, S40 |
| S42 | E09 | S08, S23, S40; S38 apenas para modalidade legado |
| S43 | E09 | S32, S40, S41, S42; S38 apenas para modalidade legado |
| S44 | E09 | S27, S35, S39, S40, S41, S42, S43; S38 GO apenas para legado |
| S45 | E10 | S28 |
| S46 | E10 | S22, S23, S39, S40, S45 |
| S47 | E10 | S15, S46 |
| S48 | E10 | S34, S35, S36, S39, S46, S47 |
| S49 | E10 | S13, S27, S46, S47, S48 |
| S50 | E11 | S13, S32, S38; condicional ao cartão baseado no Pro |
| S51 | E11 | S10, S13, S31 |
| S52 | E12 | — para preparar/iniciar; S10/S34 para leitura canônica de qualidade + ≥7 dias para fechar |

## Caminho crítico

Sem estimativas aprovadas de esforço/duração não há caminho crítico temporal calculável. Abaixo está o **caminho crítico lógico de dependências**, com os joins obrigatórios; observação, acessos e gates humanos podem dominar o tempo real.

**Até o primeiro incremento utilizável (A):**

```text
S01 → (S02 || S04) → S05 → S06 → S07 → S08
    → (S09 || S13) → S16
                         ↑ S03, S10, S12, S14, S15 também obrigatórias
```

**Até o primeiro MVP de monetização (C):**

```text
S01 → (S02 || S04) → S05 → S06 → S07 → S19
    → (S20 || S21) → S22 → S23 → S25 → S26 → S27
                                         ↑ S24     ↑ S16/cutoff, S36
```

S24 exige a convergência do cache/flags, demo/readiness, catálogo, verify e lock backend S28 antes do checkout. Esse lock mínimo não exige release B, link ou Hub. S16 exige a ponte/recuperação/compatibilidade/privacidade. OQ-04/S21 é o risco principal de viabilidade financeira; OQ-02 e a janela da ponte são o risco de autorização comercial. Pro Insights, cartão, review, creator e reward não entram neste caminho de C.

**Até a V1 com creators/contabilidade, caminho lógico mais longo (15 níveis técnicos):**

```text
S01 → (S02 || S04) → S05 → S06 → S07 → S19
    → (S20 || S21) → S22 → S23 → S39 → S40
    → S46 → S47 → S48 → S49
```

O ramo driver reward converge em `S40 → (S41 || S42) → S43 → S44`, com S27/firstPaid real e termos/custos como portas adicionais. S49 não exige ativar campanha driver nem Pro legado, mas exige exclusividade e contabilidade compartilhadas corretas. Renovações trimestrais podem amadurecer depois de 90 dias; não marcar gate de receita/retorno como aprovado por calendário.

## Paralelismos reais e propriedade dos arquivos

| Depois de | Stories/trilhas paralelizáveis | Motivo e limite |
|---|---|---|
| início | S01 || S03; preparação de S52 | Contratos, auditoria Console e listing usam evidências distintas; auditoria não muda build silenciosamente. |
| S01 e ambientes | S02 || S04 | API/rules server-side e identidade Android; contrato de token/UID único. |
| identidade S04 + API S02 | S05 || S10 || S14 || S18 || S28 | Grant backend, análise local, privacidade, catálogo e atribuição; não fazer alterações concorrentes em bootstrap/build/rules sem integração coordenada. |
| S07 e catálogo disponível | S08 || S19 | Cache Android e verify Play backend; entitlement/schema congelados. S18 e S19 também podem ocorrer simultaneamente após seus respectivos pré-requisitos. |
| S19 | S20 || S21 | RTDN/estado e prova financeira oficial; evidência paga não é derivada do trabalho RTDN. |
| S08/S10/S14 | S17 || S31, junto de S09/S13 quando prontos | Demo, política growth, recuperação e flags em pacotes distintos; navegação/global são integrados serialmente. |
| contratos de identidade/atribuição | E06/S29–S35 || E05/S18–S27 | Distribuição orgânica não depende de cobrança; integrações financeiras ficam desligadas. |
| S10 e gate Pro | S37/S38 || trilha de Billing | Insights é benefício adicional condicional, não pré-requisito comercial de novo usuário. |
| S40 e reconciliação | S41 || S42 || S46 | Defer, promoção e comissão implementam operações diferentes; reserva de canal/dias e schemas já congelados. Não aplicar dois destinos de reward simultaneamente ao mesmo direito. |
| aplicação/commission prontas | S43 || S47 | Hub Android e lote backend; ledger/estados comuns sob contrato. |
| dados/atores prontos | S44 || S48 | Gate de reward e análise de custo creator; gate exige custo da campanha driver mesmo que relatório creator ainda esteja sendo finalizado. |

`||` não autoriza editar o mesmo arquivo compartilhado ao mesmo tempo. Usar PRs/checkouts isolados durante futura execução, escopo por pacote e um integrador para `CalcMotNavigation.kt`, `MainActivity.kt`, `CalcMotApplication.kt`, Gradle, manifest, event allowlists, rules/índices e docs/contracts. Congelar contratos antes; extensões de domínio são revisadas e integradas sequencialmente. Cada agente entrega testes da própria story; QA em dispositivo usa build fixo identificado, não checkout mudando durante a captura. Não criar novos módulos/microserviços para facilitar agentes.

S41/S42 compartilham a decisão de destino do mesmo reward: testes rodam separados com fixtures; aplicação em produção passa por um único fluxo server-side reservado. S46 deve provar que canal creator não gera prêmio driver mesmo com processadores concorrentes. Isso é consistência requerida pelo PRD, não dependência artificial para impedir trabalho em paralelo.

## Incrementos e gates de release

| Incremento | Conteúdo mínimo/fechamento | Valor utilizável | Porta de avanço |
|---|---|---|---|
| **Primeiro incremento — A, S16** | Contratos/API, identidade/grant/claim/cache/recovery, Core events/compatibilidade, flags, privacidade/exclusão. S34 pode acompanhar sem bloquear Core. | Uso gratuito, legado recuperável e métricas canônicas; nenhum pagamento. | Restore de todos os legados identificados de teste, claims tardios/suporte, Core sem regressão, políticas/build. Recuperação frágil amplia ponte; cutoff posterior à janela aprovada. |
| **Primeiro MVP coerente de Growth — A+B, S33** | A + atribuição/link/referrer/Hub/safety e S34/S35. | Compartilhamento consentido e aquisição ativada medível, sem incentivo. | app/no-app, lock/firstCore, caps/opt-out e ausência de PII/marketing em oferta; baixa aquisição informa aprendizado, não receita fictícia. |
| **Primeiro MVP de monetização — A+C, S27** | A/cutoff + demo/catálogo/verify/ACK/RTDN/prova de cobrança/financeiro/reconcile/checkout/status/restore/troca, lock mínimo S28 e S36. B pode já estar entregue em paralelo, mas link/Hub não são requisito para cobrar novos organic. | Novos podem escolher plano/trial real; compra/restauração/erro têm tratamento correto. | Internal→closed→5% novos elegíveis; dupla revisão, preço/termos Play, nenhum P0/P1. Sem utilidade Pro validada não prometer Insights; monetização de Core novo continua viável. |
| **D reward driver — S44** | C + S39–S43, termos/custos; legado exige S37 GO e S38 funcional. | Dias conquistados/aplicados transparentes, teto 60 por campanha. | firstPaid real, janela void, defer/promo e exclusividade; sem benefício novo, legacy reward OFF. |
| **D creator — S49** | C + source/registry, comissão/lote/economia S45–S48 e exclusividade S39/S40. | CPA R$3+R$1 e scorecard sem PII. | Três creators em piloto, firstPaid/renovação reais, custos/repasse observados; não escalar por comissão ≤R$4. |
| **Otimizações independentes** | S51 review; S52 ASO; S50 cartão condicional a Pro/conteúdo aprovado. | Divulgação/review responsáveis e experimento de loja. | Sem recompensa de review, uma screenshot ≥7d, qualidade maturada/rotulada; P2 não retarda A/B/C. |

**O primeiro MVP coerente** aqui é A+B, por entregar preservação, valor e distribuição mensurável cedo, conforme §14.1 do PRD. A+C é o primeiro MVP que inclui cobrança, com gates próprios. Nenhum dos dois exige todos os incentivos, creators ou cartão para ser utilizável.

## Gates comerciais, observação e rollback

- **P0 plataforma/compatibilidade:** S03/S12, developer/package/target/Billing oficial/disclosure/consentimento e versões cobertas. Dependência sem cobertura → HOLD; violação/bypass/Core crítico → STOP.
- **A/cutoff:** S16 + OQ-02; publicar ponte e comunicar antes de cobrar. Evidência de recuperação insuficiente → estender janela, sem inventar data. Grant imutável e claims não são removidos por rollout.
- **C fechado:** ciclo trial/monthly/quarterly/pending/canceled/grace/hold/paused/void/ACK/RTDN/troca/restore e financeiro S21/S22. Sandbox/licença não prova dinheiro em produção; execução dos gates não é só build verde.
- **C progressão:** 5→25→50→100% sticky apenas para novos pós-cutoff; coortes novas observáveis, D1/D7/Activation72h, Core/suporte/cancelamento/refund e receita conciliada. Amostra pequena pode manter 5%; ≥20% relativo proposto é investigação/HOLD, sem declarar significância. Termos/limites/owners pendentes são explícitos.
- **D:** firstPaid pago/não anulado e janela cumprida; driver 1/3/5→7/30/60 totais, creator R$3+R$1 após ciclo contratado. Termos OQ-06, custo cash/econ/contribuição/payback e exclusividade. Legado sem Insights útil fica sem reward e com share voluntário.
- **STOP imediato:** legado comprovado bloqueado; compra cobrada/validada sem entitlement; cobrança incorreta/duplicada; reward duplicado; comissão sem pagamento/double benefit; privacidade violada; marketing/UI durante overlay/condução; regressão crítica Core/crash/ANR/latência. Owner/ticket e reparação individual, não só flag e silêncio.
- **Rollback:** desligar novos checkouts/CTAs/concessões conforme domínio; Core acessível com cobrança OFF; preservar grants, promoções prometidas, créditos e assinaturas já existentes. Não cancelar Play subscriptions automaticamente. RTDN/ACK/reconcile/defer incerto/suporte/estorno/liquidação de obrigações continuam. Repetir reversão financeira cegamente é proibido.

O cronograma 0–30/31–60/61–90 do PRD é orientação de desenvolvimento/observação, não prazo garantido nem autorização automática. Primeira renovação trimestral pode exigir mais de 90 dias. Não estimar LTV pago ou extrapolar retenção gratuita para conversão paga.

## Dúvidas e bloqueadores restantes

Após S01, **OQ-08 está resolvida; 11 OPEN_QUESTION e 2 ASSUMPTION_REQUIRED** continuam sem resposta/evidência. Não foi feita validação de consoles/APIs/políticas externas ou da conta Google; esses são trabalhos futuros de S03/S21/AR-01. A referência local do paywall cobre identidade visual, mas não substitui decisão de layout/copy dos estados trial/trimestral/erro. Próximas stories perdem a dependência documental S01, mas mantêm demais dependências e bloqueios; nenhuma foi executada automaticamente.

Mais relevantes para iniciar: ambientes/fornecedor Auth (AR-01/OQ-01), contrato/dedupe/nomenclatura (OQ-08), evidência/política de legado e cutoff (OQ-02), fallback offline/segurança (OQ-03/10), primeiro dinheiro comprovável (OQ-04), catálogo/troca/defer (OQ-05). Antes de incentivos: janela de void/antifraude/resgate/payout (OQ-06) e utilidade Pro para legado (OQ-11). Política de dados/owners (OQ-07), baseline/gates (OQ-09), variantes visuais/P2 (OQ-12) e fontes de relatório (AR-02) afetam as stories nomeadas, não justificam inventar escopo.

## Verificação documental

Verificação realizada: 12 epics/52 stories com campos obrigatórios; cobertura dos **33 IDs funcionais e 10 não funcionais**; reconciliação das ST-01–36; grafo acíclico/dependências anteriores nas fases; 52 stories presentes uma vez no YAML. Profundidades lógicas verificadas: S16=8, S27=12, S49=15. Referência visual e arquivos locais citados confirmados. `git diff --check` sem erros; arquivos novos também conferidos para whitespace/estrutura/links. Os 15 níveis do plano são topológicos, sem estimativa temporal. Build/testes Android e validação visual de UI implementada são **futuros**, pertencentes às stories; nesta entrega apenas a imagem de referência foi observada.

Arquivos criados: `docs/product/BACKLOG.md` e `docs/product/EXECUTION-PLAN.md`. Nenhuma alteração de código, deploy, Console, conta Google/Figma, billing, flag ou versionamento. Alterações locais anteriores do workspace permanecem preservadas.

### Registro posterior — S01 concluída, 2026-10-09

Criados os sete artefatos de `docs/contracts/`, conforme [README](../contracts/README.md). OQ-08 resolvida por D01–D12; contrato documental v1.0.0 congelado. [Validação registrada](../contracts/test-matrix.md): duas raízes/34 definições, OpenAPI 3.1/13 paths, 49 fixtures estruturais/HTTP e 23 oráculos normativos passaram; 30 eventos canônicos e 33 FR/10 NFR rastreáveis. Nenhuma validação Android/ambiente/Play/financeira real foi declarada. Permanecem 11 OQs e 2 ARs, listadas no README. S01 deixa de bloquear contratos das próximas stories, mas não elimina seus ambientes/políticas/dependências nem autoriza avanço automático. Execução encerrada na S01.

## Estado S02/S04 — 2026-10-13

Ambas **PARTIAL** pelos critérios residuais. Continuação: isolamento A/B no D1 real remoto via handler local/chaves efêmeras, fixtures sem direitos e cleanup comprovados; cinco novos testes de guard/handler e typecheck aprovados. Proteção em memória ativada DEV, não limite global. CPU nova versão N=2: P50 1,943ms, P90 14,163ms, zero erros; margem estável ainda HOLD. OAuth principal SHA-1 Play autorizado cadastrado; JSON/recurso Web client candidatos validados sem sobrescrever original. Login físico/267 testes Android/22 backend anteriores não repetidos. Decisões operacionais de ambiente aberto e testes adicionais S04 (distribuição, segundo aparelho, expiração/linking) detalhados no [relatório corrente](S02-S04-INTEGRATION.md). S05 pode ser desenvolvida isoladamente com esta arquitetura, mas concessão/rollout dependem de OQ-02 e fechamento operacional; nenhuma próxima story foi iniciada/autorizada nesta execução.
