package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.GratitudeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface GratitudeEntryDao {

    @Insert
    suspend fun insert(entry: GratitudeEntry): Long

    @Insert
    suspend fun insertAll(entries: List<GratitudeEntry>)

    @Update
    suspend fun update(entry: GratitudeEntry)

    @Delete
    suspend fun delete(entry: GratitudeEntry)

    @Query("SELECT * FROM gratitude_entries WHERE id = :id")
    suspend fun getById(id: Long): GratitudeEntry?

    @Query("SELECT * FROM gratitude_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<GratitudeEntry>>

    @Query("SELECT * FROM gratitude_entries WHERE date = :date ORDER BY timestamp ASC")
    fun observeByDate(date: String): Flow<List<GratitudeEntry>>

    @Query("SELECT * FROM gratitude_entries WHERE date = :date")
    suspend fun getByDate(date: String): List<GratitudeEntry>

    @Query("SELECT * FROM gratitude_entries WHERE category = :category ORDER BY timestamp DESC")
    fun observeByCategory(category: String): Flow<List<GratitudeEntry>>

    @Query("SELECT * FROM gratitude_entries WHERE photoUri IS NOT NULL ORDER BY timestamp DESC")
    fun observeWithPhotos(): Flow<List<GratitudeEntry>>

    @Query("SELECT COUNT(*) FROM gratitude_entries")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM gratitude_entries WHERE photoUri IS NOT NULL")
    suspend fun getPhotoCount(): Int

    @Query("SELECT COUNT(*) FROM gratitude_entries WHERE date = :date")
    suspend fun getCountForDate(date: String): Int

    @Query("SELECT DISTINCT date FROM gratitude_entries ORDER BY date DESC")
    fun observeDistinctDates(): Flow<List<String>>

    @Query("SELECT * FROM gratitude_entries WHERE text LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<GratitudeEntry>>
}
