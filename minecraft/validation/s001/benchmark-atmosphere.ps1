param(
    [Parameter(Mandatory)][string]$Template,
    [string]$Baseline,
    [string]$ShaderPack,
    [switch]$LabPbr,
    [switch]$Slim,
    [ValidateSet('A','B','C','D','E','F','Off','High','FXAA','Before','ExpansionOff','Leve','Equilibrado','Alto')][string[]]$Variant = @('A','B','C','D','E','F'),
    [ValidateSet('854x480','1280x720','1920x1080')][string[]]$Size = @('854x480','1280x720','1920x1080'),
    [string]$Scenes = 'aa_forest,fog_mountain,aurora_snowy_plains,fog_forest_dawn,waterside_village,material_water,material_lava,material_ores,material_armor,aa_stars',
    [ValidateRange(2,32)][int]$Distance = 8,
    [ValidateRange(48,1200)][int]$Warmup = 48,
    [ValidateSet('SUN_RAYS','SCREEN_AO','CONTACT_SHADOWS','WET_SURFACES','PUDDLE_REFLECTIONS','VEGETATION_WIND','SELECTIVE_BLOOM','DIMENSION_FOG','DYNAMIC_LIGHT','ADVANCED_SHADOWS','WINTER_WEATHER','EQUIPMENT_PBR','SOLID_REFLECTIONS')]
    [string]$DisableEffect,
    [string]$EffectMatrix,
    [string]$RunRoot = '.logs/s002-matrix'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$templatePath = (Resolve-Path -LiteralPath $Template).Path
$baselinePath = if ($Baseline) { (Resolve-Path -LiteralPath $Baseline).Path }
$runPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($RunRoot)
$pack = 'Tower of Entropy - Fantasy Foundation.zip'
$archivePath = if ($ShaderPack) { (Resolve-Path -LiteralPath $ShaderPack).Path } else { Join-Path $root "shaderpacks/$pack" }
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
if (('A' -in $Variant -or 'Before' -in $Variant) -and !$baselinePath) { throw 'Specify the baseline ZIP with -Baseline.' }
if ($DisableEffect -and ($Variant.Count -ne 1 -or $Variant[0] -ne 'Equilibrado')) {
    throw 'Use -Variant Equilibrado for individual effect measurements.'
}
$properties = "shaderpacks/$($pack -replace '\.zip$','')/shaders/shaders.properties"
$profiles = @{}
foreach ($profile in @('Leve','Equilibrado','Alto')) {
    $line = Select-String -LiteralPath (Join-Path $root $properties) -Pattern "^profile.$($profile.ToUpperInvariant())="
    $profiles[$profile] = ($line.Line.Split('=',2)[1] -split ' ') -replace ':','='
}
$added = @('SUN_RAYS','SCREEN_AO','CONTACT_SHADOWS','WET_SURFACES','PUDDLE_REFLECTIONS','VEGETATION_WIND',
    'SELECTIVE_BLOOM','DIMENSION_FOG','DYNAMIC_LIGHT','ADVANCED_SHADOWS','WINTER_WEATHER','EQUIPMENT_PBR','SOLID_REFLECTIONS')
if ($EffectMatrix) {
    if ($DisableEffect -or $Variant.Count -ne 1 -or $Variant[0] -ne 'Equilibrado') {
        throw 'Use -Variant Equilibrado without -DisableEffect for the effect matrix.'
    }
    foreach ($trial in $EffectMatrix.Split(',')) {
        $option,$scene = $trial.Split('=',2)
        if ($option -notin $added -or !$scene) { throw "Invalid effect trial $trial" }
    }
}
$settings.Before = $settings.F
$settings.ExpansionOff = @($profiles.Equilibrado | Where-Object { $_.Split('=')[0] -notin $added })
$settings.ExpansionOff += $added | ForEach-Object { "$_=0" }
foreach ($profile in $profiles.Keys) { $settings[$profile] = $profiles[$profile] }
Push-Location $root
try {
    foreach ($resolution in $Size) {
        $width,$height = $resolution.Split('x')
        foreach ($mode in $Variant) {
            $suffix = if ($DisableEffect) { "$mode-$DisableEffect" } elseif ($EffectMatrix) { "$mode-Effects" } else { $mode }
            $instance = Join-Path $runPath "$resolution-$suffix"
            if (Test-Path -LiteralPath $instance) { throw "QA instance already exists: $instance" }
            New-Item -ItemType Directory -Path "$instance/config","$instance/shaderpacks" -Force | Out-Null
            Copy-Item -LiteralPath "$templatePath/saves" -Destination $instance -Recurse
            $cameraConfig = "$templatePath/config/towerofentropy-camera-client.toml"
            if (Test-Path -LiteralPath $cameraConfig) {
                Copy-Item -LiteralPath $cameraConfig -Destination "$instance/config"
            }
            $options = Get-Content -LiteralPath "$templatePath/options.txt"
            $options = $options -replace '^renderDistance:.*$', "renderDistance:$Distance" -replace '^enableVsync:.*$', 'enableVsync:false'
            if ($LabPbr) {
                $resource = 'Tower of Entropy - LabPBR Demo.zip'
                New-Item -ItemType Directory -Path "$instance/resourcepacks" | Out-Null
                Copy-Item -LiteralPath "resourcepacks/$resource" -Destination "$instance/resourcepacks"
                $packLine = $options | Where-Object { $_ -like 'resourcePacks:*' } | Select-Object -First 1
                $packs = if ($packLine) { ConvertFrom-Json ($packLine -replace '^resourcePacks:', '') }
                $packIds = @($packs | Where-Object { $_ -and $_ -ne "file/$resource" }) + @("file/$resource")
                $options = @($options | Where-Object { $_ -notlike 'resourcePacks:*' }) +
                    @('resourcePacks:' + (ConvertTo-Json -InputObject $packIds -Compress))
            }
            [IO.File]::WriteAllLines("$instance/options.txt", [string[]]$options, [Text.UTF8Encoding]::new($false))
            @("shaderPack=$pack",'enableShaders=true') | Set-Content -LiteralPath "$instance/config/oculus.properties" -Encoding ASCII
            $archive = if ($mode -in @('A','Before')) { $baselinePath } else { $archivePath }
            Copy-Item -LiteralPath $archive -Destination "$instance/shaderpacks/$pack"
            (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash |
                Set-Content -LiteralPath "$instance/pack-sha256.txt" -Encoding ASCII
            $options = @('SHADOW_RESOLUTION=1024','SHADOW_FILTER=1','WATER_DETAIL=1','LAVA_DETAIL=1','MATERIAL_QUALITY=2',
                'AA_QUALITY=2','AURORA_QUALITY=2','FOG_QUALITY=2','AURORA_FREQUENCY=1.00','PUDDLE_REFLECTIONS=1') + $settings[$mode]
            $packOptions = [ordered]@{}
            foreach ($entry in $options) {
                $key,$value = $entry.Split('=',2)
                $packOptions[$key] = $value
            }
            if ($DisableEffect) { $packOptions[$DisableEffect] = '0' }
            $packOptions.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" } |
                Set-Content -LiteralPath "$instance/shaderpacks/$pack.txt" -Encoding ASCII
            if ($EffectMatrix) {
                Copy-Item -LiteralPath "$instance/shaderpacks/$pack.txt" -Destination "$instance/effect-reference.properties"
            }

            $gpu = if ($null -ne (Get-Command nvidia-smi -ErrorAction SilentlyContinue)) {
                Start-Process nvidia-smi -WindowStyle Hidden -PassThru -ArgumentList @(
                    '--query-gpu=timestamp,utilization.gpu,memory.used','--format=csv,noheader,nounits','-l','1'
                ) -RedirectStandardOutput "$instance-gpu.csv" -RedirectStandardError "$instance-gpu-errors.log"
            }
            try {
                $gradleArgs = @('--offline','-I','validation/s001/forge.init.gradle','runClient','-PtoeS001Loader',
                    "-PtoeS001Bench=$instance","-PtoeS0016Width=$width","-PtoeS0016Height=$height",
                    '-PtoeAtmosphereQa',"-PtoeAtmosphereScenes=$Scenes","-PtoeAtmosphereWarmup=$Warmup")
                if ($EffectMatrix) { $gradleArgs += "-PtoeAtmosphereEffects=$EffectMatrix" }
                if ($Slim) { $gradleArgs += '-PtoeGlobalSlim' }
                $ErrorActionPreference = 'Continue'
                & .\gradlew @gradleArgs *> "$instance.log"
                $ErrorActionPreference = 'Stop'
                if ($LASTEXITCODE -ne 0) { throw "Forge failed in $instance.log" }
            } finally {
                if ($gpu -and !$gpu.HasExited) { Stop-Process -Id $gpu.Id }
            }
            $results = @(Select-String -LiteralPath "$instance.log" -Pattern 'TOE_S001_RESULT ')
            $expected = if ($EffectMatrix) { 2 * $EffectMatrix.Split(',').Count } else { $Scenes.Split(',').Count }
            if ($results.Count -ne $expected -or
                (Select-String -LiteralPath "$instance.log" -Pattern 'TOE_S001_SETUP shader=(off|no-loader)|Failed to (create shader rendering pipeline|load the shaderpack)' -Quiet)) {
                throw "Incomplete shader evidence in $instance.log"
            }
        }
    }
} finally {
    Pop-Location
}
