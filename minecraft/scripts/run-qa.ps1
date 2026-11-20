$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & node animation_pipeline/scripts/prepare-qa.cjs
    if($LASTEXITCODE -ne 0){throw 'QA preservation failed'}
    New-Item -ItemType Directory run-qa/saves,run-qa/player_models -Force | Out-Null
    if(-not(Test-Path -LiteralPath run-qa/saves/TOE_QA/level.dat)){
        Copy-Item -LiteralPath run-r31/saves/TOE_R31 -Destination run-qa/saves/TOE_QA -Recurse
    }
    Get-ChildItem run/player_models -Filter '*.cpmmodel' | Copy-Item -Destination run-qa/player_models -Force
    $text=[IO.File]::ReadAllText((Join-Path $root 'run-r31/options.txt'))
    $text=$text -replace '(?m)^enableVsync:.*','enableVsync:false' -replace '(?m)^maxFps:.*','maxFps:260' -replace '(?m)^overrideWidth:.*','overrideWidth:854' -replace '(?m)^overrideHeight:.*','overrideHeight:480'
    [IO.File]::WriteAllText((Join-Path $root 'run-qa/options.txt'),$text,(New-Object Text.UTF8Encoding($false)))
    & "$PSScriptRoot/gradle.ps1" runClient -PtoeQA --console=plain
    if($LASTEXITCODE -ne 0){throw 'QA client failed'}
    & "$PSScriptRoot/gradle.ps1" clean build --console=plain
    $code=$LASTEXITCODE
    if($code -ne 0){throw 'Normal build failed'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar=Get-Item build/libs/towerofentropy-0.0.1.jar
    $zip=[IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try{$names=@($zip.Entries | ForEach-Object FullName)}finally{$zip.Dispose()}
    $excluded=(@($names | Where-Object {$_ -match 'renderlab/|QaBenchmark|R31CpmPreparation|\.(glb|blend|fbx|cpmmodel)$'}).Count -eq 0)
    if(-not $excluded){throw 'Experimental classes/assets leaked into normal JAR'}
    $audit=@{bytes=$jar.Length;entries=$names.Count;sha256=(Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash;experimentalExcluded=$excluded;build='BUILD SUCCESSFUL'}
    [IO.File]::WriteAllText((Join-Path $root '.logs/qa-jar-audit.json'),($audit | ConvertTo-Json),(New-Object Text.UTF8Encoding($false)))
    & node animation_pipeline/scripts/validate-actions.cjs
    if($LASTEXITCODE -ne 0){throw 'Godot preservation failed'}
    & node animation_pipeline/scripts/collect-qa.cjs
    if($LASTEXITCODE -ne 0){throw 'QA collection failed'}
    & node animation_pipeline/scripts/report-qa.cjs
    if($LASTEXITCODE -ne 0){throw 'QA report failed'}
    & "$PSScriptRoot/package-qa.ps1"
} finally {Pop-Location}
exit $code
