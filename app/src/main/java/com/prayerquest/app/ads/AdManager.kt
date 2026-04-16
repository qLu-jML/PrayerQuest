package com.prayerquest.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    const val ADS_ENABLED = false  // flip for production
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"  // test
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"  // test

    private var interstitialAd: InterstitialAd? = null

    fun initialize(context: Context) {
        if (ADS_ENABLED) {
            MobileAds.initialize(context)
        }
    }

    fun loadInterstitial(context: Context) {
        if (!ADS_ENABLED) return

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        if (!ADS_ENABLED || interstitialAd == null) {
            onDismissed()
            return
        }

        interstitialAd?.fullScreenContentCallback =
            object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    onDismissed()
                }
            }

        interstitialAd?.show(activity)
    }
}
