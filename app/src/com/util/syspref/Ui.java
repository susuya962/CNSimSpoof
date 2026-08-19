package com.util.syspref;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

/** 配置界面：WebView 壳 */
public class Ui extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            WebView wv = new WebView(this);
            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            wv.addJavascriptInterface(new Js(this), "bridge");
            wv.loadUrl("file:///android_asset/ui.html");
            setContentView(wv);
        } catch (Throwable t) {
            finish();
        }
    }
}
