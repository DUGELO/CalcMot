# ADR-001 — Identidade Firebase Spark e API Workers Free + D1

## Continuação S07 — 2026-10-14

Implementada [projeção local read-only](S07-ENTITLEMENT-PROJECTION.md): GET combina fontes privadas numa SELECT consistente; não escreve snapshot, concede benefício ou chama Play. Migration 0007 mantém revisão persistida na mesma transação das fontes, sem job/fila/cron. `entitlement_snapshots` continua entrada privada para demais fontes; o read model S01 é normalizado em memória. Configuração apenas local `ENTITLEMENT_PROJECTION_ENABLED=true`; remote/prod intactos. DONE local comprovado em D1/workerd, não GO operacional/CPU edge. OQ-07 permanece proposta. As notas de ausência de projeção abaixo descrevem as entregas anteriores S02/S04.

Data: 2026-10-10. Status: **arquitetura aprovada pelo usuário nesta execução**. Escopo S02/S04; nenhum deploy aprovado. Fontes: PRD §§3.2/9/10/11, contratos S01 v1.0.0 e auditoria S03.

O usuário aprovou Google Sign-In com Firebase Auth/Credential Manager, Workers TypeScript e D1, mantendo Spark/Workers Free e custo R$0. As referências a Firestore/Functions no backlog/PRD passam a ter implementação física D1/Worker neste escopo autorizado. A autoridade e os schemas wire permanecem iguais; nenhum contrato congelado foi editado.

- Fontes backend em `backend/`. Projeto Firebase verificado `calcmot`; ambiente de execução nesta entrega exclusivamente local. Não definir realm/DB de produção silenciosamente.
- O D1 usa binding privado, SQL parametrizado e chave `(firebase_project_id, uid)`. Não existe acesso SQL/SDK cliente, nem endpoint para inserir grants/snapshots. Firestore Rules não se aplicam ao D1: a proteção equivalente está na API e na ausência de credenciais D1 no Android.
- Firebase ID token RS256 é verificado via chaves públicas Google, issuer, audience, expiração, iat, auth_time e subject. UID e papéis vêm só dos claims assinados. Nenhuma service account/key privada é necessária para essa verificação.
- `GET /v1/me/entitlement` lê snapshot já autoritativo, validado pelo schema congelado. Ausência/corrupção/inconsistência temporal resulta `503 RETRY_LATER`; não gerar grants nem snapshot de monetização. O Core atual permanece fora desse endpoint. Projeção/emissão de direitos é trabalho futuro S07, não implementado aqui.
- Google login é explícito e opcional. Sem sessão, faz sign-in; sessão existente faz linking ou reautenticação, preservando UID. Credencial pertencente a outro UID gera COLLISION e não inicia merge, sign-out ou transferência. Suporte/recuperação sem acesso à conta continua pendente; não é decidido por email.
- O componente Android não chama o backend nem é inserido em Accessibility/OCR/overlay. Integração de UX de conta pertence a S09. Não habilitar anônimos automaticamente apenas para criar UID.
- Operações admin/internal ficam fechadas, inclusive para tokens Firebase comuns com papéis; nenhum handler financeiro fictício. RBAC booleano validado está preparado e testado; cada operação futura deverá autorizar seu papel/serviço antes de implementar efeitos.
- Sem limites comerciais/rate limits de OQ-06 inventados. Nesta entrega todas as escritas HTTP são recusadas. Política de rate limit e App Check, se aplicável ao ambiente final, devem ser fechadas antes da integração externa de S02; não alegar proteção contra abuso de capacidade apenas por haver autenticação.

Workers Free tem quotas, inclusive 100 mil requests/dia e 10ms de CPU; D1 Free inclui 5 milhões de linhas lidas/dia, 100 mil escritas/dia e 5GB totais. Ao exceder limites, manter indisponibilidade/falha recuperável, nunca migrar automaticamente de plano. CPU real do fluxo autenticado em edge permanece gate antes de deploy; latência local não prova orçamento de CPU. [Workers](https://developers.cloudflare.com/workers/platform/pricing/), [D1](https://developers.cloudflare.com/d1/platform/pricing/).

Firebase Auth está sujeito às quotas do projeto; não habilitar Identity Platform, SMS, Blaze ou faturamento para contorná-las. Chamada de autenticação falha sem restringir Core. [Limites Auth](https://firebase.google.com/docs/auth/limits), [verificação ID token](https://firebase.google.com/docs/auth/admin/verify-id-tokens).

Verificação JWT desta S02 não consulta revogação/desabilitação imediata de usuário: token já emitido pode continuar válido até expiração. Não há operações financeiras ativadas. Antes de adicionar operações sensíveis, decidir/verificar mecanismo de revogação e autoridade de serviço sem custos pagos ou credencial no APK.

## Atualização autorizada — 2026-10-13

O pedido posterior do usuário autoriza deploy exclusivamente no ambiente de desenvolvimento Workers Free/D1 Free, após comprovar custo R$0; a proibição original de deploy descreve a entrega local de 10/10. Produção, domínio/API de produção, releases e upgrades permanecem proibidos. CPU edge autenticada deve ser medida nesse ambiente antes de declarar compatibilidade real/DONE; esse gate não pode impedir o próprio deploy DEV necessário para medir.

Google Auth foi ativado no projeto Spark; pacote Android DEV separado e OAuth real permitem validação sem instalar sobre produção. Worker calcmot-api-dev publicado, D1 calcmot-identity-dev criado/migrado, sem snapshots. Integração Android → Firebase JWT → Worker → consulta própria D1 comprovada; CPU autenticada 5,51ms em uma amostra, sem garantia de carga futura. Configurações/credencial CLI ficam por ambiente, fora do Git; privilégios OAuth da conta só foram concedidos após confirmação explícita. Uso limitado aos recursos DEV, apesar de o escopo de Scripts/D1 ser de conta. Controles operacionais acordados permanecem gate de fechamento/abertura além desta validação DEV. Estado e evidências correntes: [relatório de integração](S02-S04-INTEGRATION.md).

### Continuação — proteção mínima e fixtures autorizadas

Usuário autorizou fixtures sintéticas somente no D1 DEV: teste com handler local e chaves RS256 efêmeras comprovou isolamento A/B em banco remoto, todos os direitos negados; cleanup/restauração da contagem zero comprovados. Não adicionar emissor/chaves de teste ao Worker publicado.

Guard em memória por isolate limita tentativas antes de JWT e leituras por identidade validada antes de D1, sem novos serviços/custo. DEV 120 tentativas/minuto/isolate e 30 leituras/minuto/UID/isolate, memória limitada a 512 titulares; reinícios e janelas limitam eficácia. Exaustão/configuração ativada inválida retorna 503 RETRY_LATER no contrato do GET. Kill switch de leitura não altera direitos nem Core. Não é cota global/distribuída; parâmetros/budget/owner de produção exigem decisão. Binding nativo futuro requer comprovação de custo zero antes de ativação, não foi presumido.

Nova versão DEV 106407db, N=2: P50 1,943ms, P90/P99 14,163ms, zero erros de CPU/runtime. Não declarar margem estável a partir da mediana; documentação permite excessos infrequentes, mas a cauda exige perfil/amostragem antes de abrir ambiente. Nenhum upgrade ou divisão antecipada de serviços. [CPU oficial](https://developers.cloudflare.com/workers/platform/limits/#cpu-time).

SHA-1 Play cadastrado no Firebase principal mediante confirmação específica; configuração candidata e recursos oficiais gerados em saída temporária, sem substituir JSON original ou publicar release. Login de artefato assinado pela Play permanece teste adicional. A arquitetura permite desenvolvimento isolado de grant legado S05, mantendo autoridade server-side; concessões dependem de política/evidência OQ-02 e não podem nascer da ausência de snapshot. S05 não foi implementada.

### Formalização posterior — OQ-02/S05

O proprietário aprovou elegibilidade por uso até 3.5/versionCode 12 inclusive,
com Core dessa versão permanentemente gratuito. Google apenas vincula/recupera;
cadastro durante migração e versão atual não substituem prova de uso histórico.
[Política e inventário](OQ-02-LEGACY-POLICY.md), [fundação S05 PARTIAL](S05-LEGACY-FOUNDATION.md).
Guardas locais adicionadas sem alterar contratos wire ou criar concessões reais:
regra explícita <=12 e fonte histórica privada verificada, preservando registros
anteriores. `policyCutoffUtc` nulo nesta preparação, sem cutoff de calendário
alternativo. Processo de comprovação/revisão e integração real continuam pendentes;
nenhuma infraestrutura ou recurso externo novo, deploy ou push nesta execução.
