# Companion — Restrições técnicas

Este companion é normativo para implementação. A arquitetura adotada encerra as antigas perguntas técnicas dos companions de UX; esses documentos continuam normativos para experiência, conteúdo e apresentação.

## Stack aprovada

| Área | Decisão |
|---|---|
| Android | `minSdk 24`, `compileSdk 36`, `targetSdk 36`. |
| Kotlin | Kotlin efetivo `2.2.10`, provido pelo Android Gradle Plugin 9. |
| Java/Gradle | Bytecode Java 11; Gradle executado com JDK 17 ou superior. |
| Compose | Preservar o baseline brownfield e BOM `2025.09.01`; reutilizar o Design System CalcMot. |
| Captura | CameraX `1.6.1`, versão única e convergente para todos os artefatos CameraX. |
| Reprodução/exportação | Media3 `1.11.0`, versão única e convergente. |
| Persistência | Room `2.8.4`. |
| Trabalho recuperável | WorkManager `2.11.2`. |

Nenhuma biblioteca deve ser introduzida em versão divergente do conjunto aprovado. A convergência do classpath é gate de build.

## Captura e serviço

- CameraX é o adapter de captura e sua confirmação de eventos governa as fases `Iniciando`, `Gravando`, `Pausando`, `Retomando`, `Rotacionando` e `Finalizando`.
- A sessão só começa por Activity visível e ação explícita do motorista.
- O bootstrap cria a notificação antes de adquirir câmera e microfone.
- O serviço é não exportado, usa `START_NOT_STICKY` e declara os tipos de foreground service `camera|microphone`.
- A câmera frontal é padrão; a entrega inicial captura somente em 480p.
- Lente e orientação são fixadas ao iniciar e permanecem invariantes até o encerramento.
- `Rotacionando` é fase explícita, sem `REC`, com somente Encerrar disponível.

## Manifest e permissões

| Item | Regra |
|---|---|
| `CAMERA` | Permissão runtime obrigatória antes de iniciar. |
| `RECORD_AUDIO` | Permissão runtime obrigatória antes de iniciar. |
| `FOREGROUND_SERVICE` | Obrigatória para o serviço. |
| `FOREGROUND_SERVICE_CAMERA` | Declarada para captura por câmera. |
| `FOREGROUND_SERVICE_MICROPHONE` | Declarada para captura por microfone. |
| `POST_NOTIFICATIONS` | Em API 33+, deve estar concedida antes de iniciar, como precondição do produto. |
| `WRITE_EXTERNAL_STORAGE` | Somente `maxSdkVersion=28`, quando necessário para salvar na galeria em API 24–28. |

Não ampliar permissões de acessibilidade, OCR ou aplicativos terceiros para atender esta funcionalidade.

## Armazenamento e persistência

- Banco Room e arquivos de mídia são fontes distintas coordenadas por identificadores estáveis; o banco guarda intenção, revisão, claims e resultado verificado.
- Originais e segmentos temporários ficam em diretórios privados excluídos de backup.
- Cada segmento rotaciona no limite de duração configurado ou ao atingir 1 GiB, o que ocorrer primeiro.
- Antes de abrir o próximo segmento, deve existir espaço para: bytes atuais da sessão + duas vezes a estimativa do próximo segmento + 512 MiB de reserva.
- Falta de espaço encerra a captura de forma controlada e preserva os segmentos já finalizados.
- O agregado da sessão mantém segmentos ordenados e imutáveis, duração capturada, lacunas, `finalizedAt`, snapshot de retenção e `expiresAt`.
- Toda operação mutável relevante usa `sessionRevision` e claim persistido; somente o dono do claim vigente pode consolidar o resultado.
- O protocolo obrigatório é reservar/claim, executar I/O, verificar e commit condicional.
- Recuperação é exclusiva por sessão e idempotente; nunca promove arquivo apenas porque ele existe.

## Reprodução, publicação e compartilhamento

- Media3 fica atrás de adapter próprio para reprodução e montagem da representação compartilhável da sessão.
- Em API 29+, publicação na galeria usa MediaStore com `IS_PENDING=1`, escrita, verificação e somente então `IS_PENDING=0`.
- Em API 24–28, publicação escreve em arquivo temporário oculto, verifica, renomeia de forma atômica quando suportado e solicita indexação por `MediaScanner`.
- Repetir Salvar reutiliza uma publicação válida identificada pela sessão; não cria duplicata silenciosa.
- Compartilhamento usa `FileProvider` com authority exclusiva do CalcMot e paths restritos ao diretório transitório de exportação.
- URIs recebem somente concessão temporária de leitura; nenhum caminho privado bruto é exposto.
- A expiração lógica bloqueia novas exportações imediatamente. Artefatos já concedidos respeitam a janela física de graça e depois são limpos de modo recuperável.
- Regras de backup excluem originais, segmentos, exports, temporários de publicação e metadados sensíveis associados.

## Isolamento e seams permitidos

Mudanças globais permitidas devem ser mínimas e limitadas a:

- catálogo/Gradle para dependências aprovadas;
- Manifest e regras de backup;
- bootstrap não bloqueante no `Application`;
- navegação explícita para a Activity/telas de gravação;
- tokens/componentes do Design System quando necessários;
- teste de readiness de liberação.

É proibido introduzir imports, chamadas, eventos ou dependências da vertical de gravação para ou a partir de:

- `accessibility` e `UberAccessibilityService`;
- `processor` e regras de oferta;
- `ninetynine` e OCR 99;
- `overlay`;
- `finance` e cálculos.

Testes arquiteturais e de regressão devem preservar esses limites e os fluxos observáveis Uber/99 do baseline de produção.

## Gates de liberação

- Executar a matriz aprovada de aparelhos/OEM/API nos cenários de iniciar, pausar, retomar, rotacionar, remover dos recentes, encerrar pela notificação, baixa memória, pouco armazenamento, interrupção e recuperação.
- Aprovar texto jurídico, transparência a passageiros, política de privacidade, Data Safety e declaração Play Console dos tipos de foreground service.
- Manter 720p desabilitado até benchmark específico de desempenho, temperatura, bateria, tamanho, sincronismo e estabilidade na matriz-alvo.
- Verificar convergência de CameraX/Media3/Room/WorkManager no classpath e ausência de mudanças nos pipelines Uber/99.

## Referências oficiais

- [CameraX VideoCapture](https://developer.android.com/media/camera/camerax/video-capture)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Foreground service launch restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [MediaStore](https://developer.android.com/training/data-storage/shared/media)
- [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider)
- [Room](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
