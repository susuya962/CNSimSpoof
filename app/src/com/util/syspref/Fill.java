package com.util.syspref;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;

/**
 * SystemUI 专用: MobileSignalController.updateMobileStatus(MobileStatus) 前,
 * 把参数里的 null serviceState/signalStrength 替换成壳对象。
 * 壳的 getter 已被 Hook 类拦截 -> 读出即 IN_SERVICE/满格。
 * MobileStatusTracker 构造时 lambda$new$0 必调一次, 之后每次回调也走这里。
 */
public class Fill extends XC_MethodHook {
    private static Object ssShell, sgShell;
    private static boolean tried = false;

    static void shells() {
        if (tried) return;
        tried = true;
        try {
            ssShell = Class.forName("android.telephony.ServiceState").getDeclaredConstructor().newInstance();
        } catch (Throwable t) { }
        try {
            sgShell = Class.forName("android.telephony.SignalStrength").getDeclaredConstructor().newInstance();
        } catch (Throwable t) { }
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
        if (!Cfg.active()) return;
        if (p.args == null || p.args.length == 0 || p.args[0] == null) return;
        shells();
        if (ssShell == null && sgShell == null) return;
        Object st = p.args[0];
        try {
            Field f = st.getClass().getDeclaredField("serviceState");
            f.setAccessible(true);
            if (f.get(st) == null) f.set(st, ssShell);
        } catch (Throwable t) { }
        try {
            Field f2 = st.getClass().getDeclaredField("signalStrength");
            f2.setAccessible(true);
            if (f2.get(st) == null) f2.set(st, sgShell);
        } catch (Throwable t) { }
    }
}
