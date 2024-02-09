"use strict";

import { NativeModules, processColor, Platform } from "react-native";
import { mapParameters } from "./utils";

const Braintree = NativeModules.Braintree;

module.exports = {
  setupWithURLScheme(token, urlscheme) {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "ios") {
        Braintree.setupWithURLScheme(token, urlscheme, function (success) {
          success == true ? resolve(true) : reject("Invalid Token");
        });
      } else {
        reject("setupWithURLScheme is only available on ios devices");
      }
    });
  },
  setup(token) {
    return new Promise(function (resolve, reject) {
      Braintree.setup(
        token,
        (test) => resolve(test),
        (err) => reject(err)
      );
    });
  },
  getDeviceData(options = {}) {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "ios") {
        Braintree.getDeviceData(options, function (err, deviceData) {
          deviceData != null ? resolve(deviceData) : reject(err);
        });
      } else {
        resolve(null);
      }
    });
  },
  showPayPalViewController() {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "ios") {
        Braintree.showPayPalViewController(function (err, nonce) {
          nonce != null ? resolve(nonce) : reject(err);
        });
      } else {
        Braintree.paypalRequest(
          (nonce) => resolve(nonce),
          (error) => reject(error)
        );
      }
    });
  },
  showApplePayViewController(options = {}) {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "ios") {
        Braintree.showApplePayViewController(options, function (err, nonce) {
          nonce != null ? resolve(nonce) : reject(err);
        });
      } else {
        reject("showApplePayViewController is only available on ios devices");
      }
    });
  },
  showVenmoViewController() {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "ios") {
        Braintree.showVenmoViewController(function (err, nonce) {
          nonce != null ? resolve(nonce) : reject(err);
        });
      } else {
        Braintree.venmoRequest(
          (nonce) => resolve(nonce),
          (error) => reject(error)
        );
      }
    });
  },
  showGooglePayViewController(options = {}) {
    return new Promise(function (resolve, reject) {
      if (Platform.OS === "android") {
        Braintree.showGooglePayViewController(
          options,
          (nonce) => resolve(nonce),
          (error) => reject(error)
        );
      } else {
        reject("showGooglePayViewController is only available on ios devices");
      }
    });
  },
};
