# Overlay V2 Decisao CalcMot - UX/Product Design

Data: 2026-06-11

## 1. Diagnostico do overlay atual

O overlay anterior funcionava, mas ainda parecia um mini-dashboard: badge, R$/km, R$/h, tempo, pill de impacto, titulo do impacto e subtexto competiam dentro de um bloco alto. O mesmo impacto aparecia duas vezes (`+R$ 0,25` no pill e novamente no subtexto), o que aumentava a carga visual sem aumentar a decisao.

Altura aproximada com impacto ligado: 304 px.

## 2. Analise emocional por estado

- OTIMA: forte, positiva, mas a versao anterior gastava espaco repetindo impacto.
- BOA: o valor `+R$ 0,25` e muito pequeno; visualmente precisa comunicar que esta dentro da meta, sem prometer grande ganho.
- ATENCAO: precisa ser entendido como limite, especialmente quando o problema e por hora.
- RUIM: deve ser inequívoco e direto, sem o usuario precisar ler duas linhas para entender que esta abaixo.

## 3. Proposta V2 aplicada

A V2 organiza o overlay como decisao de 1 segundo:

- Linha 1: badge + significado semantico.
- Linha 2: R$/km como numero principal.
- Linha 3: R$/h + duracao juntos.
- Linha 4: impacto financeiro em frase unica, quando ligado.

Altura aproximada com impacto ligado: 205-207 px. Reducao aproximada: 32% contra o overlay anterior. Com impacto desligado, o overlay fica em torno de 170 px.

## 4. Screenshots comparativos

- OTIMA: `.tmp/overlay-v2-decision-20260611/compare-great.png`
- BOA: `.tmp/overlay-v2-decision-20260611/compare-good.png`
- ATENCAO: `.tmp/overlay-v2-decision-20260611/compare-warning.png`
- RUIM: `.tmp/overlay-v2-decision-20260611/compare-bad.png`
- Metas real no aparelho: `.tmp/overlay-v2-decision-20260611/metas-impact-on.png`
- Impacto desligado: `.tmp/overlay-v2-decision-20260611/v2-good-no-impact.png`
- Arraste antes/depois: `.tmp/overlay-v2-decision-20260611/v2-drag-before.png` e `.tmp/overlay-v2-decision-20260611/v2-drag-after.png`

## 5. O que foi removido

- Card interno de impacto financeiro.
- Pill duplicada de impacto.
- Repeticao do valor de impacto no subtexto.
- Label `tempo total`.
- Labels secundarias redundantes `por km` e `por hora` quando o valor principal ja vem tipado.
- Handle colorido por estado; ele ficou neutro e mais discreto.

## 6. O que foi mantido

- Estados OTIMA, BOA, ATENCAO e RUIM.
- Cores semanticas por estado.
- R$/km como valor principal.
- R$/h e duracao.
- Impacto financeiro quando habilitado.
- Handle arrastavel.
- Logica de acessibilidade, parser, captura, OverlayManager e calculo financeiro.

## 7. Como a V2 melhora decisao em 1 segundo

A leitura agora segue de cima para baixo sem bifurcar: primeiro o estado, depois o significado, depois o numero principal, depois contexto de tempo/hora e por fim o impacto. O usuario nao precisa comparar card interno, pill e subtexto para chegar na decisao.

## 8. Arquivos alterados

- `app/src/main/java/br/com/calcmot/overlay/OverlayDesignSystem.kt`
- `app/src/main/java/br/com/calcmot/overlay/OverlayView.kt`
- `app/src/main/java/br/com/calcmot/model/FinancialImpact.kt`
- `app/src/androidTest/java/br/com/calcmot/ui/HomeScreenTest.kt`
- `app/src/test/java/br/com/calcmot/model/FinancialImpactCalculatorTest.kt`

## 9. Resultado dos testes

- `testDebugUnitTest`: passou.
- `assembleDebug`: passou.
- `connectedDebugAndroidTest`: passou no SM-A075M Android 15, 21 testes.
- Captura real no aparelho: passou para OTIMA, BOA, ATENCAO, RUIM.
- Latencia observada no overlay real: 69 ms, 34 ms, 20 ms e 31 ms nos quatro cenarios capturados.
- Arraste: confirmado visualmente; o overlay mudou de posicao pelo handle.

## 10. Riscos

- BOA com impacto muito pequeno, como `+R$ 0,25`, pode parecer positivo demais para uma margem praticamente nula.
- Em telas de fundo muito densas, o overlay compacto ainda pode cobrir conteudo importante se o usuario arrastar para o centro da tela.

## 11. Rollback

Rollback seguro: reverter os arquivos listados na secao 8. A alteracao e visual/microcopy; nao houve mudanca em AccessibilityService, parser, captura, OverlayManager, ProfitabilityCalculator, FinanceRepository ou regras de calculo.

## 12. Recomendacoes futuras

- Para BOA com impacto muito pequeno, considerar microcopy futura como `Na meta` ou `Margem pequena` em vez de `+R$ 0,25 acima da meta`.
- Avaliar regra visual para ocultar impacto monetario quando o delta absoluto for menor que um limite configuravel.
- Testar a legibilidade em oferta real sobre o app da Uber Driver, nao apenas sobre telas internas do CalcMot.
