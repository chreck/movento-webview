// This is a test harness for your module
// You should do something interesting in this harness
// to test out the module and to provide instructions
// to users on how to use it by example.

if(OS_ANDROID) {
    Ti.UI.createWebView = require('com.movento.webview').createWebView;
}

var webview = Ti.UI.createWebView();
webview.setRequestHeaders({'my-customheader-1': 'custom-header-value', 'add-as-many-headers-as-you-need': 'value'});
webview.addEventListener('load', function(event){
    Ti.API.info("getScrollHeight " + event.source.getScrollHeight()); 
});
webview.setUrl("http://www.google.com");