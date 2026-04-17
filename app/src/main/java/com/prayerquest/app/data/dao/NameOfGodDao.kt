package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.NameOfGod
import kotlinx.coroutines.flow.Flow

@Dao
interface NameOfGodDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(names: List<NameOfGod>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(name: NameOfGod)

    @Update
    suspend fun update(name: NameOfGod)

    @Query("SELECT * FROM name_of_god WHERE id = :id")
    suspend fun getById(id: String): NameOfGod?

    @Query("SELECT * FROM name_of_god ORDER BY name ASC")
    fun observeAll(): Flow<List<NameOfGod>>

    @Query("SELECT COUNT(*) FROM name_of_god")
    suspend fun count(): Int

    @Query("UPDATE name_of_god SET userPrayedCount = userPrayedCount + 1 WHERE id = :id")
    suspend fun incrementPrayedCount(id: String)

    @Query("SELECT * FROM name_of_god WHERE name LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByText(query: String): Flow<List<NameOfGod>>
}
