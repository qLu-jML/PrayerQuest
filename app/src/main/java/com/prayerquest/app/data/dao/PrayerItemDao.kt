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

    /** How many answered prayers carry any testimony content (text, photo, or voice note). Backs TESTIMONY-category badges. */
    @Query(
        "SELECT COUNT(*) FROM prayer_items " +
        "WHERE status = 'Answered' " +
        "AND ((testimonyText IS NOT NULL AND testimonyText != '') " +
        "  OR (testimonyPhotoUri IS NOT NULL AND testimonyPhotoUri != '') " +
        "  OR (testimonyVoiceUri IS NOT NULL AND testimonyVoiceUri != ''))"
    )
    suspend fun getAnsweredWithTestimonyCount(): Int

    /** How many prayers are currently in the PartiallyAnswered state. Backs PARTIAL_ANSWER-category badges. */
    @Query("SELECT COUNT(*) FROM prayer_items WHERE status = 'PartiallyAnswered'")
    suspend fun getPartiallyAnsweredCount(): Int

    @Query("SELECT * FROM prayer_items WHERE status = 'Active' ORDER BY createdAt DESC")
    suspend fun getActiveList(): List<PrayerItem>

    @Query("UPDATE prayer_items SET status = :status, answeredAt = :answeredAt, testimonyText = :testimony WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, answeredAt: Long? = null, testimony: String? = null)

    @Query("SELECT * FROM prayer_items WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<PrayerItem>>
}
