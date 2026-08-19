# -*- coding: utf-8 -*-
# 编译 Java 源码 -> classes.dex（javac + d8）
import subprocess, os, sys

SRC = r"C:\Users\hgh20\AppData\Local\Temp\spoof_java"
LIB = os.path.join(SRC, "libs")
BUILD = os.path.join(SRC, "build")
OUT_APK_DIR = r"C:\Users\hgh20\AppData\Local\Temp\spoof_build"

def run(cmd, cwd=None, timeout=300):
    r = subprocess.run(cmd, capture_output=True, timeout=timeout, cwd=cwd)
    out = ((r.stdout or b"") + (r.stderr or b"")).decode("gbk", errors="replace").strip()
    return r.returncode, out

# 1. clean
import shutil
if os.path.exists(os.path.join(BUILD, "cls")):
    shutil.rmtree(os.path.join(BUILD, "cls"))
os.makedirs(os.path.join(BUILD, "cls"), exist_ok=True)

# 2. javac（stubs + src 一起编译，classpath 含 android.jar）
srcs = []
for root, dirs, files in os.walk(os.path.join(SRC, "stubs")):
    for f in files:
        if f.endswith(".java"):
            srcs.append(os.path.join(root, f))
for root, dirs, files in os.walk(os.path.join(SRC, "src")):
    for f in files:
        if f.endswith(".java"):
            srcs.append(os.path.join(root, f))

listfile = os.path.join(BUILD, "sources.txt")
with open(listfile, "w", encoding="utf-8") as fh:
    fh.write("\n".join(srcs))

cp = os.path.join(LIB, "android34.jar")
rc, out = run(["javac", "-source", "8", "-target", "8",
               "-bootclasspath", cp,
               "-classpath", cp,
               "-d", os.path.join(BUILD, "cls"),
               "@" + listfile])
print("javac rc:", rc)
if rc != 0:
    print(out[:4000])
    sys.exit(1)

# 3. d8 -> classes.dex（只 dex 应用类！stub 类绝不能进 dex：
#    Xposed API 由 LSPosed 运行时提供；android.* stub 会成检测特征）
cls_files = []
for root, dirs, files in os.walk(os.path.join(BUILD, "cls")):
    for f in files:
        if f.endswith(".class"):
            p = os.path.join(root, f).replace("/", os.sep)
            if (os.sep + "com" + os.sep + "util" + os.sep + "syspref" + os.sep) in p:
                cls_files.append(os.path.join(root, f))

# d8 从 build-tools 34 提取
d8 = os.path.join(LIB, "bt", "android-14", "d8.bat")
print("d8:", d8)
if not d8:
    print("d8 not found - download build-tools")
    sys.exit(2)

dexlist = os.path.join(BUILD, "classes.txt")
with open(dexlist, "w", encoding="utf-8") as fh:
    fh.write("\n".join(cls_files))

rc, out = run([d8, "--release", "--min-api", "21",
               "--lib", cp,
               "--output", BUILD,
               "@" + dexlist])
print("d8 rc:", rc)
if rc != 0:
    print(out[:4000])
    sys.exit(3)

print("dex:", os.path.exists(os.path.join(BUILD, "classes.dex")))
