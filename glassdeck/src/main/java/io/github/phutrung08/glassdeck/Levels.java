package io.github.phutrung08.glassdeck;

/** Phép tính thuần cho thanh chỉnh và độ trong của menu. Thuần Java để kiểm thử được. */
final class Levels {
    private Levels() {}

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Tăng hoặc giảm một bước (dir > 0 hoặc dir < 0) rồi giữ trong khoảng min..max. */
    static int step(int value, int dir, int min, int max, int step) {
        return clamp(value + (dir < 0 ? -step : step), min, max);
    }

    /** Làm tròn về bội gần nhất của step rồi giữ trong khoảng; dùng khi đọc giá trị đã lưu. */
    static int snap(int value, int min, int max, int step) {
        return clamp(Math.round(value / (float) step) * step, min, max);
    }

    /** Độ đục (alpha 0..1) của nền menu theo "Độ trong của menu": 0% = 0,92; 80% = 0,32. */
    /** Âm lượng 0..100 thành hệ số 0..1 theo đường cong nhẹ, để các mức thấp vẫn đủ nhỏ. */
    static float soundGain(int volumePercent) {
        float p = clamp(volumePercent, 0, 100) / 100f;
        return (float) Math.pow(p, 1.5);
    }

    static float menuAlpha(int menuTransparency) {
        return 0.92f - clamp(menuTransparency, 0, 100) / 100f * 0.75f;
    }

    /** Từ mức này trở lên nền menu đủ trong để chữ cần thêm bóng cho dễ đọc. */
    static boolean menuShadow(int menuTransparency) {
        return menuTransparency >= 50;
    }
}
