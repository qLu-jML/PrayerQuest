package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.BiblePrayer
import kotlinx.coroutines.flow.Flow

@Dao
interface BiblePrayerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(prayers: List<BiblePrayer>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(prayer: BiblePrayer)

    @Update
    suspend fun update(prayer: BiblePrayer)

    @Query("SELECT * FROM bible_prayers WHERE id = :id")
    suspend fun getById(id: String): BiblePrayer?

    @Query("SELECT * FROM bible_prayers ORDER BY id ASC")
    fun observeAll(): Flow<List<BiblePrayer>>

    @Query("SELECT * FROM bible_prayers WHERE testament = :testament ORDER BY id ASC")
    fun observeByTestament(testament: String): Flow<List<BiblePrayer>>

    @Query("SELECT * FROM bible_prayers WHERE book = :book ORDER BY id ASC")
    fun observeByBook(book: String): Flow<List<BiblePrayer>>

    @Query("SELECT * FROM bible_prayers WHERE category = :category ORDER BY id ASC")
    fun observeByCategory(category: String): Flow<List<BiblePrayer>>

    @Query("SELECT COUNT(*) FROM bible_prayers")
    suspend fun getCount(): Int

    @Query("UPDATE bible_prayers SET userPrayedCount = userPrayedCount + 1 WHERE id = :id")
    suspend fun incrementPrayedCount(id: String)

    @Query(
        """
        SELECT * FROM bible_prayers
        WHERE title LIKE '%' || :query || '%'
           OR person LIKE '%' || :query || '%'
           OR reference LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        """
    )
    fun search(query: String): Flow<List<BiblePrayer>>

    @Query("SELECT * FROM bible_prayers ORDER BY userPrayedCount DESC LIMIT :limit")
    fun observeMostPrayed(limit: Int = 10): Flow<List<BiblePrayer>>
}
