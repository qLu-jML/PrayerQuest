package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stats: UserStats)

    @Update
    suspend fun update(stats: UserStats)

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun get(): UserStats?

    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun observe(): Flow<UserStats?>

    // Atomic increment helpers — avoids read-modify-write races
    @Query("UPDATE user_stats SET totalXp = totalXp + :xp, level = :newLevel WHERE id = 1")
    suspend fun addXpAndSetLevel(xp: Int, newLevel: Int)

    @Query("UPDATE user_stats SET graceCoins = graceCoins + :coins WHERE id = 1")
    suspend fun addCoins(coins: Int)

    @Query("UPDATE user_stats SET totalPrayerMinutes = totalPrayerMinutes + :minutes, totalSessions = totalSessions + 1 WHERE id = 1")
    suspend fun addSessionStats(minutes: Int)

    @Query("UPDATE user_stats SET answeredPrayerCount = answeredPrayerCount + 1 WHERE id = 1")
    suspend fun incrementAnsweredPrayers()

    @Query("UPDATE user_stats SET totalGratitudesLogged = totalGratitudesLogged + :count WHERE id = 1")
    suspend fun addGratitudes(count: Int)

    @Query("UPDATE user_stats SET totalGratitudePhotos = totalGratitudePhotos + 1 WHERE id = 1")
    suspend fun incrementGratitudePhotos()

    /**
     * Absolute setter for the user's current consecutive-gratitude-days
     * counter. Updated once per gratitude-log call from
     * [com.prayerquest.app.data.repository.GamificationRepository.onGratitudeLogged]
     * after it diffs today's gratitude count against the previous logged
     * date. Backs the `gratitude_7` / `gratitude_30` badge checks (DD §3.6).
     */
    @Query("UPDATE user_stats SET consecutiveGratitudeDays = :days WHERE id = 1")
    suspend fun setConsecutiveGratitudeDays(days: Int)

    @Query("UPDATE user_stats SET totalGroupPrayersLogged = totalGroupPrayersLogged + 1 WHERE id = 1")
    suspend fun incrementGroupPrayers()

    @Query("UPDATE user_stats SET totalFamousPrayersSaid = totalFamousPrayersSaid + 1 WHERE id = 1")
    suspend fun incrementFamousPrayers()

    // Hearts + freezes have moved to StreakDao.
    // See CLAUDE.md "Architectural decisions of record" (2026-04-16).
}
