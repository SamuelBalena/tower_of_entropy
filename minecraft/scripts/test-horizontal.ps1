$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskJdk=Split-Path (Split-Path (Get-Command javac.exe).Source -Parent) -Parent
$taskOut=Join-Path $taskRoot '.logs/a2-unit'
New-Item -ItemType Directory -Path $taskOut -Force | Out-Null
& "$taskJdk/bin/javac.exe" -d $taskOut (Join-Path $taskRoot 'src/main/java/com/towerofentropy/animation/HorizontalMotion.java') (Join-Path $taskRoot 'scripts/tests/HorizontalMotionTest.java')
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& "$taskJdk/bin/java.exe" -cp $taskOut HorizontalMotionTest
exit $LASTEXITCODE
