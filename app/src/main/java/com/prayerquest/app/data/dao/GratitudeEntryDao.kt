package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.GratitudeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface GratitudeEntryDao {

    @Insert
    suspend fun insert(entry: GratitudeEntry): Long

    @Insert
    suspend fun insertAll(entries: List<GratitudeEntry>)

    @Update
    suspend fun update(entry: GratitudeEntry)

    @Delete
    suspend fun delete(entry: GratitudeEntry)

    @Query("SELECT * FROM gratitude_entries WHERE id = :id")
    suspend fun getById(id: Long): GratitudeEntry?

    @Query("SELECT * FROM gratitude_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<GratitudeEntry>>

    @Query("SELECT * FROM gratitude_entries WHERE date = :date ORDER BY timestamp ASC")
    fun observeByDate(date: String): Flow<List<GratitudeEntry>>

    @Query("SELECT * FROM gratitude_entries WHERE date = :date")
    suspend fun getByDate(date: String): List<GratitudeEntry>

    @Query("SELECT * FROM gratitude_entries WHERE category = :category ORDER BY timestamp DESC")
    fun observeByCategory(category: String): Flow<List<GratitudeEntry>>

    @Query("SELECT * FROM gratitude_entries WHERE photoUri IS NOT NULL ORDER BY timestamp DESC")
    fun observeWithPhotos(): Flow<List<GratitudeEntry>>

    @Query("SELECT COUNT(*) FROM gratitude_entries")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM gratitude_entries WHERE photoUri IS NOT NULL")
    suspend fun getPhotoCount(): Int

    /**
     * Count of photo-attached entries whose `date` column (ISO `yyyy-MM-dd`)
     * starts with the given YYYY-MM prefix. Used to enforce the free-tier
     * monthly photo cap in [com.prayerquest.app.billing.PremiumFeatures].
     *
     * The LIKE-prefix match leverages the column's natural sort order; no
     * additional index is needed — the table stays small enough that a scan is
     * fine, and the DataStore-backed user stream doesn't read this on a hot
     * path.
     */
    @Query("SELECT COUNT(*) FROM gratitude_entries WHERE photoUri IS NOT NULL AND date LIKE :yearMonthPrefix || '%'")
    suspend fun getPhotoCountForMonth(yearMonthPrefix: String): Int

    @Query("SELECT COUNT(*) FROM gratitude_entries WHERE date = :date")
    suspend fun getCountForDate(date: String): Int

    @Query("SELECT DISTINCT date FROM gratitude_entries ORDER BY date DESC")
    fun observeDistinctDates(): Flow<List<String>>

    @Query("SELECT * FROM gratitude_entries WHERE text LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<GratitudeEntry>>

    /**
     * Case-insensitive keyword search (debounced from the ViewModel) backing
     * the Gratitude Catalogue search field. Uses the Flow-returning form so
     * results update live if an entry is edited in another tab. The `LIKE`
     * predicate is fine here because `gratitude_entries.text` is short and
     * the table never grows past a few thousand rows for a heavy user.
     */
    @Query("SELECT * FROM gratitude_entries WHERE text LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY timestamp DESC")
    fun searchEntries(query: String): Flow<List<GratitudeEntry>>

    /**
     * Per-day entry histogram that feeds the calendar heat-map at the top
     * of the Gratitude Catalogue. Returned rows are `(date, count)` pairs
     * ordered ascending by date so the ViewModel can drop them straight
     * into a `Map<LocalDate, Int>` keyed by ISO `yyyy-MM-dd`.
     */
    @Query("SELECT date AS day, COUNT(*) AS entryCount FROM gratitude_entries GROUP BY date ORDER BY date ASC")
    fun observeDateCounts(): Flow<List<GratitudeDateCount>>
}

/**
 * Projection row for [GratitudeEntryDao.observeDateCounts]. Kept next to the
 * DAO so Room treats the column names as authoritative — `day` must match
 * the aliased `date` column and `entryCount` must match the aliased count.
 */
data class GratitudeDateCount(
    val day: String,        // yyyy-MM-dd
    val entryCount: Int
)
