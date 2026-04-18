# PrayerQuest — Project Instructions for Claude

Condensed working context for any Claude session operating on this codebase. The authoritative spec is the Design Document at `design/prayer_quest_design_document_DD` (referred to as "DD"). Read that first for any non-trivial work.

## Stack
- Kotlin 2.2.10, Jetpack Compose (Material 3), Room 2.7.1 + KSP, WorkManager, DataStore, SpeechRecognizer/TTS, AdMob, Google Play Billing.
- Firebase (BOM 34.12.0): Firestore, **Auth via Google Sign-In**, Cloud Messaging, Cloud Functions. Used for Prayer Groups + Prayer Partners only; all solo features are 100% offline.
- minSdk 24, compileSdk 36, targetSdk 36, JVM 17.
- Base package: `com.prayerquest.app`
- Sibling reference codebase: `../ScriptureQuest/` — adapt patterns, don't reinvent.

## Offline-first + optional sign-in (architectural principle)
The entire app works 100% offline with no account. Prayer modes, gamification, gratitude, answered prayers, fasting, library, crisis mode, onboarding — all run against Room + WorkManager, no network, no sign-in required. **Prayer Groups and Prayer Partners are the only features that require the network, and they are strictly optional.** Signing in with Google (via `FirebaseAuthManager`) is deferred until the user actively tries to use Groups/Partners — never during onboarding, never as a blocking step. When signed in, the app pulls group data from Firestore and pushes local actions back up; when signed out, Group/Partner UI is replaced with a sign-in prompt and the rest of the app is unaffected. Never gate any solo feature behind sign-in.

## Architectural decisions of record
- **Manual DI via `AppContainer`** (no Hilt). ViewModels use companion `Factory` classes.
- **Repository pattern** with `Flow`-returning queries. ViewModels combine flows into atomic UiState.
- **StreakData owns `hearts` and `freezes`** — not UserStats. Streak-protection logic reads/writes from StreakData. (Decision of 2026-04-16.)
- **Prayer Groups use real-time Firestore snapshot listeners**, not manual refresh. Repository owns listener lifecycle. Firestore's offline cache is the offline story. (Decision of 2026-04-16.)
- **Prayer Groups/Partners are gated behind Google Sign-In** via `FirebaseAuthManager`. Signing in is optional and deferred — only prompted when the user opens the Groups/Partners tab. The rest of the app never prompts for sign-in. (Supersedes earlier DD language about anonymous auth; DD §3.7.1 updated 2026-04-18.)
- **Singleton rows** for UserStats (id=1) and StreakData (id=1), seeded via INSERT OR IGNORE in the Room SeedCallback.
- **Idempotent imports**: Famous Prayers / Names of God / Suggested-pack importers are safe to re-run.
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
- Current DB version: **10** (v9→v10 adds `photoUri` TEXT to `prayer_items` for Photo Prayers DD §3.9; v8→v9 drops the legacy `devotional` table; earlier bumps were for StreakData hearts/freezes + NameOfGod + FastingSession + onboarding/quest columns).
- All migrations are explicit in `PrayerQuestDatabase.kt`. No `fallbackToDestructiveMigration()` in shipped builds.
- Schema export is ON (`$projectDir/schemas`).

## Sprint workflow (commit every sprint AND every major bugfix)
Nathan is running one Claude conversation per sprint to finalize features + content for release. Every sprint ends with a git commit. Every meaningful bugfix between sprints ALSO ends with its own commit — don't let fixes pile up unattributed. Follow this loop:

1. Do the sprint work (or the bugfix work).
2. When the deliverables are complete, STOP and prompt Nathan to build and test:
   > "Sprint X is done. Please build the app in Android Studio and test the new behavior on your device/emulator. Reply **all clear** when you've verified it works, or tell me what's broken and I'll fix it."
   For a bugfix: "Bugfix for <thing> is ready. Please build and test, then reply **all clear** or tell me what's still broken."
3. If he replies "all clear" (or equivalent), commit everything to git **locally**.
4. If he reports issues, fix them, then re-prompt for build/test. Don't commit until he's green.

**Commit cadence:**
- One commit per sprint, title line `Sprint X: <short summary>` (use the letter, e.g. `Sprint I:`).
- One commit per major bugfix, title line `Fix: <short summary>`. "Major" = anything that touches behavior a user would notice, anything that was a P0/P1 bug, or any fix Nathan explicitly asked for.
- Trivial cleanups (typos, comment-only edits, formatting) can ride along in the next sprint commit.
- Match existing commit style (see `git log`). Always include the `Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>` trailer.

**Never push.** There is no `origin` configured here. Nathan pushes manually via PowerShell on his Windows box. Claude's job stops at a clean local commit — do not run `git push`, do not add a remote, do not suggest pushing as a next step. Just confirm the commit landed and stop.

## Sprint plan lives elsewhere
The authoritative sprint list — which sprint is next, what each sprint delivers, what's done — lives in `design/PrayerQuest_Roadmap_To_Release.md`. That document changes; this one doesn't. Read the Roadmap at the start of any session to learn the current sprint. Do NOT duplicate sprint status into this file.

## Release phases (philosophy, not schedule)
Work moves through three phases. The Roadmap document names the specific sprints in each:
- **Feature-build phase** — lettered sprints landing DD features. One sprint per Claude conversation, one commit per sprint.
- **Polish + bugfix phase** — after feature sprints are done, before/after a version goes to 100% rollout. Each notable fix is its own commit (`Fix: <summary>`), its own build, its own versionCode bump when shipping. Don't batch unrelated fixes into a single commit unless they share a root cause — one fix per commit makes Play Console release notes trivial.
- **Release phase** — staged rollout via the Play Console release pipeline. Follow `store/LAUNCH_CHECKLIST.md`.

When the Roadmap's final sprint is done, the app is in permanent polish/bugfix mode unless a new version plan is drafted. The commit cadence above still applies.

## File conventions
- Entities: `data/entity/<Name>.kt`
- DAOs: `data/dao/<Name>Dao.kt`
- Repositories: `data/repository/<Name>Repository.kt`
- ViewModels: alongside their Compose screen, e.g. `ui/home/HomeViewModel.kt`
- Screens: `ui/screen/<Name>Screen.kt` for bottom-nav destinations; `ui/<feature>/<Name>Screen.kt` for feature sub-screens
- Prayer modes: `ui/prayer/modes/<ModeName>Mode.kt` — one composable per mode, accepting `PrayerSessionViewModel`
- Importers / loaders: `data/prayer/`
- Assets: `app/src/main/assets/<category>/*.json` (prayers, names, packs, rosary, daily_office, gratitude, legal)

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
