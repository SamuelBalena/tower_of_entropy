param([switch]$Interactive,[ValidateSet('r3','r2','cpm')][string]$Mode='r3')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & node animation_pipeline/scripts/prepare-r3.cjs
    if($LASTEXITCODE -ne 0){throw 'R3 preparation/preservation failed'}
    $model='animation_pipeline/converted/m002_r3/steve-r3-refined.glb'
    $generator='animation_pipeline/scripts/blender-r3-refine.py'
    if(-not(Test-Path -LiteralPath $model) -or (Get-Item $generator).LastWriteTime -gt (Get-Item $model).LastWriteTime){
        $blender=Get-Command blender -ErrorAction SilentlyContinue
        $exe=if($blender){$blender.Source}else{'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe'}
        if(-not(Test-Path -LiteralPath $exe)){throw 'Blender not found; install official Blender or add it to PATH'}
        & $exe --background --python $generator -- $root
        if($LASTEXITCODE -ne 0){throw 'R3 Blender refinement failed'}
    }
    & node animation_pipeline/scripts/prepare-r3.cjs
    & node animation_pipeline/scripts/validate-r3.cjs
    if($LASTEXITCODE -ne 0){throw 'R3 validation failed'}
    $jar='.downloads/m002_r2/polymesh-1.0.0-forge.jar'
    if(-not(Test-Path -LiteralPath $jar)){throw 'Verified R2 Polymesh runtime is required; see R2 setup'}
    if((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne '653B222EA124A3913D1094A9C32EBF3124421C10C0A11C3A12F59BC3C723C051'){throw 'Unexpected Polymesh runtime binary'}
    New-Item -ItemType Directory run-r3/saves,run-r3/player_models -Force | Out-Null
    foreach($name in @('TOE_R3','New World')){
        if(-not(Test-Path -LiteralPath "run-r3/saves/$name/level.dat")){
            if(-not(Test-Path -LiteralPath 'run/saves/New World/level.dat')){throw 'Disposable New World fixture missing'}
            Copy-Item -LiteralPath 'run/saves/New World' -Destination "run-r3/saves/$name" -Recurse
        }
    }
    Get-ChildItem run/player_models -Filter '*.cpmmodel' | Copy-Item -Destination run-r3/player_models -Force
    if(-not(Test-Path -LiteralPath run-r3/options.txt)){
        $text=Get-Content run/options.txt -Raw
        $text=$text -replace '(?m)^renderDistance:\d+','renderDistance:5' -replace '(?m)^simulationDistance:\d+','simulationDistance:5' -replace '(?m)^maxFps:\d+','maxFps:60'
        [IO.File]::WriteAllText((Join-Path $root 'run-r3/options.txt'),$text,(New-Object Text.UTF8Encoding($false)))
    }
    $launch=@('runClient','-PtoeR3',"-PtoeR3Mode=$Mode",'--console=plain')
    if($Interactive){$launch+='-PtoeR3Auto=false'}
    & "$PSScriptRoot/gradle.ps1" @launch
    $code=$LASTEXITCODE
} finally {Pop-Location}
exit $code
