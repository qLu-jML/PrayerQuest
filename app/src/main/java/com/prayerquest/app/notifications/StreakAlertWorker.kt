package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import java.time.LocalDate
import com.prayerquest.app.R

/**
 * Streak Alert — fires in the evening to warn users they haven't logged a
 * prayer session today and risk breaking their streak. Silent no-op if the
 * user already prayed today OR if we're inside Quiet Hours.
 */
class StreakAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as PrayerQuestApplication).container
            val userPrefs = container.userPreferences
            if (!QuietHoursGuard.canPostNow(userPrefs)) return Result.success()

            // Check if user has already prayed today — no reason to alert
            // if they're safe.
            val hasPrayedToday = checkIfPrayedToday()

            if (!hasPrayedToday) {
                val title = applicationContext.getString(R.string.notifications_don_t_lose_your_streak)
                val messages = listOf(
                    applicationContext.getString(R.string.notifications_pray_now_before_midnight),
                    applicationContext.getString(R.string.notifications_keep_your_streak_alive_with_one_prayer_session),
                    applicationContext.getString(R.string.notifications_your_streak_is_waiting_for_you),
                    applicationContext.getString(R.string.notifications_don_t_let_today_pass_without_prayer),
                    applicationContext.getString(R.string.notifications_finish_strong_pray_before_the_day_ends)
                )
                val message = messages.random()

                NotificationHelper.post(
                    applicationContext,
                    NOTIFICATION_ID,
                    title,
                    message
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Cheap read against DailyActivityDao — if the user logged any activity
     * today, they're safe. Uses the same table that powers the streak
     * check-in logic, so the answer here always matches what the streak
     * repo would see.
     */
    private suspend fun checkIfPrayedToday(): Boolean {
        return try {
            val container = (applicationContext as PrayerQuestApplication).container
            val today = LocalDate.now().toString()
            val activity = container.database.dailyActivityDao().getForDate(today)
            activity != null && activity.sessionsCompleted > 0
        } catch (e: Exception) {
            // If we can't tell, fire the nudge — safer than silently
            // dropping it.
            false
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
    }
}
