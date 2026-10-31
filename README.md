# NETAV

哪吒 V 车机的侧边快捷导航栏。

不用改系统、不用 root，装个普通 App 就能在屏幕左边缘得到一排能点的图标——点开百度地图、网易云，不用再一层层点菜单。

> 个人业余作品，非官方工具。改车机有风险，请自行判断，出问题自负。

---

## 功能

| 功能 | 说明 |
|---|---|
| 左侧悬浮图标列 | 贴屏幕左边缘、垂直居中，按住任意图标可上下拖动整列 |
| 任意 App 都行 | 设置里从已安装应用列表勾选，可多选 |
| 图标美化 | 图标大小 36–110px 可调；整列不透明度 20%–100% 可调 |
| 自动抠底色 | 按四条边中点采样主色，把纯色背景渐变透明，只留标志 |
| 点击音效 | 点图标时播放车机自带的 `Effect_Tick.ogg`，与系统 UI 点击声一致 |
| 开机自启 | 重启后自动出现，无需手动打开 |
| 开机预热 | 开机后自动打开一次指定 App，提升首次加载速度 |
| 开机确保调试可用 | 自动把 `adb_enabled` / `development_settings_enabled` 写回 1 |
| 高德开关命令 | 弹窗给出禁用/释放高德的 adb 命令，复制到电脑执行 |

---

## 安装

### 方式一：adb

```bash
adb install -r Firewheel-v1.0.apk
```

### 方式二：U 盘

把 APK 拷到 U 盘，插车机安装（需在设置里允许「安装未知来源应用」）。

**两种方式装完效果完全一样，都【不需要】额外授权。**

---

## 首次使用（全程不需要 adb）

1. 打开 **Firewheel**
2. 点 **① 授予悬浮窗权限** → 系统会跳到「显示在其他应用上层 / 悬浮窗」页面 → 打开开关 → 返回 App
3. 点 **② 选择要显示的 APP** → 勾选想要的 App → 保存
4. 勾选 **③ 启用悬浮栏** → 左侧图标列出现

> 悬浮窗走的是 Android 标准「特殊应用权限」，**在 App 里点一下、系统设置里开一下就行，全程不需要 adb、不需要 root**。

### 可选：想让「开机自动开启开发者选项 + ADB」也能用

只有这一项需要 adb（一次性，之后永久有效）：

```bash
adb shell pm grant com.hz.quickdock android.permission.WRITE_SECURE_SETTINGS
```

不执行也能正常用悬浮栏，只是那个开关勾选时会弹提示「无法自动开启」，并引导你跳到设置页手动开 USB 调试。

### 备用：车机设置里找不到「悬浮窗」开关怎么办

极少数车机把这项藏起来了。**这时才需要**用命令兜底：

```bash
adb shell appops set com.hz.quickdock SYSTEM_ALERT_WINDOW allow
```

也可以用手机装「甲壳虫 ADB 助手」连车机执行（App 里的「ADB 授权命令」按钮有完整步骤）。

---

### 想用「一键小度语音」

> ⚠️ **v1.0 已移除该功能**。实测在 哪吒V（Android 10 / 百度地图汽车版 21.5.3）上，点开语音条后百度**从不建立录音流**（`dumpsys media.audio_flinger` 始终只有思必驰的 echo reference 一条），已排除权限、麦克风抢占、点击方式等因素，属百度侧限制。相关代码已删除，详见下方「已知限制」。
>
> **建议：语音控制导航直接用手机自带的 AI 助手**（小爱同学、小度、Siri 都行），让手机发导航指令，比在车机上折腾省心得多。

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
| 百度地图语音输入不可用 | 点开小度语音条后百度地图**不启动录音**（audio_flinger 无新录音流），已排除权限/抢占/点击方式，属百度车机版 21.5.3 自身限制。v1.0 已移除相关功能 |
| 无法从 App 内禁用/释放高德 | `/system/xbin/su` 只对 shell/root 开放，普通 App 执行返回 `error=13 Permission denied` |
| 方向盘语音键无法改绑到百度 | 方向盘走 CAN → MCU → `com.android.car` → `TOGGLE_VOICE` 广播，改绑需要动系统服务 |
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

## 免责声明

1. 本软件免费提供，不收取任何费用，无付费版本、无广告、无内购。
2. 本软件不修改车机系统分区、不 root、不破解任何商业软件，仅通过 Android 公开接口创建悬浮窗与快捷方式。
3. 本软件不收集、不上传任何个人数据；所有配置仅保存在本机。
4. 使用本软件的风险由使用者自行承担。因误操作、系统差异或第三方应用导致的任何设备异常或数据丢失，开发者不承担赔偿责任。
5. 本软件为个人作品，与哪吒汽车、合众新能源及其他任何汽车厂商均无关，非官方产品，未获其授权或背书。
6. 本软件面向 Android 车机，主要在 哪吒V（1080×1920 / Android 10）上实测；其他车型可能同样可用，请自行验证后再长期使用。
7. 本软件不含任何绕过授权、破解付费功能或侵犯他人知识产权的功能。
8. 赞赏完全自愿，不影响任何功能；不赞赏也可完整使用全部功能。
9. 请在遵守当地法律法规及车辆保修条款的前提下使用。
10. 下载、安装或使用本软件，即视为已阅读并同意以上条款。

---

## License

MIT



