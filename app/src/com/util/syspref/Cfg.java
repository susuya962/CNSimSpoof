package com.util.syspref;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.ActivityThread;
import android.graphics.Bitmap;
import android.provider.Settings;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * 配置中心。全部 volatile static，hook 热路径直接读。
 * 配置存储：Settings.Global（sp_ 前缀），宿主进程通过广播热重载。
 * 真卡探测：SIM_STATE_CHANGED 广播（真实硬件状态，不经我们的 hook）。
 */
public final class Cfg {
    public static volatile boolean inited = false;
    public static volatile boolean on = false;
    public static volatile boolean dual = false;
    public static volatile boolean data = true;
    public static volatile boolean realCard = false;
    public static volatile int level = 4;
    public static volatile int asu = 97;
    public static volatile int dbm = -51;

    public static volatile String n1 = "\u4e2d\u56fd\u7535\u4fe1";
    public static volatile String n2 = "\u4e2d\u56fd\u8054\u901a";
    public static volatile String p1 = "46003";
    public static volatile String p2 = "46001";
    public static volatile String ic1 = "89860312345678901234";
    public static volatile String ic2 = "89860112345678901234";
    public static volatile String im1 = "460031234567890";
    public static volatile String im2 = "460011234567890";
    public static volatile String t1 = "+8618912345678";
    public static volatile String t2 = "+8618612345678";

    public static volatile ArrayList list = null;
    public static volatile Object info1 = null;
    public static volatile Object info2 = null;

    public static volatile String pkg = "";

    private Cfg() {}

    public static boolean active() {
        return on && !realCard;
    }

    public static String mcc(String plmn) {
        if (plmn == null || plmn.length() < 5) return "460";
        return plmn.substring(0, 3);
    }

    public static String mnc(String plmn) {
        if (plmn == null || plmn.length() < 5) return "03";
        return plmn.substring(3);
    }

    /** 每进程一次：读配置 + 注册广播。失败保持 inited=false 以便重试。 */
    public static void ensure() {
        if (inited) return;
        try {
            Context ctx = (Context) ActivityThread.currentApplication();
            if (ctx == null) return;
            build(ctx);
            IntentFilter f = new IntentFilter();
            f.addAction("com.util.syspref.SYNC");
            f.addAction("android.intent.action.SIM_STATE_CHANGED");
            ctx.registerReceiver(new Rv(), f);
            inited = true;
            if ("com.android.systemui".equals(pkg)) {
                new Poll(ctx).start(); // 轮询保底: 广播投递对死过的进程不可靠
            }
        } catch (Throwable t) {
            inited = true; // 注册失败也别反复尝试（真卡探测退化为手动重启）
        }
    }

    public static void build(Context ctx) {
        try {
            android.content.ContentResolver cr = ctx.getContentResolver();
            on = Settings.Global.getInt(cr, "sp_on", 0) == 1;
            realCard = Settings.Global.getInt(cr, "sp_real", 0) == 1;
            dual = Settings.Global.getInt(cr, "sp_dual", 0) == 1;
            data = Settings.Global.getInt(cr, "sp_data", 1) == 1;
            int lv = Settings.Global.getInt(cr, "sp_lv", 4);
            if (lv < 0) lv = 0;
            if (lv > 4) lv = 4;
            level = lv;
            if (lv == 0) { asu = 3; dbm = -107; }
            else if (lv == 1) { asu = 8; dbm = -97; }
            else if (lv == 2) { asu = 16; dbm = -87; }
            else if (lv == 3) { asu = 35; dbm = -72; }
            else { asu = 97; dbm = -51; }
            String v;
            if ((v = Settings.Global.getString(cr, "sp_n1")) != null) n1 = v;
            if ((v = Settings.Global.getString(cr, "sp_n2")) != null) n2 = v;
            if ((v = Settings.Global.getString(cr, "sp_p1")) != null) p1 = v;
            if ((v = Settings.Global.getString(cr, "sp_p2")) != null) p2 = v;
            if ((v = Settings.Global.getString(cr, "sp_i1")) != null) ic1 = v;
            if ((v = Settings.Global.getString(cr, "sp_i2")) != null) ic2 = v;
            if ((v = Settings.Global.getString(cr, "sp_m1")) != null) im1 = v;
            if ((v = Settings.Global.getString(cr, "sp_m2")) != null) im2 = v;
            if ((v = Settings.Global.getString(cr, "sp_t1")) != null) t1 = v;
            if ((v = Settings.Global.getString(cr, "sp_t2")) != null) t2 = v;
            rebuild();
        } catch (Throwable t) {
        }
    }

    /** 重建伪造 SubscriptionInfo（26 参构造，与 v7 实测参数完全一致） */
    static void rebuild() {
        try {
            Class<?> si = Class.forName("android.telephony.SubscriptionInfo");
            Constructor<?> c = si.getConstructor(
                    int.class, String.class, int.class, CharSequence.class, CharSequence.class,
                    int.class, int.class, String.class, int.class, Bitmap.class,
                    String.class, String.class, String.class, boolean.class,
                    android.telephony.UiccAccessRule[].class, String.class, int.class, boolean.class,
                    String.class, boolean.class, int.class, int.class, int.class,
                    String.class, android.telephony.UiccAccessRule[].class, boolean.class);
            Bitmap bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            info1 = c.newInstance(1, ic1, 0, n1, n1, 1, 0, t1, 0, bmp,
                    mcc(p1), mnc(p1), "cn", false, null, null, -1, false,
                    null, false, 1, -1, 0, null, null, true);
            Bitmap bmp2 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            info2 = c.newInstance(2, ic2, 1, n2, n2, 1, 0, t2, 0, bmp2,
                    mcc(p2), mnc(p2), "cn", false, null, null, -1, false,
                    null, false, 1, -1, 0, null, null, true);
            ArrayList l = new ArrayList();
            l.add(info1);
            if (dual) l.add(info2);
            list = l;
        } catch (Throwable t) {
            list = null;
        }
    }
}
