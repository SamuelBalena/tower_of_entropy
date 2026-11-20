$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$folder=Join-Path $root 'docs/M002_R3_QA_EVIDENCE'
if(-not(Test-Path -LiteralPath (Join-Path $folder 'REPORT.md'))){throw 'QA report must be generated first'}
$hashes=Get-ChildItem -LiteralPath $folder -Recurse -File | Where-Object Name -ne 'CHECKSUMS.sha256' | Sort-Object FullName | ForEach-Object {
    $name=$_.FullName.Substring($folder.Length+1).Replace('\','/')
    '{0}  {1}' -f (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(),$name
}
[IO.File]::WriteAllText((Join-Path $folder 'CHECKSUMS.sha256'),(($hashes -join "`n")+"`n"),(New-Object Text.UTF8Encoding($false)))
Compress-Archive -Path (Join-Path $folder '*') -DestinationPath (Join-Path $root 'docs/M002_R3_QA_EVIDENCE.zip') -Force
