package com.prayerquest.app.data.prayer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prayerquest.app.data.dao.BiblePrayerDao
import com.prayerquest.app.data.entity.BiblePrayer

/**
 * One-time async importer for Bible prayers from
 * assets/prayers/bible_prayers.json. Mirrors FamousPrayerImporter:
 * chunked, idempotent, and safe to call on every launch.
 */
class BiblePrayerImporter(
    private val context: Context,
    private val biblePrayerDao: BiblePrayerDao
) {

    companion object {
        private const val ASSET_PATH = "prayers/bible_prayers.json"
        private const val CHUNK_SIZE = 50
    }

    suspend fun importIfNeeded() {
        val existingCount = biblePrayerDao.getCount()
        if (existingCount > 0) return  // already imported

        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }

        val type = object : TypeToken<List<BiblePrayerJson>>() {}.type
        val prayers: List<BiblePrayerJson> = Gson().fromJson(json, type)

        prayers.chunked(CHUNK_SIZE).forEach { chunk ->
            val entities = chunk.map { it.toEntity() }
            biblePrayerDao.insertAll(entities)
        }
    }

    private data class BiblePrayerJson(
        val id: String,
        val reference: String,
        val book: String = "",
        val testament: String = "",
        val title: String,
        val person: String = "",
        val category: String = "",
        val description: String = "",
        val text: String = ""
    ) {
        fun toEntity() = BiblePrayer(
            id = id,
            reference = reference,
            book = book,
            testament = testament,
            title = title,
            person = person,
            category = category,
            description = description,
            text = text
        )
    }
}
