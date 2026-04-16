package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-day activity summary. One row per calendar day the user was active.
 * Used for heat-map, stats, and goal tracking.
 */
@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey val date: String,              // yyyy-MM-dd
    val sessionsCompleted: Int = 0,
    val prayerMinutes: Int = 0,
    val xpEarned: Int = 0,
    val questsCompleted: Int = 0,
    val gratitudesLogged: Int = 0,
    val groupPrayersLogged: Int = 0,
    val goalMet: Boolean = false
)
