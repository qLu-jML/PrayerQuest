package com.prayerquest.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.prayerquest.app.MainActivity
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.R
import com.prayerquest.app.data.entity.PrayerItem
import java.util.concurrent.TimeUnit

/**
 * One-shot WorkManager job scheduled for +365 days from the day a prayer
 * was marked answered. Fires a single reminder:
 *
 *   "A year ago today, [answered prayer title] was answered. God is faithful."
 *
 * Tapping the notification deep-links to the Answered Prayer Detail screen
 * for that item so the user can re-read their testimony.
 *
 * Respects [QuietHoursGuard] — if the worker's scheduled-fire time lands
 * inside the user's quiet window we swallow the post. Result is still
 * success() because "no notification during quiet hours" is the desired
 * outcome, not a retry-worthy failure; scheduling a reminder 365 days out
 * and then asking WorkManager to try again in 15 minutes would miss the
 * actual anniversary window.
 *
 * Idempotent: callers must use unique work name "anniversary-$prayerItemId"
 * with [ExistingWorkPolicy.KEEP] so repeat taps on "Mark Answered" don't
 * stack duplicate reminders.
 */
class AnsweredPrayerAnniversaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as PrayerQuestApplication).container
            val userPrefs = container.userPreferences
            if (!QuietHoursGuard.canPostNow(userPrefs)) {
                // Quiet hours — drop quietly. We don't reschedule because the
                // anniversary moment itself is the reminder's reason; firing
                // a day late defeats the point. If we later want to shift
                // into the next allowed minute, do it here.
                return Result.success()
            }

            val prayerId = inputData.getLong(KEY_PRAYER_ID, -1L)
            if (prayerId <= 0L) return Result.success()

            // Pull the current row so the title is fresh — if the user
            // edited the prayer title between "answered" and the anniversary,
            // we show the latest version.
            val prayer: PrayerItem? = container.prayerRepository.getItem(prayerId)

            // Also guard against the item having been deleted or reactivated
            // in the year since. If it's no longer "Answered", we skip —
            // anniversaries only make sense for still-answered prayers.
            if (prayer == null || prayer.status != PrayerItem.STATUS_ANSWERED) {
                return Result.success()
            }

            val title = applicationContext.getString(R.string.notifications_a_year_ago_today)
            val body = applicationContext.getString(R.string.notifications_x_was_answered_god_is_faithful, prayer.title)

            postDeepLinkedNotification(
                prayerId = prayerId,
                title = title,
                body = body
            )

            Result.success()
        } catch (e: Exception) {
            // Swallow + succeed rather than retry: a one-shot worker that
            // retries exponentially can drift into the wrong calendar day,
            // and the anniversary notion is date-specific.
            Result.success()
        }
    }

    /**
     * Posts an anniversary notification whose tap intent carries
     * [DEEP_LINK_PRAYER_ID] so MainActivity can route to the Answered
     * Prayer Detail screen.
     *
     * Pending intent request code is derived from the prayer id so two
     * different anniversaries posted on the same day each own a distinct
     * PendingIntent (otherwise the system would coalesce them and the
     * second tap would open the first prayer).
     */
    private fun postDeepLinkedNotification(
        prayerId: Long,
        title: String,
        body: String
    ) {
        val ctx = applicationContext
        val launchIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(DEEP_LINK_PRAYER_ID, prayerId)
            // Action keeps the intent distinguishable in PendingIntent caches.
            action = ACTION_OPEN_ANNIVERSARY
        }
        val requestCode = (prayerId % Int.MAX_VALUE).toInt().coerceAtLeast(0)
        val pi: PendingIntent = PendingIntent.getActivity(
            ctx,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Channel creation is idempotent and already done by NotificationHelper,
        // but call it again defensively — workers can run before any UI has
        // touched NotificationHelper.
        NotificationHelper.createNotificationChannel(ctx)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(ctx).notify(notificationId(prayerId), notification)
            }
        } else {
            NotificationManagerCompat.from(ctx).notify(notificationId(prayerId), notification)
        }
    }

    private fun notificationId(prayerId: Long): Int {
        // Offset by a base so anniversary notifications don't collide with
        // the other worker notification ids (1001, 1002, 1003 …).
        return (NOTIFICATION_ID_BASE + (prayerId % 100_000)).toInt()
    }

    companion object {
        private const val CHANNEL_ID = "prayer_quest_reminders"
        private const val NOTIFICATION_ID_BASE = 8_000_000
        const val DEEP_LINK_PRAYER_ID = "pq.deep_link.prayer_id"
        const val ACTION_OPEN_ANNIVERSARY = "com.prayerquest.app.ACTION_OPEN_ANNIVERSARY"

        const val KEY_PRAYER_ID = "prayerItemId"

        /** Unique work name seed — guarantees idempotency across re-calls. */
        fun workNameFor(prayerItemId: Long): String = "anniversary-$prayerItemId"

        /**
         * Schedule the +365-day one-shot reminder. Called from the single
         * write-point that marks a prayer answered.
         *
         * Uses [ExistingWorkPolicy.KEEP] so a repeat call (e.g., the user
         * reactivated and re-answered a prayer the same day) does NOT
         * overwrite the original schedule — idempotent.
         *
         * @param answeredAtMs The epoch-ms moment the prayer was answered.
         *                     Delay is computed from this so if we schedule
         *                     just before midnight UTC, the notification
         *                     lands on the correct calendar anniversary in
         *                     the user's locale next year.
         */
        fun schedule(
            context: Context,
            prayerItemId: Long,
            answeredAtMs: Long = System.currentTimeMillis()
        ) {
            if (prayerItemId <= 0L) return

            val targetMs = answeredAtMs + TimeUnit.DAYS.toMillis(365)
            val delayMs = (targetMs - System.currentTimeMillis()).coerceAtLeast(0L)

            val request = OneTimeWorkRequestBuilder<AnsweredPrayerAnniversaryWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_PRAYER_ID to prayerItemId))
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workNameFor(prayerItemId),
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Cancel a pending anniversary reminder — exposed for the
         * "reactivate a prayer" flow so we don't ping the user about an
         * answered prayer they've since re-opened.
         */
        fun cancel(context: Context, prayerItemId: Long) {
            if (prayerItemId <= 0L) return
            WorkManager.getInstance(context).cancelUniqueWork(workNameFor(prayerItemId))
        }

        const val TAG = "answered_anniversary"
    }
}
