#import "RNReactNativeCblImageManager.h"
#import "RNReactNativeCblImage.h"

@implementation RNReactNativeCblImageManager

RCT_EXPORT_MODULE()

- (UIView *)view
{
    return [[RNReactNativeCblImage alloc] init];
}

@end
