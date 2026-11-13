package io.github.phutrung08.tidyo;

import io.github.phutrung08.common.Sizes;

/** Soạn câu chữ kết quả "Dọn nhanh". Thuần Java để kiểm thử; chỉ báo những gì đo được. */
final class Report {
    /** Dưới ngưỡng này coi là RAM không đổi (nhiễu của hệ thống). */
    static final long RAM_NOISE = 1024L * 1024L;

    private Report() {}

    static String stopped(int apps, long ramBefore, long ramAfter) {
        return "Đã yêu cầu dừng tiến trình nền của " + apps + " ứng dụng · " + ramChange(ramBefore, ramAfter);
    }

    static String ramChange(long before, long after) {
        long diff = after - before;
        if (diff > RAM_NOISE) return "RAM trống tăng " + Sizes.format(diff) + " (đo được)";
        if (diff < -RAM_NOISE) return "RAM trống giảm " + Sizes.format(-diff) + " (đo được)";
        return "RAM trống không đổi đáng kể";
    }

    static String junkFound(int count, long bytes) {
        return count + " file tạm (" + Sizes.format(bytes) + ")";
    }
}
