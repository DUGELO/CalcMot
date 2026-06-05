param(
    [int]$DurationSeconds = 900,
    [int]$MaxUniqueCards = 0,
    [int]$IntervalMilliseconds = 900,
    [string]$PackageName = "br.com.calcmot",
    [string]$DriverPackageName = "com.ubercab.driver",
    [string]$OutputDir = ".tmp\uiautomator-bridge",
    [switch]$ShowExtractedText,
    [switch]$CaptureScreenshots,
    [switch]$OracleOnly,
    [bool]$ClearDeviceLearningBeforeRun = $true,
    [switch]$NoClearLogcat,
    [int]$UiAutomatorDumpTimeoutSeconds = 8,
    [int]$ResourceSampleIntervalSeconds = 5
)

$ErrorActionPreference = "Stop"

function Normalize-OracleText {
    param([string]$Text)

    if ($null -eq $Text) { return "" }
    $boxDoubleDownHorizontal = [string][char]0x252C
    $boxVerticalRight = [string][char]0x251C
    $value = $Text.Replace([char]0x00A0, [char]0x20).Replace([char]0x202F, [char]0x20)
    $value = $value.Replace($boxDoubleDownHorizontal + [string][char]0x00E1, " ")
    $value = $value.Replace($boxDoubleDownHorizontal, " ")
    $value = $value.Replace($boxVerticalRight + [string][char]0x00F3, "a")
    $value = $value.Replace($boxVerticalRight + [string][char]0x00ED, "a")
    $value = $value.Replace($boxVerticalRight + [string][char]0x00AE, "e")
    $value = $value.Replace($boxVerticalRight + [string][char]0x00A1, "i")
    $value = $value.Replace($boxVerticalRight + [string][char]0x2502, "o")
    $value = $value.Replace($boxVerticalRight + [string][char]0x2551, "u")
    $value = $value.Replace($boxVerticalRight + [string][char]0x00BA, "c")
    return ($value -replace "\s+", " ").Trim()
}

function Extract-UberTextFromDump {
    param(
        [Parameter(Mandatory = $true)]
        [string]$XmlPath,
        [Parameter(Mandatory = $true)]
        [string]$DriverPackageName
    )

    $rawXml = [System.IO.File]::ReadAllText($XmlPath, [System.Text.Encoding]::UTF8)
    if ([string]::IsNullOrWhiteSpace($rawXml)) {
        return ""
    }

    [xml]$document = $rawXml
    $nodes = $document.SelectNodes("//node")
    $lines = foreach ($node in $nodes) {
        if ($node.GetAttribute("package") -ne $DriverPackageName) { continue }

        $text = $node.GetAttribute("text")
        if ([string]::IsNullOrWhiteSpace($text)) {
            $text = $node.GetAttribute("content-desc")
        }
        $text = Normalize-OracleText $text
        if ([string]::IsNullOrWhiteSpace($text)) { continue }

        $bounds = $node.GetAttribute("bounds")
        if ($bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") { continue }

        [pscustomobject]@{
            Text = $text.Trim()
            Left = [int]$Matches[1]
            Top = [int]$Matches[2]
            Right = [int]$Matches[3]
            Bottom = [int]$Matches[4]
        }
    }

    $seen = [System.Collections.Generic.HashSet[string]]::new()
    $ordered = $lines |
        Sort-Object Top, Left |
        ForEach-Object {
            $key = "$($_.Text)|$($_.Left)|$($_.Top)|$($_.Right)|$($_.Bottom)"
            if ($seen.Add($key)) { $_.Text }
        }

    return ($ordered -join "`n")
}

function Extract-OfferEvidenceFromDump {
    param(
        [Parameter(Mandatory = $true)]
        [string]$XmlPath,
        [Parameter(Mandatory = $true)]
        [string]$DriverPackageName
    )

    $rawXml = [System.IO.File]::ReadAllText($XmlPath, [System.Text.Encoding]::UTF8)
    if ([string]::IsNullOrWhiteSpace($rawXml)) {
        return @{ lines = @(); bounds = $null }
    }

    [xml]$document = $rawXml
    $nodes = $document.SelectNodes("//node")
    $semanticLines = foreach ($node in $nodes) {
        if ($node.GetAttribute("package") -ne $DriverPackageName) { continue }

        $text = $node.GetAttribute("text")
        $contentDescription = Normalize-OracleText $node.GetAttribute("content-desc")
        $visibleText = if ([string]::IsNullOrWhiteSpace($text)) { $contentDescription } else { $text }
        $visibleText = Normalize-OracleText $visibleText
        if ([string]::IsNullOrWhiteSpace($visibleText)) { continue }
        if ($visibleText -notmatch "R\$|Aceitar|Selecionar|Viagem|minuto|minutos|min\b|hora|horas|km|UberX|Priority|Exclusivo|Verificado") { continue }

        $bounds = $node.GetAttribute("bounds")
        if ($bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") { continue }

        [pscustomobject][ordered]@{
            text = (Normalize-OracleText $text).Trim()
            content_desc = $contentDescription.Trim()
            class = $node.GetAttribute("class")
            resource_id = $node.GetAttribute("resource-id")
            left = [int]$Matches[1]
            top = [int]$Matches[2]
            right = [int]$Matches[3]
            bottom = [int]$Matches[4]
        }
    }

    $lines = @($semanticLines | Sort-Object top, left)
    if ($lines.Count -eq 0) {
        return @{ lines = @(); bounds = $null }
    }

    return @{
        lines = $lines
        bounds = [ordered]@{
            left = ($lines | Measure-Object -Property left -Minimum).Minimum
            top = ($lines | Measure-Object -Property top -Minimum).Minimum
            right = ($lines | Measure-Object -Property right -Maximum).Maximum
            bottom = ($lines | Measure-Object -Property bottom -Maximum).Maximum
        }
    }
}

function Read-UiAutomatorDump {
    param(
        [Parameter(Mandatory = $true)]
        [string]$AdbPath,
        [int]$TimeoutSeconds = 8
    )

    $job = Start-Job -ScriptBlock {
        param([string]$InnerAdbPath)
        & $InnerAdbPath exec-out uiautomator dump --compressed /dev/tty 2>&1 | Out-String
    } -ArgumentList $AdbPath

    if (-not (Wait-Job $job -Timeout $TimeoutSeconds)) {
        Stop-Job $job | Out-Null
        Remove-Job $job | Out-Null
        return ""
    }

    $rawDump = (Receive-Job $job | Out-String)
    Remove-Job $job | Out-Null
    if ([string]::IsNullOrWhiteSpace($rawDump)) {
        return ""
    }

    $start = $rawDump.IndexOf("<?xml", [StringComparison]::Ordinal)
    $end = $rawDump.IndexOf("</hierarchy>", [StringComparison]::Ordinal)
    if ($start -lt 0 -or $end -lt 0) {
        return ""
    }

    $end += "</hierarchy>".Length
    return $rawDump.Substring($start, $end - $start)
}

function Capture-Screenshot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$AdbPath,
        [Parameter(Mandatory = $true)]
        [string]$Stamp,
        [Parameter(Mandatory = $true)]
        [string]$ScreenshotDir
    )

    $remotePath = "/sdcard/calcmot-uiautomator-$Stamp.png"
    $localPath = Join-Path $ScreenshotDir "$Stamp.png"
    & $AdbPath shell screencap -p $remotePath | Out-Null
    & $AdbPath pull $remotePath $localPath | Out-Null
    & $AdbPath shell rm $remotePath | Out-Null
    return $localPath
}

function Is-OfferLikeText {
    param([string]$Text)

    $Text = Normalize-OracleText $Text
    if ([string]::IsNullOrWhiteSpace($Text)) { return $false }
    if ($Text -notmatch "R\$[^\d]{0,4}[0-9]") { return $false }
    if ($Text -notmatch "(Aceitar|Selecionar)") { return $false }
    if ($Text -notmatch "km") { return $false }

    $timeDistanceMatches = [regex]::Matches(
        $Text,
        "(?is)(?:\d+\s*(?:min|minuto|minutos|h|hora|horas).{0,80}?km)"
    )
    return $timeDistanceMatches.Count -ge 2
}

function Fingerprint-OfferText {
    param([string]$Text)

    $Text = Normalize-OracleText $Text
    $price = ([regex]::Match($Text, "R\$[^\d]{0,4}[0-9]+(?:[,.][0-9]{1,2})?")).Value
    $blocks = @([regex]::Matches($Text, "(?is)\d+\s*(?:min|minuto|minutos|h|hora|horas).{0,80}?km") |
        Select-Object -First 2 |
        ForEach-Object { $_.Value -replace "\s+", " " })
    return ((@($price) + $blocks) -join " | ").Trim()
}

function ConvertTo-CanonicalOfferFingerprint {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    $lines = @($Text -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $price = $null
    foreach ($line in $lines) {
        $normalizedLine = Normalize-OracleText ($line -replace "\s+", " ")
        if ($normalizedLine -match "^\s*\+") { continue }
        if ($normalizedLine -match "(?i)inclu[ií]do|prioridade|bonus|b[oô]nus|adicional") { continue }
        $match = [regex]::Match($normalizedLine, "R\$[^\d]{0,4}([0-9]+(?:[,.][0-9]{1,2})?)")
        if ($match.Success) {
            $price = [double]::Parse(
                $match.Groups[1].Value.Replace(",", "."),
                [System.Globalization.CultureInfo]::InvariantCulture
            )
            break
        }
    }
    if ($null -eq $price) { return $null }

    $blocks = [System.Collections.Generic.List[object]]::new()
    foreach ($line in $lines) {
        $normalizedLine = Normalize-OracleText ($line -replace "\s+", " ")
        if ($normalizedLine -notmatch "(?i)km") { continue }
        $distanceMatch = [regex]::Match($normalizedLine, "([0-9]+(?:[,.][0-9]+)?)\s*km", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        if (-not $distanceMatch.Success) { continue }
        $duration = ConvertTo-Minutes -Text $normalizedLine
        if ($null -eq $duration -or $duration -le 0) { continue }
        $distance = [double]::Parse(
            $distanceMatch.Groups[1].Value.Replace(",", "."),
            [System.Globalization.CultureInfo]::InvariantCulture
        )
        if ($distance -le 0) { continue }
        $blocks.Add([pscustomobject]@{ minutes = [int]$duration; distance = $distance })
        if ($blocks.Count -ge 2) { break }
    }

    if ($blocks.Count -lt 2) { return $null }
    $totalMinutes = [int]($blocks[0].minutes + $blocks[1].minutes)
    $totalDistance = [double]($blocks[0].distance + $blocks[1].distance)
    return [string]::Format(
        [System.Globalization.CultureInfo]::InvariantCulture,
        "{0:F2}|{1:F1}|{2}",
        $price,
        $totalDistance,
        $totalMinutes
    )
}

function ConvertTo-Minutes {
    param([string]$Text)

    $Text = Normalize-OracleText $Text
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    $normalized = $Text.ToLowerInvariant() -replace "\s+", " "
    $hours = 0
    $minutes = 0

    $compact = [regex]::Match($normalized, "(\d+)\s*h\s*(\d{1,2})(?!\d)")
    if ($compact.Success) {
        $hours = [int]$compact.Groups[1].Value
        $minutes = [int]$compact.Groups[2].Value
        return ($hours * 60) + $minutes
    }

    $hourMatch = [regex]::Match($normalized, "(\d+)\s*(?:h|hora|horas)\b")
    if ($hourMatch.Success) {
        $hours = [int]$hourMatch.Groups[1].Value
    }

    $minuteMatch = [regex]::Match($normalized, "(\d+)\s*(?:min|minuto|minutos)\b")
    if ($minuteMatch.Success) {
        $minutes = [int]$minuteMatch.Groups[1].Value
    }

    $total = ($hours * 60) + $minutes
    if ($total -le 0) { return $null }
    return $total
}

function Send-OfferTextToCalcMot {
    param(
        [string]$AdbPath,
        [string]$PackageName,
        [string]$OfferText
    )

    $encoded = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($OfferText))
    & $AdbPath shell am broadcast `
        -a br.com.calcmot.DEBUG_UIAUTOMATOR_OFFER `
        -n "$PackageName/.debug.UiAutomatorOfferReceiver" `
        --es offer_text_b64 $encoded | Out-Null
}

function Send-InvalidFrameToCalcMot {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    & $AdbPath shell am broadcast `
        -a br.com.calcmot.DEBUG_UIAUTOMATOR_OFFER `
        -n "$PackageName/.debug.UiAutomatorOfferReceiver" `
        --ez invalid true | Out-Null
}

function Clear-CaptureLearningLab {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    $cleanupCommand = "'rm -rf files/capture-learning files/accessibility-lab; mkdir -p files/capture-learning files/accessibility-lab; true'"
    & $AdbPath shell run-as $PackageName sh -c $cleanupCommand 2>$null | Out-Null
}

function Read-CaptureLearningEvents {
    param(
        [string]$AdbPath,
        [string]$PackageName,
        [string]$SessionDir
    )

    $eventDir = Join-Path $SessionDir "capture-learning"
    New-Item -ItemType Directory -Force -Path $eventDir | Out-Null
    $events = [System.Collections.Generic.List[object]]::new()
    $remoteFiles = & $AdbPath shell run-as $PackageName find files/capture-learning -type f 2>$null | Sort-Object
    foreach ($remoteFile in $remoteFiles) {
        $cleanRemoteFile = $remoteFile.Trim()
        if ([string]::IsNullOrWhiteSpace($cleanRemoteFile)) { continue }
        if ($cleanRemoteFile -notmatch "\.json$") { continue }

        $relativePath = $cleanRemoteFile -replace "^.*files/capture-learning/", ""
        $relativePath = $relativePath -replace "^[\\/]+", ""
        $localPath = Join-Path $eventDir ($relativePath -replace "/", "\")
        $localParent = [System.IO.Path]::GetDirectoryName($localPath)
        [System.IO.Directory]::CreateDirectory($localParent) | Out-Null

        $content = (& $AdbPath exec-out run-as $PackageName cat $cleanRemoteFile | Out-String)
        [System.IO.File]::WriteAllText($localPath, $content, [System.Text.Encoding]::UTF8)
        try {
            $events.Add(($content | ConvertFrom-Json))
        } catch {
            Write-Warning "Could not parse capture learning event $cleanRemoteFile"
        }
    }
    return @($events)
}

function Copy-DeviceJsonTree {
    param(
        [string]$AdbPath,
        [string]$PackageName,
        [string]$SessionDir,
        [string]$RemoteDir,
        [string]$LocalName
    )

    $targetDir = Join-Path $SessionDir $LocalName
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    $copied = 0
    $remoteFiles = & $AdbPath shell run-as $PackageName find $RemoteDir -type f 2>$null | Sort-Object
    foreach ($remoteFile in $remoteFiles) {
        $cleanRemoteFile = $remoteFile.Trim()
        if ([string]::IsNullOrWhiteSpace($cleanRemoteFile)) { continue }
        if ($cleanRemoteFile -notmatch "\.json$") { continue }

        $relativePath = $cleanRemoteFile -replace "^$([regex]::Escape($RemoteDir))/", ""
        $relativePath = $relativePath -replace "^[\\/]+", ""
        $localPath = Join-Path $targetDir ($relativePath -replace "/", "\")
        $localParent = [System.IO.Path]::GetDirectoryName($localPath)
        [System.IO.Directory]::CreateDirectory($localParent) | Out-Null

        $content = (& $AdbPath exec-out run-as $PackageName cat $cleanRemoteFile | Out-String)
        [System.IO.File]::WriteAllText($localPath, $content, [System.Text.Encoding]::UTF8)
        $copied++
    }

    return $copied
}

function Build-LearningBacklog {
    param(
        [object[]]$OracleEvents,
        [object[]]$ProductionEvents
    )

    $oracleByFingerprint = @{}
    foreach ($event in $OracleEvents) {
        if ($event.state -ne "uiautomator_complete") { continue }
        if ([string]::IsNullOrWhiteSpace($event.canonical_fingerprint)) { continue }
        if (-not $oracleByFingerprint.ContainsKey($event.canonical_fingerprint)) {
            $oracleByFingerprint[$event.canonical_fingerprint] = $event
        }
    }

    $productionOverlays = @($ProductionEvents | Where-Object {
        $_.status -eq "overlay_shown" -and
        $_.source -ne "uiautomator_lab" -and
        -not [string]::IsNullOrWhiteSpace($_.fingerprint)
    })
    $productionCandidates = @($ProductionEvents | Where-Object {
        $_.status -eq "candidate" -and
        $_.source -ne "uiautomator_lab" -and
        -not [string]::IsNullOrWhiteSpace($_.fingerprint)
    })

    $overlayByFingerprint = @{}
    foreach ($overlay in $productionOverlays) {
        if (-not $overlayByFingerprint.ContainsKey($overlay.fingerprint)) {
            $overlayByFingerprint[$overlay.fingerprint] = $overlay
        }
    }

    $candidateSourceByFingerprint = @{}
    foreach ($candidate in $productionCandidates) {
        if (-not $candidateSourceByFingerprint.ContainsKey($candidate.fingerprint)) {
            $candidateSourceByFingerprint[$candidate.fingerprint] = $candidate.source
        }
    }

    $items = [System.Collections.Generic.List[object]]::new()
    $correct = 0
    foreach ($fingerprint in $oracleByFingerprint.Keys) {
        $oracle = $oracleByFingerprint[$fingerprint]
        if ($overlayByFingerprint.ContainsKey($fingerprint)) {
            $correct++
            $source = $overlayByFingerprint[$fingerprint].source
            $category = if ($source -eq "accessibility_tree") {
                "TREE_FULL"
            } else {
                "DEBUG_BRIDGE"
            }
            $items.Add([ordered]@{
                category = $category
                fingerprint = $fingerprint
                source = $source
                action = "keep_regression_fixture"
                xml_file = $oracle.xml_file
                screenshot_file = $oracle.screenshot_file
            })
        } else {
            $items.Add([ordered]@{
                category = "UIA_ONLY"
                fingerprint = $fingerprint
                source = "uiautomator_oracle"
                action = "investigate_timing_tree_refresh_or_node_mapping"
                xml_file = $oracle.xml_file
                screenshot_file = $oracle.screenshot_file
            })
        }
    }

    $falsePositiveCount = 0
    $wrongValueCount = 0
    foreach ($overlay in $productionOverlays) {
        if ($oracleByFingerprint.ContainsKey($overlay.fingerprint)) { continue }
        $nearOracle = Get-NearOracleEvent -OverlayEvent $overlay -OracleEvents $OracleEvents
        if ($null -ne $nearOracle) {
            $wrongValueCount++
            $items.Add([ordered]@{
                category = "WRONG_VALUE"
                fingerprint = $overlay.fingerprint
                expected_fingerprint = $nearOracle.canonical_fingerprint
                source = $overlay.source
                action = "add_regression_and_fix_accessibility_parser_or_scoring"
                xml_file = $nearOracle.xml_file
                screenshot_file = $nearOracle.screenshot_file
            })
        } else {
            $falsePositiveCount++
            $items.Add([ordered]@{
                category = "FALSE_POSITIVE"
                fingerprint = $overlay.fingerprint
                source = $overlay.source
                action = "tighten_spatial_validation_before_increasing_coverage"
                xml_file = $null
                screenshot_file = $null
            })
        }
    }

    return [ordered]@{
        items = @($items)
        correct_overlay_count = $correct
        false_positive_count = $falsePositiveCount
        wrong_value_count = $wrongValueCount
    }
}

function Get-NearOracleEvent {
    param(
        [object]$OverlayEvent,
        [object[]]$OracleEvents
    )

    if ($null -eq $OverlayEvent.capturedAtMillis) { return $null }
    $overlayAt = [long]$OverlayEvent.capturedAtMillis
    $windowMs = 3500
    $near = @($OracleEvents | Where-Object {
        $_.state -eq "uiautomator_complete" -and
        $null -ne $_.timestamp_ms -and
        [Math]::Abs(([long]$_.timestamp_ms) - $overlayAt) -le $windowMs
    } | Sort-Object { [Math]::Abs(([long]$_.timestamp_ms) - $overlayAt) })
    if ($near.Count -eq 0) { return $null }
    return $near[0]
}

function Get-DiagnosticSnapshot {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    $counters = @{}
    $raw = (& $AdbPath exec-out run-as $PackageName cat shared_prefs/calcmot_diagnostics.xml 2>$null | Out-String)
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $counters
    }

    foreach ($match in [regex]::Matches($raw, '<(?:long|int) name="([^"]+)" value="([^"]+)"')) {
        $name = $match.Groups[1].Value
        $value = 0L
        if ([long]::TryParse($match.Groups[2].Value, [ref]$value)) {
            $counters[$name] = $value
        }
    }

    return $counters
}

function Test-CalcMotMonitoringEnabled {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    $raw = (& $AdbPath exec-out run-as $PackageName cat shared_prefs/calcmot_settings.xml 2>$null | Out-String)
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $true
    }

    return ($raw -notmatch 'name="monitoring_enabled"\s+value="false"')
}

function Test-CalcMotAccessibilityEnabled {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    $enabled = (& $AdbPath shell settings get secure enabled_accessibility_services 2>$null | Out-String)
    if ([string]::IsNullOrWhiteSpace($enabled)) {
        return $false
    }

    $serviceName = "$PackageName/$PackageName.accessibility.UberAccessibilityService"
    return $enabled.Contains($serviceName)
}

function Get-Counter {
    param(
        [hashtable]$Counters,
        [string]$Name
    )

    if ($Counters.ContainsKey($Name)) {
        return [long]$Counters[$Name]
    }
    return 0L
}

function Get-CounterDelta {
    param(
        [hashtable]$Before,
        [hashtable]$After,
        [string]$Name
    )

    return (Get-Counter -Counters $After -Name $Name) - (Get-Counter -Counters $Before -Name $Name)
}

function Get-CalcMotResourceSample {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    $sampledAt = Get-Date
    $memRaw = (& $AdbPath shell dumpsys meminfo $PackageName 2>$null | Out-String)
    $cpuRaw = (& $AdbPath shell dumpsys cpuinfo 2>$null | Out-String)
    $topRaw = (& $AdbPath shell top -b -n 1 2>$null | Out-String)

    $pssKb = $null
    $rssKb = $null
    $nativeHeapKb = $null
    $cpuPercent = $null

    if ($memRaw -match "(?m)^\s*TOTAL\s+(\d+)") {
        $pssKb = [int]$Matches[1]
    }
    if ($memRaw -match "TOTAL RSS:\s+(\d+)KB") {
        $rssKb = [int]$Matches[1]
    }
    if ($memRaw -match "(?m)^\s*Native Heap\s+(\d+)") {
        $nativeHeapKb = [int]$Matches[1]
    }
    $escapedPackage = [regex]::Escape($PackageName)
    if ($cpuRaw -match "(?m)^\s*(\d+(?:\.\d+)?)%\s+\d+/$escapedPackage\b") {
        $cpuPercent = [double]$Matches[1]
    }
    if ($null -eq $cpuPercent -and $topRaw -match "(?m)^\s*\d+\s+\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+(\d+(?:\.\d+)?)\s+\d+(?:\.\d+)?\s+\S+\s+$escapedPackage\s*$") {
        $cpuPercent = [double]$Matches[1]
    }

    return [pscustomobject][ordered]@{
        timestamp = $sampledAt.ToString("yyyyMMdd-HHmmss-fff")
        timestamp_ms = ([DateTimeOffset]$sampledAt).ToUnixTimeMilliseconds()
        cpu_percent = $cpuPercent
        pss_kb = $pssKb
        rss_kb = $rssKb
        native_heap_pss_kb = $nativeHeapKb
    }
}

function Measure-NullableAverage {
    param(
        [object[]]$Samples,
        [string]$Property
    )

    $values = @($Samples | ForEach-Object { $_.$Property } | Where-Object { $null -ne $_ })
    if ($values.Count -eq 0) { return $null }
    return [Math]::Round(($values | Measure-Object -Average).Average, 2)
}

function Measure-NullableMaximum {
    param(
        [object[]]$Samples,
        [string]$Property
    )

    $values = @($Samples | ForEach-Object { $_.$Property } | Where-Object { $null -ne $_ })
    if ($values.Count -eq 0) { return $null }
    return ($values | Measure-Object -Maximum).Maximum
}

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
$dumpDir = Join-Path $sessionDir "uiautomator"
$screenshotDir = Join-Path $sessionDir "screenshots"
New-Item -ItemType Directory -Force -Path $dumpDir, $screenshotDir | Out-Null

& $adb shell monkey -p $DriverPackageName 1 | Out-Null
if (-not $NoClearLogcat) {
    & $adb logcat -c
}
if ($ClearDeviceLearningBeforeRun) {
    Clear-CaptureLearningLab -AdbPath $adb -PackageName $PackageName
}

$baselineDiagnostics = Get-DiagnosticSnapshot -AdbPath $adb -PackageName $PackageName
$accessibilityEnabled = Test-CalcMotAccessibilityEnabled -AdbPath $adb -PackageName $PackageName
if (-not $accessibilityEnabled) {
    Write-Warning "A acessibilidade do CalcMot NAO esta ativa. O OracleOnly ainda vera cards pelo UIAutomator, mas a producao nao consegue capturar pelo app."
}
if (-not (Test-CalcMotMonitoringEnabled -AdbPath $adb -PackageName $PackageName)) {
    Write-Warning "CalcMot esta PAUSADO no aparelho. Abra o app e ligue Monitoramento antes da sessao para validar overlay real."
}
$endAt = (Get-Date).AddSeconds($DurationSeconds)
$stopReason = "duration_elapsed"
$lastFingerprint = $null
$validFrames = 0
$invalidFrames = 0
$uniqueOfferFingerprints = [System.Collections.Generic.HashSet[string]]::new()
$submittedFingerprints = [System.Collections.Generic.HashSet[string]]::new()
$firstSeenAt = @{}
$latencySamples = [System.Collections.Generic.List[double]]::new()
$events = [System.Collections.Generic.List[object]]::new()
$fixtures = [System.Collections.Generic.List[object]]::new()
$resourceSamples = [System.Collections.Generic.List[object]]::new()
$nextResourceSampleAt = Get-Date

while ((Get-Date) -lt $endAt) {
    $now = Get-Date
    if ($now -ge $nextResourceSampleAt) {
        $resourceSamples.Add((Get-CalcMotResourceSample -AdbPath $adb -PackageName $PackageName))
        $nextResourceSampleAt = $now.AddSeconds($ResourceSampleIntervalSeconds)
    }
    $stamp = $now.ToString("yyyyMMdd-HHmmss-fff")
    $timestampMs = ([DateTimeOffset]$now).ToUnixTimeMilliseconds()
    $localXml = Join-Path $dumpDir "$stamp.xml"

    $xml = Read-UiAutomatorDump -AdbPath $adb -TimeoutSeconds $UiAutomatorDumpTimeoutSeconds
    if ([string]::IsNullOrWhiteSpace($xml)) {
        Write-Warning "UIAutomator dump failed at $stamp"
        Start-Sleep -Milliseconds $IntervalMilliseconds
        continue
    }
    [System.IO.File]::WriteAllText($localXml, $xml, [System.Text.Encoding]::UTF8)
    $screenshotPath = $null

    $offerText = Extract-UberTextFromDump -XmlPath $localXml -DriverPackageName $DriverPackageName
    $evidence = Extract-OfferEvidenceFromDump -XmlPath $localXml -DriverPackageName $DriverPackageName
    if (Is-OfferLikeText $offerText) {
        $validFrames++
        $rawFingerprint = Fingerprint-OfferText $offerText
        $canonicalFingerprint = ConvertTo-CanonicalOfferFingerprint $offerText
        $fingerprint = if ([string]::IsNullOrWhiteSpace($canonicalFingerprint)) { $rawFingerprint } else { $canonicalFingerprint }
        if (-not $firstSeenAt.ContainsKey($fingerprint)) {
            $firstSeenAt[$fingerprint] = $now
        }
        [void]$uniqueOfferFingerprints.Add($fingerprint)

        if ($fingerprint -ne $lastFingerprint) {
            if ($CaptureScreenshots) {
                $screenshotPath = Capture-Screenshot -AdbPath $adb -Stamp $stamp -ScreenshotDir $screenshotDir
            }
            Write-Host "[$stamp] UIAutomator offer candidate: $fingerprint"
            if ($rawFingerprint -ne $fingerprint) {
                Write-Host "[$stamp] Raw UIA fingerprint: $rawFingerprint"
            }
            if ($ShowExtractedText) {
                Write-Host $offerText
            }
            $lastFingerprint = $fingerprint
        }

        if (-not $OracleOnly) {
            Send-OfferTextToCalcMot -AdbPath $adb -PackageName $PackageName -OfferText $offerText
        }
        if ($submittedFingerprints.Add($fingerprint)) {
            $latencySamples.Add(($now - $firstSeenAt[$fingerprint]).TotalMilliseconds)
            if ($CaptureScreenshots -and $screenshotPath -eq $null) {
                $screenshotPath = Capture-Screenshot -AdbPath $adb -Stamp $stamp -ScreenshotDir $screenshotDir
            }
            $fixtures.Add([ordered]@{
                fingerprint = $fingerprint
                raw_fingerprint = $rawFingerprint
                canonical_fingerprint = $canonicalFingerprint
                offer_text = $offerText
                xml_file = "uiautomator/$stamp.xml"
                screenshot_file = if ($screenshotPath) { "screenshots/$stamp.png" } else { $null }
                card_bounds = $evidence.bounds
                semantic_lines = $evidence.lines
            })
        }
        $events.Add([ordered]@{
            timestamp = $stamp
            timestamp_ms = $timestampMs
            state = "uiautomator_complete"
            fingerprint = $fingerprint
            raw_fingerprint = $rawFingerprint
            canonical_fingerprint = $canonicalFingerprint
            xml_file = "uiautomator/$stamp.xml"
            screenshot_file = if ($screenshotPath) { "screenshots/$stamp.png" } else { $null }
            card_bounds = $evidence.bounds
            semantic_lines = $evidence.lines
        })

        if ($MaxUniqueCards -gt 0 -and $uniqueOfferFingerprints.Count -ge $MaxUniqueCards) {
            $stopReason = "max_unique_cards"
            Write-Host "[$stamp] Max unique cards reached: $($uniqueOfferFingerprints.Count)/$MaxUniqueCards"
            break
        }
    } else {
        $invalidFrames++
        if ($lastFingerprint -ne $null) {
            if ($CaptureScreenshots) {
                $screenshotPath = Capture-Screenshot -AdbPath $adb -Stamp $stamp -ScreenshotDir $screenshotDir
            }
            Write-Host "[$stamp] UIAutomator offer disappeared"
            $lastFingerprint = $null
        }
        if (-not $OracleOnly) {
            Send-InvalidFrameToCalcMot -AdbPath $adb -PackageName $PackageName
        }
        $events.Add([ordered]@{
            timestamp = $stamp
            timestamp_ms = $timestampMs
            state = "invalid_frame"
            fingerprint = $null
            raw_fingerprint = $null
            canonical_fingerprint = $null
            xml_file = "uiautomator/$stamp.xml"
            screenshot_file = if ($screenshotPath) { "screenshots/$stamp.png" } else { $null }
            card_bounds = $evidence.bounds
            semantic_lines = $evidence.lines
        })
    }

    Start-Sleep -Milliseconds $IntervalMilliseconds
}

$finalDiagnostics = Get-DiagnosticSnapshot -AdbPath $adb -PackageName $PackageName
$productionEvents = Read-CaptureLearningEvents -AdbPath $adb -PackageName $PackageName -SessionDir $sessionDir
$accessibilityLabSnapshotCount = Copy-DeviceJsonTree `
    -AdbPath $adb `
    -PackageName $PackageName `
    -SessionDir $sessionDir `
    -RemoteDir "files/accessibility-lab" `
    -LocalName "accessibility-lab"
$learning = Build-LearningBacklog -OracleEvents @($events) -ProductionEvents @($productionEvents)
$learningItems = @($learning.items)
$categoryCounts = [ordered]@{}
foreach ($item in $learningItems) {
    $category = [string]$item.category
    if (-not $categoryCounts.Contains($category)) {
        $categoryCounts[$category] = 0
    }
    $categoryCounts[$category]++
}
$uiautomatorLabComplete = Get-CounterDelta `
    -Before $baselineDiagnostics `
    -After $finalDiagnostics `
    -Name "capture_complete_uiautomator_lab"
$internalTreeComplete = Get-CounterDelta `
    -Before $baselineDiagnostics `
    -After $finalDiagnostics `
    -Name "capture_complete_accessibility_tree"
$overlayShown = Get-CounterDelta `
    -Before $baselineDiagnostics `
    -After $finalDiagnostics `
    -Name "stage_count_overlay_shown"
$productionOverlayShown = @($productionEvents | Where-Object {
    $_.status -eq "overlay_shown" -and $_.source -ne "uiautomator_lab"
}).Count
$correctOverlayCount = [int]$learning.correct_overlay_count
$falsePositiveCount = [int]$learning.false_positive_count
$wrongValueCount = [int]$learning.wrong_value_count
$missedCards = [Math]::Max(0, $uniqueOfferFingerprints.Count - $correctOverlayCount)
$averageLatency = if ($latencySamples.Count -gt 0) {
    [Math]::Round(($latencySamples | Measure-Object -Average).Average, 2)
} else {
    $null
}
$coverageCorrectPercent = if ($uniqueOfferFingerprints.Count -gt 0) {
    [Math]::Round(($correctOverlayCount / $uniqueOfferFingerprints.Count) * 100, 2)
} else {
    $null
}
$betaCoveragePercent = $coverageCorrectPercent
$betaReadyMin85 = (
    $coverageCorrectPercent -ne $null -and
    $coverageCorrectPercent -ge 85.0 -and
    $falsePositiveCount -eq 0 -and
    $wrongValueCount -eq 0
)

$eventsPath = Join-Path $sessionDir "oracle-events.json"
$events | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $eventsPath -Encoding UTF8
$fixturesPath = Join-Path $sessionDir "oracle-fixtures.json"
$fixtures | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $fixturesPath -Encoding UTF8
$productionEventsPath = Join-Path $sessionDir "production-events.json"
$productionEvents | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $productionEventsPath -Encoding UTF8
$backlogPath = Join-Path $sessionDir "learning-backlog.json"
$learningItems | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $backlogPath -Encoding UTF8
$resourceSamplesPath = Join-Path $sessionDir "resource-samples.json"
$resourceSamples | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $resourceSamplesPath -Encoding UTF8

$avgCpuPercent = Measure-NullableAverage -Samples @($resourceSamples) -Property "cpu_percent"
$maxCpuPercent = Measure-NullableMaximum -Samples @($resourceSamples) -Property "cpu_percent"
$avgPssKb = Measure-NullableAverage -Samples @($resourceSamples) -Property "pss_kb"
$maxPssKb = Measure-NullableMaximum -Samples @($resourceSamples) -Property "pss_kb"
$avgRssKb = Measure-NullableAverage -Samples @($resourceSamples) -Property "rss_kb"
$maxRssKb = Measure-NullableMaximum -Samples @($resourceSamples) -Property "rss_kb"

$report = [ordered]@{
    session = $session
    oracle_only = $OracleOnly.IsPresent
    ocr_enabled = $false
    accessibility_service_enabled = $accessibilityEnabled
    duration_seconds = $DurationSeconds
    max_unique_cards = $MaxUniqueCards
    stop_reason = $stopReason
    interval_milliseconds = $IntervalMilliseconds
    uiautomator_dump_timeout_seconds = $UiAutomatorDumpTimeoutSeconds
    valid_frames = $validFrames
    invalid_frames = $invalidFrames
    uiautomator_complete_cards = $uniqueOfferFingerprints.Count
    accessibility_lab_snapshots = $accessibilityLabSnapshotCount
    internal_tree_complete_cards = $internalTreeComplete
    overlay_shown = $overlayShown
    production_overlay_shown = $productionOverlayShown
    correct_overlay_cards = $correctOverlayCount
    missed_cards = $missedCards
    false_positive_count = $falsePositiveCount
    wrong_value_count = $wrongValueCount
    coverage_correct_percent = $coverageCorrectPercent
    average_latency_ms = $averageLatency
    beta_overlay_coverage_percent = $betaCoveragePercent
    beta_ready_min_85 = $betaReadyMin85
    debug_bridge_complete_cards = $uiautomatorLabComplete
    submitted_uiautomator_cards = $submittedFingerprints.Count
    resource_sample_count = $resourceSamples.Count
    resource_cpu_avg_percent = $avgCpuPercent
    resource_cpu_max_percent = $maxCpuPercent
    resource_pss_avg_kb = $avgPssKb
    resource_pss_max_kb = $maxPssKb
    resource_rss_avg_kb = $avgRssKb
    resource_rss_max_kb = $maxRssKb
    learning_categories = $categoryCounts
    dump_count = (Get-ChildItem -LiteralPath $dumpDir -Filter "*.xml").Count
}

$reportJson = Join-Path $sessionDir "oracle-report.json"
$reportMd = Join-Path $sessionDir "oracle-report.md"
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $reportJson -Encoding UTF8
$categorySummary = if ($categoryCounts.Count -gt 0) {
    ($categoryCounts.GetEnumerator() | Sort-Object Name | ForEach-Object {
        "- $($_.Name): $($_.Value)"
    }) -join "`n"
} else {
    "- none: 0"
}
$backlogPreview = if ($learningItems.Count -gt 0) {
    ($learningItems | Sort-Object category, fingerprint | Select-Object -First 20 | ForEach-Object {
        $expected = if ($_.expected_fingerprint) { " expected=$($_.expected_fingerprint)" } else { "" }
        "- $($_.category): $($_.fingerprint)$expected source=$($_.source) xml=$($_.xml_file) action=$($_.action)"
    }) -join "`n"
} else {
    "- none"
}

@"
# CalcMot UIAutomator Oracle Report

- Session: $session
- Oracle only: $($OracleOnly.IsPresent)
- OCR enabled: False
- Accessibility service enabled: $accessibilityEnabled
- Duration seconds: $DurationSeconds
- Max unique cards: $MaxUniqueCards
- Stop reason: $stopReason
- UIAutomator dump timeout seconds: $UiAutomatorDumpTimeoutSeconds
- Valid frames: $validFrames
- Invalid frames: $invalidFrames
- uiautomator_complete_cards: $($report.uiautomator_complete_cards)
- internal_tree_complete_cards: $internalTreeComplete
- overlay_shown: $overlayShown
- production_overlay_shown: $productionOverlayShown
- correct_overlay_cards: $correctOverlayCount
- missed_cards: $missedCards
- false_positive_count: $falsePositiveCount
- wrong_value_count: $wrongValueCount
- coverage_correct_percent: $coverageCorrectPercent
- average_latency_ms: $averageLatency
- beta_overlay_coverage_percent: $betaCoveragePercent
- beta_ready_min_85: $betaReadyMin85
- debug_bridge_complete_cards: $uiautomatorLabComplete
- resource_sample_count: $($resourceSamples.Count)
- resource_cpu_avg_percent: $avgCpuPercent
- resource_cpu_max_percent: $maxCpuPercent
- resource_pss_avg_kb: $avgPssKb
- resource_pss_max_kb: $maxPssKb
- resource_rss_avg_kb: $avgRssKb
- resource_rss_max_kb: $maxRssKb

## Learning categories

$categorySummary

## Learning backlog preview

$backlogPreview

## How to read

- uiautomator_complete_cards is the oracle count from shell-accessible Android UI hierarchy.
- internal_tree_complete_cards is the production AccessibilityService route count for the same session.
- ocr_enabled is always false in this lab; no screenshot/OCR/ML Kit route is allowed to create candidates.
- production_overlay_shown ignores overlays created by the UIAutomator debug bridge.
- correct_overlay_cards is the number of unique oracle cards matched by production overlay fingerprints.
- false_positive_count means production showed an overlay that the oracle did not see as a card.
- wrong_value_count means production showed an overlay near an oracle card but with a divergent fingerprint.
- missed_cards means UIAutomator saw a unique complete card that production did not match.
- resource_cpu_* and resource_*_kb come from Android dumpsys samples during the session.

Use the XML files, screenshots, oracle-events.json, production-events.json, learning-backlog.json and oracle-fixtures.json in this folder to compare exact card timing, false negatives, false positives and wrong values.
"@ | Set-Content -LiteralPath $reportMd -Encoding UTF8

Write-Host "UIAutomator bridge session saved to $sessionDir"
Write-Host "Report: $reportMd"
Write-Host "Valid frames: $validFrames"
Write-Host "Invalid frames: $invalidFrames"
Write-Host "UIAutomator complete cards: $($report.uiautomator_complete_cards)"
Write-Host "Internal tree complete cards: $internalTreeComplete"
Write-Host "Overlay shown: $overlayShown"
Write-Host "Production overlay shown: $productionOverlayShown"
Write-Host "Correct overlay cards: $correctOverlayCount"
Write-Host "Missed cards: $missedCards"
Write-Host "False positives: $falsePositiveCount"
Write-Host "Wrong values: $wrongValueCount"
Write-Host "Coverage correct percent: $coverageCorrectPercent"
Write-Host "Resource CPU avg/max percent: $avgCpuPercent / $maxCpuPercent"
Write-Host "Resource PSS avg/max KB: $avgPssKb / $maxPssKb"
Write-Host "Beta ready min 85: $betaReadyMin85"
