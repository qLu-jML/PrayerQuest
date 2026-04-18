# PrayerQuest — Roadmap to v1.0 Release (and v1.1 Backlog)

**Generated:** 2026-04-17
**Author:** Claude (project survey + DD cross-check)
**Purpose:** A sequenced punch-list from the current codebase state to shipping v1.0 on the Play Store, then a prioritized v1.1 backlog.

Each section below is a sprint with:
1. **Why** — where it sits in the DD
2. **What's already on disk** — so nothing gets re-built
3. **What's still missing** — the delta
4. **Prompt** — a self-contained block you can paste into a fresh Claude session to land that sprint

Prompts deliberately reference `design/prayer_quest_design_document_DD` (the DD) and `CLAUDE.md` so each session boots with the right spec. They also end with a verification step so the work lands correctly.

---

## Snapshot of current state (2026-04-17)

**Shipping code on disk covers a substantial chunk of MVP already:**
- Full Room schema at DB v9: PrayerItem, Collections, PrayerRecord, UserStats, StreakData (with hearts/freezes), Gratitude, FamousPrayer, BiblePrayer, NameOfGod, FastingSession, PrayerGroup + members + items + activity, DailyQuest, DailyActivity, AchievementProgress. (v8→v9 drops the legacy `devotional` table — devotional author feature was cut 2026-04-17.)
- Manual `AppContainer` DI, all repositories, companion `Factory` ViewModels.
- All 10 MVP prayer modes implemented (ACTS, Voice, Journal, Intercession, Flash-Pray, Examen, Lectio, Breath, Beads, Daily Office).
- Deprecated modes purged from `PrayerMode` enum.
- Tradition-aware Netflix-shelf Mode Picker.
- 7-step onboarding (welcome → name/goal → traditions → liturgical calendar → notifications/quiet hours → first collection → first gratitude).
- Settings, Home, Library, Profile screens; Collections CRUD; Formation Mode; premium paywall; AdMob + Billing scaffolding.
- Gratitude Log + Catalogue screens.
- Notifications with Quiet Hours guard (prayer reminder, streak alert, quest, gratitude prompt) + scheduler.
- Prayer Groups fully wired on Firestore (Auth, snapshot listeners, 5 screens, Cloud Function for user deletion cleanup).
- Famous Prayers JSON, Bible Prayers JSON, 4 suggested packs.
- Privacy policy + delete-account HTML pages.
- Play Store store/ folder scaffolded (title, short, full description, launch checklist, data safety guide, screenshots plan).

**Still missing for v1.0 (Phase 4a):**
- Names of God UI tab + `assets/prayers/names_of_god.json`.
- `assets/legal/fasting_disclaimer.md`.
- Seasonal prayer packs (Advent, Lent, Holy Week).
- Liturgical Calendar engine (pure date→season fn) + Home day indicator + auto-surfacing seasonal packs.
- Auto-tagging heuristic on prayer-item creation.
- Big Celebration Moment for answered prayers (confetti + on-the-fly verse card + 1-year anniversary reminder).
- Crisis Prayer Mode screen (curated Psalms + paced Breath Prayer + optional resource links).
- Speed Round inside Gratitude section + calendar heat-map + keyword search + export-as-image-card.
- Photo Prayers for active items (`testimonyPhotoUri` exists; active-item photo path + UI picker).
- Localization scaffold: `strings.xml` currently holds a single entry; all UI text is hardcoded.
- `SPRINT_3_TEST_CHECKLIST.md` still references the three deprecated modes — needs a refresh.
- Release hardening: ProGuard rules, real AdMob/billing IDs, upload keystore, screenshots, data safety form, signed `.aab`.

**Deferred to v1.1 (Phase 4b per DD §8):**
- Fasting Tracker UI (entity + repo already exist).
- Memory & Legacy: Year in Review, On This Day, Testimony Book PDF, Time-Capsule voice notes.
- Prayer Partners (2-person group UI on existing Firestore base).
- Localized content packs (Spanish, Korean, Portuguese).

---

# PATH TO v1.0 RELEASE

Run these sprints in order. Each is scoped to one pull-and-smoke-test boundary.

---

## Sprint A — Missing Content Assets (authoring-only, no code)

**Why:** Library tab and Liturgical surfacing depend on JSON assets that don't exist yet. Authoring is the rate-limiter, not code.
**On disk already:** `famous_prayers.json`, `bible_prayers.json`, 4 suggested packs, `gratitude/prompts.json`, `names_of_god.json` (authored 2026-04-17), `packs/advent.json`, `packs/lent.json`, `packs/holy_week.json` (all authored 2026-04-17), `legal/fasting_disclaimer.md` (authored 2026-04-17).
**Missing:**
- (nothing — Sprint A is complete as of 2026-04-17; the Bonhoeffer / Spurgeon devotional feature was cut entirely, so those assets are intentionally NOT authored.)

### Prompt — Sprint A

*Sprint A is complete. Content was authored on 2026-04-17: `names_of_god.json` (35 entries), `packs/advent.json` (15 entries including the O Antiphons), `packs/lent.json` (13 entries), `packs/holy_week.json` (7 entries Palm Sunday → Holy Saturday), `legal/fasting_disclaimer.md`. The devotional author feature (Spurgeon / Bonhoeffer) was cut from the product, so no devotional JSON assets were authored. Proceed to Sprint B.*

---

## Sprint B — Library: Names of God tab + importer wiring

**Why:** DD §3.5.3 — the Library is described as *unified* with "Famous Prayers," "Names of God," and user favorites. The entity, DAO, and repo methods are already on disk; only importer + UI are missing.
**On disk already:** `NameOfGod` entity, `NameOfGodDao`, `LibraryRepository.observeNamesOfGod` / `searchNamesOfGod` / `incrementNameOfGodPrayedCount`.
**Missing:** importer for `names_of_god.json` (idempotent, chunked, matches `FamousPrayerImporter` pattern), Names of God tab in `LibraryScreen`, detail screen with "Pray this name" → Guided ACTS or Breath Prayer, usage-count badges.

### Prompt — Sprint B

```
Read design/prayer_quest_design_document_DD §3.5.3, then CLAUDE.md.

Confirm app/src/main/assets/prayers/names_of_god.json exists (from Sprint A). If it doesn't, STOP and tell me — we can't land this sprint without it.

Implement the Names of God tab in the unified Prayer Library.

1) Importer: data/prayer/NameOfGodImporter.kt
   - Exact pattern of data/prayer/FamousPrayerImporter.kt — idempotent, chunked, safe to re-run, logs inserted/updated counts.
   - Wire it into the same boot path that runs FamousPrayerImporter and BiblePrayerImporter so it runs on first launch.
   - Skip already-imported rows via upsert-by-id.

2) Library UI:
   - Edit ui/screen/LibraryScreen.kt (large file — read first). Add a Names of God filter chip alongside the existing Famous Prayers / Bible Prayers filters.
   - When the Names of God filter is active, render a Netflix-style HorizontalRail grouped by language (Hebrew / Greek / English / Other) using the same RAIL_CARD_WIDTH / RAIL_CARD_HEIGHT constants.
   - Each card shows: name, transliteration, one-line meaning. Tap → detail screen.

3) Detail screen: ui/library/NameOfGodDetailScreen.kt
   - Mirror the UiState sealed interface pattern we fixed in Phase 1 (Loading / Loaded(NameOfGod) / NotFound).
   - Large hero (name, transliteration, meaning), Scripture reference + verse text body, usage-count chip.
   - Two primary CTAs: "Pray this name — Guided ACTS" (launches PrayerSessionScreen in GUIDED_ACTS mode pre-focused on that attribute) and "Breath Prayer for this name" (launches BREATH_PRAYER with the name as the centering phrase).
   - Increment usage count via `libraryRepository.incrementNameOfGodPrayedCount(id)` on either CTA.

4) Badges: add Grace Warrior-class achievements to domain/model/Achievement.kt:
   - NAMES_OF_GOD_FIRST (pray your first name of God)
   - NAMES_OF_GOD_ALL (pray all 30 at least once)
   - NAMES_OF_GOD_50 (50 total name-of-God prayers)
   Wire checks into GamificationRepository.onPrayerSessionCompleted()'s hot path (the existing mode-session completion flow). Do not invent a new flow.

5) Navigation: add a new destination in ui/navigation/Destinations.kt for the detail screen, route it from LibraryScreen, honour bottom-nav behaviour (root for Pray tab, back-arrow modal for drill-downs).

Verification:
- Fresh install → Library → filter "Names of God" → 30+ cards render grouped by language.
- Tap a card → detail screen shows Loading briefly then full card.
- "Pray this name" buttons both launch the expected mode with the name pre-filled.
- Uninstall-reinstall: importer runs once, no duplicates.
- Pray a name → achievement NAMES_OF_GOD_FIRST unlocks.
- Build: ./gradlew :app:assembleDebug must succeed with no warnings new vs baseline.
```

---

## Sprint C — Auto-tagging heuristic on prayer-item creation

**Why:** DD §3.5.1 — "when the user adds a prayer item, a lightweight keyword heuristic suggests category tags (Family, Health, Work, Spiritual, etc.) which the user can accept or dismiss with a tap. Fully on-device, no AI required."
**On disk already:** Nothing — `PrayerItem` has a `category: String?` column but nothing populates it automatically.
**Missing:** pure-function tagger + Compose chips in the Add-Item flow.

### Prompt — Sprint C

```
Read design/prayer_quest_design_document_DD §3.5.1 and CLAUDE.md.

Implement on-device auto-tagging for new prayer items.

1) domain/tagging/PrayerTagger.kt — a pure Kotlin singleton object:
   - Input: prayer title + description.
   - Output: List<SuggestedTag> where each tag has category, confidence (0f–1f), and the matched keyword.
   - Categories: Family, Health, Work/Vocation, Finances, Spiritual Growth, Relationships, Community/Friends, Nation/World, Travel, Grief/Loss, Thanksgiving, Other.
   - Implementation: a single immutable Map<Category, List<Regex>> of keyword patterns. Case-insensitive word-boundary matching. Score = (matches * weight) / totalWords, clamped to [0f, 1f]. Top 3 tags above a threshold (default 0.15f) are returned.
   - NO network, NO AI, NO on-device ML. Regex only. Reason: DD says "lightweight keyword heuristic, fully on-device." Honour the DD.
   - Ship with ~20–40 keywords per category. Include synonym families (e.g., Family: mom/mother/dad/father/sister/brother/son/daughter/aunt/uncle/grandma/grandpa).

2) Unit tests: test/java/com/prayerquest/app/domain/tagging/PrayerTaggerTest.kt
   - Cover: obvious Family hits ("pray for my mom"), obvious Health hits ("healing for Dad's cancer"), ambiguous inputs ("give me strength" → Spiritual Growth default, confidence low), empty input (no tags returned), multi-category ("my sister's surgery" → Family + Health).
   - Guard: threshold must reject clearly off-topic matches.

3) UI wiring in the Add Prayer Item flow (likely ui/collections/ or wherever prayer-item creation lives — grep for `addPrayerItem` to confirm).
   - As the user types title/description, debounce 300ms then call PrayerTagger.suggest() on the combined text.
   - Render up to 3 suggested tags as FilterChip above the save button. Tap to accept (chip fills), tap again to dismiss (chip clears).
   - On save, persist the first accepted tag into PrayerItem.category; if none accepted, leave null.
   - Add a small accessibility content description: "Suggested tag: Family. Tap to apply."

4) Do not break the existing manual category picker if there is one. Manual selection always wins over auto-suggested.

Verification:
- Unit tests must pass: ./gradlew :app:testDebugUnitTest
- Create a new prayer item titled "Healing for Mom's surgery" — Family + Health chips appear.
- Create one titled "Promotion at work" — Work/Vocation chip appears.
- Build is green.
```

---

## Sprint D — Liturgical Calendar engine + Home surfacing

**Why:** DD §3.5.5 — pure-function date→season engine, Home screen shows current liturgical day, seasonal packs auto-surface. Onboarding already collects the user's calendar preference (Western/Eastern); only the engine and UI are missing.
**On disk already:** `LiturgicalCalendar` enum in UserPreferences; onboarding step collects it; Advent/Lent/Holy Week packs (from Sprint A).
**Missing:** the pure calendar engine, Home indicator card, auto-surface logic in Library.

### Prompt — Sprint D

```
Read design/prayer_quest_design_document_DD §3.5.5 and §3.2 step 4. Read CLAUDE.md.

Build the Liturgical Calendar engine and surface it in Home + Library.

1) domain/liturgical/LiturgicalCalendar.kt — a pure object, no IO, no coroutines:
   - fun today(date: LocalDate = LocalDate.now(), calendar: LiturgicalCalendar): LiturgicalDay
   - LiturgicalDay data class: season (Advent / Christmas / Epiphany / OrdinaryTime / Lent / HolyWeek / Easter / Pentecost / OrdinaryTime2), dayName (e.g., "Second Sunday of Advent", "Ash Wednesday"), feast (nullable — major feasts only), western vs eastern variance handled.
   - Pure algorithm — compute Easter via Meeus/Jones/Butcher for Western; use Julian calendar + conversion for Eastern Orthodox Easter (Paschal full moon).
   - Cover major feasts: Christmas Eve, Christmas, Epiphany, Ash Wednesday, Palm Sunday, Maundy Thursday, Good Friday, Easter, Pentecost, All Saints.
   - Ship comprehensive unit tests (at least 30 dated cases across Western + Eastern calendars spanning 2024–2028). Must include: an Advent Sunday, Christmas Day, Ash Wednesday 2026, Good Friday 2026, Eastern Easter 2026, a day deep in Ordinary Time.

2) Home surfacing — edit ui/screen/HomeScreen.kt (large file; read first):
   - If user has liturgical calendar enabled (UserPreferences.liturgicalCalendar != NONE), show a small subtle card just under the streak row: dayName on one line, season on the next, a subtle stained-glass accent per the DD ("subtle, not kitsch").
   - If disabled, render nothing — no regression for non-liturgical users.
   - Tap → opens Library filtered to the relevant season's pack if one exists.

3) Seasonal pack auto-surfacing — edit wherever Library's pack list is assembled (data/prayer/SuggestedPrayerPackLoader.kt or the ViewModel):
   - Read today's LiturgicalDay at load time.
   - If season ∈ { Advent, Lent, HolyWeek } AND the corresponding pack exists, PIN it to the top of the Collections shelf with a small season badge.
   - Outside those seasons, the packs should still be loadable but not pinned.

4) Do NOT require network. All pack JSON is already bundled from Sprint A.

Verification:
- Unit tests for LiturgicalCalendar: ./gradlew :app:testDebugUnitTest — all 30+ cases green.
- Set device date to 2026-12-10 (Second Sunday of Advent, Western) → Home shows that label, Advent pack pinned in Library.
- Set device date to 2026-02-18 (Ash Wednesday, Western) → Home shows that label, Lent pack pinned.
- Disable calendar in Settings → Home indicator disappears cleanly.
- Build is green.
```

---

## Sprint E — Big Celebration Moment for answered prayers

**Why:** DD §3.5.2 — "when an item is marked 'Answered,' the app plays a full-screen celebration — confetti, a verse card rendered on-the-fly, a large 'Praise God!' moment, optional share to camera roll as a testimony image, and an anniversary reminder scheduled for one year later."
**On disk already:** Answered Prayers archive, testimony text/photo/voice, timeline view.
**Missing:** the full-screen celebration, on-the-fly verse card renderer, share-to-camera-roll, 1-year anniversary WorkManager job.

### Prompt — Sprint E

```
Read design/prayer_quest_design_document_DD §3.5.2 and §3.13 (notifications), and CLAUDE.md.

Implement the Big Celebration Moment that fires when a user marks a prayer "Answered" (not just partially answered).

1) ui/celebration/AnsweredCelebrationScreen.kt (new file)
   - Full-screen modal composable. Slides in over the current screen.
   - Elements, in order of animation:
     a) Confetti burst — use a lightweight particle effect drawn directly on Canvas (no 3rd-party confetti lib unless already in deps). 3-second animation, then steady-state gentle fall.
     b) Giant "Praise God!" in the app's display type. Gentle scale-in from 0.7x with spring animation.
     c) Auto-selected Scripture verse card (on-the-fly): pick one of ~20 curated thanksgiving verses hardcoded in a new domain/content/CelebrationVerses.kt. Render as a beautiful card (warm gradient bg, verse text, reference) using existing theme colors.
     d) Primary CTA: "Share testimony" → uses Android ShareSheet to share a rendered bitmap (capture the verse card + user's answered-prayer title via androidx.compose.ui.graphics.asAndroidBitmap or Canvas drawing) to camera roll OR any share target.
     e) Secondary CTA: "Close" returns to Answered detail screen.
   - Accessibility: confetti is decorative only; the card content must be screen-reader friendly with proper contentDescription.

2) 1-year anniversary reminder
   - In notifications/, add AnsweredPrayerAnniversaryWorker.kt (one-shot WorkManager job, scheduled for +365 days from the answered date).
   - Worker fires: "A year ago today, [answered prayer title] was answered. God is faithful." Tap → opens Answered Prayer Detail.
   - Respects QuietHoursGuard.
   - Schedule from PrayerRepository.markAnswered() OR from the celebration screen on dismiss — whichever is the single write-point for "Answered." Idempotent (use unique work id: "anniversary-$prayerItemId").

3) Trigger point
   - When user marks a PrayerItem status = Answered (not Partial), navigate to AnsweredCelebrationScreen as a modal over the previous screen. PartiallyAnswered does NOT trigger the celebration.
   - Do not celebrate items that were imported already-answered (check originalStatus != Answered).

4) Update domain/model/Achievement.kt:
   - FIRST_ANSWERED (first prayer marked answered) if not already present.
   - FAITHFUL_10 (10 answered prayers), FAITHFUL_50.
   - Wire into GamificationRepository — unlock check on mark-answered.

Verification:
- Mark any prayer Answered → full-screen celebration plays, confetti animates, verse card renders, Share button launches system share sheet with a bitmap.
- Set device clock forward 365 days → anniversary notification fires (or use WorkManager test harness).
- Mark Partial → no celebration (regression guard).
- Build is green.
```

---

## Sprint F — Crisis Prayer Mode

**Why:** DD §3.10 — prominent but subtle "in distress?" entry point from Home; curated Psalms playlist + paced Breath Prayer + gentle crisis resource links; fully offline; no XP.
**On disk already:** `BREATH_PRAYER` mode exists; `domain/content/BreathPrayerLibrary.kt`.
**Missing:** dedicated screen, entry point, curated Psalms content, crisis resource screen (no confidentiality assurances).

### Prompt — Sprint F

```
Read design/prayer_quest_design_document_DD §3.10 — read it carefully, especially the wellbeing notes about NOT making confidentiality claims and NOT requiring safety-assessment questions. Read CLAUDE.md.

Implement Crisis Prayer Mode.

1) domain/content/CrisisPsalmsLibrary.kt
   - Immutable list of curated Psalm excerpts: Psalm 23 (full), 27:1-5, 46:1-3 and 10, 91:1-6, 121 (full), 139:7-12, 42:1-5, 56:3-4. Each entry: reference, text, short one-line framing ("When you feel hunted"), estimated read time.

2) ui/crisis/CrisisPrayerScreen.kt
   - Entry is a calming fade-in. No XP, no streak impact, no quest progress — none of the gamification hot path fires for this screen. Wire this as an explicit NO-XP path, not by accident.
   - Top section: "You are not alone. God is with you." (or similar — faithful, not saccharine).
   - Middle section: three large tiles:
     a) "Psalms that breathe with you" → paged Psalms reader, one Psalm per card, swipe to advance. Slow auto-advance is available but OFF by default.
     b) "Paced breath prayer" → launches a restricted BreathPrayerMode variant with the Jesus Prayer and a slower 4-in / 6-out cadence. 3-minute default.
     c) "Crisis resources" → opens ui/crisis/CrisisResourcesScreen.kt.
   - Bottom: subtle "Return home" link.

3) ui/crisis/CrisisResourcesScreen.kt
   - List a few well-known helplines (988 Suicide & Crisis Lifeline US, Samaritans UK, Lifeline Australia) with phone numbers + "Call" intent buttons.
   - Do NOT make claims about confidentiality of any line. Do NOT make the user complete any safety-assessment flow to reach the resources. Per DD §3.10 wellbeing note.
   - Copy should be: "If you're in danger or thinking about harming yourself, these organizations may help. Tap to call." — neutral framing.
   - All content is bundled string resources. No network.

4) Entry point
   - Edit ui/screen/HomeScreen.kt: below the primary "Start Prayer" CTA, add a small low-profile link: "In distress? Crisis Prayer →". Intentionally subtle per DD.
   - Minimum tap target 48dp; accessible contentDescription.

5) Audit: grep for any code path that awards XP or touches GamificationRepository from the crisis flow. Confirm there are zero such calls. Add a test in PrayerSessionViewModelTest or a new CrisisModeTest that confirms XP doesn't change.

Verification:
- Tap the Home "In distress?" link → screen appears, three tiles work.
- Launch each tile; nothing crashes offline.
- Run a 1-minute breath session from Crisis → UserStats.totalXp does NOT increase (add a log + test to prove).
- Accessibility: TalkBack reads the screen coherently.
- Build is green.
```

---

## Sprint G — Gratitude polish: Speed Round + heat-map + search + export

**Why:** DD §3.6 — Speed Round replaces the deprecated Gratitude Blast mode; catalogue needs calendar heat-map, photo-only filter, keyword search, and export-as-image-card.
**On disk already:** GratitudeLogScreen, GratitudeCatalogueScreen, category chips, photo attachment.
**Missing:** Speed Round button + rapid-fire UI, calendar heat-map, keyword search, export-as-image-card, optional verse overlay on export.

### Prompt — Sprint G

```
Read design/prayer_quest_design_document_DD §3.6 and CLAUDE.md.

Polish the Gratitude section to ship-quality.

1) Speed Round
   - In ui/gratitude/GratitudeLogScreen.kt, add a prominent "Speed Round — rapid fire gratitude" button near the top.
   - New composable ui/gratitude/GratitudeSpeedRoundScreen.kt:
     - Full-screen. 60-second countdown timer visible at top.
     - Chip bank of ~30 quick-input prompts (scripture-backed, from assets/gratitude/prompts.json plus a curated "speed" subset). Tap a chip → it gets added as a gratitude entry with that chip as seed text.
     - Voice-to-text mic button also prominent — hold-to-speak, each pause commits a new entry.
     - Live counter: "5 thanks logged".
     - Sub-60-second completion with ≥5 entries triggers a speed bonus (+XP, toast "Grateful heart, fast hands!").
     - All entries write to the existing GratitudeEntry table — no separate store.
   - Wire bonus XP through GamificationRepository.onGratitudeLogged(speedBonus = true).

2) Calendar heat-map in ui/gratitude/GratitudeCatalogueScreen.kt
   - Compact year-view grid (53 columns × 7 rows) like GitHub contributions.
   - Cell color intensity reflects entry count that day (0 → background, 1 → light, 3+ → medium, 5+ → dark).
   - Tap a cell → filters the catalogue to that date.
   - Goes in a collapsible section at the top of the catalogue, closed by default.

3) Keyword search
   - Add a search TextField at the top of the catalogue.
   - Wire through GratitudeRepository.searchEntries(query) — a new DAO method using LIKE on text.
   - Debounce 250ms, collect on Flow.

4) Photo-only filter
   - FilterChip near the search bar: "With photos only". Toggles whether entries without photoUri are hidden.

5) Export as image card
   - Long-press any entry OR tap an overflow menu → "Share as image card".
   - Compose card renderer that lays out: gratitude text, user-selected verse overlay (optional, picked from a small curated list of thanksgiving verses — reuse CelebrationVerses from Sprint E if available), date, subtle PrayerQuest watermark.
   - Capture via androidx.compose.ui.graphics.asAndroidBitmap on a GraphicsLayer → write to cache → ACTION_SEND to camera roll / other apps.

6) Badges
   - Wire (or update) badges from DD §3.6: Gratitude Starter (7 days), Thankful Heart (30 consecutive), Abundance Mindset (100 total), Photo Gratitude (50 with photos), Faithful Thanksgiver (volume).

Verification:
- Speed Round: log 5 entries in <60s → speed bonus toast fires, XP increments.
- Heat-map renders, tapping a day filters correctly.
- Search "family" surfaces only matching entries.
- "With photos only" hides text-only entries.
- Share card renders as a bitmap and opens system share sheet.
- All existing gratitude flows (manual entry, photo picker) still work — regression guard.
- Build is green.
```

---

## Sprint H — Photo Prayers for active items

**Why:** DD §3.9 — photo attached to a prayer item, displays on the card and during prayer sessions for that item; app-private storage only; never uploaded.
**On disk already:** `PrayerItem.testimonyPhotoUri` (for answered prayers), photo picker + compression scaffolding (Gratitude uses it).
**Missing:** a separate `photoUri` column for active items, UI to attach/remove, display in prayer cards and session views.

### Prompt — Sprint H

```
Read design/prayer_quest_design_document_DD §3.9 and CLAUDE.md.

Add Photo Prayers for active items. These photos are DISTINCT from answered-prayer testimony photos — an active item can have a photo the entire time it's being prayed for (e.g., photo of the person).

1) Room migration
   - Add column `photoUri: String?` to PrayerItem.
   - Bump DB version 5 → 6 with an explicit Migration_5_6 in data/database/PrayerQuestDatabase.kt.
   - Schema export must update.

2) UI — attach/remove on prayer item
   - Edit the Add Prayer Item and Edit Prayer Item flows (grep for the screen). Add a circular photo slot with camera/gallery picker. Reuse the compression util already used for gratitude photos.
   - Store in app-private storage (files dir). Never expose URIs outside the app.

3) UI — display
   - Prayer item cards (Intercession Drill tap-bank, Flash-Pray cards, Active list) show the photo as a circular avatar thumbnail when present.
   - Inside GuidedActsMode, VoiceRecordMode, PrayerJournalMode: when praying a specific item, show the photo in the mode header.

4) Privacy guard
   - If the item is attached to a Prayer Group, do NOT upload the photo. The photo stays local. Add an inline test that confirms Firestore writes for group items never include photoUri.

5) Storage hygiene
   - On PrayerItem delete, delete the local photo file.
   - Add soft cap: free-tier users cap at 200 photos total across all prayer items + gratitude + testimony. Show a gentle premium prompt at the cap.

Verification:
- Create a prayer item with a photo → appears in the card, shows up during sessions.
- Add same item to a Group → inspect Firestore writes; photoUri is never sent.
- Delete the item → file is gone from disk.
- Cap test: create 200 photos, try to add 201st → premium paywall surfaces.
- DB migration: install older-build first, then upgrade build, confirm no data loss and photoUri column exists.
- Build is green.
```

---

## Sprint I — Localization scaffold

**Why:** DD §3.15 — "all UI strings externalized via standard Android resource files from day one." Today, `strings.xml` holds one entry; everything is hardcoded. This is the single largest blocker to shipping anywhere but English.
**On disk already:** nothing localizable.
**Missing:** systematic externalization pass + stub Spanish/Korean/Portuguese `values-*` directories ready for translation.

### Prompt — Sprint I

```
Read design/prayer_quest_design_document_DD §3.15 and CLAUDE.md.

Externalize all user-facing English strings. This is a disciplined, mechanical sweep — no feature work in this sprint.

1) Inventory
   - Grep the Compose codebase for hardcoded string literals appearing inside Text(), TextButton, contentDescription, stringResource-adjacent call sites, AnnotatedString builders, and stringFormatter calls.
   - Produce design/LOCALIZATION_AUDIT.md listing, per screen: file, line, literal. Target: capture every user-visible string.

2) Externalize
   - Add keys to app/src/main/res/values/strings.xml, grouped by feature (<!-- Home -->, <!-- Library -->, <!-- Gratitude -->, <!-- Crisis -->, <!-- Onboarding -->, <!-- Prayer modes -->, <!-- Notifications -->, <!-- Settings -->, <!-- Celebration -->).
   - Replace literals with stringResource(R.string.key) calls.
   - For strings with arguments, use <string name="foo">Something %1$s</string> + stringResource(R.string.foo, arg).
   - Pluralization → use <plurals>.

3) Stub translations
   - Create app/src/main/res/values-es/strings.xml, values-ko/strings.xml, values-pt/strings.xml with the same keys but English values temporarily (so the app builds and reveals any missing keys). Add a TODO comment at the top of each: "TRANSLATION PENDING — English fallback in place."

4) Configuration
   - Confirm AndroidManifest has android:supportsRtl="true".
   - Add res/xml/locales_config.xml listing supported locales (en, es, ko, pt).
   - Reference it in AndroidManifest: android:localeConfig="@xml/locales_config".

5) Guardrails
   - Add a Gradle lint check or a CI script (scripts/check_no_hardcoded_strings.sh) that fails if a Compose Text() call contains a literal that isn't obviously a test tag or a formatted number.

Verification:
- ./gradlew :app:lint — no MissingTranslation errors for en; values-es/ko/pt show as pending (warnings OK).
- Switch device language to Spanish → app still runs, English fallback visible. No crashes.
- Run the hardcoded-string check script → no new violations.
- LOCALIZATION_AUDIT.md is complete and committed.
```

---

## Sprint J — Test checklist refresh + internal smoke

**Why:** `SPRINT_3_TEST_CHECKLIST.md` still references Gratitude Blast, Scripture Soak, Contemplative Silence — modes that were deprecated. We need a current test checklist before internal-testing-track upload.
**On disk already:** the stale checklist.
**Missing:** a refreshed v1.0-scoped test checklist + one real pass on a device.

### Prompt — Sprint J

```
Read SPRINT_3_TEST_CHECKLIST.md, then design/prayer_quest_design_document_DD (scan for feature coverage), then CLAUDE.md.

Produce store/V1_TEST_CHECKLIST.md — the definitive pre-launch smoke test.

1) Remove any reference to Gratitude Blast, Scripture Soak, Contemplative Silence (these are deprecated — see CLAUDE.md).
2) Cover the 10 current modes only: ACTS, Voice Record, Prayer Journal, Intercession Drill, Flash-Pray Swipe, Daily Examen, Lectio Divina, Breath Prayer, Prayer Beads, Daily Office.
3) Add checklists for every feature that landed in Sprints A–I:
   - Names of God tab + detail + "Pray this name" flow
   - Auto-tagging on prayer-item creation
   - Liturgical calendar home indicator + seasonal pack pinning
   - Big Celebration for Answered + 1-year anniversary
   - Crisis Prayer Mode (verify NO XP awarded)
   - Speed Round, heat-map, gratitude search, export-as-image
   - Photo Prayers for active items
   - Locale swap (Spanish stub) — app renders fallback without crashing
4) Keep the strong sections of the old checklist (offline, first-launch seeding, bottom-nav, edge cases).
5) Add a dedicated "Release gating" block that references store/LAUNCH_CHECKLIST.md items.

Then do ONE real smoke pass on a connected Android device (or emulator). File any failures as triaged bugs in a new design/V1_SMOKE_RESULTS.md with screenshots where useful.

Verification:
- store/V1_TEST_CHECKLIST.md committed.
- design/V1_SMOKE_RESULTS.md committed, even if empty (say "Clean pass").
- No references to deprecated modes anywhere in the repo: run `grep -r "Scripture Soak\|Contemplative Silence\|Gratitude Blast\|SCRIPTURE_SOAK\|CONTEMPLATIVE_SILENCE\|GRATITUDE_BLAST" app/ design/ store/` — should return nothing.
```

---

## Sprint K — Release hardening (ProGuard, IDs, keystore, signing)

**Why:** LAUNCH_CHECKLIST.md §1 — ProGuard off, test AdMob IDs, no upload keystore config. All blockers to a real release build.
**On disk already:** debug build works; release config scaffolded.
**Missing:** ProGuard rules, AdMob real IDs, billing product ID alignment, upload keystore, signed AAB.

### Prompt — Sprint K

```
Read store/LAUNCH_CHECKLIST.md and CLAUDE.md. Read ../ScriptureQuest/app/build.gradle.kts and ../ScriptureQuest/app/proguard-rules.pro for the sibling release profile.

Harden PrayerQuest for a release build. This is infrastructure, not features.

1) app/build.gradle.kts release flavor:
   - isMinifyEnabled = true
   - isShrinkResources = true
   - proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
   - signingConfig = signingConfigs.getByName("release")
   - BuildConfig fields for AdMob unit IDs: one set for debug (Google test IDs), one set for release (placeholders like "REPLACE_WITH_REAL_AD_UNIT_ID" + a gradle warning if still placeholder at release build time).
   - Same for billing product ID — constant in PremiumManager must match Play Console SKU "prayerquest_premium_monthly".

2) app/proguard-rules.pro:
   - Keep Room @Entity @Dao @Database.
   - Keep Firestore / Firebase generated classes.
   - Keep kotlinx.serialization if used.
   - Keep all Compose inline classes.
   - Crib from ScriptureQuest's proguard-rules.pro as the template.

3) Signing
   - Add signingConfigs.release block that reads from gradle.properties (uploadKeystorePath, uploadKeystorePassword, uploadKeyAlias, uploadKeyPassword) with a clear "not set — release build will fail" warning if blank.
   - Add gradle.properties.template documenting what Nathan needs to fill in (never commit the real gradle.properties values to git — already gitignored).
   - Document the keystore-generation command in store/SIGNING_SETUP.md: keytool -genkey -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias prayerquest-upload.

4) AdMob + Billing verification
   - Grep for Google test ad unit IDs (ca-app-pub-3940256099942544/*). They MUST only appear in debug BuildConfig.
   - Fail-safe: AdManager throws if BuildConfig.DEBUG == false and ad unit id starts with ca-app-pub-3940256099942544.

5) Build
   - Run ./gradlew :app:bundleRelease (requires a filled-out gradle.properties to succeed end-to-end; if unfilled, the goal is the build surfaces the clear error).
   - Produce a short store/RELEASE_BUILD_REPORT.md capturing any final warnings/errors.

Verification:
- ./gradlew :app:assembleRelease succeeds locally with a sample keystore (you can generate a throwaway one for testing, NEVER commit it).
- Running the release APK does not show test ads (verify by BuildConfig.DEBUG check at runtime).
- store/SIGNING_SETUP.md and gradle.properties.template committed.
- No real credentials or keystores in the repo.
```

---

## Sprint L — Play Store listing assets

**Why:** LAUNCH_CHECKLIST.md §2 — title/short/full descriptions exist; feature graphic, app icon PNG, phone screenshots don't.
**On disk already:** store/listing/en-US/title.txt + short_description.txt + full_description.txt; SCREENSHOTS_PLAN.md; design/icon.jpg.
**Missing:** 512×512 app icon PNG, 1024×500 feature graphic, 8 phone screenshots, tablet screenshots.

### Prompt — Sprint L

```
Read store/SCREENSHOTS_PLAN.md, store/LAUNCH_CHECKLIST.md, and design/icon.jpg. Read CLAUDE.md.

Produce all Play Store graphic assets.

1) store/app_icon_512.png — 512×512, sourced from design/icon.jpg. Round-preview correct. No transparency. Solid background that works on light and dark Play Store themes.

2) store/feature_graphic_1024x500.png — 1024×500. Headline "Pray. Be grateful. See God's faithfulness." + PrayerQuest wordmark + a subtle background in the app's warm Indigo/gold palette (reference ui/theme/Color.kt).

3) store/screenshots/phone/ — 8 PNGs at 1080×1920 or 1080×2400:
   - Follow SCREENSHOTS_PLAN.md order.
   - Capture on a real device or emulator (Pixel 8 preferred). Use the screenshot mode (Settings → Developer options if needed). Stage the app with realistic seed data.
   - Add device-frame overlays + 1-line feature headlines using a free tool (e.g., Android Studio's built-in Studio Frame, or Figma). Each caption should read like DD §3 subsection summaries: "Pray without ceasing", "See every answered prayer", "Gratitude with photos", "Tradition-aware modes", etc.

4) store/screenshots/tablet/ — 2 PNGs at 1600×2560 (optional at launch but recommended).

5) Verify against Play Console guidelines
   - Min 320×320 icon; we're 512 — pass.
   - Feature graphic exactly 1024×500 PNG — required.
   - Screenshots at least 320px on short side — 1080 — pass.
   - No Play Console policy violations (no fake ratings, no misleading icons).

Verification:
- All files under store/ committed.
- Image dimensions verified: file store/*.png or `identify store/*.png` (ImageMagick) outputs match above.
- Upload to Play Console internal listing preview (don't publish yet) as a dry-run.
```

---

## Sprint M — Data Safety form + privacy policy hosting + consent

**Why:** LAUNCH_CHECKLIST.md §3 / §8 / §6 — required before Play can publish.
**On disk already:** privacy-policy.html, delete-account.html, DATA_SAFETY_GUIDE.md.
**Missing:** privacy policy hosted at a public URL, Data Safety form completed in Play Console, UMP/consent SDK for EEA.

### Prompt — Sprint M

```
Read privacy-policy.html, delete-account.html, store/DATA_SAFETY_GUIDE.md, store/LAUNCH_CHECKLIST.md, and CLAUDE.md.

Do the legal/policy hookup required for Play Store publishing.

1) Host the privacy policy
   - Verify Firebase Hosting is configured in firebase.json (it's an existing Firebase project). If not, add a hosting target that serves privacy-policy.html and delete-account.html from repo root.
   - Deploy: firebase deploy --only hosting.
   - Resulting URLs must be publicly reachable. Test with curl + browser.
   - Record the live URLs in store/LIVE_URLS.md: privacy policy, delete-account, (optional) marketing landing.

2) Google Play Data Safety
   - Transcribe store/DATA_SAFETY_GUIDE.md into a Play Console-ready checklist in store/DATA_SAFETY_FORM_FILLED.md — every question, every answer. This becomes the script for filling out the form.
   - Double-check: audio/photos collected? NO (on-device only). Device IDs? YES (AdMob). Purchase history? YES (Play Billing). App interactions? YES (anon analytics — if any).

3) Consent for ads (EEA/UK)
   - Integrate Google's UMP (User Messaging Platform) SDK via com.google.android.ump:user-messaging-platform.
   - On first launch in EEA/UK region, present the consent form before AdMob initialization. Cache the decision.
   - Do NOT block non-EEA users with the dialog.
   - Reference ScriptureQuest's UMP integration for the pattern.

4) Subscription paywall compliance (LAUNCH_CHECKLIST §8)
   - Audit ui/premium/PremiumPaywallScreen.kt: price must be clearly shown BEFORE the Subscribe CTA; "Cancel anytime" language must be visible; renewal terms disclosed.
   - Link to privacy policy and terms from the paywall.

Verification:
- Privacy policy URL resolves over HTTPS from a cold browser.
- store/DATA_SAFETY_FORM_FILLED.md exists and answers every question from the guide.
- UMP consent dialog shows on a test device with a VPN set to Ireland; does NOT show with US IP.
- Paywall passes the Play policy checklist items in §8.
- store/LAUNCH_CHECKLIST.md items 2, 3, 6, 8 now have concrete evidence under each checkbox.
```

---

## Sprint N — Internal testing track + closed testing

**Why:** LAUNCH_CHECKLIST.md §7 — internal testing on 3+ real devices before production.
**On disk already:** release-ready build (after Sprint K).
**Missing:** the actual upload, tester list, closed-testing invite loop.

### Prompt — Sprint N

```
Read store/LAUNCH_CHECKLIST.md and CLAUDE.md. This sprint is process, not code — you will produce checklists and run smoke tests, not ship features.

1) Upload first .aab to Play Console Internal Testing track.
   - Use the signed .aab from Sprint K.
   - Release notes: "PrayerQuest v1.0.0 — first internal build. See store/V1_TEST_CHECKLIST.md for what to smoke-test."
   - Add internal testers (Nathan + 2–4 trusted friends/family). Record emails in store/INTERNAL_TESTERS.md (gitignored if the list is sensitive — otherwise note generic tester role labels).

2) Walk through store/V1_TEST_CHECKLIST.md on each test device. Capture results in store/INTERNAL_TEST_RESULTS.md grouped by tester × section. File bugs inline as GitHub issues OR in a store/BUGS_FOUND.md file.

3) Triage
   - P0 (release-blocker): Quiet Hours violated, crash, data loss, paywall broken, ad shown to premium user, Crisis Prayer awards XP.
   - P1 (fix before promote to Closed Testing): broken nav, missing string, misaligned UI on common devices.
   - P2 (acceptable for v1.0): minor polish, non-English locale gaps.

4) Fix P0s immediately. Re-upload a new build (versionCode++). Repeat until internal testing is clean.

5) Promote to Closed Testing (50 testers max) once internal is clean. Extend the testing period 7 days minimum before going to Production.

Verification:
- store/INTERNAL_TEST_RESULTS.md shows all P0 items resolved.
- Play Console shows an Internal Testing build approved and active.
- Nathan has run the full V1_TEST_CHECKLIST on his personal device and signed off.
```

---

## Sprint O — Production release

**Why:** this is "ship it."

### Prompt — Sprint O

```
Read store/LAUNCH_CHECKLIST.md front-to-back. Every box must be checked. Read CLAUDE.md.

Walk the final release sequence.

1) Final pre-release audit (do not skip — this is the "we cannot unship" moment):
   - LAUNCH_CHECKLIST §1 Code & Build: all boxes ticked.
   - §2 Play Console listing: every asset uploaded.
   - §3 Data Safety: form submitted.
   - §4 Content Rating: IARC questionnaire completed, Expected: Everyone.
   - §5 Monetization: subscription product live, ID matches PremiumManager constant.
   - §6 Ads: real unit IDs live, UMP live for EEA.
   - §7 Testing: internal + closed testing clean.
   - §8 Policy-sensitive paywall items verified.
   - §9 Post-launch monitoring plan is set up (Nathan has Play Console + AdMob dashboard bookmarks).

2) Promote Closed Testing build to Production track. Use staged rollout:
   - Day 0: 10% rollout.
   - Day 2: if crash-free rate >99.5%, bump to 25%.
   - Day 4: to 50%.
   - Day 7: to 100%.

3) Publish store listing (if not already public).

4) Day-0 monitoring (first 48h per LAUNCH_CHECKLIST §9):
   - Check Play Console crashes/ANRs every 4h.
   - Check AdMob fill rate.
   - Check subscription events.
   - Respond to every review, especially 1★s, within 24h with a warm honest reply — NO canned responses.

5) File store/V1_POSTMORTEM.md after first week: what went well, what broke, what's first on the v1.1 backlog.

Verification:
- Play Console shows Production release LIVE.
- Crash-free rate at Day 2 >= 99.5%.
- V1_POSTMORTEM.md committed with the first week's lessons.
```

---

# POST-LAUNCH — v1.1 BACKLOG (Phase 4b per DD §8)

After v1.0 ships and stabilizes, tackle these in the order below.

---

## Sprint P — Fasting Tracker UI

**Why:** DD §3.11 — entity + DAO + repo already exist; only the UI is missing. Must include the medical disclaimer gating for fasts >24h.
**On disk already:** FastingSession entity, DAO, FastingRepository.
**Missing:** fasting flow UI, disclaimer gate, per-fast journal, post-fast reflection, badges.

### Prompt — Sprint P

```
Read design/prayer_quest_design_document_DD §3.11 carefully — especially the medical disclaimer requirements. Read CLAUDE.md. Confirm app/src/main/assets/legal/fasting_disclaimer.md exists (from Sprint A).

Build the Fasting Tracker.

1) ui/fasting/FastingHubScreen.kt — entry point from Profile or a new Home tile.
   - Three sections: "Start a fast" (primary CTA), "Active fast" (if any), "Past fasts" (chronological list with streak-like rings).

2) ui/fasting/StartFastScreen.kt
   - Pick fast type: Full / Partial / Daniel / Specific Food / Screen / Social Media / Custom.
   - Duration picker: hours or days.
   - Intention field (voice-to-text): "What are you praying about during this fast?"
   - Linked prayer items multi-select.
   - IF duration > 24 hours: full-screen medical disclaimer (render from assets/legal/fasting_disclaimer.md) with a REQUIRED checkbox "I have read and accept" before Start is enabled.

3) ui/fasting/ActiveFastScreen.kt
   - Live countdown timer.
   - "Pause and pray" button — logs a quick prayer session against the linked items.
   - Daily journal entry (voice-to-text primary).
   - Break Fast button → post-fast reflection screen.

4) ui/fasting/PostFastReflectionScreen.kt
   - Reflection prompt: what did God show you? Voice-to-text primary.
   - XP + badges (First Fast, 3-Day Warrior, Daniel Fast Completer, Faithful Faster per DD).

5) Update domain/model/Achievement.kt with fasting achievements. Wire into GamificationRepository.

Verification:
- Start a 12-hour fast → no disclaimer (under 24h).
- Start a 48-hour fast → disclaimer gates the Start button.
- Running fast shows countdown, persists across app restart.
- Break fast → reflection screen → achievements unlock.
- Build is green.
```

---

## Sprint Q — Memory & Legacy (Year in Review, Testimony Book, On This Day, Time Capsule)

**Why:** DD §3.12 — the emotional payoff features. Year in Review auto-launches Dec 15.
**On disk already:** PrayerRecord history, Gratitude history, Answered prayers. The `pdf` skill can help with PDF generation.
**Missing:** Year in Review multi-screen recap, Testimony Book PDF exporter, On This Day home card, Time Capsule flag on voice records.

### Prompt — Sprint Q

```
Read design/prayer_quest_design_document_DD §3.12 (all four subsections). Read CLAUDE.md.

Build the Memory & Legacy feature set. This is four related deliverables.

1) Year in Review (§3.12.1)
   - ui/legacy/YearInReviewScreen.kt — Spotify-Wrapped-style multi-screen reveal.
   - Slides: total minutes prayed, longest streak, top prayer categories, most-prayed person/topic, answered count, gratitudes logged, favorite mode, biggest moment.
   - Launches automatically Dec 15 via a new WorkManager one-shot; Profile has a "Your Year of Prayer" button visible after Dec 15.
   - All computation is offline against Room.

2) Testimony Book PDF (§3.12.3)
   - Invoke the `pdf` skill for this feature — read /sessions/wizardly-pensive-mayer/mnt/.claude/skills/pdf/SKILL.md first.
   - ui/legacy/ExportTestimonyBookScreen.kt — one-tap export.
   - PDF: one answered prayer per page. Date + testimony + embedded photo (if any) + a bottom Scripture verse.
   - Generate on-device (no cloud). Save to Downloads or sharesheet.
   - Uses app's font + color tokens for keepsake quality.

3) On This Day (§3.12.2)
   - Home screen card that surfaces 1–2x per week: "On this day one year ago, you prayed for [title]. She's now in your Answered list."
   - Sampling logic: pick a PrayerRecord or Answered prayer whose date is exactly -1 year, -2 years, etc. Cap display frequency with a preference key.

4) Time Capsule (§3.12.4)
   - Add `timeCapsule: Boolean` column to PrayerRecord. DB migration +1.
   - In Voice Record mode UI, add a "Seal as time capsule — replay in 1 year" checkbox.
   - WorkManager job scheduled for +365 days surfaces a notification: "A year ago today, you prayed this. Would you like to hear it again?"
   - Tapping the notification opens the playback UI.

Verification:
- Year in Review: override date to Dec 15 → launches automatically; slides animate; stats match real data.
- PDF export: a keepsake PDF opens cleanly in a PDF viewer with photos embedded.
- On This Day: backfill a 2024 record, set device date to +1y, card appears on Home.
- Time Capsule: seal a voice record, advance date, notification fires.
- Build is green.
```

---

## Sprint R — Prayer Partners

**Why:** DD §3.8 — lightweight 1-on-1 on the same Firestore base. Essentially a 2-person group with simplified UI.
**On disk already:** FirebaseAuthManager, FirestoreGroupService, Prayer Groups UI patterns.
**Missing:** Prayer Partners screens + Firestore data model subset + simpler UI.

### Prompt — Sprint R

```
Read design/prayer_quest_design_document_DD §3.8 (Prayer Partners). Read §3.7 for the Prayer Groups pattern this is built on. Read CLAUDE.md.

Implement Prayer Partners — 1-on-1 shared prayer.

1) Data model
   - Reuse /groups Firestore schema — a Partner IS a 2-person group with a flag `isPartner: true`, enforced in Firestore rules (2 members max).
   - Firestore rule update: if a doc has isPartner=true, memberCount is capped at 2.

2) UI
   - ui/partners/PrayerPartnersScreen.kt — list of partners.
   - ui/partners/InvitePartnerScreen.kt — share code "PAIR-XXXXXX" (reuse the invite-code generator; same pattern).
   - ui/partners/JoinPartnerScreen.kt.
   - ui/partners/PartnerDetailScreen.kt — shared prayer list (smaller, intimate UI vs the Groups list), "last prayed" timestamp for partner, "I prayed for your request" one-tap.

3) FCM
   - New Cloud Function: fan out a push when either partner prays for the other's item. Respect recipient quiet hours via the client (Firestore user doc stores quiet-hours preference — read it server-side).

4) Badges
   - Faithful Partner (30 days of mutual praying).
   - Lifted Up (partner prayed for you 100 times).

Verification:
- Pair two test accounts via invite code → both see a 2-member group flagged as Partner.
- Attempt to join a 3rd user → Firestore rule rejects.
- One prays → other receives FCM push (honoring quiet hours).
- Badges unlock at thresholds.
- Build is green.
```

---

## Sprint S — v1.1 release

Ship Phase P + Q + R as a single v1.1 update via the same staged-rollout dance as v1.0.

### Prompt — Sprint S

```
Read store/V1_POSTMORTEM.md and store/LAUNCH_CHECKLIST.md. Read CLAUDE.md.

Promote v1.1 to production.

1) Bump versionCode and versionName (e.g., 1.1.0).
2) Re-run the V1_TEST_CHECKLIST plus a Sprint-P/Q/R specific addendum.
3) Run the same Internal → Closed → Production staged rollout as v1.0.
4) Update store listing "What's New" copy and (optionally) add a new screenshot or two for the Memory & Legacy features (Year in Review teaser, Testimony Book cover).

Verification:
- 1.1.0 live on Play.
- Crash-free rate post-launch >= 99.5%.
- store/V1_1_POSTMORTEM.md committed.
```

---

# Ongoing streams (not sprint-shaped)

Things that won't be their own sprint but need a home:

- **Translation work** — once Sprint I's keys are in place, hand `values-es/`, `values-ko/`, `values-pt/` to translators. A rolling stream, not blocking of v1.0 (which ships English).
- **Reviews loop** — respond to 1★s and 5★s alike within 24h. Use response templates but personalize.
- **Future Updates backlog** — DD §9 has 29 deferred features. Revisit priority every 90 days based on user feedback.

---

# How to use this roadmap

1. Run Sprints A → O in order to ship v1.0.
2. Post-launch stabilization period (~30 days of monitoring, reviews, small bug-fix releases).
3. Then Sprints P → S for v1.1.
4. Each prompt is self-contained — paste into a fresh Claude session with no prior context and it'll work, because every prompt re-reads the DD and CLAUDE.md.
5. After each sprint, pause and provide the short-form summary (what was built, files touched, feedback request) — the DD + CLAUDE.md both call for this.

Sources: `design/prayer_quest_design_document_DD`, `CLAUDE.md`, `store/LAUNCH_CHECKLIST.md`, `SPRINT_3_TEST_CHECKLIST.md`, direct file survey of `app/src/main/` on 2026-04-17.
