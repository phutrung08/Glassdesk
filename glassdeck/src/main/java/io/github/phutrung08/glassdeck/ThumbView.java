package io.github.phutrung08.glassdeck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import java.io.File;

/** Ô ảnh thu nhỏ: chỉ bắt đầu nạp ảnh khi đã gắn vào cửa sổ (tức là bảng thật sự đang hiện). */
@SuppressLint("ViewConstructor")   // chỉ dựng bằng code, không có trong file layout
final class ThumbView extends ImageView {
    private final Thumbnails thumbs;
    private File file;

    ThumbView(Context c, Thumbnails thumbs) {
        super(c);
        this.thumbs = thumbs;
        setScaleType(ScaleType.CENTER_CROP);
    }

    void setFile(File f) {
        file = f;
        setImageDrawable(null);
        if (isAttachedToWindow()) request();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getDrawable() == null) request();
    }

    private void request() {
        final File want = file;
        if (want == null) return;
        final boolean cached = thumbs.peek(want) != null;
        thumbs.load(want, (Bitmap b) -> {
            if (want != file) return;
            setImageBitmap(b);
            if (!cached) {
                setAlpha(0f);
                animate().alpha(1f).setDuration(140).start();
            }
        });
    }
}
