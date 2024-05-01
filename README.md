# react-native-braintree-xplat

Follow both setup guide with native sdk first

https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4

https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/ios/v6

## Usage

### Setup

```js
BTClient.setup(token);
BTClient.setupWithURLScheme(token, "apple.merchant.id");
```

---
**PayPal, Venmo**

```js
BTClient.showPayPalViewController()
  .then(function(nonce) {
    //payment succeeded, pass nonce to server
  })
  .catch(function(err) {
    //error handling
  });
```

---

**Apple Pay**
```js
BTClient.showApplePayViewController({
    merchantIdentifier: '<your.merchant.id>',
    paymentSummaryItems: [
        {label: 'Subtotals', amount: subtotals},
        {label: 'Shipping', amount: shipping},
        {label: 'Totals', amount: totals},
    ]
    })
    .then((response) => {
        console.log(response);
        if (response.nonce && response.billingContact) {
            // Do something with the nonce
        }
    });
```


---
**Google Pay**

```js
BTClient.showGooglePayViewController({
    totalPrice: '<amount>',
    currencyCode: 'US',
    requireAddress: true,
    googleMerchantId: '<google-merchant-id>'
    })
    .then((response) => {
        console.log(response);
        if (response.nonce && response.billingContact) {
            // Do something with the nonce
        }
    });
```



## Credits

Big thanks to [@alanhhwong](https://github.com/alanhhwong) and [@surialabs](https://github.com/surialabs) for the original ios & android modules.

Works made possible by [@kraffslol](https://github.com/kraffslol/react-native-braintree-xplat)
