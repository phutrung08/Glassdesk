package io.github.phutrung08.glassdeck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LangTest {
    @Test
    public void supportedTags() {
        for (String t : new String[]{"", "vi", "en", "zh", "ja", "ko", "fr"}) assertTrue(t, Lang.isSupported(t));
    }

    @Test
    public void unsupportedTags() {
        assertFalse(Lang.isSupported(null));
        assertFalse(Lang.isSupported("de"));
        assertFalse(Lang.isSupported("VI"));
        assertFalse(Lang.isSupported("vi-VN"));
        assertFalse(Lang.isSupported(" vi"));
    }

    @Test
    public void normalize_unknownBecomesSystem() {
        assertEquals("vi", Lang.normalize("vi"));
        assertEquals("", Lang.normalize("de"));
        assertEquals("", Lang.normalize(null));
    }

    @Test
    public void locale_mapsTagOrNullForSystem() {
        assertNull(Lang.locale(""));
        assertNull(Lang.locale("xx"));
        assertEquals("ja", Lang.locale("ja").getLanguage());
        assertEquals("fr", Lang.locale("fr").toLanguageTag());
    }

    @Test
    public void code_isUpperCaseTwoLetters() {
        assertEquals("VI", Lang.code("vi"));
        assertEquals("KO", Lang.code("ko"));
        assertEquals("", Lang.code(""));
        assertEquals("", Lang.code("de"));
    }

    @Test
    public void indexOf_followsListOrder() {
        assertEquals(0, Lang.indexOf(""));
        assertEquals(1, Lang.indexOf("vi"));
        assertEquals(6, Lang.indexOf("fr"));
        assertEquals(0, Lang.indexOf("de"));
    }
}
