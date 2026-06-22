param(
    [int]$DurationMinutes = 45,
    [int]$FrameIntervalMs = 700,
    [string]$PackageName = "br.com.calcmot",
    [string]$OutputRoot = ".tmp\99-offer-live"
)

$ErrorActionPreference = "Continue"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb)) {
    throw "ADB not found at $adb"
}

$session = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $OutputRoot $session
$framesDir = Join-Path $sessionDir "frames"
$triggersDir = Join-Path $sessionDir "triggers"
$labDir = Join-Path $sessionDir "accessibility-lab"
$diagnosticsDir = Join-Path $sessionDir "99-diagnostics"
$statusFile = Join-Path $sessionDir "status.json"
$eventsFile = Join-Path $sessionDir "events.ndjson"
$stopFile = Join-Path $sessionDir "stop.requested"
New-Item -ItemType Directory -Force -Path $framesDir, $triggersDir, $labDir, $diagnosticsDir | Out-Null

function Write-Status {
    param(
        [string]$State,
        [int]$Frames,
        [int]$Triggers,
        [int]$LastSequence,
        [string]$Details
    )
    [ordered]@{
        state = $State
        session = $session
        processId = $PID
        updatedAt = (Get-Date).ToString("o")
        frames = $Frames
        triggers = $Triggers
        lastSequence = $LastSequence
        details = $Details
        sessionDir = (Resolve-Path $sessionDir).Path
    } | ConvertTo-Json | Set-Content -LiteralPath $statusFile -Encoding UTF8
}

function Get-LatestRemoteTimeline {
    $files = @(
        & $adb shell run-as $PackageName find files/99-accessibility -name timeline.ndjson 2>$null
    )
    return $files | Select-Object -Last 1
}

function Save-Screen {
    param([string]$TargetPath)
    $remote = "/sdcard/calcmot-99-watch.png"
    & $adb shell screencap -p $remote | Out-Null
    if ($LASTEXITCODE -eq 0) {
        & $adb pull $remote $TargetPath 2>$null | Out-Null
        & $adb shell rm $remote 2>$null | Out-Null
    }
}

function Save-ForensicBundle {
    param(
        [string]$Reason,
        [int]$Sequence
    )
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
    $target = Join-Path $triggersDir "${stamp}_seq-${Sequence}"
    New-Item -ItemType Directory -Force -Path $target | Out-Null
    Save-Screen (Join-Path $target "screen.png")

    foreach ($compressed in @($true, $false)) {
        $suffix = if ($compressed) { "compressed" } else { "verbose" }
        $remoteXml = "/sdcard/calcmot-99-$suffix.xml"
        $dumpArgs = if ($compressed) {
            @("shell", "uiautomator", "dump", "--compressed", $remoteXml)
        } else {
            @("shell", "uiautomator", "dump", $remoteXml)
        }
        & $adb @dumpArgs | Out-Null
        if ($LASTEXITCODE -eq 0) {
            & $adb pull $remoteXml (Join-Path $target "$suffix.xml") 2>$null | Out-Null
            & $adb shell rm $remoteXml 2>$null | Out-Null
        }
    }

    & $adb shell dumpsys accessibility |
        Set-Content -LiteralPath (Join-Path $target "dumpsys-accessibility.txt") -Encoding UTF8
    & $adb shell dumpsys window windows |
        Set-Content -LiteralPath (Join-Path $target "dumpsys-windows.txt") -Encoding UTF8
    & $adb shell dumpsys SurfaceFlinger --list |
        Set-Content -LiteralPath (Join-Path $target "surface-layers.txt") -Encoding UTF8
    & $adb logcat -d -v threadtime "CalcMot99:V" "UberReader:V" "*:S" |
        Set-Content -LiteralPath (Join-Path $target "logcat.txt") -Encoding UTF8

    [ordered]@{
        capturedAt = (Get-Date).ToString("o")
        reason = $Reason
        sequence = $Sequence
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $target "trigger.json") -Encoding UTF8
}

function Pull-RemoteFiles {
    param(
        [string]$RemoteDir,
        [string]$LocalDir,
        [int]$Limit = 1000
    )
    $remoteFiles = @(
        & $adb shell run-as $PackageName find $RemoteDir -type f 2>$null |
            Select-Object -Last $Limit
    )
    foreach ($remoteFile in $remoteFiles) {
        $clean = $remoteFile.Trim()
        if ([string]::IsNullOrWhiteSpace($clean)) { continue }
        $relative = $clean -replace ("^.*" + [regex]::Escape($RemoteDir) + "/"), ""
        $target = Join-Path $LocalDir ($relative -replace "/", "\")
        $parent = [IO.Path]::GetDirectoryName($target)
        [IO.Directory]::CreateDirectory($parent) | Out-Null
        $content = & $adb exec-out run-as $PackageName cat $clean 2>$null
        [IO.File]::WriteAllText($target, ($content -join "`n"), [Text.Encoding]::UTF8)
    }
}

$frameCount = 0
$triggerCount = 0
$lastSequence = 0
$lastHash = ""
$lastTriggeredSequence = -1
$remoteTimeline = ""
$endAt = (Get-Date).AddMinutes($DurationMinutes)
$offerPattern = "(?i)R\$\s*\d|corrida|oferta|aceitar|escolher|selecionar|solicita|embarque|destino|passageiro"

Write-Status "starting" $frameCount $triggerCount $lastSequence "Waiting for 99"
& $adb logcat -c | Out-Null

while ((Get-Date) -lt $endAt -and -not (Test-Path -LiteralPath $stopFile)) {
    $focus = (& $adb shell dumpsys window 2>$null |
        Select-String -Pattern "mCurrentFocus=" |
        Select-Object -First 1).Line
    $is99Foreground = $focus -match "com\.app99\.driver"

    if ($is99Foreground) {
        $tempFrame = Join-Path $sessionDir "current.png"
        Save-Screen $tempFrame
        if (Test-Path -LiteralPath $tempFrame) {
            $hash = (Get-FileHash -LiteralPath $tempFrame -Algorithm SHA256).Hash
            if ($hash -ne $lastHash) {
                $lastHash = $hash
                $frameCount++
                $targetFrame = Join-Path $framesDir ("{0}_{1:D5}.png" -f (Get-Date -Format "HHmmss-fff"), $frameCount)
                Move-Item -LiteralPath $tempFrame -Destination $targetFrame -Force
            } else {
                Remove-Item -LiteralPath $tempFrame -Force
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($remoteTimeline)) {
        $remoteTimeline = Get-LatestRemoteTimeline
    }
    if (-not [string]::IsNullOrWhiteSpace($remoteTimeline)) {
        $rawLines = & $adb exec-out run-as $PackageName cat $remoteTimeline 2>$null
        $entries = @(
            $rawLines |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
                ForEach-Object {
                    try { $_ | ConvertFrom-Json } catch { $null }
                } |
                Where-Object { $_ -ne $null -and [int]$_.sequence -gt $lastSequence }
        )

        foreach ($entry in $entries) {
            $sequence = [int]$entry.sequence
            $lastSequence = [Math]::Max($lastSequence, $sequence)
            $entry | ConvertTo-Json -Compress |
                Add-Content -LiteralPath $eventsFile -Encoding UTF8

            $details = [string]$entry.details
            $semanticSignal =
                $details -match $offerPattern -or
                $details -match "price=true|action=true|blocks=[12-9]|complete=true|candidate=(?!none)" -or
                $details -match "textNodes=[1-9]|descNodes=[1-9]|stateNodes=[1-9]|extrasNodes=[1-9]"
            $eventSignal =
                $entry.stage -eq "EVENT" -and
                $details -match "TYPE_ANNOUNCEMENT|TYPE_NOTIFICATION_STATE_CHANGED|TYPE_VIEW_TEXT_CHANGED"

            if (($semanticSignal -or $eventSignal) -and $sequence -ne $lastTriggeredSequence) {
                $lastTriggeredSequence = $sequence
                $triggerCount++
                Save-ForensicBundle -Reason "$($entry.stage): $details" -Sequence $sequence
            }
        }
    }

    $focusDetails = if ($is99Foreground) { "99 foreground" } else { "Waiting for 99 foreground" }
    Write-Status "running" $frameCount $triggerCount $lastSequence $focusDetails
    Start-Sleep -Milliseconds $FrameIntervalMs
}

Pull-RemoteFiles "files/99-accessibility" $diagnosticsDir 100
Pull-RemoteFiles "files/accessibility-lab" $labDir 1000
& $adb logcat -d -v threadtime "CalcMot99:V" "UberReader:V" "*:S" |
    Set-Content -LiteralPath (Join-Path $sessionDir "final-logcat.txt") -Encoding UTF8
Save-Screen (Join-Path $sessionDir "final-screen.png")
Write-Status "completed" $frameCount $triggerCount $lastSequence "Monitoring finished"
