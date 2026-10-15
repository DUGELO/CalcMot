# Growth V1 — convite discreto por uso recorrente

## Gatilho e proteção
- Só conta **um dia local em que um overlay de oferta foi efetivamente exibido**. Duas datas consecutivas habilitam a primeira oportunidade.
- Não solicita permissões de notificação. O cartão aparece apenas na **Home em primeiro plano**; nunca na Uber/99, sobre oferta ou ao dirigir.
- **Primeira oportunidade:** cartão na parte inferior da Home, acima da barra de navegação, com somente **Compartilhar** e **Agora não**.
- **Segunda oportunidade:** notificação Android silenciosa (canal de baixa importância) ao retornar à Home, **apenas** se notificações já estiverem liberadas. Se não estiverem, mostra cartão outra vez. A notificação nunca é enviada em background nem por OCR/serviço.
- Tocar na notificação abre o CalcMot e seu compartilhador Android; não envia mensagem automaticamente. A notificação é cancelada ao compartilhar por esse fluxo.

## Frequência persistida
- Mínimo de **14 dias entre oportunidades**.
- **Máximo de duas** oportunidades em janela móvel de 30 dias.
- Após duas dispensas, silêncio de **60 dias**.
- Após escolher compartilhar, silêncio de 60 dias, independentemente de envio real.
- **Não existe opção “Não mostrar novamente” nem opt-out permanente.** O usuário ainda pode dispensar com “Agora não” e pode bloquear o canal de convite nas configurações de notificação do Android. As notificações críticas de serviço não são afetadas.

## Implementação
- Estado salvo localmente em `calcmot_growth_share_v1`, sem dados de ofertas, contatos ou dados de identidade.
- Registros de overlay agendados em `Dispatchers.IO`; ações do card disparadas apenas pela Home.
- Não usa FCM, alarmes, novas permissões, link personalizado, Billing ou backend.
- Primeira instalação desta versão inicia sem histórico anterior (v3.5 não persistia os dias de overlays).

## Validação antes de produção
```powershell
git pull --ff-only origin feat/growth-organico-v1
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.ui.CalcMotGrowthSharingUiTest' --no-daemon
```
Testar botão inferior em Samsung com fontes maiores, retorno após dois dias reais, cooldowns da política, notificação em segundo ciclo, ausência de notificações quando bloqueadas e regressão Uber/99/ANR.
