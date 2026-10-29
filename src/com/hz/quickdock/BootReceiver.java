package com.hz.quickdock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

/** 开机自启：拉起悬浮栏；顺带确保 adb 开关为 1。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        if (i == null || i.getAction() == null) {
            return;
        }
        if (Store.isFixAdb(c)) {
            ensureAdb(c);
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

    /** 写 Global 设置需要 WRITE_SECURE_SETTINGS（一次性 adb grant 授予后永久有效）。 */
    private void ensureAdb(Context c) {
        try {
            Settings.Global.putInt(c.getContentResolver(),
                    Settings.Global.ADB_ENABLED, 1);
        } catch (Exception ignored) {
        }
        try {
            Settings.Global.putInt(c.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        } catch (Exception ignored) {
        }
    }
}
