package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton row (id = 1) tracking daily prayer streak.
 * Identical pattern to ScriptureQuest's StreakData.
 * Streak is protected by freezes (earned every 7 days) and hearts.
 */
@Entity(tableName = "streak_data")
data class StreakData(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",           // yyyy-MM-dd, "" = never active
    val lastFreezeUsedDate: String = ""        // yyyy-MM-dd, "" = never used
)
