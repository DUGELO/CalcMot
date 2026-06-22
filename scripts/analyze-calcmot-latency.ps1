param(
    [string]$InputPath,
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$OutputDir = "build\reports\calcmot-latency",
    [int]$MinTraceCount = 1,
    [ValidateSet("all", "uber", "99")]
    [string]$DriverApp = "all"
)

$ErrorActionPreference = "Stop"

$stageOrder = @(
    "T_MINUS1_VISUAL_OFFER_APPEARED",
    "T0_ACCESSIBILITY_EVENT_RECEIVED",
    "T1_PACKAGE_ALLOWED_CONFIRMED",
    "T2_ROOT_READ_START",
    "T3_ROOT_READ_END",
    "T4_TREE_EXTRACT_START",
    "T5_TREE_EXTRACT_END",
    "T6_CANDIDATE_PARSED",
    "T7_CANDIDATE_COMPLETE",
    "T8_STABILITY_ACCEPTED",
    "T9_OVERLAY_REQUESTED",
    "T10_OVERLAY_ADD_OR_UPDATE_START",
    "T11_OVERLAY_ADD_OR_UPDATE_END",
    "T12_OVERLAY_FIRST_DRAW",
    "T13_OVERLAY_VISIBLE_TO_USER"
)
$stageIndex = @{}
for ($i = 0; $i -lt $stageOrder.Count; $i++) {
    $stageIndex[$stageOrder[$i]] = $i
}

function Get-KeyValues([string]$Line) {
    $values = @{}
    foreach ($match in [regex]::Matches($Line, '([A-Za-z][A-Za-z0-9_]*)=("[^"]*"|[^ \r\n]+)')) {
        $key = $match.Groups[1].Value
        $value = $match.Groups[2].Value.Trim('"')
        $values[$key] = $value
    }
    return $values
}

function To-Long($Value, [long]$Default = -1) {
    $parsed = 0L
    if ([long]::TryParse([string]$Value, [ref]$parsed)) {
        return $parsed
    }
    return $Default
}

function Percentile($Values, [double]$Percentile) {
    $sorted = @($Values | Where-Object { $_ -ge 0 } | Sort-Object)
    if ($sorted.Count -eq 0) { return -1 }
    $index = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count) - 1
    $index = [Math]::Max(0, [Math]::Min($sorted.Count - 1, $index))
    return $sorted[$index]
}

function AverageOrMinusOne($Values) {
    $valid = @($Values | Where-Object { $_ -ge 0 })
    if ($valid.Count -eq 0) { return -1 }
    return [Math]::Round((($valid | Measure-Object -Average).Average), 1)
}

function MaxOrMinusOne($Values) {
    $valid = @($Values | Where-Object { $_ -ge 0 })
    if ($valid.Count -eq 0) { return -1 }
    return (($valid | Measure-Object -Maximum).Maximum)
}

function Last-Stage($Trace) {
    $last = $Trace.stages.Keys |
        Sort-Object { if ($stageIndex.ContainsKey($_)) { $stageIndex[$_] } else { -1 } } |
        Select-Object -Last 1
    if (-not $last) { return @("unknown", -1) }
    return @($last, (To-Long $Trace.stages[$last]))
}

function Infer-LossReason($LastStage, $EndReason, [bool]$Visible, [bool]$Closed) {
    if ($Visible) { return "VISIBLE_TO_USER" }
    if ($Closed -and $EndReason) { return $EndReason }
    switch ($LastStage) {
        "T1_PACKAGE_ALLOWED_CONFIRMED" { return "UNCLOSED_AFTER_PACKAGE_ALLOWED" }
        "T4_TREE_EXTRACT_START" { return "UNCLOSED_DURING_TREE_EXTRACT" }
        "T5_TREE_EXTRACT_END" { return "UNCLOSED_AFTER_TREE_EXTRACT" }
        "T7_CANDIDATE_COMPLETE" { return "UNCLOSED_AFTER_CANDIDATE_COMPLETE" }
        "T11_OVERLAY_ADD_OR_UPDATE_END" { return "UNCLOSED_AFTER_OVERLAY_WINDOW" }
        default { return "UNCLOSED_AFTER_$LastStage" }
    }
}

function Is-RenewedReason($Reason) {
    return $Reason -eq "RENEWED_VISIBLE_OVERLAY" -or
        $Reason -eq "CANDIDATE_RENEWED_VISIBLE_OVERLAY"
}

function Is-SafeModeReason($Reason) {
    return $Reason -eq "SAFE_MODE_BLOCKED_USER_APP" -or
        $Reason -eq "SAFE_MODE"
}

if ($InputPath) {
    $lines = Get-Content -LiteralPath $InputPath
} else {
    if (-not (Test-Path -LiteralPath $AdbPath)) {
        throw "adb not found: $AdbPath"
    }
    $lines = & $AdbPath logcat -d -v time CalcMotLatency:W UberReader:W OverlayManager:W '*:S'
}

$traces = @{}
$logCounters = @{
    transientIgnored = 0
    blockedOverlayRemoved = 0
    transientOverlayRemoved = 0
}

foreach ($line in $lines) {
    if ($line -match 'CALCMOT_TRANSIENT_SYSTEM_EVENT_IGNORED|OVERLAY_FOREGROUND_TRANSIENT_IGNORED') {
        $logCounters.transientIgnored += 1
    }
    if ($line -match 'OVERLAY_BLOCKED_USER_APP') {
        $logCounters.blockedOverlayRemoved += 1
        if ($line -match 'package=(android|com\.android\.systemui|com\.samsung\.android\.app\.smartcapture|unknown|br\.com\.calcmot)') {
            $logCounters.transientOverlayRemoved += 1
        }
    }

    if ($line -notmatch 'CALCMOT_LATENCY_TRACE|CALCMOT_LATENCY_TRACE_SUMMARY|CALCMOT_LATENCY_METRIC|CALCMOT_LATENCY_TRACE_END') {
        continue
    }

    $kv = Get-KeyValues $line
    $traceId = $kv["traceId"]
    if (-not $traceId) { continue }

    if (-not $traces.ContainsKey($traceId)) {
        $traces[$traceId] = [ordered]@{
            traceId = $traceId
            package = $kv["package"]
            fingerprint = $kv["fingerprint"]
            stages = @{}
            metrics = @()
            summary = @{}
            end = @{}
        }
    }

    $trace = $traces[$traceId]
    if ($kv["package"]) { $trace.package = $kv["package"] }
    if ($kv["fingerprint"]) { $trace.fingerprint = $kv["fingerprint"] }

    if ($line -match 'CALCMOT_LATENCY_TRACE_END') {
        foreach ($key in $kv.Keys) {
            $trace.end[$key] = $kv[$key]
        }
        continue
    }

    if ($line -match 'CALCMOT_LATENCY_TRACE_SUMMARY') {
        foreach ($key in $kv.Keys) {
            $trace.summary[$key] = $kv[$key]
        }
        continue
    }

    if ($line -match 'CALCMOT_LATENCY_METRIC') {
        $trace.metrics += [pscustomobject]@{
            traceId = $traceId
            metric = $kv["metric"]
            durationMs = To-Long $kv["durationMs"]
            details = $kv["details"]
        }
        continue
    }

    if ($line -match 'CALCMOT_LATENCY_TRACE ') {
        $stage = $kv["stage"]
        if ($stage) {
            $trace.stages[$stage] = To-Long $kv["deltaFromEventMs"]
        }
    }
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$rows = foreach ($trace in $traces.Values) {
    $summary = $trace.summary
    $end = $trace.end
    $last = Last-Stage $trace
    $closed = [bool]$end["closed"]
    $visible = ([string]$end["visible"]) -eq "true" -or [bool]$summary["totalToVisibleMs"]
    $endReason = $end["endReason"]
    $lossReason = Infer-LossReason $last[0] $endReason $visible $closed
    [pscustomobject]@{
        traceId = $trace.traceId
        package = $trace.package
        fingerprint = $trace.fingerprint
        closed = $closed
        visible = $visible
        endReason = $endReason
        lossReason = $lossReason
        lastStage = $last[0]
        lastDeltaMs = $last[1]
        totalToVisibleMs = To-Long $summary["totalToVisibleMs"]
        visualToVisibleMs = To-Long $summary["visualToVisibleMs"]
        visualToEventMs = To-Long $summary["visualToEventMs"]
        requestToVisibleMs = To-Long $summary["requestToVisibleMs"]
        rootReadMs = To-Long $summary["rootReadMs"]
        extractMs = To-Long $summary["extractMs"]
        stabilityMs = To-Long $summary["stabilityMs"]
        largestStage = $summary["largestStage"]
        largestStageMs = To-Long $summary["largestStageMs"]
        latencyClass = $summary["latencyClass"]
        metricCount = $trace.metrics.Count
        candidateComplete = $trace.stages.ContainsKey("T7_CANDIDATE_COMPLETE")
    }
}

$metricRows = foreach ($trace in $traces.Values) {
    foreach ($metric in $trace.metrics) {
        $metric
    }
}

$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path (Join-Path $OutputDir "latency-traces.csv")
$metricRows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path (Join-Path $OutputDir "latency-metrics.csv")

$visibleRows = @($rows | Where-Object { $_.visible -eq $true })
$renewedRows = @($rows | Where-Object { Is-RenewedReason $_.endReason })
$closedNonVisibleRows = @(
    $rows |
        Where-Object {
            $_.closed -eq $true -and
            $_.visible -ne $true -and
            -not (Is-RenewedReason $_.endReason)
        }
)
$unclosedRows = @($rows | Where-Object { $_.closed -ne $true })
$candidateRows = @($rows | Where-Object { $_.candidateComplete -eq $true })
$noCandidateRows = @($rows | Where-Object { $_.endReason -eq "NO_CANDIDATE_AFTER_TREE" })
$selectedDriverPackages = switch ($DriverApp) {
    "uber" { @("com.ubercab.driver", "com.ubercab") }
    "99" { @("com.app99.driver", "br.com.taxis99") }
    default { @("com.ubercab.driver", "com.ubercab", "br.com.taxis99", "com.app99.driver") }
}
$driverRows = @($rows | Where-Object { $_.package -in $selectedDriverPackages })
$safeModeDuringDriverTraceRows = @($driverRows | Where-Object { Is-SafeModeReason $_.endReason })
$candidateFoundRate = if ($rows.Count -gt 0) { [Math]::Round(($candidateRows.Count * 100.0) / $rows.Count, 1) } else { -1 }
$noCandidateRate = if ($rows.Count -gt 0) { [Math]::Round(($noCandidateRows.Count * 100.0) / $rows.Count, 1) } else { -1 }

$topMetrics = @(
    $metricRows |
        Where-Object { $_.durationMs -ge 0 } |
        Group-Object metric |
        ForEach-Object {
            $values = @($_.Group | ForEach-Object { $_.durationMs })
            [pscustomobject]@{
                metric = $_.Name
                count = $values.Count
                avgMs = [Math]::Round((($values | Measure-Object -Average).Average), 1)
                p95Ms = Percentile $values 95
                maxMs = (($values | Measure-Object -Maximum).Maximum)
            }
        } |
        Sort-Object -Property maxMs, p95Ms, avgMs -Descending |
        Select-Object -First 10
)

$topIncompleteCauses = @(
    $rows |
        Where-Object {
            $_.visible -ne $true -and
            -not (Is-RenewedReason $_.endReason)
        } |
        Group-Object lossReason, lastStage |
        ForEach-Object {
            $lastDeltas = @($_.Group | ForEach-Object { $_.lastDeltaMs })
            [pscustomobject]@{
                cause = $_.Group[0].lossReason
                lastStage = $_.Group[0].lastStage
                count = $_.Count
                p95LastDeltaMs = Percentile $lastDeltas 95
                maxLastDeltaMs = MaxOrMinusOne $lastDeltas
            }
        } |
        Sort-Object -Property count, maxLastDeltaMs -Descending |
        Select-Object -First 10
)

$topStages = @(
    $visibleRows |
        Where-Object { $_.largestStage -and $_.largestStageMs -ge 0 } |
        Group-Object largestStage |
        ForEach-Object {
            $values = @($_.Group | ForEach-Object { $_.largestStageMs })
            [pscustomobject]@{
                stage = $_.Name
                count = $values.Count
                avgMs = [Math]::Round((($values | Measure-Object -Average).Average), 1)
                p95Ms = Percentile $values 95
                maxMs = (($values | Measure-Object -Maximum).Maximum)
            }
        } |
        Sort-Object -Property maxMs, p95Ms, avgMs -Descending
)

$reportPath = Join-Path $OutputDir "latency-report.md"
$report = New-Object System.Collections.Generic.List[string]
$report.Add("# CalcMot Latency Audit")
$report.Add("")
$report.Add("- traces: $($traces.Count)")
$report.Add("- visible traces: $($visibleRows.Count)")
$report.Add("- renewed visible overlay: $($renewedRows.Count)")
$report.Add("- true closed non-visible traces: $($closedNonVisibleRows.Count)")
$report.Add("- true incomplete/unclosed traces: $($unclosedRows.Count)")
$report.Add("- candidate_found_rate: $candidateFoundRate %")
$report.Add("- no_candidate_rate: $noCandidateRate %")
$report.Add("- safe_mode_during_driver_trace_count: $($safeModeDuringDriverTraceRows.Count)")
$report.Add("- transient_event_ignored_count: $($logCounters.transientIgnored)")
$report.Add("- overlay_removed_by_blocked_user_app_count: $($logCounters.blockedOverlayRemoved)")
$report.Add("- overlay_removed_by_transient_count: $($logCounters.transientOverlayRemoved)")
$report.Add("- totalToVisible avg: $(AverageOrMinusOne $visibleRows.totalToVisibleMs) ms")
$report.Add("- totalToVisible p95: $(Percentile $visibleRows.totalToVisibleMs 95) ms")
$report.Add("- totalToVisible max: $(MaxOrMinusOne $visibleRows.totalToVisibleMs) ms")
if ($traces.Count -gt 0 -and $visibleRows.Count -eq 0) {
    $report.Add("- audit_valid_for_product_latency: false")
    $report.Add("- audit_invalid_reason: visible traces = 0")
} else {
    $report.Add("- audit_valid_for_product_latency: true")
}
$report.Add("")
$report.Add("## Top 10 Incomplete Causes")
$report.Add("")
$report.Add("| # | Cause | Last stage | Count | P95 last delta ms | Max last delta ms |")
$report.Add("|---|---|---|---:|---:|---:|")
$rank = 1
foreach ($cause in $topIncompleteCauses) {
    $report.Add("| $rank | $($cause.cause) | $($cause.lastStage) | $($cause.count) | $($cause.p95LastDeltaMs) | $($cause.maxLastDeltaMs) |")
    $rank++
}
$report.Add("")
$report.Add("## Renewed/Dedup")
$report.Add("")
$report.Add("- renewed visible overlay traces: $($renewedRows.Count)")
$report.Add("- p95 last delta ms: $(Percentile $renewedRows.lastDeltaMs 95)")
$report.Add("- max last delta ms: $(MaxOrMinusOne $renewedRows.lastDeltaMs)")
$report.Add("")
$report.Add("## Top 10 Metric Sources")
$report.Add("")
$report.Add("| # | Metric | Count | Avg ms | P95 ms | Max ms | Impacto estimado | Risco | Complexidade | Ganho esperado |")
$report.Add("|---|---|---:|---:|---:|---:|---|---|---|---|")
$rank = 1
foreach ($metric in $topMetrics) {
    $impact = "$($metric.p95Ms)ms p95 / $($metric.maxMs)ms max"
    $report.Add("| $rank | $($metric.metric) | $($metric.count) | $($metric.avgMs) | $($metric.p95Ms) | $($metric.maxMs) | $impact | medir antes de alterar | baixa-media | reduzir cauda do stage |")
    $rank++
}
$report.Add("")
$report.Add("## Coalescing Impact Model")
$report.Add("")
$report.Add("| From | To | Max tail gain |")
$report.Add("|---:|---:|---:|")
$report.Add("| 1000ms | 300ms | 700ms |")
$report.Add("| 1000ms | 200ms | 800ms |")
$report.Add("| 1000ms | 100ms | 900ms |")
$report.Add("")
$report.Add("## Largest Visible Stages")
$report.Add("")
$report.Add("| Stage | Count | Avg ms | P95 ms | Max ms |")
$report.Add("|---|---:|---:|---:|---:|")
foreach ($stage in $topStages) {
    $report.Add("| $($stage.stage) | $($stage.count) | $($stage.avgMs) | $($stage.p95Ms) | $($stage.maxMs) |")
}

if ($traces.Count -lt $MinTraceCount) {
    $report.Add("")
    $report.Add("> Aviso: amostra menor que MinTraceCount=$MinTraceCount.")
}

$report | Set-Content -Encoding UTF8 -Path $reportPath
Write-Host "Latency audit written to $reportPath"
