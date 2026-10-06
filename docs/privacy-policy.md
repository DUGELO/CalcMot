# Política de Privacidade do CalcMot

> Revisão de 5 de outubro de 2026: esta política ainda precisa de aprovação para a gravação de segurança e de reconciliação com Firebase/OCR. O contrato de dados implementado e a proposta de texto estão em [camera-data-release-review.md](camera/camera-data-release-review.md). Não publicar esta versão como política validada da câmera.

Última atualização: 27 de maio de 2026.

O CalcMot é um assistente para motoristas de aplicativo. Ele lê cards de oferta visíveis no app de motorista para calcular métricas como R$/km, R$/h e tempo total estimado.

## Dados processados

O app pode processar localmente textos visíveis na tela durante uma oferta, incluindo valor, distância, tempo, nota do passageiro e endereços exibidos no card.

## Acessibilidade

O CalcMot usa o Serviço de Acessibilidade do Android para detectar cards de oferta e exibir um overlay com métricas. O app lê apenas informações expostas pela árvore de acessibilidade do Android, como textos, descrições acessíveis e posição dos elementos na tela.

O processamento acontece no aparelho. O CalcMot não envia cards, screenshots, endereços, localização, nota de passageiros ou dados de corrida para servidores externos.

## O que o app não faz

O CalcMot não aceita corridas automaticamente, não recusa corridas automaticamente, não toca na tela por você e não controla o app de motorista. O app não é afiliado à Uber.

## Análise de uso — revisão pendente

O código inclui Firebase Analytics/Crashlytics e Microsoft Clarity. Clarity foi habilitado para todo o app, sem exclusões de telas ou mascaramento definidos pelo CalcMot, mantendo as regras do SDK/painel. Pode transmitir à Microsoft visuais do app, interações, metadados e identificador gerado pelo SDK. Isso pode abranger conteúdo exibido nas áreas financeira e gravação de segurança, embora os arquivos MP4 originais não sejam enviados pelo serviço de gravação.

Revisar política publicada, termos, consentimento aplicável, retenção e Data Safety. Ver [escopo Clarity](clarity-integration.md). A autorização de integração pelo responsável do app não foi tratada como consentimento dos usuários nem aprovação jurídica.

## Compartilhamento e venda

O CalcMot não vende dados pessoais e não compartilha dados de corrida com terceiros.

## Controle do usuário

Você pode pausar o monitoramento dentro do app a qualquer momento. Também pode desativar o Serviço de Acessibilidade do CalcMot nas configurações do Android.

## Suporte

Contato: eduardoangelo20001@gmail.com
