//
//  TiUIWebViewProxy+Extend.m
//  ExtWebView
//
//  Created by Martin Wildfeuer on 13.02.15.
//
//

#import "TiUIWebViewProxy+Extend.h"
#import "TiUIWebView+Extend.h"

@implementation TiUIWebViewProxy (Extend)

-(void)setRequestHeader:(NSDictionary *)headers
{
    // calls the setRequestHeaders method in TiUIWebView+Extend.m
    [self makeViewPerformSelector:@selector(setRequestHeaders_:) withObject:headers createIfNeeded:YES waitUntilDone:NO];
}

@end
