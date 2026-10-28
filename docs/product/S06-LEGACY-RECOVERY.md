# S06 — Recuperação, revisão e emissão segura de legado

**Atualização vigente — 2026-10-19: DONE no piloto operacional DEV controlado; produção HOLD.** Operador Google real autorizado, admin exclusivo no inventário, console privado com binding remoto, migrations 0002–0008 e deploy exclusivamente DEV aprovados. Fluxo real: protocolo Android → complementação → duas referências sintéticas auxiliares → avaliação assistida aprovada ainda UNDER_REVIEW → emissão única autorizada → grant persistido → entitlement 200 → V3 → logout/relogin → reinstalação DEV e recuperação. Segundo titular recusado como operador, protocolo rejeitado sem grant; D1 final 1 GRANTED/1 REJECTED/1 grant/1 emissão/0 históricos verificados. OQ-07 aprovada para o piloto e descarte testado somente em D1 local sintético. [Resultados, testes, CPU e limites](S06-S09-DEV-OPERATIONAL-RESULTS.md), [runbook executável](S06-OPERATOR-RUNBOOK.md). As pendências genéricas de operador/intake/ensaio abaixo são histórico de 14/10, resolvidas no escopo DEV; rollout, fundamentos de privacidade e capacidade não foram aprovados por esse marco.

Atualização: **2026-10-14**, branch `growth`, base `226fe22f`, trabalho anterior preservado. **S06 PARTIAL:** emissão permanente e recuperação comprovadas no D1 local; operação com identidade Firebase real do proprietário e política OQ-07 ainda pendentes. ENT-002/004, PRD §§3.2/10/11 e INV-01/02/04/14. Contratos S01 intactos.

## Continuação operacional — diagnóstico e OQ-07

**Não reimplementamos o emissor nem repetimos a suite de 85 testes.** Preparados [runbook do operador](S06-OPERATOR-RUNBOOK.md), [proposta OQ-07 para aprovação](OQ-07-DATA-POLICY-PROPOSAL.md) e ferramentas de leitura/preflight. **8 testes da rodada específica PASS**: sete novos de readiness e uma integração existente ampliada para o novo preflight + fluxo completo sintético. Proprietário sintético GO, controle ordinário GO, outro admin sintético HOLD; emissão/recuperação local permanecem válidas. Não são credenciais Google reais.

Novas evidências de leitura: Firebase mostra duas identidades Google, incluindo candidato legítimo do proprietário; a UI não revela custom claims. Console Cloud confirma service account ativa/sem chave, com papéis amplos de Authentication Admin/SDK Agent/Token Creator; proprietário possui IAM Owner. Esses papéis não equivalem à claim do operador. No computador, CLI Google/Firebase, ADC e Admin SDK não foram encontrados no PATH/locais examinados. Owner UID não foi gravado no repo nem configurado artificialmente; inventário global de admin de usuários reais não foi executado sem autoridade local.

Inventário D1 do console: **12 tabelas de negócio, todas vazias**. Auditoria de metadados confirmou guardas que impedem apagar/redigir PII em grants/histórico/revisões. OQ-07 propõe finalidade/prazos por classe e uma operação de privacidade futura separada; **a capacidade legítima de exclusão ainda não existe**. Sem aprovação/implementação S14/S15, não declarar conformidade nem iniciar coleta real de revisão. Não foi executada exclusão, nem alterado trigger/contrato.

Preflight sem configuração: HOLD; token inválido: AUTH_REQUIRED sem D1; TypeScript e sintaxe PASS. Nenhuma chave, ADC, cliente OAuth, role, IAM, usuário ou endpoint administrativo foi criado/alterado. Menor caminho sem chave e permissões mínimas estão documentados; configuração real depende das aprovações específicas do runbook. OQ-07 continua **OPEN/PROPOSTA**, não decisão aprovada. **S06 permanece PARTIAL** por operador real/ensaio Google e decisões/capacidade de privacidade pendentes. Resultados anteriores abaixo são históricos.

## Resultado objetivo

**Sim: uma aprovação assistida vigente agora pode resultar em LegacyGrant permanente e recuperável.** `decide(APPROVE)` registra a avaliação humana; o comando privado `issue` consome essa decisão exata e persiste, atomicamente, fonte S05 + auditoria de emissão + resolução GRANTED. Aprovar sozinho ainda deixa UNDER_REVIEW. GRANTED só é devolvido após releitura e validação do grant persistido e de sua conclusão. Não há projeção de entitlement S07, Premium ou prazo provisório.

O fluxo DEV **local** foi executado com identidades sintéticas assinadas e verificadas, sem credenciais reais: solicitação HTTP autenticada → referências contextualizadas → aprovação ASSISTED_ASSESSMENT → emissão → encerramento/reabertura do runtime D1 → nova sessão do mesmo UID → recuperação do mesmo grant. Isso comprova persistência e integração local; **não comprova um administrador Google real**, nem operação remota.

Quem perdeu o aparelho e nunca teve prova histórica individual pode recuperar por **avaliação humana do proprietário**: conferir titularidade, contextualizar indícios reais (por exemplo, suporte original e continuidade do canal histórico), justificar a conclusão de uso <=3.5/12 e vincular à identidade autenticada do próprio usuário. Avaliação humana é uma autoridade distinta de VERIFIABLE_HISTORY; não inventamos histórico criptográfico. Se não houver indícios suficientes ou a conta estiver em conflito, o caso permanece pendente para complementação/recuperação de identidade; nenhuma falha bloqueia o Core atualmente gratuito.

## Decisões operacionais aprovadas

- Único aprovador: proprietário do CalcMot, identificado pelo UID configurado, claim `admin=true` assinada e `auth_time` de no máximo cinco minutos. Email não dá autoridade; renovar token não substitui reautenticação.
- Avaliação assistida pode fundamentar concessão permanente com ownership confirmado, justificativa, referências contextualizadas, contradições resolvidas e conclusão explícita de uso legado.
- Elegibilidade permanece **uso até 3.5/versionCode 12**, incluindo tardios; cadastro na migração, UID, versão atual ou timestamps não ampliam a classe.
- Avaliação humana não escreve `legacy_historical_use` ou `legacy_evidence` como prova verificada. Seu hash identifica a decisão/protocolo, **não certifica a história de instalação**.
- Sem Premium, provisório de sete dias, transferências automáticas, alteração ou revogação de grants anteriores. `provisionalUntil=null`.
- OQ-07 permanece aberta: imutabilidade técnica da auditoria não aprova retenção indefinida de dados/evidências. Não ativar operação real até definir acesso, retenção e exclusão compatíveis com preservação do direito.

## Fontes históricas investigadas de fato

Foi confirmada a conta Gmail conectada do proprietário e realizada pesquisa somente de leitura, direcionada a feedback do CalcMot. Não foram enviados emails, alterados labels ou copiados nomes/telefones/corpos para o repositório.

| Fonte | O que foi efetivamente encontrado | Classificação e limites |
|---|---|---|
| Suporte original | Duas conversas antigas de feedback/problema, de agosto de 2026, na pesquisa direcionada. Uma contém **CalcMot 3.3 (10)**; a outra relata uso sem versão explícita | Fontes reais úteis para os respectivos titulares. Corpo de email é editável; conferir original, recebimento, contexto e posse do canal. Não relacionar pessoas distintas nem fundir suas evidências. Não é prova criptográfica de uso/Core/UID atual. |
| Caminho de envio do app | `FeedbackEmailLauncher` inclui opcionalmente versão/código e informações técnicas; `AppLegal.kt` já aponta suporte para o proprietário | Confirma o canal e a plausibilidade do formato, não veracidade automática do corpo ou envio por todo usuário. |
| Metadados/preferências/Room | Estruturas já inventariadas em S05 existem; nenhum dispositivo de usuário foi extraído nesta story | Auxiliares. Não requisitar vídeos, corridas, conteúdo financeiro, dados pessoais sensíveis ou novas permissões para recovery. |
| Firebase/Analytics/diagnósticos | Mensagens de diagnóstico e login aparecem na busca geral, mas não foram usadas como prova individual de legado | Uso agregado/alerta ou identidade atual não vinculam automaticamente um direito histórico. Não foi descoberto um inventário anterior por UID. |
| Grant/histórico privado | Tabelas S05 e validações locais disponíveis; nenhum grant real criado | A existência da estrutura não prova registros reais. Fixtures locais permanecem separadas de evidência de usuários. |

Busca direcionada: assunto CalcMot + feedback/problema/sugestão/dúvida/elogio, excluindo remetente do proprietário e notificações Firebase. O resultado não é censo de toda a caixa ou de todos os usuários. As mensagens não foram importadas como claims/grants. Referências privadas permanecem na conta original; o repositório registra somente a conclusão minimizada da investigação.

## Implementação e autoridade

| Componente | Garantia |
|---|---|
| [Migration 0006](../../backend/migrations/0006_legacy_review_issuance.sql) | Nova trilha privada `legacy_review_issuances`. Insert único + triggers em uma transação: recibo, grant permanente, claim GRANTED. Falha em qualquer etapa reverte tudo. Nada é semeado. |
| [Serviço de revisão](../../backend/src/legacy-review.ts) | `authorizePolicy` registra explicitamente a regra já aprovada, somente pelo proprietário. `issue` exige aprovação da revisão atual, ator autorizado, titularidade confirmada e política compatível. Releitura valida schema S01, UID e JSON da fonte versus recibo. |
| Guardas S05 | Migration nova substitui apenas guardas de emissão, mantendo integralmente a via verificada <=12 e acrescentando a via auditada S06. Grants prévios permanecem byte a byte; não editamos migrations 0001–0005. |
| Trilha de autoridade | `legacyEvidenceType=ASSISTED_ASSESSMENT` ou `VERIFIABLE_HISTORY`, `decisionActor=UID` autorizado, hash vinculado a projeto/UID/protocolo/decisão/revisão. Wire S01 permite esses strings; nenhum campo foi adicionado. |
| Concorrência/replay | Revisão vigente, unicidade de protocolo/decisão/projeto+UID e compare-and-swap. Nova evidência, complementação ou outra decisão invalida aprovação anterior. Mesmo comando retorna fonte original; outra decisão/corpo conflita. |
| Console privado | [CLI](../../backend/scripts/legacy-review-local.mjs) verifica token Firebase real por JWKS Google antes de abrir D1. Sem opção remota, sem token em argv/arquivo/log, sem credenciais no Android. `/admin/*` permanece 403. |
| Recuperação | Mesma identidade Firebase recupera fonte persistida sem reavaliar versão, aparelho ou histórico. Outro UID não acessa/recebe esse direito. Fonte recebida por outra via é preservada, nunca sobrescrita pelo emissor. |

A transação é uma instrução INSERT com triggers SQLite; não há grant emitido em uma chamada independente seguida de conclusão vulnerável a falha. Guardas menores foram necessárias para respeitar a profundidade de expressão do D1; os testes de rollback executam o motor real local. Base técnica: [SQL compatível do D1](https://developers.cloudflare.com/d1/sql-api/sql-statements/) e [Workers Binding API](https://developers.cloudflare.com/d1/worker-api/d1-database/).

A matriz de revisão já implementada é conservadora: HISTORY exige referência privada verificável do mesmo titular; ASSISTED_ASSESSMENT exige duas categorias/origens distintas contextualizadas (suporte/continuidade + instalação/uso/continuidade), além de julgamento humano explícito. Labels distintos não comprovam independência: o proprietário confere os originais. Relato isolado/cópias correlacionadas exigem complementação. Caso fora dessa matriz não recebe prova fictícia nem concessão automática. A autorização de avaliação humana está aprovada; não é uma aprovação automática por contagem de indícios.

## Procedimento operacional preparado

1. **Entrada:** canal Ajuda/Feedback existente do proprietário. Core permanece disponível sem conta/rede/revisão. Não pedir token, senha, código Google, vídeos ou histórico financeiro no suporte.
2. **Protocolo:** o próprio titular autenticado chama POST `/v1/legacy/claim`, com body S01 e Idempotency-Key. Google vincula recuperação, não decide elegibilidade. Conta perdida precisa primeiro de recuperação de ownership; não abrir pedido de terceiro no UID do operador nem mesclar por email. UI S09 está fora desta entrega.
3. **Preparar operação local:** proprietário com UID explicitamente configurado, admin legítimo e reautenticação recente; migrations aplicadas. `policy` registra a regra fixa aprovada. Política é privada e imutável; conflito de aprovador/baseline falha fechado. Registrar antes de aprovar casos; aprovação anterior à política precisa de nova decisão vigente.
4. **Analisar:** `inspect` deriva UID do protocolo; `evidence` registra somente referências opacas e classificação HISTORY/AUXILIARY/DECLARATION. Originais permanecem no canal autorizado. REQUEST_MORE complementa; OWNERSHIP_CONFLICT mantém pendente; REJECT exige justificativa e preserva grants. Recurso usa REQUEST_MORE auditado, sem apagar rejeição anterior.
5. **Aprovar:** `decide` com revisão atual, evidências selecionadas, justificativa e ownership/contradições/uso concluídos. O evento APPROVE identifica a base humana ou verificável; ainda não prometer grant emitido.
6. **Emitir:** `issue` com `approvalEventId`, **revisão devolvida pela aprovação** e key. O mesmo proprietário reautenticado deve executar. Nova revisão exige nova aprovação. Sucesso contém GRANTED e fonte validada; timeout/falha deve ser reconsultado/reexecutado com mesma intenção, nunca criar outro grant ou inserir SQL para contornar guardas.
7. **Recuperar:** titular reautentica no mesmo UID e reconsulta protocolo/fonte. Permanência independe do aparelho. Sem snapshot, `/v1/me/entitlement` continua 503 RETRY_LATER até S07; o grant S05 existe independentemente desse endpoint. Nenhuma restrição foi ligada ao Core.

### Comandos locais, sem recursos externos

Na pasta `backend`:

```powershell
$env:WRANGLER_SEND_METRICS='false'
$env:WRANGLER_LOG_PATH="$PWD/../.tmp/s06-issuance-local/wrangler.log"
node node_modules/wrangler/bin/wrangler.js d1 migrations apply DB --local --persist-to ../.tmp/s06-issuance-local
node node_modules/wrangler/bin/wrangler.js dev --local --ip 127.0.0.1 --persist-to ../.tmp/s06-issuance-local
```

Banco fixo do console: `.tmp/s06-issuance-local/v3/d1`, UUID local zero. Banco antigo de ensaios preservado; não copiar fixtures para DEV remoto. Parar Wrangler antes de usar o console no mesmo SQLite. Configuração local `FIREBASE_PROJECT_ID=calcmot`, `LEGACY_OWNER_UID` real autorizado; nunca salvar token no repo. Exemplo de entrada protegida:

```powershell
$secretInput = Read-Host 'Firebase ID Token do proprietário reautenticado' -AsSecureString
$operatorToken = [Net.NetworkCredential]::new('', $secretInput).Password
try {
  @{token=$operatorToken;action='issue';claimId='PROTOCOLO';revision=3;key='EMISSAO_UNICA';
    input=@{approvalEventId='EVENTO_APPROVE_VIGENTE'}} |
    ConvertTo-Json -Compress | node scripts/legacy-review-local.mjs
} finally { $operatorToken=$null; $secretInput.Dispose() }
```

`3` é exemplo: usar a revisão real de `inspect`/aprovação. Comando `policy` usa somente token/action; registra a regra fixa sem grant. Demais comandos: `open`/`own` para titular; `inspect`/`evidence`/`decide`/`issue` para proprietário. `issue` não aceita UID via body de concessão, datas, features ou política escolhida pelo app. Target UID do console é derivado privadamente do protocolo ou validado contra ele. Saída privada exige acesso protegido; não encaminhar JSON de operador para Analytics/log público.

## Validações reais desta continuação

- **85 testes backend PASS, 1 remoto opt-in SKIPPED**; destes, **32 S06 PASS**, incluindo 12 novos casos de emissão e uma integração persistida. TypeScript PASS. O remoto não foi executado; não é evidência de operação Cloudflare/Firebase real.
- Bundle Wrangler **dry-run PASS: 283,74 KiB / gzip 40,54 KiB**, sem publicação. Smoke do console com token inválido: **AUTH_REQUIRED** antes de acesso ao banco. O bundler local precisou ler caminhos fora do sandbox; nenhuma autorização externa ou recurso foi criado.
- Hashes **7/7 contratos S01 e 753/753 arquivos Android existentes preservados**; `git diff --check` PASS. Não há novo endpoint de emissão nem configuração de owner/admin no Worker.
- Casos novos: política explícita, emissão assistida/verificável distinta, falso operador/decisão, rejeição/complementação/conflito, aprovação desatualizada, recurso, cinco emissores concorrentes, disputa com rejeição, replay e tentativa SQL de autoatribuição.
- Falha induzida na conclusão **após INSERT do grant**: fonte nova e recibo revertidos; decisão segue pendente/vigente; grants anteriores idênticos; retry seguro emite uma vez. Não há GRANTED sem fonte.
- [Integração persistida](../../backend/tests/legacy-issuance.integration.test.ts): grant S05 anterior preservado durante migration 0006; fluxo canônico autenticado, avaliação assistida e emissão; runtime reiniciado e nova sessão recupera a mesma fonte; outro UID recusado. Fixtures: 2 grants (um anterior), 1 emissão, **somente 1 histórico anterior**, 0 snapshots. Nenhum histórico foi criado pela avaliação humana.
- Migrations 0001–0006 aplicadas pelo Wrangler exclusivamente no D1 local limpo; repetição sem migrations pendentes. Banco do console **claims/issuances/grants/snapshots=0/0/0/0**. Fixtures ficam exclusivamente nos testes locais isolados, nunca em banco remoto operacional.
- Nenhum teste físico Android repetido: não houve mudança Android, pipeline ou consumo do serviço de revisão no Core. Nenhum deploy, push, upgrade, despesa ou grant real.

## O que ainda impede operação real e DONE

| Pendência | Evidência/ação necessária |
|---|---|
| **Operador real** | `LEGACY_OWNER_UID` não está configurado no processo nem nas configs Wrangler examinadas. Não há evidência de admin legítimo do proprietário para este serviço. Definir ambiente/UID autorizado e autoridade Firebase que emita claim admin de forma segura; não atribuir privilégios a usuário de produção nesta sessão. Depois reautenticar e testar console com assinatura Google real. Fixtures RSA locais não substituem isso. |
| **OQ-07** | Aprovar retenção/acesso/exclusão de referências e auditoria, distinguindo permanência do direito de retenção de dados pessoais. Não habilitar política de retenção infinita ou purge unilateral. |
| **Ativação/ensaio real** | Escolher ambiente de ensaio autorizado, registrar política pelo operador legítimo e executar um caso contextualizado de teste com titular real. Nenhuma migration/config/deploy remoto foi realizada ou presumida. |
| **Ownership irrecuperável** | Sem controle da identidade original ou conflito de contas, permanece atendimento pelo proprietário, sem transferência automática ou concessão pela conta do suporte. Se também não houver indícios contextualizáveis, a tecnologia não prova o passado; pedir complementação e registrar decisão excepcional necessária. Core atual continua disponível. |

Testes adicionais de segundo aparelho e troca de identidade real dependem do ambiente acima; UX publicada pertence a S09, projeção a S07. Não iniciar essas stories para disfarçar fechamento. Revogação instantânea de tokens não foi adicionada: owner explícito + reautenticação recente reduzem a janela; procedimento de revogação operacional deve ser conferido antes de operação sensível.

**S06 PARTIAL.** O impedimento técnico de emissão assistida foi resolvido e a recuperação permanente funciona localmente. Falta comprovar autoridade real/ensaio operacional e resolver OQ-07. Decisão comercial e autorização de avaliação humana não são mais pendências. Provisório de sete dias permanece expressamente desativado.

Arquivos desta continuação: novos `0006_legacy_review_issuance.sql` e `legacy-issuance.integration.test.ts`; alterados `legacy-review.ts`, `legacy-review-local.mjs`, `legacy-review.test.ts`, README backend, este relatório, S05/OQ-02, BACKLOG e EXECUTION-PLAN. Sem alterações nos sete contratos ou no Android.
