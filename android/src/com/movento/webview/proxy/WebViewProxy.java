/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package com.movento.webview.proxy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.TiLifecycle.interceptOnBackPressedEvent;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.WebView;

import com.movento.webview.MoventoWebviewAndroidModule;
import com.movento.webview.impl.TiUIWebView;


@Kroll.proxy(creatableInModule=MoventoWebviewAndroidModule.class, propertyAccessors = {
	TiC.PROPERTY_BLACKLISTED_URLS,
	TiC.PROPERTY_DATA,
	TiC.PROPERTY_ON_CREATE_WINDOW,
	TiC.PROPERTY_SCALES_PAGE_TO_FIT,
	TiC.PROPERTY_URL,
	TiC.PROPERTY_WEBVIEW_IGNORE_SSL_ERROR,
	TiC.PROPERTY_OVER_SCROLL_MODE,
	TiC.PROPERTY_CACHE_MODE,
	TiC.PROPERTY_LIGHT_TOUCH_ENABLED
})
public class WebViewProxy extends TiViewProxy 
	implements Handler.Callback, OnLifecycleEvent, interceptOnBackPressedEvent
{
	private static final String TAG = "WebViewProxy";
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;

	private static final int MSG_GO_BACK = MSG_FIRST_ID + 101;
	private static final int MSG_GO_FORWARD = MSG_FIRST_ID + 102;
	private static final int MSG_RELOAD = MSG_FIRST_ID + 103;
	private static final int MSG_STOP_LOADING = MSG_FIRST_ID + 104;
	private static final int MSG_SET_HTML = MSG_FIRST_ID + 105;
	private static final int MSG_SET_USER_AGENT = MSG_FIRST_ID + 106;
	private static final int MSG_GET_USER_AGENT = MSG_FIRST_ID + 107;
	private static final int MSG_CAN_GO_BACK = MSG_FIRST_ID + 108;
	private static final int MSG_CAN_GO_FORWARD = MSG_FIRST_ID + 109;
	private static final int MSG_RELEASE = MSG_FIRST_ID + 110;
	private static final int MSG_PAUSE = MSG_FIRST_ID + 111;
	private static final int MSG_RESUME = MSG_FIRST_ID + 112;
	
	private static final int MSG_SET_HEADER = MSG_FIRST_ID + 123;
	private static final int MSG_GET_SCROLL_HEIGHT = MSG_FIRST_ID + 124;

	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;
	private static String fusername;
	private static String fpassword;

	private Message postCreateMessage;
	
	public static final String OPTIONS_IN_SETHTML = "optionsInSetHtml";

	public WebViewProxy()
	{
		super();
		defaultValues.put(TiC.PROPERTY_OVER_SCROLL_MODE, 0);
		defaultValues.put(TiC.PROPERTY_LIGHT_TOUCH_ENABLED, true);
		defaultValues.put(TiC.PROPERTY_ENABLE_JAVASCRIPT_INTERFACE, true);
	}

	public WebViewProxy(TiContext context)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		((TiBaseActivity)activity).addOnLifecycleEventListener(this);
		((TiBaseActivity)activity).addInterceptOnBackPressedEventListener(this);
		TiUIWebView webView = new TiUIWebView(this);

		if (postCreateMessage != null) {
			sendPostCreateMessage(webView.getWebView(), postCreateMessage);
			postCreateMessage = null;
		}

		return webView;
	}

	public TiUIWebView getWebView()
	{
		return (TiUIWebView) getOrCreateView();
	}
	
	@Kroll.method
	public void setRequestHeaders(HashMap d)
	{
		TiUIWebView currWebView = getWebView();
	 		if (currWebView != null) {
	 			if (TiApplication.isUIThread()) {
	 				currWebView.setRequestHeaders(d);
	 			} else {
	 				//
	 				Message message = getMainHandler().obtainMessage(MSG_SET_HEADER);
	 				message.obj = d;
	 				message.sendToTarget();
	 			}
	 		}
	 	}
	    
	@Kroll.getProperty @Kroll.method
	public String getScrollHeight()
	{
		if (peekView() != null) {
			if (TiApplication.isUIThread()) {
				return getWebView().getScrollHeight();
			} else {
				return (String) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GET_SCROLL_HEIGHT));
			}
		}
		return "0";
	}

	@Kroll.method
	public Object evalJS(String code)
	{
		// If the view doesn't even exist yet,
		// or if it once did exist but doesn't anymore
		// (like if the proxy was removed from a parent),
		// we absolutely should not try to get a JS value
		// from it.
		TiUIWebView view = (TiUIWebView) peekView();
		if (view == null) {
			Log.w(TAG, "WebView not available, returning null for evalJS result.");
			return null;
		}
		return view.getJSValue(code);
	}

	@Kroll.method @Kroll.getProperty
	public String getHtml()
	{
		if (!hasProperty(TiC.PROPERTY_HTML)) {
			return getWebView().getJSValue("document.documentElement.outerHTML");
		}
		return (String) getProperty(TiC.PROPERTY_HTML);
	}

	@Kroll.method
	public void setHtml(String html, @Kroll.argument(optional = true) KrollDict d)
	{
		setProperty(TiC.PROPERTY_HTML, html);
		setProperty(OPTIONS_IN_SETHTML, d);

		// If the web view has not been created yet, don't set html here. It will be set in processProperties() when the
		// view is created.
		TiUIView v = peekView();
		if (v != null) {
			if (TiApplication.isUIThread()) {
				((TiUIWebView) v).setHtml(html, d);
			} else {
				getMainHandler().sendEmptyMessage(MSG_SET_HTML);
			}
		}
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		if (peekView() != null) {
			switch (msg.what) {
				case MSG_GO_BACK:
					getWebView().goBack();
					return true;
				case MSG_GO_FORWARD:
					getWebView().goForward();
					return true;
				case MSG_RELOAD:
					getWebView().reload();
					return true;
				case MSG_STOP_LOADING:
					getWebView().stopLoading();
					return true;
				case MSG_SET_HEADER:
					 getWebView().setRequestHeaders((HashMap)msg.obj);
					 return true;
				case MSG_GET_SCROLL_HEIGHT: {
					 AsyncResult result = (AsyncResult) msg.obj;
					 result.setResult(getWebView().getScrollHeight());
					 return true;
				}
				case MSG_SET_USER_AGENT:
					getWebView().setUserAgentString(msg.obj.toString());
					return true;
				case MSG_GET_USER_AGENT: {
					AsyncResult result = (AsyncResult) msg.obj;
					result.setResult(getWebView().getUserAgentString());
					return true;
				}
				case MSG_CAN_GO_BACK: {
					AsyncResult result = (AsyncResult) msg.obj;
					result.setResult(getWebView().canGoBack());
					return true;
				}
				case MSG_CAN_GO_FORWARD: {
					AsyncResult result = (AsyncResult) msg.obj;
					result.setResult(getWebView().canGoForward());
					return true;
				}
				case MSG_RELEASE:
					TiUIWebView webView = (TiUIWebView) peekView();
					if (webView != null) {
						webView.destroyWebViewBinding();
					}
					super.releaseViews();
					return true;
				case MSG_PAUSE:
					getWebView().pauseWebView();
					return true;
				case MSG_RESUME:
					getWebView().resumeWebView();
					return true;
				case MSG_SET_HTML:
					String html = TiConvert.toString(getProperty(TiC.PROPERTY_HTML));
					HashMap<String, Object> d = (HashMap<String, Object>) getProperty(OPTIONS_IN_SETHTML);
					getWebView().setHtml(html, d);
					return true;
			}
		}
		return super.handleMessage(msg);
	}

	@Kroll.method
	public void setBasicAuthentication(String username, String password)
	{
		if (peekView() == null) {
			// if the view is null, we cache the username/password
			fusername = username;
			fpassword = password;
			return;
		}
		clearBasicAuthentication();
		getWebView().setBasicAuthentication(username, password);

	}

	@Kroll.method @Kroll.setProperty
	public void setUserAgent(String userAgent)
	{
		TiUIWebView currWebView = getWebView();
		if (currWebView != null) {
			if (TiApplication.isUIThread()) {
				currWebView.setUserAgentString(userAgent);
			} else {
				Message message = getMainHandler().obtainMessage(MSG_SET_USER_AGENT);
				message.obj = userAgent;
				message.sendToTarget();
			}
		}
	}

	@Kroll.method @Kroll.getProperty
	public String getUserAgent()
	{
		TiUIWebView currWebView = getWebView();
		if (currWebView != null) {
			if (TiApplication.isUIThread()) {
				return currWebView.getUserAgentString();
			} else {
				return (String) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GET_USER_AGENT));
			}
		}
		return "";
	}

	@Kroll.method
	public boolean canGoBack()
	{
		if (peekView() != null) {
			if (TiApplication.isUIThread()) {
				return getWebView().canGoBack();
			} else {
				return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_CAN_GO_BACK));
			}
		}
		return false;
	}

	@Kroll.method
	public boolean canGoForward()
	{
		if (peekView() != null) {
			if (TiApplication.isUIThread()) {
				return getWebView().canGoForward();
			} else {
				return (Boolean) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_CAN_GO_FORWARD));
			}
		}
		return false;
	}

	@Kroll.method
	public void goBack()
	{
		getMainHandler().sendEmptyMessage(MSG_GO_BACK);
	}

	@Kroll.method
	public void goForward()
	{
		getMainHandler().sendEmptyMessage(MSG_GO_FORWARD);
	}

	@Kroll.method
	public void reload()
	{
		getMainHandler().sendEmptyMessage(MSG_RELOAD);
	}

	@Kroll.method
	public void stopLoading()
	{
		getMainHandler().sendEmptyMessage(MSG_STOP_LOADING);
	}

	@Kroll.method @Kroll.getProperty
	public int getPluginState()
	{
		int pluginState = TiUIWebView.PLUGIN_STATE_OFF;

		if (hasProperty(TiC.PROPERTY_PLUGIN_STATE)) {
			pluginState = TiConvert.toInt(getProperty(TiC.PROPERTY_PLUGIN_STATE));
		}

		return pluginState;
	}

	@Kroll.method @Kroll.setProperty
	public void setPluginState(int pluginState)
	{
		switch (pluginState) {
			case TiUIWebView.PLUGIN_STATE_OFF:
			case TiUIWebView.PLUGIN_STATE_ON:
			case TiUIWebView.PLUGIN_STATE_ON_DEMAND:
				setPropertyAndFire(TiC.PROPERTY_PLUGIN_STATE, pluginState);
				break;
			default:
				setPropertyAndFire(TiC.PROPERTY_PLUGIN_STATE, TiUIWebView.PLUGIN_STATE_OFF);
		}
	}

	@Kroll.method
	public void pause() 
	{
		if (peekView() != null) {
			if (TiApplication.isUIThread()) {
				getWebView().pauseWebView();
			} else {
				getMainHandler().sendEmptyMessage(MSG_PAUSE);
			}
		}
	}

	@Kroll.method
	public void resume()
	{
		if (peekView() != null) {
			if (TiApplication.isUIThread()) {
				getWebView().resumeWebView();
			} else {
				getMainHandler().sendEmptyMessage(MSG_RESUME);
			}
		}
	}

	@Kroll.method(runOnUiThread=true) @Kroll.setProperty(runOnUiThread=true)
	public void setEnableZoomControls(boolean enabled)
	{
		setPropertyAndFire(TiC.PROPERTY_ENABLE_ZOOM_CONTROLS, enabled);
	}

	@Kroll.method @Kroll.getProperty
	public boolean getEnableZoomControls()
	{
		boolean enabled = true;

		if (hasProperty(TiC.PROPERTY_ENABLE_ZOOM_CONTROLS)) {
			enabled = TiConvert.toBoolean(getProperty(TiC.PROPERTY_ENABLE_ZOOM_CONTROLS));
		}
		return enabled;
	}

	public void clearBasicAuthentication()
	{
		fusername = null;
		fpassword = null;
	}
	
	public String getBasicAuthenticationUserName()
	{
		return fusername;
	}

	public String getBasicAuthenticationPassword()
	{
		return fpassword;
	}

	public void setPostCreateMessage(Message postCreateMessage)
	{
		if (view != null) {
			sendPostCreateMessage(getWebView().getWebView(), postCreateMessage);
		} else {
			this.postCreateMessage = postCreateMessage;
		}
	}

	private static void sendPostCreateMessage(WebView view, Message postCreateMessage)
	{
		WebView.WebViewTransport transport = (WebView.WebViewTransport) postCreateMessage.obj;
		if (transport != null) {
			transport.setWebView(view);
		}
		postCreateMessage.sendToTarget();
	}

	/**
	 * Don't release the web view when it's removed. TIMOB-7808
	 */
	@Override
	public void releaseViews()
	{
	}

	@Kroll.method
	public void release()
	{
		if (TiApplication.isUIThread()) {
			super.releaseViews();
		} else {
			getMainHandler().sendEmptyMessage(MSG_RELEASE);
		}
	}

	@Override
	public boolean interceptOnBackPressed()
	{
		TiUIWebView view = (TiUIWebView) peekView();
		if (view == null) {
			return false;
		}
		return view.interceptOnBackPressed();
	}

	@Override
	public void onStart(Activity activity) {
	}

	@Override
	public void onResume(Activity activity) {
		resume();
	}

	@Override
	public void onPause(Activity activity) {
		pause();
	}

	@Override
	public void onStop(Activity activity) {
	}

	@Override
	public void onDestroy(Activity activity) {
		TiUIWebView webView = (TiUIWebView) peekView();
		if (webView == null) {
			return;
		}

		// We allow JS polling to continue until we exit the app. If we want to stop the polling when the app is
		// backgrounded, we would need to move this to onStop(), and add the appropriate logic in onResume() to restart
		// the polling.
		webView.destroyWebViewBinding();

		WebView nativeWebView = webView.getWebView();
		if (nativeWebView == null) {
			return;
		}

		nativeWebView.stopLoading();
		super.releaseViews();
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.WebView";
	}
}
