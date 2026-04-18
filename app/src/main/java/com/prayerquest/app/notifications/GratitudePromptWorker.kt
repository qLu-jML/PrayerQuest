package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import kotlin.random.Random
import com.prayerquest.app.R

class GratitudePromptWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userPrefs = (applicationContext as PrayerQuestApplication)
                .container.userPreferences
            if (!QuietHoursGuard.canPostNow(userPrefs)) return Result.success()

            val title = applicationContext.getString(R.string.notifications_what_are_you_thankful_for_today)
            val messages = listOf(
                applicationContext.getString(R.string.notifications_take_a_moment_to_count_your_blessings),
                applicationContext.getString(R.string.notifications_gratitude_opens_the_heart_what_s_yours),
                applicationContext.getString(R.string.notifications_reflect_on_today_s_gifts_and_blessings),
                applicationContext.getString(R.string.notifications_a_grateful_heart_is_a_peaceful_heart),
                applicationContext.getString(R.string.notifications_share_your_gratitude_and_spread_joy)
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
        private const val NOTIFICATION_ID = 1004
    }
}
