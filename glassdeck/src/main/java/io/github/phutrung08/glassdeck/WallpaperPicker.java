package io.github.phutrung08.glassdeck;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bảng chọn hình nền: hàng đầu có hai chip chế độ ("Một hình" / "Tự đổi") và chip thời gian đổi,
 * bên dưới là lưới 5 cột. Lưới cuộn dọc khi có nhiều hình. Trỏ tới ô nào thì xem trước ngay phía sau bảng.
 *
 * Chế độ "Một hình": OK chọn hình rồi đóng. Chế độ "Tự đổi": OK thêm/bớt hình khỏi danh sách đổi,
 * Back lưu danh sách. Back khi chưa chọn gì thì trả hình nền về như cũ.
 */
final class WallpaperPicker {
    private static final int COLS = 5;

    private final HomeActivity h;
    private boolean multiMode;
    private Set<String> chosen = new HashSet<>();
    private Set<String> initialChosen = new HashSet<>();
    private String lastFocusKey;
    private Chip chipSingle, chipAuto, chipInterval;
    private View intervalGroup;
    private final List<WallTile> fileTiles = new ArrayList<>();

    WallpaperPicker(HomeActivity h) {
        this.h = h;
    }

    /** Mở từ menu Cài đặt: bắt đầu một lượt xem hình nền (tạm dừng tự đổi, nhớ hình đang dùng). */
    void open() {
        h.beginWallpaperBrowse();
        Prefs prefs = h.prefs();
        multiMode = Prefs.MODE_MULTI.equals(prefs.wallpaperMode());
        chosen = prefs.wallpaperMulti();
        initialChosen = new HashSet<>(chosen);
        lastFocusKey = h.appliedKey();
        build();
    }

    /** Dựng lại bảng sau khi quay về từ chọn thời gian hoặc duyệt bộ nhớ, giữ nguyên chế độ đang chỉnh. */
    void reopen(String focusKey) {
        if (focusKey != null) lastFocusKey = focusKey;
        build();
    }

    private void build() {
        final List<File> files = Wallpapers.list(h);
        final int screenW = h.getResources().getDisplayMetrics().widthPixels;
        final int panelW = screenW * 88 / 100;

        LinearLayout panel = new LinearLayout(h);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(GlassUi.panelBackground(panel));
        panel.setPadding(GlassUi.dp(h, 22), GlassUi.dp(h, 14), GlassUi.dp(h, 14), GlassUi.dp(h, 12));

        View header = header();
        panel.addView(header);
        header.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // Lưới: chừa chỗ cho phần phóng to của ô đang chọn và thanh cuộn.
        int sidePad = GlassUi.dp(h, 10), gap = GlassUi.dp(h, 14);
        int avail = panelW - GlassUi.dp(h, 22) - GlassUi.dp(h, 14) - 2 * sidePad - GlassUi.dp(h, 8);
        int tileW = (avail - (COLS - 1) * gap) / COLS;

        int maxH = h.overlays().maxHeight() - header.getMeasuredHeight() - GlassUi.dp(h, 26 + 8);
        MaxHeightScroll scroll = GlassUi.scroll(h, Math.max(GlassUi.dp(h, 120), maxH));
        scroll.setClipToPadding(false);
        LinearLayout col = new LinearLayout(h);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        col.setClipToPadding(false);
        col.setPadding(sidePad, GlassUi.dp(h, 8), sidePad + GlassUi.dp(h, 8), GlassUi.dp(h, 8));

        fileTiles.clear();
        final String applied = h.appliedKey();
        WallTile focusTile = null, firstTile = null;
        LinearLayout row = null;
        int total = files.size() + 2;
        for (int i = 0; i < total; i++) {
            WallTile t;
            if (i == 0) {
                t = new WallTile(h, WallTile.Kind.DEFAULT, null, h.getString(R.string.wallpaper_default), h.thumbs(), tileW);
                t.setApplied(applied.isEmpty());
            } else if (i == total - 1) {
                t = new WallTile(h, WallTile.Kind.ADD, null, h.getString(R.string.wp_add_tile), h.thumbs(), tileW);
            } else {
                File f = files.get(i - 1);
                t = new WallTile(h, WallTile.Kind.FILE, f, WallpaperNames.display(h, f.getName()), h.thumbs(), tileW);
                t.setApplied(f.getName().equals(applied));
                t.setBadge(multiMode && chosen.contains(f.getName()));
                fileTiles.add(t);
            }
            final WallTile tile = t;
            t.setOnClickListener(v -> onTileClick(tile));
            t.setOnFocusChangeListener((v, has) -> { if (has) onTileFocus(tile); });
            if (i % COLS == 0) {
                row = new LinearLayout(h);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpRow.bottomMargin = GlassUi.dp(h, 12);
                col.addView(row, lpRow);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i % COLS != 0) lp.leftMargin = gap;
            row.addView(t, lp);
            if (firstTile == null) firstTile = t;
            if (focusTile == null && keyOf(t).equals(lastFocusKey) && t.kind != WallTile.Kind.ADD) focusTile = t;
        }
        scroll.addView(col);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        applyMode();
        h.overlays().show(panel, panelW, OverlayHost.Zone.CENTER, true,
                focusTile != null ? focusTile : firstTile, this::onBack);
    }

    private static String keyOf(WallTile t) {
        return t.file == null ? "" : t.file.getName();
    }

    private View header() {
        LinearLayout bar = new LinearLayout(h);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(GlassUi.dp(h, 4), 0, GlassUi.dp(h, 8), GlassUi.dp(h, 6));

        bar.addView(GlassUi.title(h, h.getString(R.string.wp_title)));

        chipSingle = new Chip(h, 0, h.getString(R.string.wp_mode_single), 0, true);
        chipAuto = new Chip(h, 0, h.getString(R.string.wp_mode_auto), 0, true);
        chipSingle.onClick(() -> setMode(false));
        chipAuto.onClick(() -> setMode(true));
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpA.leftMargin = GlassUi.dp(h, 22);
        bar.addView(chipSingle, lpA);
        LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpB.leftMargin = GlassUi.dp(h, 8);
        bar.addView(chipAuto, lpB);

        bar.addView(new View(h), new LinearLayout.LayoutParams(0, 1, 1f));

        LinearLayout group = new LinearLayout(h);
        group.setOrientation(LinearLayout.HORIZONTAL);
        group.setGravity(Gravity.CENTER_VERTICAL);
        GlassText every = GlassUi.text(h, h.getString(R.string.wp_interval_label), 15, Ui.SUB);
        every.setPadding(0, 0, GlassUi.dp(h, 10), 0);
        group.addView(every);
        chipInterval = new Chip(h, 0, Intervals.label(h, h.prefs().interval()), R.drawable.ic_chevron_right, true);
        chipInterval.onClick(() -> {
            if (h.overlays().guard()) return;
            h.intervalPicker().show();
        });
        group.addView(chipInterval);
        intervalGroup = group;
        bar.addView(group);
        return bar;
    }

    private void setMode(boolean multi) {
        if (h.overlays().guard()) return;
        multiMode = multi;
        applyMode();
    }

    /** Cập nhật chip chế độ, chip thời gian (chỉ hiện ở "Tự đổi") và dấu tích trên các ô. */
    private void applyMode() {
        chipSingle.setSelectedChip(!multiMode);
        chipAuto.setSelectedChip(multiMode);
        intervalGroup.setVisibility(multiMode ? View.VISIBLE : View.GONE);
        for (WallTile t : fileTiles) t.setBadge(multiMode && chosen.contains(keyOf(t)));
    }

    private void onTileFocus(WallTile t) {
        if (t.kind == WallTile.Kind.ADD) return;
        lastFocusKey = keyOf(t);
        h.previewWallpaper(t.file);
    }

    private void onTileClick(WallTile t) {
        if (h.overlays().guard()) return;
        Prefs prefs = h.prefs();
        switch (t.kind) {
            case ADD:
                h.browseStart();
                return;
            case DEFAULT:
                if (multiMode) {
                    h.toast(R.string.toast_wp_default_not_in_auto);
                    return;
                }
                prefs.selectDefaultWallpaper();
                finishApplied();
                return;
            default:
                if (!multiMode) {
                    prefs.selectSingleWallpaper(t.file.getName());
                    finishApplied();
                    return;
                }
                String name = t.file.getName();
                if (!chosen.remove(name)) chosen.add(name);
                prefs.setWallpaperMulti(new HashSet<>(chosen));
                t.setBadge(chosen.contains(name));
        }
    }

    /** Đã chọn xong một hình: áp dụng, kết thúc lượt xem và quay về Cài đặt. */
    private void finishApplied() {
        h.endWallpaperBrowse(false);
        h.applyWallpaper(true);
        h.settings().show(SettingsMenu.WALLPAPER, true);
    }

    private void onBack() {
        Prefs prefs = h.prefs();
        boolean applied = false;
        if (multiMode) {
            int count = 0;
            for (File f : Wallpapers.list(h)) if (chosen.contains(f.getName())) count++;
            boolean unchanged = Prefs.MODE_MULTI.equals(prefs.wallpaperMode()) && chosen.equals(initialChosen);
            if (count == 0) {
                h.toast(R.string.toast_wp_none_selected);
            } else if (!unchanged) {
                prefs.selectMultiWallpaper();
                applied = true;
            }
        }
        if (applied) {
            h.endWallpaperBrowse(false);
            h.applyWallpaper(true);
        } else {
            h.endWallpaperBrowse(true);
        }
        h.settings().show(SettingsMenu.WALLPAPER, true);
    }
}
