param(
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Label = "offer-visible",
    [string]$Source = "manual"
)

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

& $AdbPath shell am broadcast `
    -n br.com.calcmot/br.com.calcmot.debug.LatencyVisualProbeReceiver `
    -a br.com.calcmot.DEBUG_LATENCY_VISUAL_PROBE `
    --es label $Label `
    --es source $Source | Out-Null

Write-Host "Visual latency probe sent: label=$Label source=$Source"
