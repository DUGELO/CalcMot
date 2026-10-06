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


## CalcMot - Contexto Tecnico

Assistente Android local para motoristas avalia ofertas de Uber e 99, calcula rentabilidade e exibe resultados em overlay. O projeto e um modulo Kotlin/Jetpack Compose; UI, AccessibilityService, OCR, calculos e persistencia convivem no `:app`. O codigo atual esta em producao; apenas o dashboard financeiro presente no worktree e trabalho em andamento e nao deve ser tratado como funcionalidade publicada. Documentacao de produto, design, privacidade e entrega fica em `docs/`; artefatos de planejamento ficam em `docs/project-artifacts/`.

## Policy

- Nunca execute ADB, instale APK, altere dispositivo ou publique artefato sem autorizacao explicita.
- Nunca imprima ou versione conteudo de `keystore.properties`, chaves, certificados ou identificadores de configuracao; mantenha material de assinatura fora do Git.
- Trate fixtures e dumps UIAutomator como dados sensiveis; nao adicione nem compartilhe capturas brutas sem origem, consentimento e anonimizacao confirmados.
- Em tarefas de Play Store ou privacidade, confronte codigo, Manifest mesclado, telemetria, OCR e SDKs com os documentos; nao repita declaracoes locais desatualizadas.

## Where Things Are

- UI e navegacao: `app/src/main/java/br/com/calcmot/MainActivity.kt` e `app/src/main/java/br/com/calcmot/ui/CalcMotNavigation.kt`
- Captura Uber/99 e seguranca de foreground: `app/src/main/java/br/com/calcmot/accessibility/UberAccessibilityService.kt`
- Parsing e regras de oferta: `app/src/main/java/br/com/calcmot/processor/`; calculos de negocio: `app/src/main/java/br/com/calcmot/model/`
- Overlay: `app/src/main/java/br/com/calcmot/overlay/`; OCR 99: `app/src/main/java/br/com/calcmot/ninetynine/`; telemetria: `app/src/main/java/br/com/calcmot/telemetry/`
- Configuracoes locais: `app/src/main/java/br/com/calcmot/AppSettings.kt`; ledger e dashboard financeiro WIP: `app/src/main/java/br/com/calcmot/finance/ledger/`
- Entrega, privacidade e validacao em dispositivo: `docs/play-store-submission.md`, `docs/privacy-policy.md` e `docs/e2e-real-device.md`

## Running and Verifying

- No PowerShell, defina `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` antes do wrapper; sem isso o terminal usa Java 8 e o Gradle 9.1 falha.
- `lintDebug` nao esta verde: ha chamadas incompativeis com `minSdk 24`; nao crie baseline nem declare lint aprovado.
- Nao use `scripts/run-real-device-e2e.ps1` sem corrigir ou fornecer o APK: o build gera splits por ABI e o script espera `app-debug.apk`.

## Known Pitfalls

- Codigo e documentacao divergem sobre Firebase, envio de metricas e captura visual da 99; reconcilie antes de qualquer release.
- `android:isAccessibilityTool="true"` diverge da finalidade declarada nos documentos da Play Store; confirme a declaracao aprovada antes de publicar.
- Com `keystore.properties` presente, debug usa a chave release; nao distribua nem instale builds debug sem autorizacao.
- Logs de overlay nao protegidos por `BuildConfig.DEBUG` podem incluir fingerprints de oferta em release.
- Executar tasks `assemble*` e `bundle*` juntas pode mudar os outputs ABI por causa da deteccao global de task no Gradle.
