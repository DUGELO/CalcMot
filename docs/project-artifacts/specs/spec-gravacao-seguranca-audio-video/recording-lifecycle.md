# Companion — Lifecycle da gravação

Este companion é normativo para o estado operacional e adota as decisões `AD-2`, `AD-5`, `AD-7`, `AD-9` e `AD-10` da arquitetura. A interface sempre deriva seu estado de fatos persistidos e confirmações do subsistema de captura.

## Fases operacionais

| Fase | Significado | Ações aceitas | Saída esperada |
|---|---|---|---|
| `Não configurado` | Pré-condições ainda não satisfeitas. | Configurar permissões, lente e duração do segmento. | `Pronto` ou permanência com erro explícito. |
| `Pronto` | Configuração válida e captura inativa. | Iniciar ou alterar configuração. | `Iniciando`. |
| `Iniciando` | Foreground service e captura estão sendo preparados. | Encerrar/cancelar. | `Gravando` após confirmação; caso contrário `Falha`. Não exibe `REC`. |
| `Gravando` | Áudio e vídeo estão confirmadamente sendo capturados. | Pausar ou encerrar. | `Pausando`, `Rotacionando`, `Finalizando` ou `Falha`. Exibe `REC`. |
| `Pausando` | Interrupção conjunta de áudio e vídeo está pendente. | Encerrar. | `Pausado` após confirmação ou `Falha`. |
| `Pausado` | Áudio e vídeo não estão sendo capturados. | Retomar ou encerrar. | `Retomando` ou `Finalizando`. |
| `Retomando` | Reinício conjunto de áudio e vídeo está pendente. | Encerrar. | `Gravando` após confirmação ou `Falha`. Não exibe `REC`. |
| `Rotacionando` | Um segmento atingiu o limite e o próximo está sendo aberto. | Somente encerrar. | `Gravando` após confirmação do novo segmento ou `Falha`. Não exibe `REC`. |
| `Finalizando` | A captura foi encerrada e artefatos/metadados estão sendo consolidados. | Nenhuma ação destrutiva concorrente. | `Encerrada` ou `Falha`. |
| `Encerrada` | A sessão não captura mais; disponibilidade, galeria e exportação seguem eixos próprios. | Reproduzir, salvar, compartilhar, estender ou excluir conforme elegibilidade. | Alterações nos eixos ortogonais. |
| `Falha` | O resultado esperado não pôde ser confirmado. | Encerrar, voltar à biblioteca ou recuperar quando elegível. | `Encerrada` com evidência verificada ou permanência em falha verdadeira. |

## Eixos ortogonais após captura

### Disponibilidade temporária

| Estado | Significado |
|---|---|
| `AVAILABLE` | Originais privados elegíveis para reprodução e exportação. |
| `EXPIRED` | Prazo lógico terminou; nenhuma nova leitura ou exportação é permitida. |
| `DELETED` | Originais privados foram removidos ou confirmados ausentes. |

### Cópia na galeria

| Estado | Significado |
|---|---|
| `NONE` | Nenhuma cópia pública válida é conhecida. |
| `COPYING` | Publicação está em andamento sob claim persistido. |
| `PUBLISHED` | Uma cópia pública independente foi verificada. |
| `FAILED` | A tentativa falhou sem prejudicar os originais. |
| `MISSING` | A cópia antes conhecida não pode mais ser confirmada. |

### Exportação para compartilhamento

| Estado | Significado |
|---|---|
| `IDLE` | Nenhuma montagem ativa. |
| `BUILDING` | Artefato reproduzível está sendo montado sob claim persistido. |
| `READY` | Artefato transitório está disponível para concessão via `FileProvider`. |
| `FAILED` | A montagem ou validação falhou. |

Os eixos são independentes. Uma sessão pode, por exemplo, estar `EXPIRED` no armazenamento temporário e ainda `PUBLISHED` na galeria.

## Regras de verdade de estado

- `REC` aparece somente em `Gravando` após confirmação do evento de início ou retomada.
- `Iniciando` cobre o bootstrap do foreground service e nunca apresenta captura como ativa.
- Em `Rotacionando`, o cronômetro de captura congela; o intervalo não capturado é registrado como lacuna e informado ao motorista.
- Em `Pausado`, nenhum áudio nem vídeo é capturado.
- O cronômetro acumula somente duração confirmadamente capturada, nunca tempo de parede em pausa, rotação ou recuperação.
- Rótulos públicos da biblioteca são projeções dos eixos persistidos, não uma enumeração que apaga fatos independentes.

## Sessões e segmentos

- Uma sessão é o agregado lógico exibido ao motorista.
- Cada segmento finalizado é imutável, ordenado e referenciado pela sessão.
- A rotação ocorre no limite configurado de 5, 10, 20, 30 ou 60 minutos, ou antes disso quando o arquivo atual alcança 1 GiB.
- Um segmento só se torna válido após finalização e verificação mínima de existência e legibilidade.
- Falha ao abrir o próximo segmento não invalida segmentos anteriores já confirmados.
- Reprodução e exportação respeitam ordem, duração capturada e lacunas registradas.

## Notificação e continuidade

- A notificação persistente existe durante o bootstrap e toda sessão ativa.
- Seus controles espelham apenas ações válidas para a fase persistida.
- Alternar aplicativos ou remover o CalcMot dos recentes não muda uma sessão ativa.
- Encerramento do processo, force-stop e reboot não reiniciam captura. Na próxima abertura, a recuperação apenas reconcilia artefatos existentes.

## Retenção

- O nível de retenção é registrado como snapshot no início da sessão.
- `expiresAt` é calculado a partir de `finalizedAt`.
- Extensão só aceita um nível total superior ao atual e nunca ultrapassa 30 dias.
- Ao atingir `expiresAt`, a transição lógica para `EXPIRED` ocorre antes da limpeza física.
- A limpeza física é idempotente e pode ser retomada após interrupção.
- Cópias `PUBLISHED` na galeria não são removidas por expiração ou exclusão dos originais privados.

## Salvar e compartilhar

- Salvar publica uma cópia independente; repetir a ação reutiliza a publicação válida em vez de duplicá-la.
- Compartilhar monta ou reutiliza um artefato transitório e abre o Sharesheet a cada ação explícita.
- Compartilhar não publica na galeria, não estende retenção e não reverte `EXPIRED`.
- A concessão de leitura tem duração limitada; a limpeza física respeita a janela de graça definida pela arquitetura sem alterar a indisponibilidade lógica.

## Falha e recuperação

- Recuperação, expiração, publicação, exportação e finalização usam revisão/claim persistido para impedir dupla promoção.
- A sequência é sempre reservar, executar I/O, verificar e então consolidar o estado.
- Um artefato só é promovido quando sua existência e legibilidade mínima forem confirmadas.
- Na dúvida, o sistema falha fechado: preserva segmentos válidos, registra a lacuna e apresenta indisponibilidade ou falha, nunca sucesso presumido.
