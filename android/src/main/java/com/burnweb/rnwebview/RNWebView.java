package com.burnweb.rnwebview;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.common.SystemClock;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;

class RNWebView extends WebView implements LifecycleEventListener {

    private final EventDispatcher mEventDispatcher;
    private final RNWebViewManager mViewManager;

    private String charset = "UTF-8";
    private String baseUrl = "file:///";
    private String injectedJavaScript = null;
    private boolean allowUrlRedirect = false;
    private boolean allowInterceptUrl;
    private ReadableArray mInjectFilterInterceptArray;


    protected class EventWebClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(WebView view, String url){

            //************** by zhangjinbo 用于Url拦截*******************//
            if (RNWebView.this.getAllowInterceptUrl()) {
                Log.i("aabbcc", "getAllowInterceptUrl==true");
                //事件拦截
                ReadableNativeArray nativeArray = (ReadableNativeArray) RNWebView.this.getInjectFilterInterceptArray();
                if (nativeArray != null && nativeArray.size() > 0) {
                    if (!TextUtils.isEmpty(url)) {
                        //是否拦截
                        boolean isIntercet = false;
                        String interceptValue = "";
                        for (int i = 0; i < nativeArray.size(); i++) {
                            String value = nativeArray.getString(i);
                            interceptValue = value;
                            Log.i("aabbcc", "interceptValue==="+interceptValue+",url==="+url);

                            if (!TextUtils.isEmpty(value) && url.indexOf(value) > 0) {
                                isIntercet = true;
                                break;
                            }
                        }
                        if (isIntercet) {
                            mEventDispatcher.dispatchEvent(new ShouldStartLoadWithRequestEvent(getId(), SystemClock.nanoTime(), view.getTitle(), false, url, view.canGoBack(), view.canGoForward(),interceptValue));
                            return true;
                        }
                    }
                }
            }
            //****************************************//
            if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:") || url.startsWith("mms:") || url.startsWith("mmsto:")) //fix 电话、邮件等
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
                return true;
            }
            if (url.startsWith("error:")) //fix 物流
            {
                return true;
            }
            //************** by zhangjinbo 判断重定向 2018年04月16日*******************//
            WebView.HitTestResult hitTestResult = view.getHitTestResult();
            if(hitTestResult == null) {
                Log.i("aabbcc", "hitTestResult=="+hitTestResult);
                return false;
            }
            if(hitTestResult.getType() == WebView.HitTestResult.UNKNOWN_TYPE) {
                Log.i("aabbcc", "hitTestResult.getType()=="+hitTestResult.getType());

                return false;
            }
            //************** by zhangjinbo 判断重定向 2018年04月16日*******************//

            if(RNWebView.this.getAllowUrlRedirect()) {
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                mEventDispatcher.dispatchEvent(new ShouldStartLoadWithRequestEvent(getId(), SystemClock.nanoTime(), view.getTitle(), false, url, view.canGoBack(), view.canGoForward()));

                view.loadUrl(url);

                return false; // then it is not handled by default action
            }

            return super.shouldOverrideUrlLoading(view, url);
        }

        public void onPageFinished(WebView view, String url) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), false, url, view.canGoBack(), view.canGoForward()));

            if(RNWebView.this.getInjectedJavaScript() != null) {
                view.loadUrl("javascript:(function() {\n" + RNWebView.this.getInjectedJavaScript() + ";\n})();");
            }
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), true, url, view.canGoBack(), view.canGoForward()));
        }
    }

    protected class CustomWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            getModule().showAlert(url, message, result);
            return true;
        }

        // For Android 4.1+
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            getModule().startFileChooserIntent(uploadMsg, acceptType);
        }

        // For Android 5.0+
        @SuppressLint("NewApi")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            return getModule().startFileChooserIntent(filePathCallback, fileChooserParams.createIntent());
        }
    }

    protected class GeoWebChromeClient extends CustomWebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    public RNWebView(RNWebViewManager viewManager, ThemedReactContext reactContext) {
        super(reactContext);

        mViewManager = viewManager;
        mEventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();

        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setBuiltInZoomControls(false);
        this.getSettings().setDomStorageEnabled(true);
        this.getSettings().setGeolocationEnabled(false);
        this.getSettings().setPluginState(WebSettings.PluginState.ON);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAllowFileAccessFromFileURLs(true);
        this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.getSettings().setLoadsImagesAutomatically(true);
        this.getSettings().setBlockNetworkImage(false);
        this.getSettings().setBlockNetworkLoads(false);
        this.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        this.setWebViewClient(new EventWebClient());
        this.setWebChromeClient(getCustomClient());
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setAllowUrlRedirect(boolean a) {
        this.allowUrlRedirect = a;
    }

    public boolean getAllowUrlRedirect() {
        return this.allowUrlRedirect;
    }

    public void setInjectedJavaScript(String injectedJavaScript) {
        this.injectedJavaScript = injectedJavaScript;
    }
    public ReadableArray getInjectFilterInterceptArray() {
        return mInjectFilterInterceptArray;
    }

    public void setInjectFilterInterceptArray(ReadableArray mInjectFilterInterceptArray) {
        this.mInjectFilterInterceptArray = mInjectFilterInterceptArray;
    }

    public void setAllowInterceptUrl(boolean a) {
        this.allowInterceptUrl = a;
    }

    public boolean getAllowInterceptUrl() {
        return this.allowInterceptUrl;
    }
    public String getInjectedJavaScript() {
        return this.injectedJavaScript;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public CustomWebChromeClient getCustomClient() {
        return new CustomWebChromeClient();
    }

    public GeoWebChromeClient getGeoClient() {
        return new GeoWebChromeClient();
    }

    public RNWebViewModule getModule() {
        return mViewManager.getPackage().getModule();
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        destroy();
    }

    @Override
    public void onDetachedFromWindow() {
        //屏蔽自动清除，解决和react-native-navigation push问题
//        this.loadDataWithBaseURL(this.getBaseUrl(), "<html>112223334444</html>", "text/html", this.getCharset(), null);
        super.onDetachedFromWindow();
    }

}
