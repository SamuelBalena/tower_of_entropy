param([string]$Destination)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$source = (Resolve-Path (Join-Path $PSScriptRoot '../../shaderpacks/Tower of Entropy - Fantasy Foundation')).Path
if (!$Destination) { $Destination = "$source.zip" }
$Destination = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Destination)
$temp = "$Destination.$PID.$([Guid]::NewGuid().ToString('N')).tmp"
$archive = [IO.Compression.ZipFile]::Open($temp, [IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem -LiteralPath $source -File -Recurse | Sort-Object FullName | ForEach-Object {
        $name = $_.FullName.Substring($source.Length + 1).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $name) | Out-Null
    }
} finally { $archive.Dispose() }
Move-Item -LiteralPath $temp -Destination $Destination -Force
