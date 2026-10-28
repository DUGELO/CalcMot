# S09 — Interface de identidade e recuperação

**Atualização vigente — 2026-10-19: fluxo operacional DEV concluído; S09 global PARTIAL / rollout HOLD.** V3 aprovada preservada, intake S01 exclusivamente identityDev opt-in, operador real e política OQ-07 do piloto validados. Grant sintético único autorizado recuperado por Google real e reconhecido na V3 após logout/relogin, reinício e reinstalação somente DEV. Outra conta não herdou o direito; rejeição não emitiu fonte; 503/offline controlados preservaram o cache real. [Relatório conclusivo com testes físicos, CPU e limites](S06-S09-DEV-OPERATIONAL-RESULTS.md). Não há mais dependência genérica de “S06 sem operador / OQ-07 sem decisão do piloto”; permanecem rollout de privacidade, segundo aparelho, capacidade de cauda e garantia de concorrência fora do pacote isolado. Nenhum redesign, produção ou S10. As notas abaixo são histórico e não revogam a aprovação visual.

**Auditoria final S09 — 2026-10-15:** revisão dirigida e integração real DEV em [S09-FINAL-AUDIT.md](S09-FINAL-AUDIT.md). Pequena correção autorizada de sucesso tardio e `flatMapLatest` (switchMap do Flow); 44 testes JVM PASS, sem redesign. S09 permanece PARTIAL; nenhuma S10 iniciada. Esta nota prevalece sobre os estados históricos abaixo.


**Atualização vigente — 2026-10-15:** V3 ajustada aprovada pelo proprietário como referência e integrada à composição funcional existente, sem dependência dos protótipos. Conta prioriza identidade; Meu acesso distingue consulta indisponível, estado desconhecido e resposta válida sem grant com linguagem neutra. Direito permanente confirmado oferece detalhes e não nova recuperação. Menu tipado, sessão/feedback isolados por titular e geração. **S09 PARTIAL** por operação S06/OQ-07 e limites registrados. [Auditoria, arquitetura e evidências correntes](S09-V3-INTEGRATION.md). As notas de conceitos/aprovação pendente abaixo são histórico anterior a esta autorização, não o status vigente.


Data: **2026-10-14**. Branch **growth**, base local `226fe22f`. **PARTIAL**: interface e coordenação Android implementadas; o envio operacional de claims e a recuperação de grant real ainda dependem de S06/OQ-07 e ambiente autorizado. Não é autorização de rollout, deploy ou próxima story.

## Direção vigente — conta Google primeiro; aprovação visual pendente

**V3 preferida, ajustes finais para avaliação:** textos naturais, autenticação neutra sem badge verde e quatro estados de Meu acesso (indisponível, permanente confirmado, não verificado, resposta sem grant confirmado) sobre a mesma composição. **12/12 testes físicos V3 PASS**, 12 capturas normal/1,5× inspecionadas; [galeria e classificação das fixtures](S09-ACCOUNT-CONCEPTS.md). Verde de sucesso apenas no direito legado confirmado da fixture; login não concede benefícios. Fonte/modelos/mapper funcionais intactos, sem foto real ou grant remoto comprovados. **PARTIAL; não aprovada para integração.**

**V3 híbrida para avaliação, 2026-10-15:** três protótipos isolados consolidam entrada pessoal A sem card genérico, perfil/tarefas B com identidade CalcMot e acesso sem perfil repetido/retry dominante. **6/6 testes físicos V3 PASS**, capturas normais e 1,5× inspecionadas e comparadas com A/B; [evidências V3](S09-ACCOUNT-CONCEPTS.md). Foto ausente na fixture usa o fallback do componente existente; não é prova de foto Google real. Nenhuma alteração funcional; **PARTIAL, V3 não aprovada**, aguardando decisão do proprietário.

**2026-10-15 — conceitos entregues para escolha:** A (identidade central, atalhos de gestão e resumo pessoal) e B (perfil compacto e tarefas em lista) renderizados no Samsung DEV. Seis capturas normais + seis com fonte 1,5; **12/12 testes dirigidos PASS**, 21,950 s, seguidos de inspeção visual das imagens finais. Alinhamento dos atalhos em fonte ampliada e barras do host corrigidos exclusivamente em `androidTest`. [Comparação, arquivos e limites](S09-ACCOUNT-CONCEPTS.md). Nenhuma integração funcional ou escolha automática; **PARTIAL / visual NÃO APROVADA**, aguardando decisão explícita. Os 213 arquivos protegidos permaneceram com os mesmos hashes desta retomada.

O proprietário corrigiu a hierarquia: Google Sign-In opcional → identidade/gestão da conta → informações de acesso → recuperação discreta, apenas para quem procurar. A composição funcional descrita abaixo é histórica e **não está visualmente aprovada**. Não se deve usar os testes anteriores como aprovação da direção rejeitada. [Conceitos A/B e referências observadas](S09-ACCOUNT-CONCEPTS.md) serão apresentados antes de nova alteração funcional. Nenhum contrato, direito ou gate mudou.

## Foto do perfil Google — complemento solicitado

A foto vem de `FirebaseUser.providerData` do provedor `google.com`, após o login Firebase existente, usando a [API oficial de perfil por provedor](https://firebase.google.com/docs/auth/android/manage-users#get_a_users_provider-specific_profile_information). Não há decodificação nova de JWT no aplicativo, consulta UserInfo, escopo OAuth adicional, credencial em URL/header de imagem ou mudança no Worker.

`IdentitySession.googlePhotoUrl` é metadado opcional com default, preservando os métodos S04. O ViewModel publica a imagem vinculada ao UID atual em RAM; logout e mudança de titular removem a foto anterior. O Composable recria o painter por titular/URL, evitando que uma resposta antiga pinte outra conta. Apenas URLs HTTPS sem credenciais embutidas são aceitas; URL ausente/inválida mantém o fallback.

`GoogleAccountAvatar.kt` mantém o círculo de 52 dp e o ícone Material de 28 dp existente durante carregamento/falha. Usa Coil Compose + transporte OkHttp 3.3.0, restritos à imagem da conta, com cache de disco e cache compartilhado de memória desativados para essa requisição. Não há cache personalizado, persistência em DataStore/SavedStateHandle, prefetch ou chamadas no caminho de ofertas. A máscara de privacidade existente envolve o cabeçalho, incluindo a foto. A persistência interna de metadados que o próprio Firebase SDK já faz não foi reconfigurada.

Arquivos desta solicitação: `IdentitySession.kt`, `FirebaseIdentityAuth.kt`, `AccountViewModel.kt`, `AccountScreen.kt`, novo `GoogleAccountAvatar.kt`, `app/build.gradle.kts`, `AccountViewModelTest.kt` e novo `GoogleAccountAvatarDeviceTest.kt`. Sem mudança de contratos S01, Core, navegação, regras de direito, backend ou produção. A foto não é prova de elegibilidade ou de titularidade de compra.

**Extensão preliminar preservada, validação interrompida para priorizar a aprovação dos conceitos.** Não há captura de foto real nem conclusão dos testes desta extensão nesta etapa. Os resultados físicos de composição anteriores permanecem históricos e não comprovam a imagem.

## Retomada física do refinamento — Samsung conectado

Samsung **SM-A075M / Android 15 / API 35**, pacote isolado `br.com.calcmot.identitydev`. **15/15 testes instrumentados PASS**, zero falhas, erros ou skips; `BUILD SUCCESSFUL` em 1m52s. XML em `app/build/outputs/androidTest-results/connected/identityDev/TEST-SM-A075M - 15-_app-.xml`; resumo em `.tmp/s09-refinement/device-results.json`. Os 22 testes JVM da etapa anterior não foram repetidos sem mudança de código.

As três páginas refinadas foram renderizadas no aparelho com fixtures explicitamente identificadas: `.tmp/screens/s09-refined/s09-refined-account-synthetic.png`, `s09-refined-access-synthetic.png`, `s09-refined-recovery-synthetic.png`. As capturas `s09-refined-access-large-font.png` e `s09-refined-recovery-large-font.png` comprovam os cenários físicos em viewport 320×640 dp/fontScale 1,5, incluindo rolagem até retry, recuperação, suporte e rodapé. A Conta em fonte ampliada também integra os 15 testes. É escala fornecida no teste Compose, sem alterar configuração global do aparelho.

**Inspeção e comparação com a composição anterior:** Conta preserva perfil/grupos, elimina o segundo aviso de indisponibilidade e deixa a continuação como ação de menor destaque. Meu acesso aproxima ícone/título/descrição e retry/recuperação, mantendo a hierarquia e o estado temporariamente indisponível. Recuperação passa dos parágrafos e avisos repetidos aos três blocos e um único rodapé de Core disponível. Não foram observados sobreposição, truncamento ou perda de ação nas capturas conferidas; fonte ampliada usa reflow e scroll. Nenhum ajuste adicional de código foi necessário após o teste físico.

O APK DEV refinado foi instalado apenas no pacote separado; o principal não foi instalado nem desinstalado. A fonte Kotlin permanece com o mesmo SHA-256 do começo desta validação. Fixtures não foram enviadas ao Firebase/Worker nem criaram grants. As imagens continuam no diretório temporário ignorado pelo Git.

**Ensaio real conectado concluído após o proprietário restabelecer a internet:** login pelo seletor nativo na conta já autorizada; perfil próprio recuperado; consulta e retry em Meu acesso mantiveram `BACKEND_UNAVAILABLE` (HTTP 503 pelo mapeamento existente), sem ausência de direito, grant ou protocolo inventados. Recuperação manteve o intake indisponível explicitamente e abriu o compositor do Gmail pela ação de suporte. Nenhuma mensagem foi enviada. Ao voltar: suporte → Recuperação → Meu acesso → Conta; a Conta ficou aberta no aparelho. Não houve alteração de Firebase/Worker, concessão, deploy, release ou push.

| Captura real refinada — 720×1600 px | Estado observado |
|---|---|
| `.tmp/screens/s09-refined/s09-refined-real-account.png` | Perfil Google real, uma única indicação de verificação indisponível, continuação discreta. |
| `.tmp/screens/s09-refined/s09-refined-real-access.png` | Estado 503 depois do retry, explicação agrupada e recuperação acessível. |
| `.tmp/screens/s09-refined/s09-refined-real-recovery.png` | Três blocos, suporte real e uma única mensagem sobre o Core atual. |
| `.tmp/screens/s09-refined/s09-refined-real-back-to-access.png` | Retorno da recuperação aos detalhes antes da Conta. |

As capturas nativas finais foram inspecionadas e comparadas com `.tmp/screens/s09/s09-real-{account,access,recovery}.png`; foram conferidos alinhamento, contraste, densidade, texto sem truncamento e barras do sistema. Arquivos originais sem montagem/edição; manifesto de hashes em `.tmp/screens/s09-refined/evidence-manifest.json`. As imagens com perfil real permanecem somente no diretório ignorado pelo Git, não no acervo versionado. As imagens de fixtures acima continuam identificadas como sintéticas e não comprovam concessões remotas.

Os cancelamentos iniciais aconteceram enquanto o aparelho informava ausência de rede padrão; após restaurar a conexão, o login funcionou sem alteração do código. Não há fundamento para registrar regressão de autenticação nesta validação. **S09 segue PARTIAL por operação S06/OQ-07 e recuperação E2E de grant real**, não por falta das três capturas ou da execução dos 15 casos físicos. TalkBack e restauração com grant real em outro aparelho continuam fora das evidências desta retomada.

## Histórico — implementação e compilação do refinamento sem ADB

Esta seção prevalece sobre a descrição visual e as capturas anteriores abaixo. O proprietário pediu para concluir o que fosse possível sem o aparelho e deixou a validação física para a próxima sessão. **Não existem novas capturas nem inspeção física do layout refinado nesta execução.** Os 10 testes físicos e screenshots anteriores pertencem ao layout anterior, não validam automaticamente este ajuste.

| Página | Refinamento implementado | Preservação |
|---|---|---|
| Conta | A falha de verificação já visível em Meu acesso não é repetida abaixo das ações. Feedback explícito de login/logout/colisão continua disponível. “Continuar no CalcMot” virou rodapé centralizado e discreto na conta conectada. | Perfil, três grupos, callbacks e estado real permanecem. Sem inferir perda de direito a partir de 503. |
| Meu acesso | Ícone, título e descrição formam um grupo: 12 dp entre ícone e texto, 8 dp entre título e explicação. Grupos separados por 16 dp; retry e recuperação ficam no mesmo grupo, com 4 dp entre alvos de pelo menos 48 dp. Antes, cada elemento tinha separação geral de 24 dp. | Mesma apresentação S08, mesmo retry e navegação, sem vermelho ou linguagem de revogação para falha temporária. |
| Recuperação | Três blocos de texto sem novos cards: “Quem pode solicitar”, “Como funciona”, “Próximo passo”. Frases menores, Google não comprova histórico, análise não garante o direito. Intake indisponível mantém suporte real. Um único rodapé: “Você pode continuar usando o Core atual.” | Nenhum envio ou grant fictício. Falha de uma consulta anterior de acesso não é apresentada como falha de uma solicitação que não foi enviada. Feedback de ação e de consulta de protocolo existente permanece, sem repetir o rodapé. |

Somente `AccountScreen.kt` mudou no código do aplicativo. Alterações de apoio: `AccountPresentationTest.kt`, `AccountScreenDeviceTest.kt`, este relatório, referências visuais, BACKLOG e EXECUTION-PLAN. ViewModel, Firebase Auth, navegação, S01–S08, backend e fontes de direitos não foram alterados. Botão/assets Google, tipografia, insets e tokens foram preservados. O helper `RecoveryBlock` é apenas uma Column com título e conteúdo; não introduz componente de infraestrutura.

Foram preparados previews das três páginas, com dados sintéticos identificados, incluindo Meu acesso e Recuperação em 320×640 dp/fontScale 1,5. **Definições de Preview compiladas não equivalem a imagens renderizadas.** A instrumentação passa a ter 15 casos: os 10 anteriores e cinco verificações de resumo sem duplicação, três blocos/ação de suporte, retry e fontes ampliadas de acesso/recuperação. Os testes preservam o marcador de dados sintéticos e os callbacks reais da rota.

Capturas anteriores continuam apenas em `.tmp/screens/s09/`, ignorado pelo Git. Não foram adicionadas imagens com email real ou dados pessoais ao repositório. Quando o Samsung estiver disponível, executar a instrumentação documentada abaixo, conferir os cinco casos novos, capturar as três páginas e comparar com `s09-real-{account,access,recovery}.png`. O teste produz `s09-refined-{account,access,recovery}-synthetic.png` e capturas de fonte ampliada; essas imagens ainda não foram geradas nesta execução.

O Figma existente foi reaberto no navegador integrado e enquadrado em visão geral. A interface apresentou modo público/convite de cadastro, sem comprovação de sessão autenticada nem acesso a medidas de camadas. Não houve novo login, edição de arquivo, importação de assets ou nova referência visual. O ajuste segue o feedback sobre as capturas do CalcMot, não um novo design Figma.

**Validação desta continuação:** `BUILD SUCCESSFUL` em 2m27s; `testIdentityDevUnitTest --tests 'br.com.calcmot.account.*'` executou **22 testes PASS**, 8 apresentação/textos e 14 ViewModel, zero falhas/erros/skips. XMLs em `app/build/test-results/testIdentityDevUnitTest/`; resumo em `.tmp/s09-refinement/jvm-results.json`. `assembleIdentityDev` e `assembleIdentityDevAndroidTest` também passaram. O APK arm64 DEV e o APK da instrumentação estão em `app/build/outputs/apk/identityDev/app-arm64-v8a-identityDev.apk` e `app/build/outputs/apk/androidTest/identityDev/app-identityDev-androidTest.apk`. Nenhum foi instalado nesta continuação.

**15 casos de UI compilados, não executados.** Retry/suporte, reflow/fontScale 1,5 e navegação têm casos preparados, mas ainda não foram comprovados fisicamente neste layout. Os novos testes JVM garantem que feedback conciso mantém o erro, não inventa ausência/concessão de direito e não duplica a disponibilidade do Core. Não se recontam os testes físicos antigos como aprovação deste refinamento.

Comparação SHA-256 de 220 arquivos de main/backend/migrations/contratos/configuração e instrumentação contra o início desta continuação: somente `AccountScreen.kt` e `AccountScreenDeviceTest.kt` mudaram nesse conjunto; os sete contratos, fontes de S01–S08 e Core permanecem idênticos. O teste JVM de apresentação e os documentos também foram atualizados. `git diff --check` e links locais dos documentos foram conferidos. **S09 permanece PARTIAL**, tanto pela operação S06/OQ-07 quanto pela validação visual deste refinamento ainda pendente. Sem instalação, deploy, push, release ou contratação.

## Escopo entregue

- **Revisão visual corrente:** referências reais inspecionadas em [S09-VISUAL-REFERENCES.md](S09-VISUAL-REFERENCES.md). Três páginas na rota existente: Conta, Meu acesso, Recuperação/acompanhamento. TopAppBar nativa, grupos ListItem, perfil próprio quando disponível, estados separados de ausência de consulta e falha 503. A composição inicial de botões em sequência foi substituída. Evidências da primeira interface abaixo são históricas quando divergirem desta revisão.
- **Estado e lifecycle:** `AccountViewModel` reutiliza S04/S08 e observa StateFlow; a rota usa `collectAsStateWithLifecycle`. Sem DI framework ou novo módulo. O modelo não retém Activity nem token; o host de login é passado apenas durante o gesto explícito. Firebase notifica ao registrar listener: agora uma notificação do mesmo UID reavalia o estado sem descartar cache/erro; uma troca real continua limpando sincronicamente o titular anterior.
- **Dados apresentados:** nome/email vêm do perfil Firebase da própria sessão, quando disponíveis, sem armazenamento adicional. Avatar usa a foto do perfil Google quando disponível, com fallback Material conforme a seção de foto acima. Perfil e protocolo usam o modifier oficial `clarityMask` somente nesta UI e nas APIs suportadas; defaults da telemetria e pipelines não foram alterados. O SDK está desligado no DEV, portanto não alegamos validação de replay remoto da máscara. [API oficial](https://learn.microsoft.com/en-us/clarity/mobile-sdk/clarity-compose-sdk).
- Entrada voluntária **Configurações → Conta e acesso**, em Compose/Material 3 e tokens do CalcMot, escura e com scroll. Login não é requisito para voltar ou continuar utilizando o Core.
- Google Sign-In explícito pelo `GoogleIdentityClient` S04, sem novo provedor ou OAuth. A sessão já vinculada usa reautenticação; conta diferente exige saída explícita, sem merge por email ou transferência de direitos.
- Consulta/retry por `AndroidEntitlements` S08, `GET /v1/me/entitlement` S01, somente na experiência de conta. O resumo usa a avaliação local do repositório; não concede direitos nem interpreta login como prova de elegibilidade.
- “Acesso gratuito permanente” somente com `legacyCore` confirmado para a identidade corrente, conforme composição solicitada pelo proprietário. Copy limita a preservação ao Core gratuito até 3.5; não inclui automaticamente recursos novos. Direitos temporários continuam sujeitos à política temporal S08, independentemente da UI. 503/offline nunca são apresentados como resposta válida sem direito.
- Estados de cancelamento, offline, sessão/401, 503, inconsistência, configuração indisponível e colisão. Todos mantêm saída/continuação; não acrescentam gate ao Core.
- Saída com confirmação, Firebase signOut e limpeza do estado do Credential Manager. A UI e o repositório abandonam o titular anterior; o cache continua particionado por projeto/UID para restauração legítima da mesma conta.
- Protocolo e estado da revisão a partir do snapshot S01 ou da resposta de uma implementação autorizada de intake. `GRANTED` na revisão **não** basta para a UI exibir Legacy Core: exige confirmação no entitlement. Revisão não concede Premium nem acesso provisório.
- Preparação estrita de `POST /v1/legacy/claim`: `Idempotency-Key`, corpo apenas `evidenceType=USER_DECLARATION`, `evidenceHash=null`; sem UID, versão ou data local como prova. Parser da view S01 rejeita campos extras/estados/datas inválidos. A declaração não concede grant.

## Limite explícito do intake

`UnavailableLegacyRequests` mantém o envio desligado. Não há POST de claim, upload de evidências ou protocolo fictício neste build. A página de recuperação explica a indisponibilidade antes de qualquer envio e oferece **Falar com suporte**, com ação real. O CTA Solicitar análise só aparece quando a implementação de intake estiver disponível; o parser/porta permitem a integração posterior sem reinventar S06. Não há confirmação modal de pedido quando não existe envio operacional.

O backend S06 está implementado localmente, mas operador real e procedimento de coleta/retenção OQ-07 ainda não estão comprovados/aprovados. O Worker DEV remoto não foi atualizado nesta execução. Portanto não há fundamento para apresentar envio, revisão humana ou recuperação de grant real como concluídos. O contato abre um aplicativo de email por ação do motorista; o CalcMot não envia automaticamente mensagens nem evidências. Se o aplicativo de email não puder abrir, o contato é exibido em diálogo selecionável, sem impedir o retorno ao Core.

O contrato não oferece GET separado da claim: a consulta de revisão existente usa `legacyReview` do entitlement; uma resposta futura do POST também pode fornecer a view. Não foi inventada nova rota nem prazo de atendimento/provisório.

## Ambientes e preservação

`ACCOUNT_API_BASE_URL` é vazio por padrão, inclusive no release. Apenas `identityDev` recebe a propriedade explícita `calcmot.identityDevWorkerUrl`, validada contra o host DEV já autorizado. Nenhuma URL DEV foi instalada como default de produção. Firebase e backend não receberam configurações novas.

A Activity hospedeira DEV é interna e usa `br.com.calcmot.identitydev`, sem bootstrap do Core ou telemetria. O acesso à tela parte de um botão no launcher de diagnóstico existente. O teste de navegação usa preferências desse pacote e restaura o valor anterior; não liga serviço de monitoramento.

Sete contratos S01 preservados por SHA-256 contra o baseline anterior à S09. Backend, migrations, manifest principal, pipelines, cálculos e overlay não foram alterados nesta story. Não houve bloqueio comercial, Billing, trial automático, paywall, deploy, push, publicação ou contratação. Versões do app preservadas.

## Arquivos desta story

| Arquivo/grupo | Mudança S09 |
|---|---|
| `app/src/main/java/br/com/calcmot/account/AccountScreen.kt` | UI, confirmações, mensagens, previews e semântica. |
| `app/src/main/java/br/com/calcmot/account/AccountRoute.kt` | Binding por entrada voluntária, lifecycle da tela e suporte. |
| `app/src/main/java/br/com/calcmot/account/AccountViewModel.kt` | Ações serializadas, identidade, cache, perfil e estados informativos com lifecycle. |
| `app/src/main/java/br/com/calcmot/account/AccountPresentation.kt` | Mapeamento de estados confirmados/aguardando/indisponíveis para copy, sem regra nova de concessão. |
| `app/src/main/java/br/com/calcmot/account/LegacyClaimContract.kt` | Preparação S01; intake indisponível explicitamente. |
| `app/src/main/java/br/com/calcmot/ui/CalcMotNavigation.kt`, `ui/HomeScreen.kt` | Rota e entrada nas configurações existentes. |
| `app/src/main/java/br/com/calcmot/identity/{IdentitySession,IdentityCoordinator,FirebaseIdentityAuth,GoogleIdentityClient}.kt` | Saída segura usando SDKs existentes e metadados opcionais do próprio perfil. Assinaturas de criação/login S04 preservadas; novos campos têm defaults. Credential Manager usa applicationContext, sem reter Activity na configuração da tela. |
| `app/src/main/java/br/com/calcmot/entitlement/{EntitlementSnapshot,EntitlementRepository,EntitlementCodec,AndroidEntitlements}.kt` | Exposição local do protocolo já presente em S01, parser UTC compartilhado e factory de wiring. Sem mudança no wire/cache S01. |
| `app/build.gradle.kts` | Base URL explícita e vazia por padrão; integração oficial lifecycle-viewmodel-compose na versão já utilizada pelo lifecycle. |
| `app/src/identityDev/java/br/com/calcmot/debug/{AccountDevActivity,AccountUiTestActivity}.kt` | Host DEV interno e host para testes de UI. |
| `app/src/identityDev/java/br/com/calcmot/debug/IdentityProbeActivity.kt`, `app/src/identityDev/AndroidManifest.xml` | Botão DEV e registro das duas Activities internas, sem novas permissões. |
| `app/src/test/java/br/com/calcmot/account/{AccountViewModelTest,AccountPresentationTest}.kt`, `identity/IdentityCoordinatorTest.kt` | Coordenação/isolamento, preparação S01, estados e regressões S04 de cancelamento, vínculo, reautenticação, colisão e logout. |
| `app/src/main/res/drawable-nodpi/google_signin_neutral_icon.png`, `res/font-v24/google_sans_medium.ttf`, `app/src/main/assets/licenses/google-sans-OFL.txt` | Asset Google oficial inalterado e tipografia licenciada somente para o botão, sem dependência visual externa. |
| `app/src/androidTest/java/br/com/calcmot/account/AccountScreenDeviceTest.kt` | UI física, estados sintéticos, navegação e screenshots. |
| Este relatório, `S09-VISUAL-REFERENCES.md`, `BACKLOG.md`, `EXECUTION-PLAN.md` | Referências, evidências e status corrente. |

Os demais arquivos já modificados/untracked no workspace pertencem às stories anteriores e foram preservados.

## Validação e evidências da revisão anterior ao refinamento

As evidências de S04 e S08 anteriores não são contabilizadas como novos testes S09. Estados sintéticos renderizados em aparelho não são prova de concessão real no backend.

### JVM

Primeira seleção da revisão visual: **100 testes PASS**, incluindo **19 S09** e regressões pertinentes de identidade, entitlement e readiness. Após os ajustes finais de perfil/logout, **32 testes focados PASS: 20 de conta + 12 de identidade** (6 apresentação, 14 ViewModel, 10 coordinator, 2 erros Firebase). São execuções parcialmente sobrepostas, não 132 testes distintos. Não foi repetida a suite completa de 299 por variante.

- Construção/abertura sem conta não fazem rede.
- Login com 503, cancelamento e colisão não inventam nem transferem grant.
- Logout + conta B não herda cache de A; relogin A recupera seu cache sob 503.
- Offline/401/503 preservam Legacy Core confirmado e o Core atual.
- Intake desligado não envia pedido; review GRANTED não se converte em entitlement.
- Resposta atrasada de outra identidade é descartada; ações concorrentes durante login não se acumulam.
- Body/headers/parser de claim respeitam S01 e rejeitam resposta adulterada.

Resultados XML em `app/build/test-results/testIdentityDevUnitTest/`. Resumos em `.tmp/s09-baseline/jvm-visual-results.json` (100) e `jvm-final-focused-results.json` (32 finais). A seleção inicial de 93 permanece histórica, sem contagem duplicada. A fonte dos grants desses testes é sintética em memória, nunca Firebase/D1 remoto. Os testes S04 verificam preservação da sessão em cancelamento/colisão, link sem troca de UID, reautenticação e logout explícito; os novos campos de perfil não alteram essas regras.

### Aparelho

Samsung **SM-A075M**, Android 15/API 35, pacote DEV isolado. **10/10 testes Compose PASS**, zero falhas/erros/skips na execução final (Gradle BUILD SUCCESSFUL, 3m45s). A passagem anterior de 9 também passou. A primeira passagem teve 5/6 PASS; a falha foi a asserção do prefixo “Protocolo:”, corrigida no teste. Uma tentativa intermediária de captura por UiAutomation conflitou com o controlador do dispositivo; a captura de teste passou a usar PixelCopy, sem alterar serviços/permissões. Uma chamada Gradle sem aspas nas propriedades PowerShell não encontrou a task; a execução final usa os argumentos reproduzíveis abaixo. Nenhum desses ensaios emitiu direitos.

| Teste físico automatizado | Resultado |
|---|---|
| Login opcional e continuar sem conta | PASS; callbacks e navegação acessíveis. |
| 503 sem LegacyGrant | PASS; mensagem e retorno disponíveis. |
| Legado confirmado sintético | PASS; copy permanente limitada ao Core, sem Pro automático. |
| Logout confirmado / solicitação voluntária | PASS; cancelar logout não executa a ação; pedido só por CTA explícito na recuperação. |
| Viewport 320×640 dp / fontScale 1,5 | PASS; scroll alcança ações e botão voltar possui descrição. |
| Configurações → conta → voltar | PASS; preferência de monitoramento DEV preservada e restaurada, sem serviço. |
| Revisão pendente / protocolo copiável | PASS; feedback de cópia e nenhum grant inventado. |
| Claim favorável sem entitlement confirmado | PASS; aguarda confirmação, sem apresentar direito concedido. |
| Conta → detalhes → recuperação → voltar/continuar | PASS; três páginas na mesma rota. |
| Recuperação aberta dos detalhes → voltar | PASS; retorna aos detalhes antes de voltar à conta. |

XML final: `app/build/outputs/androidTest-results/connected/identityDev/TEST-SM-A075M - 15-_app-.xml`. Screenshots físicos inspecionados em `.tmp/screens/s09/`: `s09-signed-out.png`, `s09-backend-unavailable.png`, `s09-legacy-synthetic.png`, `s09-large-font-small-screen.png`. Estados de grant/revisão dessas imagens são fixtures de renderização, não grants remotos.

### Ensaio manual final com Firebase/Worker DEV reais

APK final arm64 instalado exclusivamente como `br.com.calcmot.identitydev`, preservando o pacote da Play e sem alterar versionCode/versionName. As três páginas foram percorridas com a conta de teste do proprietário já autorizada. Não houve novo consentimento ou configuração Firebase/Cloudflare.

| Fluxo no APK final | Evidência / limite |
|---|---|
| Abrir sem sessão | “Proteja seu acesso ao CalcMot”, botão Google e continuação opcionais. Sem abertura automática do seletor. |
| Abrir seletor Google e cancelar | “Login cancelado”; continua sem sessão, com retorno/continuação disponíveis. |
| Entrar na conta autorizada | Perfil próprio e “Conta Google conectada”; seletor nativo Credential Manager. |
| Consultar Worker DEV | “Não foi possível verificar agora” / serviço indisponível, mapeamento `BACKEND_UNAVAILABLE`/HTTP **503** do repositório real. Sem grant, Premium ou protocolo fictício. Não foram consultados logs remotos nem medido CPU nesta story. |
| Meu acesso → Recuperação → Voltar → Voltar | Retorna primeiro aos detalhes e depois à Conta; também comprovado no teste instrumentado final. |
| Recuperação sem intake operacional | “O envio pelo aplicativo ainda não está disponível”; ação real de suporte, sem POST nem alegação de pedido recebido. |
| Confirmar logout pela UI | “Você saiu desta conta”; cabeçalho, perfil e direitos da sessão anterior não permanecem na tela. |
| Relogin da mesma conta | Login explícito disponível novamente após logout; recuperação de grant remoto não é alegada. |

### Capturas reais finais e inspeção visual

Samsung **SM-A075M / Android 15 / 720×1600 px**, pacote **identityDev**. Capturas nativas sem montagem nem edição, salvas em `.tmp/screens/s09/` (temporário ignorado pelo Git):

| Página real | Arquivo | Estado real |
|---|---|---|
| Conta | `s09-real-account.png` | Perfil Google próprio e serviço de direitos indisponível. |
| Meu acesso | `s09-real-access.png` | Verificação indisponível; Core continua disponível, sem inferir ausência de legado. |
| Recuperação | `s09-real-recovery.png` | Intake indisponível explicitamente, com ação de suporte. |
| Conta desconectada | `s09-real-account-signed-out.png` | Login voluntário e uso sem conta. |
| Cancelamento | `s09-real-account-canceled.png` | Retorno do seletor sem login. |
| Logout | `s09-real-account-logged-out.png` | Sem perfil da identidade anterior. |

Capturas finais conferidas visualmente: contraste de texto/superfície escura, alinhamento de ícones/linhas, CTAs, hierarquia, ausência de texto truncado e insets das barras do sistema. O retorno de Meu acesso após a recuperação está em `s09-real-back-to-access.png`. Capturas da janela de testes não incluem necessariamente as barras de sistema; a entrega das três páginas usa o aparelho real com barras visíveis. Quando a ferramenta de captura não apresentou um frame completo, a conferência utilizou `adb shell screencap -p` nativo e verificou os arquivos originais; não se montaram elementos faltantes.

Legado confirmado, revisão em análise e aprovação aguardando emissão também foram renderizados no Samsung, **exclusivamente com fixtures em teste**, identificadas no próprio conteúdo: `s09-legacy-synthetic.png`, `s09-review-synthetic.png`, `s09-awaiting-grant-synthetic.png`. São screenshots reais de UI sintética; não comprovam grants remotos. A captura `s09-large-font-small-screen.png` usa viewport 320×640 dp e fontScale 1,5 por CompositionLocal no teste físico. Houve conferência de reflow e rolagem até as ações; não altera a fonte global do aparelho. Não se alega cobertura de todos os fatores de escala.

A primeira composição S09 teve ensaio real de retry, suporte abrindo email sem enviar mensagem, relogin com digest local de UID igual e persistência após encerrar/reabrir somente DEV. Esses ensaios são históricos, não novos testes da revisão visual. Capturas antigas `s09-real-signed-in.png`, `s09-real-signed-out.png` e `s09-real-canceled.png` não representam o layout final.

Colisão/401/offline/troca A→B/recuperação de cache usam testes JVM determinísticos nesta revisão. Não foi escolhida uma segunda conta real sem intervenção do proprietário. Reinstalação/aparelho novo com grant real, expiração natural, TalkBack e uso em condução **não** foram executados. Não houve alteração de rede/permissões/fontes globais nem monitoramento ativo durante o ensaio.

Comando reproduzível (PowerShell, JDK do Android Studio configurado em JAVA_HOME):

```powershell
./gradlew.bat :app:connectedIdentityDevAndroidTest `
  '-Pcalcmot.identityDevInstrumentation=true' `
  '-Pcalcmot.identityDevWorkerUrl=https://calcmot-api-dev.eduardoangelo20001.workers.dev' `
  '-Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.account.AccountScreenDeviceTest' `
  --no-daemon --max-workers=2 --console=plain `
  '-Pkotlin.compiler.execution.strategy=in-process' --no-problems-report
```

## Pendências para DONE

1. Operação S06/OQ-07 autorizada, intake efetivo e recuperação E2E de grant confirmado, incluindo chegada tardia e mesma identidade após reinstalação/aparelho novo. Não criar grant comercial para passar teste.
2. Completar os ensaios reais S09 que não estão comprovados acima; seleção de segunda conta/novo consentimento depende do motorista. As limitações de OAuth/distribuição Play e segundo aparelho já descritas em S04 permanecem válidas.
3. Validar TalkBack real e a experiência completa de recuperação; sem habilitar permissões/serviços automaticamente no aparelho.
4. Produção continua sem endpoint autorizado. Eventual rollout/configuração deve ser autorizado separadamente, depois das dependências operacionais.

Não iniciar S10 ou outras stories automaticamente. OQ-02 comercial não é reaberta; OQ-03/OQ-10 aprovadas permanecem válidas e gates comerciais OFF. A tela informativa de conta não usa OQ-10 como condição de apresentação.

## Referências

S09 do [backlog](BACKLOG.md), [plano](EXECUTION-PLAN.md), PRD v5 §§3/7, [S01](../contracts/README.md), [S04](S02-S04-INTEGRATION.md), [S06](S06-LEGACY-RECOVERY.md), [S07](S07-ENTITLEMENT-PROJECTION.md), [S08](S08-ENTITLEMENT-CACHE.md) e [OQ-02](OQ-02-LEGACY-POLICY.md).

A limpeza da sessão do provedor no logout segue [Firebase Google Auth](https://firebase.google.com/docs/auth/android/google-signin) e [Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation). Não foi trocado o stack de autenticação existente.

