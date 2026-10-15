# CalcMot — compartilhamento orgânico V1

Escopo: branch `feat/growth-organico-v1`, baseada na `main` (3.5 / versionCode 12). Implementação inicial independente de Auth, Billing, backend, SMS, contatos e permissões extras.

## Comportamento

- Ação voluntária "Compartilhar CalcMot" no menu lateral e na Home pronta para calcular.
- Ao tocar, abre `Intent.ACTION_SEND` no Sharesheet nativo Android com convite genérico e endereço público da Play Store.
- Nenhuma leitura de contatos, envio automático, link individual de referral, recompensa ou push nesta fase.
- Não há modal proativo, notificação, timer ou listener de ofertas. Nada inicia a partir de OCR, AccessibilityService ou overlay.
- A tela de compartilhamento não garante imobilidade; usar somente quando estiver parado. Gatilhos proativos ficam bloqueados até S31 e observações de segurança confiáveis.

## Métricas e limitações

- `share_sheet_opened` significa exclusivamente que o Android aceitou abrir o Sharesheet.
- `source` restrito a `home` ou `drawer`, sanitizado pela allowlist existente.
- Não afirmar `share_sent`, downloads atribuídos, CAC, novos usuários ou indicação qualificada.
- Não foi criado backend, cookie, código de identificação nem novas permissões.

## Validar no Android antes da produção

1. Abrir a Home pronta, tocar "Compartilhar CalcMot"; verificar Apps do Android e mensagem/link.
2. Abrir menu lateral e repetir; cancelar sem enviar, conferir que não gera falso evento de envio.
3. Repetir com cálculo pausado; permissão pendente; tela pequena e fonte 1,5x.
4. Conferir que abrir/cancelar o compartilhador não altera monitoramento, OCR, overlays ou câmera de segurança.
5. Verificar em DebugView que `share_sheet_opened` contém somente `source` e parâmetros de ambiente já permitidos.
6. Executar `./gradlew :app:testDebugUnitTest` e `./gradlew :app:assembleDebug` no Android Studio local.
7. Fazer teste de regressão das ofertas Uber e 99 e acompanhar ANRs antes de qualquer rollout.

## Fora de escopo

Notificações ou pedidos proativos, deep links individuais, atribuição, trial, recompensas, contas e monetização. S31/S32/S33/S35 mantêm seus próprios gates de implementação e validação.
