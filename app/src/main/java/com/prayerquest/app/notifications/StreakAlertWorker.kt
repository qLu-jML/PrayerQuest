package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import java.time.LocalDate

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
                val title = "Don't lose your 🔥 streak!"
                val messages = listOf(
                    "Pray now before midnight.",
                    "Keep your streak alive with one prayer session.",
                    "Your streak is waiting for you!",
                    "Don't let today pass without prayer.",
                    "Finish strong - pray before the day ends."
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
