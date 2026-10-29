package com.hz.quickdock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

/** 开机自启：只负责拉起悬浮栏，不再自动改写 adb / 开发者选项开关。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        if (i == null || i.getAction() == null) {
            return;
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
}
