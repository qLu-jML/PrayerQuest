package com.prayerquest.app.ui.crisis

import com.prayerquest.app.ui.crisis.CrisisSessionFinalizer.NO_XP_SENTINEL
import com.prayerquest.app.ui.crisis.CrisisSessionFinalizer.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guard rails for Crisis Prayer Mode's NO-XP contract (DD §3.10).
 *
 * These tests exist because the contract is structural — nothing inside the
 * Crisis flow should reach into [com.prayerquest.app.data.repository.GamificationRepository].
 * A future well-intentioned contributor might want to hand out "just a
 * little" XP for surviving a crisis moment. That would regress the DD. This
 * file is how we catch that at CI time instead of in user feedback.
 *
 * The guardrail is TWO-fold:
 *
 * 1. **Behavioural** — [CrisisSessionFinalizer] is a log-only object that
 *    returns a telemetry result with no `xpEarned` field. The first few
 *    tests exercise it and confirm the shape.
 *
 * 2. **Structural** — [no gamification import in any ui/crisis source]
 *    scans every `.kt` file under `ui/crisis` and asserts the crisis flow
 *    does not `import` `GamificationRepository`, `UserStatsDao`,
 *    `StreakDao`, `DailyQuestDao`, or `AchievementProgressDao`. If any of
 *    those imports appear, the test fails — the Crisis screen would be
 *    able to touch gamified state, and that's a DD regression.
 */
class CrisisModeTest {

    // ── 1. Behavioural contract on the finalizer ──────────────────────

    @Test
    fun `finalizeBreathSession returns telemetry with no XP field`() {
        val result = CrisisSessionFinalizer.finalizeBreathSession(
            elapsedSeconds = 60,
            breathCycles = 6
        )
        assertEquals(Phase.BREATH, result.phase)
        assertEquals(60, result.elapsedSeconds)
        assertEquals(6, result.breathCycles)
        assertEquals(0, result.psalmsRead)
        // There is no xpEarned field on CrisisResult — confirm reflectively.
        val fields = result.javaClass.declaredFields.map { it.name }.toSet()
        assertFalse(
            "CrisisResult must NOT carry an xpEarned field — that would be " +
                "the nose of the camel under the tent; got $fields",
            "xpEarned" in fields
        )
        assertFalse("xp" in fields)
    }

    @Test
    fun `finalizePsalmsSession returns telemetry with no XP field`() {
        val result = CrisisSessionFinalizer.finalizePsalmsSession(
            elapsedSeconds = 120,
            psalmsRead = 3
        )
        assertEquals(Phase.PSALMS, result.phase)
        assertEquals(120, result.elapsedSeconds)
        assertEquals(3, result.psalmsRead)
        assertEquals(0, result.breathCycles)
    }

    @Test
    fun `NO_XP_SENTINEL literal is stable — adb logcat greps depend on it`() {
        // This is what the DD §3.10 verification checklist greps for in
        // logcat to confirm no XP path fired. Changing the literal without
        // updating the QA doc breaks verification, so it's locked here.
        assertEquals("NO_XP_AWARDED", NO_XP_SENTINEL)
    }

    // ── 2. Structural guardrail on ui/crisis imports ──────────────────

    @Test
    fun `no gamification repository or DAO import appears in any ui crisis source`() {
        val crisisDir = locateCrisisSourceDir()
        assertNotNull(
            "Expected to find app/src/main/java/.../ui/crisis — test running from " +
                "unexpected working dir? cwd=${File(".").absolutePath}",
            crisisDir
        )
        val sources = crisisDir!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        assertTrue(
            "Expected at least one crisis source; found none under ${crisisDir.path}",
            sources.isNotEmpty()
        )

        // The banned imports. Comments referencing these (KDoc "see …") are
        // fine — we only reject actual `import` lines, so a grep for a line
        // that starts (after whitespace) with `import ` and contains the
        // banned symbol is the precise rule.
        val bannedSymbols = listOf(
            "GamificationRepository",
            "UserStatsDao",
            "StreakDao",
            "DailyQuestDao",
            "AchievementProgressDao",
            "PrayerRecordDao"
        )

        val violations = mutableListOf<String>()
        for (source in sources) {
            source.forEachLine { rawLine ->
                val line = rawLine.trimStart()
                if (!line.startsWith("import ")) return@forEachLine
                for (symbol in bannedSymbols) {
                    if (symbol in line) {
                        violations += "${source.name}: $line"
                    }
                }
            }
        }
        assertTrue(
            "Crisis flow must not import any gamification or XP-writing type. " +
                "Found: $violations",
            violations.isEmpty()
        )
    }

    @Test
    fun `no onPrayerSessionCompleted or onGratitudeLogged call appears in ui crisis source`() {
        // Belt-and-braces: even if somebody accidentally depended on a
        // transitively-imported repo via a container reference, we want any
        // use of the well-known "award XP" entry points to fail the test.
        val crisisDir = locateCrisisSourceDir() ?: return
        val sources = crisisDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }

        val bannedCalls = listOf(
            ".onPrayerSessionCompleted(",
            ".onGratitudeLogged(",
            ".onPrayerMarkedAnswered(",
            ".addXpAndSetLevel(",
            ".incrementFamousPrayers(",
            ".incrementGroupPrayers("
        )

        val violations = mutableListOf<String>()
        for (source in sources) {
            source.forEachLine { rawLine ->
                // Ignore KDoc / line comments. Multi-line /* */ blocks that
                // simply mention a banned call are trimmed to the nearest
                // non-whitespace character; if that's `*` or `/` we skip.
                val trimmed = rawLine.trimStart()
                if (trimmed.startsWith("//") || trimmed.startsWith("*") ||
                    trimmed.startsWith("/*")
                ) return@forEachLine
                for (call in bannedCalls) {
                    if (call in rawLine) {
                        violations += "${source.name}: ${rawLine.trim()}"
                    }
                }
            }
        }
        assertTrue(
            "Crisis flow must not invoke any XP-writing entry point; got: $violations",
            violations.isEmpty()
        )
    }

    // ── 3. Absence of xpEarned field on CrisisResult ──────────────────

    @Test
    fun `CrisisResult explicitly omits xpEarned as a compile-time signal`() {
        val cls = CrisisSessionFinalizer.CrisisResult::class.java
        val ctorParams = cls.declaredConstructors.first().parameters
            .map { it.name }.toSet()
        assertNull(
            "CrisisResult ctor must not accept xpEarned; got $ctorParams",
            ctorParams.firstOrNull { it.equals("xpEarned", ignoreCase = true) }
        )
    }

    // ── helpers ───────────────────────────────────────────────────────

    /**
     * Locate `app/src/main/java/com/prayerquest/app/ui/crisis` from the
     * Gradle unit-test working directory. Gradle runs tests with the module
     * (`app/`) as the working dir by default, so the relative path resolves
     * without additional configuration. Returns null if the directory
     * genuinely isn't there — callers decide what to do about that.
     */
    private fun locateCrisisSourceDir(): File? {
        val candidates = listOf(
            "src/main/java/com/prayerquest/app/ui/crisis",
            "app/src/main/java/com/prayerquest/app/ui/crisis"
        )
        return candidates
            .map { File(it) }
            .firstOrNull { it.exists() && it.isDirectory }
    }
}
