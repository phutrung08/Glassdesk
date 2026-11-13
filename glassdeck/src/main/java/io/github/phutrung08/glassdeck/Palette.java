package io.github.phutrung08.glassdeck;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

/** Lấy màu chủ đạo từ ảnh bìa của app để nhuộm nền. */
final class Palette {
    static final int NEUTRAL = 0xFF24345C;

    private Palette() {}

    /** Màu nền tối, cùng tông với ảnh. Ảnh gần như đen trắng thì trả về màu trung tính. */
    static int dominant(Drawable d) {
        if (d == null) return NEUTRAL;
        final int w = 32, h = 18;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        try {
            d.setBounds(0, 0, w, h);
            d.draw(new Canvas(bmp));
        } catch (Exception e) {
            return NEUTRAL;
        }
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        bmp.recycle();

        float[] hsv = new float[3];
        double sx = 0, sy = 0, satSum = 0, weight = 0;
        int counted = 0;
        for (int p : px) {
            if (Color.alpha(p) < 128) continue;
            counted++;
            Color.colorToHSV(p, hsv);
            double wt = hsv[1] * hsv[2];
            double rad = Math.toRadians(hsv[0]);
            sx += Math.cos(rad) * wt;
            sy += Math.sin(rad) * wt;
            satSum += hsv[1] * wt;
            weight += wt;
        }
        if (counted == 0 || weight < 0.05 * counted) return NEUTRAL;
        float hue = (float) Math.toDegrees(Math.atan2(sy, sx));
        if (hue < 0) hue += 360f;
        float sat = (float) Math.max(0.45, Math.min(0.8, satSum / weight));
        return Color.HSVToColor(new float[]{hue, sat, 0.45f});
    }

    static int scale(int c, float f) {
        return Color.rgb(Math.round(Color.red(c) * f), Math.round(Color.green(c) * f), Math.round(Color.blue(c) * f));
    }
}
