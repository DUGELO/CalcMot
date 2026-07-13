# AGENTS.md - CalcMot

Este arquivo define a metodologia obrigatoria para agentes que implementarem telas, fluxos e ajustes visuais no CalcMot.

O CalcMot e um app Android em Kotlin + Jetpack Compose. A experiencia visual deve seguir o Design System do produto: escura, premium, automotiva, direta, confiavel e fiel aos prototipos enviados pelo usuario.

## Regra Principal

Quando uma tela for baseada em imagem de prototipo, a imagem e o contrato visual.

Antes de editar codigo, o agente deve:

1. Abrir e observar a imagem de referencia.
2. Identificar hierarquia, espacos, cores, tipografia, cards, icones, CTAs, indicadores, barras do sistema e estados.
3. Procurar componentes e tokens existentes no projeto.
4. Implementar usando Compose nativo e Design System, sem transformar o screenshot inteiro em imagem clicavel.
5. Validar visualmente com screenshot real ou preview antes de finalizar.

## Stack Padrao

- Kotlin
- Jetpack Compose
- Material 3
- Design System proprio do CalcMot
- Compose Preview para desenvolvimento visual
- Screenshot/device validation para telas baseadas em prototipo

Nao criar novas telas em XML. Nao usar geracao automatica Figma-to-Compose como fonte final de codigo. Ferramentas de design podem ajudar na leitura, mas a implementacao deve ser limpa, nativa e mantida pelo projeto.

## Design System

Toda tela nova deve usar, ou evoluir de forma controlada, os tokens e componentes do CalcMot:

- Cores de marca: fundo escuro profundo, verde CalcMot, azul de CTA e superficies escuras translucidas.
- Tipografia: hierarquia clara, textos legiveis, sem clipping e sem quebra acidental.
- Shapes: cards e botoes coerentes com os prototipos validados.
- Iconografia: usar Material Icons oficiais ou icones ja aceitos pelo DS. Nao improvisar icones em Canvas quando houver icone Material adequado.
- Componentes: preferir componentes reutilizaveis do CalcMot em vez de estilos soltos por tela.

As cores do prototipo validado sao fonte de verdade visual. Dynamic color do Android nao deve sobrescrever a identidade escura, verde e azul do CalcMot em telas de marca.

## Fluxo Para Criar Telas Por Prototipo

1. Salvar ou referenciar a imagem em `docs/design/references/<fluxo>/`.
2. Criar uma leitura visual objetiva:
   - tamanho e proporcao da tela;
   - estrutura vertical;
   - dimensoes aproximadas de logo, cards, botoes e indicadores;
   - paleta;
   - pesos tipograficos;
   - raios, bordas, sombras e transparencias;
   - estados e textos exatos.
3. Implementar primeiro os componentes reutilizaveis.
4. Montar a tela com slots claros para conteudo variavel.
5. Criar ou atualizar previews Compose.
6. Tirar screenshot no device/emulador quando a tarefa for visual.
7. Comparar screenshot com o prototipo e ajustar ate ficar fiel e responsivo.

Se o usuario disser que "a tela e a mesma e so muda o card", o container, hero, CTA, indicador e ritmo vertical devem permanecer iguais. Apenas o conteudo do slot deve mudar.

## Regras De UI/UX

- A primeira tela deve ser uma experiencia real, nao uma landing page generica.
- Cards nao devem ficar dentro de outros cards.
- Textos nao podem sobrepor, cortar, escapar do container ou quebrar em linhas ruins.
- Botoes devem ter area de toque ampla, minimo 48dp.
- Telas devem funcionar em celular pequeno, celular alto, font scale aumentado e, quando aplicavel, landscape.
- Se a tela precisar rolar, o scroll deve preservar hierarquia e acesso ao CTA.
- Links secundarios so entram quando fazem sentido no fluxo.
- Indicadores de pagina devem ter semantica acessivel, como "Etapa 1 de 3".
- Imagens e logos devem usar assets leves e reais do projeto sempre que existirem.
- Fundos com textura, brilho ou linhas devem ser implementados de forma leve e sem bitmap pesado, salvo quando o asset for explicitamente aprovado.

## Checklist De Fidelidade Visual

Antes de entregar uma tela baseada em prototipo, conferir:

- Logo ou hero correto.
- Textos iguais ao prototipo.
- Quantidade, ordem e conteudo dos cards.
- Icones no estilo correto.
- CTA com label, tamanho, cor e raio corretos.
- Page indicator correto.
- Espacamento vertical e padding dos cards.
- Fundo coerente com a referencia.
- Tratamento de status bar e navigation bar.
- Ausencia de resquicios de telas antigas no fluxo.
- Responsividade sem quebras estranhas.

## Testes E Validacao

Para mudancas visuais:

- Rodar testes relevantes do modulo Android quando viavel.
- Criar ou atualizar testes de UI quando o fluxo ja tiver cobertura.
- Usar screenshot real do device/emulador para validar telas criticas.
- Guardar screenshots temporarios em `.tmp/screens/` quando forem usados para comparacao.

Para builds de entrega:

- `assembleRelease`
- `bundleRelease`
- verificacao de tamanho e permissoes quando solicitado

Se a infraestrutura de screenshot test ainda nao existir para uma tela, o agente deve declarar isso e usar validacao visual por device/emulador.

## Regressao E Seguranca

- Nao fazer refatoracoes nao solicitadas.
- Nao alterar pipeline Uber, pipeline 99, OCR, acessibilidade, manifest, permissoes ou calculos sem pedido explicito.
- O comportamento da Uber e contrato de producao: zero regressao.
- Mudancas compartilhadas devem ser pequenas e justificadas.
- Alterar `versionName` e `versionCode` apenas quando o usuario pedir.
- Nao reverter mudancas existentes do workspace sem autorizacao.

## Como O Agente Deve Trabalhar

- Ler contexto local antes de decidir.
- Implementar com pequenas alteracoes verificaveis.
- Validar no proprio projeto, nao apenas por suposicao.
- Quando houver rejeicao visual do usuario, obedecer ao feedback mais recente e comparar novamente com o prototipo.
- Na resposta final, informar arquivos alterados, validacao feita e qualquer risco restante.

O objetivo nao e criar uma tela "bonita". O objetivo e criar a tela correta para o CalcMot, fiel ao prototipo, responsiva, acessivel e sustentavel dentro do Design System.
