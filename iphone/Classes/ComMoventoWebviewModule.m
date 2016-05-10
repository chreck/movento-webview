/**
 * webview
 *
 * Created by Christoph Eck
 * Copyright (c) 2016 Movento. All rights reserved.
 */

#import "ComMoventoWebviewModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"

@implementation ComMoventoWebviewModule

#pragma mark Internal

// this is generated for your module, please do not change it
-(id)moduleGUID
{
	return @"4a7e32d4-317d-4170-9630-9b68e579fabe";
}

// this is generated for your module, please do not change it
-(NSString*)moduleId
{
	return @"com.movento.webview";
}

#pragma mark Lifecycle

-(void)startup
{
	[super startup];

	NSLog(@"[INFO] %@ loaded",self);
}

-(void)shutdown:(id)sender
{
	[super shutdown:sender];
}

#pragma mark Cleanup

-(void)dealloc
{
	[super dealloc];
}

#pragma mark Internal Memory Management

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
	[super didReceiveMemoryWarning:notification];
}

@end
