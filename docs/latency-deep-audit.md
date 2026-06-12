# CalcMot - Auditoria profunda de latencia

Esta auditoria mede o pipeline real do overlay sem alterar parser, calculos, classificacao ou Overlay V2.3.

## Coleta

1. Instalar debug e habilitar o AccessibilityService.
2. Limpar logcat:

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
```

3. Quando a oferta aparecer visualmente, marcar o T-1 debug-only:

```powershell
.\scripts\mark-latency-visual-probe.ps1 -Label offer-visible -Source manual
```

4. Coletar 10 a 20 ofertas reais no Uber Driver e, se possivel, 99 Driver.
5. Salvar logcat bruto:

```powershell
& $adb logcat -d -v time CalcMotLatency:W UberReader:W OverlayManager:W '*:S' > build\reports\calcmot-latency\latency-logcat.txt
```

6. Gerar relatorio:

```powershell
.\scripts\analyze-calcmot-latency.ps1 -InputPath build\reports\calcmot-latency\latency-logcat.txt -MinTraceCount 10
```

## Logs obrigatorios

- `CALCMOT_LATENCY_TRACE`
- `CALCMOT_LATENCY_TRACE_SUMMARY`
- `CALCMOT_LATENCY_METRIC`
- `CALCMOT_ROOT_READ`
- `CALCMOT_TREE_EXTRACT`
- `CALCMOT_STABILITY_GATE`
- `CALCMOT_OVERLAY_REQUEST`
- `CALCMOT_OVERLAY_WINDOW`
- `CALCMOT_OVERLAY_DRAW`

## Metas

- media `< 300ms`
- p95 `< 700ms`
- maximo `< 1000ms`
- zero casos `> 2s`
- zero casos proximos de `5s`

## Interpretacao

O relatorio `latency-report.md` lista:

- tabela agregada por trace;
- traces completos e incompletos;
- p95/max do `totalToVisibleMs`;
- Top 10 fontes de latencia por metrica granular;
- maior estagio por trace.

Nenhuma correcao final deve ser aplicada sem evidenciar o gargalo no relatorio.
