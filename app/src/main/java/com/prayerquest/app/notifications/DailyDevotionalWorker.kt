package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.preferences.DevotionalAuthor
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Fires up to three times per day (Spurgeon morning, Spurgeon evening,
 * Bonhoeffer evening) to nudge the user toward their devotional reading.
 *
 * Each [DailyDevotionalWorker] instance runs for exactly one slot —
 * [SLOT_SPURGEON_MORNING], [SLOT_SPURGEON_EVENING], or [SLOT_BONHOEFFER] —
 * identified by the worker's inputData. The scheduler enqueues one unique
 * periodic worker per slot.
 *
 * At fire time we respect:
 *  - [DevotionalAuthor] setting — NONE disables all slots; SPURGEON skips
 *    the Bonhoeffer slot; BONHOEFFER skips both Spurgeon slots; BOTH lets
 *    every slot fire.
 *  - Per-slot enable toggles for Spurgeon morning / evening (so a user who
 *    only wants the evening reading isn't woken at 7 AM).
 *  - Quiet Hours (shared with the other workers via [QuietHoursGuard]).
 *
 * Notification title/body pull the actual devotional title + scripture
 * reference so the preview is specific ("Morning devotional ☀️ — Continue
 * in prayer · Colossians 4:2") rather than generic.
 *
 * Data source: the Spurgeon "Morning and Evening" corpus keyed by MM-DD.
 * Bonhoeffer support will ship in a future sprint; until then the
 * BONHOEFFER slot falls back to Spurgeon's reading for the day (so the
 * worker still produces a useful notification).
 */
class DailyDevotionalWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val container = (applicationContext as PrayerQuestApplication).container
            val userPrefs = container.userPreferences

            val slot = inputData.getString(KEY_SLOT) ?: SLOT_SPURGEON_MORNING
            val author = userPrefs.devotionalAuthor.first()
            if (author == DevotionalAuthor.NONE) return Result.success()

            // Author-vs-slot gate. A Spurgeon-only user shouldn't get a
            // Bonhoeffer evening notification if that worker is still
            // somehow enqueued, and vice versa.
            val relevant = when (slot) {
                SLOT_SPURGEON_MORNING, SLOT_SPURGEON_EVENING ->
                    author == DevotionalAuthor.SPURGEON || author == DevotionalAuthor.BOTH
                SLOT_BONHOEFFER ->
                    author == DevotionalAuthor.BONHOEFFER || author == DevotionalAuthor.BOTH
                else -> false
            }
            if (!relevant) return Result.success()

            // Per-Spurgeon-slot enable toggles. Morning and evening can be
            // toggled independently; defaults are both-on when Spurgeon is
            // the chosen author.
            if (slot == SLOT_SPURGEON_MORNING &&
                !userPrefs.devotionalSpurgeonMorningEnabled.first()) {
                return Result.success()
            }
            if (slot == SLOT_SPURGEON_EVENING &&
                !userPrefs.devotionalSpurgeonEveningEnabled.first()) {
                return Result.success()
            }

            if (!QuietHoursGuard.canPostNow(userPrefs)) return Result.success()

            // Map today's calendar date to MM-DD (evergreen yearly loop).
            val today = LocalDate.now()
            val mmdd = "%02d-%02d".format(today.monthValue, today.dayOfMonth)
            val lookupKey = if (mmdd == "02-29") "02-28" else mmdd
            val devotional = container.database.devotionalDao().getForDate(lookupKey)
                ?: return Result.success()  // importer hasn't finished yet

            // Pick the appropriate reading slice + title prefix. The evening
            // slot falls through to the morning reading if no evening text
            // exists (covers Bonhoeffer until its own corpus ships).
            val (titlePrefix, notificationId, readingTitle, readingRef) = when (slot) {
                SLOT_SPURGEON_MORNING ->
                    SlotData("Morning devotional ☀️", NOTIFICATION_ID_MORNING,
                        devotional.title, devotional.scriptureReference)
                SLOT_SPURGEON_EVENING -> {
                    val title = devotional.eveningTitle.ifBlank { devotional.title }
                    val ref = devotional.eveningScriptureReference
                        .ifBlank { devotional.scriptureReference }
                    SlotData("Evening reflection 🌙", NOTIFICATION_ID_EVENING, title, ref)
                }
                SLOT_BONHOEFFER ->
                    SlotData("Bonhoeffer devotional 🌙", NOTIFICATION_ID_BONHOEFFER,
                        devotional.title, devotional.scriptureReference)
                else ->
                    SlotData("Today's devotional ☀️", NOTIFICATION_ID_MORNING,
                        devotional.title, devotional.scriptureReference)
            }

            val preview = buildString {
                append(readingTitle)
                if (readingRef.isNotBlank()) {
                    append(" — ")
                    append(readingRef)
                }
            }.take(120)

            NotificationHelper.post(
                applicationContext,
                notificationId,
                titlePrefix,
                preview
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** Small tuple for the when-branch above. */
    private data class SlotData(
        val titlePrefix: String,
        val notificationId: Int,
        val readingTitle: String,
        val readingRef: String
    )

    companion object {
        const val KEY_SLOT = "slot"
        const val SLOT_SPURGEON_MORNING = "spurgeon_morning"
        const val SLOT_SPURGEON_EVENING = "spurgeon_evening"
        const val SLOT_BONHOEFFER = "bonhoeffer"

        private const val NOTIFICATION_ID_MORNING = 1005
        private const val NOTIFICATION_ID_EVENING = 1006
        private const val NOTIFICATION_ID_BONHOEFFER = 1007
    }
}
