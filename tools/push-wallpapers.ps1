#Requires -Version 5.1
<#
  Chep hinh nen tu may tinh sang tivi cho Glassdeck.
  Bo hinh (.jpg .jpeg .png .webp) vao thu muc "wallpapers" canh file nay roi chay Chep-hinh-nen.bat.

    .\push-wallpapers.ps1            # chep them hinh moi, giu hinh cu tren tivi
    .\push-wallpapers.ps1 -Sync      # tivi chi con dung nhung hinh dang co trong thu muc wallpapers

  IP tivi: doc tu config.json canh file nay ({ "Ip": "192.168.1.50" }); chua co thi hoi roi luu lai.
#>
param([switch]$Sync)
$ErrorActionPreference = 'Stop'

$Here   = $PSScriptRoot
$Local  = Join-Path $Here 'wallpapers'
$Remote = '/sdcard/Android/data/io.github.phutrung08.glassdeck/files/wallpapers'
$Cfg    = Join-Path $Here 'config.json'

# ---- Tim adb: (a) Android SDK, (b) thu muc platform-tools canh thu muc tools, (c) PATH ----
function Find-Adb {
    $a = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if (Test-Path $a) { return $a }
    $b = Join-Path (Split-Path $Here) 'platform-tools\adb.exe'
    if (Test-Path $b) { return $b }
    $c = Get-Command adb -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    return $null
}
$Adb = Find-Adb
if (-not $Adb) { throw 'Khong tim thay adb. Cai Android SDK Platform-Tools hoac them adb vao PATH.' }

# ---- IP tivi ----
$ip = $null
if (Test-Path $Cfg) {
    try { $ip = (Get-Content $Cfg -Raw | ConvertFrom-Json).Ip } catch { $ip = $null }
}
if (-not $ip) {
    $ip = (Read-Host 'Nhap dia chi IP cua tivi (vi du 192.168.1.50)').Trim()
    if ($ip -notmatch '^[0-9A-Za-z.:-]+$') { throw 'Dia chi IP khong hop le.' }
    (@{ Ip = $ip } | ConvertTo-Json) | Set-Content -Path $Cfg -Encoding UTF8
    Write-Host "Da luu IP vao $Cfg"
}
if ($ip -notmatch '^[0-9A-Za-z.:-]+$') { throw "Dia chi IP trong config.json khong hop le: $ip" }
$serial = '{0}:5555' -f $ip

if (-not (Test-Path $Local)) { New-Item -ItemType Directory $Local | Out-Null }
$files = @(Get-ChildItem $Local -File | Where-Object { $_.Extension -match '^\.(jpg|jpeg|png|webp)$' })
if ($files.Count -eq 0) { Write-Host "Thu muc $Local chua co hinh nao."; exit 0 }

& $Adb connect $serial | Out-Host
& $Adb -s $serial shell "mkdir -p '$Remote'" | Out-Null

if ($Sync) {
    $keep = $files | ForEach-Object { $_.Name }
    $onTv = @(& $Adb -s $serial shell "ls '$Remote' 2>/dev/null" | ForEach-Object { "$_".Trim() } | Where-Object { $_ })
    foreach ($n in $onTv) {
        if ($keep -contains $n) { continue }
        # Chi xoa ten an toan: tranh ten la chen lenh vao shell tren tivi.
        if ($n -notmatch '^[A-Za-z0-9._ -]+$') {
            Write-Warning "Bo qua ten file la, khong xoa: $n"
            continue
        }
        & $Adb -s $serial shell "rm -- '$Remote/$n'" | Out-Null
        Write-Host "  Da xoa tren tivi: $n"
    }
}

foreach ($f in $files) {
    & $Adb -s $serial push $f.FullName "$Remote/$($f.Name)" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Khong chep duoc $($f.Name)" }
    Write-Host ("  Da chep: {0} ({1:N0} KB)" -f $f.Name, ($f.Length / 1KB))
}
Write-Host "Xong. Tren tivi: Cai dat > Hinh nen de chon hinh." -ForegroundColor Green
