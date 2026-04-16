package com.prayerquest.app.di

import android.content.Context
import com.prayerquest.app.ads.AdManager
import com.prayerquest.app.billing.BillingManager
import com.prayerquest.app.data.database.PrayerQuestDatabase
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.data.prayer.FamousPrayerImporter
import com.prayerquest.app.data.prayer.SuggestedPrayerPackLoader
import com.prayerquest.app.data.repository.*
import com.prayerquest.app.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency injection container.
 * Same pattern as ScriptureQuest's AppContainer — held by PrayerQuestApplication.
 */
class AppContainer(context: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Database
    val database: PrayerQuestDatabase = PrayerQuestDatabase.getInstance(context, applicationScope)

    // User Preferences (DataStore)
    val userPreferences: UserPreferences = UserPreferences(context)

    // Billing
    val billingManager: BillingManager = BillingManager(context)

    // Repositories
    val prayerRepository: PrayerRepository = PrayerRepository(
        prayerItemDao = database.prayerItemDao(),
        prayerRecordDao = database.prayerRecordDao(),
        userPrayerProgressDao = database.userPrayerProgressDao()
    )

    val collectionRepository: CollectionRepository = CollectionRepository(
        collectionDao = database.prayerCollectionDao()
    )

    val progressRepository: ProgressRepository = ProgressRepository(
        userStatsDao = database.userStatsDao(),
        dailyActivityDao = database.dailyActivityDao()
    )

    val gratitudeRepository: GratitudeRepository = GratitudeRepository(
        gratitudeEntryDao = database.gratitudeEntryDao()
    )

    val prayerGroupRepository: PrayerGroupRepository = PrayerGroupRepository(
        groupDao = database.prayerGroupDao()
    )

    val gamificationRepository: GamificationRepository = GamificationRepository(
        userStatsDao = database.userStatsDao(),
        streakDao = database.streakDao(),
        dailyQuestDao = database.dailyQuestDao(),
        achievementProgressDao = database.achievementProgressDao(),
        dailyActivityDao = database.dailyActivityDao(),
        prayerRecordDao = database.prayerRecordDao(),
        gratitudeEntryDao = database.gratitudeEntryDao(),
        prayerGroupDao = database.prayerGroupDao()
    )

    // Data loaders
    val famousPrayerImporter: FamousPrayerImporter = FamousPrayerImporter(
        context = context,
        famousPrayerDao = database.famousPrayerDao()
    )

    val suggestedPackLoader: SuggestedPrayerPackLoader = SuggestedPrayerPackLoader(
        context = context
    )

    init {
        // Seed singleton rows and import famous prayers on first launch
        applicationScope.launch {
            progressRepository.ensureSeeded()
            famousPrayerImporter.importIfNeeded()
        }

        // Initialize ads (no-op if ADS_ENABLED = false)
        AdManager.initialize(context)

        // Initialize billing
        billingManager.initialize {}

        // Schedule notifications
        NotificationScheduler.scheduleAllNotifications(context)
    }
}
