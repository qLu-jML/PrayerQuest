package com.prayerquest.app.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prayerquest.app.billing.BillingManager
import com.prayerquest.app.di.AppContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ViewModel for the premium paywall. Wraps [BillingManager] so the screen
 * doesn't need to know about Play Billing's product details shape — it just
 * reads a formatted price string and fires one callback to launch the flow.
 */
class PremiumPaywallViewModel(
    private val billingManager: BillingManager,
) : ViewModel() {

    /**
     * Formatted per-period price string from Play, e.g. "$4.99/month" for the
     * user's locale and currency. Null while Play is still fetching — the UI
     * falls back to a generic "Upgrade to Premium" label until it arrives.
     */
    val priceText: Flow<String?> = billingManager.productDetails.map { details ->
        val phase = details
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
        phase?.formattedPrice
    }

    fun launchPurchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PremiumPaywallViewModel(container.billingManager) as T
    }
}
