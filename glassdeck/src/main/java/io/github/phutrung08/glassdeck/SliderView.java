package io.github.phutrung08.glassdeck;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;

/**
 * Thanh trượt vẽ riêng: rãnh 8 dp, phần đã chọn màu trắng, núm trắng 20 dp có viền mờ,
 * các mốc 0, 20, 40... bên dưới. Không tự nhận phím, SliderPanel điều khiển qua setValue().
 */
final class SliderView extends View {
    private static final int LABEL_STEP = 20;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labels = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;
    private int min, max, value;
    private float shown;      // vị trí đang vẽ, 0..1, chạy theo value
    private ValueAnimator anim;

    SliderView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        labels.setTextAlign(Paint.Align.CENTER);
        labels.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13, c.getResources().getDisplayMetrics()));
        labels.setColor(0xB8FFFFFF);
    }

    void setRange(int min, int max, int value) {
        this.min = min;
        this.max = max;
        this.value = value;
        this.shown = fraction(value);
        invalidate();
    }

    void setValue(int v) {
        if (v == value) return;
        value = v;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(shown, fraction(v)).setDuration(100);
        anim.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    private float fraction(int v) {
        return max == min ? 0f : (v - min) / (float) (max - min);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), Math.round(64 * d));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float ring = 16 * d;                      // chỗ chừa cho núm và viền ở hai đầu
        float left = ring, right = getWidth() - ring;
        float cy = 18 * d, track = 8 * d;
        paint.setColor(0x38FFFFFF);
        canvas.drawRoundRect(left, cy - track / 2, right, cy + track / 2, track / 2, track / 2, paint);
        float x = left + (right - left) * shown;
        paint.setColor(0xFFFFFFFF);
        canvas.drawRoundRect(left, cy - track / 2, x, cy + track / 2, track / 2, track / 2, paint);
        paint.setColor(0x40FFFFFF);
        canvas.drawCircle(x, cy, 16 * d, paint);   // viền mờ quanh núm
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(x, cy, 10 * d, paint);
        for (int v = min; v <= max; v += LABEL_STEP) {
            float lx = left + (right - left) * fraction(v);
            canvas.drawText(String.valueOf(v), lx, cy + 16 * d + 22 * d, labels);
        }
    }
}
