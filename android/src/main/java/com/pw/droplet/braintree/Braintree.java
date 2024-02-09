package com.pw.droplet.braintree;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.GooglePayment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class Braintree extends ReactContextBaseJavaModule {
    private String token;
    private Callback deviceDataCallback;
    private Boolean collectDeviceData = false;
    private Callback successCallback;
    private Callback errorCallback;

    private BraintreeFragment mBraintreeFragment;

    public Braintree(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(activityEventListener);
    }

    @Override
    public String getName() {
        return "Braintree";
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @ReactMethod
    public void setup(final String token, final Callback successCallback,
                      final Callback errorCallback) {
        try {
            this.mBraintreeFragment = BraintreeFragment.newInstance(
                    (FragmentActivity) getCurrentActivity(), token);
        } catch (InvalidArgumentException e) {
            Log.e("PAYMENT_REQUEST", "I got an error", e);
            errorCallback.invoke(e.getMessage());
        }
        if (this.mBraintreeFragment instanceof BraintreeFragment) {
            this.mBraintreeFragment.addListener(new BraintreeCancelListener() {
                @Override
                public void onCancel(int requestCode) {
                    nonceErrorCallback("USER_CANCELLATION");
                }
            });
            this.mBraintreeFragment.addListener(
                    new PaymentMethodNonceCreatedListener() {
                        @Override
                        public void onPaymentMethodNonceCreated(
                                PaymentMethodNonce paymentMethodNonce) {

                            if (paymentMethodNonce instanceof GooglePaymentCardNonce) {

                                WritableMap payment = new WritableNativeMap();
                                payment.putString("nonce", paymentMethodNonce.getNonce());
                                WritableMap billingContact = new WritableNativeMap();

                                billingContact.putString(
                                        "streetAddress",
                                        ((GooglePaymentCardNonce) paymentMethodNonce)
                                                .getBillingAddress()
                                                .getStreetAddress());
                                billingContact.putString(
                                        "city", ((GooglePaymentCardNonce) paymentMethodNonce)
                                                .getBillingAddress()
                                                .getLocality());
                                billingContact.putString(
                                        "state", ((GooglePaymentCardNonce) paymentMethodNonce)
                                                .getBillingAddress()
                                                .getRegion());
                                billingContact.putString(
                                        "country", ((GooglePaymentCardNonce) paymentMethodNonce)
                                                .getBillingAddress()
                                                .getCountryCodeAlpha2());
                                billingContact.putString(
                                        "postalCode", ((GooglePaymentCardNonce) paymentMethodNonce)
                                                .getBillingAddress()
                                                .getPostalCode());
                                billingContact.putString(
                                        "countryCode", ((GooglePaymentCardNonce) paymentMethodNonce)
                                                .getBillingAddress()
                                                .getCountryCodeAlpha2());
                                payment.putMap("billingContact", billingContact);
                                objCallback(payment);

                            } else {
                                nonceCallback(paymentMethodNonce.getNonce());
                            }
                        }
                    });

            this.mBraintreeFragment.addListener(new BraintreeErrorListener() {
                @Override
                public void onError(Exception error) {
                    Log.e("PAYMENT_REQUEST", "I got an error", error);
                    if (error instanceof ErrorWithResponse) {
                        ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
                        BraintreeError cardErrors =
                                errorWithResponse.errorFor("creditCard");
                        if (cardErrors != null) {
                            Gson gson = new Gson();
                            final Map<String, String> errors = new HashMap<>();
                            BraintreeError numberError = cardErrors.errorFor("number");
                            BraintreeError cvvError = cardErrors.errorFor("cvv");
                            BraintreeError expirationDateError =
                                    cardErrors.errorFor("expirationDate");
                            BraintreeError postalCode = cardErrors.errorFor("postalCode");

                            if (numberError != null) {
                                errors.put("card_number", numberError.getMessage());
                            }

                            if (cvvError != null) {
                                errors.put("cvv", cvvError.getMessage());
                            }

                            if (expirationDateError != null) {
                                errors.put("expiration_date", expirationDateError.getMessage());
                            }

                            // TODO add more fields
                            if (postalCode != null) {
                                errors.put("postal_code", postalCode.getMessage());
                            }

                            nonceErrorCallback(gson.toJson(errors));
                        } else {
                            nonceErrorCallback(errorWithResponse.getErrorResponse());
                        }
                    }
                }
            });
            this.setToken(token);
            successCallback.invoke(this.getToken());
        }
    }

    public void nonceCallback(String nonce) {
        if (nonce != null) {
            this.successCallback.invoke(nonce);
        } else {
            this.errorCallback.invoke("Sorry, we cannot access you payment method.");
        }
    }

    public void objCallback(WritableMap obj) {
        this.successCallback.invoke(obj);
    }

    public void nonceErrorCallback(String error) {
        this.errorCallback.invoke(error);
    }

    private final ActivityEventListener activityEventListener =
            new BaseActivityEventListener() {
                @Override
                public void onActivityResult(Activity activity, int requestCode,
                                             int resultCode, Intent data) {
                    int unshiftRequestCode = requestCode & 0x0000ffff;
                    if (unshiftRequestCode == BraintreeRequestCodes.GOOGLE_PAYMENT) {
                        if (data != null) {
                            switch (resultCode) {
                                case Activity.RESULT_OK:
                                    GooglePayment.tokenize(mBraintreeFragment,
                                            PaymentData.getFromIntent(data));
                                    break;
                                case Activity.RESULT_CANCELED:
                                    nonceErrorCallback("USER_CANCELLATION");
                                    break;
                            }
                        } else {
                            nonceErrorCallback("NO_DATA_AVAILABLE");
                        }
                    }
                }
            };

    @ReactMethod
    public void paypalRequest(final Callback successCallback,
                              final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
        PayPalRequest request = new PayPalRequest().currencyCode("USD").intent(
                PayPalRequest.INTENT_AUTHORIZE);
        PayPal.requestBillingAgreement(this.mBraintreeFragment, request);
    }

    @ReactMethod
    public void venmoRequest(final Callback successCallback,
                             final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
        Venmo.authorizeAccount(this.mBraintreeFragment, false);
    }

    @ReactMethod
    public void showGooglePayViewController(final ReadableMap options,
                                            final Callback successCallback,
                                            final Callback errorCallback) {
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
        GooglePaymentRequest googlePaymentRequest =
                new GooglePaymentRequest()
                        .transactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPrice(options.getString("totalPrice"))
                                        .setTotalPriceStatus(
                                                WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setCurrencyCode(options.getString("currencyCode"))
                                        .build())
                        .billingAddressRequired(options.getBoolean("requireAddress"));

        if (options.hasKey("googleMerchantId")) {
            googlePaymentRequest.googleMerchantId(
                    options.getString("googleMerchantId"));
        }
        GooglePayment.requestPayment(this.mBraintreeFragment, googlePaymentRequest);
    }

    public void onNewIntent(Intent intent) {
    }
}