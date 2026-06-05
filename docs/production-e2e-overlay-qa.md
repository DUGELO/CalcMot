# CalcMot Production E2E Overlay QA

Este roteiro mede cobertura real do overlay em producao. Ele nao muda arquitetura, nao usa OCR e nao injeta dados do UIAutomator no app.

## Preflight

Antes de rodar, feche PiP, bolhas e overlays externos, especialmente chamadas/WhatsApp/BlockerHero. O teste bloqueia por padrao se detectar condicoes que contaminam a medicao.

Confirme no celular:

- CalcMot com status pronto.
- Monitoramento ligado.
- Acessibilidade do CalcMot ativa.
- Uber Driver aberto e online.

## Rodada Curta

Use:

```powershell
.\scripts\run-production-e2e-qa.ps1 -DurationSeconds 900 -MaxUniqueCards 10 -IntervalMilliseconds 700
```

O script gera:

- `logcat-live.txt` com `logcat -v time`.
- `screenrecord/*.mp4` em blocos.
- `oracle/*/oracle-report.json` e `.md`.
- XMLs e screenshots do UIAutomator em modo `OracleOnly`.
- `e2e-summary.json` e `.md`.

## Regra De Sucesso

Nao conte sucesso apenas por log `OVERLAY_VISIBLE_TO_USER`.

Um card conta como sucesso somente quando:

- aparece no video;
- aparece no oraculo UIAutomator;
- o app registra `CARD_PATH complete=true candidate=true`;
- o overlay fica visivel no video em tempo util;
- os valores visiveis estao corretos;
- o overlay some corretamente quando o card acaba.

## Classificacao De Perdas

Use estas categorias:

- `NO_CARD_PATH`
- `INCOMPLETE_TREE`
- `PARSER_REJECTED`
- `OVERLAY_REQUESTED_NOT_VISIBLE`
- `EARLY_HIDE_WEAK_CONTEXT`
- `STRONG_CONTEXT_HIDDEN_OK`
- `STUCK_OVERLAY`
- `WRONG_VALUE`
- `FALSE_POSITIVE`

## Gate Beta

Pronto para beta fechado exige duas sessoes consecutivas com pelo menos 10 cards cada:

- `correct_overlay_coverage >= 85%`
- `wrong_value_rate = 0`
- `false_positive_rate = 0`
- `early_hide_on_weak_context = 0`
- `stuck_overlay_count = 0`
- `crash_count = 0`
- `ANR_count = 0`
