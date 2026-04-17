package com.prayerquest.app.domain.content

import java.time.LocalTime

/**
 * A fixed-hour prayer office. Each office follows the classic BCP shape:
 * opening versicle → psalm → short reading → collect → Lord's Prayer →
 * closing. This is the Sprint 3 author-lite pack drawing on public-domain
 * Book of Common Prayer text (1662, now public domain). Sprint 6 will add
 * tradition-specific variants (Anglican / Catholic Liturgy of the Hours /
 * Orthodox Horologion) selectable from onboarding.
 */
data class OfficeLiturgy(
    val hour: Hour,
    val opening: LiturgySection,
    val psalm: LiturgySection,
    val reading: LiturgySection,
    val collect: LiturgySection,
    val lordsPrayer: LiturgySection,
    val closing: LiturgySection
) {
    /**
     * Ordered sections rendered one-by-one in the mode composable's
     * pager. Kept read-only to prevent accidental mutation when this list
     * is used as a key for state.
     */
    val sections: List<LiturgySection>
        get() = listOf(opening, psalm, reading, collect, lordsPrayer, closing)
}

data class LiturgySection(
    val title: String,
    val body: String,
    val response: String? = null
)

/**
 * The four fixed hours used by [DailyOfficeLiturgy]. Traditional Liturgy of
 * the Hours has seven (plus Matins); Sprint 3 ships the four most commonly
 * used by modern lay practitioners. [timeWindow] is used to auto-suggest
 * which office to open based on the user's local clock.
 */
enum class Hour(
    val displayName: String,
    val subtitle: String,
    val emoji: String,
    val startHour: Int,
    val endHour: Int
) {
    LAUDS("Morning Prayer", "Lauds · at daybreak", "🌅", startHour = 5, endHour = 11),
    SEXT("Midday Prayer", "Sext · the sixth hour", "☀️", startHour = 11, endHour = 16),
    VESPERS("Evening Prayer", "Vespers · at sunset", "🌇", startHour = 16, endHour = 21),
    COMPLINE("Night Prayer", "Compline · before sleep", "🌙", startHour = 21, endHour = 5);

    /** True when the given clock hour falls inside this office's window. */
    fun matches(hour: Int): Boolean = if (startHour < endHour) {
        hour in startHour until endHour
    } else {
        // Compline wraps midnight (21–5)
        hour >= startHour || hour < endHour
    }

    companion object {
        /**
         * Pick the office that best matches [now]. Defaults to [LAUDS] if no
         * window contains the hour (can happen during DST edge cases — we'd
         * rather show Morning Prayer than crash).
         */
        fun forTime(now: LocalTime = LocalTime.now()): Hour =
            entries.firstOrNull { it.matches(now.hour) } ?: LAUDS
    }
}

object DailyOfficeLiturgy {

    val lauds = OfficeLiturgy(
        hour = Hour.LAUDS,
        opening = LiturgySection(
            title = "Opening Versicle",
            body = "O Lord, open our lips.",
            response = "And our mouth shall proclaim your praise."
        ),
        psalm = LiturgySection(
            title = "Psalm 95 · Venite",
            body = "O come, let us sing unto the Lord: let us heartily rejoice in the strength " +
                "of our salvation. Let us come before his presence with thanksgiving: and show " +
                "ourselves glad in him with psalms. For the Lord is a great God: and a great " +
                "King above all gods. In his hand are all the corners of the earth: and the " +
                "strength of the hills is his also. The sea is his, and he made it: and his " +
                "hands prepared the dry land."
        ),
        reading = LiturgySection(
            title = "Short Reading",
            body = "The steadfast love of the Lord never ceases; his mercies never come to an " +
                "end; they are new every morning; great is your faithfulness. (Lamentations 3:22–23)"
        ),
        collect = LiturgySection(
            title = "Collect for the Morning",
            body = "O Lord, our heavenly Father, Almighty and everlasting God, who have safely " +
                "brought us to the beginning of this day: Defend us in the same with your " +
                "mighty power; and grant that this day we fall into no sin, neither run into " +
                "any kind of danger; but that all our doings, being ordered by your governance, " +
                "may be righteous in your sight; through Jesus Christ our Lord. Amen."
        ),
        lordsPrayer = LiturgySection(
            title = "The Lord's Prayer",
            body = "Our Father, who art in heaven, hallowed be thy name. Thy kingdom come, thy " +
                "will be done, on earth as it is in heaven. Give us this day our daily bread, " +
                "and forgive us our trespasses, as we forgive those who trespass against us; " +
                "and lead us not into temptation, but deliver us from evil. For thine is the " +
                "kingdom, the power, and the glory, for ever and ever. Amen."
        ),
        closing = LiturgySection(
            title = "Going Forth",
            body = "The grace of our Lord Jesus Christ, and the love of God, and the fellowship " +
                "of the Holy Spirit, be with us all evermore. Amen."
        )
    )

    val sext = OfficeLiturgy(
        hour = Hour.SEXT,
        opening = LiturgySection(
            title = "Opening Versicle",
            body = "O God, make speed to save us.",
            response = "O Lord, make haste to help us."
        ),
        psalm = LiturgySection(
            title = "Psalm 121",
            body = "I will lift up my eyes unto the hills: from whence comes my help. My help " +
                "comes from the Lord: who made heaven and earth. He will not suffer your foot " +
                "to be moved: he who keeps you will not slumber. Behold, he who keeps Israel: " +
                "shall neither slumber nor sleep. The Lord is your keeper: the Lord is your " +
                "shade upon your right hand."
        ),
        reading = LiturgySection(
            title = "Short Reading",
            body = "Cast all your anxiety on him because he cares for you. (1 Peter 5:7)"
        ),
        collect = LiturgySection(
            title = "Collect for Midday",
            body = "Blessed Savior, at this hour you hung upon the cross, stretching out your " +
                "loving arms: Grant that all the peoples of the earth may look to you and be " +
                "saved; for your tender mercies' sake. Amen."
        ),
        lordsPrayer = LiturgySection(
            title = "The Lord's Prayer",
            body = "Our Father, who art in heaven, hallowed be thy name. Thy kingdom come, thy " +
                "will be done, on earth as it is in heaven. Give us this day our daily bread, " +
                "and forgive us our trespasses, as we forgive those who trespass against us; " +
                "and lead us not into temptation, but deliver us from evil. For thine is the " +
                "kingdom, the power, and the glory, for ever and ever. Amen."
        ),
        closing = LiturgySection(
            title = "Going Forth",
            body = "Let us bless the Lord. Thanks be to God."
        )
    )

    val vespers = OfficeLiturgy(
        hour = Hour.VESPERS,
        opening = LiturgySection(
            title = "Opening Versicle",
            body = "O God, make speed to save us.",
            response = "O Lord, make haste to help us."
        ),
        psalm = LiturgySection(
            title = "Psalm 141 · Evening Offering",
            body = "Lord, I call upon you: make haste unto me; give ear unto my voice when I " +
                "cry unto you. Let my prayer be set forth in your sight as the incense: and let " +
                "the lifting up of my hands be an evening sacrifice. Set a watch, O Lord, " +
                "before my mouth: and keep the door of my lips. Let not my heart be inclined " +
                "to any evil thing."
        ),
        reading = LiturgySection(
            title = "Short Reading",
            body = "Peace I leave with you; my peace I give to you. Not as the world gives do I " +
                "give to you. Let not your hearts be troubled, neither let them be afraid. (John 14:27)"
        ),
        collect = LiturgySection(
            title = "Collect for the Evening",
            body = "Lighten our darkness, we beseech you, O Lord; and by your great mercy " +
                "defend us from all perils and dangers of this night; for the love of your " +
                "only Son, our Savior, Jesus Christ. Amen."
        ),
        lordsPrayer = LiturgySection(
            title = "The Lord's Prayer",
            body = "Our Father, who art in heaven, hallowed be thy name. Thy kingdom come, thy " +
                "will be done, on earth as it is in heaven. Give us this day our daily bread, " +
                "and forgive us our trespasses, as we forgive those who trespass against us; " +
                "and lead us not into temptation, but deliver us from evil. For thine is the " +
                "kingdom, the power, and the glory, for ever and ever. Amen."
        ),
        closing = LiturgySection(
            title = "Going Forth",
            body = "The Lord bless us, and keep us; the Lord make his face to shine upon us, " +
                "and be gracious unto us. Amen."
        )
    )

    val compline = OfficeLiturgy(
        hour = Hour.COMPLINE,
        opening = LiturgySection(
            title = "Opening Versicle",
            body = "The Lord Almighty grant us a peaceful night, and a perfect end.",
            response = "Amen."
        ),
        psalm = LiturgySection(
            title = "Psalm 4 · Evening Rest",
            body = "Hear me when I call, O God of my righteousness: you have enlarged me when " +
                "I was in distress; have mercy upon me, and hear my prayer. I will both lay me " +
                "down in peace, and sleep: for you, Lord, only make me dwell in safety."
        ),
        reading = LiturgySection(
            title = "Short Reading",
            body = "Be sober-minded; be watchful. Your adversary the devil prowls around like a " +
                "roaring lion, seeking someone to devour. Resist him, firm in your faith. (1 Peter 5:8–9)"
        ),
        collect = LiturgySection(
            title = "Collect for the Night",
            body = "Visit, we beseech you, O Lord, this dwelling, and drive far from it all the " +
                "snares of the enemy; let your holy angels dwell herein to preserve us in " +
                "peace; and may your blessing be upon us evermore; through Jesus Christ our " +
                "Lord. Amen."
        ),
        lordsPrayer = LiturgySection(
            title = "The Lord's Prayer",
            body = "Our Father, who art in heaven, hallowed be thy name. Thy kingdom come, thy " +
                "will be done, on earth as it is in heaven. Give us this day our daily bread, " +
                "and forgive us our trespasses, as we forgive those who trespass against us; " +
                "and lead us not into temptation, but deliver us from evil. For thine is the " +
                "kingdom, the power, and the glory, for ever and ever. Amen."
        ),
        closing = LiturgySection(
            title = "Nunc Dimittis",
            body = "Lord, now let your servant depart in peace, according to your word; for my " +
                "eyes have seen your salvation, which you have prepared before the face of all " +
                "people, to be a light to lighten the Gentiles, and to be the glory of your " +
                "people Israel."
        )
    )

    /** The four offices in canonical order — first call gets [LAUDS]. */
    val all: List<OfficeLiturgy> = listOf(lauds, sext, vespers, compline)

    /** Look up the office for a given hour-of-day bucket. */
    fun forHour(hour: Hour): OfficeLiturgy = when (hour) {
        Hour.LAUDS -> lauds
        Hour.SEXT -> sext
        Hour.VESPERS -> vespers
        Hour.COMPLINE -> compline
    }
}
