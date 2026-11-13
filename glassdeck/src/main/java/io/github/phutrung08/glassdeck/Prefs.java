package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Thiết lập của Glassdeck, gom một chỗ để khoá và giá trị mặc định không bị lặp. */
final class Prefs {
    static final String MODE_DEFAULT = "default", MODE_SINGLE = "single", MODE_MULTI = "multi";
    static final String TRANSPARENCY = "transparency", MENU_TRANSPARENCY = "menu_transparency";
    static final int TRANSPARENCY_DEFAULT = 60, MENU_TRANSPARENCY_DEFAULT = 50;

    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getSharedPreferences("home", Context.MODE_PRIVATE);
    }

    SharedPreferences.Editor edit() { return sp.edit(); }

    int getInt(String key, int def) { return sp.getInt(key, def); }

    void putInt(String key, int v) { sp.edit().putInt(key, v).apply(); }

    int transparency() {
        return Levels.snap(sp.getInt(TRANSPARENCY, TRANSPARENCY_DEFAULT), 0, 100, 10);
    }

    int menuTransparency() {
        return Levels.snap(sp.getInt(MENU_TRANSPARENCY, MENU_TRANSPARENCY_DEFAULT), 0, 80, 10);
    }

    boolean clock24h() { return sp.getBoolean("clock_24h", true); }

    void setClock24h(boolean on) { sp.edit().putBoolean("clock_24h", on).apply(); }

    String language() { return Lang.normalize(sp.getString("language", "")); }

    void setLanguage(String tag) { sp.edit().putString("language", Lang.normalize(tag)).apply(); }

    String wallpaperMode() { return sp.getString("wp_mode", MODE_DEFAULT); }

    void selectDefaultWallpaper() { sp.edit().putString("wp_mode", MODE_DEFAULT).apply(); }

    void selectSingleWallpaper(String name) {
        sp.edit().putString("wp_mode", MODE_SINGLE).putString("wp_single", name).apply();
    }

    void selectMultiWallpaper() { sp.edit().putString("wp_mode", MODE_MULTI).apply(); }

    String wallpaperSingle() { return sp.getString("wp_single", ""); }

    Set<String> wallpaperMulti() { return new HashSet<>(sp.getStringSet("wp_multi", new HashSet<String>())); }

    void setWallpaperMulti(Set<String> names) { sp.edit().putStringSet("wp_multi", names).apply(); }

    int interval() { return sp.getInt("wp_interval", Intervals.DEFAULT); }

    String order() { return sp.getString("order", ""); }

    void setOrder(String joined) { sp.edit().putString("order", joined).apply(); }

    Set<String> hidden() { return new HashSet<>(sp.getStringSet("hidden", new HashSet<String>())); }

    void setHidden(Set<String> keys) { sp.edit().putStringSet("hidden", keys).apply(); }
}
