package com.util.syspref;

/** 延迟自杀: 不能在广播分发回调里直接 killProcess(AMS 会惩罚, 下次广播不投递) */
public class Killer extends Thread {
    @Override
    public void run() {
        try { Thread.sleep(600); } catch (Throwable t) { }
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
