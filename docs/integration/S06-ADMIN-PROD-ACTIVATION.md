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
- Dezenove testes administrativos/PROD: incluindo workerd/RPC e D1 local, isolamento, reautenticação, aprovação sem emissão, emissão idempotente e ativação explícita simulada em banco local.
- Typecheck aprovado. Os 18 arquivos runtime conferidos contra o manifesto inicial não mudaram; nenhum pipeline Android foi alterado.
- D1 remoto antes da operação: nove migrations, quatro índices, integridade OK, zero violações FK e zero registros nas oito categorias de negócio.

## Rollback de ativação

Restaurar primeiro a versão readonly do gateway `1e18bfd2-c6a5-46c6-97c9-64d99ea0afed`, depois a versão readonly privada `2913aabd-a653-433e-9863-d9ffa0103f64`. Não remover a política, os bancos, grants ou migrations. Isso fecha novas escritas mantendo consultas e direitos persistidos. Não transferir rotas para DEV em um rollback limitado a capacidades comerciais.

## Resultado remoto

Ainda não executado neste registro. Preencher com recibo real e versões efetivamente ativas; não inferir ativação pelo sucesso de testes locais.
