package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.PrayerRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerRecordDao {

    @Insert
    suspend fun insert(record: PrayerRecord): Long

    @Insert
    suspend fun insertAll(records: List<PrayerRecord>)

    @Query("SELECT * FROM prayer_records WHERE sessionDate = :date ORDER BY id DESC")
    fun observeByDate(date: String): Flow<List<PrayerRecord>>

    @Query("SELECT * FROM prayer_records WHERE prayerItemId = :itemId ORDER BY id DESC")
    fun observeByPrayerItem(itemId: Long): Flow<List<PrayerRecord>>

    @Query("SELECT * FROM prayer_records ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<PrayerRecord>>

    @Query("SELECT SUM(durationSeconds) FROM prayer_records WHERE sessionDate = :date")
    suspend fun getTotalSecondsForDate(date: String): Int?

    @Query("SELECT COUNT(DISTINCT mode) FROM prayer_records WHERE sessionDate = :date")
    suspend fun getDistinctModesForDate(date: String): Int

    @Query("SELECT COUNT(DISTINCT mode) FROM prayer_records")
    suspend fun getTotalDistinctModes(): Int

    @Query("SELECT SUM(durationSeconds) FROM prayer_records")
    suspend fun getTotalSeconds(): Int?

    @Query("SELECT COUNT(*) FROM prayer_records")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM prayer_records WHERE groupPrayerItemId IS NOT NULL")
    suspend fun getGroupPrayerRecordCount(): Int
}
