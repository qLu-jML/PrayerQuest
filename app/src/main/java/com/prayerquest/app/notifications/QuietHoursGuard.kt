package com.prayerquest.app.notifications

import com.prayerquest.app.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Shared guard that every notification worker consults before posting.
 *
 * Quiet Hours are user-configured in Settings (default 22:00 → 07:00). A
 * worker whose scheduled-fire-time falls inside that window MUST no-op
 * instead of posting — violating this is a P0 bug per Sprint 4 scope.
 *
 * Windows that cross midnight (e.g. 22:00 → 07:00) are handled with a
 * simple wrap-around check on minute-of-day. We compare against the device
 * clock at post time rather than the scheduled time because periodic
 * WorkManager jobs can drift, and a user who moved their quiet-hours
 * boundary since scheduling expects the *current* window to apply.
 */
object QuietHoursGuard {

    /**
     * True if we should post now. Reads the user's current preferences
     * fresh from the Flow every call — the guard is called from a worker
     * that runs once per day, so the overhead is irrelevant.
     *
     * Returns true (allowed) when:
     *  - quiet hours are disabled entirely, OR
     *  - the window is zero-width (start == end), OR
     *  - the current minute-of-day is outside the configured window.
     */
    suspend fun canPostNow(userPreferences: UserPreferences): Boolean {
        val enabled = userPreferences.quietHoursEnabled.first()
        if (!enabled) return true

        val startMin = userPreferences.quietHoursStartMin.first()
        val endMin = userPreferences.quietHoursEndMin.first()
        if (startMin == endMin) return true  // zero-width window = always allowed

        val nowMin = currentMinuteOfDay()
        return !isInWindow(nowMin, startMin, endMin)
    }

    /**
     * Pure check — exposed for unit testing and for UI preview helpers
     * (Settings screen shows "Quiet now" when the user's *current* time
     * falls inside the window they're configuring).
     *
     * Windows that cross midnight (startMin > endMin) are handled by
     * OR'ing the two halves: [start, 1440) ∪ [0, end).
     */
    fun isInWindow(minuteOfDay: Int, startMin: Int, endMin: Int): Boolean {
        if (startMin == endMin) return false
        return if (startMin < endMin) {
            // Simple case: 13:00 → 17:00
            minuteOfDay in startMin until endMin
        } else {
            // Midnight-wrapping: 22:00 → 07:00
            minuteOfDay >= startMin || minuteOfDay < endMin
        }
    }

    private fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
