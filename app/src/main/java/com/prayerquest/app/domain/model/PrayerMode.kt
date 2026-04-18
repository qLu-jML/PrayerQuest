package com.prayerquest.app.domain.model

/**
 * The 10 MVP prayer modes. Each mode offers a different way to engage with prayer.
 *
 * Note: three earlier modes have been retired —
 *   - ScriptureSoakMode → DD §9.1 (Future Update)
 *   - ContemplativeSilenceMode → DD §9.2 (Future Update)
 *   - GratitudeBlastMode → absorbed into Gratitude Section as "Speed Round"
 *
 * Do not re-add them to this enum without a DD amendment.
 */
enum class PrayerMode(
    val displayName: String,
    val description: String,
    val baseXp: Int,
    val category: Category
) {
    // Quick (under 5 minutes, low friction)
    FLASH_PRAY_SWIPE(
        displayName = "Flash-Pray Swipe",
        description = "Swipe through prayer cards for quick engagement",
        baseXp = 8,
        category = Category.QUICK
    ),
    BREATH_PRAYER(
        displayName = "Breath Prayer",
        description = "Breathe a short sacred phrase for a few minutes",
        baseXp = 10,
        category = Category.QUICK
    ),
    INTERCESSION_DRILL(
        displayName = "Intercession Drill",
        description = "Tap-bank prayer for your active lists",
        baseXp = 12,
        category = Category.QUICK
    ),

    // Guided (structured prayer frameworks)
    GUIDED_ACTS(
        displayName = "Guided ACTS",
        description = "Adoration · Confession · Thanksgiving · Supplication",
        baseXp = 15,
        category = Category.GUIDED
    ),
    DAILY_EXAMEN(
        displayName = "Daily Examen",
        description = "Ignatian 5-step review of the day",
        baseXp = 15,
        category = Category.GUIDED
    ),
    LECTIO_DIVINA(
        displayName = "Lectio Divina",
        description = "Read · Meditate · Pray · Contemplate over Scripture",
        baseXp = 15,
        category = Category.GUIDED
    ),

    // Expressive (personal voice / writing)
    VOICE_RECORD(
        displayName = "Voice Record & Reflect",
        description = "Pray aloud with real-time transcription",
        baseXp = 12,
        category = Category.EXPRESSIVE
    ),
    PRAYER_JOURNAL(
        displayName = "Prayer Journal",
        description = "Free-type journaling with voice-to-text",
        baseXp = 12,
        category = Category.EXPRESSIVE
    ),

    // Traditional (historic liturgical forms)
    PRAYER_BEADS(
        displayName = "Prayer Beads",
        description = "Rosary, Jesus Prayer rope, or Anglican beads",
        baseXp = 15,
        category = Category.TRADITIONAL
    ),
    DAILY_OFFICE(
        displayName = "Daily Office",
        description = "Fixed-hour prayer: Morning, Midday, Evening, Compline",
        baseXp = 15,
        category = Category.TRADITIONAL
    );

    enum class Category(val displayName: String) {
        QUICK("Quick"),
        GUIDED("Guided"),
        EXPRESSIVE("Expressive"),
        TRADITIONAL("Traditional")
    }

    companion object {
        val TOTAL_MODES = entries.size
    }
}
