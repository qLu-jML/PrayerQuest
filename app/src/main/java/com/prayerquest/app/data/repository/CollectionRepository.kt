package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.PrayerCollectionDao
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.entity.PrayerCollectionCrossRef
import com.prayerquest.app.data.entity.PrayerItem
import kotlinx.coroutines.flow.Flow

/**
 * Collections + cross-refs. Manages grouping prayer items into named lists.
 */
class CollectionRepository(
    private val collectionDao: PrayerCollectionDao
) {

    fun observeAll(): Flow<List<PrayerCollection>> = collectionDao.observeAll()
    fun observeById(id: Long): Flow<PrayerCollection?> = collectionDao.observeById(id)
    fun observeItemsForCollection(collectionId: Long): Flow<List<PrayerItem>> =
        collectionDao.observeItemsForCollection(collectionId)

    suspend fun getById(id: Long): PrayerCollection? = collectionDao.getById(id)
    suspend fun getItemsForCollection(collectionId: Long): List<PrayerItem> =
        collectionDao.getItemsForCollection(collectionId)
    suspend fun getCollectionCount(): Int = collectionDao.getCount()

    suspend fun create(collection: PrayerCollection): Long {
        return collectionDao.insert(collection)
    }

    suspend fun update(collection: PrayerCollection) {
        collectionDao.update(collection)
    }

    suspend fun delete(collection: PrayerCollection) {
        collectionDao.delete(collection)
    }

    suspend fun addItemToCollection(collectionId: Long, prayerItemId: Long) {
        collectionDao.insertCrossRef(PrayerCollectionCrossRef(collectionId, prayerItemId))
        refreshItemCount(collectionId)
    }

    suspend fun removeItemFromCollection(collectionId: Long, prayerItemId: Long) {
        collectionDao.deleteCrossRef(PrayerCollectionCrossRef(collectionId, prayerItemId))
        refreshItemCount(collectionId)
    }

    private suspend fun refreshItemCount(collectionId: Long) {
        val count = collectionDao.getItemCountForCollection(collectionId)
        collectionDao.updateItemCount(collectionId, count)
    }
}
