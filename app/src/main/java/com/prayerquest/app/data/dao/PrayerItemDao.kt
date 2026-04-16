package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.PrayerItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PrayerItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PrayerItem>)

    @Update
    suspend fun update(item: PrayerItem)

    @Delete
    suspend fun delete(item: PrayerItem)

    @Query("DELETE FROM prayer_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM prayer_items WHERE id = :id")
    suspend fun getById(id: Long): PrayerItem?

    @Query("SELECT * FROM prayer_items WHERE id = :id")
    fun observeById(id: Long): Flow<PrayerItem?>

    @Query("SELECT * FROM prayer_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PrayerItem>>

    @Query("SELECT * FROM prayer_items WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<PrayerItem>>

    @Query("SELECT * FROM prayer_items WHERE status = 'Active' ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<PrayerItem>>

    @Query("SELECT * FROM prayer_items WHERE status = 'Answered' ORDER BY answeredAt DESC")
    fun observeAnswered(): Flow<List<PrayerItem>>

    @Query("SELECT * FROM prayer_items WHERE category = :category ORDER BY createdAt DESC")
    fun observeByCategory(category: String): Flow<List<PrayerItem>>

    @Query("SELECT COUNT(*) FROM prayer_items")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM prayer_items WHERE status = 'Answered'")
    suspend fun getAnsweredCount(): Int

    @Query("SELECT COUNT(*) FROM prayer_items WHERE status = 'Active'")
    suspend fun getActiveCount(): Int

    @Query("UPDATE prayer_items SET status = :status, answeredAt = :answeredAt, testimonyText = :testimony WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, answeredAt: Long? = null, testimony: String? = null)

    @Query("SELECT * FROM prayer_items WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<PrayerItem>>
}
