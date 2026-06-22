# Analise de ofertas reais da 99 Motorista

Data: 19 de junho de 2026

## Ambiente

- aparelho: Samsung SM-A075M;
- package: `com.app99.driver`;
- versao: `7.10.26`;
- resolucao capturada: `720 x 1600`;
- TalkBack e exploracao por toque: desativados;
- CalcMot: servico de acessibilidade ativo;
- enderecos e identificadores pessoais: omitidos deste documento.

## Amostras visuais

Quatro ofertas reais foram preservadas pelo watcher externo:

| Formato | Tarifa | Embarque | Viagem | Observacoes |
|---|---:|---:|---:|---|
| Lista `Negocia` | R$ 9,51 | 7 min / 3,2 km | 11 min / 6,2 km | duas opcoes anunciadas |
| Card expandido | R$ 8,30 | 9 min / 3,8 km | 9 min / 5,3 km | dinheiro, taxa de deslocamento |
| Card expandido | R$ 7,00 | 3 min / 0,45 km | 5 min / 2,7 km | pagamento no app |
| Card expandido | R$ 7,00 | 3 min / 0,41 km | 5 min / 1,9 km | dinheiro, valor por km visivel |

As amostras cobrem:

- distancia em metros e quilometros;
- tarifa repetida em ofertas diferentes;
- dinheiro e pagamento no app;
- taxa de deslocamento separada da tarifa;
- valor por quilometro separado da tarifa;
- perfis Essencial e Premium;
- card em lista e card Flutter expandido.

As quatro transcricoes anonimizadas foram adicionadas a `app/src/test/resources/99`.
O parser exclusivo da 99 passou a:

- converter distancias em metros para quilometros;
- reconhecer o card expandido pela combinacao conservadora de taxa, perfil e corridas;
- reconhecer o formato de lista `Negocia`;
- manter taxa de deslocamento e valor por quilometro fora da tarifa principal.

## Correlacao temporal

O log do sistema registrou uma nova oferta aproximadamente nos instantes:

- 15:54:32;
- 15:55:07;
- 15:55:36;
- 15:56:00.

Cada entrada iniciou:

```text
didi.intent.action.newbroad.order
BroadOrderFragmentGroup
OrderShowFlutterFragment
```

Ao encerrar uma oferta, a 99 limpou e reabriu seu armazenamento privado `order_info`.

Isso indica o fluxo:

```text
backend da 99
-> intent interno de nova oferta
-> estado privado order_info
-> OrderShowFlutterFragment
-> superficie grafica Flutter
```

O CalcMot nao pode ler extras de intents internos nem o armazenamento privado de outro app.

## Resultado de acessibilidade

Durante a sessao que preservou as imagens, o servico havia sido reconectado com a 99 ja aberta e permaneceu no perfil basico. A timeline dessa sessao registrou somente a configuracao inicial, portanto ela nao constitui uma captura simultanea da arvore para os quatro cards.

Capturas anteriores da mesma versao mostraram que `flutter_deal_gesture_container` possui estrutura e IDs, mas nenhum:

- `text`;
- `contentDescription`;
- `stateDescription`;
- hint;
- tooltip;
- extra textual.

Foi implementado um bootstrap exclusivo da 99 pelo root ativo e um heartbeat de leitura da arvore. A Uber continua no watchdog anterior, sem polling adicional.

## Limite de producao

O contrato do projeto proibe OCR, screenshots e MediaProjection em producao. Assim:

- as imagens sao evidencia e dataset de laboratorio;
- imagens nao sao convertidas em candidatos no app;
- logcat, intents internos e `order_info` nao sao fontes de producao;
- o parser da 99 continua aceitando somente texto realmente publicado pela acessibilidade.

Para suporte completo sem OCR, a 99 precisa publicar tarifa, tempo e distancia pela arvore semantica Android durante o card.
