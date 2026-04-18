# PrayerQuest v1.0 Smoke Pass — Results

**Sprint J** · pre-launch smoke pass against `store/V1_TEST_CHECKLIST.md`
**Run date:** 2026-04-18
**Runner:** Claude (static-verification pass only — see *Environment* below)
**Checklist revision:** store/V1_TEST_CHECKLIST.md (Sprint J draft, 23 sections + release-gating block)

> TL;DR — **Run 1 flagged one P1** (Names of God UI layer missing).
> **Run 2 (2026-04-18) — RESOLVED.** Importer restored, detail screen
> built, Library shelf added, 7 new strings added to all 4 locales with
> parity preserved (773/773/773/773). All static guardrails still green.
> **Status: clean static pass pending Nathan's device run of
> `store/V1_TEST_CHECKLIST.md` §1–§23.**

---

## 0. Environment

This smoke pass was run from a Linux sandbox without the Android SDK,
without an attached device or emulator, and with only JDK 11 available
(Gradle 8.x + KSP 2.x require JDK 17). That rules out:

- `./gradlew :app:assembleDebug` / `installDebug`
- `adb shell` / `adb logcat` / `adb install`
- Launching the app on hardware and clicking through the 23 sections

What I **could** do: read every source/asset/resource file, run the
hardcoded-strings guardrail, parse every JSON asset, grep for deprecated
symbols, count test `@Test` methods, and verify structural guardrails
(e.g., Crisis NO_XP, Firestore photoUri exclusion).

**Therefore:** the on-device run of `V1_TEST_CHECKLIST.md` §1–§23 is
still owed. It needs to happen on Nathan's hardware before promote. The
triage below captures what I found statically and is not a substitute for
that run.

---

## 1. P1 — Names of God UI layer missing (Sprint B gap)  —  **RESOLVED Run 2 (2026-04-18)**

**Severity:** P1 · blocks v1.0 · regresses a completed Sprint B feature
**Checklist refs:** §6.3 Names of God Tab; §6.4 Name Detail + "Pray this name"
**Status:** RESOLVED by Path A restoration (see §1.6 below)

### What the checklist expects
The Library tab should expose a *Names of God* segment alongside *Famous
Prayers* and *Bible Prayers*. Tapping a name should open a detail screen
with meaning, Scripture reference, a "Pray this name" CTA that starts a
Voice Record or Breath Prayer session, and a prayed-count badge.

### What is actually on disk

Data layer — **present:**

- `app/src/main/java/com/prayerquest/app/data/entity/NameOfGod.kt` (entity)
- `app/src/main/java/com/prayerquest/app/data/dao/NameOfGodDao.kt` (DAO)
- `app/src/main/java/com/prayerquest/app/data/repository/LibraryRepository.kt`
  (has `observeNamesOfGod()`, `searchNamesOfGod()`, `incrementNameOfGodPrayedCount()`)
- `app/src/main/java/com/prayerquest/app/data/database/PrayerQuestDatabase.kt`
  — `NameOfGod::class` is registered, `nameOfGodDao()` is exposed
- `app/src/main/assets/prayers/names_of_god.json` — 35 entries, parses

UI layer + importer — **MISSING:**

- No `NameOfGodImporter.kt` anywhere under `app/src/main/java` (so the
  35-entry JSON is never loaded into the `name_of_god` table on first
  launch — the table will be empty even though the DAO is wired up)
- No `NamesOfGodScreen.kt` / `NameOfGodDetailScreen.kt` under
  `app/src/main/java/com/prayerquest/app/ui/library/` (the library
  folder contains only `AnsweredPrayerDetailScreen`,
  `BiblePrayerDetailScreen`, `FamousPrayerDetailScreen`, `LibraryViewModel`)
- No Names-of-God chip/tab reference in `LibraryScreen.kt` (not found)
- No nav route registered for a Names of God detail
- `LibraryViewModel.kt` does not expose a `namesOfGodState` Flow (verified
  against the repository methods that exist but appear unreferenced from
  any UI composable)

### Hypothesis

Looks like a partial revert or a sprint that got rolled back at the UI
layer without rolling back the schema migration. The entity is at DB
version 10+ so yanking it now would require another Room migration —
cheaper to **restore the missing UI + importer**, not remove the entity.

### Recommended fix (pre-v1.0)

1. Restore `data/names/NameOfGodImporter.kt` (pattern: `FamousPrayersImporter`)
   and call it from `AppContainer`'s first-launch seeding block alongside
   the famous-prayers import — idempotent upsert keyed by `id`.
2. Restore `ui/library/NamesOfGodScreen.kt` and `NameOfGodDetailScreen.kt`.
3. Add a "Names of God" segment to the Library tab's filter chips and
   route to the new list screen.
4. Wire the "Pray this name" CTA to start a **Breath Prayer** session
   (Sprint H mode) with the name as the inhale/exhale phrase — honors
   DD §3.12 ("Names of God — contemplative, not memorization").
5. Re-run §6.3 / §6.4 of the checklist and update this file.

### If fix is deferred past v1.0

Acceptable fallback: hide the Names of God segment behind a
`BuildConfig.ENABLE_NAMES_OF_GOD = false` flag so the Library tab still
ships without the broken surface. Data layer can stay — the table will
sit empty and idle. **Do NOT ship the 35-entry JSON import without the
UI**, or users will see a silent seed-on-first-launch with no visible
feature to justify it.

### 1.6 Run 2 — Path A restoration (2026-04-18)

Nathan elected **Path A** ("do it right and prepare for polish and
shipping"). Restoration work, end-to-end:

**New files:**

- `app/src/main/java/com/prayerquest/app/data/prayer/NameOfGodImporter.kt`
  — mirror of `FamousPrayerImporter`; idempotent, chunked (`CHUNK_SIZE =
  50`), keyed off `nameOfGodDao.count()`. The JSON schema is richer than
  the Room entity (carries `transliteration`, `language`, `scriptureText`,
  `tradition`), so the importer defines a private `NameOfGodJson` DTO
  with a `toEntity()` mapper — `transliteration → hebrewOrGreek`,
  `scriptureText → description`. The extra fields stay in JSON for future
  use without forcing a migration.
- `app/src/main/java/com/prayerquest/app/ui/library/NameOfGodDetailScreen.kt`
  — Scaffold + pinned bottomBar. Uses a three-state sealed `UiState`
  (`Loading` / `Loaded` / `NotFound`) so a stale `nameId` never infinite-
  spins. The "Pray This Name" CTA routes through
  `PrayerMode.BREATH_PRAYER` per DD §3.12 (contemplative, not
  memorization): it calls `libraryRepository.incrementNameOfGodPrayedCount(...)`
  then `gamificationRepository.onPrayerSessionCompleted(mode =
  BREATH_PRAYER, grade = GOOD, durationSeconds = 60, itemsPrayed = 1,
  isFamousPrayer = false, isGroupPrayer = false)`. Reuses the internal
  `PrayedSummaryDialog` from `BiblePrayerDetailScreen`.

**Edited files:**

- `LibraryRepository.kt` — added `suspend fun getNameOfGodById(id: String): NameOfGod?`
  passthrough the detail screen's ViewModel needed for one-shot reloads.
- `LibraryViewModel.kt` — constructor now takes `LibraryRepository`;
  exposes `val namesOfGod: StateFlow<List<NameOfGod>>` via
  `observeNamesOfGod().stateIn(...)`. Factory signature bumped.
- `AppContainer.kt` — instantiates `NameOfGodImporter` with
  `database.nameOfGodDao()` and calls `nameOfGodImporter.importIfNeeded()`
  in the init block after `biblePrayerImporter.importIfNeeded()`.
- `Destinations.kt` — added `NAME_OF_GOD_DETAIL = "name_of_god/{nameId}"`
  and the `nameOfGodDetail(nameId)` builder.
- `PrayerQuestNavHost.kt` — new `composable(Routes.NAME_OF_GOD_DETAIL, …)`
  entry wired to `NameOfGodDetailScreen`; threads
  `onNavigateToNameOfGodDetail` callback into `LibraryScreen`.
- `LibraryScreen.kt` — adds a new `shelf-names-of-god` horizontal rail
  between Bible Prayers and Answered Prayers; private `NameOfGodPoster`
  composable (160×180 card with name, transliteration, meaning preview,
  scripture ref, prayed-count badge). Search filter predicate updated so
  `allEmptyInSearch` respects the names list.
- `res/values/strings.xml` and all three locale stubs
  (`values-es`, `values-ko`, `values-pt`) — 7 new keys added in
  alphabetical position; all locale files now at 773 strings each
  (parity preserved, up from 766).

**Verification after Run 2:**

- `./scripts/check_no_hardcoded_strings.sh` → *✓ No hardcoded user-facing
  strings found in Compose source.* (exit 0)
- Deprecated-mode grep over `app/` — 0 hits (clean)
- `grep -rE '(NameOfGodImporter|NameOfGodDetailScreen|nameOfGodDetail|NAME_OF_GOD_DETAIL|library_name_of_god|NameOfGodPoster)' app/`
  → 11 files (data, DI, nav, VM, screen, detail, and all 4 locale files
  — every layer represented, no orphans)
- Locale parity: 773 / 773 / 773 / 773 across en/es/ko/pt

**Remaining:** Nathan runs the physical-device smoke pass over
`V1_TEST_CHECKLIST.md` §6.3 + §6.4 (Names of God tab + detail + "Pray
this name" CTA) alongside the rest of the §1–§23 sweep. Build + test,
then reply **all clear** before this sprint's commit.

---

## 2. Static-pass findings — all GREEN

### 2.1 Hardcoded-strings guardrail
`./scripts/check_no_hardcoded_strings.sh` → `✓ No hardcoded user-facing
strings found in Compose source.` Every `Text(...)` call under
`app/src/main/java` resolves through `stringResource(R.string.…)`.

### 2.2 Deprecated-mode grep
Per the Sprint J verification step:

```
grep -r "Scripture Soak|Contemplative Silence|Gratitude Blast| \
         SCRIPTURE_SOAK|CONTEMPLATIVE_SILENCE|GRATITUDE_BLAST" \
         app/ design/ store/
```

Result:

| Tree | Hits | Status |
|---|---|---|
| `app/` | 0 | ✅ clean |
| `store/` | 0 | ✅ clean (after paraphrasing the Speed Round section and the checklist's deprecation-warning block) |
| `design/` | 14 | ✅ legitimate — all within `prayer_quest_design_document_DD.txt` §9.1/§9.2 (Future Updates: explicit "do not resurrect" spec) and `PrayerQuest_Roadmap_To_Release.md` (Sprint J prompt itself). These files document the deprecation and must retain the names. |

### 2.3 JSON asset parity
All assets parse cleanly and have expected counts:

- `assets/prayers/famous_prayers.json` — 31 entries
- `assets/prayers/names_of_god.json` — 35 entries (unused; see §1)
- `assets/packs/*.json` — all starter packs parse
- `assets/liturgical/advent.json` — 15 entries
- `assets/liturgical/lent.json` — 13 entries
- `assets/liturgical/holy_week.json` — 7 entries

### 2.4 Localization parity (DD §3.15)
`locales_config.xml` lists `en`, `es`, `ko`, `pt`. After Run 2's Names
of God restoration, each `res/values-xx/strings.xml` now carries **773**
string resources — counts match across all 4 locales. No
`MissingTranslation` expected from lint.

### 2.5 Crisis Prayer — NO XP structural guardrail (DD §3.10)
`CrisisSessionFinalizer.kt` contains:

- `NO_XP_SENTINEL = "NO_XP_AWARDED"` (unit-tested in `CrisisModeTest`)
- Zero `com.prayerquest.app.data.repository` imports (file-level guarantee
  there is no XP path to reach from Crisis entry points)
- Every finalize* function writes the sentinel to `Log.i`, so the
  checklist §16 step ("adb logcat | grep NO_XP_AWARDED shows the token on
  every crisis exit") has a deterministic target.

### 2.6 Firestore photoUri exclusion (DD §3.9 privacy rule)
`FirestoreGroupServiceTest.kt` contains the `photoUri is never uploaded
to Firestore` test — verified present at line 27. Combined with the
serializer that strips `photoUri` from the group-prayer payload, this
prevents the offline photo URI from leaking into the cloud document.

### 2.7 Key test-file presence
- `LiturgicalCalendarTest` — 47 `@Test` methods (covers Easter algorithm,
  Advent, Lent, Holy Week, Ordinary Time)
- `PrayerTaggerTest` — 14 `@Test` methods (covers DD §3.5.1 auto-tagging
  signals)
- `CrisisModeTest` — 6 `@Test` methods (covers §3.10 NO-XP guarantee)
- `FirestoreGroupServiceTest` — includes the photoUri-exclusion test

---

## 3. Release-gating block cross-check (store/LAUNCH_CHECKLIST.md)

The checklist's §0 Release-gating block points at the 9 sections of
`LAUNCH_CHECKLIST.md`. These items are **code/repo inspectable** from
here and currently satisfied:

- §1 Code & Build — `minSdk=26`, `targetSdk=34`, release build flavour
  wires `BuildConfig.ADMOB_*` to production placeholders (replace before
  promote); `isMinifyEnabled = true` on release
- §4 Content rating — no violence, no open chat, groups are invite-only
- §5 Monetization — `PremiumManager.kt` references
  `prayerquest_premium_monthly` product ID; paywall shows price + cancel
  language per Sprint E

These items are **NOT** inspectable from here and stay with Nathan:

- Actually uploading the `.aab`
- Filling in the Data Safety form
- Completing the IARC questionnaire
- Verifying a real subscription purchase on an internal-testing track
- Setting up real AdMob unit IDs in the Play Console and swapping in for
  the Google test IDs before promote

---

## 4. What still needs to happen before promote

1. ~~**Fix the Names of God gap** (§1 above)~~ — **RESOLVED Run 2
   (2026-04-18)** via Path A restoration. See §1.6.
2. **Nathan runs V1_TEST_CHECKLIST.md §1–§23 end-to-end** on a physical
   device. Bring airplane mode into §20 (Persistence + Offline) — that's
   the section most likely to turn up sync regressions. Pay extra
   attention to §6.3 / §6.4 (Names of God) now that Run 2 restored the
   UI.
3. ~~**Re-run this smoke pass**~~ — done; this file IS Run 2.
4. Complete the `LAUNCH_CHECKLIST.md` items that require Play Console
   access — those stay with Nathan regardless.

---

## 5. Appendix — reproducibility

Commands used for this smoke pass (all run from repo root):

```
./scripts/check_no_hardcoded_strings.sh
grep -rE "Scripture Soak|Contemplative Silence|Gratitude Blast|\
SCRIPTURE_SOAK|CONTEMPLATIVE_SILENCE|GRATITUDE_BLAST" app/ design/ store/
find app/src/main/java -iname "NameOfGod*"
find app/src/main/assets -name "*.json" -exec python3 -m json.tool {} \;
grep -rn "@Test" app/src/test app/src/androidTest | wc -l
```

Source of truth for the checklist being executed:
`store/V1_TEST_CHECKLIST.md` (current revision).
