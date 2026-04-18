package com.prayerquest.app.domain.liturgical

import com.prayerquest.app.data.preferences.LiturgicalCalendar as CalendarChoice
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The liturgical "season" bucket for a given day.
 *
 * The enum intentionally flattens a complicated reality: Roman Catholic,
 * Anglican, Lutheran, and Orthodox traditions all disagree about season
 * boundaries, what counts as "Ordinary Time," and whether Pentecost is a
 * season or a single feast. The buckets below are deliberately broad so we
 * can label every day in the year with exactly one season, which is the only
 * question the Home-screen indicator and the seasonal-pack auto-pin need to
 * answer. See DD §3.5.4.
 *
 * ORDINARY_TIME is the stretch between Christmas/Epiphany and Lent;
 * ORDINARY_TIME_2 is the longer stretch between Pentecost and the next
 * Advent. Splitting them mirrors the Roman rite's "first half / second half"
 * of Ordinary Time and lets future features (e.g., badges) target one or the
 * other without reshuffling the enum.
 */
enum class LiturgicalSeason {
    ADVENT,
    CHRISTMAS,
    EPIPHANY,
    ORDINARY_TIME,
    LENT,
    HOLY_WEEK,
    EASTER,
    PENTECOST,
    ORDINARY_TIME_2
}

/**
 * Major feasts that deserve their own callout. We intentionally only list the
 * ones the DD explicitly names, plus their Eastern analogues, so the badge on
 * the Home card stays distinctive. Minor saints days and octaves don't get a
 * feast value — they just surface through the [LiturgicalDay.season] bucket.
 *
 * Western and Eastern share enum values where the concept maps cleanly
 * (Palm Sunday, Easter, Pentecost); where the two traditions diverge
 * (e.g., Ash Wednesday has no Orthodox counterpart — they keep Clean Monday
 * instead) each gets its own value and only one is emitted per date.
 */
enum class LiturgicalFeast {
    // Shared / Western primary
    CHRISTMAS_EVE,
    CHRISTMAS,
    EPIPHANY,
    ASH_WEDNESDAY,
    PALM_SUNDAY,
    MAUNDY_THURSDAY,
    GOOD_FRIDAY,
    HOLY_SATURDAY,
    EASTER,
    PENTECOST,
    ALL_SAINTS,

    // Eastern specific
    CLEAN_MONDAY,
    LAZARUS_SATURDAY,
    HOLY_THURSDAY,
    GREAT_FRIDAY,
    ORTHODOX_CHRISTMAS,
    THEOPHANY,
    ORTHODOX_ALL_SAINTS
}

/**
 * One day's liturgical identity.
 *
 * [dayName] is the user-facing string already localized to the tradition
 * (e.g., "Ash Wednesday" for Western, "Clean Monday" for Eastern on the
 * equivalent date). The Home-screen indicator renders [dayName] on the top
 * line and [season] on the bottom; that's the entire surface area this type
 * needs to support.
 */
data class LiturgicalDay(
    val season: LiturgicalSeason,
    val dayName: String,
    val feast: LiturgicalFeast? = null
)

/**
 * Pure, no-IO, no-coroutines liturgical calendar engine.
 *
 * Given a civil date and the user's tradition choice, returns the
 * [LiturgicalDay] that covers that date. Easter is computed with the
 * standard anonymous-Gregorian (Meeus/Jones/Butcher) algorithm for Western,
 * and the Meeus Julian Paschalia formula converted to Gregorian for Eastern
 * — both are well-studied integer-only algorithms with no external
 * dependencies.
 *
 * Not all tradition-specific micro-feasts are modeled; the engine is scoped
 * to the feasts the DD explicitly surfaces (Christmas Eve/Day, Epiphany,
 * Ash Wednesday, Palm Sunday, Maundy/Holy Thursday, Good/Great Friday,
 * Easter/Pascha, Pentecost, All Saints). Extending it is additive — drop a
 * new value into [LiturgicalFeast] and branch inside [computeWestern] /
 * [computeEastern].
 */
object LiturgicalCalendar {

    /**
     * The entrypoint referenced by HomeViewModel and LibraryViewModel.
     *
     * If the user hasn't enabled a calendar ([CalendarChoice.NONE]), we still
     * return a Western day — that's a safe default for any caller that
     * forgets to check the preference first. HomeScreen and Library gate on
     * the preference before surfacing anything, so NONE-users never see it.
     */
    fun today(
        date: LocalDate = LocalDate.now(),
        calendar: CalendarChoice
    ): LiturgicalDay {
        return when (calendar) {
            CalendarChoice.EASTERN -> computeEastern(date)
            CalendarChoice.WESTERN,
            CalendarChoice.NONE -> computeWestern(date)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Western calendar (Roman Catholic / Protestant common)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Compute the Western liturgical day for [date].
     *
     * Season boundaries follow the modern Roman calendar with minor
     * simplifications that match popular Protestant usage:
     *  - Advent: First Sunday of Advent (4th Sunday before Christmas) → Dec 24
     *  - Christmas: Dec 25 → Jan 5
     *  - Epiphany: Jan 6 (kept as its own season bucket per DD enum list)
     *  - Ordinary Time: Jan 7 → Ash Wednesday (exclusive)
     *  - Lent: Ash Wednesday → Saturday before Palm Sunday
     *  - Holy Week: Palm Sunday → Holy Saturday
     *  - Easter: Easter Sunday → Saturday before Pentecost
     *  - Pentecost: Pentecost Sunday only
     *  - Ordinary Time 2: Monday after Pentecost → day before 1st Sun of Advent
     */
    fun computeWestern(date: LocalDate): LiturgicalDay {
        val year = date.year

        // Easter anchors most of the moving cycle.
        val easter = westernEaster(year)
        val ashWednesday = easter.minusDays(46)
        val palmSunday = easter.minusDays(7)
        val maundyThursday = easter.minusDays(3)
        val goodFriday = easter.minusDays(2)
        val holySaturday = easter.minusDays(1)
        val pentecost = easter.plusDays(49)

        // Fixed calendar dates, tied to the calendar year of `date`.
        val christmas = LocalDate.of(year, 12, 25)
        val christmasEve = LocalDate.of(year, 12, 24)
        val epiphany = LocalDate.of(year, 1, 6)
        val allSaints = LocalDate.of(year, 11, 1)

        val firstAdventSundayThisYear = firstSundayOfAdventWestern(year)
        val firstAdventSundayPrevYear = firstSundayOfAdventWestern(year - 1)

        // Feast check runs first — a feast always wins its day even if the
        // season label around it is "just Holy Week".
        val feast = when (date) {
            christmasEve -> LiturgicalFeast.CHRISTMAS_EVE
            christmas -> LiturgicalFeast.CHRISTMAS
            epiphany -> LiturgicalFeast.EPIPHANY
            ashWednesday -> LiturgicalFeast.ASH_WEDNESDAY
            palmSunday -> LiturgicalFeast.PALM_SUNDAY
            maundyThursday -> LiturgicalFeast.MAUNDY_THURSDAY
            goodFriday -> LiturgicalFeast.GOOD_FRIDAY
            holySaturday -> LiturgicalFeast.HOLY_SATURDAY
            easter -> LiturgicalFeast.EASTER
            pentecost -> LiturgicalFeast.PENTECOST
            allSaints -> LiturgicalFeast.ALL_SAINTS
            else -> null
        }

        // Handle the year-boundary edge cases first so the main dispatch
        // below can assume `date` sits in the current year's cycle.

        // Jan 1 .. Jan 5 → Christmastide from the PREVIOUS year's Dec 25.
        if (date.monthValue == 1 && date.dayOfMonth in 1..5) {
            return LiturgicalDay(
                season = LiturgicalSeason.CHRISTMAS,
                dayName = "Christmastide",
                feast = feast
            )
        }

        // Epiphany: Jan 6 — its own season bucket.
        if (date == epiphany) {
            return LiturgicalDay(LiturgicalSeason.EPIPHANY, "Epiphany", feast)
        }

        // Advent crosses into the LAST week of November; catch that first.
        if (date >= firstAdventSundayThisYear && date <= christmasEve) {
            return advdayWestern(date, firstAdventSundayThisYear, christmasEve, feast)
        }

        // Christmastide within this year (Dec 25 .. Dec 31).
        if (date.monthValue == 12 && date.dayOfMonth in 25..31) {
            val name = if (date == christmas) "Christmas Day" else "Christmastide"
            return LiturgicalDay(LiturgicalSeason.CHRISTMAS, name, feast)
        }

        // Ordinary Time I: Jan 7 .. day before Ash Wednesday.
        if (date.isAfter(epiphany) && date.isBefore(ashWednesday)) {
            return LiturgicalDay(LiturgicalSeason.ORDINARY_TIME, "Ordinary Time", feast)
        }

        // Lent: Ash Wednesday .. Saturday before Palm Sunday.
        if (date >= ashWednesday && date.isBefore(palmSunday)) {
            val name = if (date == ashWednesday) "Ash Wednesday" else "Lent"
            return LiturgicalDay(LiturgicalSeason.LENT, name, feast)
        }

        // Holy Week: Palm Sunday .. Holy Saturday.
        if (date >= palmSunday && date.isBefore(easter)) {
            val name = when (date) {
                palmSunday -> "Palm Sunday"
                maundyThursday -> "Maundy Thursday"
                goodFriday -> "Good Friday"
                holySaturday -> "Holy Saturday"
                else -> "Holy Week"
            }
            return LiturgicalDay(LiturgicalSeason.HOLY_WEEK, name, feast)
        }

        // Easter Sunday itself.
        if (date == easter) {
            return LiturgicalDay(LiturgicalSeason.EASTER, "Easter Sunday", feast)
        }

        // Eastertide: the 49 days between Easter and Pentecost, exclusive.
        if (date.isAfter(easter) && date.isBefore(pentecost)) {
            return LiturgicalDay(LiturgicalSeason.EASTER, "Eastertide", feast)
        }

        // Pentecost Sunday.
        if (date == pentecost) {
            return LiturgicalDay(LiturgicalSeason.PENTECOST, "Pentecost", feast)
        }

        // Ordinary Time II: day after Pentecost .. day before first Sunday
        // of Advent. Covers All Saints, the big summer stretch, and early
        // November.
        if (date.isAfter(pentecost) && date.isBefore(firstAdventSundayThisYear)) {
            val name = if (date == allSaints) "All Saints' Day" else "Ordinary Time"
            return LiturgicalDay(LiturgicalSeason.ORDINARY_TIME_2, name, feast)
        }

        // Fallback — shouldn't reach here in practice, but returning
        // something sane is better than throwing and breaking the Home card.
        return LiturgicalDay(LiturgicalSeason.ORDINARY_TIME, "Ordinary Time", feast)
    }

    /**
     * Label a date inside Advent with its week-anchor Sunday.
     *
     * "Second Sunday of Advent" is the DD's example label and also how
     * liturgical almanacs refer to mid-week Advent days — Monday after the
     * 2nd Sunday is still "within the Second Sunday of Advent" from a
     * labeling standpoint. Christmas Eve keeps its distinctive label.
     */
    private fun advdayWestern(
        date: LocalDate,
        firstAdventSunday: LocalDate,
        christmasEve: LocalDate,
        feast: LiturgicalFeast?
    ): LiturgicalDay {
        if (date == christmasEve) {
            return LiturgicalDay(LiturgicalSeason.ADVENT, "Christmas Eve", feast)
        }
        val daysIn = date.toEpochDay() - firstAdventSunday.toEpochDay()
        // weekIndex 0..3 → 1st..4th Sunday of Advent label
        val weekIndex = (daysIn / 7).toInt().coerceIn(0, 3)
        val ordinal = listOf("First", "Second", "Third", "Fourth")[weekIndex]
        return LiturgicalDay(
            season = LiturgicalSeason.ADVENT,
            dayName = "$ordinal Sunday of Advent",
            feast = feast
        )
    }

    /**
     * First Sunday of Advent for Western calendar, year [year].
     *
     * Defined as the fourth Sunday before Christmas. Equivalently, the
     * Sunday strictly before Christmas, minus three weeks. Falls between
     * Nov 27 and Dec 3 inclusive for any Gregorian year.
     */
    fun firstSundayOfAdventWestern(year: Int): LocalDate {
        val christmas = LocalDate.of(year, 12, 25)
        val sundayBeforeChristmas = christmas.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
        return sundayBeforeChristmas.minusDays(21)
    }

    /**
     * Meeus / Jones / Butcher anonymous Gregorian algorithm for Western
     * Easter. Returns the Gregorian civil date of Easter Sunday.
     *
     * Integer-only; no transcendentals, no lookup tables. Valid for any
     * Gregorian year 1583 onward.
     */
    fun westernEaster(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Eastern calendar (Orthodox, Old-Calendar fixed-date convention)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Compute the Eastern (Orthodox) liturgical day for [date].
     *
     * Uses the Julian-Paschalia cycle (converted to Gregorian) for the moving
     * feasts, and Old-Calendar fixed dates for Nativity (Jan 7) and Theophany
     * (Jan 19). This matches what most English-speaking users who select
     * "Eastern" in onboarding expect — the Nativity-on-January-7 convention
     * is the one most widely associated with "Orthodox Christmas" in popular
     * usage. Users whose parish keeps the Revised Julian calendar (Nativity
     * on Dec 25) are still served correctly for the Paschal cycle, which is
     * the part that actually drives seasonal-pack selection.
     *
     * Season boundaries:
     *  - Advent (Nativity Fast): Nov 28 .. Jan 6 (crosses year)
     *  - Christmas (Afterfeast of Nativity): Jan 7 .. Jan 18
     *  - Epiphany (Theophany): Jan 19
     *  - Ordinary Time I: Jan 20 .. day before Clean Monday
     *  - Lent (Great Lent): Clean Monday .. Lazarus Saturday
     *  - Holy Week: Palm Sunday .. Great Saturday
     *  - Easter (Paschaltide): Pascha .. day before Pentecost
     *  - Pentecost: Pentecost Sunday
     *  - Ordinary Time 2: day after Pentecost .. Nov 27
     */
    fun computeEastern(date: LocalDate): LiturgicalDay {
        val year = date.year

        val pascha = easternEaster(year)
        val cleanMonday = pascha.minusDays(48)
        val lazarusSaturday = pascha.minusDays(8)
        val palmSunday = pascha.minusDays(7)
        val holyThursday = pascha.minusDays(3)
        val greatFriday = pascha.minusDays(2)
        val holySaturday = pascha.minusDays(1)
        val pentecost = pascha.plusDays(49)
        // Orthodox All Saints: Sunday after Pentecost.
        val orthodoxAllSaints = pascha.plusDays(56)

        val orthodoxChristmas = LocalDate.of(year, 1, 7)
        val orthodoxChristmasEve = LocalDate.of(year, 1, 6)
        val theophany = LocalDate.of(year, 1, 19)

        val feast = when (date) {
            orthodoxChristmasEve -> LiturgicalFeast.CHRISTMAS_EVE
            orthodoxChristmas -> LiturgicalFeast.ORTHODOX_CHRISTMAS
            theophany -> LiturgicalFeast.THEOPHANY
            cleanMonday -> LiturgicalFeast.CLEAN_MONDAY
            lazarusSaturday -> LiturgicalFeast.LAZARUS_SATURDAY
            palmSunday -> LiturgicalFeast.PALM_SUNDAY
            holyThursday -> LiturgicalFeast.HOLY_THURSDAY
            greatFriday -> LiturgicalFeast.GREAT_FRIDAY
            holySaturday -> LiturgicalFeast.HOLY_SATURDAY
            pascha -> LiturgicalFeast.EASTER
            pentecost -> LiturgicalFeast.PENTECOST
            orthodoxAllSaints -> LiturgicalFeast.ORTHODOX_ALL_SAINTS
            else -> null
        }

        // ── Nativity Fast wraps the year boundary ──
        //   Nov 28 .. Dec 31 → Nativity Fast this year
        //   Jan 1  .. Jan 6  → Nativity Fast that started LAST year
        if (date.monthValue == 11 && date.dayOfMonth >= 28) {
            return LiturgicalDay(LiturgicalSeason.ADVENT, "Nativity Fast", feast)
        }
        if (date.monthValue == 12) {
            return LiturgicalDay(LiturgicalSeason.ADVENT, "Nativity Fast", feast)
        }
        if (date.monthValue == 1 && date.dayOfMonth <= 6) {
            val name = if (date == orthodoxChristmasEve) "Nativity Eve" else "Nativity Fast"
            return LiturgicalDay(LiturgicalSeason.ADVENT, name, feast)
        }

        // ── Nativity and Afterfeast (Christmas season) ──
        if (date == orthodoxChristmas) {
            return LiturgicalDay(LiturgicalSeason.CHRISTMAS, "Nativity of Christ", feast)
        }
        if (date.isAfter(orthodoxChristmas) && date.isBefore(theophany)) {
            return LiturgicalDay(LiturgicalSeason.CHRISTMAS, "Afterfeast of Nativity", feast)
        }

        // ── Theophany ──
        if (date == theophany) {
            return LiturgicalDay(LiturgicalSeason.EPIPHANY, "Theophany", feast)
        }

        // ── Ordinary Time I (Theophany + 1 .. Clean Monday - 1) ──
        if (date.isAfter(theophany) && date.isBefore(cleanMonday)) {
            return LiturgicalDay(LiturgicalSeason.ORDINARY_TIME, "Ordinary Time", feast)
        }

        // ── Great Lent: Clean Monday .. Lazarus Saturday ──
        if (date >= cleanMonday && date <= lazarusSaturday) {
            val name = when (date) {
                cleanMonday -> "Clean Monday"
                lazarusSaturday -> "Lazarus Saturday"
                else -> "Great Lent"
            }
            return LiturgicalDay(LiturgicalSeason.LENT, name, feast)
        }

        // ── Holy Week: Palm Sunday .. Great Saturday ──
        if (date >= palmSunday && date.isBefore(pascha)) {
            val name = when (date) {
                palmSunday -> "Palm Sunday"
                holyThursday -> "Holy Thursday"
                greatFriday -> "Great Friday"
                holySaturday -> "Great Saturday"
                else -> "Holy Week"
            }
            return LiturgicalDay(LiturgicalSeason.HOLY_WEEK, name, feast)
        }

        // ── Pascha (Easter Sunday) ──
        if (date == pascha) {
            return LiturgicalDay(LiturgicalSeason.EASTER, "Pascha", feast)
        }

        // ── Paschaltide: 49 days after Pascha ──
        if (date.isAfter(pascha) && date.isBefore(pentecost)) {
            return LiturgicalDay(LiturgicalSeason.EASTER, "Paschaltide", feast)
        }

        // ── Pentecost ──
        if (date == pentecost) {
            return LiturgicalDay(LiturgicalSeason.PENTECOST, "Pentecost", feast)
        }

        // ── Ordinary Time II: day after Pentecost .. Nov 27 ──
        val nativityFastStart = LocalDate.of(year, 11, 28)
        if (date.isAfter(pentecost) && date.isBefore(nativityFastStart)) {
            val name = if (date == orthodoxAllSaints) "All Saints" else "Ordinary Time"
            return LiturgicalDay(LiturgicalSeason.ORDINARY_TIME_2, name, feast)
        }

        return LiturgicalDay(LiturgicalSeason.ORDINARY_TIME, "Ordinary Time", feast)
    }

    /**
     * Eastern (Orthodox) Easter — Pascha — returned as a GREGORIAN civil date.
     *
     * Uses Meeus's Julian Paschalia formula to compute Pascha on the Julian
     * calendar, then converts the result to the proleptic Gregorian calendar
     * so that [LocalDate] stays well-formed. For all years 1900–2099 the
     * offset is a constant 13 days; a formula is used anyway so the engine
     * stays correct past 2100 without a silent drift.
     */
    fun easternEaster(year: Int): LocalDate {
        // Meeus's Julian Paschalia (produces a date on the Julian calendar).
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val julianMonth = (d + e + 114) / 31
        val julianDay = ((d + e + 114) % 31) + 1

        // Treat the computed Julian date as if it were a Gregorian literal,
        // then add the Julian→Gregorian offset to land on the civil Gregorian
        // date. Valid because the Julian date produced here is always in
        // March/April, which is comfortably inside any Gregorian month.
        val literalAsGregorian = LocalDate.of(year, julianMonth, julianDay)
        val offsetDays = julianToGregorianOffset(year)
        return literalAsGregorian.plusDays(offsetDays.toLong())
    }

    /**
     * The number of days the Gregorian calendar currently leads the Julian
     * calendar by, for a given Gregorian [year]. 13 days for 1900–2099,
     * 14 days for 2100–2199, etc. Computed rather than hard-coded so the
     * engine keeps working past 2099 without a ticking bomb.
     *
     * Formula: floor(Y/100) - floor(Y/400) - 2.
     */
    private fun julianToGregorianOffset(year: Int): Int {
        return year / 100 - year / 400 - 2
    }
}
