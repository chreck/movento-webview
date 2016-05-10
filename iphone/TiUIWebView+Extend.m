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

#import "TiUIWebView+Extend.h"
#import "TiUtils.h"
#import <objc/runtime.h>

@implementation TiUIWebView (Extend)

-(NSDictionary *)customHeaders
{
    return objc_getAssociatedObject(self, @selector(customHeaders));
}

-(void)setCustomHeaders_:(NSDictionary *)headers
{
    objc_setAssociatedObject(self, @selector(customHeaders), headers, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

-(NSDictionary *)customHeaders_
{
        return objc_getAssociatedObject(self, @selector(customHeaders));
}

-(void)loadURLRequest:(NSMutableURLRequest*)request
{
    NSLog(@"[DEBUG] loadURLRequest");
    
    if (basicCredentials!=nil)
    {
        NSLog(@"[DEBUG] loadURLRequest basicCredentials %@", basicCredentials);
        [request setValue:basicCredentials forHTTPHeaderField:@"Authorization"];
    }
    
    if([self customHeaders]!=nil)
    {
        // set the new headers
        for(NSString *key in [self.customHeaders allKeys]){
            NSLog(@"[DEBUG] loadURLRequest headers %@", key);
            [request addValue:[self.customHeaders objectForKey:key] forHTTPHeaderField:key];
        }
    }

    if (webview!=nil){
        NSLog(@"[DEBUG] loadURLRequest in webview");
        [webview loadRequest:request];
    }
}

@end