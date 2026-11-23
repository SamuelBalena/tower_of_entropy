param([Parameter(Mandatory)][string]$Screenshots)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = (Resolve-Path -LiteralPath $Screenshots).Path

function Compare-Region($active, $disabled, [int]$top, [int]$bottom) {
    $total = 0.0
    $maximum = 0
    $count = 0
    for ($y = $top; $y -lt $bottom; $y += 3) {
        for ($x = 450; $x -lt 830; $x += 3) {
            $a = $active.GetPixel($x, $y)
            $b = $disabled.GetPixel($x, $y)
            $delta = $a.R - $b.R
            $total += $delta
            $rgbMax = [Math]::Max([Math]::Abs($delta), [Math]::Max([Math]::Abs($a.G - $b.G), [Math]::Abs($a.B - $b.B)))
            $maximum = [Math]::Max($maximum, $rgbMax)
            $count++
        }
    }
    [pscustomobject]@{ Mean = $total / $count; Maximum = $maximum; Samples = $count }
}

foreach ($scene in 'equipped_wall','equipped_glass','equipped_inventory') {
    $active = [Drawing.Bitmap]::new((Join-Path $root "s001-$scene-DYNAMIC_LIGHT-on.png"))
    $disabled = [Drawing.Bitmap]::new((Join-Path $root "s001-$scene-DYNAMIC_LIGHT-off.png"))
    try {
        if ($active.Width -ne 1280 -or $disabled.Width -ne 1280 -or $active.Height -ne 720 -or $disabled.Height -ne 720) {
            throw 'Expected the 1280x720 fixed-camera QA capture pair.'
        }
        $far = Compare-Region $active $disabled 340 410
        $front = Compare-Region $active $disabled 530 680
        if ($scene -eq 'equipped_wall' -and ($far.Maximum -ne 0 -or $front.Mean -lt 4)) {
            throw 'Opaque wall must block the sampled far region while the near surface remains lit.'
        }
        if ($scene -eq 'equipped_glass' -and ($far.Mean -lt .2 -or $front.Mean -lt 4)) {
            throw 'The glass fixture must transmit visible equipped light.'
        }
        if ($scene -eq 'equipped_inventory' -and ($far.Maximum -ne 0 -or $front.Maximum -ne 0)) {
            throw 'The torch in a non-equipped inventory slot changed the image.'
        }
        [pscustomobject]@{ Scene = $scene; FarMean = $far.Mean; FarMaxAbs = $far.Maximum; FrontMean = $front.Mean }
    } finally {
        $active.Dispose()
        $disabled.Dispose()
    }
}
