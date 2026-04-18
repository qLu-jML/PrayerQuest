package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.FamousPrayerDao
import com.prayerquest.app.data.dao.NameOfGodDao
import com.prayerquest.app.data.entity.FamousPrayer
import com.prayerquest.app.data.entity.NameOfGod
import kotlinx.coroutines.flow.Flow

/**
 * Unified access point for the Prayer Library:
 * famous prayers + Names of God (Sprint 1+) and future library content.
 *
 * Keep this repository thin — it's just a reactive read layer plus
 * "I prayed this" bumps. Editing/import happens in the importers.
 */
class LibraryRepository(
    private val famousPrayerDao: FamousPrayerDao,
    private val nameOfGodDao: NameOfGodDao
) {

    // ── Famous prayers ─────────────────────────────────────────────
    fun observeFamousPrayers(): Flow<List<FamousPrayer>> =
        famousPrayerDao.observeAll()

    suspend fun incrementFamousPrayerCount(famousPrayerId: String) =
        famousPrayerDao.incrementPrayedCount(famousPrayerId)

    // ── Names of God ───────────────────────────────────────────────
    fun observeNamesOfGod(): Flow<List<NameOfGod>> =
        nameOfGodDao.observeAll()

    fun searchNamesOfGod(query: String): Flow<List<NameOfGod>> =
        nameOfGodDao.searchByText(query)

    suspend fun incrementNameOfGodPrayedCount(id: String) =
        nameOfGodDao.incrementPrayedCount(id)

    suspend fun nameOfGodCount(): Int = nameOfGodDao.count()

    /**
     * One-shot getter for the detail screen. Suspend over Flow because the
     * detail view loads a single row, shows a NotFound state if the id
     * doesn't resolve, and otherwise never re-queries — so a cold suspend
     * read is cheaper than wiring a whole Flow pipeline only to take the
     * first emission.
     */
    suspend fun getNameOfGodById(id: String): NameOfGod? =
        nameOfGodDao.getById(id)
}
