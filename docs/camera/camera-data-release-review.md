# Câmera — contrato de dados e revisão de release

Revisão técnica: 5 de outubro de 2026. Rascunho para produto/jurídico; não é uma declaração aprovada da Play Store.

## Fatos implementados

- Início explícito por Activity própria visível; câmera e microfone autorizados; notificações/canal são precondição do produto. Serviço privado camera|microphone, START_NOT_STICKY, sem reinício de captura em boot/kill/force-stop.
- Originais, composição e staging compartilhável: subtree privado `security-recording/`. Room próprio `calcmot_recordings.db`, WAL e SHM, subtree e preferências excluídos de cloud backup/device transfer. Falhas locais usam códigos sanitizados; nenhum envio de sessão pela vertical.
- Prazo padrão de 24h; opções 3/7/15/30 dias desde finalizedAt. Expiração impede acesso/novas operações antes da limpeza física. Elevar o padrão afeta somente próximas sessões. Estender nunca reabre uma sessão vencida.
- Cópia na galeria é permanente e independente; não é apagada pelo prazo/exclusão privados nem pelo app durante limpeza. API29+: MediaStore com IS_PENDING e verificação antes da publicação. API24–28: permissão WRITE_EXTERNAL_STORAGE limitada a maxSdk28, somente ao salvar, arquivo oculto preparado/verificado e scanner.
- Compartilhar é uma ação explícita com folha nativa e grant de leitura de um MP4; FileProvider não exportado expõe apenas `security-recording/share/`. O aplicativo destinatário pode guardar/redistribuir sua própria cópia. O prazo privado continua igual. A limpeza de grants/staging respeita max(expiresAt, tentativa visível de lançamento +1h), conservadoramente mesmo se o Android não abrir a folha. Preparar uma cópia sem lançamento não prolonga o prazo privado.
- Limpeza ocorre na inicialização, biblioteca, encerramento e worker periódico de 15min; Android pode atrasar trabalho. Nenhum receiver/permissão de boot é acrescentado pelo WorkManager ao Manifest final. Abrir o app inicializa/reagenda trabalho; isso nunca adquire mídia.
- Nome público Gravação de segurança; não há disfarce de indicadores Android/notificação ou alegação de gravação invisível.

## Proposta de transparência no primeiro uso

A tela explica áudio/vídeo para registro de segurança, armazenamento local e prazo, cópia independente na galeria, limites de continuidade e dever de informar pessoas. O aceite do motorista registra somente a versão do aviso. Não representa consentimento de passageiro nem comprova base jurídica. Produto/jurídico devem aprovar texto, processo e finalidade antes de disponibilização pública; não há aprovação presumida nesta implementação.

## Reconciliação global ainda necessária

Firebase Analytics e Crashlytics estão presentes e inicializados no app. Clarity foi integrado posteriormente para todo o app, sem exclusão da Activity de gravação: seus replays podem incluir conteúdo visual exibido. O isolamento de imports do serviço não comprova ausência de coleta visual por SDK global. Ver [escopo atualizado](../clarity-integration.md). As antigas notas “sem analytics/sem coleta” não sustentam Data Safety. A ausência de imports de telemetria na câmera comprova o isolamento da vertical, não ausência de coleta no aplicativo inteiro. A política global, a captura visual/OCR da 99 e `isAccessibilityTool=true` precisam de revisão pelo responsável de entrega. Não foi alterado nenhum pipeline para resolver divergências documentais.

## Checklist revisável

| Item | Evidência local | Aceite externo |
|---|---|---|
| Migração v1→v2 preserva fatos | schemas e teste instrumentado compilado | Executar em device autorizado |
| Proprietário, CAS, claims e integridade | testes locais e fault test de rollback compilado | Campanha de falhas arquivo/DB e A/V real |
| Activity/serviço/provider privados; maxSdk28; paths limitados | Manifest fonte + mesclado | Verificar APK/AAB e plataforma-alvo |
| Backup banco/WAL/SHM/subtree | regras e teste de fonte | Teste de backup/transfer em aparelho |
| A/V, consumo, rotação, pause e coexistência | implementação + protocolo de qualificação | DEVICE-01/ENGINE-01 |
| Texto/passenger transparency | proposta factual versionada | LEGAL-01 |
| UX acessível e entendimento | previews reais com fixtures | DESIGN-01/CAM-022 com ≥5 motoristas |
| Play/data safety/FGS | revisão técnica, builds e lint registrados | RELEASE-01; sem publicação autorizada |

VersionName, versionCode e material de assinatura permanecem como no baseline. APK/AAB são artefatos locais de verificação; não instalar/distribuir/publicar sem autorização própria.
