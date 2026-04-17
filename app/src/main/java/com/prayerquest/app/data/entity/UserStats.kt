package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton row (id = 1) holding lifetime user statistics.
 * Denormalized counters for fast UI reads.
 * Seeded on database creation — lazy init via .onStart { ensureSeeded() }.
 */
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,

    // XP & Level
    val totalXp: Int = 0,
    val level: Int = 1,
    val graceCoins: Int = 0,

    // Prayer totals
    val totalPrayerMinutes: Int = 0,
    val totalSessions: Int = 0,
    val totalItemsPrayed: Int = 0,
    val answeredPrayerCount: Int = 0,

    // Gratitude totals
    val totalGratitudesLogged: Int = 0,
    val totalGratitudePhotos: Int = 0,
    val consecutiveGratitudeDays: Int = 0,

    // Group totals
    val totalGroupPrayersLogged: Int = 0,
    val totalGroupsJoined: Int = 0,
    val totalGroupsCreated: Int = 0,

    // Famous prayer totals
    val totalFamousPrayersSaid: Int = 0,

    // Session-level stats
    val longestSessionMinutes: Int = 0,
    val totalDistinctModesUsed: Int = 0,

    // Streak protection: hearts + freezes now live on StreakData.
    // See CLAUDE.md "Architectural decisions of record" (2026-04-16).

    // Quest tracking
    val consecutiveQuestDays: Int = 0,
    val totalQuestDaysCompleted: Int = 0,

    // Time-of-day flags (for achievements)
    val hasEarlyBirdSession: Boolean = false,   // session before 6 AM
    val hasNightOwlSession: Boolean = false,     // session after 10 PM
    val hasComebackAfterGap: Boolean = false     // returned after 7+ day gap
)
