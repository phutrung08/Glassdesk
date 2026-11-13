package io.github.phutrung08.glassdeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class WallpapersTest {
    private static Set<String> taken(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    @Test
    public void uniqueName_freeNameUnchanged() {
        assertEquals("a.jpg", Wallpapers.uniqueName("a.jpg", taken()::contains));
    }

    @Test
    public void uniqueName_addsSuffixBeforeExtension() {
        assertEquals("a-2.jpg", Wallpapers.uniqueName("a.jpg", taken("a.jpg")::contains));
    }

    @Test
    public void uniqueName_skipsTakenSuffixes() {
        assertEquals("a-4.jpg", Wallpapers.uniqueName("a.jpg", taken("a.jpg", "a-2.jpg", "a-3.jpg")::contains));
    }

    @Test
    public void uniqueName_noExtension() {
        assertEquals("photo-2", Wallpapers.uniqueName("photo", taken("photo")::contains));
    }

    @Test
    public void uniqueName_leadingDotIsNotAnExtension() {
        assertEquals(".hidden-2", Wallpapers.uniqueName(".hidden", taken(".hidden")::contains));
    }

    @Test
    public void uniqueName_onlyLastDotSplits() {
        assertEquals("a.b-2.png", Wallpapers.uniqueName("a.b.png", taken("a.b.png")::contains));
    }

    @Test
    public void isImageName_supportedExtensionsAnyCase() {
        assertTrue(Wallpapers.isImageName("a.jpg"));
        assertTrue(Wallpapers.isImageName("a.JPEG"));
        assertTrue(Wallpapers.isImageName("a.Png"));
        assertTrue(Wallpapers.isImageName("a.webp"));
    }

    @Test
    public void isImageName_rejectsOthersAndHiddenFiles() {
        assertFalse(Wallpapers.isImageName("a.gif"));
        assertFalse(Wallpapers.isImageName("a"));
        assertFalse(Wallpapers.isImageName(".copy123.tmp"));
        assertFalse(Wallpapers.isImageName(".hidden.jpg"));
    }
}
