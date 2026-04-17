package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One day's devotional reading — typically pulled from a public-domain
 * classic (Spurgeon's Morning & Evening, Bonhoeffer's God Is in the Manger,
 * etc.). Surfaced on the Home dashboard and in the Library.
 *
 * Keyed by MM-DD (evergreen — loops yearly) so the DevotionalWorker can fetch
 * "today's" reading deterministically.
 *
 * Spurgeon's original "Morning and Evening" gives two readings per day. We
 * store both on the same row so the evening notification can pull the
 * matching entry without an extra table + join. `title` / `scriptureReference`
 * / `passage` are the MORNING fields (kept unprefixed for backwards compat
 * with the v5 schema). The evening-prefixed fields are null/empty when no
 * evening reading exists (e.g., a Bonhoeffer-only row added later).
 */
@Entity(tableName = "devotional")
data class Devotional(
    @PrimaryKey val date: String,          // MM-DD (loops yearly; Feb-29 silently falls to 02-28)
    val author: String,                    // e.g., "Charles Spurgeon"
    val source: String = "",               // e.g., "Morning and Evening"

    // Morning reading (the original v5 columns — renaming would break
    // migrations, so we keep the unprefixed names as the "morning" slot).
    val title: String,
    val scriptureReference: String = "",
    val passage: String,

    // Evening reading. Empty strings — not null — keep the DAO's SELECT
    // returning non-null columns and make the "no evening content yet" UI
    // path trivially checkable via .isBlank().
    val eveningTitle: String = "",
    val eveningScriptureReference: String = "",
    val eveningPassage: String = "",

    val readCount: Int = 0,
    val lastReadAt: Long? = null
)
