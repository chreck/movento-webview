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

#import "TiBase.h"
#import "TiUIWebView.h"

@interface TiUIWebView (Extend)

@property (nonatomic, retain)  NSDictionary *requestHeaders;

- (float) getWebViewHeight;
@end
