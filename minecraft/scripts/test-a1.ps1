param([switch]$Restart,[switch]$Visual,[switch]$Cold)
$taskArguments=@('runClient','-PtoeA1Test','--offline','--console=plain')
if($Restart){$taskArguments+='-PtoeA1Restart'}
if($Visual){$taskArguments+='-PtoeA1Visual'}
if($Cold){$taskArguments+='-PtoeA1Cold'}
& (Join-Path $PSScriptRoot 'gradle.ps1') @taskArguments
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
$taskSuite=if($Restart){'RESTART'}elseif($Cold){'COLD'}elseif($Visual){'VISUAL'}else{'RUNTIME'}
$taskResultPath=Join-Path (Split-Path $PSScriptRoot -Parent) "animation_pipeline/reports/m002_a1/$taskSuite.json"
$taskResult=Get-Content -LiteralPath $taskResultPath -Raw | ConvertFrom-Json
$taskFailures=@($taskResult.checks | Where-Object {!$_.pass})
if($taskFailures.Count -gt 0){$taskFailures | Format-Table -Wrap;exit 1}
Write-Output "PASS: $taskSuite $(@($taskResult.checks).Count) checks"
exit 0
