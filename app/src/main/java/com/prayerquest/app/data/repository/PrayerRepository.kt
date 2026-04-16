package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.PrayerItemDao
import com.prayerquest.app.data.dao.PrayerRecordDao
import com.prayerquest.app.data.dao.UserPrayerProgressDao
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.entity.PrayerRecord
import com.prayerquest.app.data.entity.UserPrayerProgress
import kotlinx.coroutines.flow.Flow

/**
 * Prayer items CRUD, status updates, answered archive, and session recording.
 */
class PrayerRepository(
    private val prayerItemDao: PrayerItemDao,
    private val prayerRecordDao: PrayerRecordDao,
    private val userPrayerProgressDao: UserPrayerProgressDao
) {

    // --- Prayer Items ---
    fun observeAllItems(): Flow<List<PrayerItem>> = prayerItemDao.observeAll()
    fun observeActiveItems(): Flow<List<PrayerItem>> = prayerItemDao.observeActive()
    fun observeAnsweredItems(): Flow<List<PrayerItem>> = prayerItemDao.observeAnswered()
    fun observeItemsByCategory(category: String): Flow<List<PrayerItem>> = prayerItemDao.observeByCategory(category)
    fun observeItem(id: Long): Flow<PrayerItem?> = prayerItemDao.observeById(id)
    fun searchItems(query: String): Flow<List<PrayerItem>> = prayerItemDao.search(query)

    suspend fun getItem(id: Long): PrayerItem? = prayerItemDao.getById(id)
    suspend fun getActiveCount(): Int = prayerItemDao.getActiveCount()
    suspend fun getAnsweredCount(): Int = prayerItemDao.getAnsweredCount()

    suspend fun addItem(item: PrayerItem): Long = prayerItemDao.insert(item)
    suspend fun addItems(items: List<PrayerItem>) = prayerItemDao.insertAll(items)
    suspend fun updateItem(item: PrayerItem) = prayerItemDao.update(item)
    suspend fun deleteItem(item: PrayerItem) = prayerItemDao.delete(item)

    /** Mark a prayer as answered with optional testimony. */
    suspend fun markAnswered(id: Long, testimony: String? = null) {
        prayerItemDao.updateStatus(
            id = id,
            status = PrayerItem.STATUS_ANSWERED,
            answeredAt = System.currentTimeMillis(),
            testimony = testimony
        )
    }

    /** Mark a prayer as partially answered. */
    suspend fun markPartiallyAnswered(id: Long, testimony: String? = null) {
        prayerItemDao.updateStatus(
            id = id,
            status = PrayerItem.STATUS_PARTIALLY_ANSWERED,
            testimony = testimony
        )
    }

    /** Return an answered prayer back to active status. */
    suspend fun reactivate(id: Long) {
        prayerItemDao.updateStatus(id = id, status = PrayerItem.STATUS_ACTIVE)
    }

    // --- Prayer Records ---
    suspend fun recordSession(record: PrayerRecord): Long = prayerRecordDao.insert(record)
    fun observeRecentRecords(limit: Int = 50): Flow<List<PrayerRecord>> = prayerRecordDao.observeRecent(limit)
    fun observeRecordsByDate(date: String): Flow<List<PrayerRecord>> = prayerRecordDao.observeByDate(date)

    suspend fun getTotalMinutesPrayed(): Int = (prayerRecordDao.getTotalSeconds() ?: 0) / 60
    suspend fun getTotalSessions(): Int = prayerRecordDao.getTotalCount()
    suspend fun getDistinctModesUsedToday(date: String): Int = prayerRecordDao.getDistinctModesForDate(date)
    suspend fun getTotalDistinctModes(): Int = prayerRecordDao.getTotalDistinctModes()
    suspend fun getMinutesForDate(date: String): Int = (prayerRecordDao.getTotalSecondsForDate(date) ?: 0) / 60

    // --- Per-Item Progress ---
    fun observeProgress(itemId: Long): Flow<UserPrayerProgress?> = userPrayerProgressDao.observeByPrayerItem(itemId)
    fun observeAllProgress(): Flow<List<UserPrayerProgress>> = userPrayerProgressDao.observeAll()

    suspend fun updateProgressAfterSession(itemId: Long, minutes: Int, date: String) {
        val existing = userPrayerProgressDao.getByPrayerItem(itemId)
        if (existing == null) {
            userPrayerProgressDao.upsert(
                UserPrayerProgress(
                    prayerItemId = itemId,
                    lastPrayedDate = date,
                    totalSessions = 1,
                    totalMinutes = minutes
                )
            )
        } else {
            userPrayerProgressDao.incrementSession(itemId, minutes, date)
        }
    }
}
