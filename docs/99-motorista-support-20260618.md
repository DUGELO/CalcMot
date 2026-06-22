# Suporte ao 99 Motorista

## Evidência do aparelho

Validação realizada em 18 de junho de 2026:

- aparelho: Samsung SM-A075M;
- package confirmado: `com.app99.driver`;
- versão confirmada: `7.10.26` (`versionCode 1207102615`);
- atividade principal observada: `com.didiglobal.driver.main.HomePageActivity`;
- serviço de acessibilidade do CalcMot ativo;
- estado real observado: Home da 99 em `Buscando`.

O package legado `br.com.taxis99` permanece como alternativa conhecida, mas não estava instalado no aparelho validado.

## Proteção da Uber

O caminho da Uber continua delegando diretamente para:

- `OfferTreeExtractor`;
- `OfferParser`;
- as mesmas regras de estabilidade e cálculo.

O dispatch multiapp ocorre antes do parsing. A pipeline específica da 99 não altera heurísticas da Uber. Os 39 fixtures de produção da Uber são comparados pelo teste de caracterização através do dispatch novo.

## Isolamento entre apps

Ao detectar troca de Uber para 99 ou de 99 para Uber, o serviço:

1. cancela a captura em andamento;
2. limpa o stability gate e deduplicação;
3. remove o overlay visível;
4. descarta auditoria do candidato anterior;
5. inicia a nova sessão usando somente roots e linhas do package do app atual.

Fingerprints da 99 recebem escopo de origem no coordenador. Fingerprints da Uber permanecem no formato legado.

## Card real observado

Durante o E2E surgiram duas ofertas reais. A primeira:

- categoria: `Pop`;
- ação: `Escolher`;
- tarifa: `R$15,20`;
- embarque: `8 min`, `2,5 km`;
- viagem: `13 min`, `14 km`;
- versão da 99: `7.10.26`.

Endereços foram removidos do fixture `app/src/test/resources/99/offer-list-7.10.26-anonymized.txt`.

A segunda oferta real usou o formato `Negocia`:

- ação: `Aceitar por`;
- tarifa oferecida: `R$6,98`;
- embarque: `5 min`, `1,1 km`;
- viagem: `5 min`, `2,3 km`;
- havia valor por quilômetro e opções de contraproposta.

Os endereços foram removidos do fixture `app/src/test/resources/99/offer-negocia-7.10.26-anonymized.txt`. O teste garante que o parser escolhe `R$6,98`, ignorando `R$/km`, a repetição no botão e as contrapropostas.

## Estado do parser da 99

A infraestrutura e o parser conservador da 99 estão implementados. Ele exige:

- uma tarifa principal;
- dois blocos de tempo e distância;
- evidência de ação ou papéis explícitos de embarque e corrida;
- métricas financeiramente válidas.

Ganhos, saldo, histórico, bônus e cards incompletos são rejeitados.

O parser cobre os dois formatos visuais reais observados.

Na segunda chamada, a notificação de disponibilidade apareceu na árvore de acessibilidade. O cartão completo foi aberto às `19:54:40`, mas a sessão detalhada de captura começou às `19:54:46`. Assim, essa execução não autoriza concluir que o cartão completo seja inacessível; a coleta perdeu a janela crítica.

Também foi corrigida, exclusivamente para a 99, a agregação de múltiplas janelas interativas. A Uber conserva o caminho anterior e possui teste de prontidão que fixa essa separação.

O overlay, a troca segura entre apps, a descoberta de package e o parser estão validados. O projeto não declara 100% de acerto na 99 enquanto uma captura simultânea do cartão real não confirmar a origem semântica de todos os campos.

## Comandos

```powershell
.\scripts\run-production-e2e-qa.ps1 -DriverApp 99
.\scripts\run-uiautomator-bridge.ps1 -DriverApp 99 -CaptureScreenshots
.\scripts\capture-accessibility-lab.ps1 -DriverApp 99
.\scripts\analyze-calcmot-latency.ps1 -DriverApp 99
```
