package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A Name of God — content item displayed in the Prayer Library
 * (e.g., Yahweh, Elohim, El Shaddai, Jehovah-Jireh, Abba, Adonai).
 *
 * Seeded idempotently on first launch from
 * `app/src/main/assets/names_of_god/names_of_god.json` via
 * `data/prayer/NamesOfGodImporter.kt` (authoring lands in Sprint 6).
 */
@Entity(tableName = "name_of_god")
data class NameOfGod(
    @PrimaryKey val id: String,
    val name: String,
    val hebrewOrGreek: String = "",
    val meaning: String,
    val scriptureReference: String,
    val description: String = "",
    val userPrayedCount: Int = 0
)
