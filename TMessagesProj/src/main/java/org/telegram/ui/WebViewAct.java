package org.telegram.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public class WebViewAct extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
//        webView.loadUrl("http://www.google.com");
        webView.loadUrl("http://mygramapp.ir/publicview/");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("WEB_VIEW_TEST", "error code:" + errorCode);
                if (errorCode == -2) {
                    // show Alert here for Page Not found
//                    view.loadUrl("file:///android_asset/Page_Not_found.html");
                    SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, 0);
                    String customHtml = "<html lang=\"en\"><head>\n" +
                            "<meta charset=\"UTF-8\">\n" +
                            "  <style>\n" +
                            "    body {\n" +
                            "        background: "+
                            String.format("#%06X", (0xFFFFFF & themePrefs.getInt("themeColor", AndroidUtilities.defColor)))
                            +" }\n" +
                            "    section {\n" +
                            ";\n" +
                            "        color: "+
                            String.format("#%06X", (0xFFFFFF & themePrefs.getInt("prefHeaderIconsColor", 0xffffffff)))
                            +";\n" +
                            "        border-radius: 1em;\n" +
                            "        padding-top: .5em;\n" +
                            "        padding-bottom: .5em;\n" +
                            "        padding-right: 1em;\n" +
                            "        padding-left: 1em;\n" +
                            "        position: absolute;\n" +
                            "        top: 45%;\n" +
                            "        left: 22%;\n" +
                            "        margin-right: -50%;\n" +
                            "        transform: translate(-50%, -50%) }\n" +
                            "  </style>\n" +
                            "\n" +
                            " </head><body>\n" +
                            "  <section>\n" +
                            "   <h3>" +
                            LocaleController.getString("error_connection", R.string.error_connection)+
                            "</h3>\n" +
                            "</section>\n" +
                            "</body></html>";

                    view.loadData(customHtml, "text/html; charset=utf-8", "UTF-8");
                }

                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("tg:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setComponent(new ComponentName("org.mygram", "org.telegram.ui.LaunchActivity"));
//                    startActivity(intent);

                    view.getContext().startActivity(intent);

//                    view.getContext().startActivity(
//                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

//    private class Client extends WebViewClient{
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view,String url){
//
//        }
//    }

}
