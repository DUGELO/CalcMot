# S09 — Conceitos de conta Google para aprovação

**Atualização vigente — 2026-10-15:** V3 ajustada aprovada pelo proprietário como referência e integrada à composição funcional existente, sem dependência dos protótipos. Conta prioriza identidade; Meu acesso distingue consulta indisponível, estado desconhecido e resposta válida sem grant com linguagem neutra. Direito permanente confirmado oferece detalhes e não nova recuperação. Menu tipado, sessão/feedback isolados por titular e geração. **S09 PARTIAL** por operação S06/OQ-07 e limites registrados. [Auditoria, arquitetura e evidências correntes](S09-V3-INTEGRATION.md). As notas de conceitos/aprovação pendente abaixo são histórico anterior a esta autorização, não o status vigente.


Início: 2026-10-14. Retomada: **2026-10-15**. Branch `growth`. **S09 PARTIAL; direção visual ainda NÃO APROVADA.**

## V3 — ajustes finais solicitados, sem aprovação de integração

Em 2026-10-15, o proprietário indicou V3 como **direção visual preferida**, sem aprová-la integralmente ou autorizar integração. Composição preservada; não foi criada V4.

- Entrada: texto “Entre com Google para acessar sua conta no CalcMot.”; logo, wordmark, Google Sign-In e continuação opcional mantidos.
- Conta conectada: o badge verde de autenticação foi substituído por linha discreta com ícone de conta e texto secundário. Não usa a cor de sucesso para representar login. Perfil compacto, componente existente de foto/fallback e lista/saída separados permanecem iguais.
- Indisponibilidade: “Não conseguimos consultar agora” + “Sua conta Google continua conectada. Tente novamente mais tarde.”. Retry textual secundário e recuperação somente no link terciário de 48 dp.
- Estados adicionais reutilizam a mesma superfície e hierarquia; somente conteúdo, ícone semântico e ação contextual mudam. Verde de sucesso aparece no ícone de **direito legado confirmado**, não na autenticação nem na consulta desconhecida. Wordmark verde continua sendo marca, sem significado de benefício.

### Verdade dos estados e fixtures

As fixtures usam `EntitlementState` existente e passam pelo **mesmo `accessPresentation` funcional, sem alterá-lo**, antes de renderizar. Cada teste verifica a classificação esperada e `proAllowed=false`. Não são snapshots obtidos de servidor nem concessões comerciais reais.

| Cenário | Modelo existente / classificação conferida | Linguagem / tratamento V3 |
|---|---|---|
| Serviço indisponível | `BACKEND_UNAVAILABLE` → `UNAVAILABLE` | Mensagem humana acima, Info neutro, retry; nunca ausência de direito. |
| Legado confirmado | `READY`, revisão 1, `legacyCore=true`, `legacyState=GRANTED` → `LEGACY` | “Acesso gratuito permanente”; ícone VerifiedUser verde. Funcionalidades gratuitas até 3.5 preservadas, sem assinatura; recursos posteriores não incluídos automaticamente. Atualização de status secundária. |
| Verificação não concluída | `EMPTY`, sem revisão → `AWAITING` | “Acesso ainda não verificado”; ícone de espera neutro, convite “Consultar acesso”. Não simula request em andamento ou resposta do backend. |
| Resposta sem grant confirmado | `READY`, revisão 1, `legacyCore=false`, `legacyState=UNKNOWN` → `NO_CONFIRMED_GRANT` | “Acesso antigo ainda não confirmado”; ícone Search neutro e “Não encontramos um direito antigo confirmado nesta conta.”. Não afirma que o motorista é novo, nunca usou versão antiga ou perdeu o Core. |

Distinção alinhada ao schema S01 (`legacyCore` corresponde a `legacy.state=GRANTED`) e ao read model S08. Nenhuma mudança no contrato, Auth, mapper, entitlement ou Core. Os testes negativos impedem o título de acesso permanente nas fixtures desconhecidas/indisponíveis. A foto continua opcional pelo componente existente; capturas sintéticas com foto ausente não comprovam integração real ou uma foto Google carregada.

São seis telas/estados da mesma V3 (entrada, perfil e quatro variantes de Meu acesso), em fonte normal e 1,5×. A/B e a primeira V3 permanecem em seus diretórios originais.

**Resultado físico em 2026-10-15:** Samsung SM-A075M / Android 15 API 35, pacote `br.com.calcmot.identitydev`. **12/12 testes específicos V3 PASS**, zero falhas/erros/skips; 22,732 s de testes, build dirigido em 2 min 27 s. Apenas os 12 métodos V3 foram executados; sem repetir suites gerais ou A/B. XML preservado em `.tmp/s09-v3-final/results.xml`.

As 12 capturas PixelCopy da janela Compose real (720×1600) foram inspecionadas em pares normal/1,5×. Não se observaram cortes, sobreposição ou perda de ações. Fonte ampliada foi aplicada no teste, sem alteração global do Samsung. A mensagem de autenticação neutra continua legível; permanente confirmado difere por ícone verde e explicação de escopo; espera, ausência de confirmação e indisponibilidade têm ícones/textos distintos, sem sucesso verde. Ações e recuperação continuam alcançáveis. Não foi necessário redesenho ou correção adicional após renderizar.

Novos arquivos em `.tmp/screens/s09-concepts/v3-adjusted/`: 12 PNGs originais `concept-V3-*.png`, seis comparações normal/1,5×, quatro visões de conjunto e manifesto SHA-256. [Galeria completa com originais](../../.tmp/screens/s09-concepts/v3-adjusted/GALERIA.md). As composições só posicionam capturas originais lado a lado, sem modificar o conteúdo das telas. Diretórios `previous/`, `final/` e `v3/` anteriores preservados; nenhum dado pessoal real.

Renderer `androidTest` e documentação são as únicas alterações desta passagem. Os 213 arquivos protegidos de produção/contratos/backend/configuração continuam com os mesmos hashes. Foto real não exercitada: fixture sem imagem mantém o fallback do componente existente. Não houve alteração de Auth, entitlement, Core, navegação ou contrato; sem integração ao produto, grants reais, deploy, push ou release. **S09 PARTIAL; V3 é preferida, mas ainda aguarda aprovação visual explícita e autorização de integração.**

## V3 — híbrido solicitado, ainda para avaliação

O proprietário não aprovou A ou B integralmente. Solicitou apenas três novos protótipos, mantendo os anteriores. **V3 não é uma escolha automática nem integração funcional.** Nenhuma nova pesquisa, fonte ou biblioteca nesta etapa.

| Página V3 | Base preservada | Refinamento concreto / identidade CalcMot |
|---|---|---|
| Desconectada | Composição pessoal central do A; Google Sign-In dominante e continuação opcional. | Removida a superfície ao redor do botão. O avatar genérico foi substituído pelo asset real `calcmot_logo_hero`; título “Minha conta” e wordmark branco/verde itálico usam o tratamento já existente em `HomeScreen.DrawerWordmark`. Explicação curta, botão oficial e continuação sem conta próximos, sem repetir benefícios ou criar promessas. |
| Conectada | Perfil compacto e lista direta do B. | Wordmark discreto identifica a área CalcMot; perfil usa o componente existente `GoogleAccountAvatar`, nome/email e `CalcMotStatusBadge` de conexão. Consulta e ajuda em lista plana; saída em grupo separado por espaço/divisor. Nenhum card grande, métrica, credencial verificada ou CTA de login após conexão. |
| Informações de acesso | Superfície intencional do A e ações curtas do B. | Não repete nome, email ou avatar. Conexão Google confirmada em badge semântico verde; consulta desconhecida em superfície neutra com ícone Info e texto curto. Retry textual/contextual dentro da superfície, sem botão azul grande. Suporte em linha; recuperação somente no link terciário de 48 dp perto do fim. |

Os elementos reutilizados foram lidos/observados no próprio projeto antes da edição. O logo é um asset de marca existente; não foi criado fundo com gradiente ou ilustração. Não se modificaram tokens, tipografia global, assets nem componentes de produção. A centralização/ritmo pessoal do A deriva do estudo anterior Nubank; a organização por tarefa do B deriva das referências Uber/99 já documentadas abaixo. A V3 consolida essas decisões, sem novo discovery.

### Foto Google e verdade do estado

O protótipo aceita `AccountProfilePhoto?` e reutiliza **o mesmo `GoogleAccountAvatar`** já preparado na S09, que apresenta imagem quando recebe metadata disponível e fallback em ausência/falha. Não cria uma segunda integração Auth ou um carregador paralelo.

As capturas usam `Alex Santos / alex@example.test` e **foto ausente**, portanto mostram o fallback real do componente. Não consultamos a conta Firebase real nem inventamos uma foto Google. Os testes V3 verificam a presença do fallback e a ausência de imagem carregada nessa fixture. A validação de foto real permanece distinta e não é comprovada por esses renders. Os estados de autenticação e indisponibilidade de consulta também são fixtures explícitas, não integração remota.

Verde indica somente a conexão autenticada representada na fixture. Consulta de direitos indisponível permanece neutra/desconhecida; nenhum benefício ou direito legado é inferido, revogado ou apresentado como confirmado. Não há protocolo ou versão 3.5 como argumento de login.

### Execução e comparação V3

Seis métodos dirigidos `v3*` adicionados ao renderer existente: três páginas em fonte normal e as mesmas três em 1,5×. A/B e seus arquivos PNG finais permanecem preservados; não é necessário reexecutar os 12 casos A/B. Comparação planejada: entrada V3 × A; conta conectada V3 × B; acesso V3 × A/B para conferir superfície e hierarquia do retry.

**Validação física em 2026-10-15:** Samsung SM-A075M / Android 15 (API 35), pacote separado `br.com.calcmot.identitydev`. **6/6 testes PASS**, zero falhas/erros/skips; 12,804 s de testes e build dirigido em 3 min 23 s. Executados somente os seis métodos `v3*`, selecionados pelo runner; não repetidos A/B, suites gerais ou testes de Auth/entitlement. XML: `.tmp/s09-concepts-v3/results.xml`.

As três páginas normais e as três variantes 1,5× foram capturadas por PixelCopy da janela Compose real e inspecionadas, além das comparações com A/B. Fonte ampliada aplicada somente no teste, sem alterar preferência global. Textos sem clipping/sobreposição nos seis renders; logo e wordmark legíveis, botão oficial Google com continuação abaixo, perfil compacto com saída separada, conexão semanticamente distinta da consulta, retry textual e recuperação terciária. A inspeção não exigiu nova correção após esta execução.

| V3 — captura original 720×1600 | Comparação efetivamente inspecionada |
|---|---|
| `concept-V3-signed_out.png` | A × V3: mesma entrada pessoal; removida a superfície de login e introduzidos logo/wordmark existentes. |
| `concept-V3-signed_in.png` | B × V3: mesmas tarefas, com cabeçalho CalcMot, componente real de avatar/fallback, estado semântico e saída separada. |
| `concept-V3-information.png` | A × B × V3: removida a repetição de perfil, conexão isolada da consulta e retry secundário dentro da superfície. |

Diretório: `.tmp/screens/s09-concepts/v3/`; seis originais incluindo sufixos `-font150`, três comparações, hashes em `evidence-manifest.json`. [Galeria V3 e links dos originais](../../.tmp/screens/s09-concepts/v3/COMPARACAO-V3.md). As montagens somente colocam os PNGs originais lado a lado com rótulos externos. A/B preservados em `final/`; nenhuma imagem pessoal no repositório.

Arquivos desta etapa: renderer existente `S09AccountConceptDeviceTest.kt` estendido com V3 e seis métodos, este documento e notas de status em S09/BACKLOG/EXECUTION-PLAN. Os 213 arquivos protegidos de produção/contratos/backend/configuração foram conferidos por hash sem mudanças. Não houve alteração do componente de avatar, Auth, ViewModel, navegação, cache, Core, contrato ou gate. A foto Google real não foi exercitada; a fixture com foto ausente demonstrou o fallback existente. Testes/capturas não comprovam comportamento funcional, integração remota ou revisão real S06/OQ-07.

**S09 permanece PARTIAL e V3 visualmente não aprovada.** A/B também continuam sem aprovação integral. A próxima alteração funcional depende da aprovação explícita do proprietário; nada foi integrado, publicado ou escolhido automaticamente.

## Direção determinada pelo proprietário

1. Google Sign-In opcional é a ação principal da Conta desconectada. Continuação sem conta é clara e discreta.
2. Após login, perfil e estado da identidade Google são protagonistas. Consulta de acesso é complementar; sua falha não significa falha do login.
3. Recuperação de versões antigas é terciária. Não aparece como card, hero, CTA principal ou etapa obrigatória na Conta. O caminho legível “Já usava o CalcMot antes? Saiba mais” fica somente em Conta e acesso. A página específica pode continuar oferecendo suporte/ação contextual após entrada voluntária.

Esta orientação substitui a composição anterior que destacava recuperação no mesmo nível da conta. Não altera a elegibilidade comercial, a gratuidade existente nem os estados S01. Não cria novos benefícios.

## Referências efetivamente observadas nesta revisão

| Origem e URL | Observação visual | Adaptação / limites |
|---|---|---|
| Nubank — [capturas do modo escuro, TechTudo, 2023](https://www.techtudo.com.br/dicas-e-tutoriais/2023/05/como-usar-o-nubank-em-modo-escuro-edapps.ghtml) | Imagem publicada de 1000×856 aberta no navegador integrado: identidade no topo, linhas com ícone/título, separação por áreas, saída de menor destaque. | A: perfil vertical, ritmo tipográfico e uma superfície discreta para ações relacionadas. Não copiar roxo, produtos financeiros, navegação, banners nem alegar que a captura representa a versão atual. |
| Uber Driver — [menu de Conta em material oficial](https://www.uber.com/br/pt-br/blog/motorista-parceiro-compartilhe-sua-viagem/) | GIF oficial aberto e inspecionado no navegador: perfil primeiro, lista linear de ações curtas e saída separada. É material antigo, sem comprovação de data/versão atual. | B: identificação compacta, poucas linhas previsíveis, sem painel de status. Não copiar mapa, dados de corridas, ganhos, documentos ou navegação inferior. |
| 99 Motorista — [material oficial de configuração](https://motoristas.99app.com/99-moto-educacao/) | Imagem de 500×981 inspecionada: lista de veículos, seleção e confirmação contextual. A imagem encontrada não é uma tela de conta. | Somente linguagem direta e confirmação ligada à ação. Não usar o fluxo de veículos como composição de Conta, nem alegar inspeção de um perfil 99 que não foi visto. Sem amarelo, veículos, dashboard ou diálogo comercial. |
| CalcMot — tokens e telas existentes | `CalcMotTheme`, `CalcMotSpacing`, cores, tipografia e capturas S09 anteriores examinados. | Ambos mantêm fundo `#04060A`, superfícies escuras, azul de ação e verde pontual de conexão. Tipografia global preservada; Google Sans somente no botão oficial com asset Google existente. |

### Complementos investigados antes da interrupção

Na retomada não houve nova pesquisa, navegação ou download. Reutilizamos as observações e o conteúdo já obtidos:

| Referência | Tipo de evidência realmente obtida | Aplicação / limite |
|---|---|---|
| [Nubank — Brand Refresh, 10/06/2026](https://blog.nubank.com.br/brand-refresh-como-renovamos-a-identidade-visual-do-nubank/) | Documentação oficial lida: hierarquia da paleta, consistência, distinção entre tipografia de destaque/leitura e continuidade da experiência. Não é screenshot de Conta. | A usa títulos de identidade e leitura em ritmos distintos com a tipografia CalcMot existente. Superfícies têm tarefa definida. Isso é adaptação de design, não cópia de NuSans, roxo ou alegação de reproduzir uma tela do Nubank 2026. |
| [Uber Driver — guia oficial](https://www.uber.com/us/en/drive/driver-app/) | Texto oficial lido, seção Menu: perfil, ajuda, configurações e recursos organizados por tarefa. A abertura da imagem de Menu em domínio de mídia foi recusada pela revisão automática antes da interrupção; **essa imagem não foi inspecionada**. | B: perfil compacto seguido de verbos/tarefas diretamente acionáveis. Não criar configurações, documentos, métricas ou funções que o CalcMot não oferece. |
| [Uber — captura histórica de Conta no EntreCourier, 2022](https://entrecourier.com/delivery/delivery-business/money-and-bookkeeping/uber-eats-pay-stubs/) | Screenshot 541×1024 aberto e visto no navegador: título Account, ícones à esquerda, rótulos curtos e lista sem cards de resumo. Fonte editorial histórica, não documentação oficial nem UI atual comprovada. | B usa uma coluna linear de ações com separadores, sem descrição antes de cada tarefa. Conteúdo financeiro, fundo branco e navegação não são copiados. |
| [99 Motorista — guia oficial](https://99app.com/motorista/guias/guia-motorista-parceiro/) | Texto lido, especialmente “Conhecendo o menu do app”: acesso por nomes reconhecíveis e Ajuda/Suporte. Não foi obtida captura de Conta dessa página. Outra imagem promocional de 99Abastece foi vista, mas não é screenshot de Conta e não é usada como contrato visual. | B: “Consultar meu acesso”, “Ajuda e suporte”, “Sair da conta”. A familiaridade é uma inferência de design apoiada na organização textual; não se afirma que o guia contém esse layout CalcMot. |

Figma Community e Now in Android permanecem referências complementares documentadas em [S09-VISUAL-REFERENCES.md](S09-VISUAL-REFERENCES.md), sem autoridade sobre a identidade do CalcMot. Nenhum arquivo Figma foi criado/alterado nesta revisão.

## Conceitos comparáveis

| Página | A — perfil com acabamento inspirado no Nubank | B — conta compacta com usabilidade Uber/99 |
|---|---|---|
| Desconectada | **Composição pessoal central:** avatar → “Minha conta CalcMot” → explicação curta; superfície única reúne Google Sign-In e continuação opcional. Suporte é link separado. O olhar percorre identidade → decisão de entrar. | **Entrada operacional em coluna:** estado “Conta Google / Não conectada” → frase de valor → botão Google; continuação logo abaixo, suporte em linha. Sem painel de entrada nem hero central. O olhar percorre estado → ação. |
| Conectada | **Identidade central e gestão por atalhos:** avatar/nome/email/conexão formam o resumo pessoal; dois atalhos lado a lado levam a Meu acesso e Ajuda. Saída fica separada e discreta. | **Lista de tarefas:** perfil compacto no topo, conexão junto da identidade e três linhas de ações em sequência: consultar, obter ajuda, sair. Sem tiles/cards. |
| Conta e acesso | **Resumo vinculado à pessoa:** título “Seu acesso no CalcMot”; uma superfície reúne titular, autenticação e consulta de acesso, com separação interna e retry textual. Ajuda e link de versões antigas fora do resumo. | **Consulta como tarefa:** perfil resumido, seção “Consultar acesso”, linha curta de estado, explicação e ação contextual azul. Ajuda em linha e link terciário de versões antigas ao fim. |

### Onde a inspiração aparece, página por página

- **A desconectada:** a distinção entre título de identidade e corpo curto aplica os princípios tipográficos do Brand Refresh, usando somente a fonte CalcMot. A superfície reúne a decisão de autenticação, sem acrescentar benefícios. A centralização é uma inferência de composição própria, não uma medida extraída da captura Nubank.
- **A conectada:** a captura Nubank estabelece perfil antes das áreas de conta. Aqui isso vira um resumo pessoal central seguido de dois atalhos com funções reais. Os atalhos lado a lado são uma solução CalcMot; não são cópia de produtos bancários. Verde informa conexão, não verificação documental ou elegibilidade.
- **A informações:** a separação por grupos da captura Nubank é aplicada ao resumo de uma única conta. Conexão confirmada e consulta indisponível aparecem como informações distintas dentro dele. Um benefício confirmado exigiria composição afirmativa própria, sem o texto de indisponibilidade; não foi criado um estado comercial fictício para preencher o conceito.
- **B desconectada:** rótulos curtos e percurso em coluna vêm da lista observada na captura Uber. Sem texto introdutório extenso, a entrada Google é a primeira tarefa; continuar sem conta permanece explícito.
- **B conectada:** o texto oficial Uber organiza o menu por tarefas; a captura histórica demonstra a lista linear. A adaptação usa apenas três ações disponíveis no CalcMot, com perfil compacto, sem métricas ou cards de ornamento. O guia 99 apoia nomes reconhecíveis e suporte acessível, não dimensões de UI.
- **B informações:** a consulta vira uma tarefa com estado sucinto e próximo passo imediato. O botão azul é retry, não compra ou recuperação. O link de versões antigas fica ao fim, com alvo de 48 dp e texto legível, sem usar o peso de uma ação principal.

As diferenças são de **modelo de composição e percurso visual**, não somente de padding/avatar: pessoal central + atalhos/resumo no A; estado compacto + lista de tarefas/ação contextual no B. Ambos mantêm as mesmas capacidades e o mesmo caso de indisponibilidade.

Os dois usam **o mesmo cenário de exemplo**, para comparar solução visual sem trocar a hierarquia do produto: identidade conectada e consulta de acesso indisponível. Não há HTTP 503 na linguagem pública, ausência de direito inferida, Premium/grant fictício, preço ou compra. `Alex Santos / alex@example.test` é identidade sintética; iniciais representam o fallback de avatar, não uma foto Google real.

## Isolamento da implementação funcional

`S09AccountConceptDeviceTest.kt` contém os protótipos exclusivamente em `androidTest`, usando o host DEV existente. Não entra no APK publicado nem substitui `AccountScreen`, ViewModel, navegação, Auth, cache ou backend. Ações são posições visuais de protótipo; não efetuam login, logout, envio ou retry real. Isso não é evidência de integração funcional dos conceitos.

As alterações preliminares da foto de perfil, iniciadas antes da correção de direção, foram preservadas. A execução que as validaria foi interrompida; não se deve tratá-las como concluídas ou como aprovação visual. A implementação funcional aguarda a escolha explícita do proprietário.

## Validação e aprovação

As 12 capturas e o XML anteriores foram preservados em `.tmp/screens/s09-concepts/previous/` e `.tmp/s09-concepts/previous-results.xml` (12 testes, zero falhas/erros/skips; 25,515 s). Elas documentam a composição inicial, ainda próxima demais entre A/B, e não são os renders finais refinados. `.tmp/s09-concepts/initial-concepts.kt` foi preservado; a versão exata encontrada na retomada também foi copiada para `resumed-before.kt`.

### Resultado final — Samsung DEV, 2026-10-15

**12/12 testes instrumentados PASS**, zero falhas, erros ou skips, **21,950 s** de testes; build dirigido concluído em 2 min 20 s. Apenas a classe `S09AccountConceptDeviceTest` foi executada. Não foram repetidas suites gerais de Auth, entitlement ou Core. XML preservado em `.tmp/s09-concepts/final-results.xml`.

Samsung **SM-A075M / Android 15 (API 35)**, pacote `br.com.calcmot.identitydev`. Seis estados renderizados com fonte normal e os mesmos seis com `fontScale=1,5` por `CompositionLocalProvider`, sem alterar a configuração global do aparelho. Capturas **PixelCopy da janela real do host Compose**, 720×1600 px; não são imagens geradas, wireframes ou login real. Os protótipos não acionam Auth, claim ou entitlement. As iniciais são fallback sintético; não foi usada foto pessoal.

| Página | Original A | Original B |
|---|---|---|
| Conta desconectada | `concept-A-signed_out.png` | `concept-B-signed_out.png` |
| Conta conectada | `concept-A-signed_in.png` | `concept-B-signed_in.png` |
| Informações / consulta indisponível | `concept-A-information.png` | `concept-B-information.png` |

Diretório final: `.tmp/screens/s09-concepts/final/`. Os seis arquivos adicionais com sufixo `-font150` registram a fonte ampliada. [Galeria comparativa e links dos originais](../../.tmp/screens/s09-concepts/final/COMPARACAO.md); versão HTML offline ao lado. As três montagens `compare-*.png` apenas posicionam duas capturas originais lado a lado com rótulos externos, sem editar o conteúdo. Hashes dos 12 originais em `evidence-manifest.json`. Arquivos temporários ignorados pelo Git; sem dados pessoais reais.

**Inspeção efetiva:** seis telas normais comparadas lado a lado e seis de fonte ampliada examinadas. Hierarquia do login/perfil distinta entre A/B; nenhum texto sobre versão antiga na Conta; estado indisponível restrito à consulta; conexão Google continua explícita. CTA Google legível, sem botão Google depois de conectado; suporte e link terciário permanecem alcançáveis. Não foram observados sobreposição, cortes ou truncamentos nas capturas finais. Ícones e cores continuam CalcMot/Material; nenhuma nova fonte ou biblioteca foi obtida nesta retomada.

Dois problemas detectados na primeira passagem renderizada foram corrigidos **somente no teste**: atalhos do A com alturas distintas em fonte 1,5 (agora alinhados por altura intrínseca e assertion de bounds) e barra inferior cinza do host (agora estilo de barras escuro com insets seguros). A primeira passagem, também 12/12 PASS (21,608 s), foi preservada em `inspection-pass/`; o êxito dos testes não foi usado para ignorar os defeitos visuais.

**Proteção da implementação:** 213 arquivos de produção/contratos/backend/configuração monitorados por hash, zero alteração nesta retomada. Somente o protótipo `androidTest` e a documentação foram refinados. Não houve integração de A/B à rota funcional, alteração de Auth/ViewModel/cache, rede no Core, grant, deploy, push ou release. Não foi escolhida uma solução vencedora.

**Limites:** estes renders demonstram composição no aparelho e acessibilidade de leitura/rolagem nos estados testados; não comprovam comportamento funcional dos novos layouts, TalkBack completo ou recuperação real S06/OQ-07. Após a escolha, a integração funcional exigirá autorização e validação própria. S09 continua PARTIAL e visualmente não aprovada.

**Aprovação pendente:** escolher A, B ou ajustes concretos. Nenhuma escolha será inferida de build/teste bem-sucedido. Gates comerciais OFF; S06/OQ-07 e recuperação real continuam pendentes.
