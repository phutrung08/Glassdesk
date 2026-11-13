package io.github.phutrung08.glassdeck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Ui;

/**
 * Nút nhỏ bo tròn: biểu tượng tuỳ chọn + chữ. Khi remote trỏ tới thì nền trắng.
 * Chip "đang chọn" có nền trắng cố định kèm dấu tích để phân biệt với chip chỉ đang được trỏ tới.
 */
@SuppressLint("ViewConstructor")   // chỉ dựng bằng code, không có trong file layout
final class Chip extends LinearLayout {
    private final ImageView lead, check;
    private final GlassText label;
    private boolean selected;

    /** @param iconRes biểu tượng đầu chip, 0 nếu không có; @param trailingRes biểu tượng cuối chip, 0 nếu không có. */
    Chip(Context c, int iconRes, CharSequence text, int trailingRes, boolean focusable) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(GlassUi.dp(c, 14), GlassUi.dp(c, 7), GlassUi.dp(c, 14), GlassUi.dp(c, 7));
        if (focusable) GlassUi.focusable(this);

        check = GlassUi.icon(c, R.drawable.ic_check, 16, ColorStateList.valueOf(Ui.INK), false);
        check.setVisibility(GONE);
        ((LayoutParams) check.getLayoutParams()).rightMargin = GlassUi.dp(c, 4);
        addView(check);

        lead = iconRes == 0 ? null : GlassUi.icon(c, iconRes, 18, GlassUi.ICON, true);
        if (lead != null) {
            ((LayoutParams) lead.getLayoutParams()).rightMargin = GlassUi.dp(c, 6);
            addView(lead);
        }
        label = GlassUi.rowText(c, text, 15, GlassUi.TEXT);
        addView(label);
        if (trailingRes != 0) {
            ImageView t = GlassUi.icon(c, trailingRes, 18, GlassUi.ICON, true);
            ((LayoutParams) t.getLayoutParams()).leftMargin = GlassUi.dp(c, 2);
            addView(t);
        }
        setSelectedChip(false);
    }

    void setLabel(CharSequence s) {
        label.setText(s);
    }

    void setSelectedChip(boolean on) {
        selected = on;
        Context c = getContext();
        setBackground(on ? Ui.solid(c, Ui.WHITE, 40)
                : Ui.focusBackground(c, Ui.glass(c, 0x24FFFFFF, 40), 40));
        check.setVisibility(on ? VISIBLE : GONE);
        // Chip đang chọn nền trắng cố định nên chữ luôn là màu đậm.
        label.setTextColor(on ? ColorStateList.valueOf(Ui.INK) : GlassUi.TEXT);
        if (lead != null) lead.setImageTintList(on ? ColorStateList.valueOf(Ui.INK) : GlassUi.ICON);
    }

    boolean isSelectedChip() {
        return selected;
    }

    void onClick(Runnable r) {
        setOnClickListener((View v) -> r.run());
    }
}
