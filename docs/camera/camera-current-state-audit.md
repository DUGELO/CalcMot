# Auditoria atual — Câmera / gravação de segurança

Data: 2026-09-11. Escopo: análise de produto, UX, Design System e riscos técnicos; nenhuma implementação.

## Baseline e limites da evidência

**A implementação funcional auditada estava no worktree `CalcMot-epic-1`, branch `codex/epic-1-security-recording`.** O workspace principal era `CalcMot`. Ambos apontavam para HEAD `1db7bf94c5952917baefe21ee4bcd4ca7afdd0a6`; à época, a implementação da câmera estava nas alterações locais, inclusive arquivos não versionados. Esse HEAD sozinho não reproduz esta auditoria histórica.

No workspace principal há somente Ferramentas e Hub com callbacks vazios. Isso NÃO descreve a câmera do worktree. A correção do usuário sobre o worktree foi incorporada antes das conclusões abaixo. Não mesclar, limpar ou reverter nenhum dos dois workspaces como parte deste planejamento.

Classificação: **P0** bloqueia uma liberação profissional da câmera; **P1** prejudica materialmente a experiência; **P2** acabamento; **P3** melhoria opcional. Um P0 da câmera não significa que o app CalcMot atualmente publicado deva ser retirado.

- **C — confirmado:** comportamento ou ausência diretamente verificável nos fontes.
- **R — risco:** sequência possível pela leitura; reprodução por teste ainda necessária.
- **V — validação pendente:** depende de aparelho, render ou decisão externa. Não declarar como defeito reproduzido.

Não foram executados ADB, instalação, captura em aparelho, coleta de mídia, build ou testes. Foram inspecionados fontes e testes existentes, documentos e referências visuais. Não foi localizado conjunto de screenshots reais da câmera nas pastas de referências/screenshots examinadas. Há Compose Previews, mas vários reconstroem versões simplificadas das telas; não comprovam o resultado real. Este documento não aprova fidelidade visual, desempenho ou suporte a fabricantes.

## Fontes locais e mapa da implementação

Os caminhos de código desta seção são relativos ao **worktree funcional**, não ao workspace de entrega.

| Ref. | Fonte | Evidência relevante |
|---|---|---|
| E01 | `app/src/main/java/br/com/calcmot/securityrecording/ui/SecurityRecordingScreens.kt:80` | Ferramentas e Hub; configuração a partir de :84; conteúdo ativo :193; helpers :240–269 |
| E02 | `app/src/main/java/br/com/calcmot/securityrecording/ui/RecordingLibraryScreens.kt:54` | Biblioteca, player route :73, cards :96, player :113, mensagens :171 |
| E03 | `app/src/main/java/br/com/calcmot/securityrecording/ui/SecurityRecordingPreviews.kt` | Onze tipos de preview, com variantes; configuração/player usam reconstruções |
| E04 | `app/src/main/java/br/com/calcmot/ui/CalcMotNavigation.kt:264` | Rotas Tools, Hub, Configure, Active, Library e Player; callbacks funcionais |
| E05 | `app/src/main/java/br/com/calcmot/securityrecording/platform/RecordingSessionService.kt` | Serviço CameraX; eventos :174; confirmação :194; auto-stop :219; promoção :255; falha :322; notificação :390 |
| E06 | `app/src/main/java/br/com/calcmot/securityrecording/domain/RecordingCommands.kt` | Gate sincronizado, START/STOP, revisão e idempotência em memória; snapshot sem duração |
| E07 | `app/src/main/java/br/com/calcmot/securityrecording/domain/RecordingModels.kt` | Seis fases; setup 480p; reserva fixa 128 MiB + 45 MiB/min |
| E08 | `app/src/main/java/br/com/calcmot/securityrecording/data/RecordingDatabase.kt` | Banco próprio; claims são constatações de integridade, não locks operacionais; schema não exportado |
| E09 | `app/src/main/java/br/com/calcmot/securityrecording/data/RecordingLibraryRepository.kt` | Lista :21; verificação :33; recovery :45; mutex local; conclusão sem CAS :116 |
| E10 | `app/src/main/java/br/com/calcmot/securityrecording/platform/VerifiedSegmentPromoter.kt` | Sync, inspeção de container/tracks/duração, SHA-256 e rename de pending |
| E11 | `app/src/main/java/br/com/calcmot/CalcMotApplication.kt:19` | Recovery assíncrono sem gate de admissão de Start |
| E12 | `app/src/main/AndroidManifest.xml`; `app/src/main/res/xml/{backup_rules,data_extraction_rules}.xml` | Permissões e serviço próprios; exclusões de mídia/banco; sem provider de share |
| E13 | `app/src/main/java/br/com/calcmot/ui/design/{tokens,theme,components}` | DS real; botões Danger com texto claro; cards padrão 10dp |
| E14 | `app/src/androidTest/java/br/com/calcmot/ui/SecurityRecordingUiTest.kt`; `CalcMotNavigationTest.kt`; `app/src/test/java/br/com/calcmot/securityrecording/`; `ReleaseReadinessTest.kt` | Cobertura de estados puros/passividade/estrutura; não comprova lifecycle real |

Documentos consultados no workspace principal: `docs/global-design-system.md`, `docs/ux-persona-joao-batista.md`, `docs/ux-ui-internal-app-review.md`, `docs/ux-ui-humanized-product-review-20260611.md`, `docs/ux-ui-visual-qa-pr10-20260611.md`, `docs/research/mercado-copilotos-financeiros-brasil-2026-09-10.md`, `docs/privacy-policy.md`, `docs/play-store-submission.md`, `docs/design/screen-implementation-pipeline.md`, README de protótipos e pacote `docs/project-artifacts/specs/spec-gravacao-seguranca-audio-video/`.

Também consultados: `ARCHITECTURE-SPINE.md` em `docs/project-artifacts/planning-artifacts/architecture/architecture-CalcMot-2026-08-24/`, `DESIGN.md` e `EXPERIENCE.md` em `docs/project-artifacts/planning-artifacts/ux-designs/ux-CalcMot-2026-08-24/`, épicos e digests competitivos de Zeca/GigU/rebU. O relatório automático bloqueado do workspace principal é histórico de uma tentativa, não demonstra ausência do motor no outro worktree.

## Leitura visual das referências existentes

Imagens abertas: `docs/design/references/calcmot-prototypes-2026/06-home-pronto-calcular.png`, `24-configuracoes.png`, `15-historico-com-ofertas.png` e `docs/screenshots/CalcMotScreenshots/home_ready.png`.

Os protótipos têm proporção aproximada 0,47:1, fundo preto azulado, texto branco forte, verde para marca/estado positivo, CTA azul amplo, cards escuros com borda fria, ícones outline e hierarquia operacional. Configurações usa linhas com divisores; histórico agrupa itens legíveis. A Home usa hero grande para seu estado central. **Não transportar o velocímetro, as métricas financeiras ou o hero de marketing para a câmera.** Transportar foco, contraste, CTA, ritmo e linguagem.

A captura antiga `home_ready.png` mostra cinza, muitos cards e uma geração anterior da UI. É evidência histórica, não contrato de cor atual. O README dos protótipos declara as imagens canônicas; tokens Kotlin determinam nomes e valores disponíveis. Há diferença real entre cards de protótipos mais arredondados e o wrapper `CalcMotCard` de 10dp. O delta da câmera deve escolher papéis existentes explicitamente; não alterar todos os cards do app para resolver uma tela.

## Inventário por tela existente

### 1. Ferramentas — `SecurityToolsRoute` (E01:80)

- **Propósito / objetivo:** descobrir uma ferramenta local de segurança. **Ação principal atual:** card “Câmera secreta”, que abre Hub.
- **Conteúdo desnecessário:** “Recursos locais para apoiar sua jornada” não ajuda a escolher. **Duplicação:** topbar “Ferramentas” e título “Ferramentas”.
- **Hierarquia / densidade:** um único destino ocupa uma tela intermediária com título e subtítulo redundantes; pouca informação útil por toque. Não é excesso de dados, é excesso de etapas.
- **Navegação:** a cadeia drawer → Ferramentas → Hub → Configurar posterga o trabalho real. Não há retorno à sessão ativa derivado do runtime.
- **Copy:** “secreta” contradiz o posicionamento legítimo solicitado; “verificados” antecipa um resultado que só existe depois da captura.
- **Visual:** usa tokens reais e ícone Material; o problema não é ausência completa de DS. Card promocional com descrição longa faz uma tarefa operacional parecer catálogo experimental.
- **Acessibilidade:** toque no card existe; ícone decorativo corretamente sem descrição. Heading/estado e restauração de foco precisam de inspeção TalkBack; imports de `heading` não constituem uso. Não alegar alvo menor que 48dp sem medição.
- **Smell Compose:** função inteira em uma linha dificulta revisão; helper local de ActionCard duplica papel de linha navegável.
- **Estados / feedback ausentes:** destino não mostra sessão ativa, preparação pendente ou indisponibilidade. **Aresta perigosa:** reentrar pelo card durante captura permite percorrer novamente configuração, sem explicitar sessão já existente.
- **Issues:** A01 P1 C (nome); A02 P1 C (etapa/títulos); A03 P0 R (entrada sem reconciliação ativa).

### 2. Hub — `SecurityRecordingHubRoute` (E01:81)

- **Propósito / objetivo:** escolher entre preparar e rever gravações. **Ação principal atual:** nenhum destaque inequívoco entre dois cards equivalentes.
- **Desnecessário / duplicado:** “Nenhuma captura é iniciada aqui” e banner “Controle sempre visível” repetem detalhes do funcionamento; topbar e “Hub de gravação” nomeiam a mesma área duas vezes.
- **Hierarquia / densidade:** usa espaço e texto para explicar passividade, mas não responde “posso gravar agora?” ou “já estou gravando?”.
- **Navegação:** hub estático independe do runtime. Biblioteca é funcional neste worktree; não classificar os callbacks como vazios.
- **Copy:** “Hub”, “serviço” e “sessões finalizadas e verificadas” são termos internos. O texto diz que câmera só começa após confirmação, mas a configuração abre preview assim que a permissão existe: aquisição de câmera e gravação foram confundidas.
- **Visual / acessibilidade:** dois cards e banner com pesos similares; tokens/ícones oficiais presentes. Falta semântica de estado operacional porque o próprio estado não é apresentado.
- **Smell Compose:** rota não recebe snapshot nem estado de carregamento; conteúdo estático em uma linha impede evolução segura por estados.
- **Estados / feedback:** sem pronto/gravando/finalizando/recuperando/falha; sem CTA para sessão vigente. **Aresta:** iniciar outra preparação pode disputar CameraX; `unbindAll()` do serviço pode afetar preview da configuração.
- **Issues:** A02 P1 C; A03 P0 R; A04 P1 C (copy sobre câmera/preview).

### 3. Configurar gravação — `SecurityRecordingConfigurationRoute` (E01:84–177)

- **Propósito / objetivo:** autorizar recursos, confirmar enquadramento e iniciar. **Ação principal:** “Iniciar”, no fim do conteúdo rolável, depois de múltiplos blocos.
- **Desnecessário:** qualidade fixa apresentada como card positivo; cinco escolhas de duração antes do primeiro uso; saúde normal de storage/notificações como cards permanentes. “Primeira versão” expõe processo de desenvolvimento.
- **Duplicação:** título, subtítulo, estados individuais e banner “Configuração incompleta” repetem a pendência; autorização conjunta reaparece enquanto qualquer permissão faltar.
- **Hierarquia / densidade:** até sete cards de estado/seleção além de preview, banners e botões. Ausência esperada de permissão recebe o mesmo Danger de erro. CTA perde prioridade para diagnóstico.
- **Navegação:** início abre Active por sucesso do despacho, não por resultado de gravação — aceitável somente se Active representar preparação. Falta rota dedicada para reparar câmera/microfone negados permanentemente.
- **Copy:** “Microfone pronto” equivale apenas a `microphoneGranted`; “Câmera pronta” é calculado antes de confirmação de streaming (`provider`/view não nulos, sem erro). Permissão e disponibilidade real são fatos diferentes. Botão deveria dizer “Iniciar gravação”.
- **Visual:** segmented controls Material sem composição específica, duração dividida em dois grupos visuais para uma só escolha; SUCCESS/DANGER por recurso cria mural colorido. Preview `FILL_CENTER` sem contrato da área efetivamente gravada pode ocultar bordas do enquadramento.
- **Acessibilidade:** labels visíveis nas escolhas são positivos. Dois grupos de seleção para um domínio, preview sem descrição funcional, título único truncável na topbar, sem anúncio de conclusão/erro. Clipping/insets são V, não comprovados por fonte.
- **Smell Compose:** camera2 discovery, CameraX binding, permissões, criação de canal, `StatFs`, decisão de navegação e apresentação dentro do mesmo composable. `remember` perde lente/duração ao recriar; efeitos não expõem reducer/UI state testável. `StatFs` e detecção síncrona ficam no caminho da UI.
- **Estados ausentes:** descoberta em progresso distinta de nenhuma lente; streaming confirmado; provider que falhou; microfone ocupado; permissão permanente câmera/mic; settings indisponível; storage mudando; reentrada com captura ativa.
- **Feedback ausente:** uma causa prioritária com ação direta; retorno de settings sem precisar procurar card; confirmação da configuração persistida.
- **Arestas:** falha de binding troca AndroidView por mensagem sem CTA de retry; ausência de permissão aparece como “nenhuma câmera”; channel bloqueado em API 26–32 é ignorado por `notificationsReady`; storage só é amostrado em composição/admissão.
- **Issues:** A05 P1 C (densidade); A06 P1 C (prontidão); A07 P1 C (permissões); A08 P1 R (preview/enquadramento); A09 P1 C (estado local); A10 P0 C (admissão incompleta de notificação/rechecagem).

### 4. Confirmação de início — `AlertDialog` (E01:162–174)

- **Propósito / objetivo:** confirmar captura de áudio e vídeo. **Ação principal:** “Iniciar”; secundária Cancelar.
- **Desnecessário / duplicado:** repetir confirmação genérica a cada sessão não acrescenta contexto depois do primeiro uso; a preparação já tem CTA explícito. Não remover disclosure obrigatório sem política aprovada.
- **Hierarquia / densidade / navegação:** duas ações claras, conteúdo curto; falta primeiro uso sobre destino, retenção e limites de continuidade. Cancelamento não grava, comportamento a preservar.
- **Copy / visual:** diálogo Material cru, sem padrão de feedback CalcMot; não informa que preview usa câmera e que expiração do privado é diferente de galeria.
- **Acessibilidade:** sem problema de foco demonstrado; testar contenção/restauração e expansão de texto no componente real.
- **Smell / estados / feedback:** Boolean local controla modal; falha de despacho vira banner distante do preview. Mensagem “Mantenha o CalcMot visível” é usada para qualquer falha de `startForegroundService`, incluindo causas não relacionadas à visibilidade.
- **Aresta:** concede tokens de 15s e despacha Start com revisão 0; uma sessão existente pode rejeitar sem resposta acionável na tela.
- **Issues:** A11 P1 C; A03 P0 R.

### 5. Gravação ativa e resultado — `SecurityRecordingActiveContent` (E01:193–237)

- **Propósito / objetivo:** saber se captura e encerrar; depois confirmar resultado. **Primária:** Parar apenas em RECORDING; abrir biblioteca em VERIFIED.
- **Desnecessário / duplicado:** preparação e finalização usam título/subtítulo mais card “Estado”; sucesso usa título técnico, explicação e banner repetindo verificação.
- **Hierarquia / densidade:** RECORDING é enxuto, porém incompleto: REC + título + texto + Parar, sem duração. Não adicionar dashboard para preencher o vazio.
- **Navegação:** FAILED manda “volte à configuração” sem botão; resultado não abre diretamente a gravação que acabou. IDLE é renderizado como preparação, podendo mostrar espera sem sessão nem cancelamento após processo novo.
- **Copy:** “promovida para a biblioteca”, “arquivo contém duração válida” e “preservar” expõem plumbing e confundem mídia temporária com cópia mantida na galeria.
- **Visual:** REC em SectionTitle compete com outro título; não existe grupo estável de estado/duração. Botão Danger tem contraste insuficiente, conforme seção abaixo.
- **Acessibilidade:** REC tem descrição, positivo; sem live region controlada para mudança de fase. Duração inexistente impede anúncio útil. Sem feedback de STOP rejeitado.
- **Smell Compose:** `when` mistura fase de captura e disponibilidade de arquivo; store singleton não fornece sessão finalizada/duração; IDLE e PREPARING colapsados.
- **Estados ausentes:** timer, Pausando/Pausado/Retomando/Rotacionando, snapshot desconectado, recuperação, resultado parcial. Finalização não tem timeout/falha de persistência observável.
- **Feedback ausente:** motivo do encerramento automático; Stop em transição obsoleta; confirmação acionável do trecho recuperado; acesso ao resultado específico.
- **Arestas:** Status ignora áudio depois da primeira confirmação, mantendo REC indevido; falha de I/O pode deixar FINALIZING; voltar/reabrir não garante retorno à sessão real.
- **Issues:** A12 P1 C (timer); A13 P1 C (recuperação por UI); A14 P0 C (áudio pós-start); A15 P1 C (pausa ausente); A16 P0 R (transições penduradas); A17 P0 C (contraste).

### 6. Biblioteca — `SecurityRecordingLibraryRoute` (E02:54–70)

- **Propósito / objetivo:** encontrar uma sessão e revisá-la. **Primária:** abrir card válido.
- **Desnecessário / duplicado:** “Biblioteca de gravações” + “Gravações locais” + explicação de verificação repetida em cada item; linguagem de garantia em vez de tarefa.
- **Hierarquia / densidade:** todos os itens são cards verdes/vermelhos inteiros; duração e tamanho numa linha rotulada. Falhas exibem 0:00/0 KB, que podem representar ausência, não métricas verificadas. Sem agrupamento por dia ou diferenciação de resultado parcial.
- **Navegação:** item inválido não abre detalhe nem explicação acionável. Sem Nova gravação no vazio; sem ajustes/storage; captura ativa não aparece.
- **Copy:** “Verificada e disponível” não explica prazo. `interrupted_recovered` não é apresentado no item disponível: uma interrupção recuperada parece conclusão normal.
- **Visual / acessibilidade:** fonte e cores do DS, mas mural de estados positivos; card inválido sem ação reduz acesso à recuperação; agrupamentos/heading, contraste e foco precisam de teste real.
- **Smell Compose:** `sessions = emptyList()` também representa carregamento; `LaunchedEffect(Unit)` consulta uma vez; `forEach` em Column compõe toda a biblioteca; verificação completa com hash de cada mídia em toda consulta.
- **Estados / feedback ausentes:** loading, erro de consulta, retry, sincronização após finalizar, expiração, publicação/arquivo ausente, falha parcial, filtros vazios.
- **Arestas:** recovery e consulta sem catch na rota; erro pode deixar vazio enganoso. Coleção aberta não atualiza quando outra sessão termina. Sem retenção/exclusão, uso de disco cresce indefinidamente.
- **Issues:** A18 P1 C (loading/reatividade); A19 P0 C (retention/delete); A20 P1 C (histórico/findability); A21 P0 C (interrupção oculta); A22 P1 C (custo de leitura).

### 7. Reproduzir gravação — `SecurityRecordingPlayerRoute` / `RecordingPlayer` (E02:73–165)

- **Propósito / objetivo:** revisar vídeo e áudio de uma sessão. **Primária:** Reproduzir; existem sliders de posição e volume.
- **Desnecessário / duplicado:** slider permanente de volume e linha “Posição” fora do player; tempo separado dos controles. Não há necessidade comprovada de mixer próprio.
- **Hierarquia / densidade:** player, botão pequeno alinhado à esquerda, slider e volume espalham a mesma tarefa pela tela. Título não inclui data ou contexto da sessão.
- **Navegação:** funciona por ID, mas carrega/verifica a biblioteca inteira para achar um item; sem detalhe, preservar, compartilhar, excluir ou voltar ao ponto da lista explicitamente mantido.
- **Copy / visual:** loading é banner estático; indisponível usa explicação genérica. Player nativo tem controller desativado, e controles Material avulsos produzem aparência de demo.
- **Acessibilidade:** Play/Pause tem descrição, alvo Material padrão 48dp a preservar. Sliders sem rótulo semântico/valor de tempo associado; texto acima não associa automaticamente label. Não há `Player.Listener` de erro ou estado Buffering visível.
- **Smell Compose:** polling a cada 250ms substitui listeners; seek em cada arraste; release só em onDispose, sem pausa por ON_STOP; falta coordenação de áudio com captura ativa.
- **Estados ausentes:** buffering, erro de decoder, arquivo apagado durante reprodução, rotação/restauração da posição, fim da playlist, lacunas, disponibilidade expirada.
- **Feedback ausente:** erro de playback acionável e trecho indisponível; confirmação do que está sendo revisto.
- **Arestas:** player pode continuar com Activity em background enquanto composable segue composto; áudio de reprodução pode contaminar nova captura; hash recalculado não é comparado ao hash persistido, portanto mudança legível do arquivo não é sinalizada como alteração.
- **Issues:** A23 P1 C (player); A24 P1 R (lifecycle/áudio); A25 P0 C (integridade incompleta); A26 P1 C (ações/contexto ausentes).

### 8. Notificação Android — E05:390–418

- **Propósito / objetivo:** identificar captura e parar fora do app. **Primária:** Parar; existe durante preparação/gravando e é removida no terminal.
- **Desnecessário / duplicado:** título só “CalcMot” não nomeia a tarefa; não há excesso de informação.
- **Hierarquia / navegação / copy:** conteúdo curto, mas sem duração e sem `contentIntent`; tocar corpo não reconecta à sessão. “Controle sempre visível” não é promessa garantível: Android/OEM e escolhas do usuário afetam visibilidade.
- **Visual / acessibilidade:** usa NotificationCompat padrão, correto; usa mipmap do launcher como small icon, inadequado ao contrato monocromático Android. Não recriar notificação como UI customizada.
- **Smell / estados / feedback:** ação usa revisão do snapshot e rejeição fica silenciosa; sem estado Pausado/Rotacionando, nenhum resultado terminal acionável fora do app.
- **Arestas:** botão de preparação pode chegar depois do Start confirmado e ficar obsoleto; bloquear notificações depois de iniciar não é observado. Usuário pode parar o app pelo gerenciador Android, sem callback: recovery não pode depender de onDestroy.
- **Issues:** A10 P0 C; A27 P1 C (retorno); A28 P2 C (ícone); A29 P0 R (Stop rejeitado).

## Problemas técnicos que alteram a experiência

| ID / severidade | Gatilho e comportamento atual | Consequência / evidência |
|---|---|---|
| A30 P0 C | `automaticStop` espera `segmentMinutes` e envia STOP (E05:219–237); não cria próximo segmento nem limita arquivo a 1 GiB | UI chama de segmento um temporizador de término; gravação deixa de cobrir o trabalho sem explicar a regra |
| A14 P0 C | Depois de `captureConfirmationRequested=true`, todo Status retorna antes de avaliar áudio (E05:194–198) | Mudança para fonte silenciada/erro de áudio pode manter REC e “áudio e vídeo” |
| A31 P0 C | Start verifica espaço uma vez; política 128 MiB + 45 MiB/min (E07); sem monitor de disco/térmica no serviço | Viola reserva AD-6 de 512 MiB e headroom para export; código enumera THERMAL mas não o detecta |
| A32 P0 R | Application fotografa runtime no recovery, depois lê sessões; Start não aguarda bootstrap (E09:48–51; E11:19; E05:112–139) | Uma sessão nova pode entrar no SELECT depois da foto e ser finalizada pelo recovery como sessão morta |
| A33 P0 C/R | `RecordingClaimEntity` guarda `has_audio=true` etc.; recovery usa Mutex e `completeRecovery` sem expectedRevision (E08:69,109; E09) | Não implementa claim operacional durável de AD-7. Corridas entre writer/recovery não são protegidas por esse nome “claim” |
| A34 P0 C | Qualquer `Finalize.hasError` chama fail/quarentena; recovery busca apenas fases ativas, não FAILED (E05:188,322; E08:106) | Saída legível com erro não é avaliada para recuperar trecho; erro não equivale automaticamente a arquivo inútil |
| A16 P0 R | Exceções de DAO/update durante captureConfirmed/finish não recebem terminal consistente (E05:203–254); sem limite de finalização | Pode ficar preparando/finalizando sem próxima ação; não provar por teste atual |
| A25 P0 C | Promoter lê tracks/duração e calcula hash; repository descarta hash recalculado (E10; E09:33) | Verificação mínima existe, mas não prova amostras A/V decodificáveis/sincronizadas nem compara alteração ao digest armazenado |
| A19 P0 C | Banco não possui retenção/galeria/export; sem worker/exclusão local | Não cumprir retenção documentada; mídia e quarentena acumulam; não prometer “24h” na UI atual |
| A35 P0 V | Backup exclui db principal e diretório, sem nomes explícitos `-wal/-shm` (E12) | Cobertura dos sidecars deve ser comprovada ou explicitada conforme AD-12; não afirmar vazamento reproduzido |
| A36 P0 V | Política de privacidade local ainda trata somente ofertas/acessibilidade, sem câmera, áudio, retenção ou publicação | Gate de texto/base/transparência/declarações Play continua aberto; fonte local não comprova status do Console |
| A37 P0 V | Testes leem strings do fonte ou instanciam estado; sem evidência anexada de matriz real | Não qualificam encerramento por Android, chamadas, térmica, recovery ou regressão de concorrência com Uber/99 |
| A38 P0 R | CAS PREPARING→RECORDING termina em IO antes de `gate.markCaptureConfirmed()` no Main; Stop pode usar revisão anterior (E05:208–218,240–249) | Encerramento legítimo pode virar STORAGE e quarentena; sincronizar apenas o gate não serializa commits/callbacks |
| A39 P0 C/R | STOP valida revisão/fase, mas não compara `command.sessionId` com sessão ativa (E06:42–48); revisão reinicia com instância do serviço | Comando interno antigo com revisão coincidente pode atingir outra sessão. Serviço não exportado reduz exposição externa, não resolve identidade |
| A40 P0 C/R | Gate devolve Accepted original da duplicata, mas serviço executa start novamente para qualquer Accepted (E06:29; E05:97–105) | Teste de igualdade do retorno não prova idempotência dos efeitos; replay pode repetir inserção/aquisição e falhar sessão |
| A41 P0 R | Falha DB depois de rename manda MP4 já verificado para quarentena (E05:259–308); recovery não revisita FAILED/quarentena | Evidência válida pode ficar inacessível em vez de pendente de reconciliação; manter intenção e associação duráveis |
| A42 P0 R | `onDestroy` fecha recursos, mas não publica interrupção/terminal; store vivo pode continuar RECORDING; recovery exclui ID que store diz ativo (E05:356; E09:48–51) | UI pode exibir snapshot obsoleto e adiar reconciliação até outro processo; reproduzir destruição do serviço com processo da Activity vivo |

Esses achados adicionais estão cobertos por G13/G14/G19/G20 e CAM-002–006/014. A revisão independente somente leitura do lifecycle confirmou os riscos de interleaving; não executou testes e não altera a classificação R.

### Contraste calculado, sem suposição visual

Usando luminância relativa sRGB e cores opacas dos tokens (E13):

| Combinação real | Razão aproximada | Avaliação |
|---|---|---|
| TextPrimary `#F7F7F7` sobre Danger `#FF6670` | 2,65:1 | Reprova texto normal e grande; afeta Parar |
| TextInverse `#111111` sobre Danger | 6,64:1 | Candidato válido para texto do botão |
| TextPrimary sobre BrandPrimary `#1768F9` | 4,47:1 | Abaixo de 4,5:1 para texto normal; não arredondar para aprovar |
| TextMuted `#8E929B` sobre Surface `#0B1016` | 6,12:1 | Passa nessa combinação opaca; alpha pode alterar resultado |

Tipografia do botão: 15sp bold. Exigir 4,5:1, sem presumir que todo bold é texto grande. Ajuste semântico proposto no UX spec reaproveita BrandPrimaryDark e TextInverse; isso NÃO autoriza alterar os botões globais neste trabalho. Critério: [WCAG — contraste mínimo](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

## O que já é correto e deve ser preservado

- Vertical própria sem integração de câmera nos pipelines Uber/99; banco próprio, lente frontal como preferência, 480p sem opção cosmética de 720p.
- Início com Activity RESUMED e token curto de uso único; serviço não exportado, START_NOT_STICKY, tipos camera/microphone e notificação antes da aquisição no serviço.
- `REC` inicialmente condicionado a Start observado e AudioStats ativo; a falha é não continuar verificando, não ausência total de confirmação.
- Protocolo pending → sync/verify → rename e gate de revisão; fortalecer essas bases, não substituí-las por booleans da UI.
- Player não inicia automaticamente; controles possuem base nativa; sessão já é agregada em domínio.
- Testes existentes de passividade, autorização, comandos e regressão são ativos a preservar. Asserções textuais devem evoluir para comportamento nas histórias correspondentes, sem apagar invariantes.

## Dez problemas que mais importam hoje

1. A14: REC pode sobreviver à perda de áudio depois do início.
2. A30: “segmento” encerra toda a gravação; não há rotação.
3. A32/A33: recuperação e captura não compartilham exclusão durável/bootstrap.
4. A31: armazenamento e aquecimento não são monitorados durante captura.
5. A19/A36: retenção, exclusão e contrato de dados não estão entregues de ponta a ponta.
6. A34/A25: finalização com erro descarta oportunidade de recuperação; integridade é anunciada além do que foi verificado.
7. A03/A29/A27: reentrada, retorno pela notificação e Stop obsoleto não dão controle confiável.
8. A05/A06/A07: configuração é um mural; permissões são confundidas com prontidão.
9. A12/A13/A21: falta duração, resultado específico e interrupção visível.
10. A17/A18/A23: contraste, biblioteca não reativa e player avulso comprometem leitura e acabamento.

## Conclusão de release

O motor funcional é um primeiro corte local, com salvaguardas úteis e cobertura parcial. Não é correto tratá-lo como engine já qualificado para uma sessão longa em produção. O redesenho deve reduzir a UI e, simultaneamente, depender de um contrato de estado verdadeiro. A sequência e os gates estão em [camera-delivery-plan.md](camera-delivery-plan.md); prioridades por gap em [camera-gap-matrix.md](camera-gap-matrix.md).
