package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-prayer-item progress tracking.
 * Persists how often and how long the user has prayed for each item.
 */
@Entity(tableName = "user_prayer_progress")
data class UserPrayerProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayerItemId: Long,
    val lastPrayedDate: String = "",           // yyyy-MM-dd
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val currentStatus: String = "Active"       // mirrors PrayerItem.status
)
