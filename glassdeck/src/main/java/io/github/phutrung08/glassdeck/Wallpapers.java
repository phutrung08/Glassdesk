package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Hình nền nằm trong thư mục riêng của app (không cần xin quyền bộ nhớ):
 * /sdcard/Android/data/io.github.phutrung08.glassdeck/files/wallpapers
 */
final class Wallpapers {
    private Wallpapers() {}

    static File dir(Context c) {
        File d = c.getExternalFilesDir("wallpapers");
        if (d != null && !d.exists()) d.mkdirs();
        return d;
    }

    /** Tên file có đuôi hình được hỗ trợ và không phải file ẩn (bắt đầu bằng dấu chấm). */
    static boolean isImageName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return !n.startsWith(".")
                && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"));
    }

    static boolean isImage(File f) {
        return f.isFile() && isImageName(f.getName());
    }

    /**
     * Tên chưa bị chiếm: giữ nguyên nếu còn trống, ngược lại thêm hậu tố -2, -3... trước phần đuôi.
     * @param exists cho biết một tên đã tồn tại trong thư mục đích chưa
     */
    static String uniqueName(String name, Predicate<String> exists) {
        if (!exists.test(name)) return name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        String candidate;
        int i = 2;
        do {
            candidate = base + "-" + i + ext;
            i++;
        } while (exists.test(candidate));
        return candidate;
    }

    /** Hình này đã có trong thư mục hình nền chưa (cùng tên và cùng dung lượng). */
    static boolean alreadyAdded(Context c, File src) {
        File d = dir(c);
        File same = d == null ? null : new File(d, src.getName());
        return same != null && same.exists() && same.length() == src.length();
    }

    /** Chép một hình vào thư mục hình nền. Trùng tên khác nội dung thì thêm hậu tố. Trả về false nếu lỗi. */
    static boolean copyIn(Context c, File src) {
        if (alreadyAdded(c, src)) return true;
        final File d = dir(c);
        if (d == null) return false;
        File tmp;
        try {
            // Mỗi lần chép một file tạm riêng, để hai lần chép cùng lúc không ghi đè nhau.
            tmp = File.createTempFile(".copy", null, d);
        } catch (Exception e) {
            return false;
        }
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            tmp.delete();
            return false;
        }
        File dst = new File(d, uniqueName(src.getName(), name -> new File(d, name).exists()));
        if (tmp.renameTo(dst)) return true;
        tmp.delete();
        return false;
    }

    static List<File> list(Context c) {
        List<File> out = new ArrayList<>();
        File d = dir(c);
        File[] files = d == null ? null : d.listFiles();
        if (files != null) {
            for (File f : files) if (isImage(f)) out.add(f);
        }
        Collections.sort(out, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));
        return out;
    }

    /**
     * Đọc hình, thu nhỏ khi giải mã rồi cắt giữa cho vừa khít màn hình, dạng RGB_565 (nhẹ bộ nhớ).
     * Trả về null nếu không đọc được.
     */
    static Bitmap loadForScreen(File f, int w, int h) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            int sample = 1;
            while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap src = BitmapFactory.decodeFile(f.getPath(), o);
            if (src == null) return null;

            float scale = Math.max(w / (float) src.getWidth(), h / (float) src.getHeight());
            Matrix m = new Matrix();
            m.setScale(scale, scale);
            m.postTranslate((w - src.getWidth() * scale) / 2f, (h - src.getHeight() * scale) / 2f);
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            new Canvas(out).drawBitmap(src, m, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
            src.recycle();
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Vẽ một drawable (nền mặc định) ra ảnh nhỏ để dùng chung đường xử lý với hình nền. */
    static Bitmap small(Drawable d, int screenW, int screenH) {
        Bitmap b = Bitmap.createBitmap(Math.max(1, screenW / 8), Math.max(1, screenH / 8), Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(new Canvas(b));
        return b;
    }

    /** Bản mờ của hình nền: thu còn 1/8 rồi làm mờ. Kính sẽ phóng nó lên lại nên rất nhẹ (ảnh nhỏ, giữ ARGB_8888). */
    static Bitmap blurred(Bitmap screen) {
        int w = Math.max(1, screen.getWidth() / 8), h = Math.max(1, screen.getHeight() / 8);
        Bitmap small = Bitmap.createScaledBitmap(screen, w, h, true);
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        int[] tmp = new int[w * h];
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(px, tmp, w, h, 5, true);
            boxBlur(tmp, px, w, h, 5, false);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        if (small != screen) small.recycle();
        return out;
    }

    private static void boxBlur(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int outer = horizontal ? h : w, inner = horizontal ? w : h;
        for (int a = 0; a < outer; a++) {
            for (int b = 0; b < inner; b++) {
                int sr = 0, sg = 0, sb = 0, n = 0;
                for (int k = -r; k <= r; k++) {
                    int p = Math.max(0, Math.min(inner - 1, b + k));
                    int c = horizontal ? in[a * w + p] : in[p * w + a];
                    sr += (c >> 16) & 0xFF;
                    sg += (c >> 8) & 0xFF;
                    sb += c & 0xFF;
                    n++;
                }
                int v = 0xFF000000 | ((sr / n) << 16) | ((sg / n) << 8) | (sb / n);
                if (horizontal) out[a * w + b] = v;
                else out[b * w + a] = v;
            }
        }
    }

    /** Độ sáng trung bình 0..1, để chọn kính sáng hay kính tối. */
    static float luminance(Bitmap small) {
        int w = small.getWidth(), h = small.getHeight();
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        double sum = 0;
        for (int c : px) sum += 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c);
        return (float) (sum / (px.length * 255.0));
    }
}
