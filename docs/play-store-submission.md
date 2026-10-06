# Play Store Submission Notes

## Listing

- Nome público: CalcMot.
- Categoria recomendada: Auto & Vehicles.
- Público-alvo: motoristas adultos de aplicativo.
- Anúncios: não.
- Monetização v1: sem cobrança.
- Descrição curta sugerida: Assistente de leitura de ofertas para motoristas de app.
- Evitar uso de marca Uber no título, ícone, screenshots e texto principal da loja.

## Data Safety — revisão técnica pendente

Não preencher como “sem coleta/compartilhamento” a partir das notas antigas. O código inicializa Firebase Analytics e Crashlytics e integra Microsoft Clarity em todo o app; a declaração global exige conferência dos eventos, coleta automática, OCR e configuração efetiva dos SDKs. A revisão da câmera não altera a telemetria existente.

A vertical de gravação não importa telemetria nem envia áudio, vídeo, duração, tamanho, hash ou URI a servidor. Arquivos privados e banco ficam no aparelho, fora de backup. Salvar na galeria cria cópia independente; compartilhar entrega acesso temporário ao aplicativo escolhido, que pode reter uma cópia.

Conferir também [Clarity e monetização](clarity-integration.md): captura visual/interações, painel, política publicada e consentimento aplicável.

Consultar [revisão de dados e release da câmera](camera/camera-data-release-review.md). LEGAL-01, DEVICE-01, DESIGN-01, ENGINE-01 e RELEASE-01 continuam abertos até evidência/aprovação correspondente. Estes documentos não autorizam publicação.

## Accessibility API Declaration

Uso declarado: o CalcMot lê cards de oferta visíveis no app de motorista para calcular métricas úteis ao motorista em tempo real.

Pontos obrigatórios para a revisão:

- O app não é ferramenta de acessibilidade para deficiência.
- O app não executa ações autônomas.
- O app não aceita nem recusa corridas.
- O app processa os dados localmente.
- O app permite pausar o monitoramento.

## Release Checklist

- `applicationId` final: `br.com.calcmot`.
- Política de privacidade hospedada publicamente antes do envio.
- Release AAB assinado com keystore fora do Git.
- Screenshots da loja sem marca de terceiros em destaque.
- Logs sensíveis desativados em release.
