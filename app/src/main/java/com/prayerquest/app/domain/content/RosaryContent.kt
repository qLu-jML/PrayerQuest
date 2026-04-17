package com.prayerquest.app.domain.content

/**
 * A "rope" here is the shared abstraction across four very different bead
 * devotions: the Catholic Rosary, the Orthodox Jesus Prayer rope, the Anglican
 * rosary, and a user-buildable Custom rope. Each rope is a sequence of
 * [Decade]s; each decade is a sequence of [Bead]s with a prompt.
 *
 * This shape lets one composable walk through any rope without hardcoding
 * which tradition is running — the composable just iterates beads and
 * advances as the user taps. Haptic feedback fires on each bead advance in
 * the UI layer.
 *
 * All Catholic/Anglican/Orthodox prayer texts here are public-domain
 * traditional English renderings. Sprint 6 pass adds the full Luminous
 * Mysteries narrative and optional tradition-specific refinements.
 */
data class PrayerRope(
    val id: String,
    val displayName: String,
    val tradition: String,
    val description: String,
    val decades: List<Decade>
)

data class Decade(
    val title: String,
    val meditation: String,
    val beads: List<Bead>
)

data class Bead(
    val label: String,
    val prayerText: String
)

private const val OUR_FATHER_TEXT =
    "Our Father, who art in heaven, hallowed be thy name. Thy kingdom come, thy will be done, " +
        "on earth as it is in heaven. Give us this day our daily bread, and forgive us our " +
        "trespasses, as we forgive those who trespass against us; and lead us not into " +
        "temptation, but deliver us from evil."

private const val HAIL_MARY_TEXT =
    "Hail Mary, full of grace, the Lord is with thee. Blessed art thou among women, and " +
        "blessed is the fruit of thy womb, Jesus. Holy Mary, Mother of God, pray for us " +
        "sinners, now and at the hour of our death. Amen."

private const val GLORY_BE_TEXT =
    "Glory be to the Father, and to the Son, and to the Holy Spirit. As it was in the " +
        "beginning, is now, and ever shall be, world without end. Amen."

private const val JESUS_PRAYER_TEXT =
    "Lord Jesus Christ, Son of God, have mercy on me, a sinner."

object RosaryContent {

    /**
     * Condensed Joyful-Mysteries rosary — 5 decades, one Hail Mary per bead
     * (10 per decade in the tradition; we pace it at 3 for Sprint 3 to keep
     * the session bounded and finishable within the DD's session-length
     * envelope; full 10-bead pacing ships in Sprint 6 as a mode option).
     */
    val catholicRosary = PrayerRope(
        id = "catholic_rosary_joyful",
        displayName = "The Rosary · Joyful Mysteries",
        tradition = "Catholic",
        description = "Five mysteries celebrating the Incarnation and childhood of Christ.",
        decades = listOf(
            buildCatholicDecade(
                "1st Joyful Mystery · The Annunciation",
                "Contemplate Mary's 'yes' to God. Ask for the grace of humble obedience."
            ),
            buildCatholicDecade(
                "2nd Joyful Mystery · The Visitation",
                "Mary hastens to Elizabeth. Ask for the grace of love of neighbor."
            ),
            buildCatholicDecade(
                "3rd Joyful Mystery · The Nativity",
                "Christ is born in poverty. Ask for the grace of a humble heart."
            ),
            buildCatholicDecade(
                "4th Joyful Mystery · The Presentation",
                "Christ is offered in the Temple. Ask for the grace of purity of heart."
            ),
            buildCatholicDecade(
                "5th Joyful Mystery · Finding in the Temple",
                "Jesus about his Father's business. Ask for the grace of perseverance."
            )
        )
    )

    /**
     * Orthodox Jesus Prayer rope — 100 knots traditionally. We bundle a
     * compact 33-knot variant for Sprint 3 (a full 100 becomes optional in
     * Sprint 6 via a pacing preference).
     */
    val orthodoxJesusPrayer = PrayerRope(
        id = "jesus_prayer_rope_33",
        displayName = "Jesus Prayer Rope · 33 Knots",
        tradition = "Orthodox",
        description = "Thirty-three repetitions of the Jesus Prayer — one for each year of Christ's earthly life.",
        decades = listOf(
            Decade(
                title = "Jesus Prayer Rope",
                meditation = "Pray each knot slowly. Let the words enter the heart.",
                beads = List(33) { index ->
                    Bead(
                        label = "Knot ${index + 1}",
                        prayerText = JESUS_PRAYER_TEXT
                    )
                }
            )
        )
    )

    /**
     * Anglican rosary — 33 beads: Cross + Invitatory + 4 Weeks (7 beads each) +
     * between-week Cruciform beads. Sprint 3 pacing uses simplified
     * repetitive structure matching historic Anglican practice.
     */
    val anglicanRosary = PrayerRope(
        id = "anglican_rosary",
        displayName = "Anglican Prayer Beads",
        tradition = "Anglican",
        description = "Thirty-three beads in four weeks of seven, punctuated by the Cross and Cruciform beads.",
        decades = listOf(
            Decade(
                title = "Cross & Invitatory",
                meditation = "Begin at the Cross — a quiet confession of faith.",
                beads = listOf(
                    Bead(
                        label = "Cross",
                        prayerText = "In the name of the Father, and of the Son, and of the Holy Spirit. Amen."
                    ),
                    Bead(
                        label = "Invitatory Bead",
                        prayerText = "O God, make speed to save us. O Lord, make haste to help us."
                    )
                )
            ),
            Decade(
                title = "First Week",
                meditation = "Pray the Jesus Prayer on each of the seven weeks beads.",
                beads = List(7) { index ->
                    Bead(
                        label = "Week 1 · Bead ${index + 1}",
                        prayerText = JESUS_PRAYER_TEXT
                    )
                }
            ),
            Decade(
                title = "Second Week",
                meditation = "Continue the Jesus Prayer, or pray a short Scripture verse.",
                beads = List(7) { index ->
                    Bead(
                        label = "Week 2 · Bead ${index + 1}",
                        prayerText = JESUS_PRAYER_TEXT
                    )
                }
            ),
            Decade(
                title = "Third Week",
                meditation = "Let the rhythm of the beads carry your attention back to God.",
                beads = List(7) { index ->
                    Bead(
                        label = "Week 3 · Bead ${index + 1}",
                        prayerText = JESUS_PRAYER_TEXT
                    )
                }
            ),
            Decade(
                title = "Fourth Week",
                meditation = "A final seven beads before returning to the Cross.",
                beads = List(7) { index ->
                    Bead(
                        label = "Week 4 · Bead ${index + 1}",
                        prayerText = JESUS_PRAYER_TEXT
                    )
                }
            )
        )
    )

    /**
     * A compact custom rope for users whose tradition doesn't include bead
     * devotions but who want the tactile, meditative pacing. Ten beads,
     * each prompting a short line of the user's own — "the beads of your
     * own" mode from DD §3.4 item 9.
     */
    val customTenBead = PrayerRope(
        id = "custom_ten_bead",
        displayName = "Custom · Ten Beads",
        tradition = "Ecumenical",
        description = "A simple decade of your own prayers. Speak one short line on each bead.",
        decades = listOf(
            Decade(
                title = "Ten-Bead Decade",
                meditation = "Pause on each bead. Speak a single line — a thanks, a petition, a name before God.",
                beads = List(10) { index ->
                    Bead(
                        label = "Bead ${index + 1}",
                        prayerText = "[Pray one line of your own]"
                    )
                }
            )
        )
    )

    val allRopes: List<PrayerRope> = listOf(
        catholicRosary,
        orthodoxJesusPrayer,
        anglicanRosary,
        customTenBead
    )

    val default: PrayerRope = catholicRosary

    fun byId(id: String): PrayerRope? = allRopes.firstOrNull { it.id == id }
}

/**
 * Build one Catholic Rosary decade: an Our Father + 3 Hail Marys + Glory Be.
 * Traditional pacing is 10 Hail Marys; we use 3 for Sprint 3 (see rope
 * docstring above). Keeping this helper private prevents accidental reuse
 * with a different mystery title for another decade variant.
 */
private fun buildCatholicDecade(title: String, meditation: String): Decade = Decade(
    title = title,
    meditation = meditation,
    beads = buildList {
        add(Bead("Our Father", OUR_FATHER_TEXT))
        repeat(3) { index ->
            add(Bead("Hail Mary ${index + 1}", HAIL_MARY_TEXT))
        }
        add(Bead("Glory Be", GLORY_BE_TEXT))
    }
)
