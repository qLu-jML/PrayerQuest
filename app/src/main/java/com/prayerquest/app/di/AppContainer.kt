package com.prayerquest.app.di

import android.content.Context
import com.prayerquest.app.ads.AdManager
import com.prayerquest.app.billing.BillingManager
import com.prayerquest.app.billing.PremiumRepository
import com.prayerquest.app.data.database.PrayerQuestDatabase
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.data.devotional.SpurgeonDevotionalImporter
import com.prayerquest.app.data.prayer.FamousPrayerImporter
import com.prayerquest.app.data.prayer.SuggestedPrayerPackLoader
import com.prayerquest.app.data.repository.*
import com.prayerquest.app.firebase.FirebaseAuthManager
import com.prayerquest.app.firebase.FirestoreGroupService
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

    // Billing — the BillingManager owns the Play Billing client; the
    // PremiumRepository reconciles it with the DataStore mirror so workers
    // and offline UI can still see the entitlement.
    val billingManager: BillingManager = BillingManager(context)
    val premiumRepository: PremiumRepository = PremiumRepository(
        billingManager = billingManager,
        userPreferences = userPreferences,
        applicationScope = applicationScope,
    )

    // Firebase
    val firebaseAuthManager: FirebaseAuthManager = FirebaseAuthManager(context)
    val firestoreGroupService: FirestoreGroupService = FirestoreGroupService()

    // Repositories
    val prayerRepository: PrayerRepository = PrayerRepository(
        prayerItemDao = database.prayerItemDao(),
        prayerRecordDao = database.prayerRecordDao(),
        userPrayerProgressDao = database.userPrayerProgressDao(),
        famousPrayerDao = database.famousPrayerDao()
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
        groupDao = database.prayerGroupDao(),
        prayerItemDao = database.prayerItemDao(),
        authManager = firebaseAuthManager,
        firestoreService = firestoreGroupService
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

    val libraryRepository: LibraryRepository = LibraryRepository(
        famousPrayerDao = database.famousPrayerDao(),
        nameOfGodDao = database.nameOfGodDao()
    )

    val devotionalRepository: DevotionalRepository = DevotionalRepository(
        devotionalDao = database.devotionalDao()
    )

    val fastingRepository: FastingRepository = FastingRepository(
        fastingSessionDao = database.fastingSessionDao()
    )

    // Data loaders
    val famousPrayerImporter: FamousPrayerImporter = FamousPrayerImporter(
        context = context,
        famousPrayerDao = database.famousPrayerDao()
    )

    val suggestedPackLoader: SuggestedPrayerPackLoader = SuggestedPrayerPackLoader(
        context = context
    )

    val spurgeonDevotionalImporter: SpurgeonDevotionalImporter = SpurgeonDevotionalImporter(
        context = context,
        devotionalDao = database.devotionalDao()
    )

    init {
        // Seed singleton rows and import bundled content on first launch.
        // Both importers are idempotent, so re-running on every launch is a
        // no-op after the first successful completion.
        applicationScope.launch {
            progressRepository.ensureSeeded()
            famousPrayerImporter.importIfNeeded()
            spurgeonDevotionalImporter.importIfNeeded()
        }

        // Initialize ads (no-op if ADS_ENABLED = false).
        AdManager.initialize(context)

        // Connect to Play Billing, then start mirroring the entitlement into
        // DataStore so background workers and offline UI can read it.
        billingManager.startConnection()
        premiumRepository.startSync()

        // Schedule notifications. Pass our own `userPreferences` explicitly
        // because `application.container` is still being assigned at this
        // point — it is NOT safe for NotificationScheduler to look it up.
        NotificationScheduler.scheduleAllNotifications(context, userPreferences)
    }
}
