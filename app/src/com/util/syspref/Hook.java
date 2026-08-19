package com.util.syspref;

import de.robv.android.xposed.XC_MethodHook;

/** 万能 hook 回调：kind 驱动 + active() 门控（关/真卡 → 放行真实值） */
public class Hook extends XC_MethodHook {
    private final int kind;

    public Hook(int k) {
        this.kind = k;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
        if (!Cfg.active()) return;
        Object v;
        switch (this.kind) {
            case 0:  v = Integer.valueOf(0); break;          // IN_SERVICE
            case 1:  v = Cfg.p1; break;                      // 运营商码
            case 2:  v = Cfg.n1; break;                      // 运营商名
            case 3:  v = "cn"; break;                        // 国家码
            case 4:  v = Cfg.ic1; break;                     // ICCID
            case 5:  v = Cfg.im1; break;                     // IMSI
            case 6:  v = Cfg.t1; break;                      // 号码
            case 7:  v = Integer.valueOf(5); break;          // SIM READY
            case 8:  v = Integer.valueOf(2); break;          // DATA CONNECTED
            case 9:  v = Integer.valueOf(13); break;         // LTE
            case 10: v = Boolean.FALSE; break;               // 不漫游
            case 11: v = Integer.valueOf(Cfg.level); break;  // 信号格
            case 12: v = Integer.valueOf(Cfg.asu); break;    // ASU
            case 13: v = Integer.valueOf(Cfg.dbm); break;    // dBm
            case 14: v = Cfg.list; break;                    // 伪造订阅列表
            case 15:                                             // 按 subId 选卡
                v = (p.args != null && p.args.length > 0 && Integer.valueOf(2).equals(p.args[0])) ? Cfg.info2 : Cfg.info1;
                if (v == null) v = Cfg.info1;
                break;
            case 16: v = Integer.valueOf(Cfg.dual ? 2 : 1); break;
            case 17: v = Integer.valueOf(1); break;          // 默认订阅 ID
            case 18: v = Integer.valueOf(0); break;          // slot 0
            case 19: v = Boolean.TRUE; break;                // isMobileDataSupported
            case 20: v = Boolean.FALSE; break;
            case 21: v = Integer.valueOf(1); break;          // provisioning=1
            case 22: v = Integer.valueOf(2); break;          // countMax（双卡槽）
            default: v = null; break;
        }
        if (v != null) p.setResult(v);
    }
}
