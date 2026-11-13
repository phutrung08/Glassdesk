package io.github.phutrung08.glassdeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IntervalsTest {
    @Test
    public void seconds() {
        assertEquals("30 giây", Intervals.label(30));
        assertEquals("0 giây", Intervals.label(0));
        assertEquals("59 giây", Intervals.label(59));
    }

    @Test
    public void minutes() {
        assertEquals("1 phút", Intervals.label(60));
        assertEquals("5 phút", Intervals.label(300));
        assertEquals("30 phút", Intervals.label(1800));
        assertEquals("59 phút", Intervals.label(3599));
    }

    @Test
    public void hours() {
        assertEquals("1 giờ", Intervals.label(3600));
        assertEquals("2 giờ", Intervals.label(7200));
    }

    @Test
    public void defaultIsOneOfTheValues() {
        boolean found = false;
        for (int v : Intervals.VALUES) if (v == Intervals.DEFAULT) found = true;
        assertTrue(found);
    }
}
