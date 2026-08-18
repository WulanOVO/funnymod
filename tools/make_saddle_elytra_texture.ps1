# 生成鞍鞘(saddle_elytra)物品纹理:
# 将原版马鞍与鞘翅物品纹理叠放,马鞍在前(顶层),鞘翅在后(底层)。
# 用法: powershell -ExecutionPolicy Bypass -File tools/make_saddle_elytra_texture.ps1

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$saddlePath = Join-Path $root "sources\textures\item\saddle.png"
$elytraPath = Join-Path $root "sources\textures\item\elytra.png"
$outDir = Join-Path $root "src\main\resources\assets\funnymod\textures\item"
$outPath = Join-Path $outDir "saddle_elytra.png"

foreach ($p in @($saddlePath, $elytraPath)) {
    if (-not (Test-Path $p)) { throw "找不到源纹理: $p" }
}

$saddle = [System.Drawing.Bitmap]::FromFile($saddlePath)
$elytra = [System.Drawing.Bitmap]::FromFile($elytraPath)
try {
    if ($saddle.Width -ne $elytra.Width -or $saddle.Height -ne $elytra.Height) {
        throw "纹理尺寸不一致: saddle=$($saddle.Width)x$($saddle.Height), elytra=$($elytra.Width)x$($elytra.Height)"
    }

    # 逐像素 alpha 合成:saddle 在前(上层),elytra 在后(底层)
    $size = $saddle.Width
    $result = New-Object System.Drawing.Bitmap($size, $size)
    for ($x = 0; $x -lt $size; $x++) {
        for ($y = 0; $y -lt $size; $y++) {
            $back = $elytra.GetPixel($x, $y)
            $front = $saddle.GetPixel($x, $y)
            if ($front.A -eq 255) {
                $result.SetPixel($x, $y, $front)
            } elseif ($front.A -eq 0) {
                $result.SetPixel($x, $y, $back)
            } else {
                # 标准 alpha 混合
                $a = [double]$front.A / 255.0
                $r = [int]($front.R * $a + $back.R * (1 - $a))
                $g = [int]($front.G * $a + $back.G * (1 - $a))
                $b = [int]($front.B * $a + $back.B * (1 - $a))
                $outA = [int](($front.A / 255.0) + ($back.A / 255.0) * (1 - $a) * 255)
                if ($outA -gt 255) { $outA = 255 }
                $result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($outA, $r, $g, $b))
            }
        }
    }

    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $result.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "已生成: $outPath ($size x $size)"
}
finally {
    $saddle.Dispose()
    $elytra.Dispose()
    if ($result) { $result.Dispose() }
}
