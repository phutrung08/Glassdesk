package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Thanh tiến độ nhỏ 60 x 4 dp ở cuối dòng. Đổi màu theo dòng cha khi dòng được chọn (nền trắng). */
final class MiniBar extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float w, h;
    private float fraction;

    MiniBar(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        w = 60 * d;
        h = 4 * d;
        setDuplicateParentStateEnabled(true);
    }

    /** @param fraction 0..1 */
    void setFraction(float fraction) {
        this.fraction = Math.max(0f, Math.min(1f, fraction));
        invalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(Math.round(w), Math.round(h));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean focused = false;
        for (int s : getDrawableState()) if (s == android.R.attr.state_focused) focused = true;
        paint.setColor(focused ? 0x330C2A1C : 0x38FFFFFF);
        canvas.drawRoundRect(0, 0, w, h, h / 2, h / 2, paint);
        if (fraction > 0f) {
            paint.setColor(focused ? 0xFF0C2A1C : 0xFFFFFFFF);
            canvas.drawRoundRect(0, 0, Math.max(h, w * fraction), h, h / 2, h / 2, paint);
        }
    }
}
