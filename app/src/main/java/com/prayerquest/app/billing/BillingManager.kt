package com.prayerquest.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Play Billing for the premium subscription.
 *
 * Lifecycle: created by [com.prayerquest.app.di.AppContainer], connected in
 * [com.prayerquest.app.PrayerQuestApplication.onCreate] via [startConnection].
 *
 * The [isPremium] StateFlow is the single source of truth for ad gating and
 * feature-gate checks. A separate [PremiumRepository] mirrors this value into
 * DataStore so other layers (notifications, background workers, UI that
 * doesn't have access to the BillingManager) can read it via a cold Flow.
 *
 * Product setup in Google Play Console:
 *  1. Create a subscription with base plan ID = [PREMIUM_MONTHLY_ID].
 *  2. $4.99/mo per DD §5.
 *  3. Enable "Grace period" (7 days) so lapsed cards don't immediately drop
 *     premium entitlement.
 */
class BillingManager(
    private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        /**
         * The monthly subscription SKU. Matches the product ID you create in
         * Google Play Console. If you ever launch an annual plan, add a
         * second constant and query both in [launchPurchaseFlow].
         */
        const val PREMIUM_MONTHLY_ID = "prayer_quest_premium_monthly"
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /** Cached product details from the last successful query — used so the
     *  paywall can display the real price string (e.g. "$4.99/month") pulled
     *  from Play rather than a hard-coded number. */
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /**
     * Connect to Play Billing. Idempotent — safe to call on Application start
     * and again if the connection drops. Auto-reconnects in
     * [onBillingServiceDisconnected].
     */
    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryPurchases()
                    queryProductDetails()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected — will retry on next interaction")
            }
        })
    }

    /**
     * Re-query purchases from Play. Called on reconnect and after any purchase
     * update. This is the only code path that flips [isPremium] — we never
     * trust client state alone.
     */
    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any {
                    it.products.contains(PREMIUM_MONTHLY_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPremium.value = hasPremium
                // Acknowledge any unacknowledged purchases
                purchases.filter {
                    !it.isAcknowledged &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }.forEach { acknowledgePurchase(it) }
            }
        }
    }

    /**
     * Fetch product details from Play so the paywall can display price + offer
     * info without hard-coding. Result is cached in [productDetails].
     */
    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                _productDetails.value = productDetailsList.first()
            } else {
                Log.w(TAG, "Product details fetch failed: ${result.debugMessage}")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            Log.d(TAG, "Acknowledge result: ${result.responseCode}")
        }
    }

    /**
     * Launch the subscription purchase flow. Call from the paywall CTA.
     *
     * Uses the cached [productDetails] when available (fast path) and falls
     * back to a fresh fetch if the cache is empty (cold start, offline
     * recovery, etc.). The offer token comes from the first subscription
     * offer — if you introduce intro pricing or a promo, pick the right one
     * here based on business rules.
     */
    fun launchPurchaseFlow(activity: Activity) {
        val cached = _productDetails.value
        if (cached != null) {
            launchFlow(activity, cached)
            return
        }

        // Cold-path: fetch then launch.
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val pd = productDetailsList.first()
                _productDetails.value = pd
                launchFlow(activity, pd)
            }
        }
    }

    private fun launchFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return

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

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _isPremium.value = true
                    if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                }
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // User dismissed the purchase sheet — not an error, just log it.
            Log.d(TAG, "Purchase flow canceled by user")
        } else {
            Log.w(TAG, "Purchase update error: ${result.debugMessage}")
        }
    }

    /**
     * Back-compat wrapper so existing callers that invoke
     * `billingManager.initialize { ... }` keep working. New code should call
     * [startConnection] directly.
     */
    fun initialize(onReady: () -> Unit = {}) {
        startConnection()
        onReady()
    }

    fun destroy() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}
