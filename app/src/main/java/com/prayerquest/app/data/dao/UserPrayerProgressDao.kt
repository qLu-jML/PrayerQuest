package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.UserPrayerProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPrayerProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserPrayerProgress)

    @Query("SELECT * FROM user_prayer_progress WHERE prayerItemId = :itemId")
    suspend fun getByPrayerItem(itemId: Long): UserPrayerProgress?

    @Query("SELECT * FROM user_prayer_progress WHERE prayerItemId = :itemId")
    fun observeByPrayerItem(itemId: Long): Flow<UserPrayerProgress?>

    @Query("SELECT * FROM user_prayer_progress ORDER BY lastPrayedDate DESC")
    fun observeAll(): Flow<List<UserPrayerProgress>>

    @Query("UPDATE user_prayer_progress SET totalSessions = totalSessions + 1, totalMinutes = totalMinutes + :minutes, lastPrayedDate = :date WHERE prayerItemId = :itemId")
    suspend fun incrementSession(itemId: Long, minutes: Int, date: String)
}
