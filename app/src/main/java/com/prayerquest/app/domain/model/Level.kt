package com.prayerquest.app.domain.model

/**
 * XP leveling system — same thresholds as ScriptureQuest, prayer-themed titles.
 * 10 tiers from Prayer Novice to Prayer Champion.
 */
object Leveling {
    const val MAX_LEVEL = 10

    // Cumulative XP needed to reach each level (index = level)
    private val THRESHOLDS = intArrayOf(
        0,      // Level 1: 0 XP
        50,     // Level 2: 50 XP
        125,    // Level 3: 125 XP
        240,    // Level 4: 240 XP
        410,    // Level 5: 410 XP
        670,    // Level 6: 670 XP
        1_060,  // Level 7: 1,060 XP
        1_650,  // Level 8: 1,650 XP
        2_535,  // Level 9: 2,535 XP
        3_860   // Level 10: 3,860 XP
    )

    private val TITLES = arrayOf(
        "Prayer Novice",       // Level 1
        "Faithful Seeker",     // Level 2
        "Devoted Disciple",    // Level 3
        "Earnest Petitioner",  // Level 4
        "Grace Warrior",       // Level 5
        "Intercessor",         // Level 6
        "Prayer Sage",         // Level 7
        "Watchman",            // Level 8
        "Prayer Vessel",       // Level 9
        "Prayer Champion"      // Level 10
    )

    /** Returns the level (1-based) for the given cumulative XP. */
    fun levelForXp(totalXp: Int): Int {
        for (i in THRESHOLDS.indices.reversed()) {
            if (totalXp >= THRESHOLDS[i]) return i + 1
        }
        return 1
    }

    /** Cumulative XP required to reach [level]. */
    fun xpForLevel(level: Int): Int {
        val idx = (level - 1).coerceIn(0, THRESHOLDS.lastIndex)
        return THRESHOLDS[idx]
    }

    /** Returns (xpIntoCurrentLevel, xpNeededForNextLevel). */
    fun progressWithinLevel(totalXp: Int): Pair<Int, Int> {
        val level = levelForXp(totalXp)
        val currentThreshold = xpForLevel(level)
        val nextThreshold = if (level >= MAX_LEVEL) currentThreshold else xpForLevel(level + 1)
        return Pair(totalXp - currentThreshold, nextThreshold - currentThreshold)
    }

    /** Did the user cross a level boundary? */
    fun didLevelUp(previousXp: Int, newXp: Int): Boolean {
        return levelForXp(newXp) > levelForXp(previousXp)
    }

    /** Human-readable title for a level. */
    fun titleForLevel(level: Int): String {
        val idx = (level - 1).coerceIn(0, TITLES.lastIndex)
        return TITLES[idx]
    }
}
