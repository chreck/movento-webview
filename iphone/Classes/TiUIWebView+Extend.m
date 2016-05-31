/**
 * Module developed by Napp
 * Author Mads Møller
 * www.napp.dk
 *
 * by ryugoo
 *
 * Author Christoph Eck
 * www.movento.com
 */

#import "TiUIWebViewProxy+Extend.h"
#import "TiUIWebView+Extend.h"
#import "TiUtils.h"
#import <objc/runtime.h>

@implementation TiUIWebView (Extend)

// for more information about adding methods over objc see http://stackoverflow.com/questions/4146183/instance-variables-for-objective-c-categories

-(NSDictionary *)requestHeaders
{
    return objc_getAssociatedObject(self, @selector(requestHeaders));
}

-(void)setRequestHeaders_:(NSDictionary *)headers
{
    objc_setAssociatedObject(self, @selector(requestHeaders), headers, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

-(void)loadURLRequest:(NSMutableURLRequest*)request
{
    NSLog(@"[DEBUG] loadURLRequest request %@", request);
    
    if (basicCredentials!=nil)
    {
        NSLog(@"[DEBUG] loadURLRequest basicCredentials %@", basicCredentials);
        [request setValue:basicCredentials forHTTPHeaderField:@"Authorization"];
    }
    
    if([self requestHeaders]!=nil)
    {
        // set the new headers
        for(NSString *key in [self.requestHeaders allKeys]){
            NSString *value = [self.requestHeaders objectForKey:key];
            NSLog(@"[INFO] loadURLRequest header key %@ value %@", key, value);
            [request addValue:value forHTTPHeaderField:key];
        }
    }

    if (webview!=nil){
        NSLog(@"[DEBUG] loadURLRequest in webview");
        [webview loadRequest:request];
    } else {
        SEL selectorWebView = NSSelectorFromString(@"webview");
        webview = [self performSelector:selectorWebView];
        if (webview!=nil){
            [webview loadRequest:request];
        } else {
            NSLog(@"[INFO] loadURLRequest webview is nil");
        }
    }
}

- (float) getWebViewHeight{
    float height = webview.scrollView.contentSize.height;
    return height;
}

@end