package io.github.phutrung08.glassdeck;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Ui;

/**
 * Chọn thời gian đổi hình: vòng tròn bên trái cho thấy vị trí trong các mốc, bên phải là hàng chip.
 * Trái/Phải đổi mốc, OK lưu, Back huỷ. Cả hai quay lại bảng hình nền.
 */
final class IntervalPicker {
    private final HomeActivity h;

    IntervalPicker(HomeActivity h) {
        this.h = h;
    }

    void show() {
        final Prefs prefs = h.prefs();
        final int[] index = {Intervals.indexOf(prefs.interval())};

        LinearLayout panel = new LinearLayout(h);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setBackground(GlassUi.panelBackground(panel));
        panel.setPadding(GlassUi.dp(h, 28), GlassUi.dp(h, 20), GlassUi.dp(h, 28), GlassUi.dp(h, 20));
        GlassUi.focusable(panel);

        final DialView dial = new DialView(h);
        panel.addView(dial);

        LinearLayout side = new LinearLayout(h);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(GlassUi.dp(h, 28), 0, 0, 0);
        side.addView(GlassUi.title(h, h.getString(R.string.interval_title)));

        // Hai hàng ba chip: các mốc nằm vừa trong bảng.
        final Chip[] chips = new Chip[Intervals.VALUES.length];
        LinearLayout chipRow = null;
        for (int i = 0; i < chips.length; i++) {
            if (i % 3 == 0) {
                chipRow = new LinearLayout(h);
                chipRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpRow.topMargin = GlassUi.dp(h, 10);
                side.addView(chipRow, lpRow);
            }
            chips[i] = new Chip(h, 0, Intervals.label(h, Intervals.VALUES[i]), 0, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i % 3 != 0) lp.leftMargin = GlassUi.dp(h, 8);
            chipRow.addView(chips[i], lp);
        }
        GlassText hint = GlassUi.text(h, h.getString(R.string.slider_hint), 13, Ui.DIM);
        hint.setPadding(0, GlassUi.dp(h, 14), 0, 0);
        side.addView(hint);
        panel.addView(side);

        applyIndex(dial, chips, index[0], false);

        panel.setOnKeyListener((v, code, ev) -> {
            if (code != KeyEvent.KEYCODE_DPAD_LEFT && code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
            if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                int next = Intervals.move(index[0], code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                if (next != index[0]) {
                    index[0] = next;
                    applyIndex(dial, chips, next, true);
                }
            }
            return true;
        });
        panel.setOnClickListener(v -> {
            if (h.overlays().guard()) return;
            prefs.putInt("wp_interval", Intervals.VALUES[index[0]]);
            h.wallpaperPicker().reopen(null);
        });

        int width = h.getResources().getDisplayMetrics().widthPixels * 56 / 100;
        h.overlays().show(panel, width, OverlayHost.Zone.CENTER, true, panel, () -> h.wallpaperPicker().reopen(null));
    }

    private void applyIndex(DialView dial, Chip[] chips, int index, boolean animate) {
        int sec = Intervals.VALUES[index];
        dial.set(Intervals.VALUES.length, index, String.valueOf(Intervals.amount(sec)),
                Intervals.unitLabel(h, sec), animate);
        for (int i = 0; i < chips.length; i++) chips[i].setSelectedChip(i == index);
    }
}
