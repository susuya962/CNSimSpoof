package com.util.syspref;

import de.robv.android.xposed.XC_MethodHook;

/** Application.onCreate 后补一次 ensure（早期时机 ctx 可能为 null） */
public class AppHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam p) throws Throwable {
        try {
            Cfg.ensure();
        } catch (Throwable t) { }
    }
}
