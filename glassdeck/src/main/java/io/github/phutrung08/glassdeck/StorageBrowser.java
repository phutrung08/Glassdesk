package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Sizes;
import io.github.phutrung08.common.Ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Duyệt bộ nhớ tivi và USB để thêm hình nền: danh sách nơi lấy hình, rồi từng thư mục
 * (thư mục con trước, hình sau). Hình được chép vào thư mục riêng của app nên rút USB vẫn dùng được.
 * Back đi lên một cấp; ở danh sách nơi lấy hình thì quay về bảng hình nền.
 */
final class StorageBrowser {
    private final HomeActivity h;

    StorageBrowser(HomeActivity h) {
        this.h = h;
    }

    private static File volumeDir(StorageVolume v) {
        if (Build.VERSION.SDK_INT >= 30) return v.getDirectory();
        if (v.isPrimary()) return Environment.getExternalStorageDirectory();
        return v.getUuid() == null ? null : new File("/storage/" + v.getUuid());
    }

    /** Danh sách nơi lấy hình: bộ nhớ trong của tivi và các USB đang cắm. */
    void showRoots() {
        List<File> dirs = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Boolean> removable = new ArrayList<>();
        StorageManager sm = (StorageManager) h.getSystemService(Context.STORAGE_SERVICE);
        for (StorageVolume v : sm.getStorageVolumes()) {
            String state = v.getState();
            if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) continue;
            File d = volumeDir(v);
            if (d == null || !d.canRead()) continue;
            dirs.add(d);
            removable.add(v.isRemovable());
            names.add(h.getString(v.isRemovable() ? R.string.root_usb : R.string.root_internal, v.getDescription(h)));
        }
        if (dirs.isEmpty()) {
            h.toastLong(R.string.toast_no_storage);
            return;
        }
        boolean hasUsb = removable.contains(Boolean.TRUE);
        GlassMenu menu = new GlassMenu(h, h.getString(hasUsb ? R.string.roots_title : R.string.roots_title_no_usb))
                .onBack(() -> h.wallpaperPicker().reopen(null));
        for (int i = 0; i < dirs.size(); i++) {
            final File d = dirs.get(i);
            final String name = names.get(i);
            menu.add(new GlassMenu.Row(name, () -> browse(d, d, name, 0))
                    .icon(removable.get(i) ? R.drawable.ic_usb : R.drawable.ic_tv).keepOpen());
        }
        menu.show();
    }

    /**
     * Một thư mục. Bấm OK vào hình để chép vào bộ hình nền (dấu tích là hình đã có).
     * @param focus dòng chọn sẵn; khác 0 nghĩa là dựng lại sau khi chép nên không chặn bấm nhầm.
     */
    private void browse(File root, File dir, String rootLabel, int focus) {
        List<File> folders = new ArrayList<>(), images = new ArrayList<>();
        File[] all = dir.listFiles();
        if (all != null) {
            for (File f : all) {
                if (f.getName().startsWith(".")) continue;
                if (f.isDirectory()) {
                    if (!(dir.equals(root) && f.getName().equals("Android"))) folders.add(f);
                } else if (Wallpapers.isImage(f)) {
                    images.add(f);
                }
            }
        }
        Collections.sort(folders, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));
        Collections.sort(images, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));

        List<GlassMenu.Row> rows = new ArrayList<>();
        Runnable up;
        if (dir.equals(root)) {
            up = this::showRoots;
        } else {
            File parent = dir.getParentFile();
            up = () -> browse(root, parent, rootLabel, 0);
        }
        rows.add(new GlassMenu.Row(h.getString(R.string.browse_back), up)
                .icon(R.drawable.ic_arrow_back).keepOpen());
        if (images.size() > 1) {
            rows.add(new GlassMenu.Row(
                    h.getResources().getQuantityString(R.plurals.browse_add_all, images.size(), images.size()),
                    () -> copyImages(images, () -> browse(root, dir, rootLabel, 1)))
                    .icon(R.drawable.ic_add).keepOpen());
        }
        for (File f : folders) {
            rows.add(new GlassMenu.Row(f.getName(), () -> browse(root, f, rootLabel, 0))
                    .icon(R.drawable.ic_folder).keepOpen());
        }
        for (File f : images) {
            final int index = rows.size();
            boolean added = Wallpapers.alreadyAdded(h, f);
            rows.add(new GlassMenu.Row(f.getName(),
                    () -> copyImages(Collections.singletonList(f), () -> browse(root, dir, rootLabel, index)))
                    .leading(thumb(f)).value(Sizes.format(f.length()))
                    .trailingIcon(added ? R.drawable.ic_check : R.drawable.ic_add).keepOpen());
        }
        if (folders.isEmpty() && images.isEmpty()) {
            rows.add(new GlassMenu.Row(h.getString(R.string.browse_empty), () -> browse(root, dir, rootLabel, 0))
                    .keepOpen());
        }

        new GlassMenu(h, dir.equals(root) ? rootLabel : dir.getName())
                .rows(rows).focus(Math.min(focus, rows.size() - 1)).animate(focus == 0).onBack(up).show();
    }

    /** Ảnh thu nhỏ 52 x 29 dp bo góc ở đầu dòng. */
    private View thumb(File f) {
        FrameLayout box = new FrameLayout(h);
        box.setBackground(Ui.solid(h, 0x40000000, 5));
        final int radius = GlassUi.dp(h, 5);
        box.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        box.setClipToOutline(true);
        ThumbView img = new ThumbView(h, h.thumbs());
        box.addView(img, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        img.setFile(f);
        box.setLayoutParams(new LinearLayout.LayoutParams(GlassUi.dp(h, 52), GlassUi.dp(h, 29)));
        return box;
    }

    private void copyImages(List<File> files, Runnable then) {
        if (files.size() > 1) {
            h.toast(h.getResources().getQuantityString(R.plurals.toast_copying, files.size(), files.size()));
        }
        h.runBackground(() -> {
            int ok = 0;
            for (File f : files) if (Wallpapers.copyIn(h, f)) ok++;
            final int done = ok;
            h.runOnUiThread(() -> {
                if (h.isDestroyed()) return;
                h.toast(done == files.size()
                        ? h.getResources().getQuantityString(R.plurals.toast_copied, done, done)
                        : h.getString(R.string.toast_copied_partial, done, files.size()));
                // Người dùng đã thoát ra ngoài trong lúc chép thì không mở lại danh sách.
                if (h.overlays().isOpen()) then.run();
            });
        });
    }
}
