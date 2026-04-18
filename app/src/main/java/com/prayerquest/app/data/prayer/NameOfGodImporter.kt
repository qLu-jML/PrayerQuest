package com.prayerquest.app.data.prayer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prayerquest.app.data.dao.NameOfGodDao
import com.prayerquest.app.data.entity.NameOfGod

/**
 * One-time async importer for the Names of God catalogue (DD §3.12).
 *
 * Seeds the `name_of_god` Room table from
 * `app/src/main/assets/prayers/names_of_god.json` on first launch. Same
 * shape as [FamousPrayerImporter] — chunked insert, idempotent via
 * `OnConflictStrategy.IGNORE` in the DAO, guarded by an empty-table check
 * so re-running on every launch is a cheap no-op after the first success.
 *
 * The JSON schema is richer than the [NameOfGod] entity (it carries
 * `transliteration`, `language`, `scriptureText`, `tradition`). This
 * importer is the single mapping boundary between the two — see
 * [NameOfGodJson.toEntity] for the field reconciliation.
 *
 * Wiring: called from [com.prayerquest.app.di.AppContainer]'s first-launch
 * seeding block alongside [FamousPrayerImporter] and [BiblePrayerImporter].
 */
class NameOfGodImporter(
    private val context: Context,
    private val nameOfGodDao: NameOfGodDao
) {

    companion object {
        /**
         * The JSON lives under `prayers/` (not its own `names/` dir) to
         * keep all pre-loaded content the user "prays through" in a single
         * folder. The entity file's KDoc still says `assets/names_of_god/`
         * — that's an outdated comment; the asset path of record is the
         * constant below.
         */
        private const val ASSET_PATH = "prayers/names_of_god.json"

        /**
         * 50 is overkill for a 35-entry catalogue, but matching
         * [FamousPrayerImporter]'s constant keeps the import behavior
         * identical and avoids a surprise if the catalogue grows.
         */
        private const val CHUNK_SIZE = 50
    }

    /**
     * Populate the `name_of_god` table if it's empty. Safe to call on
     * every app start — bails in O(1) once seeded.
     */
    suspend fun importIfNeeded() {
        val existingCount = nameOfGodDao.count()
        if (existingCount > 0) return

        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<NameOfGodJson>>() {}.type
        val names: List<NameOfGodJson> = Gson().fromJson(json, type)

        names.chunked(CHUNK_SIZE).forEach { chunk ->
            val entities = chunk.map { it.toEntity() }
            nameOfGodDao.insertAll(entities)
        }
    }

    /**
     * Wire-format DTO for the authored catalogue. Field defaults let the
     * JSON drop optional keys without breaking the parse — e.g. if a new
     * entry omits `tradition` or `scriptureText` the importer still loads
     * and any missing columns are left at their entity defaults.
     */
    private data class NameOfGodJson(
        val id: String,
        val name: String,
        val transliteration: String = "",
        val language: String = "",
        val meaning: String,
        val scriptureReference: String,
        val scriptureText: String = "",
        val tradition: String = ""
    ) {
        /**
         * Map the (richer) JSON shape into the (flatter) Room entity.
         *
         *  - `transliteration` → `hebrewOrGreek`. Users see "Yahweh" under
         *    "YHWH", which is the intended contemplative reading order.
         *  - `scriptureText` → `description`. Pinning the verse onto the
         *    entity means the detail screen can render the Scripture
         *    grounding without a join across tables.
         *  - `language` and `tradition` are currently dropped because the
         *    UI doesn't render them. Kept in the JSON so content authors
         *    can add them back without another schema migration.
         */
        fun toEntity() = NameOfGod(
            id = id,
            name = name,
            hebrewOrGreek = transliteration,
            meaning = meaning,
            scriptureReference = scriptureReference,
            description = scriptureText
        )
    }
}
