# S05 — Fundação de direitos legados

Data: 2026-10-13. Branch `growth`, base local `226fe22f`. **PARTIAL**: fundação implementada/testável localmente; elegibilidade OQ-02 aprovada por uso de versão <=3.5/12, comprovação/operação pendentes e concessões reais desligadas. Requisitos ENT-001, PRD §§3.2/10/10.1, INV-01/02 dos contratos S01. Sem avanço para S06/S07/S09.

## Resultado e fronteira

**Continuação S06, 2026-10-14:** emissor privado de revisão vigente já integra esta fonte. A migration 0006 preserva a via histórica S05 e acrescenta autoridade ASSISTED_ASSESSMENT separada, sem inserção fictícia em histórico. Grant, auditoria e claim GRANTED são atômicos e recuperáveis localmente; não há Premium/snapshot nem grant real. [Resultado atual](S06-LEGACY-RECOVERY.md). O relato original abaixo descreve a fundação entregue antes dessa integração; operação real permanece PARTIAL.

- Fonte persistente `LegacyGrant` validada pelo schema S01, por projeto + UID autenticado, imutável, sem prazo comercial e separada de Premium.
- Evidência auxiliar não se torna verificada por timestamp, versão, hash ou campo enviado pelo cliente. Concessão exige **evidência verificada e política explicitamente aprovada no armazenamento privado**. Não há defaults de baseline, prazo de revisão ou cutoff.
- Sem política aprovada: `POLICY_REQUIRED`. Conta sem Google vinculado: `LINK_REQUIRED`. Prova de outro titular: `OWNERSHIP_REVIEW_REQUIRED`. São resultados internos, **não novos códigos HTTP/contratos públicos**.
- Intenção de recuperação persistida e deduplicada, sem transferência de UID, decisão de claim ou acesso provisório. Operação de revisão é S06 e continua ausente.
- Nenhuma nova rota pública/admin, import do serviço no Worker, snapshot ou alteração de direitos existentes. GET permanece 503 quando não há snapshot; S07 projetará fontes depois. Não gerar snapshot legado sem a story de projeção.
- Preparação Android somente de leitura local, sem Firebase/rede e sem consumidor no app atual. Não existe novo gate de acesso ao Core.

## Evidências encontradas e limites reais

Inspeção do código/arquivos, sem coletar dados de usuários ou consultar novos recursos externos. Presença de estrutura no código não comprova existência de registros em todo aparelho.

| Categoria | Evidência encontrada | Confiança / recuperação legítima |
|---|---|---|
| Instalação local | `AppSettings.isOnboardingCompleted` já compara `PackageInfo.lastUpdateTime > firstInstallTime`; versão atual 11/3.4 no Gradle | Metadados do pacote instalado, não histórico individual assinado nem prova vinculada ao titular. Atualização de uma instalação nova também satisfaz a comparação. Versão atual não é versão de primeiro uso. |
| Preferências locais | `calcmot_settings/onboarding_completed`, configuração de veículo, metas e overlay | Indícios auxiliares; onboarding também pode ser inferido e gravado pela função existente. O novo leitor lê a chave armazenada diretamente e não chama essa função mutável. Preferências podem ser alteradas/restauradas; não autorizam grant. |
| Uso local informado pelo usuário | `FinanceRepository`, `calcmot_finance/entries_json`, com `dateMillis` fornecido/local | Registro manual, editável e não autoritativo. Não enviar valores, descrições ou histórico financeiro para provar legado. |
| Gravação de segurança | Room `calcmot_recordings.db`, sessões com `createdAtUtcMs`, segmentos com hashes e metadados | Conteúdo local sensível; integridade de um segmento não prova data de uso anterior nem identidade Firebase. Regras atuais excluem banco/arquivos de cloud backup e device transfer. Não ler/copiar vídeos ou banco na preparação de legado. |
| Backup | Manifest permite backup; XMLs excluem gravações, sem excluir preferências de configuração | Preferências podem retornar numa restauração, sem garantia de backup disponível ou data confiável. Não prometer recuperação de identidade/direito com esse backup. |
| Firebase vinculado | `IdentitySession` e S04: Google, UID estável, linking/reauth e recusa de colisão | Identidade recuperável, **não prova histórica de uso**. Mesmo UID permite buscar grant já existente; criação/login no Firebase não retroage a uma instalação antiga. Sem vinculação, perda de dados pode perder a via local de identidade. |
| Histórico Analytics/Clarity | Eventos sanitizados `offer_parsed`, SDKs e relatórios agregados descritos no PRD/S03 | Não foi encontrada no app uma trilha individual autoritativa, vinculada ao UID Firebase, para conceder legado retroativamente. Não transformar agregado, sessão Clarity ou identificador analítico em prova de titularidade. Disponibilidade de export histórico individual verificável não foi comprovada. |
| Backend autoritativo | S02/S04: D1 privado, JWT e leitura própria; relatório anterior comprovou ausência de snapshots após cleanup | Nenhum inventário histórico de usuários/grants de produção foi fornecido ou comprovado. Esta execução não presumiu registros remotos nem os criou. As tabelas novas locais estão vazias fora das fixtures efêmeras. |
| Evidência futura verificável | Observação server-side durante ponte ou verificação de suporte com origem comprovável | Ainda não existe processo aprovado/coletor ativo. SQL/serviço preparam o armazenamento, não certificam uma declaração cliente. Não há código de migração assinado anterior localizado. |

A documentação Android define os timestamps como metadados do pacote em unidades de relógio civil e prevê restauração de preferências; a conclusão de que eles não autorizam legado é nossa análise de confiança. [PackageInfo](https://developer.android.com/reference/android/content/pm/PackageInfo#firstInstallTime), [Auto Backup](https://developer.android.com/identity/data/autobackup). Firebase oferece gestão de identidade; não fornece por isso prova de uso anterior do CalcMot. [Gestão de usuários](https://firebase.google.com/docs/auth/admin/manage-users).

## OQ-02 — decisão comercial aprovada

**Uso da versão 3.5/versionCode 12 ou anterior = legado, com Core gratuito permanente daquela versão.** A decisão do proprietário está formalizada na [política OQ-02](OQ-02-LEGACY-POLICY.md), com inventário e investigação da prova disponível. Google vincula/recupera identidade, sem redefinir elegibilidade; Premium posterior não é incluído automaticamente. Novos da versão 3.6 em diante não recebem grandfathering automático.

A recomendação anterior de incluir todos os cadastrados na ponte foi **substituída e não aprovada**. Janela de migração/data do login não ampliam a classe de legados e não excluem antigos tardios. Não escolher data de elegibilidade alternativa à regra por versão; o campo congelado `policyCutoffUtc` permanece nulo nesta preparação.

Foi localizado o commit local `b56f15bf8776228d35184dc3e1e25b8ccb2be759`, que declara 3.5/12. Ele permite inventariar funcionalidades; não comprova publicação/uso individual. Metadados locais, cadastro, UID e attestation da versão atual não provam sozinhos uso histórico: um APK antigo pode ser instalado posteriormente. Sem referência histórica verificável e titularidade comprovada, preparar revisão assistida sem fabricar prova nem bloquear o Core atual.

**Pendentes operacionais:** fontes reais de prova, critérios/reviewer de revisão e exceções, titularidade em conflitos, prazo provisório/retention, conferência do artefato distribuído e integração/release/testes físicos. A elegibilidade comercial e a preservação do gratuito não estão novamente em decisão. Ver a matriz detalhada da política.

## Implementação e integridade

`backend/src/legacy.ts` recebe contexto originado de `verifySession`; valida coerência subject/issuer/audience com o projeto configurado e usa somente esse UID. Não recebe um UID-alvo. Vinculação usa identities Google do token validado, não email. Não é uma segunda implementação de verificação JWT, nem recebe sessão de HTTP não validada.

`observeLocal` aceita somente digest e grava observação AUXILIARY com horário servidor; repetição devolve a referência original. Hash comprova igualdade de conteúdo, não sua veracidade. `recordGrant` consulta evidência privada verificada, titularidade e política aprovada; a política registra explicitamente os tipos aceitos e a referência dos critérios aprovados. Sem isso falha fechado para a concessão, nunca para o Core atual. O processo real de aprovação/verificação **não está implementado ou habilitado**. Fixtures o representam somente em D1 efêmero local.

Migration `0002_legacy_grants.sql` já estava no workspace e foi preservada byte a byte. Migration aditiva `0003_legacy_authority.sql` introduz políticas, evidências e intenções de recuperação, unicidade de prova verificada, concordância JSON/colunas, dependência da política e bloqueio de mutação. A tabela `legacy_claims` do rascunho 0002 permanece vazia/sem consumidor; não implementamos revisão S06. Duplicatas históricas incompatíveis fariam a criação do índice falhar, sem apagar registros; auditar/reconciliar antes de eventual migração de banco com dados. Não executar cegamente em produção.

Concessões concorrentes são arbitradas por unique constraints + `ON CONFLICT DO NOTHING`, com releitura e conferência do grant final; mesma prova não é reaproveitada por outro UID. Grant não contém `proAllowed`, preços, trial ou direitos Premium. Nenhuma escrita em `entitlement_snapshots`: evita substituir fontes Play/Promo presentes/futuras e respeita a fronteira S07. SQL privado/credencial de infraestrutura continua autoridade privilegiada; não alegar que essas regras protegem contra o próprio administrador do banco.

As políticas/evidências gravadas são imutáveis para impedir troca silenciosa de autoria/prova. Não há TTL/revogação/transferência automática. Retenção e tratamento excepcional de fraude/exclusão continuam dependentes de decisões OQ-07 e processo auditado; esta story não aprova retenção legal indefinida nem implementa deleção S15.

## Validação da primeira rodada (anterior à formalização por versão)

| Verificação | Resultado |
|---|---|
| Backend TypeScript | PASS; rascunho anterior apresentava erros de tipos, corrigidos no escopo S05. |
| Suite backend | **42 PASS, incluindo 15 S05**; 1 teste remoto opt-in SKIPPED intencionalmente, sem tocar D1 DEV. Validação final inclui fontes aceitas explicitamente na política e preservação de snapshot anterior durante migration. |
| Wrangler migrations local | 0001/0002/0003 aplicadas em `.tmp/s05-local-d1-final`; repetição: `No migrations to apply`. |
| Contagem local após migrations | Policies/evidence/grants/recovery/snapshots: **0/0/0/0/0**. Nenhum direito persistido fora das fixtures efêmeras. |
| Android | Suite completa: **269 testes, 47 suites, zero falhas/erros**, BUILD SUCCESSFUL (3m49s). Inclui dois testes novos S05; repetição final focada após ajuste de nulidade: **2 PASS**, BUILD SUCCESSFUL (1m38s). Avisos de depreciação preexistentes; nenhuma falha de regressão identificada. |
| Preservação | **7/7 contratos e 753/753 arquivos Android existentes idênticos por SHA-256**; migration 0002 preservada; `git diff --check` PASS. |

### Cobertura dos cenários pedidos

| Cenário | Evidência local automatizada / limite |
|---|---|
| Antigo com prova válida | Fonte Core gravada segundo política e prova **sintéticas** verificadas; schema S01 válido, sem Premium/snapshot. |
| Novo sem direito | Uso histórico comprovado somente de versão >=13 não basta para conceder; prova anterior <=12 pode ser recuperada depois. Versão atual não classifica a pessoa definitivamente. |
| Reinstalação/troca | Novo contexto de sessão com mesmo UID recupera o grant sem metadados locais. Teste lógico; não afirmar nova execução em segundo aparelho físico. |
| Offline | Falha de chaves/rede retorna 503; preparação Android sem import de rede e sem consumidor no Core. Não alegar teste físico novo de modo avião. |
| Firebase indisponível | Falha da descoberta confiável de chaves não aceita token nem apaga grant. App atual não chama esse fluxo para ofertas. |
| Não vinculado | `LINK_REQUIRED` para concessão; recuperação pendente não concede. Core não exige conta. |
| Conflito de identidades | Prova de outro UID recusada; intenção deduplicada, sem expor UID original/transferir grant. |
| Falsificação de elegibilidade | Observação local permanece AUXILIARY; SQL recusa local marcado VERIFIED e atualização da prova. Sem política não concede. |
| Concessão direta cliente | POST entitlement/admin recusados 403; claim não implementado 404; sem token 401. UID forjado no contexto é recusado. |
| Replay/duplicação | Cinco tentativas concorrentes geram um grant; retry não altera timestamp/revisão. Unicidade impede segunda titularidade; migrations repetidas não reaplicam. |
| Backend/snapshot ausente | 503 sem fabricar snapshot/grant; fonte existente continua recuperável. Erro DB simulado é recuperável. |

## Preservação do Core e escopo

Nenhum arquivo Android existente foi modificado: somente pacote preparatório `legacy/` e seus testes novos. Nenhuma chamada de rede/Auth foi adicionada às ofertas, OCR, cálculos, serviços ou overlay; navegação, permissões, versões e gratuito atual permanecem iguais. Não há fallback infinito novo nem cache/entrada comercial implementados antes de S08; o acesso atual simplesmente continua sem dependência desse backend.

Sem deploy local para remoto, push, release, credenciais novas, contratação, alteração Firebase/Cloudflare ou utilização de grants reais no DEV. Compatível com os mesmos bindings/SQL D1 e runtime Worker; não revalidamos CPU no edge nesta story sem deploy. Pendências operacionais S02/S04 permanecem no [relatório](S02-S04-INTEGRATION.md).

Rascunhos existentes foram copiados para `.tmp/s05-baseline/` antes das mudanças. Serviço/testes de legado foram ajustados à S05: removidos defaults comerciais, concessão por metadados locais, operações de claim e projeção de snapshots antecipadas. O gerador/package e a migration 0002 anteriores foram mantidos; `CLAUDE.md` não foi alterado. Sete contratos S01 permanecem congelados.

## Arquivos e reprodução

- Backend: [legacy.ts](../../backend/src/legacy.ts), [testes](../../backend/tests/legacy.test.ts), [0003](../../backend/migrations/0003_legacy_authority.sql); aproveitados 0002, gerador/declarações/validator legado e script de geração existentes no workspace.
- Android novo: [modelo auxiliar](../../app/src/main/java/br/com/calcmot/legacy/LegacyLocalEvidence.kt), [leitor local](../../app/src/main/java/br/com/calcmot/legacy/LegacyLocalEvidenceReader.kt), [testes](../../app/src/test/java/br/com/calcmot/legacy/LegacyLocalEvidenceTest.kt).
- Documentação: este relatório, BACKLOG, EXECUTION-PLAN e README backend.

No diretório `backend`: `npm run typecheck` e `npm test`. Teste remoto permanece opt-in; não habilitá-lo para esta story.

```powershell
# Somente local; não adicionar --remote nem usar wrangler.dev.jsonc.
$env:WRANGLER_SEND_METRICS='false'
$env:WRANGLER_LOG_PATH="$PWD/../.tmp/s05-local-d1-final/wrangler.log"
node node_modules/wrangler/bin/wrangler.js d1 migrations apply DB --local --persist-to ../.tmp/s05-local-d1-final
# Repetir: nenhuma migration pendente.
```

Na raiz, JDK 21 e Gradle cache local existentes: `./gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=2 --console=plain '-Pkotlin.compiler.execution.strategy=in-process' --no-problems-report`.

**Status atualizado S05: PARTIAL.** Elegibilidade comercial aprovada e guarda por versão implementada; comprovação/concessão real e recuperação operacional exigem fontes/processos verificáveis e stories próprias. Não marcar DONE com fixtures nem habilitar cobrança/S07 automaticamente.

## Continuação — regra aprovada 3.5/versionCode 12

Atualização posterior: S06 agora tem intake/revisão/auditoria/recurso locais **PARTIAL**, em [S06-LEGACY-RECOVERY.md](S06-LEGACY-RECOVERY.md). Ela permite registrar conclusão humana sobre conjunto de indícios, sem fingir histórico criptográfico. S05 conserva suas guardas de emissão; um futuro emissor para a decisão assistida precisa de autoridade/política próprias e transação com a revisão vigente. Nenhum grant novo ativado por essa conclusão.

[Política vinculante e inventário](OQ-02-LEGACY-POLICY.md). Adicionada migration [0004](../../backend/migrations/0004_legacy_version_35.sql), sem editar 0002/0003: limite aprovado explícito na política e prova de uso histórico privada/imutável, com referência, versão comprovada, base e reviewer. Nenhuma política/prova real semeada. Grants existentes permanecem intactos; novas concessões sem regra/prova suficientes falham fechado, sem restringir Core.

O serviço exige histórico verificado <=12, baseline `calcmot-core-3.5-vc12`, autoria/titularidade e política operacional explícita. Cadastro durante migração e observação server-side genérica não bastam, mesmo quando o tipo da observação é aceito. Não introduzir cutoff de calendário para excluir um legado que chegou tarde. `HISTORICAL_PROOF_REQUIRED` é resultado interno; schemas/OpenAPI congelados intactos.

Validação final desta continuação: **TypeScript PASS, 53 testes backend PASS (26 S05)**, um remoto opt-in SKIPPED. Migration 0004 aplicada sobre o banco local anterior, repetição sem migrations pendentes; policies/evidence/history/grants/snapshots **0/0/0/0/0**. Foram testadas versões 1/11/12 versus 13/14, ausência de prova histórica/cadastro na migração, política temporal alternativa recusada, recuperação de histórico anterior e grants pré-migration preservados. Resultados da rodada anterior acima permanecem históricos.

Nenhum arquivo Android foi alterado nesta continuação: suite Android de 269 casos anteriores não foi repetida sem mudança pertinente. Nenhum deploy, push, concessão real ou nova infraestrutura. [Arquivos e pendências da política](OQ-02-LEGACY-POLICY.md).
