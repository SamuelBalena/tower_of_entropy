$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$version = Invoke-RestMethod 'https://api.modrinth.com/v2/version/xz6IfCg1'
if ('forge' -notin $version.loaders -or '1.20.1' -notin $version.game_versions) {
    throw 'Unexpected CPM compatibility metadata.'
}
$file = $version.files | Where-Object primary | Select-Object -First 1
if ($file.filename -ne 'CustomPlayerModels-1.20-0.6.26a.jar' -or ([Uri]$file.url).Host -ne 'cdn.modrinth.com') {
    throw 'Unexpected CPM artifact.'
}
$expected = '66431b5961acdd3909ed655a75b54d0bf31d7668b4d02c37e69da3ca7cc5be5c5c32515527ce5dd8d98871c790f5f66d9d9767d45ed546a1c8170d6440faa5e6'
$target = Join-Path $root "dependencies/cpm/$($file.filename)"
Invoke-WebRequest $file.url -OutFile $target
if ((Get-FileHash $target -Algorithm SHA512).Hash.ToLowerInvariant() -ne $expected) {
    throw 'CPM SHA-512 mismatch. Do not use this file.'
}
Write-Output "CPM verified: $target"