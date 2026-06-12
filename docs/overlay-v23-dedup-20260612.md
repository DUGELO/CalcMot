# Overlay V2.3 Deduplicacao Semantica

Data: 2026-06-12

## Objetivo

Remover duplicatas cognitivas do overlay mantendo a hierarquia Meta First da V2.1.

## Estrutura com impacto ligado

1. Badge de classificacao.
2. Impacto financeiro.
3. R$/km.
4. R$/h e tempo.

O badge comunica apenas classificacao. A linha de impacto usa `ANCORA · IMPACTO`, com ancoras `META`, `TEMPO` ou `KM`.

## Excecao de margem pequena

Para margem positiva muito pequena, usar apenas `Na meta`.

`Na meta` e uma excecao intencional ao formato `ANCORA · IMPACTO`, porque nesse caso o valor monetario nao e relevante o suficiente para aparecer como protagonista.

## Validacao visual

- OTIMA: `.tmp/overlay-v23-dedup-20260612/v23-great-impact-final.png`
- BOA margem relevante: `.tmp/overlay-v23-dedup-20260612/v23-good-relevant-impact.png`
- BOA margem pequena: `.tmp/overlay-v23-dedup-20260612/v23-good-small-impact-final.png`
- MEDIA por tempo: `.tmp/overlay-v23-dedup-20260612/v23-medium-time-impact.png`
- MEDIA por km: `.tmp/overlay-v23-dedup-20260612/v23-medium-km-impact.png`
- RUIM: `.tmp/overlay-v23-dedup-20260612/v23-bad-impact.png`

## Resultado

Cada linha ficou com uma funcao unica: classificacao, impacto, eficiencia por distancia, produtividade por tempo. Nao houve alteracao de calculo, parser, captura, state machine, OverlayManager ou regras de classificacao.
