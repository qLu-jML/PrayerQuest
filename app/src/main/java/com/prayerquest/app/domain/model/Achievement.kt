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
    COMEBACK,

    // ── Sprint-18 badge expansion (2026-04-18) ────────────────────────────
    /** Reads `UserStats.longestSessionMinutes`. Thirty Still / Holy Hour. */
    LONGEST_SESSION,
    /** Reads `UserStats.totalVoiceSessions`. Spoken Prayers / Voice of Prayer. */
    VOICE,
    /** Reads `UserStats.totalJournalSessions`. Prayer Scribe / Prayer Journalist. */
    JOURNAL,
    /** Reads `UserStats.totalSundaySessions`. Sabbath Rhythm / Year of Sabbaths. */
    SABBATH,
    /** Derived via PrayerItemDao.getAnsweredWithTestimonyCount(). First Testimony / Book of Testimonies. */
    TESTIMONY,
    /** Derived via PrayerItemDao.getPartiallyAnsweredCount(). Already But Not Yet. */
    PARTIAL_ANSWER,
    /** Derived via NameOfGodDao.getDistinctPrayedCount(). Names He Answers By / All the Names. */
    NAMES_OF_GOD,
    /** Derived via FamousPrayerDao.getDistinctPrayedCount(). Heritage of Prayer / Saints of the Church. */
    FAMOUS_DISTINCT,
    /**
     * Per-mode session counts via PrayerRecordDao.getCountByMode(mode). The
     * [AchievementDef.id] is `<modeSlug>_N` and encodes which mode to query;
     * see [GamificationRepository.getValueForAchievement].
     */
    MODE_MASTERY
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
        AchievementDef("comeback", "Prodigal Return", "Return to prayer after 7+ day gap", AchievementCategory.COMEBACK, 1, 20, 3, "🕊️"),

        // ── Sprint-18 badge expansion (2026-04-18) ──────────────────────────
        // 30 new achievements across 9 new categories plus top-end extensions
        // to the existing ones. Categories / IDs are referenced by
        // GamificationRepository.getValueForAchievement — keep the id
        // conventions consistent (`<category>_<target>` or `<modeSlug>_N`).

        // --- LONGEST_SESSION (reads UserStats.longestSessionMinutes) ---
        AchievementDef("longest_30", "Thirty Still", "Complete a single 30-minute prayer session", AchievementCategory.LONGEST_SESSION, 30, 20, 2, "🧘"),
        AchievementDef("longest_60", "Holy Hour", "Complete a single 60-minute prayer session", AchievementCategory.LONGEST_SESSION, 60, 50, 5, "⏰"),

        // --- VOICE (reads UserStats.totalVoiceSessions) ---
        AchievementDef("voice_10", "Spoken Prayers", "Log 10 prayer sessions using voice transcription", AchievementCategory.VOICE, 10, 25, 3, "🎙️"),
        AchievementDef("voice_50", "Voice of Prayer", "Log 50 prayer sessions using voice transcription", AchievementCategory.VOICE, 50, 75, 6, "🗣️"),

        // --- JOURNAL (reads UserStats.totalJournalSessions) ---
        AchievementDef("journal_10", "Prayer Scribe", "Log 10 journaled prayer sessions", AchievementCategory.JOURNAL, 10, 25, 3, "✍️"),
        AchievementDef("journal_50", "Prayer Journalist", "Log 50 journaled prayer sessions", AchievementCategory.JOURNAL, 50, 75, 6, "📖"),

        // --- SABBATH (reads UserStats.totalSundaySessions) ---
        AchievementDef("sabbath_4", "Sabbath Rhythm", "Pray on 4 different Sundays", AchievementCategory.SABBATH, 4, 20, 2, "🕊️"),
        AchievementDef("sabbath_52", "Year of Sabbaths", "Pray on 52 different Sundays", AchievementCategory.SABBATH, 52, 150, 10, "🙌"),

        // --- TESTIMONY (derived via PrayerItemDao.getAnsweredWithTestimonyCount) ---
        AchievementDef("testimony_1", "First Testimony", "Add a testimony to your first answered prayer", AchievementCategory.TESTIMONY, 1, 20, 2, "📝"),
        AchievementDef("testimony_25", "Book of Testimonies", "Record 25 testimonies on answered prayers", AchievementCategory.TESTIMONY, 25, 100, 8, "📚"),

        // --- PARTIAL_ANSWER (derived via PrayerItemDao.getPartiallyAnsweredCount) ---
        AchievementDef("partial_1", "Already But Not Yet", "Mark a prayer as partially answered", AchievementCategory.PARTIAL_ANSWER, 1, 15, 2, "🌱"),

        // --- NAMES_OF_GOD (derived via NameOfGodDao.getDistinctPrayedCount) ---
        AchievementDef("names_10", "Names He Answers By", "Pray 10 distinct Names of God", AchievementCategory.NAMES_OF_GOD, 10, 30, 3, "✡️"),
        AchievementDef("names_all", "All the Names", "Pray 30 distinct Names of God", AchievementCategory.NAMES_OF_GOD, 30, 200, 15, "👑"),

        // --- FAMOUS_DISTINCT (derived via FamousPrayerDao.getDistinctPrayedCount) ---
        AchievementDef("famous_distinct_10", "Heritage of Prayer", "Pray 10 distinct famous prayers", AchievementCategory.FAMOUS_DISTINCT, 10, 30, 3, "📜"),
        AchievementDef("famous_distinct_30", "Saints of the Church", "Pray 30 distinct famous prayers", AchievementCategory.FAMOUS_DISTINCT, 30, 100, 8, "⛪"),

        // --- MODE_MASTERY (derived via PrayerRecordDao.getCountByMode)  ---
        // IDs use the PrayerMode enum name (lowercased) so the evaluator can
        // resolve `mode.name` by uppercasing the prefix. Target 25 across
        // all modes for consistency.
        AchievementDef("guided_acts_25", "ACTS of Devotion", "Complete 25 Guided ACTS sessions", AchievementCategory.MODE_MASTERY, 25, 50, 4, "⚓"),
        AchievementDef("lectio_divina_25", "Lectio Faithful", "Complete 25 Lectio Divina sessions", AchievementCategory.MODE_MASTERY, 25, 50, 4, "📖"),
        AchievementDef("daily_examen_25", "Examined Life", "Complete 25 Daily Examen sessions", AchievementCategory.MODE_MASTERY, 25, 50, 4, "🔍"),
        AchievementDef("prayer_beads_25", "Beads of Prayer", "Complete 25 Prayer Beads sessions", AchievementCategory.MODE_MASTERY, 25, 50, 4, "📿"),
        AchievementDef("breath_prayer_25", "Breath of Life", "Complete 25 Breath Prayer sessions", AchievementCategory.MODE_MASTERY, 25, 50, 4, "🫁"),
        AchievementDef("daily_office_25", "Fixed Hours", "Complete 25 Daily Office sessions", AchievementCategory.MODE_MASTERY, 25, 50, 4, "🕰️"),

        // --- GRATITUDE (long-streak extension) ---
        AchievementDef("gratitude_100_streak", "Hundred Thankful Days", "String together a 100-day gratitude streak", AchievementCategory.GRATITUDE, 100, 250, 20, "💯"),

        // --- STREAK (long-term extension) ---
        AchievementDef("streak_50", "Ever Burning", "50-day prayer streak", AchievementCategory.STREAK, 50, 100, 8, "🔥"),

        // --- PRAYER_MINUTES (top-end extension) ---
        AchievementDef("minutes_6000", "Hundred Hours", "Pray for 100 total hours", AchievementCategory.PRAYER_MINUTES, 6000, 300, 20, "💎"),

        // --- ITEMS_PRAYED (top-end extension) ---
        AchievementDef("items_500", "Book of Remembrance", "Pray for 500 different items", AchievementCategory.ITEMS_PRAYED, 500, 200, 15, "📓"),

        // --- ANSWERED_PRAYER (top-end extension) ---
        AchievementDef("answered_100", "Book of Miracles", "Record 100 answered prayers", AchievementCategory.ANSWERED_PRAYER, 100, 300, 20, "🪄"),

        // --- LEVELING (infinite-curve extension) ---
        AchievementDef("level_75", "Seasoned Intercessor", "Reach Level 75", AchievementCategory.LEVELING, 75, 400, 30, "⭐"),
        AchievementDef("level_100", "Centurion of Prayer", "Reach Level 100", AchievementCategory.LEVELING, 100, 500, 50, "💯"),

        // --- XP_MILESTONE (top-end extension) ---
        AchievementDef("xp_50000", "XP Luminary", "Earn 50,000 total XP", AchievementCategory.XP_MILESTONE, 50000, 250, 20, "💠"),

        // --- SESSION (top-end extension) ---
        AchievementDef("session_500", "Consistency", "Complete 500 prayer sessions", AchievementCategory.SESSION, 500, 200, 15, "🎯")
    )

    /** Lookup by stable ID. */
    fun byId(id: String): AchievementDef? = ALL.find { it.id == id }

    /** All achievements in a given category. */
    fun byCategory(category: AchievementCategory): List<AchievementDef> =
        ALL.filter { it.category == category }

    // ─────────────────────────────────────────────────────────────────────
    // Answered-prayer milestone aliases (DD §3.5.2 Big Celebration Moment)
    //
    // The Big Celebration wiring spec refers to these by the canonical
    // names FIRST_ANSWERED, FAITHFUL_10, FAITHFUL_50. They already ship
    // under category-based IDs (`answered_1` / `answered_10` / `answered_50`
    // — which is the shape our `evaluateAchievements()` loop expects), so
    // rather than duplicate rows we expose the DD-level names as constants
    // that point at the existing entries. Call-sites that want to unlock
    // "FIRST_ANSWERED" resolve it through here so the mapping stays in one
    // place and the Achievements catalog stays canonical.
    // ─────────────────────────────────────────────────────────────────────

    /** "First prayer marked answered" milestone. */
    val FIRST_ANSWERED: AchievementDef get() = byId("answered_1")!!

    /** "10 answered prayers" — Faithful Witness milestone. */
    val FAITHFUL_10: AchievementDef get() = byId("answered_10")!!

    /** "50 answered prayers" — Testimony Builder milestone. */
    val FAITHFUL_50: AchievementDef get() = byId("answered_50")!!
}
