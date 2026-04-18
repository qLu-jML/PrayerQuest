package com.prayerquest.app.di

import android.content.Context
import com.prayerquest.app.ads.AdManager
import com.prayerquest.app.billing.BillingManager
import com.prayerquest.app.billing.PremiumRepository
import com.prayerquest.app.data.database.PrayerQuestDatabase
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.data.prayer.BiblePrayerImporter
import com.prayerquest.app.data.prayer.FamousPrayerImporter
import com.prayerquest.app.data.prayer.NameOfGodImporter
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
        famousPrayerDao = database.famousPrayerDao(),
        biblePrayerDao = database.biblePrayerDao()
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
        prayerGroupDao = database.prayerGroupDao(),
        // Back the Sprint-18 derived badge categories (TESTIMONY /
        // PARTIAL_ANSWER / NAMES_OF_GOD / FAMOUS_DISTINCT).
        prayerItemDao = database.prayerItemDao(),
        nameOfGodDao = database.nameOfGodDao(),
        famousPrayerDao = database.famousPrayerDao()
    )

    val libraryRepository: LibraryRepository = LibraryRepository(
        famousPrayerDao = database.famousPrayerDao(),
        nameOfGodDao = database.nameOfGodDao()
    )

    val fastingRepository: FastingRepository = FastingRepository(
        fastingSessionDao = database.fastingSessionDao()
    )

    // Tracks total on-device photo count across Photo Prayers, Gratitude,
    // and Answered-Prayer Testimonies for the 200-photo free-tier soft cap.
    val photoCountRepository: PhotoCountRepository = PhotoCountRepository(context)

    // Data loaders
    val famousPrayerImporter: FamousPrayerImporter = FamousPrayerImporter(
        context = context,
        famousPrayerDao = database.famousPrayerDao()
    )

    val biblePrayerImporter: BiblePrayerImporter = BiblePrayerImporter(
        context = context,
        biblePrayerDao = database.biblePrayerDao()
    )

    val nameOfGodImporter: NameOfGodImporter = NameOfGodImporter(
        context = context,
        nameOfGodDao = database.nameOfGodDao()
    )

    val suggestedPackLoader: SuggestedPrayerPackLoader = SuggestedPrayerPackLoader(
        context = context
    )

    init {
        // Seed singleton rows and import bundled content on first launch.
        // Both importers are idempotent, so re-running on every launch is a
        // no-op after the first successful completion.
        applicationScope.launch {
            progressRepository.ensureSeeded()
            famousPrayerImporter.importIfNeeded()
            biblePrayerImporter.importIfNeeded()
            // Names of God (DD §3.12). Importer is idempotent — after the
            // first successful seed every subsequent launch is a single
            // count() query that returns early.
            nameOfGodImporter.importIfNeeded()
            // Seed the photo-count StateFlow from disk so the cap check is
            // accurate on first photo-picker open without waiting for a
            // user action to trigger a refresh.
            photoCountRepository.refresh()
        }

        // Attach the Prayer Groups cloud→Room mirror at application scope.
        //
        // Previously this was tied to PrayerGroupsViewModel.viewModelScope,
        // which meant listeners didn't attach until the user actually
        // navigated to the Groups screen. The visible symptom was "I signed
        // in but my groups don't appear until I force something to happen"
        // — the mirror only started on screen entry, and if the user signed
        // in somewhere else first (Settings, or was already signed in at
        // launch) the Groups list rendered empty before the first snapshot
        // landed. Moving it here means Room is always being hydrated as
        // long as the user is signed in, so the Groups screen's observing
        // Flow<> reads from already-populated data on first composition.
        //
        // The mirror is a no-op while signed out (its authState collector
        // only attaches nested listeners on SignedIn), so there is no cost
        // for users who never enable sync.
        prayerGroupRepository.startCloudMirror(applicationScope)

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
