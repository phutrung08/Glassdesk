package io.github.phutrung08.common;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


/** Bộ giao diện dùng chung, cùng phong cách với Clean Home: nền xanh đậm, kính trong, chọn = nền trắng. */
public final class Ui {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int SUB = 0xB8FFFFFF;          // chữ phụ
    public static final int DIM = 0x80FFFFFF;          // chữ mờ
    public static final int GLASS = 0x29FFFFFF;        // kính trong: trắng 16%
    public static final int GLASS_EDGE = 0x47FFFFFF;   // viền sáng
    public static final int PANEL = 0xE6102219;        // hộp thoại: xanh đậm 90%
    public static final int INK = 0xFF0C2A1C;          // chữ trên nền trắng
    public static final int INK_SUB = 0xB30C2A1C;

    public interface Pick { void onPick(int index); }

    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static Drawable background() {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0F5A34, 0xFF0C3A25, 0xFF07120D});
    }

    public static GradientDrawable glass(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(dp(c, 1), GLASS_EDGE);
        return g;
    }

    public static GradientDrawable solid(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Nền trắng khi remote trỏ tới, nền thường khi không. */
    public static Drawable focusBackground(Context c, Drawable normal, float radiusDp) {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, solid(c, WHITE, radiusDp));
        bg.addState(new int[]{}, normal);
        return bg;
    }

    public static ColorStateList focusColor(int focused, int normal) {
        return new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{focused, normal});
    }

    public static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    public static Typeface medium() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static String size(long bytes) {
        return Sizes.format(bytes);
    }

    /** Nút kính bo tròn. */
    public static TextView pill(Context c, String label, View.OnClickListener onClick) {
        TextView b = new TextView(c);
        b.setId(View.generateViewId());
        b.setText(label);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTextColor(focusColor(INK, WHITE));
        b.setBackground(focusBackground(c, glass(c, GLASS, 40), 40));
        b.setPadding(dp(c, 22), dp(c, 9), dp(c, 22), dp(c, 9));
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnClickListener(onClick);
        return b;
    }

    /** Khung màn hình phụ: nền, tiêu đề, dòng mô tả. Trả về cột để thêm nội dung. */
    public static LinearLayout screen(Activity a, String title, TextView hint) {
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackground(background());
        col.setPadding(dp(a, 48), dp(a, 30), dp(a, 48), dp(a, 30));
        TextView t = text(a, title, 26, WHITE);
        t.setTypeface(medium());
        col.addView(t);
        hint.setPadding(0, dp(a, 4), 0, dp(a, 16));
        col.addView(hint);
        a.setContentView(col);
        return col;
    }

    /** Tấm kính chứa một danh sách cuộn dọc. Trả về cột bên trong để thêm các dòng; tấm kính là getPanel(cột). */
    public static LinearLayout glassList(Context c, LinearLayout parent) {
        FrameLayout panel = new FrameLayout(c);
        panel.setBackground(glass(c, GLASS, 22));
        panel.setClipToOutline(true);
        panel.setPadding(dp(c, 10), dp(c, 10), dp(c, 6), dp(c, 10));

        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            // Android 10 trở lên mới cho đổi màu thanh cuộn; máy cũ giữ thanh cuộn mặc định.
            scroll.setVerticalScrollbarThumbDrawable(solid(c, 0xBFFFFFFF, 2));
            scroll.setVerticalScrollbarTrackDrawable(solid(c, 0x24FFFFFF, 2));
        }
        scroll.setScrollBarSize(dp(c, 4));
        scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // Dòng ở mép trên và dưới mờ dần để báo danh sách còn tiếp.
        scroll.setVerticalFadingEdgeEnabled(true);
        scroll.setFadingEdgeLength(dp(c, 28));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, dp(c, 8), 0);
        scroll.addView(col);
        panel.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(panel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        col.setTag(R.id.glass_panel, panel);
        return col;
    }

    /** Tấm kính bao quanh một cột do glassList() tạo ra. */
    public static View getPanel(LinearLayout list) {
        return (View) list.getTag(R.id.glass_panel);
    }

    private static TextView rowText(Context c, String s, float sp, int focused, int normal) {
        TextView t = text(c, s, sp, normal);
        t.setTextColor(focusColor(focused, normal));
        t.setDuplicateParentStateEnabled(true);
        t.setSingleLine(true);
        return t;
    }

    /** Một dòng trong danh sách: tiêu đề + dòng phụ bên trái, số liệu bên phải. Dòng đang chọn có nền trắng. */
    public static View listRow(Context c, String title, String sub, String right, String rightSub,
                        View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(focusBackground(c, solid(c, 0x00000000, 12), 12));
        row.setPadding(dp(c, 16), dp(c, 9), dp(c, 16), dp(c, 9));
        row.setFocusable(true);
        row.setClickable(true);
        row.setOnClickListener(onClick);

        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setDuplicateParentStateEnabled(true);
        left.addView(rowText(c, title, 17, INK, WHITE));
        if (sub != null) left.addView(rowText(c, sub, 12, INK_SUB, DIM));
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout rightCol = new LinearLayout(c);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END);
        rightCol.setDuplicateParentStateEnabled(true);
        rightCol.addView(rowText(c, right, 17, INK, WHITE));
        if (rightSub != null) rightCol.addView(rowText(c, rightSub, 12, INK_SUB, SUB));
        row.addView(rightCol);
        return row;
    }

    /**
     * Hộp thoại kính ở giữa màn hình. Phần phía sau tối đi và không nhận phím cho tới khi đóng.
     * Bấm Back = đóng mà không chọn gì.
     */
    public static void dialog(Activity a, String title, String message, String[] buttons, Pick onPick) {
        final ViewGroup contentRoot = a.findViewById(android.R.id.content);
        final View under = contentRoot.getChildAt(0);
        final View prevFocus = a.getCurrentFocus();

        final FrameLayout scrim = new FrameLayout(a);
        scrim.setBackgroundColor(0x66000000);
        scrim.setClickable(true);

        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(glass(a, PANEL, 22));
        panel.setPadding(dp(a, 28), dp(a, 22), dp(a, 22), dp(a, 18));

        TextView head = text(a, title, 20, WHITE);
        head.setTypeface(medium());
        panel.addView(head);

        TextView body = text(a, message, 15, SUB);
        body.setLineSpacing(dp(a, 3), 1f);
        body.setPadding(0, dp(a, 10), dp(a, 6), dp(a, 18));
        panel.addView(body);

        final Runnable close = () -> {
            contentRoot.removeView(scrim);
            if (under instanceof ViewGroup) {
                ((ViewGroup) under).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }
            if (prevFocus != null && prevFocus.isAttachedToWindow()) prevFocus.requestFocus();
        };

        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.END);
        View first = null;
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            TextView b = pill(a, buttons[i], v -> {
                close.run();
                if (onPick != null) onPick.onPick(index);
            });
            b.setOnKeyListener((v, code, ev) -> {
                if (code != KeyEvent.KEYCODE_BACK) return false;
                if (ev.getAction() == KeyEvent.ACTION_UP) close.run();
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.leftMargin = dp(a, 10);
            bar.addView(b, lp);
            if (first == null) first = b;
        }
        panel.addView(bar);

        int width = a.getResources().getDisplayMetrics().widthPixels * 56 / 100;
        scrim.addView(panel, new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        if (under instanceof ViewGroup) {
            ((ViewGroup) under).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        contentRoot.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(160).start();
        if (first != null) first.requestFocus();
    }
}
