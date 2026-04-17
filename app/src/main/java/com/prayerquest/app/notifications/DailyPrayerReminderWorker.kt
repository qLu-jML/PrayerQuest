package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import kotlin.random.Random

/**
 * Converted to CoroutineWorker so we can consult the Flow-based
 * UserPreferences for Quiet Hours before posting. Violating Quiet Hours
 * is a P0 bug, so every post path goes through [QuietHoursGuard].
 */
class DailyPrayerReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userPrefs = (applicationContext as PrayerQuestApplication)
                .container.userPreferences
            if (!QuietHoursGuard.canPostNow(userPrefs)) {
                // Silently skip — we'll fire again tomorrow at the same slot.
                return Result.success()
            }

            val title = "Time to pray! 🙏"
            val messages = listOf(
                "A few minutes with God can transform your whole day.",
                "Start your day with prayer and peace.",
                "Take a moment to connect with what matters most.",
                "Your prayers are heard and valued.",
                "Begin today with gratitude and faith."
            )
            val message = messages[Random.nextInt(messages.size)]

            NotificationHelper.post(
                applicationContext,
                NOTIFICATION_ID,
                title,
                message
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
