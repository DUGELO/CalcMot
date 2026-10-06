---
name: 'CalcMot — Câmera secreta'
status: draft
completeness: partial
updated: 2026-08-24
sources:
  - './DESIGN.md'
  - './.memlog.md'
  - '../../../specs/spec-gravacao-seguranca-audio-video/SPEC.md'
  - '../../../specs/spec-gravacao-seguranca-audio-video/recording-lifecycle.md'
  - '../../../specs/spec-gravacao-seguranca-audio-video/technical-constraints.md'
  - '../../../../AGENTS.md'
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



# CalcMot — Experience Spine da Câmera secreta

## Status, autoridade e deltas pendentes

Esta spine permanece em rascunho parcial. As opções de retenção estão definidas; decisões técnicas de captura, validações jurídicas/de consumo e a sincronização da SPEC para Pausar/Retomar e retenção extensível continuam abertas.

`DESIGN.md` é a referência de identidade visual; esta spine define arquitetura de informação, comportamento, estados e fluxos. Nenhum protótipo ou mock foi fornecido, portanto todas as superfícies são **spine-only** e nenhuma composição visual nova foi inventada. As spines prevalecem sobre mocks, wireframes e imports até reconciliação explícita.

## Foundation

Aplicativo Android mobile nativo, em Kotlin, Jetpack Compose e Material 3, com Design System próprio CalcMot. A experiência herda componentes, navegação e padrões do app; `DESIGN.md` contém somente os deltas visuais da gravação. O recurso deve funcionar enquanto outro app está em primeiro plano ou a tela está apagada, com controle verdadeiro pela notificação persistente do Android.

O único protagonista e operador é o **motorista**. Não existe conta de passageiro, início remoto, captura iniciada por Uber/99, AccessibilityService ou detecção automática de corrida.

O caminho definido é menu lateral **Ferramentas** → tela Ferramentas → card de grid **Câmera secreta**. [ASSUMPTION] Até que a navegação existente seja validada em implementação, a posição exata, o ícone Material e a composição do grid herdam os padrões atuais do CalcMot, sem redesenhar a navegação principal.

### Cobertura de capacidades

| Capacidade | Escopo | Fontes canônicas |
|---|---|---|
| CAP-1 | Configuração, permissões, lente, qualidade, segmento e preview | `Information Architecture`; `Component Patterns`; Flow 1 |
| CAP-2 | Iniciar, Pausar/Retomar, Parar e finalizar com estado verificável | `State Patterns`; `Interaction Primitives`; Flows 1–3 |
| CAP-3 | Continuidade fora do app, notificação persistente e encerramento na reinicialização | `Responsive & Platform`; `Notificação persistente`; Flow 2 |
| CAP-4 | Biblioteca, metadados, detalhe e reprodução de mídia íntegra | `Information Architecture`; `Component Patterns`; Flow 3 |
| CAP-5 | Retenção temporária, extensão, expiração e preservação indefinida | `Retenção, galeria e compartilhamento`; Flow 5 |
| CAP-6 | Cópia independente e confirmação verificável no `MediaStore` | `Component Patterns`; `Retenção, galeria e compartilhamento`; Flow 4 |
| CAP-7 | Compartilhamento pelo Android Sharesheet sem alterar retenção | `Component Patterns`; `Retenção, galeria e compartilhamento`; Flow 4 |
| CAP-8 | Uma fonte de estado entre `REC`, tela, notificação e biblioteca | `State Patterns`; `Accessibility Floor`; Flows 1–3 |

## Open Questions

| ID | Pergunta ou delta canônico | Decidido | Em aberto / bloqueio |
|---|---|---|---|
| SPEC-1 | Câmera, lente, orientação e qualidade padrão | Frontal padrão; lentes disponíveis selecionáveis; 480p padrão; 720p só após equivalência medida. | Orientação e troca durante captura. |
| SPEC-2 | Tamanho de arquivo, segmentos e reserva de armazenamento | Segmentos de 5, 10, 20, 30 e 60 minutos; padrão de 10 minutos. | Tamanho máximo, agrupamento/reprodução e reserva mínima. |
| SPEC-3 | Continuidade após remover recentes, encerrar processo ou reiniciar | Continua ao remover recentes; reinício encerra sem retomar. | Encerramento forçado e recuperação de arquivo. |
| SPEC-4 | Tolerância entre expiração e exclusão física | Expiração lógica no prazo; exclusão física na próxima limpeza; 24 horas, 3, 7, 15 ou 30 dias. | Garantias e telemetria da limpeza. |
| SPEC-5 | Transparência ao passageiro e base jurídica no Brasil | Aviso ao motorista definido. | Texto ao passageiro e base jurídica; bloqueiam publicação. |
| UX-1 | Recálculo da retenção | Opções de 3, 7, 15 ou 30 dias. | Regra de recálculo e limite de alterações. |
| UX-2 | Timer durante pausa | Pausar/Retomar confirmado. | Tempo cronológico ou efetivamente capturado. |
| UX-3 | Agrupamento de segmentos | Percepção de sessão única na biblioteca. | Agrupamento, finalização e apresentação ao Pausar/Retomar. |
| UX-4 | Recuperação operacional | Falha nunca afirma captura ou arquivo válido. | Garantias após encerramento forçado, falha de segmento, câmera/microfone ocupados, armazenamento cheio ou aquecimento. |
| UX-5 | Sincronização da especificação | Pausado, Pausando, Retomando, ações da notificação e retenção extensível confirmados. | Incorporar na SPEC e em `recording-lifecycle.md` antes da arquitetura final. |

## Information Architecture

| Superfície | Entrada | Saída |
|---|---|---|
| Menu lateral | Navegação global | Ferramentas |
| Ferramentas | Menu lateral → Ferramentas | Hub pelo card **Câmera secreta** |
| Hub de Gravação de Segurança | Ferramentas → Câmera secreta | Configuração/Preview ou Biblioteca |
| Configuração e permissões | Hub → Configurar/iniciar | Preview quando câmera e microfone estão prontos |
| Preview | Configuração pronta | Configuração ou Iniciar |
| Gravação ativa | Captura real confirmada | Pausar ou Parar |
| Gravação pausada | Gravação ativa → Pausar | Retomar ou Parar |
| Notificação persistente Android | Captura real confirmada | Estado atual no CalcMot, Pausar/Retomar ou Parar |
| Finalização | Parar ou encerramento do sistema | Temporário ou Falha após verificação |
| Biblioteca | Hub → Gravações | Detalhe de item disponível |
| Detalhe e reprodução | Biblioteca → item disponível | Reproduzir, Salvar na galeria, Compartilhar ou Excluir conforme estado |
| Extensão de retenção | Item temporário → Manter por mais tempo | Novo prazo temporário |
| Confirmação de exclusão | Item temporário → Excluir | Cancelar ou Excluir |
| Transparência de compartilhamento | Detalhe → Compartilhar | Cancelar ou abrir o Android Sharesheet |
| Android Sharesheet | Continuar compartilhamento | App receptor ou retorno ao detalhe |
| Feedback de galeria | Salvar na galeria | Salvo na galeria ou temporário com falha informada |
| Falha e recuperação | Qualquer estado operacional | Próxima ação segura ou retorno ao estado disponível |

Não há modal sobre modal. Permissões e Sharesheet pertencem ao Android; sheets, dialogs e snackbars CalcMot cobrem confirmações e feedback local. A Biblioteca nunca funciona como galeria pública: temporários continuam privados.

## Voice and Tone

Microcopy é curta, operacional e verificável. A voz de marca vive em `DESIGN.md`; aqui, o texto nunca promete captura, preservação ou compartilhamento antes da confirmação real.

| Faça | Evite |
|---|---|
| Aviso inicial: “O CalcMot gravará áudio e vídeo durante a corrida.” | Explicação longa, alarmista ou redundante antes de cada gravação |
| “Câmera pronta” / “Microfone pronto” | “Tudo certo!” sem dizer o que está disponível |
| “Gravando” e `REC` somente após confirmação real | “Gravação iniciada” no toque da CTA |
| “Pausado — áudio e vídeo não estão sendo gravados” | Manter `REC` visível durante a pausa |
| “Finalizando gravação…” | Mostrar o item como disponível antes da integridade confirmada |
| “Salvo na galeria” após `MediaStore` | “Salvo” durante cópia |
| “Temporário · expira em …” | “Seguro por 24h” quando o prazo está perto do fim |
| “Compartilhar não salva esta gravação” | Sugerir que compartilhar preserva o temporário |
| Nomear causa e próxima ação | “Erro desconhecido” como resposta final |

**Vocabulário fixo:** Iniciar gravação, Pausar, Retomar, Parar, Temporário, Expirando, Salvo na galeria, Salvar na galeria, Compartilhar, Excluir. “Câmera secreta” é o rótulo de descoberta no grid; não significa invisibilidade para Android nem ausência de indicadores de privacidade.

## Component Patterns

O comportamento abaixo usa componentes CalcMot; a aparência vive em `DESIGN.md.Components`.

| Componente | Uso | Regras comportamentais |
|---|---|---|
| Item Ferramentas | Menu lateral | Abre a tela Ferramentas; não inicia captura nem solicita permissão. |
| Card Câmera secreta | Grid Ferramentas | Usa `{components.feature-entry}`. Toque abre o Hub; sem atalhos silenciosos. |
| Estado de prontidão | Hub/Configuração | Uma linha por câmera e microfone, com rótulo e ação contextual. Cor não substitui texto. |
| Seletor de câmera | Configuração/Preview | Câmera frontal é padrão; lista somente lentes detectadas. Troca ocorre antes da captura; comportamento durante gravação permanece fora do contrato até decisão técnica. |
| Qualidade efetiva | Configuração | Mostra 480p como padrão. 720p só substitui o padrão se medições nos aparelhos-alvo comprovarem qualidade adequada e consumo de bateria equivalente ao de 480p. Não antecipar essa validação na UI. |
| Seletor de segmento | Configuração | Opções 5, 10, 20, 30 e 60 minutos; 10 minutos selecionado por padrão. Segmentação não muda a percepção de uma sessão única na biblioteca sem decisão técnica de agrupamento. |
| Preview | Antes de iniciar | Mostra enquadramento real da lente escolhida. Se câmera ou microfone faltar, troca para mensagem acionável e mantém Iniciar desabilitado. |
| Aviso inicial | Primeiro passo de início | Confirmação simples com a frase aprovada e ações Cancelar/Iniciar; não repete tutorial. |
| CTA Iniciar/Retomar | Preview/Pausado | Usa `{components.primary-action}`. Iniciar só habilita com câmera e microfone disponíveis; Retomar só habilita se o sistema puder reativar ambas. |
| Controles Pausar/Parar | Gravando | Pausar é ação normal; Parar usa `{components.stop-action}`. Debounce impede comandos duplicados durante transição. |
| Indicador `REC` | Tela e notificação | Usa `{components.rec-indicator}` e deriva da confirmação da captura real. Nunca é otimista. |
| Timer | Gravando/Pausado/Notificação | Exibe duração da mesma fonte de estado. A semântica de tempo corrido versus tempo capturado durante pausa é decisão técnica aberta. |
| Notificação persistente | Fora do app | Gravando: Pausar e Parar. Pausado: Retomar e Parar. Abrir a notificação retorna ao estado atual no CalcMot. |
| Item de gravação | Biblioteca | Card inteiro abre detalhe quando disponível. Metadados: data, duração, tamanho, estado e prazo. Arquivo inválido não abre player. |
| Badge de estado | Biblioteca/detalhe | Usa `{components.recording-status-badge}` com texto Temporário, Salvo na galeria, Expirando ou Falha. |
| Player | Detalhe | Play/pause, scrub e áudio por controles nativos/acessíveis. Ações de arquivo permanecem separadas do playback. |
| Salvar na galeria | Detalhe | Cópia idempotente por item e independente de Compartilhar; mostra progresso, bloqueia toque duplicado e só muda o estado após o `MediaStore` confirmar que a cópia publicada pode ser lida. Não presume que um app receptor preservou a mídia. |
| Compartilhar | Detalhe | Primeiro mostra transparência; depois abre Sharesheet com a URI escolhida e permissão temporária de leitura. Cancelar o Sharesheet não altera arquivo nem retenção. |
| Excluir | Detalhe | Apenas temporário privado; exige confirmação e não usa swipe-to-delete como único caminho. Cópia na galeria não é removida. |
| Feedback | Todas | Usa `{components.feedback-surface}`; sheet para explicação/ação, dialog para exclusão e snackbar para confirmação não bloqueante. |

## State Patterns

O ciclo abaixo é a única linguagem de estado entre tela, serviço, notificação e biblioteca. Os deltas que exigem sincronização da SPEC estão centralizados em `Status, autoridade e deltas pendentes` e `Open Questions`.

| Estado | Entrada verificável | UI e ações permitidas | Saída |
|---|---|---|---|
| Não configurado | Permissão/configuração ausente | Configurar; conceder câmera e microfone; Iniciar desabilitado | Pronto ou Falha |
| Pronto | Câmera e microfone disponíveis; preview confirmado | Escolher lente/segmento; Iniciar | Iniciando ou Falha |
| Iniciando | Motorista confirmou o aviso e solicitou início | Progresso curto; sem `REC`; bloquear novo início | Gravando ou Falha |
| Gravando | Captura real de áudio e vídeo confirmada | `REC`, duração, Pausar, Parar | Pausado, Finalizando ou Falha |
| Pausando | Comando aceito, captura ainda não confirmada como pausada | Sem novo comando; `REC` some assim que a captura deixa de estar ativa | Pausado ou Falha |
| Pausado | Áudio e vídeo confirmados como inativos dentro da mesma sessão | Sem `REC`; Retomar; Parar | Retomando, Finalizando ou Falha |
| Retomando | Comando aceito, captura ainda não confirmada como ativa | Sem `REC`; bloquear novo comando | Gravando ou Falha |
| Finalizando | Acionamento explícito de Parar, reinicialização ou encerramento comunicado | Aguardar; sem reprodução/salvar/compartilhar; sem `REC` | Temporário ou Falha |
| Temporário | Arquivo privado íntegro e reproduzível | Reproduzir, compartilhar, salvar na galeria, solicitar extensão, excluir | Preservado, Expirado ou Excluído |
| Salvando na galeria | Cópia para `MediaStore` em curso | Mostrar progresso; impedir duplicação; temporário e prazo permanecem | Preservado ou Temporário com falha informada |
| Preservado | `MediaStore` confirmou que a cópia publicada pode ser lida | Reproduzir e compartilhar a cópia; cópia privada mantém sua própria retenção | Permanece fora da retenção do CalcMot |
| Expirado | Prazo lógico terminou | Indisponível imediatamente; sem ações sobre temporário | Exclusão física oportunística |
| Falha | A captura ou a finalização não produz um estado válido nem um arquivo válido | Remover `REC`; mostrar causa e ação possível; nunca habilitar ações de arquivo inválido | Pronto ou nova tentativa |

Regras transversais:

- Remover o CalcMot dos recentes não altera os estados Gravando ou Pausado. A notificação continua como controle.
- Reiniciar o aparelho encerra a sessão; o CalcMot não retoma após boot. Qualquer arquivo recuperável só aparece após verificação de integridade.
- A pausa cria uma lacuna explícita: nenhum texto pode sugerir captura contínua durante o intervalo.
- `REC` não aparece em Iniciando, Pausando, Pausado, Retomando, Finalizando ou Falha.
- Ações de arquivo aparecem somente para mídia íntegra e reproduzível.
- Expiração lógica torna o item indisponível no prazo; exclusão física pode acontecer na próxima rotina de limpeza.

## Interaction Primitives

- **Toque explícito para gravar.** Somente uma Activity visível do CalcMot pode iniciar a captura. Abrir o Hub, Preview, Uber/99 ou o serviço de acessibilidade nunca inicia captura.
- **Toque único, estado confirmado.** Iniciar, Pausar, Retomar e Parar entram em estado intermediário até confirmação; toques repetidos ficam bloqueados.
- **Voltar e sair.** Voltar da tela ativa ou alternar entre apps não interrompe a sessão. A UI informa que o controle permanece na notificação.
- **Interações banidas.** Início automático, gesto secreto, controle apenas por cor, long-press como ação primária, modal sobre modal, autoplay com áudio, upload ou integração direta com rede social.

Regras redistribuídas permanecem em `Responsive & Platform`, `Component Patterns` e `Privacidade, Notificação e Retenção`.

## Accessibility Floor

- TalkBack anuncia título da superfície, papel, estado e ação. O conjunto “Gravando, áudio e vídeo ativos, duração …” é uma unidade semântica; “Pausado, áudio e vídeo não estão sendo gravados” é outra.
- Mudanças Gravando ↔ Pausado ↔ Falha são anunciadas, sem repetição a cada atualização do timer. O timer não vira live region de alta frequência.
- Todo alvo interativo tem no mínimo 48dp. Ícones de câmera, ferramentas, Pausar, Retomar e Parar têm rótulo acessível; ícones decorativos ficam fora da árvore semântica.
- Ordem de foco acompanha leitura: estado → preview/metadados → ação principal → ações secundárias. Em um dialog, o foco começa no título e permanece contido até o fechamento.
- Com a escala de fonte ampliada, o conteúdo deve quebrar e as ações devem ser empilhadas, sem truncar causa, prazo ou rótulo crítico. O texto usa os papéis de `DESIGN.md`, como `{typography.body}` e `{typography.button}`.
- Estado nunca depende só de `{colors.danger}`, `{colors.warning}` ou `{colors.success}`; badges e controles incluem texto e papel.
- Player expõe Play/Pausar, posição, duração e controle de volume com semântica nativa. Gestos têm alternativa por botão.
- Contraste visual e foco seguem `DESIGN.md`; controles do sistema herdam acessibilidade Android.
- Indicadores de privacidade do Android e a notificação não podem ser ocultados, inclusive por preferência de acessibilidade.

## Responsive & Platform

| Contexto | Comportamento |
|---|---|
| Celular pequeno | Uma coluna, conteúdo rolável, ações empilhadas quando necessário e nenhum card aninhado. A CTA permanece acessível sem cobrir conteúdo. |
| Celular alto | Preserva ritmo de `{spacing.section-gap}`; espaço extra favorece preview e conteúdo, não vazios decorativos. |
| Escala de fonte ampliada | Títulos e botões crescem verticalmente; metadados quebram em linhas; o prazo nunca é elidido. |
| Landscape | Preview e controles continuam alcançáveis com scroll/insets. Orientação de captura e transformação do preview permanecem decisão técnica aberta. |
| Outro app em primeiro plano | Notificação persistente é a superfície de estado e controle. |
| Tela apagada | Captura pode continuar; notificação permanece disponível ao desbloquear. |
| App removido dos recentes | Sessão continua. Reabrir o CalcMot reconecta à mesma fonte de estado. |
| Reinicialização | Sessão encerra e não retoma automaticamente. Ao abrir, qualquer resíduo passa por verificação antes de aparecer. |
| Android (níveis de API 24–36) | UI respeita insets, barras do sistema, runtime permissions e diferenças de foreground service; a arquitetura valida comportamentos por versão. |

A experiência não define tablet, foldable ou layout de duas colunas sem evidência de necessidade. A câmera frontal é padrão; qualidade inicial é 480p. 720p só pode virar padrão após medição de qualidade, consumo, aquecimento e estabilidade nos aparelhos-alvo.

## Privacidade, Notificação e Retenção

### Privacidade e permissões

- O aviso inicial é: **“O CalcMot gravará áudio e vídeo durante a corrida.”** A confirmação é simples e não substitui política de privacidade, base jurídica ou transparência ao passageiro.
- Câmera e microfone são explicados e solicitados no contexto. Sem ambos, Iniciar não habilita.
- A captura é discreta para o passageiro, mas nunca oculta do motorista ou do Android que câmera e microfone estão ativos.
- Não há upload, nuvem, transmissão ao vivo, acesso remoto, SDK social ou início por Uber/99/AccessibilityService.
- Antes de publicação, Política de Privacidade, Segurança dos dados e declarações Play Store devem cobrir câmera, microfone, retenção, galeria e compartilhamento.

### Notificação persistente

- Surge quando a captura real é confirmada e permanece enquanto a sessão estiver Gravando ou Pausada.
- Gravando: estado, duração, áudio e vídeo ativos, ações Pausar e Parar.
- Pausado: estado explícito, áudio e vídeo pausados, ações Retomar e Parar; sem `REC`.
- Usa a mesma fonte de estado da tela e da biblioteca. Se uma ação falhar, a notificação deixa de afirmar o estado anterior e encaminha recuperação no app quando necessário.
- O motorista sempre pode encerrar a sessão pela ação Parar. Tocar no corpo retorna ao estado atual da sessão no CalcMot.

### Retenção, galeria e compartilhamento

- Padrão: 24 horas a partir da finalização válida. Depois disso, a cópia não preservada fica logicamente indisponível e pode ser removida fisicamente na próxima rotina de limpeza.
- O motorista pode escolher permanência temporária de 24 horas (padrão), 3, 7, 15 ou 30 dias. Permanência indefinida só existe por **Salvar na galeria**. A regra técnica para recalcular um prazo já em curso ainda precisa ser ratificada pela arquitetura.
- Salvar na galeria cria cópia independente no `MediaStore`. Falha de cópia preserva o temporário e seu prazo vigente.
- Compartilhar pode usar temporário válido ou cópia preservada, abre Android Sharesheet e concede somente leitura temporária por URI `content://`.
- Compartilhar não salva, não estende e não reinicia retenção. Depois da entrega, limites e redistribuição pertencem ao app receptor.
- Excluir temporário é imediato após confirmação e nunca apaga a cópia já salva na galeria.

## Key Flows

### Flow 1 — Configurar, enquadrar e iniciar (motorista antes da corrida)

1. O motorista abre o menu lateral e toca em **Ferramentas**.
2. Na tela Ferramentas, toca no card **Câmera secreta**.
3. O Hub mostra que a configuração ainda está pendente e leva à tela Configuração e permissões.
4. O motorista mantém a câmera frontal ou escolhe outra lente disponível; vê 480p como qualidade padrão e escolhe a duração do segmento entre 5, 10, 20, 30 ou 60 minutos, com a opção de 10 minutos selecionada.
5. A interface explica câmera e microfone separadamente e solicita as permissões do Android no contexto.
6. Com ambas disponíveis, o preview real aparece; Iniciar torna-se habilitado.
7. O motorista toca em Iniciar e confirma: “O CalcMot gravará áudio e vídeo durante a corrida.”
8. A tela entra em Iniciando, ainda sem `REC`.
9. **Clímax:** a captura real é confirmada; `REC`, duração e “áudio e vídeo ativos” aparecem juntos, e a notificação persistente oferece Pausar e Parar.

Falha: permissão negada, câmera ocupada, microfone ocupado ou armazenamento insuficiente mantém `REC` ausente, informa a pendência correta e oferece a próxima ação possível.

### Flow 2 — Pausar, retomar e controlar fora do app (motorista durante a corrida)

1. Com a gravação confirmada, o motorista abre Uber ou 99; a captura continua em foreground service.
2. A tela apaga e, depois, é desbloqueada; a notificação mostra o mesmo estado e duração.
3. O motorista toca em Pausar na notificação.
4. Enquanto o sistema confirma a transição, `REC` desaparece; a notificação passa a Pausado e deixa explícito que áudio e vídeo não estão sendo gravados.
5. O motorista remove o CalcMot dos recentes; a sessão pausada e a notificação permanecem.
6. Toca em Retomar; somente após áudio e vídeo voltarem de fato, `REC` reaparece.
7. **Clímax:** sem reabrir o CalcMot, o motorista vê na notificação que a gravação está novamente ativa e mantém controle sobre Pausar e Parar.

Falha: se Retomar não reativar câmera e microfone, `REC` não volta; a notificação mostra falha e encaminha o motorista ao app. Se o aparelho reiniciar, a sessão encerra e não retoma automaticamente.

### Flow 3 — Parar, validar e reproduzir (motorista ao fim da corrida)

1. O motorista toca em Parar na notificação ou na tela ativa.
2. Todas as superfícies removem `REC` e mostram Finalizando.
3. O sistema fecha segmentos e verifica a integridade e a capacidade de reprodução do arquivo ou da sessão resultante.
4. Só depois da confirmação, a Biblioteca recebe um item Temporário com data, duração, tamanho e prazo.
5. O motorista abre o item e inicia a reprodução com áudio dentro do CalcMot.
6. **Clímax:** o player reproduz vídeo e áudio e o item mostra seu estado verdadeiro e o tempo restante, confirmando que existe evidência utilizável.

Falha: se a finalização não produzir mídia válida, a Biblioteca mostra Falha sem Play, Salvar ou Compartilhar e oferece causa/recuperação quando possível.

### Flow 4 — Preservar e compartilhar sem confundir ações (motorista depois da corrida)

1. No detalhe de um temporário válido, o motorista toca em **Salvar na galeria**.
2. A interface mostra a cópia em andamento sem alterar prematuramente o badge.
3. Após o `MediaStore` confirmar que a cópia publicada pode ser lida, aparece **Salvo na galeria**.
4. Separadamente, o motorista toca em **Compartilhar**.
5. A transparência informa que compartilhar não salva e que o CalcMot deixa de controlar retenção e redistribuição após a entrega.
6. O motorista continua e escolhe um app no Android Sharesheet.
7. **Clímax:** a cópia preservada permanece reproduzível na galeria e o compartilhamento ocorre pelo sistema, sem alterar o prazo da cópia temporária.

Falha: se salvar falhar, o temporário e seu prazo original permanecem; se o app receptor rejeitar o vídeo, o CalcMot não declara compartilhamento concluído nem altera o arquivo.

### Flow 5 — Expirar ou estender um temporário (motorista administrando retenção)

1. A Biblioteca mostra o prazo restante de um temporário.
2. Sem ação do motorista, ao atingir o prazo padrão de 24 horas o item fica logicamente indisponível.
3. A limpeza física ocorre de forma oportunística na próxima rotina; uma cópia salva na galeria permanece intacta.
4. Antes de expirar, o motorista pode escolher **Manter por mais tempo** e selecionar 3, 7, 15 ou 30 dias; 24 horas permanece o padrão.
5. O sistema confirma a escolha e recalcula o prazo conforme a regra que a arquitetura ainda deve ratificar.
6. **Clímax:** a Biblioteca mostra o novo prazo temporário verdadeiro; se o motorista quiser permanência indefinida, a única rota é **Salvar na galeria**.

Falha/limite: compartilhar não estende o prazo. Excluir manualmente exige confirmação e remove apenas o temporário privado.
