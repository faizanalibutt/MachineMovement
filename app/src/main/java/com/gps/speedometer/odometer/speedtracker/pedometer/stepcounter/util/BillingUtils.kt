package com.dev.bytes.adsmanager.billing

import android.app.Activity
import android.content.Context
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.BillingProcessor.IBillingHandler
import com.anjlab.android.iab.v3.TransactionDetails
import com.dev.bytes.R
import com.dev.bytes.adsmanager.TinyDB
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.BuildConfig
import org.jetbrains.anko.longToast
import timber.log.Timber

//const val INAPP_API_KEY =
//    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiH9zQO3vaczPyEDRI72n6CVDLjjamkYSSWhAKDWXOAyyVW8Aoqv2JiaK7In7m5ZK8/W08IoA91YgE/KSO1BEEw9t6vGbHrb6isdwTCw55iq2GFDyUwxCqySzDZF3h5n6gzmyonqheZ7pyq5oxk30hKCo6MVJh/uneJtUgpvl8qiCxLNUSrRgHhks5hvk5V1UqCajjG4BY4nIyZSvV8S6q9gw+a1If6Nono66Z2dEX0VPKgCPyqn+PAqdfOvFB/FIgTUNWyw3Y0BQfoPPLl1NkT9EJ9FO0hROwNr8xyKFDv1cIcRNvN2M1mtRQrGwbNqrXgcWDEmEFMGkr5X+aZy5XwIDAQAB"

const val TEST_PURCHASE_KEY = /*"android.test.canceled"*/"android.test.purchased"
val productKey = if (BuildConfig.DEBUG) TEST_PURCHASE_KEY else "remove_ads"

//val Context.productKey: String
//    get() = this.getString(R.string.remove_ads_key)

fun Context.setPremium() = TinyDB(this).putBoolean(getString(R.string.is_premium), true)

fun Context.initBilling(
    INAPP_API_KEY: String,
    onPurchased: (() -> Unit)? = null,
    onPurchasesRestored: (() -> Unit)? = null
) =
    BillingProcessor(this, INAPP_API_KEY, object : IBillingHandler {
        override fun onProductPurchased(productId: String, details: TransactionDetails?) {
            Timber.e("onProductPurchased $productId")
            if (productId == productKey) {
                setPremium()
//                startMainActivity(this@initBilling)
                onPurchased?.invoke()
            }
        }

        override fun onPurchaseHistoryRestored() {
            Timber.e("onPurchaseHistoryRestored")
            onPurchasesRestored?.invoke()
        }

        override fun onBillingError(errorCode: Int, error: Throwable?) =
            Timber.e("onBillingError ${error?.message} $errorCode")

        override fun onBillingInitialized() {
            Timber.e("onBillingInitialized")
            onPurchasesRestored?.invoke()
        }
    }).apply {
        initialize()
        isInitialized && isOneTimePurchaseSupported
    }

fun BillingProcessor.purchaseRemoveAds(activity: Activity) =
    if (isInitialized && isOneTimePurchaseSupported)
        purchase(activity, productKey)
    else {
        activity.longToast("Service initialization failed.. Please try again!")
        false
    }