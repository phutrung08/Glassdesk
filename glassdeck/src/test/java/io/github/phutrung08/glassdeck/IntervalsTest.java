package io.github.phutrung08.glassdeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IntervalsTest {
    @Test
    public void seconds() {
        assertEquals(Intervals.SECONDS, Intervals.unit(30));
        assertEquals(30, Intervals.amount(30));
        assertEquals(Intervals.SECONDS, Intervals.unit(0));
        assertEquals(Intervals.SECONDS, Intervals.unit(59));
        assertEquals(59, Intervals.amount(59));
    }

    @Test
    public void minutes() {
        assertEquals(Intervals.MINUTES, Intervals.unit(60));
        assertEquals(1, Intervals.amount(60));
        assertEquals(5, Intervals.amount(300));
        assertEquals(30, Intervals.amount(1800));
        assertEquals(Intervals.MINUTES, Intervals.unit(3599));
        assertEquals(59, Intervals.amount(3599));
    }

    @Test
    public void hours() {
        assertEquals(Intervals.HOURS, Intervals.unit(3600));
        assertEquals(1, Intervals.amount(3600));
        assertEquals(2, Intervals.amount(7200));
    }

    @Test
    public void defaultIsOneOfTheValues() {
        boolean found = false;
        for (int v : Intervals.VALUES) if (v == Intervals.DEFAULT) found = true;
        assertTrue(found);
    }

    @Test
    public void indexOf_knownValuesAndFallback() {
        assertEquals(0, Intervals.indexOf(30));
        assertEquals(2, Intervals.indexOf(300));
        assertEquals(5, Intervals.indexOf(3600));
        // Giá trị lạ rơi về vị trí của mặc định.
        assertEquals(Intervals.indexOf(Intervals.DEFAULT), Intervals.indexOf(123));
    }

    @Test
    public void move_stepsAndStopsAtEnds() {
        assertEquals(3, Intervals.move(2, 1));
        assertEquals(1, Intervals.move(2, -1));
        assertEquals(0, Intervals.move(0, -1));
        int last = Intervals.VALUES.length - 1;
        assertEquals(last, Intervals.move(last, 1));
    }

    @Test
    public void valuesAreIncreasing() {
        for (int i = 1; i < Intervals.VALUES.length; i++) {
            assertTrue(Intervals.VALUES[i] > Intervals.VALUES[i - 1]);
        }
    }
}
