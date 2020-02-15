/*
 * This file is part of the SDWebImage package.
 * (c) Olivier Poitrey <rs@dailymotion.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

#import "SDImageCacheDefine.h"
#import "SDImageCodersManager.h"
#import "SDImageCoderHelper.h"
#import "SDAnimatedImage.h"
#import "UIImage+Metadata.h"

UIImage * _Nullable SDImageCacheDecodeImageData(NSData * _Nonnull imageData, NSString * _Nonnull cacheKey, SDWebImageOptions options, SDWebImageContext * _Nullable context) {
    UIImage *image;
    BOOL decodeFirstFrame = options & SDWebImageDecodeFirstFrameOnly;
    NSNumber *scaleValue = context[SDWebImageContextImageScaleFactor];
    CGFloat scale = scaleValue.doubleValue >= 1 ? scaleValue.doubleValue : SDImageScaleFactorForKey(cacheKey);
    SDImageCoderOptions *coderOptions = @{SDImageCoderDecodeFirstFrameOnly : @(decodeFirstFrame), SDImageCoderDecodeScaleFactor : @(scale)};
    if (context) {
        SDImageCoderMutableOptions *mutableCoderOptions = [coderOptions mutableCopy];
        [mutableCoderOptions setValue:context forKey:SDImageCoderWebImageContext];
        coderOptions = [mutableCoderOptions copy];
    }
    
    if (!decodeFirstFrame) {
        Class animatedImageClass = context[SDWebImageContextAnimatedImageClass];
        // check whether we should use `SDAnimatedImage`
        if ([animatedImageClass isSubclassOfClass:[UIImage class]] && [animatedImageClass conformsToProtocol:@protocol(SDAnimatedImage)]) {
            image = [[animatedImageClass alloc] initWithData:imageData scale:scale options:coderOptions];
            if (options & SDWebImagePreloadAllFrames && [image respondsToSelector:@selector(preloadAllFrames)]) {
                [((id<SDAnimatedImage>)image) preloadAllFrames];
            }
        }
    }
    if (!image) {
        image = [[SDImageCodersManager sharedManager] decodedImageWithData:imageData options:coderOptions];
    }
    if (image) {
        BOOL shouldDecode = (options & SDWebImageAvoidDecodeImage) == 0;
        if ([image conformsToProtocol:@protocol(SDAnimatedImage)]) {
            // `SDAnimatedImage` do not decode
            shouldDecode = NO;
        } else if (image.sd_isAnimated) {
            // animated image do not decode
            shouldDecode = NO;
        }
        if (shouldDecode) {
            BOOL shouldScaleDown = options & SDWebImageScaleDownLargeImages;
            if (shouldScaleDown) {
                image = [SDImageCoderHelper decodedAndScaledDownImageWithImage:image limitBytes:0];
            } else {
                image = [SDImageCoderHelper decodedImageWithImage:image];
            }
        }
    }
    
    return image;
}
