# CalcMot Prototypes 2026

Esta pasta guarda o pacote visual canonico do CalcMot para implementacao de telas em Jetpack Compose.

As imagens aqui sao referencia de produto e devem ser tratadas como contrato visual. Ao implementar uma tela baseada nestes prototipos, siga tambem as regras de `AGENTS.md`.

## Uso Obrigatorio

Antes de alterar uma tela:

1. Abra o prototipo correspondente.
2. Compare estrutura, hierarquia, espacamento, cards, tipografia, cores, icones, CTA e estados.
3. Reutilize tokens e componentes do Design System do CalcMot.
4. Valide em device/emulador com screenshot quando a mudanca for visual.
5. Nao altere pipelines Uber/99, OCR, calculo, manifest ou permissoes sem pedido explicito.

## Indice

| Arquivo | Tela / Estado |
| --- | --- |
| `01-feedback-enviado.png` | Feedback enviado |
| `02-home-permissao-necessaria.png` | Home com permissao necessaria |
| `03-premium.png` | Oferta Premium |
| `04-historico-vazio.png` | Historico vazio |
| `05-ajuda.png` | Ajuda / FAQ |
| `06-home-pronto-calcular.png` | Home pronta para calcular |
| `07-permissao-falha.png` | Falha ao ativar permissao |
| `08-config-tamanho-aviso.png` | Configuracao de tamanho do aviso |
| `09-privacidade.png` | Privacidade |
| `10-home-permissao-desativada.png` | Home com permissao desativada |
| `11-enviar-feedback.png` | Enviar feedback |
| `12-config-tema-app.png` | Configuracao de tema |
| `13-menu-lateral.png` | Menu lateral |
| `14-diagnostico-pronto.png` | Diagnostico pronto |
| `15-historico-com-ofertas.png` | Historico com ofertas |
| `16-minha-meta-edicao.png` | Edicao de meta |
| `17-onboarding-inicial.png` | Onboarding inicial |
| `18-onboarding-permissao-acessibilidade.png` | Onboarding permissao de acessibilidade |
| `19-sobre.png` | Sobre |
| `20-config-posicao-aviso.png` | Configuracao de posicao do aviso |
| `21-permissao-aviso-flutuante.png` | Permissao de aviso flutuante |
| `22-home-em-espera.png` | Home em espera |
| `23-minha-meta-salva.png` | Meta salva |
| `24-configuracoes.png` | Configuracoes |
| `25-home-calculo-pausado.png` | Home com calculo pausado |

## Sistema Visual Extraido

- Fundo: preto azulado profundo, com brilho discreto e textura escura.
- Marca: verde CalcMot para sucesso, progresso, foco e destaque.
- CTA principal: azul intenso, alto contraste, altura generosa e texto branco forte.
- Alertas: amarelo para permissao pendente/falha; vermelho para ruim/abaixo da meta.
- Cards: superficies escuras translucidas, borda fria sutil, raio grande e baixo relevo.
- Tipografia: titulos grandes, bold, centralizados em telas hero; listas e configuracoes com leitura rapida.
- Icones: estilo Material/outline consistente, nunca desenhos improvisados quando houver icone oficial adequado.
- Fluxos: telas de estado usam hero visual + card(s) + CTA; telas operacionais usam topbar + cards densos.

## Agrupamento Por Fluxo

- Onboarding e permissoes: `17`, `18`, `02`, `07`, `10`, `21`.
- Home / estado operacional: `06`, `22`, `25`.
- Configuracoes: `24`, `08`, `12`, `20`.
- Meta: `16`, `23`.
- Historico: `04`, `15`.
- Suporte e confianca: `05`, `09`, `11`, `01`, `19`.
- Navegacao e diagnostico: `13`, `14`.
- Monetizacao: `03`.

## Observacoes

- Os nomes originais hash foram normalizados para facilitar implementacao e revisao.
- Os arquivos sao referencias visuais, nao assets finais obrigatorios.
- Se uma tela ja existir no app, a implementacao deve migrar com cuidado para este padrao, preservando comportamento.
