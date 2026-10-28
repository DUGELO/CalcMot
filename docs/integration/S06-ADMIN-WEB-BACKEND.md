# Admin Web — implementação vigente2026-10-20

O proprietário revogou a exigência de Access. Referência de integração: [contrato1.2.0](S06-ADMIN-WEB-CONTRACT.md). Sem Angular/Android alterados.

## Implementação

Gateway Firebase → Service Binding ADMIN → Worker privado → LegacyReviewService existente → D1 DEV. Gateway sem DB; privado sem URL/rota e HTTP sempre403. Removidos access.ts, variáveis Access, JWT/keys/cookies de borda e código/testes exclusivos. Não há flag de bypass nem dependência ausente que cause503 por Access.

Firebase RS256/issuer/audience/validade/ownerUID/admin boolean verificados em ambas as fronteiras. GET autorizado durante token válido; POST exige auth_time<=300s em ambas, além das verificações existentes S06. Perfil/email não autorizam. Tokens já emitidos não têm revogação instantânea por consulta remota nesta arquitetura; allowlist owner e expiração continuam relevantes.

API: listagem paginada/filtrada e contagens no servidor, inspeção/eventos/evidências, complementação/aprovação/recusa e emissão separada. DTOs mínimos, sem UID/token/auditoria técnica sensível. Origin/Fetch Metadata/header obrigatório, sem CORS cross-origin; cookies descartados; no-store/noindex; JSON estrito8KiB, parâmetros validados, revisão/idempotência/confirmação e auditoria S06. Aprovar não emite. Nenhum grant remoto novo.

## Arquivos

backend/src/admin/{contracts,security,strict-json,reads,api,gateway,worker}.ts; backend/tests/admin-web.test.ts; migration0009_admin_read_indexes.sql; wrangler.admin-{gateway,private}.local.jsonc e modelos dev.example.jsonc. Sem alteração de S01, LegacyReviewService ou projector. Trabalho prévio preservado.

## Execução local

Na pasta backend: `npx vitest run tests/admin-web.test.ts tests/legacy-review.test.ts tests/entitlement.integration.test.ts --testTimeout=30000 --hookTimeout=90000 --maxWorkers=1` e `npx tsc --noEmit`.

Wrangler local: `node node_modules/wrangler/bin/wrangler.js d1 migrations apply DB --local --config wrangler.admin-private.local.jsonc --persist-to ../.tmp/admin-web-local`; depois `node node_modules/wrangler/bin/wrangler.js dev --local --config wrangler.admin-gateway.local.jsonc --config wrangler.admin-private.local.jsonc --port 8790 --persist-to ../.tmp/admin-web-local`. Owner UID real somente fora do Git. Nenhuma credencial falsa habilita produção; testes interceptam JWKS sintético apenas no runtime isolado.

## Limites e status remoto

Limites120requests/min e10mutações/min por isolate não são globais. Rate Limiting binding Cloudflare é por localização e eventualmente consistente, não contador global estrito. Um contador D1 global exigiria nova migration/política e consumo de writes; não implementado/contratado. Sem garantia DDoS global. CPU edge ainda não quantificada: painel remoto exibiu No data; valores zero não são medição. workerd local não comprova CPUedge.

Access não é mais bloqueio nem requisito. Mantido como fato histórico: checkout de19/10 exigia pagamento e não foi ativado. Implantação DEV autorizada e executada: Workers/rotas/FirebaseWeb/0009. Prechecks ledger/schema/índices e fingerprint dos grants conferidos antes/depois. Não alterar calcmot-site/main, DNS ou publicar UI. OQ-07 continua política aprovada apenas DEV.

## Validação2026-10-20

63 testes PASS (12admin+31S06+20S07) em runtime local sintético. Workers reais compilados, RPC e D1 local; não apenas mocks. Incluem owner autorizado, anônimo401, usuário comum/outro admin/owner sem claim403, token inválido/expirado/projeto errado, recência de todas as mutações, leitura sem Access, isolamento/paginação, rollback/retry, aprovação sem emissão e emissão idempotente. tsc --noEmit e git diff --check PASS (avisos preexistentes de LF/CRLF). FirebaseWeb real e consultas API remota200 comprovados separadamente do Angular publicado; detalhes em S06-ADMIN-WEB-REMOTE-VALIDATION.md. CPUedge não quantificada.

## Implantação DEV concluída2026-10-20

[Resultado remoto e limites](S06-ADMIN-WEB-REMOTE-VALIDATION.md): Workers/ServiceBinding/duas rotas fail-closed, FirebaseWeb/domínioOAuth e migration0009 efetivamente implantados. OwnerWeb200, conta comum real403 via AndroidDEV, anônimo/inválido401, recência natural GET200/POST403. Grants com fingerprint inalterado. Interface Angular não publicada; nenhum novo grant, billing, IAM, DNS ou deploy de calcmot-site/main. Configuração pública enviada ao agente Angular.
