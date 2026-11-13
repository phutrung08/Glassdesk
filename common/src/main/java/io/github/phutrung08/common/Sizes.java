package io.github.phutrung08.common;

import java.util.Locale;

/** Định dạng dung lượng KB/MB/GB (1024). Thuần Java để test được. */
public final class Sizes {
    private static final long KB = 1024L, MB = KB * 1024, GB = MB * 1024;

    private Sizes() {}

    /** Định dạng theo ngôn ngữ hiện tại của ứng dụng (dấu thập phân đúng cho từng nước). */
    public static String format(long bytes) {
        return format(bytes, Locale.getDefault());
    }

    public static String format(long bytes, Locale locale) {
        if (bytes < 0) bytes = 0;
        if (bytes >= GB) return String.format(locale, "%.1f GB", bytes / (double) GB);
        if (bytes >= MB) return String.format(locale, "%.0f MB", bytes / (double) MB);
        return String.format(locale, "%.0f KB", bytes / (double) KB);
    }

    /** Phần trăm đã dùng, làm tròn, luôn trong khoảng 0..100. */
    public static int usedPercent(long total, long free) {
        if (total <= 0) return 0;
        long used = Math.max(0, Math.min(total, total - free));
        return (int) Math.round(100.0 * used / total);
    }
}
