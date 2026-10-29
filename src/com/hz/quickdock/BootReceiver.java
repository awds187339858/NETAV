package com.hz.quickdock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

/** 开机自启：拉起悬浮栏；按用户设定永久开启/关闭开发者选项 + ADB。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        if (i == null || i.getAction() == null) {
            return;
        }
        // 永久开关：开机时按设定写回 adb / 开发者选项状态
        if ("android.intent.action.BOOT_COMPLETED".equals(i.getAction())) {
            applyAdb(c, Store.isAdbAuto(c));
        }
        if (Store.isEnabled(c) && Settings.canDrawOverlays(c)) {
            try {
                Intent s = new Intent(c, DockService.class);
                s.putExtra("boot", true);
                if (Build.VERSION.SDK_INT >= 26) {
                    c.startForegroundService(s);
                } else {
                    c.startService(s);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /** 写回调试开关并回读校验；没有 WRITE_SECURE_SETTINGS 时返回 false。 */
    static boolean applyAdb(Context c, boolean on) {
        try {
            Settings.Global.putInt(c.getContentResolver(),
                    Settings.Global.ADB_ENABLED, on ? 1 : 0);
            Settings.Global.putInt(c.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, on ? 1 : 0);
            int v = Settings.Global.getInt(c.getContentResolver(),
                    Settings.Global.ADB_ENABLED, -1);
            return v == (on ? 1 : 0);
        } catch (Exception e) {
            return false;
        }
    }
}
