# -*- coding: utf-8 -*-
"""不用 Gradle，直接用 aapt2 + javac + d8 + apksigner 打一个 APK。"""
import glob
import os
import shutil
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SDK = os.path.join(os.path.dirname(HERE), "android-sdk")
BT = os.path.join(SDK, "build-tools", "35.0.0")
ANDROID_JAR = os.path.join(SDK, "platforms", "android-29", "android.jar")
JAVA_HOME = r"C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot"

AAPT2 = os.path.join(BT, "aapt2.exe")
D8 = os.path.join(BT, "d8.bat")
ZIPALIGN = os.path.join(BT, "zipalign.exe")
APKSIGNER = os.path.join(BT, "apksigner.bat")
JAVAC = os.path.join(JAVA_HOME, "bin", "javac.exe")
KEYTOOL = os.path.join(JAVA_HOME, "bin", "keytool.exe")

BUILD = os.path.join(HERE, "build")
KS = os.path.join(BUILD, "quickdock.keystore")
KS_PASS = "dock123456"
ALIAS = "dock"


def run(cmd, cwd=None):
    print(">>", " ".join(cmd[:4]), "..." if len(cmd) > 4 else "")
    r = subprocess.run(cmd, cwd=cwd, capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    if r.stdout and r.stdout.strip():
        print(r.stdout.strip()[:2000])
    if r.returncode != 0:
        print("STDERR:", (r.stderr or "")[:3000])
        raise SystemExit("FAILED (%d): %s" % (r.returncode, cmd[0]))
    return r


def main():
    os.makedirs(BUILD, exist_ok=True)
    for d in ("res_c", "gen", "classes", "dex"):
        p = os.path.join(BUILD, d)
        if os.path.isdir(p):
            shutil.rmtree(p, ignore_errors=True)

    # 1) 编译资源
    run([AAPT2, "compile", "--dir", os.path.join(HERE, "res"),
         "-o", os.path.join(BUILD, "res.zip")])

    # 2) 链接
    base = os.path.join(BUILD, "base.apk")
    if os.path.exists(base):
        os.remove(base)
    run([AAPT2, "link",
         "-o", base,
         "-I", ANDROID_JAR,
         "--manifest", os.path.join(HERE, "AndroidManifest.xml"),
         "--java", os.path.join(BUILD, "gen"),
         "--min-sdk-version", "29",
         "--target-sdk-version", "29",
         os.path.join(BUILD, "res.zip")])

    # 3) javac
    srcs = glob.glob(os.path.join(HERE, "src", "**", "*.java"), recursive=True)
    rjava = glob.glob(os.path.join(BUILD, "gen", "**", "*.java"), recursive=True)
    classes = os.path.join(BUILD, "classes")
    os.makedirs(classes, exist_ok=True)
    run([JAVAC, "-encoding", "UTF-8", "--release", "8", "-g:none",
         "-classpath", ANDROID_JAR,
         "-d", classes] + srcs + rjava)

    # 4) d8 -> dex
    dexdir = os.path.join(BUILD, "dex")
    os.makedirs(dexdir, exist_ok=True)
    clss = glob.glob(os.path.join(classes, "**", "*.class"), recursive=True)
    run([D8, "--lib", ANDROID_JAR, "--min-api", "29",
         "--output", dexdir] + clss)

    # 5) 把 classes.dex 塞进 APK
    dex = os.path.join(dexdir, "classes.dex")
    if not os.path.exists(dex):
        raise SystemExit("classes.dex 未生成")
    with zipfile.ZipFile(base, "a", zipfile.ZIP_DEFLATED) as z:
        z.write(dex, "classes.dex")
    print("   dex 已加入 APK:", os.path.getsize(dex), "bytes")

    # 6) zipalign
    aligned = os.path.join(BUILD, "aligned.apk")
    if os.path.exists(aligned):
        os.remove(aligned)
    run([ZIPALIGN, "-f", "-p", "4", base, aligned])

    # 7) keystore
    if not os.path.exists(KS):
        run([KEYTOOL, "-genkeypair", "-keystore", KS, "-alias", ALIAS,
             "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
             "-storepass", KS_PASS, "-keypass", KS_PASS,
             "-dname", "CN=QuickDock,O=HZ,C=CN"])

    # 8) 签名
    out = os.path.join(BUILD, "quickdock.apk")
    if os.path.exists(out):
        os.remove(out)
    run([APKSIGNER, "sign", "--ks", KS, "--ks-key-alias", ALIAS,
         "--ks-pass", "pass:" + KS_PASS, "--key-pass", "pass:" + KS_PASS,
         "--v1-signing-enabled", "true",
         "--v2-signing-enabled", "true",
         "--v3-signing-enabled", "true",
         "--out", out, aligned])

    # 9) 校验
    run([APKSIGNER, "verify", "--verbose", out])

    print()
    print("=== 完成 ===")
    print("APK:", out, os.path.getsize(out), "bytes")


if __name__ == "__main__":
    main()
