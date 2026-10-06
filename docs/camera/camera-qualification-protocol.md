# CAM-022/023 — protocolo de homologação pendente

Este protocolo descreve a campanha completa ainda pendente. A instalação autorizada e os testes básicos de abertura/enquadramento realizados em 5 de outubro de 2026 não representam execução ou aprovação dessa campanha. Executar novos testes de dispositivo somente com autorização explícita prevista no AGENTS.md. Usar conteúdo sintético, sem passageiros/corridas reais, sem dumps brutos de terceiros.

## Matriz a aprovar antes de executar

Produto/QA devem nomear modelos concretos, OEM, API, RAM/armazenamento, versão Android, configurações de bateria e condição térmica. Cobrir APIs24/28/29/32/33/34/36, entrada e intermediário. Nomear responsáveis e fixar limites numéricos para A/V sync, lacunas, bytes/h, CPU, bateria e temperatura. Sem esses limites não registrar “aprovado”.

## Campanha técnica

Registrar versão/build, modelo, cenário, duração, condição, resultado esperado/observado e evidência sanitizada. Comparar cada cenário de consumo com o mesmo aparelho e apps de motorista sem câmera. Gravar ≥2h em480p com múltiplas rotações, validação de áudio/vídeo, ordenação, duração/gaps e hash. Exercitar:

1. Instalação/configuração: primeiro aviso, cada permissão negada/concedida/permanente, notificações/canal desligados, retorno dos ajustes, lente ausente e orientação. Nenhuma navegação concede Start.
2. Controle: Start duplicado, Stop na preparação, Pause/Resume repetidos, evento tardio, notificação de sessão anterior, concorrência com reprodução/operação. REC nunca aparece sem A/V/progresso observado.
3. Continuidade: Uber/99 em foreground, tela apagada, recentes, chamada/preempção, privacidade/mic, encoder/câmera indisponível, low-memory e thermal severe (29+).
4. Persistência: kill/force-stop/reboot, falha após sync/rename/insert/commit; pending válido, zero, sem áudio, corrupto e hash alterado. Nova recuperação converge sem perder mídia válida ou reiniciar captura.
5. Espaço: margem exata, outra app ocupando disco, rotação1GiB/duração, falta de espaço na composição/galeria. Preservar trechos anteriores.
6. Reprodução/retensão: autoplay ausente, controles nativos, onStop, rotação/reentrada/posição, trecho ausente com reprodução parcial, prazo durante play/sheet/claim, extensão só superior, relógio UTC vs monotônico e galeria independente.
7. Export: sessão multi-arquivo, falha/retry sem duplicação, MediaStore29+ e scanner24–28, share somente leitura/um arquivo, expiração privada durante grant1h e revogação posterior.
8. Exclusão: confirmação, tombstone, seleção ativa/claim bloqueada, lote parcial, falha DB/files, recálculo físico e galeria preservada.

## UX com motoristas

≥5 participantes, consentimento e dados anonimizados. Em 320×568/360×800/393×852/852×393, fonte1/1,3/2 e TalkBack: identificar estado em≤5s; preparar/iniciar/parar/reparar; localizar sessão em≤30s; distinguir temporário/galeria/share; excluir com confirmação. Registrar sucesso/tempo/erro e citações autorizadas. Confusão sobre estar gravando exige correção antes do aceite. Conferir contrastes, touch48dp, insets e controles nativos. Previews sintéticos são evidência de composição, não de captura/homologação.

## Registro de decisão

Para cada gate: responsável, data, versão avaliada, limites, medidas, falhas abertas, aprovação/reprovação e referência à evidência. LEGAL-01, DEVICE-01, DESIGN-01, ENGINE-01 e RELEASE-01 permanecem pendentes sem esse registro. Não publicar automaticamente.
