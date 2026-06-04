param(
    [int]$DurationSeconds = 900,
    [int]$IntervalSeconds = 2,
    [string]$PackageName = "br.com.calcmot",
    [string]$DriverPackageName = "com.ubercab.driver",
    [string]$OutputDir = ".tmp\accessibility-lab",
    [int]$MaxSnapshotPull = 500,
    [bool]$ClearDeviceLabBeforeRun = $true
)

$ErrorActionPreference = "Stop"

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb)) {
    throw "ADB not found at $adb"
}
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& $adb start-server 2>&1 | Out-Null
$ErrorActionPreference = $previousErrorActionPreference

$session = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $OutputDir $session
$uiautomatorDir = Join-Path $sessionDir "uiautomator"
$screensDir = Join-Path $sessionDir "screens"
$labDir = Join-Path $sessionDir "app-lab"
New-Item -ItemType Directory -Force -Path $uiautomatorDir, $screensDir, $labDir | Out-Null

if ($ClearDeviceLabBeforeRun) {
    & $adb shell run-as $PackageName rm -rf files/accessibility-lab 2>$null | Out-Null
}

& $adb shell monkey -p $DriverPackageName 1 | Out-Null

$endAt = (Get-Date).AddSeconds($DurationSeconds)
$iteration = 0

while ((Get-Date) -lt $endAt) {
    $iteration++
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"

    $remoteXml = "/sdcard/calcmot-uiautomator-$stamp.xml"
    $localXml = Join-Path $uiautomatorDir "$stamp.xml"
    & $adb shell uiautomator dump --compressed $remoteXml | Out-Null
    if ($LASTEXITCODE -eq 0) {
        & $adb pull $remoteXml $localXml | Out-Null
        & $adb shell rm $remoteXml | Out-Null
    } else {
        Write-Warning "UIAutomator dump failed at $stamp"
    }

    $remotePng = "/sdcard/calcmot-screen-$stamp.png"
    $localPng = Join-Path $screensDir "$stamp.png"
    & $adb shell screencap -p $remotePng | Out-Null
    if ($LASTEXITCODE -eq 0) {
        & $adb pull $remotePng $localPng | Out-Null
        & $adb shell rm $remotePng | Out-Null
    } else {
        Write-Warning "Screenshot capture failed at $stamp"
    }

    Start-Sleep -Seconds $IntervalSeconds
}

$remoteFiles = & $adb shell run-as $PackageName sh -c "find files/accessibility-lab -type f | tail -n $MaxSnapshotPull" 2>$null
if ($LASTEXITCODE -eq 0) {
    foreach ($remoteFile in $remoteFiles) {
        $cleanRemoteFile = $remoteFile.Trim()
        if ([string]::IsNullOrWhiteSpace($cleanRemoteFile)) { continue }
        if ($cleanRemoteFile -notmatch "\.json$") { continue }

        $relativePath = $cleanRemoteFile -replace "^.*files/accessibility-lab/", ""
        $relativePath = $relativePath -replace "^[\\/]+", ""
        $localLabFile = Join-Path $labDir ($relativePath -replace "/", "\")
        $localLabParent = [System.IO.Path]::GetDirectoryName($localLabFile)
        if ((Test-Path -LiteralPath $localLabParent -PathType Leaf)) {
            Remove-Item -LiteralPath $localLabParent -Force
        }
        [System.IO.Directory]::CreateDirectory($localLabParent) | Out-Null

        $content = & $adb exec-out run-as $PackageName cat $cleanRemoteFile
        [System.IO.File]::WriteAllText($localLabFile, ($content -join "`n"), [System.Text.Encoding]::UTF8)
    }
}

$uiFiles = Get-ChildItem -LiteralPath $uiautomatorDir -Filter "*.xml" -ErrorAction SilentlyContinue
$labFiles = Get-ChildItem -LiteralPath $labDir -Filter "*.json" -Recurse -ErrorAction SilentlyContinue
$screenFiles = Get-ChildItem -LiteralPath $screensDir -Filter "*.png" -ErrorAction SilentlyContinue

$uiCardLikeCount = 0
$uiExamples = @()
foreach ($file in $uiFiles) {
    $xml = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    if ($xml -match "R\$\s*[0-9]" -and
        $xml -match "(Aceitar|Selecionar)" -and
        $xml -match "(minuto|minutos|min\b)" -and
        $xml -match "km") {
        $uiCardLikeCount++
        if ($uiExamples.Count -lt 5) {
            $matches = [regex]::Matches($xml, 'text="([^"]+)"') |
                ForEach-Object { $_.Groups[1].Value } |
                Where-Object {
                    $_ -match 'R\$|UberX|Priority|Aceitar|Selecionar|Viagem de|minutos|km|Exclusivo|Verificado'
                }
            $uiExamples += (($matches | Select-Object -First 12) -join " | ")
        }
    }
}

$labSnapshots = @()
foreach ($file in $labFiles) {
    try {
        $labSnapshots += [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
    } catch {
        Write-Warning "Could not parse lab snapshot $($file.FullName): $_"
    }
}

$completeTree = @($labSnapshots | Where-Object { $_.inspection.isCompleteOffer -eq $true })
$withText = @($labSnapshots | Where-Object { $_.inspection.lineCount -gt 0 })
$withPrice = @($labSnapshots | Where-Object { $_.inspection.hasPrice -eq $true })
$withButton = @($labSnapshots | Where-Object { $_.inspection.hasActionButton -eq $true })
$withBlocks = @($labSnapshots | Where-Object { $_.inspection.timeDistanceBlockCount -ge 2 })
$latencies = @($completeTree | ForEach-Object { [double]$_.inspection.elapsedSinceEventMs })
$averageLatency = if ($latencies.Count -gt 0) {
    [Math]::Round(($latencies | Measure-Object -Average).Average, 2)
} else {
    $null
}
$betaCoveragePercent = if ($uiCardLikeCount -gt 0) {
    [Math]::Round(($completeTree.Count / $uiCardLikeCount) * 100, 2)
} else {
    $null
}
$betaReadyMin85 = ($betaCoveragePercent -ne $null -and $betaCoveragePercent -ge 85.0)

$report = [ordered]@{
    session = $session
    durationSeconds = $DurationSeconds
    intervalSeconds = $IntervalSeconds
    maxSnapshotPull = $MaxSnapshotPull
    clearDeviceLabBeforeRun = $ClearDeviceLabBeforeRun
    uiautomator_complete_cards = $uiCardLikeCount
    internal_tree_complete_cards = $completeTree.Count
    overlay_shown = $null
    missed_cards = [Math]::Max(0, $uiCardLikeCount - $completeTree.Count)
    average_latency_ms = $averageLatency
    beta_overlay_coverage_percent = $betaCoveragePercent
    beta_ready_min_85 = $betaReadyMin85
    screenshotCount = $screenFiles.Count
    uiautomatorDumpCount = $uiFiles.Count
    uiautomatorCardLikeCount = $uiCardLikeCount
    uiautomatorExamples = $uiExamples
    appLabSnapshotCount = $labSnapshots.Count
    appLabWithTextCount = $withText.Count
    appLabWithPriceCount = $withPrice.Count
    appLabWithButtonCount = $withButton.Count
    appLabWithTwoBlocksCount = $withBlocks.Count
    appLabCompleteOfferCount = $completeTree.Count
    averageTreeCandidateLatencyMs = $averageLatency
}

$reportJson = Join-Path $sessionDir "accessibility-lab-report.json"
$reportMd = Join-Path $sessionDir "accessibility-lab-report.md"
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $reportJson -Encoding UTF8

$uiExampleText = if ($uiExamples.Count -gt 0) {
    ($uiExamples | ForEach-Object { "- $_" }) -join "`n"
} else {
    "- none"
}

@"
# CalcMot Accessibility Lab Report

- Session: $session
- Duration seconds: $DurationSeconds
- Max internal snapshots pulled: $MaxSnapshotPull
- Device lab cleared before run: $ClearDeviceLabBeforeRun
- Screenshots captured: $($screenFiles.Count)
- UIAutomator dumps captured: $($uiFiles.Count)
- uiautomator_complete_cards: $uiCardLikeCount
- Internal app snapshots: $($labSnapshots.Count)
- Internal snapshots with text: $($withText.Count)
- Internal snapshots with price: $($withPrice.Count)
- Internal snapshots with action button: $($withButton.Count)
- Internal snapshots with two time/km blocks: $($withBlocks.Count)
- internal_tree_complete_cards: $($completeTree.Count)
- overlay_shown: not measured by this collector
- missed_cards: $([Math]::Max(0, $uiCardLikeCount - $completeTree.Count))
- average_latency_ms: $averageLatency
- beta_overlay_coverage_percent: $betaCoveragePercent
- beta_ready_min_85: $betaReadyMin85

## UIAutomator examples
$uiExampleText

Use the PNG/XML/JSON files in this folder to manually review any mismatch:

- screenshot has card + UIAutomator has card + app snapshot missing card = service timing/root problem.
- screenshot has card + UIAutomator missing card = accessibility tree probably cannot see that card.
- UIAutomator/app snapshot partial = improve refresh, node mapping, content descriptions or parser scoring.
"@ | Set-Content -LiteralPath $reportMd -Encoding UTF8

Write-Host "Lab session saved to $sessionDir"
Write-Host "Report: $reportMd"
