# PrayerQuest — Project Instructions for Claude

Condensed working context for any Claude session operating on this codebase. The authoritative spec is the Design Document at `design/prayer_quest_design_document_DD` (referred to as "DD"). Read that first for any non-trivial work.

## Stack
- Kotlin 2.2.10, Jetpack Compose (Material 3), Room 2.7.1 + KSP, WorkManager, DataStore, SpeechRecognizer/TTS, AdMob, Google Play Billing.
- Firebase (BOM 34.12.0): Firestore, Auth, Cloud Messaging, Cloud Functions. Used for Prayer Groups + Prayer Partners only; all solo features are 100% offline.
- minSdk 24, compileSdk 36, targetSdk 36, JVM 17.
- Base package: `com.prayerquest.app`
- Sibling reference codebase: `../ScriptureQuest/` — adapt patterns, don't reinvent.

## Architectural decisions of record
- **Manual DI via `AppContainer`** (no Hilt). ViewModels use companion `Factory` classes.
- **Repository pattern** with `Flow`-returning queries. ViewModels combine flows into atomic UiState.
- **StreakData owns `hearts` and `freezes`** — not UserStats. Streak-protection logic reads/writes from StreakData. (Decision of 2026-04-16.)
- **Prayer Groups use real-time Firestore snapshot listeners**, not manual refresh. Repository owns listener lifecycle. Firestore's offline cache is the offline story. (Decision of 2026-04-16.)
- **Singleton rows** for UserStats (id=1) and StreakData (id=1), seeded via INSERT OR IGNORE in the Room SeedCallback.
- **Idempotent imports**: Famous Prayers / Names of God / Devotional / Suggested-pack importers are safe to re-run.
- **GamificationRepository.onPrayerSessionCompleted()** is the hot path. Gratitude logging, Examen completion, and Group prayer activity all feed the same path.

## PrayerMode enum (10 MVP modes)
Canonical values — do not add others without updating the DD:
- `GUIDED_ACTS`
- `VOICE_RECORD`
- `PRAYER_JOURNAL`
- `INTERCESSION_DRILL`
- `FLASH_PRAY_SWIPE`
- `DAILY_EXAMEN`
- `LECTIO_DIVINA`
- `BREATH_PRAYER`
- `PRAYER_BEADS`
- `DAILY_OFFICE`

## Deprecated / do not resurrect
- `ScriptureSoakMode` — moved to Future Update §9.1 of DD
- `ContemplativeSilenceMode` — moved to Future Update §9.2 of DD
- `GratitudeBlastMode` — absorbed into Gratitude Section as "Speed Round"; no longer a standalone prayer mode

## Room schema
- Current DB version: **5** (bumped for StreakData hearts/freezes migration + NameOfGod + Devotional + FastingSession).
- All migrations are explicit in `PrayerQuestDatabase.kt`. No `fallbackToDestructiveMigration()` in shipped builds.
- Schema export is ON (`$projectDir/schemas`).

## Sprint status (see DD §8.1 for full plan)
- **Sprint 1** — Schema + cleanup: *in progress*
- **Sprint 2** — Firestore real-time refactor: pending
- **Sprint 3** — 5 new prayer modes + Netflix-shelf Mode Picker: pending
- **Sprint 4** — Onboarding + Settings + Notifications (Quiet Hours, personalities, Devotional worker): pending
- **Sprint 5** — Home + Library + Gratitude polish (Names of God tab, Speed Round, Crisis Mode, Big Celebration, auto-tagging): pending
- **Sprint 6** — Asset authoring (devotionals, names of god, breath prayers, rosary, daily office, disclaimer): pending
- **Sprint 7** (optional) — v1.1 groundwork stubs (Fasting UI, Testimony Book, Prayer Partners)

## Release phasing
- **v1.0 (Phase 4a)**: solo features, Gratitude, Library, Answered Prayers, Big Celebration, Crisis, Fasting, Daily Devotional, Liturgical Calendar, Notifications + Quiet Hours, 8-step Onboarding, localization strings externalized.
- **v1.1 (Phase 4b)**: Firebase Groups + Prayer Partners full rollout, Memory & Legacy (Year in Review, Testimony Book PDF, On This Day, Time Capsule).

## File conventions
- Entities: `data/entity/<Name>.kt`
- DAOs: `data/dao/<Name>Dao.kt`
- Repositories: `data/repository/<Name>Repository.kt`
- ViewModels: alongside their Compose screen, e.g. `ui/home/HomeViewModel.kt`
- Screens: `ui/screen/<Name>Screen.kt` for bottom-nav destinations; `ui/<feature>/<Name>Screen.kt` for feature sub-screens
- Prayer modes: `ui/prayer/modes/<ModeName>Mode.kt` — one composable per mode, accepting `PrayerSessionViewModel`
- Importers / loaders: `data/prayer/`
- Assets: `app/src/main/assets/<category>/*.json` (prayers, names, devotionals, packs, rosary, daily_office, gratitude, legal)

## Code standards
- Compose for all UI, StateFlow for state, sealed `UiState` interfaces.
- Full light/dark theme support — never regress dark mode.
- Accessibility: content descriptions, 4.5:1 contrast, screen-reader friendly, large-text mode respected.
- Voice-to-text is a first-class citizen in every text input.
- Localization: strings externalized via `strings.xml` from day one. Launch languages: English, Spanish, Korean, Portuguese.
- Photos: app-private storage, compressed, never uploaded by default.
- Error handling: loading states, empty states, graceful offline behaviour.
- Quiet Hours compliance: EVERY notification worker must check quiet hours before firing. Violations are P0.

## Tone / UX personality
Warm, prayerful, motivating — gentle spiritual companion, not a to-do list. Encouraging copy throughout ("Keep seeking Him!", "God hears you!", "He is faithful!"). Game-like without feeling childish. Traditional modes (Rosary, Daily Office) use subtle stained-glass accents, not kitsch.
