package com.prayerquest.app.domain.content

/**
 * A single Psalm excerpt curated for [CrisisPsalmsLibrary]. Every entry is a
 * self-contained comfort text — nothing is excerpted so aggressively that the
 * verse becomes confusing without its surroundings, and nothing is so long
 * that a user in acute distress has to scroll through a full chapter to
 * reach the next breath.
 *
 * All text is drawn from the public-domain King James Version — the same
 * translation PrayerQuest's Bible importer ships. Using KJV keeps licensing
 * trivial, and its cadence reads well under duress.
 *
 * @property reference Canonical Scripture reference (e.g. "Psalm 23").
 * @property text The excerpt itself, already line-wrapped as a single string
 *   with newlines between verses so [androidx.compose.material3.Text] renders
 *   each verse on its own line without bespoke layout code.
 * @property framing One-line pastoral framing shown above the text
 *   ("When you feel hunted") — intentionally short so it doesn't crowd the
 *   passage on small screens.
 * @property estimatedReadSeconds Conservative estimate used only to label the
 *   card for users who want to gauge the commitment — not a timer, not a
 *   progress driver.
 */
data class CrisisPsalm(
    val reference: String,
    val text: String,
    val framing: String,
    val estimatedReadSeconds: Int
)

/**
 * Curated Psalms for [Crisis Prayer Mode][com.prayerquest.app.ui.crisis.CrisisPrayerScreen]
 * (DD §3.10). These are not the full Psalms library — they are the narrow
 * subset chosen for acute distress: Psalms that speak of shelter, shepherd,
 * fear, abandonment, and God's inescapable presence.
 *
 * The list is intentionally ordered from "feeling-alongside" (the user is not
 * alone) toward "honest lament" (the user can say the hard thing). A person
 * swiping through in order walks from Psalm 23's quiet pastoral comfort into
 * Psalm 42's open grief — a gentle arc, not a cheerful one.
 *
 * **Do not reorder or remove entries without reviewing DD §3.10 and the
 * user-wellbeing section of the system prompt.** Reordering changes the
 * pastoral shape of the mode.
 */
object CrisisPsalmsLibrary {

    val psalms: List<CrisisPsalm> = listOf(
        CrisisPsalm(
            reference = "Psalm 23",
            framing = "When you need a shepherd",
            text = """
                The Lord is my shepherd; I shall not want.
                He maketh me to lie down in green pastures: he leadeth me beside the still waters.
                He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake.
                Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me; thy rod and thy staff they comfort me.
                Thou preparest a table before me in the presence of mine enemies: thou anointest my head with oil; my cup runneth over.
                Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the Lord for ever.
            """.trimIndent(),
            estimatedReadSeconds = 60
        ),
        CrisisPsalm(
            reference = "Psalm 27:1-5",
            framing = "When fear is crowding in",
            text = """
                The Lord is my light and my salvation; whom shall I fear? the Lord is the strength of my life; of whom shall I be afraid?
                When the wicked, even mine enemies and my foes, came upon me to eat up my flesh, they stumbled and fell.
                Though an host should encamp against me, my heart shall not fear: though war should rise against me, in this will I be confident.
                One thing have I desired of the Lord, that will I seek after; that I may dwell in the house of the Lord all the days of my life, to behold the beauty of the Lord, and to inquire in his temple.
                For in the time of trouble he shall hide me in his pavilion: in the secret of his tabernacle shall he hide me; he shall set me up upon a rock.
            """.trimIndent(),
            estimatedReadSeconds = 70
        ),
        CrisisPsalm(
            reference = "Psalm 46:1-3, 10",
            framing = "When the ground feels like it's shaking",
            text = """
                God is our refuge and strength, a very present help in trouble.
                Therefore will not we fear, though the earth be removed, and though the mountains be carried into the midst of the sea;
                Though the waters thereof roar and be troubled, though the mountains shake with the swelling thereof. Selah.

                Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth.
            """.trimIndent(),
            estimatedReadSeconds = 55
        ),
        CrisisPsalm(
            reference = "Psalm 91:1-6",
            framing = "When you need a shelter",
            text = """
                He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty.
                I will say of the Lord, He is my refuge and my fortress: my God; in him will I trust.
                Surely he shall deliver thee from the snare of the fowler, and from the noisome pestilence.
                He shall cover thee with his feathers, and under his wings shalt thou trust: his truth shall be thy shield and buckler.
                Thou shalt not be afraid for the terror by night; nor for the arrow that flieth by day;
                Nor for the pestilence that walketh in darkness; nor for the destruction that wasteth at noonday.
            """.trimIndent(),
            estimatedReadSeconds = 70
        ),
        CrisisPsalm(
            reference = "Psalm 121",
            framing = "When you feel watched over by no one",
            text = """
                I will lift up mine eyes unto the hills, from whence cometh my help.
                My help cometh from the Lord, which made heaven and earth.
                He will not suffer thy foot to be moved: he that keepeth thee will not slumber.
                Behold, he that keepeth Israel shall neither slumber nor sleep.
                The Lord is thy keeper: the Lord is thy shade upon thy right hand.
                The sun shall not smite thee by day, nor the moon by night.
                The Lord shall preserve thee from all evil: he shall preserve thy soul.
                The Lord shall preserve thy going out and thy coming in from this time forth, and even for evermore.
            """.trimIndent(),
            estimatedReadSeconds = 65
        ),
        CrisisPsalm(
            reference = "Psalm 139:7-12",
            framing = "When you feel hunted",
            text = """
                Whither shall I go from thy spirit? or whither shall I flee from thy presence?
                If I ascend up into heaven, thou art there: if I make my bed in hell, behold, thou art there.
                If I take the wings of the morning, and dwell in the uttermost parts of the sea;
                Even there shall thy hand lead me, and thy right hand shall hold me.
                If I say, Surely the darkness shall cover me; even the night shall be light about me.
                Yea, the darkness hideth not from thee; but the night shineth as the day: the darkness and the light are both alike to thee.
            """.trimIndent(),
            estimatedReadSeconds = 60
        ),
        CrisisPsalm(
            reference = "Psalm 42:1-5",
            framing = "When you can't feel anything",
            text = """
                As the hart panteth after the water brooks, so panteth my soul after thee, O God.
                My soul thirsteth for God, for the living God: when shall I come and appear before God?
                My tears have been my meat day and night, while they continually say unto me, Where is thy God?
                When I remember these things, I pour out my soul in me: for I had gone with the multitude, I went with them to the house of God, with the voice of joy and praise, with a multitude that kept holyday.
                Why art thou cast down, O my soul? and why art thou disquieted in me? hope thou in God: for I shall yet praise him for the help of his countenance.
            """.trimIndent(),
            estimatedReadSeconds = 75
        ),
        CrisisPsalm(
            reference = "Psalm 56:3-4",
            framing = "A short word for when words are hard",
            text = """
                What time I am afraid, I will trust in thee.
                In God I will praise his word, in God I have put my trust; I will not fear what flesh can do unto me.
            """.trimIndent(),
            estimatedReadSeconds = 25
        )
    )
}
