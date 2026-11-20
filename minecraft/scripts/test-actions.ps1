$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$jdk=Split-Path (Split-Path (Get-Command javac.exe).Source -Parent) -Parent
$out=Join-Path $root '.logs/m001_3-unit'
New-Item -ItemType Directory -Path $out -Force | Out-Null
& "$jdk/bin/javac.exe" -d $out (Join-Path $root 'src/main/java/com/towerofentropy/animation/VisualActionWindow.java') (Join-Path $root 'animation_pipeline/scripts/tests/VisualActionWindowTest.java')
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
& "$jdk/bin/java.exe" -cp $out VisualActionWindowTest
exit $LASTEXITCODE
