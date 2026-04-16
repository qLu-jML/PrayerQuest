package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.prayerquest.app.data.repository.PrayerSessionRepository
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class StreakAlertWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // Check if user has prayed today
            val hasPrayedToday = runBlocking {
                checkIfPrayedToday()
            }

            // Only fire if no sessions logged today
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

    private suspend fun checkIfPrayedToday(): Boolean {
        // This would be implemented based on your database structure
        // For now, returning false to ensure notification fires
        return false
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
    }
}
