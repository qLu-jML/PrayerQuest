package com.prayerquest.app

import android.app.Application
import com.prayerquest.app.di.AppContainer

/**
 * Application class — holds the AppContainer for manual DI.
 * Same pattern as ScriptureQuest's ScriptureQuestApplication.
 */
class PrayerQuestApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
