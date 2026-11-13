# Glassdeck

Màn hình chính (launcher) gọn nhẹ cho Android TV, giao diện kính mờ, kèm Tidyo TV (ứng dụng dọn dẹp) và Tidyo PC (công cụ dọn qua ADB trên Windows).

## Tiếng Việt

### Cấu trúc repo

| Thư mục | Nội dung |
|---|---|
| `glassdeck/` | Launcher. Nhắm tới CH Play, `targetSdk` 35. |
| `tidyo/` | Tidyo TV. Chỉ phát hành trên GitHub. `targetSdk` 29 có chủ đích để giữ quyền quét file rác. |
| `common/` | Giao diện dùng chung. |
| `tools/tidyo-pc/` | `Tidyo-PC.bat`: dọn cache, tắt bloatware qua ADB. |
| `tools/push-wallpapers.ps1` | Chép hình nền từ PC lên tivi. |
| `tools/Chep-hinh-nen.bat` | Chạy script chép hình nền từ PC. |

### Glassdeck

- Đồng hồ lớn.
- Khay kính 6 ứng dụng, trượt ngang.
- Giữ OK để sắp xếp hoặc ẩn ứng dụng.
- Chọn cổng vào (HDMI/AV).
- Hình nền: một hình hoặc nhiều hình tự đổi. Lấy từ bộ nhớ tivi hoặc USB.
- Điều chỉnh độ trong suốt của kính.
- Mục **Dọn dẹp**: xem bộ nhớ/RAM, xem ứng dụng theo dung lượng, mở trang xoá cache hoặc gỡ ứng dụng.

### Tidyo TV

- Xem bộ nhớ/RAM.
- Yêu cầu dừng tiến trình nền.
- Danh sách ứng dụng theo dung lượng.
- Tìm và xoá file rác (`.tmp`, `.log`, thumbnail, `.apk`). Luôn hỏi trước khi xoá.

> **Lưu ý:** Android không cho ứng dụng xoá cache của ứng dụng khác. Muốn xoá cache hàng loạt, hãy dùng Tidyo PC.

### Quyền riêng tư

- Không ứng dụng nào xin quyền Internet.
- Không thu thập dữ liệu.
- Chi tiết xem tại [PRIVACY.md](PRIVACY.md).

### Build

**Yêu cầu:**

- JDK 17 trở lên. JDK đi kèm Android Studio dùng được.
- Android SDK.

**Lệnh:**

```powershell
.\gradlew.bat :glassdeck:assembleDebug
.\gradlew.bat :tidyo:assembleDebug
.\gradlew.bat test
.\gradlew.bat :glassdeck:bundleRelease
```

`bundleRelease` tạo file AAB để đưa lên CH Play.

**Ký bản phát hành (release signing):**

Đặt các biến sau trong `%USERPROFILE%\.gradle\gradle.properties` hoặc trong biến môi trường:

- `GLASSDECK_KEYSTORE`
- `GLASSDECK_KEYSTORE_PASSWORD`
- `GLASSDECK_KEY_ALIAS`
- `GLASSDECK_KEY_PASSWORD`

**Không bao giờ commit file keystore vào repo.**

### Cài lên tivi (sideload)

1. Bật **Developer options** và **ADB debugging** trên tivi.
2. Kết nối từ máy tính:

```powershell
adb connect <IP>:5555
adb install -r glassdeck/build/outputs/apk/debug/glassdeck-debug.apk
```

### Đặt Glassdeck làm màn hình chính

Android TV không có màn hình chọn launcher. Cách làm là tắt launcher gốc của hãng:

```powershell
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

Để khôi phục launcher gốc:

```powershell
adb shell pm enable com.google.android.tvlauncher
```

> **Cảnh báo:** Chỉ tắt launcher gốc khi Glassdeck đã chạy ổn. Nếu Glassdeck bị lỗi, hãy dùng lệnh khôi phục ở trên từ máy tính.

> **Thiết bị cụ thể:** Đã thử trên Sony BRAVIA (Android TV 11). Thiết bị Google TV có thể dùng launcher gốc với tên gói khác.

### Hình nền

Hình nền mẫu không kèm theo repo. Hãy dùng hình của bạn.

### Thư viện bên thứ ba

- Ứng dụng không dùng thư viện bên thứ ba nào.
- Chỉ dùng JUnit 4 (EPL 1.0) cho test.
- Tidyo PC tải Android SDK Platform-Tools của Google khi bạn đồng ý. Công cụ này không kèm trong repo và tuân theo giấy phép của Google.

### Thông báo về thương hiệu

Android TV và Google TV là thương hiệu của Google LLC. Sony và BRAVIA là thương hiệu của Sony Group. Dự án không liên kết với các công ty này.

### Giấy phép

MIT. Xem [LICENSE](LICENSE).

---

## English

Glassdeck is a lightweight home screen launcher for Android TV with a frosted-glass look. It includes Tidyo TV (a cleanup app) and Tidyo PC (an ADB cleanup tool for Windows).

### Repo layout

| Folder | Contents |
|---|---|
| `glassdeck/` | Launcher. Targets Google Play, `targetSdk` 35. |
| `tidyo/` | Tidyo TV. Released on GitHub only. `targetSdk` 29 is intentional, to keep access to scan junk files. |
| `common/` | Shared UI. |
| `tools/tidyo-pc/` | `Tidyo-PC.bat`: clears cache and disables bloatware over ADB. |
| `tools/push-wallpapers.ps1` | Copies wallpapers from the PC to the TV. |
| `tools/Chep-hinh-nen.bat` | Runs the wallpaper copy script on the PC. |

### Glassdeck features

- Large clock.
- Glass shelf with 6 apps, scrollable sideways.
- Hold OK to sort or hide apps.
- Choose input source (HDMI/AV).
- Wallpaper: one image or many images that rotate. Load from TV storage or USB.
- Adjust glass transparency.
- **Cleanup** section: view memory/RAM, list apps by size, open the app page to clear cache or uninstall.

### Tidyo TV features

- View memory/RAM.
- Request to stop background processes.
- App list sorted by size.
- Find and delete junk files (`.tmp`, `.log`, thumbnails, `.apk`). Always asks before deleting.

> **Note:** Android does not let an app clear another app's cache. To clear caches in bulk, use Tidyo PC.

### Privacy

- No app requests the Internet permission.
- No data is collected.
- See [PRIVACY.md](PRIVACY.md) for details.

### Build

**Requirements:**

- JDK 17 or newer. The JDK bundled with Android Studio works.
- Android SDK.

**Commands:**

```powershell
.\gradlew.bat :glassdeck:assembleDebug
.\gradlew.bat :tidyo:assembleDebug
.\gradlew.bat test
.\gradlew.bat :glassdeck:bundleRelease
```

`bundleRelease` creates an AAB for Google Play.

**Release signing:** set these in `%USERPROFILE%\.gradle\gradle.properties` or as environment variables:

- `GLASSDECK_KEYSTORE`
- `GLASSDECK_KEYSTORE_PASSWORD`
- `GLASSDECK_KEY_ALIAS`
- `GLASSDECK_KEY_PASSWORD`

**Never commit the keystore to the repo.**

### Install on a TV (sideload)

1. Turn on **Developer options** and **ADB debugging** on the TV.
2. Connect from your computer:

```powershell
adb connect <IP>:5555
adb install -r glassdeck/build/outputs/apk/debug/glassdeck-debug.apk
```

### Set Glassdeck as the home screen

Android TV has no launcher picker. To replace the stock launcher, disable it:

```powershell
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

To restore the stock launcher:

```powershell
adb shell pm enable com.google.android.tvlauncher
```

> **Warning:** Only disable the stock launcher after Glassdeck runs well. If Glassdeck breaks, use the restore command above from a computer.

> **Device note:** Tested on Sony BRAVIA (Android TV 11). Google TV devices may use a different stock launcher package.

### Wallpapers

Sample wallpapers are not included in the repo. Use your own images.

### Third-party software

- The apps do not use any third-party libraries.
- Only JUnit 4 (EPL 1.0) is used, for tests.
- Tidyo PC downloads Google's Android SDK Platform-Tools when you agree. They are not included in the repo and are subject to Google's license.

### Trademark notice

Android TV and Google TV are trademarks of Google LLC. Sony and BRAVIA are trademarks of Sony Group. This project is not affiliated with these companies.

### License

MIT. See [LICENSE](LICENSE).
