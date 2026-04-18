package com.prayerquest.app.data.gratitude

import android.content.Context
import org.json.JSONObject

/**
 * Loads the bundled gratitude prompt catalogue from
 * `assets/gratitude/prompts.json`.
 *
 * The JSON file has two parallel lists:
 *   * `prompts` — longer, Scripture-linked suggestions for the daily
 *     gratitude prompt card.
 *   * `speed`   — short, tappable chip labels for the Gratitude Speed
 *     Round (DD §3.6). Each speed item's `text` is what the user sees on
 *     the chip AND the seed text written into the GratitudeEntry when the
 *     chip is tapped.
 *
 * Parsing is synchronous, done on a background dispatcher by callers.
 * The asset is small (a few KB) so we load on-demand rather than caching
 * across the whole app; Compose ViewModels hold their own in-memory copy
 * for the duration of a screen.
 */
object GratitudePromptsLoader {

    /** Full prompt — shown in the daily prompt card with a Scripture hint. */
    data class Prompt(val text: String, val verse: String?)

    /** Speed Round chip — short label + optional Scripture reference. */
    data class SpeedChip(val text: String, val verse: String?)

    /**
     * Loads all speed-round chips from the bundled asset. Returns an empty
     * list on any parse failure so the Speed Round still opens (the user
     * can always type or speak instead).
     */
    fun loadSpeedChips(context: Context): List<SpeedChip> =
        loadArray(context, "speed").map { SpeedChip(it.text, it.verse) }

    /** Loads all long-form prompts for the daily prompt card. */
    fun loadPrompts(context: Context): List<Prompt> =
        loadArray(context, "prompts").map { Prompt(it.text, it.verse) }

    // ──────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────

    private data class Raw(val text: String, val verse: String?)

    private fun loadArray(context: Context, key: String): List<Raw> = runCatching {
        val json = context.assets.open("gratitude/prompts.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(json)
        val arr = root.optJSONArray(key) ?: return@runCatching emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val text = obj.optString("text").trim()
                if (text.isEmpty()) continue
                val verse = obj.optString("verse").takeIf { it.isNotBlank() }
                add(Raw(text, verse))
            }
        }
    }.getOrElse { emptyList() }
}
