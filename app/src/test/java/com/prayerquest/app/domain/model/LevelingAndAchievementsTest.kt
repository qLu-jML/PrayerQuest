package com.prayerquest.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint-18 sanity suite for the leveling curve + the expanded achievement
 * catalogue. Covers the two big behavioural invariants added this sprint:
 *
 *   1. [Leveling.titleForLevel] must return a purely numerical "Level N"
 *      label — no more named tiers. The app UI and store listing both
 *      rely on this.
 *   2. Every [AchievementCategory] must have at least one [AchievementDef]
 *      in [Achievements.ALL], the ID convention for MODE_MASTERY entries
 *      must resolve to a real [PrayerMode], and no duplicate IDs exist.
 *
 * Kept deliberately lightweight so it can run in the host JVM without
 * Room / Android dependencies.
 */
class LevelingAndAchievementsTest {

    // ═══════════════════════════════════════════════
    // LEVELING — uncapped, purely numerical
    // ═══════════════════════════════════════════════

    @Test
    fun `titleForLevel is always Level N — no named tiers`() {
        // The numerical label is what the UI shows now that the tier names
        // have been scrubbed (Sprint-18). Covers a spread of levels so a
        // drive-by change to titleForLevel would be caught.
        val samples = listOf(1, 2, 5, 10, 25, 50, 75, 100, 250, 1000)
        for (level in samples) {
            assertEquals("Level $level", Leveling.titleForLevel(level))
        }
    }

    @Test
    fun `xpForLevel is strictly increasing and level 1 is 0 XP`() {
        assertEquals(0, Leveling.xpForLevel(1))
        var previous = 0
        // Check that the curve grows monotonically up to a level the user
        // could plausibly reach — 200 covers the entire foreseeable
        // leveling catalogue with a healthy margin.
        for (level in 2..200) {
            val threshold = Leveling.xpForLevel(level)
            assertTrue(
                "xpForLevel($level)=$threshold must exceed previous ($previous)",
                threshold > previous
            )
            previous = threshold
        }
    }

    @Test
    fun `levelForXp inverts xpForLevel at and just above thresholds`() {
        for (level in 1..100) {
            val threshold = Leveling.xpForLevel(level)
            assertEquals(level, Leveling.levelForXp(threshold))
            assertEquals(level, Leveling.levelForXp(threshold + 1))
        }
    }

    @Test
    fun `didLevelUp fires across a boundary and not within one`() {
        val level5 = Leveling.xpForLevel(5)
        val level6 = Leveling.xpForLevel(6)
        assertTrue(Leveling.didLevelUp(level5 - 1, level5))
        assertTrue(!Leveling.didLevelUp(level5, level6 - 1))
    }

    // ═══════════════════════════════════════════════
    // ACHIEVEMENTS — catalogue shape
    // ═══════════════════════════════════════════════

    @Test
    fun `every category has at least one AchievementDef`() {
        val categoriesInCatalogue = Achievements.ALL.map { it.category }.toSet()
        for (category in AchievementCategory.values()) {
            assertTrue(
                "Category $category must have at least one AchievementDef",
                category in categoriesInCatalogue
            )
        }
    }

    @Test
    fun `no duplicate IDs in the catalogue`() {
        val ids = Achievements.ALL.map { it.id }
        assertEquals(
            "Duplicate achievement IDs found",
            ids.size,
            ids.toSet().size
        )
    }

    @Test
    fun `byId returns null for unknown and the expected def for known`() {
        assertNull(Achievements.byId("does_not_exist"))
        assertNotNull(Achievements.byId("streak_3"))
        // Sprint-18 aliases — anchors the DD §3.5.2 Big Celebration wiring
        // contract.
        assertNotNull(Achievements.FIRST_ANSWERED)
        assertEquals("answered_1", Achievements.FIRST_ANSWERED.id)
        assertEquals("answered_10", Achievements.FAITHFUL_10.id)
        assertEquals("answered_50", Achievements.FAITHFUL_50.id)
    }

    @Test
    fun `Sprint-18 new badges are all registered`() {
        // Spot-check — if any of these IDs get renamed, the hot-path
        // evaluator silently stops unlocking the badge because the
        // id-based id branches in GamificationRepository stop matching.
        val sprint18Ids = listOf(
            "longest_30", "longest_60",
            "voice_10", "voice_50",
            "journal_10", "journal_50",
            "sabbath_4", "sabbath_52",
            "testimony_1", "testimony_25",
            "partial_1",
            "names_10", "names_all",
            "famous_distinct_10", "famous_distinct_30",
            "guided_acts_25", "lectio_divina_25", "daily_examen_25",
            "prayer_beads_25", "breath_prayer_25", "daily_office_25",
            "gratitude_100_streak",
            "streak_50",
            "minutes_6000",
            "items_500",
            "answered_100",
            "level_75", "level_100",
            "xp_50000",
            "session_500"
        )
        assertEquals("Sprint-18 should register exactly 30 new badges", 30, sprint18Ids.size)
        for (id in sprint18Ids) {
            assertNotNull("Sprint-18 badge '$id' missing from Achievements.ALL", Achievements.byId(id))
        }
    }

    @Test
    fun `MODE_MASTERY ids resolve to a real PrayerMode`() {
        // The evaluator derives the mode name by stripping the trailing
        // "_N" and uppercasing — guard that every MODE_MASTERY id actually
        // matches a PrayerMode enum name.
        val modeNames = PrayerMode.values().map { it.name }.toSet()
        val modeMasteryDefs = Achievements.ALL.filter { it.category == AchievementCategory.MODE_MASTERY }
        assertTrue("MODE_MASTERY should have at least one def", modeMasteryDefs.isNotEmpty())
        for (def in modeMasteryDefs) {
            val resolved = def.id.substringBeforeLast('_').uppercase()
            assertTrue(
                "MODE_MASTERY id '${def.id}' resolved to '$resolved' which is not a PrayerMode",
                resolved in modeNames
            )
        }
    }

    @Test
    fun `every AchievementDef has a positive target and non-negative rewards`() {
        for (def in Achievements.ALL) {
            assertTrue("${def.id} targetValue must be > 0", def.targetValue > 0)
            assertTrue("${def.id} xpReward must be ≥ 0", def.xpReward >= 0)
            assertTrue("${def.id} coinReward must be ≥ 0", def.coinReward >= 0)
            assertTrue("${def.id} name must be non-blank", def.name.isNotBlank())
        }
    }
}
