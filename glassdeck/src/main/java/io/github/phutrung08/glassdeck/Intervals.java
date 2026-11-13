package io.github.phutrung08.glassdeck;

import android.content.Context;

/** Các mốc thời gian đổi hình nền, cách chọn mốc và nhãn hiển thị. */
final class Intervals {
    static final int[] VALUES = {30, 60, 300, 900, 1800, 3600};
    static final int DEFAULT = 300;
    static final int SECONDS = 0, MINUTES = 1, HOURS = 2;

    private Intervals() {}

    /** Vị trí của giá trị trong VALUES; giá trị lạ thì lấy vị trí của mặc định. */
    static int indexOf(int sec) {
        int fallback = 0;
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i] == sec) return i;
            if (VALUES[i] == DEFAULT) fallback = i;
        }
        return fallback;
    }

    /** Dịch chuyển vị trí một bước (dir > 0 sang phải, dir < 0 sang trái), dừng ở hai đầu. */
    static int move(int index, int dir) {
        return Levels.clamp(index + (dir < 0 ? -1 : 1), 0, VALUES.length - 1);
    }

    static int unit(int sec) {
        if (sec < 60) return SECONDS;
        return sec < 3600 ? MINUTES : HOURS;
    }

    /** Số đi cùng đơn vị: 30 giây -> 30, 300 giây -> 5 phút -> 5. */
    static int amount(int sec) {
        switch (unit(sec)) {
            case SECONDS: return sec;
            case MINUTES: return sec / 60;
            default: return sec / 3600;
        }
    }

    /** Nhãn đầy đủ, ví dụ "5 phút". */
    static String label(Context c, int sec) {
        int n = amount(sec);
        int plural;
        switch (unit(sec)) {
            case SECONDS: plural = R.plurals.interval_seconds; break;
            case MINUTES: plural = R.plurals.interval_minutes; break;
            default: plural = R.plurals.interval_hours;
        }
        return c.getResources().getQuantityString(plural, n, n);
    }

    /** Chỉ đơn vị, ví dụ "phút", để ghép với số lớn ở giữa vòng. */
    static String unitLabel(Context c, int sec) {
        int plural;
        switch (unit(sec)) {
            case SECONDS: plural = R.plurals.interval_unit_seconds; break;
            case MINUTES: plural = R.plurals.interval_unit_minutes; break;
            default: plural = R.plurals.interval_unit_hours;
        }
        return c.getResources().getQuantityString(plural, amount(sec));
    }
}
