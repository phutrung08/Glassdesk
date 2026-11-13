package io.github.phutrung08.tidyo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JunkScannerTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private File write(String rel, int bytes) throws IOException {
        File f = new File(tmp.getRoot(), rel);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), new byte[bytes]);
        return f;
    }

    private Set<String> names(List<File> l) {
        Set<String> s = new HashSet<>();
        for (File f : l) s.add(tmp.getRoot().toPath().relativize(f.toPath()).toString().replace('\\', '/'));
        return s;
    }

    @Test public void quetTheoLuat() throws IOException {
        write("Download/a.tmp", 1);
        write("Download/b.apk", 1);
        write("DCIM/.thumbnails/1.jpg", 1);
        write("Movies/phim.mp4", 1);
        write("Android/data/x/c.tmp", 1);
        assertEquals(new HashSet<>(Arrays.asList("Download/a.tmp", "DCIM/.thumbnails/1.jpg")),
                names(JunkScanner.scan(tmp.getRoot(), false)));
        assertEquals(new HashSet<>(Arrays.asList("Download/a.tmp", "Download/b.apk", "DCIM/.thumbnails/1.jpg")),
                names(JunkScanner.scan(tmp.getRoot(), true)));
    }

    @Test public void thuMucRongHoacKhongTonTai() {
        assertTrue(JunkScanner.scan(tmp.getRoot(), true).isEmpty());
        assertTrue(JunkScanner.scan(new File(tmp.getRoot(), "khong-co"), true).isEmpty());
    }

    @Test public void gioiHanDoSau() throws IOException {
        StringBuilder ok = new StringBuilder();
        for (int i = 0; i < JunkScanner.MAX_DEPTH; i++) ok.append("d/");
        write(ok + "in.tmp", 1);
        write(ok + "d/out.tmp", 1);
        assertEquals(1, JunkScanner.scan(tmp.getRoot(), false).size());
    }

    @Test public void khongDiTheoSymlinkThuMuc() throws IOException {
        File outside = tmp.newFolder("outside");
        Files.write(new File(outside, "secret.tmp").toPath(), new byte[1]);
        File root = tmp.newFolder("root");
        try {
            Files.createSymbolicLink(new File(root, "link").toPath(), outside.toPath());
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeNoException("Máy này không tạo được symlink", e);
        }
        Files.write(new File(root, "ok.tmp").toPath(), new byte[1]);
        List<File> found = JunkScanner.scan(root, false);
        assertEquals(1, found.size());
        assertEquals("ok.tmp", found.get(0).getName());
    }

    @Test public void isUnderChanThoatKhoiGoc() throws IOException {
        File root = tmp.newFolder("r").getCanonicalFile();
        File inside = new File(root, "a");
        inside.mkdirs();
        assertTrue(JunkScanner.isUnder(inside, root));
        assertTrue(JunkScanner.isUnder(root, root));
        // Thư mục anh em có tiền tố trùng tên ("r2" so với "r") không được coi là nằm trong.
        File sibling = tmp.newFolder("r2");
        assertFalse(JunkScanner.isUnder(sibling, root));
        assertFalse(JunkScanner.isUnder(new File(root, "../r2"), root));
    }

    @Test public void xoaChiDemFileXoaDuoc() throws IOException {
        File a = write("a.tmp", 10);
        File b = write("b.tmp", 5);
        File missing = new File(tmp.getRoot(), "da-mat.tmp");
        List<File> l = new ArrayList<>(Arrays.asList(a, b, missing));
        JunkScanner.Deleted d = JunkScanner.delete(l);
        assertEquals(2, d.files);
        assertEquals(15, d.bytes);
        assertFalse(a.exists());
        assertFalse(b.exists());
    }

    @Test public void tongDungLuong() throws IOException {
        List<File> l = Arrays.asList(write("a.tmp", 3), write("b.tmp", 4));
        assertEquals(7, JunkScanner.totalSize(l));
        assertEquals(0, JunkScanner.totalSize(new ArrayList<File>()));
    }
}
