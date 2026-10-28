# S09 — Primeiro login, concorrência e lifecycle

2026-10-15, branch `growth`. Dois diagnósticos direcionados, sem redesign. A única alteração de produção desta execução é o guard de sucesso em `AccountViewModel.kt`, realizada **depois** da reprodução. Nenhuma mudança em S04/S08, contratos, Core ou backend.

## 1. Primeiro login suspenso

Diagnóstico anterior à correção: **3 testes executados, controle legítimo PASS, 2 falhas reproduzidas**. Em ambos os retornos obsoletos, esperado zero consultas e observado uma. XML original: `.tmp/s09-lifecycle/first-login-before.xml`.

| Cenário determinístico | Verificação |
|---|---|
| Primeiro login próprio null→A; sucesso suspenso após autenticar; coletor já observou A antes do retorno | Deve consultar uma vez e preservar sessão válida. Não se usa `sameOwner(null, geração)` como condição. |
| beforeUid null; mudanças externas observadas null→B→A; só depois retorna o sucesso antigo A | Não deve iniciar consulta para o resultado superado. Era aceito pela exceção irrestrita de primeiro login. |
| beforeUid null; sequência observada B→logout→A durante suspensão | Não deve iniciar consulta. Também era aceito antes da correção. |
| B observado, A atualizado imediatamente antes de devolver o sucesso, sem dar turno ao coletor | Sincronizar a geração com a sessão corrente antes de avaliar o guard; não depender de o coletor ter executado. Caso adicional incluído na validação final. |

**Causa:** o branch anterior só aplicava a comparação de geração se beforeUid não fosse null. Conferir apenas UID final não distingue esses ciclos externos.

**Menor correção implementada:** após `identity.refreshSession()`, publicar a leitura corrente para sincronizar titular/geração; conferir UID correspondente ao resultado. Com titular anterior, manter `sameOwner`. No primeiro login, aceitar a geração inicial ou uma única transição observada, correspondente ao caso normal null→UID; múltiplas transições observadas descartam o processamento posterior do resultado. Não concede direitos, não altera Firebase e não desfaz efeitos de uma autenticação já concluída.

**Limite explícito:** contagem local não prova a origem de uma transição. Uma única transição externa para o mesmo UID do resultado pode ser indistinguível da transição própria; B intermediário conflado pelo StateFlow também não é recuperável. Garantia completa requer epoch/correlação de operação na fonte S04, com contrato de captura/retorno apropriado. Não se inventou esse sinal nem se alterou S04 automaticamente. A correção fecha os ciclos observados reproduzidos, não essa lacuna de observabilidade.

## 2. Lifecycle real de navegação

`AccountLifecycleDeviceTest` monta um NavHost Compose com **AccountRoute real** em um NavBackStackEntry real. O ViewModel é obtido pelo store da entrada usando uma factory que falha caso seja necessário criar outra instância: valida que a própria rota o criou naquele store. Não se chama `onCleared` manualmente.

Um Application de observação, fornecido por ContextWrapper apenas no teste, delega os callbacks ao Application real do Android e conta registro, remoção e retomadas efetivamente entregues. Não lê campos ocultos do framework. Reflection restrita às classes do próprio app instala Auth/remote sintéticos no repositório durante o registro, antes do efeito open; o transporte apenas conta e retorna 503 localmente. Não modifica usuários Firebase, cria grants ou chama Worker.

| Estado exercitado | Resultado exigido/comprovado pelo teste |
|---|---|
| Activity vai para STARTED/background | ViewModel continua ativo; callback não removido. Background não equivale a pop da rota. |
| Activity retorna a RESUMED enquanto a entrada existe | Callback é entregue e a consulta local aumenta: controle positivo da medição. |
| Outra rota é empilhada sobre Conta | Mesma instância do ViewModel ainda no store; callback permanece. Entrada retida não foi removida. |
| pop da rota superior e depois pop da Conta, sem saveState | Entrada DESTROYED, onCleared real executado; callback removido uma vez. |
| Depois do pop | Coletores de sessão e direitos chegam a zero; jobs do ViewModel, adaptador e timer do repositório inativos. |
| Activity retorna a RESUMED após remoção | Nenhum novo callback do adaptador, nenhuma nova consulta e nenhuma nova imagem. |

`AndroidEntitlements.close()` remove seu AuthStateListener antes de desregistrar o callback e cancelar escopos. O teste observa a execução completa desse fechamento e o descarte dos próprios coletores/callbacks; não altera usuários reais nem inspeciona a lista privada interna do SDK Firebase.

### Foto em andamento

Segundo caso usa **AccountScreen/GoogleAccountAvatar reais** em NavHost com identidade/foto sintéticas identificadas. Um interceptor Coil suspenso conta uma requisição sem fazer download. Remover a entrada cancela o pedido, deixa zero imagens ativas e retomar a Activity não o reinicia. O ImageLoader de teste é restaurado no teardown. Sem fonte pessoal ou token em evidências.

### O que não se afirma

- Enquanto Conta estiver aberta ou sua entrada continuar retida, o adaptador pode consultar em retomadas; isso não foi mudado e não comprova isolamento de recursos com ofertas.
- O fechamento impede novos inícios pelos callbacks/escopos removidos. Cancelar coroutine não é prova de aborto síncrono de um socket já bloqueado: `HttpsEntitlementRemote` usa HttpsURLConnection em Dispatchers.IO, com connect/read timeout de 10s e disconnect no finally. Não se mediu tráfego físico nem se promete zero bytes imediatamente após pop.
- Cancelamento cooperativo do Coil foi comprovado no interceptor; não é uma medição de todos os transports/packets da plataforma.
- Nada disso exigiu refatorar lifecycle, alterar S08 ou o pipeline crítico.

## Validação e artefatos

- Reprodução inicial JVM: 3 casos, 1 PASS e 2 falhas; produção ainda inalterada naquele ponto.
- Após a correção: **48/48 testes JVM PASS**, zero skips/erros/falhas: apresentação 11, ViewModel 25 (incluindo os quatro casos novos), IdentityCoordinator 10 e erro Firebase 2. XMLs em `.tmp/s09-lifecycle/final-jvm/`, resumo em `jvm-results.json`.
- Primeira execução física no Samsung SM-A075M/Android15/API35: **2/2 PASS**, zero skips/erros/falhas, 10,756s. XML preservado em `lifecycle-before-fix.xml`.
- Execução física final: **2/2 PASS**, zero skips/erros/falhas, **8,872s**; XML em `lifecycle-final.xml`. São dois casos distintos repetidos para validar a versão final/artefatos, não quatro cenários diferentes. Build final `BUILD SUCCESSFUL`.
- Contadores finais `route-lifecycle.json`: registro=1, remoção=1; coletores de sessão/direitos após pop=0/0; jobs ViewModel/adaptador/timer inativos. Depois de retomar a Activity: consultas **1→1**, callbacks **1→1** (zero novas entregas/consultas).
- `photo-lifecycle.json`: requisições **1→1** após retomada, cancelamentos=1, ativas após pop=0.
- Transporte de consulta e imagem são doubles locais explícitos; NavHost, NavBackStackEntry, AccountRoute, ViewModel, adaptador e descarte Compose são reais. Nenhuma chamada externa foi necessária ao diagnóstico físico.

Todos os arquivos de evidência ficam em `.tmp/s09-lifecycle/`, ignorada pelo Git. Não há token, UID real, foto pessoal, compra ou grant nas evidências.

Produção: `AccountViewModel.kt`. Testes: quatro casos adicionais em `AccountViewModelTest.kt`, novo `AccountLifecycleDeviceTest.kt`. [Diff mínimo de produção](S09-FIRST-LOGIN-LIFECYCLE.diff). A V3 não mudou; não houve nova pesquisa visual, alteração de navegação ou configuração de produção.

**S09 PARTIAL**: S06/OQ-07, epoch de identidade na fonte e concorrência de recursos enquanto a Conta existe continuam pendentes. Sem deploy, push, release, grant real ou S10.
