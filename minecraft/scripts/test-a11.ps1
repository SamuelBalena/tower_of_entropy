param([switch]$Restart)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskArguments=@('runClient','-PtoeA11Test','--offline','--console=plain')
if($Restart){$taskArguments+='-PtoeA11Restart'}
Push-Location $taskRoot
try{
    & "$PSScriptRoot/gradle.ps1" @taskArguments
    if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
    $taskSuite=if($Restart){'RESTART'}else{'RUNTIME'}
    $taskReport=Get-Content -Raw "animation_pipeline/reports/m002_a1_1/$taskSuite.json" | ConvertFrom-Json
    $taskFailures=@($taskReport.checks | Where-Object {!$_.pass})
    if($taskFailures.Count -gt 0){$taskFailures | Format-Table -Wrap;exit 1}
    if(!$Restart){
        foreach($taskScript in @('build-a11-viewer.cjs','audit-a11-runtime.cjs','audit-a11-transitions.cjs','verify-a11.cjs')){
            & node "animation_pipeline/scripts/$taskScript"
            if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
        }
    }
    Write-Output "PASS: $taskSuite $(@($taskReport.checks).Count) checks"
}finally{Pop-Location}
