package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlin.random.Random

class DailyPrayerReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
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
