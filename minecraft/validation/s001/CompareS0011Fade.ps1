param([Parameter(Mandatory=$true)][string]$Profile)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$shots = Join-Path (Resolve-Path -LiteralPath $Profile).Path 'screenshots'
$images = @()
try {
    foreach ($scene in 'camera_tunnel','camera_tunnel_clear','camera_tunnel_opaque') {
        $images += [Drawing.Bitmap]::new((Join-Path $shots "s001-$scene.png"))
    }
    $fade, $clear, $opaque = $images
    if ($images.Where({$_.Width -ne $fade.Width -or $_.Height -ne $fade.Height}).Count) {
        throw 'The three framebuffer sizes must match.'
    }
    $signal = 0.0
    $energy = 0.0
    $changes = 0
    $pixels = 0
    for ($y = [int]($fade.Height * .323); $y -lt [int]($fade.Height * .606); $y++) {
        for ($x = [int]($fade.Width * .381); $x -lt [int]($fade.Width * .622); $x++) {
            $f = $fade.GetPixel($x,$y)
            $c = $clear.GetPixel($x,$y)
            $o = $opaque.GetPixel($x,$y)
            if ($f.ToArgb() -ne $c.ToArgb()) { $changes++ }
            foreach ($channel in 'R','G','B') {
                $delta = [int]$f.$channel - [int]$c.$channel
                $reference = [int]$o.$channel - [int]$c.$channel
                $signal += $delta * $reference
                $energy += $reference * $reference
            }
            $pixels++
        }
    }
    if ($energy -le 0) { throw 'The opaque and zero-alpha controls match.' }
    $projected = $signal / $energy
    [pscustomobject]@{Profile=$Profile; Pixels=$pixels; Changed=$changes; ProjectedOpacity=$projected}
    # Post-processing, animation and texture filtering make the color projection approximate.
    if ($changes -lt $pixels / 3 -or $projected -lt .04 -or $projected -gt .24) {
        throw 'The faded frame does not show the expected partial silhouette.'
    }
} finally {
    foreach ($image in $images) { $image.Dispose() }
}
