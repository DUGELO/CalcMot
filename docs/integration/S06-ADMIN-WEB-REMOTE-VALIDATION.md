# S06 Admin Web — validação remota DEV

2026-10-20, branch growth. Implantação autorizada pelo proprietário; não é produção comercial. UI Angular não publicada.

## Recursos efetivamente implantados

- API: https://calcmot.com.br/api/admin/v1 (base sem recurso retorna404; usar /session autenticado).
- Gateway calcmot-admin-gateway-dev, versão392e110d-1ab8-488a-8ee9-0ff9cba63551, sem D1; ADMIN → calcmot-admin-private-dev.
- Privado versão8524ed18-c710-45c6-ba14-f880780b945f após secret; D1 somente calcmot-identity-dev,4d0c0c71-c606-45a6-a037-43bccef8c05e.
- Dois padrões exatos no dashboard: https://calcmot.com.br/api/admin/v1 e https://calcmot.com.br/api/admin/v1/*, fail-closed. Nenhuma rota global/DNS/calcmot-site/main alterados.
- workers.dev/previews desativados em ambos; probes dos hostnames workers.dev retornam404, não handler administrativo. Fetch do privado sempre403 comprovado localmente; não foi criada URL pública para testá-lo remotamente.
- Owner conferido diretamente na linha do proprietário no Firebase Console, configurado como secret em ambos Workers; nenhum UID/valor do secret consta neste documento.
- Firebase Web CalcMot Admin DEV registrado; config pública em firebase-admin-dev.public.json. calcmot.com.br adicionado aos authorized domains; localhost já existia. Google existente, sem claims/IAM/keys novos; Firebase Spark e Workers Free reconfirmados.

## Migration e integridade

Antes: ledger0001–0008, nenhum índice legacy_admin_*, um grant, quick_checkok.0009 foi a única migration pendente/aplicada. Depois: quatro índices legacy_admin_queue/legacy_admin_state_queue/legacy_admin_evidence/legacy_admin_events, ledger0001–0009 e quick_checkok. SHA256 calculado em memória sobre todas as linhas ordenadas dos grants permaneceu idêntico. Nenhum grant inserido/alterado/excluído. Queries de validação não imprimiram dados pessoais.

## Testes reais executados

| Caso | Resultado |
|---|---|
| Firebase Web Google proprietário → cliente diagnóstico Node → gateway/RPC/D1 | /session200; ownerUID confere e admin verdadeiro; sem imprimir identidade/token |
| Listagem paginada, contagens, inspeção caso DEV existente, eventos e evidências | Todos200; caso previamente concedido GRANTED; nenhuma nova emissão |
| Segunda conta Google real sem admin, SDK Android DEV → USB loopback → cliente API | /session, /claims e caso do proprietário403FORBIDDEN |
| Teste instrumentado existente handoffFreshOwnerSessionToPrivateLocalConsole, expectAccepted=false | OK1test, somente identityDev; não compilou/instalou/alterou código Android |
| Anônimo, bearer inválido, cookie-only/falso header admin |401AUTH_REQUIRED |
| Origin estrangeiro, marcador ausente, cross-site e preflight estrangeiro |403FORBIDDEN; sem ACAO |
| Base exata e subcaminho desconhecido sem sessão |404NOT_FOUND na base;401 no subcaminho, sem resposta privada |
| Recência vencida naturalmente, mesmo token real ainda válido | GET200; tentativa POST403REAUTH_REQUIRED antes de acessar caso/decidir; sem mutação |
| No-store/noindex/ambiente | cache-control:no-store; x-robots-tag:noindex,nofollow,noarchive; x-calcmot-environment:DEV |
| Home e ferramenta pública |200, sem marcador DEV |
| Como funciona, calculadora, privacidade e suporte | Redirecionamento existente para trailing slash, destino200, sem marcador DEV |

Transporte do teste positivo: página temporária loopback com SDK Firebase oficial e login Google; token somente em memória no cliente Node, que chama a API canônica como cliente de diagnóstico com os headers documentados. Isso NÃO é ensaio same-origin Angular publicado, não demonstra CSP/SSR/fallback da UI e não usa CORS de localhost liberado no gateway. Controle negativo usa token Firebase real do Android como credencial da mesma API, não é prova de login Web da segunda conta. Nenhum token em arquivo, URL, screenshot ou logs. Somente resultados sanitizados persistidos.

A suíte local dirigida já passou63casos (12admin+31S06+20S07), mais JWT adulterado focado. Aprovação sem emissão, emissão idempotente, rollback/concorrência e owner sem admin são provas locais sintéticas; NÃO repetidas como mutações remotas. Sem concessão nova autorizada neste ensaio. Expiração natural completa do IDToken e tokens de outro projeto foram cobertos localmente, não fabricados como teste remoto assinado pela Google.

## Custo e limites

Workers plans confirmou Free atual:100mil requests/dia,10msCPU/chamada. D1 Free exibido:5milhões linhas lidas/dia,100mil escritas/dia,5GB total. Banco tinha212992bytes antes dos índices; nenhuma migração para plano pago. Limites locais120/min e10mutações/min por isolate não são globais. Os dois Workers/verificações e D1 consomem cotas compartilhadas; não presumir orçamento independente por endpoint.

Métricas gateway exibiram No data durante a conferência inicial. Valores0ms/0requests dessa tela NÃO são amostra CPU válida. Probes autenticados200 mostram operação sob Free nesta amostra pequena; não comprovam margem CPU/throughput ou cota disponível futura. Sem habilitar Logs/Traces para expor tokens. Aferição CPU representativa continua pendente de telemetria disponível; não habilitar billing automaticamente.

## Rollback e pendências

Remover somente as duas rotas novas pelo dashboard do gateway (Domains → More options da rota → Remove) e eventualmente binding ADMIN. Manter workers.dev/previews desativados. Não remover DNS, custom domain ou deployment de calcmot-site. Falha deve permanecer fechada; não oferecer fallback público administrativo.0009 é apenas índice; não exige reversão de dados/grants. Retirada de recursos ou índices é operação separada, não executada.

Backend DEV remoto funcional para integração. Pendem: publicação UI separadamente autorizada, fallback /admin para index.csr.html/no-store/noindex no Worker+Assets, login/reauth/logout no Angular em origin oficial e CPUedge representativa. Operações reais de revisão/emissão não foram disparadas; não anunciar produção pronta. OQ-07 continua política DEV, S06/S09 mantêm limites operacionais já registrados.

Evidências locais ignoradas: .tmp/admin-deploy/{gateway-routes.png,firebase-web-created.png,firebase-authorized-domain.png,workers-free-current.png,owner-api-success.png,http-smoke.json,owner-auth-results.json,negative-real.json,integrity-final.json,public-final.json}. Screenshots com contexto de conta ficam fora do Git. Nenhuma evidência mostra bearer/secret. Fonte de configuração pública não é chave de serviço.

Encerramento: processos loopback de diagnóstico fechados, tab de Auth temporária fechada e reverse ADB8967 removido. Arquivo temporário do UID conferido removido após gravar secrets; nenhum bearer foi gravado. Prova adicional real-recency.png e resultados sanitizados real-auth-results.json. Logs/Traces Workers permanecem OFF.
