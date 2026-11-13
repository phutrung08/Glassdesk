package io.github.phutrung08.tidyo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JunkRulesTest {
    @Test public void nullVaRong() {
        assertFalse(JunkRules.isJunk(null, true));
        assertFalse(JunkRules.isJunk("", true));
        assertFalse(JunkRules.isJunk("/", true));
    }

    @Test public void duongDanKetThucBangGachCheoLaThuMuc() {
        assertFalse(JunkRules.isJunk("Download/a.tmp/", true));
    }

    @Test public void tmpVaLogLuonLaRac() {
        assertTrue(JunkRules.isJunk("a.tmp", false));
        assertTrue(JunkRules.isJunk("Download/x/b.log", false));
        assertTrue(JunkRules.isJunk("Download/X.TMP", false));
        assertTrue(JunkRules.isJunk("Movies/Y.Log", true));
    }

    @Test public void apkChiLaRacKhiIncludeApk() {
        assertTrue(JunkRules.isJunk("Download/app.apk", true));
        assertFalse(JunkRules.isJunk("Download/app.apk", false));
        assertTrue(JunkRules.isJunk("Download/APP.APK", true));
    }

    @Test public void fileThuongKhongPhaiRac() {
        assertFalse(JunkRules.isJunk("Movies/phim.mp4", true));
        assertFalse(JunkRules.isJunk("Download/tmp", true));
        assertFalse(JunkRules.isJunk("Download/ghi-chu.tmp.txt", true));
        assertFalse(JunkRules.isJunk("log", true));
    }

    @Test public void anhTrongThumbnailsLaRac() {
        assertTrue(JunkRules.isJunk(".thumbnails/123.jpg", false));
        assertTrue(JunkRules.isJunk("DCIM/.thumbnails/456.jpg", false));
        assertTrue(JunkRules.isJunk("DCIM/.thumbnails/sub/789", false));
    }

    @Test public void thumbnailsKhongKhopTenGanGiong() {
        assertFalse(JunkRules.isJunk("DCIM/thumbnails/456.jpg", true));
        assertFalse(JunkRules.isJunk("DCIM/.thumbnails2/456.jpg", true));
        // File tên ".thumbnails" không phải "nằm trong" thư mục đó.
        assertFalse(JunkRules.isJunk("DCIM/.thumbnails", true));
    }

    @Test public void boQuaAndroidCapCao() {
        assertFalse(JunkRules.isJunk("Android/data/app/cache/a.tmp", true));
        assertFalse(JunkRules.isJunk("Android/obb/x.apk", true));
        assertFalse(JunkRules.isJunk("Android/data/app/.thumbnails/1.jpg", true));
    }

    @Test public void androidKhongPhaiCapCaoThiVanTinh() {
        assertTrue(JunkRules.isJunk("Download/Android/a.tmp", false));
        // Phân biệt hoa thường: "android" chữ thường không phải thư mục hệ thống Android/.
        assertTrue(JunkRules.isJunk("android/a.tmp", false));
        // File tên "Android" ở cấp cao nhất (không phải thư mục) không bị loại theo luật này.
        assertFalse(JunkRules.isJunk("Android", true));
    }

    @Test public void dauGachCheoDauDuocBo() {
        assertTrue(JunkRules.isJunk("/Download/a.tmp", false));
        assertFalse(JunkRules.isJunk("/Android/data/a.tmp", false));
    }
}
