#Requires -Version 5.1
<#
  Doi cac file PNG do generate.py tao ra thanh JPEG chat luong cao (nhe hon nhieu cho tivi).
    .\convert.ps1 -In <thu muc PNG> -Out <thu muc JPEG> [-Quality 92]
#>
param(
    [Parameter(Mandatory)] [string]$In,
    [Parameter(Mandatory)] [string]$Out,
    [int]$Quality = 92
)
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force $Out | Out-Null
$codec = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
$params = New-Object Drawing.Imaging.EncoderParameters 1
$params.Param[0] = New-Object Drawing.Imaging.EncoderParameter ([Drawing.Imaging.Encoder]::Quality), ([long]$Quality)
foreach ($f in Get-ChildItem $In -Filter *.png) {
    $img = [Drawing.Image]::FromFile($f.FullName)
    $dst = Join-Path $Out ($f.BaseName + '.jpg')
    $img.Save($dst, $codec, $params)
    $img.Dispose()
    Write-Host ("  {0} ({1:N0} KB)" -f (Split-Path $dst -Leaf), ((Get-Item $dst).Length / 1KB))
}
