package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.DailyQuest
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuestDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(quests: List<DailyQuest>)

    @Update
    suspend fun update(quest: DailyQuest)

    @Query("SELECT * FROM daily_quests WHERE date = :date ORDER BY slotIndex ASC")
    fun observeForDate(date: String): Flow<List<DailyQuest>>

    @Query("SELECT * FROM daily_quests WHERE date = :date ORDER BY slotIndex ASC")
    suspend fun getForDate(date: String): List<DailyQuest>

    @Query("SELECT COUNT(*) FROM daily_quests WHERE date = :date")
    suspend fun getCountForDate(date: String): Int

    @Query("UPDATE daily_quests SET progress = :progress, completed = :completed WHERE date = :date AND slotIndex = :slot")
    suspend fun updateProgress(date: String, slot: Int, progress: Int, completed: Boolean)

    @Query("UPDATE daily_quests SET rewardClaimed = 1 WHERE date = :date AND slotIndex = :slot")
    suspend fun claimReward(date: String, slot: Int)

    @Query("SELECT COUNT(*) FROM daily_quests WHERE date = :date AND completed = 1")
    suspend fun getCompletedCountForDate(date: String): Int

    @Query("DELETE FROM daily_quests WHERE date < :cutoffDate")
    suspend fun pruneOlderThan(cutoffDate: String)
}
