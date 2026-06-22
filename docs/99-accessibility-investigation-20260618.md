# Investigação de acessibilidade da 99 Motorista

Data: 18 de junho de 2026

## Ambiente

- aparelho: Samsung SM-A075M;
- Android API observada pelos testes: 35;
- app: 99 Motorista `7.10.26`;
- package confirmado: `com.app99.driver`;
- activity principal: `com.didiglobal.driver.main.HomePageActivity`;
- serviço testado: `br.com.calcmot/.accessibility.UberAccessibilityService`.

## Configuração normal do CalcMot

O serviço já estava configurado sem filtro `android:packageNames` e com:

- `android:isAccessibilityTool="true"`;
- `android:canRetrieveWindowContent="true"`;
- `flagReportViewIds`;
- `flagRetrieveInteractiveWindows`;
- `flagIncludeNotImportantViews`;
- `TYPE_WINDOW_STATE_CHANGED`;
- `TYPE_WINDOW_CONTENT_CHANGED`;
- `TYPE_WINDOWS_CHANGED`;
- `notificationTimeout=0`.

O `dumpsys accessibility` confirmou `retrieveInteractiveWindows=true`, `fetchFlags=896` e capability de leitura de conteúdo.

## Evidência de evento e timing

Durante uma oferta real, o CalcMot recebeu eventos `TYPE_WINDOW_CONTENT_CHANGED` (`eventType=2048`) e executou o burst completo de leituras em:

- 0 ms;
- 80 ms;
- 160 ms;
- 300 ms;
- 500 ms;
- 750 ms;
- 1.000 ms.

Portanto, o bloqueio observado não foi causado por escutar somente `TYPE_WINDOW_STATE_CHANGED`.

Em uma segunda chamada real, às `19:54:38`, a árvore publicou:

- `Toque para selecionar uma`;
- `1 corrida(s) nas opções de corridas`.

O cartão completo foi aberto e registrado visualmente às `19:54:40`. Ele continha tarifa, valor por quilômetro, avaliação, dois blocos de tempo/distância e ação de aceite. Endereços não foram preservados.

Entretanto, a sessão interna usada nessa tentativa começou às `19:54:46`, cerca de seis segundos depois da abertura. Por isso, os snapshots dessa sessão não podem ser usados para concluir se os textos do cartão estavam ou não acessíveis no instante crítico.

## UIAutomator

O XML foi coletado com `uiautomator dump --compressed`, equivalente à árvore inspecionada pelo UI Automator Viewer.

Resultados:

- Home/Buscando: 11 nós da 99, nenhum texto ou content description;
- tela Solicitações sem card: 11 nós, nenhum texto;
- dump coletado depois de a chamada expirar: somente containers, sem os valores da oferta.

## Árvore interna do AccessibilityService

O laboratório interno inspecionou também propriedades que o dump comum não mostra:

- `text`;
- `contentDescription`;
- `stateDescription`;
- `paneTitle`;
- `hintText`;
- `tooltipText`;
- `extras`;
- IDs e bounds.

Nos snapshots ociosos foram observados containers como:

- `flutter_home_page_container`;
- `flutter_deal_gesture_container`;
- `flu_v2_btm_sht_top_container_landscape`;
- outros containers nativos/Flutter.

Na chamada real das `19:54:38`, a inspeção detectou a mensagem de seleção, mas não a tarifa nem os dois blocos. A coleta detalhada começou depois da abertura do cartão, então esse resultado é inconclusivo para o conteúdo completo.

## Leitura de múltiplas janelas

Foi identificado um retorno antecipado na coleta de roots: quando `rootInActiveWindow` pertencia ao app motorista, as demais janelas interativas não eram agregadas.

A correção foi restrita por origem:

- Uber: mantém exatamente o retorno antecipado anterior;
- 99: combina o root ativo com todos os roots de janelas da 99 e remove duplicatas.

Isso permite investigar overlays e bottom sheets próprios da 99 sem alterar o comportamento da Uber.

## Instrumentação exclusiva da 99

O build debug agora registra uma timeline estruturada somente quando o app ativo é a 99:

- `CALCMOT_99_EVENT`: tipo, package, classe, janela, content changes, texto, descrição e `event.source`;
- `CALCMOT_99_CAPTURE_SESSION`: geração, coalescência e janela que iniciou o burst;
- `CALCMOT_99_ROOTS`: root ativo, janelas selecionadas, package, classe e quantidade de filhos;
- `CALCMOT_99_SNAPSHOT`: contagem por propriedade semântica, classes, IDs, bounds, campos detectados e transição de resultado;
- `CALCMOT_99_NO_CANDIDATE`: melhor motivo e rejeições de cada source;
- `CALCMOT_99_APP_SWITCH`: troca entre pipelines.

Os registros persistentes ficam em `files/99-accessibility/session-*/timeline.ndjson`. Os snapshots completos ficam em `files/accessibility-lab/session-*`.

Essa telemetria não existe no source set release. A Uber não recebe os eventos adicionais nem o burst estendido.

Enquanto a 99 está ativa, o serviço também observa:

- `TYPE_VIEW_TEXT_CHANGED`;
- `TYPE_VIEW_FOCUSED`;
- `TYPE_VIEW_ACCESSIBILITY_FOCUSED`;
- `TYPE_ANNOUNCEMENT`;
- `TYPE_NOTIFICATION_STATE_CHANGED`.

Ao voltar para a Uber, o perfil é restaurado para os três eventos originais. O burst da Uber permanece em `0, 80, 160, 300, 500, 750 e 1.000 ms`; somente a 99 continua até `2.500 ms`.

Coleta:

```powershell
.\scripts\capture-accessibility-lab.ps1 -DriverApp 99 -DurationSeconds 900 -IntervalSeconds 1
```

## Diagnóstico da camada de renderização

Uma inspeção ao vivo em 19 de junho de 2026 comparou screenshot, UIAutomator, árvore interna, janelas, superfícies e recursos do APK `7.10.26`.

Na Home, o screenshot mostrava normalmente:

- saldo `R$0,00`;
- mapa e rótulos geográficos;
- banners promocionais;
- estado `Buscando`;
- botões e ícones.

No mesmo instante, o UIAutomator trouxe somente três `FrameLayout`, sem `text`, `content-desc` ou IDs úteis. A árvore interna era maior, mas continuava sem a semântica visual:

- 61 nós em um estado completo da Home;
- 25 IDs de recurso;
- zero nós com `text`;
- zero nós com `contentDescription`;
- zero nós com `stateDescription`;
- zero nós com extras textuais.

O `SurfaceFlinger` confirmou uma composição separada:

```text
SurfaceView[com.app99.driver/com.didiglobal.driver.main.HomePageActivity]
SurfaceView[...](BLAST)
```

O APK confirmou que os IDs observados são hosts de renderização:

- `main_flutter_flutter_root` é `FlutterFrameLayout` ou `OrderShowFlutterFrameLayout`;
- `flutter_deal_gesture_container` é um container de gesto sem conteúdo semântico próprio;
- `broad_order_container` hospeda a experiência de solicitações;
- `main_flutter_native_root` é o fallback nativo.

Os layouts `fragment_broad_order_flutter.xml` e `fragment_main_flutter.xml` criam esses hosts, mas não contêm os valores de uma oferta.

Os templates `assets/broad_order_card_ui2.xml` e `assets/broad_order_card_ui3.xml` mostram onde os dados realmente entram:

- `.order_detail.income_info.driver_income_symbol`;
- `.order_detail.income_info.driver_income`;
- `.order_detail.passenger_info.star_level`;
- `.map_detail.starting.eta.value` e `.unit`;
- `.map_detail.starting.eda.value` e `.unit`;
- `.map_detail.destination.eta`;
- `.map_detail.destination.eda`;
- `.map_detail.destination.title`.

Esses campos são preenchidos em runtime e desenhados pelo renderizador do card. Os IDs Android não escondem a tarifa ou as distâncias: eles apenas identificam o container que recebe a superfície gráfica.

## Controle positivo

Durante a mesma sessão, telas nativas e híbridas secundárias da 99 expuseram texto normalmente. Por exemplo, a árvore publicou `Nova atualização de recurso` e eventos de uma `WebView`.

Isso demonstra que:

- o AccessibilityService recebe eventos da 99;
- a recursão encontra textos quando a tela os publica;
- o filtro por package está correto;
- o problema está concentrado na superfície Flutter/customizada da Home e das ofertas.

Não surgiu uma nova chamada verdadeira durante essa janela de escuta. Portanto, ainda não é correto declarar a captura do card real como aprovada. A 99 foi deixada aberta em `HomePageActivity`, com o serviço vinculado e a telemetria pronta para a próxima solicitação.

## Experimento com AccessibilityBridge forçada

Em 19 de junho de 2026, o build debug passou a declarar:

```xml
android:canRequestTouchExplorationMode="true"
```

A flag `FLAG_REQUEST_TOUCH_EXPLORATION_MODE` é aplicada dinamicamente apenas quando a 99 está em primeiro plano. Ao abrir a Uber, a flag é removida e o perfil retorna aos três eventos originais.

O `dumpsys accessibility` confirmou na 99:

- `requestTouchExplorationMode=true`;
- capability `3`;
- `TouchExplorer` ativo;
- `fetchFlags=896`.

O serviço procurou e tentou focar:

- `main_flutter_flutter_root`;
- `flutter_deal_gesture_container`;
- `broad_order_container`.

Na Home observada, `flutter_deal_gesture_container` estava presente e retornou:

- `importantForAccessibility=true`;
- `ACTION_ACCESSIBILITY_FOCUS=true` na primeira tentativa;
- `accessibilityFocused=true`;
- filhos antes do foco: `1`;
- filhos depois do foco e `refresh()`: `1`;
- texto vazio;
- `contentDescription` vazia.

O evento gerado foi `TYPE_VIEW_ACCESSIBILITY_FOCUSED`, mas `event.text` e `event.contentDescription` permaneceram vazios.

Depois do foco, foram analisados 141 snapshots:

- zero nós com texto;
- zero nós com descrição;
- zero nós com estado;
- zero nós com extras textuais.

O dump não compactado `99-verbose-touch-exploration.xml` teve:

- 53 nós estruturais;
- zero atributos `text` preenchidos;
- zero atributos `content-desc` preenchidos;
- presença de `flutter_deal_gesture_container`;
- ausência de filhos semânticos Flutter.

Portanto, o host Android aceitou o foco, mas o renderizador usado nessa Home não publicou uma árvore semântica interna. O foco apenas selecionou o container inteiro, visível como uma borda azul ao redor da tela.

A Estratégia B também permanece ativa. Nesta execução não houve `TYPE_ANNOUNCEMENT`; foram recebidos dois `TYPE_VIEW_ACCESSIBILITY_FOCUSED`, ambos sem payload textual.

O resultado ainda deve ser repetido durante uma chamada verdadeira, pois a 99 pode construir uma árvore diferente quando o card de oferta está ativo.

## Experimento com TalkBack

O TalkBack da Samsung foi ativado junto com o CalcMot.

O sistema confirmou:

- `requestTouchExplorationMode=true` no TalkBack;
- `TouchExplorer` ativo;
- `retrieveInteractiveWindows=true`;
- `TYPES_ALL_MASK`.

Mesmo nesse estado, a Home ociosa da 99 continuou com 11 nós e zero textos.

## Experimento com o próprio CalcMot solicitando touch exploration

Foi gerado temporariamente um build debug com:

- `android:canRequestTouchExplorationMode="true"`;
- `FLAG_REQUEST_TOUCH_EXPLORATION_MODE`;
- `AccessibilityEvent.TYPES_ALL_MASK`.

O `dumpsys accessibility` confirmou:

- `requestTouchExplorationMode=true`;
- capabilities `3`;
- `TouchExplorer` ativo;
- `TYPES_ALL_MASK`;
- `fetchFlags=896`.

Resultado na Home ociosa: 11 nós da 99 e zero textos.

Este experimento isolado não é suficiente para concluir como o card se comporta durante a entrada de uma chamada verdadeira. A configuração precisa permanecer ativa até coincidir com uma oferta real.

## Conclusão provisória

As hipóteses abaixo foram eliminadas:

- ausência de `isAccessibilityTool`;
- falta de `canRetrieveWindowContent`;
- falta de `flagIncludeNotImportantViews`;
- falta de `flagRetrieveInteractiveWindows`;
- filtro incorreto de package;
- package errado;
- ausência de `TYPE_WINDOW_CONTENT_CHANGED`;
- janela de captura curta;
- dependência de TalkBack;
- dependência de touch exploration;
- falta de leitura de `contentDescription`, `stateDescription`, hints ou extras.

Na versão `7.10.26`, as telas ociosas observadas não publicaram a semântica visual necessária. Isso não significa que a 99 inteira seja inacessível: telas híbridas secundárias expõem texto normalmente.

A conclusão sobre o card de chamada permanece pendente até capturar a árvore bruta no mesmo instante em que uma chamada verdadeira estiver visível. A tentativa das `19:54:40` perdeu essa janela por aproximadamente seis segundos.

O material coletado sustenta uma solicitação técnica à 99 para que tarifa, tempo, distância e ação recebam semântica Android (`Semantics`, labels e roles) na tela de solicitações.
