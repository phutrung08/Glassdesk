package io.github.phutrung08.glassdeck;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Công tắc 34 x 18 dp: nền xanh khi bật, xám mờ khi tắt, núm trắng trượt trong 150 ms. */
final class SwitchView extends View {
    private static final int ON = 0xFF1DB954, OFF = 0x66808A86, KNOB = 0xFFFFFFFF;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float w, h, pad;
    private boolean checked;
    private float progress;   // 0 = tắt, 1 = bật
    private ValueAnimator anim;

    SwitchView(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        w = 34 * d;
        h = 18 * d;
        pad = 2 * d;
    }

    boolean isChecked() {
        return checked;
    }

    void setChecked(boolean on, boolean animate) {
        if (on == checked && !animate) return;
        checked = on;
        if (anim != null) anim.cancel();
        float to = on ? 1f : 0f;
        if (!animate) {
            progress = to;
            invalidate();
            return;
        }
        anim = ValueAnimator.ofFloat(progress, to).setDuration(150);
        anim.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(Math.round(w), Math.round(h));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(blend(OFF, ON, progress));
        canvas.drawRoundRect(0, 0, w, h, h / 2, h / 2, paint);
        float r = h / 2 - pad;
        float cx = h / 2 + progress * (w - h);
        paint.setColor(KNOB);
        canvas.drawCircle(cx, h / 2, r, paint);
    }

    private static int blend(int from, int to, float t) {
        int a = Math.round(((from >>> 24) & 0xFF) * (1 - t) + ((to >>> 24) & 0xFF) * t);
        int r = Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
