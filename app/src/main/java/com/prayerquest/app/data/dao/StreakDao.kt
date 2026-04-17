package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.StreakData
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(streak: StreakData)

    @Update
    suspend fun update(streak: StreakData)

    @Query("SELECT * FROM streak_data WHERE id = 1")
    suspend fun get(): StreakData?

    @Query("SELECT * FROM streak_data WHERE id = 1")
    fun observe(): Flow<StreakData?>

    // Streak-protection atomic setters. Moved here from UserStatsDao
    // on 2026-04-16 — see CLAUDE.md "Architectural decisions of record".
    @Query("UPDATE streak_data SET hearts = :hearts WHERE id = 1")
    suspend fun setHearts(hearts: Int)

    @Query("UPDATE streak_data SET freezes = :freezes WHERE id = 1")
    suspend fun setFreezes(freezes: Int)
}
