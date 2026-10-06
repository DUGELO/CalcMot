# CalcMot — Design Tokens Reference

**Objetivo:** referência técnica para design e desenvolvimento do site institucional e futuras superfícies do produto.  
**Status:** inventário extraído do repositório atual; não substitui os arquivos Kotlin.

## 1. Como ler este documento

- **Extraído:** existe literalmente no código, asset ou documentação indicada.
- **Inferido:** leitura de telas, comportamento ou conjunto de fontes.
- **Recomendado:** orientação para o site; não é token oficial do app até ser implementado no Design System.

Quando tokens com o mesmo papel divergem, o documento preserva os dois valores e aponta a origem.

## 2. Localização dos arquivos fonte

| Caminho | Conteúdo | Tokens/elementos encontrados | Relevância |
| --- | --- | --- | --- |
| `app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotColors.kt` | paleta global ativa | marca, fundos, superfícies, texto, semântica, dinheiro, overlay | principal fonte de cores do app shell |
| `app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotTypography.kt` | escala Material e estilos de produto | títulos, corpo, métricas, botão; `FontFamily.Default` | principal fonte tipográfica |
| `app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotSpacing.kt` | espaçamento global | 2–32 dp, padding de tela/card, gaps | ritmo e responsividade |
| `app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotShape.kt` | formas globais | 6, 10, 16, 20, 28 dp e pill | cards, campos, botões, badges |
| `app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotElevation.kt` | elevação | 0, 2, 6, 12 dp e overlay 8 dp | profundidade discreta |
| `app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotOpacity.kt` | opacidade | disabled 0,45; muted 0,65; overlay 0,82/0,86/0,94 | contraste e translucidez |
| `app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotMotion.kt` | motion | 120/180/250 ms | linguagem de rapidez |
| `app/src/main/java/br/com/calcmot/ui/design/theme/CalcMotColorScheme.kt` | Material 3 dark/light | mapeia background, surface, primary, tertiary, error | confirma dark-first e mapeamento semântico |
| `app/src/main/java/br/com/calcmot/ui/design/theme/CalcMotTheme.kt` | tema Compose | `CalcMotTheme`, dark default, Material Typography | fonte de aplicação dos tokens |
| `app/src/main/java/br/com/calcmot/ui/design/components/CalcMotComponents.kt` | componentes base | scaffold, top bar, card, botão, switch, campo, banner, badge, empty state | vocabulário visual reutilizável |
| `app/src/main/java/br/com/calcmot/ui/design/domain/CalcMotDomainComponents.kt` | componentes de domínio | permissão, meta, resumo, impacto, histórico, saúde, feedback | vocabulário do produto |
| `app/src/main/java/br/com/calcmot/overlay/OverlayDesignSystem.kt` | sistema específico do overlay | classificações, cores, translucidez, tipografia, dimensões, temas | fonte prioritária do ativo de decisão |
| `app/src/main/java/br/com/calcmot/overlay/OverlayView.kt` | composição do overlay | header, impacto, R$/km, R$/h, duração, resolução de qualidade | anatomia real do overlay |
| `app/src/main/java/br/com/calcmot/model/FinancialImpact.kt` | cálculo de meta/impacto | GREAT/GOOD/WARNING/BAD, threshold de 20%, textos | semântica e limites da classificação |
| `app/src/main/java/br/com/calcmot/ui/theme/Color.kt` | tema legado | `#1A1B1E`, `#2C2D31`, `#4C82FF`, semântica antiga | compatibilidade; não é a fonte global preferida |
| `app/src/main/java/br/com/calcmot/ui/theme/Type.kt` | tipografia legada | Default, 32/24/18/16/14/12 sp; TODO Figtree | confirma ausência de fonte customizada |
| `app/src/main/res/drawable/calcmot_logo_hero.png` | asset de logo/hero | ícone velocímetro + calculadora + check | asset visual usado nas telas |
| `app/src/main/res/drawable/calcmot_logo_512px.png` | asset do launcher | mesma marca em 512 px | ícone de aplicativo |
| `app/src/main/res/mipmap-*/*` | launcher Android | ícones por densidade | não usar como wordmark de site sem tratamento |
| `docs/design/references/calcmot-prototypes-2026/README.md` | contrato visual de protótipos | fundo, verde, azul, cards, fluxos, estados | fonte visual canônica |
| `docs/design/references/calcmot-prototypes-2026/*.png` | 25 telas de referência | onboarding, home, meta, histórico, ajuda, privacidade etc. | valida hierarquia, textos e composição |
| `docs/screenshots/ux-ui-material3-after/*` | screenshots de QA | home, onboarding, goals, help, privacy, settings | evidência de runtime/Material 3 |
| `docs/design/social/*` | peças institucionais | semáforo, logo, mockup, mensagens | direção de comunicação; não necessariamente token runtime |
| `docs/global-design-system.md` | resumo do DS | status, guardrails, componentes e dark-first | confirmação de escopo |
| `docs/overlay-design-system.md` | resumo do overlay | semântica, componentes, QA | confirma ÓTIMA premium |
| `docs/overlay-v21-meta-first-20260611.md` | decisões de UX do overlay | ordem do impacto, “Na meta”, taxonomia | confirma prioridade da decisão |
| `docs/overlay-v23-dedup-20260612.md` | decisão de microcopy | redução de redundância | orienta comunicação compacta |
| `docs/ux-ui-visual-qa-pr10-20260611.md` | validação visual real | estados, latência observada, leitura e confiança | evidência de uso em dispositivo |
| `docs/privacy-policy.md` | política atual | leitura local, dados, limites, controle | fonte para confiança; rever antes de publicação |
| `docs/play-store-submission.md` | declaração de produto | nome público, uso declarado, limites | fonte de claims públicos |

## 3. Tabela de tokens globais

| Token | Valor | HEX | Arquivo de origem | Uso atual | Uso recomendado no site | Observações |
| --- | --- | --- | --- | --- | --- | --- |
| `BrandPrimary` | Compose Color | `#1768F9` | `ui/design/tokens/CalcMotColors.kt:6` | CTA, primary, links e informação | CTA principal, links e foco | azul de ação |
| `BrandPrimaryDark` | Compose Color | `#0F55D9` | `.../CalcMotColors.kt:7` | light scheme/contraste | estado pressionado ou fundo claro | derivação existente |
| `BrandSecondary` | Compose Color | `#4D8DFF` | `.../CalcMotColors.kt:8` | links/apoios | ações secundárias | não é semântica positiva |
| `BrandAccent` | Compose Color | `#61E329` | `.../CalcMotColors.kt:9` | tertiary Material e marca | acento de logo, check e prontidão | verde ácido principal |
| `AppBackground` | Compose Color | `#04060A` | `.../CalcMotColors.kt:11` | canvas dark | fundo global do site | dark-first |
| `Surface` | Compose Color | `#0B1016` | `.../CalcMotColors.kt:13` | cards padrão | cards e agrupamentos | não aninhar cards |
| `SurfaceElevated` | Compose Color | `#101720` | `.../CalcMotColors.kt:14` | cards destacados | hero surface/status | uso pontual |
| `SurfaceSoft` | Compose Color | `#141A22` | `.../CalcMotColors.kt:15` | surfaceVariant | apoio/variação | neutro frio |
| `TextPrimary` | Compose Color | `#F7F7F7` | `.../CalcMotColors.kt:17` | títulos, números | títulos e números | quase branco |
| `TextSecondary` | Compose Color | `#B9BBC2` | `.../CalcMotColors.kt:18` | corpo auxiliar | descrições | verificar contraste em tamanhos pequenos |
| `TextMuted` | Compose Color | `#8E929B` | `.../CalcMotColors.kt:19` | metadados/desabilitado | metadata e estados não essenciais | não usar para ação essencial |
| `TextInverse` | Compose Color | `#111111` | `.../CalcMotColors.kt:20` | onSecondary/onTertiary | texto em superfícies claras/coloridas | usar apenas com contraste validado |
| `Success` | Compose Color | `#61E329` | `.../CalcMotColors.kt:22` | prontidão, status, meta | sucesso e progresso | identidade verde |
| `Warning` | Compose Color | `#FFC453` | `.../CalcMotColors.kt:23` | atenção/permissão | aviso contextual | difere do Warning do overlay |
| `Danger` | Compose Color | `#FF6670` | `.../CalcMotColors.kt:24` | erro e danger card | erro/risco de interface | difere do Bad do overlay |
| `Info` | Compose Color | `#1768F9` | `.../CalcMotColors.kt:25` | info | informação | alias semântico do azul |
| `Great` global | Compose Color | `#1768F9` | `.../CalcMotColors.kt:26` | premium card/goal row | não publicar como ÓTIMA sem resolver divergência | conflito com overlay |
| `HeroBackground` | Compose Color | `#04060A` | `.../CalcMotColors.kt:28` | onboarding hero | hero | igual ao background |
| `HeroSurface` | Compose Color | `0xCC0E1218` / CSS `#0E1218CC` | `.../CalcMotColors.kt:29` | hero translúcido | painel dentro do hero | alfa em ARGB Compose |
| `HeroBorder` | Compose Color | `0x334D5966` / CSS `#4D596633` | `.../CalcMotColors.kt:30` | borda hero | limite de mockup/card | alfa em ARGB Compose |
| `HeroGlowGreen` | Compose Color | `#61E329` | `.../CalcMotColors.kt:31` | brilho hero | glow radial discreto | baixa opacidade |
| `PositiveMoney` | Compose Color | `#20C997` | `.../CalcMotColors.kt:34` | dashboard financeiro | resultado positivo | não confundir com BrandAccent |
| `NegativeMoney` | Compose Color | `#FF5252` | `.../CalcMotColors.kt:35` | dashboard financeiro | perda/custo | não é necessariamente erro |
| `NeutralMoney` | Compose Color | `#FFCA28` | `.../CalcMotColors.kt:36` | valor neutro | sem direção definida | não chamar de ganho |
| `BorderSubtle` | Compose Color | `0x334D5966` / CSS `#4D596633` | `.../CalcMotColors.kt:38` | cards/divisores | bordas leves | alfa em ARGB Compose |
| `BorderStrong` | Compose Color | `0x665F6E7D` / CSS `#5F6E7D66` | `.../CalcMotColors.kt:39` | destaque/foco | borda de seção ativa | alfa em ARGB Compose |
| `OverlayBackground` | Compose Color | `0xE604060A` / CSS `#04060AE6` | `.../CalcMotColors.kt:41` | overlay global | mockup de overlay dark | alfa em ARGB Compose |
| `OverlayBackgroundSoft` | Compose Color | `0xD904060A` / CSS `#04060AD9` | `.../CalcMotColors.kt:42` | overlay suave | estado sobre app | alfa em ARGB Compose |

## 4. Tokens do overlay

| Token | HEX/valor | Origem | Uso |
| --- | --- | --- | --- |
| `CalcMotColors.Bad` | `#E53935` | `overlay/OverlayDesignSystem.kt:39` | RUIM no overlay |
| `CalcMotColors.Warning` | `#FFB300` | `...:40` | MÉDIA no overlay |
| `CalcMotColors.Good` | `#2E7D32` | `...:41` | BOA no overlay |
| `CalcMotColors.Great` | `#6D3BFF` | `...:42` | ÓTIMA/GREAT, premium purple |
| `RoyalBlueAlternative` | `#2457FF` | `...:43` | alternativa registrada, não usada como decisão final |
| `OverlayBackground` | `0xF01A1A1A` / CSS `#1A1A1AF0` | `...:45` | classic translucent container |
| `OverlayBackgroundSoft` | `0xD91A1A1A` / CSS `#1A1A1AD9` | `...:46` | soft overlay |
| `SurfaceElevated` | `0xF2232323` / CSS `#232323F2` | `...:47` | superfície elevada do overlay |
| `TextPrimary` | `#FFFFFF` | `...:49` | números e títulos |
| `TextSecondary` | `#E0E0E0` | `...:50` | labels/apoio |
| `TextMuted` | `#BDBDBD` | `...:51` | drag handle/metadados |
| `Divider` | `0x33FFFFFF` / CSS `#FFFFFF33` | `...:53` | divisor translúcido |
| `PrototypeGood` | `#5A9821` | `...:55` | acento do tema OUTLINED |
| `PrototypeBad` | `#D92D20` | `...:56` | acento do tema OUTLINED |
| `PrototypeGreat` | `#9C2A9A` | `...:57` | acento ÓTIMA do tema OUTLINED |
| `PrototypeWarning` | `#DA7311` | `...:58` | acento MÉDIA do tema OUTLINED |
| `PrototypeLightSurface` | `#FFFFFF` | `...:59` | fundo OUTLINED |
| `PrototypeDarkText` | `#171717` | `...:60` | texto OUTLINED |
| `OverlayStrong/Medium/Soft` | `0.94/0.86/0.82` | `...:64–70` | opacidade perceptiva |
| `ValuePrimary` | `23 sp Bold` | `...:74–79` | valor principal |
| `MetricLabel` | `11 sp Medium` | `...:81–84` | labels |
| `MetricValue` | `15 sp Bold` | `...:86–89` | R$/h, duração |
| `ImpactMessage` | `12 sp Bold` | `...:91–94` | impacto |
| `MetaImpactValue` | `14 sp Bold` | `...:96–99` | impacto/meta |
| `ImpactSubMessage` | `11 sp Medium` | `...:101–104` | apoio |
| `OverlayPadding` | `8 dp` | `...:107–114` | container |
| `OverlayRadius` | `16 dp` | `...:116–120` | classic |
| `BadgeRadius` | `999 dp` | `...:118` | pílula |
| `CardRadius` | `20 dp` | `...:119` | card relacionado |
| `Overlay elevation` | `8 dp` | `...:122–125` | sobreposição |

### Divergência a resolver

O overlay semântico `GREAT` usa `#6D3BFF`, mas o tema padrão `OUTLINED` (confirmado em `AppSettings.kt:75–79` e no default de `OverlayView.kt`) usa `#9C2A9A` como `PrototypeGreat`. O token global `Great` usa `#1768F9`. Para o site, não criar um quarto roxo: usar apenas essas referências enquanto produto/design resolve o token oficial.

## 4.1. Tokens legados mantidos por compatibilidade

`app/src/main/java/br/com/calcmot/ui/theme/Color.kt` ainda contém uma paleta anterior: `SurfaceBackground #1A1B1E`, `SurfacePrimary #2C2D31`, `TextPrimary #F5F5F5`, `TextSecondary #A0A3A8`, `InteractiveAccent #4C82FF`, `BorderSubtle #3A3C42`, `SemanticGood #3DDC84`, `SemanticAttention #FFC453` e `SemanticBad #FF6670`.

`MetricaTheme` delega ao tema global `CalcMotTheme`; por isso, para novas superfícies e para o site, a paleta em `ui/design/tokens` é a fonte preferencial. Os valores legados devem permanecer documentados para evitar que uma migração futura confunda compatibilidade com identidade atual.

## 5. Tipografia, spacing, shape, motion e opacity

### Tipografia

Fonte: `FontFamily.Default`, sem família customizada identificada. Ver `ui/design/tokens/CalcMotTypography.kt:10–25`.

| Estilo | Tamanho | Peso |
| --- | ---: | --- |
| ScreenTitle | 24 sp | Bold |
| ScreenSubtitle | 15 sp | Medium |
| SectionTitle | 18 sp | Bold |
| CardTitle | 16 sp | SemiBold |
| Body | 14 sp | Normal |
| BodyStrong | 14 sp | SemiBold |
| Caption | 12 sp | Medium |
| MetricHero | 28 sp | Bold |
| MetricValue | 20 sp | Bold |
| MetricLabel | 12 sp | Medium |
| Button | 15 sp | Bold |

Site: Inter + fallback de sistema é recomendação, não extração.

### Spacing

Origem: `ui/design/tokens/CalcMotSpacing.kt:6–18`.

`Xxs 2`, `Xs 4`, `Sm 8`, `Md 12`, `Lg 16`, `Xl 24`, `Xxl 32 dp`; horizontal de tela `16 dp`, vertical `20 dp`, padding de card `16 dp`, gap de card `12 dp`, gap de seção `20 dp`.

### Shape

Origem: `ui/design/tokens/CalcMotShape.kt:6–11`.

`Xs 6`, `Sm 10`, `Md 16`, `Lg 20`, `Xl 28 dp`; `Pill 999 dp`. O overlay tem ainda raio 16 dp clássico e 24 dp nos temas OUTLINED/SOLID.

### Elevation

Origem: `CalcMotElevation.kt:6–10`: `None 0`, `Low 2`, `Medium 6`, `High 12`, `Overlay 8 dp`.

### Motion e opacity

Origem: `CalcMotMotion.kt:4–6` e `CalcMotOpacity.kt:4–9`.

Motion: `Fast 120 ms`, `Medium 180 ms`, `Slow 250 ms`. Opacity: disabled `0.45`, muted `0.65`, secondary `0.82`, overlay soft `0.82`, medium `0.86`, strong `0.94`.

## 6. Componentes e equivalentes no site

| Componente | Arquivo | Tokens principais | Comportamento | Equivalente no site |
| --- | --- | --- | --- | --- |
| `CalcMotScaffold` | `ui/design/components/CalcMotComponents.kt:75` | AppBackground | container Material 3 dark | layout de seção |
| `CalcMotTopBar` | `...:91` | TextPrimary, SectionTitle, spacing | navegação com alvo 48 dp | header responsivo |
| `CalcMotCard` | `...:189` | Surface, shape Sm, borders | clicável ou estático | cards de feature/resultado |
| `CalcMotButton` | `...:224` | BrandPrimary, SurfaceElevated, Danger, Great | primary, secondary, ghost, danger, premium; min 48 dp | CTA e links de ação |
| `CalcMotSwitchRow` | `...:285` | typography, text | alternância de preferência | demo de pausar/histórico |
| `CalcMotTextField` | `...:315` | Material Outlined | entrada de texto | calculadora/configuração |
| `CalcMotNumberField` | `...:342` | decimal keyboard | metas e custos | formulário de meta |
| `CalcMotSectionHeader` | `...:370` | ScreenTitle, ScreenSubtitle | título + contexto | cabeçalho de seção |
| `CalcMotInfoBanner` | `...:392` | card variants | aviso semântico | privacidade/explicação |
| `CalcMotStatusBadge` | `...:413` | Pill, cor a 16% | badge curto | status e classificações |
| `CalcMotEmptyState` | `...:435` | card + body | estado sem dados | explicar futuro sem números fictícios |
| `CalcMotBottomActionBar` | `...:462` | Surface, padding | ações agrupadas | CTA fixo mobile |
| `PermissionStatusCard` | `domain/CalcMotDomainComponents.kt:43` | card, badge, button | ativo/pendente/necessário/erro | seção confiança/como funciona |
| `GoalPresetCard` | `...:86` | premium card, radio, Success | preset selecionável | seletor de meta |
| `DailySummaryCard` | `...:156` | highlight, MetricValue | ofertas/acima/abaixo/médias | painel de prova |
| `FinancialImpactSummaryCard` | `...:180` | success/danger | impacto financeiro | exemplo de resultado |
| `OfferHistoryItem` | `...:201` | card, badge, Body | oferta + km/h + impacto | lista de mockups |
| `ServiceHealthCard` | `...:229` | highlight | saúde do serviço | FAQ técnico secundário |
| `BetaFeedbackCard` | `...:251` | premium, buttons | feedback positivo/negativo | não usar como aquisição principal |
| `CalcMotOverlayContainer` | `overlay/OverlayDesignSystem.kt:151` | background, border, radius, padding | janela compacta e movível no classic | mockup principal do hero |
| `OfferDecisionHeader` | `...:203` | badge + meaning | classificação e significado | legenda da demo |
| `MetricRow` | `...:243` | ValuePrimary/MetricValue | número + label | animação de cálculo |
| `FinancialImpactLine` | `...:275` | MetaImpactValue | impacto opcional | indicador acima/abaixo da meta |
| `OverlayMetricSummary` | `...:309` | R$/km, R$/h, tempo | hierarquia numérica | mockup de oferta |

## 7. Semântica de cores

| Semântica | App global | Overlay | Uso no site |
| --- | --- | --- | --- |
| ÓTIMA | `Great #1768F9` | `Great #6D3BFF`; outlined `PrototypeGreat #9C2A9A` | resolver token antes de publicação; tratar como premium |
| BOA | `Success #61E329` em vários contextos | `Good #2E7D32` | positivo saudável, sempre com texto |
| MÉDIA | `Warning #FFC453` | `Warning #FFB300` | atenção/contexto |
| RUIM | `Danger #FF6670` | `Bad #E53935` | abaixo da meta/risco |
| sucesso | `Success #61E329` | `Good #2E7D32` | prontidão e confirmação |
| alerta | `Warning #FFC453` | `Warning #FFB300` | pendência/no limite |
| erro | `Danger #FF6670` | `Bad #E53935` | falha/abaixo |
| informação | `Info #1768F9` | texto/tema | informação e CTA |
| fundo | `#04060A` | `#F01A1A1A` sobre o contexto | canvas e mockup |
| superfície | `#0B1016`, `#101720`, `#141A22` | `#232323` alfa | cards e agrupamentos |
| texto | `#F7F7F7`, `#B9BBC2`, `#8E929B` | `#FFFFFF`, `#E0E0E0`, `#BDBDBD` | hierarquia e contraste |
| borda | `#334D5966`, `#665F6E7D` | `#33FFFFFF` ou acento | limites discretos |

## 8. Classificação e lógica

Origem: `app/src/main/java/br/com/calcmot/model/FinancialImpact.kt:90–109`.

- `GREAT`: R$/km e R$/h pelo menos 20% acima das duas metas.
- `GOOD`: atende às duas metas.
- `WARNING`: atende a apenas uma das metas.
- `BAD`: não atende a nenhuma ou há dados inválidos.

Textos do cálculo: `Excelente: acima da sua meta`, `Boa por km e por hora`, `Na meta`, `Abaixo por km`, `Abaixo por hora` e `+R$ ... estimado`. O site deve manter o marcador “estimado” sempre que mostrar impacto projetado.

## 9. Assets e screenshots

### Assets de marca

- `app/src/main/res/drawable/calcmot_logo_hero.png`: ícone quadrado com velocímetro, volante/calculadora, check e verde; usado em `HomeScreen.kt`, `HomeReadyScreen.kt`, `HomePausedScreen.kt` e `OnboardingScreen.kt`.
- `app/src/main/res/drawable/calcmot_logo_512px.png`: variante do mesmo símbolo usada pelo launcher XML.
- O wordmark “CalcMot” aparece em Compose com `Calc` claro/itálico e `Mot` verde/itálico; não foi localizado um arquivo vetorial separado do wordmark.

### Referências visuais prioritárias

- Onboarding: `docs/design/references/calcmot-prototypes-2026/17-onboarding-inicial.png`.
- Home pronta: `.../06-home-pronto-calcular.png`.
- Home pausada: `.../25-home-calculo-pausado.png`.
- Histórico: `.../15-historico-com-ofertas.png`.
- Privacidade: `.../09-privacidade.png`.
- Ajuda: `.../05-ajuda.png`.
- Menu: `.../13-menu-lateral.png`.
- Meta: `.../16-minha-meta-edicao.png` e `.../23-minha-meta-salva.png`.
- Peça institucional do semáforo: `docs/design/social/calcmot-post-institucional-03-semaforo-1080x1350.png`.

Screenshots `.tmp` e dumps UIAutomator não devem ser embutidos no site ou versionados sem anonimização e consentimento; a documentação de QA alerta que algumas capturas contêm dados pessoais.

## 10. Recomendações técnicas para o site

### Fundo

Usar `#04060A` como base e `#0B1016`/`#101720` em cards. Adicionar glow verde radial de baixa opacidade apenas em hero ou transições. Manter áreas de respiro; não preencher tudo com textura ou gradiente.

### Cards

Converter `CalcMotCard` em blocos com borda sutil, raio visual entre 10–20 px e padding próximo de 16–24 px. Reservar superfícies elevadas para hero/status. Não aninhar cards.

### Botões

CTA primário em `#1768F9`, texto `#F7F7F7`, alvo de toque mínimo 48 px no mobile, raio próximo de 10 px. Usar verde para confirmar/prontidão, não para competir com o CTA azul. Tratar ÓTIMA como estado, não como botão padrão.

### Hero e mockup

Combinar wordmark, claim de decisão e overlay real. A ordem visual deve ser: problema → números → classificação → autonomia do motorista. Um único CTA primário acima da dobra.

### Benefícios

Mostrar quatro benefícios alinhados aos pilares: decisão em tempo real, meta, clareza financeira e controle/privacidade. Usar ícones simples e uma frase por benefício.

### Privacidade

Colocar um resumo de três bullets perto do primeiro CTA e linkar a política completa. Informar leitura local quando aplicável, limites do controle e possibilidade de pausar/apagar.

### FAQ

Priorizar: o que lê, por que acessibilidade, se aceita/recusa, compatibilidade, cálculo, histórico, estimado x confirmado e como desativar.

### Responsividade e acessibilidade

- mobile first, layout em uma coluna até a largura em que métricas possam respirar;
- não depender apenas de cor;
- respeitar aumento de fonte e `prefers-reduced-motion`;
- manter foco visível e alvos mínimos de 44–48 px;
- não cortar `R$/km`, `R$/h`, “ÓTIMA” ou textos de privacidade;
- mostrar “Etapa n de 3” quando houver carrossel/onboarding;
- validar contraste de `TextSecondary`, amarelo e roxo em cada superfície.

### CTA e claims

O CTA deve dizer exatamente o que acontece: abrir Play Store, ver demonstração ou entender o cálculo. Claims sobre Uber/99, processamento local, histórico, telemetria e disponibilidade de dashboard devem ser confrontados com o estado publicado do app antes do lançamento.

## 11. Pendências técnicas de design

- Resolver o token único da cor ÓTIMA entre global, overlay e protótipo.
- Decidir se a paleta semântica global será harmonizada com a paleta específica do overlay sem quebrar o runtime.
- Criar/registrar wordmark em SVG ou equivalente, se o site precisar dele fora do Compose.
- Definir tipografia web aprovada; hoje só existe `FontFamily.Default`.
- Confirmar telas e recursos financeiros que são WIP antes de apresentá-los como produto atual.
- Fazer auditoria de contraste web com os valores alfa completos das bordas/superfícies.
- Confirmar URL, selo e texto final da Play Store.
