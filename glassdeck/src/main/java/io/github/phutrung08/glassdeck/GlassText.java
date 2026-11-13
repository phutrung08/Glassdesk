package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.widget.TextView;

import io.github.phutrung08.common.Ui;

/**
 * Chữ trong menu. Khi nền menu trong (GlassUi.shadowOn) chữ có bóng mờ để vẫn đọc được trên
 * hình nền sáng; dòng đang chọn có nền trắng nên không thêm bóng.
 */
final class GlassText extends TextView {
    private static final int SHADOW_COLOR = 0x99000000;
    private boolean shadowShown;

    GlassText(Context c) {
        super(c);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        refreshShadow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshShadow();
    }

    void refreshShadow() {
        boolean focused = false;
        for (int s : getDrawableState()) if (s == android.R.attr.state_focused) focused = true;
        boolean want = GlassUi.shadowOn && !focused;
        if (want == shadowShown) return;
        shadowShown = want;
        if (want) setShadowLayer(Ui.dp(getContext(), 6), 0, 0, SHADOW_COLOR);
        else setShadowLayer(0, 0, 0, 0);
    }
}
