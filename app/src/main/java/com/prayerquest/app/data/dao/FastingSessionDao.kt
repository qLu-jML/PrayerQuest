package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.FastingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingSessionDao {

    @Insert
    suspend fun insert(session: FastingSession): Long

    @Update
    suspend fun update(session: FastingSession)

    @Delete
    suspend fun delete(session: FastingSession)

    @Query("SELECT * FROM fasting_session WHERE id = :id")
    suspend fun getById(id: Long): FastingSession?

    @Query("SELECT * FROM fasting_session WHERE status = 'ACTIVE' ORDER BY startDate DESC LIMIT 1")
    fun observeActive(): Flow<FastingSession?>

    @Query("SELECT * FROM fasting_session ORDER BY startDate DESC")
    fun observeAll(): Flow<List<FastingSession>>

    @Query("SELECT * FROM fasting_session WHERE status = :status ORDER BY startDate DESC")
    fun observeByStatus(status: String): Flow<List<FastingSession>>

    @Query("SELECT COUNT(*) FROM fasting_session WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int
}
