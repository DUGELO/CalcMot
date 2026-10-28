# OQ-07 — Identidade, direitos, retenção e exclusão

**Histórico superado no escopo do piloto DEV:** o proprietário aprovou prazos/condições específicos em 16/10, formalizados em [OQ-07-DEV-APPROVED-POLICY.md](OQ-07-DEV-APPROVED-POLICY.md), implementados proporcionalmente e testados conforme [resultado operacional](S06-S09-DEV-OPERATIONAL-RESULTS.md). Esta proposta antiga não autoriza outros prazos nem descreve o estado corrente. Rollout jurídico/público continua separado.

Data: **2026-10-14**. **PROPOSTA PARA APROVAÇÃO — NÃO APROVADA, NÃO PUBLICADA, NÃO ATIVADA.** Não substitui a política publicada nem implementa S14/S15. Custo de infraestrutura autorizado: R$ 0. A elegibilidade <=3.5/versionCode 12 e a permanência do Core estão aprovadas; os prazos abaixo são recomendações operacionais, não novos critérios de elegibilidade.

## Inventário observado e limites

Conferência somente de leitura. O Console Firebase `calcmot` mostrou **duas identidades Google**, criação/último login em 13/10, identificador e UID. O candidato do proprietário existe; UID/email não foram copiados ao relatório. Essa tela **não mostra custom claims** e não prova quem tem admin. Nenhum usuário foi alterado.

No painel do D1 DEV, duas tabelas e metadados de SQL foram visíveis; consultas históricas de fixtures também aparecem com SQL literal. A ferramenta Studio não carregou o schema nesta inspeção, portanto não há censo remoto de linhas nesta rodada. Não presumir implantação de S05/S06 ou dados reais ali. Nenhuma consulta de escrita foi enviada e não houve export de usuários.

No banco **local** do console, a inspeção nova encontrou **12 tabelas de negócio, todas com zero linhas**. Schema/migrations demonstram o que o código pode armazenar, não registros de clientes reais. O script [s06-local-inventory.mjs](../../backend/scripts/s06-local-inventory.mjs) lê somente campos/contagens/guardas do banco fixo local; não extrai conteúdo pessoal. Fixtures de testes anteriores são locais e isoladas.

| Categoria | Onde/campos efetivos | Finalidade e classificação |
|---|---|---|
| Identidade e titularidade | Firebase Auth: UID, provedor Google, email e metadados de criação/login confirmados na UI; SDK pode disponibilizar nome/foto/ID do provedor. `IdentitySession` do app usa UID/providers, sem copiar perfil para D1 | Dados pessoais. Identidade atual não comprova uso histórico. Campos opcionais por conta não foram exportados/inventariados. |
| Sessão | ID Token validado em memória; SDK Firebase mantém sessão no aparelho. Token não é persistido pelo backend/console | Credencial: não guardar em log, suporte, banco, repo ou Analytics. Refresh não substitui reautenticação. |
| Direito permanente | `legacy_grants`: projeto/UID, estado, política, data, tipo/hash de evidência, baseline, migração/revisão, ator; cópia em `grant_json`. `legacy_review_issuances` duplica grant JSON e liga aprovação/conclusão | Dados pessoais vinculáveis, fonte do Core; não Premium. Permanência do benefício não autoriza conservar todos os documentos de suporte. |
| Evidência histórica | `legacy_evidence`, `legacy_historical_use`, referências/digest, UID, versão comprovada, base, verificador e datas | Prova verificada tem autoridade própria. Hash não torna o registro anônimo. Avaliação humana não é inserida como história verificada. |
| Solicitação e vínculo | `legacy_claims` + `claim_json`, `legacy_claim_requests`, `legacy_claim_identities`, `legacy_recovery_requests` | Protocolo, estado, UID, digests de idempotência, vínculo Google e datas. Mesmo sem email são dados pessoais. |
| Referências de atendimento | `legacy_review_evidence`: categoria, origem, referência opaca, autor/data; originais permanecem no suporte Gmail | Backend não precisa de corpos de email, anexos, nome, telefone ou vídeos. Referência opaca continua vinculável se aponta a pessoa/caso. |
| Auditoria administrativa | `legacy_review_events`: ator, motivo livre em `details_json`, evidências, decisões e revisões. `legacy_policies`: aprovador/regra. Recibo de emissão em `legacy_review_issuances` | Justificativa pode conter PII acidental; instruir operador a usar texto factual mínimo, sem copiar atendimento. Ator também é dado pessoal do proprietário. |
| Projeção | `entitlement_snapshots`: projeto/UID/revisão/JSON | Tabela S02 de leitura; nenhuma projeção S07 adicionada. Não é a fonte única do direito. |
| Logs e recuperação | Logs locais Wrangler, artefatos `.tmp`, SQL metadata/Time Travel do provedor; IP/user-agent de autenticação tratados pelo Firebase | Risco de cópias fora da tabela principal. Não confundir apagar uma linha com apagar logs/backups. SQL sempre parametrizado para casos reais; não colar valores pessoais no console SQL. |
| Telemetria já existente | Analytics/Crashlytics/Clarity constam da política local existente | Retenções/configurações de cada painel não foram auditadas aqui. Revisão S14/S15 necessária; não afirmar que este inventário cobre toda coleta do produto. |

## Compatibilidade jurídica: condições, não certificação

A LGPD exige finalidade/necessidade/transparência, direitos de acesso/correção e tratamento de pedidos de eliminação; conservação após o término precisa caber em hipótese legal. Confirmação simplificada/acesso têm regras específicas no art. 19; o prazo de 15 dias para declaração completa **não é prazo universal de exclusão**. Fundamentos candidatos: execução da relação de serviço para identidade/direito; exercício regular de direitos quando demonstrado; interesse legítimo somente com avaliação concreta. Não usar consentimento genérico para justificar tudo. [LGPD, arts. 6–10/15–19/46](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm).

Verificar a aplicabilidade de guarda de registros de acesso prevista no art. 15 do Marco Civil, conforme natureza jurídica/atividade do controlador. A hipótese legal de seis meses não torna documentos de revisão ou tokens em registros obrigatórios. Não existe justificativa para guardar toda PII por esse prazo. [Marco Civil](https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm).

Firebase Auth opera nos EUA segundo o fornecedor. A operação D1 observada mostrou região ENAM, sem evidência de residência Brasil. Identificar controlador legal/contato, subprocessadores, mecanismo de transferência e termos aplicáveis antes de declarar conformidade; não presumir que usar plano Free dispensa essas verificações. [Firebase Privacy](https://firebase.google.com/support/privacy), [ANPD — transferência internacional](https://www.gov.br/anpd/pt-br/assuntos/assuntos-internacionais/transferencia-internacional-de-dados).

Google Play exige caminhos de solicitação no app e fora dele quando aplicável à criação de contas. A página pública atual precisa ser atualizada antes do rollout de conta; este documento interno não satisfaz sozinho essa exigência. [Requisitos de exclusão](https://support.google.com/googleplay/android-developer/answer/13327111?hl=pt-BR).

**OPEN_QUESTION:** identificação jurídica do controlador e aplicabilidade de obrigações específicas, base por finalidade, transferências/contratos, retenção excepcional e procedimento de perda total de identidade. A aprovação do proprietário registra política empresarial; não substitui verificação jurídica de obrigações aplicáveis.

## Prazos propostos — sujeitos à aprovação

Dias abaixo são corridos. Encerramento significa decisão final após recurso ou fechamento assistido comunicado, não simplesmente parar de usar o app. Legal hold só por motivo específico documentado, com finalidade, escopo, responsável, prazo/revisão; não por default global.

| Dados | Prazo/gatilho proposto | Operação e justificativa |
|---|---|---|
| Perfil Firebase necessário ao login | Enquanto a conta existir para finalidade ativa; revisar necessidade anualmente. Pedido verificado: iniciar exclusão em até **30 dias**, salvo impedimento motivado | Excluir usuário/invalidar sessões pelo processo autorizado futuro; não copiar perfil para D1. Inatividade não extingue legado nem dispara exclusão automática. |
| Registro mínimo do Core e vínculo ao titular | Enquanto a conta exercer a finalidade de recuperação, com revisão anual. Ao excluir conta: decidir destino e minimizar/excluir vínculo em até **30 dias** | Benefício não expira. Necessidade continuada é diferente de guardar toda evidência para sempre. Ver decisão de recuperação abaixo; não excluir grant automaticamente nem manter UID ocultamente após pedido integral. |
| Evidências brutas/anexos e originais de suporte usados no caso | Até **180 dias após encerramento**; eliminar antes se desnecessários ou houver pedido procedente | Permitir complementação/contestação curta; guardar só conclusão mínima depois. Não coletar vídeos, corridas, dados financeiros ou documento civil por default. |
| Referências, narrativas e contexto detalhado de revisão | Até **180 dias após encerramento**, depois eliminar ou anonimizar de verdade | Remover texto livre/links ao atendimento; hash de email ou UID sozinho continua pessoal, não serve como falsa anonimização. |
| Protocolos sem emissão, rejeitados ou sem continuidade | Revisão humana de finalidade a cada **90 dias**; propor encerramento após **180 dias sem resposta**, comunicar e permitir retomada assistida | Não converter inatividade em rejeição comercial ou perda de legado. Após encerramento, conservar só auditoria mínima pelo prazo abaixo; expurgar conteúdo excedente. |
| Auditoria mínima de decisão/privacidade | **2 anos após conclusão**, com revisão anual de necessidade | Data, ação, resultado, política e identificação mínima do responsável; sem corpo do atendimento. Prazo de gestão proposto, não prazo prescricional universal. Depois anonimizar ou eliminar vínculo, salvo hipótese específica documentada. |
| Digests de requests/idempotência e intents sem direito | **180 dias após encerramento**; recibo mínimo de emissão acompanha a finalidade do grant, sem conservar relato | Reduz replays durante operação. Futuro expurgo deve manter deduplicação/recuperação de fontes existentes sem transferir/grant duplicado. |
| Logs de diagnóstico controlados pelo CalcMot | **30 dias**, sem bearer/perfil/PII por padrão | Não se aplica a eventual guarda legal específica de registros de acesso. Metadados de terceiros têm prazos próprios ainda a confirmar no painel/contrato. |
| Comprovante mínimo de atendimento de privacidade | **2 anos**; remover conteúdo do pedido após execução e guardar só resultado necessário | Demonstrar atendimento sem reter indefinidamente tudo que foi apagado. Vinculação residual também é tratamento e precisa de base/finalidade. |
| Exports locais de desenvolvimento com dados reais, se excepcionalmente autorizados | **7 dias no máximo**, ou imediatamente após validação | Preferir nenhum export. Não manter token. Fixtures sintéticas não autorizam copiar dados de clientes. |

**Limites dos fornecedores:** o Firebase informa IPs de Auth por algumas semanas e remoção de outros dados de autenticação dos sistemas ativos/backups em até **180 dias após iniciar exclusão**. Não prometer eliminação física global em 30 dias. O D1 Free oferece janela de restauração Time Travel de **7 dias**; ela não é SLA de exclusão de todos os logs/metadados da Cloudflare. Não criar backup extra ou recurso pago. [Firebase](https://firebase.google.com/support/privacy), [D1 Time Travel](https://developers.cloudflare.com/d1/reference/time-travel/).

## Direito permanente versus exclusão integral

**Não são equivalentes:** apagar dados identificáveis, anonimizar dados e revogar um direito. UID, hash estável ou HMAC com chave/mapeamento disponível são dados pessoais/pseudonimizados, não anonimização garantida. Não renomear UID no grant e declarar cumprimento.

Recomendação para aprovação: manter só o registro mínimo necessário durante a conta; antes de exclusão integral, oferecer ao titular um **comprovante portátil verificável do direito**, sem anexos históricos, para futura recuperação assistida com ownership. O direito comercial continua; não pode depender de recadastro na janela da migração. Esse mecanismo ainda precisa de desenho/implementação segura em S15, sem endpoint de autoatribuição, transferência automática ou mudança silenciosa do DTO S01. **Não foi criado nesta execução.**

Alternativa: retenção residual mínima por fundamento específico validado, claramente informada e contestável, com acesso restrito e revisão anual. Não aprovar automaticamente conservação eterna por haver grant permanente. Quando aplicável a um pedido concreto, informar exatamente o que fica, por quê, até quando/qual evento e como contestar.

Sem vínculo retido **e** sem prova em posse do titular, não se pode prometer recuperação automática após exclusão integral. Revisão assistida continua disponível, mas não fabrica prova nem libera benefícios por email. Essa escolha operacional é pendência de aprovação/implementação; não se resolve alterando elegibilidade ou impondo retenção clandestina.

## Imutabilidade: impedimento atual e correção necessária

As tabelas de grants/evidências/histórico/auditoria/recibos possuem triggers que abortam DELETE e UPDATE; `claim_json`/`grant_json` também duplicam identificadores. FKs ligam histórico a evidência e recibo a decisão/protocolo. Remover só Firebase Auth, só o UID da coluna, ou só a linha principal deixa dados e pode destruir recovery. Os testes anteriores provam imutabilidade comercial, **não capacidade de exclusão legal**.

**HOLD para operação com casos pessoais reais até existir procedimento executável.** A política proposta exige uma exceção de privacidade formalmente autorizada, por titular/finalidade, separada da revisão comercial. Implementação futura S15 deve preservar o fato do direito, tratar todas as cópias/FKs, permitir redução/eliminação de identificadores e minimizar auditoria; testar dry-run, retry, falha e ausência de revogação. Não remover guardas globalmente nem expor um flag de purge ao cliente. Nenhuma migration/rota de exclusão foi criada ou executada agora.

Assim, a compatibilidade com privacidade está **condicionada**, não garantida pelo schema atual. Os schemas S01 permanecem intactos; eventual incompatibilidade da operação de apagamento com DTO/recuperação exige revisão explícita antes de implementar. Não modificar registros imutáveis a escondidas.

## Procedimento de solicitação proposto

1. Canal inicial existente: suporte do proprietário, sem cobrança e sem exigir reinstalação. Disponibilizar no futuro caminho destacado no app/site; não afirmar que já existe fluxo completo S15.
2. Confirmar titularidade proporcionalmente via sessão/reautenticação ou atendimento assistido; email isolado não autoriza apagar dados de terceiro. Nunca solicitar token/senha/código Google em suporte.
3. Informar escopo: perfil, vínculo, fontes, revisões, cópias de suporte e retenções justificadas. Separar apagar conta, apagar evidências e manter conta/benefício. Não condicionar direito de privacidade a aceitar perda comercial.
4. Responder confirmação/acesso nos termos legais; meta proposta de triagem em **5 dias úteis** e execução de exclusão em **30 dias**, explicando exceções e janela do provedor. Não tratar metas internas como prazos universais da LGPD.
5. Após aprovação e implementação futura, emitir plano por titular antes da mutação; processar D1/Firebase/suporte/cópias e encaminhar obrigações aos operadores. Registrar conclusão mínima, sem copiar os dados removidos para um novo log.
6. Restaurar backup somente com isolamento e reaplicar solicitações de privacidade antes de atender tráfego; cópia não pode ressuscitar dados apagados. Confirmar descarte de exports e janelas efetivas do provedor. Sem recursos pagos.

## Aprovação necessária

- **OQ07-D1:** aprovar/ajustar a matriz de prazos e os eventos de início; nenhuma regra de expurgo está ligada.
- **OQ07-D2:** escolher recuperação após exclusão integral: recomendação de comprovante portátil + atendimento assistido, ou retenção residual juridicamente fundamentada. Nada disso está presumido/implementado.
- **OQ07-D3:** confirmar controlador/contato, fundamentos e transferências; definir procedimento de legal hold e revisão anual, evitando retenção infinita.
- **OQ07-D4:** autorizar, depois, implementação S14/S15 de minimização/exclusão compatível com as guardas e atualização da política pública. A aprovação deste texto não é autorização para deletar usuários/grants reais.

**Status OQ-07: OPEN — proposta entregue para aprovação. S06 permanece PARTIAL.** Inventário e planejamento não equivalem a política aprovada, implantação ou exclusão efetiva.
