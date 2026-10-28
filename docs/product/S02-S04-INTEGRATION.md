# S02 + S04 — integração real em desenvolvimento

Atualização: **2026-10-13**, branch **growth**. **S02 PARTIAL / S04 PARTIAL**: integração real comprovada; isolamento no D1 remoto com fixtures autorizadas e OAuth principal preparados nesta continuação. CPU de novas amostras ainda não demonstra margem estável no Free. Este relatório substitui os estados anteriores; não autoriza outras stories ou publicação.

## Configurações concluídas e evidências

| Item | Evidência / resultado |
|---|---|
| Firebase | Projeto `calcmot`, plano **Spark** confirmado no Console. Authentication inicializado e Google habilitado; nome público CalcMot, suporte eduardoangelo20001@gmail.com escolhido pelo usuário. Sem Blaze, Identity Platform paga, SMS ou faturamento. |
| Android DEV | App Firebase CalcMot Auth DEV; pacote `br.com.calcmot.identitydev`; app ID `1:303161771474:android:5ff13ed1e964bfa6d8740b`. |
| OAuth DEV | Certificado efetivamente usado pelo APK cadastrado somente no pacote DEV após autorização. JSON real salvo pelo usuário em `app/src/identityDev/google-services.json`, ignorado pelo Git. Projeto/pacote/Android OAuth client conferidos; exatamente um Web client tipo 3 para o client DEV. Recurso gerado `default_web_client_id` presente e igual ao Web client real. |
| Cloudflare | Conta verificada no painel; **Workers Free / $0 / Current plan**, D1 Free. Sem cartão, contratação ou upgrade. Site `calcmot-site` e domínio de produção intactos. |
| D1 DEV | `calcmot-identity-dev`, UUID `4d0c0c71-c606-45a6-a037-43bccef8c05e`, região ENAM, replicação desativada. Migration versionada `0001_entitlement_snapshots.sql` aplicada remotamente somente nesse banco. Tabela e trigger verificados; **zero snapshots**. Região DEV não decide residência de produção. |
| Worker DEV | `https://calcmot-api-dev.eduardoangelo20001.workers.dev`; versão atual **106407db-b27c-4d43-b141-525c0a795d10**, com controles em memória. Versão anterior 1fd118fe foi a primeira integração. Binding DB no D1 acima; projeto calcmot; sem custom domain/route, logs/traces desativados. |
| Credenciais | OAuth oficial Wrangler, escopos explicitamente autorizados: account:read, user:read, workers_scripts:write, d1:write e offline_access. Credencial criptografada fora do repositório, chave no Windows Credential Manager. Configuração real `backend/wrangler.dev.jsonc` ignorada; template versionável separado. |

O painel Cloudflare chama o ambiente padrão de cada script de “Production”; nesta execução o script publicado é exclusivamente **calcmot-api-dev**. Não é a API de produção do CalcMot.

### Certificados reais: não intercambiáveis

| Certificado | SHA-1 | Origem / limite |
|---|---|---|
| Debug local e upload | `FF:A7:EA:91:AD:22:03:E8:4E:C0:CE:5C:B8:98:EE:33:74:A2:1A:3C` | signingReport e apksigner do APK efetivo. Neste checkout o debug herda o signer de upload quando keystore.properties existe. Cadastrado somente no pacote DEV. |
| Distribuição Play | `D6:0E:41:D9:FD:C3:AB:F2:54:AB:06:56:91:70:48:F8:21:A1:C5:89` | Informado pelo usuário da seção “Chave de assinatura do app”. Cadastrado no Firebase **br.com.calcmot** após nova confirmação explícita; row SHA-1 e Android OAuth client do JSON oficial correspondentes comprovados. Leitura/cópia/download automáticos do certificado Play continuam sem resultado; o JSON confirma o cadastro, não é verificação independente da origem do certificado. |

SHA-256 Play registrado na S03 e reconferido no snippet oficial Play nesta continuação: `05:4E:84:4D:BA:7E:C6:B7:BC:C8:A5:27:26:B1:C9:D2:43:BB:C3:F3:A6:34:08:EC:9C:FD:FC:A4:3E:D0:6F:3A`. Não foi calculado SHA-1 a partir desse hash. `app/google-services.json` preservado, SHA-256 local `35F556041DBE88833B7A5FE4BDD25A8055440CE9AEBBFE95B314E2CC9AAE4E3D`; login de instalação assinada pela Play ainda não é declarado validado.

## Implementação entregue

**S02:** Worker TypeScript verifica Firebase ID Token RS256, assinatura/chaves Google, issuer, audience, expiração e claims de identidade. UID deriva exclusivamente de `sub` validado. SQL parametrizado e chave `(firebase_project_id, uid)` isolam os registros. `GET /v1/me/entitlement` usa schema e envelope S01 sem edição. Ausência de snapshot resulta `503 RETRY_LATER`; cliente não escreve direitos, benefícios, compras, pagamentos ou ledger. Rotas de escrita/admin/internal permanecem fechadas. Migrations e testes D1 locais versionados; sem handlers financeiros fictícios.

**S04:** componente Firebase Auth + Credential Manager, login opcional explícito, linking/reautenticação conservando titular, erros tipados e recuperação pelo mesmo Google account/projeto. Sem merge por email ou transferência de benefícios. Nenhuma inicialização/chamada de rede no Core, OCR, overlay ou processamento de ofertas. Conta Google de login não identifica o comprador Play.

**Ferramenta exclusiva DEV:** build type `identityDev`, pacote separado e `IdentityProbeActivity` em Compose com tema existente. Application DEV não inicializa Core; MainActivity desabilitada apenas nesse pacote; coleta Analytics/Crashlytics desabilitada. Botões testam login, refresh, entitlement e logout DEV. Insets e layout conferidos em screenshot real. Não é tela S09 nem fluxo publicado.

O token obtido para a requisição é mantido em memória e enviado somente por HTTPS ao host DEV fixado no build, sem redirects. A ferramenta não o exporta em UI, intents, clipboard, arquivos ou logs. O SDK Firebase mantém sua sessão no armazenamento privado normal. UID completo não é mostrado; resumo local serve apenas à comparação no aparelho, sem telemetria.

Uma colisão real revelou que `FirebaseAuthInvalidCredentialsException` também sinaliza `ERROR_USER_MISMATCH`. Corrigido o mapeamento para **COLLISION**, com dois testes de regressão e repetição real bem-sucedida. A classificação corresponde à [documentação de reautenticação Firebase](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseUser#reauthenticate(com.google.firebase.auth.AuthCredential)).

## Testes reais no Android e no edge

Dispositivo físico Samsung A07 / SM-A075M, Android 15, Google Play Services. Instalações/desinstalação restritas ao pacote DEV no usuário Android 0; nenhum app publicado foi removido. APK arm64 DEV assinado, pacote conferido, versões recebidas **11 / 3.4** preservadas.

| Cenário executado | Resultado / limite |
|---|---|
| Login Google real, consentimento concluído pelo usuário | **LOGIN_OK**, sessão vinculada. |
| Cancelamento do seletor | **CANCELED**, sem conceder sessão. |
| Atualização do APK DEV | Sessão e identidade original preservadas. |
| Renovação forçada | **REFRESH_OK**; não é evidência de expiração natural. |
| Wi-Fi/dados desativados manualmente | **OFFLINE**, identidade preservada. Usuário reativou a rede depois. |
| Sessão A, segunda conta selecionada manualmente | **COLLISION** após correção; identidade original mantida, sem merge/troca/concessão. Não substitui o cenário de linking de credencial já vinculada a outro UID anônimo. |
| Reinstalação somente DEV | Nova instalação mostrou “Sem sessão”; login na conta original retornou **LOGIN_OK**, mesma identidade recuperada. |
| Worker sem Authorization | **401 AUTH_REQUIRED**, envelope contratual e no-store. |
| Android Firebase ID Token real → Worker → D1 | **503 RETRY_LATER**, request ID `35ecb05e-c2cd-4379-8555-2943e279265d`. Consulta própria D1 corroborada no painel; não apenas resposta HTTP isolada. |

Consulta real observada no painel D1:

```sql
SELECT snapshot_json, revision FROM entitlement_snapshots
WHERE firebase_project_id = ? AND uid = ?
```

Uma execução, zero linhas lidas/escritas. Binding e projeto estão conferidos; a consulta ocorre somente após validação JWT. Nenhum UID/token foi registrado como evidência. Banco sem snapshots não recebeu direitos fictícios para produzir 200. Escritas de migration/DDL e bookkeeping existiram (11 linhas reportadas no painel); a consulta autenticada de entitlement não escreveu dados.

### Consumo CPU inicial e plano gratuito — antes desta continuação

Janela **Last 1 hour**, isolando a invocação autenticada: **P50/P90/P99/P999 = 5,51 ms**, incluindo validação JWT, busca de chaves Google e acesso D1. Amostra **N=1**, abaixo do limite **10 ms CPU Workers Free**; não é teste de carga nem garantia de percentil/cold starts futuros. Zero erros de runtime/limite de CPU observados nessa validação. Startup de 2 ms e duração SQL de 0,8 ms são métricas distintas e não substituem CPU.

Workers Free: 100 mil requests/dia e 10 ms CPU/request. D1 Free: 5 milhões de linhas lidas/dia, 100 mil escritas/dia e 5 GB totais. Compatibilidade comprovada para a amostra, sujeita às quotas; excedê-las não autoriza upgrade. [Workers](https://developers.cloudflare.com/workers/platform/pricing/), [D1](https://developers.cloudflare.com/d1/platform/pricing/).

## Validação automatizada e preservação

| Validação | Resultado |
|---|---|
| Backend typecheck / testes | **PASS / 22 testes**: assinatura/claims/expiração, autorização, isolamento UID/projeto, SQL injection, escrita negada e integridade/revisão. |
| Wrangler local / D1 local | Backend funcional localmente; migration e smoke test sem token 401, sem provisionamento obrigatório. |
| Bundle e deploy DEV | **196,87 KiB / gzip 27,69 KiB**; dry-run e deploy concluídos. |
| Android final | `:app:assembleIdentityDev :app:testDebugUnitTest`: **BUILD SUCCESSFUL**, **267 testes, zero falhas/erros**, incluindo os dois testes novos da correção de colisão. |
| Manifesto/APK DEV | Application e launcher isolados, signer/pacote/versão conferidos. Build real inclui Google Services; tentativa anterior de excluir tarefa de recursos não é usada como validação. |
| Contratos S01 | **7/7 hashes preservados**. |
| Conferência final documental | **PASS**: quatro arquivos protegidos idênticos ao baseline; links locais válidos; `git diff --check` sem erros; configurações DEV ignoradas; branch growth. |

Nenhum push, release, alteração de versão, assinatura paga, recurso pago ou implementação de S05/S09/Billing/RTDN/recompensas. Alterações anteriores do workspace preservadas. Manifesto fonte e configuração de produção preservados; teste DEV não comprova por si só ausência de regressão funcional de ofertas no app publicado.

## Novas validações desta continuação — fechamento objetivo

### Isolamento no D1 DEV remoto e limpeza

Autorização do usuário: duas fixtures sintéticas, exclusivamente no D1 DEV, sem direitos comerciais. Teste versionado `backend/tests/dev-d1-isolation.test.ts`, opt-in explícito, trava script/conta/UUID DEV. Dois UIDs sintéticos únicos, revisions **101 / 202**, `legacyCore/coreAllowed/proAllowed=false`, Play/promo NONE, nenhuma feature Pro. Não foram usados UIDs de clientes reais.

| Cenário novo | Resultado |
|---|---|
| UID A / UID B consultam seus registros no D1 remoto | **200**, revision 101 / 202 respectivamente, todos os direitos negados. |
| A informa uid=B na query; B informa uid=A | **200 próprio**, revision original de cada titular; nenhum acesso ao outro registro. GET congelado ignora query sem trocar o titular. |
| Token ausente/inválido | **401 AUTH_REQUIRED**. |
| POST na rota read-only | **403 FORBIDDEN**, aplicável à operação recusada, não um status novo do GET. |
| Identidade sem registro | **503 RETRY_LATER**, sem criar snapshot. |
| Cleanup em finally | Apenas os dois registros deste run apagados; zero fixtures e contagem original restaurada. |
| Conferência remota independente após o teste | **Total de snapshots = 0**, zero linhas escritas nessa conferência. |

**Limite da evidência:** handler/validação de assinatura executados localmente, com chaves de teste efêmeras e adapter Wrangler para o D1 real remoto. Não foram tokens Google das duas identidades nem requests HTTP das fixtures ao Worker público. A verificação Google do Worker publicado permanece intacta; o JWT Google real no edge já foi comprovado e foi usado nas novas amostras abaixo. Prova de isolamento do armazenamento remoto está concluída, sem alegar um cenário diferente do executado.

### Controles gratuitos implementados e proposta operacional

`backend/src/abuse.ts` e handler: proteção em memória por isolate, antes da validação JWT e antes da consulta D1. DEV **120 tentativas/minuto/isolate** e **30 leituras/minuto/UID validado/isolate**; máximo 512 titulares na memória, sem eviction para contornar contador. Reinicialização da instância e fronteira de minuto reiniciam contadores. Sem novo serviço, armazenamento persistente, cartão ou custos. Configuração ativada inválida falha fechada; harness local sem configuração permanece explicitamente sem throttle, não deve ser promovido para ambiente aberto.

Exaustão devolve **503 RETRY_LATER** no envelope congelado, sem introduzir 429 no GET. `ENTITLEMENT_READ_ENABLED=false` é um desligamento de leitura, sem excluir/alterar grants nem bloquear Core. Cache JWKS/cooldown e limite de tamanho/formato do bearer existentes foram preservados; não há cache de token/entitlement para dispensar verificação de assinatura/expiração. Cinco testes novos passaram (quatro do guard e um do handler), além do teste remoto; typecheck passou. Android e antigos fluxos já comprovados não foram repetidos.

**Proposta de produção, ainda não aprovada:** adotar inicialmente 30 leituras/minuto/UID e proteção de rajada antes de JWT, com valores ajustados à cadência de consumo que S08 definir; exigir configuração explícita, owner operacional e procedimento de desligamento por CPU/quotas. Reservar um orçamento diário para API dentro dos 100 mil requests/dia **compartilhados pela conta**, após medir o site existente. Não escolher silenciosamente orçamento/percentual de alerta/owner nem garantir cota global com contador local.

O guard implementado limita rajadas e trabalho por instância, **não** tráfego distribuído, persistência de contador ou orçamento diário da conta. Os Workers rejeitados pelo guard ainda consomem invocações. Alternativa mínima futura: binding nativo de rate limit por UID/projeto, somente após comprovar disponibilidade sem cobrança na conta e aprovar namespace/valores; counters desse binding também são locais ao ponto de presença e permissivos, não cota global exata. Não foi ativado por presunção de custo. [Limites/binding oficial](https://developers.cloudflare.com/workers/runtime-apis/bindings/rate-limit/).

### CPU real após os controles

Versão **106407db**, filtro **Actively deployed versions**, janela **Last 1 hour**, painel confirmou **2 invocações**, uma busca de chaves Google, zero erros de runtime e zero `Exceeded CPU Time Limits`. Duas consultas Android distintas terminaram em 503; request IDs `382fcb8c-8d70-4c67-b80b-ce601f98cc2b` e `1e965277-1e59-4871-8495-e6f0537c58f5`. Tokens não foram exportados. Tentativas de automação sem resposta nova observada não foram contabilizadas como amostras.

| Métrica do painel | Resultado |
|---|---|
| P50 | **1,943 ms** (display 1,94) |
| P90 / P99 / P999 | **14,163 ms** (display 14,16) |
| Limite nominal Free | **10 ms** |
| Runtime / CPU limit errors | **0 / 0** |
| Bundle / startup | 198,46 KiB / gzip 28,12 KiB; startup 1ms, separado de CPU/request. |

**HOLD para afirmar margem estável**, não STOP nem prova de plataforma paga: P50 abaixo de 10ms, cauda observada acima, amostra pequena N=2. A Cloudflare documenta tolerância a excessos infrequentes por isolate e término se forem consistentes; isso explica a possibilidade de ausência de erro sem garantir capacidade futura. Não atribuir o pico a JWT/JWKS/cold start sem perfil específico. Próximo passo mínimo é perfil local do caminho autenticado e nova amostragem controlada no mesmo DEV, preservando assinatura/claims e custo R$0; não contratar plano nem dividir serviços só para contornar o limite. [CPU e tolerância oficiais](https://developers.cloudflare.com/workers/platform/limits/#cpu-time).

### OAuth principal preparado, configuração original preservada

SHA-1 Play autorizado salvo no Firebase `br.com.calcmot`, separado de `br.com.calcmot.identitydev`; nenhum SHA de upload cadastrado no app principal nesta continuação. Usuário baixou JSON oficial para **`.tmp/s02-s04-real/main-oauth/google-services.json`**, sem substituir arquivo existente. Conferência: projeto calcmot, um client principal, um Android OAuth client com hash Play e um Web client tipo 3; esse Web client é o mesmo usado pelo DEV no mesmo projeto.

Tarefa oficial **processDebugGoogleServices PASS**, usando init script temporário que seleciona apenas o candidato e gera recursos em `.tmp/s02-s04-real/main-oauth/generated/`. `default_web_client_id` gerado corresponde ao Web client desse JSON. Nenhum APK/release principal foi montado/assinado/instalado, e o arquivo original foi preservado.

Login com a assinatura Play não executado: a chave de distribuição é gerenciada pela Play e não está disponível para assinar APK local. Debug deste checkout usa upload, que não substitui assinatura Play; instalar pacote principal local também arriscaria conflito com produção. Validar o candidato em artefato efetivamente assinado pela Play exigirá fluxo de teste de distribuição autorizado, fora desta sessão sem release. Não é ausência de Web client/OAuth cadastrado; é evidência de distribuição ainda pendente.

Novas evidências privadas locais: `d1-isolation-result.json`, `worker-guarded-cpu.jpg`, `worker-guarded-metrics.jpg`, `firebase-main-play-sha-saved.jpg` e candidato/recursos OAuth em `main-oauth/`. Testes reproduzíveis no [README backend](../../backend/README.md). Fixtures já removidas.

## Pendências concretas e status individual

**S02 — PARTIAL.** Backend/identidade real, D1 remoto e isolamento com fixtures, limpeza e controles DEV concluídos. Restam:

1. **Decisão operacional real:** aprovar parâmetros de ambiente aberto, orçamento compartilhado da conta e responsável/processo de contingência. Os limites DEV técnicos não aprovam política de produção. Operações administrativas continuam fechadas.
2. **Validação adicional de CPU:** N=2 mostrou cauda 14,163ms >10ms, sem erro de execução. Perfil/amostragem precisam demonstrar que o excesso é infrequente e corrigível antes de afirmar margem estável. Não é bloqueio da implementação local ou do DEV já funcional.

**S04 — PARTIAL.** Login/cancelamento/offline/refresh/colisão e reinstalação reais comprovados. Para fechar a story:

1. **Aparelho secundário:** testar a mesma conta/projeto e continuidade de UID em outro dispositivo com Google Play Services.
2. **Expiração natural:** executar o fluxo com sessão/token efetivamente expirado e recuperação/rejeição real; refresh forçado e testes unitários não substituem essa evidência.
3. **Linking excepcional:** cenário real controlado de credencial já vinculada a outro titular anônimo ainda não executado; provider anônimo não foi habilitado só para fabricar o cenário.
4. **Teste adicional de distribuição:** cadastro OAuth principal e JSON/recurso Web client preparados; login de artefato assinado pela Play ainda não executado. A leitura independente do SHA-1/certificado Play permanece limitação do controlador; origem foi informada pelo usuário, e o cadastro no Firebase está comprovado.
5. **OPEN_QUESTION OQ-01 residual:** política de suporte/recuperação sem acesso à conta Google ou para colisão que exija intervenção; nenhuma escolha de ownership/transferência automática. Provedor Google já aprovado e não está novamente em decisão.

Revogação imediata de token emitido não é consultada neste backend; operações financeiras futuras exigem decisão própria antes de serem ativadas, conforme ADR. Não há pagamentos habilitados. Deploy de produção continua sem autorização; configuração DEV já está concluída e não requer novo consentimento para sua existência.

### A arquitetura permite iniciar S05?

**Sim, para desenvolvimento e validação isolada**, com UID derivado do token, D1 privado e emissão server-side. O Core atual não depende de Auth/API; nenhuma fixture bloqueou um usuário real. Não significa autorizar S05 nesta execução nem liberar migração/concessões em produção. S05 continua exigindo **OQ-02**: política/evidência de legado, baseline, janela/cutoff e fluxo de exceção aprovados; jamais conceder por autodeclaração ou negar legado por falha da API. CPU/controles operacionais precisam de fechamento antes de abertura externa/rollout. Nenhum grant S05 foi implementado.

## Arquivos e reprodução

- Backend: `backend/src/`, `tests/`, `migrations/0001_entitlement_snapshots.sql`, dependências/configuração local e [README](../../backend/README.md); template `backend/wrangler.dev.example.jsonc`. Configuração real DEV ignorada.
- Android: `app/src/main/java/br/com/calcmot/identity/`, testes `app/src/test/java/br/com/calcmot/identity/`, allowlist de ReleaseReadinessTest e `app/build.gradle.kts`; ferramenta `app/src/identityDev/AndroidManifest.xml`, `java/br/com/calcmot/debug/IdentityProbeActivity.kt`, [README DEV](../../app/src/identityDev/README.md). JSON DEV ignorado pela `.gitignore`.
- Documentos: este relatório, [ADR-001](ADR-001-FREE-IDENTITY-BACKEND.md), [BACKLOG](BACKLOG.md) e [EXECUTION-PLAN](EXECUTION-PLAN.md), com status corrente.

Comando final validado (JDK 21 / SDK existente):

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:GRADLE_USER_HOME="$PWD/.gradle-user"
./gradlew.bat :app:assembleIdentityDev :app:testDebugUnitTest --no-daemon --max-workers=2 --console=plain '-Pkotlin.compiler.execution.strategy=in-process' '-Pcalcmot.identityDevWorkerUrl=https://calcmot-api-dev.eduardoangelo20001.workers.dev' --no-problems-report
```

Evidências privadas locais, ignoradas pelo Git, em `.tmp/s02-s04-real/`: `google-provider-enabled.jpg`, `firebase-dev-sha-saved.jpg`, `cloudflare-workers-free.jpg`, `cloudflare-d1-free.jpg`, `worker-auth-cpu.jpg`, `d1-owner-query.jpg`, `android-dev-validated.png`, `android-offline.png`, `android-collision.png` e `android-reinstall-recovered.png`. Não publicar credenciais, códigos de autorização, tokens ou informações privadas de testers.
