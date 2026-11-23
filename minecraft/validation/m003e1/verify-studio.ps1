param(
    [Parameter(Mandatory)][string]$Template,
    [string]$RunRoot = '.logs/m003e1-studio',
    [ValidateSet('classic','slim')][string[]]$Variant = @('classic','slim')
)
$ErrorActionPreference = 'Stop'
$templatePath = (Resolve-Path -LiteralPath $Template).Path
$runPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($RunRoot)
$root = (Resolve-Path "$PSScriptRoot/../..").Path
$pack = 'Tower of Entropy - Fantasy Foundation.zip'

Push-Location $root
try {
    if (!(Test-Path -LiteralPath "$templatePath/saves/Global Survival one/level.dat")) {
        throw 'Expected the Global Survival one QA fixture in the template.'
    }
    foreach ($skin in $Variant) {
        $instance = Join-Path $runPath $skin
        if (Test-Path -LiteralPath $instance) { throw "QA instance already exists: $instance" }
        New-Item -ItemType Directory -Path "$instance/config","$instance/shaderpacks" | Out-Null
        Copy-Item -LiteralPath "$templatePath/saves","$templatePath/options.txt" -Destination $instance -Recurse
        $args = @('--offline','-I','validation/s001/forge.init.gradle','-I','validation/m003e1/forge.init.gradle','runClient',
            "-PtoeStudioQa=$instance")
        if ($skin -eq 'classic') {
            $args += '-PtoeS001Loader'
            Copy-Item -LiteralPath "shaderpacks/$pack" -Destination "$instance/shaderpacks"
            @("shaderPack=$pack",'enableShaders=true') | Set-Content "$instance/config/oculus.properties" -Encoding ASCII
        } else {
            $args += '-PtoeStudioSlim','-PtoeNoOculus'
        }
        $ErrorActionPreference = 'Continue'
        & .\gradlew.bat @args *> "$instance.log"
        $exit = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        $expected = if ($skin -eq 'classic') { 'slim=false oculus=true' } else { 'slim=true oculus=false' }
        if ($exit -or !(Select-String -LiteralPath "$instance.log" -Pattern "TOE_STUDIO PASS $expected" -Quiet)) {
            throw "Studio QA failed. See $instance.log"
        }
        Select-String -LiteralPath "$instance.log" -Pattern 'TOE_STUDIO (PASS|FRAMEBUFFER)' |
            ForEach-Object { $_.Line }
    }
} finally {
    Pop-Location
}
