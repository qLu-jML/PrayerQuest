package com.prayerquest.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        GroupPrayerItemCrossRef::class
    ],
    version = 1,
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
                .fallbackToDestructiveMigration()
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
    }
}
