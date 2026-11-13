package io.github.phutrung08.glassdeck;

import android.content.Context;

import java.util.Locale;

/** Tên hiển thị của hình nền: hình đi kèm có tên đẹp (từ tài nguyên chuỗi), hình khác hiện tên file. */
final class WallpaperNames {
    private static final String[] KEYS = {
            "lua-hoang-hon", "lua-dai-duong", "lua-cuc-quang", "lua-hong-dao", "lua-lua-bang",
            "tidyo-xanh-duong", "tidyo-xanh-la", "tidyo-do", "tidyo-tim", "tidyo-vang", "tidyo-cam",
            "hoat-hinh-nui-chang-vang", "hoat-hinh-thanh-pho-dem"};

    private WallpaperNames() {}

    /** Tên file bỏ phần đuôi cuối cùng; tên bắt đầu bằng dấu chấm giữ nguyên. */
    static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /** Khoá của hình đi kèm (không phân biệt hoa thường), hoặc null nếu là hình khác. */
    static String knownKey(String fileName) {
        String base = baseName(fileName).toLowerCase(Locale.ROOT);
        for (String k : KEYS) if (k.equals(base)) return k;
        return null;
    }

    static String display(Context c, String fileName) {
        String key = knownKey(fileName);
        return key == null ? baseName(fileName) : c.getString(stringId(key));
    }

    private static int stringId(String key) {
        switch (key) {
            case "lua-hoang-hon": return R.string.wp_name_sunset;
            case "lua-dai-duong": return R.string.wp_name_ocean;
            case "lua-cuc-quang": return R.string.wp_name_aurora;
            case "lua-hong-dao": return R.string.wp_name_peach;
            case "lua-lua-bang": return R.string.wp_name_fire_ice;
            case "tidyo-xanh-duong": return R.string.wp_name_tidyo_blue;
            case "tidyo-xanh-la": return R.string.wp_name_tidyo_green;
            case "tidyo-do": return R.string.wp_name_tidyo_red;
            case "tidyo-tim": return R.string.wp_name_tidyo_purple;
            case "tidyo-vang": return R.string.wp_name_tidyo_yellow;
            case "tidyo-cam": return R.string.wp_name_tidyo_orange;
            case "hoat-hinh-nui-chang-vang": return R.string.wp_name_dusk_mountains;
            default: return R.string.wp_name_night_city;
        }
    }
}
