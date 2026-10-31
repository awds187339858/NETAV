# -*- coding: utf-8 -*-
import json
import os
import subprocess
import urllib.request

REPO = "awds187339858/NETAV"
TAG = "v1.0"
NAME = "NETAV快捷侧栏"
APK = os.path.join("release", "NETAV-v1.0.apk")
PROXY = "http://127.0.0.1:7890"

BODY = """## NETAV 快捷侧栏 v1.0

哪吒 V 车机侧边快捷导航栏。

### 功能
- 左侧悬浮图标列，可上下拖动、调大小、调透明度、自动抠图标底色
- 任意已安装 App 都能加入，支持调整顺序
- 开机自启、开机预热、点击音效（走系统 FX 通道，与车机 UI 一致）
- 可选的「开机自动开启开发者选项 + ADB」

### 安装
1. 下载 `NETAV-v1.0.apk`
2. U 盘拷到车机安装，或 `adb install -r NETAV-v1.0.apk`
3. 打开 App → 点「① 授予悬浮窗权限」→ 系统里打开 → 选 App → 启用悬浮栏

**全程不需要 adb、不需要 root**（只有可选的「开发者选项」功能需要一次性 `pm grant`）。

### 说明
- 实测机型：哪吒 V 2021 款 400L（Android 10 / 1080×1920）
- 其他 Android 8+ 车机理论可用，欢迎反馈
- 免费、开源、无广告、不收集任何数据
"""


def get_token():
    p = subprocess.run(
        ["git", "credential", "fill"],
        input="protocol=https\nhost=github.com\n\n",
        capture_output=True, text=True)
    for line in (p.stdout or "").splitlines():
        if line.startswith("password="):
            return line.split("=", 1)[1]
    return None


def opener():
    h = urllib.request.ProxyHandler({"http": PROXY, "https": PROXY})
    return urllib.request.build_opener(h)


def main():
    token = get_token()
    if not token:
        print("FAIL: 拿不到 token")
        return 1
    print("token 已取得（长度 %d，不显示内容）" % len(token))

    if not os.path.exists(APK):
        print("FAIL: 找不到 APK", APK)
        return 1

    op = opener()

    # 先看是否已有同名 release
    try:
        req = urllib.request.Request(
            "https://api.github.com/repos/%s/releases" % REPO,
            headers={"Authorization": "token " + token,
                     "Accept": "application/vnd.github+json"})
        rels = json.loads(op.open(req).read())
        for r in rels:
            if r.get("tag_name") == TAG:
                print("已存在 release:", r["html_url"], "- 将删除重建")
                d = urllib.request.Request(
                    "https://api.github.com/repos/%s/releases/%d" % (REPO, r["id"]),
                    method="DELETE",
                    headers={"Authorization": "token " + token,
                             "Accept": "application/vnd.github+json"})
                op.open(d)
                break
    except Exception as e:
        print("查询已有 release 失败（继续）:", e)

    # 创建 release
    payload = json.dumps({
        "tag_name": TAG,
        "name": NAME,
        "body": BODY,
        "draft": False,
        "prerelease": False,
    }, ensure_ascii=False).encode("utf-8")

    req = urllib.request.Request(
        "https://api.github.com/repos/%s/releases" % REPO,
        data=payload, method="POST",
        headers={"Authorization": "token " + token,
                 "Accept": "application/vnd.github+json",
                 "Content-Type": "application/json; charset=utf-8"})
    rel = json.loads(op.open(req).read())
    print("release 创建成功:", rel["html_url"])
    print("  id =", rel["id"], " name =", rel["name"], " tag =", rel["tag_name"])

    # 上传 APK
    data = open(APK, "rb").read()
    url = ("https://uploads.github.com/repos/%s/releases/%d/assets?name=NETAV-v1.0.apk"
           % (REPO, rel["id"]))
    req2 = urllib.request.Request(
        url, data=data, method="POST",
        headers={"Authorization": "token " + token,
                 "Accept": "application/vnd.github+json",
                 "Content-Type": "application/octet-stream"})
    asset = json.loads(op.open(req2).read())
    print("APK 上传成功:", asset["browser_download_url"])
    print("  size =", asset["size"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

