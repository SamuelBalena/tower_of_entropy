$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$jdk=Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source -Parent) -Parent
$out=Join-Path $root '.logs/m001_2-unit'
New-Item -ItemType Directory -Path $out -Force | Out-Null
& "$jdk/bin/javac.exe" -d $out (Join-Path $root 'src/main/java/com/towerofentropy/animation/LocomotionStateMachine.java') (Join-Path $root 'animation_pipeline/scripts/tests/LocomotionStateMachineTest.java')
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& "$jdk/bin/java.exe" -cp $out LocomotionStateMachineTest
exit $LASTEXITCODE
