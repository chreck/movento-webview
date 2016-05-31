//
//  TiUIWebViewProxy+Extend.m
//  ExtWebView
//
//  Created by Martin Wildfeuer on 13.02.15.
//
//

#import "TiUIWebViewProxy+Extend.h"
#import "TiUIWebView+Extend.h"
#import <objc/runtime.h>

@implementation TiUIWebViewProxy (Extend)

-(void)setRequestHeader:(NSDictionary *)headers
{
    // calls the setRequestHeaders method in TiUIWebView+Extend.m
    [self makeViewPerformSelector:@selector(setRequestHeaders_:) withObject:headers createIfNeeded:YES waitUntilDone:NO];
}

-(id)getScrollHeight:(id)args {
    float height = 0;
    @try {
        height = [(TiUIWebView*)self.view getWebViewHeight];
        NSLog(@"[DEBUG] getScrollHeight_ height: %f", height);
    } @catch (NSException *exception) {
        NSLog(@"[WARN] getScrollHeight EXCEPTION OCCURED %@", exception);
    } @finally {
        return @(height);
    }
}

@end
