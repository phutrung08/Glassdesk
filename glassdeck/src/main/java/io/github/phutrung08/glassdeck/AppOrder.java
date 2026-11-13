package io.github.phutrung08.glassdeck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Thứ tự sắp xếp app. Thuần Java (không dùng kiểu Android) để kiểm thử được. */
final class AppOrder {
    private AppOrder() {}

    /** App công cụ hệ thống (cài đặt, hẹn giờ, trợ giúp...) xếp sau app giải trí. */
    static boolean isUtility(String pkg) {
        return pkg.startsWith("com.sony.dtv.") || pkg.startsWith("com.realtek.")
                || pkg.startsWith("com.android.") || pkg.equals("com.google.android.play.games");
    }

    /**
     * So sánh hai app của màn hình chính: app có trong thứ tự đã lưu trước (theo vị trí đã lưu),
     * rồi app giải trí trước app công cụ, cuối cùng theo tên không phân biệt hoa thường.
     */
    static int compare(List<String> saved, String keyX, boolean utilityX, String labelX,
                       String keyY, boolean utilityY, String labelY) {
        int ix = saved.indexOf(keyX), iy = saved.indexOf(keyY);
        if (ix >= 0 && iy >= 0) return ix - iy;
        if (ix >= 0) return -1;
        if (iy >= 0) return 1;
        if (utilityX != utilityY) return utilityX ? 1 : -1;
        return labelX.compareToIgnoreCase(labelY);
    }

    /** Màn Dọn dẹp: dung lượng lớn trước, bằng nhau thì theo tên. */
    static int compareBySize(long sizeX, String labelX, long sizeY, String labelY) {
        if (sizeX != sizeY) return sizeX > sizeY ? -1 : 1;
        return labelX.compareToIgnoreCase(labelY);
    }

    /** Thứ tự đã lưu là các khoá cách nhau bằng dấu xuống dòng; chuỗi rỗng là chưa có thứ tự. */
    static List<String> parse(String saved) {
        List<String> out = new ArrayList<>();
        if (saved != null && !saved.isEmpty()) Collections.addAll(out, saved.split("\n"));
        return out;
    }

    static String join(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) sb.append(k).append('\n');
        return sb.toString().trim();
    }
}
