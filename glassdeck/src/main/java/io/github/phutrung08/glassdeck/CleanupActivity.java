package io.github.phutrung08.glassdeck;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.github.phutrung08.common.DeviceStats;
import io.github.phutrung08.common.RingView;
import io.github.phutrung08.common.Sizes;
import io.github.phutrung08.common.Ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Dọn dẹp: hai vòng bộ nhớ trong và RAM, rồi danh sách app xếp theo dung lượng cài đặt.
 * Bấm OK vào một app để mở trang thông tin của hệ thống, nơi xoá bộ nhớ đệm, buộc dừng hoặc gỡ cài đặt.
 * Glassdeck không tự dừng hay gỡ app nào.
 */
public class CleanupActivity extends Activity {
    private static final class Entry {
        String pkg, label;
        long bytes;
    }

    private RingView storageRing, ramRing;
    private TextView storageLabel, ramLabel;
    private LinearLayout list;
    private int job;
    private String lastPkg;
    private ExecutorService exec;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        exec = Executors.newSingleThreadExecutor();
        TextView hint = Ui.text(this, getString(R.string.cleanup_hint), 14, Ui.SUB);
        LinearLayout col = Ui.screen(this, getString(R.string.cleanup_title), hint);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.setBackground(Ui.glass(this, Ui.GLASS, 22));
        stats.setPadding(Ui.dp(this, 24), Ui.dp(this, 14), Ui.dp(this, 24), Ui.dp(this, 14));
        storageRing = new RingView(this);
        ramRing = new RingView(this);
        storageLabel = Ui.text(this, "", 15, Ui.SUB);
        ramLabel = Ui.text(this, "", 15, Ui.SUB);
        stats.addView(statBlock(getString(R.string.cleanup_storage), storageRing, storageLabel), weight());
        stats.addView(statBlock(getString(R.string.cleanup_ram), ramRing, ramLabel), weight());
        LinearLayout.LayoutParams lpStats = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpStats.bottomMargin = Ui.dp(this, 14);
        col.addView(stats, lpStats);

        list = Ui.glassList(this, col);
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout statBlock(String title, RingView ring, TextView label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(ring, new LinearLayout.LayoutParams(Ui.dp(this, 84), Ui.dp(this, 84)));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(Ui.dp(this, 16), 0, 0, 0);
        TextView t = Ui.text(this, title, 18, Ui.WHITE);
        t.setTypeface(Ui.medium());
        text.addView(t);
        text.addView(label);
        box.addView(text);
        return box;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Quay lại từ trang thông tin app: nạp lại vì app có thể đã bị gỡ hoặc đã xoá dữ liệu.
        load();
    }

    @Override
    protected void onDestroy() {
        job++;
        exec.shutdownNow();
        super.onDestroy();
    }

    private void load() {
        final int mine = ++job;
        try {
            exec.execute(() -> loadInBackground(mine));
        } catch (RejectedExecutionException ignored) {
            // Màn hình đang đóng.
        }
    }

    private void loadInBackground(int mine) {
        DeviceStats stats = DeviceStats.read(this);
        List<Entry> found = queryApps();
        runOnUiThread(() -> {
            if (mine != job || isDestroyed()) return;
            storageRing.setPercent(stats.storageUsedPercent());
            ramRing.setPercent(stats.ramUsedPercent());
            storageLabel.setText(freeOf(stats.storageFree, stats.storageTotal));
            ramLabel.setText(freeOf(stats.ramFree, stats.ramTotal));
            show(found);
        });
    }

    private String freeOf(long free, long total) {
        return getString(R.string.cleanup_free_of, Sizes.format(free), Sizes.format(total));
    }

    private List<Entry> queryApps() {
        PackageManager pm = getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Map<String, Entry> byPackage = new LinkedHashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            if (byPackage.containsKey(ai.packageName)) continue;
            Entry e = new Entry();
            e.pkg = ai.packageName;
            e.label = String.valueOf(ri.loadLabel(pm));
            e.bytes = apkBytes(ai);
            byPackage.put(e.pkg, e);
        }
        List<Entry> out = new ArrayList<>(byPackage.values());
        Collections.sort(out, (x, y) -> AppOrder.compareBySize(x.bytes, x.label, y.bytes, y.label));
        return out;
    }

    /** Tổng dung lượng file APK (gồm cả các phần tách) của app. */
    private static long apkBytes(ApplicationInfo ai) {
        long total = new File(ai.sourceDir).length();
        if (ai.splitSourceDirs != null) {
            for (String s : ai.splitSourceDirs) total += new File(s).length();
        }
        return total;
    }

    private void show(List<Entry> entries) {
        list.removeAllViews();
        View focus = null;
        for (Entry e : entries) {
            View row = Ui.listRow(this, e.label, e.pkg, Sizes.format(e.bytes), null, v -> {
                lastPkg = e.pkg;
                openDetails(e);
            });
            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (focus == null || e.pkg.equals(lastPkg)) focus = row;
        }
        if (focus != null) focus.requestFocus();
    }

    private void openDetails(Entry e) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + e.pkg)));
        } catch (Exception ex) {
            Toast.makeText(this, getString(R.string.toast_details_failed, e.label), Toast.LENGTH_SHORT).show();
        }
    }
}
