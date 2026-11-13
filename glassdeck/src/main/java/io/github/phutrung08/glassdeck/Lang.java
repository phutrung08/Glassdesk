package io.github.phutrung08.glassdeck;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.util.Locale;

/** Ngôn ngữ giao diện do người dùng chọn. Chuỗi rỗng = theo hệ thống. */
final class Lang {
    /** Các lựa chọn, theo thứ tự hiện trong danh sách. */
    static final String[] TAGS = {"", "vi", "en", "zh", "ja", "ko", "fr"};

    private Lang() {}

    static boolean isSupported(String tag) {
        if (tag == null) return false;
        for (String t : TAGS) if (t.equals(tag)) return true;
        return false;
    }

    /** Giá trị hợp lệ để lưu: mã không biết thì coi như "theo hệ thống". */
    static String normalize(String tag) {
        return isSupported(tag) ? tag : "";
    }

    /** Locale tương ứng, hoặc null khi theo hệ thống. */
    static Locale locale(String tag) {
        String t = normalize(tag);
        return t.isEmpty() ? null : Locale.forLanguageTag(t);
    }

    /** Mã hai chữ cái hiện ở đầu dòng ("VI"); rỗng khi theo hệ thống. */
    static String code(String tag) {
        return normalize(tag).toUpperCase(Locale.ROOT);
    }

    static int indexOf(String tag) {
        String t = normalize(tag);
        for (int i = 0; i < TAGS.length; i++) if (TAGS[i].equals(t)) return i;
        return 0;
    }

    /** Bọc context bằng ngôn ngữ đã chọn; chưa chọn thì trả lại nguyên context. */
    static Context wrap(Context base) {
        Locale locale = locale(new Prefs(base).language());
        if (locale == null) {
            Locale.setDefault(android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0));
            return base;
        }
        // Để định dạng số (dấu thập phân của dung lượng) theo đúng ngôn ngữ đã chọn.
        Locale.setDefault(locale);
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.setLocales(new LocaleList(locale));
        return base.createConfigurationContext(conf);
    }

    /** Locale đang dùng của một context (đã bọc hoặc theo hệ thống). */
    static Locale current(Context c) {
        return c.getResources().getConfiguration().getLocales().get(0);
    }
}
