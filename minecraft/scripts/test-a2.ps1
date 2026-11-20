param([ValidateSet('Visual','Previous','Extra','PreviousExtra','Benchmark','PreviousBenchmark','Restart','Iframes')][string]$Suite='Visual')
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskArguments=@('runClient','-PtoeA2Test','--offline','--console=plain')
switch($Suite){
 'Previous' {$taskArguments+='-PtoeA2Previous'}
 'Extra' {$taskArguments+='-PtoeA2Extra'}
 'PreviousExtra' {$taskArguments+=@('-PtoeA2Extra','-PtoeA2Previous')}
 'Benchmark' {$taskArguments+='-PtoeA2Bench'}
 'PreviousBenchmark' {$taskArguments+=@('-PtoeA2Bench','-PtoeA2Previous')}
 'Restart' {$taskArguments+='-PtoeA2Restart'}
 'Iframes' {$taskArguments+='-PtoeA2Iframes'}
}
Push-Location $taskRoot
try{& "$PSScriptRoot/gradle.ps1" @taskArguments;exit $LASTEXITCODE}finally{Pop-Location}
