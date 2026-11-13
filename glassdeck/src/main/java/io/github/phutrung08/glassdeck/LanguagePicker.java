package io.github.phutrung08.glassdeck;

import android.content.Context;

/** Chọn ngôn ngữ giao diện. Đổi ngôn ngữ thì dựng lại màn hình để mọi chữ đổi theo. */
final class LanguagePicker {
    private final HomeActivity h;

    LanguagePicker(HomeActivity h) {
        this.h = h;
    }

    /** Tên ngôn ngữ viết bằng chính ngôn ngữ đó; "theo hệ thống" thì dịch theo giao diện hiện tại. */
    static String nativeName(Context c, String tag) {
        switch (Lang.normalize(tag)) {
            case "vi": return c.getString(R.string.lang_vi);
            case "en": return c.getString(R.string.lang_en);
            case "zh": return c.getString(R.string.lang_zh);
            case "ja": return c.getString(R.string.lang_ja);
            case "ko": return c.getString(R.string.lang_ko);
            case "fr": return c.getString(R.string.lang_fr);
            default: return c.getString(R.string.language_system);
        }
    }

    void show() {
        final String current = h.prefs().language();
        GlassMenu menu = new GlassMenu(h, h.getString(R.string.settings_language))
                .focus(Lang.indexOf(current))
                .onBack(() -> h.settings().show(SettingsMenu.LANGUAGE, true));
        for (String tag : Lang.TAGS) {
            GlassMenu.Row row = new GlassMenu.Row(nativeName(h, tag), () -> choose(tag, current)).keepOpen();
            if (tag.isEmpty()) row.icon(R.drawable.ic_translate);
            else row.code(Lang.code(tag));
            if (tag.equals(current)) row.trailingIcon(R.drawable.ic_check);
            menu.add(row);
        }
        menu.show();
    }

    private void choose(String tag, String current) {
        if (tag.equals(current)) {
            h.settings().show(SettingsMenu.LANGUAGE, true);
            return;
        }
        h.prefs().setLanguage(tag);
        h.recreate();
    }
}
