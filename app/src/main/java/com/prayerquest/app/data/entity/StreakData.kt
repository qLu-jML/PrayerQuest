package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton row (id = 1) tracking daily prayer streak.
 * Identical pattern to ScriptureQuest's StreakData.
 *
 * Streak protection (hearts + freezes) lives here — NOT on UserStats.
 * See CLAUDE.md "Architectural decisions of record" (2026-04-16).
 *
 * - `hearts`: life-style fail safes, default 3. Consumed automatically on a
 *   missed day when no freeze is available; streak continues while any heart
 *   remains. Replenished by specific achievements / quests.
 * - `freezes`: earned every 7-day streak milestone (capped by gamification
 *   rules). Consumed first on a missed day before hearts.
 */
@Entity(tableName = "streak_data")
data class StreakData(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",           // yyyy-MM-dd, "" = never active
    val lastFreezeUsedDate: String = "",       // yyyy-MM-dd, "" = never used
    val hearts: Int = 3,
    val freezes: Int = 0
)
