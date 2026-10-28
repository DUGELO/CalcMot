# S09 — Auditoria final da V3 integrada

**Diagnósticos finais — 2026-10-15:** primeiro login suspenso corrigido após reprodução; 48 testes JVM PASS e 2 testes físicos de NavBackStackEntry/lifecycle PASS. Remoção real da entrada encerra ViewModel/adaptador e não inicia novas consultas/fotos ao retomar a Activity. Epoch na fonte e I/O já em andamento continuam limites explícitos. [Evidências e diff mínimo](S09-FIRST-LOGIN-LIFECYCLE.md). S09 PARTIAL; V3 preservada.


2026-10-15, branch `growth`, commit local `226fe22f` com alterações preexistentes preservadas. V3 aprovada; nenhuma nova direção visual. Escopo: auditoria, ensaio DEV e correção pequena de sessão autorizada pelo proprietário durante a execução. Sem mudança de S01–S08, Core ou produção.

## Parecer arquitetural

| Verificação | Resultado |
|---|---|
| Uma composição funcional | `AccountScreen` usa `AccountContent`; não há AccountConcept/ConceptPage/A/B/V3 em `main/account`. Testes renderizam a mesma composição; histórico arquivado. |
| Menu tipado/previsível | `AccountActionId`, `AccountActionSection`, lista `accountActions`; visibilidade depende da sessão, logout desabilitado durante operação. `groupBy` preserva ordem de primeira ocorrência dos grupos e ordem dentro de cada grupo; não é ordenação global de itens intercalados. |
| Despacho exaustivo | `when(AccountActionId)` sem `else` no Screen; mapeamento visual exaustivo no Content. Adicionar enum requer tratar visual/ação, detectável pelo compilador. |
| UI e integrações | Content/Screen não chamam Firebase nem endpoint. Avatar delega carregamento de imagem ao Coil existente; não é uma segunda API de identidade. State/status recebidos determinam apresentação, não grants/eligibilidade/expiração. |
| Coordenação | ViewModel pequeno, StateFlow único de apresentação, Mutex de intents, dados derivados da sessão e do repositório. O espelho `request` é o resultado da solicitação ainda não projetado; `currentUid`/geração são guards, não uma segunda autoridade de identidade. |
| Reutilização | Route mantém factory local, lifecycle-aware collection, suporte e Google existentes. Sem novo Manager/Coordinator, DI, módulo, registry ou provedor. |
| Operações | Auth em S04; direitos/projeção/cache em backend/S07/S08; S09 coordena intents e mostra fatos. Menu não concede autorização por renderização. |

### Correção autorizada nesta auditoria

**P2 — sucesso tardio após A→B→A observado:** o branch de sucesso do login verificava somente UID. A geração podia ser diferente e ainda provocar uma nova consulta depois da operação ter sido superada. Reprodução: duas mudanças observadas, conclusão da operação original; esperado 0 requests, encontrado 1. Corrigido somente em S09: sucesso de uma sessão previamente existente também exige a geração capturada. Primeiro login continua aceitando a transição legítima de desconectado para autenticado. Nenhuma concessão/transferência de direitos foi constatada.

**switchMap solicitado pelo proprietário:** utilizado o equivalente Kotlin Flow `flatMapLatest`, sem adicionar LiveData/RxJava. Sessão seleciona a observação corrente do StateFlow de direitos; troca cancela a observação anterior, `buffer(0)` evita fila intermediária. Substitui os dois coletores anteriores. Não se cancela arbitrariamente a autenticação S04 nem se promete desfazer efeitos já concluídos; os guards de UID/geração continuam necessários.

### Limitação que permanece

**A→B→A não observado:** StateFlow pode confluir o estado intermediário B. A geração S09 é derivada das mudanças que chegam ao observador; sem um epoch/evento de transição fornecido pela fonte S04 não há como reconstruir uma mudança que ela não entregou. Reprodução diagnóstica inicial: troca externa B→A no mesmo turno, seguida de Failure; esperado NONE, encontrado CANCELED. O switchMap também depende das emissões recebidas e não inventa esse sinal. Não se alterou S04 para disfarçar esse limite. As intents normais da UI permanecem serializadas pelo Mutex; essa reprodução usa mudança externa à coordenação S09. Não há evidência de leitura de dados de UID B por A ou concessão indevida; permanece lacuna para a garantia geral de feedback por geração em mutações externas rápidas.

## Extensões localizadas

- **Novo item:** acrescentar enum/entrada em `AccountPresentation`, ícone/semântica em `AccountContent`, despacho/destino explícito no Screen/Route e seu teste. Nenhuma alteração em Firebase, cache ou classes de outra funcionalidade. Remover é retirar a entrada; reordenar é mover a entrada respeitando seus grupos. Não há intenção de adicionar recursos agora.
- **Novo login futuro:** implementar a integração autorizada na fronteira S04 e ligar a intenção no Route; compor a ação pelo slot `AccountSignInContent`. UID é identidade, provider/perfil são metadados. Não vincular por email, não inferir benefícios e não criar um segundo AuthManager. Google é o único provedor implementado.

## I/O e ofertas

Inspeção dirigida das referências de conta/entitlement nos diretórios de acessibilidade, OCR, overlay e runtime: nenhum vínculo novo encontrado. O processamento não chama AccountRoute nem a API de conta.

**Concorrência é possível, ausência de disputa não está comprovada:** entrar em Conta inicia consulta; foto pode carregar no mesmo período. Enquanto AndroidEntitlements estiver vivo, seus ActivityLifecycleCallbacks podem iniciar restore/refresh em retomada de Activity. Ser Dispatchers.IO não isola CPU, rádio, memória ou sockets do processo. Não há produtor confiável de drain integrado ao runtime; SafeEntryObservation permanece UNKNOWN sem produtor. Não inferir segurança de IDLE, toggle ou ON_RESUME. Não há medição física desta concorrência em identityDev, cujo host deliberadamente não inicia o Core. Mantida pendência de integração/medição com autorização própria, sem modificar pipeline ou S08.

## Integração real e acessibilidade

### Testes automatizados e correção

- Diagnóstico inicial dirigido: **3 casos, 1 PASS e 2 falhas reproduzidas**. Sucesso antigo com UID B continuou corretamente descartado; A→B→A observado gerou 1 consulta indevida; B→A conflado mostrou CANCELED. XML preservado em `.tmp/s09-final-audit/diagnostic-results.xml`; fonte dos diagnósticos em `diagnostic-tests.kt`.
- Correção autorizada e regressão permanente `auditSuccessfulLoginAfterObservedABAReturnMustBeDiscarded`: **PASS**. Teste de diagnóstico de transição não observável ficou arquivado, sem deixar teste deliberadamente falhando na suíte ativa. Isso não significa resolução da limitação.
- Versão final com `flatMapLatest` + guard: **44/44 testes JVM PASS**, zero falhas/erros/skips: AccountPresentation 11, AccountViewModel 21, IdentityCoordinator 10, erro Firebase 2. `assembleIdentityDev` PASS. XMLs em `.tmp/s09-final-audit/final-jvm/`; resumo em `final-jvm-results.json`.
- Não se repetiu a suíte instrumentada visual anterior (34 casos distintos), pois nenhuma composição, token, navegação ou texto mudou nesta auditoria. As regressões anteriores continuam evidência histórica, não novos testes físicos desta rodada.

### Ensaio real no Samsung

Somente `br.com.calcmot.identitydev`, Samsung SM-A075M, Android15/API35, conta já autorizada. Nenhuma conta alternativa selecionada, nenhuma desinstalação ou alteração do pacote principal.

1. Conta desconectada abriu sem solicitar login automaticamente. Login iniciado por botão oficial, seletor nativo Google, seleção da conta autorizada e conclusão real.
2. Nome/email correspondentes à conta selecionada e “Conta Google conectada” confirmados. Foto real fornecida pelo provedor carregada e inspecionada, sem substituir por fixture.
3. Meu acesso mostrou “Não conseguimos consultar agora”, com conta Google conectada e sem grant/Premium inventados. Retry preservou essa distinção. O probe DEV já existente, com a mesma sessão e endpoint, confirmou **HTTP 503 RETRY_LATER** com requestId. O status V3 corresponde ao mapeamento 503→BACKEND_UNAVAILABLE do adaptador S08; nenhum token foi extraído para teste externo.
4. Logout V3 removeu nome/email/foto/status conectado. “Continuar sem conta” retornou ao host DEV. Reiniciar o processo confirmou “Sem sessão” no Firebase local; não é logout de produção.
5. APK final contendo `flatMapLatest` e guard instalado por atualização somente no DEV. Relogin pela V3 concluído na mesma conta; nome/email/foto/estado confirmados novamente. Retry permaneceu sem concessão fictícia.
6. Também foi repetido **logout e relogin na versão final com flatMapLatest**, com remoção/restauração correta de nome/email/foto e estado conectado.
7. Reinício do processo, com sessão vinculada, recuperou **a mesma impressão digital local do UID**, comparada em memória pelo probe existente, sem exportar UID bruto. A igualdade é evidência de continuidade do titular nesta conta; não é teste com dois usuários reais.

Capturas com perfil real ficam somente em `.tmp/s09-final-audit/`, ignorada pelo Git. `real-results.json` registra apenas resultados sanitizados. Nunca incluir token, UID bruto, foto/email real ou URL pessoal no relatório/diff/repositório. O Core foi preservado por inspeção e testes anteriores; não se iniciou análise real de oferta neste host DEV isolado.

### TalkBack

**Ensaio assistido real confirmado pelo proprietário:** TalkBack Samsung temporariamente habilitado, binding e exploração por toque ativos confirmados por dumpsys; o servidor auxiliar mobilecli identificado foi encerrado para não suprimir o serviço. Primeiro o proprietário confirmou o nome anunciado seguido de “Título”; isso corresponde ao `heading()` do perfil, não altera o nome. Em seguida confirmou que **leu todos os campos e navegou corretamente** no percurso solicitado (nome, email, conta conectada e ações). Essa é confirmação humana do ensaio, não inferência de configuração.

Além disso, captura `talkback-connected-focus-2.png` mostra foco no item “Consultar meu acesso”. A ativação temporária foi removida ao terminar, preservando outros serviços; ausência de TalkBack na lista final corresponde à ausência na lista inicial deste ensaio. Não foi alterado serviço ou permissão do CalcMot.

Cobertura limitada à tela/percurso de Conta descritos; não é auditoria completa WCAG, de todas as páginas, todos os estados ou dispositivos. Nenhuma alteração visual foi necessária.


## Diffs e baselines

- `S09-V3-INTEGRATION.diff`: quatro arquivos de composição/estado V3 contra as cópias preservadas imediatamente antes da integração, incluindo as correções atuais.
- `S09-KOTLIN-WORKSPACE.diff`: pacote account e vínculos Kotlin de navegação/identidade contra HEAD `226fe22f`, incluindo arquivos novos não rastreados. **Acumulado**, contém alterações já existentes do trabalho anterior; não é prova de autoria exclusiva da V3 e não deve ser reaplicado cegamente ao workspace atual.
- `S09-FINAL-AUDIT-FIX.diff`: alteração de produção feita nesta auditoria, comparada com a entrada desta execução.

**S09 PARTIAL:** S06/OQ-07, intake/recuperação real, limite de transição externa não observada e concorrência de I/O continuam pendentes. O percurso assistido TalkBack da Conta foi comprovado, sem alegar cobertura geral de acessibilidade. Sem S10, deploy, push, release, faturamento ou grant real.
