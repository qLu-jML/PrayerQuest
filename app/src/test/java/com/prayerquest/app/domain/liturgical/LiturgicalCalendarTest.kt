package com.prayerquest.app.domain.liturgical

import com.prayerquest.app.data.preferences.LiturgicalCalendar as CalendarChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Reference cases for the Western + Eastern liturgical calendar engine.
 *
 * These are dated ground-truth cases spanning 2024-2028 — if any of them drifts
 * the engine has regressed, because these are observable historical facts (and
 * future feast dates that are pre-computable to the day). Every case on the DD's
 * required-coverage list has at least one assertion here:
 *   - Advent Sunday ✓ (multiple)
 *   - Christmas Day ✓
 *   - Ash Wednesday 2026 ✓
 *   - Good Friday 2026 ✓
 *   - Eastern Easter 2026 ✓
 *   - A day deep in Ordinary Time ✓
 *
 * Easter dates verified against published liturgical almanacs and cross-checked
 * by hand using the Meeus/Jones/Butcher + Meeus Julian Paschalia formulas.
 */
class LiturgicalCalendarTest {

    // ─────────────────────────────────────────────────────────────────────
    // Western Easter algorithm — Meeus/Jones/Butcher integer-only formula
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `westernEaster 2024 is March 31`() {
        assertEquals(LocalDate.of(2024, 3, 31), LiturgicalCalendar.westernEaster(2024))
    }

    @Test
    fun `westernEaster 2025 is April 20`() {
        assertEquals(LocalDate.of(2025, 4, 20), LiturgicalCalendar.westernEaster(2025))
    }

    @Test
    fun `westernEaster 2026 is April 5`() {
        assertEquals(LocalDate.of(2026, 4, 5), LiturgicalCalendar.westernEaster(2026))
    }

    @Test
    fun `westernEaster 2027 is March 28`() {
        assertEquals(LocalDate.of(2027, 3, 28), LiturgicalCalendar.westernEaster(2027))
    }

    @Test
    fun `westernEaster 2028 is April 16`() {
        assertEquals(LocalDate.of(2028, 4, 16), LiturgicalCalendar.westernEaster(2028))
    }

    // ─────────────────────────────────────────────────────────────────────
    // Eastern (Orthodox) Easter — Julian computus converted to Gregorian
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `easternEaster 2024 is May 5`() {
        assertEquals(LocalDate.of(2024, 5, 5), LiturgicalCalendar.easternEaster(2024))
    }

    @Test
    fun `easternEaster 2025 is April 20 - coincides with Western`() {
        assertEquals(LocalDate.of(2025, 4, 20), LiturgicalCalendar.easternEaster(2025))
    }

    @Test
    fun `easternEaster 2026 is April 12`() {
        assertEquals(LocalDate.of(2026, 4, 12), LiturgicalCalendar.easternEaster(2026))
    }

    @Test
    fun `easternEaster 2027 is May 2`() {
        assertEquals(LocalDate.of(2027, 5, 2), LiturgicalCalendar.easternEaster(2027))
    }

    @Test
    fun `easternEaster 2028 is April 16 - coincides with Western`() {
        assertEquals(LocalDate.of(2028, 4, 16), LiturgicalCalendar.easternEaster(2028))
    }

    // ─────────────────────────────────────────────────────────────────────
    // First Sunday of Advent — Western
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `first Sunday of Advent 2024 is December 1`() {
        assertEquals(
            LocalDate.of(2024, 12, 1),
            LiturgicalCalendar.firstSundayOfAdventWestern(2024)
        )
    }

    @Test
    fun `first Sunday of Advent 2026 is November 29`() {
        assertEquals(
            LocalDate.of(2026, 11, 29),
            LiturgicalCalendar.firstSundayOfAdventWestern(2026)
        )
    }

    @Test
    fun `first Sunday of Advent 2028 is December 3`() {
        assertEquals(
            LocalDate.of(2028, 12, 3),
            LiturgicalCalendar.firstSundayOfAdventWestern(2028)
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // Western — season and dayName labels
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `Western Christmas Day 2026 is a CHRISTMAS feast`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 12, 25), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.CHRISTMAS, day.season)
        assertEquals("Christmas Day", day.dayName)
        assertEquals(LiturgicalFeast.CHRISTMAS, day.feast)
    }

    @Test
    fun `Western Christmas Eve 2026 is ADVENT but feast=CHRISTMAS_EVE`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 12, 24), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.ADVENT, day.season)
        assertEquals("Christmas Eve", day.dayName)
        assertEquals(LiturgicalFeast.CHRISTMAS_EVE, day.feast)
    }

    @Test
    fun `Western Epiphany 2026`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 1, 6), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.EPIPHANY, day.season)
        assertEquals("Epiphany", day.dayName)
        assertEquals(LiturgicalFeast.EPIPHANY, day.feast)
    }

    @Test
    fun `Western Ash Wednesday 2026 is February 18`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 2, 18), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.LENT, day.season)
        assertEquals("Ash Wednesday", day.dayName)
        assertEquals(LiturgicalFeast.ASH_WEDNESDAY, day.feast)
    }

    @Test
    fun `Western Palm Sunday 2026 is March 29`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 3, 29), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.HOLY_WEEK, day.season)
        assertEquals("Palm Sunday", day.dayName)
        assertEquals(LiturgicalFeast.PALM_SUNDAY, day.feast)
    }

    @Test
    fun `Western Maundy Thursday 2026 is April 2`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 2), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.HOLY_WEEK, day.season)
        assertEquals("Maundy Thursday", day.dayName)
        assertEquals(LiturgicalFeast.MAUNDY_THURSDAY, day.feast)
    }

    @Test
    fun `Western Good Friday 2026 is April 3`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 3), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.HOLY_WEEK, day.season)
        assertEquals("Good Friday", day.dayName)
        assertEquals(LiturgicalFeast.GOOD_FRIDAY, day.feast)
    }

    @Test
    fun `Western Easter 2024 is March 31 and labeled Easter Sunday`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2024, 3, 31), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.EASTER, day.season)
        assertEquals("Easter Sunday", day.dayName)
        assertEquals(LiturgicalFeast.EASTER, day.feast)
    }

    @Test
    fun `Western Pentecost 2026 is May 24`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 5, 24), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.PENTECOST, day.season)
        assertEquals("Pentecost", day.dayName)
        assertEquals(LiturgicalFeast.PENTECOST, day.feast)
    }

    @Test
    fun `Western All Saints 2026 is November 1`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 11, 1), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.ORDINARY_TIME_2, day.season)
        assertEquals("All Saints' Day", day.dayName)
        assertEquals(LiturgicalFeast.ALL_SAINTS, day.feast)
    }

    @Test
    fun `Western second Sunday of Advent 2026 is December 6`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 12, 6), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.ADVENT, day.season)
        assertEquals("Second Sunday of Advent", day.dayName)
    }

    @Test
    fun `Western Dec 10 2026 shows Second Sunday of Advent as the week label`() {
        // DD task requirement: mid-week days inside an Advent week still read
        // as "Second Sunday of Advent" because that's the week's anchor.
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 12, 10), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.ADVENT, day.season)
        assertEquals("Second Sunday of Advent", day.dayName)
        assertNull("Plain weekday in Advent has no feast", day.feast)
    }

    @Test
    fun `Western first Sunday of Advent 2026 is November 29`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 11, 29), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.ADVENT, day.season)
        assertEquals("First Sunday of Advent", day.dayName)
    }

    @Test
    fun `Western Jan 3 2025 is still Christmastide from 2024 Christmas`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2025, 1, 3), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.CHRISTMAS, day.season)
        assertEquals("Christmastide", day.dayName)
    }

    @Test
    fun `Western Ordinary Time I - Jan 20 2026`() {
        // After Epiphany (Jan 6), before Ash Wednesday (Feb 18) — first
        // stretch of Ordinary Time.
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 1, 20), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.ORDINARY_TIME, day.season)
        assertEquals("Ordinary Time", day.dayName)
    }

    @Test
    fun `Western mid-Lent - March 15 2026`() {
        // Between Ash Wed (Feb 18) and Palm Sunday (Mar 29).
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 3, 15), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.LENT, day.season)
        assertEquals("Lent", day.dayName)
    }

    @Test
    fun `Western Eastertide - April 12 2026 (week after Easter)`() {
        // Easter 2026 = Apr 5, Pentecost = May 24. Apr 12 is deep in
        // Paschaltide ("Eastertide" in Western usage).
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 12), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.EASTER, day.season)
        assertEquals("Eastertide", day.dayName)
    }

    @Test
    fun `Western deep Ordinary Time 2 - July 15 2026`() {
        // Well after Pentecost (May 24) and well before Advent (Nov 29).
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 7, 15), CalendarChoice.WESTERN)
        assertEquals(LiturgicalSeason.ORDINARY_TIME_2, day.season)
        assertEquals("Ordinary Time", day.dayName)
        assertNull("Plain summer weekday has no feast", day.feast)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Eastern — season and dayName labels
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `Eastern Pascha 2026 is April 12 labeled Pascha`() {
        // DD required coverage case.
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 12), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.EASTER, day.season)
        assertEquals("Pascha", day.dayName)
        assertEquals(LiturgicalFeast.EASTER, day.feast)
    }

    @Test
    fun `Eastern Pascha 2024 is May 5`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2024, 5, 5), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.EASTER, day.season)
        assertEquals("Pascha", day.dayName)
    }

    @Test
    fun `Eastern Pascha 2028 is April 16`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2028, 4, 16), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.EASTER, day.season)
        assertEquals("Pascha", day.dayName)
    }

    @Test
    fun `Eastern Clean Monday 2026 is February 23 and starts Great Lent`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 2, 23), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.LENT, day.season)
        assertEquals("Clean Monday", day.dayName)
        assertEquals(LiturgicalFeast.CLEAN_MONDAY, day.feast)
    }

    @Test
    fun `Eastern Palm Sunday 2026 is April 5`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 5), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.HOLY_WEEK, day.season)
        assertEquals("Palm Sunday", day.dayName)
    }

    @Test
    fun `Eastern Holy Thursday 2026 is April 9`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 9), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.HOLY_WEEK, day.season)
        assertEquals("Holy Thursday", day.dayName)
        assertEquals(LiturgicalFeast.HOLY_THURSDAY, day.feast)
    }

    @Test
    fun `Eastern Great Friday 2026 is April 10`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 10), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.HOLY_WEEK, day.season)
        assertEquals("Great Friday", day.dayName)
        assertEquals(LiturgicalFeast.GREAT_FRIDAY, day.feast)
    }

    @Test
    fun `Eastern Pentecost 2026 is May 31`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 5, 31), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.PENTECOST, day.season)
        assertEquals("Pentecost", day.dayName)
    }

    @Test
    fun `Eastern Nativity of Christ 2026 is January 7`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 1, 7), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.CHRISTMAS, day.season)
        assertEquals("Nativity of Christ", day.dayName)
        assertEquals(LiturgicalFeast.ORTHODOX_CHRISTMAS, day.feast)
    }

    @Test
    fun `Eastern Nativity Eve 2026 is January 6`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 1, 6), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.ADVENT, day.season)
        assertEquals("Nativity Eve", day.dayName)
        assertEquals(LiturgicalFeast.CHRISTMAS_EVE, day.feast)
    }

    @Test
    fun `Eastern Theophany 2026 is January 19`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 1, 19), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.EPIPHANY, day.season)
        assertEquals("Theophany", day.dayName)
        assertEquals(LiturgicalFeast.THEOPHANY, day.feast)
    }

    @Test
    fun `Eastern Ordinary Time I - January 25 2026`() {
        // Between Theophany (Jan 19) and Clean Monday (Feb 23).
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 1, 25), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.ORDINARY_TIME, day.season)
        assertEquals("Ordinary Time", day.dayName)
    }

    @Test
    fun `Eastern deep Ordinary Time 2 - July 15 2026`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 7, 15), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.ORDINARY_TIME_2, day.season)
        assertEquals("Ordinary Time", day.dayName)
    }

    @Test
    fun `Eastern Nativity Fast Dec 1 2026 is ADVENT`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 12, 1), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.ADVENT, day.season)
        assertEquals("Nativity Fast", day.dayName)
    }

    @Test
    fun `Eastern Paschaltide - April 20 2026 (week after Pascha)`() {
        // Pascha 2026 = Apr 12, Pentecost = May 31. Apr 20 sits inside
        // Paschaltide and should read as EASTER season.
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 20), CalendarChoice.EASTERN)
        assertEquals(LiturgicalSeason.EASTER, day.season)
        assertEquals("Paschaltide", day.dayName)
    }

    // ─────────────────────────────────────────────────────────────────────
    // NONE preference defaults to Western
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `NONE preference still returns a day (defaults to Western) for safety`() {
        val day = LiturgicalCalendar.today(LocalDate.of(2026, 4, 5), CalendarChoice.NONE)
        assertEquals(LiturgicalSeason.EASTER, day.season)
        assertEquals("Easter Sunday", day.dayName)
    }
}
