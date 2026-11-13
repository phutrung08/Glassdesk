package io.github.phutrung08.glassdeck;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Ảnh thu nhỏ 320x180 (RGB_565) giải mã ở luồng nền và nhớ trong LruCache khoảng 12 MB.
 * Mỗi yêu cầu mang "thẻ" của thời điểm gửi; cancelPending() làm các yêu cầu chưa xong thành cũ,
 * kết quả của chúng bị bỏ và việc chưa chạy được bỏ qua.
 */
final class Thumbnails {
    static final int W = 320, H = 180;

    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap b) { return b.getByteCount(); }
    };
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final BooleanSupplier alive;
    private volatile int generation;

    /** @param alive cho biết màn hình còn sống, để không giao kết quả cho màn hình đã huỷ. */
    Thumbnails(BooleanSupplier alive) {
        this.alive = alive;
    }

    private static String key(File f) {
        return f.getPath() + "@" + f.lastModified();
    }

    Bitmap peek(File f) {
        return cache.get(key(f));
    }

    /** Giao ảnh cho callback trên luồng giao diện: ngay nếu đã có trong bộ nhớ, ngược lại sau khi giải mã. */
    void load(File f, Consumer<Bitmap> callback) {
        final String key = key(f);
        Bitmap hit = cache.get(key);
        if (hit != null) {
            callback.accept(hit);
            return;
        }
        final int gen = generation;
        try {
            pool.execute(() -> {
                if (gen != generation) return;
                Bitmap b = Wallpapers.loadForScreen(f, W, H);
                if (b == null) return;
                cache.put(key, b);
                ui.post(() -> {
                    if (gen != generation || !alive.getAsBoolean()) return;
                    callback.accept(b);
                });
            });
        } catch (RejectedExecutionException ignored) {
            // Đã tắt: màn hình đang đóng.
        }
    }

    /** Bỏ các yêu cầu đang chờ (gọi khi đổi hoặc đóng bảng). Ảnh đã giải mã vẫn nằm trong bộ nhớ. */
    void cancelPending() {
        generation++;
    }

    void shutdown() {
        generation++;
        pool.shutdownNow();
        cache.evictAll();
    }
}
