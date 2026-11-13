#Requires -Version 5.1
<#
  Tidyo PC - dọn dẹp Android TV qua ADB từ máy tính.
  Không cài gì lên tivi. Mọi lệnh chạy qua adb shell.

  Cách dùng:
    .\Tidyo-PC.ps1                       # mở menu
    .\Tidyo-PC.ps1 -Ip 192.168.1.50      # chỉ định IP tivi (được lưu lại cho lần sau)
    .\Tidyo-PC.ps1 -Action Clean -Yes    # dọn nhanh không hỏi (dùng cho lịch tự động)
#>
param(
    [string]$Ip,
    [ValidateSet('Menu', 'Info', 'Clean', 'Apps')]
    [string]$Action = 'Menu',
    [switch]$Yes
)

[Console]::OutputEncoding = [Text.Encoding]::UTF8

$Root        = $PSScriptRoot
$ConfigFile  = Join-Path $Root 'config.json'
$BloatFile   = Join-Path $Root 'bloatware.txt'
$KeepFile    = Join-Path $Root 'keep.txt'
$DisabledLog = Join-Path $Root 'disabled-by-tidyo.txt'
$LogFile     = Join-Path $Root 'Tidyo-PC.log'
$script:Adb    = $null
$script:Serial = $null

# ---------- Tiện ích ----------

function Write-Title([string]$t) { Write-Host ''; Write-Host "=== $t ===" -ForegroundColor Cyan }
function Write-Ok([string]$t)    { Write-Host "  [OK] $t" -ForegroundColor Green }
function Write-Warn([string]$t)  { Write-Host "  [!] $t" -ForegroundColor Yellow }

function Write-Log([string]$t) {
    $line = '{0}  {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $t
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

function Confirm-Step([string]$question) {
    if ($Yes) { return $true }
    $a = Read-Host "$question (c = có / Enter = bỏ qua)"
    return ($a -match '^(c|y)')
}

function Format-KB([double]$kb) {
    if ($kb -ge 1MB) { return '{0:N2} GB' -f ($kb / 1MB) }
    if ($kb -ge 1KB) { return '{0:N0} MB' -f ($kb / 1KB) }
    return '{0:N0} KB' -f $kb
}

# Tên gói hợp lệ. BẮT BUỘC kiểm tra trước khi đưa vào bất kỳ chuỗi `adb shell` nào
# (bloatware.txt / keep.txt do người dùng sửa được, đầu ra từ tivi cũng không đáng tin tuyệt đối).
$PackagePattern = '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+\z'
function Test-PackageName([string]$name) { return ($name -cmatch $PackagePattern) }

# Chỉ giữ tên gói hợp lệ; tên không hợp lệ bị bỏ qua kèm cảnh báo.
function Select-ValidPackages($names, [string]$source) {
    $ok = @()
    foreach ($n in @($names)) {
        if (Test-PackageName $n) { $ok += $n }
        else { Write-Warn "Bỏ qua tên gói không hợp lệ ($source): '$n'" }
    }
    return $ok
}

# Đặt chuỗi vào trong dấu nháy đơn cho sh của tivi; dấu ' bên trong thành '\''.
function ConvertTo-ShQuoted([string]$s) { return "'" + $s.Replace("'", "'\''") + "'" }

# Chạy một lệnh trong shell của tivi, trả về mảng dòng.
function Sh([string]$cmd) {
    $out = & $script:Adb -s $script:Serial shell $cmd
    return @($out | ForEach-Object { "$_".TrimEnd() } | Where-Object { $_ -ne '' })
}

# Cho người dùng chọn nhiều mục: "1,3,5" hoặc "a" (tất cả). Trả về mảng chỉ số (bắt đầu từ 0).
function Read-Selection([int]$count) {
    $a = Read-Host "Nhập số cần chọn (vd: 1,3,5), 'a' = tất cả, Enter = không chọn"
    if (-not $a) { return @() }
    if ($a.Trim() -eq 'a') { return @(0..($count - 1)) }
    $idx = @()
    foreach ($tok in ($a -split '[,\s]+')) {
        if ($tok -match '^\d+$') {
            $n = [int]$tok
            if ($n -ge 1 -and $n -le $count) { $idx += ($n - 1) }
        }
    }
    return @($idx | Select-Object -Unique)
}

# ---------- ADB ----------

function Find-Adb {
    # Thứ tự: Android SDK của Android Studio, thư mục platform-tools cạnh script, rồi PATH.
    if ($env:LOCALAPPDATA) {
        $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
        if (Test-Path $sdk) { return $sdk }
    }
    $local = Join-Path $Root 'platform-tools\adb.exe'
    if (Test-Path $local) { return $local }
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Install-Adb {
    $url = 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip'
    Write-Warn 'Chưa có adb trên máy này.'
    Write-Host "  Nguồn tải chính thức của Google: $url"
    $a = Read-Host '  Tải về (khoảng 7 MB) và giải nén vào thư mục Tidyo PC? (c = có)'
    if ($a -notmatch '^(c|y)') { return $null }
    $zip = Join-Path $env:TEMP 'platform-tools.zip'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    Expand-Archive -Path $zip -DestinationPath $Root -Force
    Remove-Item $zip -Force
    return (Find-Adb)
}

function Connect-TV {
    if (-not $Ip -and (Test-Path $ConfigFile)) {
        $Ip = (Get-Content $ConfigFile -Raw | ConvertFrom-Json).Ip
    }
    if (-not $Ip) {
        Write-Host 'Xem IP tivi tại: Cài đặt > Mạng > Trạng thái mạng (hoặc Giới thiệu > Trạng thái).'
        $Ip = Read-Host 'Nhập địa chỉ IP của tivi (vd: 192.168.1.50)'
    }
    if ($Ip -notmatch '^\d{1,3}(\.\d{1,3}){3}$') {
        Write-Warn "IP không hợp lệ: '$Ip'"
        return $false
    }
    $script:Serial = "${Ip}:5555"

    Write-Host "Đang kết nối $($script:Serial) ..."
    & $script:Adb connect $script:Serial | Out-Host

    for ($try = 1; $try -le 5; $try++) {
        $line = & $script:Adb devices | Where-Object { $_ -like "$($script:Serial)*" } | Select-Object -First 1
        $state = if ($line) { ($line -split '\s+')[1] } else { 'none' }
        if ($state -eq 'device') {
            @{ Ip = $Ip } | ConvertTo-Json | Set-Content -Path $ConfigFile -Encoding UTF8
            Write-Ok 'Đã kết nối tivi.'
            return $true
        }
        if ($state -eq 'unauthorized') {
            Write-Warn "Tivi đang hiện hộp thoại 'Cho phép gỡ lỗi USB?'. Hãy chọn 'Luôn cho phép từ máy tính này' rồi bấm OK."
        } else {
            Write-Warn "Chưa kết nối được (trạng thái: $state). Kiểm tra: tivi đang bật, cùng mạng, đã bật Gỡ lỗi ADB."
        }
        $a = Read-Host '  Enter = thử lại, q = thoát'
        if ($a -eq 'q') { break }
        & $script:Adb disconnect $script:Serial | Out-Null
        & $script:Adb connect $script:Serial | Out-Host
    }
    return $false
}

# ---------- Đọc trạng thái ----------

function Get-Storage {
    $o = @(Sh "stat -f -c '%a %b %S' /data 2>/dev/null")
    if ($o.Count -gt 0 -and $o[0] -match '^(\d+) (\d+) (\d+)$') {
        $bs = [double]$Matches[3]
        return [pscustomobject]@{
            FreeKB  = [double]$Matches[1] * $bs / 1024
            TotalKB = [double]$Matches[2] * $bs / 1024
        }
    }
    $o = @(Sh 'df -k /data 2>/dev/null')
    if ($o.Count -gt 0) {
        $t = $o[-1] -split '\s+'
        if ($t.Count -ge 4 -and $t[1] -match '^\d+$' -and $t[3] -match '^\d+$') {
            return [pscustomobject]@{ FreeKB = [double]$t[3]; TotalKB = [double]$t[1] }
        }
    }
    return $null
}

function Get-Memory {
    $total = $null; $avail = $null
    foreach ($l in @(Sh 'cat /proc/meminfo')) {
        if ($l -match '^MemTotal:\s+(\d+)')     { $total = [double]$Matches[1] }
        if ($l -match '^MemAvailable:\s+(\d+)') { $avail = [double]$Matches[1] }
    }
    if ($null -eq $total) { return $null }
    return [pscustomobject]@{ TotalKB = $total; AvailKB = $avail }
}

function Show-Status {
    $s = Get-Storage
    $m = Get-Memory
    if ($s) {
        $pct = 100 * $s.FreeKB / $s.TotalKB
        Write-Host ('  Bộ nhớ trong : còn trống {0} / {1} ({2:N0}%)' -f (Format-KB $s.FreeKB), (Format-KB $s.TotalKB), $pct)
    } else { Write-Warn 'Không đọc được dung lượng bộ nhớ trong.' }
    if ($m -and $null -ne $m.AvailKB) {
        Write-Host ('  RAM          : còn trống {0} / {1}' -f (Format-KB $m.AvailKB), (Format-KB $m.TotalKB))
    } elseif ($m) {
        Write-Host ('  RAM          : tổng {0}' -f (Format-KB $m.TotalKB))
    }
    return [pscustomobject]@{ Storage = $s; Memory = $m }
}

function Get-SdkLevel {
    $o = @(Sh 'getprop ro.build.version.sdk')
    if ($o.Count -gt 0 -and $o[0] -match '^\d+$') { return [int]$o[0] }
    return 0
}

# ---------- 1. Khảo sát ----------

function Show-Info {
    Write-Title 'Thông tin tivi'
    $brand   = (@(Sh 'getprop ro.product.brand') -join '')
    $model   = (@(Sh 'getprop ro.product.model') -join '')
    $release = (@(Sh 'getprop ro.build.version.release') -join '')
    $sdk     = Get-SdkLevel
    Write-Host "  Máy          : $brand $model"
    Write-Host "  Android      : $release (API $sdk)"
    $null = Show-Status
    $all = @(Sh 'pm list packages').Count
    $usr = @(Sh 'pm list packages -3').Count
    $dis = @(Sh 'pm list packages -d').Count
    Write-Host "  Ứng dụng     : $all gói ($usr do bạn cài, $dis đang bị vô hiệu hoá)"
    Write-Log "INFO $brand $model Android $release API $sdk"
}

# ---------- 2. Dung lượng từng app ----------

function Get-AppSizes {
    # Android 8+: dumpsys diskstats có sẵn kích thước app / dữ liệu / cache.
    $names = $null; $app = $null; $data = $null; $cache = $null
    foreach ($l in @(Sh 'dumpsys diskstats')) {
        if ($l -match '^Package Names:\s*\[(.*)\]')  { $names = @($Matches[1] -split ',' | ForEach-Object { $_.Trim(' "') }) }
        if ($l -match '^App Sizes:\s*\[(.*)\]')      { $app   = @($Matches[1] -split ',' | ForEach-Object { $_.Trim() }) }
        if ($l -match '^App Data Sizes:\s*\[(.*)\]') { $data  = @($Matches[1] -split ',' | ForEach-Object { $_.Trim() }) }
        if ($l -match '^Cache Sizes:\s*\[(.*)\]')    { $cache = @($Matches[1] -split ',' | ForEach-Object { $_.Trim() }) }
    }
    $rows = @()
    if ($names -and $app -and $names.Count -eq $app.Count) {
        for ($i = 0; $i -lt $names.Count; $i++) {
            $d = if ($data  -and $data.Count  -gt $i) { [double]$data[$i] }  else { 0 }
            $c = if ($cache -and $cache.Count -gt $i) { [double]$cache[$i] } else { 0 }
            $rows += [pscustomobject]@{
                Package = $names[$i]
                AppKB   = [double]$app[$i] / 1024
                DataKB  = $d / 1024
                CacheKB = $c / 1024
                TotalKB = ([double]$app[$i] + $d + $c) / 1024
            }
        }
        return [pscustomobject]@{ Full = $true; Rows = $rows }
    }

    # Android 7: chỉ đo được kích thước file APK.
    $cmd = 'pm list packages -f | while read l; do l=${l#package:}; f=${l%=*}; n=${l##*=}; echo $(stat -c %s $f 2>/dev/null) $n; done'
    foreach ($l in @(Sh $cmd)) {
        if ($l -match '^(\d+) (\S+)$') {
            $kb = [double]$Matches[1] / 1024
            $rows += [pscustomobject]@{ Package = $Matches[2]; AppKB = $kb; DataKB = 0; CacheKB = 0; TotalKB = $kb }
        }
    }
    return [pscustomobject]@{ Full = $false; Rows = $rows }
}

function Show-Apps {
    Write-Title 'Ứng dụng chiếm nhiều dung lượng nhất'
    $r = Get-AppSizes
    if ($r.Rows.Count -eq 0) { Write-Warn 'Không đọc được kích thước ứng dụng.'; return }
    if (-not $r.Full) { Write-Warn 'Android 7 chỉ cho đo kích thước file cài đặt (APK), không có số liệu dữ liệu/cache.' }
    $user = @(Sh 'pm list packages -3' | ForEach-Object { $_ -replace '^package:', '' })
    $r.Rows | Sort-Object TotalKB -Descending | Select-Object -First 20 | ForEach-Object {
        [pscustomobject]@{
            'Gói'      = $_.Package
            'Loại'     = if ($user -contains $_.Package) { 'bạn cài' } else { 'hệ thống' }
            'Tổng'     = Format-KB $_.TotalKB
            'App'      = Format-KB $_.AppKB
            'Dữ liệu'  = Format-KB $_.DataKB
            'Cache'    = Format-KB $_.CacheKB
        }
    } | Format-Table -AutoSize | Out-Host
}

# ---------- 3. Dọn nhanh ----------

$JunkExpr = "find /sdcard -path /sdcard/Android -prune -o -type f \( -iname '*.apk' -o -iname '*.tmp' -o -iname '*.log' -o -path '*/.thumbnails/*' \)"

# Luật rác (giống JunkRules.java của Tidyo TV, có tính .apk): đường dẫn tuyệt đối dưới /sdcard/,
# bỏ qua /sdcard/Android/, không có '..'.
function Test-JunkPath([string]$p) {
    if (-not $p.StartsWith('/sdcard/') -or $p.StartsWith('/sdcard/Android/')) { return $false }
    $parts = $p.Substring(8) -split '/'
    if ($parts -contains '..') { return $false }
    $name = $parts[-1].ToLowerInvariant()
    if ($name -eq '') { return $false }
    if ($parts.Count -gt 1 -and ($parts[0..($parts.Count - 2)] -ccontains '.thumbnails')) { return $true }
    return ($name.EndsWith('.apk') -or $name.EndsWith('.tmp') -or $name.EndsWith('.log'))
}

# Xoá đúng các file đã liệt kê: mỗi file một lệnh rm -f -- '<đường dẫn>' (dấu ' trong đường dẫn thành '\'').
# Lệnh chỉ in D nếu file có thật trước đó và đã biến mất sau rm, nên số đếm là số file xoá được thực sự.
function Remove-DeviceFiles($files) {
    $count = 0; $kb = 0.0
    $cmd = ''; $chunk = @()
    $all = @($files) + @($null)   # phần tử $null cuối cùng để đẩy mẻ còn lại
    foreach ($f in $all) {
        $piece = ''
        if ($f) {
            $q = ConvertTo-ShQuoted $f.Path
            $piece = "if [ -e $q ]; then rm -f -- $q; [ -e $q ] || echo D; fi; "
        }
        if ($chunk.Count -gt 0 -and (-not $f -or ($cmd.Length + $piece.Length) -gt 4000)) {
            $n = @(@(Sh $cmd) | Where-Object { $_ -eq 'D' }).Count
            $count += $n
            $kb += ($chunk | Measure-Object KB -Sum).Sum * $n / $chunk.Count
            $cmd = ''; $chunk = @()
        }
        if ($f) { $cmd += $piece; $chunk += $f }
    }
    return [pscustomobject]@{ Count = $count; KB = $kb }
}

function Clear-Junk {
    Write-Title 'File rác trong bộ nhớ dùng chung'
    $files = @()
    $skipped = 0
    foreach ($l in @(Sh "$JunkExpr -exec stat -c '%s %n' {} \; 2>/dev/null")) {
        if ($l -match '^(\d+) (.+)$') {
            $p = $Matches[2]
            # Bỏ file có xuống dòng/nháy kép trong tên (không xử lý an toàn được) và mọi đường dẫn
            # không đúng luật rác (phòng khi tên file chứa xuống dòng làm lệch đầu ra của find).
            if ($p -match '["\r\n]' -or -not (Test-JunkPath $p)) { $skipped++; continue }
            $files += [pscustomobject]@{ KB = [double]$Matches[1] / 1024; Path = $p }
        }
    }
    if ($skipped -gt 0) { Write-Warn "Bỏ qua $skipped file có tên không xử lý an toàn được." }
    if ($files.Count -eq 0) { Write-Ok 'Không có file rác (APK thừa, .tmp, .log, thumbnail).'; return }
    $sum = ($files | Measure-Object KB -Sum).Sum
    $files | Sort-Object KB -Descending | Select-Object -First 30 | ForEach-Object {
        Write-Host ('  {0,10}  {1}' -f (Format-KB $_.KB), $_.Path)
    }
    if ($files.Count -gt 30) { Write-Host "  ... và $($files.Count - 30) file nhỏ hơn" }
    Write-Host ("  Tổng: {0} file, {1}" -f $files.Count, (Format-KB $sum))
    if ($Yes) { Write-Warn 'Chế độ tự động: không xoá file, chỉ liệt kê.'; return }
    $a = Read-Host '  Xoá vĩnh viễn các file trên? Không khôi phục được. (c = có / Enter = bỏ qua)'
    if ($a -notmatch '^(c|y)') { return }
    $r = Remove-DeviceFiles $files
    $deleted = $r.Count; $deletedKB = $r.KB
    Write-Ok ("Đã xoá {0}/{1} file ({2})." -f $deleted, $files.Count, (Format-KB $deletedKB))
    if ($deleted -lt $files.Count) { Write-Warn 'Một số file không xoá được (không đủ quyền hoặc đã biến mất).' }
    Write-Log ("JUNK xoá {0}/{1} file, {2}" -f $deleted, $files.Count, (Format-KB $deletedKB))
}

function Invoke-QuickClean {
    Write-Title 'Trước khi dọn'
    $before = Show-Status

    Write-Title 'Xoá cache của tất cả ứng dụng'
    Sh 'pm trim-caches 999G 2>&1' | Out-Host
    Write-Ok 'Đã yêu cầu hệ thống xoá cache.'

    Write-Title 'Dừng các tiến trình chạy nền'
    Sh 'am kill-all 2>&1' | Out-Host
    Write-Ok 'Đã dừng các tiến trình nền có thể dừng an toàn.'

    Clear-Junk

    Start-Sleep -Seconds 2
    Write-Title 'Sau khi dọn'
    $after = Show-Status

    if ($before.Storage -and $after.Storage) {
        $gain = $after.Storage.FreeKB - $before.Storage.FreeKB
        Write-Host ('  => Bộ nhớ trong giải phóng: {0}' -f (Format-KB ([math]::Max(0, $gain)))) -ForegroundColor Green
        Write-Log ('CLEAN bộ nhớ trống {0} -> {1}' -f (Format-KB $before.Storage.FreeKB), (Format-KB $after.Storage.FreeKB))
    }
    if ($before.Memory -and $after.Memory -and $null -ne $before.Memory.AvailKB) {
        $gain = $after.Memory.AvailKB - $before.Memory.AvailKB
        Write-Host ('  => RAM giải phóng: {0}' -f (Format-KB ([math]::Max(0, $gain)))) -ForegroundColor Green
    }
}

# ---------- 4. Buộc dừng app do người dùng cài ----------

function Read-ListFile([string]$path) {
    if (-not (Test-Path $path)) { return @() }
    $rows = @()
    foreach ($l in (Get-Content $path -Encoding UTF8)) {
        $l = $l.Trim()
        if (-not $l -or $l.StartsWith('#')) { continue }
        $parts = $l -split '\|', 2
        $note = if ($parts.Count -gt 1) { $parts[1].Trim() } else { '' }
        $pkg = $parts[0].Trim()
        if (-not (Test-PackageName $pkg)) { Write-Warn "Bỏ qua tên gói không hợp lệ trong $(Split-Path $path -Leaf): '$pkg'"; continue }
        $rows += [pscustomobject]@{ Package = $pkg; Note = $note }
    }
    return $rows
}

function Invoke-ForceStop {
    Write-Title 'Buộc dừng ứng dụng bạn đã cài'
    $keep = @(Read-ListFile $KeepFile | ForEach-Object { $_.Package })
    $pkgs = @(Select-ValidPackages @(Sh 'pm list packages -3' | ForEach-Object { $_ -replace '^package:', '' }) 'từ tivi' | Where-Object { $keep -notcontains $_ } | Sort-Object)
    if ($pkgs.Count -eq 0) { Write-Ok 'Không có ứng dụng nào để dừng.'; return }
    for ($i = 0; $i -lt $pkgs.Count; $i++) { Write-Host ('  {0,2}. {1}' -f ($i + 1), $pkgs[$i]) }
    Write-Host '  (App bị buộc dừng sẽ chạy lại bình thường khi bạn mở nó. Gói ghi trong keep.txt không hiện ở đây.)'
    $sel = @(Read-Selection $pkgs.Count)
    if ($sel.Count -eq 0) { return }
    $chosen = @($sel | ForEach-Object { $pkgs[$_] })
    $null = Sh ("for p in {0}; do am force-stop `$p; done" -f ($chosen -join ' '))
    Write-Ok "Đã buộc dừng $($chosen.Count) ứng dụng."
    Write-Log ("FORCE-STOP " + ($chosen -join ', '))
}

# ---------- 5. Bloatware ----------

function Invoke-Debloat {
    Write-Title 'Vô hiệu hoá ứng dụng cài sẵn không dùng'
    $list = @(Read-ListFile $BloatFile)
    if ($list.Count -eq 0) { Write-Warn "Không tìm thấy danh sách: $BloatFile"; return }
    $enabled = @(Select-ValidPackages @(Sh 'pm list packages -e' | ForEach-Object { $_ -replace '^package:', '' }) 'từ tivi')
    $found = @($list | Where-Object { $enabled -contains $_.Package })
    if ($found.Count -eq 0) { Write-Ok 'Không có gói nào trong danh sách đang bật trên tivi này.'; return }

    for ($i = 0; $i -lt $found.Count; $i++) {
        Write-Host ('  {0,2}. {1,-48} {2}' -f ($i + 1), $found[$i].Package, $found[$i].Note)
    }
    Write-Host '  Vô hiệu hoá = ẩn và ngừng chạy, KHÔNG gỡ. Bật lại bất cứ lúc nào ở mục 6.'
    $sel = @(Read-Selection $found.Count)
    foreach ($i in $sel) {
        $p = $found[$i].Package
        $out = (@(Sh "pm disable-user --user 0 $p 2>&1") -join ' ')
        if ($out -match 'disabled') {
            Write-Ok "$p"
            Add-Content -Path $DisabledLog -Value $p -Encoding UTF8
            Write-Log "DISABLE $p"
        } else {
            Write-Warn "$p : $out"
        }
    }
}

# ---------- 6. Khôi phục ----------

function Invoke-Restore {
    Write-Title 'Bật lại ứng dụng đã vô hiệu hoá'
    $mine = @()
    if (Test-Path $DisabledLog) { $mine = @(Get-Content $DisabledLog -Encoding UTF8 | ForEach-Object { $_.Trim() }) }
    $disabled = @(Select-ValidPackages @(Sh 'pm list packages -d' | ForEach-Object { $_ -replace '^package:', '' }) 'từ tivi' | Sort-Object)
    if ($disabled.Count -eq 0) { Write-Ok 'Không có gói nào đang bị vô hiệu hoá.'; return }
    for ($i = 0; $i -lt $disabled.Count; $i++) {
        $tag = if ($mine -contains $disabled[$i]) { '(do Tidyo PC tắt)' } else { '(tắt sẵn từ trước)' }
        Write-Host ('  {0,2}. {1,-48} {2}' -f ($i + 1), $disabled[$i], $tag)
    }
    $sel = @(Read-Selection $disabled.Count)
    foreach ($i in $sel) {
        $p = $disabled[$i]
        $out = (@(Sh "pm enable $p 2>&1") -join ' ')
        if ($out -match 'enabled') {
            Write-Ok "$p"
            Write-Log "ENABLE $p"
            if (Test-Path $DisabledLog) {
                $rest = @(Get-Content $DisabledLog -Encoding UTF8 | Where-Object { $_.Trim() -ne $p })
                Set-Content -Path $DisabledLog -Value $rest -Encoding UTF8
            }
        } else {
            Write-Warn "$p : $out"
        }
    }
}

# ---------- Chương trình chính ----------

$script:Adb = Find-Adb
if (-not $script:Adb) { $script:Adb = Install-Adb }
if (-not $script:Adb) {
    Write-Warn 'Không có adb nên không thể tiếp tục.'
    exit 1
}

if (-not (Connect-TV)) { exit 1 }

switch ($Action) {
    'Info'  { Show-Info }
    'Apps'  { Show-Apps }
    'Clean' { Invoke-QuickClean }
    default {
        Show-Info
        while ($true) {
            Write-Host ''
            Write-Host '--------- TIDYO PC ---------' -ForegroundColor Cyan
            Write-Host ' 1. Xem thông tin tivi'
            Write-Host ' 2. Xem ứng dụng chiếm dung lượng'
            Write-Host ' 3. Dọn nhanh (cache + tiến trình nền + file rác)'
            Write-Host ' 4. Buộc dừng ứng dụng bạn đã cài'
            Write-Host ' 5. Vô hiệu hoá ứng dụng cài sẵn (bloatware)'
            Write-Host ' 6. Bật lại ứng dụng đã vô hiệu hoá'
            Write-Host ' 0. Thoát'
            $c = Read-Host 'Chọn'
            switch ($c) {
                '1' { Show-Info }
                '2' { Show-Apps }
                '3' { Invoke-QuickClean }
                '4' { Invoke-ForceStop }
                '5' { Invoke-Debloat }
                '6' { Invoke-Restore }
                '0' { & $script:Adb disconnect $script:Serial | Out-Null; exit 0 }
                default { }
            }
        }
    }
}

& $script:Adb disconnect $script:Serial | Out-Null
