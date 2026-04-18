package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.entity.PrayerCollectionCrossRef
import com.prayerquest.app.data.entity.PrayerItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: PrayerCollection): Long

    @Update
    suspend fun update(collection: PrayerCollection)

    @Delete
    suspend fun delete(collection: PrayerCollection)

    @Query("SELECT * FROM prayer_collections WHERE id = :id")
    suspend fun getById(id: Long): PrayerCollection?

    @Query("SELECT * FROM prayer_collections WHERE id = :id")
    fun observeById(id: Long): Flow<PrayerCollection?>

    @Query("SELECT * FROM prayer_collections ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PrayerCollection>>

    @Query("SELECT COUNT(*) FROM prayer_collections")
    suspend fun getCount(): Int

    // Cross-ref operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PrayerCollectionCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: PrayerCollectionCrossRef)

    @Query("DELETE FROM prayer_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun deleteAllCrossRefsForCollection(collectionId: Long)

    @Query("""
        SELECT pi.* FROM prayer_items pi
        INNER JOIN prayer_collection_cross_ref cr ON pi.id = cr.prayerItemId
        WHERE cr.collectionId = :collectionId
        ORDER BY pi.createdAt DESC
    """)
    fun observeItemsForCollection(collectionId: Long): Flow<List<PrayerItem>>

    @Query("""
        SELECT pi.* FROM prayer_items pi
        INNER JOIN prayer_collection_cross_ref cr ON pi.id = cr.prayerItemId
        WHERE cr.collectionId = :collectionId
    """)
    suspend fun getItemsForCollection(collectionId: Long): List<PrayerItem>

    @Query("SELECT COUNT(*) FROM prayer_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun getItemCountForCollection(collectionId: Long): Int

    @Query("UPDATE prayer_collections SET itemCount = :count WHERE id = :collectionId")
    suspend fun updateItemCount(collectionId: Long, count: Int)
}
