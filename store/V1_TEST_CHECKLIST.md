# PrayerQuest — v1.0 Pre-Launch Smoke Test Checklist

This is the definitive pre-launch manual test for PrayerQuest v1.0. It supersedes `SPRINT_3_TEST_CHECKLIST.md`, covers the 10 MVP prayer modes (per DD §3.1.2 and `CLAUDE.md`), and adds checklists for every feature landed in Sprints A–I of `design/PrayerQuest_Roadmap_To_Release.md`.

**How to use:** walk each section on a fresh install AND after normal use. Check items off as you verify them. Anything unchecked becomes a bug in `design/V1_SMOKE_RESULTS.md` (or a follow-up issue), tagged P0/P1/P2 per the triage rules at the bottom. Do not promote a build to Closed Testing with any P0 open.

**Device matrix target:** one phone running Android 13+, one phone running Android 14, one tablet (optional). Emulator is acceptable for a first pass; the final pass before promotion must run on at least one real device.

---

## 0. Release gating (do this first)

Before running any functional section below, confirm:

- [ ] The build under test is a **release-variant** `.aab` (or a release-signed APK), not `debug`. (Verify via `./gradlew :app:bundleRelease` output or Play Console upload page.)
- [ ] `versionCode` and `versionName` bumped vs. the previous release. (`app/build.gradle.kts`)
- [ ] Every unchecked item in `store/LAUNCH_CHECKLIST.md` §1 Code & Build is closed.
- [ ] `store/LAUNCH_CHECKLIST.md` §2 Play Console listing assets uploaded (title, short/full description, icon 512×512, feature graphic 1024×500, ≥8 phone screenshots).
- [ ] `store/LAUNCH_CHECKLIST.md` §3 Data Safety form submitted using `store/DATA_SAFETY_GUIDE.md` as the source of truth.
- [ ] `store/LAUNCH_CHECKLIST.md` §4 Content Rating (IARC) answered; expected rating: Everyone.
- [ ] `store/LAUNCH_CHECKLIST.md` §5 Monetization: `prayerquest_premium_monthly` subscription live in Play Console; product ID string matches the constant in `PremiumManager.kt`.
- [ ] `store/LAUNCH_CHECKLIST.md` §6 Ads: real AdMob unit IDs wired into the release BuildConfig (no `ca-app-pub-3940256099942544/…` test IDs in release). AdMob app ID in `AndroidManifest.xml`. UMP consent SDK configured for EEA/UK.
- [ ] `store/LAUNCH_CHECKLIST.md` §7 Internal testing: at least one full run of this checklist on Internal Testing track by Nathan + 2 testers.
- [ ] `store/LAUNCH_CHECKLIST.md` §8 Policy-sensitive paywall: subscription price shown *before* the Subscribe CTA; cancel-anytime text visible; no medical/health claims in listing copy.
- [ ] `store/LAUNCH_CHECKLIST.md` §9 Post-launch monitoring bookmarks ready (Play Console crash/ANR dashboard, AdMob fill-rate dashboard, subscriptions dashboard).

Stop and fix any of the above before continuing. A green functional pass on an unsigned debug build is not a release.

---

## 1. First Launch & Core Infrastructure

- [ ] App opens to the Home screen without crashing.
- [ ] Famous Prayers are seeded on first launch (Library → Famous Prayers shows 30+ entries).
- [ ] Bible Prayers are seeded on first launch (prayed-as-prayer passages show in Library).
- [ ] Suggested collection packs appear in the Library: Foundational Prayers, Prayers for Peace, Prayers for Family, Prayers of Gratitude (plus the seasonal packs — see §7).
- [ ] `UserStats` and `StreakData` singletons seed cleanly (Profile shows level 1, 0 XP, 0-day streak, hearts/freezes at their defaults).
- [ ] Bottom navigation shows the expected tabs: **Home · Pray · Library · Profile · Settings** (or the current nav set — verify against `ui/navigation/Destinations.kt`).
- [ ] Tapping each bottom tab switches screens and the selected tab is highlighted.

## 2. Onboarding (DD §3.2)

Walk the full 7-step flow on a brand-new install:

- [ ] Step 1 — Welcome & vision animation renders.
- [ ] Step 2 — name + daily prayer-minutes goal (3/5/10/15/custom) + daily gratitude count (1/3/5) save correctly and persist after restart.
- [ ] Step 3 — tradition multi-select (Protestant, Catholic, Orthodox, Anglican, Non-denominational) pre-enables the matching modes in the Mode Picker.
- [ ] Step 4 — liturgical calendar question accepts NONE / Western / Eastern. NONE hides the Home indicator; Western/Eastern surface it (see §7).
- [ ] Step 5 — up to 3 reminder times + quiet-hours window save; reminders schedule immediately via WorkManager.
- [ ] Step 6 — "Foundational Prayers" collection auto-seeds; user can add 2–3 items with voice-to-text.
- [ ] Step 7 — first gratitude saves, Gratitude Starter (day 1/7) badge unlocks, celebration animation plays.
- [ ] "Skip setup, I'll configure later" on any step lands on Home with sensible defaults and does not crash.
- [ ] After completion, streak = 1, first XP awarded, Home renders with quests.

## 3. Home Dashboard

- [ ] Streak flame displays (0 days on fresh install, increments after first completed session).
- [ ] Hearts indicator visible and reflects `StreakData.hearts` (not `UserStats`).
- [ ] Freezes indicator visible if ≥ 1.
- [ ] Level card shows current level label ("Level 1", "Level 2", …) + XP progress bar. Leveling is uncapped — no named tiers.
- [ ] "Start Prayer" CTA navigates to the Mode Picker with a back arrow (modal).
- [ ] "In distress? Crisis Prayer →" subtle low-profile link appears beneath the Start Prayer CTA (Sprint F / DD §3.10).
- [ ] "Log Today's Gratitude" quick button navigates to the Gratitude Log screen.
- [ ] "Prayer Groups" tile navigates to the Groups list (or a sign-in prompt if not yet signed in — see §9).
- [ ] Daily quests card renders with 3 quest slots.
- [ ] Prayer-minutes strip reflects totals after sessions.
- [ ] Liturgical day indicator appears if the user opted into a calendar (see §7).

## 4. Pray Tab — Netflix-Shelf Mode Picker (DD §3.1.3)

- [ ] Tapping the **Pray** bottom tab goes directly to the Mode Picker with no intermediate screen and no back arrow (root behaviour).
- [ ] Reaching the picker from Home's "Start Prayer" button shows a back arrow (modal behaviour).
- [ ] Modes are organised into horizontal shelves (Quick / Guided / Expressive / Traditional per DD §3.1.3).
- [ ] Each card shows: icon, mode name, estimated duration, "last used" timestamp.
- [ ] Shelves for traditions the user opted out of collapse under an "Explore more traditions" footer.
- [ ] Individual per-mode toggles in Settings hide cards correctly.
- [ ] Tapping a mode card starts a prayer session in that mode.
- [ ] Back arrow (when present) returns to the origin screen.

## 5. Prayer Session — All 10 MVP Modes

Run a full session in each mode from the Pray tab. Verify the mode-specific behaviour and that the session ends in a Session Summary (see §5.11).

- [ ] **Guided ACTS Walkthrough** — Adoration → Confession → Thanksgiving → Supplication, tap-to-advance prompts, optional TTS voice prompts work.
- [ ] **Voice Record & Reflect** — mic button, live SpeechRecognizer transcription appears, transcript editable before completion, "Skip scoring" escape hatch works.
- [ ] **Prayer Journal** — free-type text area; voice-to-text button produces a formatted recap; optional linking to specific prayer items works.
- [ ] **Intercession Drill** — pulls active list items into a tap-bank; 30-sec timer + quick thanks/confession chips run per item; voice-to-text per item works.
- [ ] **Flash-Pray Swipe** — swipeable cards from the user's actives / group items / famous prayers; swipe (any direction) logs card as prayed-for; persistent "Close Prayer" button ends the session at any time; completion screen appears after all cards.
- [ ] **Daily Examen** — classic Ignatian 5-step (Gratitude → Review → Notice emotions → Confess → Resolve), each with a gentle prompt + optional voice-to-text; step 1 auto-writes a Gratitude entry.
- [ ] **Lectio Divina** — Lectio → Meditatio → Oratio → Contemplatio runs against a passage from the bundled `LectioPassageLibrary` (50+ entries); user-pasted passage also supported.
- [ ] **Breath Prayer** — paced inhale/exhale animation, default Jesus Prayer cadence, ~3-minute default, library includes 10+ classical breath prayers.
- [ ] **Prayer Beads / Rosary** — decade tracker; bead fill animation; haptic tap each bead; Joyful / Sorrowful / Glorious / Luminous mysteries all selectable; Anglican Rope (33) and Jesus Prayer Rope (100-knot) selectable; "beads of your own" custom mode works.
- [ ] **Daily Office** — auto-detects time of day (Lauds / Sext / Vespers / Compline); runs the ecumenical BCP-derived liturgy; tradition selection from onboarding (Anglican / Catholic / Simple Ecumenical) adapts content.

### 5.11 Session Completion

- [ ] Ending any mode takes the user directly to the **Session Summary** screen — no blank screen with only a back arrow.
- [ ] Single-mode sessions skip the 4-button grade row and go straight to the summary (Sprint 3 hot-fix regression guard).
- [ ] Summary shows XP earned, streak info, level-up banner (if any), new achievements (if any), and encouragement copy ("Keep seeking Him!" / "God hears you!" / "He is faithful!").
- [ ] "Done" on the summary returns to the origin screen (Home → Home; Pray tab → Pray; Collection Detail → that Collection Detail).

## 6. Prayer Library (DD §3.5.3, Sprint B)

The Library is *unified* — Famous Prayers, Bible Prayers, Names of God, and user favourites live under filter chips on one tab.

- [ ] Library tab shows filter chips: **Famous Prayers · Bible Prayers · Names of God · My Favourites** (or the current chip set).
- [ ] **Famous Prayers** — 30+ cards render; search + author/category filter work; tapping a card opens the detail screen (full text, author, source); "I prayed this today" button logs a record and increments the usage counter; counter persists across restart.
- [ ] **Bible Prayers** — bundled prayer passages render; tap → detail; "Pray this passage" launches Lectio Divina with the passage pre-loaded.
- [ ] **Names of God tab** (Sprint B, DD §3.5.3):
  - [ ] Tab is present in the filter chips.
  - [ ] 30+ cards render (Hebrew / Greek / English / Other language grouping).
  - [ ] Each card shows name, transliteration, one-line meaning.
  - [ ] Tap → `NameOfGodDetailScreen` shows Loading briefly then full card: hero name, transliteration, meaning, Scripture reference + verse text, usage-count chip.
  - [ ] **"Pray this name — Guided ACTS"** button launches Guided ACTS pre-focused on that attribute; usage count increments.
  - [ ] **"Breath Prayer for this name"** button launches Breath Prayer with the name as the centering phrase; usage count increments.
  - [ ] Fresh install: importer runs once, no duplicates on subsequent launches.
  - [ ] Praying a name unlocks the `NAMES_OF_GOD_FIRST` achievement; `NAMES_OF_GOD_ALL` unlocks after praying all 30; `NAMES_OF_GOD_50` at 50 total.
- [ ] Users can add their own favourite prayer to the Library and earn the same usage badges.

## 7. Liturgical Calendar (DD §3.5.4, Sprint D)

Run against a build where the user opted into a calendar in onboarding.

- [ ] Home shows a subtle liturgical-day card just under the streak row: day name on one line (e.g., "Second Sunday of Advent"), season on the next, subtle stained-glass accent.
- [ ] Tapping the card opens the Library filtered to the relevant season's pack if one exists (Advent / Lent / Holy Week).
- [ ] Setting device date to **2026-12-06** (Second Sunday of Advent, Western) → card shows that label; Advent pack pinned to the top of the Library Collections shelf.
- [ ] Setting device date to **2026-02-18** (Ash Wednesday, Western) → card shows that label; Lent pack pinned.
- [ ] Setting device date to **Palm Sunday 2026** → card shows that label; Holy Week pack pinned.
- [ ] Setting device date to a date deep in Ordinary Time → no pack is pinned; card still shows the season ("Ordinary Time").
- [ ] Switching to **Eastern Orthodox** calendar in Settings and choosing a date during Orthodox Lent surfaces the correct Eastern label.
- [ ] Disabling the calendar in Settings → Home indicator disappears, no regression for non-liturgical users.
- [ ] Seasonal packs remain browsable in the Library outside their seasons but are no longer pinned.
- [ ] Unit tests pass: `./gradlew :app:testDebugUnitTest` includes `LiturgicalCalendarTest` green (≥ 30 dated cases spanning 2024–2028).

## 8. Answered Prayers + Big Celebration Moment (DD §3.5.2, Sprint E)

- [ ] From Home / Library / Collection Detail, an active prayer can be marked **Answered** or **Partially Answered**.
- [ ] **Marking Answered** triggers the full-screen **AnsweredCelebrationScreen**:
  - [ ] Confetti particle burst animates over ~3 seconds then falls gently.
  - [ ] "Praise God!" hero text scale-ins with a spring animation.
  - [ ] A Scripture verse card renders on-the-fly (picked from `CelebrationVerses.kt`), using warm gradient + app theme colours.
  - [ ] **"Share testimony"** CTA opens the system share sheet with a rendered bitmap (verse card + prayer title).
  - [ ] **"Close"** returns to the Answered Prayer detail screen.
- [ ] **Marking Partially Answered** does NOT trigger the celebration (regression guard).
- [ ] A prayer imported as already-answered does not trigger celebration.
- [ ] Answered Prayers archive lists Answered + Partially Answered prayers in reverse-chronological order by answer date.
- [ ] Detail screen shows testimony text, date answered, optional photo/voice note.
- [ ] Timeline view renders ("God's faithfulness over time").
- [ ] **1-year anniversary reminder**: `AnsweredPrayerAnniversaryWorker` is scheduled on mark-answered with unique work id `anniversary-<prayerItemId>`; set device clock +365d → notification fires "A year ago today, <title> was answered. God is faithful." Tap → Answered Prayer detail.
- [ ] Anniversary worker respects Quiet Hours (does NOT fire inside quiet-hours window — if it would, it reschedules).
- [ ] Achievements `FIRST_ANSWERED` / `FAITHFUL_10` / `FAITHFUL_50` unlock at thresholds.
- [ ] `Exportable Testimony Book` button does NOT appear in v1.0 Profile (deferred to v1.1, DD §3.12.3).

## 9. Gratitude Section (DD §3.6, Sprint G)

### 9.1 Daily Gratitude Log

- [ ] Home "Log Gratitude" → Gratitude Log screen renders.
- [ ] Text field + voice-to-text button both work.
- [ ] Optional photo from camera or gallery attaches; image is compressed; stored in app-private storage; `photoUri` persisted.
- [ ] Category chips selectable (Family, Provision, Nature, Spiritual Growth, Health, Other).
- [ ] Optional faith-themed prompt suggestions load from `assets/gratitude/prompts.json`.
- [ ] Save returns to previous screen; the new entry appears in the catalogue.
- [ ] Logging awards XP (scaled: 1 = base, 3 = medium, 5 = high, photo bonus).
- [ ] Gratitude counts toward daily quests AND streak protection.

### 9.2 Speed Round (DD §3.6)

- [ ] Prominent "Speed Round — rapid fire gratitude" button appears near the top of the Gratitude Log screen.
- [ ] Full-screen Speed Round shows a 60-second countdown timer at top.
- [ ] Chip bank of ~30 quick-input prompts renders (from `prompts.json` plus curated speed subset).
- [ ] Tapping a chip writes a new `GratitudeEntry` with the chip text seeded.
- [ ] Hold-to-speak voice mic commits a new entry on each pause.
- [ ] Live counter ("5 thanks logged") updates.
- [ ] Completing ≥ 5 entries in under 60 seconds fires the speed bonus (toast "Grateful heart, fast hands!", extra XP).
- [ ] All entries write to the single `GratitudeEntry` table (no separate store).

### 9.3 Gratitude Catalogue (Sprint G)

- [ ] Chronological / grid view of all past gratitudes.
- [ ] Top collapsible section holds the **calendar heat-map** (53 × 7 grid; colour intensity by entry count that day); closed by default; tap a cell → catalogue filters to that date.
- [ ] **Keyword search** TextField at the top filters by text (debounced ~250ms, via `GratitudeRepository.searchEntries`).
- [ ] **"With photos only"** filter chip hides text-only entries.
- [ ] Category filter chips work.
- [ ] Edit an existing entry works (including adding a photo later).
- [ ] **Export as image card**: long-press an entry OR overflow menu → "Share as image card" renders a bitmap (gratitude text + optional curated thanksgiving verse overlay + date + subtle PrayerQuest watermark) and opens system share sheet.

### 9.4 Gratitude Badges

- [ ] `Gratitude Starter` (7 days) unlocks after 7 consecutive daily log entries.
- [ ] `Thankful Heart` (30 consecutive) unlocks after 30 days in a row.
- [ ] `Abundance Mindset` (100 total) unlocks at the 100th entry.
- [ ] `Photo Gratitude` (50 with photos) unlocks at the 50th photo-attached entry.
- [ ] `Faithful Thanksgiver` volume milestones fire as entries accumulate.

## 10. Auto-Tagging on Prayer-Item Creation (DD §3.5.1, Sprint C)

- [ ] Add Prayer Item flow shows up to 3 suggested tag chips above the save button after you type/speak title + description (debounced ~300ms).
- [ ] Typing "Healing for Mom's surgery" surfaces **Family** + **Health** suggestions.
- [ ] Typing "Promotion at work" surfaces **Work/Vocation**.
- [ ] Typing "Give me strength" surfaces **Spiritual Growth** with low confidence (below threshold ⇒ hidden if clearly off-topic).
- [ ] Tapping a chip toggles accept/dismiss; on save, the first accepted tag persists to `PrayerItem.category`; no accepted chip → null.
- [ ] Manual category picker (if shown) always wins over auto-suggested.
- [ ] Accessibility: each chip reads as "Suggested tag: <name>. Tap to apply."
- [ ] Unit tests pass: `./gradlew :app:testDebugUnitTest` includes `PrayerTaggerTest` green (Family, Health, ambiguous, empty, multi-category cases).

## 11. Photo Prayers for Active Items (DD §3.9, Sprint H)

- [ ] Add / Edit Prayer Item flow shows a circular photo slot with camera/gallery picker.
- [ ] Selected photo is compressed and stored in app-private storage; `PrayerItem.photoUri` persists.
- [ ] Photo renders as a circular avatar thumbnail:
  - [ ] Active list cards.
  - [ ] Intercession Drill tap-bank.
  - [ ] Flash-Pray swipe cards.
- [ ] Photo appears in the session-mode header of Guided ACTS, Voice Record, Prayer Journal when praying that specific item.
- [ ] Adding a photoed item to a **Prayer Group** never uploads the `photoUri`. Inspect Firestore writes for group item doc — `photoUri` field is absent.
- [ ] Deleting a `PrayerItem` deletes its local photo file from disk.
- [ ] Free-tier soft cap at 200 photos (across prayer items + gratitude + testimony) surfaces the premium paywall when exceeded; premium tier removes the cap.
- [ ] DB migration: install the pre-v10 build, upgrade to v10 (adds `photoUri` to `prayer_items`) — no data loss, column exists.

## 12. Crisis Prayer Mode (DD §3.10, Sprint F — **no XP!**)

- [ ] Subtle "In distress? Crisis Prayer →" link under Home's primary CTA launches the Crisis Prayer screen.
- [ ] Screen opens with a calming fade-in; header reads faithful-not-saccharine copy ("You are not alone. God is with you." or equivalent).
- [ ] Three tiles render:
  - [ ] **Psalms that breathe with you** — paged Psalm reader (Ps 23, 27:1-5, 46:1-3+10, 91:1-6, 121, 139:7-12, 42:1-5, 56:3-4 from `CrisisPsalmsLibrary`). Swipe to advance. Slow auto-advance is OFF by default.
  - [ ] **Paced breath prayer** — launches a restricted Breath Prayer variant with 4-in / 6-out cadence, 3-minute default.
  - [ ] **Crisis resources** — `CrisisResourcesScreen` lists 988 Lifeline (US), Samaritans (UK), Lifeline (AU) with phone numbers and tap-to-call intent. Copy makes NO confidentiality claim and NO safety-assessment gate.
- [ ] Subtle "Return home" link at bottom.
- [ ] **Zero-XP guard (critical)**: complete a full Crisis breath session → `UserStats.totalXp` is unchanged; streak is unchanged; no daily quest progresses. Verify via Profile before/after screenshots AND via the `CrisisModeTest` unit test (`./gradlew :app:testDebugUnitTest`).
- [ ] TalkBack reads the screen coherently; tap targets ≥ 48dp; contrast passes 4.5:1.
- [ ] All content is bundled; airplane-mode run works.

## 13. Prayer Groups (DD §3.7)

Prayer Groups are **Firebase-backed** (Firestore + Google Sign-In + Cloud Functions + FCM) per `CLAUDE.md`, *not* the "offline MVP" wording that may still appear in older DD drafts.

- [ ] Home "Prayer Groups" tile shows a sign-in prompt when signed out — **never** during onboarding, never as a blocking step.
- [ ] Tapping "Sign in with Google" opens the Google Sign-In flow; successful sign-in opens the Groups list.
- [ ] **Create Group** — name/description/emoji + generates a unique invite code like `PRAY-XXXXXX`; group appears in the list immediately via Firestore snapshot.
- [ ] **Join Group** — entering a valid code joins successfully; invalid code shows an error; a revoked / expired code is rejected cleanly.
- [ ] Group Detail screen lists shared prayer items and members; updates in real time via Firestore snapshot listener (no manual refresh button / action).
- [ ] Adding a prayer item syncs to all other members; they receive an FCM push ("Sarah added a new request: …"), respecting **their** quiet hours.
- [ ] "I prayed for this" one-tap increments `prayedByCount`; activity-feed entry appears.
- [ ] Praying a group item via any prayer mode logs to the group's activity.
- [ ] `photoUri` on a local prayer item is NEVER uploaded to Firestore (verify in the group-item writes path — regression guard from §11).
- [ ] Member display name (chosen at join time) is what other members see — not the Google account email.
- [ ] Leave Group works for members; Admin can remove members + moderate; solo features keep working after leaving.
- [ ] Sign-out via Settings → Account detaches Firestore listeners; Groups tab reverts to a sign-in prompt; all solo features continue working as before.
- [ ] Offline behaviour: airplane-mode a signed-in device → Groups list renders from Firestore cache; "I prayed for this" queues and syncs on reconnect.
- [ ] **Delete Account** (Settings → Account) removes user Firestore data, revokes Google sign-in, deletes the Firebase Auth account. Safety-net `onUserDeleted` Cloud Function cleans up leftovers.
- [ ] Prayer Group badges: `Group Prayer Warrior` (first group joined), `Faithful Intercessor` (50 group requests prayed), `Community Builder` (create 5 groups), `United in Prayer` (volume milestone) all unlock at thresholds.

## 14. Profile

- [ ] Level header + XP progress bar.
- [ ] Daily goal picker (minutes + optional gratitude count) saves and persists.
- [ ] Theme toggle (Light / Dark / System) applies immediately; dark mode is fully supported on every screen.
- [ ] Tradition selector editable post-onboarding.
- [ ] Lifetime stats grid includes: total minutes, sessions, gratitudes logged, answered prayers, group activity summary, total fasts completed (0 / hidden for v1.0).
- [ ] Achievement grid renders all categories (streaks, minutes, mode diversity, famous-prayer milestones, answered, gratitude, Names of God, group, Crisis is NOT counted) with locked/unlocked states.
- [ ] "The Science Behind PrayerQuest" expandable card opens.
- [ ] "Your Year of Prayer" button is HIDDEN before Dec 15 (v1.1 feature; expected absent in v1.0).
- [ ] "Export Testimony Book" button is HIDDEN (v1.1 feature).

## 15. Settings

- [ ] Tradition section, Daily goal, Quiet Hours window, Reminder times (up to 3/day) and per-time-of-day personalities, Calendar preference, Per-mode on/off toggles all persist after restart.
- [ ] Theme toggle applies globally.
- [ ] Changing tradition filters the Mode Picker shelves (opted-out modes collapse under "Explore more traditions").
- [ ] Privacy Policy / Delete Account links open the hosted URLs (`privacy-policy.html` / `delete-account.html`).
- [ ] Notification toggles: Daily Prayer Reminder, Streak Alert, Quest Notification, Daily Gratitude Prompt, Group Activity — each independently on/off.

## 16. Gamification Hot Path

After each prayer / gratitude / group action, verify:

- [ ] XP total increases on the summary AND on Profile.
- [ ] Streak advances by 1 on the first session of the day; stays flat on subsequent sessions the same day.
- [ ] A missed day consumes a freeze / heart per DD streak-protection logic (freezes first, then hearts).
- [ ] Daily quests progress only when their criteria are met.
- [ ] Full-quest-set bonus fires when all 3 daily quests complete.
- [ ] Level-up banner appears on the summary screen the moment a threshold is crossed.
- [ ] Gratitude logging, Examen completion, and Group prayer activity all feed the same `GamificationRepository.onPrayerSessionCompleted()` hot path (per CLAUDE.md).
- [ ] Crisis Prayer does NOT feed the hot path (see §12).

## 17. Notifications & Quiet Hours (DD §3.13)

Quiet Hours violations are P0.

- [ ] Settings → Notifications toggles each notification type independently.
- [ ] Up to 3 daily reminder times save and fire at the chosen times (test with a near-future time).
- [ ] Each reminder uses the selected personality copy.
- [ ] `StreakAlertWorker` fires late afternoon on a day with no session — gentle tone, no guilt.
- [ ] `QuestNotificationWorker` fires when a daily quest is nearly complete.
- [ ] `GratitudePromptWorker` fires at the user-chosen gratitude time.
- [ ] `AnsweredPrayerAnniversaryWorker` respects Quiet Hours.
- [ ] FCM Group push (new request added) respects the recipient's Quiet Hours window.
- [ ] Schedule a reminder for a time inside the Quiet Hours window → it does NOT fire.
- [ ] Android 13+: POST_NOTIFICATIONS runtime permission is requested on first notification setup; a denied permission does not crash the app.

## 18. Localization (DD §3.15, Sprint I)

- [ ] `res/values/strings.xml` contains all Home / Library / Gratitude / Crisis / Onboarding / Prayer modes / Notifications / Settings / Celebration keys (no hardcoded Compose literals).
- [ ] `res/values-es/strings.xml`, `values-ko/strings.xml`, `values-pt/strings.xml` exist with matching key sets (English fallbacks acceptable for v1.0 translation stubs).
- [ ] `res/xml/locales_config.xml` lists en, es, ko, pt; `AndroidManifest.xml` references it via `android:localeConfig`.
- [ ] `android:supportsRtl="true"` is set in `AndroidManifest.xml`.
- [ ] Switch device language to **Spanish** → app renders, no crashes, English fallback text visible where translation is pending.
- [ ] Switch device language to **Korean** → same — no crash, fallback English visible.
- [ ] Switch device language to **Portuguese** → same.
- [ ] `./gradlew :app:lint` reports no `MissingTranslation` errors for `en`; `values-es/ko/pt` warnings are acceptable.
- [ ] `scripts/check_no_hardcoded_strings.sh` passes (no new Compose `Text(...)` literals introduced).
- [ ] `design/LOCALIZATION_AUDIT.md` exists and enumerates the externalisation sweep.

## 19. Persistence & Offline

- [ ] Airplane mode → every solo feature works (100% offline): prayer modes, gamification, gratitude, answered prayers, library, crisis mode, onboarding, fasting (hidden v1.0).
- [ ] Kill and relaunch the app → all data restored (prayers, sessions, gratitudes, groups cache, streak, hearts/freezes, achievements, settings).
- [ ] Orientation change during any prayer session does not drop state.
- [ ] Background → foreground mid-session resumes cleanly.
- [ ] Signed-out user never transmits identity to Firebase for any solo feature.
- [ ] Signed-in user in airplane mode can open Groups, view cached items, tap "I prayed" (queued), and the queued writes sync on reconnect.

## 20. Ads & Premium (DD §6)

- [ ] Release build serves real AdMob ad units (NOT the `ca-app-pub-3940256099942544/*` Google test IDs). Verify via BuildConfig fail-safe in `AdManager`.
- [ ] Banner / interstitial / rewarded ads load on a free-tier test device.
- [ ] **Premium users see zero ads.** Verify by toggling a test account into premium and walking every screen.
- [ ] Paywall shows the subscription price clearly BEFORE the Subscribe CTA.
- [ ] "Cancel anytime" copy visible on paywall.
- [ ] Renewal terms disclosed; privacy policy and terms linked.
- [ ] UMP consent dialog shows for a device with EEA/UK IP; does NOT show for US IP.
- [ ] Purchase flow: subscribe → premium unlocks immediately → restart → still premium. Restore Purchase after reinstall works.

## 21. Edge Cases

- [ ] Start a prayer session with **zero active prayer items** — mode runs with synthetic/default content, no crash.
- [ ] Swipe through Flash-Pray without marking any as prayed → summary still shows (prayed count = 0).
- [ ] Create a collection with no items and try "Start Praying" → gracefully falls back (either informs the user or uses default content).
- [ ] Log gratitude without a photo → saves fine.
- [ ] Very long prayer titles / journal entries don't break the UI.
- [ ] Very long gratitude text exports cleanly as an image card.
- [ ] Create a prayer item with an emoji-only title — renders in all card surfaces.
- [ ] A device with TTS / SpeechRecognizer disabled shows a graceful fallback in every voice-first mode.
- [ ] Launching the app while `liturgical calendar = Eastern` on an Orthodox-only feast day resolves to a valid `LiturgicalDay` (no null pointer).

## 22. Collections (Sprint 3 regression guards retained)

- [ ] "New Collection" → name/emoji/description persists and appears in the list.
- [ ] Collection Detail shows items, item count, description.
- [ ] "Add Items" flow attaches existing prayer items to the collection.
- [ ] "Start Praying" from a collection opens the Mode Picker **with collection context**.
- [ ] The session uses the collection's **actual items** — not placeholders ("Praying for:" banner lists real titles; Flash-Pray cards show those titles).
- [ ] After a collection session, Back/Done returns to the Collection Detail screen.

## 23. Pray Tab Root Behaviour (Sprint 3 regression guards retained)

- [ ] **Pray bottom tab is a root** (no back arrow).
- [ ] **Home → Start Prayer** shows a back arrow on the picker (modal).
- [ ] **Flash-Pray summary appears** after swiping through all cards (no blank screen).
- [ ] **Single-mode session → summary directly** (grade bar skipped).
- [ ] **Back-nav from summary** lands on the origin screen (Home → Home, Collection → Collection Detail), not on the Mode Picker.

---

## Triage Rules

When filing failures into `design/V1_SMOKE_RESULTS.md`, tag each issue:

- **P0** (release-blocker): crash, data loss, Quiet Hours violated, paywall broken, ad shown to premium, Crisis Prayer awards XP, Firestore writes `photoUri` for group items, sign-out doesn't detach listeners, any solo feature requires network.
- **P1** (fix before promoting to Closed Testing): broken navigation, missing strings in `values/`, misaligned UI on common devices, achievement not unlocking at the right threshold, share-image renders blank.
- **P2** (acceptable for v1.0): polish, non-English locale gaps, minor animation timing, tablet-only layout quirks.

Fix all P0s, re-upload with `versionCode++`, and re-run this checklist before promoting.

## Deprecated — do not test, do not resurrect

Three prayer modes were cut during the refactor-to-DD sprints and must NOT appear anywhere in the shipped build. The authoritative list is in `CLAUDE.md` under "Deprecated / do not resurrect"; two of the three are captured for a future release in DD §9.1 and §9.2; the third was absorbed into the Gratitude Section as Speed Round (see §9.2 of this checklist).

If any of those three mode names appears in a shipping build (UI, Compose literal, mode picker, enum, asset filename, notification copy), that's a P0 regression and the build must be pulled. The guardrail `grep` command for this check lives in `design/PrayerQuest_Roadmap_To_Release.md` under Sprint J's verification block.

---

**Sources:** `CLAUDE.md`, `design/prayer_quest_design_document_DD.txt`, `design/PrayerQuest_Roadmap_To_Release.md`, `store/LAUNCH_CHECKLIST.md`, `SPRINT_3_TEST_CHECKLIST.md` (superseded by this document).
