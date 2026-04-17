package com.prayerquest.app.domain.content

/**
 * A short, two-phrase prayer designed for paced breathing: one phrase on the
 * inhale, one on the exhale. Keeping the phrases short (≤ 6 words each) keeps
 * the rhythm meditative instead of rushed.
 *
 * [tradition] is informational only — the Mode Picker doesn't gate breath
 * prayers by tradition, but the library exposes the label so users curious
 * about the origin ("Orthodox — the Jesus Prayer") can browse by heritage.
 */
data class BreathPrayer(
    val id: String,
    val title: String,
    val inhale: String,
    val exhale: String,
    val tradition: String,
    val description: String
)

/**
 * Curated classical breath prayers bundled with the app (DD §3.4 item 8 — ships
 * with 10+ classical breath prayers). This is the Sprint 3 author-lite pack;
 * Sprint 6 will pair each entry with a public-domain audio companion and add
 * seasonal / liturgical-calendar additions. All text here is either public
 * domain (historic prayers) or original phrasing derived from canonical
 * Scripture — safe to ship without licensing review.
 *
 * Expanding this list is additive: new IDs are safe to append, but do not
 * renumber or remove existing IDs — user stats in [UserPrayerProgress] and
 * future usage badges key off [BreathPrayer.id].
 */
object BreathPrayerLibrary {

    val prayers: List<BreathPrayer> = listOf(
        BreathPrayer(
            id = "jesus_prayer",
            title = "The Jesus Prayer",
            inhale = "Lord Jesus Christ, Son of God",
            exhale = "have mercy on me, a sinner",
            tradition = "Orthodox · Hesychast",
            description = "The ancient prayer of the heart, repeated through the centuries."
        ),
        BreathPrayer(
            id = "be_still",
            title = "Be Still",
            inhale = "Be still",
            exhale = "and know that I am God",
            tradition = "Scripture · Psalm 46:10",
            description = "A simple anchor in anxiety — God's invitation to rest."
        ),
        BreathPrayer(
            id = "lord_here_i_am",
            title = "Here I Am",
            inhale = "Lord, here I am",
            exhale = "send me",
            tradition = "Scripture · Isaiah 6:8",
            description = "A posture of availability and surrender."
        ),
        BreathPrayer(
            id = "my_shepherd",
            title = "The Lord Is My Shepherd",
            inhale = "The Lord is my shepherd",
            exhale = "I shall not want",
            tradition = "Scripture · Psalm 23:1",
            description = "Every breath a reminder of providential care."
        ),
        BreathPrayer(
            id = "maranatha",
            title = "Maranatha",
            inhale = "Come",
            exhale = "Lord Jesus",
            tradition = "Aramaic · 1 Corinthians 16:22",
            description = "The earliest Christian prayer — longing for Christ's presence."
        ),
        BreathPrayer(
            id = "your_kingdom",
            title = "Your Kingdom Come",
            inhale = "Father,",
            exhale = "your kingdom come",
            tradition = "The Lord's Prayer",
            description = "Aligning the heart with the prayer Jesus taught."
        ),
        BreathPrayer(
            id = "abba_father",
            title = "Abba, Father",
            inhale = "Abba",
            exhale = "Father",
            tradition = "Scripture · Romans 8:15",
            description = "The Spirit's own name for God resting on our breath."
        ),
        BreathPrayer(
            id = "holy_spirit_come",
            title = "Holy Spirit, Come",
            inhale = "Holy Spirit",
            exhale = "fill this moment",
            tradition = "Ecumenical",
            description = "An invitation for the Spirit to meet you where you are."
        ),
        BreathPrayer(
            id = "peace_be_still",
            title = "Peace, Be Still",
            inhale = "Peace",
            exhale = "be still",
            tradition = "Scripture · Mark 4:39",
            description = "Christ's word to the storm — now to your own."
        ),
        BreathPrayer(
            id = "in_you_i_trust",
            title = "In You I Trust",
            inhale = "In you, Lord",
            exhale = "I put my trust",
            tradition = "Scripture · Psalm 31:1",
            description = "A confession of dependence, one breath at a time."
        ),
        BreathPrayer(
            id = "create_in_me",
            title = "Create in Me",
            inhale = "Create in me",
            exhale = "a clean heart, O God",
            tradition = "Scripture · Psalm 51:10",
            description = "David's prayer of renewal — shaped to the breath."
        ),
        BreathPrayer(
            id = "veni_sancte_spiritus",
            title = "Veni Sancte Spiritus",
            inhale = "Come, Holy Spirit",
            exhale = "kindle the fire of your love",
            tradition = "Catholic · Pentecost Sequence",
            description = "A medieval hymn shortened to a single breath cycle."
        )
    )

    /** Stable default shown when the user opens Breath Prayer mode for the first time. */
    val default: BreathPrayer = prayers.first { it.id == "jesus_prayer" }
}
