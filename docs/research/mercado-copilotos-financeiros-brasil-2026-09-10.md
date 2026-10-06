# Mercado brasileiro de copilotos e ferramentas financeiras para motoristas

**Recomendação:** posicionar o CalcMot como uma memória financeira confiável que ajuda o motorista a fechar o dia e melhorar decisões futuras. Priorizar conciliação de recebimentos; validar, em seguida, rentabilidade do ciclo completo e planejamento para uma meta líquida. Calculadora, semáforo, gráficos, câmera e cadastro de despesas são recursos de apoio e aquisição, com pouca defesa competitiva isoladamente.

O principal ativo potencial é a sequência **oferta observada → viagem confirmada → valor recebido → custo e tempo realizados → correção do motorista**. Hoje o CalcMot possui partes dessa infraestrutura, mas a base acessível não comprova essa sequência em produção. O moat proposto ainda precisa ser construído e demonstrado.

**Escopo e qualidade da evidência.** Corte da análise: 10 de setembro de 2026. Mercado brasileiro, motorista individual de Uber/99 e distribuição Android. Funcionalidades e preços foram confrontados com páginas oficiais e listagens públicas; avaliações são relatos qualitativos, não amostras representativas. Dados locais provêm de código, documentos de sessões reais e artefatos existentes no workspace, cuja referência Git é `1db7bf9`, com alterações locais substanciais. Não houve acesso a métricas de receita, retenção, assinantes ou a uma base operacional de usuários. As oportunidades, preços de teste, prazos e critérios de aprovação abaixo são hipóteses de produto, não resultados obtidos.

**1. Mercado, comprador e limites do dimensionamento**

A divulgação sobre 2025 reportada pela Agência Brasil em 04/09/2026 aponta aproximadamente 1,959 milhão de trabalhadores por aplicativos, dos quais cerca de 1,2 milhão ligados a transporte de passageiros. Outra reportagem da mesma divulgação apresenta 1,8 milhão na ocupação principal e 1 milhão em transporte particular de passageiros. O recorte de trabalho principal versus complementar explica parte da diferença; as categorias detalhadas das matérias não devem ser somadas sem as tabelas originais. A publicação primária completa de 2025 não ficou acessível nesta análise. Portanto, esses números servem para indicar ordem de grandeza, não para calcular o mercado pagante do CalcMot. [Agência Brasil][s01], [Poder360][s02].

Como referência anterior, o relatório governamental de março de 2026, baseado na PNAD 2024, registra 964 mil pessoas no trabalho principal por aplicativos de transporte de passageiros. Essa população não equivale a assinantes potenciais: há sobreposição entre plataformas, aparelhos incompatíveis, uso ocasional e usuários satisfeitos com ferramentas gratuitas. Downloads de concorrentes também não equivalem a usuários ativos, pessoas distintas ou receita. [Secretaria-Geral — relatório GTT][s03].

O mercado comporta empresas relevantes de nicho, mas o preço dos utilitários é pressionado. Não há evidência pública suficiente nesta análise para atribuir alto willingness-to-pay a uma funcionalidade nova. Preços anunciados mostram a oferta comercial; pagamento e renovação de motoristas mostram demanda. Essa distinção deve comandar o investimento.

| Segmento | Dor e recorrência esperadas | Hipótese comercial | Prioridade |
|---|---|---|---|
| Motorista frequente, usa Uber e 99, já paga algum copiloto | Fechar ganhos, dinheiro e repasses; confiar nos números | Maior chance de pagar por melhoria demonstrável, mas exige vantagem sobre a assinatura atual | Primeiro piloto |
| Motorista com aluguel ou prestação e compromisso semanal | Saber quanto pode retirar e quantas horas faltam | Dor recorrente forte; pouco espaço para mensalidade sem retorno claro | Estratificar no piloto |
| Motorista com horário familiar rígido | Maximizar resultado dentro de horas disponíveis | Planejamento pode valer mais que recomendação de corrida isolada | Segunda validação |
| Uso ocasional como renda complementar | Baixo volume de dados e de uso | Maior dificuldade para assinatura contínua e aprendizagem personalizada | Não é foco inicial |
| Frotas, crédito, seguro e intermediação financeira | Comprador e operação diferentes | Exigiriam outra estratégia, governança e distribuição | Fora da recomendação atual |

Esses segmentos são escolhas analíticas, não personas confirmadas por entrevistas. Recrutar pessoas que usam os dois apps não implica que o produto consiga observar simultaneamente todas as ofertas dos dois.

**2. Comparação competitiva**

“Anunciado” significa descrito pelo fornecedor. Não houve teste prático dos concorrentes. Ausência em uma página não prova ausência no produto.

| Produto | Funcionalidades anunciadas relevantes | Monetização pública observada | Limitação/gap que importa para o CalcMot |
|---|---|---|---|
| GigU, ligado à StopClub | Semáforo, R$/km e R$/h, custos/lucro, histórico, mapa, voz e câmera | R$ 12,90/mês; R$ 114,99/ano; teste de 7 dias. Câmera anunciada como gratuita | Amplitude e distribuição fortes. Leitura confiável e resultado financeiro realizado precisam ser comparados em campo. [Site][s04], [Google Play][s05] |
| rebU | Cálculo de ofertas Uber, controle financeiro, corrida particular, melhores regiões, grupos e segurança | Compras no app; tabela oficial distingue gratuito/Premium e menciona vitalício. Preço atual não confirmado | Concorrente de rotina ampla; relatos de travamentos e consumo de recursos. Não confundir pesquisa promocional antiga com ganho causal. [Google Play][s06], [Tabela Premium][s07] |
| Zeca | Semáforo, histórico, mapa, câmera e compatibilidade com várias plataformas | R$ 29,90/ano na descrição; anúncios e compras no app | Âncora extremamente baixa para ferramentas básicas. Reformulação de receitas/despesas anunciada em setembro. Avaliações textuais suficientes não ficaram acessíveis para diagnóstico próprio de falhas. [Google Play][s08] |
| Rota Pro | Custo por veículo, histórico, melhores horários, jornada, voz, prints e câmera | R$ 97/ano anunciado; também informa 12 × R$ 12,45, total de R$ 149,40 | Cobertura anunciada já ocupa grande parte das ideias óbvias. Parcelamento não é equivalente ao total à vista. FAQ aponta dependência de versão, fonte, idioma e incompatibilidade com Android Go. [Site][s09] |
| RODÔapp | Ganhos/gastos por lançamentos, metas dinâmicas e sazonais, combustível, odômetro e exportação | R$ 19,90/mês; R$ 97/ano; teste PRO de 7 dias | Entrada simplificada ainda depende do motorista. O próprio fornecedor declara não conectar contas Uber/99. Não inferir integração automática a partir do nome RodoCalc. [Site][s10] |
| Copilote | Painel financeiro, eficiência por turnos/zonas, manutenção e relatório PDF | Preço não confirmado na página pública acessível | Evidência principalmente comercial, sem auditoria do produto. Demonstra saturação também no posicionamento de “lucro real”. [Site][s11] |
| Drivvo | Abastecimentos, despesas, manutenção e relatórios | Página oficial anuncia uso pessoal gratuito; plano de frotas separado | Substituto forte para gestão do veículo. Lembrete de óleo ou registro de combustível isolado não justifica novo produto premium. [Preços][s12] |
| Uber Driver | Extratos, dados de ganhos, promoções e tendências de demanda/ganhos | Recursos incorporados à plataforma | Possui informação de viagens superior à de um observador de tela; escopo centrado na própria operação. [Mapa de ganhos][s13], [Acompanhamento][s14] |
| 99 Motorista | Valor/km, Central de Ganhos, incentivos, jornada e demonstrativos | Recursos incorporados à plataforma | Parte do cálculo já é nativa. Demonstrativos mensais/anuais em PDF tornam exportação simples pouco diferenciada. Condições variam por produto/cidade. [Pacote de ganhos][s15], [Demonstrativos][s16] |

Os preços são os anunciados na consulta, sem compra ou verificação de elegibilidade no checkout. R$ 29,90/ano equivale a cerca de R$ 2,49/mês; R$ 97/ano, a R$ 8,08/mês; R$ 114,99/ano, a R$ 9,58/mês. Equivalência econômica não significa opção de cobrança mensal por esses valores.

Há três grupos competitivos: decisão instantânea, organização financeira e recursos nativos das plataformas. A oportunidade está na ligação entre eles. Uma lista maior de funções aumenta custo de suporte e permissões sem necessariamente aumentar disposição a pagar.

**3. Reclamações e necessidades mal resolvidas**

A amostra foi intencional: resenhas expostas nas lojas e reclamações localizadas por assunto. Ela identifica modos de falha, sem estimar prevalência, churn ou incidência de erro. Reclamações são alegações; respostas das empresas e datas alteram sua interpretação. Não foram reproduzidos nomes de consumidores nem endereços de viagens.

| Evidência pública | Leitura correta | Consequência para o produto |
|---|---|---|
| Resenhas do GigU de abril/2025 e julho/2026 relatam números inconsistentes, ausência de leitura, aquecimento e bateria | Casos específicos; a loja também mostra avaliação geral elevada e respostas de suporte | Medir precisão, cobertura, latência e consumo separadamente. Um card rápido com valor errado destrói confiança. [Google Play][s05] |
| Resenhas do rebU de maio/agosto de 2025 mencionam peso, travamento do valor e desativação do cálculo | Relatos antigos, que não provam a situação da versão atual | Não vender confiabilidade sem teste longitudinal em aparelhos modestos. [Google Play][s06] |
| Reclamação sobre rebU, julho/2025, com avaliação final em maio/2026, atribui insatisfação à interrupção do R$/km após compra vitalícia | Cadastro da empresa no portal exige cautela de atribuição; é indício complementar | Receita vitalícia financia mal uma obrigação indefinida de adaptação a terceiros. [Reclame Aqui][s17] |
| Reclamações GigU de abril/maio de 2026 tratam de cancelamento e conta usada na Google Play | No caso de abril, constava resolvido; a resposta explica vínculo da assinatura à conta Google | Recuperação da compra, cancelamento e exclusão precisam ser claros e distintos. Não usar fricção como retenção. [Caso abril][s18], [Caso maio][s19] |
| Reclamações Uber/99 de maio/setembro de 2026 relatam diferenças entre oferta e pagamento | Não demonstram desconto indevido em cada caso; recálculo, pagamento em dinheiro e associação errada devem ser descartados | Investigar conciliação assistida, sem acusação automática e sem promessa de restituição. [Uber][s20], [99][s21] |
| Relato de desenvolvedor/motorista sobre dificuldade de controle em Campinas | Origem promocional e autodeclarada; evidência fraca para demanda independente | Tempo de lançamento deve ser medido diretamente, em vez de presumir que outro dashboard resolve o problema. [Relato público][s22] |

A própria 99 descreve exceções em que o valor final muda, como alteração de destino, paradas e diferenças relevantes de tempo/distância; a regra de 99Negocia é distinta. Um auditor precisa conhecer a categoria e o contexto antes de chamar uma diferença de erro. [99 — tabela de ganhos][s23].

Os gaps mais relevantes são **confiabilidade operacional**, **fechamento sem dupla contagem**, **tempo improdutivo omitido da análise** e **tradução do histórico em uma decisão pessoal verificável**. “Ninguém resolve isso” seria uma conclusão excessiva: há concorrentes cobrindo partes desses problemas, mas não foi encontrada evidência pública suficiente de resolução consistente de ponta a ponta.

**4. O que o CalcMot tem de fato**

| Capacidade encontrada | Evidência local | O que permite concluir |
|---|---|---|
| Oferta com tarifa, km/min de embarque e viagem e nota opcional | [OfferCandidate][l01] | Permite calcular proposta por km/h incluindo embarque. Não contém destino, coordenadas, pagamento realizado ou identificador oficial da viagem |
| Impacto em relação à meta e cálculo de rentabilidade parametrizada | [FinancialImpact][l02], [Profitability][l03] | Diferença para a meta não é dinheiro economizado. Modelo de combustível/manutenção depende de custos informados; não mede lucro econômico completo |
| Captura Uber por acessibilidade e caminho isolado de OCR 99 | [Serviço][l04], [Captura 99][l05] | Reaproveitamento técnico real, com dependência da interface da plataforma |
| Ledger Room no workspace | [Modelos][l06], [Repositório][l07] | Separa oferta, viagem, ganho, despesa, sessão, confiança e correção. É WIP; schema não prova preenchimento ou publicação |
| Registro de ofertas com controle do usuário | [Recorder][l08], [AppSettings][l09] | Histórico depende de habilitação/consentimento; começa desativado. Uma oferta registrada como CONFIRMED continua com realização UNKNOWN |
| Associação conservadora e interface de extratores | [Associação][l10], [Perfis][l11] | A lista de perfis financeiros habilitados em produção está vazia. Não há fundamento para afirmar conciliação automática de extratos pronta |
| Análise por hora | [FinancialAnalytics][l12] | Média e mediana de taxas das ofertas, com mínimo de três registros por hora. Isso não é lucro/hora da jornada nem amostra suficiente para previsão robusta |
| Sessões e retenção | [Repositório][l07] | Sessão estimada por atividade e janela de inatividade de 90 minutos; expurgo após seis meses. Não mede continuamente tempo online nos dois apps |
| Relatórios locais | [ReportExporter][l13] | Exportação PDF/XLSX inicial; PDF limita a listagem a 32 ofertas. Não é um extrato anual completo ou comprovante oficial de renda |
| Telemetria e estudo de deduplicação | [TelemetryProvider][l14], [MetricsResearch][l15], [SafeTelemetryPolicy][l16] | Instrumentação com filtros e buckets; estudo opcional. Não é uma base central de viagens detalhadas, nem prova de retenção ou pagamento |
| Área de gravação | [SecurityRecordingScreens][l17] | Há interface e planejamento. Não foi encontrado motor de gravação operacional nesse módulo; não tratar câmera como capacidade entregue |

Campos como categoria, bônus, tipo especial e quantidade de avaliações existem no modelo do ledger, mas a observação atual não os preenche necessariamente. O caminho consultado parte do OfferCandidate, que não possui esses atributos. “Ter coluna no banco” e “ter dado observável e validado” são estágios distintos.

Também há tela Premium e evento de paywall no código, mas não foi encontrada integração de Play Billing nos fontes/dependências examinados. Não há prova local de assinatura ativa, conversão ou receita. A documentação de entrega ainda descreve monetização inicial sem cobrança. [PremiumScreen][l18], [Notas da loja][l19].

**Dados reais disponíveis e seu alcance.** O relatório da sessão Uber de 01/06/2026 registra 430 frames, 133 frames com oferta completa e 41 fingerprints únicos. A tarifa média reportada é R$ 18,95; distância média total de 13,8 km; tempo médio de 27,2 minutos; média de R$/km de 1,55, com intervalo de 1,09 a 3,71. São métricas documentadas daquela sessão, não uma reexecução do corpus nesta análise. [Relatório da sessão][l20].

Os 30,93% de frames completos **não são recall do parser**: o denominador inclui tela sem card e estados incompletos. As 41 ofertas não são 41 viagens, motoristas ou observações independentes de uma população brasileira. A média das razões pode ser 1,55 enquanto a razão das médias é aproximadamente 18,95/13,8 = 1,37; são medidas diferentes. Nenhuma mede lucro líquido realizado.

As quatro ofertas reais anonimizadas documentadas em 19/06/2026 permitem verificar o efeito do embarque. Os cálculos abaixo foram refeitos a partir das tarifas e distâncias/tempos publicados no documento, sem abrir ou redistribuir capturas brutas. [Sessão 99][l21].

| Tarifa observada | Km de embarque + viagem | Minutos totais estimados | Receita/km total | Receita/h estimada |
|---:|---:|---:|---:|---:|
| R$ 9,51 | 3,20 + 6,20 = 9,40 | 18 | R$ 1,01 | R$ 31,70 |
| R$ 8,30 | 3,80 + 5,30 = 9,10 | 18 | R$ 0,91 | R$ 27,67 |
| R$ 7,00 | 0,45 + 2,70 = 3,15 | 8 | R$ 2,22 | R$ 52,50 |
| R$ 7,00 | 0,41 + 1,90 = 2,31 | 8 | R$ 3,03 | R$ 52,50 |

Na segunda oferta, usar apenas os 5,3 km da viagem daria R$ 1,57/km. Incluindo o embarque, dá R$ 0,91/km: uma aparência 72% superior quando se omite o deslocamento. Isso demonstra um problema concreto de denominador. Não demonstra prejuízo, porque faltam custos e execução. As duas ofertas de R$ 7 também mostram que tarifa idêntica não identifica viagem repetida.

O corpus disponível é útil para regressão e investigação técnica, mas insuficiente para treinar inteligência regional, provar aumento de renda, estimar taxa de aceite ou calcular LTV. Não há nos dados acessíveis o elo completo com viagens concluídas, odômetro, tempo ocioso e recebimentos. Esse é o primeiro investimento em dados que faz sentido.

Para medir resultado, manter três contas explícitas: caixa disponível, margem operacional e lucro econômico. Compra de combustível e consumo no período podem diferir; provisão de manutenção e despesa realizada não devem ser descontadas duas vezes. A prestação integral do financiamento pesa no caixa, enquanto juros e depreciação têm outro tratamento na análise econômica. Antes de personalizar recomendações, definir convenções de custo consistentes por veículo e não chamar receita menos despesas cadastradas de lucro completo quando faltarem custos.

**5. Limitações técnicas, Play Store e LGPD**

| Tema | Situação e implicação |
|---|---|
| Compatibilidade real | `minSdk 24` e `targetSdk 36` no projeto. O OCR 99 usa `takeScreenshot` em API 30+; abaixo disso seleciona fonte sem suporte. O XML específico v30 declara captura. Logo, instalar no Android 7 não garante leitura 99. [Gradle][l22], [Captura][l05], [Configuração v30][l23] |
| MediaProjection | Documentos locais citam possibilidade, mas não foi encontrada implementação desse fallback. Android exige consentimento por sessão e configuração de serviço aplicável. Não projetar captura contínua universal como capacidade gratuita. [Android][s24] |
| Bloqueio de captura | Janelas protegidas podem impedir screenshots. OCR não é mecanismo legítimo para contornar controles de segurança. Se a plataforma deixar de fornecer texto/imagem acessível, aceitar perda de cobertura. [Android — FLAG_SECURE][s25] |
| Observação entre apps | Acessibilidade vê conteúdo exposto nas janelas disponíveis; tela em primeiro plano, transições e OEM limitam cobertura. Sessão de serviço ativo não prova que o motorista estava disponível nos dois aplicativos |
| API oficial | A Driver API da Uber é de acesso limitado. Não planejar integração de viagens/pagamentos como garantida. Não foi localizada API pública equivalente da 99 para esse uso; isso não exclui parceria privada. [Uber Developers][s26] |
| Qualidade e bateria | OCR, repetição de eventos, mapas, GPS e câmera competem por CPU, memória e energia. Validar efeito incremental no conjunto real de apps, sobretudo aparelho modesto carregando no carro |
| Acessibilidade na Play | CalcMot declara `isAccessibilityTool=true`, enquanto a documentação local diz que não é ferramenta voltada a deficiência. A política reserva essa flag ao propósito correspondente e exige divulgação/consentimento para outros usos. Há divergência concreta a resolver antes de expandir/publicar. [Política][s27], [XML][l24], [Notas][l19] |
| Automação | A política distingue automação determinística de ações autônomas iniciadas/planejadas/executadas pelo app. Não é correto dizer que todo clique automatizado é proibido; ainda assim, recomenda-se manter decisão e ação com o motorista, considerando também contrato das plataformas e risco operacional. [Orientação da API][s28] |
| Câmera/GPS contínuos | Novas permissões e serviços exigem ciclo de vida explícito. Há restrições de início em background e permissões de uso. O manifest consultado não fornece infraestrutura pronta para todas essas funções. [Android — serviços][s29] |
| Financeiro e privacidade | Registro local não dispensa finalidade, necessidade, segurança e direitos. Consentimento do motorista não equivale ao consentimento de passageiros. Endereços, trajetos e horários podem tornar pessoas identificáveis. [LGPD][s30], [ANPD — perguntas][s31] |

**Divergência de privacidade comprovada no projeto.** A política local, datada de maio, fala em leitura apenas da árvore; as notas de loja falam em ausência de analytics. O código inicializa Firebase Analytics/Crashlytics e possui OCR 99. O manifest mesclado de release existente, de 14/08/2026, inclui INTERNET, ACCESS_NETWORK_STATE e inicializador Firebase. Foi inspecionado como artefato já gerado, sem reconstrução; portanto, não certifica um próximo release. [Política local][l25], [Provider][l14], [Manifest mesclado][l26].

O filtro de telemetria customizada é uma proteção útil, mas não descreve sozinho a coleta automática dos SDKs. Ausência de AD_ID não implica ausência de identificadores de instalação ou coleta. Firebase documenta identificadores próprios e controles de inicialização. É necessário reconciliar código, SDKs, consentimentos, política pública e Data Safety; esta análise não determina que toda coleta existente seja ilegal. [Firebase][s32].

A disputa Uber/StopClub também impede prometer risco zero de bloqueio contratual. O despacho do Cade de 13/07/2026 propôs revisão de investigação arquivada; cobertura especializada de 23/07 informou aprovação da avocação pelo Tribunal. Isso é procedimento concorrencial, não autorização geral para capturar dados, nem condenação final da Uber. Não foi verificada decisão posterior de mérito. [Despacho primário, cópia do DOU][s33], [MLex, resumo público][s34].

**Arquitetura de dados recomendada.** Manter três finalidades separadas: histórico pessoal; diagnóstico de funcionamento; pesquisa opcional para melhorar modelos. Não reutilizar automaticamente consentimento do estudo de deduplicação para construir mapa coletivo. Normalizar dados no aparelho e descartar texto/imagem desnecessários. Registrar origem, versão da extração, confiança, correção e cobertura; permitir exportar e excluir sem assinatura. Projetar retenção por finalidade: seis meses são insuficientes para comparar sazonalidade anual, mas guardar tudo indefinidamente também não é solução. [ANPD — legítimo interesse][s35], [Direitos][s36].

Uma futura contribuição coletiva deve evitar trajetos individuais e localização exata; agregação espacial/temporal, limite de contribuição e supressão de células pequenas são salvaguardas de projeto, não prova automática de anonimização. Avaliar risco de reidentificação e elaborar RIPD quando pertinente ao risco. Começar por aprendizagem pessoal reduz a necessidade de centralizar dados e evita depender de escala nacional. [ANPD — RIPD][s37].

**6. Oportunidades aprovadas para experimentação**

Passaram três ideias. Nenhuma possui alto WTP já demonstrado. Todas atendem ao filtro por problema recorrente, potencial de retenção e/ou efeito de histórico; só passam a justificar construção ampla após os experimentos. Dificuldade Android usa escala de 1, simples, a 5, dependência intensa de captura, estados e aparelhos. Prazos são estimativas para um engenheiro Android experiente com apoio parcial de QA, excluindo revisão de loja, recrutamento e negociação de API.

| Prioridade | Ideia | Propriedade que justifica investigar | Maturidade atual | Assinatura a testar |
|---|---|---|---|---|
| 1 | Fechamento conciliado e auditor de recebimentos | Problema recorrente; hábito diário; histórico corrigido acumulado | Ledger parcial; faltam entradas de extrato e conciliação validada | R$ 14,90 versus R$ 19,90/mês |
| 2 | Rentabilidade calibrada pelo ciclo completo | Histórico pessoal de espera/retorno; dificuldade de captura e validação | Oferta existente; resultado e contexto ausentes | Pacote total de R$ 19,90 versus R$ 24,90/mês |
| 3 | Planejador para meta líquida e horário de saída | Decisão semanal recorrente; retenção; preferências e resultados acumulados | Análise horária inicial, sem jornada real suficiente | Pacote total de R$ 19,90 versus R$ 24,90/mês |

As faixas se referem a alternativas de pacote, não à soma de mensalidades por módulo. Não ampliar para cinco ou dez apostas apenas para preencher um roadmap.

**Ideia 1 — Fechamento conciliado e auditor de recebimentos**

**Problema.** O motorista precisa entender quanto ganhou, quanto recebeu em dinheiro, quanto falta repassar e quais diferenças merecem conferência. Ofertas, histórico de viagens e saldo da carteira são objetos diferentes; misturá-los gera dupla contagem e perda de confiança. Há relatos públicos concretos de divergências, mas sua frequência e recuperabilidade precisam ser medidas.

**Solução.** Um fechamento de turno de até dois minutos, com valores classificados como confirmado, informado e pendente. Importar primeiro arquivos escolhidos pelo motorista ou registrar totais resumidos; evoluir para perfis de telas específicas somente com validação. Conciliar recebimentos, despesas e ajustes; ligar oferta à viagem apenas quando houver evidência suficiente. Um PDF mensal da 99 ajuda no total do período, mas pode não fornecer detalhe por corrida: usar amostras antes de definir o contrato de importação.

Separar receita de caixa: um repasse bancário de ganhos já reconhecidos não é nova receita. Dinheiro recebido, taxas de plataforma, gorjetas, pedágios e bônus precisam de tratamento explícito. Cada diferença recebe explicação possível e evidência disponível; o motorista decide abrir o suporte e enviar um resumo. Um registro interno ou hash não prova sozinho a tarifa contratada nem garante aceitação da contestação.

**Moat.** Coleção consentida de formatos, regras de associação, casos ambíguos resolvidos e histórico financeiro corrigido. Concorrentes podem copiar a tela rapidamente; precisam de dados e operação para reproduzir uma conciliação confiável. O moat é moderado e condicional: empresas com distribuição maior podem superar o CalcMot. Portabilidade deve ser preservada; aprisionar os dados não é a estratégia.

**Dados necessários.** Oferta com origem/horário; viagem concluída ou confirmação humana; componentes do ganho; modalidade de pagamento; repasses; saldo inicial/final quando necessário; despesas; identificação suficiente para associação sem nome/endereço de passageiro. Incorporar idempotência, reversões e versões de correções. A taxa de fechamento com dados suficientes é tão importante quanto precisão.

**Dificuldade Android: 3/5 no MVP assistido; 5/5 na captura automática ampla.** Estimativa de 4–8 semanas para importação limitada, revisão e conciliação utilizável. Leitura automática de múltiplas telas/plataformas exigiria esforço adicional sem prazo garantido. Processar fora do caminho crítico do overlay; reutilizar o ledger e o motor de associação como base, sem inferir conclusão pelo desaparecimento do card.

**Risco Play Store/LGPD: médio no MVP local; alto se ampliar captura indiscriminada.** Arquivos podem conter identificação e dados financeiros; selecionar campos necessários e não enviar bruto à telemetria. Ampliar finalidade da acessibilidade exige revisão das declarações. Um piloto de importação escolhida pelo usuário reduz essa dependência. Não entrar no app bancário para capturar Pix por acessibilidade.

**Potencial de assinatura: médio, podendo se tornar alto no segmento frequente.** A hipótese é pagar por fechamento recorrente e redução do trabalho de conferência. Auditoria de diferenças sozinha pode ter uso episódico; mantê-la dentro do fechamento. Testar R$ 14,90 e R$ 19,90, incluindo pessoas que já pagam concorrentes. Perguntar qual assinatura seria substituída e observar renovação, não apenas preferência declarada.

**Experimento mínimo.** Vinte motoristas frequentes Uber/99, 14 dias de fechamento assistido e uma primeira renovação paga após 30 dias. Recolher apenas dados consentidos necessários; conciliação inicial pode ser manual, com revisão de cada diferença. Metas propostas: ao menos 14/20 completam 10 de 14 fechamentos; tempo mediano final até dois minutos; zero dupla contagem no conjunto revisado; pelo menos 6/20 pagam R$ 14,90 para continuar e 4 desses 6 renovam. Publicar os números absolutos e perdas de participantes.

**Critério de rejeição.** Se o benefício só existir em raras disputas, se a revisão demorar mais que o controle atual, ou se a entrada exigir credenciais/API inacessível, não construir captura automática. Se ninguém pagar, o ledger permanece infraestrutura e controle básico. Os limites do piloto são decisões de investimento; não representam significância estatística ou previsão nacional.

**Ideia 2 — Rentabilidade calibrada pelo ciclo completo da corrida**

**Problema.** Uma oferta pode pagar bem nos quilômetros exibidos e terminar longe da próxima oportunidade; tempo real, espera e deslocamento vazio podem reduzir o resultado. A operação atual já inclui embarque, mas não sabe quanto tempo/km adicionais se seguirão nem se a estimativa da plataforma costuma errar naquele contexto.

**Solução.** Mostrar uma faixa estimada de margem e tempo por ciclo, com aviso quando o histórico pessoal for insuficiente. Aprender o desvio entre estimado e realizado e o esforço até a próxima viagem, respeitando decisões de retorno do motorista. Começar no relatório pós-turno; só colocar recomendação instantânea no overlay depois de validar a previsão fora da amostra.

Definir ciclo sem sobreposição: início do deslocamento de uma viagem até o início do deslocamento da próxima, alocando espera/rodagem vazia uma única vez. A última corrida precisa de encerramento especial: retorno ao destino escolhido, parada pessoal ou observação censurada. Ausência de próxima corrida não pode ser interpretada automaticamente como demanda zero.

**Exemplo sintético.** Uma oferta de R$ 30 por 12 km e 30 minutos aparenta R$ 2,50/km e R$ 60/h. Com mais 8 km e 20 minutos vazios, o ciclo passa a R$ 1,50/km e R$ 36/h. Supondo custo variável de R$ 0,70/km, restam R$ 16, equivalentes a R$ 19,20/h de margem de contribuição. Não é lucro líquido completo: custos fixos e outros ajustes ainda estão fora. Esse exemplo ilustra a decisão; não descreve ganhos reais do CalcMot.

**Moat.** Histórico longitudinal pessoal com resultados de ciclo, correções e contexto, além de capacidade de capturá-los com baixo consumo. Aprendizagem melhora com uso repetido; esse benefício existe mesmo sem rede. Uma rede futura pode acelerar início em regiões semelhantes, mas requer densidade, consentimento e governança que hoje não existem. Não há vantagem difícil de copiar apenas por usar ML ou por possuir fingerprints.

**Dados necessários.** Resultado da ideia 1; início/fim/pausas confirmados; odômetro ou percurso autorizado; tempo de espera; fim de turno; região ampla selecionada ou extraída sob nova finalidade; custos reais do veículo e estado de captura. Não há coordenadas/destinos no OfferCandidate atual. Para teste inicial, pedir região ampla e leituras de odômetro durante paradas; sem novo GPS contínuo.

**Dificuldade Android: 4/5 para pós-turno; 5/5 para tempo real.** Aproximadamente 8–12 semanas após existir coleta validada, mais semanas de observação. A dificuldade principal é rotular corretamente viagem, ociosidade e fim de jornada. GPS futuro aumenta permissões, consumo e teste em OEM; ausência de cobertura deve produzir abstenção, não número fictício.

**Risco Play Store/LGPD: médio no diário manual local; alto com localização contínua/rede.** Região e horário ainda podem identificar rotina. Não coletar lista de passageiros, histórico de endereços exatos ou risco criminal por perfil. Manter decisão com o motorista e explicar fonte/limites da estimativa. Documentar qualquer nova captura ou compartilhamento; não prometer que integração visual sobreviverá a toda atualização.

**Potencial de assinatura: médio/alto como hipótese de pacote.** Só se a previsão mudar decisões de forma útil e o ganho sobreviver aos custos de uso. Testar pacote R$ 19,90 versus R$ 24,90 após entregar benefício; não cobrar por uma faixa ampla demais para decidir. Aumento de margem deve ser medido por hora de trabalho total, sem incluir como economia valores apenas “abaixo da meta”.

**Experimento mínimo.** Vinte e quatro motoristas, quatro semanas, em uma cidade e poucos tipos de jornada. Coletar pelo menos 20 ciclos completos por participante; separar cronologicamente treino e validação. Comparar modelo pessoal simples com a estimativa do card e uma mediana pessoal sem contexto. Meta proposta: reduzir em pelo menos 20% o erro absoluto médio do tempo de ciclo contra o melhor baseline simples, mantendo previsão utilizável em ao menos 70% dos ciclos. Divulgar erros por motorista e também na cauda, não só média global.

Somente depois fazer piloto prospectivo de orientação com blocos de turnos comparáveis, elegidos previamente pelo motorista. Verificar margem/hora, tempo total, km vazio e proporção de recomendações efetivamente usadas. A ausência de resultado para ofertas recusadas impede afirmar retrospectivamente quanto teria sido ganho com outra escolha.

**Critério de rejeição.** Se uma mediana simples produzir o mesmo resultado, usar a solução simples. Se a coleta for trabalhosa, a cobertura seletiva ou a melhoria desaparecer no período seguinte, não construir motor geográfico. Pedido de interesse ou redução de erro não substitui teste de pagamento e resultado econômico.

**Ideia 3 — Planejador de jornada para meta líquida e horário de saída**

**Problema.** A pergunta recorrente é quanto ainda precisa trabalhar dentro do tempo disponível, incluindo pausas e compromisso de encerrar. Meta bruta dividida por dias e ranking histórico de R$/km não respondem isso. Promoções condicionais podem levar a prolongar trabalho sem certeza de completar os requisitos.

**Solução.** Um plano antes do turno, revisão numa pausa e fechamento no final. Estimar faixa de resultado para blocos de trabalho elegíveis, com custos, disponibilidade pessoal e limite de horas. Exibir probabilidade de atingir a meta apenas quando houver calibração suficiente; caso contrário, apresentar cenários. Comparar continuar, mudar o próximo bloco ou encerrar. Retirada para despesas pessoais, margem operacional e lucro econômico devem ter nomes distintos.

Missões entram pelo ganho incremental provável. Para bônus B, o valor de uma decisão depende da diferença entre a probabilidade de completar a missão com e sem aquela decisão, não de B dividido igualmente por todas as corridas. Regras e progresso precisam ser confirmados; sem isso, omitir a recomendação de bônus. A plataforma já possui informação de incentivos; a vantagem proposta é integrá-los ao custo e à restrição pessoal entre apps.

**Moat.** Histórico de jornadas fechadas, preferências de disponibilidade, previsões anteriores e resultados observados. O plano fica mais adequado à vida daquele motorista ao longo do tempo. É copiável em interface; a defesa vem da confiança do fechamento e da calibração acumulada. Um ranking de horas com nova embalagem reprova o filtro de diferenciação.

**Dados necessários.** Dados conciliados da ideia 1; horas totais, pausas e km de jornada; custo variável e alocação de custos fixos; meta líquida; janelas disponíveis; regras de missão, elegibilidade e progresso voluntariamente informados. A ideia 2 aprimora decisões de ciclo, mas não é necessária para o primeiro planejador de turnos. Bônus não confirmado não entra como recebido.

**Dificuldade Android: 3/5 no planejamento local; 4–5/5 na atualização automática.** Estimativa de 6–10 semanas após a base de jornadas confirmadas. Sem necessidade inicial de mapa vivo ou GPS contínuo. Atualizações devem ocorrer em momentos seguros, evitando telas complexas enquanto dirige. Uma sessão inferida pela chegada de ofertas não basta como denominador de horas.

**Risco Play Store/LGPD: médio.** Dados financeiros e rotina pessoal exigem finalidade e controle. Automatizar alternância de apps ou aceite fica fora da solução proposta. Não recomendar horas extras para compensar fadiga, nem tratar uma previsão como garantia de rendimento. Não oferecer crédito ou promessa de renda mínima embutidos.

**Potencial de assinatura: médio, com hipótese de forte retenção.** Melhor candidato para motoristas frequentes com horas restritas. Testar como evolução do mesmo pacote de R$ 19,90/24,90, sem outra cobrança. Pode ter valor baixo para veteranos cuja própria rotina já supera a recomendação; recrutar esses usuários evita validar apenas com iniciantes entusiasmados.

**Experimento mínimo.** Quinze motoristas com quatro semanas de histórico fechado; entregar planejamento manual individual por três semanas, sem automação. Cada um escolhe previamente dois blocos de horários comparáveis e aceitáveis; alternar orientação/rotina habitual, registrando eventos locais e mudanças de disponibilidade. Meta proposta: uso em pelo menos dois de três planejamentos semanais por 10/15 participantes; redução mediana de 30 minutos para meta comparável, sem queda mediana de margem/hora; ao menos 5/15 pagam R$ 19,90 pelo mês seguinte. A amostra identifica utilidade e atrito, não autoriza promessa causal universal.

**Critério de rejeição.** Se apenas repetir horários que o motorista já conhece, ou aumentar horas para melhorar receita bruta, rejeitar. Se previsão de missão exigir leitura não confiável, retirar a missão do MVP. Se só funcionar numa cidade/faixa, posicionar de forma restrita até replicar.

**7. Capacidade obrigatória de sustentação: qualidade de leitura**

Confiabilidade não deve ser uma quarta assinatura. O investimento defensável é uma operação de compatibilidade: corpus consentido/anônimo, diagnóstico de indisponibilidade por plataforma/versão, limites de consumo, recuperação guiada e desligamento de leitores não validados. Esse trabalho protege retenção das três ideias, mas uma tela de diagnóstico isolada é commodity.

O CalcMot já possui proteção de contexto e instrumentação de latência. Os documentos apresentam metas, como p95 inferior a 700 ms, mas a auditoria consultada ainda pede confirmação com ofertas reais. Não há base para anunciar que o CalcMot é mais rápido que concorrentes. [Auditoria de latência][l27], [Bank Safe Mode][l28].

O primeiro ensaio deve medir: proporção de ofertas elegíveis lidas corretamente; falsos positivos; repetição; tempo da oferta visível ao overlay; consumo incremental e aquecimento; recuperação após perda de serviço. É necessário um conjunto observado de ofertas elegíveis para medir recall. Dez horas com serviço ativo não significam dez horas de dados completos. Testar em aparelhos modestos e diferentes OEMs; nenhuma execução de ADB ou instalação foi feita nesta análise.

**8. Ideias rejeitadas e o motivo**

| Ideia isolada | Decisão | Motivo |
|---|---|---|
| Semáforo, R$/km, R$/h, porcentagem de lucro | Rejeitar como aposta premium | Presentes em vários copilotos; parte do cálculo já é nativa na 99 |
| Custo/km e formulário de combustível | Recurso de base | Necessário aos cálculos, facilmente replicável; custo informado não é dado observado |
| Dashboard de gráficos e exportação PDF/Excel | Recurso de base | Amplamente oferecido; exportar não fecha nem concilia as contas |
| Ranking de melhor hora/região baseado em ofertas | Rejeitar | Concorrentes já anunciam; não mede ociosidade, resultados ou efeito da escolha |
| Meta dinâmica “falta X por dia” | Rejeitar isoladamente | Já existe; só entra no planejador se incorporar restrições e resultados calibrados |
| Lembrete de óleo, pneus e IPVA | Rejeitar isoladamente | Gestão de veículo gratuita/paga já cobre; não há dado mecânico exclusivo no CalcMot |
| Chatbot financeiro genérico/“IA que dá dicas” | Rejeitar | Interface copiável, custo adicional e ausência de dados para resposta melhor |
| Câmera secreta como motivo de assinatura | Rejeitar nesta estratégia | Concorrentes oferecem câmera, inclusive gratuita; alto esforço de energia, armazenamento e privacidade |
| Rede social, botão de pânico e mapa público de risco | Rejeitar no escopo | Dependem de rede/operação e elevam responsabilidade; nenhuma vantagem atual comprovada |
| Aceite/recusa automática, robô de negociação | Rejeitar | Atrito com plataformas e políticas; amplia risco sem demonstrar vantagem de dados atual |
| Calculadora de contraproposta 99Negocia | Rejeitar por ora | Fórmula de piso é copiável; faltam propostas enviadas e resultados para aprender elasticidade pessoal |
| Marketplace de benefícios, combustível e crédito | Rejeitar | Depende de escala e parceiros; competir com distribuição das plataformas desvia do ativo atual |
| Declaração tributária/comprovante “oficial” de renda | Rejeitar como promessa | Dados locais não bastam para finalidade fiscal/probatória; relatórios podem apenas apoiar conferência |
| Mapa nacional de lucro em tempo real | Rejeitar agora | Sem cobertura de jornadas, localização consentida e massa crítica; número de ofertas não resolve esses requisitos |

As funcionalidades descartadas podem ser úteis. A rejeição é ao investimento nelas como tese de diferenciação e assinatura do CalcMot. Não remover trabalho existente apenas por essa classificação.

**9. Monetização e economia da operação**

Recomenda-se **um plano pago inicial**, ancorado no fechamento conciliado, mantendo leitura básica, acesso aos próprios registros e controles de privacidade utilizáveis. Se a coleta pessoal for gratuita, ela deve existir para benefício do motorista; não condicionar o uso a contribuição para pesquisa. Novas capacidades entram no mesmo pacote quando demonstrarem valor.

Testar mensalidade antes de plano anual para observar renovação. Preço vitalício é inadequado para financiar manutenção contínua de captura. Uma oferta anual pode vir depois, com total e renovação claros; desconto não deve esconder baixa retenção mensal. Experimento de preço deve oferecer o mesmo valor a grupos comparáveis, sem cobrança surpresa ou falsa funcionalidade disponível.

Para assinatura autorrenovável no Brasil, a tabela consultada da Google Play mantém referência de 15% nos mercados ainda fora das novas regras regionais. Usar esse cenário no planejamento e confirmar o programa aplicável na implantação. Venda digital dentro do app normalmente exige Play Billing, ressalvados programas/exceções. Pix externo não elimina automaticamente obrigações da loja. [Taxas][s38], [Pagamentos][s39].

| Preço mensal hipotético | Após 15% da loja | Restante com R$ 3 de custo variável | Restante com R$ 8 de custo variável |
|---:|---:|---:|---:|
| R$ 14,90 | R$ 12,67 | R$ 9,67 | R$ 4,67 |
| R$ 19,90 | R$ 16,92 | R$ 13,92 | R$ 8,92 |
| R$ 24,90 | R$ 21,17 | R$ 18,17 | R$ 13,17 |

Tabela de sensibilidade, sem impostos, reembolsos, aquisição e custos fixos. Os custos de R$ 3/R$ 8 são premissas, não custos observados. Em cenário de R$ 19,90 e R$ 3 variáveis, três meses geram aproximadamente R$ 41,75 antes dos itens omitidos; isso mostra como suporte manual frequente e aquisição cara podem consumir a margem. Não há dados para afirmar CAC máximo ou LTV real.

Mil assinantes a R$ 19,90 representam R$ 19.900 de receita bruta mensal; 5 mil, R$ 99.500. São cenários aritméticos, não projeções. O tamanho nacional de motoristas não demonstra que o CalcMot conseguirá adquiri-los. O funil que importa é aparelho compatível → permissões compreendidas → primeiro fechamento útil → pagamento → renovação → contribuição após suporte.

Medir ganho percebido com evidência: tempo economizado, diferença esclarecida/efetivamente recuperada, melhoria em margem por hora total ou redução de horas para objetivo comparável. Evitar somar esses benefícios quando forem a mesma melhoria. Não vender “30% a mais” com base em alegações comerciais de concorrentes.

**10. Sequência de validação e decisões de investimento**

| Etapa | Trabalho | Resultado necessário para continuar |
|---|---|---|
| Dias 1–15 | Recrutamento segmentado, diário de fechamento, comparação com controle atual e amostras consentidas de extratos | Mostrar que há dado suficiente e uma dor recorrente cujo fechamento pode ser simplificado |
| Dias 16–45 | Piloto assistido da ideia 1 e cobrança transparente pela continuidade | Uso repetido, precisão revisada, pagamento e primeira evidência de renovação |
| Dias 46–90, condicionado aos anteriores | Produto mínimo da conciliação; coleta de jornada; estudo silencioso da ideia 2; planejamento manual da ideia 3 quando houver histórico | Priorizar somente a próxima capacidade que superar baseline, gerar uso e pagamento |

O horizonte descreve aprendizado, não promessa de lançar as três capacidades em 90 dias. A captura financeira automática ampla só deve ser orçada depois de testar arquivos/perfis reais. Trabalho de privacidade e qualidade da leitura é requisito para release, com escopo separado da experimentação manual.

Indicadores mínimos: fechamentos por semana trabalhada; tempo de revisão; parcela de receita conciliada; diferenças pendentes; correções por registro; dados insuficientes; margem por hora total; retenção por semana e segunda cobrança; custo de suporte por assinante; cancelamentos e falhas de compra. Comparar pessoas que pararam de dirigir com pessoas que continuam dirigindo e abandonaram o produto.

No recrutamento, incluir assinantes atuais de concorrentes, usuários de planilha/caderno e motoristas sem controle; registrar os grupos separadamente. Incentivo por participação em pesquisa não pode ser contado como disposição a pagar. Observar se o CalcMot substitui uma ferramenta, exige gasto adicional ou só é usado durante acompanhamento humano. Essa última situação pode validar um serviço, mas não uma assinatura de software escalável.

**Decisão recomendada:** financiar primeiro a prova de um fechamento confiável que motoristas queiram repetir e pagar. Reservar rentabilidade de ciclo e planejamento como expansões condicionais. Se o CalcMot não conseguir fechar a conta com pouco esforço, previsões e “IA” apenas multiplicarão a incerteza.

**Fontes e rastreabilidade**

Fontes web consultadas em 10/09/2026. Páginas sem data são retratos da consulta. Lojas e fornecedores descrevem suas próprias ofertas; reclamações não foram verificadas independentemente. Alguns portais bloquearam a abertura integral, embora trechos indexados estivessem disponíveis; nesses casos, a utilização ficou restrita ao trecho e isso é indicado abaixo. Os links locais identificam o material examinado, sem reproduzir dados brutos sensíveis.

| ID | Fonte e data | Uso e limitação |
|---|---|---|
| S01 | [Agência Brasil, Bruno de Freitas Moura, 04/09/2026][s01] | PNAD 2025, texto indexado; abertura integral falhou |
| S02 | [Poder360, Bruna Rossi, 04/09/2026][s02] | Recorte de ocupação principal da PNAD 2025 |
| S03 | [Secretaria-Geral, relatório GTT, março/2026][s03] | Referência à PNAD 2024; trecho indexado |
| S04–S05 | [GigU, site sem data][s04]; [Google Play, atualização 26/08/2026][s05] | Preços, funções e resenhas selecionadas |
| S06–S07 | [rebU, Google Play, atualização 15/04/2026][s06]; [Motorista Top, sem data][s07] | Funções/resenhas; tabela Premium apenas em texto indexado |
| S08 | [Zeca, Google Play, atualização 09/09/2026][s08] | Preço e funções declarados; não confundir com outro app homônimo de abastecimento |
| S09 | [Rota Pro, site sem data][s09] | Oferta, preço, condições de compatibilidade; sem ensaio próprio |
| S10–S12 | [RODÔapp][s10]; [Copilote][s11]; [Drivvo][s12], sem data | Funções e preços comerciais, quando expostos |
| S13–S14 | [Uber, mapa de ganhos, 19/07/2023][s13]; [Acompanhamento de ganhos, sem data][s14] | Recursos nativos; disponibilidade individual pode variar |
| S15–S16 | [99, pacote de ganhos][s15]; [Demonstrativos][s16], sem data | Recursos nativos e PDFs |
| S17 | [Reclame Aqui, rebU, 29/07/2025; atualização 25/05/2026][s17] | Relato complementar; atribuição empresarial não verificada |
| S18–S19 | [Reclame Aqui, GigU, 09/04/2026][s18]; [07/05/2026][s19] | Relatos/respostas sobre cancelamento; consulta indexada |
| S20–S21 | [Reclame Aqui, Uber, 10/05/2026][s20]; [99, 08/05/2026][s21] | Alegações de diferença financeira; trechos indexados, sem auditoria dos casos |
| S22 | [Reddit, relato em Campinas, agosto/2026][s22] | Evidência fraca: relato promocional autodeclarado |
| S23 | [99, nova tabela de ganhos, sem data][s23] | Exceções de recálculo e distinção de categoria |
| S24–S25 | [Android, MediaProjection][s24]; [Proteção de atividades][s25] | Limites técnicos de captura |
| S26 | [Uber Developers, Driver API][s26] | Acesso limitado; nenhuma autorização de API inferida |
| S27–S28 | [Google Play, permissões sensíveis][s27]; [AccessibilityService][s28] | Declaração, consentimento e limites de automação |
| S29 | [Android, restrições de início de FGS, atualização 01/09/2026][s29] | Background, câmera, microfone e localização |
| S30–S31 | [LGPD, texto compilado][s30]; [ANPD, perguntas frequentes][s31] | Bases, princípios e dados identificáveis |
| S32 | [Firebase, identificadores de instalação, atualização 09/09/2026][s32] | Não confundir ausência de AD_ID com ausência de coleta |
| S33–S34 | [Cade, Despacho 37, 13/07/2026, cópia do DOU][s33]; [MLex, 23/07/2026][s34] | Documento primário e resumo público indexado da evolução posterior; sem decisão final verificada |
| S35–S37 | [ANPD, legítimo interesse, 02/02/2024][s35]; [Direitos][s36]; [RIPD][s37] | Governança e salvaguardas |
| S38–S39 | [Google Play, taxas][s38]; [Política de pagamentos][s39] | Economia de assinatura e integração de cobrança |

[s01]: https://agenciabrasil.ebc.com.br/economia/noticia/2026-09/brasil-tem-2-milhoes-de-pessoas-que-trabalham-por-meio-de-aplicativos
[s02]: https://www.poder360.com.br/poder-economia/18-milhao-trabalhavam-por-meio-de-plataformas-digitais-em-2025/
[s03]: https://www.gov.br/secretariageral/pt-br/trabalhadores-por-app/20260324-relatorio-gtt-trabalhadores-por-app.pdf
[s04]: https://gigu.app/br
[s05]: https://play.google.com/store/apps/details?hl=pt_BR&id=co.gigu.app
[s06]: https://play.google.com/store/apps/details?hl=pt_BR&id=com.uberdomarlon.rebu
[s07]: https://motoristatop.com/rebu/
[s08]: https://play.google.com/store/apps/details?hl=pt_BR&id=co.zeca.app
[s09]: https://rotapro.app/
[s10]: https://www.rodoapp.com.br/
[s11]: https://copilote.com.br/
[s12]: https://www.drivvo.com/pt-BR/pricing/
[s13]: https://www.uber.com/br/pt-br/blog/atualizacoes-no-seu-mapa-de-ganhos/
[s14]: https://www.uber.com/br/pt-br/drive/basics/tracking-your-earnings/
[s15]: https://motoristas.99app.com/pacotedeganhos/
[s16]: https://motoristas.99app.com/demonstrativo-de-ganhos/
[s17]: https://www.reclameaqui.com.br/motorista-top/aplicativo-nao-informa-valores-por-km-e-falta-de-suporte_MbWL94-TUCRKDBlb/
[s18]: https://www.reclameaqui.com.br/stopclub-tecnologia-solucoes-e-servicos/impossibilidade-de-cancelamento-e-cobranca-indevida-no-app-gigu_lue6eAY3BFW5nSkH/
[s19]: https://www.reclameaqui.com.br/stopclub-tecnologia-solucoes-e-servicos/rejeitando-a-realizar-o-cancelamento_Ur15uE4QZqtspDPn/
[s20]: https://www.reclameaqui.com.br/uber/motorista-da-uber-reclama-de-reducao-indevida-de-ganhos-em-viagem-e-pede-revisao-do-valor-pago-e-esclarecimento-sobre-atividade-suspeita_KNEF6fgVk0bgyRcl/
[s21]: https://www.reclameaqui.com.br/99taxis/motorista-questiona-discrepancia-entre-valor-ofertado-e-cobrado-em-corrida_iKiV8skhiTXv0WY7/
[s22]: https://www.reddit.com/r/campinas/comments/1vyxf7f/rodo_de_uber_aqui_em_campinas_e_cansei_de_n%C3%A3o_ter/
[s23]: https://motoristas.99app.com/tabela-ganhos-faq/
[s24]: https://developer.android.com/media/grow/media-projection?authuser=0
[s25]: https://developer.android.com/security/fraud-prevention/activities?authuser=4
[s26]: https://developer.uber.com/docs/drivers/tutorials/api/introduction
[s27]: https://support.google.com/googleplay/android-developer/answer/16558241?hl=en&rd=1
[s28]: https://support.google.com/googleplay/android-developer/answer/10964491?hl=en
[s29]: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start?authuser=2
[s30]: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm
[s31]: https://www.gov.br/anpd/pt-br/acesso-a-informacao/perguntas-frequentes/perguntas-frequentes
[s32]: https://firebase.google.com/support/privacy/manage-iids
[s33]: https://static.poder360.com.br/uploads/2026/07/Uber-Stopclub.pdf
[s34]: https://www.mlex.com/mlex/articles/2505258/brazil-cade-tribunal-to-examine-two-complaints-against-uber
[s35]: https://www.gov.br/anpd/pt-br/assuntos/noticias/anpd-lanca-guia-orientativo-sobre-legitimo-interesse
[s36]: https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares
[s37]: https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd
[s38]: https://support.google.com/googleplay/android-developer/answer/112622?hl=en-CA
[s39]: https://support.google.com/googleplay/android-developer/answer/10281818?hl=en
[l01]: ../../app/src/main/java/br/com/calcmot/model/tripData.kt
[l02]: ../../app/src/main/java/br/com/calcmot/model/FinancialImpact.kt
[l03]: ../../app/src/main/java/br/com/calcmot/model/Profitability.kt
[l04]: ../../app/src/main/java/br/com/calcmot/accessibility/UberAccessibilityService.kt
[l05]: ../../app/src/main/java/br/com/calcmot/ninetynine/NinetyNineCaptureSource.kt
[l06]: ../../app/src/main/java/br/com/calcmot/finance/ledger/LedgerModels.kt
[l07]: ../../app/src/main/java/br/com/calcmot/finance/ledger/FinancialLedgerRepository.kt
[l08]: ../../app/src/main/java/br/com/calcmot/finance/ledger/DriverDataRecorder.kt
[l09]: ../../app/src/main/java/br/com/calcmot/AppSettings.kt
[l10]: ../../app/src/main/java/br/com/calcmot/finance/ledger/LedgerAssociationEngine.kt
[l11]: ../../app/src/main/java/br/com/calcmot/finance/ledger/PlatformScreenProfileExtractor.kt
[l12]: ../../app/src/main/java/br/com/calcmot/finance/ledger/FinancialAnalytics.kt
[l13]: ../../app/src/main/java/br/com/calcmot/finance/ledger/ReportExporter.kt
[l14]: ../../app/src/main/java/br/com/calcmot/telemetry/TelemetryProvider.kt
[l15]: ../../app/src/main/java/br/com/calcmot/telemetry/MetricsResearch.kt
[l16]: ../../app/src/main/java/br/com/calcmot/telemetry/SafeTelemetryPolicy.kt
[l17]: ../../app/src/main/java/br/com/calcmot/securityrecording/ui/SecurityRecordingScreens.kt
[l18]: ../../app/src/main/java/br/com/calcmot/ui/PremiumScreen.kt
[l19]: ../../docs/play-store-submission.md
[l20]: ../../docs/uiautomator-session-20260601-metrics.md
[l21]: ../../docs/99-real-offer-analysis-20260619.md
[l22]: ../../app/build.gradle.kts
[l23]: ../../app/src/main/res/xml-v30/accessibility_service_config.xml
[l24]: ../../app/src/main/res/xml/accessibility_service_config.xml
[l25]: ../../docs/privacy-policy.md
[l26]: ../../app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml
[l27]: ../../docs/latency-deep-audit.md
[l28]: ../../docs/bank-safe-mode-latency-report.md
