package com.prayerquest.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prayerquest.app.data.dao.*
import com.prayerquest.app.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PrayerItem::class,
        PrayerCollection::class,
        PrayerCollectionCrossRef::class,
        FamousPrayer::class,
        PrayerRecord::class,
        UserPrayerProgress::class,
        UserStats::class,
        StreakData::class,
        DailyQuest::class,
        AchievementProgress::class,
        DailyActivity::class,
        GratitudeEntry::class,
        PrayerGroup::class,
        PrayerGroupMember::class,
        GroupPrayerItem::class,
        GroupPrayerItemCrossRef::class,
        GroupPrayerActivity::class,
        NameOfGod::class,
        Devotional::class,
        FastingSession::class
    ],
    version = 6,
    exportSchema = true
)
abstract class PrayerQuestDatabase : RoomDatabase() {

    // DAOs
    abstract fun prayerItemDao(): PrayerItemDao
    abstract fun prayerCollectionDao(): PrayerCollectionDao
    abstract fun famousPrayerDao(): FamousPrayerDao
    abstract fun prayerRecordDao(): PrayerRecordDao
    abstract fun userPrayerProgressDao(): UserPrayerProgressDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun streakDao(): StreakDao
    abstract fun dailyQuestDao(): DailyQuestDao
    abstract fun achievementProgressDao(): AchievementProgressDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun gratitudeEntryDao(): GratitudeEntryDao
    abstract fun prayerGroupDao(): PrayerGroupDao
    abstract fun nameOfGodDao(): NameOfGodDao
    abstract fun devotionalDao(): DevotionalDao
    abstract fun fastingSessionDao(): FastingSessionDao

    companion object {
        private const val DATABASE_NAME = "prayer_quest_db"

        @Volatile
        private var INSTANCE: PrayerQuestDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): PrayerQuestDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, scope).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, scope: CoroutineScope): PrayerQuestDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PrayerQuestDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(SeedCallback(scope))
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                // Dev-phase policy (matches ScriptureQuest): any schema gap we
                // haven't written an explicit migration for wipes and rebuilds.
                // MIGRATION_4_5 still runs for v4 devices; pre-v4 installs
                // (e.g. dev devices left at v2/v3) drop cleanly. Before launch
                // this must either be removed or paired with full migration
                // coverage from every released schema version.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        /**
         * Seeds singleton rows (UserStats id=1, StreakData id=1) on first database creation.
         * Uses INSERT OR IGNORE so it's safe to re-run.
         */
        private class SeedCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        database.userStatsDao().insert(UserStats())
                        database.streakDao().insert(StreakData())
                    }
                }
            }
        }

        /**
         * v4 → v5 — Sprint 1 schema refactor (2026-04-16):
         *
         *   1. Hearts + freezes move from `user_stats` to `streak_data`.
         *      Values are copied across, and `user_stats` is recreated
         *      without those columns (SQLite can't DROP COLUMN pre-3.35,
         *      and minSdk = 24 targets older SQLite builds, so we go the
         *      safe "create-new / copy / drop / rename" route).
         *   2. New tables added: `name_of_god`, `devotional`, `fasting_session`.
         *
         * Column types + defaults here must match what Room generates
         * for the current entity classes — keep them in sync.
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ── 1. Add hearts/freezes to streak_data ───────────────
                db.execSQL(
                    "ALTER TABLE streak_data ADD COLUMN hearts INTEGER NOT NULL DEFAULT 3"
                )
                db.execSQL(
                    "ALTER TABLE streak_data ADD COLUMN freezes INTEGER NOT NULL DEFAULT 0"
                )

                // Copy existing values from user_stats into streak_data (singleton id=1).
                db.execSQL(
                    """
                    UPDATE streak_data
                    SET hearts   = COALESCE((SELECT hearts   FROM user_stats WHERE id = 1), 3),
                        freezes  = COALESCE((SELECT freezes  FROM user_stats WHERE id = 1), 0)
                    WHERE id = 1
                    """.trimIndent()
                )

                // ── 2. Recreate user_stats without hearts/freezes ──────
                db.execSQL(
                    """
                    CREATE TABLE user_stats_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        totalXp INTEGER NOT NULL DEFAULT 0,
                        level INTEGER NOT NULL DEFAULT 1,
                        graceCoins INTEGER NOT NULL DEFAULT 0,
                        totalPrayerMinutes INTEGER NOT NULL DEFAULT 0,
                        totalSessions INTEGER NOT NULL DEFAULT 0,
                        totalItemsPrayed INTEGER NOT NULL DEFAULT 0,
                        answeredPrayerCount INTEGER NOT NULL DEFAULT 0,
                        totalGratitudesLogged INTEGER NOT NULL DEFAULT 0,
                        totalGratitudePhotos INTEGER NOT NULL DEFAULT 0,
                        consecutiveGratitudeDays INTEGER NOT NULL DEFAULT 0,
                        totalGroupPrayersLogged INTEGER NOT NULL DEFAULT 0,
                        totalGroupsJoined INTEGER NOT NULL DEFAULT 0,
                        totalGroupsCreated INTEGER NOT NULL DEFAULT 0,
                        totalFamousPrayersSaid INTEGER NOT NULL DEFAULT 0,
                        longestSessionMinutes INTEGER NOT NULL DEFAULT 0,
                        totalDistinctModesUsed INTEGER NOT NULL DEFAULT 0,
                        consecutiveQuestDays INTEGER NOT NULL DEFAULT 0,
                        totalQuestDaysCompleted INTEGER NOT NULL DEFAULT 0,
                        hasEarlyBirdSession INTEGER NOT NULL DEFAULT 0,
                        hasNightOwlSession INTEGER NOT NULL DEFAULT 0,
                        hasComebackAfterGap INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO user_stats_new (
                        id, totalXp, level, graceCoins,
                        totalPrayerMinutes, totalSessions, totalItemsPrayed,
                        answeredPrayerCount,
                        totalGratitudesLogged, totalGratitudePhotos, consecutiveGratitudeDays,
                        totalGroupPrayersLogged, totalGroupsJoined, totalGroupsCreated,
                        totalFamousPrayersSaid,
                        longestSessionMinutes, totalDistinctModesUsed,
                        consecutiveQuestDays, totalQuestDaysCompleted,
                        hasEarlyBirdSession, hasNightOwlSession, hasComebackAfterGap
                    )
                    SELECT
                        id, totalXp, level, graceCoins,
                        totalPrayerMinutes, totalSessions, totalItemsPrayed,
                        answeredPrayerCount,
                        totalGratitudesLogged, totalGratitudePhotos, consecutiveGratitudeDays,
                        totalGroupPrayersLogged, totalGroupsJoined, totalGroupsCreated,
                        totalFamousPrayersSaid,
                        longestSessionMinutes, totalDistinctModesUsed,
                        consecutiveQuestDays, totalQuestDaysCompleted,
                        hasEarlyBirdSession, hasNightOwlSession, hasComebackAfterGap
                    FROM user_stats
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE user_stats")
                db.execSQL("ALTER TABLE user_stats_new RENAME TO user_stats")

                // ── 3. New tables for Sprint 1 ─────────────────────────
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS name_of_god (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        hebrewOrGreek TEXT NOT NULL DEFAULT '',
                        meaning TEXT NOT NULL,
                        scriptureReference TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        userPrayedCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS devotional (
                        date TEXT NOT NULL PRIMARY KEY,
                        author TEXT NOT NULL,
                        source TEXT NOT NULL DEFAULT '',
                        title TEXT NOT NULL,
                        scriptureReference TEXT NOT NULL DEFAULT '',
                        passage TEXT NOT NULL,
                        readCount INTEGER NOT NULL DEFAULT 0,
                        lastReadAt INTEGER
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fasting_session (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        startDate TEXT NOT NULL,
                        endDate TEXT,
                        plannedDurationHours INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        intention TEXT NOT NULL,
                        linkedPrayerItemIds TEXT NOT NULL DEFAULT '',
                        journalEntries TEXT NOT NULL DEFAULT '',
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v5 → v6 — Spurgeon evening readings (2026-04-16).
         *
         * Adds three columns to `devotional` for the evening slot. All three
         * default to empty string so legacy rows inserted by v5 (morning-only
         * Spurgeon JSON) stay valid; the Morning-and-Evening importer fills
         * them in on next launch. The DailyDevotionalWorker treats a blank
         * eveningPassage as "no evening content for this day — skip the
         * evening notification."
         */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE devotional ADD COLUMN eveningTitle TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE devotional ADD COLUMN eveningScriptureReference TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE devotional ADD COLUMN eveningPassage TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
