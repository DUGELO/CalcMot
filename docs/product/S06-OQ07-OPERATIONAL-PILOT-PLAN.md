# OQ-07 / S06 — decisões e piloto operacional proposto

**Plano histórico executado após autorizações específicas:** consultar [resultado operacional de 19/10](S06-S09-DEV-OPERATIONAL-RESULTS.md), [política do piloto aprovada](OQ-07-DEV-APPROVED-POLICY.md) e [runbook corrente](S06-OPERATOR-RUNBOOK.md). Houve deploy/migrations exclusivamente DEV e grant sintético único autorizado; os diagnósticos abaixo descrevem o estado anterior, não pendências genéricas ainda abertas.

Data: **2026-10-16**. Branch `growth`. **PROPOSTA, não autorização de implementação ou operação.** S06 e S09 permanecem PARTIAL. Auditoria S09 preservada, inclusive limites de sessão e I/O. Nesta rodada houve leitura de código/documentos e documentação oficial; nenhum teste operacional, consulta ao DEV remoto, mudança de configuração, deploy, grant ou exclusão foi executado.

Fontes locais: [política aprovada OQ-02](OQ-02-LEGACY-POLICY.md), [S05](S05-LEGACY-FOUNDATION.md), [S06](S06-LEGACY-RECOVERY.md), [runbook do operador](S06-OPERATOR-RUNBOOK.md), [proposta anterior OQ-07](OQ-07-DATA-POLICY-PROPOSAL.md), [S07](S07-ENTITLEMENT-PROJECTION.md), [S02/S04](S02-S04-INTEGRATION.md), BACKLOG S06/S14/S15 e contratos S01. Notas antigas que dizem que emissão/projeção estão ausentes são históricas: a implementação local existe; implantação remota não está comprovada.

## 1. O que já está decidido

- Uso efetivo até 3.5/versionCode 12 dá direito permanente ao Core daquela versão. Login, cadastro durante migração e versão atual não definem elegibilidade.
- Proprietário é o único aprovador. Julgamento assistido contextualizado, titularidade confirmada e justificativa podem fundamentar ASSISTED_ASSESSMENT; não se fabrica VERIFIABLE_HISTORY.
- Aprovação não é emissão. GRANTED exige grant persistido e conclusão auditada; legado nunca concede Premium.
- Sem provisório de sete dias, transferência automática, revogação de grants por revisão ou bloqueio do Core por falha de conta/rede.
- Spark + Workers Free + D1 Free; infraestrutura adicional R$ 0. Sem cobrança ou produção.

## 2. OQ-07: decisões ainda abertas

Os prazos 7/30/90/180 dias e dois anos da proposta anterior **continuam não aprovados**. Não existe expurgo agendado autorizado. A tabela abaixo oferece alternativas; escolher finalidade, gatilho e prazo por categoria é necessário, sem transformar um número em suposta obrigação universal da LGPD.

| Decisão | Alternativas e recomendação para avaliação | Vantagens, riscos e esforço |
|---|---|---|
| D1 — Coleta mínima | **Recomendada:** declaração inicial + protocolo; operador registra classificação, referência opaca e conclusão contextualizada. Originais só no suporte quando necessários. Alternativa: cópias de evidência em repositório dedicado, que não existe hoje. | Referências reduzem exposição e custo; exigem localizar o original no suporte. Duplicação facilita consulta, mas aumenta superfície, armazenamento e obrigações de exclusão. Sem documentos civis, vídeos ou dados financeiros por padrão. |
| D2 — Evidência e suporte | Janela curta após conclusão/recurso, por exemplo **30 dias**, versus **180 dias** anteriormente propostos. Escolher separadamente anexos, narrativa e referência. | Curta minimiza PII, mas reduz material para recurso; longa facilita contestação, porém amplia risco e revisão manual. Ambos têm custo humano de triagem/exclusão; nenhum é prazo legal automaticamente aplicável. |
| D3 — Auditoria mínima | Prazo definido, por exemplo **1 ano ou 2 anos**, com finalidade/base justificadas; separar recibo necessário ao direito da narrativa descartável. Legal hold somente específico e revisável. | Mais tempo auxilia apuração, mas não justifica conservar anexos ou UID para sempre. Necessário avaliar obrigações concretas e revisão de necessidade. |
| D4 — Conta e direito permanente | Manter vínculo mínimo enquanto necessário à conta/recuperação, sem expirar comercialmente o grant. Na exclusão integral: **(a)** retenção residual mínima com fundamento validado e transparência; **(b)** comprovante portátil verificável + recuperação assistida, a implementar. | (a) menor engenharia, mas mantém dado pessoal e pode não atender determinado pedido; não é exceção automática à eliminação. (b) reduz vínculo retido, porém requer desenho antifraude, tratamento de perda/roubo e ownership; não é seguro prometer que já existe. Sem vínculo e sem prova portátil, recuperação automática não pode ser garantida. |
| D5 — Direitos dos titulares | Canal existente de suporte, titularidade proporcional, registro mínimo de execução; aprovar metas internas de triagem/conclusão e tratamento de impedimentos. | Menor custo técnico, maior carga manual. Não pedir token/senha/código. Apagar Firebase isoladamente deixa cópias no D1/suporte. Não anunciar exclusão concluída sem capacidade executável. |
| D6 — Casos abandonados, logs e cópias | Escolher gatilho de encerramento comunicado, retenção de idempotência sem grant, logs sanitizados e descarte de exports. Alternativa preferível a exports: não gerar cópias pessoais. | Inatividade não extingue legado. Remover dedupe sem análise pode permitir replays; backups podem ressuscitar dados. Definir reaplicação de pedidos após restauração e conferir janelas dos provedores. |
| D7 — Governança e transparência | Identificar controlador legal/contato; fundamento por finalidade, operadores/transferências internacionais, resposta a incidente e substituição em indisponibilidade do proprietário. | Proprietário aprovador já definido, mas não substitui identificação jurídica, canal de privacidade e exame das obrigações. Trabalho documental/humano, sem novo serviço pago obrigatório. |

Recomendação: não escolher retenção residual versus comprovante portátil por conveniência técnica. Primeiro validar juridicamente o fundamento e o resultado devido ao titular; então aprovar a operação correspondente. Direito permanente não implica retenção ilimitada de toda evidência. UID, digest estável e referências vinculáveis continuam dados pessoais.

A LGPD exige necessidade, transparência, segurança e atendimento dos direitos; conservação após término precisa de hipótese aplicável. O prazo do art. 19 para declaração completa não é prazo universal de exclusão. Este plano não certifica conformidade nem aprovação jurídica. [LGPD, arts. 6, 7, 15–19 e 46](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm).

**Bloqueio técnico de privacidade:** triggers comerciais imutáveis, JSONs duplicados, FKs e recibos hoje impedem redigir/apagar todas as cópias de forma controlada. Antes de casos pessoais reais de revisão, precisa haver operação específica, autorizada e testada de minimização/exclusão (escopo S14/S15 pertinente), sem remover guardas globalmente nem revogar direito por descarte de anexo. Não está autorizada nesta execução. Consentimento de participante de teste não elimina essa obrigação.

## 3. Auditoria de prontidão S06

| Etapa | Evidência local conferida nesta rodada | Falta para DEV operacional |
|---|---|---|
| Operador | `LegacyReviewService.owner`: UID explícito, `admin` booleano e `auth_time` não futuro, idade <=300s. Validação de JWT é prévia. | Autoridade Admin SDK legítima, UID conferido fora do repo, inventário autorizado de claims e aprovação específica para eventual escrita. IAM Owner e login no Console não provam admin Firebase. |
| Recebimento | `backend/src/index.ts`: POST `/v1/legacy/claim`, token verificado, limite de corpo, idempotência, flag `LEGACY_CLAIMS_ENABLED`; UID vem da sessão. | Conferir versão implantada, binding, schema e flags reais. Template DEV não contém habilitação de claim/projeção e não prova configuração remota. |
| Revisão | `legacy-review.ts`: inspect/evidence/decide/issue, revisões e titularidade, trilha assistida distinta. | `legacy-review-local.mjs` cria Miniflare com persistência fixa local. Não acessa o D1 remoto e não processa automaticamente pedidos do Worker DEV. |
| Superfície administrativa | Worker público rejeita `/admin/*` e `/internal/*`. Nenhuma rota privada de revisão implantada comprovada. | Meio privado de executar o mesmo serviço contra o D1 DEV; não usar SQL manual de grant nem transformar rota pública em console administrativo. |
| Emissão | Serviço e migrations locais até 0007 suportam fonte permanente, auditoria atômica e revisão da projeção. | Inventário remoto de `d1_migrations`/schema/índices/triggers e ensaio real autorizado. Ter 0007 no repo não prova aplicação em DEV. |
| Android | `AccountRoute` injeta `UnavailableLegacyRequests`. `LegacyClaimContract` já prepara USER_DECLARATION/null e valida protocolo; S08 consulta/cacheia entitlement. | Adaptador autenticado de intake somente DEV, idempotência durável e isolamento de respostas por sessão. Não existe POST funcional de recuperação no Android atual. |
| Acompanhamento | Serviço privado tem `getOwn`; S01 expõe POST de claim e GET de entitlement. | Definir retomada contratual: repetir POST com a mesma chave/corpo para obter caso vigente, validando comportamento e persistência da chave; não inventar GET de claim nem exibir subestados administrativos não fornecidos pelo contrato. |
| Reconhecimento do direito | Projetor S07 lê fontes; GET pode retornar legado separado de Pro. | Publicação DEV expressamente autorizada com projeção habilitada e validação Android do grant real de teste. Hoje 503 visto no DEV não comprova emissão/projeção. |
| Reinstalação | Recuperação por mesmo projeto/UID tem testes locais anteriores. | Reinstalar só identityDev e comprovar mesma identidade Firebase, novo fetch e direito persistido no mesmo D1. Troca de UID exige revisão de titularidade, nunca heurística por email. |

## 4. Menor caminho proposto para piloto DEV

### Etapa A — preparação sem casos pessoais novos

1. Aprovar decisões OQ-07 e o escopo de implementação de privacidade necessário; manter intake OFF enquanto não houver procedimento executável para dados reais.
2. Autorizar inspeção remota somente leitura: plano efetivo, conta/banco DEV exatos, deploy ativo, bindings/flags e ledger/schema das migrations. Registrar metadados e contagens, sem exportar PII ou SQL com valores pessoais. Credenciais anteriores não autorizam uma nova concessão ou deploy.
3. Separar ensaio sintético de operação pessoal. Ensaiar migrations faltantes e todas as mudanças sobre cópia **sintética local**, sem reexecutar indiscriminadamente suítes antigas nem alterar migrations existentes.

### Etapa B — autoridade real, após aprovação específica

4. Preferir Admin SDK local com ADC por impersonação de conta-alvo dedicada e sem chave persistente, após validar compatibilidade e permissões. Leitura: `firebaseauth.users.get`; escrita eventual de claim: `firebaseauth.users.update`, não necessária durante diagnóstico. Impersonação: `iam.serviceAccounts.getAccessToken` no alvo aprovado, sem papel amplo no projeto. Não presumir que a service account atual, ampla, é o alvo adequado.
5. Autorizar separadamente qualquer criação de identidade administrativa/IAM, instalação local, OAuth e atribuição de claim ao UID real conferido. Preservar demais claims; conferir usuários ordinários e definir retirada do privilégio após piloto. Não existe permissão Firebase para editar apenas a claim de um único usuário: restrição depende também do procedimento e ferramenta.
6. Reautenticar proprietário para ID Token Firebase recente, confirmar assinatura/issuer/audience/expiração, owner e `auth_time` <=300s no preflight existente. Obter token por mecanismo local protegido, nunca por chat, suporte, argv ou log. Refresh não substitui reautenticação. Não reutilizar token de operador como token do solicitante.

A documentação alerta que credencial de usuário do OAuth client padrão do gcloud não é aceita pelo Firebase Auth Admin; alternativa é cliente OAuth Desktop próprio, com aprovação. ADC/impersonação precisa de ensaio efetivo antes de afirmar funcionamento e pode manter refresh token local, embora não seja chave privada de service account. [Admin setup](https://firebase.google.com/docs/admin/setup), [permissões Firebase](https://docs.cloud.google.com/iam/docs/roles-permissions/firebaseauth).

### Etapa C — ponte privada e Android, implementação futura autorizada

7. **Caminho preferido para avaliação:** adaptar somente a entrada do console local para binding remoto do D1 DEV via API oficial Wrangler (`getPlatformProxy`/remote bindings), preservando `LegacyReviewService`, validação Firebase e transações `DB.batch`. Restringir alvo por allowlist fixa DEV, sem modo produção, sem servidor administrativo exposto, sem escrita SQL livre. Duas autoridades distintas: OAuth Cloudflare para acessar binding e Firebase proprietário para autorizar a decisão. Falha de qualquer uma impede operação.
8. Fazer prova técnica sintética desse binding na versão Wrangler instalada e confirmar custo e isolamento antes de aprovar implementação final. Documentação da plataforma não prova compatibilidade deste console. Se exigir plano pago, exposição pública administrativa ou perder atomicidade, parar e trazer alternativa; não substituir por export/import, edição manual de grant ou novo endpoint público.
9. Implementar privacidade aprovada, ensaiar dry-run/execução/retry/falha sobre dados sintéticos e conferir todas as cópias. Autorizar somente então migrations faltantes e publicação **DEV** do Worker de usuário, sem domínio de produção. Não publicar console administrativo. Confirmar claims/projeção explicitamente, defaults de produção preservados.
10. Conectar `LegacyRequests` na variante identityDev ao POST congelado usando sessão S04 e infraestrutura existente; manter produção indisponível. Persistir chave/corpo por identidade para retries e retomada; cancelar/descartar respostas tardias. Não coletar evidência sensível pelo app. Consulta S08 continua independente do pipeline de ofertas.

[Wrangler API](https://developers.cloudflare.com/workers/wrangler/api/) e [desenvolvimento com bindings remotos](https://developers.cloudflare.com/workers/local-development/): operações remotas consomem cotas do recurso real; local não significa sem efeito remoto. Nenhum binding remoto foi aberto nesta execução.

### Etapa D — caso controlado, somente com autorização de concessão

11. Aprovar participante/UID de teste, banco DEV e natureza do grant. Caso sintético deve estar identificado, sem direito comercial real. Caso histórico real requer análise contextualizada e autorização específica, não pode ser usado como fixture descartável. Não conceder legado ao proprietário só por ser operador.
12. Titular solicita no Android → protocolo confirmado → suporte mínimo pelo canal existente → operador confere titularidade e indícios → registra referências/contexto → complementa ou decide → aprovação vigente → emissão privada atômica → releitura do grant → GET S07 → S08/UI reconhecem Core permanente, Pro falso.
13. Reiniciar runtime e reinstalar somente DEV: login da mesma identidade recupera fonte e entitlement; não reapresenta grant como nova concessão. Se identidade original for perdida, atendimento assistido sem transferência automática. Usuário com evidência insuficiente recebe complementação/decisão justificada, nunca promessa de aprovação; Core atual permanece disponível.

## 5. Custo e capacidade

**Previsão de infraestrutura adicional: R$ 0**, condicionada à permanência nos planos gratuitos e cotas compartilhadas. Trabalho do operador, revisão jurídica e manutenção têm custo humano não quantificado; não há contratação autorizada. Não é promessa de escala ilimitada.

| Serviço | Limite/documentação consultada em 16/10/2026 | Conduta do piloto |
|---|---|---|
| Workers Free | 100 mil requests/dia por conta, CPU nominal 10ms/request | Medir JWT/JWKS frio e quente, intake, GET e falhas. S02 registrou apenas N=2, P50 1,943ms e cauda 14,163ms sem erro: não demonstra margem estável para fluxo ampliado. Parar ou diminuir carga, nunca upgrade automático. |
| D1 Free | 5 milhões de linhas lidas/dia, 100 mil escritas/dia, 5GB total e 500MB/banco | Índices e batches; medir `rows_read`/`rows_written`, incluindo índices e administração. Ao esgotar cotas, queries falham; preservar Core e retry seguro. |
| Firebase Spark | Google Auth no serviço padrão sem custo; limites dependem do produto/configuração efetivos | Não habilitar Identity Platform/SMS/Blaze. Não importar cotas de Identity Platform como se provassem a configuração atual. Verificar rate limits e disponibilidade do projeto antes do piloto. |
| Console local/suporte | Máquina e canal existentes | Sem hospedagem de painel, armazenamento adicional de anexos ou serviço pago. Restringir credenciais locais e evitar exports. |

Fontes: [Workers limites](https://developers.cloudflare.com/workers/platform/limits/), [D1 preços/cotas](https://developers.cloudflare.com/d1/platform/pricing/), [D1 limites](https://developers.cloudflare.com/d1/platform/limits/), [Firebase preços](https://firebase.google.com/pricing), [Auth limites](https://firebase.google.com/docs/auth/limits). Limites serão reconferidos antes de ativação; não foram medidas novas CPU/cotas nesta rodada.

## 6. Testes ponta a ponta e critérios de saída

Executar somente após autorização da etapa correspondente. Resultados antigos locais são pré-condições, não evidência nova de DEV operacional.

| Teste | Evidência exigida |
|---|---|
| Proprietário real / usuário comum / outro admin | Owner reautenticado aceito; outros e auth_time antigo/futuro recusados, sem mutação. Conferência de claims por autoridade legítima, sem expor tokens. |
| Intake e isolamento | Android recebe protocolo S01; replay mesma chave/corpo devolve caso vigente; payload conflitante recusado; outro UID/projeto não consulta ou decide esse caso. |
| Revisão | Evidência verificável e assistida distintas; relato isolado não concede; complementação, rejeição e recurso auditáveis; aprovação ainda não é GRANTED. |
| Emissão | Duas execuções/concorrência/replay emitem um grant; revisão obsoleta falha; falha transacional não deixa grant/recibo parcialmente confirmado. Grants anteriores preservados. |
| GET e Android | Mesmo UID vê `legacyCore=true`, `legacy.state=GRANTED`, sem Pro fictício; outro UID não herda; 401/503/offline não viram revogação do Core. |
| Recuperação | Reinício de runtime, logout/login e reinstalação DEV recuperam mesma fonte; protocolo e revisão coerentes, sem criar segundo grant. |
| Privacidade | Inventário por titular, dry-run e execução autorizada em dados sintéticos, todas as cópias/FKs/JSONs tratadas, retry seguro e direito permanente preservado conforme opção aprovada; restauração não ressuscita dados removidos. |
| Custo/rollback | Múltiplas amostras frias/quentes, CPU/erros/consumo de linhas registrados; intake pode ser desligado sem apagar grants ou prejudicar leitura de direitos existentes/Core. |

**GO piloto operacional** somente quando: OQ-07 aprovada e executável para os dados coletados; operador legítimo comprovado; schema/deploy DEV inventariados; ponte privada validada; Android envia pedido e reconhece emissão; caso autorizado completo recupera após reinstalação; controles negativos, atomicidade e cotas comprovados; proprietário sabe complementar/rejeitar/tratar conflito e pedidos de privacidade. GO piloto não é GO produção nem conclusão global de S14/S15.

**Estado atual: HOLD para casos pessoais reais; GO somente para preparação e ensaios sintéticos que venham a ser autorizados. S06/S09 PARTIAL.** Nenhuma aprovação comercial nova foi inferida. Próxima autorização deve separar: política OQ-07, implementação de privacidade/ponte/Android DEV, autoridade/IAM/claim, deploy DEV e concessão controlada. Aprovar este plano não concede automaticamente todos esses poderes.
