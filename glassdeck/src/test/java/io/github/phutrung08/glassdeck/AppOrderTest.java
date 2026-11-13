package io.github.phutrung08.glassdeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class AppOrderTest {
    private static final List<String> NONE = Collections.emptyList();

    @Test
    public void utilityPrefixesAndExactName() {
        assertTrue(AppOrder.isUtility("com.sony.dtv.settings"));
        assertTrue(AppOrder.isUtility("com.realtek.x"));
        assertTrue(AppOrder.isUtility("com.android.vending"));
        assertTrue(AppOrder.isUtility("com.google.android.play.games"));
        assertFalse(AppOrder.isUtility("com.google.android.youtube.tv"));
        assertFalse(AppOrder.isUtility("com.google.android.play.games.extra"));
        assertFalse(AppOrder.isUtility("vn.cleantv.app"));
    }

    @Test
    public void bothSaved_followSavedPosition() {
        List<String> saved = Arrays.asList("b", "a");
        assertTrue(AppOrder.compare(saved, "b", true, "Z", "a", false, "A") < 0);
        assertTrue(AppOrder.compare(saved, "a", false, "A", "b", true, "Z") > 0);
    }

    @Test
    public void onlyFirstSaved_goesFirst() {
        List<String> saved = Collections.singletonList("x");
        assertEquals(-1, AppOrder.compare(saved, "x", true, "Z", "y", false, "A"));
    }

    @Test
    public void onlySecondSaved_goesFirst() {
        List<String> saved = Collections.singletonList("y");
        assertEquals(1, AppOrder.compare(saved, "x", false, "A", "y", true, "Z"));
    }

    @Test
    public void unsaved_entertainmentBeforeUtility() {
        assertEquals(1, AppOrder.compare(NONE, "x", true, "A", "y", false, "B"));
        assertEquals(-1, AppOrder.compare(NONE, "x", false, "Z", "y", true, "A"));
    }

    @Test
    public void unsaved_sameKind_byLabelIgnoringCase() {
        assertTrue(AppOrder.compare(NONE, "x", false, "apple", "y", false, "Banana") < 0);
        assertTrue(AppOrder.compare(NONE, "x", false, "Banana", "y", false, "apple") > 0);
        assertEquals(0, AppOrder.compare(NONE, "x", false, "Same", "y", false, "same"));
    }

    @Test
    public void sortsWholeList() {
        List<String> saved = Arrays.asList("s2", "s1");
        // {key, utility, label}
        List<String[]> apps = new ArrayList<>(Arrays.asList(
                new String[]{"u", "1", "Alpha tool"},
                new String[]{"s1", "0", "Saved one"},
                new String[]{"e", "0", "Zeta"},
                new String[]{"s2", "1", "Saved two"},
                new String[]{"d", "0", "delta"}));
        Collections.sort(apps, (x, y) -> AppOrder.compare(saved,
                x[0], x[1].equals("1"), x[2], y[0], y[1].equals("1"), y[2]));
        List<String> keys = new ArrayList<>();
        for (String[] a : apps) keys.add(a[0]);
        assertEquals(Arrays.asList("s2", "s1", "d", "e", "u"), keys);
    }

    @Test
    public void compareBySize_largerFirst_thenLabel() {
        assertEquals(-1, AppOrder.compareBySize(200, "B", 100, "A"));
        assertEquals(1, AppOrder.compareBySize(100, "A", 200, "B"));
        assertTrue(AppOrder.compareBySize(100, "a", 100, "B") < 0);
        assertEquals(0, AppOrder.compareBySize(100, "x", 100, "X"));
    }

    @Test
    public void parse_emptyAndNull_giveEmptyList() {
        assertTrue(AppOrder.parse("").isEmpty());
        assertTrue(AppOrder.parse(null).isEmpty());
    }

    @Test
    public void parseAndJoin_roundTrip() {
        List<String> keys = Arrays.asList("a/.A", "b/.B", "c/.C");
        String joined = AppOrder.join(keys);
        assertEquals("a/.A\nb/.B\nc/.C", joined);
        assertEquals(keys, AppOrder.parse(joined));
        assertEquals("", AppOrder.join(Collections.<String>emptyList()));
    }
}
