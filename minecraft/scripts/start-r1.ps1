param(
    [ValidateSet('baseline','fixed','fullmotion','conflict')][string]$Mode='fixed',
    [switch]$Interactive
)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & node animation_pipeline/scripts/prepare-r1.cjs
    if($LASTEXITCODE -ne 0){throw 'R1 preservation/preparation failed'}
    $useFullMotion=$Mode -in @('fullmotion','conflict')
    if($useFullMotion){
        $jar=Join-Path $root '.downloads/m002_r1/full-motion-forge-1.0.0.jar'
        New-Item -ItemType Directory (Split-Path $jar -Parent) -Force | Out-Null
        if(-not (Test-Path -LiteralPath $jar)){
            Invoke-WebRequest -Uri 'https://edge.forgecdn.net/files/9425/804/full-motion-forge-1.0.0.jar' -OutFile $jar -UseBasicParsing
        }
        if((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne '251147913624538579837C02573B59E28414D82D54BBC2172F0B3CFE683FE358'){
            throw 'Unexpected Full Motion binary; official file 9425804 required'
        }
    }
    New-Item -ItemType Directory run-r1/saves,run-r1/player_models,run-r1/config -Force | Out-Null
    if(-not (Test-Path -LiteralPath 'run-r1/saves/TOE_R1/level.dat')){
        if(-not (Test-Path -LiteralPath 'run/saves/New World/level.dat')){throw 'Disposable M001/M002 New World save missing; no other world will be modified'}
        Copy-Item -LiteralPath 'run/saves/New World' -Destination run-r1/saves/TOE_R1 -Recurse
    }
    Get-ChildItem run/player_models -Filter '*.cpmmodel' | Copy-Item -Destination run-r1/player_models -Force
    if(-not (Test-Path -LiteralPath run-r1/options.txt)){
        $text=Get-Content run/options.txt -Raw
        $text=$text -replace '(?m)^renderDistance:\d+','renderDistance:5' -replace '(?m)^simulationDistance:\d+','simulationDistance:5' -replace '(?m)^maxFps:\d+','maxFps:60'
        [IO.File]::WriteAllText((Join-Path $root 'run-r1/options.txt'),$text,(New-Object Text.UTF8Encoding($false)))
    }
    '{"enabled":false,"otherPlayers":false,"disabled":["fullmotion:slide","fullmotion:dive"]}' | Set-Content run-r1/config/fullmotion.json -Encoding ASCII
    $labArgs=@('runClient','-PtoeR1',"-PtoeR1Mode=$Mode",'--console=plain')
    if($useFullMotion){$labArgs+='-PtoeR1FullMotion'}
    if($Interactive){$labArgs+='-PtoeR1Auto=false'}
    & "$PSScriptRoot/gradle.ps1" @labArgs
    $code=$LASTEXITCODE
} finally {Pop-Location}
exit $code
