$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
if(!(Test-Path -LiteralPath (Join-Path $taskRoot 'run-a3/saves/TOE_A3/level.dat'))){throw 'Circuito TOE_A3 ausente. Execute test-a3.ps1 -Suite Circuit para preparar a cópia isolada.'}
& "$PSScriptRoot/gradle.ps1" runClient -PtoeA3Play --offline --console=plain
exit $LASTEXITCODE
