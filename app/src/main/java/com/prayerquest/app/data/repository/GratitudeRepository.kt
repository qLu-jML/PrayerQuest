package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.GratitudeEntryDao
import com.prayerquest.app.data.entity.GratitudeEntry
import kotlinx.coroutines.flow.Flow

/**
 * Gratitude entries CRUD, photo management, and daily prompt logic.
 */
class GratitudeRepository(
    private val gratitudeEntryDao: GratitudeEntryDao
) {

    fun observeAll(): Flow<List<GratitudeEntry>> = gratitudeEntryDao.observeAll()
    fun observeByDate(date: String): Flow<List<GratitudeEntry>> = gratitudeEntryDao.observeByDate(date)
    fun observeByCategory(category: String): Flow<List<GratitudeEntry>> = gratitudeEntryDao.observeByCategory(category)
    fun observeWithPhotos(): Flow<List<GratitudeEntry>> = gratitudeEntryDao.observeWithPhotos()
    fun observeDistinctDates(): Flow<List<String>> = gratitudeEntryDao.observeDistinctDates()
    fun search(query: String): Flow<List<GratitudeEntry>> = gratitudeEntryDao.search(query)

    suspend fun getById(id: Long): GratitudeEntry? = gratitudeEntryDao.getById(id)
    suspend fun getByDate(date: String): List<GratitudeEntry> = gratitudeEntryDao.getByDate(date)
    suspend fun getTotalCount(): Int = gratitudeEntryDao.getTotalCount()
    suspend fun getPhotoCount(): Int = gratitudeEntryDao.getPhotoCount()
    suspend fun getCountForDate(date: String): Int = gratitudeEntryDao.getCountForDate(date)

    suspend fun add(entry: GratitudeEntry): Long = gratitudeEntryDao.insert(entry)
    suspend fun addAll(entries: List<GratitudeEntry>) = gratitudeEntryDao.insertAll(entries)
    suspend fun update(entry: GratitudeEntry) = gratitudeEntryDao.update(entry)
    suspend fun delete(entry: GratitudeEntry) = gratitudeEntryDao.delete(entry)

    /** Check if the user has logged gratitude today. */
    suspend fun hasLoggedToday(date: String): Boolean = gratitudeEntryDao.getCountForDate(date) > 0
}
