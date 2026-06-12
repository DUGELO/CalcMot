# Relatorio - Bank Safe Mode + Latencia

## Resumo executivo

Esta rodada implementa Bank Safe Mode tecnico, Overlay Guard critico e trace de latencia ponta a ponta sem alterar o Overlay V2.3, parser, calculos ou classificacao.

## Bank Safe Mode

Pacotes permitidos:

- `com.ubercab.driver`
- `com.ubercab`
- `br.com.taxis99`
- `com.app99.driver`

Pacotes fora da whitelist nunca podem capturar/processar roots, janelas, arvore, parser ou calculo.

A remocao visual forcada do overlay fica restrita a apps criticos/sensiveis, como bancos, pagamentos, Configuracoes e navegadores principais. Apps comuns fora da whitelist pausam captura e limpam pipeline, mas nao matam o overlay automaticamente.

Comportamento em apps criticos:

- o pipeline retorna antes de ler `rootInActiveWindow`, `windows` ou `windowsOnAllDisplays`;
- jobs de captura/polling sao cancelados;
- `CaptureCoordinator` e estado de candidato sao limpos;
- overlay e debug overlay sao removidos;
- logs emitidos: `CALCMOT_SAFE_MODE`, `CALCMOT_PAUSED_NON_ALLOWED_APP`, `BANK_SAFE_MODE_ACTIVE`, `OVERLAY_FORCE_HIDDEN_NON_ALLOWED_APP`.

Comportamento em apps comuns fora da whitelist:

- o pipeline tambem retorna antes de ler roots/windows/arvore;
- jobs de captura/polling sao cancelados;
- `CaptureCoordinator` e estado de candidato sao limpos;
- overlay nao e removido automaticamente;
- log emitido: `CALCMOT_CAPTURE_PAUSED_NON_DRIVER_USER_APP`.

Eventos de `br.com.calcmot`, `unknown`, `com.android.systemui` e `com.samsung.android.app.smartcapture` sao ignorados sem safe idle visual para evitar sumicos falsos do overlay.

## Overlay Guard

`OverlayManager` recebe o pacote em foreground e mantem o ultimo app motorista confiavel por uma janela curta de `2_000ms`. Pacotes proprios, desconhecidos e transitorios sao ignorados para bloqueio visual. Pacotes criticos removem overlay imediatamente e emitem `OVERLAY_BLOCKED_USER_APP`.

## Latencia

Foi adicionado trace por oferta com `SystemClock.elapsedRealtime()` e `traceId`, cobrindo:

- evento de acessibilidade;
- confirmacao de pacote permitido;
- leitura de root/window;
- extracao de arvore;
- parsing/candidato completo;
- estabilidade;
- request de overlay;
- add/update de WindowManager;
- primeiro draw;
- visibilidade ao usuario.

O fechamento emite `CALCMOT_LATENCY_TRACE_SUMMARY` com `totalToVisibleMs`, `requestToVisibleMs`, `rootReadMs`, `extractMs`, `stabilityMs`, `largestStage` e `latencyClass`.

## Correcao minima aplicada

Para reduzir risco de atrasos de ate 5s sem alterar parser ou calculo:

- burst normal limitado a tentativas ate `1_000ms`;
- janela de coalescencia reduzida para `1_000ms`;
- roots de app motorista sao priorizadas antes de roots nao verificadas;
- a active root motorista e inspecionada antes de montar snapshots de todas as janelas;
- a varredura para no primeiro candidato completo confiavel;
- snapshot combinado e usado apenas como fallback;
- candidato completo confiavel com botao de acao continua podendo bypassar estabilidade, comportamento ja existente.

## Validacao executada

- `.\gradlew.bat testDebugUnitTest` passou.
- `.\gradlew.bat assembleDebug` passou.
- `.\gradlew.bat bundleRelease` passou.
- `.\gradlew.bat connectedDebugAndroidTest` passou em SM-A075M Android 15.

## Pendente de sessao real

A meta final de performance precisa ser confirmada com 10 a 20 ofertas reais no app de motorista:

- `totalToVisibleMs <= 1000ms`;
- p95 `<= 700ms`;
- media `<= 300ms`;
- 0 casos de aproximadamente 5s.

Usar logcat filtrando `CALCMOT_LATENCY_TRACE`, `CALCMOT_LATENCY_TRACE_SUMMARY`, `CALCMOT_SAFE_MODE`, `CALCMOT_PACKAGE_GUARD`, `CALCMOT_ROOT_READ`, `CALCMOT_TREE_EXTRACT`, `CALCMOT_STABILITY_GATE`, `CALCMOT_OVERLAY_REQUEST`, `CALCMOT_OVERLAY_WINDOW`, `CALCMOT_OVERLAY_DRAW` e `OVERLAY_BLOCKED_NON_ALLOWED_APP`.
