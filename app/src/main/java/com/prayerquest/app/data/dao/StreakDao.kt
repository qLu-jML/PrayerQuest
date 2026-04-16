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
}
