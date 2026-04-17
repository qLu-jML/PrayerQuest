package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import kotlin.random.Random

class QuestNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userPrefs = (applicationContext as PrayerQuestApplication)
                .container.userPreferences
            if (!QuietHoursGuard.canPostNow(userPrefs)) return Result.success()

            val title = "New daily quests are ready! 🎯"
            val messages = listOf(
                "Complete all 3 for bonus XP.",
                "Today's quests await you - earn rewards!",
                "Start your quest journey for extra rewards.",
                "New challenges are ready to complete.",
                "Unlock rewards by finishing today's quests."
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
        private const val NOTIFICATION_ID = 1003
    }
}
