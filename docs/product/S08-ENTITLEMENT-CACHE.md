# S08 — Cache Android e operação offline

Data: **2026-10-14**. Branch `growth`, trabalho anterior preservado. **PARTIAL por integração/validação técnica**; OQ-03 e OQ-10 foram aprovadas pelo proprietário na execução atual. Nenhum bloqueio comercial foi conectado ao Core. Requisitos originais ENT-003/004, NFR-002/005/008, PRD §§11.3–11.4; S01 INV-01–04. Não inicia S09.

## Execução corrente — política aprovada, gates OFF

[Política vigente](OQ-03-OQ-10-APPROVED-POLICY.md) separa decisão comercial encerrada das pendências técnicas. As seções anteriores de proposta/implementação abaixo são históricas e não prevalecem sobre esta atualização.

- `OfflinePolicy.APPROVED_V1`: paid 72h / trial 24h simultaneamente, promo 24h. Play usa só `play.checkedAt`; timer nunca estende `validUntil`. Review `DISABLED`, Legacy Core independente.
- `PromoVerifier` recebe confirmação de fonte/titular/período; timestamp local `promoVerifiedAt` protegido no envelope v2. Não usar `checkedAt` raiz nem GET como substituto dessa confirmação. Port sem implementação real retorna ausência, promo fica em revalidação. Cache v1 continua legível; nenhum timestamp inventado durante migração.
- `SafeEntryObservation.kt`: StateFlow de observação inteira por sessão/geração, ack de conclusão, trabalho em trânsito e overlay. Gerações antigas/sinais ausentes/contradições não qualificam; origem precisa ser ação voluntária com foco. `commercialGatesEnabled=false`. Nenhum produtor/consumidor real ou hook ao Core foi conectado.
- **Bloqueios técnicos concretos:** fonte promo autoritativa e adapter de verificação não implementados nas stories anteriores; Snapshot atual do runtime não oferece ack de drenagem/término consistente. Produzi-lo exige observação do ciclo de trabalho no pipeline crítico, que não alteramos sem integração específica. Não inferir por IDLE/toggle/resume. Restore/entrada segura real e concorrência de apresentação ainda precisam de ensaio.

Arquivos desta continuação: alterados `EntitlementSnapshot.kt`, `EntitlementRepository.kt`, `AndroidEntitlements.kt`, `EntitlementRepositoryTest.kt` e `EntitlementStoreDeviceTest.kt`; criados `SafeEntryObservation.kt`, `SafeEntryObservationTest.kt` e política aprovada; atualizados este relatório, proposta histórica, BACKLOG e EXECUTION-PLAN. Sem SQL, backend, contratos ou UI comercial alterados. OQ-07 segue proposta não aprovada; metadado de recência integra a futura revisão de privacidade, sem exclusões reais.

**Resultados finais atuais: 47 testes específicos JVM PASS, zero failures/errors; 2 testes físicos Samsung SM-A075M/API 35 PASS, zero failures/errors.** JVM: repository 33, codec 4, observador real 4, modelo de proposta 6. A suite inteira de 299 não foi repetida. Mudança de política, review e persistência justificou reexecutar somente esse pacote.

No hardware: cache v1/v2 legível; campo promocional desconhecido de v1 descartado mesmo com HMAC sintético válido, migração sem concessão; adulteração rejeitada; boot real do processo consultado; cache de boot antigo controlado exige revalidação; paid com verificação antiga expira conforme 72h; UID diferente não recebe cache e UID original recupera Legacy Core. Dados, fonte e Auth desse segundo teste são sintéticos; **não foi reboot físico, conta Google real ou concessão comercial**. Nenhum comando modificou o pacote de produção. Nenhuma falha pendente dos testes finais; endurecidas as guardas de replay após desconexão e de v1 sem proveniência promocional.

Reprodução do recorte final:

```powershell
./gradlew.bat :app:testIdentityDevUnitTest --tests 'br.com.calcmot.entitlement.*' :app:connectedIdentityDevAndroidTest '-Pcalcmot.identityDevInstrumentation=true' '-Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.entitlement.EntitlementStoreDeviceTest' --no-daemon --max-workers=2 --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

JUnit atual em `app/build/test-results/testIdentityDevUnitTest/` e `app/build/outputs/androidTest-results/connected/identityDev/`. Sete contratos e fontes/migrations backend, assim como arquivos anteriores do Core (`app/src/main` antes de S08), mantiveram hashes. `git diff --check` e links dos documentos PASS. Sem deploy, push, release de produção, Billing, S09, paywall, direito real, serviço pago ou exclusão de dados pessoais. S08 **PARTIAL**, com política aprovada e infraestrutura preparada; integração de origem promo/ack e validação real de entrada/restauração ainda pendentes.

## Continuação — aparelho e propostas OQ-03/OQ-10

Samsung **SM-A075M, Android 15/API 35** conectado: `EntitlementStoreDeviceTest` físico **1 PASS, 0 failures/errors**, exclusivo `identityDev`. Keystore/HMAC, AtomicFile, persistência entre instâncias do store, isolamento por UID e adulteração realmente testados. XML em `app/build/outputs/androidTest-results/connected/identityDev/TEST-SM-A075M - 15-_app-.xml`. Não houve correção necessária do cache nem comando de instalação/desinstalação do pacote principal. Sem reboot/morte de processo/revalidação Google real neste ensaio.

**8 testes novos de proposta PASS**, execução filtrada, sem repetir os 299 já comprovados: 2 repository para 72h pago/24h trial, promo 24h/expiry menor, e 6 de entrada segura em modelo exclusivamente de teste. Não são uma política instalada nem ensaio de UX sobre Uber/99. Nenhum código de produção mudou nesta continuação; contratos preservados. Artefato novo: [proposta para aprovação e riscos](OQ-03-OQ-10-OFFLINE-SAFE-ENTRY-PROPOSAL.md); alterado teste repository e criado `SafeEntryProposalTest.kt`, além destes documentos/BACKLOG/EXECUTION-PLAN.

**Pendências reais:** OQ-03/OQ-10 não aprovadas; limite Play atual não distingue pago/trial simultaneamente; recência de promo/`checkedAt` da projeção precisa de semântica confiável; não existe ack consistente de encerramento/drenagem de sessão nem origem voluntária de entrada. Ensaio de relógio/boot físico e integração de recuperação continuam adicionais. Gates permanecem OFF. O histórico abaixo registra a entrega inicial, quando não havia aparelho ADB.

## Componentes e limites

- [EntitlementRepository](../../app/src/main/java/br/com/calcmot/entitlement/EntitlementRepository.kt): `StateFlow` em RAM, restore e refresh explícitos em dispatcher IO, mutex para operações concorrentes, epoch de sessão contra respostas atrasadas inclusive troca A→B→A. UID vem do Auth existente, projeto da configuração Firebase; nenhum parâmetro de chamada escolhe outro titular. Conta/projeto distintos não compartilham cache.
- [Modelo](../../app/src/main/java/br/com/calcmot/entitlement/EntitlementSnapshot.kt): distingue grant permanente, estado Play/fase trial ou período pago, promo, revisão finita e falha operacional. `PENDING`, `UNKNOWN`, ausência de fonte, erro e vencimento não viram compras ou trials.
- [Codec](../../app/src/main/java/br/com/calcmot/entitlement/EntitlementCodec.kt): valida forma S01, tipos, datas UTC, versão, revisão, fontes e união no `serverNow`. Rejeita snapshot inconsistente, regressão e alterações de fonte sem revisão. Revisão igual com novo `serverNow` e normalização por expiração é aceita: a S07 não incrementa revisão só pela passagem do tempo.
- [Adaptadores Android](../../app/src/main/java/br/com/calcmot/entitlement/AndroidEntitlements.kt): Firebase Auth existente, GET HTTPS canônico sem redirecionamento, bearer só em memória, corpo limitado tecnicamente a 64 KiB, sem logs de tokens/UID. Não há POST de entitlement, endpoint administrativo, SDK Billing, serviço externo novo ou configuração de produção.

`AndroidEntitlements` prepara lifecycle de abertura/retorno por Activity e listener Auth, sem rede em services. A instância **não foi criada no Application ou navegação publicados**: requer ambiente/base URL explícitos. Não existe URL de produção default, job periódico, retry automático nem ligação com pipelines. Após criação futura autorizada, abrir/retornar chama restore/refresh; operações explícitas de conta também podem chamar o repositório. Fechar a instância cancela listeners/temporizadores. Não implementar UI comercial/conta nesta story.

`StateFlow` é observação com titular explícito; valores antigos capturados por uma UI não são autorização. Um futuro consumidor de direitos deve obter `current()`, que confere identidade e tempo novamente em RAM. O listener limpa o estado na troca de conta e as respostas em trânsito reconferem identidade/epoch antes da persistência/publicação. A leitura de boot via Settings ocorre uma única vez na construção do relógio Android, não a cada avaliação.

## Persistência e recuperação

Um arquivo por hash de `(projeto, UID)` em `noBackupFilesDir/entitlements-v1`, com escrita `AtomicFile`. Payload versionado contém projeto/UID, boot e o objeto **EntitlementCache S01**: `snapshot`, `cachedAtElapsedRealtime`, `policyVersion`. Não modifica o schema congelado nem adiciona campos ao snapshot HTTP. Não grava ID token, email, conta Google ou evidência bruta de suporte.

HMAC SHA-256 com chave Android Keystore protege integridade local do envelope e vínculo ao titular/âncora. É proteção local, **não assinatura do servidor**, prova histórica ou defesa absoluta contra aparelho com root/processo comprometido. Snapshot é gerado pelo backend e recebido por HTTPS conforme PRD/S01; não foi inventada uma assinatura JWS no protocolo.

Arquivo inválido, chave perdida e versão de armazenamento não suportada não concedem acesso. Refresh autenticado pode recuperar do servidor e substituir atomicamente o cache somente após um 200 válido. Falha/503 preserva o cache confirmado ainda disponível em RAM e nunca aciona revogação do Core atual. Não há exclusão de registros pessoais implementada.

Após morte do processo, mesmo projeto/UID restaura o arquivo válido. Reinstalação perde arquivo e chave: a mesma identidade precisa recuperar o grant autoritativo pela S07, sem criar outro. Sem conta recuperável ou servidor disponível, não prometer recuperação perfeita do comprovante local; Core atualmente gratuito permanece fora de qualquer gate. Outros UIDs não herdam direito nem recebem transferência automática.

OQ-07 continua **proposta não aprovada**. Cache contém identificadores pessoais/metadados de direitos e deverá integrar o procedimento S15 de retenção/exclusão. Não foi aprovado TTL de dados, limpeza por inatividade ou retenção infinita. Não confundir durabilidade do direito com autorização de retenção de todos os dados.

## Tempo e fontes

- `serverNow + (elapsedRealtime atual − âncora)` avalia períodos sem relógio civil. Âncora usa início da requisição: incluir todo RTT é conservador, não acrescenta tempo comprado pela latência.
- Boot é conferido por `Settings.Global.BOOT_COUNT`. Boot diferente/desconhecido, regressão monotônica ou perda da âncora exigem revalidação de fontes temporárias. Não há tolerância de reboot inventada. Grant permanente independe dessas datas.
- Grant confirmado nunca é convertido em Pro nem revogado por 401/503, falta de internet, revisão inferior ou snapshot posterior sem o mesmo grant. Resposta contraditória é `INCONSISTENT` e preserva a confirmação anterior daquele titular.
- Fonte Play candidata exige ACTIVE/CANCELED_WITH_ACCESS/GRACE e prazo não vencido; fase UNKNOWN não autoriza novo Pro. Promo respeita início/fim. Review só usa `provisionalUntil` autoritativo já existente, sem criar sete dias, e nunca concede Pro.
- Mesmo após uma eventual política aprovada, acesso temporário fica limitado ao período da fonte e à idade máxima de verificação. Para Play, usa **`play.checkedAt`**, não a hora do GET: repetir S07 não revalida uma compra na Play. Promo/review usam `checkedAt` da fonte/projeção. Nenhum refresh renova um período comprado ou inicia trial.
- Temporizador acorda apenas nos limites conhecidos e recalcula **em RAM**, sem rede/Auth/disco. `StateFlow` não mantém Pro indefinidamente aguardando uma mudança de revisão ou polling do consumidor. Reavaliação explícita também é sem I/O; nenhuma dessas operações está conectada a oferta/overlay.

`elapsedRealtime` inclui suspensão e reinicia no boot; `AtomicFile` exige sincronização externa, feita no store. Referências oficiais consultadas: [SystemClock](https://developer.android.com/reference/android/os/SystemClock), [BOOT_COUNT](https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT), [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile). Nada disso é relógio criptograficamente seguro.

## OQ-03 e OQ-10 — decisões ainda necessárias

**Nenhuma política temporal foi aprovada/instalada.** `OfflinePolicy` é parâmetro técnico sem valores default; no adaptador Android é nulo e o cache registra `OQ-03-PENDING`. Fonte temporária plausível fica `POLICY_REQUIRED`, `proAllowed=false`; expirou, fica `EXPIRED`. Isso não limita o Core atual nem o grant permanente. As durações dos testes são explicitamente sintéticas, não recomendação ou aprovação comercial.

Falta aprovar:

1. Idade máxima de revalidação/tolerância finita por trial, assinatura/estado Play, promo e eventual review; decidir se existe extensão além de expiry. A implementação atual **não estende expiry**.
2. Tratamento de reboot/perda da âncora e discrepância de relógio, recuperação prioritária de compra em verificação e entrega/versionamento de política confiável. Não habilitamos Remote Config.
3. OQ-10: sinais e definição verificável de entrada segura/oferta recente, adiamento de restrição e limite final aprovado. Sem essa definição não foi ligado bloqueio comercial, prompt, paywall ou suspensão do monitoramento.

O objeto de política preparado limita idade máxima por fonte; distinções comerciais adicionais e eventual tolerância além do período precisam de aprovação e ajuste explícito, não de reinterpretar números de teste. Legado permanente fica fora de toda expiração de Premium.

## Validação e reprodução

[Testes do repositório](../../app/src/test/java/br/com/calcmot/entitlement/EntitlementRepositoryTest.kt) e [codec](../../app/src/test/java/br/com/calcmot/entitlement/EntitlementCodecTest.kt) cobrem legado, ausência de fonte/503, sessão/offline/401, conexão interrompida, conta/projeto/epoch, cache/revisão, reboot/tempo, trial/assinatura/promo/review, fontes combinadas, perda de arquivo/chave, recuperação após reinstalação e falha de persistência. Reavaliação repetida e expiração automática conferem contadores sem novos requests/disco/token.

[Teste instrumentado](../../app/src/androidTest/java/br/com/calcmot/entitlement/EntitlementStoreDeviceTest.kt) prepara AtomicFile + Android Keystore reais, recriação do store, isolamento e adulteração de fixture. Só executa com `br.com.calcmot.identitydev`; limpa apenas seu arquivo sintético, não chave compartilhada ou dados de usuários. **ADB não detectou aparelho nesta sessão**: compilação não será apresentada como execução física/reinstalação/reboot real.

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
./gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=2 --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
./gradlew.bat :app:compileIdentityDevAndroidTestKotlin '-Pcalcmot.identityDevInstrumentation=true' --no-daemon --max-workers=2 --console=plain
# Somente com aparelho DEV autorizado; não executado nesta sessão:
./gradlew.bat :app:connectedIdentityDevAndroidTest '-Pcalcmot.identityDevInstrumentation=true' '-Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.entitlement.EntitlementStoreDeviceTest'
```

Opt-in Gradle muda apenas o build type da instrumentação para o pacote DEV. Não altera applicationId principal, versões, manifest ou assinatura publicada. Gson 2.11.0 é dependência explícita para parser JSON estrito em Android/JVM; não cria serviço, framework ou custo de infraestrutura.

Resultados desta execução:

- **`:app:testDebugUnitTest`: 299 PASS, zero failures/errors.** Inclui 30 S08 (26 repository, 4 codec); XMLs conferidos em `app/build/test-results/testDebugUnitTest/`.
- **`:app:testIdentityDevUnitTest -Pcalcmot.identityDevInstrumentation=true`: 299 PASS, zero failures/errors**, sobre a versão final, com os mesmos 30 S08. São as mesmas cases em duas variantes, não 598 testes distintos. Comando executado junto de `:app:compileIdentityDevAndroidTestKotlin`; **BUILD SUCCESSFUL**.
- **Instrumentação DEV compilada**, incluindo store/Keystore. Não executada em hardware; sem dispositivo ADB. Reinstalação, boot, relógio e falha de chave da matriz JVM são simulações, não ensaios físicos.
- Fixture sintética do teste instrumentado e seu objeto EntitlementCache passaram no **Ajv 2020-12 + formats contra S01 original**. Validação local minimizada em `.tmp/s08-contract-validation.json`, sem identidade/token real.
- Hashes: **7 contratos, 17 arquivos anteriores de backend/src+migrations e 181 arquivos anteriores de app/src/main sem alteração** em relação ao início da S08. Novas fontes Android estão em pacote separado. `git diff --check` passou; links locais do relatório/BACKLOG/EXECUTION-PLAN conferidos.
- Primeira rodada completa identificou dois ajustes de teste corrigidos: exceção de JSON inválido e lista exata de adaptadores Firebase incluindo S08. Não restam falhas nessa suíte. Avisos de depreciação já existentes não foram alterados.
- Nenhum ensaio HTTPS/Google/backend real novo ou CPU de aparelho foi apresentado como comprovado. Não repetimos os testes backend S02–S07 porque nenhuma fonte/migration/configuração dele mudou. Zero deploy, push, release, novo serviço externo ou custo de infraestrutura.

Para reproduzir a variante final e compilação instrumentada no mesmo comando:

```powershell
./gradlew.bat :app:testIdentityDevUnitTest :app:compileIdentityDevAndroidTestKotlin '-Pcalcmot.identityDevInstrumentation=true' --no-daemon --max-workers=2 --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

## Arquivos desta execução

Criados: quatro fontes `app/src/main/java/br/com/calcmot/entitlement/` (`EntitlementSnapshot.kt`, `EntitlementCodec.kt`, `EntitlementRepository.kt`, `AndroidEntitlements.kt`), dois testes JVM em `app/src/test/java/br/com/calcmot/entitlement/`, `EntitlementStoreDeviceTest.kt` em `androidTest/` e este relatório.

Alterados: `app/build.gradle.kts` (parser e opt-in DEV), `ReleaseReadinessTest.kt` (incluir o adaptador S08 na lista exata de integrações Firebase), BACKLOG e EXECUTION-PLAN. Nenhuma migration SQL nova: armazenamento Android versionado e independente dos bancos existentes. Implementações S02–S07 preservadas; alterações anteriores do workspace mantidas.

## Critérios originais e status

| Critério S08 | Evidência / situação |
|---|---|
| RAM/cache e nenhuma consulta por oferta | Pacote isolado, Core inalterado; testes de zero chamadas adicionais. |
| Grant durável independente de Premium | Cache por titular, rejeição de regressão/contradição, recuperação S07; testes sintéticos. |
| Relógio/reboot não estendem acesso | Âncora monotônica/boot e validade finita parametrizada; ensaio físico pendente. |
| Queda de rede não interrompe monitoramento | Nenhum gate, UI comercial ou integração ao pipeline. |
| Tolerância e review conforme OQ-03 | **Pendente de decisão**, sem prazo inventado/ativado. |
| Restrição/restauração na entrada segura | Operações de recuperação prontas; integração restritiva **bloqueada por OQ-10**, sem S09. |

**S08 PARTIAL.** Store/Keystore já validados fisicamente na continuação acima. Próximos passos restritos a concluir S08: aprovar OQ-03/OQ-10, resolver as lacunas de fase/recência/entrada segura e validar relógio/boot/lifecycle/recuperação em ambiente autorizado antes de ligar consumo comercial. Nenhum deploy, push, release, grant comercial, Billing, RTDN, paywall, S09 ou serviço pago nesta execução.
