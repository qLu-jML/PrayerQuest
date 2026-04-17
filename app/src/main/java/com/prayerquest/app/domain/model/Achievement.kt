package com.prayerquest.app.domain.model

/**
 * Achievement catalog — 20+ achievements across multiple categories.
 * Adapted from ScriptureQuest with prayer, gratitude, and group badges.
 */
data class AchievementDef(
    val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val targetValue: Int,
    val xpReward: Int,
    val coinReward: Int,
    val icon: String = ""   // emoji or icon name
)

enum class AchievementCategory {
    STREAK,
    PRAYER_MINUTES,
    ITEMS_PRAYED,
    MODE_DIVERSITY,
    FAMOUS_PRAYER,
    ANSWERED_PRAYER,
    GRATITUDE,
    GROUP,
    LEVELING,
    XP_MILESTONE,
    SESSION,
    TIME_OF_DAY,
    COMEBACK
}

object Achievements {

    val ALL: List<AchievementDef> = listOf(
        // --- STREAK ---
        AchievementDef("streak_3", "Spark", "3-day prayer streak", AchievementCategory.STREAK, 3, 10, 1, "🔥"),
        AchievementDef("streak_7", "Kindling", "7-day prayer streak", AchievementCategory.STREAK, 7, 25, 2, "🔥"),
        AchievementDef("streak_30", "Burning Bright", "30-day prayer streak", AchievementCategory.STREAK, 30, 75, 5, "🔥"),
        AchievementDef("streak_100", "Unquenchable", "100-day prayer streak", AchievementCategory.STREAK, 100, 150, 10, "🔥"),
        AchievementDef("streak_365", "Year of Devotion", "365-day prayer streak", AchievementCategory.STREAK, 365, 500, 25, "👑"),

        // --- PRAYER MINUTES ---
        AchievementDef("minutes_60", "First Hour", "Pray for a total of 60 minutes", AchievementCategory.PRAYER_MINUTES, 60, 15, 2, "⏱️"),
        AchievementDef("minutes_600", "Prayer Warrior", "Pray for 10 total hours", AchievementCategory.PRAYER_MINUTES, 600, 50, 5, "⚔️"),
        AchievementDef("minutes_3000", "Watchman on the Wall", "Pray for 50 total hours", AchievementCategory.PRAYER_MINUTES, 3000, 150, 10, "🏰"),

        // --- ITEMS PRAYED ---
        AchievementDef("items_1", "First Petition", "Pray for your first item", AchievementCategory.ITEMS_PRAYED, 1, 10, 1, "🙏"),
        AchievementDef("items_50", "Faithful Intercessor", "Pray for 50 different items", AchievementCategory.ITEMS_PRAYED, 50, 40, 3, "🙏"),
        AchievementDef("items_200", "Prayer List Master", "Pray for 200 different items", AchievementCategory.ITEMS_PRAYED, 200, 100, 8, "📋"),

        // --- MODE DIVERSITY ---
        AchievementDef("modes_all", "Well-Rounded", "Use all 8 prayer modes", AchievementCategory.MODE_DIVERSITY, 8, 50, 5, "🎯"),

        // --- FAMOUS PRAYER ---
        AchievementDef("famous_10", "Classic Prayers", "Pray 10 famous prayers", AchievementCategory.FAMOUS_PRAYER, 10, 20, 2, "📜"),
        AchievementDef("famous_50", "Lord's Prayer 50×", "Pray 50 famous prayers", AchievementCategory.FAMOUS_PRAYER, 50, 50, 5, "📜"),
        AchievementDef("famous_100", "100× Warrior", "Pray 100 famous prayers", AchievementCategory.FAMOUS_PRAYER, 100, 100, 10, "⚔️"),

        // --- ANSWERED PRAYER ---
        AchievementDef("answered_1", "God Answers", "Record your first answered prayer", AchievementCategory.ANSWERED_PRAYER, 1, 20, 2, "✅"),
        AchievementDef("answered_10", "Faithful Witness", "Record 10 answered prayers", AchievementCategory.ANSWERED_PRAYER, 10, 50, 5, "✅"),
        AchievementDef("answered_50", "Testimony Builder", "Record 50 answered prayers", AchievementCategory.ANSWERED_PRAYER, 50, 150, 10, "🏆"),

        // --- GRATITUDE ---
        AchievementDef("gratitude_7", "Gratitude Starter", "Log gratitude for 7 days", AchievementCategory.GRATITUDE, 7, 15, 2, "🌱"),
        AchievementDef("gratitude_30", "Thankful Heart", "Log gratitude for 30 consecutive days", AchievementCategory.GRATITUDE, 30, 50, 5, "💛"),
        AchievementDef("gratitude_100", "Abundance Mindset", "Log 100 total gratitude entries", AchievementCategory.GRATITUDE, 100, 100, 8, "🌟"),
        AchievementDef("gratitude_photo_50", "Photo Gratitude", "Log 50 gratitude entries with photos", AchievementCategory.GRATITUDE, 50, 75, 5, "📸"),
        AchievementDef("gratitude_500", "Faithful Thanksgiver", "Log 500 total gratitude entries", AchievementCategory.GRATITUDE, 500, 200, 15, "🙌"),

        // --- GROUP ---
        AchievementDef("group_first", "Group Prayer Warrior", "Join your first Prayer Group", AchievementCategory.GROUP, 1, 15, 2, "👥"),
        AchievementDef("group_pray_50", "Faithful Intercessor", "Pray for 50 group requests", AchievementCategory.GROUP, 50, 50, 5, "🤝"),
        AchievementDef("group_create_5", "Community Builder", "Create 5 Prayer Groups", AchievementCategory.GROUP, 5, 75, 5, "🏗️"),

        // --- LEVELING ---
        AchievementDef("level_5", "Rising Disciple", "Reach Level 5", AchievementCategory.LEVELING, 5, 30, 3, "📈"),
        AchievementDef("level_10", "Double Digits", "Reach Level 10", AchievementCategory.LEVELING, 10, 100, 10, "👑"),
        AchievementDef("level_25", "Devoted Disciple", "Reach Level 25", AchievementCategory.LEVELING, 25, 150, 15, "⭐"),
        AchievementDef("level_50", "Prayer Champion", "Reach Level 50", AchievementCategory.LEVELING, 50, 250, 25, "🏆"),

        // --- XP MILESTONE ---
        AchievementDef("xp_1000", "Thousandaire", "Earn 1,000 total XP", AchievementCategory.XP_MILESTONE, 1000, 25, 3, "💰"),
        AchievementDef("xp_10000", "XP Titan", "Earn 10,000 total XP", AchievementCategory.XP_MILESTONE, 10000, 100, 10, "💎"),

        // --- SESSION ---
        AchievementDef("session_first", "First Prayer", "Complete your first prayer session", AchievementCategory.SESSION, 1, 10, 1, "🎉"),
        AchievementDef("session_100", "Centurion", "Complete 100 prayer sessions", AchievementCategory.SESSION, 100, 75, 5, "💯"),

        // --- TIME OF DAY ---
        AchievementDef("early_bird", "Morning Watch", "Pray before 6 AM", AchievementCategory.TIME_OF_DAY, 1, 15, 2, "🌅"),
        AchievementDef("night_owl", "Evening Meditation", "Pray after 10 PM", AchievementCategory.TIME_OF_DAY, 1, 15, 2, "🌙"),

        // --- COMEBACK ---
        AchievementDef("comeback", "Prodigal Return", "Return to prayer after 7+ day gap", AchievementCategory.COMEBACK, 1, 20, 3, "🕊️")
    )

    /** Lookup by stable ID. */
    fun byId(id: String): AchievementDef? = ALL.find { it.id == id }

    /** All achievements in a given category. */
    fun byCategory(category: AchievementCategory): List<AchievementDef> =
        ALL.filter { it.category == category }
}
