package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A Prayer Group — strictly invite-only community for shared prayer.
 * Each group has a unique share code for joining.
 */
@Entity(tableName = "prayer_groups")
data class PrayerGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val emoji: String = "",
    val shareCode: String,                     // e.g. "PRAY-8K9M2X"
    val createdBy: String = "local_user",      // userId — local for MVP
    val createdAt: Long = System.currentTimeMillis()
)
