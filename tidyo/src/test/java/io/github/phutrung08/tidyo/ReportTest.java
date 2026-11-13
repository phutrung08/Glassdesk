package io.github.phutrung08.tidyo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReportTest {
    private static final long MB = 1024L * 1024L;

    @Test public void ramTang() {
        String s = Report.ramChange(100 * MB, 300 * MB);
        assertTrue(s, s.startsWith("RAM trống tăng "));
        assertTrue(s.endsWith("(đo được)"));
    }

    @Test public void ramGiam() {
        String s = Report.ramChange(300 * MB, 100 * MB);
        assertTrue(s, s.startsWith("RAM trống giảm "));
    }

    @Test public void ramTrongNguongNhieu() {
        assertEquals("RAM trống không đổi đáng kể", Report.ramChange(100 * MB, 100 * MB));
        // Đúng bằng ngưỡng vẫn coi là không đổi (chỉ khác biệt LỚN HƠN ngưỡng mới được báo).
        assertEquals("RAM trống không đổi đáng kể", Report.ramChange(100 * MB, 101 * MB));
        assertEquals("RAM trống không đổi đáng kể", Report.ramChange(101 * MB, 100 * MB));
    }

    @Test public void stoppedKhongHuaTocDo() {
        String s = Report.stopped(12, 100 * MB, 100 * MB);
        assertTrue(s.startsWith("Đã yêu cầu dừng tiến trình nền của 12 ứng dụng"));
        assertFalse(s.contains("nhanh"));
    }

    @Test public void junkFoundCoSoLuongVaDungLuong() {
        String s = Report.junkFound(3, 5 * MB);
        assertTrue(s, s.startsWith("3 file tạm ("));
    }
}
