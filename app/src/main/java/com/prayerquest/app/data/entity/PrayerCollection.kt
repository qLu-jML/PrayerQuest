package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named group of prayer items (e.g. "Family Prayers", "Work Requests").
 */
@Entity(tableName = "prayer_collections")
data class PrayerCollection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "",
    val topicTag: String = "",
    val description: String = "",
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
