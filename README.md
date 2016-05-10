# com.movento.webview Module

## Description

This module extends the native UI component TiUIWebView provided by the Titanium Mobile SDK.

This is not a set of new TiUIWebView component. Instead, we are extending the existing framework.

## Accessing the Module

Instantiate the module through ```require();```  This will modify and override the native Titanium classes.  

## Reference

### WebView

* Custom request headers for WebView

```javascript
var webView = Ti.UI.createWebView({
url: 'http://www.appcelerator.com'
});
webView.setCustomHeaders({'my-customheader-1': 'custom-header-value', 'add-as-many-headers-as-you-need': 'value'});
```

## Changelog

see CHANGELOG.txt

## Author

**Mads Moller**  
web: http://www.napp.dk  
email: mm@napp.dk  
twitter: @nappdev  

**Christoph Eck**
web: http://http://www.movento.com/

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