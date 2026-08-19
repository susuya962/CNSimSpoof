package com.util.syspref;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Core implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        try {
            Cfg.pkg = lp.packageName;
            Cfg.ensure();
        } catch (Throwable t) { }

        try {
            XposedHelpers.findAndHookMethod(Class.forName("android.app.Application"),
                    "onCreate", new AppHook());
        } catch (Throwable t) { }

        Class<?> tm;
        try {
            tm = Class.forName("android.telephony.TelephonyManager");
        } catch (Throwable t) {
            return;
        }

        try {
            hook(tm, "getSimOperator", 1);
            hook(tm, "getNetworkOperator", 1);
            hook(tm, "getSimOperatorName", 2);
            hook(tm, "getNetworkOperatorName", 2);
            hook(tm, "getSimCountryIso", 3);
            hook(tm, "getNetworkCountryIso", 3);
            hook(tm, "getSimSerialNumber", 4);
            hook(tm, "getSubscriberId", 5);
            hook(tm, "getLine1Number", 6);
            hook(tm, "getSimState", 7);
            hook(tm, "getDataState", 8);
            hook(tm, "getDataNetworkType", 9);
            hook(tm, "getNetworkType", 9);
            hook(tm, "isNetworkRoaming", 10);
            hookI(tm, "getSimOperator", 1);
            hookI(tm, "getNetworkOperator", 1);
            hookI(tm, "getSimOperatorName", 2);
            hookI(tm, "getNetworkOperatorName", 2);
            hookI(tm, "getSimCountryIso", 3);
            hookI(tm, "getNetworkCountryIso", 3);
            hookI(tm, "getSimSerialNumber", 4);
            hookI(tm, "getSubscriberId", 5);
            hookI(tm, "getLine1Number", 6);
            hookI(tm, "getSimState", 7);
        } catch (Throwable t) { }

        try {
            Class<?> ss = Class.forName("android.telephony.ServiceState");
            hook(ss, "getState", 0);
            hook(ss, "getVoiceRegState", 0);
            hook(ss, "getDataRegState", 0);
            hook(ss, "getVoiceRegistrationState", 0);
            hook(ss, "getDataRegistrationState", 0);
            hook(ss, "getRoaming", 10);
            hook(ss, "isEmergencyOnly", 20);
            hook(ss, "getOperatorAlphaLong", 2);
            hook(ss, "getOperatorAlphaShort", 2);
        } catch (Throwable t) { }

        try {
            Class<?> sg = Class.forName("android.telephony.SignalStrength");
            hook(sg, "getLevel", 11);
            hook(sg, "getAsuLevel", 12);
            hook(sg, "getDbm", 13);
        } catch (Throwable t) { }

        if (!"com.android.systemui".equals(lp.packageName)) return;

        try {
            Class<?> sm = Class.forName("android.telephony.SubscriptionManager");
            hook(sm, "getActiveSubscriptionInfoList", 14);
            hook(sm, "getCompleteActiveSubscriptionInfoList", 14);
            hookI(sm, "getActiveSubscriptionInfo", 15);
            hookI(sm, "getSlotIndex", 18);
            hook(sm, "getDefaultDataSubscriptionId", 17);
            hook(sm, "getDefaultVoiceSubscriptionId", 17);
            hook(sm, "getDefaultSmsSubscriptionId", 17);
            hook(sm, "getActiveSubscriptionInfoCount", 16);
            hook(sm, "getActiveSubscriptionInfoCountMax", 22);
        } catch (Throwable t) { }

        try {
            Class<?> k = Class.forName("com.android.keyguard.KeyguardUpdateMonitor", true, lp.classLoader);
            hookI(k, "getSimState", 7);
        } catch (Throwable t) { }

        try {
            Class<?> du = Class.forName("com.android.settingslib.net.DataUsageController", true, lp.classLoader);
            hook(du, "isMobileDataSupported", 19);
            hookI(du, "isMobileDataSupported", 19);
        } catch (Throwable t) { }

        try {
            Class<?> me = Class.forName("com.motorola.android.telephony.MotoExtTelephonyManager", true, lp.classLoader);
            hookI(me, "getCurrentUiccCardProvisioningStatus", 21);
        } catch (Throwable t) { }

        try {
            Class<?> sst = Class.forName("com.android.systemui.moto.SimStates", true, lp.classLoader);
            hookI(sst, "getState", 7);
            hookI(sst, "isSimAbsent", 20);
            hookI(sst, "isSimLocked", 20);
        } catch (Throwable t) { }

        // 信号格核心: updateMobileStatus 前把参数里 null 状态填成壳(getter 已 hook -> 满格)
        try {
            Class<?> msc = Class.forName("com.android.systemui.statusbar.policy.MobileSignalController", true, lp.classLoader);
            Class<?> mst = Class.forName("com.android.settingslib.mobile.MobileStatusTracker$MobileStatus", true, lp.classLoader);
            XposedHelpers.findAndHookMethod(msc, "updateMobileStatus", mst, new Fill());
        } catch (Throwable t) { }
    }

    static void hook(Class<?> c, String name, int kind) {
        try {
            XposedHelpers.findAndHookMethod(c, name, new Hook(kind));
        } catch (Throwable t) { }
    }

    static void hookI(Class<?> c, String name, int kind) {
        try {
            XposedHelpers.findAndHookMethod(c, name, Integer.TYPE, new Hook(kind));
        } catch (Throwable t) { }
    }
}
