package io.github.phutrung08.glassdeck;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Ui;

/**
 * Bảng nhỏ ngay phía trên khay để chỉnh một mức 0..max theo bước 10. Trái/Phải đổi giá trị và áp dụng
 * ngay (khay, nút hoặc chính bảng này đổi theo). OK giữ giá trị; Back trả về giá trị cũ.
 * Cả hai đều quay lại menu Cài đặt, đúng dòng đã mở bảng này.
 */
final class SliderPanel {
    private static final int STEP = 10;

    private final HomeActivity h;

    SliderPanel(HomeActivity h) {
        this.h = h;
    }

    void show(int titleRes, String key, int min, int max, int dflt, int returnRow) {
        final Prefs prefs = h.prefs();
        final int old = Levels.snap(prefs.getInt(key, dflt), min, max, STEP);
        final int[] cur = {old};

        LinearLayout panel = new LinearLayout(h);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(GlassUi.panelBackground(panel));
        panel.setPadding(GlassUi.dp(h, 28), GlassUi.dp(h, 18), GlassUi.dp(h, 28), GlassUi.dp(h, 14));
        GlassUi.focusable(panel);

        GlassText title = GlassUi.title(h, h.getString(titleRes));
        panel.addView(title);

        final GlassText value = GlassUi.text(h, h.getString(R.string.percent, old), 44, Ui.WHITE);
        value.setTypeface(Ui.medium());
        value.setGravity(Gravity.CENTER);
        panel.addView(value, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final SliderView slider = new SliderView(h);
        slider.setRange(min, max, old);
        panel.addView(slider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GlassText hint = GlassUi.text(h, h.getString(R.string.slider_hint), 13, Ui.DIM);
        hint.setGravity(Gravity.CENTER);
        panel.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        panel.setOnKeyListener((v, code, ev) -> {
            if (code != KeyEvent.KEYCODE_DPAD_LEFT && code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
            if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                int next = Levels.step(cur[0], code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1, min, max, STEP);
                if (next != cur[0]) {
                    cur[0] = next;
                    prefs.putInt(key, next);          // áp dụng ngay; Back sẽ trả về giá trị cũ
                    h.refreshFrost();
                    value.setText(h.getString(R.string.percent, next));
                    slider.setValue(next);
                    GlassUi.refreshShadows(panel);
                }
            }
            return true;
        });
        panel.setOnClickListener(v -> {
            if (h.overlays().guard()) return;
            h.settings().show(returnRow, true);
        });

        int width = h.getResources().getDisplayMetrics().widthPixels * 52 / 100;
        h.overlays().show(panel, width, OverlayHost.Zone.ABOVE_TRAY, true, panel, () -> {
            prefs.putInt(key, old);
            h.refreshFrost();
            h.settings().show(returnRow, true);
        });
    }
}
