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

        // ---- 小度语音按钮 ----
        CheckBox voiceBtn = new CheckBox(this);
        voiceBtn.setText("显示「小度语音」按钮（点击直达百度语音，需无障碍权限）");
        voiceBtn.setTextSize(14);
        voiceBtn.setChecked(Store.isVoiceBtn(this));
        voiceBtn.setOnCheckedChangeListener(
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                        Store.setVoiceBtn(MainActivity.this, v);
                        if (Store.isEnabled(MainActivity.this)) {
                            DockService.refresh(MainActivity.this);
                        }
                    }
                });
        root.addView(voiceBtn);

        Button accBtn = new Button(this);
        accBtn.setText("打开无障碍设置（授权语音助手）");
        accBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(
                            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "打不开无障碍设置：" + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        root.addView(accBtn);

        // ---- 高德开关按钮 ----
        CheckBox amapBtn = new CheckBox(this);
        amapBtn.setText("显示「高德开关」按钮（一键禁用/释放高德并释放内存）");
        amapBtn.setTextSize(14);
        amapBtn.setChecked(Store.isAmapBtn(this));
        amapBtn.setOnCheckedChangeListener(
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                        Store.setAmapBtn(MainActivity.this, v);
                        if (Store.isEnabled(MainActivity.this)) {
                            DockService.refresh(MainActivity.this);
                        }
                    }
                });
        root.addView(amapBtn);

        // ---- 立即开启开发者选项 ----
        Button devBtn = new Button(this);
        devBtn.setText("立即开启开发者选项 + adb（免密码）");
        devBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean ok = false;
                try {
                    android.provider.Settings.Global.putInt(getContentResolver(),
                            android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                    android.provider.Settings.Global.putInt(getContentResolver(),
                            android.provider.Settings.Global.ADB_ENABLED, 1);
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
                Toast.makeText(MainActivity.this,
                        ok ? "已开启：开发者选项 + USB 调试" : "失败：缺少 WRITE_SECURE_SETTINGS 权限",
                        Toast.LENGTH_SHORT).show();
                updateUi();
            }
        });
        root.addView(devBtn);

        // ---- 高德开关命令（复制到电脑执行）----
        Button amapCmdBtn = new Button(this);
        amapCmdBtn.setText("高德禁用 / 释放 命令");
        amapCmdBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAmapCmd();
            }
        });
        root.addView(amapCmdBtn);

        // ---- 开机预热 ----
        CheckBox preload = new CheckBox(this);
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

        CheckBox fixAdb = new CheckBox(this);
        fixAdb.setText("开机时确保 adb 调试可用（需一次性授权）");
        fixAdb.setTextSize(14);
        fixAdb.setChecked(Store.isFixAdb(this));
        fixAdb.setOnCheckedChangeListener(
                new android.widget.CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                        Store.setFixAdb(MainActivity.this, v);
                    }
                });
        root.addView(fixAdb);

        TextView tip = new TextView(this);
        tip.setTextSize(14);
        tip.setTextColor(Color.DKGRAY);
        tip.setLineSpacing(dp(3), 1f);
        tip.setPadding(0, dp(28), 0, 0);
        tip.setText("【怎么用】\n"
                + "1. 左侧悬浮栏贴着屏幕左边缘，按住任意图标可上下拖动整列\n"
                + "2. 点图标 = 启动对应 App\n"
                + "3. 长按任意图标 = 打开这个设置页\n"
                + "4. 麦克风图标 = 一键唤起百度地图的小度语音\n\n"
                + "【语音说明】\n"
                + "· 点麦克风后，百度地图会自动打开并开始听\n"
                + "· 不需要喊「你好哪吒」，也不用喊「小度小度」\n"
                + "· 直接说指令，例如「导航到潍坊站」\n"
                + "· 「导航回家」需要先在百度地图里设好家里的地址\n\n"
                + "【关于高德】\n"
                + "· 高德已禁用，不导航、不定位，只占少量内存，不影响百度导航\n"
                + "· 需要恢复，用上面「高德禁用 / 释放 命令」，复制到电脑执行\n\n"
                + "【不想要了】\n"
                + "· 关掉「启用悬浮栏」即隐藏；卸载本程序不留任何残留\n");
        root.addView(tip);

        // ---- 赞赏 ----
        TextView donateTitle = new TextView(this);
        donateTitle.setTextSize(19);
        donateTitle.setTextColor(Color.BLACK);
        donateTitle.setPadding(0, dp(40), 0, dp(12));
        donateTitle.setText("赞赏支持");
        root.addView(donateTitle);

        ImageView qr = new ImageView(this);
        qr.setImageResource(R.drawable.donate);
        int qw = (int) (getResources().getDisplayMetrics().widthPixels * 0.55f);
        qr.setLayoutParams(new LinearLayout.LayoutParams(qw, qw));
        qr.setAdjustViewBounds(true);
        root.addView(qr);

        TextView cnText = new TextView(this);
        cnText.setTextSize(13);
        cnText.setTextColor(Color.DKGRAY);
        cnText.setLineSpacing(dp(3), 1f);
        cnText.setPadding(0, dp(14), 0, 0);
        cnText.setText("这个工具是我业余时间用 AI 帮忙做的，免费分享给大家，"
                + "省了来回点菜单的功夫。可以给我回一回 tokens，也是有费用的；"
                + "不方便也没关系，觉得好用，随意赞赏，谢谢车友。");
        root.addView(cnText);

        TextView enText = new TextView(this);
        enText.setTextSize(12);
        enText.setTextColor(Color.GRAY);
        enText.setLineSpacing(dp(3), 1f);
        enText.setPadding(0, dp(14), 0, dp(28));
        enText.setText("This tool was built in my spare time with the help of AI, "
                + "and I'm sharing it with you all for free — it saves you the "
                + "back-and-forth of digging through the menus. If you find it useful, "
                + "a small tip to help me cover the AI/token costs would mean a lot; "
                + "no pressure at all if it's not convenient. Feel free to donate "
                + "whatever feels right. Thanks, fellow Neta owners!");
        root.addView(enText);

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
        tv.setTextColor(Color.BLACK);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(20), dp(16), dp(20), dp(16));
        new AlertDialog.Builder(this)
                .setTitle("高德开关命令")
                .setView(tv)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void showDonate() {
        String msg = "这套东西没有广告、没有会员、不偷偷跑流量，\n"
                + "也不会收集你的任何数据。\n\n"
                + "从找到车机的语音入口，到让百度能听见你说话，\n"
                + "中间试错了几十次，来回重启了不知道多少遍。\n\n"
                + "如果你觉得它让开车方便了一点，\n"
                + "可以扫码请我喝瓶水。\n\n"
                + "不打赏也完全不影响使用，功能永远免费。";
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextSize(15);
        tv.setTextColor(Color.BLACK);
        tv.setLineSpacing(dp(4), 1f);
        tv.setPadding(dp(24), dp(16), dp(24), dp(8));
        TextView tip = new TextView(this);
        tip.setText("（赞赏码图片待添加）");
        tip.setTextSize(12);
        tip.setTextColor(Color.GRAY);
        tip.setPadding(dp(24), 0, dp(24), dp(16));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(tv);
        box.addView(tip);

        new AlertDialog.Builder(this)
                .setTitle("赞赏支持")
                .setView(box)
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
