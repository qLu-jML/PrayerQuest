package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.FastingSessionDao
import com.prayerquest.app.data.entity.FastingSession
import kotlinx.coroutines.flow.Flow

/**
 * Fasting sessions — user-declared fasts paired with prayer intentions.
 * A medical disclaimer must be acknowledged before starting a fast
 * (enforced by the Fasting UI in v1.0; copy lives in
 * `assets/legal/fasting_disclaimer.md`, authored in Sprint 6).
 */
class FastingRepository(
    private val fastingSessionDao: FastingSessionDao
) {

    fun observeActive(): Flow<FastingSession?> = fastingSessionDao.observeActive()
    fun observeAll(): Flow<List<FastingSession>> = fastingSessionDao.observeAll()

    fun observeByStatus(status: String): Flow<List<FastingSession>> =
        fastingSessionDao.observeByStatus(status)

    suspend fun start(session: FastingSession): Long = fastingSessionDao.insert(session)
    suspend fun update(session: FastingSession) = fastingSessionDao.update(session)
    suspend fun cancel(session: FastingSession) =
        fastingSessionDao.update(session.copy(status = "CANCELLED"))

    suspend fun complete(session: FastingSession, endDate: String) =
        fastingSessionDao.update(session.copy(status = "COMPLETED", endDate = endDate))

    suspend fun getById(id: Long): FastingSession? = fastingSessionDao.getById(id)

    suspend fun getCompletedCount(): Int = fastingSessionDao.getCompletedCount()
}
