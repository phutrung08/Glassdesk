package io.github.phutrung08.glassdeck;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

/**
 * Tiếng "tích" nhẹ khi di chuyển và khi bấm OK trong Glassdeck. Âm thanh tự tổng hợp
 * (tools/sound-gen), phát bằng SoundPool nên không trễ. Âm lượng và bật/tắt theo thiết lập "Âm thanh".
 * Muốn chỉ còn tiếng này, tắt "Âm thanh hệ thống" của tivi.
 */
final class NavSound {
    private static final String TAG = "NavSound";
    /** Hai lần phát cách nhau ít nhất chừng này, để giữ phím di chuyển không thành tiếng rền. */
    private static final long MIN_GAP_MS = 70;
    private static final float CLICK_RATE = 0.8f;   // OK hơi trầm hơn tiếng di chuyển

    private final Prefs prefs;
    private final SoundPool pool;
    private final int soundId;
    private volatile boolean loaded;
    private long lastPlay;

    private NavSound(Activity a) {
        prefs = new Prefs(a);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build();
        pool.setOnLoadCompleteListener((p, id, status) -> loaded = status == 0);
        soundId = pool.load(a, R.raw.nav_tick, 1);
    }

    /** Gắn vào một màn hình: mỗi lần tiêu điểm đổi chỗ thì phát tiếng di chuyển. Trả về để gọi release(). */
    static NavSound install(Activity a) {
        final NavSound s = new NavSound(a);
        View decor = a.getWindow().getDecorView();
        // Chỉ khi đang có tiêu điểm cũ và mới: bỏ qua lúc màn hình mở hoặc nạp lại danh sách.
        decor.getViewTreeObserver().addOnGlobalFocusChangeListener((oldF, newF) -> {
            if (oldF != null && newF != null) s.play(1f);
        });
        return s;
    }

    /** Màn hình chuyển phím sang đây; nhả phím OK thì phát tiếng bấm. */
    void onKey(KeyEvent e) {
        int code = e.getKeyCode();
        boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER;
        if (ok && e.getAction() == KeyEvent.ACTION_UP && !e.isCanceled()) play(CLICK_RATE);
    }

    /** Phát thử theo âm lượng đang đặt (khi người dùng kéo thanh trượt). */
    void preview() {
        lastPlay = 0;
        play(1f);
    }

    private void play(float rate) {
        float gain = Levels.soundGain(prefs.soundVolume());
        long now = android.os.SystemClock.uptimeMillis();
        if (!loaded || gain <= 0f || now - lastPlay < MIN_GAP_MS) return;
        lastPlay = now;
        pool.play(soundId, gain, gain, 1, 0, rate);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "play gain=" + gain + " rate=" + rate);
    }

    void release() {
        pool.release();
    }
}
