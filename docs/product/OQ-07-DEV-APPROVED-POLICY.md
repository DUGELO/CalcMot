# OQ-07 — política operacional aprovada para o piloto S06 DEV

**2026-10-16. Aprovação expressa do proprietário nesta execução.** Escopo: piloto DEV controlado de identidade e recuperação S06/S09, não rollout público, certificação LGPD ou validação jurídica. A proposta anterior permanece histórico; seus prazos diferentes não estão ativados.

- Controlador declarado: **proprietário, pessoa física**. Contato de privacidade/suporte: o contato CalcMot existente em `AppLegal.kt`, **eduardoangelo20001@gmail.com**. Sem CPF/documento civil coletado para esse piloto. Identificação jurídica pública completa e avaliação dos fundamentos continuam necessárias antes de rollout.
- Coleta: identidade autenticada, declaração, protocolo, referências mínimas, classificação e justificativa contextualizada. Sem documentos civis, vídeos, dados de corrida ou financeiros por padrão. Originais adicionais somente no suporte, quando necessários.
- Evidências adicionais: **30 dias após encerramento definitivo**. Suporte: **90 dias após encerramento definitivo**. Auditoria minimizada: **12 meses**, com finalidade/fundamento aplicáveis. Logs técnicos sanitizados: **30 dias**. Nenhum desses números é apresentado como prazo legal universal.
- Grant: validade comercial permanente, separado de Premium. Descartar referência/anexo ou expirar auditoria não revoga o grant. Enquanto a conta utiliza recuperação, vínculo e fonte mínimos continuam necessários ao serviço; isso não autoriza conservar toda a revisão indefinidamente.
- Exclusão integral: **sem retenção residual automática**. Identidade/UID/digest vinculável são dados pessoais. Explicar que eliminar integralmente o vínculo inviabiliza recuperação automática desse vínculo; encaminhar a decisão concreta e eventuais fundamentos aplicáveis, sem impor renúncia ao direito comercial. Não há comprovante portátil implementado nem promessa de recuperação automática após eliminação integral.

## Finalidades e fundamentos propostos

Identificação/recuperação e manutenção do direito: prestação da relação de serviço, conforme enquadramento concreto. Revisão e auditoria: necessidade de comprovar a decisão e, quando aplicável, exercício regular de direitos. Não utilizar consentimento genérico para justificar toda conservação; fundamento jurídico e transferências precisam ser avaliados por finalidade. Aprovação empresarial não equivale a parecer jurídico. [LGPD](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm).

## Procedimento proporcional

1. Receber no canal existente; confirmar titularidade via sessão e atendimento proporcional, sem pedir token/senha/código Google. Distinguir apagar anexos, apagar atendimento e excluir integralmente a conta.
2. Operador encerra definitivamente o protocolo somente depois de comunicar decisão e tratar recurso pendente. Uma rejeição isolada não inicia automaticamente a retenção. Reabertura invalida o fechamento.
3. Executar `privacy-plan` no console privado, conferir titular/escopo/prazo e autorização. `privacy-execute` é separado de decisão comercial; ações são atômicas, por projeto/titular, com recibo mínimo e retry. Não existe endpoint público ou parâmetro Android de descarte.
4. Aos 30 dias, remover evidência contextual da revisão e eventuais referências históricas descartáveis; grant permanece byte a byte. Aos 12 meses, remover narrativa/eventos detalhados/recibos redundantes e minimizar reviewer da claim; fonte mínima do direito permanece necessária à conta ativa.
5. No suporte, proprietário elimina originais adicionais aos 30 dias e atendimento residual aos 90 dias, incluindo lixeira/cópias quando aplicável; registrar somente a conclusão mínima. Não existe integração Gmail automática nem exclusão de mensagens autorizada nesta missão. Nenhuma mensagem foi enviada ou removida.
6. Logs: nunca bearer, perfil ou conteúdo de evidência; revisar e remover artefatos pessoais autorizados aos 30 dias. Logs/telemetria já existentes do produto não foram reclassificados como auditados por esta entrega. Conferir retenções próprias dos provedores antes de prometer eliminação física global.
7. Exclusão integral exige pedido verificado e autorização específica: primeiro tratar Firebase/sessões e eventual impedimento fundamentado; depois D1, suporte e cache local. Revogar refresh tokens não invalida instantaneamente todo JWT já emitido: aguardar a expiração dos tokens ou comprovar bloqueio efetivo antes de considerar encerrada a janela de recriação de dados. A operação D1 sozinha retorna etapas externas pendentes, nunca “exclusão global concluída”. Não foi autorizado apagar conta/grant real.
8. Restauração de backup exige ambiente isolado e reaplicação dos pedidos de privacidade antes de atender tráfego. Nenhuma restauração pessoal foi executada. Não criar backup extra por padrão. Retenções justificadas excepcionais exigem decisão específica, motivo, escopo e revisão; não há legal hold global automático.

O proprietário responde pela revisão e manutenção manual do piloto. Pode designar futuramente outro responsável mediante autorização explícita e configuração de autoridade; a condição de operador não é inferida do email. A rotina manual deve ser executada nos vencimentos, sem cron pago ou expurgo invisível.

## Implementação e limites

Migration aditiva `0008_legacy_privacy.sql`: fechamento explícito, recibos e exceções SQL estritamente vinculadas à operação atômica. Nenhuma migration antiga foi editada. `LegacyPrivacyService` implementa dry-run, verificação de revisão e operações privadas; as fontes comerciais mantêm imutabilidade fora desse procedimento.

**Aprovação da política não significa comprovação operacional concluída.** Resultados locais/remotos e status finais são registrados no relatório de encerramento S06/S09. Sem autorização de produção ou exclusão real.
