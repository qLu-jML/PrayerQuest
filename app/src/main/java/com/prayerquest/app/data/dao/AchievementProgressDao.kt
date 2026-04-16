package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.AchievementProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementProgressDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(progress: AchievementProgress)

    @Update
    suspend fun update(progress: AchievementProgress)

    @Query("SELECT * FROM achievement_progress WHERE achievementId = :id")
    suspend fun getById(id: String): AchievementProgress?

    @Query("SELECT * FROM achievement_progress")
    fun observeAll(): Flow<List<AchievementProgress>>

    @Query("SELECT * FROM achievement_progress WHERE unlockedAt > 0")
    fun observeUnlocked(): Flow<List<AchievementProgress>>

    @Query("SELECT COUNT(*) FROM achievement_progress WHERE unlockedAt > 0")
    fun observeUnlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM achievement_progress WHERE unlockedAt > 0")
    suspend fun getUnlockedCount(): Int

    @Query("SELECT * FROM achievement_progress WHERE unlockedAt > 0 ORDER BY unlockedAt DESC LIMIT :limit")
    fun observeRecentlyUnlocked(limit: Int = 5): Flow<List<AchievementProgress>>
}
