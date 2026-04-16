package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-achievement progress row.
 * achievementId is a stable string matching AchievementDef.id.
 */
@Entity(tableName = "achievement_progress")
data class AchievementProgress(
    @PrimaryKey val achievementId: String,
    val currentValue: Int = 0,
    val unlockedAt: Long = 0L,                 // epoch ms, 0 = locked
    val rewardClaimed: Boolean = false
) {
    val isUnlocked: Boolean get() = unlockedAt > 0L
}
