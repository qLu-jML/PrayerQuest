package com.prayerquest.app

import android.app.Application
import com.prayerquest.app.ads.AppOpenAdManager
import com.prayerquest.app.di.AppContainer

/**
 * Application class — holds the AppContainer for manual DI.
 * Same pattern as ScriptureQuest's ScriptureQuestApplication.
 *
 * Also registers the app-open ad lifecycle watcher here — it needs the
 * Application object and a stable provider for the premium entitlement flag,
 * both of which are only available after [onCreate] has constructed the
 * container.
 */
class PrayerQuestApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Hook up the app-open ad. No-op if ads are disabled or when the user
        // is premium (the provider returns the live StateFlow value, so a
        // mid-session upgrade immediately silences app-open ads on the next
        // foreground event).
        AppOpenAdManager.register(this) {
            container.premiumRepository.isPremiumState.value
        }
    }
}
