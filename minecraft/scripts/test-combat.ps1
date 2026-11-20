$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$jdk=Split-Path (Split-Path (Get-Command javac.exe).Source -Parent) -Parent
$out=Join-Path $root '.logs/m002_0-unit'
New-Item -ItemType Directory -Path $out -Force | Out-Null
& "$jdk/bin/javac.exe" -d $out (Join-Path $root 'src/main/java/com/towerofentropy/combat/AttackGeometry.java') (Join-Path $root 'src/main/java/com/towerofentropy/combat/AttackState.java') (Join-Path $root 'src/main/java/com/towerofentropy/combat/CombatMove.java') (Join-Path $root 'scripts/tests/CombatRulesTest.java')
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& "$jdk/bin/java.exe" -cp $out CombatRulesTest
exit $LASTEXITCODE
