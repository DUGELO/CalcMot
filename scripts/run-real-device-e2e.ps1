param(
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [ValidateSet("uber", "99")]
    [string]$DriverApp = "uber",
    [string]$DriverPackageName = ""
)

$ErrorActionPreference = "Stop"

if (!(Test-Path -LiteralPath $AdbPath)) {
    throw "ADB not found at $AdbPath"
}

if (!(Test-Path -LiteralPath $ApkPath)) {
    throw "APK not found at $ApkPath. Run .\gradlew.bat assembleDebug first."
}

if ([string]::IsNullOrWhiteSpace($DriverPackageName)) {
    $DriverPackageName = if ($DriverApp -eq "99") { "com.app99.driver" } else { "com.ubercab.driver" }
}

Write-Host "Connected devices:"
& $AdbPath devices

Write-Host "Installing CalcMot debug APK..."
& $AdbPath install -r $ApkPath

Write-Host "Opening CalcMot..."
& $AdbPath shell monkey -p br.com.calcmot 1 | Out-Host

Write-Host ""
Write-Host "Manual step: enable CalcMot accessibility service on the phone."
Write-Host "After the app shows Pronto, keep this terminal open for filtered logs."
Write-Host ""

Write-Host "Clearing logcat..."
& $AdbPath logcat -c

Write-Host "Opening driver app: $DriverPackageName"
& $AdbPath shell monkey -p $DriverPackageName 1 | Out-Host

Write-Host "Filtered logs: UberReader OverlayManager"
& $AdbPath logcat -s UberReader OverlayManager
