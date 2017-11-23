package com.burnweb.rnwebview;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

public class ShouldStartLoadWithRequestEvent extends Event<ShouldStartLoadWithRequestEvent> {

    public static final String EVENT_NAME = "shouldStartLoadWithRequestEvent";

    private final String mTitle;
    private final boolean mIsLoading;
    private final String mUrl;
    private final boolean mCanGoBack;
    private final boolean mCanGoForward;
    private  String mInjectInterceptValue;

    public ShouldStartLoadWithRequestEvent(int viewId, long timestampMs, String title, boolean isLoading, String url, boolean canGoBack, boolean canGoForward) {
        super(viewId);

        mTitle = title;
        mIsLoading = isLoading;
        mUrl = url;
        mCanGoBack = canGoBack;
        mCanGoForward = canGoForward;
    }

    public ShouldStartLoadWithRequestEvent(int viewId,long timestampMs, String mTitle, boolean mIsLoading, String mUrl, boolean mCanGoBack, boolean mCanGoForward, String mInjectInterceptValue) {
        super(viewId);
        this.mTitle = mTitle;
        this.mIsLoading = mIsLoading;
        this.mUrl = mUrl;
        this.mCanGoBack = mCanGoBack;
        this.mCanGoForward = mCanGoForward;
        this.mInjectInterceptValue = mInjectInterceptValue;
    }
    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
    }

    private WritableMap serializeEventData() {
        WritableMap eventData = Arguments.createMap();
        eventData.putString("title", mTitle);
        eventData.putBoolean("loading", mIsLoading);
        eventData.putString("url", mUrl);
        eventData.putBoolean("canGoBack", mCanGoBack);
        eventData.putBoolean("canGoForward", mCanGoForward);
        eventData.putString("injectInterceptValue", mInjectInterceptValue);
        return eventData;
    }

}
