/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package com.movento.webview.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiMimeTypeHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiBackgroundDrawable;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.WebViewProxy;
import ti.modules.titanium.ui.android.AndroidModule;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

@SuppressWarnings("deprecation")
public class TiUIWebView extends TiUIView
{

	private static final String TAG = "TiUIWebView";
	private TiWebViewClient client;
	private TiWebChromeClient chromeClient;
	private boolean bindingCodeInjected = false;
	private boolean isLocalHTML = false;
	private HashMap<String, String> extraHeaders = new HashMap<String, String>();

	private static Enum<?> enumPluginStateOff;
	private static Enum<?> enumPluginStateOn;
	private static Enum<?> enumPluginStateOnDemand;
	private static Method internalSetPluginState;
	private static Method internalWebViewPause;
	private static Method internalWebViewResume;

	public static final int PLUGIN_STATE_OFF = 0;
	public static final int PLUGIN_STATE_ON = 1;
	public static final int PLUGIN_STATE_ON_DEMAND = 2;

	private static enum reloadTypes {
		DEFAULT, DATA, HTML, URL
	}
	
	private reloadTypes reloadMethod = reloadTypes.DEFAULT;
	private Object reloadData = null;
	
	private class TiWebView extends WebView
	{
		public TiWebViewClient client;

		public TiWebView(Context context)
		{
			super(context);
		}

		@Override
		public void destroy()
		{
			if (client != null) {
				client.getBinding().destroy();
			}
			super.destroy();
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev)
		{
			
			boolean handled = false;

			// In Android WebView, all the click events are directly sent to WebKit. As a result, OnClickListener() is
			// never called. Therefore, we have to manually call performClick() when a click event is detected.
			//
			// In native Android and in the Ti world, it's possible to to have a touchEvent click on a link in a webview and
			// also to be detected as a click on the webview.  So we cannot let handling of the event one way block
			// the handling the other way -- it must be passed to both in all cases for everything to work correctly.
			//
			if (ev.getAction() == MotionEvent.ACTION_UP) {
				Rect r = new Rect(0, 0, getWidth(), getHeight());
				if (r.contains((int) ev.getX(), (int) ev.getY())) {
					handled = proxy.fireEvent(TiC.EVENT_CLICK, dictFromEvent(ev));
				}
			}

			boolean swipeHandled = false;

			// detect will be null when touch is disabled
			if (detector != null) {
				swipeHandled = detector.onTouchEvent(ev);
			}

			// Don't return here -- must call super.onTouchEvent()
			
			boolean superHandled = super.onTouchEvent(ev);
			
			return (superHandled || handled || swipeHandled);
		}

		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom)
		{
			super.onLayout(changed, left, top, right, bottom);
			TiUIHelper.firePostLayoutEvent(proxy);
		}
	}
	
	//TIMOB-16952. Overriding onCheckIsTextEditor crashes HTC Sense devices
	private class NonHTCWebView extends TiWebView
	{
		public NonHTCWebView(Context context)
		{
			super(context);
		}
		
		@Override
		public boolean onCheckIsTextEditor()
		{
			if (proxy.hasProperty(TiC.PROPERTY_SOFT_KEYBOARD_ON_FOCUS)) {
				int value = TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_SOFT_KEYBOARD_ON_FOCUS), TiUIView.SOFT_KEYBOARD_DEFAULT_ON_FOCUS);
				
				if (value == TiUIView.SOFT_KEYBOARD_HIDE_ON_FOCUS) {
					return false;
				} else if (value == TiUIView.SOFT_KEYBOARD_SHOW_ON_FOCUS) {
					return true;
				}
			}
			return super.onCheckIsTextEditor();
		}
	}
	
	private boolean isHTCSenseDevice()
	{
		boolean isHTC = false;
		
		FeatureInfo[] features = TiApplication.getInstance().getApplicationContext().getPackageManager().getSystemAvailableFeatures();
		if(features == null) { 
			return isHTC;
		}
		for (FeatureInfo f : features) {
			String fName = f.name;
			if (fName != null) {
				isHTC = fName.contains("com.htc.software.Sense");
				if (isHTC) {
					Log.i(TAG, "Detected com.htc.software.Sense feature "+fName);
					break;
				}
			}
		}
		
		return isHTC;
	}
	

	public TiUIWebView(TiViewProxy proxy)
	{
		super(proxy);
        
		// We can only support debugging in API 19 and higher
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			// Only enable webview debugging, when app is debuggable
			if (0 != (proxy.getActivity().getApplicationContext().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
				WebView.setWebContentsDebuggingEnabled(true);
			}
		}
		
		TiWebView webView = isHTCSenseDevice() ? new TiWebView(proxy.getActivity()) : new NonHTCWebView(proxy.getActivity());
		webView.setVerticalScrollbarOverlay(true);

		WebSettings settings = webView.getSettings();
		settings.setUseWideViewPort(true);
		settings.setJavaScriptEnabled(true);
		settings.setSupportMultipleWindows(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		settings.setLoadsImagesAutomatically(true);
		settings.setDomStorageEnabled(true); // Required by some sites such as Twitter. This is in our iOS WebView too.
		File path = TiApplication.getInstance().getFilesDir();
		if (path != null) {
			settings.setDatabasePath(path.getAbsolutePath());
			settings.setDatabaseEnabled(true);
		}
		
		File cacheDir = TiApplication.getInstance().getCacheDir();
		if (cacheDir != null) {
			settings.setAppCacheEnabled(true);
			settings.setAppCachePath(cacheDir.getAbsolutePath());
		}

		// enable zoom controls by default
		boolean enableZoom = true;

		if (proxy.hasProperty(TiC.PROPERTY_ENABLE_ZOOM_CONTROLS)) {
			enableZoom = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_ENABLE_ZOOM_CONTROLS));
		}

		settings.setBuiltInZoomControls(enableZoom);
		settings.setSupportZoom(enableZoom);

		if (Build.VERSION.SDK_INT >= TiC.API_LEVEL_JELLY_BEAN) {
			settings.setAllowUniversalAccessFromFileURLs(true); // default is "false" for JellyBean, TIMOB-13065
		}

		// We can only support webview settings for plugin/flash in API 8 and higher.
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
			initializePluginAPI(webView);
		}

		boolean enableJavascriptInterface = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_ENABLE_JAVASCRIPT_INTERFACE), true);
		chromeClient = new TiWebChromeClient(this);
		webView.setWebChromeClient(chromeClient);
		client = new TiWebViewClient(this, webView);
		webView.setWebViewClient(client);
		if (Build.VERSION.SDK_INT > 16 || enableJavascriptInterface) {
			client.getBinding().addJavascriptInterfaces();
		}
		//setLayerType() is supported in API 11+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
		webView.client = client;

		if (proxy instanceof WebViewProxy) {
			WebViewProxy webProxy = (WebViewProxy) proxy;
			String username = webProxy.getBasicAuthenticationUserName();
			String password = webProxy.getBasicAuthenticationPassword();
			if (username != null && password != null) {
				setBasicAuthentication(username, password);
			}
			webProxy.clearBasicAuthentication();
		}

		TiCompositeLayout.LayoutParams params = getLayoutParams();
		params.autoFillsHeight = true;
		params.autoFillsWidth = true;

		setNativeView(webView);
	}

	public WebView getWebView()
	{
		return (WebView) getNativeView();
	}

	private void initializePluginAPI(TiWebView webView)
	{
		try {
			synchronized (this.getClass()) {
				// Initialize
				if (enumPluginStateOff == null) {
					Class<?> webSettings = Class.forName("android.webkit.WebSettings");
					Class<?> pluginState = Class.forName("android.webkit.WebSettings$PluginState");

					Field f = pluginState.getDeclaredField("OFF");
					enumPluginStateOff = (Enum<?>) f.get(null);
					f = pluginState.getDeclaredField("ON");
					enumPluginStateOn = (Enum<?>) f.get(null);
					f = pluginState.getDeclaredField("ON_DEMAND");
					enumPluginStateOnDemand = (Enum<?>) f.get(null);
					internalSetPluginState = webSettings.getMethod("setPluginState", pluginState);
					// Hidden APIs
					// http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/webkit/WebView.java;h=bbd8b95c7bea66b7060b5782fae4b3b2c4f04966;hb=4db1f432b853152075923499768639e14403b73a#l2558
					internalWebViewPause = webView.getClass().getMethod("onPause");
					internalWebViewResume = webView.getClass().getMethod("onResume");
				}
			}
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "ClassNotFound: " + e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			Log.e(TAG, "NoSuchMethod: " + e.getMessage(), e);
		} catch (NoSuchFieldException e) {
			Log.e(TAG, "NoSuchField: " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, "IllegalAccess: " + e.getMessage(), e);
		}
	}

	@Override
	public void processProperties(KrollDict d)
	{
		super.processProperties(d);

		if (d.containsKey(TiC.PROPERTY_SCALES_PAGE_TO_FIT)) {
			WebSettings settings = getWebView().getSettings();
			settings.setLoadWithOverviewMode(TiConvert.toBoolean(d, TiC.PROPERTY_SCALES_PAGE_TO_FIT));
		}
		
		if (d.containsKey(TiC.PROPERTY_CACHE_MODE)) {
			int mode = TiConvert.toInt(d.get(TiC.PROPERTY_CACHE_MODE), AndroidModule.WEBVIEW_LOAD_DEFAULT);
			getWebView().getSettings().setCacheMode(mode);
		}

		if (d.containsKey(TiC.PROPERTY_URL) && !TiC.URL_ANDROID_ASSET_RESOURCES.equals(TiConvert.toString(d, TiC.PROPERTY_URL))) {
			setUrl(TiConvert.toString(d, TiC.PROPERTY_URL));
		} else if (d.containsKey(TiC.PROPERTY_HTML)) {
			setHtml(TiConvert.toString(d, TiC.PROPERTY_HTML), (HashMap<String, Object>) (d.get(WebViewProxy.OPTIONS_IN_SETHTML)));
		} else if (d.containsKey(TiC.PROPERTY_DATA)) {
			Object value = d.get(TiC.PROPERTY_DATA);
			if (value instanceof TiBlob) {
				setData((TiBlob) value);
			}
		}
		
		if (d.containsKey(TiC.PROPERTY_LIGHT_TOUCH_ENABLED)) {
			WebSettings settings = getWebView().getSettings();
			settings.setLightTouchEnabled(TiConvert.toBoolean(d,TiC.PROPERTY_LIGHT_TOUCH_ENABLED));
		}

		// If TiUIView's processProperties ended up making a TiBackgroundDrawable
		// for the background, we must set the WebView background color to transparent
		// in order to see any of it.
		if (nativeView != null && nativeView.getBackground() instanceof TiBackgroundDrawable) {
			nativeView.setBackgroundColor(Color.TRANSPARENT);
		}

		if (d.containsKey(TiC.PROPERTY_PLUGIN_STATE)) {
			setPluginState(TiConvert.toInt(d, TiC.PROPERTY_PLUGIN_STATE));
		}
		
		if (d.containsKey(TiC.PROPERTY_OVER_SCROLL_MODE)) {
			if (Build.VERSION.SDK_INT >= 9) {
				nativeView.setOverScrollMode(TiConvert.toInt(d.get(TiC.PROPERTY_OVER_SCROLL_MODE), View.OVER_SCROLL_ALWAYS));
			}
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (TiC.PROPERTY_URL.equals(key)) {
			setUrl(TiConvert.toString(newValue));
		} else if (TiC.PROPERTY_HTML.equals(key)) {
			setHtml(TiConvert.toString(newValue));
		} else if (TiC.PROPERTY_DATA.equals(key)) {
			if (newValue instanceof TiBlob) {
				setData((TiBlob) newValue);
			}
		} else if (TiC.PROPERTY_SCALES_PAGE_TO_FIT.equals(key)) {
			WebSettings settings = getWebView().getSettings();
			settings.setLoadWithOverviewMode(TiConvert.toBoolean(newValue));
		} else if (TiC.PROPERTY_OVER_SCROLL_MODE.equals(key)) {
			if (Build.VERSION.SDK_INT >= 9) {
				nativeView.setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
			}
		} else if (TiC.PROPERTY_CACHE_MODE.equals(key)) { 
			getWebView().getSettings().setCacheMode(TiConvert.toInt(newValue));
		} else if (TiC.PROPERTY_LIGHT_TOUCH_ENABLED.equals(key)) {
			WebSettings settings = getWebView().getSettings();
			settings.setLightTouchEnabled(TiConvert.toBoolean(newValue));
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}

		// If TiUIView's propertyChanged ended up making a TiBackgroundDrawable
		// for the background, we must set the WebView background color to transparent
		// in order to see any of it.
		boolean isBgRelated = (key.startsWith(TiC.PROPERTY_BACKGROUND_PREFIX) || key.startsWith(TiC.PROPERTY_BORDER_PREFIX));
		if (isBgRelated && nativeView != null && nativeView.getBackground() instanceof TiBackgroundDrawable) {
			nativeView.setBackgroundColor(Color.TRANSPARENT);
		}
	}

	private boolean mightBeHtml(String url)
	{
		String mime = TiMimeTypeHelper.getMimeType(url);
		if (mime.equals("text/html")) {
			return true;
		} else if (mime.equals("application/xhtml+xml")) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setRequestHeaders(HashMap items) {
		Map<String, String> map = items;
		for (Map.Entry<String, String> item : map.entrySet()) {
			extraHeaders.put(item.getKey().toString(), item.getValue()
					.toString());
		}
	}

	public void setUrl(String url)
	{
		reloadMethod = reloadTypes.URL;
		reloadData = url;
		String finalUrl = url;
		Uri uri = Uri.parse(finalUrl);
		boolean originalUrlHasScheme = (uri.getScheme() != null);

		if (!originalUrlHasScheme) {
			finalUrl = getProxy().resolveUrl(null, finalUrl);
		}

		if (TiFileFactory.isLocalScheme(finalUrl) && mightBeHtml(finalUrl)) {
			TiBaseFile tiFile = TiFileFactory.createTitaniumFile(finalUrl, false);
			if (tiFile != null) {
				StringBuilder out = new StringBuilder();
				InputStream fis = null;
				try {
					fis = tiFile.getInputStream();
					InputStreamReader reader = new InputStreamReader(fis, "utf-8");
					BufferedReader breader = new BufferedReader(reader);
					String line = breader.readLine();
					while (line != null) {
						if (!bindingCodeInjected) {
							int pos = line.indexOf("<html");
							if (pos >= 0) {
								int posEnd = line.indexOf(">", pos);
								if (posEnd > pos) {
									out.append(line.substring(pos, posEnd + 1));
									out.append(TiWebViewBinding.SCRIPT_TAG_INJECTION_CODE);
									if ((posEnd + 1) < line.length()) {
										out.append(line.substring(posEnd + 1));
									}
									out.append("\n");
									bindingCodeInjected = true;
									line = breader.readLine();
									continue;
								}
							}
						}
						out.append(line);
						out.append("\n");
						line = breader.readLine();
					}
					setHtmlInternal(out.toString(), (originalUrlHasScheme ? url : finalUrl), "text/html"); // keep app:// etc. intact in case
																								   	       // html in file contains links
																						 				   // to JS that use app:// etc.
					return;
				} catch (IOException ioe) {
					Log.e(TAG, "Problem reading from " + url + ": " + ioe.getMessage()
						+ ". Will let WebView try loading it directly.", ioe);
				} finally {
					if (fis != null) {
						try {
							fis.close();
						} catch (IOException e) {
							Log.w(TAG, "Problem closing stream: " + e.getMessage(), e);
						}
					}
				}
			}
		}

		Log.d(TAG, "WebView will load " + url + " directly without code injection.", Log.DEBUG_MODE);
		// iOS parity: for whatever reason, when a remote url is used, the iOS implementation
		// explicitly sets the native webview's setScalesPageToFit to YES if the
		// Ti scalesPageToFit property has _not_ been set.
		if (!proxy.hasProperty(TiC.PROPERTY_SCALES_PAGE_TO_FIT)) {
			getWebView().getSettings().setLoadWithOverviewMode(true);
		}
		isLocalHTML = false;
		if (extraHeaders.size() > 0){
 			getWebView().loadUrl(finalUrl, extraHeaders);
 		} else {
 			getWebView().loadUrl(finalUrl);
 		}

	}

	public void changeProxyUrl(String url)
	{
		getProxy().setProperty("url", url);
		if(!TiC.URL_ANDROID_ASSET_RESOURCES.equals(url)) {
			reloadMethod = reloadTypes.URL;
			reloadData = url;
		}
	}

	public String getUrl()
	{
		return getWebView().getUrl();
	}

	private static final char escapeChars[] = new char[] { '%', '#', '\'', '?' };

	private String escapeContent(String content)
	{
		// The Android WebView has a known bug
		// where it forgets to escape certain characters
		// when it creates a data:// URL in the loadData() method
		// http://code.google.com/p/android/issues/detail?id=1733
		for (char escapeChar : escapeChars) {
			String regex = "\\" + escapeChar;
			content = content.replaceAll(regex, "%" + Integer.toHexString(escapeChar));
		}
		return content;
	}

	public void setHtml(String html)
	{
		reloadMethod = reloadTypes.HTML;
		reloadData = null;
		setHtmlInternal(html, TiC.URL_ANDROID_ASSET_RESOURCES, "text/html");
	}

	public void setHtml(String html, HashMap<String, Object> d)
	{
		if (d == null) {
			setHtml(html);
			return;
		}
		
		reloadMethod = reloadTypes.HTML;
		reloadData = d;
		String baseUrl = TiC.URL_ANDROID_ASSET_RESOURCES;
		String mimeType = "text/html";
		if (d.containsKey(TiC.PROPERTY_BASE_URL_WEBVIEW)) {
			baseUrl = TiConvert.toString(d.get(TiC.PROPERTY_BASE_URL_WEBVIEW));
		} 
		if (d.containsKey(TiC.PROPERTY_MIMETYPE)) {
			mimeType = TiConvert.toString(d.get(TiC.PROPERTY_MIMETYPE));
		}
		
		setHtmlInternal(html, baseUrl, mimeType);
	}

	/**
	 * Loads HTML content into the web view.  Note that the "historyUrl" property 
	 * must be set to non null in order for the web view history to work correctly 
	 * when working with local files (IE:  goBack() and goForward() will not work if 
	 * null is used)
	 * 
	 * @param html					HTML data to load into the web view
	 * @param baseUrl				url to associate with the data being loaded
	 * @param mimeType				mime type of the data being loaded
	 */
	private void setHtmlInternal(String html, String baseUrl, String mimeType)
	{
		// iOS parity: for whatever reason, when html is set directly, the iOS implementation
		// explicitly sets the native webview's setScalesPageToFit to NO if the
		// Ti scalesPageToFit property has _not_ been set.

		WebView webView = getWebView();
		if (!proxy.hasProperty(TiC.PROPERTY_SCALES_PAGE_TO_FIT)) {
			webView.getSettings().setLoadWithOverviewMode(false);
		}
		boolean enableJavascriptInjection = true;
		if (proxy.hasProperty(TiC.PROPERTY_ENABLE_JAVASCRIPT_INTERFACE)) {
			enableJavascriptInjection = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_ENABLE_JAVASCRIPT_INTERFACE), true);
		}
		// Set flag to indicate that it's local html (used to determine whether we want to inject binding code)
		isLocalHTML = true;
		enableJavascriptInjection = (Build.VERSION.SDK_INT > 16 || enableJavascriptInjection);

		if (!enableJavascriptInjection) {
			webView.loadDataWithBaseURL(baseUrl, html, mimeType, "utf-8", baseUrl);
			return;
		}

		if (html.contains(TiWebViewBinding.SCRIPT_INJECTION_ID)) {
			// Our injection code is in there already, go ahead and show.
			webView.loadDataWithBaseURL(baseUrl, html, mimeType, "utf-8", baseUrl);
			return;
		}

		int tagStart = html.indexOf("<html");
		int tagEnd = -1;
		if (tagStart >= 0) {
			tagEnd = html.indexOf(">", tagStart + 1);

			if (tagEnd > tagStart) {
				StringBuilder sb = new StringBuilder(html.length() + 2500);
				sb.append(html.substring(0, tagEnd + 1));
				sb.append(TiWebViewBinding.SCRIPT_TAG_INJECTION_CODE);
				if ((tagEnd + 1) < html.length()) {
					sb.append(html.substring(tagEnd + 1));
				}
				webView.loadDataWithBaseURL(baseUrl, sb.toString(), mimeType, "utf-8", baseUrl);
				bindingCodeInjected = true;
				return;
			}
		}

		webView.loadDataWithBaseURL(baseUrl, html, mimeType, "utf-8", baseUrl);
	}

	public void setData(TiBlob blob)
	{
		reloadMethod = reloadTypes.DATA;
		reloadData = blob;
		String mimeType = "text/html";
		
		// iOS parity: for whatever reason, in setData, the iOS implementation
		// explicitly sets the native webview's setScalesPageToFit to YES if the
		// Ti scalesPageToFit property has _not_ been set.
		if (!proxy.hasProperty(TiC.PROPERTY_SCALES_PAGE_TO_FIT)) {
			getWebView().getSettings().setLoadWithOverviewMode(true);
		}
		
		if (blob.getType() == TiBlob.TYPE_FILE) {
			String fullPath = blob.getNativePath();
			if (fullPath != null) {
				setUrl(fullPath);
				return;
			}
		}
		
		if (blob.getMimeType() != null) {
			mimeType = blob.getMimeType();
		}
		if (TiMimeTypeHelper.isBinaryMimeType(mimeType)) {
			getWebView().loadData(blob.toBase64(), mimeType, "base64");
		} else {
			getWebView().loadData(escapeContent(new String(blob.getBytes())), mimeType, "utf-8");
		}
	}

	public String getJSValue(String expression)
	{
		return client.getBinding().getJSValue(expression);
	}

	public void setBasicAuthentication(String username, String password)
	{
		client.setBasicAuthentication(username, password);
	}

	public void destroyWebViewBinding()
	{
		client.getBinding().destroy();
	}

	public void setPluginState(int pluginState)
	{
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
			TiWebView webView = (TiWebView) getNativeView();
			WebSettings webSettings = webView.getSettings();
			if (webView != null) {
				try {
					switch (pluginState) {
						case PLUGIN_STATE_OFF:
							internalSetPluginState.invoke(webSettings, enumPluginStateOff);
							break;
						case PLUGIN_STATE_ON:
							internalSetPluginState.invoke(webSettings, enumPluginStateOn);
							break;
						case PLUGIN_STATE_ON_DEMAND:
							internalSetPluginState.invoke(webSettings, enumPluginStateOnDemand);
							break;
						default:
							Log.w(TAG, "Not a valid plugin state. Ignoring setPluginState request");
					}
				} catch (InvocationTargetException e) {
					Log.e(TAG, "Method not supported", e);
				} catch (IllegalAccessException e) {
					Log.e(TAG, "Illegal Access", e);
				}
			}
		}
	}

	public void pauseWebView()
	{
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
			View v = getNativeView();
			if (v != null) {
				try {
					internalWebViewPause.invoke(v);
				} catch (InvocationTargetException e) {
					Log.e(TAG, "Method not supported", e);
				} catch (IllegalAccessException e) {
					Log.e(TAG, "Illegal Access", e);
				}
			}
		}
	}

	public void resumeWebView()
	{
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
			View v = getNativeView();
			if (v != null) {
				try {
					internalWebViewResume.invoke(v);
				} catch (InvocationTargetException e) {
					Log.e(TAG, "Method not supported", e);
				} catch (IllegalAccessException e) {
					Log.e(TAG, "Illegal Access", e);
				}
			}
		}
	}

	public void setEnableZoomControls(boolean enabled)
	{
		getWebView().getSettings().setSupportZoom(enabled);
		getWebView().getSettings().setBuiltInZoomControls(enabled);
	}

	public void setUserAgentString(String userAgentString)
	{
		WebView currWebView = getWebView();
		if (currWebView != null) {
			currWebView.getSettings().setUserAgentString(userAgentString);
		}
	}

	public String getUserAgentString()
	{
		WebView currWebView = getWebView();
		return (currWebView != null) ? currWebView.getSettings().getUserAgentString() : "";
	}

	public boolean canGoBack()
	{
		return getWebView().canGoBack();
	}

	public boolean canGoForward()
	{
		return getWebView().canGoForward();
	}

	public void goBack()
	{
		getWebView().goBack();
	}

	public void goForward()
	{
		getWebView().goForward();
	}

	public void reload()
	{
		switch (reloadMethod) {
		case DATA:
			if (reloadData != null && reloadData instanceof TiBlob) {
				setData((TiBlob) reloadData);
			} else {
				Log.d(TAG, "reloadMethod points to data but reloadData is null or of wrong type. Calling default", Log.DEBUG_MODE);
				getWebView().reload();
			}
			break;
			
		case HTML:
			if (reloadData == null || (reloadData instanceof HashMap<?,?>) ) {
				setHtml(TiConvert.toString(getProxy().getProperty(TiC.PROPERTY_HTML)), (HashMap<String,Object>)reloadData);
			} else {
				Log.d(TAG, "reloadMethod points to html but reloadData is of wrong type. Calling default", Log.DEBUG_MODE);
				getWebView().reload();
			}
			break;
		
		case URL:
			if (reloadData != null && reloadData instanceof String) {
				setUrl((String) reloadData);
			} else {
				Log.d(TAG, "reloadMethod points to url but reloadData is null or of wrong type. Calling default", Log.DEBUG_MODE);
				getWebView().reload();
			}
			break;
			
		default:
			getWebView().reload();
		}
	}
	
	public String getScrollHeight() {
		return TiConvert.toString(getWebView().getContentHeight());
	}

	public void stopLoading()
	{
		getWebView().stopLoading();
	}

	public boolean shouldInjectBindingCode()
	{
		return isLocalHTML && !bindingCodeInjected;
	}

	public void setBindingCodeInjected(boolean injected)
	{
		bindingCodeInjected = injected;
	}

	public boolean interceptOnBackPressed()
	{
		return chromeClient.interceptOnBackPressed();
	}
}
