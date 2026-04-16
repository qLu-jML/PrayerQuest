package com.prayerquest.app.domain.model

/**
 * The 8 MVP prayer modes. Each mode offers a different way to engage with prayer.
 */
enum class PrayerMode(
    val displayName: String,
    val description: String,
    val baseXp: Int
) {
    GUIDED_ACTS(
        displayName = "Guided ACTS",
        description = "Step-by-step Adoration, Confession, Thanksgiving, Supplication",
        baseXp = 15
    ),
    VOICE_RECORD(
        displayName = "Voice Record & Reflect",
        description = "Pray aloud with real-time transcription",
        baseXp = 12
    ),
    PRAYER_JOURNAL(
        displayName = "Prayer Journal",
        description = "Free-type journaling with voice-to-text",
        baseXp = 12
    ),
    GRATITUDE_BLAST(
        displayName = "Gratitude Blast",
        description = "Rapid-fire blessings with speed bonus",
        baseXp = 10
    ),
    INTERCESSION_DRILL(
        displayName = "Intercession Drill",
        description = "Tap-bank prayer for your active lists",
        baseXp = 12
    ),
    SCRIPTURE_SOAK(
        displayName = "Scripture Soak",
        description = "Pray over Scripture with personalized words",
        baseXp = 15
    ),
    CONTEMPLATIVE_SILENCE(
        displayName = "Contemplative Silence",
        description = "Timer with gentle prompts for listening prayer",
        baseXp = 10
    ),
    FLASH_PRAY_SWIPE(
        displayName = "Flash-Pray Swipe",
        description = "Swipe through prayer cards for quick engagement",
        baseXp = 8
    );

    companion object {
        val TOTAL_MODES = entries.size
    }
}
