# CalcMot Admin PROD — resultados da preparação

Atualização23/10/2026, branch growth. Ordem: implementar/preparar PROD isolado, preservar DEV, sem concessões comerciais ou alterações IAM/claims. Autorização posterior de uso real foi registrada; nesta execução foi preparado o ambiente sem ativar tráfego comercial. Não confundir recurso provisionado com serviço homologado em produção.

## Infraestrutura remota concluída

| Recurso | Resultado comprovado |
|---|---|
| calcmot-identity-prod | UUID d0232771-b6e7-49a1-ae9f-05267fbdf3a1; distinto do DEV |
| Migrations |0001–0009 aplicadas no PROD novo; nenhuma migration antiga editada |
| Integridade | quick_check=ok; foreign_key_check vazio; quatro índices administrativos |
| Dados de negócio | zero solicitações, grants, políticas, eventos, emissões, evidências, histórico e snapshots |
| calcmot-admin-private-prod | D1 somente PROD, sem rotas/workers.dev/previews; versão2913aabd-a653-433e-9863-d9ffa0103f64 após secret |
| calcmot-admin-gateway-prod | apenas Service Binding ADMIN→privado PROD, sem D1 e sem alvos públicos; versão1e18bfd2-c6a5-46c6-97c9-64d99ea0afed após secret |
| Operador | mesmo UID proprietário conferido no Firebase; secret nos dois Workers; sem conta, claim ou permissão nova |
| Estado | ADMIN_ENVIRONMENT=PROD, ADMIN_MUTATIONS_ENABLED=false nos dois Workers |
| Site/DEV | rotas não transferidas; calcmot-site/main não publicado por esta execução |

Cloudflare Free reconferido no painel22/10: plano atual US$0, sem cartão/upgrade. Firebase Auth e configuração pública Web existentes reutilizados; nenhum novo app/projeto/Auth provider criado. O isolamento dos direitos é D1/bindings, mantendo a identidade Firebase recuperável aprovada. Custo adicional contratado: R$0; cotas Free compartilhadas continuam limite de disponibilidade.

O secret foi configurado sem imprimir seu valor e sem gravar token. O arquivo temporário de entrada do UID foi eliminado; ele não é parte do repositório. O inventário remoto contém somente contagens/metadados, sem UID/email/narrativas.

## Código e artefatos

- security.ts: identificação explícita do ambiente, validação de configuração e gate técnico de mutações; PROD sem flag explícita permissiva continua fechado.
- gateway.ts/api.ts: verificam ambiente esperado da operação antes de D1, impedindo aba DEV antiga de escrever após cutover. Metadata interna de alvo é sobrescrita pelo gateway e verificada pelo privado. Resposta com ambiente divergente é recusada. JWT/owner/admin/Origin/recência continuam independentes.
- contracts.ts: extensão administrativa1.3.0 coordenada com frontend; environment DEV|PROD e mutationsEnabled. Os contratos comerciais S01 não foram alterados.
- worker.ts: HTTP direto continua403, inclusive com metadados de ambiente; operação de negócio somente RPC autenticado.
- Três exemplos de configuração PROD: gateway, privado e API Android futura. Todos sem alvos públicos; a API Android futura não foi implantada e mantém intake/leitura OFF.
- prepare-admin-prod.mjs: artefatos offline, rejeição de UUID DEV, manifesto SHA256 de migrations/fontes/lockfile/configs; nenhuma chamada remota. Configs geradas em backend/.tmp/admin-prod-release, sem segredo.
- validate-prod-migrations-local.mjs: aplicação repetida em D1 local novo e inventário; sem opção remota.
- check-admin-prod-readiness.mjs + admin-prod-readiness.sql: conferência remota somente de agregados e integridade; recusa banco não vazio nesta fase.
- bootstrap-admin-prod-policy.mjs: preparo de registro privado da política pelo mesmo LegacyReviewService; somente política, sem grant/action genérico. --check offline passou. --apply não executado: exige sessão real e recente do proprietário e canal confiável de stdin em memória. Não foi criado endpoint administrativo novo.

## Validações executadas

| Verificação | Resultado |
|---|---|
| TypeScript backend | PASS |
| S06 revisão/emissão local |31 testes PASS |
| S07 projeção e recuperação local |20 testes PASS |
| Admin Web final |15 testes PASS, workerd/RPC + D1 locais |
| Preparo de release/guardas/ferramenta |3 testes PASS |
| Migrations Wrangler local | duas aplicações, ledger9, índices4, integridadeOK; zero seed comercial |
| Bundles PROD dry-run | gateway44,01KiB e privado156,46KiB antes das últimas marcações de versão; sem publish no dry-run. Deploy posterior de fontes finais156,61KiB/44,10KiB |
| Inventário PROD remoto | PASS; todas as oito contagens0; consultas finais sem escritas |
| Preservação DEV | fingerprint do grant existente igual;1grant; schema/índices íntegros |
| HTTP oficial23/10 | anônimo401/environmentDEV/no-store; demonstra tráfego ainda DEV |
| Páginas públicas | quatro páginas conferidas200, sem header administrativo; frontend validou build/seis SSG |

São69 casos backend distintos nessas quatro suites; não uma execução única de69 testes após toda edição documental. A última mudança funcional de runtime foi coberta pelos15 testes admin; mudanças posteriores de ferramenta tiveram seus3 casos e typecheck. A execução inicial restrita teve erro de permissão do esbuild e foi interrompida; a repetição autorizada fora dessa restrição passou. O primeiro harness PROD usou um fetch de binding que não preservava a origem do teste: corrigido para dois runtimes workerd independentes, sem relaxar a validação do servidor.

O inventário inicial excedeu o limite D1 de termos de UNION; o SQL foi dividido e validado local/remotamente. O modo Wrangler --file retornava resumo, não os resultados: a confirmação final usa --command no script e valida os valores reais. Tempo SQL agregado amostrado1,1245ms NÃO é CPU edge/JWT nem comprova margem de10ms.

Um ensaio exploratório de binding remoto retornou403 sem o header do aplicativo. Isso não comprova execução da API PROD/JWT: o transporte de preview não alcançou a resposta esperada do Worker. O probe experimental não integra o código entregue. Os testes remotos autenticados PROD permanecem pendentes; nenhum sucesso foi fabricado a partir dessa negativa intermediária.

## Frontend — pedido encaminhado e atendido localmente

Agente Angular explicitamente instruído a preparar src/environments/environment.ts, environment.staging.ts e environment.production.ts, conforme exemplo do proprietário. Default e staging selecionam DEV; PROD seleciona PROD somente consulta. Seleção via fileReplacements/configurações aditivas, sem segredo ou seletor por URL/storage. O comando automático atual continua DEV; nenhuma mudança de workflow/pipeline.

O agente reportou41 testes administrativos, teste de seleção, testes edge, builds DEV/PROD, SEO/paridade de seis páginas e empacotamento/workerd locais PASS. Em23/10 consolidou QA e documentação sem repetir builds já válidos. Configs públicas Firebase permanecem centralizadas; UID proprietário, tokens e D1 nunca entram nas envs do browser. Dados de exemplo/preview são identificados sintéticos.

Referência no outro repositório: docs/admin/ENVIRONMENT-PREPARATION.md. Nenhum push/merge/deploy desse preparo foi realizado; não declarar PROD ativo pelo fato de environment.production.ts existir.

## Pendências objetivas antes de ativar

1. Validar registro inicial da política PROD com sessão real recente por canal privado seguro. O script foi preparado, mas a operação não foi executada. Não extrair token da sessão do navegador nem enviar credencial pelo chat para contornar isso.
2. Coordenar PR/build Angular de ambiente PROD, preservando pipeline, e as duas rotas exatas do gateway. Manter somente consulta durante validação real. Sem publicar um dos lados isoladamente com fallback permissivo.
3. Validar autenticação e autorização PROD por caminho real autorizado e coletar múltiplas amostras CPU autenticadas com D1. Serviços sem alvos públicos não têm, nesta preparação, URL HTTP adicional para inventar.
4. Confirmar operação OQ-07 para titulares reais, incluindo canais/fundamentos/execução de retenção e exclusão. A política aprovada para DEV e a autorização geral de uso real não são apresentadas como parecer jurídico ou descarte automático aprovado.
5. Fechar o ensaio positivo DEV no escopo solicitado com evidência física adicional Android quando disponível. O grant DEV anterior/recuperação comprovados foram preservados; não houve nova emissão nesta rodada. ADB não detectou aparelho22/10. Não condicionar a rotina administrativa web ao USB.
6. Revisar autorização de ativação comercial separadamente: gates permanecem OFF e nenhum grant real foi criado.

**Status:** preparação de infraestrutura isolada e código local concluída; cutover/operação comercial HOLD pelos critérios acima. O roteiro executável, impactos, comandos e rollback estão em S06-ADMIN-PROD-CUTOVER.md. Não é uma declaração de produção operacional DONE.
