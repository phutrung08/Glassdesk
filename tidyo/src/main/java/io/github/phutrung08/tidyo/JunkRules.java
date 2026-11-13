package io.github.phutrung08.tidyo;

import java.util.Locale;

/** Luật nhận diện file rác. Thuần Java, không dùng kiểu Android nên kiểm thử được trên máy tính. */
final class JunkRules {
    private JunkRules() {}

    /**
     * @param relativePath đường dẫn tương đối so với gốc bộ nhớ dùng chung, ngăn cách bằng '/'
     *                     (vd "Download/a.tmp"). Dấu '/' đầu được bỏ qua.
     * @param includeApk   có tính file .apk là rác hay không
     */
    static boolean isJunk(String relativePath, boolean includeApk) {
        if (relativePath == null) return false;
        String p = relativePath;
        while (p.startsWith("/")) p = p.substring(1);
        if (p.isEmpty() || p.endsWith("/")) return false;

        String[] parts = p.split("/");
        // Android/ ở cấp cao nhất là dữ liệu của các ứng dụng khác: không đụng tới.
        if (parts.length > 1 && parts[0].equals("Android")) return false;

        // File nằm trong một thư mục .thumbnails (ở bất kỳ cấp nào phía trên file).
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(".thumbnails")) return true;
        }

        String name = parts[parts.length - 1].toLowerCase(Locale.ROOT);
        if (name.endsWith(".tmp") || name.endsWith(".log")) return true;
        return includeApk && name.endsWith(".apk");
    }
}
