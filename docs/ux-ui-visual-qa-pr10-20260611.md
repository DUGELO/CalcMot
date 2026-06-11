# CalcMot UX/UI Visual QA - PR 10

Data: 11 de junho de 2026
Dispositivo: SM-A075M, Android 15

## Objetivo

Validar no aparelho real se a experiencia ficou clara para um motorista cansado, olhando rapido para a tela, com foco em:

- primeira instalacao e permissao pendente;
- Home em espera e pausada;
- drawer orientado a tarefas;
- Seguranca;
- Metas;
- Resultado;
- Ajuda e Privacidade;
- overlay real com impacto financeiro;
- ausencia de overlay residual fora do app de motorista.

## Evidencias capturadas

Capturas salvas em `.tmp/visual-qa-20260611-pr10/`:

- `00-current.png`: primeira instalacao/permissao pendente.
- `04-drawer.png`: drawer com Inicio, Metas, Resultado, Seguranca, Ajuda e Area tecnica.
- `05-security-top.png`: tela Seguranca.
- `07-result-top.png`: tela Resultado vazia.
- `08-help-top.png`: tela Ajuda.
- `09-privacy-top.png`: tela Privacidade.
- `10-home-paused.png`: Home pausada.
- `11-metas-chip-fixed.png`: Metas apos ajuste de chip.
- `19-overlay-great-impact-real.png`: overlay OTIMA com impacto financeiro.
- `20-overlay-good-impact-real.png`: overlay BOA com impacto financeiro.
- `21-overlay-warning-impact-real.png`: overlay ATENCAO com impacto financeiro.
- `22-overlay-bad-impact-real.png`: overlay RUIM com impacto financeiro.
- `23-whatsapp-no-overlay.png`: WhatsApp sem overlay residual. Contem dados pessoais, nao embutir em documentacao publica.
- `24-settings-no-overlay.png`: Configuracoes sem overlay residual. Contem dados pessoais, nao embutir em documentacao publica.

## Resultado por tela

### Primeira instalacao

A primeira dobra explica a permissao em linguagem humana, com CTA unico e bullets curtos. O botao "Continuar" desabilitado fica acompanhado de texto claro sobre o que falta.

### Home em espera

Estado, explicacao e CTA principal aparecem no topo. A acao primaria esta clara. O resumo ainda aparece relativamente cedo, mas abaixo do bloco de seguranca e sem impedir a acao principal.

### Home pausada

O estado "CalcMot pausado por voce" ficou claro. O CTA vira "Ligar CalcMot", evitando sugerir abrir Uber enquanto o app esta pausado.

### Drawer

O menu esta orientado a tarefas: Inicio, Metas, Resultado, Seguranca e Ajuda. Diagnostico fica visualmente separado por "Area tecnica" no build debug.

### Seguranca

Boa primeira dobra. O usuario entende onde o app atua, onde nao deve atuar e que pode pausar. O texto nao parece tecnico.

### Metas

Os presets estao legiveis e configuraveis rapidamente. A captura real revelou quebra no chip "Prioriza hora"; isso foi corrigido para "Por hora", mantendo os tres chips em uma linha.

### Resultado

Separado de Metas, com formulario e estado vazio claros. Nao promete economia garantida.

### Ajuda e Privacidade

Ajuda tem dois caminhos claros: privacidade e suporte. Privacidade abre com garantias antes dos detalhes legais.

### Overlay

As quatro variacoes renderizaram como overlay real via AccessibilityService/debug bridge:

- OTIMA: distinta de BOA por cor e mensagem "Muito acima da meta".
- BOA: leitura objetiva, sem exagero visual.
- ATENCAO: mostra motivo financeiro em ate duas linhas.
- RUIM: comunica queda contra a meta sem texto longo.

O valor principal ja vem tipado, por exemplo `R$ 1,80/km`; por isso labels redundantes de km/hora foram removidos. O tempo manteve o label `tempo total`.

Latencias observadas no log durante a rodada de overlay: 23 ms a 65 ms apos `OVERLAY_SHOW_REQUESTED`.

### Apps fora da Uber

Depois de enviar frame invalido, WhatsApp e Configuracoes abriram sem overlay residual. As capturas tem dados pessoais e devem ser usadas apenas como evidencia local.

## Ajustes feitos durante o QA

- Removidos labels redundantes `por km` e `por hora` do overlay.
- Mantido `tempo total` para o valor de duracao.
- Chip de modo em Metas ajustado de `Prioriza km` / `Prioriza hora` para `Por km` / `Por hora`, evitando quebra em tela pequena.

## Conclusao

PR 10 aprovado visualmente no SM-A075M. A experiencia esta mais direta, segura e escaneavel. O overlay com impacto financeiro continua compacto e legivel nas quatro variacoes.
