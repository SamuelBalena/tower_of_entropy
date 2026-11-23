param([Parameter(Mandatory)][string]$Screenshots)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = (Resolve-Path -LiteralPath $Screenshots).Path
$images = @()
try {
    foreach ($tick in 3,24,51) {
        $image = [Drawing.Bitmap]::new((Join-Path $root "s00282-wall-toggle-$tick.png"))
        $images += $image
        if ($image.Width -ne 1280 -or $image.Height -ne 720) {
            throw 'Expected the 1280x720 fixed-camera wall sequence.'
        }
    }
    $openGain = 0
    $closedMax = 0
    $samples = 0
    for ($y = 340; $y -lt 410; $y += 3) {
        for ($x = 450; $x -lt 830; $x += 3) {
            $closed = $images[0].GetPixel($x, $y)
            $open = $images[1].GetPixel($x, $y)
            $restored = $images[2].GetPixel($x, $y)
            $openGain += $open.R - $closed.R
            $delta = [Math]::Max([Math]::Abs($restored.R - $closed.R),
                [Math]::Max([Math]::Abs($restored.G - $closed.G), [Math]::Abs($restored.B - $closed.B)))
            $closedMax = [Math]::Max($closedMax, $delta)
            $samples++
        }
    }
    $mean = $openGain / $samples
    if ($mean -lt .2 -or $closedMax -ne 0) {
        throw "Expected light to reach the far floor only while the wall is absent (mean=$mean, restored max=$closedMax)."
    }
    [pscustomobject]@{ Samples = $samples; MeanRedGainOpen = $mean; MaxRgbDeltaRestored = $closedMax }
} finally {
    foreach ($image in $images) { $image.Dispose() }
}
