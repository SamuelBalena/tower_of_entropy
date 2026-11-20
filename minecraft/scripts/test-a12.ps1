param([ValidateSet('Visual','Restart','Benchmark','Baseline','BaselineBenchmark','Iframes','Equivalence')][string]$Suite='Visual')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskArguments=@('runClient','-PtoeA12Test','--offline','--console=plain')
switch($Suite){
 'Restart' {$taskArguments+='-PtoeA12Restart'}
 'Baseline' {$taskArguments+='-PtoeA12Legacy'}
 'Benchmark' {$taskArguments+='-PtoeA12Bench'}
 'BaselineBenchmark' {$taskArguments+=@('-PtoeA12Bench','-PtoeA12Legacy')}
 'Iframes' {$taskArguments+='-PtoeA12Iframes'}
 'Equivalence' {$taskArguments+='-PtoeA12Equivalence'}
}
Push-Location $taskRoot
try { & "$PSScriptRoot/gradle.ps1" @taskArguments; if($LASTEXITCODE -ne 0){exit $LASTEXITCODE} } finally { Pop-Location }
