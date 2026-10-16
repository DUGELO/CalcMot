# CalcMot — avaliação Google Play baseada em engajamento

## Funcionalidade
- **Acesso permanente:** menu lateral > Avaliar CalcMot abre `market://details?id=br.com.calcmot`, com fallback HTTPS. Não consome a cota da API nativa.
- **Convite orgânico:** `com.google.android.play:review:2.0.2` via `ReviewManagerFactory`, exibido exclusivamente se o Google Play permitir. Sem modal prévio, perguntas sobre satisfação, incentivo por nota ou customização do cartão nativo.
- Elegibilidade por **ofertas efetivamente exibidas**, não simplesmente por abertura do aplicativo: mínimo **5 dias consecutivos** de uso confirmado; intervalo de **21 dias** entre solicitações. Para engajamento forte (pelo menos **10 dias consecutivos**), intervalo de **14 dias**.
- **Sem máximo de solicitações por instalação.** Cada nova tentativa exige uso ativo e o período mínimo correspondente. A cota adicional do Google Play permanece independente; intervalos curtos podem não resultar em diálogo visível.
- **Exclusão mútua:** somente na Home pronta e em primeiro plano; compartilhar tem prioridade. Nunca solicitar sobre Uber/99 ou durante uma oferta.
- Persistência em `calcmot_play_review_v1`: data da última solicitação e quantidade de chamadas. A contagem é de tentativas, NÃO de avaliações recebidas. Estados antigos com `attempts = 2` não são bloqueados pela nova política.

## Avaliações já feitas
A API pública de In-App Review **não revela** se uma avaliação foi feita, se o cartão apareceu ou que nota foi dada. A arquitetura atual não dispõe desse sinal confiável. Não inferir, não consultar identidade do Google Play e não marcar como "já avaliou" apenas após um callback. O próprio Google Play pode deixar de mostrar o card, conforme sua cota interna.

## Testes
```powershell
git pull --ff-only origin feat/growth-organico-v1
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.ui.CalcMotGrowthSharingUiTest' --no-daemon
```
O APK Debug instalado via ADB pode não exibir a janela oficial. Validar com instalação elegível pela Play Store (teste interno), preservar permissão do usuário para dispensar e nunca reportar "avaliou" quando apenas houve tentativa.

Documentação: https://developer.android.com/guide/playcore/in-app-review
