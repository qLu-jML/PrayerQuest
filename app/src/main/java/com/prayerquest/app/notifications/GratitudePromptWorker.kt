package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import kotlin.random.Random

class GratitudePromptWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userPrefs = (applicationContext as PrayerQuestApplication)
                .container.userPreferences
            if (!QuietHoursGuard.canPostNow(userPrefs)) return Result.success()

            val title = "What are you thankful for today? 🌟"
            val messages = listOf(
                "Take a moment to count your blessings.",
                "Gratitude opens the heart - what's yours?",
                "Reflect on today's gifts and blessings.",
                "A grateful heart is a peaceful heart.",
                "Share your gratitude and spread joy."
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
