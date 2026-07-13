# Pipeline de implementacao de telas por prototipo

Este e o fluxo padrao para transformar os prototipos de `docs/design/references/calcmot-prototypes-2026/` em telas reais do CalcMot.

## 1. Selecionar

- Escolher o proximo arquivo do indice canonico.
- Abrir a imagem antes de qualquer edicao.
- Registrar qual tela/estado sera implementado.

## 2. Ler o prototipo

Extrair uma ficha visual objetiva:

- tipo de tela: estado, configuracao, lista, formulario, sucesso, erro;
- estrutura: topbar, hero, cards, CTA, links, rodape;
- textos exatos;
- hierarquia tipografica;
- espacamento vertical e padding;
- cores de destaque;
- icones e estilo;
- elementos que precisam rolar em telas menores.

## 3. Mapear para o Design System

- Usar tokens de `CalcMotColors`, `CalcMotTypography`, `CalcMotSpacing` e `CalcMotShape`.
- Reutilizar componentes existentes antes de criar novos.
- Criar componente novo apenas se ele virar padrao reutilizavel.
- Usar Material Icons oficiais quando houver equivalente.

## 4. Implementar em Compose

- Criar a tela em arquivo proprio quando o fluxo crescer.
- Separar tela, cards e linhas internas.
- Manter textos reais para acessibilidade.
- Nao usar screenshot inteiro como UI.
- Nao alterar pipeline Uber/99/OCR/calculo/permissoes se a tarefa for apenas visual.

## 5. Integrar com cuidado

- Adicionar rota/destino minimo para acessar a tela.
- Preservar comportamento existente.
- Se a tela for apenas um estado futuro, manter callbacks isolados para ligar depois.

## 6. Validar

- Criar ou atualizar preview Compose.
- Rodar build/testes relevantes.
- Para UI visual, capturar screenshot no device/emulador quando disponivel.
- Para prototipos em desenvolvimento, usar a `PrototypePreviewActivity` debug-only e abrir a tela por `adb` sem depender de permissoes ou fluxo interno.
- Comparar contra o prototipo e ajustar por evidencia visual, nao por palpite.

## 7. Fechar

Na resposta final, informar:

- prototipo usado;
- arquivos alterados;
- validacao executada;
- diferencas conhecidas ou pendencias.

## Sequencia inicial

1. `01-feedback-enviado.png` - implementada e aprovada em device.
2. `02-home-permissao-necessaria.png` - implementada e validada em device.
3. `03-premium.png` - implementada e validada em device.
4. `04-historico-vazio.png` - implementada e validada em device.
5. `05-ajuda.png` - implementada e validada em device.
6. `06-home-pronto-calcular.png` - implementada e validada em device com CTAs separados para Uber e 99.
7. Seguir o indice de `docs/design/references/calcmot-prototypes-2026/README.md`.
