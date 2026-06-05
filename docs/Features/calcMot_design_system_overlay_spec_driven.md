# Spec-Driven Development — CalcMot Design System para Overlay

**Produto:** CalcMot  
**Módulo:** Design System / Overlay / Impacto Financeiro  
**Status:** Draft estruturado para implementação  
**Prioridade:** P0 após estabilização funcional do overlay  
**Objetivo:** elevar percepção de valor, clareza visual e confiança do motorista sem prejudicar a experiência na tela da Uber.

---

## 1. Visão

O CalcMot já possui um overlay funcional baseado no conceito universal de semáforo:

- verde: oferta boa;
- amarelo/laranja: atenção;
- vermelho: oferta ruim.

Com a evolução do produto e a chegada da classificação **ÓTIMA**, o design system precisa evoluir para suportar uma categoria premium, visualmente distinta e emocionalmente forte.

A classificação **ÓTIMA** deve parecer especial, rara e valiosa. Ela não deve ser apenas um verde mais forte. Deve comunicar:

```text
essa oferta é acima da média
essa é uma oportunidade premium
essa corrida merece atenção positiva imediata
```

A proposta inicial é usar uma cor premium, como **roxo premium** ou **azul royal**, apoiada por teoria das cores, contraste, acessibilidade e uso cuidadoso de opacidade.

---

## 2. Problema

O motorista está dirigindo ou parado em ambiente de trânsito. Ele precisa interpretar o overlay em frações de segundo.

O design precisa responder:

```text
essa corrida é ruim?
essa corrida exige atenção?
essa corrida é boa?
essa corrida é ótima?
```

Sem poluir a tela, sem esconder dados importantes da Uber e sem forçar leitura detalhada.

O overlay deve ser:

- rápido de entender;
- visualmente confiável;
- premium;
- leve;
- consistente;
- legível;
- pouco intrusivo;
- compatível com tela clara/escura da Uber;
- seguro em uso real.

---

## 3. Objetivos do Design System

### O1 — Clareza instantânea

O motorista deve entender a qualidade da oferta em até 1 segundo.

### O2 — Valor percebido

O design deve parecer produto premium, não gambiarra.

### O3 — Consistência

Todas as telas e componentes do CalcMot devem usar os mesmos tokens visuais.

### O4 — Não atrapalhar a Uber

O overlay deve manter opacidade controlada e ocupar pouco espaço visual.

### O5 — Escalabilidade

O design system deve suportar:

- overlay atual;
- impacto financeiro;
- dashboard;
- configurações;
- histórico;
- onboarding.

---

## 4. Princípios de UI/UX

### P1 — Semáforo universal preservado

O padrão vermelho/amarelo/verde deve continuar porque é intuitivo.

### P2 — Categoria ÓTIMA precisa ser premium

A classificação ÓTIMA deve ter cor própria, diferente do verde.

Candidatas:

```text
Roxo premium
Azul royal
Azul-violeta
```

### P3 — Opacidade controlada

O overlay deve ter fundo com opacidade suficiente para legibilidade, mas não pode bloquear a percepção da tela da Uber.

Recomendação inicial:

```text
background opacity: 82% a 90%
borda/acento: 100%
conteúdo textual: 95% a 100%
```

### P4 — Design por hierarquia

O motorista deve ler primeiro:

```text
classificação
R$/km
R$/hora
impacto financeiro
tempo total
```

### P5 — Menos é mais

O overlay deve mostrar somente o necessário no momento da decisão.

### P6 — Cor não pode ser o único sinal

Além da cor, usar:

- texto;
- ícone;
- badge;
- contraste;
- peso tipográfico.

Isso ajuda acessibilidade e motoristas com daltonismo.

---

## 5. Semântica de cores

### RUIM

**Cor:** vermelho.  
**Significado:** risco, prejuízo, abaixo da meta, evitar.

Uso:

```text
corrida abaixo das metas
impacto financeiro negativo
baixa atratividade
```

### ATENÇÃO

**Cor:** amarelo/âmbar/laranja.  
**Significado:** cautela, avaliar, passa em uma métrica mas falha em outra.

Uso:

```text
boa por hora mas ruim por km
boa por km mas ruim por hora
impacto misto
```

### BOA

**Cor:** verde.  
**Significado:** aprovado, seguro, dentro da meta.

Uso:

```text
passa em R$/km e R$/hora
corrida aceitável
```

### ÓTIMA

**Cor:** roxo premium ou azul royal.  
**Significado:** oportunidade premium, acima da média, destaque positivo.

Uso:

```text
passa nas metas com folga
>= 20% acima da meta de km e hora
oferta rara e valiosa
```

---

## 6. Decisão de cor para ÓTIMA

### Opção A — Roxo Premium

Vantagens:

- comunica exclusividade;
- diferencia muito do verde;
- cria sensação de VIP;
- combina com ideia de oportunidade especial;
- não conflita com vermelho/amarelo/verde.

Riscos:

- se muito escuro, pode perder legibilidade;
- se muito neon, pode parecer jogo/gamificação excessiva.

### Opção B — Azul Royal

Vantagens:

- comunica confiança e estabilidade;
- tem aparência profissional;
- bom contraste em UI escura;
- passa sensação de produto premium.

Riscos:

- pode parecer informativo, não necessariamente “ótimo”;
- pode competir com elementos azuis da interface da Uber ou Android.

### Decisão inicial recomendada

Usar **roxo premium** como cor principal da categoria ÓTIMA.

Racional:

```text
Vermelho, amarelo e verde já ocupam o universo do semáforo.
A categoria ÓTIMA precisa sair do semáforo comum e parecer uma camada acima.
Roxo premium comunica status, raridade e oportunidade especial.
```

---

## 7. Tokens de cor — proposta inicial

> Os valores abaixo são proposta inicial. Devem ser testados em tela real com opacidade, brilho e contraste.

```kotlin
object CalcMotColors {
    val Bad = Color(0xFFE53935)
    val Warning = Color(0xFFFFB300)
    val Good = Color(0xFF2E7D32)
    val Great = Color(0xFF6D3BFF) // Roxo premium

    val RoyalBlueAlternative = Color(0xFF2457FF)

    val OverlayBackground = Color(0xE61A1A1A) // ~90% opacidade
    val OverlayBackgroundSoft = Color(0xD91A1A1A) // ~85% opacidade
    val SurfaceElevated = Color(0xF2232323)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE0E0E0)
    val TextMuted = Color(0xFFBDBDBD)

    val Divider = Color(0x33FFFFFF)
}
```

---

## 8. Tokens de opacidade

```kotlin
object CalcMotOpacity {
    const val OverlayStrong = 0.90f
    const val OverlayMedium = 0.86f
    const val OverlaySoft = 0.82f

    const val AccentFull = 1.00f
    const val SecondaryText = 0.82f
    const val Disabled = 0.45f
}
```

Regras:

- overlay em card ativo: 86% a 90%;
- overlay em modo compacto: 82% a 86%;
- borda/acento por classificação: 100%;
- texto principal: 100%;
- texto secundário: 80% a 90%.

---

## 9. Tokens tipográficos

O overlay precisa ser legível em tela pequena e em movimento.

```kotlin
object CalcMotTypography {
    val ValuePrimary = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )

    val MetricLabel = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )

    val MetricValue = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )

    val ImpactMessage = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )

    val ImpactSubMessage = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}
```

---

## 10. Tokens de espaçamento

```kotlin
object CalcMotSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp

    val OverlayPadding = 10.dp
    val MetricGap = 6.dp
    val SectionGap = 8.dp
}
```

---

## 11. Tokens de borda e shape

```kotlin
object CalcMotShape {
    val OverlayRadius = 16.dp
    val BadgeRadius = 999.dp
    val CardRadius = 20.dp
}
```

Regras:

- overlay deve parecer moderno, mas não exageradamente arredondado;
- badges de classificação podem usar pill shape;
- borda lateral ou topo pode indicar classificação.

---

## 12. Tokens de elevação/sombra

O overlay precisa se destacar da Uber sem parecer pesado.

```kotlin
object CalcMotElevation {
    val Overlay = 8.dp
    val Floating = 12.dp
}
```

Usar sombra com cuidado. Em overlay com opacidade, sombra excessiva pode poluir a tela.

---

## 13. Componentes do Design System

### DS01 — CalcMotOverlayContainer

Container base do overlay.

Responsável por:

- fundo opaco/translúcido;
- borda;
- shape;
- padding;
- área arrastável, se existir;
- estado visual por classificação.

Critérios:

```text
não bloquear leitura da Uber
não ocupar área excessiva
suportar modo compacto
```

---

### DS02 — OfferQualityBadge

Badge principal da classificação:

```text
ÓTIMA
BOA
ATENÇÃO
RUIM
```

Critérios:

- cor baseada na classificação;
- texto curto;
- alto contraste;
- não depender apenas de cor.

---

### DS03 — MetricRow

Linha de métrica:

```text
R$ 1,21/km
R$ 71,36/h
58 min
```

Critérios:

- números destacados;
- labels menores;
- alinhamento consistente.

---

### DS04 — FinancialImpactBlock

Bloco opcional de impacto financeiro.

Exemplos:

```text
R$ 27,92 abaixo da sua meta
Abaixo no R$/km
```

ou:

```text
R$ 12,40 acima da sua meta
Passa no km e na hora
```

Critérios:

- só aparece se ativado pelo usuário;
- não substitui métricas atuais;
- compacto;
- não aumenta latência visual;
- se cálculo falhar, desaparece sem quebrar overlay.

---

### DS05 — PremiumGreatTreatment

Tratamento visual para classificação ÓTIMA.

Pode incluir:

- cor roxo premium;
- badge “ÓTIMA”;
- borda premium;
- leve gradiente opcional;
- ícone pequeno de estrela/diamante, se não poluir.

Critérios:

```text
parecer premium sem parecer cassino/jogo
```

---

### DS06 — GoalStatusPill

Pill pequena para mostrar se está acima/abaixo da meta.

Exemplos:

```text
+R$ 18,20
-R$ 27,92
```

---

### DS07 — OverlayDragHandle

Indicador discreto de que o overlay pode ser movido.

Critérios:

- muito sutil;
- não competir com dados;
- não parecer botão principal.

---

### DS08 — DailySummaryCard

Componente futuro para dashboard diário.

---

## 14. Estados visuais do overlay

### Estado RUIM

```text
cor: vermelho
emoção: alerta
ação mental: evitar/analisar com cuidado
```

### Estado ATENÇÃO

```text
cor: âmbar/laranja
emoção: cautela
ação mental: avaliar trade-off
```

### Estado BOA

```text
cor: verde
emoção: seguro
ação mental: aceita dentro da meta
```

### Estado ÓTIMA

```text
cor: roxo premium
emoção: oportunidade
ação mental: oferta acima da média
```

---

## 15. Layout proposto do overlay

### Sem Impacto Financeiro

```text
[BOA]

R$ 1,89/km
R$ 62,40/h
24 min
```

### Com Impacto Financeiro

```text
[ÓTIMA]

R$ 2,34/km
R$ 74,20/h
18 min

+R$ 12,40 acima da sua meta
Passa no km e na hora
```

### Com Atenção

```text
[ATENÇÃO]

R$ 1,21/km
R$ 71,36/h
58 min

R$ 27,92 abaixo da sua meta
Abaixo no R$/km
```

---

## 16. Requisitos funcionais

### RF01 — Criar tokens de cor

Como desenvolvedor, quero tokens de cor centralizados para manter consistência visual.

Critérios:

- cores centralizadas em objeto/arquivo do design system;
- estados RUIM/ATENÇÃO/BOA/ÓTIMA têm cor própria;
- ÓTIMA usa cor premium;
- overlay usa fundo com opacidade controlada.

Prioridade: P0

---

### RF02 — Criar tokens de opacidade

Como desenvolvedor, quero opacidades padronizadas para garantir legibilidade sem atrapalhar a Uber.

Prioridade: P0

---

### RF03 — Criar tokens tipográficos

Como desenvolvedor, quero estilos tipográficos reutilizáveis.

Prioridade: P0

---

### RF04 — Criar tokens de spacing/shape/elevation

Como desenvolvedor, quero espaçamentos e formas consistentes.

Prioridade: P0

---

### RF05 — Refatorar overlay para usar tokens

Como usuário, quero uma interface mais consistente sem perder a funcionalidade atual.

Critérios:

- overlay atual preserva informação;
- visual passa a usar tokens;
- não altera lógica de cálculo;
- não altera pipeline de captura.

Prioridade: P0

---

### RF06 — Adicionar estado visual ÓTIMA

Como motorista, quero perceber rapidamente quando uma oferta é excepcional.

Critérios:

- classificação ÓTIMA usa cor premium;
- não confunde com BOA;
- mensagem e badge são claros;
- contraste adequado.

Prioridade: P0

---

### RF07 — Implementar FinancialImpactBlock opcional

Como motorista, quero que o impacto financeiro apareça como plus, se eu ativar.

Critérios:

- bloco opcional;
- não substitui overlay atual;
- segue design system;
- layout compacto.

Prioridade: P0

---

### RF08 — Criar variações de overlay

Como motorista, quero que o overlay continue legível em diferentes situações.

Variações:

- compacto;
- normal;
- com impacto financeiro;
- sem impacto financeiro;
- ótima;
- ruim;
- atenção;
- boa.

Prioridade: P1

---

### RF09 — Criar modo de acessibilidade visual

Como motorista, quero uma opção de maior contraste se eu tiver dificuldade de leitura.

Prioridade: P2

---

### RF10 — Criar documentação visual

Como desenvolvedor, quero exemplos dos componentes e estados.

Prioridade: P1

---

## 17. Requisitos não funcionais

### RNF01 — Latência

Design system não pode aumentar:

```text
visibleLatencyMs > 300ms
```

### RNF02 — Legibilidade

Texto principal deve ser legível rapidamente em celular pequeno.

### RNF03 — Contraste

Cores precisam manter contraste suficiente com texto branco/preto.

### RNF04 — Não interferência

Overlay não pode cobrir dados críticos da Uber além do necessário.

### RNF05 — Performance

Evitar:

- animações pesadas;
- gradientes excessivos;
- sombras caras;
- recomposição desnecessária.

### RNF06 — Consistência

Todos os componentes do overlay devem usar tokens.

---

## 18. Backlog estruturado

### Épico 1 — Foundation Tokens

- US01 — Criar CalcMotColors — P0
- US02 — Criar CalcMotOpacity — P0
- US03 — Criar CalcMotTypography — P0
- US04 — Criar CalcMotSpacing — P0
- US05 — Criar CalcMotShape — P0
- US06 — Criar CalcMotElevation — P0

### Épico 2 — Overlay Components

- US07 — Criar CalcMotOverlayContainer — P0
- US08 — Criar OfferQualityBadge — P0
- US09 — Criar MetricRow — P0
- US10 — Criar FinancialImpactBlock — P0
- US11 — Criar GoalStatusPill — P1
- US12 — Criar OverlayDragHandle — P1

### Épico 3 — Premium GREAT State

- US13 — Definir cor final de ÓTIMA — P0
- US14 — Implementar tratamento visual premium — P0
- US15 — Testar roxo premium vs azul royal — P1
- US16 — Garantir contraste da categoria ÓTIMA — P0

### Épico 4 — Overlay Layout System

- US17 — Refatorar overlay atual para tokens — P0
- US18 — Criar layout normal — P0
- US19 — Criar layout compacto — P1
- US20 — Criar layout com Impacto Financeiro — P0
- US21 — Criar fallback se impacto estiver desativado — P0

### Épico 5 — Accessibility & Safety

- US22 — Validar contraste — P0
- US23 — Adicionar suporte a modo alto contraste — P2
- US24 — Garantir que cor não seja único sinal — P0
- US25 — Validar legibilidade em tela pequena — P0

### Épico 6 — QA Visual

- US26 — Testar em card real da Uber — P0
- US27 — Testar em tela clara/escura — P1
- US28 — Medir visibleLatencyMs antes/depois — P0
- US29 — Validar opacidade em direção/situação real — P0
- US30 — Criar checklist visual — P0

---

## 19. Planejamento de implementação

### Sprint 1 — Foundations

Objetivo:

```text
criar tokens e preparar base do design system
```

Escopo:

- cores;
- opacidade;
- tipografia;
- spacing;
- shape;
- elevation.

Entrega:

```text
tokens centralizados e prontos para uso
```

---

### Sprint 2 — Overlay Refactor

Objetivo:

```text
refatorar o overlay atual sem mudar comportamento
```

Escopo:

- CalcMotOverlayContainer;
- OfferQualityBadge;
- MetricRow;
- refatorar overlay atual para tokens.

Critério:

```text
overlay visual muda, mas comportamento funcional permanece igual
```

---

### Sprint 3 — Estado ÓTIMA premium

Objetivo:

```text
adicionar categoria premium para ofertas ótimas
```

Escopo:

- cor roxo premium;
- alternativa azul royal para teste;
- badge ÓTIMA;
- tratamento visual premium.

Critério:

```text
ÓTIMA é percebida como melhor que BOA em teste visual
```

---

### Sprint 4 — Impacto Financeiro no Overlay

Objetivo:

```text
integrar bloco plus opcional de impacto financeiro
```

Escopo:

- FinancialImpactBlock;
- GoalStatusPill;
- layout com impacto;
- fallback sem impacto.

Critério:

```text
overlay atual não é substituído
impacto aparece como plus opcional
```

---

### Sprint 5 — QA Visual e Refinamento

Objetivo:

```text
lapidar UI/UX em uso real
```

Escopo:

- teste em vídeo;
- teste com cards reais;
- teste de opacidade;
- teste de latência;
- ajuste fino de cores.

---

## 20. Recursividade de melhoria — Iteração 1

### Problema identificado

O backlog inicial cria tokens e componentes, mas ainda não define claramente como validar se a cor ÓTIMA realmente gera percepção premium.

### Melhoria

Adicionar teste A/B visual:

```text
roxo premium vs azul royal
```

Critério:

```text
5 usuários/motoristas respondem qual parece mais “oportunidade excelente”
```

### Ajuste no backlog

Adicionar US15:

```text
Testar roxo premium vs azul royal
```

---

## 21. Recursividade de melhoria — Iteração 2

### Problema identificado

O overlay pode ficar bonito, mas poluído.

### Melhoria

Adicionar regra de densidade visual:

```text
máximo de 2 linhas extras quando Impacto Financeiro estiver ativo
```

### Ajuste

FinancialImpactBlock deve ter:

```text
1 linha principal
1 sublinha opcional
```

Nada além disso no overlay.

---

## 22. Recursividade de melhoria — Iteração 3

### Problema identificado

Cores podem falhar em baixa luminosidade ou com daltonismo.

### Melhoria

Adicionar redundância visual:

```text
RUIM = badge + texto “RUIM”
ATENÇÃO = badge + texto “ATENÇÃO”
BOA = badge + texto “BOA”
ÓTIMA = badge + texto “ÓTIMA”
```

A cor ajuda, mas o texto confirma.

---

## 23. Recursividade de melhoria — Iteração 4

### Problema identificado

Opacidade pode atrapalhar ou ficar ilegível dependendo da tela da Uber.

### Melhoria

Criar duas opções futuras:

```text
opacidade confortável
opacidade alta legibilidade
```

No MVP, usar uma opacidade padrão segura.

---

## 24. Recursividade de melhoria — Iteração 5

### Problema identificado

Categoria ÓTIMA pode parecer gamificada demais.

### Melhoria

Evitar efeitos exagerados:

- sem brilho piscando;
- sem animação chamativa;
- sem neon forte;
- sem confete;
- sem sons.

Premium deve ser discreto, não arcade.

---

## 25. Critérios de sucesso do Design System

O design system será considerado bem-sucedido quando:

```text
1. Overlay atual estiver refatorado para tokens.
2. Estado ÓTIMA tiver cor premium clara e distinta.
3. Impacto Financeiro aparecer como plus opcional.
4. Overlay continuar legível em tela real da Uber.
5. Opacidade não atrapalhar o motorista.
6. visibleLatencyMs continuar <= 300ms.
7. Motorista entender a classificação em até 1 segundo.
8. Motorista entender impacto financeiro em até 2 segundos.
9. Design parecer mais premium que apps comuns do nicho.
```

---

## 26. O que não fazer

Não implementar:

- animações pesadas;
- gradientes chamativos demais;
- overlay grande demais;
- substituição total do overlay;
- cores sem contraste;
- informação demais;
- design que esconda o card da Uber;
- efeito de jogo/cassino;
- som;
- popups intrusivos.

---

## 27. Definition of Done

```text
- tokens implementados;
- overlay usando tokens;
- estado ÓTIMA implementado;
- cor premium validada;
- FinancialImpactBlock opcional implementado;
- overlay atual preservado;
- QA visual aprovado;
- visibleLatencyMs <= 300ms;
- nenhuma regressão na captura;
- nenhuma regressão na máquina de estado;
- documentação visual criada.
```

---

## 28. Prompt de implementação para agente IA

```text
Você é um Senior Android Engineer e Product Designer trabalhando no CalcMot.

Implemente um Design System nativo em Jetpack Compose para o overlay do CalcMot.

Regras absolutas:
1. Não substituir o overlay atual.
2. Não alterar pipeline de captura.
3. Não alterar OfferTreeExtractor.
4. Não alterar Overlay State Machine.
5. Não reintroduzir OCR, ML Kit ou screenshots.
6. O Design System deve apenas melhorar visual, consistência e percepção de valor.
7. A categoria ÓTIMA deve ganhar tratamento premium, preferencialmente roxo premium.
8. O Impacto Financeiro deve aparecer como bloco plus opcional, nunca como substituto.
9. O overlay deve manter opacidade para não atrapalhar a Uber.
10. A feature não pode aumentar visibleLatencyMs acima de 300ms.

Implemente em etapas:
1. Tokens de cor, opacidade, tipografia, spacing, shape e elevation.
2. Componentes de overlay: container, badge, metric row.
3. Estado ÓTIMA premium.
4. FinancialImpactBlock opcional.
5. QA visual e ajuste fino.

Critério de sucesso:
- overlay atual preservado;
- visual mais premium;
- estado ÓTIMA claramente distinto de BOA;
- Impacto Financeiro opcional;
- opacidade confortável;
- legibilidade alta;
- visibleLatencyMs <= 300ms.
```
