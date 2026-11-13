package io.github.phutrung08.glassdeck;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

import io.github.phutrung08.common.Ui;

/**
 * Vòng chọn thời gian: rãnh tròn mờ, cung trắng dài theo vị trí trong các mốc,
 * số lớn và đơn vị ở giữa. Cung chạy sang vị trí mới trong 150 ms.
 */
final class DialView extends View {
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float d;
    private int steps = 1;
    private float sweep;      // đang vẽ, 0..1
    private String number = "", unit = "";
    private ValueAnimator anim;

    DialView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        ring.setStrokeWidth(10 * d);
        big.setColor(Ui.WHITE);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTypeface(Ui.medium());
        big.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40, c.getResources().getDisplayMetrics()));
        small.setColor(Ui.SUB);
        small.setTextAlign(Paint.Align.CENTER);
        small.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, c.getResources().getDisplayMetrics()));
    }

    /** @param steps tổng số mốc; @param index mốc đang chọn (0..steps-1); @param animate có chạy cung không. */
    void set(int steps, int index, String number, String unit, boolean animate) {
        this.steps = Math.max(1, steps);
        this.number = number;
        this.unit = unit;
        float to = (index + 1) / (float) this.steps;
        if (anim != null) anim.cancel();
        if (!animate) {
            sweep = to;
            invalidate();
            return;
        }
        anim = ValueAnimator.ofFloat(sweep, to).setDuration(150);
        anim.addUpdateListener(a -> {
            sweep = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int size = Math.round(150 * d);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = ring.getStrokeWidth() / 2 + 2 * d;
        oval.set(inset, inset, getWidth() - inset, getHeight() - inset);
        ring.setColor(0x33FFFFFF);
        canvas.drawArc(oval, 0, 360, false, ring);
        ring.setColor(Ui.WHITE);
        canvas.drawArc(oval, -90, 360f * sweep, false, ring);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        canvas.drawText(number, cx, cy + 10 * d, big);
        canvas.drawText(unit, cx, cy + 32 * d, small);
    }
}
