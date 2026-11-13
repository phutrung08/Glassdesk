package io.github.phutrung08.glassdeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LevelsTest {
    @Test
    public void soundGainIsClampedAndIncreasing() {
        assertEquals(0f, Levels.soundGain(0), 0f);
        assertEquals(1f, Levels.soundGain(100), 1e-6f);
        assertEquals(0f, Levels.soundGain(-20), 0f);
        assertEquals(1f, Levels.soundGain(250), 1e-6f);
        float prev = -1f;
        for (int v = 0; v <= 100; v += 10) {
            float g = Levels.soundGain(v);
            assertTrue("tăng dần tại " + v, g > prev);
            prev = g;
        }
        // Mức mặc định 60% phải êm hơn mức tuyến tính (0,6).
        assertTrue(Levels.soundGain(60) < 0.6f);
    }

    @Test
    public void clamp_keepsValueInRange() {
        assertEquals(0, Levels.clamp(-5, 0, 100));
        assertEquals(100, Levels.clamp(130, 0, 100));
        assertEquals(40, Levels.clamp(40, 0, 100));
    }

    @Test
    public void step_movesByStepAndStopsAtEnds() {
        assertEquals(70, Levels.step(60, 1, 0, 100, 10));
        assertEquals(50, Levels.step(60, -1, 0, 100, 10));
        assertEquals(100, Levels.step(100, 1, 0, 100, 10));
        assertEquals(0, Levels.step(0, -1, 0, 100, 10));
        assertEquals(80, Levels.step(80, 1, 0, 80, 10));
    }

    @Test
    public void snap_roundsToNearestStepWithinRange() {
        assertEquals(60, Levels.snap(60, 0, 100, 10));
        assertEquals(60, Levels.snap(57, 0, 100, 10));
        assertEquals(50, Levels.snap(54, 0, 100, 10));
        assertEquals(80, Levels.snap(100, 0, 80, 10));
        assertEquals(0, Levels.snap(-20, 0, 100, 10));
    }

    @Test
    public void menuAlpha_matchesTheSpecifiedMapping() {
        assertEquals(0.92f, Levels.menuAlpha(0), 0.001f);
        assertEquals(0.545f, Levels.menuAlpha(50), 0.001f);
        assertEquals(0.32f, Levels.menuAlpha(80), 0.001f);
    }

    @Test
    public void menuAlpha_decreasesAsTransparencyGrows() {
        for (int t = 10; t <= 80; t += 10) {
            assertTrue(Levels.menuAlpha(t) < Levels.menuAlpha(t - 10));
        }
    }

    @Test
    public void menuShadow_startsAtFifty() {
        assertFalse(Levels.menuShadow(40));
        assertTrue(Levels.menuShadow(50));
        assertTrue(Levels.menuShadow(80));
    }
}
