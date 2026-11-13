package io.github.phutrung08.tidyo;

import android.app.Activity;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.phutrung08.common.Ui;

/** Danh sách ứng dụng theo dung lượng. Bấm OK vào một app để mở trang cài đặt của nó. */
public class AppsActivity extends Activity {
    private static final class Row {
        String label, pkg;
        long total, cache;
        boolean user;
    }

    private TextView hint;
    private LinearLayout list;
    private String focusedPkg;
    /** Tăng mỗi lần nạp; kết quả của lần cũ bị bỏ qua. */
    private int job;
    private boolean loading;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        hint = Ui.text(this, "Đang đọc dung lượng…", 14, Ui.SUB);
        LinearLayout col = Ui.screen(this, "Ứng dụng", hint);
        list = Ui.glassList(this, col);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void openSettings(String pkg) {
        focusedPkg = pkg;
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
        } catch (Exception e) {
            Toast.makeText(this, "Tivi này không mở được trang cài đặt ứng dụng", Toast.LENGTH_LONG).show();
        }
    }

    private void load() {
        if (loading) return; // onResume dồn dập: bỏ qua lần nạp chồng
        loading = true;
        final int mine = ++job;
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            StorageStatsManager ssm = (StorageStatsManager) getSystemService(Context.STORAGE_STATS_SERVICE);
            List<Row> out = new ArrayList<>();
            boolean full = true;
            for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                Row r = new Row();
                r.pkg = ai.packageName;
                r.label = String.valueOf(pm.getApplicationLabel(ai));
                r.user = (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
                try {
                    StorageStats s = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, ai.packageName,
                            Process.myUserHandle());
                    r.cache = s.getCacheBytes();
                    r.total = s.getAppBytes() + s.getDataBytes();
                } catch (Exception e) {
                    // Chưa được cấp quyền "Truy cập dữ liệu sử dụng": chỉ đo được file cài đặt.
                    full = false;
                    r.total = ai.sourceDir != null ? new File(ai.sourceDir).length() : 0;
                }
                out.add(r);
            }
            Collections.sort(out, (x, y) -> Long.compare(y.total, x.total));
            final boolean fullStats = full;
            runOnUiThread(() -> {
                if (isDestroyed() || mine != job) return;
                loading = false;
                list.removeAllViews();
                View toFocus = null;
                for (Row r : out) {
                    View row = Ui.listRow(this, r.label,
                            (r.user ? "bạn cài · " : "hệ thống · ") + r.pkg,
                            Ui.size(r.total),
                            fullStats ? "cache " + Ui.size(r.cache) : null,
                            v -> openSettings(r.pkg));
                    list.addView(row);
                    if (toFocus == null || r.pkg.equals(focusedPkg)) toFocus = row;
                }
                hint.setText(fullStats
                        ? out.size() + " ứng dụng · bấm OK để mở trang cài đặt (xoá bộ nhớ đệm, buộc dừng, gỡ)"
                        : out.size() + " ứng dụng · chỉ hiện kích thước file cài đặt vì chưa có quyền truy cập dữ liệu sử dụng");
                if (toFocus != null) toFocus.requestFocus();
            });
        }).start();
    }
}
