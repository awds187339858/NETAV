package com.hz.quickdock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 设置界面：选 App、开关悬浮栏、授予权限。 */
public class MainActivity extends Activity {

    private TextView status;
    private TextView listView;
    private CheckBox enableBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.WHITE);
        sv.addView(root);

        TextView title = new TextView(this);
        title.setText("快捷侧栏");
        title.setTextSize(26);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, dp(8), 0, dp(16));
        root.addView(status);

        Button perm = new Button(this);
        perm.setText("① 授予悬浮窗权限");
        perm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "无法打开权限页：" + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        root.addView(perm);

        Button pick = new Button(this);
        pick.setText("② 选择要显示的 App");
        pick.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickApps();
            }
        });
        root.addView(pick);

        listView = new TextView(this);
        listView.setTextSize(14);
        listView.setTextColor(Color.BLACK);
        listView.setPadding(0, dp(8), 0, dp(16));
        root.addView(listView);

        enableBox = new CheckBox(this);

        enableBox.setTextColor(Color.BLACK);
        enableBox.setText("③ 启用悬浮栏（开机自动启动）");
        enableBox.setTextSize(16);
        enableBox.setOnCheckedChangeListener(
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                        Store.setEnabled(MainActivity.this, v);
                        if (v) {
                            if (!Settings.canDrawOverlays(MainActivity.this)) {
                                Toast.makeText(MainActivity.this,
                                        "先授予悬浮窗权限", Toast.LENGTH_LONG).show();
                                b.setChecked(false);
                                Store.setEnabled(MainActivity.this, false);
                                return;
                            }
                            startService(new Intent(MainActivity.this, DockService.class));
                        } else {
                            stopService(new Intent(MainActivity.this, DockService.class));
                        }
                        updateUi();
                    }
                });
        root.addView(enableBox);

        // ---- 图标大小 ----
        final TextView sizeLabel = new TextView(this);
        sizeLabel.setTextSize(14);
        sizeLabel.setTextColor(Color.BLACK);
        sizeLabel.setText("图标大小：" + Store.getSize(this) + " px");
        sizeLabel.setPadding(0, dp(16), 0, 0);
        root.addView(sizeLabel);

        SeekBar sizeBar = new SeekBar(this);
        sizeBar.setMax(110 - 36);
        sizeBar.setProgress(Store.getSize(this) - 36);
        sizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                sizeLabel.setText("图标大小：" + (p + 36) + " px");
            }

            public void onStartTrackingTouch(SeekBar sb) {
            }

            public void onStopTrackingTouch(SeekBar sb) {
                Store.setSize(MainActivity.this, sb.getProgress() + 36);
                if (Store.isEnabled(MainActivity.this)) {
                    DockService.refresh(MainActivity.this);
                }
            }
        });
        root.addView(sizeBar);

        // ---- 不透明度 ----
        final TextView alphaLabel = new TextView(this);
        alphaLabel.setTextSize(14);
        alphaLabel.setTextColor(Color.BLACK);
        alphaLabel.setText("不透明度：" + Store.getAlpha(this) + "%");
        alphaLabel.setPadding(0, dp(12), 0, 0);
        root.addView(alphaLabel);

        SeekBar alphaBar = new SeekBar(this);
        alphaBar.setMax(100 - 20);
        alphaBar.setProgress(Store.getAlpha(this) - 20);
        alphaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                alphaLabel.setText("不透明度：" + (p + 20) + "%");
            }

            public void onStartTrackingTouch(SeekBar sb) {
            }

            public void onStopTrackingTouch(SeekBar sb) {
                Store.setAlpha(MainActivity.this, sb.getProgress() + 20);
                if (Store.isEnabled(MainActivity.this)) {
                    DockService.refresh(MainActivity.this);
                }
            }
        });
        root.addView(alphaBar);

        // ---- 抠背景 ----
        CheckBox cutout = new CheckBox(this);
        cutout.setTextColor(Color.BLACK);
        cutout.setText("自动去掉图标的纯色底色（只留标志）");
        cutout.setTextSize(14);
        cutout.setChecked(Store.isCutout(this));
        cutout.setOnCheckedChangeListener(
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                        Store.setCutout(MainActivity.this, v);
                        if (Store.isEnabled(MainActivity.this)) {
                            DockService.refresh(MainActivity.this);
                        }
                    }
                });
        root.addView(cutout);

        // ---- 开发者选项 / ADB 永久开关 ----
        CheckBox adbAuto = new CheckBox(this);
        adbAuto.setTextColor(Color.BLACK);
        adbAuto.setText("开机自动开启开发者选项 + ADB（取消则开机自动关闭）");
        adbAuto.setTextSize(14);
        adbAuto.setChecked(Store.isAdbAuto(this));
        adbAuto.setOnCheckedChangeListener(
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    private boolean suppress = false;

                    public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                        if (suppress) {
                            return;
                        }
                        Store.setAdbAuto(MainActivity.this, v);
                        boolean ok = BootReceiver.applyAdb(MainActivity.this, v);
                        if (ok) {
                            Toast.makeText(MainActivity.this,
                                    v ? "已开启：以后每次开机自动开启"
                                      : "已关闭：以后每次开机保持关闭",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // 没有 adb 授权：不假装成功，回滚并引导手动开启
                            suppress = true;
                            b.setChecked(false);
                            suppress = false;
                            Store.setAdbAuto(MainActivity.this, false);
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("无法自动开启")
                                    .setMessage("本机没有 adb 授权，App 没法直接改这个开关。\n\n"
                                            + "想现在就用，点「去设置」在这里开：\n"
                                            + "开发者选项 → 找到「USB 调试」→ 打开\n\n"
                                            + "（想让它以后自动开启，需要在电脑上执行一次授权命令，"
                                            + "详见「使用说明 / 关于」）")
                                    .setPositiveButton("去设置",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface d, int w) {
                                                    try {
                                                        startActivity(new Intent(
                                                                android.provider.Settings
                                                                        .ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                                                    } catch (Exception e) {
                                                        Toast.makeText(MainActivity.this,
                                                                "打不开开发者选项：" + e.getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                        updateUi();
                    }
                });
        root.addView(adbAuto);

        // ---- 高德开关命令（复制到电脑执行）----
        Button amapCmdBtn = new Button(this);
        amapCmdBtn.setText("高德禁用 / 释放 命令");
        amapCmdBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAmapCmd();
            }
        });
        root.addView(amapCmdBtn);

        // ---- ADB 授权命令 ----
        Button authCmdBtn = new Button(this);
        authCmdBtn.setText("ADB 授权命令（悬浮窗 / 调试权限）");
        authCmdBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAuthCmd();
            }
        });
        root.addView(authCmdBtn);

        // ---- 开机预热 ----
        CheckBox preload = new CheckBox(this);
        preload.setTextColor(Color.BLACK);
        preload.setText("开机预热（开机后自动打开一次，提升首次加载速度）");
        preload.setTextSize(14);
        preload.setChecked(Store.isPreload(this));
        preload.setOnCheckedChangeListener(
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                        Store.setPreload(MainActivity.this, v);
                    }
                });
        root.addView(preload);

        final TextView preloadLabel = new TextView(this);
        preloadLabel.setTextSize(13);
        preloadLabel.setTextColor(Color.DKGRAY);
        preloadLabel.setText("预热目标：" + labelOf(Store.getPreloadPkg(this)));
        root.addView(preloadLabel);

        Button pickPreload = new Button(this);
        pickPreload.setText("选择要预热的 App");
        pickPreload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickPreloadApp(preloadLabel);
            }
        });
        root.addView(pickPreload);

        Button orderBtn = new Button(this);
        orderBtn.setText("调整图标顺序");
        orderBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOrderDialog();
            }
        });
        root.addView(orderBtn);

        Button guideBtn = new Button(this);
        guideBtn.setText("使用说明 / 关于");
        guideBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showGuide();
            }
        });
        root.addView(guideBtn);

        return sv;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void updateUi() {
        boolean canOverlay = Settings.canDrawOverlays(this);
        boolean on = Store.isEnabled(this);
        status.setText("悬浮窗权限：" + (canOverlay ? "已授予" : "未授予")
                + "　　运行状态：" + (on ? "已启用" : "未启用"));

        List<String> pkgs = Store.getPkgs(this);
        if (pkgs.isEmpty()) {
            listView.setText("尚未选择任何 App");
        } else {
            PackageManager pm = getPackageManager();
            StringBuilder sb = new StringBuilder("已选 " + pkgs.size() + " 个：\n");
            for (String p : pkgs) {
                String label;
                try {
                    label = pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString();
                } catch (Exception e) {
                    label = "(已卸载)";
                }
                sb.append("  · ").append(label).append('\n');
            }
            listView.setText(sb.toString());
        }
        enableBox.setChecked(on);
    }

    private void pickApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(main, 0);

        Map<String, String> map = new LinkedHashMap<String, String>();
        for (ResolveInfo ri : ris) {
            if (ri.activityInfo == null) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            if (map.containsKey(pkg)) {
                continue;
            }
            String label;
            try {
                label = ri.loadLabel(pm).toString();
            } catch (Exception e) {
                label = pkg;
            }
            map.put(pkg, label);
        }

        final List<Map.Entry<String, String>> entries =
                new ArrayList<Map.Entry<String, String>>(map.entrySet());
        Collections.sort(entries, new java.util.Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) {
                return a.getValue().compareTo(b.getValue());
            }
        });

        final int n = entries.size();
        final String[] pkgs = new String[n];
        final String[] labels = new String[n];
        final List<String> cur = Store.getPkgs(this);
        final boolean[] checked = new boolean[n];
        for (int i = 0; i < n; i++) {
            pkgs[i] = entries.get(i).getKey();
            labels[i] = entries.get(i).getValue() + "　(" + pkgs[i] + ")";
            checked[i] = cur.contains(pkgs[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("选择要显示的 App（可多选）")
                .setMultiChoiceItems(labels, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface d, int which, boolean isChecked) {
                                checked[which] = isChecked;
                            }
                        })
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        List<String> sel = new ArrayList<String>();
                        for (int i = 0; i < n; i++) {
                            if (checked[i]) {
                                sel.add(pkgs[i]);
                            }
                        }
                        Store.setPkgs(MainActivity.this, sel);
                        updateUi();
                        if (Store.isEnabled(MainActivity.this)) {
                            DockService.refresh(MainActivity.this);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String labelOf(String pkg) {
        if (pkg == null || pkg.length() == 0) {
            return "(未设置)";
        }
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg + "（未安装）";
        }
    }

    private void showAmapCmd() {
        String msg = "在电脑上执行（车机需已连 adb）：\n\n"
                + "【禁用高德 · 释放内存】\n"
                + "adb shell pm disable com.autonavi.amapauto\n"
                + "adb shell killall -9 com.autonavi.amapauto\n\n"
                + "【恢复高德】\n"
                + "adb shell pm enable com.autonavi.amapauto\n\n"
                + "说明：\n"
                + "· 禁用后高德不导航、不定位，进程缩成约 129MB 空壳\n"
                + "· 顶部「导航」入口会消失，桌面地图卡片换成别的卡片\n"
                + "· 恢复后要重新启动车机，地图才会回来";
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextSize(15);
        tv.setTextColor(0xFFE8E8E8);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(20), dp(16), dp(20), dp(16));
        new AlertDialog.Builder(this)
                .setTitle("高德开关命令")
                .setView(tv)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void showAuthCmd() {
        String msg = "车机设置里找不到「悬浮窗」开关？用下面两条命令解决。\n"
                + "只需执行一次，重装 App 后要重做。\n\n"
                + "【1. 悬浮窗权限（必做，否则悬浮栏不显示）】\n"
                + "adb shell appops set com.hz.quickdock "
                + "SYSTEM_ALERT_WINDOW allow\n\n"
                + "【2. 开发者选项自动开关（可选）】\n"
                + "adb shell pm grant com.hz.quickdock "
                + "android.permission.WRITE_SECURE_SETTINGS\n\n"
                + "【怎么连上车机】\n"
                + "方式一：手机（推荐，不用电脑）\n"
                + "1. 手机装「甲壳虫 ADB 助手」\n"
                + "2. 车机开热点，手机连这个热点\n"
                + "3. 车机进 开发者选项 打开 USB 调试\n"
                + "4. 甲壳虫里填「车机IP:5555」连接\n"
                + "5. 进「命令行」把上面的命令粘进去执行\n\n"
                + "方式二：电脑\n"
                + "1. 电脑连车机热点或同一 WiFi\n"
                + "2. 电脑装 adb（platform-tools）\n"
                + "3. 执行：adb connect 车机IP:5555\n"
                + "4. 再执行上面的命令\n\n"
                + "【车机 IP 在哪看】\n"
                + "车机设置 → WLAN → 点当前连接的网络 → IP 地址\n\n"
                + "【怎么确认成功】\n"
                + "执行后回到本 App，顶部应显示「悬浮窗权限：已授予」。";
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextSize(14);
        tv.setTextColor(0xFFE8E8E8);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(20), dp(16), dp(20), dp(16));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("ADB 授权命令")
                .setView(sv)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void showGuide() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(16), dp(22), dp(8));

        TextView tv = new TextView(this);
        tv.setTextSize(14);
        tv.setTextColor(0xFFE8E8E8);
        tv.setLineSpacing(dp(4), 1f);
        tv.setText("【这是什么】\n"
                + "一个贴在屏幕左边的悬浮快捷栏，点一下就能打开常用 App，"
                + "不用再一层层点菜单。不 root、不改系统、卸载即净。\n\n"
                + "【怎么用】\n"
                + "1. 按住任意图标可上下拖动整列\n"
                + "2. 点图标 = 启动对应 App（带点击音效）\n"
                + "3. 长按任意图标 = 打开本设置页\n"
                + "4. 「选择要显示的 APP」可勾选任意已安装应用\n"
                + "5. 「调整图标顺序」里点某一项，就让它上移一位\n\n"
                + "【悬浮窗权限打不开怎么办】\n"
                + "如果点「① 授予悬浮窗权限」后找不到开关，或者打开后回来仍显示"
                + "「未授予」，说明车机把这项藏起来了。\n"
                + "解决办法：点「ADB 授权命令」按钮，把第 1 条命令复制到"
                + "甲壳虫 ADB 助手或电脑执行，回本页就会变成「已授予」。\n\n"
                + "【设置项说明】\n"
                + "· 图标大小 / 不透明度：拖滑杆实时生效\n"
                + "· 自动去掉纯色底色：把图标的白底/红底抠透明，只留标志\n"
                + "· 开机预热：开机后自动打开一次指定 App，首次点开更快\n"
                + "· 立即开启开发者选项 + ADB：一键写回调试开关（免密码）\n\n"
                + "【怎么禁用高德（省内存）】\n"
                + "高德被系统三个组件（carautomapmanager / 思必驰 / 智能提醒）绑定，"
                + "App 自己杀不掉，必须从外部发命令。\n\n"
                + "方法一：手机 + 甲壳虫 ADB（推荐，不用电脑）\n"
                + "1. 车机开热点，或手机与车机连同一个 WiFi\n"
                + "2. 手机装「甲壳虫 ADB 助手」\n"
                + "3. 先在车机上点本页「立即开启开发者选项 + ADB」\n"
                + "4. 甲壳虫里填「车机IP:5555」连接\n"
                + "5. 进「命令行」输入：\n"
                + "    pm disable com.autonavi.amapauto\n"
                + "6. 想恢复就输入：\n"
                + "    pm enable com.autonavi.amapauto\n\n"
                + "方法二：电脑 ADB\n"
                + "    adb connect 车机IP:5555\n"
                + "    adb shell pm disable com.autonavi.amapauto\n"
                + "    adb shell pm enable com.autonavi.amapauto   （恢复）\n\n"
                + "【关于开发者密码】\n"
                + "· 本 App 是直接写系统设置，不需要密码\n"
                + "· 如果自动开启失败（权限被撤销），只能手动：\n"
                + "   车机设置 → 开发者选项 → 输密码 → 打开 USB 调试\n"
                + "· 哪吒V 默认密码：#VA3@2021\n"
                + "· 其它车型密码可能不同，可到车主群或论坛查\n\n"
                + "【开发者选项 + ADB 永久开关】\n"
                + "· 勾选：立即开启，且以后每次开机自动开启\n"
                + "· 取消勾选：立即关闭，且以后每次开机保持关闭\n"
                + "· 状态会被记住，重启不会丢\n"
                + "· 依赖一次性授权，装机后执行一次即可：\n"
                + "    adb shell pm grant com.hz.quickdock "
                + "android.permission.WRITE_SECURE_SETTINGS\n"
                + "· 授权丢失时开关会失效，此时只能手动进开发者选项开启\n\n"
                + "【不想要了】\n"
                + "关掉「启用悬浮栏」即隐藏；卸载本程序不留任何残留。\n\n"
                + "【免责声明】\n"
                + "1. 本软件免费提供，不收取任何费用，无付费版本、无广告、无内购。\n"
                + "2. 本软件不修改车机系统分区、不 root、不破解任何商业软件，"
                + "仅通过 Android 公开接口创建悬浮窗与快捷方式。\n"
                + "3. 本软件不收集、不上传任何个人数据；所有配置仅保存在本机。\n"
                + "4. 使用本软件的风险由使用者自行承担。因误操作、系统差异或"
                + "第三方应用导致的任何设备异常或数据丢失，开发者不承担赔偿责任。\n"
                + "5. 本软件为个人作品，与哪吒汽车、合众新能源及其他任何汽车厂商均无关，"
                + "非官方产品，未获其授权或背书。\n"
                + "6. 本软件面向 Android 车机，主要在 哪吒V（1080×1920 / Android 10）上实测；"
                + "其他车型可能同样可用，请自行验证后再长期使用。\n"
                + "7. 本软件不含任何绕过授权、破解付费功能或侵犯他人知识产权的功能。\n"
                + "8. 赞赏完全自愿，不影响任何功能；不赞赏也可完整使用全部功能。\n"
                + "9. 请在遵守当地法律法规及车辆保修条款的前提下使用。\n"
                + "10. 下载、安装或使用本软件，即视为已阅读并同意以上条款。");
        box.addView(tv);

        ImageView qr = new ImageView(this);
        qr.setImageResource(R.drawable.donate);
        int qw = (int) (getResources().getDisplayMetrics().widthPixels * 0.5f);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(qw, qw);
        qp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        qp.topMargin = dp(22);
        qr.setLayoutParams(qp);
        qr.setAdjustViewBounds(true);
        box.addView(qr);

        TextView cn = new TextView(this);
        cn.setTextSize(13);
        cn.setTextColor(0xFFD0D0D0);
        cn.setLineSpacing(dp(4), 1f);
        cn.setPadding(0, dp(14), 0, 0);
        cn.setText("这个工具是我业余时间用 AI 帮忙做的，免费分享给大家，"
                + "省了来回点菜单的功夫。可以给我回一回 tokens，也是有费用的；"
                + "不方便也没关系，觉得好用，随意赞赏，谢谢车友。");
        box.addView(cn);

        TextView en = new TextView(this);
        en.setTextSize(11);
        en.setTextColor(0xFF9E9E9E);
        en.setLineSpacing(dp(4), 1f);
        en.setPadding(0, dp(10), 0, dp(6));
        en.setText("This tool was built in my spare time with the help of AI, "
                + "and I'm sharing it with you all for free — it saves you the "
                + "back-and-forth of digging through the menus. If you find it useful, "
                + "a small tip to help me cover the AI/token costs would mean a lot; "
                + "no pressure at all if it's not convenient. Feel free to donate "
                + "whatever feels right. Thanks, fellow Neta owners!");
        box.addView(en);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("使用说明 / 关于")
                .setView(sv)
                .setPositiveButton("知道了", null)
                .show();
    }

    /** 调整图标顺序：点某一项就让它上移一位。 */
    private void showOrderDialog() {
        final List<String> pkgs = new ArrayList<String>(Store.getPkgs(this));
        if (pkgs.size() < 2) {
            Toast.makeText(this, "至少选 2 个 App 才能调整顺序", Toast.LENGTH_SHORT).show();
            return;
        }
        PackageManager pm = getPackageManager();
        final String[] names = new String[pkgs.size()];
        for (int i = 0; i < pkgs.size(); i++) {
            String label;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkgs.get(i), 0)).toString();
            } catch (Exception e) {
                label = pkgs.get(i);
            }
            names[i] = (i + 1) + ". " + label;
        }
        new AlertDialog.Builder(this)
                .setTitle("调整顺序（点某一项 = 上移一位）")
                .setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            Toast.makeText(MainActivity.this, "已经在最上面了",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String tmp = pkgs.get(which - 1);
                        pkgs.set(which - 1, pkgs.get(which));
                        pkgs.set(which, tmp);
                        Store.setPkgs(MainActivity.this, pkgs);
                        if (Store.isEnabled(MainActivity.this)) {
                            DockService.refresh(MainActivity.this);
                        }
                        showOrderDialog();
                    }
                })
                .setNegativeButton("完成", null)
                .show();
    }

    private void showDonate() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(16), dp(22), dp(8));

        ImageView qr = new ImageView(this);
        qr.setImageResource(R.drawable.donate);
        int qw = (int) (getResources().getDisplayMetrics().widthPixels * 0.52f);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(qw, qw);
        qp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        qr.setLayoutParams(qp);
        qr.setAdjustViewBounds(true);
        box.addView(qr);

        TextView cn = new TextView(this);
        cn.setTextSize(13);
        cn.setTextColor(0xFFD0D0D0);
        cn.setLineSpacing(dp(4), 1f);
        cn.setPadding(0, dp(14), 0, 0);
        cn.setText("这个工具是我业余时间用 AI 帮忙做的，免费分享给大家，"
                + "省了来回点菜单的功夫。可以给我回一回 tokens，也是有费用的；"
                + "不方便也没关系，觉得好用，随意赞赏，谢谢车友。");
        box.addView(cn);

        TextView en = new TextView(this);
        en.setTextSize(12);
        en.setTextColor(Color.GRAY);
        en.setLineSpacing(dp(4), 1f);
        en.setPadding(0, dp(12), 0, 0);
        en.setText("This tool was built in my spare time with the help of AI, "
                + "and I'm sharing it with you all for free — it saves you the "
                + "back-and-forth of digging through the menus. If you find it useful, "
                + "a small tip to help me cover the AI/token costs would mean a lot; "
                + "no pressure at all if it's not convenient. Feel free to donate "
                + "whatever feels right. Thanks, fellow Neta owners!");
        box.addView(en);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("赞赏支持")
                .setView(sv)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void pickPreloadApp(final TextView label) {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(main, 0);

        Map<String, String> map = new LinkedHashMap<String, String>();
        for (ResolveInfo ri : ris) {
            if (ri.activityInfo == null) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            if (map.containsKey(pkg)) {
                continue;
            }
            String lb;
            try {
                lb = ri.loadLabel(pm).toString();
            } catch (Exception e) {
                lb = pkg;
            }
            map.put(pkg, lb);
        }

        final List<Map.Entry<String, String>> entries =
                new ArrayList<Map.Entry<String, String>>(map.entrySet());
        Collections.sort(entries, new java.util.Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) {
                return a.getValue().compareTo(b.getValue());
            }
        });

        final int n = entries.size();
        final String[] pkgs = new String[n];
        String[] labels = new String[n];
        int cur = -1;
        String curPkg = Store.getPreloadPkg(this);
        for (int i = 0; i < n; i++) {
            pkgs[i] = entries.get(i).getKey();
            labels[i] = entries.get(i).getValue() + "　(" + pkgs[i] + ")";
            if (pkgs[i].equals(curPkg)) {
                cur = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("选择开机预热的 App")
                .setSingleChoiceItems(labels, cur, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        Store.setPreloadPkg(MainActivity.this, pkgs[which]);
                        label.setText("预热目标：" + labelOf(pkgs[which]));
                        d.dismiss();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
        setDockVisible(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setDockVisible(true);
    }

    private void setDockVisible(boolean visible) {
        try {
            Intent i = new Intent(this, DockService.class);
            i.setAction(visible ? "com.hz.quickdock.SHOW" : "com.hz.quickdock.HIDE");
            startService(i);
        } catch (Exception ignored) {
        }
    }
}
