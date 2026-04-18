package com.prayerquest.app.data.entity

import androidx.room.Entity

/**
 * One of 3 daily quest slots for a given day.
 * Generated once per local day, pruned after 14 days.
 * Composite PK: (date, slotIndex).
 */
@Entity(
    tableName = "daily_quests",
    primaryKeys = ["date", "slotIndex"]
)
data class DailyQuest(
    val date: String,                          // yyyy-MM-dd
    val slotIndex: Int,                        // 0, 1, or 2
    val questTypeId: String,                   // maps to QuestType enum
    val target: Int,                           // e.g. "pray 10 minutes" → target = 10
    val progress: Int = 0,
    val xpReward: Int = 0,
    val coinReward: Int = 0,
    val completed: Boolean = false,
    val rewardClaimed: Boolean = false
)
