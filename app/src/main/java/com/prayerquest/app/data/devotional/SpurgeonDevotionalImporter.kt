package com.prayerquest.app.data.devotional

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prayerquest.app.data.dao.DevotionalDao
import com.prayerquest.app.data.entity.Devotional

/**
 * One-time async importer for the 366-day Spurgeon devotional corpus from
 * assets/devotionals/spurgeon_morning_and_evening.json. Same pattern as
 * FamousPrayerImporter — chunked & idempotent via INSERT OR IGNORE on the
 * Devotional PK.
 *
 * Date keys use MM-DD format (e.g., "01-01", "12-25") so the collection is
 * "evergreen" — the daily devotional worker maps `today` to its MM-DD and
 * looks up the matching entry, which naturally loops each year without
 * needing an annual re-import. 02-29 is included; on non-leap years the
 * worker's lookup naturally falls back to 02-28.
 *
 * History: The v1 import shipped only morning readings (single-slot
 * Spurgeon). v2 ships the full "Morning and Evening" dataset, which is the
 * actual published structure of Spurgeon's book. Both readings for a day
 * live on the same row now — see [Devotional] for column layout.
 *
 * Upgrade behavior: if the existing table was populated from the v1
 * morning-only JSON, `INSERT OR IGNORE` will skip rows we already have
 * (same PK). To actually get the evening text in place we run an upsert
 * pass below when any existing row has a blank eveningPassage.
 */
class SpurgeonDevotionalImporter(
    private val context: Context,
    private val devotionalDao: DevotionalDao
) {

    companion object {
        private const val ASSET_PATH = "devotionals/spurgeon_morning_and_evening.json"
        private const val CHUNK_SIZE = 30
        /** Expected count — 365 days + Feb 29 = 366. */
        private const val EXPECTED_ENTRIES = 366
    }

    /**
     * Import / upgrade the Spurgeon catalogue. Safe to call on every launch:
     *  - If the table is empty (fresh install), insert all entries.
     *  - If the table is full but still missing evening content (v1 → v2
     *    upgrade), upsert every row so the evening fields get written.
     *  - Otherwise, no-op.
     */
    suspend fun importIfNeeded() {
        val existing = devotionalDao.count()
        val needsEveningBackfill = existing > 0 && devotionalDao.countMissingEvening() > 0

        if (existing >= EXPECTED_ENTRIES && !needsEveningBackfill) return

        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<DevotionalJson>>() {}.type
        val entries: List<DevotionalJson> = Gson().fromJson(json, type)

        if (needsEveningBackfill) {
            // Upsert path — existing rows keep their PK but gain the new
            // evening columns. Cheaper than deleting + re-inserting.
            entries.forEach { devotionalDao.upsert(it.toEntity()) }
        } else {
            // Fresh import path — chunked insertOrIgnore keeps memory flat.
            entries.chunked(CHUNK_SIZE).forEach { chunk ->
                devotionalDao.insertAll(chunk.map { it.toEntity() })
            }
        }
    }

    /**
     * DTO mirroring the JSON asset schema. Kept separate from the Room
     * entity so we can version/evolve either side without breaking the
     * other.
     *
     * The v2 schema splits `title`/`scriptureReference`/`passage` into
     * explicit morning/evening variants. Legacy v1 single-slot JSON is
     * no longer supported in the importer (we ship v2 JSON in assets/).
     */
    private data class DevotionalJson(
        val date: String,
        val author: String = "Charles Spurgeon",
        val source: String = "Morning and Evening",
        val morningTitle: String = "",
        val morningScriptureReference: String = "",
        val morningPassage: String = "",
        val eveningTitle: String = "",
        val eveningScriptureReference: String = "",
        val eveningPassage: String = ""
    ) {
        fun toEntity() = Devotional(
            date = date,
            author = author,
            source = source,
            title = morningTitle,
            scriptureReference = morningScriptureReference,
            passage = morningPassage,
            eveningTitle = eveningTitle,
            eveningScriptureReference = eveningScriptureReference,
            eveningPassage = eveningPassage
        )
    }
}
