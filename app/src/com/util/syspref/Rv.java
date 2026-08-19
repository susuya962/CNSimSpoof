package com.util.syspref;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** SYNC 配置同步 + SIM 真卡探测接收器 */
public class Rv extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent i) {
        try {
            String act = i == null ? null : i.getAction();
            if ("com.util.syspref.SYNC".equals(act)) {
                try { // 诊断桩: 收到即盖章
                    android.provider.Settings.Global.putLong(
                            ctx.getContentResolver(), "sp_seen", System.currentTimeMillis());
                    android.provider.Settings.Global.putString(
                            ctx.getContentResolver(), "sp_pkg", String.valueOf(Cfg.pkg));
                } catch (Throwable t) { }
                Cfg.build(ctx);
                if ("com.android.systemui".equals(Cfg.pkg)) {
                    new Killer().start(); // 延迟自杀, 让广播分发先完成
                }
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(act)) {
                String ss = i.getStringExtra("ss");
                boolean real = ":READY".equals(":" + ss) || "LOADED".equals(ss) || "IMSI".equals(ss);
                Cfg.realCard = real;
                try {
                    android.provider.Settings.Global.putInt(
                            ctx.getContentResolver(), "sp_real", real ? 1 : 0);
                } catch (Throwable t2) { }
                if ("com.android.systemui".equals(Cfg.pkg) && real) {
                    new Killer().start();
                }
            }
        } catch (Throwable t) {
        }
    }
}
