# Tutorial de Envio do CalcMot para a Play Store

Última atualização: 27 de maio de 2026.

Este roteiro serve para publicar a primeira versão do CalcMot no Google Play Console usando o arquivo AAB assinado gerado para produção.

## 1. Arquivos prontos

Use os arquivos da pasta `CalcMot Play Store`:

- `CalcMot-v1.0-code1-release-playstore.aab`: arquivo para envio no Play Console.
- `CalcMot-v1.0-code1-release.apk`: APK release para teste local em aparelho real.
- `calcmot-upload.jks`: chave de upload. Guarde com segurança e não envie para terceiros.
- `CALCMOT_UPLOAD_KEY_README.txt`: dados da chave. Guarde em local seguro.
- `TUTORIAL_PLAYSTORE_CALCMOT.md`: este tutorial.

## 2. Criar o app no Play Console

1. Acesse o Google Play Console.
2. Crie um novo app chamado `CalcMot`.
3. Idioma padrão: `Português (Brasil)`.
4. Tipo: `App`.
5. Gratuito na primeira versão.
6. Categoria sugerida: `Auto e veículos`.
7. Público-alvo: adultos em geral.
8. Anúncios: `Não`, se a versão continuar sem anúncios.

## 3. Texto da loja

Sugestão de descrição curta:

`Assistente de leitura de ofertas para motoristas de app, com cálculo local de R$/km, R$/h e tempo total.`

Evite prometer ganhos garantidos. Evite usar marca de terceiros como se houvesse parceria. Use linguagem como `app de motorista` e deixe claro que o CalcMot é independente.

## 4. Política de privacidade

O app já possui uma página interna de política de privacidade. Para envio real na Play Store, prepare também uma URL pública ativa com o mesmo conteúdo de `docs/privacy-policy.md`, porque o Play Console exige um link de política de privacidade no cadastro do app.

Pontos que a política precisa manter:

- O app lê cards de oferta visíveis no app de motorista.
- A leitura da árvore de acessibilidade e os cálculos ocorrem localmente no aparelho.
- Screenshots, endereços, localização e dados de corrida não são enviados a servidores.
- O app não aceita, não recusa e não controla corridas automaticamente.
- Suporte: `eduardoangelo20001@gmail.com`.

## 5. Declaração de Acessibilidade

Na declaração do uso da API de Acessibilidade, explique:

`O CalcMot usa o Serviço de Acessibilidade para identificar cards de oferta visíveis em apps de motorista e calcular métricas locais como R$/km, R$/h e tempo total. O app não executa ações automáticas, não aceita corridas, não recusa corridas e não controla a tela.`

Também informe que o uso principal não é uma ferramenta para deficiência, e sim uma funcionalidade declarada para leitura de ofertas visíveis mediante consentimento do usuário.

## 6. Segurança de Dados

Se a versão continuar processando tudo localmente e sem servidor:

- Coleta de dados: `Não`.
- Compartilhamento de dados: `Não`.
- Dados enviados para servidor: `Não`.
- Dados vendidos: `Não`.

Se no futuro houver login, analytics, pagamentos ou backend, esta seção precisa ser refeita.

## 7. Upload do AAB

1. Vá em `Produção`.
2. Crie uma nova versão.
3. Envie `CalcMot-v1.0-code1-release-playstore.aab`.
4. Preencha notas da versão:

`Primeira versão do CalcMot, com onboarding, disclosure de acessibilidade, monitoramento local de ofertas visíveis, cálculo de R$/km, R$/h e tempo total.`

## 8. Checklist antes de enviar para revisão

- Nome público: `CalcMot`.
- Pacote: `br.com.calcmot`.
- Email de suporte: `eduardoangelo20001@gmail.com`.
- Política de privacidade interna funcionando.
- URL pública ativa da política pronta antes do envio final.
- AAB assinado enviado.
- App testado em aparelho real.
- Serviço de acessibilidade explicado antes de pedir permissão.
- App não executa ações automáticas no app de motorista.

## 9. Teste manual recomendado

1. Instale o APK release no celular.
2. Abra o CalcMot.
3. Leia a tela inicial e a política interna.
4. Ative a acessibilidade manualmente.
5. Ligue o monitoramento.
6. Abra o app de motorista.
7. Confirme que o overlay só aparece em cards completos.
8. Confirme que mapa, popup e card incompleto não mostram overlay.

## 10. Referências oficiais

- Política de dados do usuário: https://support.google.com/googleplay/android-developer/answer/10144311
- Segurança dos dados: https://support.google.com/googleplay/android-developer/answer/10787469
- Uso da API de Acessibilidade: https://support.google.com/googleplay/android-developer/answer/10964491
