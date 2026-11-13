package io.github.phutrung08.tidyo;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.util.List;

/** Phần "làm việc": dừng tiến trình nền và tìm file rác. Không có mã mạng. */
final class Cleaner {
    private Cleaner() {}

    static boolean hasStorage(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    static long availableRam(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    /** Yêu cầu hệ thống dừng tiến trình nền của mọi ứng dụng khác. Trả về số ứng dụng đã gửi yêu cầu. */
    static int killBackground(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        String self = c.getPackageName();
        int n = 0;
        for (ApplicationInfo ai : c.getPackageManager().getInstalledApplications(0)) {
            if (ai.packageName.equals(self)) continue;
            am.killBackgroundProcesses(ai.packageName);
            n++;
        }
        return n;
    }

    /**
     * Tìm file rác trong bộ nhớ dùng chung theo {@link JunkRules}.
     * File .apk chỉ gồm khi includeApk = true.
     */
    static List<File> scan(boolean includeApk) {
        return JunkScanner.scan(Environment.getExternalStorageDirectory(), includeApk);
    }
}
