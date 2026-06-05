param(
    [int]$DurationSeconds = 900,
    [int]$MaxUniqueCards = 10,
    [int]$IntervalMilliseconds = 700,
    [int]$ScreenRecordSegmentSeconds = 180,
    [string]$PackageName = "br.com.calcmot",
    [string]$DriverPackageName = "com.ubercab.driver",
    [string]$OutputDir = ".tmp\qa-production-e2e",
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [switch]$SkipOpenDriver,
    [switch]$AllowInvalidPreflight
)

$ErrorActionPreference = "Stop"

function Get-Text {
    param([scriptblock]$Command)

    try {
        return (& $Command | Out-String).Trim()
    } catch {
        return "ERROR: $($_.Exception.Message)"
    }
}

function Get-CalcMotMonitoringEnabled {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    $raw = Get-Text { & $AdbPath exec-out run-as $PackageName cat shared_prefs/calcmot_settings.xml 2>$null }
    if ([string]::IsNullOrWhiteSpace($raw) -or $raw.StartsWith("ERROR:")) {
        return $true
    }
    return ($raw -notmatch 'name="monitoring_enabled"\s+value="false"')
}

function Test-AccessibilityEnabled {
    param(
        [string]$EnabledServices,
        [string]$PackageName
    )

    $serviceName = "$PackageName/$PackageName.accessibility.UberAccessibilityService"
    return $EnabledServices.Contains($serviceName)
}

function Get-WindowMatchCount {
    param(
        [string]$WindowDump,
        [string]$PackageName
    )

    if ([string]::IsNullOrWhiteSpace($WindowDump)) { return 0 }
    $windowBlockPattern = "(?ms)Window #[0-9]+ Window\{[^\r\n]*" +
        [regex]::Escape($PackageName) +
        ".*?(?=\r?\n\s*Window #[0-9]+ Window\{|\z)"
    $blocks = [regex]::Matches($WindowDump, $windowBlockPattern)
    $overlayLikeBlocks = $blocks | Where-Object {
        $block = $_.Value
        $block -match "ty=(ACCESSIBILITY_OVERLAY|APPLICATION_OVERLAY|SYSTEM_ALERT|TOAST)" -or
            ($block -match "mAttrs\.type=" -and $block -notmatch "ty=BASE_APPLICATION")
    }
    return @($overlayLikeBlocks).Count
}

function Start-LogcatCapture {
    param(
        [string]$AdbPath,
        [string]$OutputPath
    )

    return Start-Job -Name "CalcMotE2ELogcat" -ScriptBlock {
        param($InnerAdbPath, $InnerOutputPath)
        & $InnerAdbPath logcat -v time *:V | Out-File -LiteralPath $InnerOutputPath -Encoding utf8
    } -ArgumentList $AdbPath, $OutputPath
}

function Start-ScreenRecordCapture {
    param(
        [string]$AdbPath,
        [string]$Session,
        [string]$OutputDir,
        [int]$DurationSeconds,
        [int]$SegmentSeconds
    )

    return Start-Job -Name "CalcMotE2EScreenRecord" -ScriptBlock {
        param($InnerAdbPath, $InnerSession, $InnerOutputDir, $InnerDurationSeconds, $InnerSegmentSeconds)

        New-Item -ItemType Directory -Force -Path $InnerOutputDir | Out-Null
        $jobLog = Join-Path $InnerOutputDir "screenrecord-job.log"
        $endAt = (Get-Date).AddSeconds($InnerDurationSeconds)
        $index = 0
        while ((Get-Date) -lt $endAt) {
            $remainingSeconds = [int][Math]::Ceiling(($endAt - (Get-Date)).TotalSeconds)
            if ($remainingSeconds -le 0) { break }

            $limit = [Math]::Min($InnerSegmentSeconds, $remainingSeconds)
            $remotePath = "/sdcard/calcmot-e2e-$InnerSession-$index.mp4"
            $localPath = Join-Path $InnerOutputDir ("screenrecord-{0:000}.mp4" -f $index)

            Add-Content -LiteralPath $jobLog -Value "[$(Get-Date -Format o)] screenrecord start index=$index limit=$limit remote=$remotePath"
            & $InnerAdbPath shell screenrecord --time-limit $limit $remotePath 2>&1 | Add-Content -LiteralPath $jobLog
            $remoteExists = (& $InnerAdbPath shell "if [ -f $remotePath ]; then echo exists; fi" 2>$null | Out-String).Trim()
            if ($remoteExists -eq "exists") {
                & $InnerAdbPath pull $remotePath $localPath 2>&1 | Add-Content -LiteralPath $jobLog
                & $InnerAdbPath shell rm $remotePath 2>&1 | Add-Content -LiteralPath $jobLog
                Add-Content -LiteralPath $jobLog -Value "[$(Get-Date -Format o)] screenrecord pulled local=$localPath"
            } else {
                Add-Content -LiteralPath $jobLog -Value "[$(Get-Date -Format o)] screenrecord remote file not found after stop: $remotePath"
            }
            $index++
        }
    } -ArgumentList $AdbPath, $Session, $OutputDir, $DurationSeconds, $SegmentSeconds
}

function Stop-CaptureJob {
    param(
        [System.Management.Automation.Job]$Job,
        [string]$AdbPath,
        [switch]$StopScreenRecord,
        [int]$WaitSeconds = 30
    )

    if ($null -eq $Job) { return }
    if (-not $StopScreenRecord -and $Job.State -eq "Running") {
        Stop-Job -Job $Job | Out-Null
    }
    if ($StopScreenRecord -and $Job.State -eq "Running") {
        & $AdbPath shell "pkill -2 screenrecord >/dev/null 2>&1 || true" | Out-Null
    }
    if ($Job.State -eq "Running") {
        Wait-Job -Job $Job -Timeout $WaitSeconds | Out-Null
    }
    if ($Job.State -eq "Running") {
        Stop-Job -Job $Job | Out-Null
    }
    Receive-Job -Job $Job -ErrorAction SilentlyContinue | Out-Null
    Remove-Job -Job $Job -Force -ErrorAction SilentlyContinue
}

function Count-Pattern {
    param(
        [string]$Text,
        [string]$Pattern
    )

    if ([string]::IsNullOrWhiteSpace($Text)) { return 0 }
    return ([regex]::Matches($Text, $Pattern)).Count
}

if (!(Test-Path -LiteralPath $AdbPath)) {
    throw "ADB not found at $AdbPath"
}

$session = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $OutputDir $session
$oracleRoot = Join-Path $sessionDir "oracle"
$screenRecordDir = Join-Path $sessionDir "screenrecord"
$preflightDir = Join-Path $sessionDir "preflight"
$postflightDir = Join-Path $sessionDir "postflight"
New-Item -ItemType Directory -Force -Path $sessionDir, $oracleRoot, $screenRecordDir, $preflightDir, $postflightDir | Out-Null
$sessionDir = (Resolve-Path -LiteralPath $sessionDir).Path
$oracleRoot = (Resolve-Path -LiteralPath $oracleRoot).Path
$screenRecordDir = (Resolve-Path -LiteralPath $screenRecordDir).Path
$preflightDir = (Resolve-Path -LiteralPath $preflightDir).Path
$postflightDir = (Resolve-Path -LiteralPath $postflightDir).Path
$logcatPath = Join-Path $sessionDir "logcat-live.txt"

if (-not $SkipOpenDriver) {
    & $AdbPath shell monkey -p $DriverPackageName 1 | Out-Null
    Start-Sleep -Seconds 1
}

$devices = Get-Text { & $AdbPath devices }
$enabledServices = Get-Text { & $AdbPath shell settings get secure enabled_accessibility_services }
$accessibilityDump = Get-Text { & $AdbPath shell dumpsys accessibility }
$overlayAppOps = Get-Text { & $AdbPath shell appops get $PackageName SYSTEM_ALERT_WINDOW }
$manifestPath = Join-Path (Split-Path -Parent $PSScriptRoot) "app\src\main\AndroidManifest.xml"
$usesSystemAlertWindow = $false
if (Test-Path -LiteralPath $manifestPath) {
    $usesSystemAlertWindow = (Get-Content -LiteralPath $manifestPath -Raw) -match "android\.permission\.SYSTEM_ALERT_WINDOW"
}
$appPid = Get-Text { & $AdbPath shell pidof $PackageName }
$windowBefore = Get-Text { & $AdbPath shell dumpsys window }
$memBefore = Get-Text { & $AdbPath shell dumpsys meminfo $PackageName }
$cpuBefore = Get-Text { & $AdbPath shell dumpsys cpuinfo }
$monitoringEnabled = Get-CalcMotMonitoringEnabled -AdbPath $AdbPath -PackageName $PackageName
$accessibilityEnabled = Test-AccessibilityEnabled -EnabledServices $enabledServices -PackageName $PackageName
$a11yToolListed = ($accessibilityDump -match "\(A11yTool\)" -or $accessibilityDump -match "isAccessibilityTool=true")
$overlayAllowed = if ($usesSystemAlertWindow) {
    ($overlayAppOps -match "allow|MODE_ALLOWED")
} else {
    $true
}
$calcMotWindowCountBefore = Get-WindowMatchCount -WindowDump $windowBefore -PackageName $PackageName
$driverFocused = ($windowBefore -match [regex]::Escape($DriverPackageName))
$possibleExternalFloatingWindow = (
    $windowBefore -match "(?m)^\s*Window #[0-9]+ Window\{[^\r\n]*(VoipActivity|PictureInPicture|pip)" -or
    $windowBefore -match "(?m)^\s*mAttrs\.type=APPLICATION_OVERLAY"
)

[System.IO.File]::WriteAllText((Join-Path $preflightDir "adb-devices.txt"), $devices, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $preflightDir "enabled-accessibility-services.txt"), $enabledServices, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $preflightDir "dumpsys-accessibility.txt"), $accessibilityDump, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $preflightDir "appops-overlay.txt"), $overlayAppOps, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $preflightDir "dumpsys-window-before.txt"), $windowBefore, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $preflightDir "meminfo-before.txt"), $memBefore, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $preflightDir "cpuinfo-before.txt"), $cpuBefore, [System.Text.Encoding]::UTF8)

$preflightWarnings = [System.Collections.Generic.List[string]]::new()
if (-not $accessibilityEnabled) { $preflightWarnings.Add("AccessibilityService is not enabled.") }
if (-not $a11yToolListed) { $preflightWarnings.Add("AccessibilityService was not confirmed as A11yTool in dumpsys accessibility.") }
if ($usesSystemAlertWindow -and -not $overlayAllowed) { $preflightWarnings.Add("SYSTEM_ALERT_WINDOW was not confirmed as allowed.") }
if (-not $monitoringEnabled) { $preflightWarnings.Add("CalcMot monitoring is disabled.") }
if (-not $driverFocused) { $preflightWarnings.Add("Uber Driver focus was not confirmed before the session.") }
if ($calcMotWindowCountBefore -gt 0) { $preflightWarnings.Add("CalcMot windows were present before the session: $calcMotWindowCountBefore.") }
if ($possibleExternalFloatingWindow) { $preflightWarnings.Add("Possible external floating/PiP window detected. Close it before a definitive QA run.") }

if ($preflightWarnings.Count -gt 0 -and -not $AllowInvalidPreflight) {
    $warningsText = ($preflightWarnings | ForEach-Object { "- $_" }) -join "`n"
    [System.IO.File]::WriteAllText((Join-Path $sessionDir "preflight-blocked.md"), "# Preflight blocked`n`n$warningsText`n", [System.Text.Encoding]::UTF8)
    throw "Preflight failed. See $sessionDir\preflight-blocked.md. Re-run with -AllowInvalidPreflight only for partial evidence."
}

& $AdbPath logcat -c
$logcatJob = $null
$screenRecordJob = $null

try {
    $logcatJob = Start-LogcatCapture -AdbPath $AdbPath -OutputPath $logcatPath
    $screenRecordJob = Start-ScreenRecordCapture `
        -AdbPath $AdbPath `
        -Session $session `
        -OutputDir $screenRecordDir `
        -DurationSeconds $DurationSeconds `
        -SegmentSeconds $ScreenRecordSegmentSeconds

    $bridgePath = Join-Path $PSScriptRoot "run-uiautomator-bridge.ps1"
    & $bridgePath `
        -DurationSeconds $DurationSeconds `
        -MaxUniqueCards $MaxUniqueCards `
        -IntervalMilliseconds $IntervalMilliseconds `
        -PackageName $PackageName `
        -DriverPackageName $DriverPackageName `
        -OutputDir $oracleRoot `
        -OracleOnly `
        -CaptureScreenshots `
        -NoClearLogcat
} finally {
    Stop-CaptureJob -Job $screenRecordJob -AdbPath $AdbPath -StopScreenRecord
    Stop-CaptureJob -Job $logcatJob -AdbPath $AdbPath
}

$windowAfter = Get-Text { & $AdbPath shell dumpsys window }
$memAfter = Get-Text { & $AdbPath shell dumpsys meminfo $PackageName }
$cpuAfter = Get-Text { & $AdbPath shell dumpsys cpuinfo }
$calcMotWindowCountAfter = Get-WindowMatchCount -WindowDump $windowAfter -PackageName $PackageName

[System.IO.File]::WriteAllText((Join-Path $postflightDir "dumpsys-window-after.txt"), $windowAfter, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $postflightDir "meminfo-after.txt"), $memAfter, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText((Join-Path $postflightDir "cpuinfo-after.txt"), $cpuAfter, [System.Text.Encoding]::UTF8)

$oracleSession = Get-ChildItem -LiteralPath $oracleRoot -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$oracleReport = $null
if ($oracleSession) {
    $oracleReportPath = Join-Path $oracleSession.FullName "oracle-report.json"
    if (Test-Path -LiteralPath $oracleReportPath) {
        $oracleReport = Get-Content -LiteralPath $oracleReportPath -Raw | ConvertFrom-Json
    }
}

$logcatText = ""
if (Test-Path -LiteralPath $logcatPath) {
    $logcatText = Get-Content -LiteralPath $logcatPath -Raw
}

$errorCounts = [ordered]@{
    fatal_exception = Count-Pattern -Text $logcatText -Pattern "FATAL EXCEPTION"
    anr = Count-Pattern -Text $logcatText -Pattern "\bANR\b|Application Not Responding"
    input_dispatch_timeout = Count-Pattern -Text $logcatText -Pattern "Input dispatching timed out|Input dispatch timeout"
    bad_token_exception = Count-Pattern -Text $logcatText -Pattern "BadTokenException"
    window_leaked = Count-Pattern -Text $logcatText -Pattern "WindowLeaked"
    overlay_show_requested = Count-Pattern -Text $logcatText -Pattern "OVERLAY_SHOW_REQUESTED"
    overlay_visible_to_user = Count-Pattern -Text $logcatText -Pattern "OVERLAY_VISIBLE_TO_USER"
    overlay_force_hidden_invalid_context = Count-Pattern -Text $logcatText -Pattern "OVERLAY_FORCE_HIDDEN_INVALID_CONTEXT"
    card_path_complete_true = Count-Pattern -Text $logcatText -Pattern "CARD_PATH.*complete=true"
    card_path_candidate_true = Count-Pattern -Text $logcatText -Pattern "CARD_PATH.*candidate=true"
}

$summary = [ordered]@{
    session = $session
    session_dir = (Resolve-Path -LiteralPath $sessionDir).Path
    oracle_session_dir = if ($oracleSession) { $oracleSession.FullName } else { $null }
    ocr_enabled = $false
    oracle_only = $true
    duration_seconds = $DurationSeconds
    max_unique_cards = $MaxUniqueCards
    interval_milliseconds = $IntervalMilliseconds
    pid = $appPid
    accessibility_enabled = $accessibilityEnabled
    a11y_tool_confirmed = $a11yToolListed
    overlay_allowed = $overlayAllowed
    monitoring_enabled = $monitoringEnabled
    driver_focused_preflight = $driverFocused
    calcmot_window_count_before = $calcMotWindowCountBefore
    calcmot_window_count_after = $calcMotWindowCountAfter
    preflight_warnings = @($preflightWarnings)
    oracle_complete_cards = if ($oracleReport) { $oracleReport.uiautomator_complete_cards } else { $null }
    internal_tree_complete_cards = if ($oracleReport) { $oracleReport.internal_tree_complete_cards } else { $null }
    production_overlay_shown = if ($oracleReport) { $oracleReport.production_overlay_shown } else { $null }
    correct_overlay_cards_by_fingerprint = if ($oracleReport) { $oracleReport.correct_overlay_cards } else { $null }
    coverage_correct_percent_by_fingerprint = if ($oracleReport) { $oracleReport.coverage_correct_percent } else { $null }
    visual_audit_required = $true
    logcat_counts = $errorCounts
    screenrecord_files = @(Get-ChildItem -LiteralPath $screenRecordDir -Filter "*.mp4" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
}

$summaryJsonPath = Join-Path $sessionDir "e2e-summary.json"
$summaryMdPath = Join-Path $sessionDir "e2e-summary.md"
$summary | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $summaryJsonPath -Encoding UTF8

$warningsMarkdown = if ($preflightWarnings.Count -gt 0) {
    ($preflightWarnings | ForEach-Object { "- $_" }) -join "`n"
} else {
    "- none"
}
$videoFilesMarkdown = if ($summary.screenrecord_files.Count -gt 0) {
    ($summary.screenrecord_files | ForEach-Object { "- $_" }) -join "`n"
} else {
    "- none"
}
$oracleReportMd = if ($oracleSession) { Join-Path $oracleSession.FullName "oracle-report.md" } else { "not generated" }

@"
# CalcMot Production E2E QA

## Executive Summary

- Session: $session
- OCR enabled: False
- UIAutomator mode: OracleOnly, no injection
- Duration seconds: $DurationSeconds
- Max unique cards: $MaxUniqueCards
- Accessibility enabled: $accessibilityEnabled
- A11yTool confirmed: $a11yToolListed
- Overlay permission allowed: $overlayAllowed
- Monitoring enabled: $monitoringEnabled
- Uber focused before run: $driverFocused
- CalcMot windows before/after: $calcMotWindowCountBefore / $calcMotWindowCountAfter

## Oracle/Log Metrics

- Oracle complete cards: $($summary.oracle_complete_cards)
- Internal tree complete cards: $($summary.internal_tree_complete_cards)
- Production overlay shown: $($summary.production_overlay_shown)
- Correct overlay cards by fingerprint: $($summary.correct_overlay_cards_by_fingerprint)
- Coverage by fingerprint: $($summary.coverage_correct_percent_by_fingerprint)
- OVERLAY_SHOW_REQUESTED logs: $($errorCounts.overlay_show_requested)
- OVERLAY_VISIBLE_TO_USER logs: $($errorCounts.overlay_visible_to_user)
- OVERLAY_FORCE_HIDDEN_INVALID_CONTEXT logs: $($errorCounts.overlay_force_hidden_invalid_context)
- CARD_PATH complete=true logs: $($errorCounts.card_path_complete_true)
- CARD_PATH candidate=true logs: $($errorCounts.card_path_candidate_true)

## Stability Signals

- FATAL EXCEPTION: $($errorCounts.fatal_exception)
- ANR: $($errorCounts.anr)
- Input dispatch timeout: $($errorCounts.input_dispatch_timeout)
- BadTokenException: $($errorCounts.bad_token_exception)
- WindowLeaked: $($errorCounts.window_leaked)

## Preflight Warnings

$warningsMarkdown

## Artifacts

- Logcat: $logcatPath
- Oracle report: $oracleReportMd
- Summary JSON: $summaryJsonPath
- Preflight folder: $preflightDir
- Postflight folder: $postflightDir

## Screen Recordings

$videoFilesMarkdown

## Visual Audit Rule

This file is not the final quality verdict. A card is successful only when the screen recording shows the card and the CalcMot overlay visible with correct values in useful time, and the overlay disappears correctly after the card ends.

Use the screen recordings, oracle screenshots/XMLs and logcat together to fill the final card-by-card table.
"@ | Set-Content -LiteralPath $summaryMdPath -Encoding UTF8

Write-Host "Production E2E QA session saved to $sessionDir"
Write-Host "Summary: $summaryMdPath"
Write-Host "Logcat: $logcatPath"
Write-Host "Oracle report: $oracleReportMd"
Write-Host "Screen recordings: $screenRecordDir"
