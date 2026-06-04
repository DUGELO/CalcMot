# Roteiro E2E no Celular Real

Este roteiro valida o CalcMot no fluxo real de uso: instalar, ativar, deixar em segundo plano, abrir o app de motorista e observar ofertas.

## Pre-condicoes

- Celular conectado via USB com depuracao autorizada.
- Android Platform Tools disponivel em `LOCALAPPDATA\Android\Sdk\platform-tools`.
- Build debug gerado por `.\gradlew.bat assembleDebug`.
- App de motorista instalado no aparelho.

## Execucao

1. Instalar o app:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

2. Abrir o CalcMot:

```powershell
& $adb shell monkey -p br.com.calcmot 1
```

3. No celular, ativar manualmente o servico de acessibilidade do CalcMot.

4. Voltar ao CalcMot e conferir:

- Status `Pronto`.
- Switch `Monitoramento` ligado.
- Posicao do overlay em `Alto`.
- Preview exibindo R$/km, R$/h e tempo total.

5. Limpar logs e abrir logcat filtrado:

```powershell
& $adb logcat -c
& $adb logcat -s UberReader OverlayManager
```

6. Abrir o app de motorista:

```powershell
& $adb shell monkey -p com.ubercab.driver 1
```

7. Quando uma oferta aparecer, classificar o resultado:

- Servico recebeu evento.
- Arvore de acessibilidade trouxe card completo.
- Arvore de acessibilidade trouxe card completo.
- Validador espacial rejeitou.
- Parser rejeitou.
- Stability gate aguardou segundo frame.
- Overlay foi exibido.

## Criterios de Aceite

- O app permanece funcionando em segundo plano.
- O overlay aparece somente com card completo e estavel.
- O overlay nao aparece em mapa, popup, numeros soltos ou card incompleto.
- O overlay nao reaproveita dados da oferta anterior.
- A posicao `Alto` fica acima do card sem cobrir o texto essencial da oferta.
- Nenhum log de release deve expor card bruto, endereco ou valor.

## Observacao

O Android nao permite ativar servico de acessibilidade automaticamente em aparelho comum. Essa etapa deve continuar manual no E2E real.
