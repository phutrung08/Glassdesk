package io.github.phutrung08.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Vòng tròn phần trăm (bộ nhớ, RAM): rãnh kính, cung trắng. */
public final class RingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private int percent = 0;

    public RingView(Context c) {
        super(c);
        track.setStyle(Paint.Style.STROKE);
        track.setColor(Ui.GLASS);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(Ui.WHITE);
        label.setColor(Ui.WHITE);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(Ui.medium());
    }

    public void setPercent(int p) {
        percent = Math.max(0, Math.min(100, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = Math.min(getWidth(), getHeight());
        float stroke = w * 0.08f;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        float pad = stroke / 2f + 1f;
        box.set(pad, pad, w - pad, w - pad);
        canvas.drawArc(box, 0, 360, false, track);
        if (percent > 0) canvas.drawArc(box, -90, 360f * percent / 100f, false, arc);
        label.setTextSize(w * 0.2f);
        float y = w / 2f - (label.descent() + label.ascent()) / 2f;
        canvas.drawText(percent + "%", w / 2f, y, label);
    }
}
