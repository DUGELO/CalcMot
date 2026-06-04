# Play Store Submission Notes

## Listing

- Nome público: CalcMot.
- Categoria recomendada: Auto & Vehicles.
- Público-alvo: motoristas adultos de aplicativo.
- Anúncios: não.
- Monetização v1: sem cobrança.
- Descrição curta sugerida: Assistente de leitura de ofertas para motoristas de app.
- Evitar uso de marca Uber no título, ícone, screenshots e texto principal da loja.

## Data Safety

Preencher como sem coleta e sem compartilhamento se a implementação continuar local:

- Leitura local da árvore de acessibilidade.
- Sem backend.
- Sem analytics.
- Sem envio de screenshots, cards, endereços, localização ou métricas.

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
