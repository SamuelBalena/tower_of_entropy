param(
    [Parameter(Mandatory)][string]$Template,
    [string]$RunRoot = '.logs/s00282-matrix',
    [ValidateRange(1,8)][int]$Repetitions = 3,
    [ValidateRange(48,1200)][int]$Warmup = 150,
    [ValidateRange(60,1200)][int]$Measure = 120,
    [switch]$ProfileLighting
)
$ErrorActionPreference = 'Stop'
$templatePath = (Resolve-Path -LiteralPath $Template).Path
$runPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($RunRoot)
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$baseline = '60c02215beccbac2690bf272fe83141be62f0ee9'
$pack = 'Tower of Entropy - Fantasy Foundation.zip'
$bench = Join-Path $PSScriptRoot 'benchmark-atmosphere.ps1'
$export = Join-Path $PSScriptRoot 'export-atmosphere-results.ps1'
$baselineOptions = @{
    BaselineSources = "$runPath/reference/src/main"
    ShaderPack = "$runPath/reference/shaderpacks/$pack"
}

Push-Location $root
try {
    if (Test-Path -LiteralPath $runPath) { throw "Benchmark output already exists: $runPath" }
    New-Item -ItemType Directory -Path $runPath | Out-Null
    $reference = "$runPath/reference.zip"
    git archive --format=zip --output=$reference $baseline -- src/main "shaderpacks/$pack"
    if ($LASTEXITCODE) { throw "Could not extract published reference $baseline" }
    Expand-Archive -LiteralPath $reference -DestinationPath "$runPath/reference"
    $common = @{
        Template = $templatePath
        Variant = 'Equilibrado'
        Warmup = $Warmup
        Measure = $Measure
        ProfileLighting = $ProfileLighting
    }
    $trials = 'DYNAMIC_LIGHT=equipped_corridor,DYNAMIC_LIGHT=equipped_wall'
    $sizes = @('854x480','1920x1080')

    function Run-Trial([string]$Version, [string]$Label, [string[]]$Size, [string]$Matrix) {
        $directory = Join-Path $runPath "$Version-$Label"
        $referenceOptions = if ($Version -eq 's00281') { $baselineOptions } else { @{} }
        & $bench @common @referenceOptions -Size $Size -EffectMatrix $Matrix -RunRoot $directory
        & $export -RunRoot $directory -Output "$directory/results.csv"
    }

    for ($i = 1; $i -le $Repetitions; $i++) {
        $order = if ($i % 2) { 's00281','s00282' } else { 's00282','s00281' }
        foreach ($version in $order) { Run-Trial $version "720-$i" '1280x720' $trials }
    }
    $wide = ('partition','column','lantern_main','back','dual','torch_main','torch_off','soul',
        'env_cave','env_forest','env_village' |
        ForEach-Object { "DYNAMIC_LIGHT=equipped_$_" }) -join ','
    Run-Trial 's00281' 'wide-720' '1280x720' $wide
    Run-Trial 's00282' 'wide-720' '1280x720' $wide

    $bounds = "$trials,DYNAMIC_LIGHT=equipped_dual"
    Run-Trial 's00282' 'framebuffer-bounds' $sizes $bounds
    Run-Trial 's00281' 'framebuffer-bounds' $sizes $bounds

    $results = foreach ($run in Get-ChildItem -LiteralPath $runPath -Directory -Filter 's0028*') {
        Import-Csv -LiteralPath "$($run.FullName)/results.csv" |
            Select-Object @{ Name = 'run'; Expression = { $run.Name } }, *
    }
    $results | Export-Csv -LiteralPath "$runPath/results.csv" -NoTypeInformation -Encoding UTF8
} finally {
    Pop-Location
}
