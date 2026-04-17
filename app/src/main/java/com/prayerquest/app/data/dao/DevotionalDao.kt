package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.Devotional
import kotlinx.coroutines.flow.Flow

@Dao
interface DevotionalDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(devotionals: List<Devotional>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(devotional: Devotional)

    @Query("SELECT * FROM devotional WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): Devotional?

    @Query("SELECT * FROM devotional WHERE date = :date LIMIT 1")
    fun observeForDate(date: String): Flow<Devotional?>

    @Query("SELECT * FROM devotional ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<Devotional>>

    @Query("UPDATE devotional SET readCount = readCount + 1, lastReadAt = :timestamp WHERE date = :date")
    suspend fun markRead(date: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM devotional")
    suspend fun count(): Int

    /**
     * Counts rows that haven't yet had their evening reading populated.
     * Used by [SpurgeonDevotionalImporter] to detect the v1 → v2 upgrade
     * path where the table is full but every row is morning-only.
     */
    @Query("SELECT COUNT(*) FROM devotional WHERE eveningPassage = ''")
    suspend fun countMissingEvening(): Int
}
