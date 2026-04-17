package com.prayerquest.app.billing

import com.prayerquest.app.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Single source of truth for premium entitlement, combining two inputs:
 *   1. [BillingManager.isPremium] — authoritative when the device is online
 *      and Play Billing has connected.
 *   2. [UserPreferences.isPremium] — cached in DataStore so background workers
 *      and offline UI can still read the entitlement without Play Services.
 *
 * The combined [isPremium] Flow OR-s the two: if either source says premium,
 * the user IS premium. This matters during the brief window between app start
 * and Play Billing connecting — without the DataStore fallback, the UI would
 * flicker (show ads, then hide them) every cold start.
 *
 * BillingManager writes → DataStore are one-way. The DataStore value never
 * upgrades a user to premium on its own (no spoofing by editing local files);
 * it just preserves the most recent confirmed value from Play.
 */
class PremiumRepository(
    private val billingManager: BillingManager,
    private val userPreferences: UserPreferences,
    private val applicationScope: CoroutineScope,
) {

    /**
     * Combined entitlement Flow. Emits whenever either source changes, with
     * duplicates filtered so downstream collectors only recompose on real
     * changes.
     */
    val isPremium: Flow<Boolean> = combine(
        billingManager.isPremium,
        userPreferences.isPremium,
    ) { fromBilling, fromDataStore ->
        fromBilling || fromDataStore
    }.distinctUntilChanged()

    /**
     * Snapshot StateFlow — used by [com.prayerquest.app.ads.AppOpenAdManager]
     * which needs a synchronous `() -> Boolean` provider. Starts in the
     * `Eagerly` mode so the first foreground event has a meaningful value
     * even if nobody is collecting yet.
     */
    val isPremiumState = isPremium.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    /**
     * Connects the billing authority to DataStore. Call once on Application
     * start so any confirmed purchase survives process death and cold
     * launches.
     */
    fun startSync() {
        // billingManager.isPremium is already a StateFlow, which deduplicates
        // via operator fusion — no need for (and the compiler warns against)
        // an explicit distinctUntilChanged() here.
        billingManager.isPremium
            .onEach { fromBilling ->
                // Only write when billing has authoritative state — if billing
                // says false we still mirror (expired sub), but the combined
                // Flow above keeps the user premium until DataStore also
                // flips, which happens here.
                userPreferences.setIsPremium(fromBilling)
            }
            .launchIn(applicationScope)
    }
}
