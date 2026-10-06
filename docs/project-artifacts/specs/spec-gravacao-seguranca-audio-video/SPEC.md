---
id: SPEC-gravacao-seguranca-audio-video
companions:
  - recording-lifecycle.md
  - technical-constraints.md
  - ../../planning-artifacts/architecture/architecture-CalcMot-2026-08-24/ARCHITECTURE-SPINE.md
  - ../../planning-artifacts/ux-designs/ux-CalcMot-2026-08-24/DESIGN.md
  - ../../planning-artifacts/ux-designs/ux-CalcMot-2026-08-24/EXPERIENCE.md
sources:
  - ../../planning-artifacts/research/competitive-camera-de-seguranca-para-motoristas-zeca-2026-08-21/research.md
---

# SPEC — Gravação de segurança em áudio e vídeo

Este arquivo é o contrato canônico da funcionalidade. Os companions listados no frontmatter fazem parte integral da SPEC. Em caso de conflito técnico, `ARCHITECTURE-SPINE.md`, `recording-lifecycle.md` e `technical-constraints.md` prevalecem sobre dúvidas técnicas antigas registradas nos companions de UX; `DESIGN.md` e `EXPERIENCE.md` permanecem autoridades de experiência, conteúdo e apresentação.

## Por quê

Motoristas precisam registrar, de forma explícita, previsível e local, áudio e vídeo de situações de segurança durante o trabalho. A solução deve permitir capturar, revisar, salvar na galeria e compartilhar evidências sem interferir nos fluxos de produção da Uber, da 99, do OCR, da acessibilidade, do overlay ou dos cálculos do CalcMot.

## Capacidades

### CAP-1 — Configurar a gravação

**Intenção:** preparar uma sessão de gravação antes de iniciá-la.

**Resultado observável:**

- A câmera frontal é a seleção padrão; o motorista pode escolher outra lente disponível antes de iniciar.
- A prévia permite confirmar enquadramento e disponibilidade de câmera e microfone.
- Lente e orientação ficam fixas após o início da sessão.
- A entrega inicial oferece somente 480p. A opção 720p permanece indisponível até aprovação por benchmark da matriz-alvo.
- O motorista escolhe segmentos de 5, 10, 20, 30 ou 60 minutos; o padrão é 10 minutos.
- Câmera, microfone e, quando exigida pela plataforma, permissão de notificações são pré-condições explícitas para iniciar.

### CAP-2 — Controlar uma sessão

**Intenção:** iniciar, pausar, retomar e encerrar a captura com estado verdadeiro e compreensível.

**Resultado observável:**

- A sessão captura áudio e vídeo sincronizados.
- Pausar interrompe tanto áudio quanto vídeo; retomar reinicia ambos.
- O indicador `REC` só aparece quando a captura foi confirmada como ativa.
- Durante `Rotacionando`, nenhum `REC` é exibido e somente Encerrar permanece acionável.
- Iniciar, pausar, retomar e encerrar só são apresentados como concluídos após confirmação do subsistema de captura.
- Lacunas causadas por pausa, rotação, falha ou recuperação são registradas e informadas ao motorista.

### CAP-3 — Manter continuidade em segundo plano

**Intenção:** preservar uma gravação ativa quando o motorista alterna de aplicativo ou remove o CalcMot dos recentes.

**Resultado observável:**

- A gravação ativa continua em foreground service com notificação persistente e controles coerentes com o estado atual.
- Remover o app dos recentes não encerra uma sessão já ativa.
- Encerrar pelo app ou pela notificação finaliza a sessão de modo determinístico.
- O sistema não tenta reiniciar gravação após encerramento do processo, force-stop ou reinicialização do aparelho.

### CAP-4 — Consultar e reproduzir gravações

**Intenção:** localizar e revisar uma sessão gravada como uma única evidência lógica.

**Resultado observável:**

- A biblioteca apresenta cada sessão como um agregado, mesmo quando composta por múltiplos segmentos ordenados e imutáveis.
- A disponibilidade temporária e a cópia publicada na galeria podem coexistir sem duplicar a sessão na biblioteca.
- Sessões e segmentos válidos podem ser reproduzidos com áudio.
- Estado, duração capturada, lacunas, expiração temporária e disponibilidade na galeria são derivados do estado persistido, nunca inferidos apenas da interface.

### CAP-5 — Aplicar retenção temporária

**Intenção:** manter gravações privadas por um prazo previsível e permitir extensão controlada.

**Resultado observável:**

- Os níveis de retenção são definidos pelo produto e limitados a 30 dias.
- A política de retenção é registrada como snapshot no início da sessão.
- Uma extensão só pode elevar o prazo total e é recalculada a partir de `finalizedAt`, respeitando o limite de 30 dias.
- A expiração lógica ocorre antes da exclusão física e torna a cópia temporária indisponível para reprodução, exportação ou compartilhamento.
- Expirar ou excluir a cópia temporária não remove uma cópia já publicada na galeria.

### CAP-6 — Salvar uma cópia na galeria

**Intenção:** publicar uma cópia independente e legível fora do armazenamento privado do CalcMot.

**Resultado observável:**

- Salvar na galeria cria uma cópia pública independente, reproduzível por aplicativos compatíveis.
- A publicação é idempotente: repetir a ação não cria cópias extras quando uma publicação válida já existe.
- Falhas de publicação não corrompem nem removem a gravação temporária.
- A exclusão ou expiração da gravação temporária não afeta a cópia publicada.

### CAP-7 — Compartilhar uma gravação

**Intenção:** enviar uma sessão por meio do Sharesheet do Android sem publicá-la implicitamente.

**Resultado observável:**

- Compartilhar entrega uma representação reproduzível da sessão a aplicativos escolhidos pelo motorista.
- Cada acionamento explícito pode abrir novamente o Sharesheet; a montagem do artefato de exportação é idempotente.
- Compartilhar não salva na galeria, não estende retenção e não altera a disponibilidade lógica da gravação.
- Artefatos transitórios de compartilhamento têm acesso restrito e limpeza controlada.

### CAP-8 — Recuperar com verdade após falhas

**Intenção:** reconciliar metadados e arquivos depois de encerramentos inesperados, sem inventar sucesso.

**Resultado observável:**

- A recuperação só promove segmentos cuja existência, legibilidade e integridade mínima tenham sido verificadas.
- Operações concorrentes de finalizar, publicar, exportar, expirar e recuperar são protegidas por revisão e claims persistidos.
- Arquivos órfãos, operações interrompidas e estados pendentes são reconciliados de modo idempotente.
- O motorista vê falha, lacuna ou indisponibilidade quando a evidência não pode ser confirmada.

## Restrições

- O fluxo começa por uma ação explícita e visível em Activity própria, com aviso inicial e controles definidos pelos companions de UX.
- Uber, 99, AccessibilityService, OCR, overlay e cálculos nunca iniciam, controlam ou observam internamente uma sessão de gravação.
- Áudio e vídeo são obrigatórios; a indisponibilidade de câmera, microfone ou permissão necessária impede o início.
- A continuidade usa foreground service dos tipos `camera|microphone` e notificação persistente.
- Remoção dos recentes preserva uma sessão ativa; encerramento do processo, force-stop e reboot não disparam reinício automático.
- O lifecycle, inclusive `Rotacionando`, e os eixos ortogonais de disponibilidade, galeria e exportação são definidos em `recording-lifecycle.md`.
- Cada sessão é um agregado de segmentos; limites, rotação, folga de armazenamento e reconciliação seguem `technical-constraints.md` e a arquitetura adotada.
- Originais temporários permanecem no armazenamento privado do app. Galeria usa publicação explícita; compartilhamento usa concessão temporária e independente.
- A implementação deve manter isolamento estrito dos fluxos Uber/99 e dos módulos de acessibilidade, parsing, OCR, overlay e finanças.
- Baseline Android: `minSdk 24`, `compileSdk/targetSdk 36`; versões aprovadas e demais detalhes de stack estão em `technical-constraints.md`.
- Indicadores de privacidade do Android, permissões e notificação devem refletir a captura real.
- Não há backup em nuvem, upload automático, streaming remoto nem sincronização entre dispositivos.
- Texto jurídico, transparência a passageiros, Data Safety e declaração de foreground service precisam de aprovação antes da publicação.
- Decisões técnicas da arquitetura adotada substituem perguntas técnicas antigas nos companions de UX; estes continuam normativos para experiência, conteúdo e apresentação.
- Todos os companions do frontmatter integram este contrato e devem ser considerados na validação e na decomposição em histórias.

## Fora de escopo

- Gravação secreta, acionamento automático por Uber/99, AccessibilityService ou qualquer evento de oferta.
- Captura de tela, conteúdo interno de aplicativos terceiros ou integração com pipelines de oferta.
- Upload automático, nuvem, transmissão ao vivo ou conta remota de evidências.
- Edição de vídeo, filtros, reconhecimento facial, transcrição ou análise de conteúdo.
- Garantia de gravação após force-stop, encerramento do processo ou reinicialização do aparelho.
- Qualidade 720p na entrega inicial.

## Sinais de sucesso

- Uma sessão de 480p inicia, pausa, retoma, rotaciona segmentos e encerra sem apresentar `REC` falso.
- A duração mostrada corresponde apenas ao tempo efetivamente capturado.
- A sessão continua ao alternar apps e ao remover o CalcMot dos recentes, com controle pela notificação.
- A biblioteca reproduz uma sessão composta por segmentos em ordem e revela lacunas existentes.
- Salvar na galeria e compartilhar funcionam de forma independente, idempotente e sem alterar retenção.
- Expiração, exclusão e recuperação convergem para estados verdadeiros mesmo após interrupções.
- Testes de regressão demonstram que os fluxos Uber e 99 permanecem funcionalmente idênticos ao baseline de produção.

## Questões em aberto

1. Qual texto e base jurídica aprovados no Brasil devem orientar o aviso ao motorista e a transparência aos passageiros?
2. Qual é a matriz exata de aparelhos, fabricantes e versões Android que constitui o gate obrigatório de liberação?
