# CalcMot Admin PROD — ativação operacional

27/10/2026. Este documento complementa o registro histórico de publicação somente leitura em `S06-ADMIN-PROD-LIVE.md`.

## Autorização vigente

O proprietário respondeu expressamente “Sim, habilitar operações reais em PROD” à confirmação de substituir a restrição de somente leitura e habilitar evidências, decisões e emissão de LegacyGrant, com reautenticação, auditoria e confirmação individual. Isso autoriza configurar a operação; não autoriza criar um grant durante a ativação.

## Sequência e limites

1. Conferir versões, bindings e D1 PROD, preservando DEV e grants existentes.
2. Registrar somente a política `legacy-through-3.5-vc12` pelo script privado existente, mediante sessão Firebase Google legítima do proprietário, allowlist, claim administrativa e recência de 300 segundos.
3. Conferir recibo e reler D1: uma política, zero grants, zero solicitações artificiais.
4. Habilitar `ADMIN_MUTATIONS_ENABLED=true` no privado e depois no gateway, mantendo as duas rotas existentes, sem novo endpoint público nem D1 no gateway. Frontend lê a capacidade efetiva em `/session`; não cria autoridade por configuração.
5. Validar sessão e controles negativos, sem emitir direitos nem inserir casos artificiais em PROD.

## Canal privado sem USB

`backend/scripts/prod-policy-session.mjs` escuta somente `127.0.0.1:8971`, durante dez minutos. Seu link abre a interface oficial com um nonce de uso único no fragmento, nunca um Firebase token. A interface remove o fragmento, verifica o desafio, pede confirmação explícita e reautenticação Google. O SDK envia o token somente em memória ao receiver local, com Origin exato e nonce. O receiver valida assinatura Firebase e autorização real do proprietário na API PROD; o bootstrap existente repete as verificações e recebe o token exclusivamente por stdin.

Sem token em argumentos, logs, clipboard ou arquivos. O único arquivo de resultado contém recibo não pessoal, versão, data e `grantsWritten=0`. O receiver encerra após consumo ou timeout. `/challenge` não concede autoridade nem persiste dados. Nenhum endpoint público de bootstrap é criado.

## Validação realizada antes da ativação

- Oito testes do receiver: uso único, Origin/nonce/UID declarado, TTL/body/content type, concorrência, falha sanitizada, CORS/PNA, desafio e Host contra DNS rebinding.
- Vinte testes administrativos/PROD: incluindo workerd/RPC e D1 local, isolamento, reautenticação, aprovação sem emissão, emissão idempotente e ativação explícita simulada em banco local. CI remoto completo: 142 testes Vitest aprovados, um teste remoto mutante desativado, oito testes Node aprovados, typecheck e dois dry-runs aprovados.
- Typecheck aprovado. Os 18 arquivos runtime conferidos contra o manifesto inicial não mudaram; nenhum pipeline Android foi alterado.
- D1 remoto antes da operação: nove migrations, quatro índices, integridade OK, zero violações FK e zero registros nas oito categorias de negócio.

## Rollback de ativação

Restaurar primeiro a versão readonly do gateway `1e18bfd2-c6a5-46c6-97c9-64d99ea0afed`, depois a versão readonly privada `2913aabd-a653-433e-9863-d9ffa0103f64`. Não remover a política, os bancos, grants ou migrations. Isso fecha novas escritas mantendo consultas e direitos persistidos. Não transferir rotas para DEV em um rollback limitado a capacidades comerciais.

## Resultado remoto

Ativação concluída em **28/10/2026**, após confirmação explícita do proprietário e conferência da autorização posterior que revogou a restrição readonly. Uma rejeição automática baseada na restrição antiga foi resolvida mediante leitura da resposta humana original; nenhuma autorização ou verificação de identidade foi contornada.

### Código e publicação

- Backend PR5 integrado exclusivamente em `growth`: merge `c8490e8b20bb6ada9af86b613381d4b3974a6f94`, head validado `8292dc3bffcbf1c85e816b32cb000ed6dad6c491`. O merge não publicou Workers.
- Frontend PR11: merge `6dd578a0cf3e1eb5da8335d6e95070a44e9cc016`.
- Correção frontend PR12: merge `bb5687ad543bf98214c33d6aa7a4bde655d061e9`; head `b1d605117bc3aead911dfba9f57c67bf87b14c07` com QA, Lighthouse, preview e regressão Router/browser aprovados. Fluxo automático original preservado.
- Site ativo `a47e0b09-4e88-49c7-a65c-6729461290af`, deployment `db798cc3-c79b-49ac-b7b0-5e17c6159956`, 100%.
- Privado PROD ativo `f266dcde-3eff-421a-9c93-d62fc25a73dd`, 100%, D1 exclusivamente PROD.
- Gateway PROD ativo `2e40165b-98f9-40a6-af5e-7de0cd2bebb2`, 100%, sem D1, binding somente para o privado PROD.
- Ambos efetivamente com `ADMIN_MUTATIONS_ENABLED=true`, owner secret preservado e sem mudança IAM/claims. As duas rotas oficiais foram preservadas; nenhum trigger ou DNS novo.

### Política efetivamente registrada

Fluxo legítimo comprovado: login Google → intenção explícita na aba autenticada → desafio local validado → confirmação humana → reautenticação Google → script privado existente → registro D1. A resposta da ferramenta foi incerta após a persistência; não se repetiu a escrita. O recibo foi recuperado por releitura restrita dos metadados da política, conferindo versão, baseline, critérios, limite vc12, evidências aceitas e ausência de grants. Essa recuperação não fabricou uma avaliação nem um registro histórico.

- Política: `legacy-through-3.5-vc12`.
- Baseline: `calcmot-core-3.5-vc12`.
- Registro: `2026-10-28T14:03:44.942Z` (11:03:44, Brasília).
- Operação: `policy-6ceb947c6333efb3e98551b9`.
- Recibo verificado em `2026-10-28T14:05:25.381Z`, com `grantsWritten=0` e `recoveredByReadback=true`.
- Artefato privado sanitizado: `backend/.tmp/admin-prod-release/policy-receipt.json`.

### Verificação operacional real

- Novo login do proprietário após a ativação: interface exibe **“Produção · Operações autorizadas pela sessão. Emissão exige confirmação individual.”** Consultas de fila e contagens reais funcionam; banco sem solicitações artificiais.
- Logout removeu dados administrativos da interface; relogin recuperou sessão operacional.
- Anônimo e token inválido: HTTP401 `AUTH_REQUIRED`. Origin indevida e marcador ausente: HTTP403 `FORBIDDEN`. Respostas PROD, no-store/noindex. O controle com conta comum Google foi comprovado na homologação anterior; a regra owner/admin não foi alterada nesta ativação.
- URLs workers.dev e URLs de versão de ambos Workers retornaram404 sem resposta administrativa. Metadado de existência de preview no artefato de versão não foi confundido com roteamento público; inacessibilidade foi testada por HTTP.
- `/admin`200 PROD/no-store/noindex, sem dados administrativos em SSR. Calculadora e sitemap200; `/admin` ausente do sitemap. As seis páginas e o SEO passaram no CI do frontend; verificações públicas remotas preservadas.
- D1 PROD: nove migrations, quatro índices, quick_check OK, zero violações FK, uma política e zero claims/grants/events/issuances/evidence/historical_use/snapshots. Nenhum grant real ou de teste foi criado.
- DEV: um grant original, fingerprint privado inalterado, integridade OK. Sem importação DEV→PROD.
- Sem cartão, upgrade, billing, recursos novos ou alteração Android/Core/S01. Recência300s, idempotência, revisão vigente, auditoria e aprovação separada da emissão permanecem obrigatórias.

Captura real: `.tmp/screens/admin-prod-live/dashboard-prod-operational-2026-10-28.png`. Evidência sanitizada das versões: `.tmp/admin-prod-operational.json`, verificada em `2026-10-28T14:12:06.016Z`.

**Status: painel administrativo PROD operacional, com capacidades reais habilitadas.** O registro inicial da política foi concluído e não é parte da rotina do operador. A validação desta liberação não criou um caso ou grant comercial apenas para teste. A emissão futura depende de uma solicitação legítima, análise, aprovação e confirmação individuais.
