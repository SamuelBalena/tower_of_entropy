param([Parameter(Mandatory)][string]$RunRoot, [Parameter(Mandatory)][string]$Output)
$ErrorActionPreference = 'Stop'
$culture = [Globalization.CultureInfo]::InvariantCulture
function AtSecond([string]$stamp) {
    [TimeSpan]::ParseExact($stamp, 'hh\:mm\:ss', $culture).TotalSeconds
}

$results = foreach ($file in Get-ChildItem -LiteralPath $RunRoot -Filter '*.log' -File) {
    if ($file.BaseName -notmatch '^(?<size>\d+x\d+)-(?<variant>[A-F]|Off|High|FXAA|Before|ExpansionOff|Leve|Equilibrado(?:-[A-Z_]+|-Effects)?|Alto)$') { continue }
    $requested, $variant = $Matches.size, $Matches.variant
    $lines = Select-String -LiteralPath $file.FullName -Pattern 'TOE_S001_(SETUP|RESULT)|TOE_S0011_TIMING|^BUILD SUCCESSFUL|Creating pipeline for dimension|Reloading pipeline on dimension change'
    $setup = $lines | Where-Object Line -Match 'TOE_S001_SETUP' | Select-Object -First 1
    if (!$setup -or $setup.Line -match 'shader=off' -or $setup.Line -notmatch 'size=(\d+x\d+)') { continue }
    $actual = $Matches[1]
    $hashPath = Join-Path $file.DirectoryName "$($file.BaseName)/pack-sha256.txt"
    $hash = if (Test-Path -LiteralPath $hashPath) { (Get-Content -Raw -LiteralPath $hashPath).Trim() }
    if (!($lines | Where-Object Line -Match '^BUILD SUCCESSFUL')) { continue }
    $gpuPath = Join-Path $file.DirectoryName "$($file.BaseName)-gpu.csv"
    $gpu = if (Test-Path -LiteralPath $gpuPath) {
        foreach ($row in Get-Content -LiteralPath $gpuPath) {
            if ($row -match ' (\d{2}:\d{2}:\d{2})\.\d+,\s*(\d+),\s*(\d+)') {
                [pscustomobject]@{ second = AtSecond $Matches[1]; load = [int]$Matches[2]; memory = [int]$Matches[3] }
            }
        }
    }
    $timing = @{}
    foreach ($line in $lines) {
        if ($line.Line -match 'TOE_S0011_TIMING (\S+) p99=([\d.]+)') {
            $timing[$Matches[1]] = [double]::Parse($Matches[2],$culture)
        }
    }
    $pipelineDimension = $null
    foreach ($line in $lines) {
        if ($line.Line -match "(?:Creating pipeline for dimension|Reloading pipeline on dimension change).*NamespacedId\{namespace='([^']+)', name='([^']+)'\}") {
            $pipelineDimension = "$($Matches[1]):$($Matches[2])"
            continue
        }
        if ($line.Line -notmatch '^\[(\d{2}:\d{2}:\d{2})\].*TOE_S001_RESULT (\S+) n=(\d+) fps=([\d.]+) low=([\d.]+) p95=([\d.]+)') { continue }
        $second = AtSecond $Matches[1]
        $scene = $Matches[2]
        $frames = [int]$Matches[3]
        $fps = [double]::Parse($Matches[4],$culture)
        $low = [double]::Parse($Matches[5],$culture)
        $p95 = [double]::Parse($Matches[6],$culture)
        $dimension = if ($line.Line -match ' dimension=(\S+)') { $Matches[1] } else { $pipelineDimension }
        $window = @($gpu | Where-Object { $age = ($second - $_.second + 86400) % 86400; $age -ge 1 -and $age -le 3 })
        [pscustomobject]@{
            sha256 = $hash
            requested = $requested
            framebuffer = $actual
            variant = $variant
            scene = $scene
            dimension = $dimension
            frames = $frames
            fps = $fps.ToString("F2",$culture)
            low1 = $low.ToString("F2",$culture)
            p95_ms = $p95.ToString("F3",$culture)
            p99_ms = $timing[$scene].ToString("F3",$culture)
            gpu_pct = $(if ($window.Count) { ($window | Measure-Object load -Average).Average.ToString("F1",$culture) })
            vram_mib = $(if ($window.Count) { [int](($window | Measure-Object memory -Maximum).Maximum) })
        }
    }
}
if (!$results) { throw "No completed runs in $RunRoot" }
$results | Export-Csv -LiteralPath $Output -NoTypeInformation -Encoding UTF8
