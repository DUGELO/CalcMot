# Admin Web — confirmação final sem Access

2026-10-20: proprietário revogou Cloudflare Access e autorizou implementação/testes locais. Autorização remota explícita recebida; alvos implantados após prechecks. Confirmações action-time de domínio OAuth e duas rotas também recebidas. Sem publicação da UI ou alteração do Worker público.

- calcmot-admin-gateway-dev: somente https://calcmot.com.br/api/admin/v1 e https://calcmot.com.br/api/admin/v1/*; Service Binding ADMIN, nenhum DB, workers.dev/previews desativados.
- calcmot-admin-private-dev: somente RPC, HTTP403, sem URLs/rotas/previews; D1 calcmot-identity-dev,4d0c0c71-c606-45a6-a037-43bccef8c05e.
- 0009_admin_read_indexes.sql: quatro índices aditivos, apenas após ledger/schema/índices/grants e ensaio sintético. Não modificar grants/migrations anteriores.
- Firebase calcmot: Web CalcMot Admin DEV e domínio calcmot.com.br somente se necessário; sem novas claims/IAM. Owner existente fora do Git.
- Firebase JWT válido/ownerUID/admin em ambas fronteiras; leituras com token válido, mutações<=300s. Contrato1.2.0 coordenado com Angular.
- API terá entrada pública autenticada; Worker interno privado. Nenhuma garantia de bloqueio de borda Access é anunciada. UI login /admin pode ser pública, dados somente autorizados, sem SSR/SSG privado; publicação UI separada.
- Não alterar calcmot-site/main, DNS, UI Angular, grants ou produção; nenhum cartão/billing.

Rollback: retirar apenas as duas novas rotas e binding; site público/grants permanecem. Índices0009 não exigem rollback destrutivo de dados; eventual remoção de índices é operação separada. Falha de gateway não deve fazer fallback com dados privados.

Checkout Zero Trust de19/10 é histórico: não ativado, não é mais requisito/bloqueio. Sem implementação substituta de identidade própria. Migração/publicação/login web real/CPU edge ainda pendentes.

## Execução efetiva

0009 aplicada, quatro índices presentes, quick_checkok e fingerprint do único grant inalterado. Ambos Workers publicados, owner secret a partir de UID diretamente conferido no Firebase Console. Gateway somente duas rotas fail-closed confirmadas pelo dashboard; Wrangler não possuía zone-read/route-write e não teve escopos ampliados. FirebaseWeb e domínio oficial cadastrados, Spark/WorkersFree preservados. Consultas autenticadas reais200; negativos anônimos/inválidos401 e boundaries403. Consulte [validação remota](S06-ADMIN-WEB-REMOTE-VALIDATION.md) para limites dos testes.
