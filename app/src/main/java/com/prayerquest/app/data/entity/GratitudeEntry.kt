package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single gratitude log entry.
 * Users log 1, 3, or 5 per day. Supports optional photo and prayer link.
 */
@Entity(tableName = "gratitude_entries")
data class GratitudeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                          // yyyy-MM-dd
    val text: String,
    val photoUri: String? = null,              // app-private storage path
    val category: String = CATEGORY_OTHER,     // Family | Provision | Nature | SpiritualGrowth | Health | Other
    val linkedPrayerId: Long? = null,          // optional FK to PrayerItem
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_FAMILY = "Family"
        const val CATEGORY_PROVISION = "Provision"
        const val CATEGORY_NATURE = "Nature"
        const val CATEGORY_SPIRITUAL_GROWTH = "SpiritualGrowth"
        const val CATEGORY_HEALTH = "Health"
        const val CATEGORY_OTHER = "Other"

        val ALL_CATEGORIES = listOf(
            CATEGORY_FAMILY, CATEGORY_PROVISION, CATEGORY_NATURE,
            CATEGORY_SPIRITUAL_GROWTH, CATEGORY_HEALTH, CATEGORY_OTHER
        )
    }
}
