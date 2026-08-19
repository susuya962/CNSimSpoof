# -*- coding: utf-8 -*-
# 通用部署: 编译 -> 打包 -> 签名 -> 安装 -> 授权(WRITE_SECURE_SETTINGS) -> LSPosed DB 启用+路径校正 -> 软重启 SystemUI
# 用法: python deploy_v8c.py  (手机已 root + adb 连接; LSPosed 管理器可用时第3步可改为手动勾选)
import subprocess, time, os, sqlite3

# ===== 按需修改的本机路径 =====
T = r"C:\Users\hgh20\AppData\Local\Temp"          # 工作目录(compile_dex.py / aapt_build.py / 中间产物)
SIGNER = r"C:\Users\hgh20\scoop\apps\uber-apk-signer.jar"  # uber-apk-signer 路径
PKG = "com.util.syspref"

def run(cmd, timeout=120):
    r = subprocess.run(cmd, capture_output=True, timeout=timeout)
    return r.returncode, ((r.stdout or b"") + (r.stderr or b"")).decode("gbk", errors="replace")

def sh(s, timeout=60):
    return run(["adb", "shell", "su", "-c", s], timeout)[1].strip()

# 1. 编译 + 打包 + 签名
for script in ["compile_dex.py", "aapt_build.py"]:
    rc, out = run(["python", os.path.join(T, script)])
    print(script, "->", rc)
    if rc != 0:
        print(out[-1500:])
        raise SystemExit(1)

APK = os.path.join(T, "SysPref-v8b.apk")
rc, out = run(["java", "-jar", SIGNER, "-a", APK, "--overwrite"])
print("sign:", rc)
if rc != 0:
    raise SystemExit(2)

# 2. 安装 + 授权（关键！WRITE_SECURE_SETTINGS 是签名权限，侧载默认拒绝，UI 写配置全靠它）
rc, out = run(["adb", "install", "-r", APK])
print("install:", out.strip().splitlines()[-1] if out.strip() else rc)
if "Success" not in out:
    raise SystemExit(3)
print("grant:", run(["adb", "shell", "pm", "grant", PKG, "android.permission.WRITE_SECURE_SETTINGS"])[1].strip() or "(done)")

# 3. 路径校正（WAL 感知: 三件套拉回读真值）
path = run(["adb", "shell", "pm", "path", PKG])[1].strip().replace("package:", "").splitlines()[0]
print("apk path:", path)

sh("cp /data/adb/lspd/config/modules_config.db /sdcard/Download/v8c.db; cp /data/adb/lspd/config/modules_config.db-wal /sdcard/Download/v8c.db-wal 2>/dev/null; cp /data/adb/lspd/config/modules_config.db-shm /sdcard/Download/v8c.db-shm 2>/dev/null; chmod 644 /sdcard/Download/v8c.db*")
DB = os.path.join(T, "v8c.db")
for f in ["v8c.db", "v8c.db-wal", "v8c.db-shm"]:
    run(["adb", "pull", f"/sdcard/Download/{f}", os.path.join(T, f)])

db = sqlite3.connect(DB)
cur = db.cursor()
row = list(cur.execute("SELECT apk_path FROM modules WHERE module_pkg_name='" + PKG + "'"))
print("db path now:", row)
if row and row[0][0] != path:
    cur.execute("UPDATE modules SET apk_path=? WHERE module_pkg_name='" + PKG + "'", (path,))
    db.commit()
    print("-> updated")
# checkpoint 把 wal 合进主库再推回
cur.execute("PRAGMA wal_checkpoint(TRUNCATE)")
db.close()

run(["adb", "push", DB, "/sdcard/Download/v8c.db"])
sh("cp /sdcard/Download/v8c.db /data/adb/lspd/config/modules_config.db && chown root:root /data/adb/lspd/config/modules_config.db && chmod 600 /data/adb/lspd/config/modules_config.db && rm -f /data/adb/lspd/config/modules_config.db-wal /data/adb/lspd/config/modules_config.db-shm 2>/dev/null; true")
print("db pushed")

# 5. 软重启 SystemUI（免整机重启, 已验证 daemon 会重新加载）
old = sh("pidof com.android.systemui").strip()
print("killing systemui", old)
sh(f"kill {old}")
time.sleep(10)

