param([switch]$Interactive,[ValidateSet('skinned','baseline')][string]$Mode='skinned')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & node animation_pipeline/scripts/prepare-r2.cjs
    if($LASTEXITCODE -ne 0){throw 'R2 preparation/preservation failed'}
    $jar=Join-Path $root '.downloads/m002_r2/polymesh-1.0.0-forge.jar'
    New-Item -ItemType Directory (Split-Path $jar -Parent) -Force | Out-Null
    if(-not(Test-Path -LiteralPath $jar)){
        Invoke-WebRequest 'https://github.com/phe6/polymesh/releases/download/v1.0.0/polymesh-1.0.0-forge.jar' -OutFile $jar -UseBasicParsing
    }
    if((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne '653B222EA124A3913D1094A9C32EBF3124421C10C0A11C3A12F59BC3C723C051'){throw 'Unexpected Polymesh runtime binary'}
    New-Item -ItemType Directory run-r2/saves,run-r2/player_models -Force | Out-Null
    if(-not(Test-Path -LiteralPath 'run-r2/saves/TOE_R2/level.dat')){
        if(-not(Test-Path -LiteralPath 'run/saves/New World/level.dat')){throw 'Disposable New World fixture missing; no other save will be opened'}
        Copy-Item -LiteralPath 'run/saves/New World' -Destination run-r2/saves/TOE_R2 -Recurse
    }
    Get-ChildItem run/player_models -Filter '*.cpmmodel' | Copy-Item -Destination run-r2/player_models -Force
    if(-not(Test-Path -LiteralPath run-r2/options.txt)){
        $text=Get-Content run/options.txt -Raw
        $text=$text -replace '(?m)^renderDistance:\d+','renderDistance:5' -replace '(?m)^simulationDistance:\d+','simulationDistance:5' -replace '(?m)^maxFps:\d+','maxFps:60'
        [IO.File]::WriteAllText((Join-Path $root 'run-r2/options.txt'),$text,(New-Object Text.UTF8Encoding($false)))
    }
    $r2Args=@('runClient','-PtoeR2',"-PtoeR2Mode=$Mode",'--console=plain')
    if($Interactive){$r2Args+='-PtoeR2Auto=false'}
    & "$PSScriptRoot/gradle.ps1" @r2Args
    $code=$LASTEXITCODE
} finally {Pop-Location}
exit $code
