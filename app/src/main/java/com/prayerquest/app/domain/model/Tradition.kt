package com.prayerquest.app.domain.model

/**
 * The five prayer traditions surfaced during onboarding (DD §3.2 step 3) and
 * used by the Mode Picker to decide which shelves to display prominently vs.
 * collapse behind the "Explore more traditions" footer.
 *
 * Tradition-aware does not mean tradition-exclusive. Any user can enable any
 * mode — the enabled-traditions set only tunes defaults and shelf visibility.
 */
enum class Tradition(val displayName: String) {
    PROTESTANT("Protestant / Evangelical"),
    CATHOLIC("Catholic"),
    ORTHODOX("Orthodox"),
    ANGLICAN("Anglican / Episcopal"),
    NON_DENOMINATIONAL("Non-denominational");

    companion object {
        /**
         * Default set applied until the user completes the tradition step of
         * onboarding (or if they skip setup). We opt everyone in to the
         * universal-feeling set so the Mode Picker still looks populated on
         * day one, but Traditional modes default OFF so a brand-new Protestant
         * user isn't greeted with a Rosary shelf they didn't ask for. User can
         * switch any of these on instantly in Settings → Prayer Modes.
         */
        val DEFAULT: Set<Tradition> = setOf(PROTESTANT, NON_DENOMINATIONAL)
    }
}

/**
 * Traditions that consider a given mode "native" practice. A mode is shown on
 * the Mode Picker if ANY of its native traditions is in the user's enabled
 * set. Modes whose native-traditions set is empty are universal (shown to
 * everyone) — this covers the Quick/Guided/Expressive shelves that every
 * tradition uses.
 *
 * Values reflect where each practice *originated* and is most strongly
 * associated, not ownership. Evangelical Protestants absolutely pray the
 * Examen and Lectio Divina — but they'll typically opt in consciously, so
 * those modes only appear by default for users who chose a tradition where
 * they're everyday practice.
 */
val PrayerMode.nativeTraditions: Set<Tradition>
    get() = when (this) {
        // Universal — shown regardless of tradition selection
        PrayerMode.FLASH_PRAY_SWIPE,
        PrayerMode.BREATH_PRAYER,
        PrayerMode.INTERCESSION_DRILL,
        PrayerMode.GUIDED_ACTS,
        PrayerMode.VOICE_RECORD,
        PrayerMode.PRAYER_JOURNAL -> emptySet()

        // Ignatian — Catholic foundation, adopted widely
        PrayerMode.DAILY_EXAMEN -> setOf(
            Tradition.CATHOLIC,
            Tradition.ANGLICAN,
            Tradition.ORTHODOX
        )

        // Monastic / contemplative tradition
        PrayerMode.LECTIO_DIVINA -> setOf(
            Tradition.CATHOLIC,
            Tradition.ANGLICAN,
            Tradition.ORTHODOX
        )

        // Rosary / Jesus Prayer rope / Anglican beads
        PrayerMode.PRAYER_BEADS -> setOf(
            Tradition.CATHOLIC,
            Tradition.ANGLICAN,
            Tradition.ORTHODOX
        )

        // BCP / Liturgy of the Hours / Horologion
        PrayerMode.DAILY_OFFICE -> setOf(
            Tradition.CATHOLIC,
            Tradition.ANGLICAN,
            Tradition.ORTHODOX
        )
    }

/**
 * True when a mode is visible by default for the given enabled-traditions set.
 * Universal modes (empty nativeTraditions) are always visible; tradition-tagged
 * modes appear when the user has at least one overlapping tradition enabled.
 *
 * The user's per-mode override set (see UserPreferences.disabledModes) takes
 * precedence — this function answers "is the mode eligible for the default
 * visible shelf" only.
 */
fun PrayerMode.isVisibleFor(enabledTraditions: Set<Tradition>): Boolean {
    val native = nativeTraditions
    if (native.isEmpty()) return true
    return native.any { it in enabledTraditions }
}
