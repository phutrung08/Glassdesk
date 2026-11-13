package io.github.phutrung08.glassdeck;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu dạng danh sách trong bảng kính: [biểu tượng] nhãn ...... giá trị / ô điều khiển.
 * Dùng cho mọi menu một cột của Glassdeck (cài đặt, ngôn ngữ, menu app, cổng vào, duyệt thư mục).
 */
final class GlassMenu {
    /** Một dòng. Mọi trường đều tuỳ chọn trừ nhãn. */
    static final class Row {
        final CharSequence label;
        final Runnable action;
        int icon;                    // biểu tượng trong ô vuông ở đầu dòng
        String code;                 // hoặc mã chữ mờ (vd. "VI")
        View leading;                // hoặc view tuỳ ý (ảnh thu nhỏ)
        CharSequence value;          // chữ phụ bên phải
        int trailingIcon;            // biểu tượng cuối dòng (mũi tên, dấu tích...)
        View trailing;               // hoặc ô điều khiển (công tắc, thanh nhỏ)
        boolean keepOpen;            // true: không đóng menu trước khi chạy action

        Row(CharSequence label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        Row icon(int res) { icon = res; return this; }
        Row code(String c) { code = c; return this; }
        Row leading(View v) { leading = v; return this; }
        Row value(CharSequence v) { value = v; return this; }
        Row chevron() { trailingIcon = R.drawable.ic_chevron_right; return this; }
        Row trailingIcon(int res) { trailingIcon = res; return this; }
        Row trailing(View v) { trailing = v; return this; }
        Row keepOpen() { keepOpen = true; return this; }
    }

    private final HomeActivity h;
    private final CharSequence title;
    private final List<Row> rows = new ArrayList<>();
    private int focus;
    private boolean animate = true;
    private View footer;
    private Runnable onBack;
    private int widthPercent = 46;

    GlassMenu(HomeActivity h, CharSequence title) {
        this.h = h;
        this.title = title;
    }

    GlassMenu add(Row r) { rows.add(r); return this; }
    GlassMenu rows(List<Row> list) { rows.addAll(list); return this; }
    GlassMenu focus(int index) { focus = index; return this; }
    /** false khi dựng lại cùng một menu (vd. sau khi chép hình): không mờ dần, không chặn bấm nhầm. */
    GlassMenu animate(boolean on) { animate = on; return this; }
    GlassMenu footer(View v) { footer = v; return this; }
    GlassMenu onBack(Runnable r) { onBack = r; return this; }
    GlassMenu widthPercent(int p) { widthPercent = p; return this; }

    void show() {
        final OverlayHost overlays = h.overlays();
        LinearLayout panel = new LinearLayout(h);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(GlassUi.panelBackground(panel));
        panel.setPadding(GlassUi.dp(h, 12), GlassUi.dp(h, 14), GlassUi.dp(h, 10), GlassUi.dp(h, 12));

        GlassText head = GlassUi.title(h, title);
        head.setPadding(GlassUi.dp(h, 14), 0, GlassUi.dp(h, 14), GlassUi.dp(h, 8));
        panel.addView(head);
        head.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int used = head.getMeasuredHeight() + GlassUi.dp(h, 26);
        if (footer != null) {
            footer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            used += footer.getMeasuredHeight();
        }

        MaxHeightScroll list = GlassUi.scroll(h, Math.max(GlassUi.dp(h, 90), overlays.maxHeight() - used));
        LinearLayout col = new LinearLayout(h);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, GlassUi.dp(h, 8), 0);
        View first = null;
        for (int i = 0; i < rows.size(); i++) {
            View v = rowView(rows.get(i));
            col.addView(v, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (first == null || i == focus) first = v;
        }
        list.addView(col);
        panel.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (footer != null) panel.addView(footer);

        int width = h.getResources().getDisplayMetrics().widthPixels * widthPercent / 100;
        overlays.show(panel, width, OverlayHost.Zone.CENTER, animate, first, onBack);
    }

    private View rowView(Row r) {
        LinearLayout v = new LinearLayout(h);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setBackground(Ui.focusBackground(h, Ui.solid(h, 0x00000000, 12), 12));
        v.setPadding(GlassUi.dp(h, 12), GlassUi.dp(h, 7), GlassUi.dp(h, 14), GlassUi.dp(h, 7));
        GlassUi.focusable(v);

        View lead = null;
        if (r.leading != null) {
            lead = r.leading;
        } else if (r.icon != 0) {
            lead = GlassUi.iconBadge(h, r.icon);
        } else if (r.code != null) {
            GlassText code = GlassUi.rowText(h, r.code, 13, GlassUi.TEXT_DIM);
            code.setTypeface(Ui.medium());
            code.setMinWidth(GlassUi.dp(h, 26));
            lead = code;
        }
        if (lead != null) {
            LinearLayout.LayoutParams lp = lead.getLayoutParams() instanceof LinearLayout.LayoutParams
                    ? (LinearLayout.LayoutParams) lead.getLayoutParams()
                    : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = GlassUi.dp(h, 12);
            v.addView(lead, lp);
        }

        v.addView(GlassUi.rowText(h, r.label, 17, GlassUi.TEXT),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (r.value != null) {
            GlassText value = GlassUi.rowText(h, r.value, 15, GlassUi.TEXT_SUB);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = GlassUi.dp(h, 12);
            v.addView(value, lp);
        }
        View tail = r.trailing;
        if (tail == null && r.trailingIcon != 0) {
            tail = GlassUi.icon(h, r.trailingIcon, 20, GlassUi.ICON_DIM, true);
        }
        if (tail != null) {
            LinearLayout.LayoutParams lp = tail.getLayoutParams() instanceof LinearLayout.LayoutParams
                    ? (LinearLayout.LayoutParams) tail.getLayoutParams()
                    : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = GlassUi.dp(h, 10);
            v.addView(tail, lp);
        }

        final boolean guarded = animate;
        v.setOnClickListener(x -> {
            if (guarded && h.overlays().guard()) return;
            if (!r.keepOpen) h.overlays().close();
            r.action.run();
        });
        return v;
    }
}
