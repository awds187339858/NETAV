# 快捷侧栏 · QuickDock

给**哪吒 V（2021 款 400L，Android 10）**车机做的侧边悬浮快捷栏。

不用改系统、不用 root，装个普通 App 就能在屏幕左边缘得到一排能点的图标——点开百度地图、网易云，或者一键唤起百度地图的「小度」语音，不用再一层层点菜单。

> 个人业余作品，非官方工具。改车机有风险，请自行判断，出问题自负。

---

## 功能

| 功能 | 说明 |
|---|---|
| 左侧悬浮图标列 | 贴屏幕左边缘、垂直居中，按住任意图标可上下拖动整列 |
| 任意 App 都行 | 设置里从已安装应用列表勾选，可多选 |
| 图标美化 | 图标大小 36–110px 可调；整列不透明度 20%–100% 可调 |
| 自动抠底色 | 按四条边中点采样主色，把纯色背景渐变透明，只留标志 |
| 开机自启 | 重启后自动出现，无需手动打开 |
| 一键小度语音 | 点麦克风图标 → 百度地图自动打开并进入语音监听，**不用喊唤醒词** |
| 开机预热 | 开机后自动打开一次指定 App，提升首次加载速度 |
| 开机确保调试可用 | 自动把 `adb_enabled` / `development_settings_enabled` 写回 1 |
| 高德开关命令 | 弹窗给出禁用/释放高德的 adb 命令，复制到电脑执行 |

---

## 安装

### 方式一：adb（推荐）

```bash
adb install -r QuickDock.apk
```

装完先授权一次：

```bash
# 悬浮窗
adb shell appops set com.hz.quickdock SYSTEM_ALERT_WINDOW allow

# 开机确保 adb 可用（可选）
adb shell pm grant com.hz.quickdock android.permission.WRITE_SECURE_SETTINGS
```

### 方式二：U 盘

把 APK 拷到 U 盘，插车机安装。装完**仍需**用 adb 执行上面两条授权，否则悬浮窗不会显示。

---

## 首次使用

1. 打开「快捷侧栏」
2. 点 **① 授予悬浮窗权限** → 打开开关
3. 点 **② 选择要显示的 APP** → 勾选想要的 App → 保存
4. 勾选 **③ 启用悬浮栏** → 左侧图标列出现

### 想用「一键小度语音」

1. 勾选 **显示「小度语音」按钮**
2. 点 **打开无障碍设置**，找到「快捷侧栏」并开启
   （或用命令追加，不覆盖已有的无障碍服务）
3. 点悬浮栏的麦克风图标 → 百度地图打开并开始听 → 直接说「导航到 xx」

> 无障碍服务只在你点麦克风后的 15 秒内工作，且只接收百度地图的窗口事件，平时完全不参与任何界面。

---

## 编译

不用 Gradle，直接用 SDK 里的工具链打 APK。

### 依赖

- JDK 11+（实测 21）
- Android SDK：
  - `platforms/android-29`
  - `build-tools/35.0.0`（**必须 35**，34 的 d8 处理匿名内部类会抛 NPE）
  - `cmdline-tools`

### 步骤

```bash
# 1. 修改 build.py 顶部的路径
JAVA_HOME  = r"C:\Program Files\Microsoft\jdk-21..."
SDK        = r"...\android-sdk"

# 2. 编译
python build.py

# 3. 产物
build/quickdock.apk
```

`build.py` 会自动：`aapt2 compile` → `aapt2 link` → `javac --release 8` → `d8` → 塞 dex → `zipalign` → 首次自动生成 keystore 并 `apksigner` 签名。

> **keystore 不要提交**（`build/quickdock.keystore`）。换签名后无法覆盖安装旧版本。

---

## 已知限制

| 限制 | 原因 |
|---|---|
| 无法从 App 内禁用/释放高德 | `/system/xbin/su` 只对 shell/root 开放，普通 App 执行返回 `error=13 Permission denied` |
| 语音键（方向盘）无法改绑到百度 | 方向盘走 CAN → MCU → `com.android.car` → `TOGGLE_VOICE` 广播，改绑需要动系统服务 |
| 重启后无障碍可能被系统重置 | 部分车机会清空 `enabled_accessibility_services`，本 App 已在开机后 4/12/30 秒自动补回 |
| 图标坐标依赖 1080×1920 屏幕 | 分辨率差异大的车机需要自行调整 `dp()` 换算 |

---

## 卸载

```bash
adb uninstall com.hz.quickdock
```

或车机应用列表里卸载。**不写系统分区、不改系统设置**，卸载后不留残留。

如果改过无障碍设置，卸载前建议手动关掉「快捷侧栏」的无障碍开关。

---

## 说明

这个工具是我业余时间用 AI 帮忙做的，免费分享给大家，省了来回点菜单的功夫。可以给我回一回 tokens，也是有费用的；不方便也没关系，觉得好用，随意赞赏，谢谢车友。

This tool was built in my spare time with the help of AI, and I'm sharing it with you all for free — it saves you the back-and-forth of digging through the menus. If you find it useful, a small tip to help me cover the AI/token costs would mean a lot; no pressure at all if it's not convenient. Feel free to donate whatever feels right. Thanks, fellow Neta owners!

---

## License

MIT
