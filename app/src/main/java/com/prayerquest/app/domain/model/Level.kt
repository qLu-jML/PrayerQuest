package com.prayerquest.app.domain.model

import kotlin.math.floor
import kotlin.math.pow

/**
 * XP leveling system — purely numerical, no level cap.
 *
 * Uses a smooth curve so early levels come quickly and later levels
 * require progressively more XP.  Formula: threshold(L) = floor(50 × 1.5^(L-2))
 * for L ≥ 2, with level 1 starting at 0 XP.
 */
object Leveling {

    /**
     * Cumulative XP required to reach [level] (1-based).
     * Level 1 = 0 XP.  For level ≥ 2 the thresholds grow exponentially.
     */
    fun xpForLevel(level: Int): Int {
        if (level <= 1) return 0
        // Sum of the geometric series: 50 * (1.5^0 + 1.5^1 + … + 1.5^(level-2))
        // = 50 * (1.5^(level-1) - 1) / 0.5 = 100 * (1.5^(level-1) - 1)
        return floor(100.0 * (1.5.pow(level - 1) - 1)).toInt()
    }

    /** Returns the level (1-based) for the given cumulative XP. */
    fun levelForXp(totalXp: Int): Int {
        var level = 1
        while (xpForLevel(level + 1) <= totalXp) {
            level++
        }
        return level
    }

    /** Returns (xpIntoCurrentLevel, xpNeededForNextLevel). */
    fun progressWithinLevel(totalXp: Int): Pair<Int, Int> {
        val level = levelForXp(totalXp)
        val currentThreshold = xpForLevel(level)
        val nextThreshold = xpForLevel(level + 1)
        return Pair(totalXp - currentThreshold, nextThreshold - currentThreshold)
    }

    /** Did the user cross a level boundary? */
    fun didLevelUp(previousXp: Int, newXp: Int): Boolean {
        return levelForXp(newXp) > levelForXp(previousXp)
    }

    /** Display label for a level — purely numerical. */
    fun titleForLevel(level: Int): String = "Level $level"
}
