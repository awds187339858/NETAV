package com.hz.quickdock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

/**
 * 悬浮侧栏服务：贴屏幕左边缘、垂直居中，一列可点的 App 图标。
 * 图标可上下拖动整列位置，长按任意图标进入设置。
 */
public class DockService extends Service {

    private WindowManager wm;
    private View dock;
    private WindowManager.LayoutParams lp;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String act = intent == null ? null : intent.getAction();
        // 设置页打开时临时隐藏，避免遮挡文字
        if (dock != null && "com.hz.quickdock.HIDE".equals(act)) {
            dock.setVisibility(View.GONE);
            return START_STICKY;
        }
        if (dock != null && "com.hz.quickdock.SHOW".equals(act)) {
            dock.setVisibility(View.VISIBLE);
            return START_STICKY;
        }
        startAsForeground();
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "缺少悬浮窗权限，请在设置里授予", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        buildDock();
        if (intent != null && intent.getBooleanExtra("boot", false)) {
            // 开机后分多个时间点补回无障碍服务，越早越好，避免用户开机就用时还没恢复
            final Runnable fixAcc = new Runnable() {
                @Override
                public void run() {
                    ensureAccessibility();
                }
            };
            handler.postDelayed(fixAcc, 4000L);
            handler.postDelayed(fixAcc, 12000L);
            handler.postDelayed(fixAcc, 30000L);
            schedulePreload();
        }
        return START_STICKY;
    }

    /**
     * 车机重启会把 enabled_accessibility_services 里的第三方服务摘掉，
     * 这里在开机后补回去（保留思必驰那条，只做追加）。
     */
    private void ensureAccessibility() {
        try {
            String mine = getPackageName() + "/" + VoiceClickService.class.getName();
            String cur = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (cur == null) {
                cur = "";
            }
            if (!cur.contains(mine)) {
                String nv = cur.isEmpty() ? mine : cur + ":" + mine;
                Settings.Secure.putString(getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, nv);
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            } else {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            }
        } catch (Exception ignored) {
        }
    }

    /** 用 su 执行命令（车机 su 无授权弹窗，直接给 root）。 */
    private int runSu(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"su", "0", "sh", "-c", cmd});
            int rc = p.waitFor();
            if (rc != 0) {
                try {
                    java.io.InputStream es = p.getErrorStream();
                    byte[] buf = new byte[512];
                    int n = es.read(buf);
                    android.util.Log.e("QuickDock", "su stderr: "
                            + (n > 0 ? new String(buf, 0, n) : "(empty)"));
                } catch (Exception ignored) {
                }
            }
            return rc;
        } catch (Exception e) {
            android.util.Log.e("QuickDock", "su exec error: " + e.getClass().getName()
                    + " / " + e.getMessage());
            return -1;
        }
    }

    private boolean amapDisabled() {
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager()
                    .getApplicationInfo("com.autonavi.amapauto", 0);
            return !ai.enabled;
        } catch (Exception e) {
            return false;
        }
    }

    /** 一键切换高德：禁用（同时杀掉进程释放内存）/ 释放。 */
    private void toggleAmap() {
        final boolean wasDisabled = amapDisabled();
        final String cmd = wasDisabled
                ? "pm enable com.autonavi.amapauto"
                : "pm disable com.autonavi.amapauto; killall -9 com.autonavi.amapauto 2>/dev/null; true";
        android.util.Log.e("QuickDock", "toggleAmap: wasDisabled=" + wasDisabled + " cmd=" + cmd);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int rc = runSu(cmd);
                android.util.Log.e("QuickDock", "toggleAmap: su rc=" + rc);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DockService.this,
                                wasDisabled ? "高德已释放" : "高德已禁用并释放内存",
                                Toast.LENGTH_LONG).show();
                        buildDock();
                    }
                });
            }
        }).start();
    }

    /** 开机后延迟预热：拉起目标 App 让界面缓存建好，几秒后自动回桌面。 */
    private void schedulePreload() {
        if (!Store.isPreload(this)) {
            return;
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doPreload();
            }
        }, 25000L);
    }

    private void doPreload() {
        final String pkg = Store.getPreloadPkg(this);
        if (pkg == null || pkg.length() == 0) {
            return;
        }
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) {
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent home = new Intent(Intent.ACTION_MAIN);
                        home.addCategory(Intent.CATEGORY_HOME);
                        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(home);
                    } catch (Exception ignored) {
                    }
                }
            }, 7000L);
        } catch (Exception ignored) {
        }
    }

    private void startAsForeground() {
        String ch = "dock";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel nc = new NotificationChannel(
                    ch, "快捷侧栏", NotificationManager.IMPORTANCE_MIN);
            nc.setShowBadge(false);
            nm.createNotificationChannel(nc);
        }
        Notification n = new Notification.Builder(this, ch)
                .setContentTitle("快捷侧栏运行中")
                .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
                .setOngoing(true)
                .build();
        startForeground(1001, n);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void removeDock() {
        if (dock != null) {
            try {
                wm.removeView(dock);
            } catch (Exception ignored) {
            }
            dock = null;
        }
    }

    private void buildDock() {
        removeDock();
        List<String> pkgs = Store.getPkgs(this);
        if (pkgs.isEmpty()) {
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        // 背景完全透明：只留标志本身，不画底条
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x00000000);
        bg.setCornerRadius(dp(18));
        root.setBackground(bg);
        int pad = dp(5);
        root.setPadding(pad, pad, pad, pad);

        int size = dp(Store.getSize(this));
        int margin = dp(5);
        int count = 0;

        for (final String pkg : pkgs) {
            Drawable icon = loadIcon(pkg);
            if (icon == null) {
                continue;   // App 已卸载
            }
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(icon);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ip =
                    new LinearLayout.LayoutParams(size, size);
            ip.topMargin = margin;
            iv.setLayoutParams(ip);
            iv.setOnTouchListener(makeTouch(new Runnable() {
                public void run() {
                    launch(pkg);
                }
            }));
            iv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent i = new Intent(DockService.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return true;
                }
            });
            root.addView(iv);
            count++;
        }

        if (Store.isVoiceBtn(this)) {
            ImageView vb = new ImageView(this);
            vb.setImageResource(android.R.drawable.ic_btn_speak_now);
            vb.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(size, size);
            vp.topMargin = margin;
            vb.setLayoutParams(vp);
            vb.setOnTouchListener(makeTouch(new Runnable() {
                public void run() {
                    voiceLaunch();
                }
            }));
            vb.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent i = new Intent(DockService.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return true;
                }
            });
            root.addView(vb);
            count++;
        }

        if (Store.isAmapBtn(this)) {
            final boolean off = amapDisabled();
            ImageView ab = new ImageView(this);
            ab.setImageResource(off
                    ? android.R.drawable.ic_menu_revert
                    : android.R.drawable.ic_menu_close_clear_cancel);
            ab.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(size, size);
            ap.topMargin = margin;
            ab.setLayoutParams(ap);
            ab.setOnTouchListener(makeTouch(new Runnable() {
                public void run() {
                    toggleAmap();
                }
            }));
            ab.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent i = new Intent(DockService.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return true;
                }
            });
            root.addView(ab);
            count++;
        }

        if (count == 0) {
            return;
        }

        lp = new WindowManager.LayoutParams();
        lp.type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        lp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.x = 0;
        lp.y = Store.getY(this);

        dock = root;
        int alpha = Store.getAlpha(this);
        root.setAlpha(Math.max(0.1f, Math.min(1.0f, alpha / 100f)));
        try {
            wm.addView(dock, lp);
        } catch (Exception e) {
            dock = null;
            Toast.makeText(this, "添加悬浮窗失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Drawable loadIcon(String pkg) {
        try {
            Drawable d = getPackageManager().getApplicationIcon(pkg);
            if (d == null) {
                return null;
            }
            if (!Store.isCutout(this)) {
                return d;
            }
            // Adaptive Icon：前景层通常已自带透明背景，优先用它
            if (Build.VERSION.SDK_INT >= 26 && d instanceof AdaptiveIconDrawable) {
                Drawable fg = ((AdaptiveIconDrawable) d).getForeground();
                if (fg != null) {
                    Bitmap fb = toBitmap(fg, 192);
                    if (fb != null) {
                        return new BitmapDrawable(getResources(), cutBackground(fb));
                    }
                }
            }
            Bitmap bm = toBitmap(d, 192);
            if (bm != null) {
                return new BitmapDrawable(getResources(), cutBackground(bm));
            }
            return d;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap toBitmap(Drawable d, int size) {
        try {
            Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            d.setBounds(0, 0, size, size);
            d.draw(cv);
            return bm;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 抠背景：在四条边中点 + 四个内缩角采样，取出现最多的颜色当背景色，
     * 再按色距把接近背景的像素渐变透明。找不到明显纯色底就原样返回。
     */
    private static Bitmap cutBackground(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);

        int inset = Math.max(2, w / 16);
        int[] pts = new int[]{
                px[w / 2], px[(h - 1) * w + w / 2],
                px[(h / 2) * w], px[(h / 2) * w + w - 1],
                px[inset * w + inset], px[inset * w + (w - 1 - inset)],
                px[(h - 1 - inset) * w + inset], px[(h - 1 - inset) * w + (w - 1 - inset)]
        };

        int base = 0;
        int baseCount = 0;
        for (int i = 0; i < pts.length; i++) {
            int p = pts[i];
            if (((p >>> 24) & 0xff) < 8) {
                continue;
            }
            int cnt = 0;
            for (int j = 0; j < pts.length; j++) {
                if (((pts[j] >>> 24) & 0xff) >= 8 && close(p, pts[j], 40)) {
                    cnt++;
                }
            }
            if (cnt > baseCount) {
                baseCount = cnt;
                base = p;
            }
        }
        if (baseCount < 4) {
            return src;     // 没有明显纯色底，不处理
        }

        int br = (base >> 16) & 0xff;
        int bg = (base >> 8) & 0xff;
        int bb = base & 0xff;

        final double T1 = 26.0;
        final double T2 = 78.0;
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = (p >>> 24) & 0xff;
            if (a == 0) {
                continue;
            }
            double dr = ((p >> 16) & 0xff) - br;
            double dg = ((p >> 8) & 0xff) - bg;
            double db = (p & 0xff) - bb;
            double dist = Math.sqrt(dr * dr + dg * dg + db * db);
            int na;
            if (dist < T1) {
                na = 0;
            } else if (dist > T2) {
                na = a;
            } else {
                na = (int) (a * (dist - T1) / (T2 - T1));
            }
            px[i] = (na << 24) | (p & 0x00ffffff);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    private static boolean close(int a, int b, int tol) {
        int dr = Math.abs(((a >> 16) & 0xff) - ((b >> 16) & 0xff));
        int dg = Math.abs(((a >> 8) & 0xff) - ((b >> 8) & 0xff));
        int db = Math.abs((a & 0xff) - (b & 0xff));
        return dr <= tol && dg <= tol && db <= tol;
    }

    private View.OnTouchListener makeTouch(final Runnable onTap) {
        return new View.OnTouchListener() {
            private float downRawY;
            private int downLpY;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawY = e.getRawY();
                        downLpY = lp != null ? lp.y : 0;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (lp == null || dock == null) {
                            return true;
                        }
                        int dy = (int) (e.getRawY() - downRawY);
                        if (Math.abs(dy) > dp(10)) {
                            moved = true;
                        }
                        if (moved) {
                            lp.y = downLpY + dy;
                            try {
                                wm.updateViewLayout(dock, lp);
                            } catch (Exception ignored) {
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved && lp != null) {
                            Store.setY(DockService.this, lp.y);
                        } else {
                            onTap.run();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

    /** 唤起百度地图并让小度语音自动就绪。 */
    private void voiceLaunch() {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage("com.baidu.naviauto");
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(i);
            }
        } catch (Exception ignored) {
        }
        VoiceClickService.arm();
        handler.postDelayed(new Runnable() {
            public void run() {
                VoiceClickService.arm();
            }
        }, 3000L);
    }

    private void launch(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) {
                Toast.makeText(this, "无法启动 " + pkg, Toast.LENGTH_SHORT).show();
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "启动失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 供设置页调用：配置变了，重建界面。 */
    public static void refresh(android.content.Context c) {
        Intent i = new Intent(c, DockService.class);
        c.startService(i);
    }

    @Override
    public void onDestroy() {
        removeDock();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
