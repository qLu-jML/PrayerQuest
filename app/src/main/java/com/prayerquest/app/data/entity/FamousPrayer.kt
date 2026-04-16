package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A curated famous / historical prayer from the pre-loaded catalogue.
 * Imported from assets/prayers/famous_prayers.json on first launch.
 */
@Entity(tableName = "famous_prayers")
data class FamousPrayer(
    @PrimaryKey val id: String,               // Stable string ID from JSON
    val title: String,
    val author: String = "",
    val category: String = "",
    val text: String,
    val source: String = "",
    val dateComposed: String = "",
    val userPrayedCount: Int = 0
)
