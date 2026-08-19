package com.util.syspref;

/**
 * SystemUI 进程内配置版本轮询: sp_ver 变化 -> 自杀重启(重载配置)。
 * 广播在 AMS 对"广播中死过"的进程有投递惩罚, 不能依赖; 此线程保底, 2s 内必生效。
 */
public class Poll extends Thread {
    private final android.content.Context ctx;

    Poll(android.content.Context c) {
        this.ctx = c;
        setDaemon(true);
    }

    @Override
    public void run() {
        android.content.ContentResolver cr;
        try {
            cr = ctx.getContentResolver();
        } catch (Throwable t) {
            return;
        }
        long lastV = getv(cr);
        long lastR = getr(cr);
        while (true) {
            try { Thread.sleep(2000); } catch (Throwable t) { return; }
            long v = getv(cr);
            long r = getr(cr);
            if (v != lastV || r != lastR) {
                new Killer().start();
                return;
            }
        }
    }

    private static long getv(android.content.ContentResolver cr) {
        try {
            return android.provider.Settings.Global.getLong(cr, "sp_ver", 0L);
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static long getr(android.content.ContentResolver cr) {
        try {
            return android.provider.Settings.Global.getLong(cr, "sp_real", 0L);
        } catch (Throwable t) {
            return 0L;
        }
    }
}
