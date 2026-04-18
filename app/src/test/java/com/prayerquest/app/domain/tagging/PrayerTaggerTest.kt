package com.prayerquest.app.domain.tagging

import com.prayerquest.app.domain.tagging.PrayerTagger.Category
import com.prayerquest.app.domain.tagging.PrayerTagger.SuggestedTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the on-device auto-tagger against the DD §3.5.1 heuristic spec.
 *
 * These tests are NOT exhaustive keyword checks — they are behavioural
 * contracts that the add-prayer-item UI depends on. Changing any of them
 * means the UI needs to be revisited, not that the test should be updated
 * to match a drift in behaviour.
 */
class PrayerTaggerTest {

    private fun List<SuggestedTag>.categoryOrNull(c: Category): SuggestedTag? =
        firstOrNull { it.category == c }

    // ─── Single-category "obvious" hits ─────────────────────────────────────

    @Test
    fun `family keyword in short title yields Family suggestion`() {
        val tags = PrayerTagger.suggest(title = "pray for my mom", description = "")
        val family = tags.categoryOrNull(Category.FAMILY)
        assertNotNull(
            "Family must be suggested for 'pray for my mom'; got $tags",
            family
        )
        assertTrue(
            "Family confidence must clear threshold; was ${family!!.confidence}",
            family.confidence >= PrayerTagger.DEFAULT_THRESHOLD
        )
    }

    @Test
    fun `explicit health phrasing yields Health as top suggestion`() {
        val tags = PrayerTagger.suggest(
            title = "healing for Dad's cancer",
            description = ""
        )
        assertTrue("Expected at least one tag", tags.isNotEmpty())
        assertEquals(
            "Health should outrank Family when Health has 2 hits and Family has 1",
            Category.HEALTH,
            tags.first().category
        )
        val health = tags.categoryOrNull(Category.HEALTH)!!
        assertTrue(
            "Health confidence must clear threshold; was ${health.confidence}",
            health.confidence >= PrayerTagger.DEFAULT_THRESHOLD
        )
    }

    @Test
    fun `promotion at work yields Work Vocation`() {
        val tags = PrayerTagger.suggest(title = "Promotion at work", description = "")
        val work = tags.categoryOrNull(Category.WORK_VOCATION)
        assertNotNull(
            "Work/Vocation must be suggested for 'Promotion at work'; got $tags",
            work
        )
        assertEquals(Category.WORK_VOCATION, tags.first().category)
    }

    @Test
    fun `healing for Moms surgery yields Family and Health`() {
        val tags = PrayerTagger.suggest(
            title = "Healing for Mom's surgery",
            description = ""
        )
        val family = tags.categoryOrNull(Category.FAMILY)
        val health = tags.categoryOrNull(Category.HEALTH)
        assertNotNull("Family expected; got $tags", family)
        assertNotNull("Health expected; got $tags", health)
        // Health has 2 hits vs Family's 1 over the same 4-word input, so it
        // should sort above Family in the list.
        assertTrue(
            "Health confidence should exceed Family confidence",
            health!!.confidence > family!!.confidence
        )
    }

    // ─── Ambiguous / low-confidence default ─────────────────────────────────

    @Test
    fun `ambiguous give me strength defaults to Spiritual Growth with low confidence`() {
        val tags = PrayerTagger.suggest(title = "give me strength", description = "")
        val spiritual = tags.categoryOrNull(Category.SPIRITUAL_GROWTH)
        assertNotNull(
            "Spiritual Growth should catch 'strength'; got $tags",
            spiritual
        )
        assertTrue(
            "Confidence should clear threshold; was ${spiritual!!.confidence}",
            spiritual.confidence >= PrayerTagger.DEFAULT_THRESHOLD
        )
        assertTrue(
            "Confidence should be <= 0.5 (relatively low for ambiguous input); " +
                "was ${spiritual.confidence}",
            spiritual.confidence <= 0.5f
        )
    }

    // ─── Multi-category interleaving ───────────────────────────────────────

    @Test
    fun `multi-category sister surgery yields both Family and Health`() {
        val tags = PrayerTagger.suggest(
            title = "my sister's surgery",
            description = ""
        )
        assertNotNull(
            "Family must be suggested for sibling mention; got $tags",
            tags.categoryOrNull(Category.FAMILY)
        )
        assertNotNull(
            "Health must be suggested for 'surgery'; got $tags",
            tags.categoryOrNull(Category.HEALTH)
        )
    }

    // ─── Empty / blank input ───────────────────────────────────────────────

    @Test
    fun `empty title and description returns no tags`() {
        assertTrue(PrayerTagger.suggest(title = "", description = "").isEmpty())
    }

    @Test
    fun `whitespace only input returns no tags`() {
        assertTrue(PrayerTagger.suggest(title = "   ", description = "\t\n ").isEmpty())
    }

    // ─── Threshold guard against off-topic noise ───────────────────────────

    @Test
    fun `threshold rejects off-topic long text with no keyword hits`() {
        // None of the words match any keyword list — must return empty, not
        // "Other" or a desperately low-confidence Spiritual tag.
        val tags = PrayerTagger.suggest(
            title = "The weather today is rather pleasant outside",
            description = "I noticed the sunshine warming the porch nicely"
        )
        assertTrue(
            "Off-topic text must produce no suggestions; got $tags",
            tags.isEmpty()
        )
    }

    @Test
    fun `threshold filters single keyword hit in long journal style text`() {
        // One keyword ("mom") dropped into 50+ words of unrelated prose must
        // NOT be enough to surface a Family chip — this is the primary guard
        // against over-eager tagging when the user is free-writing.
        val longDescription = buildString {
            repeat(15) {
                append("The clouds drifted slowly over the field, and ")
            }
            append("I remembered my mom briefly.")
        }
        val tags = PrayerTagger.suggest(
            title = "Random reflection",
            description = longDescription
        )
        assertNull(
            "Single keyword in long text must not clear 0.15 threshold; got $tags",
            tags.categoryOrNull(Category.FAMILY)
        )
    }

    // ─── Returned list contract ────────────────────────────────────────────

    @Test
    fun `at most three suggestions returned even when many match`() {
        // Pack the input with one hit from five different categories so a
        // naive implementation would emit 5. We only ever want three chips.
        val tags = PrayerTagger.suggest(
            title = "mom surgery job money faith",
            description = ""
        )
        assertTrue(
            "Must cap at ${PrayerTagger.MAX_SUGGESTIONS}; got ${tags.size}",
            tags.size <= PrayerTagger.MAX_SUGGESTIONS
        )
    }

    @Test
    fun `suggestions are sorted by descending confidence`() {
        val tags = PrayerTagger.suggest(
            title = "Healing for Mom's surgery",
            description = ""
        )
        for (i in 1 until tags.size) {
            assertTrue(
                "Tags must be sorted descending by confidence — " +
                    "${tags[i - 1]} precedes ${tags[i]}",
                tags[i - 1].confidence >= tags[i].confidence
            )
        }
    }

    @Test
    fun `case insensitive matching`() {
        // matchedKeyword preserves the casing of the input, so compare on
        // (category, confidence) only — that's what the UI binds to.
        fun List<SuggestedTag>.signature() =
            map { it.category to it.confidence }

        val lower = PrayerTagger.suggest(title = "mom", description = "")
        val upper = PrayerTagger.suggest(title = "MOM", description = "")
        val mixed = PrayerTagger.suggest(title = "Mom", description = "")
        assertEquals(lower.signature(), upper.signature())
        assertEquals(lower.signature(), mixed.signature())
        assertFalse("Expected at least one tag for single keyword", lower.isEmpty())
    }

    @Test
    fun `matched keyword is populated for accessibility`() {
        val tags = PrayerTagger.suggest(title = "Mom is sick", description = "")
        val family = tags.categoryOrNull(Category.FAMILY)
        assertNotNull(family)
        assertTrue(
            "matchedKeyword should be populated; was '${family!!.matchedKeyword}'",
            family.matchedKeyword.isNotBlank()
        )
    }
}
