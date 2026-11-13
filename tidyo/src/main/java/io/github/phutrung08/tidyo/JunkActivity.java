package io.github.phutrung08.tidyo;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.phutrung08.common.Ui;

/** Liệt kê file rác (APK thừa, .tmp, .log, thumbnail). Luôn hỏi trước khi xoá. */
public class JunkActivity extends Activity {
    private static final int REQ_STORAGE = 1;

    private static final class Item {
        final File file;
        final long size;

        Item(File file) {
            this.file = file;
            this.size = file.length();
        }
    }

    private final List<File> files = new ArrayList<>();
    private TextView hint, deleteAll;
    private LinearLayout list;
    private String rootPath;
    /** Tăng mỗi lần quét/xoá; kết quả của việc cũ bị bỏ qua. */
    private int job;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        rootPath = Environment.getExternalStorageDirectory().getPath();
        hint = Ui.text(this, "Đang quét…", 14, Ui.SUB);
        LinearLayout col = Ui.screen(this, "File rác", hint);

        deleteAll = Ui.pill(this, "Xoá tất cả", v -> confirmDeleteAll());
        deleteAll.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 14);
        col.addView(deleteAll, lp);

        list = Ui.glassList(this, col);
        Ui.getPanel(list).setVisibility(View.INVISIBLE);

        if (Cleaner.hasStorage(this)) scan();
        else requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code != REQ_STORAGE) return;
        if (Cleaner.hasStorage(this)) scan();
        else hint.setText("Chưa được cấp quyền bộ nhớ nên không quét được file.");
    }

    private void scan() {
        hint.setText("Đang quét…");
        final int mine = ++job;
        new Thread(() -> {
            List<File> found = Cleaner.scan(true);
            List<Item> items = new ArrayList<>(found.size());
            for (File f : found) items.add(new Item(f));
            Collections.sort(items, (x, y) -> Long.compare(y.size, x.size));
            runOnUiThread(() -> {
                if (isDestroyed() || mine != job) return;
                show(items);
            });
        }).start();
    }

    private void show(List<Item> items) {
        files.clear();
        list.removeAllViews();
        long sum = 0;
        for (Item it : items) {
            files.add(it.file);
            sum += it.size;
            String dir = it.file.getParent() == null ? "" : it.file.getParent();
            if (dir.startsWith(rootPath)) dir = "Bộ nhớ" + dir.substring(rootPath.length());
            list.addView(Ui.listRow(this, it.file.getName(), dir, Ui.size(it.size), null,
                    v -> confirmDeleteOne(it)));
        }
        View panel = Ui.getPanel(list);
        if (files.isEmpty()) {
            hint.setText("Không có file rác (APK thừa, .tmp, .log, thumbnail).");
            deleteAll.setVisibility(View.GONE);
            panel.setVisibility(View.INVISIBLE);
        } else {
            hint.setText(files.size() + " file · " + Ui.size(sum) + " · bấm OK vào một file để xoá riêng file đó");
            deleteAll.setVisibility(View.VISIBLE);
            panel.setVisibility(View.VISIBLE);
            list.getChildAt(0).requestFocus();
        }
    }

    private void confirmDeleteOne(Item it) {
        Ui.dialog(this, "Xoá file này?",
                it.file.getName() + " (" + Ui.size(it.size) + ")\nKhông khôi phục được.",
                new String[]{"Giữ lại", "Xoá"},
                which -> {
                    if (which == 1) deleteThenScan(Collections.singletonList(it.file));
                });
    }

    private void confirmDeleteAll() {
        final List<File> snapshot = new ArrayList<>(files);
        Ui.dialog(this, "Xoá " + snapshot.size() + " file?",
                "Tổng " + Ui.size(JunkScanner.totalSize(snapshot)) + ", gồm cả file .apk trong danh sách.\nKhông khôi phục được.",
                new String[]{"Giữ lại", "Xoá tất cả"},
                which -> {
                    if (which == 1) deleteThenScan(snapshot);
                });
    }

    /** Xoá ngoài luồng giao diện rồi quét lại. */
    private void deleteThenScan(List<File> victims) {
        hint.setText("Đang xoá…");
        final int mine = ++job;
        new Thread(() -> {
            JunkScanner.delete(victims);
            runOnUiThread(() -> {
                if (isDestroyed() || mine != job) return;
                scan();
            });
        }).start();
    }
}
