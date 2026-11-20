$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$jdk=Split-Path (Split-Path (Get-Command javac.exe).Source -Parent) -Parent
$out=Join-Path $root '.logs/m0021-unit'
New-Item -ItemType Directory -Path $out -Force | Out-Null
& "$jdk/bin/javac.exe" -d $out (Join-Path $root 'src/main/java/com/towerofentropy/combat/RollRules.java') (Join-Path $root 'scripts/tests/RollRulesTest.java')
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& "$jdk/bin/java.exe" -cp $out RollRulesTest
exit $LASTEXITCODE
