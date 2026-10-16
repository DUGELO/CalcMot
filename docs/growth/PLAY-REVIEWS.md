# CalcMot — avaliação Google Play sem atrito

## Implementação
- **Acesso permanente:** menu lateral > Avaliar CalcMot abre `market://details?id=br.com.calcmot`, com fallback HTTPS. Essa ação não usa a API de avaliações (cota invisível) e funciona para quem procura a avaliação manualmente.
- **Convite ocasional:** usa `com.google.android.play:review:2.0.2`, API oficial `ReviewManagerFactory`. Sem modal próprio, sem perguntas prévias, sem escolha de estrelas e sem recompensa por avaliação.
- O Android/Google Play decide se o diálogo nativo aparece. O retorno da API **não confirma** avaliação, nota nem exibição. Não registrar “avaliação enviada”.
- Requisito: pelo menos **5 dias consecutivos** com overlays de oferta realmente exibidos, conforme `GrowthShareStore.currentStreak`.
- Dispara exclusivamente na Home pronta/em primeiro plano, sem painel de compartilhamento ou drawer abertos. Na mesma visita, o convite de compartilhamento tem prioridade; não disparar os dois.
- Persistência local separada `calcmot_play_review_v1`: até 2 tentativas automáticas ao longo da instalação e 120 dias de intervalo entre elas, além da cota interna do Google Play. Sem dados pessoais nem de avaliação.
- Não pedir notificação, não alterar Accessibility/OCR/overlay/serviço.
- Não solicitar avaliação apenas a usuários que indiquem gostar do app; não condicionar a ajuda à nota.

## Limitações dos testes
- O APK instalado por `installDebug` pode não mostrar o diálogo do Google Play. Para validar a UI nativa real use uma instalação elegível via Play Store (faixa de teste interno) e sua conta de teste. O `FakeReviewManager` não exibe interface.
- É possível testar o botão manual no Samsung imediatamente; a abertura de avaliação via Play Store depende da distribuição e das cotas do Google.
- Testar `:app:testDebugUnitTest`, `:app:assembleDebug`, testes instrumentados de menu e navegação, e regressões de compartilhamento e Uber/99 antes do merge.

Referência oficial: https://developer.android.com/guide/playcore/in-app-review/kotlin-java
