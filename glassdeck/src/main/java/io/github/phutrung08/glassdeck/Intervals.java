package io.github.phutrung08.glassdeck;

/** Các mốc thời gian đổi hình nền và nhãn hiển thị. Thuần Java để kiểm thử được. */
final class Intervals {
    static final int[] VALUES = {30, 60, 300, 900, 1800, 3600};
    static final int DEFAULT = 300;

    private Intervals() {}

    static String label(int sec) {
        if (sec < 60) return sec + " giây";
        if (sec < 3600) return (sec / 60) + " phút";
        return (sec / 3600) + " giờ";
    }
}
