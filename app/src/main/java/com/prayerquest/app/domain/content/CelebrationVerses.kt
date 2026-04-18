package com.prayerquest.app.domain.content

/**
 * Curated thanksgiving / praise Scripture passages surfaced by the Big
 * Celebration Moment (DD §3.5.2) when a user marks a prayer "Answered."
 *
 * The catalog is deliberately small (~20 entries) so each verse can be
 * hand-picked for tone — warm, grateful, God-glorifying — rather than
 * scraped from a larger database. Translations are KJV / WEB where
 * possible so we avoid bundling copyrighted text in the APK.
 *
 * Pure data, no dependencies. Pickers read from [ALL] and may choose a
 * random entry or filter by [CelebrationVerse.tone] if a future caller
 * wants mood-specific copy.
 */
data class CelebrationVerse(
    /** Stable ID so we can A/B test specific verses without swapping order. */
    val id: String,
    /** Verse body — the "quote" line shown inside the card. */
    val text: String,
    /** Human-readable reference, e.g. "Psalm 118:24". */
    val reference: String,
    /** Optional translation marker for analytics + the share-sheet caption. */
    val translation: String = "WEB",
    /** Rough emotional tone for future picker filtering. */
    val tone: Tone = Tone.THANKSGIVING
) {
    enum class Tone { THANKSGIVING, FAITHFULNESS, PRAISE, JOY }
}

object CelebrationVerses {

    val ALL: List<CelebrationVerse> = listOf(
        CelebrationVerse(
            id = "psalm_118_24",
            text = "This is the day that the Lord has made. We will rejoice and be glad in it!",
            reference = "Psalm 118:24",
            tone = CelebrationVerse.Tone.JOY
        ),
        CelebrationVerse(
            id = "psalm_107_1",
            text = "Give thanks to the Lord, for he is good, for his loving kindness endures forever.",
            reference = "Psalm 107:1",
            tone = CelebrationVerse.Tone.THANKSGIVING
        ),
        CelebrationVerse(
            id = "psalm_116_1",
            text = "I love the Lord, because he listens to my voice and my cries for mercy.",
            reference = "Psalm 116:1",
            tone = CelebrationVerse.Tone.THANKSGIVING
        ),
        CelebrationVerse(
            id = "psalm_34_4",
            text = "I sought the Lord, and he answered me, and delivered me from all my fears.",
            reference = "Psalm 34:4",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "psalm_103_2",
            text = "Praise the Lord, my soul, and don't forget all his benefits.",
            reference = "Psalm 103:2",
            tone = CelebrationVerse.Tone.PRAISE
        ),
        CelebrationVerse(
            id = "lamentations_3_22",
            text = "It is of the Lord's loving kindnesses that we are not consumed, because his compassion doesn't fail. They are new every morning. Great is your faithfulness.",
            reference = "Lamentations 3:22-23",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "proverbs_3_5",
            text = "Trust in the Lord with all your heart, and don't lean on your own understanding.",
            reference = "Proverbs 3:5",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "isaiah_40_31",
            text = "Those who wait for the Lord will renew their strength. They will mount up with wings like eagles.",
            reference = "Isaiah 40:31",
            tone = CelebrationVerse.Tone.PRAISE
        ),
        CelebrationVerse(
            id = "isaiah_41_10",
            text = "Don't be afraid, for I am with you. Don't be dismayed, for I am your God. I will strengthen you. I will help you.",
            reference = "Isaiah 41:10",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "jeremiah_33_3",
            text = "Call to me, and I will answer you, and will show you great and difficult things, which you don't know.",
            reference = "Jeremiah 33:3",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "matthew_7_7",
            text = "Ask, and it will be given you. Seek, and you will find. Knock, and it will be opened for you.",
            reference = "Matthew 7:7",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "mark_11_24",
            text = "Whatever things you pray and ask for, believe that you have received them, and you shall have them.",
            reference = "Mark 11:24",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "philippians_4_6",
            text = "In everything, by prayer and petition with thanksgiving, let your requests be made known to God.",
            reference = "Philippians 4:6",
            tone = CelebrationVerse.Tone.THANKSGIVING
        ),
        CelebrationVerse(
            id = "philippians_4_19",
            text = "My God will supply every need of yours according to his riches in glory in Christ Jesus.",
            reference = "Philippians 4:19",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "ephesians_3_20",
            text = "Now to him who is able to do exceedingly abundantly above all that we ask or think, according to the power that works in us — to him be the glory!",
            reference = "Ephesians 3:20-21",
            tone = CelebrationVerse.Tone.PRAISE
        ),
        CelebrationVerse(
            id = "1_thessalonians_5_18",
            text = "In everything give thanks, for this is the will of God in Christ Jesus toward you.",
            reference = "1 Thessalonians 5:18",
            tone = CelebrationVerse.Tone.THANKSGIVING
        ),
        CelebrationVerse(
            id = "james_1_17",
            text = "Every good gift and every perfect gift is from above, coming down from the Father of lights, with whom can be no variation.",
            reference = "James 1:17",
            tone = CelebrationVerse.Tone.THANKSGIVING
        ),
        CelebrationVerse(
            id = "1_john_5_14",
            text = "This is the boldness which we have toward him: that if we ask anything according to his will, he listens to us.",
            reference = "1 John 5:14",
            tone = CelebrationVerse.Tone.FAITHFULNESS
        ),
        CelebrationVerse(
            id = "psalm_28_7",
            text = "The Lord is my strength and my shield. My heart has trusted in him, and I am helped. Therefore my heart greatly rejoices. With my song I will thank him.",
            reference = "Psalm 28:7",
            tone = CelebrationVerse.Tone.PRAISE
        ),
        CelebrationVerse(
            id = "psalm_30_11",
            text = "You have turned my mourning into dancing for me. You have removed my sackcloth, and clothed me with gladness.",
            reference = "Psalm 30:11",
            tone = CelebrationVerse.Tone.JOY
        ),
        CelebrationVerse(
            id = "psalm_126_3",
            text = "The Lord has done great things for us, and we are glad.",
            reference = "Psalm 126:3",
            tone = CelebrationVerse.Tone.JOY
        )
    )

    /**
     * Deterministic picker seeded by [prayerId] so that the same answered
     * prayer always surfaces the same verse — if the user shares and comes
     * back later, the card looks identical. Random picks would cause the
     * verse to shuffle on re-open.
     */
    fun pickForPrayer(prayerId: Long): CelebrationVerse {
        val index = ((prayerId % ALL.size) + ALL.size) % ALL.size  // non-negative mod
        return ALL[index.toInt()]
    }

    /** Lookup by stable id (for tests / future deep links). */
    fun byId(id: String): CelebrationVerse? = ALL.find { it.id == id }
}
