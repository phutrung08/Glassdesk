package io.github.phutrung08.glassdeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WallpaperNamesTest {
    @Test
    public void baseName_stripsLastExtensionOnly() {
        assertEquals("lua-hoang-hon", WallpaperNames.baseName("lua-hoang-hon.jpg"));
        assertEquals("a.b", WallpaperNames.baseName("a.b.png"));
        assertEquals("photo", WallpaperNames.baseName("photo"));
        assertEquals(".hidden", WallpaperNames.baseName(".hidden"));
    }

    @Test
    public void knownKey_recognisesBundledWallpapers() {
        assertEquals("lua-hoang-hon", WallpaperNames.knownKey("lua-hoang-hon.jpg"));
        assertEquals("tidyo-xanh-la", WallpaperNames.knownKey("tidyo-xanh-la.JPEG"));
        assertEquals("hoat-hinh-thanh-pho-dem", WallpaperNames.knownKey("Hoat-Hinh-Thanh-Pho-Dem.webp"));
    }

    @Test
    public void knownKey_unknownFilesAreNull() {
        assertNull(WallpaperNames.knownKey("holiday.jpg"));
        // Bản sao có hậu tố -2 không còn là hình đi kèm.
        assertNull(WallpaperNames.knownKey("lua-hoang-hon-2.jpg"));
        assertNull(WallpaperNames.knownKey(""));
    }
}
