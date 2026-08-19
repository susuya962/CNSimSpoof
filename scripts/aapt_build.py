# -*- coding: utf-8 -*-
# aapt 直接打包路线(绕过 apktool): manifest(纯ASCII) + aapt package
# 产出含正确二进制 manifest 的 APK 壳, 然后 7z 注入 classes.dex + assets
import subprocess, os, shutil

LIB = r"C:\Users\hgh20\AppData\Local\Temp\spoof_java\libs"
AAPT = os.path.join(LIB, "bt", "android-14", "aapt.exe")
DEX = r"C:\Users\hgh20\AppData\Local\Temp\spoof_java\build\classes.dex"
OUT_APK = r"C:\Users\hgh20\AppData\Local\Temp\SysPref-v8b.apk"
SH = r"C:\Users\hgh20\AppData\Local\Temp\stage"
ASSETS_SRC = None
UI_HTML = r"C:\Users\hgh20\AppData\Local\Temp\spoof_build\assets\ui.html"

def run(cmd, timeout=120):
    r = subprocess.run(cmd, capture_output=True, timeout=timeout)
    out = ((r.stdout or b"") + (r.stderr or b"")).decode("gbk", errors="replace")
    return r.returncode, out

# 1. staging 目录
if os.path.exists(SH):
    shutil.rmtree(SH)
os.makedirs(os.path.join(SH, "assets"))
os.makedirs(os.path.join(SH, "res", "values"))

# 2. manifest（纯 ASCII, label 引用资源）
MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.util.syspref">

    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="28"/>
    <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS"/>

    <application
        android:label="@string/app_label"
        android:icon="@android:drawable/sym_def_app_icon"
        android:allowBackup="false">

        <meta-data android:name="xposedmodule" android:value="true"/>
        <meta-data android:name="xposedminversion" android:value="93"/>
        <meta-data android:name="xposeddescription" android:value="System parameter overlay"/>

        <activity
            android:name="com.util.syspref.Ui"
            android:label="@string/app_label"
            android:exported="true"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
with open(os.path.join(SH, "AndroidManifest.xml"), "w", encoding="ascii") as f:
    f.write(MANIFEST)

# 3. 资源（中文字符引用, UTF-8）
with open(os.path.join(SH, "res", "values", "strings.xml"), "w", encoding="utf-8") as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
            '  <string name="app_label">\u7cfb\u7edf\u53c2\u6570</string>\n'
            '</resources>\n')

# 4. assets
shutil.copy2(UI_HTML, os.path.join(SH, "assets", "ui.html"))
with open(os.path.join(SH, "assets", "xposed_init"), "w", encoding="ascii") as f:
    f.write("com.util.syspref.Core")

# 5. aapt package
rc, out = run([AAPT, "p", "-f",
               "--version-code", "14",
               "--version-name", "1.13",
               "-M", os.path.join(SH, "AndroidManifest.xml"),
               "-S", os.path.join(SH, "res"),
               "-A", os.path.join(SH, "assets"),
               "-I", os.path.join(LIB, "android34.jar"),
               "-F", OUT_APK])
print("aapt:", rc)
if rc != 0:
    print(out[:3000])
    raise SystemExit(1)

# 6. 注入 classes.dex（aapt 用 -F 产出的是要用 aapt add? 不, 直接 7z 加）
# aapt p -F 产出的 .apk 内文件用 zip 对齐存储——直接 7z a 加进去
rc, out = run(["7z", "a", "-tzip", OUT_APK, DEX])
print("7z dex:", rc)
if rc != 0:
    print(out[:1000])
    raise SystemExit(2)

# 7. zipalign（4 字节对齐, .so 无关紧要但规范）
ZIPALIGN = os.path.join(LIB, "bt", "android-14", "zipalign.exe")
aligned = OUT_APK.replace(".apk", "-al.apk")
if os.path.exists(ZIPALIGN):
    rc, out = run([ZIPALIGN, "-f", "4", OUT_APK, aligned])
    print("zipalign:", rc)
    if rc == 0:
        os.replace(aligned, OUT_APK)

print("APK:", os.path.getsize(OUT_APK))

# 8. 验证 manifest 二进制内容
rc, out = run([AAPT, "dump", "xmltree", OUT_APK, "AndroidManifest.xml"])
tree = ((r_out if (r_out := out) else ""))
ok_meta = "xposedmodule" in tree
ok_act = "com.util.syspref.Ui" in tree
print("meta xposedmodule:", ok_meta, "| activity Ui:", ok_act)
if not (ok_meta and ok_act):
    print(tree[:2000])
    raise SystemExit(3)
print("OK")
