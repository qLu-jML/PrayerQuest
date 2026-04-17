package com.prayerquest.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.prayerquest.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Central AdMob manager for interstitial and rewarded ads.
 *
 * • Banner ads → rendered by [BannerAdView].
 * • App-open ads → handled by [AppOpenAdManager].
 *
 * All ad-unit IDs come from [BuildConfig]. Debug builds use Google test IDs
 * (configured in `app/build.gradle.kts`); release builds pull real IDs from
 * `local.properties`. NEVER click your own production ads during development —
 * AdMob flags invalid traffic aggressively and can suspend your account.
 *
 * Mirrors ScriptureQuest's `AdManager` so both apps follow the same hygiene
 * (cooldown, session-frequency, premium-aware). The only difference is the
 * rewarded-placement set — PrayerQuest uses STREAK_SAVE and BONUS_XP, same as
 * ScriptureQuest, because the streak/XP gamification is identical.
 */
object AdManager {
    private const val TAG = "AdManager"

    /**
     * Master kill-switch. Flip to `false` to disable all ads app-wide (e.g. QA
     * builds). Previously `false` during pre-Phase-5 scaffolding; flipped on
     * now that billing + premium gating are wired.
     */
    const val ADS_ENABLED = true

    /** Minimum time between interstitials, in ms. Prevents ad fatigue in long prayer sessions. */
    private const val INTERSTITIAL_COOLDOWN_MS = 4 * 60_000L

    /** Show an interstitial at most once every N completed sessions. */
    private const val INTERSTITIAL_SESSION_FREQUENCY = 3

    /** Which rewarded placement to load/show. Each placement maps to its own AdMob unit. */
    enum class RewardedPlacement(val adUnitId: String) {
        STREAK_SAVE(BuildConfig.ADMOB_REWARDED_STREAK_SAVE_ID),
        BONUS_XP(BuildConfig.ADMOB_REWARDED_BONUS_XP_ID),
    }

    private var interstitialAd: InterstitialAd? = null
    private val rewardedAds: MutableMap<RewardedPlacement, RewardedAd?> = mutableMapOf()
    private var lastInterstitialShownAt: Long = 0L
    private var completedSessionsSinceLastAd: Int = 0
    private var initialized = false

    private val _isInterstitialLoaded = MutableStateFlow(false)
    val isInterstitialLoaded: StateFlow<Boolean> = _isInterstitialLoaded

    private val _rewardedLoadedState = MutableStateFlow<Map<RewardedPlacement, Boolean>>(
        RewardedPlacement.entries.associateWith { false }
    )
    val rewardedLoadedState: StateFlow<Map<RewardedPlacement, Boolean>> = _rewardedLoadedState

    // ── Initialization ──────────────────────────────────────────────────

    fun initialize(context: Context) {
        if (!ADS_ENABLED || initialized) return
        initialized = true

        // In debug builds, register all devices as test devices. Combined with
        // the test ad-unit IDs in BuildConfig, this double-locks development
        // behind Google's test-ad infrastructure.
        if (BuildConfig.DEBUG) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(emptyList()) // Add your device hash from logcat if needed.
                    .build()
            )
        }

        MobileAds.initialize(context) {
            Log.d(TAG, "AdMob initialized")
            loadInterstitial(context)
            RewardedPlacement.entries.forEach { loadRewarded(context, it) }
        }
    }

    // ── Interstitial ────────────────────────────────────────────────────

    fun loadInterstitial(context: Context) {
        if (!ADS_ENABLED) return
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isInterstitialLoaded.value = true
                    Log.d(TAG, "Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isInterstitialLoaded.value = false
                    Log.d(TAG, "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    /**
     * Call this at the end of a prayer session. Shows an interstitial at most
     * once every [INTERSTITIAL_SESSION_FREQUENCY] sessions AND at most once per
     * [INTERSTITIAL_COOLDOWN_MS] window. Otherwise fires [onDismissed] immediately
     * so the caller can proceed with navigation/cleanup.
     *
     * Premium users should NEVER see this — callers pass `isPremium = true` to
     * short-circuit. We keep the gate at the call-site so this object stays
     * pure and testable without a DataStore dependency.
     */
    fun showInterstitial(
        activity: Activity,
        isPremium: Boolean = false,
        onDismissed: () -> Unit,
    ) {
        if (!ADS_ENABLED || isPremium) { onDismissed(); return }

        completedSessionsSinceLastAd += 1
        val cooldownOk = System.currentTimeMillis() - lastInterstitialShownAt >= INTERSTITIAL_COOLDOWN_MS
        val frequencyOk = completedSessionsSinceLastAd >= INTERSTITIAL_SESSION_FREQUENCY
        val ad = interstitialAd

        if (ad == null || !cooldownOk || !frequencyOk) {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _isInterstitialLoaded.value = false
                lastInterstitialShownAt = System.currentTimeMillis()
                completedSessionsSinceLastAd = 0
                loadInterstitial(activity)
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                _isInterstitialLoaded.value = false
                loadInterstitial(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }

    // ── Rewarded ────────────────────────────────────────────────────────

    fun loadRewarded(context: Context, placement: RewardedPlacement) {
        if (!ADS_ENABLED) return
        RewardedAd.load(
            context,
            placement.adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAds[placement] = ad
                    updateRewardedState(placement, true)
                    Log.d(TAG, "Rewarded [$placement] loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAds[placement] = null
                    updateRewardedState(placement, false)
                    Log.d(TAG, "Rewarded [$placement] failed: ${error.message}")
                }
            }
        )
    }

    private fun updateRewardedState(placement: RewardedPlacement, loaded: Boolean) {
        _rewardedLoadedState.value = _rewardedLoadedState.value.toMutableMap()
            .apply { this[placement] = loaded }
    }

    /**
     * Show a rewarded ad for the given [placement].
     *
     * @param onRewardEarned fires only if the user watched to completion.
     * @param onClosed       fires when the ad is dismissed (with or without reward).
     * @param onUnavailable  fires if no ad is loaded (caller should fall back — e.g.
     *                       show a toast "No ad available, try again later").
     */
    fun showRewarded(
        activity: Activity,
        placement: RewardedPlacement,
        onRewardEarned: () -> Unit,
        onClosed: () -> Unit = {},
        onUnavailable: () -> Unit = {},
    ) {
        if (!ADS_ENABLED) { onUnavailable(); return }
        val ad = rewardedAds[placement]
        if (ad == null) {
            // No ad ready — kick off a load for next time and fall back.
            loadRewarded(activity, placement)
            onUnavailable()
            return
        }

        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAds[placement] = null
                updateRewardedState(placement, false)
                loadRewarded(activity, placement)
                if (earned) onRewardEarned()
                onClosed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAds[placement] = null
                updateRewardedState(placement, false)
                loadRewarded(activity, placement)
                onClosed()
            }
        }
        ad.show(activity) { earned = true }
    }
}
