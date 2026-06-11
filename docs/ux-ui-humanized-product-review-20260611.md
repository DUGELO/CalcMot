# Spec-Driven UX/UI Review - CalcMot Humanized Product Experience

Data: 11 de junho de 2026

Evidências usadas:
- Captura real do onboarding no SM-A075M: `.tmp/calcmot-ux-audit-app-home.png`
- Dump textual do onboarding: `.tmp/calcmot-ux-audit-app-home.xml`
- Passagem visual tela a tela no SM-A075M:
  - `.tmp/calcmot-screen-home-top.png`
  - `.tmp/calcmot-screen-home-bottom.png`
  - `.tmp/calcmot-screen-home-paused.png`
  - `.tmp/calcmot-screen-drawer.png`
  - `.tmp/calcmot-screen-finance-top.png`
  - `.tmp/calcmot-screen-finance-mid.png`
  - `.tmp/calcmot-screen-finance-bottom.png`
  - `.tmp/calcmot-screen-privacy-top.png`
  - `.tmp/calcmot-screen-privacy-bottom.png`
  - `.tmp/calcmot-screen-diagnostics.png`
- Capturas de overlay da rodada anterior: `.tmp/calcmot-impact_great.png`, `.tmp/calcmot-impact_good.png`, `.tmp/calcmot-impact_warning.png`, `.tmp/calcmot-impact_bad.png`, `.tmp/calcmot-impact-good-opacity-clean.png`
- Código revisado: `HomeScreen.kt`, `OnboardingScreen.kt`, `FinanceScreen.kt`, `PrivacyPolicyScreen.kt`, `CalcMotDomainComponents.kt`, `OverlayDesignSystem.kt`, `MainActivity.kt`

## Passagem tela por tela

### Onboarding / permissao pendente

Evidencia: `.tmp/calcmot-ux-audit-app-home.png`

O que funciona:
- Explica antes de pedir a permissao sensivel.
- Diz que nao toca na tela, nao aceita corridas e nao recusa corridas.
- Mostra acesso a politica de privacidade e suporte.

Problemas:
- Ha dois botoes iguais "Abrir acessibilidade" na mesma tela.
- O titulo "Servico de acessibilidade" e tecnico para usuario comum.
- O texto do disclosure e longo e defensivo, com muitas informacoes em uma unica frase.
- "Concluir" aparece desabilitado sem explicar o que falta de forma humana.

Recomendacao:
- Manter apenas um CTA primario.
- Trocar para "Permitir leitura da oferta".
- Separar a explicacao em 3 bullets curtos: calcula oferta, tudo no aparelho, nao toca na tela.

### Home ativa

Evidencias: `.tmp/calcmot-screen-home-top.png`, `.tmp/calcmot-screen-home-bottom.png`

O que funciona:
- Status "Pronto" esta visivel.
- Switch de pausar/ligar existe e da controle.
- Botao "Abrir app de motorista" e claro.
- Previa do overlay e visualmente boa.

Problemas:
- O CTA principal aparece depois de status, meta financeira e resumo; deveria vir antes.
- "Monitoramento ativo" soa como vigilancia.
- "Atualizar estado" compete com o switch, mas raramente e a acao principal.
- "Meta financeira no centro" e um card conceitual, nao uma acao.
- "Resumo de hoje" com tudo zerado ocupa muito espaco e entrega pouco valor na primeira dobra.
- Posicao do aviso aparece antes de metas/seguranca; e ajuste secundario.

Recomendacao:
- Primeira dobra: estado + explicacao curta + CTA.
- Texto sugerido: "CalcMot em espera. Abra a Uber para analisar ofertas."
- Mover resumo, posicao e previa para secoes abaixo.

### Home pausada

Evidencia: `.tmp/calcmot-screen-home-paused.png`

O que funciona:
- O estado "Pausado" e visivel.
- O switch desligado comunica controle.

Problemas:
- "Ligue o switch" e linguagem de interface, nao linguagem humana.
- O card "Meta financeira no centro" fica vermelho/perigoso quando pausado, mas a meta nao e um erro.
- Ainda aparece "Abrir app de motorista" como CTA primario, mesmo com o app pausado.

Recomendacao:
- CTA primario no pausado deve ser "Ligar CalcMot".
- Texto sugerido: "Pausado por voce. O CalcMot nao vai analisar ofertas ate voce ligar de novo."
- Nao pintar cards conceituais como perigo por causa do estado pausado.

### Menu lateral

Evidencia: `.tmp/calcmot-screen-drawer.png`

O que funciona:
- Simples, poucos itens, boa legibilidade.

Problemas:
- "Financas" mistura metas, custos e lancamentos.
- "Privacidade" e "Suporte" sao importantes, mas falta uma entrada mais humana: "Seguranca".
- "Diagnostico" deve permanecer apenas em debug.

Recomendacao:
- Reorganizar como: Inicio, Metas, Resultado, Seguranca, Ajuda.
- Em debug, manter Diagnostico separado visualmente.

### Financas - topo

Evidencia: `.tmp/calcmot-screen-finance-top.png`

O que funciona:
- Campos de lancamento sao claros.
- Ganho/custo e formulario fazem sentido para diario financeiro.

Problemas:
- A tela abre com "Resultado liquido" zerado e formulario de lancamento, mas a tarefa principal para o overlay e configurar meta.
- "Metas, custos e lancamentos" confirma que a tela tem escopo demais.
- "Impacto no aviso" fica abaixo da dobra, mesmo sendo uma decisao central do produto.

Recomendacao:
- Separar "Metas" como tela propria.
- Jogar lancamentos para "Resultado" ou "Diario".

### Financas - metas

Evidencia: `.tmp/calcmot-screen-finance-mid.png`

O que funciona:
- Presets ajudam.
- Valores por km/hora estao visiveis.
- "Salvar metas" e claro.

Problemas:
- "Presets de meta" e tecnico; melhor "Escolha uma meta rapida".
- "Prioriza km" e "Prioriza hora" exigem raciocinio que pode nao estar claro.
- Campos aparecem antes de uma explicacao curta de como a meta afeta o overlay.

Recomendacao:
- Texto de apoio: "Use sua meta para o CalcMot dizer se a oferta ficou acima ou abaixo do que voce quer ganhar."
- Presets com linguagem: "Comecando", "Equilibrado", "Exigente".

### Financas - custos e historico vazio

Evidencia: `.tmp/calcmot-screen-finance-bottom.png`

O que funciona:
- "Conta da corrida" explica lucro liquido.
- Campos de custo sao reconheciveis para motorista.

Problemas:
- Campos avancados ocupam muito espaco e podem intimidar.
- "Boa R$/km" e "Atencao R$/km" sao configuracoes de classificacao, nao custo do carro.
- A notificacao do WhatsApp apareceu sobre a tela durante captura, o que reforca a necessidade de o app ser robusto visualmente em Android real.

Recomendacao:
- Colapsar "Custos do carro" como avancado.
- Separar classificacao ("Boa/Atenção") da conta de custo operacional.

### Privacidade - topo

Evidencia: `.tmp/calcmot-screen-privacy-top.png`

O que funciona:
- Conteudo honesto e completo.
- Afirma processamento local e ausencia de envio de dados.

Problemas:
- A primeira dobra parece contrato/documento.
- A palavra "Servico de Acessibilidade" aparece novamente em tela comum.
- Os pontos mais tranquilizadores estao diluidos em cards longos.

Recomendacao:
- Adicionar topo com 3 garantias:
  - "Nao toca na tela"
  - "Nao envia ofertas para servidor"
  - "Voce pode pausar quando quiser"
- Depois mostrar detalhes legais.

### Privacidade - final

Evidencia: `.tmp/calcmot-screen-privacy-bottom.png`

O que funciona:
- "O que o app nao faz" e "Controle do usuario" sao excelentes para confianca.

Problemas:
- Esses blocos deveriam aparecer mais cedo, antes da parte documental.
- "monitoramento" ainda aparece em "Controle do usuario".

Recomendacao:
- Subir "O que o app nao faz" para o topo.
- Trocar "pausar o monitoramento" por "pausar o CalcMot".

### Diagnostico

Evidencia: `.tmp/calcmot-screen-diagnostics.png`

O que funciona:
- A tela e explicitamente tecnica.
- O conteudo e util para QA e teste de campo.

Problemas:
- Termos como UIA, arvore, roots, frames e overlay sao inadequados para usuario comum.
- Em debug isso e aceitavel; em release seria P0/P1 de confianca.

Recomendacao:
- Garantir que nunca apareca em release.
- No debug, manter um aviso no topo: "Area tecnica para teste".

### Overlay

Evidencias: `.tmp/calcmot-impact_great.png`, `.tmp/calcmot-impact_good.png`, `.tmp/calcmot-impact_warning.png`, `.tmp/calcmot-impact_bad.png`, `.tmp/calcmot-impact-good-opacity-clean.png`

O que funciona:
- Responde rapido "compensa ou nao".
- R$/km tem boa prioridade visual.
- Impacto financeiro e legivel e curto.
- Opacidade atual melhora leitura sobre a Uber.

Problemas:
- O valor principal ja diz `/km` e o label repete `R$/km`.
- Quando impacto financeiro esta ligado, o overlay fica mais alto; ainda legivel, mas deve ser testado em oferta real.
- `ÓTIMA` e `BOA` precisam de validacao com usuario para garantir diferenca semantica, nao so cor.

Recomendacao:
- Testar label "por km" ou remover label do valor principal.
- Manter impacto em ate uma linha forte + uma linha secundaria.
- Validar em oferta real com motorista.

## 1. Diagnóstico geral

O CalcMot já tem uma base técnica funcional e visualmente mais consistente do que um protótipo comum: existe design system, cards padronizados, estados principais, política de privacidade, metas financeiras, resumo diário e overlay com boa legibilidade. O produto, porém, ainda fala mais como uma ferramenta técnica organizada do que como um assistente simples para motorista cansado.

O maior risco de UX está na primeira experiência: a tela inicial explica acessibilidade com transparência, mas tem texto denso, dois botões iguais para a mesma ação e uma sequência visual que faz a permissão parecer o produto inteiro. Para uma permissão sensível, isso aumenta atrito e suspeita.

A Home pós-permissão, pelo código, tenta fazer tudo ao mesmo tempo: status, resumo financeiro, abertura da Uber, posição do aviso e prévia do overlay. A intenção é boa, mas há excesso de cards e o CTA principal compete com configurações. A pergunta central do motorista, "posso trabalhar agora?", precisa ser respondida antes de qualquer outra coisa.

A tela de Finanças concentra três produtos dentro de uma tela: lançamentos do dia, metas do overlay e conta de custo operacional. Isso é útil, mas intimida. Meta por km e meta por hora precisam ser a experiência principal; custos e lançamentos devem ser secundários ou avançados.

O overlay está no melhor ponto do produto: responde rapidamente se a corrida compensa, principalmente depois dos ajustes de fonte, opacidade e impacto financeiro. Ainda há redundância em labels como valor com `/km` mais label `R$/km`, mas isso é P3, não bloqueador.

## 2. Principais problemas encontrados

- Onboarding com CTA duplicado: `Abrir acessibilidade` aparece no card e novamente logo abaixo.
- Linguagem ainda técnica na superfície comum: "Serviço de acessibilidade", "Atualizar estado", "métricas", "monitoramento".
- Home com muitos blocos antes de consolidar uma ação principal.
- Finanças mistura meta, custo do carro, resultado líquido e histórico de lançamentos.
- Permissões não são apresentadas como passos de benefício e controle.
- Estado "em espera" não aparece claramente; `Pronto` e `Monitoramento ativo` podem sugerir vigilância contínua.
- Diagnóstico debug está bem isolado em debug, mas o drawer ainda pode deixar o produto com cara técnica em builds de teste.
- Privacidade é honesta, mas longa e em cards demais; funciona como documento, não como tranquilizador rápido.
- Falta uma tela ou seção de segurança percebida: onde funciona, onde não funciona, como pausar, bancos/apps sensíveis.

## 3. Problemas P0

### P0.1 - Permissão sensível ainda pode parecer ampla demais

Problema: A primeira tela fala "Serviço de acessibilidade" e pede abertura da acessibilidade duas vezes.

Por que isso prejudica a persona: motorista desconfiado pode entender que o app quer "ver o celular todo", especialmente antes de perceber valor financeiro.

Severidade: P0.

Recomendação: transformar em fluxo guiado de permissão com promessa de controle: primeiro valor, depois segurança, depois ação.

Exemplo melhor: "Permita a leitura da tela da corrida. O CalcMot lê somente a oferta visível enquanto você usa um app de motorista."

Impacto esperado: menos abandono na primeira permissão e mais confiança.

### P0.2 - Falta estado explícito de segurança em apps sensíveis

Problema: O produto diz que processa localmente, mas não mostra de forma simples que não atua em banco, WhatsApp, configurações ou apps sensíveis.

Por que isso prejudica a persona: a maior ansiedade não é técnica; é "esse app está vendo meu banco?".

Severidade: P0.

Recomendação: incluir seção/tela "Segurança" com estados: "CalcMot em espera", "CalcMot ativo na Uber", "CalcMot pausado por segurança".

Exemplo melhor: "Fora dos apps de motorista, o CalcMot fica em espera e não mostra cálculo."

Impacto esperado: reduz medo e aumenta percepção de controle.

## 4. Problemas P1

### P1.1 - Home não tem uma única ação principal

Problema: A Home apresenta status, meta, resumo, botão de abrir app, posição do aviso e prévia do overlay no mesmo fluxo.

Por que isso prejudica a persona: em celular pequeno, o motorista não entende em 5 segundos qual é o próximo passo.

Severidade: P1.

Recomendação: primeira dobra da Home deve ter apenas status, explicação curta e CTA principal.

Exemplo melhor: "CalcMot em espera. Abra a Uber para analisar ofertas." CTA: "Abrir Uber Driver".

Impacto esperado: mais clareza no uso diário.

### P1.2 - Onboarding explica muito antes de orientar

Problema: O texto do disclosure é honesto, mas longo e com muitas negativas em uma frase.

Por que isso prejudica a persona: texto longo em permissão sensível aumenta carga cognitiva e pode soar defensivo.

Severidade: P1.

Recomendação: dividir em 3 linhas curtas: benefício, limite, controle.

Exemplo melhor:
"Calcula R$/km e R$/hora quando aparece uma oferta.
Tudo acontece no seu aparelho.
O CalcMot não toca na tela e não aceita corridas."

Impacto esperado: compreensão mais rápida sem reduzir transparência.

### P1.3 - Finanças mistura configuração e diário de trabalho

Problema: a tela `Finanças` junta lançamentos, meta financeira, custo do carro e histórico.

Por que isso prejudica a persona: meta por km/hora parece "formulário de sistema", não decisão financeira simples.

Severidade: P1.

Recomendação: separar "Metas" de "Resultado do dia". Deixar custos como "Conta avançada".

Exemplo melhor: tela `Metas`: presets primeiro; campos avançados recolhidos.

Impacto esperado: configuração mais rápida e menos intimidadora.

### P1.4 - Termos de estado não refletem o comportamento real

Problema: `Pronto`, `Monitoramento ativo` e `Atualizar estado` são corretos, mas genéricos.

Por que isso prejudica a persona: "monitoramento ativo" pode soar como vigilância.

Severidade: P1.

Recomendação: usar estados semânticos do trabalho do motorista.

Exemplo melhor:
- "CalcMot em espera"
- "Ativo na Uber"
- "Pausado por você"
- "Falta permitir leitura da oferta"

Impacto esperado: maior confiança e menor ambiguidade.

## 5. Problemas P2/P3

### P2 - Excesso de cards

Problema: quase toda informação vira card. Isso organiza, mas deixa a tela pesada.

Recomendação: manter cards para estados e formulários; usar blocos simples para texto auxiliar e atalhos.

### P2 - Badges genéricos

Problema: `Necessário`, `Ativo`, `Pendente` são funcionais, mas pouco humanos.

Recomendação: usar badges contextuais: "Falta permitir", "Pronto para usar", "Pausado".

### P2 - Drawer com navegação pouco orientada a tarefa

Problema: `Finanças`, `Privacidade`, `Suporte`, `Diagnóstico` não refletem as tarefas principais.

Recomendação: `Início`, `Metas`, `Resultado`, `Segurança`, `Ajuda`.

### P2 - Privacidade em formato de documento

Problema: a tela é correta legalmente, mas longa.

Recomendação: adicionar no topo um resumo rápido de 3 garantias antes do texto completo.

### P3 - Overlay com labels redundantes

Problema: `R$ 2,20/km` com label `R$/km` repete informação.

Recomendação: testar label `por km` ou remover o label do valor principal.

### P3 - `ÓTIMA` e `BOA` ainda dependem mais de cor do que de hierarquia

Problema: `ÓTIMA` usa roxo premium, mas a diferença semântica pode ser sutil para leitura rápida.

Recomendação: manter roxo, mas considerar subtexto curto em preview/legenda: "muito acima da meta".

## 6. Nova arquitetura sugerida

### Home

- Status atual: "CalcMot em espera", "Ativo na Uber", "Pausado por você" ou "Falta permissão".
- CTA principal único: "Abrir Uber Driver" ou "Permitir leitura da oferta".
- Resumo do dia em uma frase: "Hoje você analisou X ofertas; Y ficaram acima da sua meta."
- Atalhos compactos: `Metas`, `Segurança`, `Resultado`.

### Onboarding

- Tela 1: valor do app em linguagem simples.
- Tela 2: segurança e limites.
- Tela 3: permissão de leitura da tela da corrida.
- Tela 4: confirmação e abrir Uber.

### Permissões / Segurança

- Passo 1: "Mostrar cálculo por cima da Uber" se aplicável ao fallback.
- Passo 2: "Ler a oferta visível".
- Passo 3: "Manter em espera enquanto você trabalha".
- Bloco de controle: "Pausar CalcMot".
- Bloco de confiança: "Não toca na tela. Não aceita corrida. Não envia dados para servidor."

### Metas

- Presets: `Começando`, `Equilibrado`, `Exigente`.
- Meta por km e meta por hora com explicação curta.
- Avançado recolhido: combustível, manutenção, limites de classificação.

### Resultado

- Resultado líquido do dia.
- Ofertas acima/abaixo da meta.
- Histórico escaneável, sem endereços completos por padrão.

### Overlay

- Classificação.
- R$/km.
- R$/hora.
- Impacto financeiro.
- Tempo total.

## 7. Microcopy recomendada

| Atual | Recomendado |
| --- | --- |
| Serviço de acessibilidade | Permissão para ler a oferta |
| Abrir acessibilidade | Permitir leitura da oferta |
| Atualizar estado | Já permiti, verificar novamente |
| Concluir | Continuar |
| Monitoramento ativo | CalcMot em espera |
| Pode abrir o app de motorista. O aviso aparece quando a oferta estiver completa. | Abra a Uber. O CalcMot calcula quando aparecer uma oferta completa. |
| Monitoramento pausado | CalcMot pausado por você |
| Ligue o switch para voltar a analisar ofertas. | Ligue novamente quando quiser analisar ofertas. |
| Essa permissão permite ler ofertas visíveis e mostrar o aviso. | Permite ler apenas a oferta visível e mostrar o cálculo por cima da Uber. |
| Impacto no aviso | Mostrar impacto na meta |
| Presets de meta | Escolha uma meta rápida |
| Meta R$/km | Quero ganhar pelo menos por km |
| Meta R$/h | Quero ganhar pelo menos por hora |
| Salvar metas | Salvar minha meta |
| Conta da corrida | Custos do carro |
| Resultado líquido | Resultado de hoje |
| Nenhum lançamento ainda. | Nenhum ganho ou custo anotado hoje. |

## 8. Ajustes visuais recomendados

- Reduzir a primeira tela para uma ação principal e uma ação secundária.
- Remover CTA duplicado do onboarding.
- Usar menos cards para textos explicativos; cards devem indicar estado, formulário ou item repetido.
- Dar mais peso visual ao status da Home do que à prévia do overlay.
- Transformar posição do aviso e prévia em seção "Ajustes do aviso", abaixo da dobra.
- Na tela de metas, mostrar presets como botões segmentados ou cards menores; campos avançados recolhidos.
- No resumo diário, trocar grade de métricas por frase financeira principal e métricas secundárias.
- Manter overlay escuro e compacto; a opacidade atual melhora a leitura sobre a Uber.
- No overlay, testar ordem: classificação, R$/km, R$/hora, impacto, tempo. A captura atual já está próxima disso.
- Evitar roxo como cor dominante em todas as ações; reservar roxo para CTA principal e `ÓTIMA`.

## 9. Backlog UX/UI

### Épico 1 - UX Audit

- Mapear telas atuais e remover duplicidades de CTA.
- Documentar estados sensíveis.
- Revisar densidade de cards na Home e Finanças.

### Épico 2 - Microcopy Humanizada

- Reescrever onboarding.
- Reescrever estados da Home.
- Reescrever permissões com foco em benefício e segurança.
- Reescrever metas com linguagem financeira simples.
- Revisar textos de privacidade para resumo rápido.

### Épico 3 - Arquitetura de Informação

- Separar `Metas` de `Resultado`.
- Criar seção `Segurança`.
- Reordenar Home por ação principal.
- Mover prévia/posição do overlay para ajustes secundários.

### Épico 4 - Segurança Percebida

- Adicionar estados "em espera", "ativo na Uber", "pausado por você".
- Explicar apps sensíveis/banco.
- Tornar pausa visível e compreensível.
- Explicar que o app só deve atuar em apps de motorista.

### Épico 5 - Simplificação Visual

- Reduzir cards explicativos.
- Simplificar badges.
- Revisar CTAs concorrentes.
- Ajustar densidade para Android pequeno.

### Épico 6 - Overlay UX

- Testar label `por km` no lugar de `R$/km`.
- Validar `ÓTIMA` vs `BOA` com usuários.
- Validar impacto financeiro em oferta real.
- Revisar intrusão do overlay em telas sem oferta.

### Épico 7 - QA com Persona

- Teste de primeira instalação com motorista.
- Teste de confiança em permissão.
- Teste de decisão em oferta real.
- Teste abrindo banco/WhatsApp para verificar percepção de segurança.
- Teste de leitura do overlay em 1 segundo.

## 10. Plano de implementação seguro

### PR 1 - Microcopy e estados de segurança

Escopo: strings e estados visuais em Onboarding/Home. Não tocar em AccessibilityService, parser, state machine ou cálculos.

Critério: onboarding sem CTA duplicado, termos técnicos removidos da tela comum, estados "em espera/pausado/falta permissão" claros.

### PR 2 - Home e hierarquia principal

Escopo: reorganizar primeira dobra da Home.

Critério: uma ação principal por estado; resumo e prévia abaixo da dobra.

### PR 3 - Permissões e segurança

Escopo: criar seção/tela de segurança percebida.

Critério: motorista entende onde o app atua, onde não atua e como pausar.

### PR 4 - Metas e impacto financeiro

Escopo: transformar metas em experiência primária e mover custos avançados para seção recolhida.

Critério: motorista configura meta em menos de 30 segundos.

### PR 5 - Overlay UX cleanup

Escopo: pequenos ajustes de label e ordem visual, sem mudar cálculo.

Critério: overlay responde "compensa?", "por quê?" e "quanto acima/abaixo?" em 1 segundo.

### PR 6 - QA visual com persona

Escopo: validação no SM-A075M e, idealmente, com motorista real.

Critério: capturas, logs de latência e teste de entendimento sem explicação.

## 11. Definition of Done

- Motorista entende o valor do app em menos de 30 segundos.
- Onboarding tem um CTA principal por etapa.
- Permissão de acessibilidade é explicada como leitura da oferta, não como serviço técnico.
- Home responde claramente se o CalcMot está em espera, ativo, pausado ou sem permissão.
- Existe controle visível para pausar.
- O app explica que não toca na tela, não aceita/recusa corridas e não envia dados de corrida para servidor.
- Metas por km e por hora são configuráveis sem passar por campos avançados.
- Resultado do dia mostra valor percebido sem prometer ganho garantido.
- Overlay mantém leitura em 1 segundo.
- Não há termos técnicos em telas comuns fora de diagnóstico/debug.
- Design system é respeitado.
- `AccessibilityService`, `OfferTreeExtractor`, `OverlayStateMachine`, `OverlayManager`, parser e cálculos permanecem intactos.
