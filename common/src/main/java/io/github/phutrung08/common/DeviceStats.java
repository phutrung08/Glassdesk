package io.github.phutrung08.common;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

/** Số liệu bộ nhớ trong và RAM, dùng chung cho Tidyo TV và mục Dọn dẹp của Glassdeck. */
public final class DeviceStats {
    public final long storageTotal, storageFree, ramTotal, ramFree;

    private DeviceStats(long storageTotal, long storageFree, long ramTotal, long ramFree) {
        this.storageTotal = storageTotal;
        this.storageFree = storageFree;
        this.ramTotal = ramTotal;
        this.ramFree = ramFree;
    }

    public static DeviceStats read(Context c) {
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return new DeviceStats(fs.getTotalBytes(), fs.getAvailableBytes(), mi.totalMem, mi.availMem);
    }

    public int storageUsedPercent() { return Sizes.usedPercent(storageTotal, storageFree); }

    public int ramUsedPercent() { return Sizes.usedPercent(ramTotal, ramFree); }

    public String storageLabel() { return "trống " + Sizes.format(storageFree) + " / " + Sizes.format(storageTotal); }

    public String ramLabel() { return "trống " + Sizes.format(ramFree) + " / " + Sizes.format(ramTotal); }
}
