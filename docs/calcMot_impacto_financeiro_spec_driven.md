# Spec-Driven Development — CalcMot: Impacto Financeiro no Overlay

**Produto:** CalcMot  
**Módulo:** Impacto Financeiro / Meta do Motorista  
**Status:** Draft para implementação  
**Prioridade:** P0 após estabilização de captura/overlay  
**Última atualização:** 2026-06-05  

---

## 1. Contexto

O CalcMot já possui um overlay funcional que exibe métricas objetivas da oferta da Uber, como:

- valor por km;
- valor por hora;
- tempo total;
- classificação visual atual do card.

A nova funcionalidade de **Impacto Financeiro** não deve substituir o overlay atual. Ela deve entrar como um **plus opcional no overlay**, permitindo que o motorista veja quanto uma oferta está acima ou abaixo da meta configurada por ele.

O motorista pode:

1. manter a funcionalidade desativada;
2. ativar o impacto financeiro no overlay;
3. usar os valores padrão do app;
4. editar as metas para a realidade dele.

Essa funcionalidade tem como objetivo aumentar o valor percebido do CalcMot, conectando o cálculo técnico diretamente ao bolso do motorista.

---

## 2. Problema

O motorista recebe ofertas rapidamente, muitas vezes cansado e no trânsito. Ele não quer fazer contas.

Ele quer responder rapidamente:

- essa corrida compensa?
- essa corrida está acima ou abaixo da minha meta?
- estou trabalhando dentro do meu padrão mínimo?
- quanto essa oferta está deixando de pagar em relação ao que eu considero justo?
- devo aceitar ou recusar?

O overlay atual ajuda com métricas como R$/km e R$/h, mas ainda exige interpretação. O Impacto Financeiro traduz a oferta para uma mensagem prática.

Exemplo:

```text
R$ 27,92 abaixo da sua meta por km
```

ou:

```text
R$ 12,40 acima da sua meta
```

---

## 3. Objetivo da funcionalidade

Mostrar, de forma simples e opcional, quanto cada oferta está acima ou abaixo da meta financeira do motorista.

A funcionalidade deve calcular o impacto usando:

- meta mínima de R$/km;
- meta mínima de R$/hora;
- valor da oferta;
- distância total;
- tempo total.

---

## 4. Princípios de produto

### P1 — Não substituir o overlay atual

O overlay atual deve continuar existindo com suas métricas principais.

O Impacto Financeiro será um bloco adicional, opcional e configurável.

### P2 — Não mentir para o usuário

Evitar frases como:

```text
Você economizou R$ X
```

Porque o app não sabe com certeza se o motorista aceitou ou recusou por causa do CalcMot.

Preferir:

```text
R$ X abaixo da sua meta
R$ X acima da sua meta
Você recebeu R$ X em ofertas abaixo da sua meta
```

### P3 — Ser entendido em até 2 segundos

O motorista deve entender a mensagem rapidamente, sem esforço cognitivo.

### P4 — Não prejudicar o tempo do overlay

A nova funcionalidade não pode aumentar a latência visual do overlay.

Meta:

```text
visibleLatencyMs <= 300ms
```

Ideal:

```text
visibleLatencyMs <= 150ms
```

### P5 — Rodar offline

Tudo deve funcionar localmente no aparelho.

Sem backend nesta fase.

---

## 5. Escopo

### Incluído no MVP

- metas padrão do app;
- configuração de meta mínima por km;
- configuração de meta mínima por hora;
- ativar/desativar impacto financeiro no overlay;
- cálculo de impacto por km;
- cálculo de impacto por hora;
- classificação da oferta;
- mensagem simples no overlay;
- testes unitários do cálculo;
- persistência local das configurações via DataStore.

### Fora do MVP

- backend;
- login;
- assinatura;
- IA;
- OCR;
- ML Kit;
- ranking;
- gamificação pesada;
- mapa;
- previsão de demanda;
- exportação;
- histórico completo com Room;
- Play Store;
- monetização.

---

## 6. Configuração padrão

O app deve vir com valores padrão para permitir uso imediato:

```text
Meta padrão por km: R$ 1,70/km
Meta padrão por hora: R$ 50/h
Impacto financeiro no overlay: desativado por padrão ou ativado mediante onboarding
Modo padrão: equilibrado
```

Observação: se o usuário não configurar nada, o app pode usar os valores padrão, mas deve deixar claro que são metas editáveis.

---

## 7. Requisitos funcionais

### RF01 — Ativar ou desativar Impacto Financeiro no overlay

**Como motorista**, quero escolher se o impacto financeiro aparece no overlay, para não poluir minha tela se eu não quiser usar essa informação.

**Prioridade:** P0

**Critérios de aceite:**

- existe uma opção para ativar/desativar o Impacto Financeiro;
- quando desativado, o overlay atual permanece igual;
- quando ativado, o overlay atual ganha uma seção adicional;
- a preferência persiste após fechar o app;
- a ativação não quebra o layout atual.

---

### RF02 — Configurar meta mínima por km

**Como motorista**, quero configurar minha meta mínima de R$/km para avaliar se a corrida compensa pela distância.

**Prioridade:** P0

**Critérios de aceite:**

- usuário consegue editar a meta por km;
- valor persiste localmente;
- valor padrão existe;
- não aceita valor zero;
- não aceita valor negativo;
- aceita valores decimais;
- exibe moeda brasileira.

Exemplos:

```text
R$ 1,50/km
R$ 1,70/km
R$ 2,00/km
```

---

### RF03 — Configurar meta mínima por hora

**Como motorista**, quero configurar minha meta mínima de R$/hora para avaliar se a corrida compensa pelo tempo.

**Prioridade:** P0

**Critérios de aceite:**

- usuário consegue editar a meta por hora;
- valor persiste localmente;
- valor padrão existe;
- não aceita valor zero;
- não aceita valor negativo;
- aceita valores decimais;
- exibe moeda brasileira.

Exemplos:

```text
R$ 40/h
R$ 50/h
R$ 60/h
```

---

### RF04 — Usar presets rápidos de meta

**Como motorista**, quero escolher um preset rápido para não precisar configurar tudo manualmente.

**Prioridade:** P1

**Presets sugeridos:**

```text
Conservador: R$ 1,50/km e R$ 40/h
Equilibrado: R$ 1,70/km e R$ 50/h
Exigente: R$ 2,00/km e R$ 60/h
```

**Critérios de aceite:**

- usuário consegue aplicar um preset;
- metas são atualizadas imediatamente;
- usuário ainda pode editar manualmente depois.

---

### RF05 — Calcular impacto financeiro por km

**Como motorista**, quero ver quanto a oferta está acima ou abaixo da minha meta por km.

**Prioridade:** P0

**Fórmula:**

```text
targetValueByKm = totalDistanceKm × minValuePerKm
impactByKm = offerPrice - targetValueByKm
```

**Critérios de aceite:**

- calcula targetValueByKm;
- calcula impactByKm;
- arredonda para 2 casas decimais;
- funciona com qualquer TripData válido;
- rejeita distância zero ou nula.

---

### RF06 — Calcular impacto financeiro por hora

**Como motorista**, quero ver quanto a oferta está acima ou abaixo da minha meta por hora.

**Prioridade:** P0

**Fórmula:**

```text
hours = totalMinutes / 60
targetValueByHour = hours × minValuePerHour
impactByHour = offerPrice - targetValueByHour
```

**Critérios de aceite:**

- calcula targetValueByHour;
- calcula impactByHour;
- arredonda para 2 casas decimais;
- funciona com qualquer TripData válido;
- rejeita tempo zero ou nulo.

---

### RF07 — Calcular impacto final conservador

**Como motorista**, quero receber uma mensagem conservadora para não ser induzido a aceitar corrida ruim.

**Prioridade:** P0

**Regra MVP:**

```text
finalImpact = menor impacto entre impactByKm e impactByHour
```

Motivo:

Uma corrida pode ser boa por hora, mas ruim por km. Nesse caso, o app deve alertar o ponto mais fraco.

**Critérios de aceite:**

- se impactByKm for menor, mensagem prioriza km;
- se impactByHour for menor, mensagem prioriza hora;
- se ambos forem positivos, mensagem informa impacto positivo;
- se um for positivo e outro negativo, classificação é ATENÇÃO.

---

### RF08 — Classificar oferta

**Como motorista**, quero ver se a oferta é ótima, boa, atenção ou ruim.

**Prioridade:** P0

**Classificações:**

```text
ÓTIMA
BOA
ATENÇÃO
RUIM
```

**Regras iniciais:**

```text
ÓTIMA:
valuePerKm >= minValuePerKm × 1.2
e
valuePerHour >= minValuePerHour × 1.2

BOA:
valuePerKm >= minValuePerKm
e
valuePerHour >= minValuePerHour

ATENÇÃO:
passa em uma meta, mas falha em outra

RUIM:
falha nas duas metas
```

---

### RF09 — Exibir Impacto Financeiro no overlay como plus opcional

**Como motorista**, quero ver a mensagem de impacto financeiro no overlay sem perder as métricas atuais.

**Prioridade:** P0

**Critérios de aceite:**

- o overlay atual não é substituído;
- a seção de impacto aparece apenas se a configuração estiver ativa;
- métricas atuais continuam visíveis;
- impacto aparece de forma compacta;
- layout não estoura;
- o texto é compreensível em até 2 segundos;
- cálculo não aumenta visibleLatencyMs acima de 300ms.

---

### RF10 — Gerar mensagem de impacto financeiro

**Como motorista**, quero ver uma frase simples explicando o impacto.

**Prioridade:** P0

**Exemplos de mensagens:**

Oferta ruim:

```text
R$ 27,92 abaixo da sua meta
```

Subtexto:

```text
Abaixo no R$/km
```

Oferta boa:

```text
R$ 12,40 acima da sua meta
```

Subtexto:

```text
Passa no km e na hora
```

Atenção:

```text
Boa por hora, ruim por km
```

Subtexto:

```text
R$ 27,92 abaixo no km
```

Ótima:

```text
Excelente: acima da sua meta
```

Subtexto:

```text
+R$ 18,20 estimado
```

---

### RF11 — Fallback com valores padrão

**Como motorista**, quero que o app funcione mesmo se eu não configurar metas.

**Prioridade:** P0

**Critérios de aceite:**

- se o usuário não configurar metas, usa valores padrão;
- valores padrão são visíveis/editáveis na tela de configuração;
- não exibe erro por ausência de configuração.

---

### RF12 — Registrar oferta analisada localmente

**Como motorista**, quero que o app registre ofertas analisadas para ver meu resumo depois.

**Prioridade:** P1

**Dados mínimos:**

```text
timestamp
fingerprint
valor
km total
tempo total
R$/km
R$/h
impactoKm
impactoHora
finalImpact
classificação
```

**Critérios de aceite:**

- não salvar endereço completo;
- não salvar dados sensíveis desnecessários;
- deduplicar por fingerprint em janela curta;
- registro não pode travar overlay.

---

### RF13 — Criar resumo diário

**Como motorista**, quero ver um resumo financeiro do meu dia.

**Prioridade:** P1

**Métricas:**

```text
Ofertas analisadas
Ofertas ótimas
Ofertas boas
Ofertas atenção
Ofertas ruins
Valor total abaixo da meta
Valor total acima da meta
Média R$/km
Média R$/h
```

**Texto sugerido:**

```text
Hoje você recebeu R$ 183,40 em ofertas abaixo da sua meta.
```

---

### RF14 — Histórico simples

**Como motorista**, quero ver as últimas ofertas analisadas.

**Prioridade:** P2

**Critérios de aceite:**

- lista últimas ofertas;
- mostra classificação;
- mostra impacto;
- mostra horário;
- não exibe endereços completos.

---

### RF15 — Personalização avançada de modo

**Como motorista**, quero priorizar km, hora ou equilíbrio.

**Prioridade:** P2

**Modos:**

```text
BALANCED
PRIORITIZE_KM
PRIORITIZE_HOUR
```

---

## 8. Requisitos não funcionais

### RNF01 — Latência

A funcionalidade não pode aumentar a latência visual do overlay.

```text
visibleLatencyMs <= 300ms
```

Ideal:

```text
visibleLatencyMs <= 150ms
```

---

### RNF02 — Offline-first

Toda regra de cálculo deve funcionar offline.

---

### RNF03 — Persistência leve

Usar:

```text
DataStore para configurações/metas
Room apenas quando histórico/resumo for implementado
```

---

### RNF04 — Privacidade

Não salvar inicialmente:

- endereço completo;
- nome de passageiro;
- localizações detalhadas;
- prints;
- OCR;
- dados desnecessários.

Salvar apenas métricas da oferta.

---

### RNF05 — Não alterar pipeline de captura

Esta funcionalidade não deve mexer no pipeline de AccessibilityService, OfferTreeExtractor ou Overlay State Machine.

Ela deve consumir TripData já validado.

---

### RNF06 — Não quebrar overlay atual

Se a funcionalidade de Impacto Financeiro falhar, o overlay atual deve continuar funcionando.

---

### RNF07 — Testabilidade

O cálculo deve ser isolado em classe pura, testável por unidade.

---

## 9. Contratos de dados

### DriverGoal

```kotlin
data class DriverGoal(
    val minValuePerKm: Double,
    val minValuePerHour: Double,
    val mode: GoalMode = GoalMode.BALANCED,
    val showFinancialImpactOnOverlay: Boolean = false
)
```

### GoalMode

```kotlin
enum class GoalMode {
    BALANCED,
    PRIORITIZE_KM,
    PRIORITIZE_HOUR
}
```

### OfferFinancialImpact

```kotlin
data class OfferFinancialImpact(
    val valuePerKm: Double,
    val valuePerHour: Double,
    val targetValueByKm: Double,
    val targetValueByHour: Double,
    val impactByKm: Double,
    val impactByHour: Double,
    val finalImpact: Double,
    val weakestMetric: ImpactMetric,
    val classification: OfferClassification,
    val message: String,
    val subMessage: String
)
```

### ImpactMetric

```kotlin
enum class ImpactMetric {
    KM,
    HOUR,
    BOTH
}
```

### OfferClassification

```kotlin
enum class OfferClassification {
    GREAT,
    GOOD,
    WARNING,
    BAD
}
```

### OfferAnalysisEntity — P1

```kotlin
data class OfferAnalysisEntity(
    val id: String,
    val timestamp: Long,
    val fingerprint: String,
    val price: Double,
    val totalDistanceKm: Double,
    val totalMinutes: Int,
    val valuePerKm: Double,
    val valuePerHour: Double,
    val impactByKm: Double,
    val impactByHour: Double,
    val finalImpact: Double,
    val classification: OfferClassification
)
```

---

## 10. Regra de cálculo detalhada

### Entrada

```text
TripData:
price
totalDistanceKm
totalMinutes
valuePerKm
valuePerHour

DriverGoal:
minValuePerKm
minValuePerHour
mode
showFinancialImpactOnOverlay
```

### Validações

Rejeitar cálculo se:

```text
price <= 0
totalDistanceKm <= 0
totalMinutes <= 0
minValuePerKm <= 0
minValuePerHour <= 0
```

### Cálculo

```text
targetValueByKm = totalDistanceKm × minValuePerKm
impactByKm = price - targetValueByKm

hours = totalMinutes / 60
targetValueByHour = hours × minValuePerHour
impactByHour = price - targetValueByHour

finalImpact = min(impactByKm, impactByHour)
```

### Classificação

```text
if valuePerKm >= minValuePerKm * 1.2
and valuePerHour >= minValuePerHour * 1.2:
    GREAT

else if valuePerKm >= minValuePerKm
and valuePerHour >= minValuePerHour:
    GOOD

else if valuePerKm >= minValuePerKm
or valuePerHour >= minValuePerHour:
    WARNING

else:
    BAD
```

---

## 11. Exemplo real

Oferta:

```text
R$ 68,98
57 km
58 min
```

Metas:

```text
R$ 1,70/km
R$ 50/h
```

Cálculos:

```text
targetKm = 57 × 1,70 = R$ 96,90
impactKm = 68,98 - 96,90 = -R$ 27,92

targetHour = (58/60) × 50 = R$ 48,33
impactHour = 68,98 - 48,33 = +R$ 20,65

finalImpact = -R$ 27,92
weakestMetric = KM
classification = WARNING
```

Mensagem:

```text
R$ 27,92 abaixo da sua meta
```

Submensagem:

```text
Abaixo no R$/km
```

---

## 12. Layout no overlay

### Regra

O overlay atual permanece como base.

O Impacto Financeiro entra como bloco adicional opcional.

### Estrutura sugerida

```text
[Status atual / semáforo]

R$ 1,21/km
R$ 71,36/h
58 min

[Plus opcional]
R$ 27,92 abaixo da sua meta
Abaixo no R$/km
```

### Quando ocultar o bloco

Ocultar bloco se:

```text
showFinancialImpactOnOverlay = false
```

ou se:

```text
FinancialImpactCalculator retornar erro
```

Neste caso, o overlay atual continua normal.

---

## 13. Backlog por épico

### Épico 1 — Configuração de metas

- US01 — Ativar/desativar Impacto Financeiro no overlay — P0
- US02 — Definir meta por km — P0
- US03 — Definir meta por hora — P0
- US04 — Presets rápidos — P1

### Épico 2 — Cálculo de impacto financeiro

- US05 — Calcular impacto por km — P0
- US06 — Calcular impacto por hora — P0
- US07 — Calcular impacto final conservador — P0
- US08 — Classificar oferta — P0
- US09 — Gerar mensagem de impacto — P0

### Épico 3 — Overlay plus

- US10 — Exibir bloco opcional no overlay — P0
- US11 — Manter overlay atual intacto — P0
- US12 — Fallback seguro se cálculo falhar — P0

### Épico 4 — Persistência e resumo

- US13 — Salvar oferta analisada localmente — P1
- US14 — Criar resumo diário — P1
- US15 — Histórico simples — P2

### Épico 5 — Validação de valor percebido

- US16 — Testar com 5 motoristas por 7 dias — P1
- US17 — Medir entendimento da métrica — P1
- US18 — Medir intenção de pagamento — P1

---

## 14. Plano de implementação

### Sprint 1 — Impacto Financeiro puro + overlay opcional

**Objetivo:** calcular impacto por card e exibir como bloco opcional.

**Escopo:**

- DriverGoal;
- GoalMode;
- OfferFinancialImpact;
- ImpactMetric;
- OfferClassification;
- FinancialImpactCalculator;
- valores padrão;
- showFinancialImpactOnOverlay;
- bloco opcional no overlay;
- testes unitários.

**Não incluir:**

- histórico;
- Room;
- resumo diário;
- backend;
- assinatura.

**Critério de sucesso:**

```text
Todo TripData válido consegue gerar OfferFinancialImpact.
Se showFinancialImpactOnOverlay=true, o overlay mostra o bloco plus.
Se false, overlay atual permanece igual.
```

---

### Sprint 2 — Tela de configurações + DataStore

**Objetivo:** permitir configuração real pelo motorista.

**Escopo:**

- tela de metas;
- DataStore;
- editar meta por km;
- editar meta por hora;
- ativar/desativar bloco no overlay;
- presets.

**Critério de sucesso:**

```text
Motorista consegue ajustar metas para sua realidade.
Configuração persiste ao fechar app.
```

---

### Sprint 3 — Resumo diário

**Objetivo:** mostrar impacto acumulado do dia.

**Escopo:**

- Room;
- OfferAnalysisEntity;
- DailySummary;
- total de ofertas analisadas;
- total abaixo/acima da meta;
- médias R$/km e R$/h.

---

### Sprint 4 — Beta de valor percebido

**Objetivo:** validar se a funcionalidade aumenta percepção de valor.

**Teste:**

```text
5 motoristas
7 dias
```

Perguntas:

```text
Você entendeu a mensagem?
Isso te ajudou a decidir?
Você confiou no cálculo?
Você pagaria R$ 9,90/mês?
Você pagaria R$ 19,90/mês?
Qual métrica mais te ajudou?
```

---

## 15. Testes unitários obrigatórios

### FinancialImpactCalculatorTest

Casos:

```text
1. corrida abaixo da meta por km
2. corrida acima da meta por km
3. corrida acima por hora mas abaixo por km
4. corrida abaixo nas duas metas
5. corrida ótima nas duas metas
6. classificação WARNING
7. classificação BAD
8. classificação GOOD
9. classificação GREAT
10. arredondamento para 2 casas
11. tempo zero rejeitado
12. distância zero rejeitada
13. meta por km zero rejeitada
14. meta por hora zero rejeitada
15. showFinancialImpactOnOverlay=false não quebra overlay
```

---

## 16. QA obrigatório

### QA01 — Overlay atual intacto

Com Impacto Financeiro desativado:

```text
overlay atual deve ficar igual
```

### QA02 — Overlay com plus ativado

Com Impacto Financeiro ativado:

```text
overlay deve mostrar bloco adicional
```

### QA03 — Latência

Comparar:

```text
visibleLatencyMs antes da feature
visibleLatencyMs depois da feature
```

A feature não pode ultrapassar 300ms.

### QA04 — Cálculo visual

Para uma oferta real, conferir manualmente:

```text
valor
km
min
impacto por km
impacto por hora
mensagem
classificação
```

### QA05 — Configuração

Alterar meta por km/hora e verificar se o overlay muda a mensagem no próximo card.

---

## 17. Métricas de sucesso

A funcionalidade será considerada bem-sucedida quando:

```text
1. Todo card com overlay correto também consegue calcular impacto financeiro.
2. O bloco plus aparece apenas se o motorista ativar.
3. O overlay atual continua funcionando sem a feature.
4. O impacto é calculado contra metas configuráveis.
5. O motorista entende a mensagem em menos de 2 segundos.
6. visibleLatencyMs permanece <= 300ms.
7. Em teste com 5 motoristas, pelo menos 3 dizem que pagariam pelo app.
```

---

## 18. Métricas que aumentam valuation

Depois da funcionalidade implementada, medir:

```text
ofertas analisadas por motorista por dia
dias ativos por semana
ofertas abaixo da meta detectadas
ofertas acima da meta detectadas
motoristas que voltam no dia seguinte
motoristas que ativam o bloco plus
motoristas que editam metas
motoristas que dizem que pagariam
```

Essas métricas valem mais que download, porque indicam valor percebido e hábito de uso.

---

## 19. Riscos

### R1 — Mensagem parecer promessa de economia

Mitigação:

Usar linguagem de meta, não de economia garantida.

### R2 — Overlay ficar poluído

Mitigação:

Bloco opcional, compacto e configurável.

### R3 — Aumentar latência

Mitigação:

Cálculo puro, local, barato e testado.

### R4 — Motorista não entender

Mitigação:

Testar textos com motoristas reais.

### R5 — Configuração errada gerar percepção ruim

Mitigação:

Presets e valores padrão.

---

## 20. Decisão de produto

A funcionalidade de Impacto Financeiro será implementada como:

```text
plus opcional no overlay atual
```

e não como substituição do overlay.

O motorista controla:

```text
aparece ou não aparece no overlay
quais metas usar
usar padrão do app ou editar para sua realidade
```

Essa decisão é final para o MVP.

---

## 21. Definition of Done

A feature só estará pronta quando:

```text
- DriverGoal implementado
- DataStore ou fallback de configuração pronto
- FinancialImpactCalculator implementado
- testes unitários passando
- overlay atual preservado
- bloco plus opcional funcionando
- metas padrão funcionando
- metas editáveis funcionando
- visibleLatencyMs <= 300ms
- QA com cards reais validado
- nenhuma alteração no pipeline de captura
```

---

## 22. Prompt de implementação para agente IA

```text
Você é um Senior Android Engineer trabalhando no CalcMot.

Implemente a funcionalidade Impacto Financeiro como um plus opcional no overlay atual.

Regras absolutas:
1. Não substituir o overlay atual.
2. Não alterar pipeline de AccessibilityService, OfferTreeExtractor ou Overlay State Machine.
3. Não reintroduzir OCR, ML Kit ou screenshots.
4. O Impacto Financeiro deve consumir apenas TripData já validado.
5. A seção de Impacto Financeiro só aparece se showFinancialImpactOnOverlay=true.
6. Se o cálculo falhar, o overlay atual continua funcionando sem o bloco plus.
7. O motorista pode usar valores padrão ou editar metas.
8. A feature não pode aumentar visibleLatencyMs acima de 300ms.

Implementar primeiro:
- DriverGoal
- GoalMode
- OfferFinancialImpact
- ImpactMetric
- OfferClassification
- FinancialImpactCalculator
- valores padrão
- testes unitários do cálculo
- integração opcional no overlay

Não implementar ainda:
- histórico
- Room
- backend
- assinatura
- login
- gamificação
- IA

Critério de sucesso:
- Com a feature desligada, overlay atual fica igual.
- Com a feature ligada, overlay mostra impacto financeiro.
- Metas padrão funcionam.
- Cálculo por km e por hora está correto.
- Classificação GREAT/GOOD/WARNING/BAD funciona.
- Testes unitários passam.
```
