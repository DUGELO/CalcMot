# CalcMot — convite orgânico após uso real

## Regra

- Conta **dia local distinto com aviso de oferta realmente exibido**, não uma abertura de app nem um OCR rejeitado.
- Elegível quando houver **2 dias consecutivos** de uso confirmado; no retorno à Home no dia 2 ou 3 (uso ontem), se houver permissão ativa.
- Primeira instalação desta versão começa com o contador zerado: não recupera histórico passado, pois o app v3.5 não persistia essa métrica.
- O rastreamento é local em SharedPreferences isolado (`calcmot_growth_share_v1`), sem contato, identidade, localização ou conteúdo das ofertas.
- Escritas acionadas pelo overlay ocorrem em `Dispatchers.IO`, no máximo um agendamento por dia por processo.

## UX segura

- Um cartão discreto **somente na Home visível**, nunca sobre a Uber/99, nunca via push ou por AccessibilityService.
- Não abre Sharesheet nem envia mensagens automaticamente.
- Ações: Compartilhar, Agora não e Não mostrar novamente (opt-out persistente).
- Ao sair da Home (Activity ON_PAUSE), o cartão some e o próximo retorno obedece ao cooldown.
- Abertura do Sharesheet é diferente de mensagem efetivamente enviada.

## Frequência

- Primeiro convite apenas após 2 dias consecutivos confirmados.
- No máximo duas apresentações em qualquer janela móvel de 30 dias.
- Depois de aparecer/dispensar: 14 dias de silêncio.
- Após a segunda dispensa: 60 dias de silêncio.
- Após tocar Compartilhar no convite: 60 dias de silêncio, mesmo que o Sharesheet seja cancelado.
- Após Não mostrar novamente: não há reexibição. Compartilhamento voluntário no menu/Home permanece disponível.

## QA

1. Testes unitários: `./gradlew :app:testDebugUnitTest`.
2. Teste instrumentado de UI: `./gradlew :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.ui.CalcMotGrowthSharingUiTest'`.
3. Verificar manualmente após **dois dias reais distintos** de ofertas, retornando à Home.
4. Confirmar que não aparece em Uber, 99, quando a app está em background, nem enquanto a permissão está inativa.
5. Confirmar fechamento, opt-out e todos os intervalos de silêncio sem mudar o relógio do aparelho de produção.
6. Revalidar pipeline Uber/99, ANRs e performance antes de release.

**Não alterar o relógio principal do motorista para simular dois dias.** Use testes puros da política ou aparelho/emulador dedicado para validação acelerada.
