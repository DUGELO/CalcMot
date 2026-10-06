# Plano de entrega — Gravação de segurança

Data: 2026-09-11. **Plano somente; não executar implementação, ADB, instalação ou publicação nesta tarefa.**

Fonte funcional: worktree `CalcMot-epic-1`, branch `codex/epic-1-security-recording`. Documentos desta entrega: `docs/camera/`. Não implementar sobre os stubs do workspace principal nem copiar a vertical por cima de mudanças locais. Antes da primeira história, registrar diff/baseline do worktree correto sem limpar trabalho alheio.

Contratos: [auditoria](camera-current-state-audit.md), [UX/UI](camera-product-ux-spec.md), [gaps](camera-gap-matrix.md). Os nomes Sxx, N01, Gxx, Axx e AD-x referenciam esses documentos e a architecture spine existente.

## Estratégia e primeira história

**Implementar CAM-001 primeiro: apresentação e componentes profissionais da câmera, com previews da família de estados e checkpoint visual.** Isso torna o redesenho concreto desde o início: enxugar carga cognitiva, retirar termos internos e compor uma ferramenta operacional CalcMot. Não começar por adicionar mais controles à configuração atual. Não conectar controles novos enquanto o contrato de engine correspondente não existir.

Há duas trilhas que convergem: apresentação de produto (CAM-001 e telas) e confiabilidade (CAM-002–008). A primeira pode ser revisada independentemente; a segunda é pré-requisito dos controles reais. Não usar correções do motor como justificativa para deixar UX/UI para o fim. Nenhuma entrega intermediária habilita uma release pública da câmera sem os gates P0.

Todas as histórias abaixo têm uma capacidade coesa, dependências explícitas e saída revisável. São unidades de uma sessão de agente sempre que o ambiente estiver preparado. **CAM-023 e CAM-024 são campanhas/gates com trabalho humano externo e podem exceder uma sessão**; não fingir que testes de aparelhos e aprovação de documentos são um patch de código. História revisável não equivale a release independente.

Os caminhos abreviados são relativos ao worktree funcional: `SR = app/src/main/java/br/com/calcmot/securityrecording/`; `UI = app/src/main/java/br/com/calcmot/ui/`. Arquivos novos citados são locais prováveis dentro da vertical, não fatos de implementação atual. Não reorganizar todas as pastas só para seguir a seed da arquitetura.

### Reconciliação de contratos antes de conectar novos fluxos

Registrar, na revisão de CAM-001, o delta de nome/IA/copy em relação a DESIGN/EXPERIENCE de agosto. Antes de CAM-010/012, atualizar os contratos canônicos pelos workflows próprios, mantendo CAP/AD estáveis: substituir “Câmera secreta”, retirar card de configuração do Hub, alinhar pronto/autorizado, esclarecer preview e primeiro uso, preservar retenção/estado técnico. Não apagar memlogs nem reescrever fatos históricos. A proposta presente não autoriza enfraquecer AD-1–AD-14.

## Ordem das histórias

| Ordem | História / capacidade | Natureza | Depende de |
|---|---|---|---|
| 1 | CAM-001 — Família visual e componentes operacionais CalcMot | UX/UI P0/P1 | Referências locais e esta spec |
| 2 | CAM-002 — Persistência do contrato operacional e migração | Fundação P0 | — |
| 3 | CAM-003 — Bootstrap e recuperação exclusivos | Confiabilidade P0 | 002 |
| 4 | CAM-004 — Comandos/callbacks serializados e idempotentes | Confiabilidade P0 | 002,003 |
| 5 | CAM-005 — Captura A/V observável e duração verdadeira | Estado P0 | 004 |
| 6 | CAM-006 — Finalização verificável e recuperável | Preservação P0 | 002–005 |
| 7 | CAM-007 — Reserva e monitor de armazenamento | Continuidade P0 | 005,006 |
| 8 | CAM-008 — Rotação de arquivos na mesma sessão | Continuidade P0 | 004–007 |
| 9 | CAM-009 — Setup persistido e enquadramento verdadeiro | Preparação P1 | 001,002,004 |
| 10 | CAM-010 — Primeiro uso e transparência da gravação | Produto P1 / aprovação P0 | 001, delta canônico, LEGAL-01 |
| 11 | CAM-011 — Permissões contextuais e reparo | UX P1 / controle P0 | 009,010 |
| 12 | CAM-012 — Principal focada e navegação pela sessão real | UX P1 / estado P0 | 001,003–011 |
| 13 | CAM-013 — Pausa e retomada A/V com lacunas | Controle P1 | 004–008,012 |
| 14 | CAM-014 — Notificação e retorno com controle confiável | Controle P0/P1 | 004,005,012,013 |
| 15 | CAM-015 — Histórico reativo por sessão | Biblioteca P1 | 001–003,006 |
| 16 | CAM-016 — Detalhe e reprodução com lifecycle | Revisão P1 / integridade P0 | 001,006,015 |
| 17 | CAM-017 — Retenção lógica e limpeza privada | Dados P0 | 002,003,006 |
| 18 | CAM-018 — Armazenamento e exclusão privada controlada | Dados P0 | 007,015,017 |
| 19 | CAM-019 — Cópia independente na galeria | Preservação P1 | 002,006,016–018 |
| 20 | CAM-020 — Compartilhamento transitório pelo Android | Distribuição P1 | 017,019 |
| 21 | CAM-021 — Ajustes e extensão de prazo | Preferências P1 | 001,009,011,017 |
| 22 | CAM-022 — Homologação UX/UI integrada e acessibilidade | UX gate P0/P1 | 012–021 |
| 23 | CAM-023 — Qualificação em aparelhos e coexistência Uber/99 | QA gate P0 | 022, DEVICE-01 |
| 24 | CAM-024 — Contrato de dados e prontidão de release | Release gate P0 | 010,019,020,023, LEGAL-01 |

IDs numéricos abreviados na coluna representam CAM correspondente. A ordem é topológica; não executar uma dependência futura silenciosamente. CAM-015/017 podem avançar depois de suas dependências sem esperar toda a trilha de pausa/notification, se houver capacidade disponível e autorização do workflow de implementação.

## Histórias executáveis

### CAM-001 — Família visual e componentes operacionais CalcMot

- **Capacidade:** apresentar a câmera como uma ferramenta operacional de produto, com composição própria e carga cognitiva reduzida, antes do wiring do engine.
- **Dependências:** leitura das imagens canônicas, tokens atuais e seção de direção visual da UX spec. Registrar o delta dos companions UX; design/produto revisa composição antes do wiring em CAM-012.
- **Arquivos prováveis:** `SR/ui/SecurityRecordingPreviews.kt`, novos `SR/ui/components/RecordingStateHeader.kt`, `RecordingActionBar.kt`, `RecordingSessionRow.kt`; extensão opt-in em `UI/design/components/CalcMotComponents.kt` somente se necessária para cores/acessibilidade; não alterar defaults dos consumidores.
- **Aceitação UX:** previews de Pronto/Gravando/Pausado/Iniciando/Finalizando/Falha usam o mesmo container; principal pronta ≤4 grupos, ativa ≤3; uma CTA preenchida; zero cards de saúde por recurso; textos do usuário sem Hub/runtime/serviço/segmento/promovida. Compor também Enquadramento, Histórico e Detalhe conforme S05/S07/S08 para revisão da família. Não usar reconstruções que diferem dos componentes de apresentação reais.
- **Aceitação técnica:** composables puros recebem modelo/slots/callbacks sem câmera, DAO, permissões ou serviço; zero comando de gravação emitido por preview; aliases usam tokens existentes; Danger/TextInverse e azul contrastante aprovados por cálculo; biblioteca nativa preserva foco/toque/semântica.
- **Não objetivos:** iniciar/parar mídia real, mudar banco, pipeline, comportamento de rotas ou publicar telas com controles fictícios. Não criar assets gerados para preencher vazios.
- **Arestas:** 320dp de largura, altura compacta, fonte2,0, labels longos e contraste de estados pressionados. Não truncar causa/CTA para passar no preview.
- **Evidência de conclusão:** captura/render de previews dos componentes reais e comparação humana com identidade CalcMot; checklist de defeitos observáveis da spec sem mural de cards/controles avulsos. Device só com autorização explícita separada.

### CAM-002 — Persistência do contrato operacional e migração

- **Capacidade:** armazenar estado, identidade do dono e operações de sessão sem conflitar com fatos de verificação.
- **Dependências:** AD-2/7/8/9/12; nenhuma mudança no ledger.
- **Arquivos prováveis:** `SR/data/RecordingDatabase.kt`, `SR/domain/RecordingModels.kt`, `RecordingCommands.kt`, `RecordingLibrary.kt`; schemas e testes de migração da câmera.
- **Aceitação UX:** valores desconhecidos não viram zero; sessão interrompida pode conservar trechos válidos e motivo; privado e galeria podem coexistir sem duplicar sessão.
- **Aceitação técnica:** persistir processEpoch/owner, configuração efetiva, duração monotônica confirmada, completionReason, finalizedAt, snapshot de retenção, expiresAt e eixos ortogonais; adicionar claim operacional único por sessão com operationId/kind/baseRevision/processEpoch/status; preservar os atuais fatos name/value em estrutura distinta; mutações usam CAS e revisão única. Exportar schema e migrar banco v1 com dados válidos/falhos sem fallback destrutivo. Não aplicar expiração retroativa a dados de teste existentes sem regra de migração explicitamente registrada.
- **Não objetivos:** implementar captura, exportação, cleanup ou refatorar bancos de finanças.
- **Arestas:** migração interrompida, sessão v1 ativa, finalizedAt ausente, segmento órfão, coluna desconhecida; migração repetida não duplica dados. Regra para registros v1 sem prazo deve ficar definida no teste de migração antes do merge.
- **Evidência:** testes com banco v1 real de fixture sintética e reinício; invariantes de claim único e revisão; nenhum import proibido.

### CAM-003 — Bootstrap e recuperação exclusivos

- **Capacidade:** reconciliar gravações do processo anterior antes de admitir captura/limpeza/operações novas.
- **Dependências:** CAM-002; AD-3/7.
- **Arquivos prováveis:** `SR/data/RecordingLibraryRepository.kt`, novo `SR/application/SecurityRecordingBootstrap.kt`; seam mínimo em `CalcMotApplication.kt`; DAO de recuperação.
- **Aceitação UX:** durante recovery, “Verificando gravações anteriores…” sem REC/Start; mostrar resultado disponível/parcial/pendente/sem trecho conforme fato. Nunca reiniciar mídia após reboot/kill.
- **Aceitação técnica:** bootstrap uma vez por processEpoch, gate aguardado por Start/work/claims; excluir escritor vivo por identidade durável, não fotografia anterior do singleton; reconciliação reserva/verifica/commita com CAS. Arquivo legível cujo commit falhou permanece rastreável e elegível a próxima reconciliação.
- **Não objetivos:** iniciar CameraX/FGS por Application ou recovery; adicionar receiver de boot; editar bootstrap de finanças do workspace principal para se adequar ao worktree.
- **Arestas:** forçar recovery ler IDLE, Start tentar inserir e SELECT seguinte; Start deve aguardar. Duas chamadas de recovery, erro DB transitório, pending sem metadata, destroy do serviço com processo vivo.
- **Evidência:** testes de interleaving determinísticos e segunda execução idempotente; zero aquisição de câmera/mic na inicialização.

### CAM-004 — Comandos e callbacks serializados e idempotentes

- **Capacidade:** executar cada intenção na sessão correta uma vez, com resposta de aceitação/rejeição observável.
- **Dependências:** CAM-002/003; AD-2.
- **Arquivos prováveis:** `SR/domain/RecordingCommands.kt`, `SR/application/RecordingRuntimeStore.kt`, novo gateway/controller; `SR/platform/RecordingSessionService.kt`.
- **Aceitação UX:** toque repetido não gera duas sessões; Parar durante preparação recebe resultado seguro; comando rejeitado mostra estado atualizado e ação válida, sem silêncio.
- **Aceitação técnica:** fila única cobre comandos, callbacks e commits; comparar sessionId, revisão e commandId; dedupe inclui efeitos do serviço, não só retorno Accepted; um envelope repetido não reinsere sessão nem readquire CameraX. Expor outcome e snapshot read-only; owner publica desconexão/terminal quando observado.
- **Não objetivos:** pausa, rotação ou auto-retry de Start; remover guardas de Activity/token.
- **Arestas:** CAS de confirmação conclui antes de gate em memória e Stop chega no intervalo; Stop velho de sessão anterior com revisão coincidente; duplicata Accepted; stop sem sessão; binder perdido.
- **Evidência:** testes contam efeitos reais do adapter fake e controlam ordem DAO/Main; regressão de autorização explícita preservada.

### CAM-005 — Captura A/V observável e duração verdadeira

- **Capacidade:** afirmar Gravando somente enquanto A/V está confirmado e expor tempo efetivamente capturado.
- **Dependências:** CAM-004.
- **Arquivos prováveis:** `SR/platform/RecordingSessionService.kt`, adapter de eventos/recursos, `SR/domain/RecordingModels.kt`, snapshot/runtime.
- **Aceitação UX:** sem REC em início/retomada/falha; perda observada de áudio/câmera remove afirmação completa e informa causa; timer não conta preparação/lacunas. Não chamar permissão de microfone pronto.
- **Aceitação técnica:** observar AudioStats e erros em todos os Status, inclusive após primeira confirmação; enum fechado distingue permission/camera/microphone/foreground/thermal/storage; duração vem de estatística monotônica confirmada; API29+ severe ou maior solicita parada controlada; APIs anteriores usam falhas observadas e qualificação, sem sensor fictício.
- **Não objetivos:** modo só vídeo/só áudio, captar telefone, inserir telemetria de sessão ou mudar qualidade silenciosamente.
- **Arestas:** Start observado antes de áudio ativo, áudio silenciado depois de 2 minutos, encoder error, câmera preemptada, toggle de privacidade, thermal sem suporte.
- **Evidência:** sequências de eventos sintéticos com REC/timer esperados; teste físico de chamadas/toggles reservado a CAM-023.

### CAM-006 — Finalização verificável e recuperável

- **Capacidade:** entregar um resultado verdadeiro e preservar artefatos válidos mesmo se fechamento ou persistência falhar.
- **Dependências:** CAM-002–005.
- **Arquivos prováveis:** `SR/platform/RecordingSessionService.kt`, `VerifiedSegmentPromoter.kt`, `SR/data/RecordingLibraryRepository.kt`, DAO/claims; verifier atrás de port.
- **Aceitação UX:** Finalizando não é sucesso; cancelamento antes de capturar não é “perda de gravação”; trecho parcial tem motivo; DB indisponível diz resultado pendente. Resultado expõe sessionId específico.
- **Aceitação técnica:** intenção/claim precede I/O; classificar Finalize por código e verificar artefato elegível antes de descartar; container/tracks/duração/amostras legíveis e comparação do hash persistido nos acessos de integridade; sync/rename/commit recuperáveis. Limite de espera por Finalize produz estado pendente reconciliável, nunca promoção forçada; definir esse limite no teste do adapter antes do merge, preservando handles em uso.
- **Não objetivos:** reparar MP4 arbitrário, garantir autenticidade jurídica ou apagar quarentena sem protocolo.
- **Arestas:** Finalize com erro mas trecho legível; arquivo zero; track de áudio sem amostra; hash alterado; falha DB após rename; callback nunca chega; duas callbacks terminais; commit transitório de recovery falha.
- **Evidência:** fixtures de mídia sintéticas válidas/corrompidas e fault injection em cada fronteira arquivo/DB; segunda recuperação converge sem perder arquivo válido.

### CAM-007 — Reserva e monitor de armazenamento

- **Capacidade:** admitir captura somente com reserva e encerrar antes que falta de espaço inviabilize finalização.
- **Dependências:** CAM-005/006; política AD-6.
- **Arquivos prováveis:** `SR/domain/RecordingModels.kt` (StoragePolicy), adapter StatFs/estimativa, serviço e testes de política.
- **Aceitação UX:** antes do início, falta de espaço oferece Gerenciar espaço; durante captura, resultado informa encerramento por espaço com trechos verificados. Nenhum contador preciso de minutos baseado em constante não medida.
- **Aceitação técnica:** aplicar fórmula `finalizedSessionBytes + 2×estimatedNextSegmentBytes + 512MiB`; perfil de SD medido, recalibração por bitrate; monitor de bytes/espaço no writer; quando reserva para sessão atual +512MiB deixa de existir, pedir término uma vez. `StatFs` fora da composição e decisão final revalidada no writer.
- **Não objetivos:** limpeza silenciosa, apagar galeria ou alterar reservas da spine sem benchmark aprovado.
- **Arestas:** disco cresce por outro app, erro StatFs, Long/overflow, margem exatamente no limite, estimativa ausente, stop em curso.
- **Evidência:** testes de fronteira e queda de espaço simulada; perfil e cadência escolhida registrados para benchmark CAM-023, sem declarar suporte antes dele.

### CAM-008 — Rotação de arquivos na mesma sessão

- **Capacidade:** continuar sessão longa em segmentos válidos, preservando os anteriores.
- **Dependências:** CAM-004–007.
- **Arquivos prováveis:** `SR/platform/RecordingSessionService.kt`, controller, models/DAO de segmentos e gaps.
- **Aceitação UX:** limite de divisão não encerra sessão; troca diz “Trocando arquivo…”, sem REC/tempo avançando; só Parar disponível; histórico permanece um item e detalhe mostra intervalo.
- **Aceitação técnica:** rotacionar por duração ou1GiB; finalizar/verificar segmento e checar reserva antes do próximo; novo Start confirmado volta a Gravando; ordinal único e gap monotônico persistidos; falha no próximo arquivo mantém segmentos anteriores elegíveis.
- **Não objetivos:** prometer zero lacuna, trocar lente/orientação, compor export de sessão ou salvar na galeria.
- **Arestas:** Parar durante rotação, dois limites simultâneos, permissão/câmera perdida na abertura seguinte, espaço insuficiente, crash entre arquivos.
- **Evidência:** testes com limites reduzidos por configuração de teste e sessão multi-segmento; arquivo1 não muda quando arquivo2 falha.

### CAM-009 — Setup persistido e enquadramento verdadeiro

- **Capacidade:** lembrar preferências e confirmar quadro real sem iniciar gravação.
- **Dependências:** CAM-001/002/004.
- **Arquivos prováveis:** `SR/ui/SecurityRecordingScreens.kt` separado em route/presentation; adapter de preview; repository de setup; novas rotas S05.
- **Aceitação UX:** Enquadramento tem quadro inteiro, “Prévia — não está gravando”, lente e Usar este enquadramento; nenhuma duração/qualidade/configuração avançada; permissão negada não aparece como câmera inexistente.
- **Aceitação técnica:** validar perfil CameraX SD e streaming, não só dimensão SurfaceTexture; somente uma posse de preview/capture; liberar preview ao sair; setup sobrevive recriação; congelar lente/orientação/qualidade efetiva no Start. Frontal preferida quando disponível, fallback de lente explícito; sem perfil aprovado, não iniciar.
- **Não objetivos:** ligar preview ao entrar em Hub/Ferramentas, probe de mic não explicado, câmera dupla,720p ou troca durante sessão.
- **Arestas:** provider falha, binding falha e retry funciona, lente desaparece, rotação da Activity, font2,0, usuário volta enquanto engine fica ativo.
- **Evidência:** teste de ownership/estado e preview real validado com mídia sintética/dispositivo autorizado na homologação; nenhuma afirmação de enquadramento fiel sem imagem real.

### CAM-010 — Primeiro uso e transparência da gravação

- **Capacidade:** explicar captura/dados/limites antes de pedir permissão e lembrar aviso vigente.
- **Dependências:** CAM-001; reconciliação canônica; **LEGAL-01** para texto obrigatório antes de disponibilizar experiência ao público. Pode preparar apresentação com copy marcada proposta, sem tratá-la como aprovada.
- **Arquivos prováveis:** nova S03 na vertical, repository de aviso versionado, recursos de texto; docs de privacidade em revisão própria.
- **Aceitação UX:** S03 com finalidade, privado/prazo, galeria independente, continuidade condicionada e transparência aprovada; Preparar gravação e Agora não; principal sem texto jurídico repetido; um toque explícito inicia em uso recorrente após preparação válida.
- **Aceitação técnica:** aceitar aviso não pede automaticamente todos os recursos nem Start; versão de aviso persistida; só mudança material de política reabre fluxo; leitura/aceite funcionam offline.
- **Não objetivos:** colher consentimento de passageiro por checkbox do motorista, afirmar base jurídica própria ou mudar consentimentos de telemetria.
- **Arestas:** falha ao salvar aceite, back, instalação nova, atualização de finalidade, abertura só da biblioteca.
- **Evidência:** teste de nenhum recurso adquirido ao abrir/cancelar; revisão do texto versionado por dono nomeado no gate LEGAL-01.

### CAM-011 — Permissões contextuais e reparo

- **Capacidade:** conceder/reparar o próximo recurso sem mural de permissões.
- **Dependências:** CAM-009/010.
- **Arquivos prováveis:** coordinator e S04 em `SR/ui/`; precondições no gateway/serviço; testes de fluxos.
- **Aceitação UX:** uma explicação de recurso por etapa, CTA nomeada, Agora não; câmera/mic permanentemente negados oferecem settings; retorno revalida; última autorização leva a Enquadramento/Pronto, nunca grava sozinha. Termo “autorizado” não promete disponibilidade.
- **Aceitação técnica:** request history + rationale distinguem primeira solicitação de permanente; revalidar runtime/uso único/privacidade e notificações/canal no writer; API33+ POST é regra do produto documentada; API26–32 respeita canal operacional; falha ao abrir settings tem alternativa legível. Nenhuma permissão ampla de mídia para privados.
- **Não objetivos:** remover precondição A/V, usar overlay/acessibilidade para gravar, pedir bateria irrestrita ou escrever galeria antecipadamente.
- **Arestas:** câmera sim/mic não, swipe-dismiss, canal off com POST sim, retorno sem mudança, permissão revogada entre CTA e service.
- **Evidência:** testes de coordinator com permissão parcial/permanente e rechecagem na fronteira de Start; não depender só de strings do fonte.

### CAM-012 — Principal focada e navegação pela sessão real

- **Capacidade:** usar a composição CAM-001 para preparar/controlar sessão e abrir resultado específico.
- **Dependências:** CAM-001 e CAM-003–011; contratos canônicos reconciliados.
- **Arquivos prováveis:** `SR/ui/SecurityRecordingScreens.kt`, routes de resultado/problema, `UI/CalcMotNavigation.kt`, `UI/UiTestTags.kt`; seam de discovery em Home somente para renomear destino.
- **Aceitação UX:** S01/S02/S06/S11 conforme hierarquias; remover Hub estático e cards de health da jornada principal; recorrente pronto tem uma CTA Iniciar gravação; gravação tem estado/duração/Parar; erro tem uma causa e ação. Resultado abre o mesmo sessionId. Nenhum termo interno ou diagnósticos no estado normal.
- **Aceitação técnica:** routes projetam snapshots e outcomes por lifecycle; reentrada reconhece sessão viva; nenhum segundo preview/Start concorrente; IDLE sem sessão mostra pronto/preparar, nunca espera infinita; duplicate navigation é idempotente; fases não implementadas não ganham controle falso.
- **Não objetivos:** redesenhar Home/drawer global, integrar corridas/financeiro, habilitar câmera por navegar ou apagar componentes usados por outras telas.
- **Arestas:** back durante captura/finalização, processo novo com backstack antigo, clique repetido em entrada, sessão termina enquanto navega, runtime indisponível.
- **Evidência:** teste de jornada recorrente com um pedido de Start, teste de retorno à sessão e inspeção visual dos estados reais com fontes/insets.

### CAM-013 — Pausa e retomada A/V com lacunas

- **Capacidade:** pausar e retomar ambos os streams na mesma sessão sob confirmação.
- **Dependências:** CAM-004–008/012.
- **Arquivos prováveis:** gate/controller/serviço, models/gaps, apresentação de S02; testes de transição.
- **Aceitação UX:** Pausando/Pausado/Retomando têm textos da spec; Pausado deixa explícito que não grava A/V; tempo congela; Retomar é primária e Parar permanece; REC só volta após confirmação.
- **Aceitação técnica:** comandos idempotentes na fila; mesmo Recording/FGS preservado conforme AD-3; gap registrado e excluído da duração; retomar só enquanto dono vivo e recursos válidos; falha encerra sem retry automático.
- **Não objetivos:** pausa por evento Uber/99, retomar após kill/reboot, prometer remoção dos indicadores Android ou anexar áudio enquanto vídeo pausado.
- **Arestas:** pause→Stop antes do callback, mic perdido na retomada, rotação coincide com pausa, notificação dispara comando antigo, serviço morreu pausado.
- **Evidência:** testes A/V com eventos e persistência de gap; validação física incluída em CAM-023.

### CAM-014 — Notificação e retorno com controle confiável

- **Capacidade:** controlar e reencontrar a sessão fora do app.
- **Dependências:** CAM-004/005/012/013.
- **Arquivos prováveis:** builder atual no serviço ou adapter notification da vertical; seam de intent em MainActivity/navigation somente se necessário; small icon novo; testes de PendingIntent.
- **Aceitação UX:** N01 espelha fase/duração; Gravando Pausar/Parar, Pausado Retomar/Parar, transições só ações válidas; tocar corpo abre sessão atual; terminal oferece resultado quando notificações permitidas, sem sucesso inventado.
- **Aceitação técnica:** notificação bootstrap antes da aquisição; IDs/canal estáveis; conteúdo e controles derivam do mesmo snapshot; PendingIntents explícitos/imutáveis com sessão/revisão, sem controlar sessão errada; observar perda do controle operacional e aplicar política de parada/reparo. Small icon monocromático.
- **Não objetivos:** custom RemoteViews, full-screen intent, recriar FGS de câmera por notification tap em processo morto ou garantir alerta após kill.
- **Arestas:** Stop de PREPARING chega em RECORDING, sessão anterior tem revisão coincidente, canal bloqueado em captura, Android para app sem callbacks, recentes removidos.
- **Evidência:** testes do conteúdo/identidade de comandos e retorno; inspeção de notificação real por API/OEM em CAM-023.

### CAM-015 — Histórico reativo por sessão

- **Capacidade:** encontrar resultado por data/hora e atualizá-lo sem recarregar toda mídia.
- **Dependências:** CAM-001–003/006.
- **Arquivos prováveis:** `SR/ui/RecordingLibraryScreens.kt`, repository/DAO, modelos de lista e testes UI.
- **Aceitação UX:** S07 com grupos Hoje/Ontem/data; linha neutra sem tamanho no estado normal; pendências e interrupções textuais; item com falha abre explicação; loading/vazio/filtro vazio/erro distintos; Nova gravação no vazio; voltar restaura scroll/filtro.
- **Aceitação técnica:** consulta reativa de metadados e por ID; lazy list com keys; não calcular hash de todos os arquivos na consulta; finalizar/recuperar/excluir/expirar atualiza projeção; filtro simples por período sem metadados de corrida.
- **Não objetivos:** mosaico de rostos, estatísticas financeiras, busca por passageiro/rota, filtro avançado ou media scan geral do aparelho.
- **Arestas:**1000 itens, mesma hora de início, mudança timezone, erro DB, sessão finaliza com lista aberta, expirado sem cópia pública.
- **Evidência:** teste de lista aberta recebendo nova sessão e teste de consulta sem invocar verifier de mídia da coleção.

### CAM-016 — Detalhe e reprodução com lifecycle

- **Capacidade:** revisar sessão por ID com contexto, lacunas e transporte acessível.
- **Dependências:** CAM-001/006/015.
- **Arquivos prováveis:** `SR/ui/RecordingLibraryScreens.kt` separado em detalhe/player; adapter Media3; consulta repository por ID.
- **Aceitação UX:** S08 organiza data, resultado, player/tempo, prazo e ações; remover slider permanente de volume; buffering/erro/trecho indisponível têm feedback; sem autoplay; integridade não vira selo jurídico.
- **Aceitação técnica:** usar listeners/state do Media3, não polling para toda UI; pausa em ON_STOP; liberar player no lifecycle correto; coordenação impede playback de contaminar captura; seek multi-segmento em fronteira segue RecordingPlayback; validar arquivo acessado e digest sem revarrer biblioteca. Restaurar posição sem auto-play.
- **Não objetivos:** player background, edição/transcrição, substituir controles nativos por Canvas ou ativar export ainda não implementado.
- **Arestas:** decoder falha, arquivo removido/alterado, segmento2 ausente, fim, rotação, escala de fonte, captura inicia enquanto detalhe aberto, prazo acaba em reprodução.
- **Evidência:** testes de lifecycle/coordenação e player com fixtures sintéticas; TalkBack de controles/tempo/seek validado em CAM-022.

### CAM-017 — Retenção lógica e limpeza privada

- **Capacidade:** cumprir o prazo do privado sem afetar galeria e sem excluir mídia em operação.
- **Dependências:** CAM-002/003/006; AD-9.
- **Arquivos prováveis:** retention policy/repository/DAO; worker exclusivo da vertical; scheduler de bootstrap/finalização/biblioteca; apresentação de disponibilidade.
- **Aceitação UX:** prazo padrão24h desde finalizar; mostra instante final verdadeiro; privado expirado não abre player/export; eventual cópia pública permanece visível separadamente; limpeza pendente não sugere disponibilidade restaurada.
- **Aceitação técnica:** snapshot24h/3/7/15/30d; epochUTC; `expiresAt <= now` torna EXPIRED e bloqueia novos claims; limpeza após bootstrap e sem captura/claim conflitante; operações já admitidas seguem regra AD-9; limpeza retomável de originals/work/quarentena indexada; worker próprio não usa LedgerMaintenanceWorker.
- **Não objetivos:** apagar galeria, garantir exclusão física no exato segundo, estender por compartilhar ou mudar prazo de sessões antigas por alterar default.
- **Arestas:** app fechado no vencimento, clock/timezone, crash entre expiração/unlink, claim pré-expiração, entrada no player aberta, banco v1 migrado com regra definida em CAM-002.
- **Evidência:** testes com relógio injetado no limite exato e corrida expire/export/delete; reinício converge sem reativar privado.

### CAM-018 — Armazenamento e exclusão privada controlada

- **Capacidade:** mostrar consumo real e excluir privados elegíveis com confirmação e resultado por item.
- **Dependências:** CAM-007/015/017.
- **Arquivos prováveis:** nova S10, dialog S13, repository/DAO de delete pending/tombstone, storage monitor.
- **Aceitação UX:** privados usados/espaço livre medidos; lista por tamanho/data e seleção; ativos/bloqueados não selecionáveis com motivo; dialog informa que galeria permanece; resultado parcial nomeia quantidade pendente; não prometer Undo.
- **Aceitação técnica:** reservar operação por sessão, registrar DELETE_PENDING, unlink e tombstone duráveis; excluir não disputa captura/export/recovery; contabilizar bytes físicos observados após operação; limpar grants/staging respeitando AD-10; sem recursive delete de caminho montado pela UI.
- **Não objetivos:** apagar galeria, limpeza silenciosa, liberar espaço calculado a partir de bytes que ainda estão sob concessão ou alterar políticas de todo app.
- **Arestas:** expira enquanto seleciona, um de cinco deletes falha, processo morre após unlink, outro app consome espaço, ativos mudam de fase, falha StatFs.
- **Evidência:** testes de fault injection e recuperação do delete; confirmação/resultado parcial em UI integrada.

### CAM-019 — Cópia independente na galeria

- **Capacidade:** publicar uma representação reproduzível da sessão e confirmar só após verificação.
- **Dependências:** CAM-002/006/016–018; pins aprovados e AD-10.
- **Arquivos prováveis:** novo `SR/platform/media/SessionMediaAssembler`, adapter MediaStore, repository/claims; S15 e ação S08; Gradle/Manifest somente para necessidade desta capacidade.
- **Aceitação UX:** Salvar na galeria mostra montagem/cópia; sucesso persistente “Cópia na galeria” com Abrir na galeria; prazo do privado não muda; falha preserva original; operação em progresso não tem porcentagem inventada.
- **Aceitação técnica:** MP4 canônico único para vários segmentos, ordem correta e lacunas descritas; single-flight e idempotência por artifactId; API29+ pending→copy/verify→publish→CAS; API24–28 pede WRITE_EXTERNAL_STORAGE maxSdk28 só ao salvar, staging oculto→sync/verify/rename→MediaScanner→URI verificada. Recovery reconcilia publicação interrompida; URI ausente vira MISSING.
- **Não objetivos:** backup, upload, autenticar prova, apagar original automaticamente ou mudar qualidades de captura.
- **Arestas:** expira depois do claim, disco enche durante montagem, crash após publish antes do commit, usuário exclui galeria, repetir Salvar, permissão legado negada.
- **Evidência:** testes do protocolo por API e export sintético multi-segmento reproduzível; sem duplicata após replay/recovery.

### CAM-020 — Compartilhamento transitório pelo Android

- **Capacidade:** entregar export elegível por grant temporário sem preservar implicitamente.
- **Dependências:** CAM-017/019.
- **Arquivos prováveis:** adapter FileProvider/share e lifecycle de staging; XML de paths restritos/Manifest; S14/S15; repository/cleanup.
- **Aceitação UX:** transparência “Compartilhar não salva esta gravação”; Sharesheet padrão; cancelar não muda retenção/galeria; jamais “Enviado” só pelo intent; sem app compatível tem mensagem acionável.
- **Aceitação técnica:** provider não exportado `${applicationId}.securityrecording.files` só `share/`; grant somente leitura de artefato verificado; montagem reutilizável/idempotente e lançamento repetível por intenção explícita. Expiração bloqueia novas operações; limpeza física em `max(expiresAt,lastShareLaunchedAt+1h)` e revogação de grants sem reabrir privado. Source da galeria só se leitura/reexport desse caminho testada; caso contrário oferecer Abrir na galeria.
- **Não objetivos:** expor sessions/work, integrar SDK social, upload, conservar por share ou provar recebimento.
- **Arestas:** receptor rejeita formato, cancelamento, origem expira, artefato em uso, processo morre após lançar, URI pública ausente.
- **Evidência:** testes de paths/grants/lifetime e dois lançamentos sem duas montagens; mídia sintética em teste de Sharesheet autorizado.

### CAM-021 — Ajustes e extensão de prazo

- **Capacidade:** alterar opções de sessões futuras e elevar prazo total elegível por disclosure progressivo.
- **Dependências:** CAM-001/009/011/017.
- **Arquivos prováveis:** S09/S12, setup/retention repository, recursos de texto; navigation seams da vertical.
- **Aceitação UX:** linhas e grupos da S09; qualidade480p informativa, sem card/720p; divisão só nos Ajustes; valor confirmado após persistência; extensão mostra data final e “desde o encerramento”, só níveis maiores, até30d.
- **Aceitação técnica:** defaults duráveis valem só para próximos Start; sessão ativa preserva snapshot, opções de captura ficam read-only; extensão usa CAS e bloqueia expirado; Enquadramento/Permissões/Armazenamento são destinos próprios.
- **Não objetivos:** alterar sessão ativa, resetar prazo em cada extensão, esconder indicadores, diagnóstico avançado ou modo automático por corrida.
- **Arestas:** prefs falham, lente ausente, prazo vira durante sheet, sessão já30d, duas telas tentam elevar, fonte2,0 nos valores longos.
- **Evidência:** testes de prazo total, não retroatividade e UI sem escolhas de configuração na principal.

### CAM-022 — Homologação UX/UI integrada e acessibilidade

- **Capacidade:** fechar a qualidade da família de telas reais e da redução de carga cognitiva.
- **Dependências:** CAM-012–021; checkpoint CAM-001 revisado. Qualquer teste/dispositivo exige autorização explícita do usuário conforme política local.
- **Arquivos prováveis:** `SR/ui/` somente ajustes da câmera; previews reais; `SecurityRecordingUiTest.kt`, navigation tests; referências aprovadas em `docs/design/references/camera/`; screenshots sintéticos temporários em `.tmp/screens/`.
- **Aceitação UX:** principal respeita orçamento4/3 grupos e CTA dominante; zero termos internos listados na spec; nenhum mural de cards ou player avulso; estados da família mantêm container/ritmo; contraste, touch48dp, fonte1,0/1,3/2,0 e tamanhos320×568/360×800/393×852/852×393 aprovados. Incluir erro, vazio, loading, parcial e prazo próximo, não só tela pronta.
- **Aceitação técnica:** previews usam os composables reais com fixtures; testes de navegação/estado/semântica, não só captura não vazia. TalkBack cobre iniciar/parar/reparar/reproduzir/excluir sem timer falante; player/slider associados semanticamente; insets consumidos uma vez.
- **Não objetivos:** mudar identidade global, pipelines ou capturar passageiros reais; declarar “pixel perfect” sem referência específica e screenshot comparável.
- **Arestas:** topbar longa, texto traduzido/fonte grande, sistema3 botões/gestos, teclado quando houver, landscape, longos horários/prazos, screenshot antigo confundido com referência atual.
- **Evidência:** comparação visual anotada por tela e registro do checklist; teste moderado com ≥5 motoristas, compreensão dos estados até5s e localização de sessão até30s; confusão sobre estar gravando exige revisão antes do aceite. Não afirmar superioridade competitiva sem comparação equivalente.

### CAM-023 — Qualificação em aparelhos e coexistência Uber/99

- **Capacidade:** demonstrar que sessão longa, falhas e consumo sustentam uso profissional na matriz-alvo.
- **Dependências:** CAM-022; **DEVICE-01**, matriz de modelos/OEM/APIs aprovada por produto/QA. Campanha pode ocupar mais de uma sessão; autorização de device é própria.
- **Arquivos prováveis:** testes de integração da câmera; documentos de benchmark/QA; `.tmp/screens/` para evidência sintética local. Corrigir falhas somente na vertical, reabrindo história correspondente.
- **Aceitação UX:** compreender estado/resultado/causa sem termos técnicos; controlar via notificação; sem distração por promos/popups; suporte e limites comunicados conforme aparelhos realmente aprovados.
- **Aceitação técnica:** matriz proposta inclui APIs24/28/29/32/33/34/36, pelo menos aparelho de entrada e intermediário, OEMs relevantes; produto nomeia modelos concretos, não só API. Executar ≥2h contínuas480p com rotações e apps de motorista, tela apagada/recentes, pausa, chamadas, low-memory, thermal, disco, kill/force-stop/reboot, recuperação e export. Medir A/V sync, lacunas, crash/ANR, bytes/h, CPU/bateria e temperatura frente ao mesmo cenário sem câmera; registrar versão, modelo, duração e condições. Antes do aceite, engenharia/produto fixa limites numéricos de consumo/A-V sync por matriz e reprova os excedidos; sem limite ou sem medida, gate permanece aberto. Testes de falhas não podem apresentar REC falso.
- **Não objetivos:** instalar APK sem autorização, capturar corridas/passageiros reais, alegar suporte universal, mudar Uber/99/OCR/overlay para acomodar câmera ou habilitar720p.
- **Arestas:** carregamento com calor, navegação/mapa simultâneos, otimização OEM, armazenamento externo/legado, processo morto sem callback, app removido dos recentes distinto de force-stop.
- **Evidência:** relatório por cenário com resultado observável e bugs; regressão do baseline Uber/99 sem alteração de lógica. Build verde não substitui a campanha.

### CAM-024 — Contrato de dados e prontidão de release

- **Capacidade:** tornar declarações de produto/privacidade e pacote Android coerentes com a câmera entregue, sem publicar.
- **Dependências:** CAM-010/019/020/023; LEGAL-01. Revisões externas podem exceder uma sessão.
- **Arquivos prováveis:** `docs/privacy-policy.md`, `docs/play-store-submission.md`, docs da câmera; Manifest mesclado e backup/data extraction rules; testes `ReleaseReadinessTest.kt`; código de diagnóstico da vertical e seam de `CalcMotApplication.kt` quando necessário.
- **Aceitação UX:** finalidade, áudio/vídeo, onde fica mídia, prazo, exclusão, cópia na galeria, compartilhamento e limites de continuidade explicados em linguagem humana; texto ao passageiro/aplicação do processo aprovados pelo responsável jurídico/produto; nenhum material usa ocultação como valor.
- **Aceitação técnica:** comprovar exclusões do banco/WAL/SHM/subtree em cloud backup e device transfer; logs de recovery usam código sanitizado, sem Throwable de I/O que revele path/metadados; sem envio de sessão ao Firebase. Confrontar docs com manifesto mesclado/SDKs/fluxos reais e declaração FGS/Data Safety; não copiar afirmações antigas sobre telemetria. Verificar serviço/provider não exportados e paths restritos; não ampliar acessibilidade/mediaProjection/boot. Rodar testes relevantes, readiness e convergência de classpath. Builds de entrega assembleRelease e bundleRelease separados, com JAVA_HOME do JBR; não alterar versão ou assinatura.
- **Não objetivos:** publicar Play, distribuir APK, ler/imprimir material de assinatura, redefinir finalidade aprovada de AccessibilityService ou criar baseline para esconder lint existente.
- **Arestas:** docs do workspace principal divergem do worktree funcional, sidecars, logs de falha, galeria sobrevive ao app, receiver/permission transitivos, diferenças de splits entre assemble e bundle.
- **Evidência:** checklist de release com owners, aprovações e relatório técnico; lint conhecido registrado como não verde se persistir, sem chamar aprovado. Entregar pacote revisável; publicação só com autorização explícita posterior.

## Gates externos e bloqueadores P0

| Gate | Responsável necessário | Evidência para fechar |
|---|---|---|
| LEGAL-01 | Produto + jurídico nomeados pelo projeto | Texto/processo de transparência a passageiros, finalidade/base e documentos aprovados para a versão de câmera |
| DEVICE-01 | Produto + QA | Lista concreta de modelos/OEM/APIs, condições e limites de aceitação de A/V/consumo aprovada antes de executar CAM-023; o relatório resultante fecha ENGINE-01/RELEASE-01 |
| DESIGN-01 | Produto/design | Família visual CAM-001 e versão integrada CAM-022 revisadas; sem overload ou termos internos |
| ENGINE-01 | Engenharia + QA | CAM-002–008/014/016–018: sem REC falso, corrida de recovery, Stop incorreto, arquivo válido perdido ou exclusão concorrente |
| RELEASE-01 | Responsável de entrega | CAM-024, declarações/manifest/backup/telemetria coerentes e todos P0 fechados; nenhuma publicação automática |

P0 atuais de release: verdade de A/V; serialização de comandos/recovery e identidade de sessão; Stop seguro; finalização/integridade recuperável; rotação verdadeira; reserva/monitor de espaço e térmica; retenção/exclusão; contraste crítico; dados/backup/transparência; qualificação real na matriz. Flags e códigos de erro não fecham nenhum desses gates sem comportamento demonstrado.

## Código que não deve ser tocado agora

Nesta tarefa de auditoria, **nenhum código**. Nas primeiras histórias, limitar alterações à câmera e seams citados em cada capacidade.

- `app/src/main/java/br/com/calcmot/accessibility/`, especialmente `UberAccessibilityService.kt`; `processor/`; `ninetynine/`; `overlay/`; cálculos em `model/`; `finance/` e ledger. Gravação não importa esses pacotes nem observa ofertas/corridas.
- Telemetria de ofertas e consentimentos de pesquisa; não enviar duração, horário, tamanho, URI, filename, hash ou estado de gravação ao Firebase para “medir UX”. Medidas de QA são locais e sanitizadas.
- Manifest, permissões, backup e bootstrap globais antes da história que os justifica; não remover `FOREGROUND_SERVICE` do worktree funcional com base no teste antigo do workspace principal.
- Defaults de cores, raios e botões de todo o DS; melhorias de contraste da câmera devem ser opt-in até revisão dos consumidores. Não reescrever HomeReadyScreen ou onboarding de acessibilidade para combinar com câmera.
- VersionName/versionCode, keystore/propriedades/certificados, pipelines de entrega e arquivos locais de outros worktrees.
- As salvaguardas corretas do motor: Activity explícita/token, serviço não exportado, START_NOT_STICKY, notificação antes da aquisição, verificação antes de disponibilidade e isolamento Uber/99. Corrigir falhas sem enfraquecer essas invariantes.

## Verificação desta entrega documental

Esta entrega cria os quatro documentos solicitados e mantém fontes do aplicativo intactos. Verificação documental deve confirmar links locais, presença dos campos de histórias, sequência CAM única/topológica e cobertura das sete fases. Nenhum build/teste Android ou validação visual em device foi executado aqui. Os checkpoints de UI real, pesquisa com motoristas, benchmark e aprovações externas são trabalho futuro explicitamente nomeado, não resultados presumidos.
