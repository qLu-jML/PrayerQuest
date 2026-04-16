package com.prayerquest.app.domain.model

/**
 * Types of daily quests available for the 3 rotating slots.
 * Adapted from ScriptureQuest for prayer-specific activities.
 */
enum class QuestType(
    val displayName: String,
    val description: String,
    val defaultTarget: Int,
    val xpReward: Int,
    val coinReward: Int
) {
    PRAY_MINUTES(
        displayName = "Pray",
        description = "Pray for %d minutes today",
        defaultTarget = 10,
        xpReward = 20,
        coinReward = 2
    ),
    USE_MODES(
        displayName = "Mode Explorer",
        description = "Use %d different prayer modes",
        defaultTarget = 3,
        xpReward = 25,
        coinReward = 3
    ),
    JOURNAL_ENTRY(
        displayName = "Journal",
        description = "Write %d journal entry",
        defaultTarget = 1,
        xpReward = 15,
        coinReward = 2
    ),
    LOG_GRATITUDE(
        displayName = "Give Thanks",
        description = "Log %d gratitude entries",
        defaultTarget = 3,
        xpReward = 15,
        coinReward = 2
    ),
    PRAY_FOR_ITEMS(
        displayName = "Intercessor",
        description = "Pray for %d different items",
        defaultTarget = 5,
        xpReward = 20,
        coinReward = 2
    ),
    FAMOUS_PRAYER(
        displayName = "Classic Prayer",
        description = "Pray %d famous prayer(s)",
        defaultTarget = 1,
        xpReward = 15,
        coinReward = 2
    ),
    GROUP_PRAYER(
        displayName = "Community",
        description = "Pray for %d group request(s)",
        defaultTarget = 2,
        xpReward = 20,
        coinReward = 3
    ),
    COMPLETE_SESSION(
        displayName = "Full Session",
        description = "Complete %d prayer session(s)",
        defaultTarget = 1,
        xpReward = 15,
        coinReward = 2
    );

    companion object {
        /** Picks 3 non-repeating quest types for daily rotation. */
        fun generateDailySet(): List<QuestType> {
            return entries.shuffled().take(3)
        }
    }
}
