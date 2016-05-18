// This is a test harness for your module
// You should do something interesting in this harness
// to test out the module and to provide instructions
// to users on how to use it by example.

require('com.movento.webview');

var webView = Ti.UI.createWebView({
    url: 'http://www.appcelerator.com'
});
webView.setRequestHeaders({'my-customheader-1': 'custom-header-value', 'add-as-many-headers-as-you-need': 'value'});
