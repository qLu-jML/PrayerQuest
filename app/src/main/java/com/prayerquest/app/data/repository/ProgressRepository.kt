package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.DailyActivityDao
import com.prayerquest.app.data.dao.UserStatsDao
import com.prayerquest.app.data.entity.DailyActivity
import com.prayerquest.app.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

/**
 * User stats and daily activity tracking.
 */
class ProgressRepository(
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao
) {

    // --- UserStats (singleton) ---
    fun observeStats(): Flow<UserStats?> = userStatsDao.observe()
    suspend fun getStats(): UserStats = userStatsDao.get() ?: UserStats()
    suspend fun ensureSeeded() {
        if (userStatsDao.get() == null) {
            userStatsDao.insert(UserStats())
        }
    }

    suspend fun addXp(xp: Int, newLevel: Int) = userStatsDao.addXpAndSetLevel(xp, newLevel)
    suspend fun addCoins(coins: Int) = userStatsDao.addCoins(coins)
    suspend fun addSessionStats(minutes: Int) = userStatsDao.addSessionStats(minutes)
    suspend fun incrementAnsweredPrayers() = userStatsDao.incrementAnsweredPrayers()
    suspend fun addGratitudes(count: Int) = userStatsDao.addGratitudes(count)
    suspend fun incrementGratitudePhotos() = userStatsDao.incrementGratitudePhotos()
    suspend fun incrementGroupPrayers() = userStatsDao.incrementGroupPrayers()
    suspend fun incrementFamousPrayers() = userStatsDao.incrementFamousPrayers()
    suspend fun setHearts(hearts: Int) = userStatsDao.setHearts(hearts)
    suspend fun setFreezes(freezes: Int) = userStatsDao.setFreezes(freezes)

    // --- DailyActivity ---
    fun observeActivity(date: String): Flow<DailyActivity?> = dailyActivityDao.observeForDate(date)
    fun observeRecentActivity(limit: Int = 30): Flow<List<DailyActivity>> = dailyActivityDao.observeRecent(limit)
    fun observeActivityRange(start: String, end: String): Flow<List<DailyActivity>> =
        dailyActivityDao.observeRange(start, end)

    suspend fun getOrCreateActivity(date: String): DailyActivity {
        return dailyActivityDao.getForDate(date) ?: DailyActivity(date = date).also {
            dailyActivityDao.upsert(it)
        }
    }

    suspend fun updateActivity(activity: DailyActivity) = dailyActivityDao.upsert(activity)
}
