package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A prayer recorded in Scripture. Imported from
 * assets/prayers/bible_prayers.json on first launch.
 *
 * Distinct from [FamousPrayer] which holds the curated canon of classic
 * prayers (Lord's Prayer, Serenity Prayer, St. Francis, etc.). Bible
 * prayers are browsed together in the Library's "Bible Prayers" tab.
 *
 * `text` is currently blank — a follow-up pass will populate KJV verse
 * text per reference (public-domain source from openbible.com).
 */
@Entity(tableName = "bible_prayers")
data class BiblePrayer(
    @PrimaryKey val id: String,
    val reference: String,      // e.g. "Genesis 15:2-3"
    val book: String,           // e.g. "Genesis"
    val testament: String,      // "Old" | "New"
    val title: String,          // e.g. "Abraham for an heir"
    val person: String,         // e.g. "Abraham" — primary speaker
    val category: String,       // Petition | Intercession | Thanksgiving | ...
    val description: String,    // Short summary (from hopefaithprayer.com)
    val text: String = "",      // KJV verse text — populated in a later pass
    val userPrayedCount: Int = 0
)
