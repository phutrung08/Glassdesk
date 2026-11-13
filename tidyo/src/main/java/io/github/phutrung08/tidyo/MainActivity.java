package io.github.phutrung08.tidyo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import io.github.phutrung08.common.DeviceStats;
import io.github.phutrung08.common.RingView;
import io.github.phutrung08.common.Ui;

/** Màn hình chính: tấm kính ở giữa chứa nút Dọn nhanh và hai vòng số liệu; nút kính ở góc trên. */
public class MainActivity extends Activity {
    private RingView ringStorage, ringRam;
    private TextView subStorage, subRam, status, mainLabel, mainHint;
    private View mainBtn;
    private boolean busy;
    /** Tăng mỗi lần bắt đầu một việc nền; kết quả của việc cũ bị bỏ qua. */
    private int job;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ui.background());
        root.setClipChildren(false);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView name = Ui.text(this, "Tidyo TV", 22, Ui.WHITE);
        name.setTypeface(Ui.medium());
        brand.addView(name);
        brand.addView(Ui.text(this, Build.MODEL + " · Android " + Build.VERSION.RELEASE, 13, Ui.DIM));
        FrameLayout.LayoutParams lpBrand = wrap(Gravity.TOP | Gravity.START);
        lpBrand.setMargins(Ui.dp(this, 56), Ui.dp(this, 28), 0, 0);
        root.addView(brand, lpBrand);

        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        TextView appsPill = Ui.pill(this, "Ứng dụng", v -> startActivity(new Intent(this, AppsActivity.class)));
        TextView junkPill = Ui.pill(this, "File rác", v -> startActivity(new Intent(this, JunkActivity.class)));
        TextView infoPill = Ui.pill(this, "Thông tin", v -> showInfo());
        pills.addView(appsPill);
        pills.addView(junkPill, gapLeft());
        pills.addView(infoPill, gapLeft());
        FrameLayout.LayoutParams lpPills = wrap(Gravity.TOP | Gravity.END);
        lpPills.setMargins(0, Ui.dp(this, 30), Ui.dp(this, 48), 0);
        root.addView(pills, lpPills);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setClipChildren(false);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setBackground(Ui.glass(this, Ui.GLASS, 28));
        panel.setPadding(Ui.dp(this, 24), Ui.dp(this, 30), Ui.dp(this, 24), Ui.dp(this, 30));
        // Nút tròn phóng to khi được chọn: không cắt phần tràn vào vùng lề của tấm kính.
        panel.setClipChildren(false);
        panel.setClipToPadding(false);

        ringStorage = new RingView(this);
        subStorage = Ui.text(this, "", 13, Ui.DIM);
        panel.addView(gauge(ringStorage, "Bộ nhớ đã dùng", subStorage));

        mainBtn = buildMainButton();
        LinearLayout.LayoutParams lpMain = new LinearLayout.LayoutParams(Ui.dp(this, 196), Ui.dp(this, 196));
        lpMain.setMargins(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        panel.addView(mainBtn, lpMain);

        ringRam = new RingView(this);
        subRam = Ui.text(this, "", 13, Ui.DIM);
        panel.addView(gauge(ringRam, "RAM đã dùng", subRam));
        center.addView(panel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = Ui.text(this, " \n ", 15, Ui.SUB);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, Ui.dp(this, 18), 0, 0);
        center.addView(status);

        FrameLayout.LayoutParams lpCenter = wrap(Gravity.CENTER);
        lpCenter.topMargin = Ui.dp(this, 34);
        root.addView(center, lpCenter);
        setContentView(root);

        mainBtn.setNextFocusUpId(appsPill.getId());
        appsPill.setNextFocusDownId(mainBtn.getId());
        junkPill.setNextFocusDownId(mainBtn.getId());
        infoPill.setNextFocusDownId(mainBtn.getId());
        mainBtn.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private FrameLayout.LayoutParams wrap(int gravity) {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
    }

    private LinearLayout.LayoutParams gapLeft() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 10);
        return lp;
    }

    private View gauge(RingView ring, String title, TextView sub) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(ring, new LinearLayout.LayoutParams(Ui.dp(this, 124), Ui.dp(this, 124)));
        TextView t = Ui.text(this, title, 15, Ui.SUB);
        t.setPadding(0, Ui.dp(this, 10), 0, 0);
        t.setGravity(Gravity.CENTER);
        sub.setGravity(Gravity.CENTER);
        col.addView(t);
        col.addView(sub);
        // Cột có bề rộng cố định để dòng dung lượng không bị cắt khi số liệu đổi độ dài.
        col.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 200), ViewGroup.LayoutParams.WRAP_CONTENT));
        return col;
    }

    /** Nút tròn: kính khi không chọn, trắng đặc với chữ tối khi được chọn. */
    private View buildMainButton() {
        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.OVAL);
        normal.setColor(Ui.GLASS);
        normal.setStroke(Ui.dp(this, 1), Ui.GLASS_EDGE);
        GradientDrawable focused = new GradientDrawable();
        focused.setShape(GradientDrawable.OVAL);
        focused.setColor(Ui.WHITE);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{}, normal);

        LinearLayout btn = new LinearLayout(this);
        btn.setId(View.generateViewId());
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(bg);
        btn.setFocusable(true);
        btn.setClickable(true);

        mainLabel = Ui.text(this, "Dọn nhanh", 26, Ui.WHITE);
        mainLabel.setTypeface(Ui.medium());
        mainLabel.setTextColor(Ui.focusColor(Ui.INK, Ui.WHITE));
        mainLabel.setDuplicateParentStateEnabled(true);
        mainLabel.setGravity(Gravity.CENTER);
        mainHint = Ui.text(this, "bấm OK", 14, Ui.SUB);
        mainHint.setTextColor(Ui.focusColor(Ui.INK_SUB, Ui.SUB));
        mainHint.setDuplicateParentStateEnabled(true);
        mainHint.setGravity(Gravity.CENTER);
        btn.addView(mainLabel);
        btn.addView(mainHint);

        btn.setOnFocusChangeListener((v, has) ->
                v.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f)
                        .translationZ(has ? Ui.dp(this, 10) : 0).setDuration(180).start());
        btn.setOnClickListener(v -> quickClean());
        return btn;
    }

    private void refresh() {
        DeviceStats s = DeviceStats.read(this);
        ringStorage.setPercent(s.storageUsedPercent());
        subStorage.setText(s.storageLabel());
        ringRam.setPercent(s.ramUsedPercent());
        subRam.setText(s.ramLabel());
    }

    private void quickClean() {
        if (busy) return;
        busy = true;
        final int mine = ++job;
        mainLabel.setText("Đang dọn…");
        mainHint.setText("");
        status.setText(" \n ");
        new Thread(() -> {
            long ramBefore = Cleaner.availableRam(this);
            int apps = Cleaner.killBackground(this);
            boolean storage = Cleaner.hasStorage(this);
            // Chỉ quét, KHÔNG xoá: việc xoá phải được người dùng xác nhận.
            List<File> junk = storage ? Cleaner.scan(false) : java.util.Collections.<File>emptyList();
            long junkBytes = JunkScanner.totalSize(junk);
            try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
            long ramAfter = Cleaner.availableRam(this);

            String stopped = Report.stopped(apps, ramBefore, ramAfter);
            runOnUiThread(() -> {
                if (isDestroyed() || mine != job) return;
                busy = false;
                mainLabel.setText("Dọn nhanh");
                mainHint.setText("bấm OK");
                refresh();
                if (!storage) {
                    status.setText(stopped + "\nChưa có quyền bộ nhớ nên không quét file tạm");
                } else if (junk.isEmpty()) {
                    status.setText(stopped + "\nKhông có file tạm nào");
                } else {
                    String found = Report.junkFound(junk.size(), junkBytes);
                    status.setText(stopped + "\nTìm thấy " + found + ", chưa xoá gì");
                    askDelete(stopped, junk, junkBytes);
                }
            });
        }).start();
    }

    private void askDelete(String stopped, List<File> junk, long junkBytes) {
        Ui.dialog(this, "Xoá file tạm?",
                "Tìm thấy " + Report.junkFound(junk.size(), junkBytes)
                        + " (.tmp, .log, thumbnail). File .apk không nằm trong số này.\nXoá xong không khôi phục được.",
                new String[]{"Giữ lại", "Xoá"},
                which -> {
                    if (which == 0) {
                        status.setText(stopped + "\nĐã giữ lại " + junk.size() + " file tạm");
                    } else {
                        deleteJunk(stopped, junk);
                    }
                });
    }

    private void deleteJunk(String stopped, List<File> junk) {
        final int mine = ++job;
        busy = true;
        mainLabel.setText("Đang xoá…");
        mainHint.setText("");
        new Thread(() -> {
            JunkScanner.Deleted d = JunkScanner.delete(junk);
            runOnUiThread(() -> {
                if (isDestroyed() || mine != job) return;
                busy = false;
                mainLabel.setText("Dọn nhanh");
                mainHint.setText("bấm OK");
                status.setText(stopped + "\nĐã xoá " + d.files + " file tạm (" + Ui.size(d.bytes) + ")");
                refresh();
            });
        }).start();
    }

    private String versionName() {
        try {
            @SuppressWarnings("deprecation")
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return v == null ? "" : v;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private void showInfo() {
        String text = "Tidyo TV " + versionName() + " — ứng dụng tự làm, không quảng cáo.\n\n"
                + "• Không có quyền Internet: không thể gửi bất kỳ dữ liệu nào ra ngoài.\n"
                + "• Dọn nhanh: yêu cầu hệ thống dừng tiến trình nền của các ứng dụng khác "
                + "(Android có thể khởi động lại một số ứng dụng, nên không hứa máy nhanh hơn), "
                + "rồi tìm file .tmp, .log và thumbnail. Chỉ xoá sau khi bạn bấm Xoá.\n"
                + "• File .apk không bao giờ bị xoá trong Dọn nhanh, bạn tự chọn trong mục File rác.\n"
                + "• Android không cho một ứng dụng xoá cache của ứng dụng khác. "
                + "Vào mục Ứng dụng, chọn một app để mở trang cài đặt của nó rồi bấm Xoá bộ nhớ đệm, "
                + "hoặc dùng công cụ Tidyo PC trên máy tính (tools\\tidyo-pc\\Tidyo-PC.bat) để xoá cache tất cả.";
        Ui.dialog(this, "Thông tin", text, new String[]{"Đóng"}, null);
    }
}
