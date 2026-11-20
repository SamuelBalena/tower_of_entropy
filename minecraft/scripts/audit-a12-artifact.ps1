$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskJar=Join-Path $taskRoot 'build/libs/towerofentropy-0.0.1.jar'
$taskZip=[IO.Compression.ZipFile]::OpenRead($taskJar)
try {
 $taskNames=@($taskZip.Entries | ForEach-Object {$_.FullName})
 $taskUnexpected=@($taskNames | Where-Object {$_ -match 'A12|A11|A1Runtime|RollRuntime|R4Runtime|R31|SkinnedSteveRenderer|experiments/|screenshots/'})
 if($taskUnexpected.Count){throw "QA/experimental entries in normal JAR: $taskUnexpected"}
 foreach($taskRequired in @('com/towerofentropy/client/R3VisualPose.class','com/towerofentropy/animation/VisualTuning.class','assets/towerofentropy/visual-support.json')){if($taskNames -notcontains $taskRequired){throw "Missing $taskRequired"}}
 $taskAsset='assets/towerofentropy/models/gltf/steve_r3.glb'
 $taskEntry=$taskZip.GetEntry($taskAsset);if(!$taskEntry){throw 'Missing approved GLB'}
 $taskSha=[Security.Cryptography.SHA256]::Create();$taskStream=$taskEntry.Open()
 try{$taskEmbedded=([BitConverter]::ToString($taskSha.ComputeHash($taskStream))).Replace('-','')}finally{$taskStream.Dispose();$taskSha.Dispose()}
 $taskExpected=(Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $taskRoot "animation_pipeline/converted/m002_r4/resources/$taskAsset")).Hash
 if($taskEmbedded -ne $taskExpected){throw 'Packaged GLB changed'}
 $taskResult=[ordered]@{status='PASS';bytes=(Get-Item -LiteralPath $taskJar).Length;sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $taskJar).Hash;entries=$taskNames.Count;noQaOrR31Classes=$true;approvedGlbSha256=$taskEmbedded;visualSupportIncluded=$true}
 $taskResult | ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $taskRoot 'animation_pipeline/reports/m002_a1_2/ARTIFACT.json')
 $taskResult
}finally{$taskZip.Dispose()}
