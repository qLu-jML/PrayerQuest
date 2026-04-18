package com.prayerquest.app.data.prayer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prayerquest.app.data.dao.FamousPrayerDao
import com.prayerquest.app.data.entity.FamousPrayer

/**
 * One-time async importer for famous prayers from assets/prayers/famous_prayers.json.
 * Modeled after ScriptureQuest's BibleImporter — chunked & idempotent.
 */
class FamousPrayerImporter(
    private val context: Context,
    private val famousPrayerDao: FamousPrayerDao
) {

    companion object {
        private const val ASSET_PATH = "prayers/famous_prayers.json"
        private const val CHUNK_SIZE = 50
    }

    /**
     * Import famous prayers from JSON asset if not already imported.
     * Safe to call multiple times — uses INSERT OR IGNORE.
     */
    suspend fun importIfNeeded() {
        val existingCount = famousPrayerDao.getCount()
        if (existingCount > 0) return  // already imported

        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }

        val type = object : TypeToken<List<FamousPrayerJson>>() {}.type
        val prayers: List<FamousPrayerJson> = Gson().fromJson(json, type)

        // Chunked insert to bound memory
        prayers.chunked(CHUNK_SIZE).forEach { chunk ->
            val entities = chunk.map { it.toEntity() }
            famousPrayerDao.insertAll(entities)
        }
    }

    private data class FamousPrayerJson(
        val id: String,
        val title: String,
        val author: String = "",
        val category: String = "",
        val text: String,
        val source: String = "",
        val dateComposed: String = ""
    ) {
        fun toEntity() = FamousPrayer(
            id = id,
            title = title,
            author = author,
            category = category,
            text = text,
            source = source,
            dateComposed = dateComposed
        )
    }
}
