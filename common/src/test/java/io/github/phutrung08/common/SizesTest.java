package io.github.phutrung08.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SizesTest {
    @Test public void formatsKilobytes() {
        assertEquals("0 KB", Sizes.format(0));
        assertEquals("1 KB", Sizes.format(1024));
        assertEquals("0 KB", Sizes.format(-5));
    }

    @Test public void formatsMegabytes() {
        assertEquals("1 MB", Sizes.format(1024L * 1024));
        assertEquals("617 MB", Sizes.format(617L * 1024 * 1024));
    }

    @Test public void formatsGigabytesWithVietnameseDecimalComma() {
        assertEquals("1,0 GB", Sizes.format(1024L * 1024 * 1024));
        assertEquals("11,4 GB", Sizes.format((long) (11.4 * 1024 * 1024 * 1024)));
    }

    @Test public void usedPercentIsClampedAndRounded() {
        assertEquals(0, Sizes.usedPercent(0, 0));
        assertEquals(0, Sizes.usedPercent(100, 150));
        assertEquals(100, Sizes.usedPercent(100, -10));
        assertEquals(19, Sizes.usedPercent(1000, 810));
        assertEquals(70, Sizes.usedPercent(2000, 605));
    }
}
