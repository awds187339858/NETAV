package com.hz.quickdock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** 配置存储：侧栏要显示哪些 App、是否启用、纵向偏移。 */
public final class Store {

    private static final String SP = "dock";
    private static final String K_PKGS = "pkgs";
    private static final String K_ENABLED = "enabled";
    private static final String K_Y = "y";
    private static final String K_ADB = "fix_adb";
    private static final String K_SIZE = "icon_size";
    private static final String K_ALPHA = "bar_alpha";
    private static final String K_CUTOUT = "cutout_bg";
    private static final String K_PRELOAD = "preload";
    private static final String K_PRELOAD_PKG = "preload_pkg";

    private Store() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    public static List<String> getPkgs(Context c) {
        String s = sp(c).getString(K_PKGS, "");
        List<String> out = new ArrayList<String>();
        if (s != null && s.length() > 0) {
            for (String p : s.split("\n")) {
                p = p.trim();
                if (p.length() > 0 && !out.contains(p)) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    public static void setPkgs(Context c, List<String> pkgs) {
        StringBuilder sb = new StringBuilder();
        for (String p : pkgs) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(p);
        }
        sp(c).edit().putString(K_PKGS, sb.toString()).apply();
    }

    public static boolean isEnabled(Context c) {
        return sp(c).getBoolean(K_ENABLED, false);
    }

    public static void setEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(K_ENABLED, v).apply();
    }

    public static int getY(Context c) {
        return sp(c).getInt(K_Y, 0);
    }

    public static void setY(Context c, int y) {
        sp(c).edit().putInt(K_Y, y).apply();
    }

    /** 是否开启了「开机自动确保 adb 可用」。 */
    public static boolean isFixAdb(Context c) {
        return sp(c).getBoolean(K_ADB, true);
    }

    public static void setFixAdb(Context c, boolean v) {
        sp(c).edit().putBoolean(K_ADB, v).apply();
    }

    /** 图标边长（dp）。 */
    public static int getSize(Context c) {
        return sp(c).getInt(K_SIZE, 62);
    }

    public static void setSize(Context c, int v) {
        sp(c).edit().putInt(K_SIZE, v).apply();
    }

    /** 整列不透明度，20~100（%）。 */
    public static int getAlpha(Context c) {
        return sp(c).getInt(K_ALPHA, 100);
    }

    public static void setAlpha(Context c, int v) {
        sp(c).edit().putInt(K_ALPHA, v).apply();
    }

    /** 是否自动抠掉图标纯色背景。 */
    public static boolean isCutout(Context c) {
        return sp(c).getBoolean(K_CUTOUT, true);
    }

    public static void setCutout(Context c, boolean v) {
        sp(c).edit().putBoolean(K_CUTOUT, v).apply();
    }

    /** 开机预热：开机后自动拉起某个 App 再回桌面，让它的界面缓存提前建好。 */
    public static boolean isPreload(Context c) {
        return sp(c).getBoolean(K_PRELOAD, false);
    }

    public static void setPreload(Context c, boolean v) {
        sp(c).edit().putBoolean(K_PRELOAD, v).apply();
    }

    public static String getPreloadPkg(Context c) {
        return sp(c).getString(K_PRELOAD_PKG, "com.baidu.naviauto");
    }

    public static void setPreloadPkg(Context c, String pkg) {
        sp(c).edit().putString(K_PRELOAD_PKG, pkg == null ? "" : pkg).apply();
    }

    /** 是否在悬浮栏显示「小度语音」按钮。 */
    public static boolean isVoiceBtn(Context c) {
        return sp(c).getBoolean("voice_btn", false);
    }

    public static void setVoiceBtn(Context c, boolean v) {
        sp(c).edit().putBoolean("voice_btn", v).apply();
    }

    /** 是否在悬浮栏显示「高德开关」按钮。 */
    public static boolean isAmapBtn(Context c) {
        return sp(c).getBoolean("amap_btn", false);
    }

    public static void setAmapBtn(Context c, boolean v) {
        sp(c).edit().putBoolean("amap_btn", v).apply();
    }
}
