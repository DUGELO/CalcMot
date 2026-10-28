# S06 — Preparação e validação do operador

## Operação DEV comprovada — 2026-10-19

**Esta seção prevalece sobre o diagnóstico de 14/10 abaixo.** Operador Google real validado; claim admin autorizada somente ao UID conferido, demais claims preservadas, nenhum outro admin no inventário completo de bootstrap. Foi usada autoridade já existente no Cloud Shell, sem instalar CLI local, criar chaves ou mudar IAM. [Resultados do ensaio](S06-S09-DEV-OPERATIONAL-RESULTS.md). A política [OQ-07 DEV](OQ-07-DEV-APPROVED-POLICY.md) está aprovada; a proposta antiga é histórico.

### Retomar o console privado

1. Na pasta `backend`, executar `node scripts/s06-dev-session.mjs` com a credencial Wrangler existente e configuração DEV local ignorada pelo Git. A guarda aceita somente o Worker/banco DEV já conferidos. Não cadastrar credenciais no arquivo. O console deve permanecer em `127.0.0.1:8966`; não expor por túnel ou mudar para `0.0.0.0`.
2. Abrir `http://127.0.0.1:8966/`, conferir o UID do proprietário no Firebase e fixá-lo no campo protegido. Fica apenas na memória desse processo; não preencher com email nem inferir UID do solicitante. Reiniciar o console exige repetir a configuração.
3. No Samsung, login/reautenticação Google explícita **somente no CalcMot Auth DEV**. `Renovar sessão` sozinho não renova `auth_time`. Confirmar `LOGIN_OK` antes do handoff. Não compartilhar bearer pelo chat/clipboard/prints.
4. Com ADB autorizado, na raiz do projeto:

```powershell
$devAdb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
& $devAdb reverse tcp:8967 tcp:8967
& $devAdb shell am instrument -w -e class br.com.calcmot.account.OperatorSessionHandoffDeviceTest#handoffFreshOwnerSessionToPrivateLocalConsole br.com.calcmot.identitydev.test/androidx.test.runner.AndroidJUnitRunner
```

O teste opt-in envia o Firebase token apenas pelo socket local USB/ADB, sem disco/log/argumento. Exige APK de testes identityDev instalado. Esse APK é ferramenta de desenvolvimento; nunca integrar handoff ao release.

5. A sessão do operador vence em até **300 segundos da reautenticação**, mesmo que o ID Token dure mais. Repetir reautenticação/handoff quando necessário; não relaxar a guarda. A sessão de usuário comum é rejeitada e limpa a autoridade anterior da ponte.
6. No formulário JSON, usar `inspect`, `evidence`, `decide`, `issue` com protocolo, revisão e chave da intenção. Não enviar `uid` ou `token`. O dispatcher deriva o titular do protocolo; reusa o serviço S06, nunca INSERT manual. Valores aqui são placeholders, não uma autorização de grant:

```json
{"action":"inspect","claimId":"PROTOCOLO"}
```

`decide` seleciona evidências contextualizadas já registradas; APPROVE permanece UNDER_REVIEW. `issue` exige a revisão devolvida por APPROVE e `input.approvalEventId` vigente, após autorização comercial/contextual aplicável. Repetir a mesma key em timeout, sem criar uma nova intenção. A prova de GRANTED é a fonte persistida, não a presença do botão ou o texto da avaliação.

7. Para privacidade: `privacy-close` após encerramento efetivo; `privacy-plan` com `input.mode` (`EVIDENCE_RETENTION`, `AUDIT_RETENTION`, `ERASE_ACCOUNT`). Conferir o dry-run e seu fingerprint; `privacy-execute` usa `input.confirmation` com operationId, fingerprint e ownershipConfirmed. Exclusão integral exige ainda eraseWithoutAutomaticRecovery e autorização específica. Não executar com dados reais neste piloto; nenhuma exclusão real foi autorizada.
8. Encerrar com Ctrl+C; o processo elimina token/owner em memória e fecha o binding. Remover o reverse com `adb reverse --remove tcp:8967`. Não revogar/desativar claims reais ou permissões como “limpeza” sem aprovação própria. Para incidente de operador: cessar console, pedir remoção da claim/revogação à autoridade legítima e considerar a janela de JWT já emitido.

### Build de intake DEV

`LEGACY_INTAKE_ENABLED` é false por padrão. O ensaio usa `-Pcalcmot.identityDevLegacyIntake=true` **junto de** `-Pcalcmot.identityDevWorkerUrl=https://calcmot-api-dev.eduardoangelo20001.workers.dev` e a variante identityDev. A URL DEV não é default de produção. A ferramenta de teste exige `-Pcalcmot.identityDevInstrumentation=true`. Nenhum build/release publicado foi substituído.

O pacote DEV deve estar sem serviço de leitura habilitado para o ensaio isolado. A fixture opt-in salva o valor anterior do toggle DEV e persiste sua desativação; não mexe nas permissões nem no app Play. Na reinstalação, defaults são novos e não é correto assumir que a fixture antiga ainda exista. O Core real não é exercitado por esse pacote isolado.

## Diagnóstico histórico — não representa o estado atual

Data: **2026-10-14**. **PARTIAL / diagnóstico e preparação**, sem atribuição de privilégios, deploy, custo, chave de serviço ou alteração de usuário. Não substitui a aprovação OQ-07 nem habilita concessões reais. [Recuperação S06](S06-LEGACY-RECOVERY.md), [proposta OQ-07](OQ-07-DATA-POLICY-PROPOSAL.md).

## Diagnóstico novo, sem credenciais expostas

| Verificação | Resultado |
|---|---|
| Firebase Auth, Console | Duas identidades Google; candidato do proprietário/UID legítimo presentes. Não exportamos UID/email. A tela não mostra custom claims. |
| `LEGACY_OWNER_UID`, `GOOGLE_APPLICATION_CREDENTIALS`, `CLOUDSDK_CONFIG` no processo | Ausentes. Configs Wrangler examinadas também não definem owner. |
| Google Cloud CLI/Firebase CLI | Não encontrados no PATH; Google Cloud SDK não encontrado nos caminhos padrão examinados. Não é censo de todos os discos/instalações alternativas. |
| ADC | Arquivos padrão em APPDATA/gcloud e USERPROFILE/.config/gcloud ausentes; nenhum caminho explícito de credencial configurado. `credentials.db` não encontrado nesses diretórios. Nenhum conteúdo de credencial foi impresso. |
| Firebase Admin SDK no backend | Não instalado. Nenhum pacote foi instalado nesta execução; scripts administrativos são opcionais e não entram no Worker/APK. |
| Conta de serviço da S03 | Reconferida no Console: **ativa, nenhuma chave**. IAM visível no projeto: Administrador do Firebase Authentication, Agente de serviço administrador do SDK Admin do Firebase e Criador do token da conta de serviço. É uma identidade com escrita ampla, inadequada para presumir privilégio mínimo de auditoria. Capacidade efetiva do operador de impersoná-la não foi exercida/confirmada. |
| IAM da conta Google do proprietário | Papel **Proprietário** visível no projeto. Isso não cria ADC local, não significa custom claim Firebase `admin=true` e não autoriza escrever usuários nesta execução. Não houve alteração de IAM. |
| Privilégios de outros usuários reais | **Não comprovados**: depende de inventário Admin SDK autorizado, todas as páginas. Uma sessão ordinária não demonstra ausência global de admins. |

Navegar no Console com Google, obter ID Token Firebase e autenticar Admin SDK são três capacidades diferentes. API key/`google-services.json` não autorizam administração. Não criar credencial para contornar esse diagnóstico.

## Caminho recomendado sem chave privada

**Preferência condicional:** ADC com impersonação de uma conta de serviço **somente leitura**, após verificar e aprovar a conta-alvo e suas permissões. A conta padrão da S03 não deve ser impersonada às cegas: pode ter acesso maior do que a auditoria exige. Se não houver alvo com escopo adequado, apresentar a criação de uma identidade dedicada sem chave para aprovação; não criar agora.

O Google documenta ADC por impersonação para bibliotecas Node.js. É um caminho a validar com o SDK/API de Authentication no ambiente autorizado, não evidência de funcionamento nesta máquina. Se APIs/organização exigirem contratação/faturamento, interromper essa opção; não habilitar billing. Não exige hospedar Function, Run ou backend pago. [ADC local](https://docs.cloud.google.com/docs/authentication/set-up-adc-local-dev-environment).

**Alternativa oficial:** ADC de usuário com OAuth Desktop próprio e `projectId` explícito. O Firebase adverte que credenciais de usuário geradas pelo OAuth client padrão do gcloud não são aceitas por Firebase Authentication. Criar/configurar cliente Desktop requer aprovação; não reutilizar client Android/Web como se fosse Desktop. [Firebase Admin setup](https://firebase.google.com/docs/admin/setup).

Ambas evitam arquivo de **chave privada de service account**. ADC pode guardar refresh token no perfil local: não é acesso sem segredo nem cofre automático. Restringir ACL ao proprietário, não copiar para repo/Cloudflare/Android, revogar ao encerrar o uso autorizado. Nenhuma ADC foi criada/substituída nesta execução.

### Permissões mínimas e aprovações separadas

| Operação | Permissão / âmbito | O que precisa ser aprovado |
|---|---|---|
| Ler usuários/custom claims | `firebaseauth.users.get` no projeto `calcmot`; `roles/firebaseauth.viewer` é opção predefinida de leitura com permissões adicionais documentadas | Usar autoridade já legítima; se faltarem permissões, apresentar concessão de leitura específica, sem Editor/Owner/Admin amplos. |
| Impersonar conta alvo | `iam.serviceAccounts.getAccessToken`; papel `roles/iam.serviceAccountTokenCreator` **somente na service account alvo aprovada**, não no projeto todo | Aprovação explícita de alvo, escopo efetivo e duração; inspecionar IAM antes. Esse papel é sensível, não decorre de autorizar diagnóstico. |
| Conferir políticas IAM | Permissões de leitura das políticas de projeto/conta, conforme recurso | Não adicionar `setIamPolicy` ou papel administrativo para apenas ler. Se leitura negada, registrar ausência de evidência. |
| Emitir custom claim admin ao proprietário | `firebaseauth.users.update`, por autoridade confiável, após aprovação explícita do **UID e projeto** | Alteração sensível de usuário real. Não está autorizada agora. O papel completo `firebaseauth.admin` é amplo e não é mínimo obrigatório para leitura. Não atribuir role IAM ao próprio app Android. |
| Operar console S06 local | Firebase ID Token Google válido, `admin=true` booleano, UID exato configurado e `auth_time` <=300s | Independente da ADC: role IAM não vira custom claim automaticamente. Reautenticar no Google; refresh simples não renova auth_time. |

Referências: [papéis Authentication](https://docs.cloud.google.com/iam/docs/roles-permissions/firebaseauth), [custom claims](https://firebase.google.com/docs/auth/admin/custom-claims). Não existe permissão IAM exclusiva para alterar só `admin` de um UID: ferramentas e procedimento precisam limitar a escrita aprovada e preservar demais claims. Não há escritor de claims neste pacote.

### Sequência a executar somente depois das aprovações

1. Autorizar instalação local do Google Cloud CLI oficial e runtime Admin SDK oficial, sem implantação ou mudança de dependências de produção. Examinar a autoridade/IAM existente por leitura; não baixar chave privada.
2. Aprovar método/alvo de autenticação, eventuais papéis estritamente necessários e escopo do login. Se houver ADC anterior, preservá-la; o comando abaixo escreve credencial local e **não foi executado**:

```powershell
gcloud auth application-default login --impersonate-service-account CONTA_READONLY_APROVADA
```

Para a alternativa Desktop aprovada, usar `gcloud auth application-default login --client-id-file CAMINHO_OAUTH_DESKTOP_APROVADO`, conforme documentação. Não gerar cliente ou consentir novos escopos sem aprovação.

3. Copiar o UID do proprietário verificado no Console **para o processo local**, nunca para código, relatório ou `.dev.vars` versionado. Não inferir UID por email. No processo PowerShell autorizado:

```powershell
$env:FIREBASE_PROJECT_ID='calcmot'
$env:LEGACY_OWNER_UID = Read-Host 'UID legítimo do proprietário conferido no Console'
```

4. Usar [s06-admin-inventory.mjs](../../backend/scripts/s06-admin-inventory.mjs) num runtime administrativo isolado com Firebase Admin SDK e ADC já autorizados. O script deve estar em diretório cujo `node_modules` resolva o SDK (pode ser copiado para esse ambiente); ele não instala dependências. Lê **todas as páginas** e emite apenas contagens/booleans. Não define claims. Se falhar, nenhuma conclusão global de privilégio.
5. Se o proprietário não tiver admin, interromper e solicitar aprovação específica de atribuição a seu UID e autoridade escritora. Conferir que nenhum outro usuário teve privilégio alterado e preservar claims pré-existentes. Não executar essa escrita por página pública, console Android ou SQL.
6. Após a atribuição legítima, reautenticar proprietário e obter seu ID Token apenas por fluxo protegido local; não enviar ao chat/suporte. Executar o preflight abaixo. Repetir com uma identidade ordinária autorizada, cada uma sem compartilhar bearer com outra pessoa.

## Ferramentas preparadas e verificações novas

- [s06-operator-preflight.mjs](../../backend/scripts/s06-operator-preflight.mjs): não abre D1 e não escreve. Usa o `verifySession` existente, com JWKS Google fixo, issuer/audience/assinatura/expiração; emite somente booleans. Não aceita claims, URL de chaves ou UID no JSON de entrada.
- [operator-readiness.ts](../../backend/scripts/operator-readiness.ts): avaliação após sessão verificada, sem grant/role ou PII no resultado. `GO` é só para a checagem específica, não GO de release/S06.
- [s06-admin-inventory.mjs](../../backend/scripts/s06-admin-inventory.mjs): apenas `listUsers`; não muda usuário/IAM. Acesso global não foi executado por ausência de runtime/autoridade.
- [s06-local-inventory.mjs](../../backend/scripts/s06-local-inventory.mjs): banco fixo local, metadados/contagens, nenhuma linha pessoal/SQL livre/remoto.

Preflight de configuração, sem token:

```powershell
node scripts/s06-operator-preflight.mjs --configuration
```

Preflight do operador, somente após configurar UID externo e reautenticar:

```powershell
$secretInput = Read-Host 'ID Token do próprio operador reautenticado' -AsSecureString
$operatorToken = [Net.NetworkCredential]::new('', $secretInput).Password
try {
  @{token=$operatorToken;expected='owner'} | ConvertTo-Json -Compress |
    node scripts/s06-operator-preflight.mjs
} finally { $operatorToken=$null; $secretInput.Dispose() }
```

Controle ordinário usa `expected='ordinary'` e token próprio autorizado. Não expor tokens no histórico/argv/log. Depois usar o console S06 existente, sem endpoints administrativos públicos. Não exportar perfil ou imprimir erro bruto do SDK.

## Ensaio controlado: limites e procedimento

O caminho de emissão não foi reimplementado. O teste local de integração foi ampliado para validar **preflight → proprietário sintético assinado → console existente → solicitação/análise/aprovação/emissão/recuperação persistida**, incluindo recusa de outro admin sintético. O ambiente continua Miniflare/D1 local, sem usuários reais. A assinatura RSA local verifica o mecanismo, não prova um Google admin real.

Para ensaio real futuro: inventário admin aprovado + proprietário reautenticado + titular de teste autorizado; banco local dedicado sem grants de usuários reais, política fixa aprovada; protocolo canônico, análise contextualizada, APPROVE e `issue` privados; reiniciar runtime, novo login do mesmo UID e releitura da fonte. Conferir grant/case/recibo iguais, histórico não fabricado e controles negativos. Não implantar essa trilha em produção nem confundir fonte com snapshot S07.

## Estado operacional

Configuração local atual retorna **HOLD: projeto e owner não definidos no processo**. Teste novo com token inválido retorna AUTH_REQUIRED sem leitura de D1. Sete testes novos de readiness passaram; typecheck e sintaxe passaram. Rodada específica da integração ampliada registrada no relatório S06. Os 85 testes anteriores não foram executados novamente em bloco.

**S06 PARTIAL:** nenhum operador real foi configurado/privilegiado; auditoria global de custom claims e ensaio Google real continuam pendentes. OQ-07 é proposta, sem exclusão executável autorizada. Não confundir sessão do Console ou testes locais com fechamento operacional.
