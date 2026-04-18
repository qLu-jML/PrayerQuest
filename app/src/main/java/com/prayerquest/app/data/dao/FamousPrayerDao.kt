package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.FamousPrayer
import kotlinx.coroutines.flow.Flow

@Dao
interface FamousPrayerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(prayers: List<FamousPrayer>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(prayer: FamousPrayer)

    @Update
    suspend fun update(prayer: FamousPrayer)

    @Query("SELECT * FROM famous_prayers WHERE id = :id")
    suspend fun getById(id: String): FamousPrayer?

    @Query("SELECT * FROM famous_prayers ORDER BY title ASC")
    fun observeAll(): Flow<List<FamousPrayer>>

    @Query("SELECT * FROM famous_prayers WHERE category = :category ORDER BY title ASC")
    fun observeByCategory(category: String): Flow<List<FamousPrayer>>

    @Query("SELECT COUNT(*) FROM famous_prayers")
    suspend fun getCount(): Int

    /** How many distinct Famous Prayers the user has prayed at least once. Backs FAMOUS_DISTINCT-category badges. */
    @Query("SELECT COUNT(*) FROM famous_prayers WHERE userPrayedCount > 0")
    suspend fun getDistinctPrayedCount(): Int

    @Query("UPDATE famous_prayers SET userPrayedCount = userPrayedCount + 1 WHERE id = :id")
    suspend fun incrementPrayedCount(id: String)

    @Query("SELECT * FROM famous_prayers WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<FamousPrayer>>

    @Query("SELECT * FROM famous_prayers ORDER BY userPrayedCount DESC LIMIT :limit")
    fun observeMostPrayed(limit: Int = 10): Flow<List<FamousPrayer>>
}
