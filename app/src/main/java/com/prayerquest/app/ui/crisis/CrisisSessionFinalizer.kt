package com.prayerquest.app.ui.crisis

import android.util.Log

/**
 * The NO-XP path for [CrisisPrayerScreen] and its sub-screens.
 *
 * Every prayer elsewhere in the app finalizes through
 * [com.prayerquest.app.data.repository.GamificationRepository.onPrayerSessionCompleted].
 * That path awards XP, advances streak state, increments quest progress,
 * touches achievement checkpoints, and writes a [com.prayerquest.app.data.entity.PrayerRecord].
 *
 * Crisis Prayer is specified as explicitly un-gamified (DD §3.10 line 264:
 * *"Fully offline. No XP awarded (intentional — this is not about
 * gamification)."*). To keep that guarantee from drifting, the Crisis flow
 * finalizes through THIS object instead of the regular hot path. The file
 * intentionally has **no** `com.prayerquest.app.data.repository` imports —
 * there is nothing here to grow into an "award a tiny bit of XP because the
 * user breathed for a while" accident.
 *
 * If you find yourself wanting to wire XP in from a crisis entry point,
 * re-read DD §3.10 and the user-wellbeing note in the system prompt. The
 * right move is almost always to log more context here, not to reach into
 * the gamification repo.
 */
object CrisisSessionFinalizer {

    /** Logcat tag for grep-friendly filtering during verification. */
    const val LOG_TAG = "CrisisPrayer"

    /**
     * Marker string intentionally printed on every Crisis finalization so the
     * QA checklist (DD §3.10 verification) can grep `adb logcat` for it and
     * be sure no gamification path fired. Do not change the literal — the
     * unit test in `CrisisModeTest` asserts on it.
     */
    const val NO_XP_SENTINEL = "NO_XP_AWARDED"

    /**
     * Simple return type so callers can render a brief confirmation summary
     * without needing to re-derive the numbers. There is deliberately NO
     * `xpEarned` field — a compile-time signal that this path doesn't award
     * XP.
     */
    data class CrisisResult(
        val phase: Phase,
        val elapsedSeconds: Int,
        val breathCycles: Int = 0,
        val psalmsRead: Int = 0
    )

    enum class Phase { BREATH, PSALMS, RESOURCES, HOME }

    /**
     * Call at the end of a Crisis breath session. Accepts the cycle/elapsed
     * counters purely for telemetry; returns a [CrisisResult] for any UI
     * that wants to acknowledge the session without scoring it.
     */
    fun finalizeBreathSession(elapsedSeconds: Int, breathCycles: Int): CrisisResult {
        Log.i(
            LOG_TAG,
            "breath session finalized · ${elapsedSeconds}s · $breathCycles cycles · $NO_XP_SENTINEL"
        )
        return CrisisResult(
            phase = Phase.BREATH,
            elapsedSeconds = elapsedSeconds,
            breathCycles = breathCycles
        )
    }

    /**
     * Call when the user leaves the paged Psalms reader. `psalmsRead` is the
     * number of cards the user swiped through — useful for telemetry, not
     * XP.
     */
    fun finalizePsalmsSession(elapsedSeconds: Int, psalmsRead: Int): CrisisResult {
        Log.i(
            LOG_TAG,
            "psalms session finalized · ${elapsedSeconds}s · $psalmsRead psalms · $NO_XP_SENTINEL"
        )
        return CrisisResult(
            phase = Phase.PSALMS,
            elapsedSeconds = elapsedSeconds,
            psalmsRead = psalmsRead
        )
    }

    /**
     * Logged whenever the Crisis Resources screen is opened. Does not record
     * whether the user dialed a helpline — that would require touching the
     * phone-dialer return state, and the wellbeing guidance is to make
     * calling a helpline feel invisible, not tracked.
     */
    fun logResourcesOpened() {
        Log.i(LOG_TAG, "crisis resources opened · $NO_XP_SENTINEL")
    }

    /** Logged when the root Crisis screen is opened. Pure telemetry. */
    fun logCrisisEntered() {
        Log.i(LOG_TAG, "crisis prayer entered · $NO_XP_SENTINEL")
    }
}
