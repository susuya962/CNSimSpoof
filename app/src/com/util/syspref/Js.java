package com.util.syspref;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

/** JS 桥（顶级类，避免 d8 对新式内部类属性的解析问题） */
public class Js {
    private final Activity act;

    public Js(Activity a) {
        this.act = a;
    }

    @JavascriptInterface
    public String getCfg() {
        try {
            ContentResolver cr = act.getContentResolver();
            StringBuffer sb = new StringBuffer("{");
            sb.append("\"on\":").append(gi(cr, "sp_on", 0)).append(",");
            sb.append("\"dual\":").append(gi(cr, "sp_dual", 0)).append(",");
            sb.append("\"data\":").append(gi(cr, "sp_data", 1)).append(",");
            sb.append("\"lv\":").append(gi(cr, "sp_lv", 4)).append(",");
            kv(sb, "n1", gs(cr, "sp_n1"));
            kv(sb, "n2", gs(cr, "sp_n2"));
            kv(sb, "p1", gs(cr, "sp_p1"));
            kv(sb, "p2", gs(cr, "sp_p2"));
            kv(sb, "i1", gs(cr, "sp_i1"));
            kv(sb, "i2", gs(cr, "sp_i2"));
            kv(sb, "m1", gs(cr, "sp_m1"));
            kv(sb, "m2", gs(cr, "sp_m2"));
            kv(sb, "t1", gs(cr, "sp_t1"));
            sb.append("\"t2\":\"").append(esc(gs(cr, "sp_t2"))).append("\"}");
            return sb.toString();
        } catch (Throwable t) {
            return "{}";
        }
    }

    private static int gi(ContentResolver cr, String k, int d) {
        try {
            return Settings.Global.getInt(cr, k, d);
        } catch (Throwable t) {
            return d;
        }
    }

    private static String gs(ContentResolver cr, String k) {
        try {
            return Settings.Global.getString(cr, k);
        } catch (Throwable t) {
            return "";
        }
    }

    private static void kv(StringBuffer sb, String k, String v) {
        sb.append("\"").append(k).append("\":\"").append(esc(v)).append("\",");
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    @JavascriptInterface
    public void set(int on, int dual, int data, int lv,
                    String n1, String n2, String p1, String p2,
                    String i1, String i2, String m1, String m2,
                    String t1, String t2) {
        try {
            ContentResolver cr = act.getContentResolver();
            Settings.Global.putInt(cr, "sp_on", on);
            Settings.Global.putInt(cr, "sp_dual", dual);
            Settings.Global.putInt(cr, "sp_data", data);
            Settings.Global.putInt(cr, "sp_lv", lv);
            put(cr, "sp_n1", n1);
            put(cr, "sp_n2", n2);
            put(cr, "sp_p1", p1);
            put(cr, "sp_p2", p2);
            put(cr, "sp_i1", i1);
            put(cr, "sp_i2", i2);
            put(cr, "sp_m1", m1);
            put(cr, "sp_m2", m2);
            put(cr, "sp_t1", t1);
            put(cr, "sp_t2", t2);
        } catch (Throwable t) { }
    }

    private static void put(ContentResolver cr, String k, String v) {
        try {
            if (v == null || v.length() == 0) return;
            Settings.Global.putString(cr, k, v);
        } catch (Throwable t) { }
    }

    @JavascriptInterface
    public String apply() {
        try {
            ContentResolver cr = act.getContentResolver();
            // 版本号+1 -> SystemUI 内 Poll 线程 2s 内发现并自杀重载(广播会被 system_server 抢收, 不可靠)
            try {
                long v = Settings.Global.getLong(cr, "sp_ver", 0L);
                Settings.Global.putLong(cr, "sp_ver", v + 1);
            } catch (Throwable t) { }
            Intent it = new Intent("com.util.syspref.SYNC"); // 广播当加速器
            it.setPackage("com.android.systemui");
            act.sendBroadcast(it);
            return "ok";
        } catch (Throwable t) {
            return "err";
        }
    }
}
