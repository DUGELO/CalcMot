---
name: 'CalcMot — Câmera secreta'
description: 'Delta visual da gravação de segurança sobre o Design System Android existente do CalcMot.'
status: draft
completeness: partial
updated: 2026-08-24
sources:
  - '../../../specs/spec-gravacao-seguranca-audio-video/SPEC.md'
  - '../../../specs/spec-gravacao-seguranca-audio-video/recording-lifecycle.md'
  - '../../../specs/spec-gravacao-seguranca-audio-video/technical-constraints.md'
  - '../../../../app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotColors.kt'
  - '../../../../app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotTypography.kt'
  - '../../../../app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotSpacing.kt'
  - '../../../../app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotShape.kt'
  - '../../../../app/src/main/java/br/com/calcmot/ui/design/tokens/CalcMotElevation.kt'
  - '../../../../app/src/main/java/br/com/calcmot/ui/design/components/CalcMotComponents.kt'
  - '../../../../app/src/main/java/br/com/calcmot/ui/design/components/CalcMotFeedbackSheet.kt'
  - '../../../../AGENTS.md'
colors:
  brand-primary: '#1768F9'
  brand-secondary: '#4D8DFF'
  brand-accent: '#61E329'
  app-background: '#04060A'
  surface: '#0B1016'
  surface-elevated: '#101720'
  surface-soft: '#141A22'
  text-primary: '#F7F7F7'
  text-secondary: '#B9BBC2'
  text-muted: '#8E929B'
  success: '#61E329'
  warning: '#FFC453'
  danger: '#FF6670'
  border-subtle: '#4D596633'
  border-strong: '#5F6E7D66'
typography:
  screen-title:
    fontFamily: 'Android Default'
    fontSize: 24sp
    fontWeight: '700'
  screen-subtitle:
    fontFamily: 'Android Default'
    fontSize: 15sp
    fontWeight: '500'
  section-title:
    fontFamily: 'Android Default'
    fontSize: 18sp
    fontWeight: '700'
  card-title:
    fontFamily: 'Android Default'
    fontSize: 16sp
    fontWeight: '600'
  body:
    fontFamily: 'Android Default'
    fontSize: 14sp
    fontWeight: '400'
  body-strong:
    fontFamily: 'Android Default'
    fontSize: 14sp
    fontWeight: '600'
  caption:
    fontFamily: 'Android Default'
    fontSize: 12sp
    fontWeight: '500'
  metric-hero:
    fontFamily: 'Android Default'
    fontSize: 28sp
    fontWeight: '700'
  button:
    fontFamily: 'Android Default'
    fontSize: 15sp
    fontWeight: '700'
rounded:
  sm: 10dp
  md: 16dp
  lg: 20dp
  xl: 28dp
  full: 999dp
spacing:
  xs: 4dp
  sm: 8dp
  md: 12dp
  lg: 16dp
  xl: 24dp
  xxl: 32dp
  screen-horizontal: 16dp
  screen-vertical: 20dp
  card-padding: 16dp
  card-gap: 12dp
  section-gap: 20dp
components:
  feature-entry:
    base: 'CalcMotListItem / CalcMotCard'
    background: '{colors.surface}'
    foreground: '{colors.text-primary}'
    accent: '{colors.brand-primary}'
    border: '{colors.border-subtle}'
    radius: '{rounded.sm}'
  recording-card:
    base: 'CalcMotCard'
    background: '{colors.surface}'
    foreground: '{colors.text-primary}'
    secondary-foreground: '{colors.text-secondary}'
    border: '{colors.border-subtle}'
    radius: '{rounded.sm}'
    padding: '{spacing.card-padding}'
  primary-action:
    base: 'CalcMotButton.PRIMARY'
    background: '{colors.brand-primary}'
    foreground: '{colors.text-primary}'
    radius: '{rounded.sm}'
    min-height: 48dp
  stop-action:
    base: 'CalcMotButton.DANGER'
    background: '{colors.danger}'
    foreground: '{colors.text-primary}'
    radius: '{rounded.sm}'
    min-height: 48dp
  rec-indicator:
    foreground: '{colors.danger}'
    typography: '{typography.caption}'
    radius: '{rounded.full}'
  recording-status-badge:
    base: 'CalcMotStatusBadge'
    temporary: '{colors.warning}'
    preserved: '{colors.success}'
    expiring: '{colors.warning}'
    failure: '{colors.danger}'
    radius: '{rounded.full}'
  feedback-surface:
    base: 'CalcMotFeedbackSheet / CalcMotFeedbackAlertDialog / CalcMotSnackbarHost'
    background: '{colors.surface-elevated}'
    foreground: '{colors.text-primary}'
    secondary-foreground: '{colors.text-secondary}'
---

## Contrato vigente de experiência — revisão de 5 de outubro de 2026

Esta revisão manual adota [camera-product-ux-spec.md](../../../../camera/camera-product-ux-spec.md) e o plano CAM-001–024. Substitui as instruções de nome, descoberta e composição conflitantes abaixo; CAP-1–CAP-8 e AD-1–AD-14 permanecem estáveis. Os trechos anteriores registram a decisão histórica de agosto e não devem orientar a UI atual.

- Nome público: **Gravação de segurança**; topbar operacional: **Gravação**. A entrada em Ferramentas é passiva e usa ícone Material; abrir não adquire câmera/microfone.
- Principal: um painel de estado e ações; configuração fica em Ajustes. Retirar o antigo card de configuração e as linhas de saúde por recurso.
- Primeiro uso: transparência versionada → permissões contextuais → enquadramento real → pronto. A autorização final nunca inicia mídia. Texto de transparência é proposta sujeita a LEGAL-01.
- Permissão concedida significa **autorizado**. REC exige evento observado, áudio ativo e avanço de duração de vídeo; retomada exige progresso posterior à pausa.
- Enquadramento usa preview CameraX inteiro e informa que não grava; libera recursos ao sair. Uso recorrente preparado inicia por uma ação explícita, sem repetir modal genérico.
- Pausa, retomada, troca de arquivo e finalização mantêm a mesma composição e não exibem REC. A troca permite somente Parar.
- Histórico e detalhe usam a sessão real, disponibilidade privada separada da galeria, player nativo sem autoplay, prazo desde o encerramento e exclusão confirmada. Miniaturas foram solicitadas pelo usuário; não são mídia real nos previews sintéticos.

O aceite visual integrado e a qualificação em aparelhos permanecem gates; renders não comprovam captura ou continuidade física.



# CalcMot — Câmera secreta

> Spine visual parcial que herda integralmente o Design System Compose do CalcMot e documenta apenas os deltas da gravação. Sem protótipo ou mock, nenhuma composição visual nova foi inventada. Esta spine prevalece sobre mocks futuros até reconciliação explícita; decisões técnicas, jurídicas e de consumo abertas permanecem em `EXPERIENCE.md`.

## Brand & Style

A gravação de segurança preserva a identidade escura, premium, automotiva, direta e confiável do CalcMot. Ela deve parecer uma ferramenta operacional do app, não uma câmera social, um editor de vídeo ou uma experiência de vigilância teatral. O motorista entende em um olhar se a captura está pronta, gravando, pausada, finalizando ou indisponível.

Não existe uma nova marca visual para “Câmera secreta”. O nome é o rótulo de entrada escolhido para o grid de Ferramentas; dentro do recurso, a interface continua explícita sobre áudio, vídeo, permissões, gravação e retenção. Indicadores de privacidade do Android e a notificação persistente nunca são disfarçados.

O sistema é Compose nativo, Material 3 e componentes CalcMot. Ícones vêm de Material Icons ou de assets já aceitos; não há ícones improvisados em Canvas. Dynamic color não substitui esta identidade.

## Colors

Todos os valores são tokens reais de `CalcMotColors`; os valores de oito dígitos usam notação CSS `#RRGGBBAA` para preservar o alpha definido no código.

- **Fundo e superfícies.** `{colors.app-background}` sustenta todas as telas. `{colors.surface}`, `{colors.surface-elevated}` e `{colors.surface-soft}` criam hierarquia tonal sem introduzir uma nova paleta.
- **Ações normais.** `{colors.brand-primary}` identifica Iniciar, Retomar, salvar e outras ações primárias não destrutivas. `{colors.brand-secondary}` fica restrito aos usos secundários já previstos pelo DS.
- **Captura ativa e ação Parar.** `{colors.danger}` é o sinal inequívoco de `REC` e da ação Parar durante uma gravação. Pausar e Retomar não usam vermelho.
- **Estados de arquivo.** `{colors.warning}` comunica temporário/expirando, `{colors.success}` confirma cópia publicada na galeria e `{colors.danger}` marca falha. Cor nunca é o único meio de distinguir estados; o texto do badge é obrigatório.
- **Texto.** `{colors.text-primary}` carrega títulos, valores e ações; `{colors.text-secondary}` explica; `{colors.text-muted}` serve apenas a metadados menos prioritários.
- **Contorno.** `{colors.border-subtle}` é a borda normal; `{colors.border-strong}` só reforça separação ou foco já previsto pelo componente.

Não criar cor especial para pausa, câmera, áudio, qualidade, segmentos ou retenção. Não usar `{colors.brand-accent}` como substituto de `REC`: o verde permanece token da marca/sucesso, e não significa captura ativa.

Antes da implementação, validar contraste mínimo WCAG AA nas combinações usadas pelo recurso: 4,5:1 para texto comum e 3:1 para texto grande, ícones funcionais, bordas de foco e controles. Nenhum token de estado pode ser aprovado apenas por existir no Design System atual.

## Typography

A rampa é a de `CalcMotTypography`, com `FontFamily.Default` do Android. Títulos de tela usam `{typography.screen-title}`; títulos de seção usam `{typography.section-title}`; títulos de card e itens usam `{typography.card-title}`; instruções usam `{typography.body}`; metadados e prazo restante usam `{typography.caption}`; ações usam `{typography.button}`.

O tempo de gravação pode usar `{typography.metric-hero}` quando for o valor principal da tela ativa. `REC` usa `{typography.caption}` com peso e cor, nunca tipografia decorativa. Rótulos não ficam em caixa alta, exceto o literal curto `REC`.

Textos devem aceitar o aumento da escala de fonte, crescer verticalmente e quebrar em linhas legíveis. Títulos de ação, prazo de retenção e mensagens de falha não podem ser truncados de forma a esconder significado.

## Layout & Spacing

O recurso usa a escala real de `CalcMotSpacing`. Margens de tela são `{spacing.screen-horizontal}` e `{spacing.screen-vertical}`; cards usam `{spacing.card-padding}`; listas e grids usam `{spacing.card-gap}`; blocos maiores separam-se por `{spacing.section-gap}`.

As telas são prioritariamente de uma coluna. Configuração, preview, controles e biblioteca não são encaixados em cards dentro de cards. O preview é uma superfície funcional; sua proporção e orientação dependem da decisão técnica ainda aberta, e não são fixadas sem protótipo ou validação nos aparelhos-alvo.

Em telas pequenas ou com fonte ampliada, o conteúdo rola e as ações críticas continuam alcançáveis sem sobreposição. A barra inferior existente pode hospedar ações quando isso preservar o acesso ao CTA, respeitando os insets do sistema.

## Elevation & Depth

A hierarquia usa primeiro contraste tonal e borda. Os tokens reais são `None` 0dp, `Low` 2dp, `Medium` 6dp, `High` 12dp e `Overlay` 8dp. Cards de gravação permanecem no nível visual padrão; sheets, dialogs e feedback podem usar o nível de overlay já adotado pelo DS.

Não adicionar brilho vermelho pulsante, sombra cromática, gradiente ou profundidade cenográfica para simular urgência. Estado verdadeiro vem de texto, cor semântica e disponibilidade da ação.

## Shapes

Cards e botões herdam `{rounded.sm}`. Superfícies maiores podem usar `{rounded.md}`, `{rounded.lg}` ou `{rounded.xl}` somente quando o componente CalcMot correspondente já o fizer. Badges usam `{rounded.full}`.

Áreas de toque têm no mínimo 48dp, independentemente do raio visual. Preview e thumbnail seguem o raio de seu container; controles nunca dependem de pequenos alvos circulares sem rótulo acessível.

## Components

- **Entrada Ferramentas.** O item de menu “Ferramentas” usa iconografia Material e o tratamento de navegação existente. O card “Câmera secreta” no grid usa `{components.feature-entry}`; o ícone de câmera é funcional, não decorativo.
- **Card de configuração.** Usa `{components.recording-card}` para câmera frontal padrão, lente disponível, qualidade efetiva, duração de segmento e estado de câmera/microfone. Não criar subcards para cada linha.
- **Preview.** É a região de enquadramento antes de Iniciar. Estado indisponível troca a imagem por uma explicação acionável; não simula imagem quando a câmera falha.
- **Ação Iniciar/Retomar.** Usa `{components.primary-action}`. Iniciar permanece desabilitada até câmera e microfone estarem disponíveis. Retomar só aparece no estado Pausado.
- **Ação Pausar.** É uma ação normal e explícita, com variante primária, secundária ou outlined já existente; nunca usa `{colors.danger}`.
- **Ação Parar.** Usa `{components.stop-action}` na tela e a ação equivalente de encerramento na notificação do Android. O visual de perigo não se estende às demais ações.
- **Indicador `REC`.** Usa `{components.rec-indicator}` e só aparece após confirmação de captura real. Some imediatamente ao pausar, falhar ou começar a finalizar.
- **Timer.** Usa `{typography.metric-hero}` na tela ativa e a convenção nativa da notificação. A semântica do contador durante pausa depende de decisão técnica; o visual não antecipa essa resposta.
- **Item de biblioteca.** Usa `{components.recording-card}` com data, duração, tamanho, estado e prazo. O badge usa `{components.recording-status-badge}` e sempre inclui rótulo textual.
- **Feedback e confirmações.** Usam `{components.feedback-surface}`. Exclusão manual exige confirmação; falhas mostram causa e próxima ação; sucesso de galeria só aparece após confirmação do `MediaStore`.
- **Notificação e Sharesheet.** São superfícies Android, não skins CalcMot. Respeitam o componente e a iconografia do sistema; a identidade do app entra por nome, conteúdo e small icon aprovado, não por uma recriação visual customizada.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Herdar tokens e componentes CalcMot por nome | Criar uma paleta “de câmera” ou copiar estética de rede social |
| Mostrar `REC` em `{colors.danger}` somente após captura real | Mostrar `REC` durante início, pausa, finalização ou falha |
| Usar `{colors.brand-primary}` para Iniciar, Pausar/Retomar e ações normais | Colorir Pausar de vermelho ou fazer toda a tela piscar |
| Mostrar estado e prazo em texto, além da cor | Depender apenas de vermelho, verde ou amarelo |
| Manter a notificação Android reconhecível e operável | Ocultar indicadores do sistema ou simular uma notificação dentro do app |
| Usar cards simples, sem aninhamento | Colocar preview, configuração e controles dentro de múltiplos cards encaixados |
| Validar em preview Compose e screenshot real antes de fechar o visual | Tratar esta spine sem mock como fidelidade visual final |
| Preservar esta spine como contrato visual | Deixar um mock futuro sobrescrever tokens ou estados sem reconciliação |
