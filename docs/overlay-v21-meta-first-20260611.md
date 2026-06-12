# Overlay V2.1 Meta First CalcMot

Data: 2026-06-11

## 1. O que estava errado na V2

A V2 ficou compacta e removeu o dashboard interno, mas a ordem ainda dizia: primeiro R$/km, depois meta. Quando o impacto financeiro esta ligado, isso deixa o principal diferencial do CalcMot com cara de rodape. Tambem havia um problema semantico: o estado visual intermediario ainda usava um label de alerta, quando a corrida era apenas media/no limite.

## 2. O que foi ajustado na V2.1

- O label visual intermediario virou `MÉDIA`.
- Com impacto ligado, a ordem agora e: badge + significado, impacto financeiro, R$/km, R$/h + tempo.
- Com impacto desligado, a ordem continua: badge + significado, R$/km, R$/h + tempo.
- Impacto positivo menor que R$ 1,00 aparece como `Na meta`.
- Padding e espacamentos foram reduzidos para manter altura compacta.

## 3. Por que MÉDIA e melhor neste contexto

`MÉDIA` comunica qualidade da corrida. O label anterior soava como alerta operacional, mesmo quando a oferta estava apenas no limite da meta. A nova taxonomia fica: `ÓTIMA`, `BOA`, `MÉDIA`, `RUIM`.

## 4. Como o impacto financeiro ganhou protagonismo

O impacto saiu do rodape e passou a ser a primeira informacao numerica quando a meta esta ligada. Isso responde primeiro: esta acima ou abaixo da minha meta? O R$/km continua grande logo abaixo para confirmar a decisao.

## 5. Como BOA com margem pequena foi tratada

O caso `+R$ 0,25 acima da meta` agora aparece como `Na meta`. Isso evita falsa sensacao de grande vitoria sem alterar calculo nem classificacao.

## 6. Comparativo visual

- V2 vs V2.1 OTIMA: `.tmp/overlay-v21-meta-first-20260611/final-compare-v2-v21-great.png`
- V2 vs V2.1 BOA margem pequena: `.tmp/overlay-v21-meta-first-20260611/final-compare-v2-v21-good-small.png`
- V2 vs V2.1 MEDIA: `.tmp/overlay-v21-meta-first-20260611/final-compare-v2-v21-medium.png`
- V2 vs V2.1 RUIM: `.tmp/overlay-v21-meta-first-20260611/final-compare-v2-v21-bad.png`
- Impacto desligado: `.tmp/overlay-v21-meta-first-20260611/final-compare-v2-v21-no-impact-good.png`
- Grade V2.1 impacto ligado: `.tmp/overlay-v21-meta-first-20260611/final-grid-v21-impact-on.png`
- Grade V2.1 impacto desligado: `.tmp/overlay-v21-meta-first-20260611/final-grid-v21-impact-off.png`
- BOA margem relevante: `.tmp/overlay-v21-meta-first-20260611/v21-good-relevant-impact.png`
- BOA margem pequena: `.tmp/overlay-v21-meta-first-20260611/v21-good-small-impact.png`
- Arraste: `.tmp/overlay-v21-meta-first-20260611/v21-drag-before.png` e `.tmp/overlay-v21-meta-first-20260611/v21-drag-after.png`

## 7. Altura V2 vs V2.1

- V2 com impacto ligado: aproximadamente 205-207 px.
- V2.1 com impacto ligado: aproximadamente 198-200 px.
- V2.1 com impacto desligado: aproximadamente 170-171 px.

Resultado: Meta First venceu sem aumentar a altura.

## 8. Arquivos alterados

- `app/src/main/java/br/com/calcmot/overlay/OverlayDesignSystem.kt`
- `app/src/main/java/br/com/calcmot/overlay/OverlayView.kt`
- `app/src/main/java/br/com/calcmot/ui/FinanceScreen.kt`
- `app/src/test/java/br/com/calcmot/overlay/TripQualityTest.kt`

## 9. Testes executados

- `testDebugUnitTest`: passou.
- `assembleDebug`: passou.
- `connectedDebugAndroidTest`: passou no SM-A075M Android 15, 21 testes.
- Captura real no aparelho: OTIMA, BOA margem relevante, BOA margem pequena, MEDIA, RUIM.
- Impacto desligado: validado em corrida alta, BOA, MEDIA e RUIM.
- Arraste pelo handle: confirmado visualmente.
- Latencia observada: 18 ms a 74 ms nos cenarios capturados.

## 10. Riscos

- O modo sem impacto nao gera `ÓTIMA` hoje, porque a taxonomia sem meta vem da regua de lucratividade `BOA/MÉDIA/RUIM`. Alterar isso exigiria mudar regra de classificacao, o que ficou fora do escopo.
- `Na meta` e mais honesto para margem pequena, mas oculta o valor exato. Se o produto quiser transparencia numerica, a alternativa futura e `+R$ 0,25 · na meta`.

## 11. Rollback

Rollback seguro: reverter os quatro arquivos listados na secao 8. Nao houve alteracao em parser, captura, AccessibilityService, OverlayManager, state machine, calculo financeiro, ProfitabilityCalculator ou FinanceRepository.

## 12. Recomendacao final de produto

Manter V2.1. Ela responde melhor a pergunta principal com meta ligada: essa corrida me coloca acima ou abaixo da minha meta? O R$/km continua forte para confirmacao, e o overlay preserva a compacidade premium da V2.
