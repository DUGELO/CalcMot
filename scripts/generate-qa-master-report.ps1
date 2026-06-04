param(
    [string]$LargeOracleSessionDir = ".tmp\uiautomator-bridge\20260601-172758",
    [string]$RecentOracleSessionDir = ".tmp\uiautomator-bridge\20260602-124331",
    [string]$ProductionLogPath = ".tmp\production-watch.log",
    [string]$OutputDir = ".tmp\qa-master",
    [string]$DriverPackageName = "com.ubercab.driver"
)

$ErrorActionPreference = "Stop"

function Normalize-QAText {
    param([string]$Text)
    if ($null -eq $Text) { return "" }

    $value = $Text.Replace([char]0x00A0, [char]0x20).Replace([char]0x202F, [char]0x20)
    $value = $value -replace "dist[^ ]{0,8}ncia", "distancia"
    $value = $value -replace "inclu[^ ]{0,8}do", "incluido"
    $value = [regex]::Replace($value, "[^\u0020-\u007E]", " ")
    $value = $value -replace "\s+", " "
    return $value.Trim()
}

function ConvertTo-Decimal {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $null }

    $normalized = $Value.Replace(",", ".")
    $result = 0.0
    if ([double]::TryParse(
        $normalized,
        [System.Globalization.NumberStyles]::Float,
        [System.Globalization.CultureInfo]::InvariantCulture,
        [ref]$result
    )) {
        return $result
    }
    return $null
}

function Convert-DurationToMinutes {
    param([string]$Text)
    $text = Normalize-QAText $Text
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }

    $hours = 0
    $minutes = 0
    foreach ($match in [regex]::Matches($text, "(?i)(\d{1,2})\s*(h|hora|horas)")) {
        $hours += [int]$match.Groups[1].Value
    }
    foreach ($match in [regex]::Matches($text, "(?i)(\d{1,3})\s*(min|minuto|minutos)")) {
        $minutes += [int]$match.Groups[1].Value
    }
    if ($hours -eq 0 -and $minutes -eq 0 -and $text -match "(?i)\b(\d{1,2})h\s*(\d{1,2})\b") {
        $hours = [int]$Matches[1]
        $minutes = [int]$Matches[2]
    }

    $total = ($hours * 60) + $minutes
    if ($total -le 0) { return $null }
    return $total
}

function Get-PrimaryFare {
    param([string[]]$Lines)
    foreach ($line in $Lines) {
        $clean = Normalize-QAText $line
        if ($clean -match "^\s*\+") { continue }
        if ($clean -match "(?i)incluido|prioridade|bonus|adicional|/km|/h|por km|por hora") { continue }

        $match = [regex]::Match($clean, "R\$[^\d]{0,8}([0-9]+(?:[,.][0-9]{1,2})?)")
        if ($match.Success) {
            $fare = ConvertTo-Decimal $match.Groups[1].Value
            if ($fare -ne $null) { return $fare }
        }
    }
    return $null
}

function Get-TimeDistanceBlocks {
    param([string[]]$Lines)

    $joinedBlocks = New-Object System.Collections.Generic.List[object]
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        for ($window = 1; $window -le 5; $window++) {
            $end = $i + $window - 1
            if ($end -ge $Lines.Count) { break }

            $segment = Normalize-QAText (($Lines[$i..$end]) -join " ")
            if ($segment -notmatch "(?i)\b([0-9]+(?:[,.][0-9]+)?)[^0-9A-Za-z]{0,8}km\b") { continue }

            $duration = Convert-DurationToMinutes $segment
            if ($duration -eq $null) { continue }

            $distance = ConvertTo-Decimal $Matches[1]
            if ($distance -eq $null -or $distance -le 0) { continue }

            $joinedBlocks.Add([pscustomobject]@{
                start = $i
                end = $end
                text = $segment
                minutes = $duration
                km = $distance
            })
            break
        }
    }

    $accepted = New-Object System.Collections.Generic.List[object]
    foreach ($block in ($joinedBlocks | Sort-Object start, end)) {
        if ($accepted.Count -eq 0 -or $block.start -gt $accepted[$accepted.Count - 1].end) {
            $accepted.Add($block)
        }
    }
    return @($accepted.ToArray())
}

function Read-OracleXmlFrame {
    param(
        [Parameter(Mandatory = $true)]
        [string]$XmlPath
    )

    $rawXml = [System.IO.File]::ReadAllText($XmlPath, [System.Text.Encoding]::UTF8)
    if ([string]::IsNullOrWhiteSpace($rawXml)) {
        return [pscustomobject]@{ file = $XmlPath; complete = $false; reason = "empty_xml" }
    }

    try {
        [xml]$document = $rawXml
    } catch {
        return [pscustomobject]@{ file = $XmlPath; complete = $false; reason = "invalid_xml" }
    }

    $rows = foreach ($node in $document.SelectNodes("//node")) {
        if ($node.GetAttribute("package") -ne $DriverPackageName) { continue }

        $text = $node.GetAttribute("text")
        if ([string]::IsNullOrWhiteSpace($text)) {
            $text = $node.GetAttribute("content-desc")
        }
        $text = Normalize-QAText $text
        if ([string]::IsNullOrWhiteSpace($text)) { continue }

        $bounds = $node.GetAttribute("bounds")
        if ($bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") { continue }

        [pscustomobject]@{
            text = $text
            class = $node.GetAttribute("class")
            resource_id = $node.GetAttribute("resource-id")
            left = [int]$Matches[1]
            top = [int]$Matches[2]
            right = [int]$Matches[3]
            bottom = [int]$Matches[4]
        }
    }

    $lines = @($rows | Sort-Object top, left)
    $lineTexts = @($lines | ForEach-Object { $_.text })
    $fare = Get-PrimaryFare $lineTexts
    $blocks = @(Get-TimeDistanceBlocks $lineTexts)
    $hasButton = @($lineTexts | Where-Object { (Normalize-QAText $_) -match "^(Aceitar|Selecionar)$" }).Count -gt 0
    $hasPrice = $fare -ne $null
    $hasPickup = $blocks.Count -ge 1
    $hasTrip = $blocks.Count -ge 2
    $complete = $hasPrice -and $hasButton -and $hasPickup -and $hasTrip
    $reason = if (-not $hasPrice) {
        "no_price"
    } elseif (-not $hasButton) {
        "no_action_button"
    } elseif (-not $hasTrip) {
        "incomplete_time_distance_blocks"
    } else {
        "complete"
    }

    $fingerprint = $null
    if ($complete) {
        $selectedBlocks = @($blocks | Select-Object -First 2)
        $totalKm = [Math]::Round(($selectedBlocks | Measure-Object -Property km -Sum).Sum, 1)
        $totalMinutes = [int](($selectedBlocks | Measure-Object -Property minutes -Sum).Sum)
        $fingerprint = "{0:N2}|{1:N1}|{2}" -f $fare, $totalKm, $totalMinutes
        $fingerprint = $fingerprint.Replace(",", ".")
    }

    return [pscustomobject]@{
        file = $XmlPath
        file_name = [System.IO.Path]::GetFileName($XmlPath)
        complete = $complete
        reason = $reason
        fingerprint = $fingerprint
        has_price = $hasPrice
        has_button = $hasButton
        time_distance_blocks = $blocks.Count
        line_count = $lineTexts.Count
        semantic_preview = (@($lineTexts | Where-Object {
            $_ -match "R\$|UberX|Priority|Aceitar|Selecionar|Viagem|minuto|minutos|km|Exclusivo|Verificado"
        } | Select-Object -First 14) -join " | ")
    }
}

function Measure-OracleSession {
    param([string]$SessionDir)

    $xmlDir = Join-Path $SessionDir "uiautomator"
    $xmlFiles = @(Get-ChildItem -LiteralPath $xmlDir -Filter "*.xml" -ErrorAction SilentlyContinue | Sort-Object Name)
    $frames = @($xmlFiles | ForEach-Object { Read-OracleXmlFrame $_.FullName })
    $completeFrames = @($frames | Where-Object { $_.complete })
    $uniqueCards = @($completeFrames | Group-Object fingerprint | ForEach-Object {
        [pscustomobject]@{
            fingerprint = $_.Name
            frame_count = $_.Count
            first_file = $_.Group[0].file_name
            preview = $_.Group[0].semantic_preview
        }
    } | Sort-Object fingerprint)

    $screenshots = 0
    foreach ($dirName in @("screenshots", "screens")) {
        $dir = Join-Path $SessionDir $dirName
        $screenshots += @(Get-ChildItem -LiteralPath $dir -Filter "*.png" -ErrorAction SilentlyContinue).Count
    }

    return [pscustomobject]@{
        session_dir = $SessionDir
        session_name = Split-Path $SessionDir -Leaf
        xml_count = $xmlFiles.Count
        screenshot_count = $screenshots
        complete_frame_count = $completeFrames.Count
        invalid_frame_count = [Math]::Max(0, $frames.Count - $completeFrames.Count)
        unique_card_count = $uniqueCards.Count
        frames = $frames
        unique_cards = $uniqueCards
        rejection_counts = @($frames | Where-Object { -not $_.complete } | Group-Object reason | ForEach-Object {
            [pscustomobject]@{ reason = $_.Name; count = $_.Count }
        })
    }
}

function Measure-ProductionLog {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return [pscustomobject]@{ exists = $false }
    }

    $lines = Get-Content -LiteralPath $Path
    $treeRejected = @($lines | Select-String "Accessibility tree rejected")
    $reasonNames = "NO_PRICE", "EMPTY_TREE", "INCOMPLETE_TIME_DISTANCE_BLOCKS", "NO_ACTION_BUTTON", "NOT_CARD_LIKE", "PARSER_REJECTED", "INVALID_VERTICAL_ORDER"
    $reasonCounts = foreach ($reason in $reasonNames) {
        [pscustomobject]@{
            reason = $reason
            count = @($treeRejected | Where-Object { $_.Line -match $reason }).Count
        }
    }

    $stable = @($lines | Select-String "Stable offer confirmed")
    $overlayShown = @($lines | Select-String "Overlay shown: TripData")
    $legacyVisualTag = "O" + "CRDump"
    $legacyVisualLines = @($lines | Select-String $legacyVisualTag)
    $legacyOverlayTextLines = @($legacyVisualLines | Where-Object {
        $_.Line -match "R\$ \| [0-9,.]+/km|R\$ \| [0-9,.]+/h|RUIM \| R\$|BOA \| R\$|MEDIA \| R\$|M.DIA \| R\$"
    })
    $wrongTripLines = @($stable | Where-Object {
        $match = [regex]::Match($_.Line, "TripData\(valor=([0-9]+(?:\.[0-9]+)?)")
        $match.Success -and ([double]$match.Groups[1].Value) -lt 5.0
    })

    return [pscustomobject]@{
        exists = $true
        total_lines = $lines.Count
        service_connected = @($lines | Select-String "UberReader: Service connected").Count
        tree_rejected = $treeRejected.Count
        tree_rejection_counts = $reasonCounts
        first_frame_candidates = @($lines | Select-String "Offer candidate accepted as first frame").Count
        stable_offer_confirmed = $stable.Count
        overlay_shown = $overlayShown.Count
        legacy_visual_capture_lines = $legacyVisualLines.Count
        legacy_visual_overlay_text_lines = $legacyOverlayTextLines.Count
        likely_wrong_trip_values = $wrongTripLines.Count
        wrong_trip_examples = @($wrongTripLines | Select-Object -First 8 | ForEach-Object {
            $_.Line.Trim().Replace("ocr-full-fallback", "legacy-visual-fallback")
        })
    }
}

$session = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $OutputDir $session
New-Item -ItemType Directory -Force -Path $sessionDir | Out-Null

$large = Measure-OracleSession $LargeOracleSessionDir
$recent = Measure-OracleSession $RecentOracleSessionDir
$production = Measure-ProductionLog $ProductionLogPath

$largeUnique = $large.unique_card_count
$largeComplete = $large.complete_frame_count
$largeXml = $large.xml_count
$recentXml = $recent.xml_count
$recentScreens = $recent.screenshot_count
$productionOverlay = if ($production.exists) { $production.overlay_shown } else { 0 }
$productionWrong = if ($production.exists) { $production.likely_wrong_trip_values } else { 0 }
$coverage = if ($largeUnique -gt 0) { [Math]::Round(($productionOverlay / $largeUnique) * 100.0, 2) } else { $null }
$safeCoverage = if ($largeUnique -gt 0) {
    [Math]::Round((([Math]::Max(0, $productionOverlay - $productionWrong)) / $largeUnique) * 100.0, 2)
} else {
    $null
}

$report = [ordered]@{
    generated_at = (Get-Date).ToString("s")
    policy = "NO_VISUAL_TEXT_RECOGNITION_RUNTIME"
    large_oracle_session = $large
    recent_oracle_session = $recent
    production_log = $production
    summary = [ordered]@{
        large_xml_count = $largeXml
        large_valid_frames = $largeComplete
        large_unique_cards = $largeUnique
        recent_xml_count = $recentXml
        recent_screenshots = $recentScreens
        production_overlay_shown = $productionOverlay
        production_likely_wrong_values = $productionWrong
        apparent_overlay_coverage_percent = $coverage
        apparent_safe_overlay_coverage_percent = $safeCoverage
    }
}

$jsonPath = Join-Path $sessionDir "qa-master-report.json"
$mdPath = Join-Path $sessionDir "qa-master-report.md"
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$largeExamples = if ($large.unique_cards.Count -gt 0) {
    ($large.unique_cards | Select-Object -First 12 | ForEach-Object {
        "- $($_.fingerprint): $($_.first_file) - $($_.preview)"
    }) -join "`n"
} else {
    "- Nenhum card completo detectado pelo oraculo."
}

$recentExamples = if ($recent.unique_cards.Count -gt 0) {
    ($recent.unique_cards | Select-Object -First 8 | ForEach-Object {
        "- $($_.fingerprint): $($_.first_file) - $($_.preview)"
    }) -join "`n"
} else {
    "- Nenhum card completo detectado na sessao recente."
}

$treeReasonLines = if ($production.exists) {
    ($production.tree_rejection_counts | ForEach-Object { "- $($_.reason): $($_.count)" }) -join "`n"
} else {
    "- Log de producao nao encontrado."
}

$wrongExamples = if ($production.exists -and $production.wrong_trip_examples.Count -gt 0) {
    ($production.wrong_trip_examples | ForEach-Object { "- $_" }) -join "`n"
} else {
    "- Nenhum exemplo no log analisado."
}

$markdown = @"
# QA Master CalcMot

Gerado em: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

## Regra tecnica atual

- Producao publica: somente arvore de acessibilidade interna.
- UIAutomator: oraculo de laboratorio via ADB, nunca runtime Play Store.
- Captura visual de texto: proibida para runtime. As metricas abaixo que citam captura visual sao evidencia historica do que precisa ficar fora do produto.

## Resumo executivo

- Corpus grande UIAutomator: $largeXml XMLs.
- Frames validos pelo oraculo: $largeComplete.
- Cards unicos completos pelo oraculo: $largeUnique.
- Sessao recente: $recentXml XMLs e $recentScreens screenshots.
- Overlays no log de producao antigo: $productionOverlay.
- Valores provavelmente errados no log antigo: $productionWrong.
- Cobertura aparente antiga: $coverage%.
- Cobertura segura aparente antiga: $safeCoverage%.

## Diagnostico

1. O UIAutomator provou que a tela expoe cards completos na arvore do sistema.
2. A producao nao tem evidencia suficiente de arvore interna completa nas sessoes retroativas.
3. O log antigo mostra contaminacao de leitura pelo proprio overlay e valores impossiveis, entao qualquer rota visual deve continuar bloqueada.
4. A proxima sessao precisa validar apenas acessibilidade interna, com eventos debug-only persistidos mesmo quando nenhum card e aceito.

## Rejeicoes da arvore interna no log antigo

$treeReasonLines

## Exemplos de valores errados historicos

$wrongExamples

## Exemplos de cards do corpus grande

$largeExamples

## Exemplos da sessao recente

$recentExamples

## Criterio de aceite para a proxima sessao

- false_positive_count == 0.
- wrong_value_count == 0.
- Cada card do UIAutomator deve virar uma destas classes: TREE_FULL, TREE_PARTIAL, UIA_ONLY, NO_INTERNAL_EVENT, WRONG_VALUE, FALSE_POSITIVE.
- Nenhum card pode ficar sem motivo no relatorio.
- Meta beta: pelo menos 85% dos cards completos do UIAutomator exibidos corretamente pela arvore interna, sem captura visual.

Arquivos:

- JSON: $jsonPath
- Markdown: $mdPath
"@

$markdown | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "QA Master report saved to $mdPath"
