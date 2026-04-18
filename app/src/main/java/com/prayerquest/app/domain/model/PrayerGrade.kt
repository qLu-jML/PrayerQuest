package com.prayerquest.app.domain.model

/**
 * Post-prayer session grade. Feeds XP multiplier and streak logic.
 */
enum class PrayerGrade(
    val displayName: String,
    val xpMultiplier: Float
) {
    AGAIN("Again", 0.5f),
    HARD("Hard", 0.75f),
    GOOD("Good", 1.0f),
    EASY("Easy", 1.25f)
}
