package com.pw.droplet.braintree;

import android.content.Intent;
import android.util.Log;
import android.content.Context;
import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeError;

import com.braintreepayments.api.GooglePayClient;
import com.braintreepayments.api.GooglePayListener;
import com.braintreepayments.api.GooglePayOnActivityResultCallback;
import com.braintreepayments.api.GooglePayCardNonce;
import com.braintreepayments.api.GooglePayRequest;

import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PayPalDataCollectorRequest;
import com.braintreepayments.api.PayPalDataCollectorCallback;
import com.braintreepayments.api.PayPalListener;
import com.braintreepayments.api.PayPalDataCollector;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;

import com.braintreepayments.api.VenmoClient;
import com.braintreepayments.api.VenmoRequest;
import com.braintreepayments.api.VenmoListener;
import com.braintreepayments.api.VenmoPaymentMethodUsage;
import com.braintreepayments.api.VenmoOnActivityResultCallback;
import com.braintreepayments.api.VenmoAccountNonce;

import com.braintreepayments.api.DataCollectorRequest;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.DataCollectorCallback;

import com.braintreepayments.api.ErrorWithResponse;
import com.braintreepayments.api.InvalidArgumentException;
import com.braintreepayments.api.PaymentMethodNonce;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;

import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
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
  private Context mContext;
  private BraintreeClient braintreeClient;

  private GooglePayClient mGooglePayClient;
  private PayPalClient mPayPalClient;
  private VenmoClient mVenmoClient;

  private DataCollectorRequest googleDataCollectorRequest;
  private DataCollectorRequest venmoDataCollectorRequest;
  private DataCollector googleDataCollector;
  private DataCollector venmoDataCollector;
  private PayPalDataCollectorRequest payPalDataCollectorRequest;
  private PayPalDataCollector payPalDataCollector;

  private GooglePayListener googlePayListener;
  private VenmoListener venmoListener;
  private PayPalListener payPalListener;

  public Braintree(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addActivityEventListener(activityEventListener);
    mContext = reactContext;
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
    this.braintreeClient = new BraintreeClient(mContext, token);
    this.mGooglePayClient = new GooglePayClient((FragmentActivity) getCurrentActivity(), this.braintreeClient);
    this.mGooglePayClient.setListener((GooglePayListener) mContext);

    this.mPayPalClient = new PayPalClient(this.braintreeClient);
    this.mPayPalClient.setListener((PayPalListener) mContext);

    this.mVenmoClient = new VenmoClient((FragmentActivity) getCurrentActivity(), this.braintreeClient);
    this.mVenmoClient.setListener((VenmoListener) mContext);
  }

  public void onCancel(int requestCode) {
    nonceErrorCallback("USER_CANCELLATION");
  }

  public void onPaymentMethodNonceCreated(
    PaymentMethodNonce paymentMethodNonce) {

    if (paymentMethodNonce instanceof GooglePayCardNonce) {

      WritableMap payment = new WritableNativeMap();
      payment.putString("nonce", paymentMethodNonce.getString());
      WritableMap billingContact = new WritableNativeMap();

      billingContact.putString(
        "streetAddress",
        ((GooglePayCardNonce) paymentMethodNonce)
          .getBillingAddress()
          .getStreetAddress());
      billingContact.putString(
        "city", ((GooglePayCardNonce) paymentMethodNonce)
          .getBillingAddress()
          .getLocality());
      billingContact.putString(
        "state", ((GooglePayCardNonce) paymentMethodNonce)
          .getBillingAddress()
          .getRegion());
      billingContact.putString(
        "country", ((GooglePayCardNonce) paymentMethodNonce)
          .getBillingAddress()
          .getCountryCodeAlpha2());
      billingContact.putString(
        "postalCode", ((GooglePayCardNonce) paymentMethodNonce)
          .getBillingAddress()
          .getPostalCode());
      billingContact.putString(
        "countryCode", ((GooglePayCardNonce) paymentMethodNonce)
          .getBillingAddress()
          .getCountryCodeAlpha2());
      payment.putMap("billingContact", billingContact);
      objCallback(payment);

    } else {
      nonceCallback(paymentMethodNonce.getString());
    }
  }

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
      public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch (requestCode) {
          case BraintreeRequestCodes.GOOGLE_PAY:
            GooglePayOnActivityResultCallback googlePayOnActivityResultCallback = new GooglePayOnActivityResultCallback() {
              @Override
              public void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error) {
                if (paymentMethodNonce != null) {
                  googlePayListener.onGooglePaySuccess(paymentMethodNonce);
                } else if (error != null) {
                  googlePayListener.onGooglePayFailure(error);
                }
              }
            };
              mGooglePayClient.tokenize(PaymentData.getFromIntent(data), googlePayOnActivityResultCallback);
            break;
        }
      }
    };

  @ReactMethod
  public void paypalRequest(final Callback successCallback,
                            final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");
    request.setCurrencyCode("USD");
    request.setIntent(PayPalPaymentIntent.AUTHORIZE);
    this.mPayPalClient.tokenizePayPalAccount((FragmentActivity) getCurrentActivity(), request);
  }

  @ReactMethod
  public void venmoRequest(final Callback successCallback,
                           final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE);
    request.setShouldVault(false);

    this.mVenmoClient.tokenizeVenmoAccount((FragmentActivity) getCurrentActivity(), request);
  }

  @ReactMethod
  public void showGooglePayViewController(final ReadableMap options,
                                          final Callback successCallback,
                                          final Callback errorCallback) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;
    GooglePayRequest googlePayRequest = new GooglePayRequest();
    googlePayRequest.setTransactionInfo(
      TransactionInfo.newBuilder()
        .setTotalPrice(options.getString("totalPrice"))
        .setTotalPriceStatus(
          WalletConstants.TOTAL_PRICE_STATUS_FINAL)
        .setCurrencyCode(options.getString("currencyCode"))
        .build());
    googlePayRequest.setBillingAddressRequired(options.getBoolean("requireAddress"));
    this.mGooglePayClient.requestPayment((FragmentActivity) this.mContext, googlePayRequest);
  }

  @ReactMethod
  public void getDeviceData(final ReadableMap options,
                            final Callback successCallback,
                            final Callback errorCallback) {

    try {
      this.deviceDataCallback = successCallback;
      this.collectDeviceData = true;
      if (this.mPayPalClient instanceof PayPalClient) {
        payPalDataCollectorRequest = new PayPalDataCollectorRequest(false, null);
        PayPalDataCollectorCallback payPalDataCollectorCallback = new PayPalDataCollectorCallback() {
          @Override
          public void onResult(@Nullable String deviceData, @Nullable Exception error) {

                if (deviceData != null) {
                  successCallback.invoke(deviceData);
                } else {
                  errorCallback.invoke("BT:: DEVICE_DATA is empty.");
                }
          }
        };

        payPalDataCollector.collectDeviceData(this.mContext, payPalDataCollectorRequest,  payPalDataCollectorCallback);
      } else if(this.mGooglePayClient instanceof GooglePayClient) {
        googleDataCollectorRequest = new DataCollectorRequest(false);
        DataCollectorCallback googleDataCollectorCallback = new DataCollectorCallback() {
          @Override
          public void onResult(@Nullable String deviceData, @Nullable Exception error) {
            if (deviceData != null) {
              successCallback.invoke(deviceData);
            } else {
              errorCallback.invoke("BT:: DEVICE_DATA is empty.");
            }
          }
        };

        googleDataCollector.collectDeviceData(this.mContext, googleDataCollectorRequest,googleDataCollectorCallback);
      }  else if(this.mVenmoClient instanceof VenmoClient) {
        venmoDataCollectorRequest = new DataCollectorRequest(false);
        DataCollectorCallback venmoDataCollectorCallback = new DataCollectorCallback() {
          @Override
          public void onResult(@Nullable String deviceData, @Nullable Exception error) {
            if (deviceData != null) {
              successCallback.invoke(deviceData);
            } else {
              errorCallback.invoke("BT:: DEVICE_DATA is empty.");
            }
          }
        };

        venmoDataCollector.collectDeviceData(this.mContext, venmoDataCollectorRequest, venmoDataCollectorCallback);
      }
    }
    catch(Exception e) {
      errorCallback.invoke("BT:: DATA_COLLECTOR not init.");
    }
  }





  public void onNewIntent(Intent intent) {
  }
}
