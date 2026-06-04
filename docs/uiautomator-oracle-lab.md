# UIAutomator Oracle Lab

O UIAutomator no CalcMot e uma ferramenta de laboratorio, nao uma dependencia da versao Play Store.

## Trilhas

- Producao: `AccessibilityService` extrai o card pela arvore de acessibilidade. Se nada ficar completo e validado, o app fica em silencio e registra motivo claro.
- Laboratorio OracleOnly: `scripts/run-uiautomator-bridge.ps1 -OracleOnly` usa ADB shell para ler a arvore com UIAutomator, mas nao injeta candidatos no app. A producao real precisa resolver por acessibilidade, e o relatorio compara o oraculo contra os eventos do app.
- Laboratorio bridge: sem `-OracleOnly`, o script tambem envia candidatos ao APK debug. Isso e util para validar calculo/overlay, mas nao serve para medir beta ou Play Store.
- Use `-CaptureScreenshots` em sessoes de analise profunda para salvar imagem junto dos XMLs.

## Relatorio de sessao

O script gera `oracle-report.json` e `oracle-report.md` com estes campos:

- `uiautomator_complete_cards`: cards completos vistos pelo UIAutomator.
- `accessibility_service_enabled`: confirma se a acessibilidade do CalcMot estava ativa no inicio da sessao.
- `ocr_enabled`: sempre `false` neste laboratorio; screenshots podem ser salvos como evidencia, mas nao viram OCR nem candidatos do app.
- `internal_tree_complete_cards`: cards completos vistos pela arvore interna do AccessibilityService.
- `overlay_shown`: overlays exibidos durante a sessao.
- `production_overlay_shown`: overlays exibidos por rotas de producao, ignorando a ponte UIAutomator.
- `correct_overlay_cards`: cards unicos do oraculo que tiveram overlay de producao com o mesmo fingerprint.
- `missed_cards`: cards completos do oraculo que a producao nao conseguiu casar.
- `false_positive_count`: overlay de producao sem card correspondente no oraculo.
- `wrong_value_count`: overlay de producao perto de um card real, mas com valor divergente.
- `coverage_correct_percent`: percentual correto real, calculado por `correct_overlay_cards / uiautomator_complete_cards`.
- `average_latency_ms`: latencia media ate o candidato ser enviado pela ponte debug.
- `beta_overlay_coverage_percent`: percentual de overlays exibidos sobre cards unicos completos vistos pelo oraculo.
- `beta_ready_min_85`: indica se a sessao atingiu a meta minima de 85% para beta fechado, com zero falso positivo e zero valor divergente.

O arquivo `oracle-events.json` guarda, por frame, timestamp, fingerprint bruto, fingerprint canonico, bounds do card, textos, content-desc, classes, resource ids, ordem vertical das linhas semanticas vistas pelo UIAutomator e, quando ativado, o caminho do screenshot. O `production-events.json` guarda os eventos debug-only do app sem texto bruto. O `learning-backlog.json` classifica cada card como `TREE_FULL`, `DEBUG_BRIDGE`, `UIA_ONLY`, `FALSE_POSITIVE` ou `WRONG_VALUE`. O `oracle-fixtures.json` guarda os primeiros frames unicos completos para virar regressao automatizada.

## Sessao recomendada

```powershell
.\scripts\run-uiautomator-bridge.ps1 -DurationSeconds 1800 -IntervalMilliseconds 700 -OracleOnly -CaptureScreenshots
```

Essa e a sessao que vale para a meta de 85%. Se precisar validar rapidamente se o overlay desenha e calcula, rode sem `-OracleOnly`, mas marque o resultado como bridge/debug.

Antes de considerar qualquer resultado, confira se `accessibility_service_enabled` esta `true` e se o app nao avisou que o monitoramento esta pausado. Se a acessibilidade estiver desligada, a sessao ainda serve para coletar XML/screenshot do oraculo, mas nao mede captura real de producao.

## Criterio de decisao

- Se a arvore interna entregar 90% ou mais dos cards completos com baixa latencia, seguimos com ela como rota principal de captura.
- Se a arvore interna vier parcial com frequencia, corrigimos refresh, varredura de janelas, contentDescription, viewIdResourceName e scoring do parser.
- Se a arvore interna falhar demais, o app deve falhar de forma observavel e o corpus UIAutomator vira fonte de fixtures para melhorar a propria leitura da arvore.
- Beta fechado so vira producao ampla quando `correct_overlay_cards / uiautomator_complete_cards >= 85%`, `false_positive_count == 0` e `wrong_value_count == 0` em sessao real sem depender da ponte UIAutomator para exibir o overlay.

## Regra de release

Nenhuma chamada a `/system/bin/uiautomator`, nenhuma action `DEBUG_UIAUTOMATOR_*` e nenhum snapshot bruto entram na versao release.
