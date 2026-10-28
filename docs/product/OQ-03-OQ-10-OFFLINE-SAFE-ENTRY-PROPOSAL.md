# OQ-03 / OQ-10 — Proposta para aprovação, sem ativação

**Atualização vinculante:** o proprietário aprovou a política em execução posterior. Este documento é histórico; dúvidas comerciais abaixo foram substituídas pela [política aprovada](OQ-03-OQ-10-APPROVED-POLICY.md). Inferência de recência promo por GET não foi ativada; confirmação específica da fonte é necessária. Gates continuam OFF. Não interpretar “não aprovado” abaixo como status atual.

Data: 2026-10-14. S08 permanece **PARTIAL**. Valores sugeridos pelo proprietário para avaliação, **não aprovados**. Não modifica PRD/S01, regras de elegibilidade, Core ou serviços. Não autoriza paywall, cobranças ou próxima story.

## OQ-03 — recomendação técnica

Recomendo os limites propostos, condicionados à aprovação explícita e à validade da fonte:

| Fonte confirmada | Idade máxima proposta sem nova verificação autoritativa | Limite absoluto |
|---|---|---|
| Play / PAID_PERIOD | 72 horas | `min(play.validUntil, play.checkedAt + 72h)` |
| Play / TRIAL | 24 horas | `min(play.validUntil, play.checkedAt + 24h)` |
| Promo ACTIVE | 24 horas | `startAt <= agora < min(validUntil, checkedAt + 24h)` |
| Legacy Core confirmado | Sem vencimento comercial | Independente de rede, boot e assinatura. Vinculado ao mesmo titular. |
| UNKNOWN/PENDING/ON_HOLD/PAUSED/EXPIRED/REVOKED | Não cria acesso temporário novo | Reconciliação, sem converter erro em compra ou legado. |

ACTIVE/CANCELED_WITH_ACCESS/GRACE apenas conservam acesso enquanto fonte válida; graça não acrescenta 72h ao período Play. Se a fase estiver UNKNOWN, exigir revalidação, não escolher 72h por conveniência. Fontes se unem independentemente: vencimento de uma não apaga outra ainda válida. Review é só Core provisório, nunca Pro; **prazo continua indefinido comercialmente e não foi ativado**, conforme decisões S06.

**“Revalidação” precisa significar verificação da fonte, não apenas GET/cache novo.** S07 pode retornar HTTP 200 com `serverNow` recente e `play.checkedAt` antigo. Essa resposta não renova a janela Play. Para promo, `checkedAt` raiz da S07 representa alteração da projeção, não heartbeat de conferência: com a implementação atual, uma promoção estável de várias semanas deixaria de ser candidata após 24h mesmo online. Antes de ativar, definir uma operação confiável que confirme a fonte e atualize recência sem estender período, respeitando o protocolo existente; não reescrever marcadores nem contratos silenciosamente. Isso não foi implementado nesta execução.

Além disso, outra fonte pode alterar `checkedAt` raiz: esse instante não deve ser tratado como prova isolada de verificação da promo. Alternativa mínima recomendada, **condicional à fonte promo autoritativa no backend**: considerar a última resposta 200 própria ao GET, que realmente consulte/valide aquela fonte, como revalidação de promo e ancorar a janela no `serverNow` recebido. Não usar resposta reapresentada do cache como nova consulta, nem aplicar essa regra à Play. Os campos S01/cache existentes permitem essa preparação sem inventar novo campo HTTP; a semântica da fonte promocional ainda deve ser comprovada antes de ativar. A tabela acima descreve o comportamento parametrizado atual com `checkedAt`; a política final precisa resolver essa lacuna explicitamente.

No boot/perda da âncora monotônica: `REVALIDATION_REQUIRED` para **novos acessos temporários**, sem extrapolar pelo relógio civil. Mesmo com poucos minutos restantes, o pagante pode precisar de internet após reiniciar. Core legado e Core atualmente gratuito não são afetados. `elapsedRealtime` inclui suspensão e reinicia no boot, por isso o relógio civil não deve restabelecer a janela. [Referência Android](https://developer.android.com/reference/android/os/SystemClock).

Cache de direito não é cache de credencial: uma autorização offline de 72h não torna um ID Token válido por 72h nem dispensa JWT no backend. Tokens Firebase têm duração curta e renovação própria; não persistimos bearer com o entitlement. [Firebase sessions](https://firebase.google.com/docs/auth/admin/manage-sessions).

### Impactos e riscos

- 72h reduz falsos bloqueios por conectividade, mas pode atrasar conhecimento de revogação/refund/hold; não revogar legado para compensar isso. Não comprova dinheiro ou recompensa.
- 24h no trial/promo limita abuso, mas reboots e verificação antiga podem causar revalidação antecipada legítima. Uma fonte com expiry menor vence antes: sem dias extras nem trial renovado pelo cliente.
- Renovação Play real deve atualizar verificação/período autoritativos nas stories Billing; S08 não inventa renovação. Uma falha 401/503 preserva confirmação local dentro dos limites aprovados e nunca revoga Core legado.
- Não adicionar polling por oferta ou retry sem limite. Preferir abertura/retorno e recuperação explícita, fora do pipeline, com orçamento de requests/CPU de S02 ainda a validar operacionalmente. Nenhum novo recurso/serviço pago.
- HMAC local detecta adulteração comum; não é defesa absoluta contra dispositivo/processo comprometido nem assinatura financeira. Não prometer relógio seguro em aparelho com root.

### Compatibilidade e decisões necessárias

`OfflinePolicy` atual possui **um** limite Play. Não basta instalar 72h para todos: aprovação de 72h pago / 24h trial exige seleção por `billingPhase` e política versionada/confiável. Os testes propostos exercitam configurações separadas por fase; **não comprovam configuração simultânea instalada**. Runtime continua `policy=null` / `OQ-03-PENDING`.

Para encerrar OQ-03, aprovar explicitamente limites por fonte/fase, falha de âncora, sem extensão de expiry, origem/atualização da política, semântica de verificação da promo e comportamento de review (ou mantê-lo desativado). A fronteira comercial legado <=3.5/12 está encerrada e não entra nessa decisão.

## OQ-10 — entrada voluntária e deferimento conservador

Recomendação: mostrar pendências apenas na **interface principal do CalcMot**, em retorno/ação claramente voluntários, com aviso não modal e recuperação escolhida pelo usuário. Nunca abrir Activity comercial de service/overlay, notificação ou callback financeiro; nunca apresentar por cima de Uber/99. `ON_RESUME` sozinho não prova origem voluntária: pode ser restauração, permissão ou deep link.

Para uma futura apresentação comercial restritiva, exigir conjuntamente:

1. MainActivity do CalcMot retomada e com foco, ação de revisão de acesso voluntária.
2. Sessão de trabalho encerrada explicitamente pelo usuário, com confirmação de processamento concluído; botão de monitoramento desligado sozinho não basta.
3. Nenhuma captura/OCR/análise em andamento, nenhuma oferta em trânsito e nenhum overlay de oferta ainda apresentado.
4. Revalidar os sinais imediatamente antes da apresentação; se houver mudança/inconsistência, adiar. Dados ausentes/desconhecidos também adiam.

Isso é uma condição de UX conservadora, **não comprovação de veículo parado**. Não inferir parada pela falta de oferta, por IDLE, tempo sem evento, app em foreground ou toggle desligado. Não adicionar GPS/sensores/permissões para resolver essa incerteza.

Vencimento gera estado pendente em RAM. Não chamar hideOverlay/stopReading, não cancelar análise, não fazer rede na oferta e não exibir paywall automaticamente. Uma análise iniciada deve concluir com seu contexto consistente; a restrição futura exige regra aprovada de transição segura. Pro temporário não continua autorizando novas ações indefinidamente só porque a interface comercial foi adiada. Nenhum desses gates foi integrado nesta execução.

### Sinais realmente existentes e lacunas

| Fonte inspecionada | Disponível | Limitação real |
|---|---|---|
| `MainActivity.kt` | Lifecycle ON_RESUME para permissões/energia | Não registra origem voluntária nem garante foco/parada. |
| `ui/CalcMotNavigation.kt` / AppSettings | Toggle `monitoringEnabled` | Preferência não confirma fim da sessão nem drenagem de trabalho em trânsito. |
| `ReadingPipelineRuntime.Snapshot` | CAPTURING/OCR/BUSY/IDLE/FAILED, overlayStatus, conexão e alguns tempos | IDLE/default/desconexão não significam veículo parado. Não há protocolo atômico de término de sessão ou geração de apresentação. |

**Bloqueio técnico para ativação:** não há confirmação explícita e consistente de fim da sessão/drenagem nem origem voluntária de entrada. Os testes usam esses sinais como hipóteses identificadas, não como APIs já existentes. Uma futura integração mínima precisa de autorização própria para seam de observação/ack, sem reescrever o Core. Não usar timeouts do watchdog (25s/15min) como política comercial ou “oferta recente”.

**Decisão operacional residual:** se a sessão nunca terminar ou seus sinais se perderem, a UI comercial fica adiada; isso não pode virar acesso temporário infinito. Aprovar a transição/limite de restrição e recuperação que concilie segurança do motorista com o limite finito do direito. Não escolhemos um número de segundos ou duração máxima de condução. Até aprovação e integração, Core atual permanece gratuito e gates OFF.

## Ensaios e evidências

- **Físico:** Samsung SM-A075M, Android 15 / API 35; `EntitlementStoreDeviceTest` executado em `br.com.calcmot.identitydev`: **1 PASS, 0 failures/errors**. Mac/Keystore, AtomicFile, leitura após recriar store, isolamento de UID e rejeição de adulteração realmente exercitados. Apenas fixture sintética local; sem concessão real ou login novo.
- XML: `app/build/outputs/androidTest-results/connected/identityDev/TEST-SM-A075M - 15-_app-.xml`. Comando `connectedIdentityDevAndroidTest` com filtro de classe e opt-in DEV. Não rodou o restante dos testes instrumentados. Não emitimos comando de instalação/desinstalação de `br.com.calcmot`; Shell não permite inventário completo de outro perfil Samsung (user 150), portanto não declaramos inspeção de todos os perfis.
- Esse único teste **não** comprova falha de energia no meio de AtomicFile, morte real do processo, reboot físico, revalidação Google/S07, relógio físico alterado ou uso em corrida. Essas lacunas continuam separadas dos resultados reais.
- **Casos novos de proposta:** dois métodos no repository test parametrizam PAID_PERIOD/72h e TRIAL/24h, promo/24h e expiry anterior; seis métodos de `SafeEntryProposalTest` cobrem app terceiro/background, entrada voluntária durante sessão ativa, ausência de oferta sem prova de término, estados desconhecidos/busy/overlay, alteração de geração e recuperação explícita não bloqueante. Modelo somente em `src/test`, sem código de gate no aplicativo.
- **8 cases novas PASS, zero failures/errors**, executadas isoladamente em `testIdentityDevUnitTest`: 2 repository e 6 SafeEntryProposalTest. As cases parametrizadas incluem limites 72h/24h e fonte menor que a janela; o modelo de sinais permanece somente no teste. Não repetimos a suite de 299 nem fabricamos ensaio físico da estratégia de entrada segura.

Para validar após aprovação futura: bordas 72h/24h/expiry com fonte renovada ou revogada, phase UNKNOWN, combinação de fontes, reboot/relógio no Samsung, tráfego real de revalidação e corrida de apresentação vs nova oferta. Testar foco/origem real MainActivity, ack da sessão, overlay/análise ativos e rede interrompida. Gates continuarão OFF até existir evidência e decisão autorizada.

**Recomendação final:** aprovar OQ-03 com as ressalvas de fonte/recência acima; aprovar o princípio OQ-10 de entrada voluntária e deferimento sob incerteza, resolvendo separadamente o limite/transição durante sessão persistente. Este documento não é aprovação do proprietário nem autorização para implementar UI comercial ou alterar pipelines.
