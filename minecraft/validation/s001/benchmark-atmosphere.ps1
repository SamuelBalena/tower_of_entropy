param(
    [Parameter(Mandatory)][string]$Template,
    [string]$Baseline,
    [ValidateSet('A','B','C','D','E','F','Off','High','FXAA')][string[]]$Variant = @('A','B','C','D','E','F'),
    [ValidateSet('854x480','1280x720','1920x1080')][string[]]$Size = @('854x480','1280x720','1920x1080'),
    [string]$Scenes = 'aa_forest,fog_mountain,aurora_snowy_plains,fog_forest_dawn,waterside_village,material_water,material_lava,material_ores,material_armor,aa_stars',
    [ValidateRange(2,32)][int]$Distance = 8,
    [string]$RunRoot = '.logs/s002-matrix'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$templatePath = (Resolve-Path -LiteralPath $Template).Path
$baselinePath = if ($Baseline) { (Resolve-Path -LiteralPath $Baseline).Path }
$runPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($RunRoot)
$pack = 'Tower of Entropy - Fantasy Foundation.zip'
$settings = @{
    A = @('ANTIALIASING=1')
    B = @('ANTIALIASING=2','AURORA=0','FOG_MODE=0')
    C = @('ANTIALIASING=1','AURORA=1','FOG_MODE=0')
    D = @('ANTIALIASING=1','AURORA=0','FOG_MODE=4')
    E = @('ANTIALIASING=1','AURORA=1','FOG_MODE=4')
    F = @('ANTIALIASING=2','AURORA=1','FOG_MODE=4')
    Off = @('ANTIALIASING=0','AURORA=0','FOG_MODE=0')
    High = @('ANTIALIASING=2','AURORA=1','FOG_MODE=3')
    FXAA = @('ANTIALIASING=1','AURORA=0','FOG_MODE=0')
}
if ('A' -in $Variant -and !$baselinePath) { throw 'Specify the original S001.7.1 ZIP with -Baseline.' }
Push-Location $root
try {
    foreach ($resolution in $Size) {
        $width,$height = $resolution.Split('x')
        foreach ($mode in $Variant) {
            $instance = Join-Path $runPath "$resolution-$mode"
            if (Test-Path -LiteralPath $instance) { throw "QA instance already exists: $instance" }
            New-Item -ItemType Directory -Path "$instance/config","$instance/shaderpacks" -Force | Out-Null
            Copy-Item -LiteralPath "$templatePath/saves" -Destination $instance -Recurse
            $cameraConfig = "$templatePath/config/towerofentropy-camera-client.toml"
            if (Test-Path -LiteralPath $cameraConfig) {
                Copy-Item -LiteralPath $cameraConfig -Destination "$instance/config"
            }
            $options = Get-Content -LiteralPath "$templatePath/options.txt"
            $options = $options -replace '^renderDistance:.*$', "renderDistance:$Distance" -replace '^enableVsync:.*$', 'enableVsync:false'
            [IO.File]::WriteAllLines("$instance/options.txt", [string[]]$options, [Text.UTF8Encoding]::new($false))
            @("shaderPack=$pack",'enableShaders=true') | Set-Content -LiteralPath "$instance/config/oculus.properties" -Encoding ASCII
            $archive = if ($mode -eq 'A') { $baselinePath } else { "shaderpacks/$pack" }
            Copy-Item -LiteralPath $archive -Destination "$instance/shaderpacks/$pack"
            (@('SHADOW_RESOLUTION=1024','SHADOW_FILTER=1','WATER_DETAIL=1','LAVA_DETAIL=1','MATERIAL_QUALITY=2',
                'AA_QUALITY=2','AURORA_QUALITY=2','FOG_QUALITY=2','AURORA_FREQUENCY=1.00') + $settings[$mode]) |
                Set-Content -LiteralPath "$instance/shaderpacks/$pack.txt" -Encoding ASCII

            $gpu = if ($null -ne (Get-Command nvidia-smi -ErrorAction SilentlyContinue)) {
                Start-Process nvidia-smi -WindowStyle Hidden -PassThru -ArgumentList @(
                    '--query-gpu=timestamp,utilization.gpu,memory.used','--format=csv,noheader,nounits','-l','1'
                ) -RedirectStandardOutput "$instance-gpu.csv" -RedirectStandardError "$instance-gpu-errors.log"
            }
            try {
                $gradleArgs = @('--offline','-I','validation/s001/forge.init.gradle','runClient','-PtoeS001Loader',
                    "-PtoeS001Bench=$instance","-PtoeS0016Width=$width","-PtoeS0016Height=$height",
                    '-PtoeAtmosphereQa',"-PtoeAtmosphereScenes=$Scenes")
                $ErrorActionPreference = 'Continue'
                & .\gradlew @gradleArgs *> "$instance.log"
                $ErrorActionPreference = 'Stop'
                if ($LASTEXITCODE -ne 0) { throw "Forge failed in $instance.log" }
            } finally {
                if ($gpu -and !$gpu.HasExited) { Stop-Process -Id $gpu.Id }
            }
            $results = @(Select-String -LiteralPath "$instance.log" -Pattern 'TOE_S001_RESULT ')
            if ($results.Count -ne $Scenes.Split(',').Count -or
                (Select-String -LiteralPath "$instance.log" -Pattern 'TOE_S001_SETUP shader=off|Failed to create shader rendering pipeline' -Quiet)) {
                throw "Incomplete shader evidence in $instance.log"
            }
        }
    }
} finally {
    Pop-Location
}
