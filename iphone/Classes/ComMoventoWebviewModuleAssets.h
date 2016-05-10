/**
 * Module developed by Napp
 * Author Mads Møller
 * www.napp.dk
 * Author Christoph Eck
 * www.movento.com
 */

@interface ComMoventoWebviewModuleAssets : NSObject
{
}
- (NSData*) moduleAsset;
- (NSData*) resolveModuleAsset:(NSString*)path;

@end
