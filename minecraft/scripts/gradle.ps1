$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$javaCommand = Get-Command javac.exe -ErrorAction Stop
$jdk = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
$ErrorActionPreference = 'Continue'
$javaVersion = & "$jdk/bin/java.exe" -version 2>&1 | Out-String
$ErrorActionPreference = 'Stop'
if ($javaVersion -notmatch 'version "17\.' -or $javaVersion -notmatch '64-Bit') {
    throw 'JDK 17 64-bit must be available on PATH (java and javac).'
}
$env:JAVA_HOME = $jdk
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user-home'
Push-Location $projectRoot
try {
    # Windows PowerShell 5 treats native stderr as errors; use the native exit code.
    $ErrorActionPreference = 'Continue'
    & .\gradlew.bat @args
    $result = $LASTEXITCODE
} finally { Pop-Location }
exit $result