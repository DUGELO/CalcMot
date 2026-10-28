# Validação real S02/S04 — somente desenvolvimento

`identityDev` gera o pacote separado `br.com.calcmot.identitydev`. Não substitui `br.com.calcmot`, não muda versionCode/versionName e não adiciona login à versão publicada. Usa Compose e os componentes de tema existentes. O Application desta variante não inicializa o Core; o launcher é somente a ferramenta de teste. Não habilitar os serviços de ofertas nesta instalação de teste.

## Configuração real

1. Firebase `calcmot`, Spark, Google habilitado. App **CalcMot Auth DEV**, pacote `br.com.calcmot.identitydev`.
2. O build deste checkout usa o certificado de upload no debug enquanto `keystore.properties` existe. SHA-1 comprovado: `FF:A7:EA:91:AD:22:03:E8:4E:C0:CE:5C:B8:98:EE:33:74:A2:1A:3C`. Cadastrado somente no app DEV, mediante confirmação do usuário. Se mudar o signer, conferir `signingReport` e o APK novamente. Não usar esse SHA como assinatura distribuída pela Play.
3. Baixar a configuração do app DEV para `app/src/identityDev/google-services.json` (ignorado pelo Git). Não substituir o arquivo do app principal. Deve conter projeto `calcmot`, client Android desse pacote/certificado e OAuth Web client de tipo 3. Não copiar ID inventado ou OAuth secret.
4. Endpoint DEV publicado e validado em 13/10: `https://calcmot-api-dev.eduardoangelo20001.workers.dev`. O relatório de integração registra a versão, binding D1 e chamada autenticada real; não usar domínio de produção.

## Build e instalação

Após a configuração real, na raiz do projeto (JDK 21 / SDK existente):

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:GRADLE_USER_HOME="$PWD/.gradle-user"
./gradlew.bat :app:assembleIdentityDev --no-daemon --max-workers=2 --console=plain '-Pkotlin.compiler.execution.strategy=in-process' '-Pcalcmot.identityDevWorkerUrl=https://calcmot-api-dev.eduardoangelo20001.workers.dev' --no-problems-report
```

Não excluir `processIdentityDevGoogleServices` desse build: compilar Kotlin sem a configuração não comprova um APK autenticável. Conferir package/signer com ferramentas Android antes de instalar. Escolher o APK do ABI do aparelho em `app/build/outputs/apk/identityDev/`; instalar somente esse pacote. Não instalar `debug` sobre produção, não aumentar versão e não desinstalar o app publicado para contornar conflito.

## Protocolo no dispositivo

Registrar resultados e horários em [S02-S04-INTEGRATION.md](../../../docs/product/S02-S04-INTEGRATION.md), sem tokens, emails de testers ou UID completo. A prova local é somente um resumo SHA-256 para comparação no aparelho, nunca telemetria.

| Teste | Resultado exigido |
|---|---|
| Entrar com Google voluntariamente | `LOGIN_OK`, sessão vinculada. Observar consentimento no aparelho; não expor token. |
| Abrir seleção e cancelar | `CANCELED`; sessão anterior e prova local mantidas. |
| Fechar e reabrir o pacote DEV | Sessão persistida, mesma prova local. |
| Sair e entrar na mesma conta | Mesmo UID/prova local do mesmo projeto. |
| Reinstalar somente DEV / segundo aparelho | Mesma conta recupera mesma prova local. Nunca remover produção. |
| Desligar internet durante troca/refresh | `OFFLINE` ou erro recuperável adequado; sem chamada no Core. Reativar rede depois. |
| Renovar sessão | `REFRESH_OK`; não equivale a testar token efetivamente expirado. Para expiração real, aguardar o prazo real e repetir, sem alterar relógio/token. |
| Sessão A, selecionar Google B | Troca/colisão recusada, prova local de A mantida, sem merge por email. Linking de credencial já usada exige cenário real controlado adicional; não declarar coberto apenas por essa reautenticação. |
| Consultar entitlement | Envia somente Firebase ID token por HTTPS ao host DEV fixado no build; redirects não são seguidos. Sem snapshot, `503 RETRY_LATER`. Nenhum benefício é criado. |
| Segundo tester / UID forjado | A API deve usar apenas o `sub` validado. Dois 503 com banco vazio não provam, sozinhos, leitura cruzada de registros. Completar a evidência real e conservar os testes de isolamento D1. |

Medir CPU no **Worker DEV** pelo painel Analytics, em janela correlacionada aos requests reais autenticados e às consultas D1. Tempo de resposta, CPU local e números do Worker do site não substituem CPU edge. Não habilitar captura de Authorization/PII para conseguir a evidência.
