$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskJar=Join-Path $taskRoot 'build/libs/towerofentropy-0.0.1.jar'
$taskZip=[IO.Compression.ZipFile]::OpenRead($taskJar)
try {
 $taskNames=@($taskZip.Entries | ForEach-Object {$_.FullName})
 $taskUnexpected=@($taskNames | Where-Object {$_ -match 'GlobalRuntime|A3Runtime|A2Runtime|A2Benchmark|A12|A11|A1Runtime|RollRuntime|R4Runtime|R31|SmokeTest|SkinnedSteveRenderer|experiments/|screenshots/'})
 if($taskUnexpected.Count){throw "QA/experimental entries in normal JAR: $taskUnexpected"}
 $taskHashes=[ordered]@{}
 foreach($taskSpec in @(@('steve_r3','m002_r4'),@('steve_a3','m002_a3'),@('steve_a3_slim','m002_a3_1'))){
  $taskAsset="assets/towerofentropy/models/gltf/$($taskSpec[0]).glb"
  $taskEntry=$taskZip.GetEntry($taskAsset);if(!$taskEntry){throw "Missing $taskAsset"}
  $taskSha=[Security.Cryptography.SHA256]::Create();$taskStream=$taskEntry.Open()
  try{$taskEmbedded=([BitConverter]::ToString($taskSha.ComputeHash($taskStream))).Replace('-','')}finally{$taskStream.Dispose();$taskSha.Dispose()}
  $taskExpected=(Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $taskRoot "animation_pipeline/converted/$($taskSpec[1])/resources/$taskAsset")).Hash
  if($taskEmbedded -ne $taskExpected){throw "Packaged GLB differs: $taskAsset"}
  $taskHashes[$taskSpec[0]]=$taskEmbedded
 }
 foreach($taskRequired in @('META-INF/mods.toml','META-INF/TOE_THIRD_PARTY_ANIMATIONS.txt','com/towerofentropy/client/PlayerRendererR3.class','com/towerofentropy/client/VisualContext.class','com/towerofentropy/client/UpperBodyPose.class','assets/towerofentropy/a3-poses.json','assets/towerofentropy/a3-support.json','assets/towerofentropy/visual-support.json','assets/towerofentropy/compat/toe-steve-ual-actions.cpmmodel','assets/towerofentropy/compat/toe-steve-ual-actions-preview.cpmmodel')){
  if($taskNames -notcontains $taskRequired){throw "Missing $taskRequired"}
 }
 $taskResult=[ordered]@{status='PASS';bytes=(Get-Item -LiteralPath $taskJar).Length;sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $taskJar).Hash;entries=$taskNames.Count;noQaOrR31Classes=$true;models=$taskHashes;runtimeResourcesIncluded=$true;scope='Artifact audit; not a production-profile runtime launch'}
 $taskResult | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $taskRoot 'animation_pipeline/reports/m002_a3_1/ARTIFACT.json')
 $taskResult
}finally{$taskZip.Dispose()}
