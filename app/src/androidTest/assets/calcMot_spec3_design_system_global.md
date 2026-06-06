# Spec 3 — Design System Global da Aplicação CalcMot

**Produto:** CalcMot  
**Módulo:** Design System Global / App Shell / Configurações / Dashboard / Histórico / Onboarding  
**Status:** Draft estruturado para implementação  
**Prioridade:** P0 após Design System do Overlay e Impacto Financeiro  
**Objetivo:** criar uma base visual única, escalável e profissional para toda a aplicação CalcMot, mantendo consistência com o overlay e preparando o app para beta fechado, Play Store, métricas de valor percebido e monetização futura.

---

## 1. Contexto

O CalcMot evoluiu de um protótipo técnico para um MVP funcional com:

- captura de cards reais da Uber Driver pela árvore de acessibilidade;
- overlay em produção real;
- cálculo de R$/km e R$/hora;
- Impacto Financeiro contra meta do motorista;
- Design System específico para o overlay;
- classificação visual por semáforo e estado premium **ÓTIMA**.

O Design System do Overlay resolve o ponto crítico de decisão rápida sobre a tela da Uber.

Agora é necessário criar o **Design System Global da Aplicação**, responsável por todas as telas internas do app:

- Home;
- Onboarding;
- Permissões;
- Configuração de metas;
- Resumo diário;
- Histórico;
- Painel de impacto financeiro;
- Diagnóstico do serviço;
- Configurações avançadas;
- Beta feedback;
- Monetização futura.

O objetivo é transformar o CalcMot de “app técnico com overlay” em **produto completo, confiável e premium para motoristas**.

---

## 2. Diferença entre Design System do Overlay e Design System Global

### Design System do Overlay

Focado no momento crítico da corrida.

Características:

- compacto;
- rápido;
- pouco intrusivo;
- legível sobre a Uber;
- otimizado para decisão em segundos;
- controla opacidade;
- mostra status, métricas e impacto financeiro.

### Design System Global da Aplicação

Focado na experiência completa do usuário.

Características:

- explica valor;
- guia permissões;
- configura metas;
- mostra resultados;
- aumenta confiança;
- reforça retenção;
- prepara monetização;
- melhora percepção de produto.

---

## 3. Visão do Design System Global

O CalcMot deve parecer:

```text
financeiro
confiável
moderno
premium
direto
simples para motorista cansado
```

O app não deve parecer:

```text
gambiarra
painel técnico cru
app experimental
ferramenta hacker
poluído
difícil de configurar
```

A experiência deve comunicar:

```text
o CalcMot ajuda o motorista a decidir melhor e proteger sua meta financeira
```

---

## 4. Objetivos

### O1 — Consistência visual

Todas as telas internas devem usar os mesmos tokens e componentes.

### O2 — Confiança

O usuário deve sentir que está usando um produto sério.

### O3 — Clareza

Motorista deve entender configurações, permissões e resultados sem explicação técnica.

### O4 — Valor percebido

O app deve mostrar claramente o benefício financeiro do uso.

### O5 — Escalabilidade

O Design System deve suportar novas telas, dashboard, histórico e monetização futura.

### O6 — Desacoplamento

Mudanças futuras de cor, tipografia, espaçamento, cards, botões e temas devem ocorrer em pontos únicos.

### O7 — Não interferir no core

O Design System Global não pode alterar o pipeline de captura, parser, overlay state machine ou cálculo.

---

## 5. Princípios de UI/UX

### P1 — Motorista em primeiro lugar

O usuário pode estar cansado, com pressa ou em trânsito. Textos e fluxos devem ser curtos.

### P2 — Financeiro sem complexidade

O app deve mostrar impacto financeiro com linguagem simples.

### P3 — Configuração guiada

Permissões de acessibilidade, overlay e bateria devem ser guiadas passo a passo.

### P4 — Premium discreto

Visual moderno, mas sem gamificação exagerada.

### P5 — Decisão rápida

Telas internas devem reforçar a lógica usada no overlay.

### P6 — Reutilização

Nenhuma tela deve criar estilo próprio fora do Design System.

### P7 — Acessibilidade visual

Contraste, tamanho de fonte e estados visuais devem permitir leitura rápida.

### P8 — Transparência

Não prometer economia garantida. Usar linguagem de meta e alerta.

---

## 6. Arquitetura recomendada

```text
ui/
  design/
    tokens/
      CalcMotColors.kt
      CalcMotTypography.kt
      CalcMotSpacing.kt
      CalcMotShape.kt
      CalcMotElevation.kt
      CalcMotOpacity.kt
      CalcMotMotion.kt
      CalcMotIcons.kt

    theme/
      CalcMotTheme.kt
      CalcMotThemeMode.kt
      CalcMotColorScheme.kt

    components/
      CalcMotScaffold.kt
      CalcMotTopBar.kt
      CalcMotCard.kt
      CalcMotButton.kt
      CalcMotIconButton.kt
      CalcMotSwitchRow.kt
      CalcMotTextField.kt
      CalcMotNumberField.kt
      CalcMotSectionHeader.kt
      CalcMotInfoBanner.kt
      CalcMotStatusBadge.kt
      CalcMotProgressCard.kt
      CalcMotEmptyState.kt
      CalcMotDivider.kt
      CalcMotBottomActionBar.kt

    domain-components/
      PermissionStatusCard.kt
      GoalPresetCard.kt
      DailySummaryCard.kt
      FinancialImpactSummaryCard.kt
      OfferHistoryItem.kt
      MetricHighlightCard.kt
      ServiceHealthCard.kt
      BetaFeedbackCard.kt

    overlay/
      CalcMotOverlayContainer.kt
      OfferQualityBadge.kt
      MetricRow.kt
      FinancialImpactBlockDS.kt
      OverlayDragHandle.kt
```

---

## 7. Camadas do Design System

### 7.1 Foundation Tokens

Tokens base:

- cores;
- tipografia;
- espaçamento;
- shape;
- elevação;
- opacidade;
- motion;
- ícones.

### 7.2 Core Components

Componentes genéricos:

- botões;
- cards;
- inputs;
- switches;
- banners;
- top bar;
- scaffold;
- empty state.

### 7.3 Domain Components

Componentes específicos do CalcMot:

- card de permissão;
- card de meta;
- card de impacto financeiro;
- resumo diário;
- histórico de oferta;
- status do serviço;
- feedback beta.

### 7.4 Feature Screens

Telas completas:

- Home;
- Onboarding;
- Permissões;
- Metas;
- Dashboard;
- Histórico;
- Configurações;
- Diagnóstico;
- Feedback;
- Assinatura futura.

---

## 8. Tokens globais

### 8.1 CalcMotColors

O app deve ter uma paleta global que converse com o overlay, mas não seja limitada a ele.

```kotlin
object CalcMotColors {
    // Brand
    val BrandPrimary = Color(0xFF6D3BFF) // roxo premium
    val BrandPrimaryDark = Color(0xFF4B22C8)
    val BrandSecondary = Color(0xFF2457FF) // azul royal alternativo
    val BrandAccent = Color(0xFF00C2A8)

    // Backgrounds
    val AppBackground = Color(0xFF101114)
    val AppBackgroundLight = Color(0xFFF7F8FA)
    val Surface = Color(0xFF1A1B20)
    val SurfaceElevated = Color(0xFF23242A)
    val SurfaceSoft = Color(0xFF2B2D34)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE0E0E0)
    val TextMuted = Color(0xFF9EA3AD)
    val TextInverse = Color(0xFF111111)

    // Semantic
    val Success = Color(0xFF2E7D32)
    val Warning = Color(0xFFFFB300)
    val Danger = Color(0xFFE53935)
    val Info = Color(0xFF2196F3)
    val Great = Color(0xFF6D3BFF)

    // Financial
    val PositiveMoney = Color(0xFF20C997)
    val NegativeMoney = Color(0xFFFF5252)
    val NeutralMoney = Color(0xFFFFCA28)

    // Borders
    val BorderSubtle = Color(0x22FFFFFF)
    val BorderStrong = Color(0x44FFFFFF)

    // Overlay
    val OverlayBackground = Color(0xE61A1A1A)
    val OverlayBackgroundSoft = Color(0xD91A1A1A)
}
```

### Regras

- `Great` deve ser usado para estado **ÓTIMA**.
- `Success`, `Warning`, `Danger` devem manter semáforo universal.
- `PositiveMoney` e `NegativeMoney` devem ser usados em impacto financeiro.
- Telas internas devem priorizar `BrandPrimary`, `Surface` e `TextPrimary`.

---

### 8.2 CalcMotTypography

```kotlin
object CalcMotTypography {
    val ScreenTitle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val ScreenSubtitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)

    val SectionTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
    val CardTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val BodyStrong = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val Caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)

    val MetricHero = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val MetricValue = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val MetricLabel = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)

    val Button = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
}
```

---

### 8.3 CalcMotSpacing

```kotlin
object CalcMotSpacing {
    val Xxs = 2.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp

    val ScreenHorizontal = 16.dp
    val ScreenVertical = 20.dp
    val CardPadding = 16.dp
    val CardGap = 12.dp
    val SectionGap = 20.dp
}
```

---

### 8.4 CalcMotShape

```kotlin
object CalcMotShape {
    val Xs = 6.dp
    val Sm = 10.dp
    val Md = 16.dp
    val Lg = 20.dp
    val Xl = 28.dp
    val Pill = 999.dp
}
```

---

### 8.5 CalcMotElevation

```kotlin
object CalcMotElevation {
    val None = 0.dp
    val Low = 2.dp
    val Medium = 6.dp
    val High = 12.dp
    val Overlay = 8.dp
}
```

---

### 8.6 CalcMotOpacity

```kotlin
object CalcMotOpacity {
    const val Disabled = 0.45f
    const val Muted = 0.65f
    const val Secondary = 0.82f
    const val OverlaySoft = 0.82f
    const val OverlayMedium = 0.86f
    const val OverlayStrong = 0.90f
}
```

---

### 8.7 CalcMotMotion

Animações devem ser discretas.

```kotlin
object CalcMotMotion {
    const val Fast = 120
    const val Medium = 180
    const val Slow = 250
}
```

Regras:

- sem animação pesada;
- sem loop infinito;
- sem brilho piscando;
- sem efeito de cassino;
- transições curtas e funcionais.

---

## 9. Tema global

### CalcMotTheme

O app deve ter um tema central:

```kotlin
@Composable
fun CalcMotTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // aplica colors, typography, shapes e componentes base
}
```

### Tema inicial

MVP deve priorizar tema escuro.

Motivo:

- motorista usa muito à noite;
- reduz impacto visual;
- combina com overlay;
- parece mais premium;
- melhora contraste em contexto automotivo.

Tema claro pode ser P2.

---

## 10. Componentes globais

### 10.1 CalcMotScaffold

Base para telas internas.

Responsável por:

- background;
- safe area;
- top bar opcional;
- padding padrão;
- conteúdo;
- bottom action opcional.

Critérios:

- todas as telas principais devem usar;
- evitar padding manual duplicado.

### 10.2 CalcMotTopBar

Top bar padrão.

Usado em:

- Configurações;
- Metas;
- Histórico;
- Resumo diário;
- Diagnóstico.

Critérios:

- título claro;
- botão voltar opcional;
- ação secundária opcional.

### 10.3 CalcMotCard

Card padrão para conteúdo.

Variações:

```text
default
highlight
danger
success
premium
```

Critérios:

- usa tokens de surface, shape e spacing;
- não usa cor hardcoded;
- suporta título, subtítulo, conteúdo e ação.

### 10.4 CalcMotButton

Variações:

```text
primary
secondary
ghost
danger
premium
```

Critérios:

- usa tokens;
- altura mínima consistente;
- estado disabled;
- feedback visual.

### 10.5 CalcMotSwitchRow

Linha de configuração com switch.

Exemplos:

```text
Mostrar Impacto Financeiro no overlay
Modo compacto
Alto contraste
```

Critérios:

- título;
- descrição curta;
- switch alinhado;
- acessível.

### 10.6 CalcMotTextField / CalcMotNumberField

Inputs padrão.

Usos:

- meta R$/km;
- meta R$/hora;
- meta diária;
- nome do perfil.

Critérios:

- validação clara;
- prefixo R$ quando necessário;
- teclado numérico;
- mensagem de erro.

### 10.7 CalcMotInfoBanner

Banner informativo.

Variações:

```text
info
warning
danger
success
premium
```

Usos:

- explicar permissão;
- alertar serviço desativado;
- mostrar beta;
- aviso de política.

### 10.8 CalcMotStatusBadge

Badge de status.

Exemplos:

```text
Ativo
Pendente
Atenção
Erro
Beta
```

### 10.9 CalcMotEmptyState

Estado vazio.

Exemplo:

```text
Ainda não analisamos ofertas hoje.
Abra a Uber Driver para começar.
```

---

## 11. Componentes de domínio

### 11.1 PermissionStatusCard

Mostra status de permissões críticas:

- Acessibilidade;
- Sobreposição;
- Bateria sem restrição;
- Monitoramento.

Estados:

```text
Ativo
Pendente
Necessário
Erro
```

Critérios:

- deve explicar por que a permissão é necessária;
- deve ter CTA direto.

### 11.2 GoalPresetCard

Presets de meta:

```text
Conservador
Equilibrado
Exigente
Personalizado
```

Exemplo:

```text
Equilibrado
R$ 1,70/km
R$ 50/h
```

### 11.3 DailySummaryCard

Resumo do dia:

```text
Ofertas analisadas
Ofertas abaixo da meta
Ofertas acima da meta
Média R$/km
Média R$/h
```

### 11.4 FinancialImpactSummaryCard

Card de impacto financeiro acumulado.

Exemplo:

```text
Hoje você recebeu R$ 183,40 em ofertas abaixo da sua meta.
```

Regra:

Não usar “economizou” como fato absoluto.

### 11.5 OfferHistoryItem

Item de histórico.

Campos:

- horário;
- valor;
- R$/km;
- R$/hora;
- classificação;
- impacto financeiro.

Não exibir endereço completo no MVP.

### 11.6 ServiceHealthCard

Mostra saúde do app:

```text
Serviço ativo
Overlay permitido
Último card detectado
Último overlay exibido
CPU/memória se modo debug
```

### 11.7 BetaFeedbackCard

Coleta feedback simples:

```text
O app te ajudou hoje?
Você confiou nos cálculos?
Pagaria por isso?
```

---

## 12. Telas do app

### 12.1 Home

Objetivo:

Mostrar status geral do CalcMot.

Conteúdo:

- status das permissões;
- status do monitoramento;
- resumo rápido do dia;
- CTA para abrir/configurar metas;
- CTA para testar overlay;
- CTA para ver histórico.

Componentes:

- `CalcMotScaffold`;
- `PermissionStatusCard`;
- `DailySummaryCard`;
- `FinancialImpactSummaryCard`;
- `CalcMotButton`.

### 12.2 Onboarding

Objetivo:

Guiar usuário na primeira configuração.

Etapas:

1. explicar o que é o CalcMot;
2. ativar sobreposição;
3. ativar acessibilidade;
4. configurar metas;
5. explicar overlay;
6. finalizar.

Critérios:

- linguagem simples;
- sem termos técnicos excessivos;
- cada permissão com motivo claro.

### 12.3 Permissões

Objetivo:

Gerenciar permissões críticas.

Itens:

- AccessibilityService;
- SYSTEM_ALERT_WINDOW;
- bateria;
- notificações, se usadas.

### 12.4 Configuração de metas

Objetivo:

Permitir editar:

- meta por km;
- meta por hora;
- impacto financeiro no overlay;
- preset.

### 12.5 Dashboard diário

Objetivo:

Mostrar valor percebido.

Métricas:

- ofertas analisadas;
- ofertas boas;
- ofertas ruins;
- ofertas abaixo da meta;
- ofertas acima da meta;
- média R$/km;
- média R$/hora;
- melhor oferta do dia;
- pior oferta do dia.

### 12.6 Histórico

Objetivo:

Exibir últimas ofertas analisadas.

Filtros futuros:

- boas;
- ruins;
- atenção;
- ótimas.

### 12.7 Configurações

Itens:

- overlay compacto;
- alto contraste;
- impacto financeiro no overlay;
- metas;
- permissões;
- feedback;
- privacidade;
- versão do app.

### 12.8 Diagnóstico

Objetivo:

Ajudar beta e suporte.

Exibir:

- serviço ativo;
- overlay permission;
- última captura;
- última razão de rejeição;
- último overlay;
- versão do app;
- botão exportar logs sanitizados, futuro.

### 12.9 Beta Feedback

Objetivo:

Coletar aprendizado.

Perguntas:

- o app te ajudou?
- o overlay atrapalhou?
- você entendeu o impacto financeiro?
- você pagaria?
- qual nota de 0 a 10?

---

## 13. Requisitos funcionais

### RF01 — Criar tokens globais

Criar tokens centralizados para:

- cores;
- tipografia;
- espaçamento;
- shape;
- elevação;
- opacidade;
- motion;
- ícones.

Prioridade: P0.

### RF02 — Criar tema global CalcMotTheme

Todas as telas internas devem usar o tema global.

Prioridade: P0.

### RF03 — Criar componentes base

Criar:

- Scaffold;
- TopBar;
- Card;
- Button;
- SwitchRow;
- TextField;
- NumberField;
- InfoBanner;
- StatusBadge;
- EmptyState.

Prioridade: P0.

### RF04 — Criar componentes de domínio

Criar:

- PermissionStatusCard;
- GoalPresetCard;
- DailySummaryCard;
- FinancialImpactSummaryCard;
- OfferHistoryItem;
- ServiceHealthCard.

Prioridade: P1.

### RF05 — Refatorar telas existentes para usar Design System

Telas existentes não devem manter estilo hardcoded.

Prioridade: P0.

### RF06 — Criar Home com status do app

Home deve mostrar:

- permissões;
- monitoramento;
- resumo do dia;
- ações principais.

Prioridade: P0.

### RF07 — Criar tela de metas com presets

Permitir editar metas do motorista.

Prioridade: P0.

### RF08 — Criar tela de permissões guiada

Explicar e direcionar ativação.

Prioridade: P0.

### RF09 — Criar Dashboard diário

Mostrar impacto financeiro acumulado.

Prioridade: P1.

### RF10 — Criar Histórico simples

Mostrar últimas ofertas analisadas.

Prioridade: P2.

### RF11 — Criar Diagnóstico beta

Mostrar saúde do serviço.

Prioridade: P1.

### RF12 — Criar Feedback beta

Coletar feedback real.

Prioridade: P1.

### RF13 — Suportar modo compacto de app

Telas devem funcionar bem em celulares pequenos.

Prioridade: P1.

### RF14 — Suportar tema escuro como padrão

MVP deve usar tema escuro.

Prioridade: P0.

### RF15 — Preparar suporte futuro a tema claro

Não precisa implementar no MVP, mas tokens devem permitir.

Prioridade: P2.

---

## 14. Requisitos não funcionais

### RNF01 — Não afetar captura

Design System Global não pode alterar AccessibilityService, OfferTreeExtractor ou Overlay State Machine.

### RNF02 — Performance

Telas internas devem ser leves.

### RNF03 — Manutenibilidade

Nenhuma tela deve usar estilo hardcoded.

### RNF04 — Acessibilidade

Contraste adequado e textos legíveis.

### RNF05 — Privacidade

Não exibir ou salvar dado sensível desnecessário.

### RNF06 — Modularidade

Componentes devem ser reutilizáveis.

### RNF07 — Testabilidade

Componentes críticos devem ter previews e testes básicos.

---

## 15. Backlog estruturado

### Épico 1 — Foundation

- US01 — Criar CalcMotColors — P0
- US02 — Criar CalcMotTypography — P0
- US03 — Criar CalcMotSpacing — P0
- US04 — Criar CalcMotShape — P0
- US05 — Criar CalcMotElevation — P0
- US06 — Criar CalcMotOpacity — P0
- US07 — Criar CalcMotMotion — P1
- US08 — Criar CalcMotTheme — P0

### Épico 2 — Core Components

- US09 — Criar CalcMotScaffold — P0
- US10 — Criar CalcMotTopBar — P0
- US11 — Criar CalcMotCard — P0
- US12 — Criar CalcMotButton — P0
- US13 — Criar CalcMotSwitchRow — P0
- US14 — Criar CalcMotTextField — P0
- US15 — Criar CalcMotNumberField — P0
- US16 — Criar CalcMotInfoBanner — P1
- US17 — Criar CalcMotStatusBadge — P1
- US18 — Criar CalcMotEmptyState — P1
- US19 — Criar CalcMotBottomActionBar — P2

### Épico 3 — Domain Components

- US20 — Criar PermissionStatusCard — P0
- US21 — Criar GoalPresetCard — P0
- US22 — Criar DailySummaryCard — P1
- US23 — Criar FinancialImpactSummaryCard — P1
- US24 — Criar OfferHistoryItem — P2
- US25 — Criar ServiceHealthCard — P1
- US26 — Criar BetaFeedbackCard — P1

### Épico 4 — Screens

- US27 — Refatorar Home — P0
- US28 — Criar Onboarding — P0
- US29 — Criar Permissões — P0
- US30 — Criar Configuração de Metas — P0
- US31 — Criar Dashboard Diário — P1
- US32 — Criar Histórico — P2
- US33 — Criar Configurações — P1
- US34 — Criar Diagnóstico — P1
- US35 — Criar Feedback Beta — P1

### Épico 5 — Quality

- US36 — Criar previews Compose — P0
- US37 — Criar checklist visual — P0
- US38 — Validar contraste — P0
- US39 — Testar em celular pequeno — P1
- US40 — Testar tema escuro — P0
- US41 — Garantir zero hardcoded style — P0

---

## 16. Planejamento de implementação

### Sprint 1 — Foundation Global

Objetivo:

```text
criar a base visual global da aplicação
```

Entrega:

- tokens;
- tema;
- estrutura de pastas;
- previews simples.

### Sprint 2 — Core Components

Objetivo:

```text
criar componentes reutilizáveis globais
```

Entrega:

- Scaffold;
- TopBar;
- Card;
- Button;
- SwitchRow;
- TextField;
- NumberField.

### Sprint 3 — Telas críticas

Objetivo:

```text
melhorar setup e configuração do app
```

Entrega:

- Home;
- Permissões;
- Metas;
- Onboarding.

### Sprint 4 — Valor percebido

Objetivo:

```text
mostrar resultado financeiro ao usuário
```

Entrega:

- Dashboard diário;
- FinancialImpactSummaryCard;
- DailySummaryCard.

### Sprint 5 — Beta & Diagnóstico

Objetivo:

```text
suportar beta fechado e aprendizado
```

Entrega:

- Diagnóstico;
- Feedback Beta;
- ServiceHealthCard.

---

## 17. Recursividade de melhoria

### Iteração 1 — Evitar duplicidade com Overlay DS

Problema:

Tokens globais podem duplicar tokens do overlay.

Melhoria:

Criar tokens globais e permitir tokens específicos de overlay apenas quando necessário.

Regra:

```text
Overlay pode usar tokens globais, mas mantém tokens especializados de opacidade e densidade.
```

### Iteração 2 — Evitar app bonito mas sem valor

Problema:

Design bonito sem mostrar impacto financeiro não aumenta retenção.

Melhoria:

Priorizar Dashboard Diário e FinancialImpactSummaryCard.

### Iteração 3 — Evitar configuração confusa

Problema:

Permissões e metas podem confundir.

Melhoria:

Criar onboarding guiado e PermissionStatusCard com CTA.

### Iteração 4 — Evitar hardcoded

Problema:

Dev pode aplicar cor direta nas telas.

Melhoria:

Adicionar regra de lint/manual review:

```text
nenhum Color(0x...), dp, sp ou FontWeight direto fora dos tokens/componentes
```

### Iteração 5 — Preparar monetização sem implementar

Problema:

Monetização futura pode exigir telas novas.

Melhoria:

Design System deve suportar botão premium, banner premium e status beta, sem implementar pagamento agora.

---

## 18. QA visual global

Checklist:

```text
Home parece produto real
Permissões são fáceis de entender
Metas são fáceis de editar
Dashboard mostra valor percebido
Histórico não exibe dados sensíveis
Tema escuro é consistente
Botões e cards são consistentes
Não há estilos visuais fora dos tokens
```

---

## 19. Definition of Done

```text
- tokens globais implementados
- CalcMotTheme criado
- componentes base criados
- Home usando Design System
- Permissões usando Design System
- Metas usando Design System
- ao menos um resumo financeiro usando Design System
- previews Compose criados
- zero estilo hardcoded nas telas refatoradas
- nenhuma alteração no pipeline de captura
- nenhuma regressão no overlay
```

---

## 20. Prompt de implementação para agente IA

```text
Você é um Senior Android Engineer e Product Designer trabalhando no CalcMot.

Implemente o Spec 3 — Design System Global da Aplicação.

Objetivo:
Criar uma base visual global para todas as telas internas do app, mantendo consistência com o Design System do Overlay, sem alterar o pipeline de captura ou a máquina de estado.

Regras absolutas:
1. Não alterar AccessibilityService.
2. Não alterar OfferTreeExtractor.
3. Não alterar Overlay State Machine.
4. Não reintroduzir OCR, ML Kit, screenshots ou UIAutomator em produção.
5. Não substituir o Design System do Overlay; apenas integrá-lo ao sistema global.
6. Não criar estilos hardcoded nas telas.
7. Toda cor, tipografia, espaçamento, shape, elevation e opacidade devem vir de tokens.
8. Componentes devem ser reutilizáveis.
9. Tema escuro deve ser padrão.
10. Não implementar monetização nesta fase.

Implementar primeiro:
- tokens globais;
- CalcMotTheme;
- componentes base;
- Home;
- Permissões;
- Metas;
- previews Compose.

Depois:
- Dashboard diário;
- Diagnóstico;
- Feedback Beta;
- Histórico simples.

Critério de sucesso:
- app visualmente consistente;
- telas internas parecem produto real;
- overlay não sofre regressão;
- configuração de metas fica mais clara;
- permissões ficam mais fáceis de entender;
- impacto financeiro ganha espaço no app, não só no overlay.
```
