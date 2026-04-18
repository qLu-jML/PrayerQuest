package com.prayerquest.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.prayerquest.app.R

/**
 * Bottom navigation destinations — Home, Pray, Library, Groups, Profile.
 *
 * Settings was moved out of the bottom bar — it's now reachable via the
 * cog icon in the Home top bar and a "Settings" button inside the Profile
 * screen. Groups took its slot so Prayer Groups is a top-level tap.
 *
 * Each destination has either an [icon] (Material ImageVector) or an [iconRes]
 * (custom drawable resource) — never both.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null
) {
    HOME("home", "Home", icon = Icons.Default.Home),
    PRAY("pray", "Pray", iconRes = R.drawable.ic_praying_hands),
    LIBRARY("library", "Library", icon = Icons.Default.AutoStories),
    GROUPS("prayer_groups", "Groups", icon = Icons.Default.Groups),
    PROFILE("profile", "Profile", icon = Icons.Default.Person)
}

/**
 * Non-bottom-bar routes for detail screens (Phase 2+).
 */
object Routes {
    /**
     * Sentinel value used for the optional `collectionId` query arg — nav
     * LongType doesn't support nullable, so -1 means "no collection". Bake
     * the constant in one place so producers/consumers stay in sync.
     */
    const val NO_COLLECTION: Long = -1L

    /**
     * Mode picker — "Netflix of Prayer Modes" (DD §3.1.3). Entry point for every
     * prayer session: user picks a mode here, then the session runs that mode.
     *
     * Optional `collectionId` query arg — when launched from a collection
     * detail screen we carry it through so the resulting session prays the
     * items in that collection instead of the user's global Active list. The
     * picker itself doesn't care about the id; it just forwards it when
     * navigating to the session.
     */
    const val PRAYER_MODE_PICKER = "prayer_mode_picker?collectionId={collectionId}"
    fun prayerModePicker(collectionId: Long? = null): String =
        "prayer_mode_picker?collectionId=${collectionId ?: NO_COLLECTION}"

    /**
     * Prayer session accepts the chosen mode as a path arg. The value is a
     * [com.prayerquest.app.domain.model.PrayerMode] enum name (e.g.
     * "GUIDED_ACTS"). An optional "mixed" sentinel (empty string) preserves the
     * older cycle-through-all-modes behavior for callers that still want it
     * (e.g., quest prompts requiring ≥3 modes).
     *
     * Optional `collectionId` query arg — when set (≥ 0) the session loads
     * that collection's prayer items as the session queue instead of the
     * user's global Active list.
     */
    const val PRAYER_SESSION = "prayer_session/{mode}?collectionId={collectionId}"
    const val PRAYER_SESSION_MIXED = "prayer_session/MIXED"
    fun prayerSession(mode: String, collectionId: Long? = null): String =
        "prayer_session/$mode?collectionId=${collectionId ?: NO_COLLECTION}"

    /**
     * List picker — shown between the Mode Picker and the Session when the
     * user hasn't already chosen a collection. Offers three buckets:
     *   1. "General Prayers" — their global active list (no collection filter)
     *   2. Any private prayer collection they've created
     *   3. A prayer group's shared requests (routes through Group Detail)
     *
     * The chosen mode name is carried through as a path arg so the screen
     * can hand it off to [prayerSession]. When the user is already inside a
     * collection (e.g. launched from Collection Detail), callers skip this
     * screen and jump straight to the session — the list is already implicit.
     */
    const val PRAYER_LIST_PICKER = "prayer_list_picker/{mode}"
    fun prayerListPicker(mode: String): String = "prayer_list_picker/$mode"

    const val COLLECTION_DETAIL = "collection_detail/{collectionId}"
    const val CREATE_COLLECTION = "create_collection"
    const val EDIT_COLLECTION = "edit_collection/{collectionId}"
    const val ADD_ITEMS_TO_COLLECTION = "add_items_to_collection/{collectionId}"
    fun addItemsToCollection(collectionId: Long): String =
        "add_items_to_collection/$collectionId"
    const val FAMOUS_PRAYER_DETAIL = "famous_prayer/{prayerId}"
    const val BIBLE_PRAYER_DETAIL = "bible_prayer/{prayerId}"
    const val ANSWERED_PRAYERS = "answered_prayers"
    const val GRATITUDE_LOG = "gratitude_log"
    const val GRATITUDE_CATALOGUE = "gratitude_catalogue"

    /**
     * Gratitude Speed Round (DD §3.6) — 60-second rapid-fire gratitude
     * logger. Distinct route from [GRATITUDE_LOG] because the UX shape is
     * totally different (single-screen, no per-entry photo picker, no
     * category selector, live timer) and its back-stack semantics diverge:
     * the user explicitly wants "back" to drop them back on the Log screen
     * they came from, not wipe the staged entries list.
     */
    const val GRATITUDE_SPEED_ROUND = "gratitude_speed_round"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
    const val ADD_GROUP_PRAYER = "add_group_prayer/{groupId}"
    fun addGroupPrayer(groupId: Long): String = "add_group_prayer/$groupId"

    /**
     * Premium paywall — shown from Profile, inline gates, or the "tap to
     * remove ads" CTA. Static route (no args) because the offer is the same
     * regardless of where the user came from. If we ever want to A/B test
     * different headlines per entry point, move to a query arg.
     */
    const val PAYWALL = "paywall"

    /**
     * Big Celebration Moment (DD §3.5.2) — full-screen modal that plays when
     * a prayer is marked Answered (not just PartiallyAnswered). Callers reach
     * it by navigating to [answeredCelebration] after checking
     * [com.prayerquest.app.data.repository.MarkAnsweredResult.wasNewlyAnswered]
     * so re-marking an already-answered prayer never replays the celebration.
     *
     * The screen itself schedules the +365-day anniversary worker in a
     * LaunchedEffect so the celebration view is the single write-point for
     * both the confetti UI and the anniversary follow-up.
     */
    const val ANSWERED_CELEBRATION = "answered_celebration/{prayerId}"
    fun answeredCelebration(prayerId: Long): String =
        "answered_celebration/$prayerId"

    // ═══════════════════════════════════════════
    // CRISIS PRAYER MODE (DD §3.10)
    // ═══════════════════════════════════════════
    //
    // Intentionally a separate route tree from the regular prayer session
    // flow. Crisis Prayer does NOT award XP, does NOT advance streaks, and
    // must not share a back-stack entry with a session ViewModel that
    // might. Keeping the routes distinct means the NavHost entries wire
    // to dedicated screens with no GamificationRepository dependency.

    /** Root Crisis screen — three-tile hub reached from the Home "In distress?" link. */
    const val CRISIS_PRAYER = "crisis_prayer"

    /** Paged Psalms reader inside Crisis Prayer Mode. */
    const val CRISIS_PSALMS = "crisis_psalms"

    /** Restricted Jesus-Prayer breath variant, 4-in/6-out. */
    const val CRISIS_BREATH = "crisis_breath"

    /** Crisis resources list (988 / Samaritans / Lifeline). */
    const val CRISIS_RESOURCES = "crisis_resources"
}
