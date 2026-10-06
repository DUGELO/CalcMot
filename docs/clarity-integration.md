# Clarity e monetização — integração de 5 de outubro de 2026

Integração habilitada para todo o app com autorização explícita do responsável após esclarecimento do envio de interações/conteúdo visual à Microsoft. Sem mascaramento, exclusões de Activity, pause/resume ou filtro de telas definidos pelo CalcMot. Permanecem os comportamentos de privacidade e configuração próprios do SDK/painel.

## Instalação e configuração

`com.microsoft.clarity:clarity-compose:3.10.0`, release confirmada nos metadados oficiais Maven Central. `mavenCentral()` já estava configurado. Versão fixa para builds reproduzíveis, em vez de `3.+`.

O identificador fornecido foi colocado somente em `local.properties`, ignorado pelo Git, chave `clarity.projectId`. CI usa `CALCMOT_CLARITY_PROJECT_ID`, com precedência. Não versionar/imprimir o identificador. BuildConfig é gerado em build ignorado. Uma configuração vazia não inicializa o SDK; neste worktree o projeto fornecido está configurado.

Inicialização com `LogLevel.None`, uma vez por processo. MainActivity e SecurityRecordingActivity são pontos de entrada: ambas chamam o mesmo inicializador idempotente em onCreate, cobrindo também abertura fria por notificação. API24–28 não inicializa coleta: a faixa atual suportada pelo Clarity começa em API29. Nenhum minSdk/versionCode/versionName foi alterado.

## Informação para estratégia de monetização

| Sinal | Uso na análise |
|---|---|
| `view_app_*`, `view_recording_*` | Adoção das áreas, fluxo de ativação e abandono antes de chegar ao valor |
| `monitoring_enabled/disabled` | Ativação do benefício central, frequência e fricção |
| `financial_impact_enabled/disabled`, `financial_goal_saved` | Interesse nas ferramentas financeiras e potencial de pacote |
| `paywall_viewed`, `premium_cta_clicked`, `premium_dismissed`, `premium_back` | Interesse versus abandono de uma oferta, quando Premium estiver acessível |
| Eventos existentes sanitizados de oferta/overlay/OCR | Segmentação por plataforma, benefício entregue e problemas que prejudicam retenção |

Os eventos da telemetria existente são encaminhados após seu sanitizer e throttling existentes. Tags prefixadas `calcmot_` reutilizam somente seus parâmetros já sanitizados, incluindo buckets quando disponíveis. Nenhum parser, OCR, serviço de acessibilidade, overlay ou cálculo foi alterado. Custom tags/eventos não acrescentam texto OCR bruto, endereços, arquivos de mídia, URIs, identificadores de sessão da câmera ou custom user ID. Isso não limita a captura visual padrão do SDK nas telas.

Visitas usam nomes de rota agrupados; voltar de outra Activity restaura o nome correto. Sinais iniciais aguardam callback assíncrono de sessão; buffer temporário de startup limitado a 256 itens para proteger memória. O SDK administra buffer/upload/limites de rede próprios. Não configurar consentimento fictício nem chamar APIs com preferências que não foram coletadas do usuário.

## Plano de análise, sem resultados inventados

1. Observar ativação e retorno antes de propor preço: entrada → monitoramento ativo → benefício exibido → retorno. Separar Uber/99 pelas tags já disponíveis. Usar sessões como denominador para não confundir eventos repetidos com pessoas.
2. Comparar uso das ferramentas financeiras e gravação entre usuários que retornam e os que abandonam. Hipóteses a testar: pacote de finanças para uso recorrente; recursos de segurança com valor demonstrável. Adoção isolada não prova disposição a pagar.
3. Para uma oferta acessível, medir `premium_cta_clicked / paywall_viewed` e `premium_dismissed / paywall_viewed` no mesmo período/coorte. Inspecionar replays/heatmaps nos abandonos antes de mudar texto, momento ou apresentação.
4. Testar uma hipótese por experimento, acompanhando retenção/ativação como métricas de proteção. Não escolher preços ou prever receita sem dados coletados.

PremiumScreen existe como componente, mas não foi encontrado um ponto de entrada de produção nesta navegação. A instrumentação foi preparada; não foi criado paywall, cobrança ou checkout. Cliques em “Começar agora” medem interesse, não compra. Não há evento de compra confirmada nem dados de receita para concluir conversão pagante.

## Dados e validação

Clarity pode capturar visuais/interações do app, inclusive áreas financeira e câmera, segundo SDK/painel, e enviar dados à Microsoft. Seus replays não são os arquivos MP4 originais do serviço, mas podem representar conteúdo exibido. Portanto, os contratos/documentos de privacidade anteriores não garantem que todo conteúdo visual da câmera permaneça local depois desta inclusão. Reconciliar política publicada, consentimento aplicável, termos, Data Safety e configurações efetivas antes de release. Autorização do desenvolvedor não equivale a consentimento dos usuários.

Em 5 de outubro de 2026, uma instalação autorizada com assinatura de teste foi usada para testes básicos de abertura e enquadramento. Esses testes não confirmam coleta no painel nem homologam o SDK em produção. Não foi publicado artefato nem confirmada sessão no painel. A confirmação de coleta exige rede e acesso ao projeto. Evidências CAM anteriores cobrem builds anteriores a Clarity; qualificação de consumo/continuidade/privacidade precisa ser refeita com este SDK.

## Fontes oficiais

- [Instalação e APIs](https://learn.microsoft.com/en-us/clarity/mobile-sdk/android-sdk)
- [Suporte Compose](https://learn.microsoft.com/en-us/clarity/mobile-sdk/clarity-compose-sdk)
- [Plataformas e coleta](https://learn.microsoft.com/en-us/clarity/mobile-sdk/mobile-sdk-overview)
- [Regras do painel](https://learn.microsoft.com/en-us/clarity/mobile-sdk/sdk-data-capture-rules)
- [Versões Maven Central](https://repo.maven.apache.org/maven2/com/microsoft/clarity/clarity-compose/maven-metadata.xml)
