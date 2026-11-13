package io.github.phutrung08.glassdeck;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
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
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import io.github.phutrung08.common.Sizes;
import io.github.phutrung08.common.Ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Màn hình chính: nền cố định, đồng hồ lớn, nút kính mờ ở góc trên,
 * và một khay kính mờ ở dưới chứa hàng app trượt ngang (thấy 6 app một lúc).
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

    private SharedPreferences prefs;
    private final List<App> apps = new ArrayList<>();
    private final Map<String, View> tiles = new HashMap<>();
    private FrameLayout root, content, tray;
    private LinearLayout pills, row;
    private HorizontalScrollView scroll;
    private TextView inputsPill, settingsPill;
    private View overlay, focusBeforeOverlay;
    private String lastFocused;
    private int tileW, tileH;

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

    private final BroadcastReceiver packageChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { loadApps(); }
    };

    // ---------- Dựng giao diện ----------

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("home", MODE_PRIVATE);

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
        inputsPill = pill("Cổng vào", v -> showInputs());
        settingsPill = pill("Cài đặt", v -> showSettings());
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

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 84);
        clock.setTextColor(0xFFFFFFFF);
        clock.setTypeface(Ui.medium());
        clock.setIncludeFontPadding(false);
        clock.setPadding(dp(56), 0, 0, 0);
        clock.setShadowLayer(dp(8), 0, dp(1), 0x59000000);
        clockBox.addView(clock);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        date.setTextColor(0xE0FFFFFF);
        date.setShadowLayer(dp(6), 0, dp(1), 0x73000000);
        date.setPadding(dp(58), dp(6), 0, 0);
        clockBox.addView(date);
        content.addView(clockBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL));

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
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Bấm Home khi đang ở màn hình chính: đóng hộp thoại, quay về ô đầu tiên.
        closeOverlay();
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
        if (overlay == null && getCurrentFocus() == null) focusKey(lastFocused);
    }

    @Override
    public void onBackPressed() {
        if (overlay != null) {
            closeOverlay();
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

    // ---------- Danh sách app ----------

    private void loadApps() {
        final int job = ++appsJob;
        new Thread(() -> {
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
        }).start();
    }

    private List<String> savedOrder() {
        return AppOrder.parse(prefs.getString("order", ""));
    }

    private Set<String> hidden() {
        return new HashSet<>(prefs.getStringSet("hidden", new HashSet<String>()));
    }

    /** App đang hiện, theo thứ tự đã lưu; app chưa có trong thứ tự xếp sau. */
    private List<App> visibleSorted() {
        Set<String> hide = hidden();
        List<String> order = savedOrder();
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
        prefs.edit().putString("order", AppOrder.join(keys)).apply();
    }

    private void rebuildRow(String focusKey) {
        row.removeAllViews();
        tiles.clear();
        for (App a : visibleSorted()) row.addView(makeTile(a));
        row.post(() -> { if (overlay == null) focusKey(focusKey); });
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
            Toast.makeText(this, "Không mở được " + a.label, Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- Hộp thoại kính ----------

    /** ScrollView co theo nội dung nhưng không cao quá giới hạn; quá thì cuộn. */
    private static final class MaxHeightScroll extends ScrollView {
        int maxHeight = Integer.MAX_VALUE;
        MaxHeightScroll(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) {
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
        }
    }

    /**
     * Hộp thoại nằm chính giữa khoảng trống giữa hàng nút phía trên và khay app,
     * không bao giờ đè lên hai phần đó. Danh sách dài hơn thì cuộn, có thanh cuộn luôn hiện.
     */
    private void showMenu(String title, String[] items, Ui.Pick onPick) {
        showMenu(title, items, 0, true, onPick);
    }

    /** @param focusIndex dòng được chọn sẵn; @param animate false khi mở lại cùng một menu (danh sách đánh dấu). */
    private void showMenu(String title, String[] items, int focusIndex, boolean animate, Ui.Pick onPick) {
        if (overlay != null) closeOverlay();
        focusBeforeOverlay = getCurrentFocus();
        final long shownAt = android.os.SystemClock.uptimeMillis();

        FrameLayout scrim = new FrameLayout(this);
        scrim.setBackgroundColor(0x66000000);

        int top = pills.getBottom();
        int bottom = ((View) tray.getParent()).getTop() + tray.getTop();
        int gap = dp(20);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(new Frost.Glass(panel, dp(22), true, dp(1)));
        panel.setPadding(dp(12), dp(16), dp(10), dp(12));

        TextView head = Ui.text(this, title, 19, Ui.WHITE);
        head.setTypeface(Ui.medium());
        head.setPadding(dp(14), 0, dp(14), dp(10));
        panel.addView(head);
        head.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        MaxHeightScroll list = new MaxHeightScroll(this);
        list.maxHeight = Math.max(dp(90), bottom - top - 2 * gap - head.getMeasuredHeight() - dp(28));
        list.setVerticalScrollBarEnabled(true);
        list.setScrollbarFadingEnabled(false);
        if (Build.VERSION.SDK_INT >= 29) {
            list.setVerticalScrollbarThumbDrawable(Ui.solid(this, 0xBFFFFFFF, 2));
            list.setVerticalScrollbarTrackDrawable(Ui.solid(this, 0x24FFFFFF, 2));
        }
        list.setScrollBarSize(dp(4));
        list.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        list.setVerticalFadingEdgeEnabled(true);
        list.setFadingEdgeLength(dp(28));
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, dp(8), 0);
        View first = null;
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            TextView it = Ui.text(this, items[i], 17, Ui.WHITE);
            it.setTextColor(Ui.focusColor(Ui.INK, Ui.WHITE));
            it.setBackground(Ui.focusBackground(this, Ui.solid(this, 0x00000000, 12), 12));
            it.setPadding(dp(14), dp(11), dp(14), dp(11));
            it.setFocusable(true);
            it.setClickable(true);
            it.setOnClickListener(v -> {
                // Bỏ qua lần bấm lọt sang từ hộp thoại vừa đóng (vd. hộp thoại xin quyền của hệ thống).
                if (animate && android.os.SystemClock.uptimeMillis() - shownAt < 350) return;
                closeOverlay();
                onPick.onPick(index);
            });
            col.addView(it, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (first == null || i == focusIndex) first = it;
        }
        list.addView(col);
        panel.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Vùng giữa hàng nút và khay; hộp thoại canh giữa trong vùng này.
        FrameLayout zone = new FrameLayout(this);
        zone.addView(panel, new FrameLayout.LayoutParams(
                root.getWidth() * 46 / 100, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        FrameLayout.LayoutParams lpZone = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bottom - top - 2 * gap);
        lpZone.topMargin = top + gap;
        scrim.addView(zone, lpZone);

        content.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay = scrim;
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (animate) {
            scrim.setAlpha(0f);
            scrim.animate().alpha(1f).setDuration(160).start();
        }
        if (first != null) first.requestFocus();
    }

    private void closeOverlay() {
        if (overlay == null) return;
        root.removeView(overlay);
        overlay = null;
        content.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        if (focusBeforeOverlay != null && focusBeforeOverlay.isAttachedToWindow()) focusBeforeOverlay.requestFocus();
        else focusKey(lastFocused);
        focusBeforeOverlay = null;
    }

    // ---------- Nội dung các menu ----------

    private void showAppMenu(App a) {
        String[] items = {"Đưa lên đầu", "Chuyển sang trái", "Chuyển sang phải", "Ẩn khỏi màn hình chính", "Thông tin ứng dụng"};
        showMenu(a.label, items, which -> {
            List<App> list = visibleSorted();
            int i = -1;
            for (int k = 0; k < list.size(); k++) if (list.get(k).key().equals(a.key())) i = k;
            if (i < 0) return;
            switch (which) {
                case 0: list.add(0, list.remove(i)); break;
                case 1: if (i > 0) Collections.swap(list, i, i - 1); break;
                case 2: if (i < list.size() - 1) Collections.swap(list, i, i + 1); break;
                case 3: {
                    Set<String> h = hidden();
                    h.add(a.key());
                    prefs.edit().putStringSet("hidden", h).apply();
                    lastFocused = null;
                    rebuildRow(null);
                    return;
                }
                default:
                    try {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + a.cn.getPackageName())));
                    } catch (Exception e) {
                        Toast.makeText(this, "Không mở được trang thông tin", Toast.LENGTH_SHORT).show();
                    }
                    return;
            }
            saveOrder(list);
            rebuildRow(a.key());
        });
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
            Toast.makeText(this, "Tivi không cung cấp danh sách cổng vào. Hãy dùng nút Input trên remote.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[inputs.size()];
        for (int i = 0; i < names.length; i++) {
            TvInputInfo info = inputs.get(i);
            CharSequence custom = info.loadCustomLabel(this);
            names[i] = String.valueOf(custom != null ? custom : info.loadLabel(this));
        }
        showMenu("Cổng vào", names, which -> {
            TvInputInfo info = inputs.get(which);
            Uri uri = info.isPassthroughInput()
                    ? TvContract.buildChannelUriForPassthroughInput(info.getId())
                    : TvContract.Channels.CONTENT_URI;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Toast.makeText(this, "Không chuyển được sang " + names[which], Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSettings() {
        String[] items = {
                "Cài đặt tivi",
                "Dọn dẹp",
                "Tidyo TV · dọn dẹp tivi",
                "Hình nền",
                "Độ trong suốt: " + transparency() + "%",
                "Hiện lại ứng dụng đã ẩn (" + hidden().size() + ")"
        };
        showMenu("Cài đặt", items, which -> {
            switch (which) {
                case 0:
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this, "Không mở được Cài đặt", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 1:
                    startActivity(new Intent(this, CleanupActivity.class));
                    break;
                case 2: {
                    Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(TIDYO);
                    if (i == null) {
                        Toast.makeText(this, "Chưa cài Tidyo TV", Toast.LENGTH_LONG).show();
                    } else {
                        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                    break;
                }
                case 3:
                    showWallpaperMenu();
                    break;
                case 4:
                    showTransparencyMenu();
                    break;
                default:
                    prefs.edit().remove("hidden").apply();
                    rebuildRow(lastFocused);
            }
        });
    }

    // ---------- Độ trong suốt ----------

    private int transparency() {
        return prefs.getInt("transparency", 60);
    }

    private static String mark(boolean on) {
        return on ? "●  " : "○  ";
    }

    private void showTransparencyMenu() {
        final int[] levels = {0, 20, 40, 60, 80, 100};
        int current = transparency(), focus = 0;
        String[] items = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            String note = levels[i] == 0 ? " · đậm nhất" :levels[i] == 100 ? " · trong nhất" : "";
            items[i] = mark(levels[i] == current) + levels[i] + "%" + note;
            if (levels[i] == current) focus = i;
        }
        showMenu("Độ trong suốt của kính", items, focus, true, which -> {
            prefs.edit().putInt("transparency", levels[which]).apply();
            refreshFrost();
        });
    }

    private void refreshFrost() {
        if (frostSource != null) Frost.update(frostSource, frostLuminance, transparency(), frostDefault);
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

    private String wallpaperMode() {
        return prefs.getString("wp_mode", "default");
    }

    private void showWallpaperMenu() {
        String mode = wallpaperMode();
        int interval = prefs.getInt("wp_interval", Intervals.DEFAULT);
        int count = Wallpapers.list(this).size();
        String[] items = {
                mark(mode.equals("default")) + "Nền mặc định",
                mark(mode.equals("single")) + "Một hình cố định",
                mark(mode.equals("multi")) + "Nhiều hình, tự đổi",
                "Thời gian đổi hình: " + Intervals.label(interval),
                "Thêm hình từ tivi hoặc USB · đang có " + count + " hình"
        };
        showMenu("Hình nền", items, which -> {
            if (which == 0) {
                prefs.edit().putString("wp_mode", "default").apply();
                applyWallpaper(true);
            } else if (which == 1) {
                pickSingle();
            } else if (which == 2) {
                pickMulti(0, true);
            } else if (which == 3) {
                showIntervalMenu();
            } else {
                browseStart();
            }
        });
    }

    private boolean noWallpapers(List<File> files) {
        if (!files.isEmpty()) return false;
        Toast.makeText(this, "Chưa có hình nào. Chọn \"Thêm hình từ tivi hoặc USB\" để thêm.",
                Toast.LENGTH_LONG).show();
        return true;
    }

    private void pickSingle() {
        List<File> files = Wallpapers.list(this);
        if (noWallpapers(files)) return;
        String current = prefs.getString("wp_single", "");
        String[] items = new String[files.size()];
        int focus = 0;
        for (int i = 0; i < items.length; i++) {
            boolean on = wallpaperMode().equals("single") && files.get(i).getName().equals(current);
            items[i] = mark(on) + files.get(i).getName();
            if (on) focus = i;
        }
        showMenu("Chọn một hình", items, focus, true, which -> {
            prefs.edit().putString("wp_mode", "single").putString("wp_single", files.get(which).getName()).apply();
            applyWallpaper(true);
        });
    }

    /** Danh sách đánh dấu: bấm OK để chọn hoặc bỏ chọn từng hình, dòng cuối để xác nhận. */
    private void pickMulti(int focusIndex, boolean animate) {
        List<File> files = Wallpapers.list(this);
        if (noWallpapers(files)) return;
        Set<String> chosen = new HashSet<>(prefs.getStringSet("wp_multi", new HashSet<String>()));
        String[] items = new String[files.size() + 1];
        int n = 0;
        for (int i = 0; i < files.size(); i++) {
            boolean on = chosen.contains(files.get(i).getName());
            if (on) n++;
            items[i] = mark(on) + files.get(i).getName();
        }
        items[files.size()] = "Xong · đã chọn " + n + " hình";
        showMenu("Chọn nhiều hình", items, focusIndex, animate, which -> {
            if (which < files.size()) {
                String name = files.get(which).getName();
                if (!chosen.remove(name)) chosen.add(name);
                prefs.edit().putStringSet("wp_multi", chosen).apply();
                pickMulti(which, false);
            } else if (chosen.isEmpty()) {
                Toast.makeText(this, "Chưa chọn hình nào nên giữ nguyên hình nền hiện tại", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("wp_mode", "multi").apply();
                applyWallpaper(true);
            }
        });
    }

    private void showIntervalMenu() {
        int current = prefs.getInt("wp_interval", Intervals.DEFAULT), focus = 0;
        String[] items = new String[Intervals.VALUES.length];
        for (int i = 0; i < Intervals.VALUES.length; i++) {
            items[i] = mark(Intervals.VALUES[i] == current) + Intervals.label(Intervals.VALUES[i]);
            if (Intervals.VALUES[i] == current) focus = i;
        }
        showMenu("Thời gian đổi hình", items, focus, true, which -> {
            prefs.edit().putInt("wp_interval", Intervals.VALUES[which]).apply();
            scheduleNextSlide();
        });
    }

    /** Đọc thiết lập và hiện hình nền tương ứng. */
    private void applyWallpaper(boolean animate) {
        handler.removeCallbacks(nextSlide);
        slides.clear();
        String mode = wallpaperMode();
        List<File> files = Wallpapers.list(this);
        if (mode.equals("single")) {
            String name = prefs.getString("wp_single", "");
            for (File f : files) if (f.getName().equals(name)) slides.add(f);
        } else if (mode.equals("multi")) {
            Set<String> chosen = prefs.getStringSet("wp_multi", new HashSet<String>());
            for (File f : files) if (chosen.contains(f.getName())) slides.add(f);
        }
        if (slides.isEmpty()) {
            // Nền mặc định, hoặc hình đã chọn không còn trên tivi.
            wallpaperJob++;
            showWallpaper(null, Wallpapers.small(defaultBg, Frost.screenW, Frost.screenH), animate);
            return;
        }
        slideIndex = 0;
        loadSlide(animate);
    }

    private void loadSlide(boolean animate) {
        final int job = ++wallpaperJob;
        final File f = slides.get(slideIndex % slides.size());
        new Thread(() -> {
            Bitmap screen = Wallpapers.loadForScreen(f, Frost.screenW, Frost.screenH);
            Bitmap blur = screen == null ? null : Wallpapers.blurred(screen);
            runOnUiThread(() -> {
                if (job != wallpaperJob || isDestroyed()) return;
                if (screen == null) {
                    Toast.makeText(this, "Không đọc được hình " + f.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    showWallpaper(screen, blur, animate);
                }
                scheduleNextSlide();
            });
        }).start();
    }

    private void scheduleNextSlide() {
        handler.removeCallbacks(nextSlide);
        if (slides.size() > 1) handler.postDelayed(nextSlide, prefs.getInt("wp_interval", Intervals.DEFAULT) * 1000L);
    }

    private void showNextSlide() {
        if (slides.size() < 2) return;
        slideIndex = (slideIndex + 1) % slides.size();
        loadSlide(true);
    }

    /** @param screen hình vừa khít màn hình, hoặc null để về nền mặc định; @param blur bản mờ cho kính. */
    private void showWallpaper(Bitmap screen, Bitmap blur, boolean animate) {
        int ms = animate ? 700 : 0;
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

    private void browseStart() {
        if (checkSelfPermission(storagePermission()) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{storagePermission()}, REQ_STORAGE);
            return;
        }
        showRoots();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != REQ_STORAGE) return;
        if (checkSelfPermission(storagePermission()) == PackageManager.PERMISSION_GRANTED) {
            // Đợi hộp thoại hệ thống đóng hẳn rồi mới hiện danh sách.
            handler.postDelayed(this::showRoots, 300);
        } else {
            Toast.makeText(this, "Cần quyền đọc bộ nhớ để lấy hình trong tivi hoặc USB", Toast.LENGTH_LONG).show();
        }
    }

    private static File volumeDir(StorageVolume v) {
        if (Build.VERSION.SDK_INT >= 30) return v.getDirectory();
        if (v.isPrimary()) return Environment.getExternalStorageDirectory();
        return v.getUuid() == null ? null : new File("/storage/" + v.getUuid());
    }

    /** Danh sách nơi lấy hình: bộ nhớ trong của tivi và các USB đang cắm. */
    private void showRoots() {
        List<File> dirs = new ArrayList<>();
        List<String> names = new ArrayList<>();
        StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        for (StorageVolume v : sm.getStorageVolumes()) {
            String state = v.getState();
            if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) continue;
            File d = volumeDir(v);
            if (d == null || !d.canRead()) continue;
            dirs.add(d);
            names.add((v.isRemovable() ? "USB · " : "Bộ nhớ tivi · ") + v.getDescription(this));
        }
        if (dirs.isEmpty()) {
            Toast.makeText(this, "Không đọc được bộ nhớ nào. Thử rút USB ra cắm lại.", Toast.LENGTH_LONG).show();
            return;
        }
        boolean hasUsb = false;
        for (String n : names) if (n.startsWith("USB")) hasUsb = true;
        String title = hasUsb ? "Lấy hình từ đâu?" : "Lấy hình từ đâu? (chưa thấy USB)";
        showMenu(title, names.toArray(new String[0]), which -> browse(dirs.get(which), dirs.get(which), names.get(which), 0));
    }

    /**
     * Duyệt một thư mục: thư mục con trước, hình sau. Bấm OK vào hình để chép vào bộ hình nền
     * (dấu ✓ là hình đã có). Hình được chép vào tivi nên rút USB ra vẫn dùng được.
     */
    private void browse(File root, File dir, String rootLabel, int focus) {
        List<File> folders = new ArrayList<>(), images = new ArrayList<>();
        File[] all = dir.listFiles();
        if (all != null) {
            for (File f : all) {
                if (f.getName().startsWith(".")) continue;
                if (f.isDirectory()) {
                    if (!(dir.equals(root) && f.getName().equals("Android"))) folders.add(f);
                } else if (Wallpapers.isImage(f)) {
                    images.add(f);
                }
            }
        }
        Collections.sort(folders, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));
        Collections.sort(images, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));

        List<String> items = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (dir.equals(root)) {
            items.add("↑  Chọn nơi khác");
            actions.add(this::showRoots);
        } else {
            items.add("↑  Lên thư mục trên");
            File parent = dir.getParentFile();
            actions.add(() -> browse(root, parent, rootLabel, 0));
        }
        if (images.size() > 1) {
            items.add("Thêm cả " + images.size() + " hình trong thư mục này");
            actions.add(() -> copyImages(images, () -> browse(root, dir, rootLabel, 1)));
        }
        for (File f : folders) {
            items.add("▸  " + f.getName());
            actions.add(() -> browse(root, f, rootLabel, 0));
        }
        for (File f : images) {
            final int index = items.size();
            items.add((Wallpapers.alreadyAdded(this, f) ? "✓  " : "+  ") + f.getName() + "  · " + Sizes.format(f.length()));
            actions.add(() -> copyImages(Collections.singletonList(f), () -> browse(root, dir, rootLabel, index)));
        }
        if (folders.isEmpty() && images.isEmpty()) {
            items.add("Thư mục này không có hình (.jpg .png .webp)");
            actions.add(() -> browse(root, dir, rootLabel, 0));
        }

        String title = dir.equals(root) ? rootLabel : dir.getName();
        showMenu(title, items.toArray(new String[0]), Math.min(focus, items.size() - 1), focus == 0,
                which -> actions.get(which).run());
    }

    private void copyImages(List<File> files, Runnable then) {
        if (files.size() > 1) Toast.makeText(this, "Đang chép " + files.size() + " hình…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int ok = 0;
            for (File f : files) if (Wallpapers.copyIn(this, f)) ok++;
            final int done = ok;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                String msg = done == files.size()
                        ? "Đã thêm " + done + " hình. Vào Một hình cố định hoặc Nhiều hình để dùng."
                        : "Đã thêm " + done + "/" + files.size() + " hình, số còn lại không chép được.";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                then.run();
            });
        }).start();
    }

    private boolean isDefaultHome() {
        ResolveInfo ri = getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && getPackageName().equals(ri.activityInfo.packageName);
    }
}
