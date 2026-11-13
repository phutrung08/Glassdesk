package io.github.phutrung08.tidyo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Duyệt cây thư mục và xoá file, dựa trên {@link JunkRules}. Chỉ dùng java.io nên kiểm thử được trên máy tính. */
final class JunkScanner {
    static final int MAX_DEPTH = 10;

    private JunkScanner() {}

    /** Kết quả xoá: số file đã xoá được và tổng dung lượng của chúng. */
    static final class Deleted {
        final int files;
        final long bytes;

        Deleted(int files, long bytes) {
            this.files = files;
            this.bytes = bytes;
        }
    }

    /** Tìm file rác dưới root. Không đi theo liên kết tượng trưng (symlink). */
    static List<File> scan(File root, boolean includeApk) {
        List<File> out = new ArrayList<>();
        File canonRoot;
        try {
            canonRoot = root.getCanonicalFile();
        } catch (IOException e) {
            return out;
        }
        walk(root, "", 0, canonRoot, includeApk, out);
        return out;
    }

    private static void walk(File dir, String prefix, int depth, File canonRoot, boolean apk, List<File> out) {
        if (depth > MAX_DEPTH) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (Files.isSymbolicLink(f.toPath())) continue;
            String rel = prefix + f.getName();
            if (f.isDirectory()) {
                if (!isUnder(f, canonRoot)) continue;
                walk(f, rel + "/", depth + 1, canonRoot, apk, out);
            } else if (f.isFile() && JunkRules.isJunk(rel, apk)) {
                out.add(f);
            }
        }
    }

    /** Đường dẫn chuẩn tắc của dir phải nằm trong root (chặn thoát ra ngoài qua liên kết). */
    static boolean isUnder(File dir, File canonRoot) {
        try {
            String c = dir.getCanonicalPath();
            String r = canonRoot.getPath();
            return c.equals(r) || c.startsWith(r.endsWith(File.separator) ? r : r + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    /** Xoá từng file; chỉ đếm file xoá thành công. */
    static Deleted delete(List<File> files) {
        int n = 0;
        long bytes = 0;
        for (File f : files) {
            long len = f.length();
            if (f.delete()) {
                n++;
                bytes += len;
            }
        }
        return new Deleted(n, bytes);
    }

    static long totalSize(List<File> files) {
        long sum = 0;
        for (File f : files) sum += f.length();
        return sum;
    }
}
