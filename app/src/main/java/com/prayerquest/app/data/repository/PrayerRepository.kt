package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.BiblePrayerDao
import com.prayerquest.app.data.dao.FamousPrayerDao
import com.prayerquest.app.data.dao.PrayerItemDao
import com.prayerquest.app.data.dao.PrayerRecordDao
import com.prayerquest.app.data.dao.UserPrayerProgressDao
import com.prayerquest.app.data.entity.BiblePrayer
import com.prayerquest.app.data.entity.FamousPrayer
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
    private val userPrayerProgressDao: UserPrayerProgressDao,
    private val famousPrayerDao: FamousPrayerDao? = null,
    private val biblePrayerDao: BiblePrayerDao? = null
) {

    // --- Prayer Items ---
    fun observeAllItems(): Flow<List<PrayerItem>> = prayerItemDao.observeAll()
    fun observeActiveItems(): Flow<List<PrayerItem>> = prayerItemDao.observeActive()
    fun observeAnsweredItems(): Flow<List<PrayerItem>> = prayerItemDao.observeAnswered()
    fun observeItemsByCategory(category: String): Flow<List<PrayerItem>> = prayerItemDao.observeByCategory(category)
    fun observeItem(id: Long): Flow<PrayerItem?> = prayerItemDao.observeById(id)
    fun searchItems(query: String): Flow<List<PrayerItem>> = prayerItemDao.search(query)

    suspend fun getItem(id: Long): PrayerItem? = prayerItemDao.getById(id)
    suspend fun getActiveItems(): List<PrayerItem> = prayerItemDao.getActiveList()
    suspend fun getActiveCount(): Int = prayerItemDao.getActiveCount()
    suspend fun getAnsweredCount(): Int = prayerItemDao.getAnsweredCount()

    suspend fun addItem(item: PrayerItem): Long = prayerItemDao.insert(item)
    suspend fun addItems(items: List<PrayerItem>) = prayerItemDao.insertAll(items)
    suspend fun updateItem(item: PrayerItem) = prayerItemDao.update(item)
    suspend fun deleteItem(item: PrayerItem) = prayerItemDao.delete(item)

    /**
     * Mark a prayer as answered with optional testimony.
     *
     * Returns a [MarkAnsweredResult] describing the transition so callers
     * can decide whether to trigger the Big Celebration Moment (DD §3.5.2).
     *
     * The key field is [MarkAnsweredResult.wasNewlyAnswered]: true only
     * when the item was NOT already in the "Answered" state prior to this
     * call. That gates:
     *  - the full-screen celebration modal (no celebration for items
     *    that were imported already-answered or re-flagged answered
     *    after a reactivate/re-answer round-trip on the same day);
     *  - the +365 day anniversary worker (same logic — only schedule for
     *    a *new* Answered transition).
     *
     * Single write-point for Answered → the celebration screen and the
     * anniversary worker both read from this result to stay consistent.
     */
    suspend fun markAnswered(id: Long, testimony: String? = null): MarkAnsweredResult {
        val existing = prayerItemDao.getById(id)
        val originalStatus = existing?.status
        val answeredAtMs = System.currentTimeMillis()
        prayerItemDao.updateStatus(
            id = id,
            status = PrayerItem.STATUS_ANSWERED,
            answeredAt = answeredAtMs,
            testimony = testimony
        )
        return MarkAnsweredResult(
            prayerItemId = id,
            originalStatus = originalStatus,
            answeredAtMs = answeredAtMs,
            wasNewlyAnswered = originalStatus != null &&
                    originalStatus != PrayerItem.STATUS_ANSWERED
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

    /**
     * Return value for [markAnswered]. All fields are plain data so
     * callers can inspect the transition outside of a coroutine.
     *
     * @property prayerItemId     The item that was marked answered.
     * @property originalStatus   Status on disk immediately before this call;
     *                            null only if the row didn't exist (caller
     *                            should treat as a no-op in that case).
     * @property answeredAtMs     Epoch-ms timestamp written to the row. The
     *                            anniversary worker uses this to compute the
     *                            +365 day target.
     * @property wasNewlyAnswered True iff this call flipped the status from
     *                            non-Answered → Answered. Gate for the
     *                            Big Celebration Moment + anniversary.
     */
    data class MarkAnsweredResult(
        val prayerItemId: Long,
        val originalStatus: String?,
        val answeredAtMs: Long,
        val wasNewlyAnswered: Boolean
    )

    // --- Famous Prayers ---
    fun observeAllFamousPrayers(): Flow<List<FamousPrayer>> =
        famousPrayerDao?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getFamousPrayer(id: String): FamousPrayer? = famousPrayerDao?.getById(id)

    suspend fun incrementFamousPrayerPrayedCount(id: String) {
        famousPrayerDao?.incrementPrayedCount(id)
    }

    // --- Bible Prayers ---
    fun observeAllBiblePrayers(): Flow<List<BiblePrayer>> =
        biblePrayerDao?.observeAll() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getBiblePrayer(id: String): BiblePrayer? = biblePrayerDao?.getById(id)

    suspend fun incrementBiblePrayerPrayedCount(id: String) {
        biblePrayerDao?.incrementPrayedCount(id)
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
