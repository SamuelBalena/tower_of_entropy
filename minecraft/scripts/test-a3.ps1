param([ValidateSet('Runtime','Extra','Final','Iframes','Circuit','Restart')][string]$Suite='Runtime')
$ErrorActionPreference='Stop'
$taskArguments=@('runClient','-PtoeA3Test','--offline','--console=plain')
switch($Suite){'Extra'{$taskArguments+='-PtoeA3Extra'}'Final'{$taskArguments+='-PtoeA3FinalCheck'}'Iframes'{$taskArguments+='-PtoeA3Iframes'}'Circuit'{$taskArguments+='-PtoeA3Circuit'}'Restart'{$taskArguments+='-PtoeA3Restart'}}
& "$PSScriptRoot/gradle.ps1" @taskArguments
exit $LASTEXITCODE
