# Admin Web — homologação final no Angular publicado

Data:2026-10-22. Escopo: testes pelo navegador em https://calcmot.com.br/admin, backend DEV; nenhuma concessão nova. Sem alteração de contratos/Core/Android/infraestrutura. Evidências de API isolada de20/10 não são contadas novamente como testes do Angular.

## Comprovado nesta sessão

- Login Google voluntário do proprietário, concluído pelo usuário, abre visão geral e fila reais:0em análise,1direito emitido,1recusado.
- Caso DEV já concedido mostra revisão5, direito emitido e avaliação concluída.
- Evidências:2referências AUXILIARY sintéticas, identificadas como auxiliares, sem virar prova histórica verificável; campos de seleção desabilitados no caso encerrado. Não existe upload/abertura automática de URLs.
- Auditoria completa renderizada: complementação0→1, evidência1→2, evidência2→3, aprovação3→4 e emissão4→5. Aprovação e emissão aparecem como eventos separados; datas/autor e justificativas retornadas pelo serviço.
- Logout no detalhe elimina imediatamente detalhe, evidências, auditoria e ação Sair, apresentando login. Recarregar a URL privada após logout continua sem dados do caso.
- GET público sem credenciais em /admin e na URL direta do caso:200shellCSR, Cache-Control:no-store, X-Robots-Tag:noindex,nofollow,noarchive e CSP presente. HTML de4899bytes idêntico em ambos, sem referências sintéticas, justificativa privada ou campo de grant procurados. Essa verificação direcionada não é uma auditoria exaustiva contra toda forma de vazamento.

## Em andamento / não declarar comprovado

- Conta comum negada no próprio Angular: PASS. Após seleção humana da segunda conta, o painel exibiu “Esta conta não tem autorização para acessar o painel”, sem detalhe/evidências/auditoria anteriores. Login posterior do proprietário pela mesma tela restaurou seu acesso. Não confundir com o teste API/Android de20/10.
- Reautenticação sensível no Angular: a fila tem somente casos encerrados, sem formulário de mutação. Proprietário AUTORIZOU explicitamente o ciclo REQUEST_MORE no caso sintético negativo, adição de evidências/avaliações e REJECT final, sem grant. Preparação local Web foi impedida pela restrição de referrer da chave Web; restrição preservada, não é defeito do painel canônico. Chaves Android locais são distintas da Web. Preparado receptor de sessão via USB/DEV, aguardando aparelho, sem extrair token da sessão Angular. O teste API20/10 GET200/POST403após300s permanece válido, distinto do teste UI.

## Evidências locais

.tmp/admin-final-qa/evidence-audit-real.png e logout-reload-real.png. Capturas reais do painel, com referências explicitamente sintéticas e sem token/UID/email; fora do Git. Não são mocks. Nenhum grant novo. Até este registro, a reabertura ainda não foi executada. Captura adicional: unauthorized-web-real.png.

## Resultado desta passada

Evidências, histórico, logout/reload, conta comum negada e retorno ao proprietário: PASS no Angular publicado. Sem defeito funcional reproduzido nesses fluxos, sem mudança de código por preferência. Reautenticação no diálogo sensível e ciclo de evidência/avaliação: PENDENTES do aparelho, apesar da autorização específica já recebida. ADB consultado não detectou dispositivo. Reabertura NÃO executada; caso negativo permanece REJECTED e grant existente teve fingerprint reconferido idêntico. Preparações loopback encerradas ao aguardar aparelho; nenhum token armazenado ou enviado por chat. Feature não declarada integralmente homologada.

## Correção de direção — sem dependência de ADB

O proprietário recusou a dependência de aparelho para concluir a homologação. Preparações locais estão encerradas; não é necessário conectar Samsung. A lacuna é operacional na UI: backend já permite REJECTED→UNDER_REVIEW por REQUEST_MORE auditado (legacy-review.ts), mas o painel oculta toda avaliação em casos recusados. Encaminhada ao agente Angular a menor correção: ação Reabrir análise somente para REJECTED, justificativa/REQUEST_MORE apenas, confirmação/recência/revisão/idempotência existentes. GRANTED não pode ser reaberto. Sem novos endpoints, auth paralela, alteração de chave Firebase ou extração de token. Publicação segue aprovação/PR/pipeline existentes. O ciclo sintético autorizado continua não executado até a ação Web estar disponível.

Validação backend dirigida após correção de direção:2testes PASS (11não executados por filtro), workerd+RPC+D1 local sintético. Novo caso verifica REJECTED→REQUEST_MORE auditado com revisão vigente, replay idempotente, revisão obsoleta409 e grant nulo; teste de emissão existente ampliado comprova REQUEST_MORE409 em GRANTED e grant preservado. Somente testes alterados, sem código de produção/contrato/migration/deploy.

## Correção Web local pronta

Agente Angular implementou somente reabertura de REJECTED sem grant com modo tipado no formulário existente e guarda no store.30testes administrativos, QA/build/seis páginas públicas passaram. Inspeção própria em127.0.0.1:4312 com fixtures explicitamente sintéticas: formulário mostra justificativa/REQUEST_MORE sem opções de aprovação; diálogo com recência expirada simulada pede identidade; após reautenticação simulada mantém checkbox desmarcado e submit desabilitado; Voltar cancela sem modificar estado/revisão0; caso GRANTED não oferece reabertura. Captura localreopen-local-reauth-confirmation.png é protótipo sintético, NÃO autenticação Google real. Sem ADB e sem escrita remota. Diff/validações no repositório frontend: docs/admin/REOPEN-REVIEW.md. Ensaio real sensível depende agora da publicação da correção pelo fluxoPR/main, não de aparelho. Não houve push/merge/deploy desta correção.

## PR isolado e condição de merge — 22/10/2026

Com autorização do proprietário, o agente Angular abriu https://github.com/DUGELO/calcmot-site/pull/6 na branch codex/admin-reopen-review, commit e78dcfd7b05f3a5bc7d2cc3f068516702ea9f841. Conferido o commit: somente quatro arquivos de produção em src/app/admin, dois arquivos de testes administrativos e docs/admin/REOPEN-REVIEW.md. Nenhuma configuração de infraestrutura, dependência, backend ou contrato no diff.

Site CI remoto integral e Lighthouse passaram; os 30 testes administrativos e build com seis páginas públicas também passaram em checkout limpo. A falha GA4 anteriormente observada não se repetiu neste CI e não foi alterada pelo PR.

Merge não executado: o check Cloudflare preview falhou após build Angular bem-sucedido com a mensagem “Your Wrangler configuration is missing a previews block”. O ajuste é de configuração de publicação existente, fora do diff administrativo. Não foi contornado o check nem alterado o pipeline. Produção permanece na versão 9bfca9aa-816a-40ee-bba7-ed524d56ee03, conforme reconferência do agente Angular. A homologação Web real da reabertura/reauth continua pendente da publicação; nenhum POST administrativo remoto ou grant novo nesta etapa. Não existe dependência de ADB para concluir o fluxo.

## Bloqueio resolvido e merge publicado — 22/10/2026

O proprietário autorizou corrigir o bloqueio de preview. Commit e420a72 adicionou somente previews:{} em wrangler.jsonc e uma guarda em scripts/qa-admin.mjs. Site CI integral, Lighthouse e preview Cloudflare passaram antes do merge. PR #6 integrado por squash em a956ec8ba67f51b43fff1f0061862bf93a521048. CI da main também aprovado.

O agente Angular confirmou autodeploy concluído: versão 1940ff84-5711-4e20-8e2d-f99febca7978 ativa em 100%. Seis páginas públicas e proteções administrativas conferidas por leitura. Sem deploy manual, alterações de backend ou novos grants. A correção de reabertura está publicada; o ensaio autenticado de reabertura e reautenticação real permanece pendente e não é dado como comprovado por esses checks.
