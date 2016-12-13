# com.movento.webview Module

## Description

This module extends the native UI component TiUIWebView provided by the Titanium Mobile SDK.

This is not a set of new TiUIWebView component. Instead, we are extending the existing framework.

### Motivation

It is not possible to set custom request headers on Android and iOS (open jira https://jira.appcelerator.org/browse/TIMOB-17467). To get the correct height from a webview it is necessary to use evalJS. But this can be wrong and it is slow.

```javascript
webview.addEventListener('load', function(event){
Ti.API.info("height " + e.source.evalJS("document.height;"); 
});
```
The implemenation is based on some ideas from 
https://github.com/viezel/NappUI and

## Accessing the Module

Copy the following into the alloy.js file

```javascript
// to create the correct webview automatically in OS_ANDROID
if(OS_ANDROID) {
    Ti.UI.createWebView = require('com.movento.webview').createWebView;
}
```

On the android device it can not load the correct module. Use the following for defining in Alloy. 

```xml
<WebView platform="android" module="com.movento.webview" id="webView"/>
<WebView platform="ios" id="webView"/>
```

```javascript
var webview = Ti.UI.createWebView();
webview.setRequestHeaders({'my-customheader-1': 'custom-header-value', 'add-as-many-headers-as-you-need': 'value'});
webview.addEventListener('load', function(event){
    Ti.API.info("getScrollHeight " + event.source.getScrollHeight()); 
});
webview.setUrl("http://www.google.com");
```

If you are using the webview in a window call the setUrl() and setRequestHeaders() after the window fires the 'open' event. I.e. 
```javascript
var win = Ti.UI.createWindow({
});
var openListener = function(event) {
	win.add(webview);
	webview.setRequestHeaders({'my-customheader-1': 'custom-header-value', 'add-as-many-headers-as-you-need': 'value'});
	webview.setUrl('http://www.google.com');
};
win.addEventListener('open', openListener);
win.open();
```

The implementation of the request headers will be included when the Jira https://jira.appcelerator.org/browse/TIMOB-17467 is closed. At the moment it is solved in the version of Titanium SDK 6.1.0.

## Changelog

see CHANGELOG.txt

### Build

Call ./build.sh file in terminal

## Author

**Mads Moller**  
web: http://www.napp.dk  
email: mm@napp.dk  
twitter: @nappdev  

**Christoph Eck**
web: http://www.movento.com/

## License

Copyright (c) 2010-2013 Mads Moller

Copyright (c) 2016 Christoph Eck, movento Schweiz AG

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
