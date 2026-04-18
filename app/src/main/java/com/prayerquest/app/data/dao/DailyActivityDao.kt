package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.DailyActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: DailyActivity)

    @Query("SELECT * FROM daily_activity WHERE date = :date")
    suspend fun getForDate(date: String): DailyActivity?

    @Query("SELECT * FROM daily_activity WHERE date = :date")
    fun observeForDate(date: String): Flow<DailyActivity?>

    @Query("SELECT * FROM daily_activity ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activity WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeRange(startDate: String, endDate: String): Flow<List<DailyActivity>>

    @Query("SELECT SUM(prayerMinutes) FROM daily_activity WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalMinutesInRange(startDate: String, endDate: String): Int?
}
