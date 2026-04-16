package com.prayerquest.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {
    companion object {
        const val PREMIUM_PRODUCT_ID = "prayer_quest_premium"
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private lateinit var billingClient: BillingClient

    fun initialize(onReady: () -> Unit) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryAndRestorePurchases()
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to reconnect
                initialize(onReady)
            }
        })
    }

    private fun queryAndRestorePurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val isPremiumUser = purchases.any { purchase ->
                    purchase.products.contains(PREMIUM_PRODUCT_ID) && purchase.isAcknowledged
                }
                _isPremium.value = isPremiumUser

                // Auto-acknowledge any unacknowledged purchases
                purchases.forEach { purchase ->
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                    }
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val productDetailsParams = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .addProduct(productDetailsParams)
                .build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                if (subscriptionOfferDetails != null && subscriptionOfferDetails.isNotEmpty()) {
                    val offerToken = subscriptionOfferDetails[0].offerToken
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        )
                        .build()
                    billingClient.launchBillingFlow(activity, flowParams)
                }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: com.android.billingclient.api.BillingResult,
        purchases: MutableList<com.android.billingclient.api.Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PREMIUM_PRODUCT_ID)) {
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { _ ->
                            _isPremium.value = true
                        }
                    } else {
                        _isPremium.value = true
                    }
                }
            }
        }
    }

    fun disconnect() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}
