# CalcMot UX/UI Interno - Material 3 Production Ready

## Diagnostico

### P0

- Home parecia painel, com muitos cards e explicacoes simultaneas.
- Estado sem permissao usava peso visual de erro, assustando mais do que guiando.
- Estado pausado tinha botao e toggle para a mesma acao.
- Screenshots podiam indicar topo cortado por padding inconsistente entre topbar e conteudo.

### P1

- Metas empilhava presets como cards e ainda mostrava controles duplicados.
- Configuracoes misturava controle com texto institucional.
- Ajuda renderizava varios cards com o mesmo peso visual.
- Privacidade trazia detalhe demais logo no inicio.

### P2

- Navegacao com texto "Menu" parecia prototipo.
- Teste visual anterior gerava PNG, mas quase nao reprovava poluicao, duplicidade ou estado visual indevido.

## Alteracoes aplicadas

- Home refeita com Material 3: `TopAppBar`, `ElevatedCard`, `OutlinedCard`, `Button`, `OutlinedButton` e `TextButton`.
- Home agora tem no maximo dois cards principais: status e meta.
- Permissao faltando virou estado necessario, sem vermelho dominante.
- Pausado ficou com uma unica acao primaria: ligar calculo automatico.
- Configuracoes virou tela de controles com `ListItem`, `Switch`, `SingleChoiceSegmentedButtonRow` e `HorizontalDivider`.
- Metas usa segmented control para Comecando, Equilibrado e Exigente, com inputs manuais claros.
- Ajuda virou FAQ compacto com `ListItem`.
- Privacidade ganhou resumo principal e detalhes abaixo da primeira dobra.
- Onboarding foi reduzido para tres checks, card de permissao e CTAs claros.

## Removido

- Card grande de semaforo na Home.
- Card grande de seguranca na Home.
- Toggle duplicado na Home pausada.
- Preview do overlay na Home.
- Presets empilhados como cards grandes em Metas.
- Chips de modo `Por km / Por hora` sem funcao clara.
- Cards explicativos grandes em Configuracoes.
- Mural de cards na Ajuda.
- Botao `Continuar` desabilitado sem explicacao no Onboarding.
- Botao textual `Menu` na topbar.

## Material 3 usado

- `Scaffold`
- `TopAppBar`
- `ModalNavigationDrawer`
- `NavigationDrawerItem`
- `ElevatedCard`
- `OutlinedCard`
- `ListItem`
- `Button`
- `OutlinedButton`
- `TextButton`
- `Switch`
- `SingleChoiceSegmentedButtonRow`
- `SegmentedButton`
- `FilterChip`
- `HorizontalDivider`

## Componentes CalcMot mantidos

- `CalcMotHeroStatus` e `CalcMotMetricSummary` foram implementados como componentes privados de produto, usando Material 3 por baixo.
- Componentes antigos continuam apenas onde a rodada nao exigiu redesenho completo, como Resultado e diagnostico debug.

## Screenshots

Before:

`docs/screenshots/ux-ui-material3-before`

After limpo:

`docs/screenshots/ux-ui-material3-after-clean`

Telas cobertas:

- Home sem permissao
- Home pronta
- Home pausada
- Metas
- Configuracoes
- Ajuda
- Privacidade
- Onboarding

## Testes atualizados

- Home sem permissao reprova se houver mais de uma acao primaria, toggle duplicado ou estado visual de erro textual.
- Home pronta reprova se houver mais de dois cards principais, card de semaforo, card de seguranca ou preview de overlay.
- Home pausada reprova se houver toggle e botao para a mesma acao.
- Metas valida segmented presets, atualizacao dos inputs e reflexo da meta salva na Home.
- Configuracoes valida toggle, posicao do aviso e ausencia de blocos explicativos grandes.
- Ajuda valida FAQ compacto com 5 itens.
- Privacidade valida resumo principal e ausencia de promessas falsas.
- Teste visual reprova screenshot vazio, vermelho dominante na permissao, excesso de cards principais na Home, toggle duplicado, mural de Ajuda e termos tecnicos proibidos.

## Escopo preservado

Nao foram alterados:

- overlay;
- parser;
- calculos;
- classificacao financeira;
- captura;
- latencia;
- whitelist;
- AccessibilityService;
- OverlayManager;
- OverlayDesignSystem;
- OverlayView;
- microcopy do Overlay V2.3;
- Safe Mode.
