# OQ-07 — rotina PROD aprovada e atendimento a titulares

23/10/2026. Aprovação expressa do proprietário: **“Confirmo os mesmos prazos e condições para PROD”**. Esta aprovação estende a rotina do piloto a PROD, sem autorizar exclusões nesta execução. Não é parecer jurídico, aprovação de coleta extra ou autorização para ativar concessões.

## Responsável, canal e minimização

Controlador declarado: proprietário CalcMot, pessoa física. Canal público já existente: eduardoangelo20001@gmail.com. O proprietário atende e decide; outro operador exige designação/autorização específica. Não inferir autorização do email. Nome/qualificação pública do controlador e aviso de privacidade devem refletir sua identificação real; não inventar CPF, CNPJ ou nome civil a partir do perfil Google. A atualização do aviso público será coordenada com o site, não publicada por este PR backend.

Coletar apenas identidade recuperável, protocolo, declaração, referências mínimas e fundamento da avaliação. Não solicitar documentos civis, cartões, vídeos de corrida, localização, ganhos ou senhas por padrão. Email não prova titularidade do UID e login Google não prova uso histórico. Referências não são automaticamente abertas nem transmitidas a terceiros. Não enviar evidências para Analytics/Clarity/logs de Worker. Esta rotina administrativa não certifica a telemetria geral do Android ou do site.

## Inventário e prazos aprovados

| Classe | Fonte/finalidade | Rotina aprovada |
|---|---|---|
| Identidade/titularidade | Firebase Auth; UID/projeto no D1, identificação e recuperação | Enquanto necessário à relação e ao direito recuperável, com revisão de necessidade e pedidos verificados; não guardar todo atendimento por causa do UID |
| Fonte mínima do direito | LegacyGrant e autoridade mínima separada | Direito comercial permanente; apagar evidência descartável não revoga grant. Não prometer recuperação automática após eliminar integralmente seu vínculo |
| Evidências adicionais/referências | Revisão individual S06 |30 dias após encerramento definitivo |
| Atendimento de suporte | Canal de suporte |90 dias após encerramento definitivo; originais adicionais seguem30 dias |
| Auditoria administrativa minimizada | Decisão, revisão, operação e emissão |12 meses, finalidade/fundamento específicos; detalhes descartáveis não são licença para retenção infinita |
| Logs sanitizados/artefatos operacionais | Diagnóstico técnico |30 dias; sem tokens, perfis ou narrativas. Conferir retenção dos provedores e não prometer apagamento físico imediato global |

Uma recusa isolada não constitui encerramento definitivo: tratar recurso/complementação e comunicar resultado. Reabertura invalida fechamento anterior. Os prazos não são apresentados como exigência legal universal nem substituem análise de exceção fundamentada. Não existe retenção residual automática ou legal hold geral após exclusão integral.

## Procedimento de atendimento executável

1. **Receber:** registrar protocolo de privacidade não sensível, data, tipo de pedido e canal de retorno no atendimento privado existente. Não registrar JWT/senha/código Google. Diferenciar acesso/correção, descarte de anexos, exclusão de atendimento e exclusão integral.
2. **Confirmar titularidade proporcionalmente:** usar identidade autenticada e contexto do atendimento sem expor dados de outro UID. Se insuficiente, pedir apenas complementação necessária; não criar grant ou transferir conta para “resolver” o pedido.
3. **Triar:** identificar Firebase, D1, suporte e artefatos abrangidos. Para confirmação/acesso, prestar resposta simplificada quando possível; declaração completa em até15 dias do requerimento, segundo art.19/LGPD e orientação ANPD. Não aplicar esse prazo indistintamente a todos os direitos nem inventar SLA comercial para revisão legada. Comunicar providências, razões e pendências quando não houver atendimento imediato.
4. **Preparar:** operador autorizado usa o serviço privado `LegacyPrivacyService.plan`, somente após autenticação/recência, e confere titular, escopo, prazo, revisão/fingerprint e impacto. O painel atual não tem endpoint de exclusão; não fingir que logout exclui conta ou que existe um botão concluído de privacidade.
5. **Autorizar:** execução destrutiva depende de pedido verificado e confirmação específica do responsável. `execute` exige recibo/fingerprint/operationId e confirmação; exclusão integral exige ainda confirmação de perda de recuperação automática do vínculo. Não executar diretamente SQL administrativo nem desabilitar triggers globalmente.
6. **Executar por componente autorizado:** D1 usa operação atômica privada existente; Firebase, suporte e cópias têm etapas separadas. Sem canal privado PROD validado, suspender a execução e registrar bloqueio, nunca redirecionar ferramenta DEV para PROD removendo guardas. Nenhuma exclusão é autorizada por este documento sozinho.
7. **Conferir e responder:** verificar recibo e inventário pós-operação, preservar direitos não abrangidos e informar precisamente etapas concluídas/externas pendentes. Não responder “todos os dados excluídos” se Firebase, suporte, cache ou retenção de provedor ainda não foram tratados.
8. **Agendar vencimentos:** o proprietário verifica os vencimentos manualmente em rotina diária do piloto. Sem cron pago ou novo sistema de tickets. Registrar somente conclusão mínima e data de próxima revisão; não guardar a narrativa integral para provar que foi eliminada.

Modelos de resposta: “Recebemos seu pedido [protocolo]. Vamos confirmar a titularidade e informar os dados/etapas abrangidos”; “Concluímos [etapas]. Permanecem pendentes [etapas e motivo]. O direito permanente não foi revogado pelo descarte de evidências”. Personalizar conforme o resultado real; não enviar mensagens automaticamente nesta execução.

## Segurança, incidentes e recuperação

Firebase e D1 usam o projeto/UID validado; não entregar registros por coincidência de email. Operador precisa de allowlist, admin legítimo e reautenticação recente. Auditoria de revisão/emissão continua transacional. Permissões D1 de infraestrutura não substituem a sessão administrativa do serviço.

Revogar refresh tokens não elimina instantaneamente ID Tokens já emitidos. Uma exclusão integral só pode ser considerada encerrada após tratar a janela de sessão/recriação e as etapas externas. Backups/restauração devem respeitar solicitações anteriores; restaurar em ambiente isolado e reaplicar decisões de privacidade antes de voltar a atender tráfego. Não apagar banco ou grant para fazer rollback do aplicativo.

Em vazamento suspeito: cessar mutações e coleta adicional, preservar evidência mínima com acesso restrito e acionar o responsável. Avaliar obrigações de comunicação aplicáveis com base no incidente, sem declarar prazo universal ou omitir apuração. Nenhuma alteração IAM, exclusão ou comunicação externa está autorizada por esta preparação.

## Bases e transparência

Finalidades operacionais: vínculo/recuperação e manutenção do direito; análise individual; comprovação de decisão; atendimento a direitos; proteção contra abuso. O enquadramento jurídico deve ser confirmado por finalidade e contexto, inclusive fornecedores/transferências internacionais; não atribuir consentimento genérico a todo tratamento. O aviso público precisa explicar Firebase/Cloudflare, minimização, direitos e contato antes de receber solicitações reais. A autorização do proprietário não é consentimento dos motoristas.

Fontes oficiais verificadas23/10/2026: [ANPD — direitos dos titulares](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados/direito-dos-titulares), [LGPD — arts.18 e19](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm). Nenhuma certificação de conformidade integral é alegada.

**GO documental:** prazos, responsável, canal e procedimento definidos e aprovados. **HOLD de execução destrutiva:** canal PROD autenticado, pedido concreto e confirmação específica ainda necessários. Nenhum descarte foi realizado.
