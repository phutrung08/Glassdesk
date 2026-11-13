package io.github.phutrung08.glassdeck;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import io.github.phutrung08.common.Ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Màn hình chính: nền cố định, đồng hồ lớn, nút kính mờ ở góc trên,
 * và một khay kính mờ ở dưới chứa hàng app trượt ngang (thấy 6 app một lúc).
 * Các bảng cài đặt nằm ở các lớp riêng (SettingsMenu, WallpaperPicker...) và nổi lên qua OverlayHost.
 */
public class HomeActivity extends Activity {
    private static final class App {
        ComponentName cn;
        String label;
        Drawable banner, icon;
        int color = Palette.NEUTRAL;
        String key() { return cn.flattenToShortString(); }
    }

    private static final float FOCUS_SCALE = 1.12f;
    private static final DecelerateInterpolator EASE = new DecelerateInterpolator(1.6f);
    private static final String TIDYO = "io.github.phutrung08.tidyo";
    private static final int SLIDE_FADE_MS = 700, PREVIEW_FADE_MS = 300, PREVIEW_DEBOUNCE_MS = 300;

    private Prefs prefs;
    private final List<App> apps = new ArrayList<>();
    private final Map<String, View> tiles = new HashMap<>();
    private FrameLayout root, content, tray;
    private LinearLayout pills, row;
    private HorizontalScrollView scroll;
    private TextView inputsPill, settingsPill;
    private TextClock clock, ampm, date;
    private String lastFocused;
    private int tileW, tileH;

    // Các bảng cài đặt
    private ExecutorService exec;
    private Thumbnails thumbs;
    private OverlayHost overlays;
    private SettingsMenu settings;
    private WallpaperPicker wallpaperPicker;
    private SliderPanel sliderPanel;
    private NavSound sound;
    private IntervalPicker intervalPicker;
    private LanguagePicker languagePicker;
    private StorageBrowser browser;

    // Hình nền
    private Drawable defaultBg;
    private ImageView wallA, wallB;
    private boolean wallBFront;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<File> slides = new ArrayList<>();
    private int slideIndex;
    private int wallpaperJob, appsJob;
    private Bitmap frostSource;
    private float frostLuminance;
    private boolean frostDefault = true;
    private final Runnable nextSlide = this::showNextSlide;
    // Tên file đang hiện trên màn hình ("" = nền mặc định) và lượt xem hình nền trong Cài đặt.
    private String shownKey = "";
    private boolean browsing;
    private String browseStartKey;
    private File previewFile;
    private final Runnable previewRunnable = this::runPreview;

    private final BroadcastReceiver packageChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { loadApps(); }
    };

    // ---------- Dựng giao diện ----------

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        exec = Executors.newFixedThreadPool(2);
        thumbs = new Thumbnails(() -> !isDestroyed());
        GlassUi.setShadow(Levels.menuShadow(prefs.menuTransparency()));

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int trayW = screenW - 2 * dp(48);
        tileW = (trayW - 2 * dp(18) - 5 * dp(14)) / 6;
        tileH = tileW * 9 / 16;

        Frost.screenW = screenW;
        Frost.screenH = getResources().getDisplayMetrics().heightPixels;

        root = new FrameLayout(this);
        defaultBg = Ui.background();
        root.setBackground(defaultBg);
        // Hai lớp hình chồng nhau để chuyển hình bằng cách mờ dần.
        wallA = wallpaperLayer();
        wallB = wallpaperLayer();
        content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        inputsPill = pill(getString(R.string.pill_inputs), v -> showInputs());
        settingsPill = pill(getString(R.string.pill_settings), v -> settings.show(0, true));
        pills.addView(inputsPill);
        LinearLayout.LayoutParams lpS = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpS.leftMargin = dp(10);
        pills.addView(settingsPill, lpS);
        FrameLayout.LayoutParams lpPills = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lpPills.setMargins(0, dp(30), dp(48), 0);
        content.addView(pills, lpPills);

        // Đồng hồ: bên trái, giữa màn hình theo chiều dọc.
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);

        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setOrientation(LinearLayout.HORIZONTAL);
        clock = new TextClock(this);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 84);
        clock.setTextColor(0xFFFFFFFF);
        clock.setTypeface(Ui.medium());
        clock.setIncludeFontPadding(false);
        clock.setPadding(dp(56), 0, 0, 0);
        clock.setShadowLayer(dp(8), 0, dp(1), 0x59000000);
        clockRow.addView(clock);
        // Chữ sáng/chiều nhỏ nằm cùng đường cơ sở với giờ; chỉ hiện khi dùng đồng hồ 12 giờ.
        ampm = new TextClock(this);
        ampm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        ampm.setTextColor(0xE0FFFFFF);
        ampm.setTypeface(Ui.medium());
        ampm.setIncludeFontPadding(false);
        ampm.setPadding(dp(10), 0, 0, 0);
        ampm.setShadowLayer(dp(6), 0, dp(1), 0x59000000);
        clockRow.addView(ampm);
        clockBox.addView(clockRow);

        date = new TextClock(this);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        date.setTextColor(0xE0FFFFFF);
        date.setShadowLayer(dp(6), 0, dp(1), 0x73000000);
        date.setPadding(dp(58), dp(6), 0, 0);
        clockBox.addView(date);
        content.addView(clockBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL));
        applyClock();

        // Khay kính: đứng yên, hàng app bên trong trượt ngang và bị cắt theo góc bo của khay.
        tray = new FrameLayout(this);
        tray.setBackground(new Frost.Glass(tray, dp(22), false, dp(1)));
        tray.setClipToOutline(true);
        scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(dp(11), dp(16), dp(11), dp(16));
        scroll.addView(row);
        tray.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lpTray = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpTray.setMargins(dp(48), 0, dp(48), 0);
        bottom.addView(tray, lpTray);

        FrameLayout.LayoutParams lpBottom = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lpBottom.bottomMargin = dp(36);
        content.addView(bottom, lpBottom);
        setContentView(root);

        overlays = new OverlayHost(this);
        settings = new SettingsMenu(this);
        wallpaperPicker = new WallpaperPicker(this);
        sliderPanel = new SliderPanel(this);
        intervalPicker = new IntervalPicker(this);
        languagePicker = new LanguagePicker(this);
        browser = new StorageBrowser(this);
        sound = NavSound.install(this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageChanged, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageChanged, f);
        }

        applyWallpaper(false);
        loadApps();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packageChanged);
        handler.removeCallbacksAndMessages(null);
        // Kết quả của luồng nền còn đang chạy trở thành cũ và sẽ bị bỏ qua.
        wallpaperJob++;
        appsJob++;
        exec.shutdownNow();
        thumbs.shutdown();
        sound.release();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Bấm Home khi đang ở màn hình chính: đóng hộp thoại, quay về ô đầu tiên.
        overlays.close();
        endWallpaperBrowse(true);
        focusFirst();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(nextSlide);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleNextSlide();
        if (!overlays.isOpen() && getCurrentFocus() == null) focusKey(lastFocused);
    }

    @Override
    public void onBackPressed() {
        if (overlays.isOpen()) {
            overlays.back();
            return;
        }
        View first = row.getChildCount() > 0 ? row.getChildAt(0) : null;
        if (first != null && !first.isFocused()) {
            focusFirst();
        } else if (!isDefaultHome()) {
            finish();
        }
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    /** Nút kính ở góc trên: dùng nút chung của Ui nhưng nền là kính mờ theo hình nền. */
    private TextView pill(String label, View.OnClickListener onClick) {
        TextView t = Ui.pill(this, label, onClick);
        t.setBackground(Ui.focusBackground(this, new Frost.Glass(t, dp(40), false, dp(1)), 40));
        return t;
    }

    // ---------- Phần các bảng cài đặt dùng chung ----------

    Prefs prefs() { return prefs; }
    OverlayHost overlays() { return overlays; }

    void previewSound() { sound.preview(); }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        sound.onKey(e);
        return super.dispatchKeyEvent(e);
    }
    Thumbnails thumbs() { return thumbs; }
    SettingsMenu settings() { return settings; }
    WallpaperPicker wallpaperPicker() { return wallpaperPicker; }
    SliderPanel sliderPanel() { return sliderPanel; }
    IntervalPicker intervalPicker() { return intervalPicker; }
    LanguagePicker languagePicker() { return languagePicker; }
    FrameLayout root() { return root; }
    FrameLayout content() { return content; }

    /** Mép dưới của hàng nút phía trên và mép trên của khay: bảng chỉ được nằm giữa hai mép này. */
    int pillsBottom() { return pills.getBottom(); }
    int trayTop() { return ((View) tray.getParent()).getTop() + tray.getTop(); }

    void focusLast() {
        focusKey(lastFocused);
    }

    void toast(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
    }

    void toast(CharSequence text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    void toastLong(int res) {
        Toast.makeText(this, res, Toast.LENGTH_LONG).show();
    }

    /** Chạy việc nặng ở luồng nền; sau khi màn hình huỷ thì bỏ qua. */
    void runBackground(Runnable r) {
        try {
            exec.execute(r);
        } catch (RejectedExecutionException ignored) {
            // Màn hình đang đóng.
        }
    }

    /** Đặt lại đồng hồ theo thiết lập 12/24 giờ và ngôn ngữ (cả dòng ngày). */
    void applyClock() {
        boolean h24 = prefs.clock24h();
        String f = h24 ? "HH:mm" : "h:mm";
        clock.setFormat24Hour(f);
        clock.setFormat12Hour(f);
        ampm.setFormat24Hour("a");
        ampm.setFormat12Hour("a");
        ampm.setVisibility(h24 ? View.GONE : View.VISIBLE);
        String pattern = DateFormat.getBestDateTimePattern(Lang.current(this), "EEEEdMMMM");
        date.setFormat24Hour(pattern);
        date.setFormat12Hour(pattern);
    }

    // ---------- Danh sách app ----------

    private void loadApps() {
        final int job = ++appsJob;
        runBackground(() -> {
            PackageManager pm = getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            List<App> found = new ArrayList<>();
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                ActivityInfo ai = ri.activityInfo;
                if (ai.packageName.equals(getPackageName())) continue;
                // Tidyo TV nằm trong menu Cài đặt, không chiếm chỗ trong khay.
                if (ai.packageName.equals(TIDYO)) continue;
                App a = new App();
                a.cn = new ComponentName(ai.packageName, ai.name);
                a.label = String.valueOf(ri.loadLabel(pm));
                try {
                    a.banner = ai.loadBanner(pm);
                    if (a.banner == null) a.banner = ai.applicationInfo.loadBanner(pm);
                    if (a.banner == null) {
                        a.icon = ri.loadIcon(pm);
                        a.color = Palette.dominant(a.icon);
                    }
                } catch (Exception ignored) { }
                found.add(a);
            }
            runOnUiThread(() -> {
                if (job != appsJob || isDestroyed()) return;
                apps.clear();
                apps.addAll(found);
                rebuildRow(lastFocused);
            });
        });
    }

    /** App đang hiện, theo thứ tự đã lưu; app chưa có trong thứ tự xếp sau. */
    private List<App> visibleSorted() {
        Set<String> hide = prefs.hidden();
        List<String> order = AppOrder.parse(prefs.order());
        List<App> out = new ArrayList<>();
        for (App a : apps) if (!hide.contains(a.key())) out.add(a);
        Collections.sort(out, (x, y) -> AppOrder.compare(order,
                x.key(), AppOrder.isUtility(x.cn.getPackageName()), x.label,
                y.key(), AppOrder.isUtility(y.cn.getPackageName()), y.label));
        return out;
    }

    private void saveOrder(List<App> list) {
        List<String> keys = new ArrayList<>();
        for (App a : list) keys.add(a.key());
        prefs.setOrder(AppOrder.join(keys));
    }

    private void rebuildRow(String focusKey) {
        row.removeAllViews();
        tiles.clear();
        for (App a : visibleSorted()) row.addView(makeTile(a));
        row.post(() -> { if (!overlays.isOpen()) focusKey(focusKey); });
    }

    /** Hiện lại mọi app đã ẩn (từ menu Cài đặt). */
    void restoreHiddenApps() {
        int n = prefs.hidden().size();
        if (n == 0) {
            toast(R.string.toast_no_hidden);
            return;
        }
        prefs.setHidden(new HashSet<String>());
        rebuildRow(lastFocused);
        toast(getResources().getQuantityString(R.plurals.toast_hidden_restored, n, n));
    }

    private void focusFirst() {
        if (row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    private void focusKey(String key) {
        View t = key == null ? null : tiles.get(key);
        if (t != null) t.requestFocus();
        else focusFirst();
    }

    // ---------- Ô ứng dụng ----------

    private View makeTile(App a) {
        final int radius = dp(12);
        FrameLayout tile = new FrameLayout(this);
        tile.setId(View.generateViewId());
        tile.setBackgroundColor(Palette.scale(a.color, 0.8f));
        tile.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        tile.setClipToOutline(true);

        if (a.banner != null) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(a.banner);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            // App không có ảnh bìa: biểu tượng và tên nằm ngay trong ô.
            LinearLayout inner = new LinearLayout(this);
            inner.setGravity(Gravity.CENTER_VERTICAL);
            inner.setPadding(dp(10), 0, dp(8), 0);
            if (a.icon != null) {
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(a.icon);
                inner.addView(iv, new LinearLayout.LayoutParams(dp(36), dp(36)));
            }
            TextView name = new TextView(this);
            name.setText(a.label);
            name.setTextColor(0xFFFFFFFF);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            name.setMaxLines(2);
            name.setPadding(dp(8), 0, 0, 0);
            inner.addView(name);
            tile.addView(inner, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setNextFocusUpId(inputsPill.getId());
        tile.setOnClickListener(v -> launch(a));
        // Giữ OK (hoặc bấm phím Menu) để mở menu sắp xếp. Tự bắt phím lặp đầu tiên thay vì dùng
        // long-click của View, để lần nhả phím sau đó không bị tính là một lần bấm mở app.
        final boolean[] menuOpened = {false};
        tile.setOnKeyListener((v, code, ev) -> {
            if (code == KeyEvent.KEYCODE_MENU && ev.getAction() == KeyEvent.ACTION_UP) {
                showAppMenu(a);
                return true;
            }
            boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER;
            if (!ok) return false;
            if (ev.getAction() == KeyEvent.ACTION_DOWN && ev.getRepeatCount() == 1) {
                menuOpened[0] = true;
                v.setPressed(false);
                showAppMenu(a);
                return true;
            }
            if (menuOpened[0]) {
                if (ev.getAction() == KeyEvent.ACTION_UP) menuOpened[0] = false;
                return true;
            }
            return false;
        });
        tile.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? FOCUS_SCALE : 1f).scaleY(has ? FOCUS_SCALE : 1f)
                    .translationZ(has ? dp(10) : 0).setDuration(220).setInterpolator(EASE).start();
            if (has) {
                lastFocused = a.key();
                inputsPill.setNextFocusDownId(v.getId());
                settingsPill.setNextFocusDownId(v.getId());
                centerOn(v);
            }
        });
        tiles.put(a.key(), tile);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
        lp.setMargins(dp(7), 0, dp(7), 0);
        tile.setLayoutParams(lp);
        return tile;
    }

    private void centerOn(View tile) {
        scroll.post(() -> {
            int target = tile.getLeft() + tile.getWidth() / 2 - scroll.getWidth() / 2;
            scroll.smoothScrollTo(Math.max(0, target), 0);
        });
    }

    private void launch(App a) {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setComponent(a.cn)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (Exception e) {
            toast(getString(R.string.toast_cannot_open, a.label));
        }
    }

    // ---------- Menu app và cổng vào ----------

    private void showAppMenu(App a) {
        GlassMenu menu = new GlassMenu(this, a.label);
        menu.add(new GlassMenu.Row(getString(R.string.app_menu_to_front), () -> moveApp(a, 0)));
        menu.add(new GlassMenu.Row(getString(R.string.app_menu_move_left), () -> moveApp(a, 1)));
        menu.add(new GlassMenu.Row(getString(R.string.app_menu_move_right), () -> moveApp(a, 2)));
        menu.add(new GlassMenu.Row(getString(R.string.app_menu_hide), () -> {
            Set<String> h = prefs.hidden();
            h.add(a.key());
            prefs.setHidden(h);
            lastFocused = null;
            rebuildRow(null);
        }));
        menu.add(new GlassMenu.Row(getString(R.string.app_menu_info), () -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + a.cn.getPackageName())));
            } catch (Exception e) {
                toast(R.string.toast_info_failed);
            }
        }));
        menu.show();
    }

    /** @param how 0 = đưa lên đầu, 1 = sang trái, 2 = sang phải */
    private void moveApp(App a, int how) {
        List<App> list = visibleSorted();
        int i = -1;
        for (int k = 0; k < list.size(); k++) if (list.get(k).key().equals(a.key())) i = k;
        if (i < 0) return;
        if (how == 0) list.add(0, list.remove(i));
        else if (how == 1 && i > 0) Collections.swap(list, i, i - 1);
        else if (how == 2 && i < list.size() - 1) Collections.swap(list, i, i + 1);
        saveOrder(list);
        rebuildRow(a.key());
    }

    private void showInputs() {
        List<TvInputInfo> inputs = new ArrayList<>();
        try {
            TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tim != null) {
                for (TvInputInfo info : tim.getTvInputList()) {
                    if (info.isHidden(this)) continue;
                    // Chỉ lấy cổng thật (HDMI, AV, ăng-ten), bỏ "kênh" do các app tự đăng ký.
                    boolean builtIn = info.isPassthroughInput()
                            || (info.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                            && !info.getServiceInfo().packageName.startsWith("com.google.");
                    if (builtIn) inputs.add(info);
                }
            }
        } catch (Exception ignored) { }
        Collections.sort(inputs, (x, y) ->
                String.valueOf(x.loadLabel(this)).compareToIgnoreCase(String.valueOf(y.loadLabel(this))));
        if (inputs.isEmpty()) {
            toastLong(R.string.toast_no_inputs);
            return;
        }
        GlassMenu menu = new GlassMenu(this, getString(R.string.pill_inputs));
        for (TvInputInfo info : inputs) {
            CharSequence custom = info.loadCustomLabel(this);
            final String name = String.valueOf(custom != null ? custom : info.loadLabel(this));
            menu.add(new GlassMenu.Row(name, () -> {
                Uri uri = info.isPassthroughInput()
                        ? TvContract.buildChannelUriForPassthroughInput(info.getId())
                        : TvContract.Channels.CONTENT_URI;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Exception e) {
                    toast(getString(R.string.toast_input_switch_failed, name));
                }
            }));
        }
        menu.show();
    }

    // ---------- Kính mờ ----------

    /** Cập nhật kính khay/nút (theo "Độ trong của khay và nút") và nền menu (theo "Độ trong của menu"). */
    void refreshFrost() {
        int menuT = prefs.menuTransparency();
        GlassUi.setShadow(Levels.menuShadow(menuT));
        if (frostSource != null) {
            Frost.update(frostSource, frostLuminance, prefs.transparency(), menuT, frostDefault);
        }
    }

    // ---------- Hình nền ----------

    private ImageView wallpaperLayer() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        v.setAlpha(0f);
        root.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return v;
    }

    /** Tên file của hình đang được dùng ("" = nền mặc định); trong lúc xem hình nền là hình dùng trước khi xem. */
    String appliedKey() {
        return browseStartKey != null ? browseStartKey : shownKey;
    }

    /** Bắt đầu xem hình nền trong Cài đặt: tạm dừng tự đổi và nhớ hình đang dùng để khôi phục. */
    void beginWallpaperBrowse() {
        if (browsing) return;
        browsing = true;
        browseStartKey = shownKey;
        wallpaperJob++;   // việc nạp hình tự đổi đang dở trở thành cũ
        handler.removeCallbacks(nextSlide);
    }

    /**
     * Kết thúc lượt xem.
     * @param restore true: không chọn gì mới, trả hình nền về như lúc bắt đầu. false: người dùng sẽ tự áp dụng hình mới.
     */
    void endWallpaperBrowse(boolean restore) {
        if (!browsing) return;
        browsing = false;
        handler.removeCallbacks(previewRunnable);
        wallpaperJob++;   // bỏ kết quả xem trước chưa kịp hiện
        String start = browseStartKey;
        browseStartKey = null;
        if (!restore) return;
        if (!shownKey.equals(start)) applyWallpaper(true);
        else scheduleNextSlide();
    }

    /** Xem trước một hình (null = nền mặc định) phía sau bảng, sau 300 ms đứng yên. */
    void previewWallpaper(File f) {
        handler.removeCallbacks(previewRunnable);
        if ((f == null ? "" : f.getName()).equals(shownKey)) {
            wallpaperJob++;   // đang xem đúng hình này rồi; bỏ kết quả của hình trước đó chưa kịp hiện
            return;
        }
        previewFile = f;
        handler.postDelayed(previewRunnable, PREVIEW_DEBOUNCE_MS);
    }

    private void runPreview() {
        if (!browsing) return;
        if (previewFile == null) showDefaultWallpaper(PREVIEW_FADE_MS);
        else loadImage(previewFile, PREVIEW_FADE_MS, false);
    }

    /** Đọc thiết lập và hiện hình nền tương ứng. */
    void applyWallpaper(boolean animate) {
        handler.removeCallbacks(nextSlide);
        slides.clear();
        String mode = prefs.wallpaperMode();
        List<File> files = Wallpapers.list(this);
        if (mode.equals(Prefs.MODE_SINGLE)) {
            String name = prefs.wallpaperSingle();
            for (File f : files) if (f.getName().equals(name)) slides.add(f);
        } else if (mode.equals(Prefs.MODE_MULTI)) {
            Set<String> chosen = prefs.wallpaperMulti();
            for (File f : files) if (chosen.contains(f.getName())) slides.add(f);
        }
        if (slides.isEmpty()) {
            // Nền mặc định, hoặc hình đã chọn không còn trên tivi.
            showDefaultWallpaper(animate ? SLIDE_FADE_MS : 0);
            return;
        }
        slideIndex = 0;
        loadSlide(animate);
    }

    private void showDefaultWallpaper(int ms) {
        wallpaperJob++;
        shownKey = "";
        showWallpaper(null, Wallpapers.small(defaultBg, Frost.screenW, Frost.screenH), ms);
    }

    private void loadSlide(boolean animate) {
        loadImage(slides.get(slideIndex % slides.size()), animate ? SLIDE_FADE_MS : 0, true);
    }

    /** Nạp một hình ở luồng nền rồi hiện bằng cách mờ dần; kết quả cũ (đã có việc mới hơn) bị bỏ. */
    private void loadImage(File f, int fadeMs, boolean scheduleAfter) {
        final int job = ++wallpaperJob;
        runBackground(() -> {
            Bitmap screen = Wallpapers.loadForScreen(f, Frost.screenW, Frost.screenH);
            Bitmap blur = screen == null ? null : Wallpapers.blurred(screen);
            runOnUiThread(() -> {
                if (job != wallpaperJob || isDestroyed()) return;
                if (screen == null) {
                    toast(getString(R.string.toast_wp_read_failed, WallpaperNames.display(this, f.getName())));
                } else {
                    showWallpaper(screen, blur, fadeMs);
                    shownKey = f.getName();
                }
                if (scheduleAfter) scheduleNextSlide();
            });
        });
    }

    void scheduleNextSlide() {
        handler.removeCallbacks(nextSlide);
        if (browsing) return;
        if (slides.size() > 1) handler.postDelayed(nextSlide, prefs.interval() * 1000L);
    }

    private void showNextSlide() {
        if (slides.size() < 2) return;
        slideIndex = (slideIndex + 1) % slides.size();
        loadSlide(true);
    }

    /** @param screen hình vừa khít màn hình, hoặc null để về nền mặc định; @param blur bản mờ cho kính. */
    private void showWallpaper(Bitmap screen, Bitmap blur, int ms) {
        // Huỷ chuyển động đang dở của lần trước để các hành động kết thúc cũ không xoá nhầm hình mới.
        wallA.animate().cancel();
        wallB.animate().cancel();
        if (screen == null) {
            fadeOut(wallA, ms);
            fadeOut(wallB, ms);
            wallBFront = false;
        } else if (wallBFront) {
            // Hình mới nằm ở lớp dưới, làm mờ dần lớp trên để lộ ra.
            wallA.setImageBitmap(screen);
            wallA.setAlpha(1f);
            fadeOut(wallB, ms);
            wallBFront = false;
        } else {
            wallB.setImageBitmap(screen);
            wallB.animate().alpha(1f).setDuration(ms).withEndAction(() -> {
                // Lớp dưới đã bị che hẳn: bỏ hình cũ để chỉ giữ một ảnh toàn màn hình trong bộ nhớ.
                wallA.setImageDrawable(null);
                wallA.setAlpha(0f);
            }).start();
            wallBFront = true;
        }
        frostSource = blur;
        frostLuminance = Wallpapers.luminance(blur);
        frostDefault = screen == null;
        refreshFrost();
    }

    /** Làm mờ dần một lớp hình nền, xong thì bỏ hình khỏi lớp đó. */
    private static void fadeOut(ImageView layer, int ms) {
        layer.animate().alpha(0f).setDuration(ms).withEndAction(() -> layer.setImageDrawable(null)).start();
    }

    // ---------- Thêm hình từ tivi hoặc USB ----------

    private static final int REQ_STORAGE = 7;

    /** Android 13 trở lên dùng quyền đọc hình; bản cũ hơn dùng quyền đọc bộ nhớ. */
    private static String storagePermission() {
        return Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /** Mở trình duyệt bộ nhớ; chưa có quyền thì xin trước. */
    void browseStart() {
        if (checkSelfPermission(storagePermission()) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{storagePermission()}, REQ_STORAGE);
            return;
        }
        browser.showRoots();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != REQ_STORAGE) return;
        if (checkSelfPermission(storagePermission()) == PackageManager.PERMISSION_GRANTED) {
            // Đợi hộp thoại hệ thống đóng hẳn rồi mới hiện danh sách.
            handler.postDelayed(browser::showRoots, 300);
        } else {
            toastLong(R.string.toast_perm_needed);
        }
    }

    private boolean isDefaultHome() {
        ResolveInfo ri = getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && getPackageName().equals(ri.activityInfo.packageName);
    }
}
