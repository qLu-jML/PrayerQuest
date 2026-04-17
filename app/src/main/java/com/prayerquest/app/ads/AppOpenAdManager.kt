package com.prayerquest.app.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.prayerquest.app.BuildConfig
import java.util.Date

/**
 * Shows an app-open ad when the user foregrounds the app (cold launch or warm resume).
 *
 * Design decisions:
 * • Skips the very first onStart so splash/onboarding doesn't get covered.
 * • Enforces a [MIN_GAP_MS] cooldown so quick task-switches don't trigger ads.
 * • Expires cached ads after [AD_EXPIRY_MS] per Google's policy.
 * • Honors the [isPremiumProvider] — premium users bypass entirely.
 *
 * Wired from [com.prayerquest.app.PrayerQuestApplication] via [register].
 * Mirrors ScriptureQuest's `AppOpenAdManager` one-to-one, just with a Premium
 * gate added so this doesn't show to paying users.
 */
class AppOpenAdManager private constructor(
    private val application: Application,
    private val isPremiumProvider: () -> Boolean,
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd: Boolean = false
    private var isLoadingAd: Boolean = false
    private var loadedAt: Long = 0L
    private var lastShownAt: Long = 0L
    private var currentActivity: Activity? = null
    private var hasSkippedFirst: Boolean = false  // skip the initial cold-launch show

    companion object {
        private const val TAG = "AppOpenAd"

        /** Google-required: cached app-open ads expire after 4 hours. */
        private const val AD_EXPIRY_MS = 4 * 60 * 60 * 1000L

        /** Minimum gap between shows — prevents ad on quick task-switches. */
        private const val MIN_GAP_MS = 4 * 60_000L

        @Volatile private var instance: AppOpenAdManager? = null

        fun register(application: Application, isPremiumProvider: () -> Boolean) {
            if (!AdManager.ADS_ENABLED) return
            if (instance != null) return
            val mgr = AppOpenAdManager(application, isPremiumProvider)
            instance = mgr
            application.registerActivityLifecycleCallbacks(mgr)
            ProcessLifecycleOwner.get().lifecycle.addObserver(mgr)
            // Kick off the first preload so by the next foreground we have an ad ready.
            mgr.loadAd()
        }
    }

    // ── ProcessLifecycle: fires when app moves to foreground ──────────────

    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { showIfAvailable(it) }
    }

    // ── Ad load / show ────────────────────────────────────────────────────

    private fun loadAd() {
        if (isLoadingAd || isAvailable()) return
        if (isPremiumProvider()) return  // don't even waste the network call
        isLoadingAd = true

        AppOpenAd.load(
            application,
            BuildConfig.ADMOB_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadedAt = Date().time
                    Log.d(TAG, "App-open ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    Log.d(TAG, "App-open ad failed: ${error.message}")
                }
            }
        )
    }

    private fun isAvailable(): Boolean {
        val fresh = Date().time - loadedAt < AD_EXPIRY_MS
        return appOpenAd != null && fresh
    }

    private fun showIfAvailable(activity: Activity) {
        if (isShowingAd) return
        if (isPremiumProvider()) return

        // Skip the very first foreground event so we never cover splash/onboarding.
        if (!hasSkippedFirst) {
            hasSkippedFirst = true
            loadAd()
            return
        }

        // Enforce cooldown — no ad on quick task-switches.
        if (Date().time - lastShownAt < MIN_GAP_MS) return

        if (!isAvailable()) { loadAd(); return }

        val ad = appOpenAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                lastShownAt = Date().time
            }
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }
        }
        ad.show(activity)
    }

    // ── ActivityLifecycleCallbacks (track the foreground activity) ────────

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }
}
