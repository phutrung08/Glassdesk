package io.github.phutrung08.glassdeck;

import android.content.Intent;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import io.github.phutrung08.common.DeviceStats;
import io.github.phutrung08.common.Sizes;

import java.io.File;
import java.util.Set;

/** Menu Cài đặt: các dòng có biểu tượng, giá trị hoặc ô điều khiển, và hai nút nhỏ ở cuối. */
final class SettingsMenu {
    // Vị trí các dòng, để quay lại đúng dòng sau khi chỉnh xong ở bảng con.
    static final int WALLPAPER = 0, TRAY_OPACITY = 1, MENU_OPACITY = 2, CLOCK = 3, SOUND = 4, LANGUAGE = 5;

    private static final String TIDYO = "io.github.phutrung08.tidyo";

    private final HomeActivity h;

    SettingsMenu(HomeActivity h) {
        this.h = h;
    }

    void show(int focus, boolean animate) {
        final Prefs prefs = h.prefs();
        final GlassMenu menu = new GlassMenu(h, h.getString(R.string.settings_title)).focus(focus).animate(animate);
        menu.widthPercent(50);

        menu.add(new GlassMenu.Row(h.getString(R.string.settings_wallpaper),
                () -> h.wallpaperPicker().open())
                .icon(R.drawable.ic_wallpaper).value(wallpaperValue()).chevron().keepOpen());

        menu.add(new GlassMenu.Row(h.getString(R.string.settings_tray_opacity),
                () -> h.sliderPanel().show(R.string.settings_tray_opacity, Prefs.TRANSPARENCY,
                        0, 100, Prefs.TRANSPARENCY_DEFAULT, TRAY_OPACITY))
                .icon(R.drawable.ic_opacity).trailing(percentBar(prefs.transparency(), 100)).keepOpen());

        menu.add(new GlassMenu.Row(h.getString(R.string.settings_menu_opacity),
                () -> h.sliderPanel().show(R.string.settings_menu_opacity, Prefs.MENU_TRANSPARENCY,
                        0, 80, Prefs.MENU_TRANSPARENCY_DEFAULT, MENU_OPACITY))
                .icon(R.drawable.ic_layers).trailing(percentBar(prefs.menuTransparency(), 80)).keepOpen());

        final SwitchView clockSwitch = new SwitchView(h);
        clockSwitch.setDuplicateParentStateEnabled(true);
        clockSwitch.setChecked(prefs.clock24h(), false);
        menu.add(new GlassMenu.Row(h.getString(R.string.settings_clock24), () -> {
            // Bật/tắt ngay tại chỗ, không đóng menu.
            boolean on = !clockSwitch.isChecked();
            prefs.setClock24h(on);
            clockSwitch.setChecked(on, true);
            h.applyClock();
        }).icon(R.drawable.ic_schedule).trailing(clockSwitch).keepOpen());

        menu.add(new GlassMenu.Row(h.getString(R.string.settings_sound),
                () -> h.sliderPanel().show(R.string.settings_sound, Prefs.SOUND_VOLUME,
                        0, 100, Prefs.SOUND_VOLUME_DEFAULT, SOUND, h::previewSound))
                .icon(R.drawable.ic_volume_up).trailing(percentBar(prefs.soundVolume(), 100)).keepOpen());

        menu.add(new GlassMenu.Row(h.getString(R.string.settings_language),
                () -> h.languagePicker().show())
                .icon(R.drawable.ic_translate).value(LanguagePicker.nativeName(h, prefs.language()))
                .chevron().keepOpen());

        DeviceStats stats = DeviceStats.read(h);
        menu.add(new GlassMenu.Row(h.getString(R.string.settings_cleanup),
                () -> h.startActivity(new Intent(h, CleanupActivity.class)))
                .icon(R.drawable.ic_cleaning_services)
                .value(h.getString(R.string.free_value, Sizes.format(stats.storageFree))).chevron());

        int hiddenCount = prefs.hidden().size();
        menu.add(new GlassMenu.Row(h.getString(R.string.settings_hidden_apps), h::restoreHiddenApps)
                .icon(R.drawable.ic_visibility_off).value(String.valueOf(hiddenCount)));

        menu.footer(footer());
        menu.show();
    }

    private String wallpaperValue() {
        Prefs prefs = h.prefs();
        String mode = prefs.wallpaperMode();
        if (Prefs.MODE_MULTI.equals(mode)) {
            int n = 0;
            Set<String> chosen = prefs.wallpaperMulti();
            for (File f : Wallpapers.list(h)) if (chosen.contains(f.getName())) n++;
            if (n > 0) return h.getResources().getQuantityString(R.plurals.wallpaper_value_auto, n, n);
        } else if (Prefs.MODE_SINGLE.equals(mode)) {
            String name = prefs.wallpaperSingle();
            for (File f : Wallpapers.list(h)) {
                if (f.getName().equals(name)) return WallpaperNames.display(h, name);
            }
        }
        return h.getString(R.string.wallpaper_default);
    }

    /** Thanh nhỏ 60 x 4 dp và số phần trăm ở cuối dòng. */
    private View percentBar(int value, int max) {
        LinearLayout box = new LinearLayout(h);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setDuplicateParentStateEnabled(true);
        MiniBar bar = new MiniBar(h);
        bar.setFraction(value / (float) max);
        box.addView(bar);
        GlassText pct = GlassUi.rowText(h, h.getString(R.string.percent, value), 15, GlassUi.TEXT_SUB);
        pct.setMinWidth(GlassUi.dp(h, 44));
        pct.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = GlassUi.dp(h, 10);
        box.addView(pct, lp);
        return box;
    }

    /** Đường kẻ mảnh và hàng hai nút nhỏ: cài đặt của tivi, và Tidyo TV. */
    private View footer() {
        LinearLayout footer = new LinearLayout(h);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.addView(GlassUi.divider(h));
        LinearLayout bar = new LinearLayout(h);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(GlassUi.dp(h, 8), GlassUi.dp(h, 2), 0, GlassUi.dp(h, 2));

        Chip tv = new Chip(h, R.drawable.ic_settings, h.getString(R.string.settings_tv_settings), 0, true);
        tv.onClick(() -> {
            if (h.overlays().guard()) return;
            h.overlays().close();
            try {
                h.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception e) {
                h.toast(R.string.toast_settings_failed);
            }
        });
        Chip tidyo = new Chip(h, R.drawable.ic_tv, h.getString(R.string.settings_tidyo), 0, true);
        tidyo.onClick(() -> {
            if (h.overlays().guard()) return;
            h.overlays().close();
            Intent i = h.getPackageManager().getLeanbackLaunchIntentForPackage(TIDYO);
            if (i == null) h.toast(R.string.toast_tidyo_missing);
            else h.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        });
        bar.addView(tv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = GlassUi.dp(h, 10);
        bar.addView(tidyo, lp);
        footer.addView(bar);
        return footer;
    }
}
