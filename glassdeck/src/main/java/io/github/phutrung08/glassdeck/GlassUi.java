package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Ui;

/** Các mảnh giao diện kính dùng chung cho menu và hộp thoại của Glassdeck. */
final class GlassUi {
    /** Bật khi nền menu đủ trong (xem Levels.menuShadow); đặt bởi HomeActivity. */
    static boolean shadowOn;

    /** Màu chữ phụ trên nền tối và trên nền trắng khi đang chọn. */
    static final ColorStateList TEXT = Ui.focusColor(Ui.INK, Ui.WHITE);
    static final ColorStateList TEXT_SUB = Ui.focusColor(Ui.INK_SUB, Ui.SUB);
    static final ColorStateList TEXT_DIM = Ui.focusColor(Ui.INK_SUB, Ui.DIM);
    static final ColorStateList ICON = Ui.focusColor(Ui.INK, Ui.WHITE);
    static final ColorStateList ICON_DIM = Ui.focusColor(Ui.INK, 0xB3FFFFFF);

    private GlassUi() {}

    static int dp(Context c, float v) {
        return Ui.dp(c, v);
    }

    static GlassText text(Context c, CharSequence s, float sp, int color) {
        GlassText t = new GlassText(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    /** Chữ trong một dòng có thể chọn: đổi màu theo trạng thái của dòng cha. */
    static GlassText rowText(Context c, CharSequence s, float sp, ColorStateList colors) {
        GlassText t = text(c, s, sp, Ui.WHITE);
        t.setTextColor(colors);
        t.setDuplicateParentStateEnabled(true);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return t;
    }

    static GlassText title(Context c, CharSequence s) {
        GlassText t = text(c, s, 19, Ui.WHITE);
        t.setTypeface(Ui.medium());
        return t;
    }

    /** Biểu tượng tô màu; với duplicate = true thì đổi màu theo dòng cha khi được chọn. */
    static ImageView icon(Context c, int res, float sizeDp, ColorStateList tint, boolean duplicate) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setImageTintList(tint);
        v.setDuplicateParentStateEnabled(duplicate);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return v;
    }

    /** Biểu tượng nằm trong ô vuông bo góc 26 dp ở đầu dòng. */
    static View iconBadge(Context c, int res) {
        FrameLayout box = new FrameLayout(c);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, Ui.solid(c, 0x140C2A1C, 7));
        bg.addState(new int[]{}, Ui.solid(c, 0x24FFFFFF, 7));
        box.setBackground(bg);
        box.setDuplicateParentStateEnabled(true);
        ImageView iv = icon(c, res, 18, ICON, true);
        box.addView(iv, new FrameLayout.LayoutParams(dp(c, 18), dp(c, 18), Gravity.CENTER));
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 26), dp(c, 26)));
        return box;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x2EFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1));
        lp.setMargins(dp(c, 6), dp(c, 8), dp(c, 6), dp(c, 8));
        v.setLayoutParams(lp);
        return v;
    }

    /** Tắt lớp sáng mặc định của hệ thống trên view nhận focus do mình tự vẽ trạng thái chọn. */
    static <T extends View> T focusable(T v) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setDefaultFocusHighlightEnabled(false);
        return v;
    }

    /** Vùng cuộn dọc kiểu kính: thanh cuộn mảnh luôn hiện, mép trên và dưới mờ dần. */
    static MaxHeightScroll scroll(Context c, int maxHeight) {
        MaxHeightScroll s = new MaxHeightScroll(c);
        s.maxHeight = maxHeight;
        s.setVerticalScrollBarEnabled(true);
        s.setScrollbarFadingEnabled(false);
        if (Build.VERSION.SDK_INT >= 29) {
            s.setVerticalScrollbarThumbDrawable(Ui.solid(c, 0xBFFFFFFF, 2));
            s.setVerticalScrollbarTrackDrawable(Ui.solid(c, 0x24FFFFFF, 2));
        }
        s.setScrollBarSize(dp(c, 4));
        s.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        s.setVerticalFadingEdgeEnabled(true);
        s.setFadingEdgeLength(dp(c, 28));
        s.setOverScrollMode(View.OVER_SCROLL_NEVER);
        return s;
    }

    static Drawable panelBackground(View host) {
        return new Frost.Glass(host, dp(host.getContext(), 22), true, dp(host.getContext(), 1));
    }

    static void setShadow(boolean on) {
        shadowOn = on;
    }

    /** Cập nhật bóng chữ của mọi GlassText trong một cây view đang hiện. */
    static void refreshShadows(View v) {
        if (v instanceof GlassText) ((GlassText) v).refreshShadow();
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) refreshShadows(g.getChildAt(i));
        }
    }
}
