# OQ-02 — Política aprovada de usuários legados

Data da formalização: 2026-10-13. Autoridade: decisão expressa do proprietário nesta conversa. **Elegibilidade comercial RESOLVIDA; comprovação e operação parcialmente pendentes.** Base técnica `growth/226fe22f`. Não autoriza concessões reais, deploy, release, cobrança ou push.

## Decisão vinculante

**Atualização operacional aprovada pelo proprietário em 2026-10-14:** somente o proprietário pode aprovar. Avaliação humana assistida pode autorizar LegacyGrant permanente com titularidade confirmada, justificativa e evidências contextualizadas, em trilha distinta de VERIFIABLE_HISTORY. Não registrar avaliação humana como histórico verificado. Sem Premium, provisório de sete dias, transferências automáticas ou revogação/alteração de grants anteriores. Retenção/exclusão permanece OQ-07. Emissor privado e recuperação local implementados na S06; operador real e ensaio operacional ainda pendentes. [Evidências e procedimento vigente](S06-LEGACY-RECOVERY.md). Menções anteriores a aprovação/emissor pendentes abaixo são históricas e ficam substituídas por esta atualização.

**Todo usuário que utilizou o CalcMot na versão 3.5 (versionCode 12) ou anterior é legado e preserva permanentemente o Core gratuito existente até essa versão.**

- Atualização, reinstalação, troca de aparelho, preço e plano não extinguem o direito.
- Google vincula/recupera titularidade; data do login, criação do UID e versão atual não redefinem elegibilidade.
- Recursos Premium posteriores não estão automaticamente incluídos.
- Usuários novos da versão 3.6 em diante não recebem grandfathering automático.
- Versão declarada, timestamp, preferências/DataStore/Room, UID ou simples cadastro durante a migração **não autorizam concessão**.
- Falha de Auth/backend ou ausência de prova não bloqueia o Core atualmente gratuito. Casos antigos inconclusivos têm via de revisão assistida, sem falsificar comprovação.

Fica **substituída e não aprovada** a recomendação anterior de tornar legados todos os cadastrados na ponte. A janela de migração organiza comunicação/suporte; não amplia a população elegível nem faz um legado perder seu direito por chegar tarde. Nenhuma nova data de elegibilidade foi definida. Calendário de eventual rollout não substitui a fronteira aprovada por uso de versão.

Identificador técnico do inventário: `calcmot-core-3.5-vc12`. É um rótulo para o conjunto aprovado, não nova feature nem prova histórica. Os sete contratos S01 e o PRD-fonte permanecem intactos. O campo S01 `policyCutoffUtc` aceita nulo e fica **nulo nesta preparação**, sem inventar equivalência entre versão e timestamp.

## Inventário gratuito preservado

Evidência de fonte: commit local **`b56f15bf8776228d35184dc3e1e25b8ccb2be759`**, cujo `app/build.gradle.kts` declara **versionName 3.5 / versionCode 12**. A comparação de `app/src/main` com `226fe22f` identificou apenas a adição posterior de quatro arquivos de identidade, sem alterações nas funcionalidades abaixo. Isso documenta código local daquela versão; não é prova de uso individual nem atestado de publicação/identidade binária de um APK da Play.

| Conjunto protegido | Comportamento gratuito identificado no código 3.5 | Fontes locais correspondentes |
|---|---|---|
| Leitura de ofertas Uber e 99 | Captura/leitura já suportada, interpretação e avaliação existentes, funcionamento automático/pausa e estados de espera | `ReadingPipelineRuntime.kt`, `accessibility/`, `ninetynine/`, `processor/` |
| Cálculos e semáforo | Avaliação de rentabilidade, R$/km, R$/hora, classificação e impacto na meta, conforme algoritmos e limitações atuais | `model/Profitability.kt`, `model/FinancialImpact.kt`, processadores e `AppSettings.kt` |
| Aviso flutuante | Exibição dos resultados, posição, temas/estilos e preferências já disponibilizados | `overlay/`, `ui/OverlayPositionScreen.kt`, `ui/OverlayThemeScreen.kt` |
| Minha meta e configurações | Meta por km/hora e modos existentes; ativação da leitura e da exibição de impacto; preferências de cálculo já existentes | `ui/FinanceScreen.kt`, `ui/HomeScreen.kt`, `AppSettings.kt`; rota GOAL em `CalcMotNavigation.kt` |
| Gravação de segurança | Setup/consentimento próprios, gravação já oferecida, histórico/biblioteca, reprodução, salvamento na galeria, compartilhamento, exclusão e ajuste dos prazos existentes | `securityrecording/ui/RecordingRoutes.kt`, `RecordingLibraryScreens.kt`, `data/RecordingFilesRepository.kt`, rotas Ferramentas |
| Diagnóstico e recuperação técnica | Status, diagnóstico de leitura, cópia e reinício manual já oferecidos | `ui/ReadingDiagnosticsScreen.kt`, `CalcMotNavigation.kt`, runtime |
| Onboarding, ajuda e suporte | Setup/permissões, estados Home, ajuda, privacidade e envio voluntário de feedback | `ui/OnboardingScreen.kt`, `Home*`, `HelpScreen.kt`, `PrivacyPolicyScreen.kt`, `FeedbackScreen.kt` |

Não retirar esses comportamentos nem vendê-los novamente ao legado. As mesmas condições técnicas existentes (permissões, compatibilidade de aparelho/app, armazenamento e regras locais de gravação) continuam aplicáveis; gratuidade permanente não promete compatibilidade impossível ou armazenamento infinito.

**Limites do inventário:** `FinanceRepository` e blocos antigos de histórico/finanças existem no código, mas a rota atual Minha meta utiliza a tela de meta; não comprovamos que gestão financeira manual ou histórico de corridas eram funcionalidades publicadas e acessíveis nessa versão. Preservar código/dados existentes, sem anunciar como produto novo nem cobrar por uma função comprovadamente gratuita anterior. `PremiumScreen`/protótipos antigos não provam assinatura/benefício pago implementado. Features que existiam em versões anteriores e foram retiradas antes de 3.5 exigem inventário histórico complementar; esta sessão não as presume nem autoriza sua cobrança. Pro Insights, Billing e recompensas futuros não entram automaticamente no conjunto legado.

## O que pode provar elegibilidade

| Evidência | Uso seguro | O que não prova |
|---|---|---|
| Grant autoritativo já confirmado + mesma identidade recuperada | Restaurar sem recalcular elegibilidade pela versão atual/data do login | Não autoriza transferir para UID distinto sem processo comprovável. |
| Registro histórico independente, individual e legitimamente vinculável ao titular, que comprove uso em <=12 | Verificar origem/autenticidade, contexto de versão e titularidade; registrar referência/digest, reviewer e decisão | Não pode ser criado retroativamente a partir de metadados atuais. Não foi encontrado um inventário desse tipo no backend antigo. |
| Feedback antigo originalmente recebido pelo suporte | Candidato concreto: `FeedbackEmailLauncher` da 3.5 podia incluir versão/código e estado do app por escolha do usuário. Se a mensagem original existir, conferir recebimento no provedor, contexto histórico e titularidade em revisão | Corpo de email é editável; encaminhamento/screenshot e versão escrita não bastam. Não lemos uma caixa postal nem presumimos que essas mensagens existem; incluir app info e enviar eram voluntários. |
| Revisão assistida com prova histórica verificável | Conferência humana auditada de evidência independente; somente marcar verificada se efetivamente comprovado o uso elegível e titular | Reviewer/booleano/reference no banco, sem verificação real, não tornam a alegação verdadeira. |
| Avaliação assistida de conjunto razoável, sem registro individual definitivo | Proprietário conclui e justifica uso legado/titularidade, com indícios corroborados e tratamento de contradições; decisão ASSISTED_ASSESSMENT | Não é prova criptográfica nem entrada fictícia em `legacy_historical_use`. S06 registra a decisão; emissão baseada nela continua pendente, sem concessão automática. |
| Metadados do pacote, onboarding, preferências, dados locais e relato | Contextualizar e abrir revisão; auxiliar investigação com minimização de dados | Sozinhos não comprovam origem, uso, versão histórica ou titularidade. Não copiar vídeos/dados financeiros para o backend de growth. |
| Firebase Google vinculado recentemente | Identificar o solicitante e oferecer recuperação futura | Não prova instalação/uso anterior, nem a conta compradora Play. |
| Registro server-side de cadastro durante migração | Auditar o pedido e sua data | Não prova que usou <=12: **nunca concede automaticamente**. |
| Play Integrity atual, mesmo `PLAY_RECOGNIZED`, versão <=12 e `LICENSED` | Se um dia integrado e validado no servidor, reforça autenticidade/contexto **atual**; apenas componente auxiliar da análise histórica | Não informa versão usada no passado nem demonstra sozinho que a pessoa é um usuário histórico. Não dispensa prova de uso/titularidade nem resolve reinstalações antigas. |

Play Integrity informa pacote/certificado/versão atual reconhecida; licensing informa aquisição/atualização pela conta Play. Inferência de segurança: isso não é um histórico de uso individual. [Formato/veredictos oficiais](https://developer.android.com/google/play/integrity/verdicts). A documentação de [PackageInfo](https://developer.android.com/reference/android/content/pm/PackageInfo#firstInstallTime) e [backup](https://developer.android.com/identity/data/autobackup) também não transforma dados locais em registros históricos do servidor.

Não habilitamos Integrity, export de Analytics, anonymous Auth, APIs novas ou infraestrutura para tentar preencher a lacuna por presunção. Relatórios agregados GA4/Clarity não identificam legitimamente o titular de um direito. Uma fonte individual histórica só pode ser considerada se de fato existir, for acessível legitimamente e vincular uso, versão e titular sem reconstrução fictícia.

## Migração e recuperação propostas

1. **Manter Core gratuito e sem gate novo.** Preparar versão/comunicação de migração em story própria; não exigir login para continuar o uso atual.
2. **Vínculo opcional Google:** identificar a conta recuperável e associar o pedido. O usuário já era elegível pelo uso histórico, mesmo que só faça login depois da versão 3.6. Vincular não concede nem retira elegibilidade.
3. **Buscar primeiro um grant existente pelo UID validado.** Se existir, restaurar a fonte permanente sem olhar versão instalada, data de reinstalação ou término da janela. Conflito entre UIDs segue suporte, sem merge por email.
4. **Sem grant:** coletar somente indícios mínimos e, quando disponível, referência a prova histórica verificável. Pedido inconclusivo fica em revisão; registro de migração e hash local não viram prova histórica.
5. **Verificação confiável:** conferir autenticidade e ligação do documento/registro ao titular e ao uso de versão <=12; registrar origem, versão histórica comprovada, digest/referência, responsável e horário da verificação. Nenhum writer público desse material está implementado. Um sinal que só comprove uso da versão >=13 não basta para conceder, nem prova que o usuário nunca usou uma anterior.
6. **Materialização futura autorizada:** após aprovação operacional e sem concessões nesta tarefa, gravar uma única fonte Core imutável. Sem fonte Premium ou snapshot antecipado. Restaurar em reinstalações/aparelhos pelo mesmo titular. Etapas de emissão, revisão operacional e UX exigem suas stories/autorizações próprias.

**Revisão não significa concessão automática.** Para usuário antigo plausível sem prova conclusiva, propor protocolo humano, pedido de complementação e tratamento provisório finito conforme S06/OQ-03, sem bloqueio do Core atual. Priorizar recuperação de grant e registros originais de suporte que já existam, evitando exigir conteúdo de corridas, vídeos, documentos pessoais ou provas que nunca foram geradas. Conjunto de indícios pode orientar a análise; os critérios de suficiência ainda precisam ser operacionalizados, sem transformar um indicador isolado em certeza. Não escolher silenciosamente duração, aceitar relato como prova ou marcar alguém como novo por falta de conta. Se a prova não puder ser recuperada, manter explicitamente **não comprovado**; o proprietário decidirá o processo excepcional que concilie inclusão e risco, sem chamar uma exceção de prova técnica. A elegibilidade comercial aprovada não está em discussão.

**Instalação tardia de APK antigo/downgrade:** versão antiga atual não distingue usuário histórico de recém-chegado. Não conceder pela versão instalada, nem criar uma nova data de exclusão por conta própria. Apurar a história por revisão e referência verificável; sem ela, prova insuficiente. A disponibilidade de um artefato 3.5 para testes não transforma todos que o instalarem agora em usuários historicamente comprovados.

Não é possível reconhecer com certeza todas as instalações antigas que perderam os dados e nunca foram identificadas/sincronizadas. O compromisso comercial continua permanente; a lacuna é de identificação e recuperação. A alternativa segura imediata é conservar o gratuito atual, comunicar o limite e concluir suporte antes de qualquer transição comercial; não ampliar silenciosamente a classe de legados para esconder essa limitação.

## Preparação técnica executada, sem ativação

- Migration **0004** aditiva: registra regra explícita `maximum_historical_version_code=12` por versão de política, sem semear políticas reais; policies anteriores sem regra não emitem novos grants.
- Tabela privada imutável `legacy_historical_use` associa prova de uso histórico ao evidenceId/titular já privado, com versão comprovada, base/referência, data e verifier. `CURRENT_APP_ATTESTATION` e `CLIENT_DECLARATION` não são bases históricas aceitas.
- Serviço requer essa prova, o baseline aprovado e política por versão com cutoff nulo. Sem prova: `HISTORICAL_PROOF_REQUIRED`, resultado interno, sem alterar OpenAPI. UTC não substitui a versão como critério.
- Trigger adicional impede SQL de emitir grants sem uso histórico verificado <=12. Grants preexistentes não são atualizados, revogados ou reclassificados.
- Sem emissor/coletor de prova verificada, revisão operacional, endpoint de concessão ou consumer no Core. Nenhum fluxo público aceita que o cliente envie `used_version_code` para autoatribuir direito.

As bases de verificação são registros do resultado de uma análise confiável futura; este código **não inventa um verificador criptográfico para documentos inexistentes**. Fixtures são exclusivamente sintéticas/locais, não evidência real de usuários.

## Pontos operacionais pendentes — sem reabrir elegibilidade

| Pendente | Evidência/decisão necessária |
|---|---|
| Prova histórica disponível | Inventariar fontes independentes reais; confirmar se alguma permite vincular legitimamente uso <=12 e titular. Ausência não se resolve com UID/Integrity/data local. |
| Critérios de revisão | Definir evidências suficientes, complementação, reviewer autorizado, auditoria/recusa/contestação e caso irrecuperável. Sem critérios aprovados, não assinalar `VERIFIED`. |
| Titularidade excepcional | Processo de perda da conta/conflito entre UIDs (OQ-01), sem transferências por email. |
| Provisório e retenção | Prazo e tratamento finitos S06/OQ-03, retenção/acesso/exclusão OQ-07; não ativados. |
| Artefato publicado 3.5 | Conferir APK/AAB distribuído e conjunto real de funcionalidades contra o commit local; preservar qualquer gratuito histórico adicional identificado. Não exige nova decisão comercial. |
| Migração/release | Comunicação, janela operacional, testes físicos e gates de S16; nunca tornar chegada tardia ou versão atual em exclusão de legado. |
| Ativação técnica | Processo confiável para políticas/verificação, integração e autorização futura de ambiente, mais pendências operacionais S02/S04. Nenhuma concessão real/deploy autorizados agora. |

## Validação desta formalização

Continuação S06: [revisão assistida e procedimento](S06-LEGACY-RECOVERY.md). Encontradas duas conversas reais antigas de suporte, uma referindo 3.3/10; nenhuma foi convertida automaticamente em direito. Proprietário é o responsável final pelas exceções. Proposta v1 de avaliação combinada implementada/testada localmente, sem marcar indício como prova histórica; ainda sem emissor ativado ou provisório. A exigência de registro definitivo não é a única via de julgamento humano, mas a política de concessão e sua emissão precisam distinguir essa autoridade da prova verificável.

TypeScript PASS e **53 testes backend PASS**, incluindo **26 S05**; um teste remoto opt-in permaneceu SKIPPED, sem acesso ao D1 DEV. Cobertura inclui versões históricas 1/11/12 versus 13/14, registro de migração sem prova recusado, rejeição de política temporal alternativa, recuperação posterior de uso anterior e preservação de grants de schema anterior.

Wrangler aplicou somente 0004 sobre o banco **local** já migrado, e a repetição retornou `No migrations to apply`. Contagem final policies/evidence/history/grants/snapshots **0/0/0/0/0**. Nenhuma prova real ou concessão semeada. Android não foi alterado nem retestado nesta continuação; resultado anterior de 269 testes é histórico, não uma nova validação física.

**S05 PARTIAL:** política comercial formalizada e guarda técnica local implementada. Próximo passo é fechar fontes/critérios de comprovação e revisão, validar casos reais de restauração e executar as stories de suporte/projeção quando autorizadas; não ativar benefícios com fixtures ou decisões presumidas.
