package io.github.phutrung08.glassdeck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.github.phutrung08.common.Ui;

import java.io.File;

/** Một ô trong bảng hình nền: hình 16:9 bo góc, tên bên dưới, dấu tích khi được chọn, viền trắng khi đang dùng. */
@SuppressLint("ViewConstructor")   // chỉ dựng bằng code, không có trong file layout
final class WallTile extends LinearLayout {
    enum Kind { DEFAULT, FILE, ADD }

    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.6f);

    final Kind kind;
    final File file;                 // null trừ khi kind == FILE
    private final FrameLayout thumb;
    private final ImageView badge;

    WallTile(Context c, Kind kind, File file, CharSequence name, Thumbnails thumbs, int width) {
        super(c);
        this.kind = kind;
        this.file = file;
        setOrientation(VERTICAL);
        GlassUi.focusable(this);

        final int radius = GlassUi.dp(c, 10);
        thumb = new FrameLayout(c);
        thumb.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        thumb.setClipToOutline(true);
        switch (kind) {
            case DEFAULT:
                thumb.setBackground(Ui.background());
                break;
            case FILE:
                thumb.setBackground(Ui.solid(c, 0x59000000, 10));   // kính tối trong lúc chờ ảnh
                ThumbView img = new ThumbView(c, thumbs);
                thumb.addView(img, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                img.setFile(file);
                break;
            default: {
                GradientDrawable dashed = new GradientDrawable();
                dashed.setColor(0x14FFFFFF);
                dashed.setCornerRadius(radius);
                dashed.setStroke(GlassUi.dp(c, 2), 0x99FFFFFF, GlassUi.dp(c, 7), GlassUi.dp(c, 5));
                thumb.setBackground(dashed);
                ImageView plus = GlassUi.icon(c, R.drawable.ic_add, 32, ColorStateList.valueOf(Ui.WHITE), false);
                thumb.addView(plus, new FrameLayout.LayoutParams(
                        GlassUi.dp(c, 32), GlassUi.dp(c, 32), Gravity.CENTER));
            }
        }

        badge = new ImageView(c);
        badge.setImageResource(R.drawable.ic_check);
        badge.setImageTintList(ColorStateList.valueOf(Ui.WHITE));
        badge.setBackground(Ui.solid(c, 0xFF1DB954, 12));
        badge.setPadding(GlassUi.dp(c, 4), GlassUi.dp(c, 4), GlassUi.dp(c, 4), GlassUi.dp(c, 4));
        badge.setVisibility(GONE);
        FrameLayout.LayoutParams lpBadge = new FrameLayout.LayoutParams(
                GlassUi.dp(c, 24), GlassUi.dp(c, 24), Gravity.TOP | Gravity.END);
        lpBadge.setMargins(0, GlassUi.dp(c, 6), GlassUi.dp(c, 6), 0);
        thumb.addView(badge, lpBadge);
        addView(thumb, new LayoutParams(width, width * 9 / 16));

        GlassText label = GlassUi.rowText(c, name, 14, GlassUi.TEXT);
        label.setGravity(Gravity.CENTER);
        label.setBackground(Ui.focusBackground(c, Ui.solid(c, 0x00000000, 8), 8));
        label.setPadding(GlassUi.dp(c, 8), GlassUi.dp(c, 3), GlassUi.dp(c, 8), GlassUi.dp(c, 3));
        LayoutParams lpLabel = new LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLabel.topMargin = GlassUi.dp(c, 6);
        addView(label, lpLabel);
    }

    /** Viền trắng quanh hình đang được dùng làm hình nền. */
    void setApplied(boolean on) {
        if (!on) {
            thumb.setForeground(null);
            return;
        }
        GradientDrawable ring = new GradientDrawable();
        ring.setColor(0x00000000);
        ring.setCornerRadius(GlassUi.dp(getContext(), 10));
        ring.setStroke(GlassUi.dp(getContext(), 3), Ui.WHITE);
        thumb.setForeground(ring);
    }

    /** Dấu tích xanh ở góc trên phải. */
    void setBadge(boolean on) {
        badge.setVisibility(on ? VISIBLE : GONE);
    }

    @Override
    protected void onFocusChanged(boolean gain, int direction, Rect previouslyFocused) {
        super.onFocusChanged(gain, direction, previouslyFocused);
        float s = gain ? 1.06f : 1f;
        animate().scaleX(s).scaleY(s).setDuration(200).setInterpolator(EASE).start();
        thumb.animate().translationZ(gain ? GlassUi.dp(getContext(), 8) : 0).setDuration(200)
                .setInterpolator(EASE).start();
    }
}
