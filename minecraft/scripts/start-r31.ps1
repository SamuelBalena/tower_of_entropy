param([switch]$Interactive,[ValidateSet('r31','r3','r2','cpm')][string]$Mode='r31')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    if(Test-Path -LiteralPath 'animation_pipeline/reports/M002_R4_PHASE_A_GATE.json'){
        # Promotion intentionally changed the old core hashes. Keep the approved R3.1
        # model intact, validate against the promotion checkpoint, and reuse it.
        if(Test-Path -LiteralPath 'animation_pipeline/reports/m002_a1_2/BASELINE.json'){
            & node animation_pipeline/scripts/verify-a12.cjs
        }elseif(Test-Path -LiteralPath 'animation_pipeline/reports/m002_a1_1/BASELINE.json'){
            & node animation_pipeline/scripts/verify-a11.cjs
        }elseif(Test-Path -LiteralPath 'animation_pipeline/reports/m002_a1/BASELINE.json'){
            # A1 appends clips while keeping the original model/experiments intact.
            & node animation_pipeline/scripts/verify-a1.cjs
        }else{
            & node animation_pipeline/scripts/verify-r4.cjs
        }
        if($LASTEXITCODE -ne 0){throw 'R4/R3.1 preservation failed'}
        $launch=@('runClient','-PtoeR31',"-PtoeR31Mode=$Mode",'--console=plain')
        if($Interactive){$launch+='-PtoeR31Auto=false'}
        & "$PSScriptRoot/gradle.ps1" @launch
        exit $LASTEXITCODE
    }
    & node animation_pipeline/scripts/prepare-r31.cjs
    if($LASTEXITCODE -ne 0){throw 'R3.1 preservation failed'}
    $generator='animation_pipeline/scripts/blender-r31-refine.py'
    $model='animation_pipeline/converted/m002_r3_1/steve-r31-raw.glb'
    if(-not(Test-Path -LiteralPath $model) -or (Get-Item $generator).LastWriteTime -gt (Get-Item $model).LastWriteTime){
        $command=Get-Command blender -ErrorAction SilentlyContinue
        $exe=if($command){$command.Source}else{'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe'}
        if(-not(Test-Path -LiteralPath $exe)){throw 'Official Blender installation required'}
        & $exe --background --python $generator -- $root
        if($LASTEXITCODE -ne 0){throw 'R3.1 Blender bake failed'}
    }
    & node animation_pipeline/scripts/patch-r31-glb.cjs
    if($LASTEXITCODE -ne 0){throw 'R3.1 GLB materialization failed'}
    & node animation_pipeline/scripts/prepare-r31.cjs
    if($LASTEXITCODE -ne 0){throw 'R3.1 resource preparation failed'}
    & node animation_pipeline/scripts/validate-r31.cjs
    if($LASTEXITCODE -ne 0){throw 'R3.1 validation failed'}
    $jar='.downloads/m002_r2/polymesh-1.0.0-forge.jar'
    if(-not(Test-Path -LiteralPath $jar)){throw 'Verified Polymesh runtime from R2 is required'}
    if((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne '653B222EA124A3913D1094A9C32EBF3124421C10C0A11C3A12F59BC3C723C051'){throw 'Unexpected Polymesh runtime binary'}
    New-Item -ItemType Directory run-r31/saves,run-r31/player_models -Force | Out-Null
    foreach($name in @('TOE_R31','New World')){
        if(-not(Test-Path -LiteralPath "run-r31/saves/$name/level.dat")){
            if(-not(Test-Path -LiteralPath 'run/saves/New World/level.dat')){throw 'Disposable New World fixture missing'}
            Copy-Item -LiteralPath 'run/saves/New World' -Destination "run-r31/saves/$name" -Recurse
        }
    }
    Get-ChildItem run/player_models -Filter '*.cpmmodel' | Copy-Item -Destination run-r31/player_models -Force
    if(-not(Test-Path -LiteralPath run-r31/options.txt)){
        $text=Get-Content run/options.txt -Raw
        $text=$text -replace '(?m)^renderDistance:\d+','renderDistance:5' -replace '(?m)^simulationDistance:\d+','simulationDistance:5' -replace '(?m)^maxFps:\d+','maxFps:60'
        [IO.File]::WriteAllText((Join-Path $root 'run-r31/options.txt'),$text,(New-Object Text.UTF8Encoding($false)))
    }
    $launch=@('runClient','-PtoeR31',"-PtoeR31Mode=$Mode",'--console=plain')
    if($Interactive){$launch+='-PtoeR31Auto=false'}
    & "$PSScriptRoot/gradle.ps1" @launch
    $code=$LASTEXITCODE
} finally {Pop-Location}
exit $code
