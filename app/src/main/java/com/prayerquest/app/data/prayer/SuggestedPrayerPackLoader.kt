package com.prayerquest.app.data.prayer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Reads suggested prayer packs from assets/packs/ (JSON files).
 * Modeled after ScriptureQuest's SuggestedPackLoader.
 */
class SuggestedPrayerPackLoader(
    private val context: Context
) {

    /**
     * Load all suggested packs from the packs/ assets directory.
     */
    fun loadAll(): List<SuggestedPrayerPack> {
        val packFiles = context.assets.list("packs") ?: return emptyList()
        return packFiles
            .filter { it.endsWith(".json") }
            .mapNotNull { loadPack("packs/$it") }
            .sortedBy { it.category }
    }

    private fun loadPack(assetPath: String): SuggestedPrayerPack? {
        return try {
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            Gson().fromJson(json, SuggestedPrayerPack::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * A suggested prayer collection pack from JSON assets.
 */
data class SuggestedPrayerPack(
    val id: String,
    val name: String,
    val emoji: String = "",
    val category: String = "",
    val description: String = "",
    val prayers: List<SuggestedPrayerItem> = emptyList()
)

data class SuggestedPrayerItem(
    val title: String,
    val description: String = "",
    val category: String = ""
)
