# S06 Admin Web — contrato de integração v1.2.0

## Release administrativa1.3.0 — preparação coordenada PROD, 22/10/2026

Esta extensão foi comunicada ao agente Angular antes de integração. O DEV remoto permanece1.2.0 até rollout separado; esta seção descreve o código preparado1.3.0. Não altera os sete contratos S01 nem autoriza ativação comercial.

- `GET /session`: `contractVersion:"1.3.0"`, `environment:"DEV"|"PROD"`, novo `mutationsEnabled:boolean`; demais campos iguais. `X-CalcMot-Environment` identifica o ambiente em todas as respostas, inclusive erros. O frontend compara o ambiente configurado com corpo e header antes de apresentar dados ou executar ação. Compatibilidade1.2 somente no DEV explicitamente selecionado.
- POSTs enviam `X-CalcMot-Expected-Environment` com o ambiente selecionado. PROD exige valor `PROD`; ausência ou divergência retorna503 `RETRY_LATER` antes de acessar D1, mesmo se a chave de ativação futura estiver habilitada. Impede abas antigas DEV de escrever após cutover. Esse header não concede autoridade: JWT, UID owner, claim admin, Origin, recência300s, revisão e idempotência continuam obrigatórios.
- `ADMIN_ENVIRONMENT=PROD` é explícito nos dois Workers novos. `ADMIN_MUTATIONS_ENABLED=false` mantém todas as mutações bloqueadas503; omissão também bloqueia PROD. Sem essas vars novas, DEV existente mantém comportamento anterior. Vars inválidas são fail-closed. `mutationsEnabled` da sessão é informativo, não autorização frontend substituta do servidor.
- Gateway sobrescreve `X-CalcMot-Target-Environment` no RPC privado; o serviço recusa divergência antes de D1. Esse header é interno e não deve ser construído no Angular. Resposta de serviço com ambiente inesperado é recusada pelo gateway. Bindings/D1 corretos também são validados no preparo/configuração; metadata não atesta magicamente um UUID de banco incorreto.
- Endpoints, payloads de revisão/emissão, erros e paginação permanecem os mesmos. Sem endpoint de bootstrap de política, SQL livre ou novas permissões. Aprovar continua diferente de emitir.

Exemplo sintético1.3 de sessão preparada: `{"contractVersion":"1.3.0","environment":"PROD","mutationsEnabled":false,"authenticated":true,"reauthenticationRequired":false,"reauthenticateBy":"2026-10-22T18:05:00.000Z","serverNow":"2026-10-22T18:00:00.000Z"}`.

Plano de release e rollback: [S06-ADMIN-PROD-CUTOVER.md](S06-ADMIN-PROD-CUTOVER.md). As seções1.2 abaixo registram o contrato DEV já implantado, não uma configuração PROD automática.

Revisão2026-10-20 autorizada explicitamente pelo proprietário: requisito Cloudflare Access revogado. Contrato administrativo1.2.0 remove ACCESS_REQUIRED e dependências de cookies/assertions da borda. GETs permitem sessão Firebase válida; POSTs sensíveis mantêm auth_time<=300s. Tipos e mudança comunicados ao agente Angular. S01 permanece intacto.

Primeira entrega: 2026-10-19, branch growth. Contrato **administrativo novo**, separado dos sete contratos comerciais S01 intactos. Frontend Angular não está sendo implementado neste checkout. Tipos compartilháveis: `backend/src/admin/contracts.ts` (sem dependências de runtime/Cloudflare). Alterações incompatíveis exigem coordenação explícita com o agente Angular antes de integrar; não mudar unilateralmente quando a integração começar.

**Direção vigente do proprietário:** domínio canônico confirmado `https://calcmot.com.br` (www301 para apex), UI `/admin` no Angular oficial, API `/api/admin/v1` independente. Não criar Pages ou segundo frontend. Backend/D1 desta entrega são **DEV/homologação**, mesmo se futuramente roteados no domínio oficial: `/session` inclui `environment:"DEV"`, respostas incluem `X-CalcMot-Environment: DEV`; frontend deve identificar homologação claramente e não apresentar grants sintéticos como produção. Página de login /admin pode ser pública; dados e operações sempre exigem autorização backend. Backend DEV implantado2026-10-20 mediante autorização específica; UI /admin ainda não publicada por esta execução.

## Fronteira e autenticação

Base relativa, no mesmo origin do painel: **`/api/admin/v1`**. Caminho sugerido para a UI: `/admin` (não é endpoint desta API). Nenhum endpoint administrativo é adicionado ao Worker público Android.

Navegador → gateway Worker → Service Binding RPC → Worker administrativo privado → S06/D1 DEV. HTTP privado sempre403, sem workers.dev/previews/routes. Ambos verificam Firebase e owner/admin independentemente. Gateway não possui D1. Nenhum segredo frontend ou dependência da implementação Angular.

Google web usa **Firebase Authentication JavaScript SDK**, `signInWithPopup`/redirect e `getIdToken`, não o Google OAuth ID Token bruto. Enviar `Authorization: Bearer <Firebase ID Token>` em cada request. JWT RS256 verificado com JWKS Google, issuer `https://securetoken.google.com/PROJETO`, audience PROJETO, exp/iat/auth_time/sub e UID owner explícito + custom claim **admin boolean true**. Email não concede autorização. Nada de chave de serviço no frontend.

Usar persistência Firebase em memória/sessão de aba; não persistir tokens manualmente em localStorage, URL, log ou Analytics. API relativa com `credentials:'omit'` recomendado (Angular HttpClient sem withCredentials); same-origin também é aceito, mas cookies não autorizam nada e são descartados antes do RPC. Enviar Bearer Firebase e X-CalcMot-Admin:1. GETs exigem token válido/owner/admin, sem limite de300s. Todo POST exige autenticação recente<=300s em gateway e serviço privado; regras S06 não foram alteradas. /session.reauthenticationRequired indica necessidade para mutações, não bloqueia leitura. getIdToken(true) não renova auth_time: usar reautenticação Google explícita. Cancelamento não dispara mutação automaticamente.

JWT emitido não tem revogação instantânea neste mecanismo. Retirar owner da configuração bloqueia esse UID; retirada de claim exige considerar expiração do token já emitido. Logout browser não é logout global. Leituras podem durar até expiração/renovação válida do Firebase token, sem sessão própria adicional.

## Rotas

| Método e sufixo | Request | Response 200 |
|---|---|---|
| GET `/session` | sem query | `{contractVersion, environment:"DEV", authenticated:true, reauthenticationRequired, reauthenticateBy, serverNow}`; sem UID/email/token |
| GET `/claims` | `limit?`, `cursor?`, `state?`, `protocol?` | `{items:ClaimView[], nextCursor:string\|null, serverNow}` |
| GET `/claims/counts` | sem query | `{counts:{UNDER_REVIEW,GRANTED,REJECTED},serverNow}`; números reais, escopo do projeto |
| GET `/claims/{claimId}` | sem query | `{view, latestDecision:DecisionSummary\|null, grant:GrantSummary\|null}` |
| GET `/claims/{claimId}/events` | `limit?`, `cursor?` | `{items:EventView[],nextCursor,serverNow}` |
| GET `/claims/{claimId}/evidence` | `limit?`, `cursor?` | `{items:EvidenceView[],nextCursor,serverNow}` |
| POST `/claims/{claimId}/evidence` | EvidenceInput S06 + headers abaixo | `{view}` |
| POST `/claims/{claimId}/decisions` | DecisionInput S06 + headers | `{view,decision:DecisionSummary}` |
| POST `/claims/{claimId}/issuance` | `{approvalEventId,confirmation:{claimId,permanentCoreOnly:true}}` + headers | `{view,grant:GrantSummary,idempotent:boolean}` |

`ClaimView`: `claimId`, `state` (UNDER_REVIEW/GRANTED/REJECTED), `openedAt`, `resolvedAt:null|string`, `provisionalUntil:null|string`, `revision` inteiro >=0, `serverNow`. Datas UTC ISO8601. Provisório continua desativado; não apresentar prazo fictício.

`latestDecision` é a última decisão de revisão, **não** a última emissão/evento genérico. APPROVE não altera o state para GRANTED. Sua validade para emissão depende de `toRevision === view.revision`, titularidade, política e demais guardas S06; frontend não decide suficiência. Usar eventId devolvido e nova revisão na emissão.

`EventKind`: EVIDENCE_ADDED, APPROVE, REJECT, REQUEST_MORE, OWNERSHIP_CONFLICT, RESTORE, GRANT_ISSUED. `basis`: NONE, VERIFIABLE_HISTORY, ASSISTED_ASSESSMENT, EXISTING_GRANT. GRANT_ISSUED deriva do recibo S06 persistido; não existe estado comercial novo APPROVED nem contagem fictícia de grants para APPROVE.

## Paginação, filtros e minimização

`limit`: inteiro decimal canônico 1–50, default20. Keyset no servidor, busca de até limit+1; não usar offset nem baixar banco completo. Cursor opaco, limitado e validado, vinculado à coleção, filtros e projeto. Não é credencial. Alterar filtros reinicia paginação. Claims em openedAt/claimId decrescentes; eventos em toRevision/eventId crescentes; evidências em evidenceId crescente. Dataset pode mudar entre páginas: não prometer fotografia transacional de toda a fila.

`state`: enum exato opcional; `protocol`: protocolo **exato**, sem busca livre por email/UID e sem wildcard. Ambos podem ser combinados. Não há pesquisa de pessoas. Campos desconhecidos/duplicados em query/body e cursores incompatíveis retornam400. claimId e referências são identificadores opacos ASCII `[A-Za-z0-9_.:/-]`, 1–128; no path usar somente segmento URL-encoded, sem barra/ponto de navegação.

Não retornar UID do solicitante, email, hashes de idempotência, JWT, grant_json ou actor_uid. Referências de evidência são opacas, não URLs automaticamente navegáveis. Exibir texto escapado, sem HTML, sem buscar preview externo ou incluir documentos civis/dados de corridas. Eventos expõem apenas campos operacionais permitidos; ator é OWNER/SYSTEM. OQ-07 DEV aplica-se às referências, justificativas e auditoria; não criar retenção infinita de payload no frontend.

## Mutações, revisão e retry

Headers obrigatórios: `Content-Type: application/json`, `If-Match: "REVISAO"` (inteiro entre aspas), `Idempotency-Key` opaca 1–128 caracteres. Corpo máximo 8KiB UTF8; não aceitar compressão/redirect/SQL/multipart/uploads.

`EvidenceInput`: kind HISTORY/AUXILIARY/DECLARATION; category SUPPORT_RECORD/INSTALL_CONTEXT/USAGE_CONTEXT/ACCOUNT_CONTINUITY/USER_STATEMENT; originKey/reference; authorityEvidenceId somente HISTORY. HISTORY referencia prova **já autoritativa**, não cria histórico. DECLARATION exige USER_STATEMENT. Validação/suficiência pertencem à S06.

`DecisionInput`: outcome APPROVE/REJECT/REQUEST_MORE/OWNERSHIP_CONFLICT; evidenceIds (até16, sem duplicatas), reason (1–1000), ownershipConfirmed, contradictionsCleared, legacyUseConcluded booleanos. Complementação/recurso usa REQUEST_MORE; conflito não transfere direito. Não há endpoint para editar grant, política, operador ou prazo.

Intenção deve conservar body, If-Match e Idempotency-Key em retry de timeout. Uma resposta pode conter a **view atual** após replay; não presumir que a revisão original continua vigente. Mesma key com payload/revisão incompatível →409. Revisão obsoleta →409; consultar novamente antes de nova intenção. Emissão é separada, atômica e idempotente pelo recibo S06; uma confirmação visual não cria direito. Nenhuma escrita financeira/Premium.

## Erros

Envelope `{code,message,requestId,retryable,serverNow}`. Mensagem PT-BR segura; UI deve usar code, não parsear message. Nunca devolver exceção SQL/claims/token/body.

| HTTP | code | Orientação |
|---|---|---|
|401|AUTH_REQUIRED|Entrar novamente; token ausente/inválido/expirado|
|403|FORBIDDEN|Operador/owner/origin não autorizado ou HTTP privado direto|

|403|REAUTH_REQUIRED|Reautenticar explicitamente; não repetir decisão automaticamente|
|400|INVALID_REQUEST|Parâmetro/header/cursor/body inválido|
|404|NOT_FOUND|Protocolo/rota inexistente no projeto autorizado|
|405|METHOD_NOT_ALLOWED|Método não disponível|
|413|PAYLOAD_TOO_LARGE|Máximo 8KiB|
|415|UNSUPPORTED_MEDIA_TYPE|JSON obrigatório|
|428|PRECONDITION_REQUIRED|If-Match ou Idempotency-Key ausente|
|409|CONFLICT|Revisão, replay ou concorrência incompatível|
|409|GRANT_PRESERVED|Grant existente não será alterado|
|422|MORE_EVIDENCE_REQUIRED / LINK_REQUIRED / POLICY_REQUIRED|Guarda S06; corrigir processo, não contornar no frontend|
|429|RATE_LIMITED|Limite técnico; Retry-After em segundos|
|503|RETRY_LATER|Falha de chave/serviço/banco/configuração; não é ausência de direito|

## Exemplos exclusivamente sintéticos

```json
{"kind":"AUXILIARY","category":"SUPPORT_RECORD","originKey":"synthetic-support","reference":"synthetic:case-support"}
```
```json
{"outcome":"REQUEST_MORE","evidenceIds":[],"reason":"Exemplo sintético: pedir contexto adicional.","ownershipConfirmed":false,"contradictionsCleared":false,"legacyUseConcluded":false}
```
```json
{"approvalEventId":"synthetic-approval","confirmation":{"claimId":"synthetic-case","permanentCoreOnly":true}}
```
```json
{"view":{"claimId":"synthetic-case","state":"UNDER_REVIEW","openedAt":"2026-10-19T12:00:00.000Z","resolvedAt":null,"provisionalUntil":null,"revision":3,"serverNow":"2026-10-19T12:05:00.000Z"},"decision":{"eventId":"synthetic-approval","kind":"APPROVE","basis":"ASSISTED_ASSESSMENT","toRevision":3,"occurredAt":"2026-10-19T12:05:00.000Z"}}
```

## Segurança e implantação coordenada

- Gateway exige origin configurado exato; sem wildcard CORS, ACAO ou OPTIONS cross-origin. POST requer Origin exato. GET pode omitir Origin, mas exige marcador e rejeita cross-site. Cookies não conferem autoridade. RPC valida novamente Firebase/owner/admin.
- Para GET same-origin, browsers podem omitir Origin: frontend envia `X-CalcMot-Admin: 1` e gateway exige esse header, rejeita Origin estranho e `Sec-Fetch-Site: cross-site`. Essa marca **não é autenticação**. Métodos POST exigem Origin exato. Não enviar token do servidor SSR no HTML, não logar headers no proxy. SSR não deve pré-carregar dados administrativos.
- CSRF: Bearer explícito não é credencial ambiente; header obrigatório, Origin/Fetch Metadata e ausência de CORS reduzem abuso browser. XSS ainda pode agir com a sessão: CSP restritiva no site, sem scripts/iframes/análises de terceiros no admin, sem innerHTML, sem token em storage persistente. Rota /admin no mesmo origin compartilha risco XSS com o site; subdomínio isolado é alternativa se a hospedagem não permitir separação suficiente.
- Rate limiting técnico configurável obrigatório por isolate, pré-auth e por owner/mutação após verificação. Sem serviço pago; não é limite global distribuído ou garantia contra DDoS. Valores DEV locais de exemplo não aprovam política de produção.
- Autorização Firebase obrigatória em todas as rotas; sem fluxo Access, Basic Auth, senha própria ou segredo frontend. Shell de login público não pode conter dados administrativos em SSR/SSG; no-store/noindex e proteção de token/XSS continuam necessários.
- Se Angular estiver em Worker/Pages Functions, usar binding RPC no mesmo account; não depende de SSR. Se em hosting externo/estático sem binding, avaliar gateway Worker roteado somente `/api/admin/v1` e `/api/admin/v1/*` no domínio Cloudflare ou subdomínio administrativo com UI+API same-origin. Não expor o Worker interno por URL para “resolver CORS”. Decisão depende da inspeção da hospedagem pelo agente Angular.

Fontes oficiais verificadas: [RPC/Service Bindings](https://developers.cloudflare.com/workers/runtime-apis/bindings/service-bindings/rpc/), [Pages bindings](https://developers.cloudflare.com/pages/functions/bindings/#service-bindings), [Firebase Google web](https://firebase.google.com/docs/auth/web/google-signin), [Auth SDK](https://firebase.google.com/docs/reference/js/auth). Nenhum serviço foi ativado por esse documento.

## Configuração remota conferida2026-10-20

Base `https://calcmot.com.br/api/admin/v1`. Workers gateway/private DEV, Service Binding ADMIN, D1 DEV e0009 implantados. Configuração pública Firebase: [firebase-admin-dev.public.json](firebase-admin-dev.public.json); não é credencial administrativa. calcmot.com.br autorizado para OAuth; localhost já existia. Não inicializar Analytics no painel. HTTPS/bearer/owner/admin continuam autoridade; teste do shell Angular no domínio oficial depende do corte UI separado.
