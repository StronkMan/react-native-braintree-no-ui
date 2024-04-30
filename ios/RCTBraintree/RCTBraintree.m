//
//  RCTBraintree.m
//  RCTBraintree
//
//  Created by Rickard Ekman on 18/06/16.
//  Copyright © 2016 Rickard Ekman. All rights reserved.
//

#import "RCTBraintree.h"
#import <React/RCTLog.h>

@implementation RCTBraintree {
  bool runCallback;
}

static NSString *URLScheme;

+ (instancetype)sharedInstance {
  static RCTBraintree *_sharedInstance = nil;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    _sharedInstance = [[RCTBraintree alloc] init];
  });
  return _sharedInstance;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

- (instancetype)init {
  self = [super init];
  return self;
}
RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(setupWithURLScheme
                  : (NSString *)clientToken urlscheme
                  : (NSString *)urlscheme callback
                  : (RCTResponseSenderBlock)callback) {
  URLScheme = urlscheme;
    BTAppContextSwitcher.sharedInstance.returnURLScheme = urlscheme;
  self.braintreeClient =
      [[BTAPIClient alloc] initWithAuthorization:clientToken];
  self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.braintreeClient];

  if (self.braintreeClient == nil) {
    callback(@[ @false ]);
  } else {
    callback(@[ @true ]);
  }
}


RCT_EXPORT_METHOD(showPayPalViewController : (RCTResponseSenderBlock)callback) {
  dispatch_async(dispatch_get_main_queue(), ^{
      BTPayPalClient *payPalDriver =
        [[BTPayPalClient alloc] initWithAPIClient:self.braintreeClient];
      BTPayPalVaultRequest *request = [[BTPayPalVaultRequest alloc] initWithOfferCredit:NO userAuthenticationEmail:nil];
      [payPalDriver tokenizeWithVaultRequest: request completion: ^(BTPayPalAccountNonce *tokenizedPayPalAccount, NSError *error) {
        NSMutableArray *args = @[ [NSNull null] ];
        if (error == nil && tokenizedPayPalAccount != nil) {
          args = [@[
            [NSNull null], tokenizedPayPalAccount.nonce,
            tokenizedPayPalAccount.email, tokenizedPayPalAccount.firstName,
            tokenizedPayPalAccount.lastName
          ] mutableCopy];

          if (tokenizedPayPalAccount.phone != nil) {
            [args addObject:tokenizedPayPalAccount.phone];
          }
        } else if (error != nil) {
          args = @[ error.description, [NSNull null] ];
        }
        callback(args);
      }];
  });
}

RCT_EXPORT_METHOD(showVenmoViewController : (RCTResponseSenderBlock)callback) {
  dispatch_async(dispatch_get_main_queue(), ^{
      BTVenmoClient *venmoDriver =
        [[BTVenmoClient alloc] initWithAPIClient:self.braintreeClient];
      BTVenmoRequest *request = [[BTVenmoRequest alloc] initWithPaymentMethodUsage:BTVenmoPaymentMethodUsageMultiUse];
      [venmoDriver tokenizeWithVenmoRequest: request completion: ^(BTVenmoAccountNonce *venmoAccount, NSError *error) {
          NSMutableArray *args = @[ [NSNull null] ];
          if (error == nil && venmoAccount != nil) {
            if (venmoAccount.nonce != nil) {
              args = @[ [NSNull null], venmoAccount.nonce ];
            }
          } else {
            if (error != nil) {
              args = @[ error.description, [NSNull null] ];
            } else {
              args = @[ @"Payment Cancelled", [NSNull null] ];
            }
          }
          callback(args);
      }];
  });
}

RCT_EXPORT_METHOD(getDeviceData
                  : (NSDictionary *)options callback
                  : (RCTResponseSenderBlock)callback) {
  dispatch_async(dispatch_get_main_queue(), ^{
    NSLog(@"%@", options);

    NSError *error = nil;
    NSString *deviceData = nil;
    NSString *environment = options[@"environment"];
    NSString *dataSelector = options[@"dataCollector"];


    [self.dataCollector collectDeviceData:^(NSString *dataToken, NSError *error) {
        NSArray *args = @[];
        if (error == nil) {
            args = @[ [NSNull null], dataToken ];
        } else {
            args = @[ error.description, [NSNull null] ];
        }

        callback(args);
    }];
  });
}

RCT_EXPORT_METHOD(showApplePayViewController
                  : (NSDictionary *)options callback
                  : (RCTResponseSenderBlock)callback) {
  dispatch_async(dispatch_get_main_queue(), ^{
    self.callback = callback;
    PKPaymentRequest *paymentRequest = [[PKPaymentRequest alloc] init];
    NSArray *items = options[@"paymentSummaryItems"];
    NSLog(@"Options items: %@", items);
    NSMutableArray *paymentSummaryItems = [NSMutableArray new];
    for (NSDictionary *item in items) {
      NSString *label = item[@"label"];
      NSString *amount = [item[@"amount"] stringValue];
      [paymentSummaryItems
          addObject:
              [PKPaymentSummaryItem
                  summaryItemWithLabel:label
                                amount:[NSDecimalNumber
                                           decimalNumberWithString:amount]]];
    }
    paymentRequest.requiredBillingContactFields =
        [NSSet setWithObjects:PKContactFieldPostalAddress,
                              PKContactFieldPhoneNumber, nil];
    paymentRequest.shippingMethods = nil;
    paymentRequest.requiredShippingContactFields = nil;
    paymentRequest.paymentSummaryItems = paymentSummaryItems;

    paymentRequest.merchantIdentifier = options[@"merchantIdentifier"];
    ;
    paymentRequest.supportedNetworks = @[
      PKPaymentNetworkVisa, PKPaymentNetworkMasterCard, PKPaymentNetworkAmex,
      PKPaymentNetworkDiscover
    ];
    paymentRequest.merchantCapabilities = PKMerchantCapability3DS;
    paymentRequest.currencyCode = @"USD";
    paymentRequest.countryCode = @"US";
    if ([paymentRequest respondsToSelector:@selector(setShippingType:)]) {
      paymentRequest.shippingType = PKShippingTypeDelivery;
    }

    PKPaymentAuthorizationViewController *viewController =
        [[PKPaymentAuthorizationViewController alloc]
            initWithPaymentRequest:paymentRequest];
    viewController.delegate = self;

    [self.reactRoot presentViewController:viewController
                                 animated:YES
                               completion:nil];
  });
}

- (BOOL)application:(UIApplication *)application
              openURL:(NSURL *)url
    sourceApplication:(NSString *)sourceApplication
           annotation:(id)annotation {

  if ([url.scheme localizedCaseInsensitiveCompare:URLScheme] == NSOrderedSame) {
    return [BTAppContextSwitcher.sharedInstance handleOpenURL:url];
  }
  return NO;
}

#pragma mark - BTViewControllerPresentingDelegate

- (void)paymentDriver:(id)paymentDriver
    requestsPresentationOfViewController:(UIViewController *)viewController {
  [self.reactRoot presentViewController:viewController
                               animated:YES
                             completion:nil];
}

- (void)paymentDriver:(id)paymentDriver
    requestsDismissalOfViewController:(UIViewController *)viewController {
  if (!viewController.isBeingDismissed) {
    [viewController.presentingViewController dismissViewControllerAnimated:YES
                                                                completion:nil];
  }
}

- (UIViewController *)reactRoot {
  UIViewController *root =
      [UIApplication sharedApplication].keyWindow.rootViewController;
  UIViewController *maybeModal = root.presentedViewController;

  UIViewController *modalRoot = root;

  if (maybeModal != nil) {
    modalRoot = maybeModal;
  }

  return modalRoot;
}

#pragma mark PKPaymentAuthorizationViewControllerDelegate

- (void)
    paymentAuthorizationViewController:
        (__unused PKPaymentAuthorizationViewController *)controller
                   didAuthorizePayment:(PKPayment *)payment
                            completion:
                                (void (^)(PKPaymentAuthorizationStatus status))
                                    completion {
  NSLog(@"paymentAuthorizationViewController:didAuthorizePayment");

  BTApplePayClient *applePayClient =
      [[BTApplePayClient alloc] initWithAPIClient:self.braintreeClient];
  [applePayClient
      tokenizeApplePayPayment:payment
                   completion:^(
                       BTApplePayCardNonce *_Nullable tokenizedApplePayPayment,
                       NSError *_Nullable error) {
                     if (error) {
                       completion(PKPaymentAuthorizationStatusFailure);
                       self.callback(
                           @[ @"Error processing card", [NSNull null] ]);
                     } else {
                       CNPostalAddress *paymentAddress =
                           payment.billingContact.postalAddress;
                       self.callback(@[
                         [NSNull null], @{
                           @"nonce" : tokenizedApplePayPayment.nonce,
                           @"type" : tokenizedApplePayPayment.type,
                           @"localizedDescription" :
                               tokenizedApplePayPayment.description,
                           @"billingContact" : @{
                             @"streetAddress" : paymentAddress.street,
                             @"city" : paymentAddress.city,
                             @"state" : paymentAddress.state,
                             @"country" : paymentAddress.country,
                             @"postalCode" : paymentAddress.postalCode,
                             @"countryCode" : paymentAddress.ISOCountryCode,
                           }
                         }
                       ]);
                       completion(PKPaymentAuthorizationStatusSuccess);
                     }
                   }];
}

- (void)paymentAuthorizationViewControllerDidFinish:
    (PKPaymentAuthorizationViewController *)controller {
  // Just close the view controller. We either succeeded or the user hit cancel.
  [self.reactRoot dismissViewControllerAnimated:YES completion:nil];
}

- (void)paymentAuthorizationViewControllerWillAuthorizePayment:
    (PKPaymentAuthorizationViewController *)controller {
  // Move along. Nothing to see here.
}

@end
