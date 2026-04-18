package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import kotlin.random.Random
import com.prayerquest.app.R

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

            val title = applicationContext.getString(R.string.notifications_time_to_pray)
            val messages = listOf(
                applicationContext.getString(R.string.notifications_a_few_minutes_with_god_can_transform_your_whole_da),
                applicationContext.getString(R.string.notifications_start_your_day_with_prayer_and_peace),
                applicationContext.getString(R.string.notifications_take_a_moment_to_connect_with_what_matters_most),
                applicationContext.getString(R.string.notifications_your_prayers_are_heard_and_valued),
                applicationContext.getString(R.string.notifications_begin_today_with_gratitude_and_faith)
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
