# S03 — Auditoria de plataforma e publicação

Auditoria inicial: **2026-10-09**. Revalidação: **2026-10-10**, America/Sao_Paulo. Checkout: `growth`, HEAD `9d78bd76b3a373f139a8360a6e527fa35e7ec0fa`.

Escopo exclusivo: S03 de [BACKLOG.md](BACKLOG.md) e [EXECUTION-PLAN.md](EXECUTION-PLAN.md), PRD v5 e [contratos v1.0.0](../contracts/README.md). Requisitos OPS-002, BILL-007 e CORE-002; origem ST-29, PRD §§8.1/17. A orientação posterior do usuário prevalece sobre o planejamento anterior. Nenhuma story de implementação foi iniciada; nenhum código, configuração de produção, produto Play, recurso Firebase ou credencial foi criado/alterado.

## Resultado: HOLD para publicar a monetização

**GO** significa condição comprovada no alcance indicado. **HOLD** significa evidência, decisão ou preparação necessária antes da integração/ativação correspondente. **STOP** significa impedimento confirmado que exige suspensão. **Nenhum STOP identificado nesta auditoria.** A release atual estar publicada não aprova automaticamente a futura monetização.

O acesso ao navegador foi recuperado após reinício/permissões atualizadas. Foram lidos Firebase, Google Cloud e Play Console na conta `eduardoangelo20001@gmail.com`. Permanecem limites específicos de leitura, explicitados abaixo; não se presume prontidão de recursos por existir uma tela ou SDK.

## Checklist com evidências e responsáveis

### Revalidação de 2026-10-10

O controlador voltou a permitir leitura do navegador integrado. A aba original do Firebase foi preservada aberta. A leitura atual confirmou a conta `eduardoangelo20001@gmail.com`; nenhuma ação de inicialização, upgrade, criação ou salvamento foi executada.

| Item reconferido | Evidência atual | Resultado |
|---|---|---|
| Firebase overview | Projeto CalcMot, plano Spark; Analytics com 45 usuários ativos por dia, gráfico 03–09/10/2026. | GO atividade/plano; não comprova eventos V1. |
| Authentication | Tela inicial com botão “Vamos começar”. | HOLD preparação, OQ-01 continua aberta. |
| Firestore | Tela inicial com botão “Criar banco de dados”. | HOLD preparação; banco/região/rules não comprovados. |
| Functions | “Para usar Functions, faça upgrade do plano de preços do seu projeto”. | HOLD deploy; nenhuma escolha de plano aprovada. |
| Publicação Play | Lista de apps: `br.com.calcmot`, Produção, atualização 09/10/2026. Testar e lançar: **12 (3.5), rollout 100%**. Dashboard sem alterações não publicadas. | GO publicação atual; substitui a observação 11 (3.4) de 09/10 abaixo. |
| Registro Android | Dashboard Play identifica `br.com.calcmot` como registrado no Android Developer Console. | GO registro, corroborando C11. |
| Conta comercial | Dashboard ainda solicita configurar conta do comerciante. | HOLD preparação comercial; não comprova catálogo vazio. |
| Artefato local versus Play | Gradle permanece 11 (3.4), target 36/min 24. Artefatos comparados abaixo são 11 (3.4); não são a nova release Play 12 (3.5). | HOLD equivalência com binário atual: target/permissões/certificado de 12 não foram extraídos nesta revalidação. Não alterar versão local. |
| APIs Cloud | Dashboard APIs de `calcmot` navegou, mas não apresentou inventário carregado na leitura atual. | B04 permanece HOLD; não inferir API ativada/desativada. |
| Domínio/DAL | HTTPS raiz 200; assetlinks 400 com rejeição do header host. API pública DAL retorna `ERROR_CODE_FETCH_ERROR`, sem associação comprovada. Consultas em 15:13:10–15:13:12 UTC. | GO conectividade; HOLD App Links. |
| Normas oficiais | Reconferidas target API 36 para atualizações desde 31/08/2026, Billing 9.1.0 como release mais recente listada e Developer Verification vigente desde 30/09/2026 no Brasil. | Baselines normativas mantidas; links abaixo. |

As demais evidências de Console abaixo **continuam datadas de 09/10**, incluindo App Signing, status de políticas, detalhes SHA Firebase, IAM, Run, Pub/Sub e RTDN. Não foram promovidas a observações de 10/10. A atualização 12 (3.5) deve ter seu artefato e declarações confrontados antes de qualquer futura aprovação de release monetizada; esta auditoria não afirma que o binário novo é igual ao local.

Evidências do checklist a seguir coletadas em **2026-10-09**, salvo indicação histórica; a tabela acima prevalece nos itens reconferidos. **Titular** = proprietário do projeto/conta e responsável por decisões/acessos; **Sxx** = responsável pela implementação/validação posterior daquela story, sem autorização de avanço nesta entrega.

| Condição | Status | Evidência / limite | Responsável / próximo passo |
|---|---|---|---|
| Package e publicação | GO | Gradle/APK: `br.com.calcmot`. Play Console EA Mobile Apps: CalcMot em produção; versão 11 (3.4), rollout 100%, atualização 07/10/2026. | S27: comparar futuro artefato/track. |
| targetSdk e suporte | GO local | Gradle compile/target 36, min 24; `aapt dump badging` confirma APK target 36, versão 11/3.4. Exigência oficial API 36 para novas versões desde 31/08/2026 atendida localmente. | S18/S27: conferir build integrado. |
| Billing vigente | GO baseline / HOLD integração | Notas oficiais: 9.1.0 em 18/06/2026. AAR oficial minSdk 23, compatível com min 24. Dependência Billing ausente no checkout atual. | S18: resolver dependências e validar build/manifesto. |
| Assinatura local | GO | `apksigner verify` passou; APK v2 válido, um signer. Certificado APK/AAB coincide com **upload key** do Console. | S27: verificar artefato futuro. |
| Play App Signing | GO SHA-256 / HOLD SHA-1 de distribuição | Console: versões assinadas pela Play, chave em uso; snippet DAL fornece SHA-256 de distribuição diferente da upload key. SHA-1 da chave distribuída não foi extraído nesta sessão. | Titular/S04: obter SHA-1 correto antes de OAuth publicado. S29: usar SHA-256 Play. |
| Developer Verification | GO registro atual | Tela “Nomes de pacote”: `br.com.calcmot`, **Registrado**, uma chave, atualização 05/06/2026. Aba Identidade usa dados da conta Play; dados pessoais não reproduzidos aqui. | Titular: acompanhar novas pendências e eventual outra chave/canal de distribuição. |
| Políticas e declarações atuais | GO estado do Console | Status da política: “Nenhum problema foi encontrado”. Conteúdo do app/Precisa de atenção: “Tudo em dia”. Evidência válida para o estado observado, não para mudanças futuras. | Titular/S14/S27: revisar declarações da versão monetizada. |
| Serviço de acessibilidade | GO escopo conhecido | Aprovação existente confirmada pelo usuário; listagem descreve assistência à leitura/comparação. Onboarding tem ação explícita “Entendo e quero ativar”. | Preservar comportamento aprovado, sem mudança proposta. |
| Permissões | GO inventário / HOLD manifesto futuro | APK atual contém câmera, áudio, foreground services camera/microphone, notificações, armazenamento até API 28, Internet/rede, wake lock e permissão transitiva Install Referrer. Sem BILLING no APK atual. | S18/S27: conferir manifesto final e declarações pertinentes ao uso real; não alterar agora. |
| Firebase projeto/app | GO | Console e google-services concordam: projeto `calcmot`, número `303161771474`, app Android `1:303161771474:android:a060ee5f467931e1d8740b`, package `br.com.calcmot`. Ambiente no Console: “Não especificado”. | Titular/AR-01: definir separação desenvolvimento/produção. |
| Plano e acesso Firebase | GO evidência / HOLD deploy backend | Console mostra Spark; Usuários e permissões mostra titular como Proprietário. Isso não define orçamento nem autoriza upgrade. | Titular/AR-01: decisão de backend e faturamento quando necessário. |
| Analytics | GO atividade observada | Overview mostra 47 usuários ativos por dia; gráfico 02–08/10/2026. Há SDK Analytics/Crashlytics no checkout. Não prova coleta de cada evento da V1. | S10/S34: validar eventos/consulta previstos. |
| Auth | HOLD preparação | Navegação Authentication/users estabiliza na tela inicial Authentication com “Vamos começar”. Nenhum provedor recuperável configurado foi comprovado; não foi clicado o botão de inicialização. SDK Auth ausente no checkout. | Titular/OQ-01 e S04: aprovar provedor/linking e preparar configuração autorizada posteriormente. |
| Certificados Firebase | HOLD OAuth | Configurações/Geral do app: tabela SHA apenas com cabeçalho e sem linhas. Nenhuma impressão digital adicionada. | Titular/S04: cadastrar certificados corretos após autorização de configuração. |
| Firestore | HOLD preparação | Tela inicial “Cloud Firestore” com “Criar banco de dados”; não há banco operacional/rules/região comprovados nessa tela. Sem fontes rules/indexes no checkout. | Titular/S02: definir ambiente/região e provisionamento posterior. |
| Functions | HOLD plano/deploy | Tela pede upgrade do plano para usar Functions. Nenhum backend disponibilizado foi demonstrado. | Titular/AR-01: escolher backend/runtime/plano; sem upgrade nesta story. |
| Cloud Run | HOLD preparação | Serviços do projeto apresenta tabela vazia e instrução para criar serviço/ativar Cloud Run Admin. Não há endpoint V1 disponível demonstrado. | Titular/S02: decisão Functions ou Run; não criar alternativa silenciosa. |
| Contas de serviço | GO inventário parcial / HOLD acesso Play | Firebase indica duas contas de serviço com acesso. Inventário Cloud mostra `firebase-adminsdk-fbsvc@calcmot.iam.gserviceaccount.com`, ativada, sem chave. Contas com IAM e contas criadas no projeto não são o mesmo inventário. | Titular/S19: comprovar identidade de execução e permissões Play; não presumir que Admin SDK tem acesso financeiro. |
| Pub/Sub | HOLD infraestrutura RTDN | Projeto CalcMot: “Sem tópicos para exibir” e “Nenhuma assinatura para exibir”, sem filtro aplicado. | Titular/S20: topic/subscription/publisher/consumer autorizados posteriormente. |
| RTDN Play | HOLD configuração | Configuração de monetização: checkbox “Ativar notificações em tempo real” desmarcado; campo tópico e teste desabilitados. Nenhum teste enviado. | Titular/S20: integração e evidência de entrega/reconsulta Play. |
| Google Play Developer API | HOLD evidência | Marketplace apresenta `androidpublisher.googleapis.com` e botão Ativar, mas cabeçalho não mantém projeto selecionado; não é prova conclusiva do estado da API no `calcmot`. Dashboard APIs falhou “Falha ao carregar” após nova tentativa. Permissões Play de identidade de serviço não auditadas. | Titular/S19–S21: conferir APIs habilitadas e concessões mínimas reais. |
| Conta de comerciante/catálogo | HOLD preparação comercial | Monetização pede conta do comerciante. Página Assinaturas bloqueada: exige configurar Google Payments. IDs/ofertas não foram conferidos; não concluir catálogo vazio a partir de página bloqueada. | Titular/OQ-05/S18: conta comercial e catálogo, fora desta auditoria. |
| Domínio HTTPS | GO conectividade | DNS responde e `https://calcmot.com.br/` retorna HTTP 200. Não comprova controle do hosting. | Titular/AR-01/S29: evidenciar acesso de publicação. |
| Android App Links | HOLD verificado | assetlinks HTTP 400; consulta pública DAL: `ERROR_CODE_FETCH_ERROR`. Manifesto atual sem filtro HTTPS autoVerify para `/r/`. Não afirmar que arquivo inexiste. | S29/S30: associação com certificado Play e validação no Android. |
| Privacidade/Data Safety futura | HOLD reconciliação | Play pública declara nenhum dado coletado/compartilhado; política pública apresenta analytics/login/cloud como futuros. SDKs e Console mostram Analytics. Categorias/fluxo efetivo da versão distribuída precisam ser reconciliados antes da próxima release. | Titular/S14/S27: atualizar conforme processamento efetivo; não inferir infração automática. |

Base normativa: [target API](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en), [Billing releases](https://developer.android.com/google/play/billing/release-notes), [Developer Verification](https://developer.android.com/developer-verification), [Functions e deploy](https://firebase.google.com/docs/functions/get-started), [preparação Billing/RTDN](https://developer.android.com/google/play/billing/getting-ready), [Play Developer API](https://developers.google.com/android-publisher/getting_started), [App Links](https://developer.android.com/training/app-links/verify-applinks), [dados dos SDKs Firebase](https://firebase.google.com/docs/android/play-data-disclosure).

Evidências públicas: [CalcMot na Play](https://play.google.com/store/apps/details?id=br.com.calcmot&hl=pt_BR&gl=BR), [política publicada](https://dugelo.github.io/calcmot-privacy-policy/), [domínio](https://calcmot.com.br/). O inventário de permissões não autoriza mudanças; a permissão transitiva Install Referrer não comprova implementação de atribuição.

## Certificados e artefatos comparados

**SHA-256 de distribuição Play**, obtido do snippet Digital Asset Links no Console:

`05:4E:84:4D:BA:7E:C6:B7:BC:C8:A5:27:26:B1:C9:D2:43:BB:C3:F3:A6:34:08:EC:9C:FD:FC:A4:3E:D0:6F:3A`

**Upload key**, igual ao certificado dos artefatos locais:

- SHA-256: `CE:41:E6:7E:A3:B1:72:B6:50:44:36:ED:F2:FE:3D:57:AF:26:EF:1D:56:D9:57:D8:2F:7D:7A:25:9C:A8:FE:66`.
- SHA-1: `FF:A7:EA:91:AD:22:03:E8:4E:C0:CE:5C:B8:98:EE:33:74:A2:1A:3C`.

Não usar upload SHA-256 como associação da instalação distribuída pela Play. Não substituir SHA-1 de distribuição por SHA-1 de upload ao preparar login da versão publicada.

| Artefato existente | Tamanho | SHA-256 do arquivo |
|---|---:|---|
| `app/build/outputs/apk/release/app-arm64-v8a-release.apk` | 17.792.902 bytes | `6d825adeff7e0ac07551d10ce762a59c6724c783b36b9343fff76b5e30482ca8` |
| `app/build/outputs/bundle/release/app-release.aab` | 21.084.944 bytes | `6900a7f08e35c4cf11810b62e7d3e8da5fa3dca6547ce199d935b3b1c2f2fabf` |

São artefatos locais de 08/10/2026; versão igual não prova conteúdo binário idêntico ao upload de produção de 07/10. Não foi executado build novo.

Billing 9.1.0 oficial foi baixado **somente para inspeção** em `.tmp/s03-audit/billing-9.1.0.aar`, SHA-256 `d28286d4e4c18725510980de01211d9b72501e7146d96817fa0c9999af9b0d22`. Manifesto min 23/target 35; target da biblioteca não substitui target 36 do app. POM billing-ktx depende de Kotlin 2.2.10; catálogo local declara 2.0.21 e AGP 9.0.0. Resolução efetiva/compilação ainda precisam ser validadas em S18; nenhum upgrade antecipado foi decidido.

## Registro de leitura dos consoles

Todas as leituras abaixo: 2026-10-09, conta indicada pelo usuário. São observações do DOM/AX pela ferramenta de navegador, sem mudanças remotas.

| Evidência | Página / caminho observado | Resultado |
|---|---|---|
| C01 | Firebase `/project/calcmot/overview` | Spark, identidade da conta e Analytics com atividade. |
| C02 | Firebase `/project/calcmot/authentication` | “Vamos começar”; não inicializado pela auditoria. |
| C03 | Firebase `/project/calcmot/firestore` | “Criar banco de dados”. |
| C04 | Firebase `/project/calcmot/functions` | Upgrade necessário. |
| C05 | Firebase `/project/calcmot/settings/general/android:br.com.calcmot` | IDs correspondentes, ambiente não especificado, tabela SHA sem linhas. |
| C06 | Firebase Configurações/Usuários e permissões | Titular proprietário; duas contas de serviço com acesso. |
| C07 | Cloud `/iam-admin/serviceaccounts?project=calcmot` | Admin SDK ativada e sem chave. |
| C08 | Cloud `/cloudpubsub/topic` e `/cloudpubsub/subscription`, projeto CalcMot | Nenhum tópico/assinatura. |
| C09 | Cloud `/run/services`, projeto CalcMot | Tabela vazia/instrução de criar serviço. |
| C10 | Cloud `/apis/dashboard?project=calcmot` | Falha ao carregar, inclusive na repetição. Estado da Developer API não confirmado. |
| C11 | Play conta EA Mobile Apps / Verificação de desenvolvedor Android | Package registrado, uma chave. |
| C12 | Play CalcMot / Testar e lançar | Produção 11 (3.4), rollout 100%. Avisos de edge-to-edge para próxima versão, sem bloqueio de publicação declarado. |
| C13 | Play CalcMot / Protegido com Play / Assinatura de apps | Chave em uso; upload certificate e snippet DAL lidos. |
| C14 | Play CalcMot / Monetização / Assinaturas | Conta de comerciante necessária; catálogo inacessível. |
| C15 | Play CalcMot / Configuração de monetização | RTDN desativado; pausa de assinatura aparece ativada. Preservar configuração e validar PAUSED em S20/S25, sem mudar política comercial. |
| C16 | Play CalcMot / Status da política e Conteúdo do app | Nenhum problema e declarações em dia. |

Play developer ID `4797547373591118386`, app ID `4973607789747717944`: identificadores para localizar evidência, não credenciais. Dados pessoais de identidade e chaves de licença não são reproduzidos.

Respostas públicas/POMs/HTMLs de auditoria estão em `.tmp/s03-audit/`, com metadados em `public-network-evidence.json`, `public-network-recheck.json` e `digital-asset-links.json`. Consulta DAL em 18:13:35 UTC retornou erro de fetch. Temporários não substituem este registro durável. A imagem fornecida pelo usuário corroborou overview/Spark, posteriormente lidos diretamente.

Histórico em `CalcMot-Metrics-2026-08-to-current/README.md` e exports mostra Analytics/Clarity e consulta BigQuery sem datasets sob aquela conta/projeto. Não é inventário atual de toda a infraestrutura. Clarity BuildConfig local vazio não prova configuração da versão distribuída; não concluir presença/ausência em produção sem comparar o artefato/configuração correspondente.

## AR-01: avanço e bloqueios específicos

Obtidos: projeto/app Firebase, plano Spark, acesso de proprietário, status inicial de Auth/Firestore/Functions, inventário de serviços Run/PubSub, package registrado, assinatura Play distinta da upload key, ausência de conta comercial operacional na tela e estado RTDN, conectividade do domínio. Acesso ao navegador deixou de ser bloqueio geral.

| ID | Decisão/evidência necessária | Afeta / owner |
|---|---|---|
| B01 | Definir ambiente de desenvolvimento/produção, backend Functions ou Run, runtime e pasta de fontes. Console “ambiente não especificado” não é política de ambientes. | S02, AR-01 / titular. |
| B02 | Aprovar OQ-01: provedor, colisão de linking e recuperação. Configurar Auth e OAuth apenas em execução autorizada futura; obter SHA-1 da assinatura Play e cadastrar SHA pertinentes. | S04/S09 / titular. |
| B03 | Preparar banco/regras/região/backend no ambiente aprovado. Functions exige decisão de upgrade/faturamento; não escolher Run como desvio silencioso. | S02/deploy / titular. |
| B04 | Confirmar habilitação da Developer API e identidade de execução com permissões Play necessárias. Uma conta Admin SDK não prova autorização de pedidos/subscriptions. | S19–S21 / titular. |
| B05 | Configurar conta do comerciante e conferir IDs/ofertas/mensal/trimestral/trial; resolver OQ-05 antes da operação comercial. | S18/gates Billing / titular. |
| B06 | Topic/subscription/consumer e concessão Pub/Sub Publisher ao serviço Play; configuração RTDN e teste autenticado/reconsulta API. | S20 / titular e agente da story. |
| B07 | Evidenciar controle do hosting e publicar associação DAL usando certificado Play; domínio hoje não tem associação verificável. | S29/S30 / titular. |
| B08 | Reconciliar política/Data Safety com coleta da versão efetiva e futuras categorias de Auth/monetização. Estado verde atual do Console não substitui revisão da próxima release. | S14/S27 / titular. |

OQ-04 continua aberta: acesso API/RTDN não substitui prova oficial de primeira cobrança paga/não anulada. Não foi investigado um pedido real nesta story. OQ-02/03/06/07/09 e demais perguntas não foram resolvidas por esta auditoria. Nenhum prazo comercial, custo fixo ou janela financeira foi inventado.

## OQ-01: recomendação objetiva, não aprovada

Recomendo **Google Sign-in via Firebase Authentication e Credential Manager**, com vinculação voluntária durante a ponte gratuita e recuperação pelo mesmo Google account em novo aparelho/reinstalação. Preservar UID por linking legítimo. Core não deve exigir login síncrono por oferta. Não tratar conta Google de login como necessariamente igual à conta compradora da Play.

Se a credencial pertence a outro UID, exigir política explícita de ownership/collision; não mesclar por email nem sobrescrever grants, dados ou compras automaticamente. Perda de conta não vinculada ou acesso ao Google exige política de suporte, sem prometer recuperação universal. **OQ-01 permanece OPEN_QUESTION** até aprovação do provedor e dessas regras.

Pré-requisitos: provider habilitado em execução posterior autorizada, SHA-1 correto da versão distribuída, Web client ID correto, configuração Firebase atual e testes de linking/recovery. O snapshot local sem OAuth e tabela SHA vazia mostram preparação pendente, não autorizam criar credenciais.

Email/password acrescenta senha/reset/suporte; telefone acrescenta SMS/quotas e controles de abuso. Não são defaults justificados pelo PRD. Fontes: [Google Auth Android](https://firebase.google.com/docs/auth/android/google-signin), [account linking](https://firebase.google.com/docs/auth/android/account-linking), [phone Auth](https://firebase.google.com/docs/auth/android/phone-auth).

## Pré-requisitos para iniciar S02 e S04 em paralelo

S01 está concluída; transporte, UID, nomes/estados/eventos e ownership devem usar os contratos congelados. Não criar rotas financeiras fictícias nem mudar contratos para contornar ambiente indisponível.

| Frente | Para implementação/validação local | Para integração externa |
|---|---|---|
| Comum | Decisão B01 sobre ambiente/realm e coordenação UID/erros/ownership; definição do provedor e política pertinente de OQ-01. | Configuração e acesso comprovados ao ambiente aprovado. |
| S02 | Backend/runtime/pasta definidos; papéis e parâmetros de proteção acordados. Fixtures/emuladores podem provar rejeição de token/UID forjado e proteção dos registros, sem deploy. | B03: Firestore/backend/regras/IAM e faturamento quando exigido; autorização específica para provisionamento. |
| S04 | OQ-01 aprovada; dependências/configuração e estratégia de testes de Auth/linking/recovery definidas. | B02: Auth/OAuth e certificados corretos preparados e comprovados. |

Depois dessas decisões, **S02 || S04**: agente de backend implementa transporte/proteção; agente Android implementa identidade/linking. Coordenar arquivos/UID pelos contratos existentes. Conta comercial, catálogo, RTDN, prova financeira e DAL bloqueiam integrações/gates posteriores, não devem ser inventados como dependências globais para o primeiro trabalho local de S02/S04. Nenhuma das duas stories foi iniciada.

## Validação e fechamento exclusivo

- Package/SDK do APK comparados ao Gradle; certificado local APK/AAB comparado à upload key; certificado de distribuição separado por evidência Play.
- Baseline Billing conferida em fonte oficial; AAR/POM inspecionados. Compatibilidade de build completo ainda pertence a S18.
- HTTPS/assetlinks consultados; erro corroborado pela API pública Digital Asset Links. Não há associação comprovada.
- Estado de serviços e publicação lido diretamente nos consoles; inventário de APIs permanece inconclusivo por falha da página.
- Status/owners e condições para S02/S04 separados dos gates comerciais. `ACTIVE`/RTDN não viraram prova de dinheiro; legado, reward 7/23/30 total 60 e exclusividade driver/creator preservados nos contratos.
- Na revalidação de 10/10, seis contratos já diferiam do baseline temporário antigo de 09/10: README, analytics-events, billing-state-matrix, entitlement.schema, referral.schema e test-matrix. A auditoria não atribui origem nem reverte essas diferenças. Foi capturado um baseline do conteúdo recebido nesta execução em `.tmp/s03-audit/s03-2026-10-10-baseline.json`; a validação final compara os sete contratos e cinco arquivos preexistentes a esse baseline. Nenhum contrato foi editado nesta revalidação.
- Consultas públicas atuais registradas em `.tmp/s03-audit/public-2026-10-10.json`. PRD permanece com SHA-256 `7CD6D94723F381B69B71413EEEFFA4287DB0A4CE032DD28017031A5C3FEFE221`. Sem build/teste Android novo, deploy, envio de teste RTDN ou salvamento de configuração.
- Resultado final de 10/10: **PASS** — 12 arquivos protegidos comparados ao baseline desta execução, zero mudanças; zero links locais quebrados; `git diff --check` sem erros. Leitura cruzada mantém PAUSED, ACTIVE separado de pagamento, grants de legado e ledger 7/23/30 total 60; nenhuma pendência externa foi convertida em configuração presumida.

**S03 encerrada como auditoria documental com HOLD de prontidão**, não como aprovação de publicação. S12/S16/S18 podem consumir as evidências e limites deste relatório; dependências remanescentes e gates continuam válidos. Próxima execução depende de pedido específico; não avançar automaticamente para S02/S04.
