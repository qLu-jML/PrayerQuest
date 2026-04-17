package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.DevotionalDao
import com.prayerquest.app.data.entity.Devotional
import kotlinx.coroutines.flow.Flow

/**
 * Daily devotional readings (Spurgeon, Bonhoeffer, etc.).
 * Populated by a one-time importer seeded in Sprint 6; used by the
 * Home dashboard and the background DevotionalWorker (Sprint 4).
 */
class DevotionalRepository(
    private val devotionalDao: DevotionalDao
) {

    fun observeForDate(date: String): Flow<Devotional?> =
        devotionalDao.observeForDate(date)

    suspend fun getForDate(date: String): Devotional? =
        devotionalDao.getForDate(date)

    fun observeRecent(limit: Int = 30): Flow<List<Devotional>> =
        devotionalDao.observeRecent(limit)

    suspend fun markRead(date: String) = devotionalDao.markRead(date)

    suspend fun count(): Int = devotionalDao.count()
}
