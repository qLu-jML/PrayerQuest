package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.GratitudeDateCount
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

    /**
     * Debounced keyword search used by the Gratitude Catalogue. Callers
     * debounce on the ViewModel side (250ms) before collecting this Flow so
     * typing doesn't thrash Room.
     */
    fun searchEntries(query: String): Flow<List<GratitudeEntry>> =
        gratitudeEntryDao.searchEntries(query)

    /**
     * Per-day entry counts for the calendar heat-map at the top of the
     * Gratitude Catalogue. Rows are already sorted ascending by date, so
     * the UI layer can fold them into a `Map<LocalDate, Int>` in one pass.
     */
    fun observeDateCounts(): Flow<List<GratitudeDateCount>> =
        gratitudeEntryDao.observeDateCounts()

    suspend fun getById(id: Long): GratitudeEntry? = gratitudeEntryDao.getById(id)
    suspend fun getByDate(date: String): List<GratitudeEntry> = gratitudeEntryDao.getByDate(date)
    suspend fun getTotalCount(): Int = gratitudeEntryDao.getTotalCount()
    suspend fun getPhotoCount(): Int = gratitudeEntryDao.getPhotoCount()
    suspend fun getCountForDate(date: String): Int = gratitudeEntryDao.getCountForDate(date)

    /**
     * How many photo-attached gratitude entries fall in the given month
     * ("YYYY-MM"). Backs the free-tier monthly photo quota gate — callers
     * compare against [com.prayerquest.app.billing.PremiumFeatures.FREE_GRATITUDE_PHOTOS_PER_MONTH].
     */
    suspend fun getPhotoCountForMonth(yearMonthPrefix: String): Int =
        gratitudeEntryDao.getPhotoCountForMonth(yearMonthPrefix)

    suspend fun add(entry: GratitudeEntry): Long = gratitudeEntryDao.insert(entry)
    suspend fun addAll(entries: List<GratitudeEntry>) = gratitudeEntryDao.insertAll(entries)
    suspend fun update(entry: GratitudeEntry) = gratitudeEntryDao.update(entry)
    suspend fun delete(entry: GratitudeEntry) = gratitudeEntryDao.delete(entry)

    /** Check if the user has logged gratitude today. */
    suspend fun hasLoggedToday(date: String): Boolean = gratitudeEntryDao.getCountForDate(date) > 0
}
