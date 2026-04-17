package com.prayerquest.app.billing

/**
 * Central registry of every feature limit that's gated by premium.
 *
 * Keeping these in one file — rather than scattered across repositories and
 * ViewModels — makes it trivial to:
 *  • audit what a free vs. premium user actually gets
 *  • tweak caps for a promotional campaign without hunting for magic numbers
 *  • reference the same caps from the paywall UI (so the feature list on the
 *    upgrade screen always matches the actual enforcement)
 *
 * All functions take an `isPremium: Boolean` rather than looking it up via a
 * side channel — the call site always has it handy (via [LocalIsPremium] or
 * a ViewModel's StateFlow) and passing it keeps this object pure and
 * testable.
 */
object PremiumFeatures {

    /** Hard cap on gratitude-entry photo attachments per month for free users. */
    const val FREE_GRATITUDE_PHOTOS_PER_MONTH = 10

    /**
     * Hard cap on testimony photos per Answered Prayer archive for free users.
     * Premium is unlimited. Kept smaller than the monthly gratitude limit
     * because answered-prayer entries are rarer / more intentional.
     */
    const val FREE_ANSWERED_PRAYER_PHOTOS = 3

    /**
     * Free-tier group member cap. Premium members get a larger cap (below)
     * which lets a family / small-group leader add everyone in one place.
     */
    const val FREE_GROUP_MEMBER_LIMIT = 5
    const val PREMIUM_GROUP_MEMBER_LIMIT = 25

    /**
     * Free-tier groups-created cap — how many groups one user can be the
     * admin of. Prevents a single user from spinning up unlimited groups and
     * having the invite-code pool bloat. Premium lifts the cap so a pastor /
     * small-group leader can run several groups from one account.
     */
    const val FREE_GROUPS_CREATED_LIMIT = 2
    const val PREMIUM_GROUPS_CREATED_LIMIT = 10

    /**
     * Premium-only: custom emoji/icon per group. Free-tier groups pick from a
     * small curated emoji set; premium can paste any unicode emoji (or in a
     * future release, upload a custom image). The gate check here is simple
     * enough to just inline, but keeping the constant here documents that
     * it's an intentional premium-only feature.
     */
    const val FREE_GROUP_EMOJI_PICKER_SIZE = 12
    // Premium: unlimited — callers should branch on isPremium rather than
    // relying on this value. Present for symmetry / documentation.

    // ── Photo caps ───────────────────────────────────────────────────────

    fun gratitudePhotosPerMonthFor(isPremium: Boolean): Int =
        if (isPremium) Int.MAX_VALUE else FREE_GRATITUDE_PHOTOS_PER_MONTH

    fun answeredPrayerPhotosFor(isPremium: Boolean): Int =
        if (isPremium) Int.MAX_VALUE else FREE_ANSWERED_PRAYER_PHOTOS

    // ── Group caps ───────────────────────────────────────────────────────

    fun groupMemberLimitFor(isPremium: Boolean): Int =
        if (isPremium) PREMIUM_GROUP_MEMBER_LIMIT else FREE_GROUP_MEMBER_LIMIT

    fun groupsCreatedLimitFor(isPremium: Boolean): Int =
        if (isPremium) PREMIUM_GROUPS_CREATED_LIMIT else FREE_GROUPS_CREATED_LIMIT

    /**
     * Whether this user can pick a custom (non-curated) emoji for a new
     * group. Used by [com.prayerquest.app.ui.groups.CreateGroupScreen] to
     * decide whether to show the full emoji grid or route through the
     * paywall on emoji tap.
     */
    fun canUseCustomGroupEmoji(isPremium: Boolean): Boolean = isPremium
}
