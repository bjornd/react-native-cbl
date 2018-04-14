/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "RNReactNativeCblImage.h"

#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTImageSource.h>
#import <React/RCTUtils.h>
#import <React/UIView+React.h>

#import "RCTImageBlurUtils.h"
#import "RCTImageLoader.h"
#import "RCTImageUtils.h"

@implementation RNReactNativeCblImage

- (instancetype)initWithBridge:(RCTBridge *)bridge
{
    if ((self = [super init])) {
        NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
        [center addObserver:self
                   selector:@selector(clearImageIfDetached)
                       name:UIApplicationDidReceiveMemoryWarningNotification
                     object:nil];
        [center addObserver:self
                   selector:@selector(clearImageIfDetached)
                       name:UIApplicationDidEnterBackgroundNotification
                     object:nil];
    }
    return self;
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)updateWithImage:(UIImage *)image
{
    if (!image) {
        super.image = nil;
        return;
    }
    
    // Apply rendering mode
    if (_renderingMode != image.renderingMode) {
        image = [image imageWithRenderingMode:_renderingMode];
    }
    
    if (_resizeMode == RCTResizeModeRepeat) {
        image = [image resizableImageWithCapInsets:_capInsets resizingMode:UIImageResizingModeTile];
    } else if (!UIEdgeInsetsEqualToEdgeInsets(UIEdgeInsetsZero, _capInsets)) {
        // Applying capInsets of 0 will switch the "resizingMode" of the image to "tile" which is undesired
        image = [image resizableImageWithCapInsets:_capInsets resizingMode:UIImageResizingModeStretch];
    }
    
    // Apply trilinear filtering to smooth out mis-sized images
    self.layer.minificationFilter = kCAFilterTrilinear;
    self.layer.magnificationFilter = kCAFilterTrilinear;
    
    super.image = image;
}

- (void)setImage:(UIImage *)image
{
    image = image ?: _defaultImage;
    if (image != self.image) {
        [self updateWithImage:image];
    }
}

- (void)setBlurRadius:(CGFloat)blurRadius
{
    if (blurRadius != _blurRadius) {
        _blurRadius = blurRadius;
    }
}

- (void)setCapInsets:(UIEdgeInsets)capInsets
{
    if (!UIEdgeInsetsEqualToEdgeInsets(_capInsets, capInsets)) {
        if (UIEdgeInsetsEqualToEdgeInsets(_capInsets, UIEdgeInsetsZero) ||
            UIEdgeInsetsEqualToEdgeInsets(capInsets, UIEdgeInsetsZero)) {
            _capInsets = capInsets;
        } else {
            _capInsets = capInsets;
            [self updateWithImage:self.image];
        }
    }
}

- (void)setRenderingMode:(UIImageRenderingMode)renderingMode
{
    if (_renderingMode != renderingMode) {
        _renderingMode = renderingMode;
        [self updateWithImage:self.image];
    }
}

- (void)setResizeMode:(RCTResizeMode)resizeMode
{
    if (_resizeMode != resizeMode) {
        _resizeMode = resizeMode;
        
        if (_resizeMode == RCTResizeModeRepeat) {
            // Repeat resize mode is handled by the UIImage. Use scale to fill
            // so the repeated image fills the UIImageView.
            self.contentMode = UIViewContentModeScaleToFill;
        } else {
            self.contentMode = (UIViewContentMode)resizeMode;
        }        
    }
}

- (void)clearImage
{
    [self.layer removeAnimationForKey:@"contents"];
    self.image = nil;
}

- (void)clearImageIfDetached
{
    if (!self.window) {
        [self clearImage];
    }
}

@end
