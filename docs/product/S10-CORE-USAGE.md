# S10 — recibos locais e uso real do Core

28/10/2026. **PARTIAL — emissão no Core não ativada.** Escopo exclusivo S10; nenhum Admin, serviço remoto, contrato S01, parser, OCR, overlay, cálculo ou permissão alterado. Este relatório diferencia a implementação do consumidor de recibos da identificação do resultado real na origem.

## Implementação

- `CoreAnalysisReceipt`: os sete campos locais de S01, instante UTC original, timezone reconhecido e data local consistente. Sem preço, distância, endereço, imagem ou texto de oferta.
- `CoreAnalysisLedger`: deduplicação por identidade canônica/analysisId; sets de dias; primeira análise; união explícita de identidades e escolha do primeiro instante original. Replay não reinterpreta data/timezone. Demo não modifica estado. A união não é inferida de email ou Analytics.
- `AndroidCoreAnalysisStore`: JSON versionado, `AtomicFile`, leitura limitada a 1 MiB, persistência antes dos eventos, recusa de conteúdo inconsistente/corrompido. Destino previsto: `noBackupFilesDir`, sem backup/reinstalação automática dos recibos.
- `CoreAnalysisRecorder`: fila limitada a 64 comandos, `trySend` não suspensivo, consumidor em `Dispatchers.IO`, falhas de armazenamento/SDK isoladas e sem payload em logs. Não há singleton global nem inicialização no aplicativo.
- Eventos canônicos na infraestrutura existente: `core_analysis_completed`, `first_core_value`, `core_active_day`. Allowlist específica exporta somente `app_family` e `classification`, removendo inclusive buckets financeiros e tags de dispositivo aceitos pelos eventos diagnósticos antigos. Nenhum `first_open` manual.

O commit local antes de S10 preserva trabalho S04–S09 anteriormente não versionado. O diff de S10 deve ser revisado separadamente desse checkpoint, sem incorporar mudanças anteriores como se fossem desta story.

## Ponto de integração investigado e impedimento reproduzível

O candidato mais próximo é `UberAccessibilityService.applyCaptureDecision`, ramo `CaptureDecision.ShowOverlay`, compartilhado pelo fluxo Uber/accessibility e 99/OCR. Ele ocorre depois da aceitação pelo `CaptureCoordinator`, antes da exibição. **Não é, sozinho, o identificador exigido por D11.**

1. Dez frames do mesmo candidato normalmente produzem uma exibição e nove renovações.
2. `expireOverlayIfStale()` apaga o fingerprint corrente. O mesmo candidato depois do TTL produz outra exibição: um UUID por `ShowOverlay` duplicaria a análise. Duas falhas transitórias `INVALID_FRAME` também podem apagar esse estado e gerar outra exibição do mesmo resultado.
3. Um novo candidato com os mesmos valores, sem fronteira semântica observada, produz renovação: o fingerprint sozinho fundiria ofertas diferentes.
4. O estado corrente não fornece ID persistente de resultado; reinício/reconstrução do coordenador perde a correlação. `markSuccessfulRead` é sucesso de leitura/extrator, também não uma conclusão única.
5. A fonte `UIAUTOMATOR_LAB` usa o mesmo caminho e deve ser excluída de qualquer futura integração real. O módulo já recusa `DEMO`, mas o mapeamento da origem ainda não está conectado.

`CoreAnalysisSeamDiagnosticTest` caracteriza essas diferenças. Não foi criado um time-window novo, fingerprint analítico de preço ou heurística de parser. Não foi instalado observer de `offer_parsed`, frame, OCR ou overlay. **Os IDs usados nos testes são explicitamente fornecidos pelo produtor sintético; isso não comprova a geração correta de IDs em ofertas reais.**

### Bloqueios para concluir/ativar

- **S10-SEAM:** estabelecer/revisar a propriedade do resultado lógico na origem, com ID opaco por resultado, continuidade em TTL/refinamento/reinício e fronteira verificável de nova oferta, inclusive com valores iguais. O contrato D11 proíbe substituir isso por janela de tempo/preço. A alteração necessária do lifecycle protegido precisa de revisão explícita; este patch não altera sua lógica funcional.
- **S10-IDENTITY:** S04 preserva UID em `linkWithCredential`, mas não publica um mapa que autorize unir instalação anônima local e conta distinta. O consumidor de união está implementado/testado; não foi ligado a logout/login, email ou mero AuthStateListener. Precisam existir associação legítima, projeto/identidade consolidados e captura do titular original sem race de sessão.
- **S10-TIME:** a origem precisa fornecer confiança temporal e os valores do sucesso original. O consumidor recusa tempo declarado não confiável; não foi inventada uma âncora confiável nem um limiar para corrigir relógio.
- **S10-RETENTION / ASSUMPTION_REQUIRED:** não foi encontrado prazo específico aprovado para recibos/sets de uso nos contratos consultados. Os 30 dias de logs administrativos OQ-07 não foram convertidos silenciosamente em política de Analytics. Limites técnicos de 1.024 recibos/dias e 64 identidades/aliases impedem crescimento ilimitado; ao esgotar, a métrica falha fechada, sem apagar histórico ou bloquear o Core. **Isso não é retenção completa nem autoriza ativação.** É necessário definir descarte/replay antigo e minimização de first-value/dias antes de instalar o produtor.

## Semântica de entrega e identidade

O estado é persistido antes da chamada aos SDKs: há **uma tentativa no máximo**, não garantia de entrega exatamente uma vez no GA4. Crash entre persistência e envio ou falha do SDK pode perder evento; reenvio cego poderia duplicá-lo. Não há outbox remoto ou ACK inventado.

Consolidação une o estado local e não gera eventos. Não consegue retirar eventos já enviados por duas identidades antes do vínculo. G19 aplica o mapa legítimo antes de consumir os recibos; não comprova correção retroativa no GA4. Nenhum UID/analysisId/dedupeKey é exportado para tentar resolver isso.

O armazenamento é dedupe local, não fonte de direitos. Reinstalação remove esses dados em `noBackupFilesDir`; recuperação cross-device de métricas não foi fabricada. Direitos permanentes e os componentes S04–S09 permanecem intactos.

## Validação

**123 testes JVM distintos PASS, zero falhas/erros**, em dois lotes dirigidos. O último resultado por classe substitui o anterior; os três diagnósticos repetidos não foram contados duas vezes.

| Grupo | Testes distintos PASS |
|---|---:|
| CoreAnalysisLedgerTest, incluindo G18/G19 diretamente do contrato, schema, disco JVM, linking, capacidade e concorrência |16|
| CoreAnalysisRecorderTest: fila, indisponibilidade, persistência, ausência de I/O no chamador e privacidade |6|
| CoreAnalysisSeamDiagnosticTest: Uber/99, TTL, valores iguais e retry inválido |4|
| TelemetryPolicyTest: comportamento anterior e três eventos novos aprovados |9|
| CaptureCoordinatorTest + UberPipelineBaselineTest + NinetyNineSanitizerTest |24|
| OverlayStateMachine/WindowOwnership/PendingOverlayOperation/TripQuality |20|
| OfferTreeExtractor/OfferStabilityGate/OfferParser/DurationParser |44|

**Samsung SM-A075M / Android 15 / identityDev:** três execuções instrumentadas PASS, correspondentes a dois cenários: (1) `CoreAnalysisStoreDeviceTest.atomicFilePersistsAcrossOwnersAndRejectsCorruption`; (2) `CoreAnalysisRestartDeviceTest.receiptSurvivesProcessRestart`, executado uma vez para escrever e outra para reler, com `am force-stop br.com.calcmot.identitydev` e confirmação da ausência do processo entre ambas. O replay altera data/timezone de entrada, mas o recibo/dia original permanece e não retorna novo evento. Diretórios sintéticos removidos; metadados `versionCode` e `lastUpdateTime` do pacote de produção comparados antes/depois e inalterados. Não foi desinstalado ou atualizado o CalcMot de produção.

O teste instrumentado usa somente dados sintéticos em diretório próprio do pacote DEV, sem login, ofertas, grants, chamadas ao backend ou chamadas canônicas aos SDKs. Não se alega que a inicialização normal do Application/Firebase preexistente deixou de ocorrer.

Builds `testDebugUnitTest`, `connectedIdentityDevAndroidTest` e `assembleIdentityDevAndroidTest`: **BUILD SUCCESSFUL** na versão final. A primeira execução encontrou a asserção antiga da allowlist: o esperado foi estendido com os três nomes congelados, e a seleção final passou. Uma tentativa anterior sem `calcmot.identityDevInstrumentation=true` não encontrou a tarefa IdentityDev; foi substituída pela tarefa Debug correta. A restrição de leitura do SDK pelo sandbox foi resolvida executando o build local com acesso autorizado, sem modificar SDK/configuração do projeto.

Comandos reproduzíveis (JDK 21/Android SDK 36; conservar os argumentos DEV locais já existentes ao instalar o pacote):

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'br.com.calcmot.telemetry.CoreAnalysis*' --tests 'br.com.calcmot.telemetry.TelemetryPolicyTest' --tests 'br.com.calcmot.processor.CoreAnalysisSeamDiagnosticTest' --tests 'br.com.calcmot.processor.CaptureCoordinatorTest' --tests 'br.com.calcmot.processor.UberPipelineBaselineTest' --tests 'br.com.calcmot.ninetynine.NinetyNineSanitizerTest' --tests 'br.com.calcmot.overlay.*' --no-daemon --max-workers=2
.\gradlew.bat :app:testDebugUnitTest --tests 'br.com.calcmot.processor.CoreAnalysisSeamDiagnosticTest' --tests 'br.com.calcmot.processor.OfferTreeExtractorTest' --tests 'br.com.calcmot.processor.OfferStabilityGateTest' --tests 'br.com.calcmot.processor.OfferParserTest' --tests 'br.com.calcmot.processor.DurationParserTest' --no-daemon --max-workers=2
.\gradlew.bat :app:connectedIdentityDevAndroidTest -Pcalcmot.identityDevInstrumentation=true -Pandroid.testInstrumentationRunnerArguments.class=br.com.calcmot.telemetry.CoreAnalysisStoreDeviceTest --no-daemon --max-workers=2
.\gradlew.bat :app:assembleIdentityDevAndroidTest -Pcalcmot.identityDevInstrumentation=true --no-daemon --max-workers=2
adb install -r app/build/outputs/apk/androidTest/identityDev/app-identityDev-androidTest.apk
$caseId = [guid]::NewGuid().ToString()
adb shell am instrument -w -e class br.com.calcmot.telemetry.CoreAnalysisRestartDeviceTest#receiptSurvivesProcessRestart -e s10Phase write -e s10RunId $caseId br.com.calcmot.identitydev.test/androidx.test.runner.AndroidJUnitRunner
adb shell am force-stop br.com.calcmot.identitydev
adb shell am instrument -w -e class br.com.calcmot.telemetry.CoreAnalysisRestartDeviceTest#receiptSurvivesProcessRestart -e s10Phase read -e s10RunId $caseId br.com.calcmot.identitydev.test/androidx.test.runner.AndroidJUnitRunner
```

Evidências locais ignoradas: `.tmp/s10/jvm-results.json`, `parser-regressions.json`, `jvm-summary.json`, `device-results.json`, `restart-write.txt`, `restart-read.txt`, `restart-summary.json`, `device-preservation.json`. XMLs do último lote JVM em `app/build/test-results/testDebugUnitTest/`; resultado conectado em `app/build/outputs/androidTest-results/connected/identityDev/`. Nenhuma conta real, token ou conteúdo de oferta faz parte dessas fixtures ou do relatório.

Os testes do ledger só comprovam dedupe **recebendo IDs corretos**. Os testes do coordenador comprovam seu comportamento atual. Nenhuma dessas evidências, isolada ou combinada, autoriza declarar a integração de análises reais concluída.

Não houve benchmark p95/p99 de ofertas reais, ensaio de OCR ao vivo ou medição de competição de I/O com o Core. Os parsers/extratores/regressões funcionaram offline com fixtures locais; isso não é homologação física de ofertas Uber/99. Não foi executado `assembleRelease`/`bundleRelease`, publicação ou nova versão comercial.

## Preservação e próximos passos

Não há chamada nova de rede, escrita de métrica ou inicialização Firebase em AccessibilityService/OCR/parser/overlay. Esses arquivos permanecem idênticos ao checkpoint anterior a S10. O produtor não está instalado; não foi enviado evento canônico real nem conteúdo privado de oferta por este patch. Isso não é uma certificação dos mecanismos automáticos de gravação/telemetria preexistentes.

Próximo passo ainda é concluir **S10**, resolvendo os quatro bloqueios específicos acima e então ligar/testar a origem real em Uber e 99. Não iniciar S11–S16, Billing, monetização ou publicação comercial.
