package io.github.phutrung08.glassdeck;

import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Hệ thống bảng kính nổi trên màn hình chính: một lớp mờ phủ nhẹ phía sau và đúng một bảng tại một lúc.
 * Bảng không bao giờ đè lên hàng nút phía trên hay khay app (trừ khi nằm trên khay).
 * Khi một bảng thay bảng khác thì chỗ trỏ ban đầu được giữ lại để trả về lúc đóng hẳn.
 */
final class OverlayHost {
    /** Bảng nằm giữa khoảng trống giữa hàng nút và khay, hoặc ngay phía trên khay. */
    enum Zone { CENTER, ABOVE_TRAY }

    private static final int STRAY_CLICK_MS = 350;

    private final HomeActivity h;
    private View overlay, focusBefore;
    private Runnable onBack;
    private long shownAt;

    OverlayHost(HomeActivity h) {
        this.h = h;
    }

    boolean isOpen() {
        return overlay != null;
    }

    /** Bỏ qua lần bấm lọt sang từ hộp thoại vừa đóng (vd. hộp thoại xin quyền của hệ thống). */
    boolean guard() {
        return SystemClock.uptimeMillis() - shownAt < STRAY_CLICK_MS;
    }

    /** Chiều cao tối đa của một bảng nằm trong khoảng trống giữa hàng nút và khay. */
    int maxHeight() {
        return h.trayTop() - h.pillsBottom() - 2 * gap();
    }

    private int gap() {
        return GlassUi.dp(h, 12);
    }

    /**
     * @param panel   bảng đã dựng
     * @param width   chiều rộng bảng (px)
     * @param animate hiện mờ dần và bật chặn bấm nhầm; false khi dựng lại cùng một bảng
     * @param focus   view nhận focus đầu tiên (null = không đặt)
     * @param back    việc làm khi bấm Back (null = chỉ đóng)
     */
    void show(View panel, int width, Zone zone, boolean animate, View focus, Runnable back) {
        final boolean fresh = overlay == null;
        if (fresh) {
            focusBefore = h.getCurrentFocus();
        } else {
            h.root().removeView(overlay);
        }
        h.thumbs().cancelPending();
        onBack = back;
        shownAt = SystemClock.uptimeMillis();

        FrameLayout scrim = new FrameLayout(h);
        scrim.setBackgroundColor(0x33000000);
        scrim.setClickable(true);

        int top = h.pillsBottom() + gap();
        int bottom = h.trayTop() - (zone == Zone.CENTER ? gap() : GlassUi.dp(h, 14));
        FrameLayout.LayoutParams lpZone = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bottom - top);
        lpZone.topMargin = top;
        FrameLayout area = new FrameLayout(h);
        area.addView(panel, new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT,
                zone == Zone.CENTER ? Gravity.CENTER : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        scrim.addView(area, lpZone);

        h.content().setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay = scrim;
        h.root().addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (animate && fresh) {
            scrim.setAlpha(0f);
            scrim.animate().alpha(1f).setDuration(160).start();
        }
        if (focus != null) focus.requestFocus();
    }

    void back() {
        if (onBack != null) onBack.run();
        else close();
    }

    void close() {
        if (overlay == null) return;
        h.thumbs().cancelPending();
        h.root().removeView(overlay);
        overlay = null;
        onBack = null;
        h.content().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        View prev = focusBefore;
        focusBefore = null;
        if (prev != null && prev.isAttachedToWindow()) prev.requestFocus();
        else h.focusLast();
    }
}
