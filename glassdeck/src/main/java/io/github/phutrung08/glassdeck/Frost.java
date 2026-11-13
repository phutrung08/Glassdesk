package io.github.phutrung08.glassdeck;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Kính mờ: mỗi tấm kính vẽ đúng phần hình nền đã làm mờ nằm phía sau nó,
 * phủ thêm một lớp màu (sáng hoặc tối) với độ đậm theo "Độ trong của khay và nút" (khay, nút) hoặc "Độ trong của menu" (hộp thoại).
 */
final class Frost {
    static int screenW = 1920, screenH = 1080;

    private static Bitmap blurred;
    private static int version;
    private static int glassColor = 0x29FFFFFF;
    private static int panelColor = 0xE6102219;
    private static final List<WeakReference<Glass>> ALL = new ArrayList<>();

    private Frost() {}

    /**
     * @param blur         bản mờ của hình nền đang hiện
     * @param luminance    độ sáng trung bình của hình nền, 0..1
     * @param transparency     0 = đặc, 100 = trong hoàn toàn; cho khay và nút
     * @param menuTransparency "Độ trong của menu" 0..80; chỉ cho nền hộp thoại và menu
     * @param defaultBg        đang dùng nền mặc định (để hộp thoại giữ tông xanh)
     */
    static void update(Bitmap blur, float luminance, int transparency, int menuTransparency, boolean defaultBg) {
        blurred = blur;
        version++;
        float t = (100 - Math.max(0, Math.min(100, transparency))) / 100f;
        if (luminance < 0.38f) {
            // Hình tối: kính sáng lên bằng lớp trắng mỏng.
            glassColor = argb(t * 0.40f, 0xFFFFFF);
        } else {
            // Hình sáng: kính tối lại để chữ trắng vẫn đọc được, không bao giờ trong hẳn.
            glassColor = argb(0.25f + t * 0.60f, 0x0E1512);
        }
        panelColor = argb(Levels.menuAlpha(menuTransparency), defaultBg ? 0x102219 : 0x0E1512);
        for (Iterator<WeakReference<Glass>> it = ALL.iterator(); it.hasNext(); ) {
            Glass g = it.next().get();
            if (g == null) it.remove();
            else g.invalidateSelf();
        }
    }

    private static int argb(float alpha, int rgb) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | rgb;
    }

    /** Bỏ các tham chiếu yếu đã bị thu hồi để danh sách không phình ra khi mở/đóng menu nhiều lần. */
    private static void register(Glass g) {
        for (Iterator<WeakReference<Glass>> it = ALL.iterator(); it.hasNext(); ) {
            if (it.next().get() == null) it.remove();
        }
        ALL.add(new WeakReference<>(g));
    }

    static final class Glass extends Drawable {
        private final View host;
        private final float radius;
        private final boolean panel;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Matrix matrix = new Matrix();
        private final int[] loc = new int[2];
        private BitmapShader shader;
        private int shaderVersion = -1;
        // Vị trí đã dùng để tính ma trận lần trước; không đổi thì khỏi tính lại.
        private int lastX, lastY;
        private boolean matrixValid;

        /** @param panel true cho hộp thoại (đậm hơn), false cho nút và khay. */
        Glass(View host, float radiusPx, boolean panel, float edgePx) {
            this.host = host;
            this.radius = radiusPx;
            this.panel = panel;
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(edgePx);
            edge.setColor(0x47FFFFFF);
            register(this);
        }

        @Override
        public void draw(Canvas c) {
            rect.set(getBounds());
            Bitmap bmp = blurred;
            if (bmp != null) {
                if (shaderVersion != version || shader == null) {
                    shader = new BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    shaderVersion = version;
                    fill.setShader(shader);
                    matrixValid = false;
                }
                // Căn bản mờ theo vị trí của tấm kính trên màn hình.
                host.getLocationInWindow(loc);
                if (!matrixValid || loc[0] != lastX || loc[1] != lastY) {
                    matrix.setScale(screenW / (float) bmp.getWidth(), screenH / (float) bmp.getHeight());
                    matrix.postTranslate(-loc[0], -loc[1]);
                    shader.setLocalMatrix(matrix);
                    lastX = loc[0];
                    lastY = loc[1];
                    matrixValid = true;
                }
                c.drawRoundRect(rect, radius, radius, fill);
            }
            tint.setColor(panel ? panelColor : glassColor);
            c.drawRoundRect(rect, radius, radius, tint);
            float half = edge.getStrokeWidth() / 2f;
            rect.inset(half, half);
            c.drawRoundRect(rect, radius, radius, edge);
        }

        @Override
        public void getOutline(Outline o) {
            o.setRoundRect(getBounds(), radius);
        }

        @Override public void setAlpha(int a) { }
        @Override public void setColorFilter(ColorFilter f) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
