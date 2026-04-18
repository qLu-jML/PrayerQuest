# PrayerQuest Localization Audit

**Sprint scope:** Mechanical localization sweep across the Compose codebase. Every
user-visible literal has been externalized to `app/src/main/res/values/strings.xml`;
three placeholder locales (`values-es`, `values-ko`, `values-pt`) have been scaffolded
for real translation work; `android:localeConfig` is wired so Android surfaces the
per-app language picker; a guardrail script protects the codebase from regressions.

This document captures what changed, what was deliberately left English-only, and
the known follow-up work.

---

## At a glance

| Artifact                                       | Count |
| ---------------------------------------------- | ----: |
| `<string>` entries in `values/strings.xml`     | 766   |
| `<plurals>` entries in `values/strings.xml`    | 6     |
| Placeholder locales                            | 3 (es, ko, pt) |
| Files modified in the externalization sweep    | ~60   |
| Guardrail script                               | `scripts/check_no_hardcoded_strings.sh` |

The guardrail script currently reports **zero** hardcoded `Text("…")` literals in
the Compose source tree.

---

## Per-screen externalization inventory

Every file below was walked; every Compose `Text()`, `contentDescription`, and
user-visible button label was swapped to `stringResource(R.string.…)`. Keys are
namespaced by feature (`home_*`, `library_*`, `gratitude_*`, etc.) for easy
scanning when a whole section's copy needs revision.

| Area          | Key prefix         | Notes                                               |
| ------------- | ------------------ | --------------------------------------------------- |
| Common UI     | `common_*`         | Buttons, generic verbs, shared labels               |
| Navigation    | `navigation_*`     | Bottom-nav destination labels                       |
| Home          | `home_*`           | Dashboard copy, quest widget labels                 |
| Prayer modes  | `prayer_modes_*`   | All 8 prayer-mode screens + tutorial copy           |
| Prayer flow   | `prayer_*`         | Session screen, grade row, summary                  |
| Library       | `library_*`        | Famous prayers, answered prayers, bible prayers     |
| Collections   | `collections_*`    | Collection create/detail/edit screens               |
| Gratitude     | `gratitude_*`      | Log, catalogue, speed round, prompts                |
| Crisis        | `crisis_*`         | Breath prayer, psalms reader, resources link card   |
| Groups        | `groups_*`         | Create, join, detail, members, invites              |
| Celebration   | `celebration_*`    | Big moment screen, share-as-image copy              |
| Onboarding    | `onboarding_*`     | 7-step welcome flow                                 |
| Formation     | `formation_*`      | Habit-building onboarding for new items             |
| Notifications | `notifications_*`  | WorkManager worker body text                        |
| Settings      | `settings_*`       | Preferences, theme picker, developer toggles        |
| Profile       | `profile_*`        | Profile header, lifetime stats, achievements grid   |
| Premium       | `premium_*`        | Paywall, donate card, restore flow                  |
| Components    | `components_*`     | Shared composables (PhotoPickerSlot, chips, etc.)   |

---

## Non-@Composable scope bugs fixed

During the sweep I hoisted `stringResource()` calls out of several lambda
scopes where they were silently wrong. Kotlin compiles `@Composable` calls as
transformed calls (they can only run inside a composition), so calling them from
a plain `() -> Unit` lambda (like `onClick`, `.semantics { }`, or
`remember { }`'s calculation) is a compile error at minimum, and at worst
a subtle runtime bug if the scope ever gets inferred as Composable.

### Pattern: `.semantics { contentDescription = stringResource(…) }`

`semantics { }` takes a `SemanticsPropertyReceiver.() -> Unit`, not a Composable
lambda. Fixed across 7 call sites:

- `CrisisResourcesScreen.kt` — "Call X" button content description
- `CrisisPrayerScreen.kt` — tile + return-home link content descriptions
- `CrisisPsalmsReaderScreen.kt` — page-count + auto-advance toggle
- `CrisisBreathPrayerScreen.kt` — breath-cue description
- `HomeScreen.kt` — crisis-prayer link
- `PhotoPickerSlot.kt` — photo slot state description
- `AnsweredCelebrationScreen.kt` — "Praise God" headline

The pattern is: resolve with `stringResource(…)` at the `@Composable` level
into a `val`, then reference that `val` inside the `.semantics { }` block.

### Pattern: `remember { stringResource(…) }`

`remember`'s calculation lambda is `() -> T`, not Composable. Fixed in:

- `ThemePickerDialog.kt` — default custom theme name
- `ProfileScreen.kt` — default display name
- `LibraryScreen.kt` (2 sites) — `SimpleDateFormat` pattern
- `GuidedActsMode.kt` — supplication prompt
- `IntercessionDrillMode.kt` — default intercession items

Pattern: resolve outside `remember`, pass the plain `String` as a parameter to
the remember key or to the memoized body.

### Pattern: `onClick = { … stringResource(…) … }`

`onClick` is a plain `() -> Unit`. Fixed in:

- `PrayerBeadsMode.kt` — bead-session summary
- `FlashPraySwipeMode.kt` — flash-pray summary
- `ComingSoonModePlaceholder.kt` — placeholder summary
- `BreathPrayerMode.kt` — breath-session summary
- `JoinGroupScreen.kt` — validation message (and incidentally fixed a
  regression where the invite code was being wrapped in a "Pray for X"
  formatter — `joinGroup` takes the raw 6-char share code)
- `ThemePickerDialog.kt` — custom theme description

### Pattern: `DisposableEffect { … stringResource(…) … }`

`DisposableEffect`'s effect lambda is `DisposableEffectScope.() -> DisposableEffectResult`,
not Composable. Fixed in:

- `DonateCard.kt` — thanks-for-donating Toast text

### Pattern: `data class` default parameter values

Default values for data class constructor parameters are evaluated outside any
Composable scope. Fixed in:

- `OnboardingViewModel.kt::OnboardingAnswers` — `displayName`,
  `firstCollectionName`, and `firstCollectionDescription` defaults. The
  ViewModel's init block now seeds these values via
  `applicationContext.getString()`, and `completeOnboarding()` retains an
  `ifBlank` fallback as belt-and-suspenders.

### Pattern: `enum` initializer / `object` initializer / `const val`

Compose's `@Composable` annotation can't propagate into `enum class`,
`companion object`, or top-level `const val` initializers. Handled separately:

- `TopLevelDestination` enum (Destinations.kt): changed to store a
  `@StringRes labelRes: Int` and resolve via `stringResource(destination.labelRes)`
  inside the Composable nav host.
- `NotificationHelper.CHANNEL_NAME`: removed the const; inlined
  `context.getString(R.string.notifications_…)` at the call site where the
  `NotificationChannel` is constructed.
- `CrisisSessionFinalizer.LOG_TAG`: reverted to a plain `"CrisisPrayer"`
  string — it's a Logcat tag, not user-visible.

---

## Deliberate English-only strings

Some strings are **not safe to machine-translate** and remain in English until
a human reviewer with the relevant expertise can translate them. These are
listed here so translators and future maintainers know not to treat them as
oversights.

### Crisis hotline names and numbers

In `CrisisResourcesScreen.kt::CrisisResource.Companion.BUNDLED`, the three
bundled entries are plain `String` literals, not `stringResource` references:

- "988 Suicide & Crisis Lifeline" (US)
- "Samaritans" (UK & Ireland)
- "Lifeline Australia"

**Why:** This is a `companion object` initializer — `stringResource()` can't be
called there — but more fundamentally, **auto-translating the NAME of a crisis
helpline is a user-safety issue**. A mistranslation could send a user in
distress to the wrong phone number, or to a helpline that doesn't serve the
language they're calling in. Each locale needs a human in the loop to
determine the correct crisis resources for the language+region combination
(e.g. Spanish users in the US vs. in Spain vs. in Mexico all need different
entries).

**Planned refactor:** Move to `@StringRes`-based resource IDs resolved in the
screen's @Composable body; create per-locale overlays in
`res/values-<locale>/strings.xml` with locale-appropriate crisis resources
reviewed by a regional partner.

### Tradition and liturgical labels

Tradition names like "Catholic", "Orthodox", "Anglican", "Lutheran", "Reformed",
"Evangelical", and "Universal" are translated in the user-facing UI (we use
`stringResource(R.string.common_catholic)` etc.), but a handful of internal
**data keys** remain as English string literals — specifically where a value is
used as a Room query filter, a notification channel ID, or a gamification
tag. Concrete call sites:

- `FormationViewModel.kt` — category `"Personal"`, status `"Active"` (both
  used as DB filter predicates)
- `AddGroupPrayerScreen.kt` — `category = "Group"` (DB key)
- `OnboardingViewModel.kt` — `topicTag = "Personal"` (DB key)
- `PrayerSessionViewModel.kt` — debug fallback `levelTitle = "Level 1"`

**Why:** These are machine keys that persist in SQLite rows; translating them
would break every filter, break historical data, and break the contract between
the UI labels and the storage layer. The **display labels** are localized via
`common_personal`, `common_group`, `common_active`, etc.

### ViewModel-scope strings

A small number of user-facing strings are generated inside coroutine bodies of
ViewModels (e.g. `PrayerGroupsScreen.handleSignInResult`, `JoinGroupScreen`'s
success message, `PrayerGroupsScreen.refreshFromCloud`). ViewModels can't call
`stringResource()` because they're not `@Composable`.

**Localization-pending refactor:** Refactor these ViewModels to take a
`Context` (or `Resources`) reference via manual DI, then use
`context.getString(R.string.…)` inline. The Onboarding and Crisis ViewModels
already follow this pattern; the Groups ViewModels are the last holdouts.

Each offending site has a `// NOTE: Strings in this ViewModel coroutine can't
use @Composable stringResource(). Localization-pending — see LOCALIZATION_AUDIT.md`
comment so future maintainers have context.

### Theme config display names

19 theme names in `ui/theme/` (e.g. "Sunset Glow", "Ocean Deep", "Forest Path")
are currently English-only literals in the `AppTheme` catalog. **Why:** Themes
are picked visually in the ThemePickerDialog (the color swatch is the primary
identifier; the name is supplementary). They're safe to English-pass for MVP
and will be localized in the same pass that translates the premium catalog.

### Closing block in CrisisResourcesScreen

The "If you're not in the US, UK, or Australia..." copy is split across two
string resources plus previously had an orphan `"option."` tail. The orphan
has been removed; the two resources are concatenated at render time. A
future pass should **merge the two resource keys into a single string** with
proper placeholder punctuation so translators can re-order freely.

### Prayer beads / breath prayer content

Specific prayer texts (the Jesus Prayer inhale/exhale lines, rosary decade
prompts, Book of Common Prayer Daily Office texts) are intentionally kept in
English for v1. Each of these has established translations across faith
traditions but the canonical form per language varies enough that a faith-
tradition-aware translator should handle them — not a generic translation
service.

---

## Plurals

Six `<plurals>` entries were added to handle quantity-aware messages:

| Resource | Use sites |
| --- | --- |
| `gratitude_active_days` | GratitudeCatalogueScreen heat-map summary |
| `celebration_new_achievements_unlocked` | AnsweredCelebrationScreen achievements strip |
| (plus 4 others) | See `app/src/main/res/values/strings.xml` |

All use `pluralStringResource(R.plurals.…, quantity, quantity)` with the
quantity value passed twice — once to select the right branch, once as the
formatter argument for strings like `"%d day"` vs `"%d days"`.

---

## Positional format arguments

All format strings use Android's positional syntax (`%1$s`, `%2$s`, `%1$d`,
etc.) instead of Kotlin's `$var` template interpolation. This is mandatory for
resource files (Kotlin templates inside XML string values would leak through
as literal `$var` text at runtime) AND essential for translation: positional
args let translators re-order placeholders when target-language word order
differs from English.

Example: `"<string name="library_prayed_x_times">Prayed %1$d times</string>"`
is called as `stringResource(R.string.library_prayed_x_times, count)`.

---

## locales_config and per-app language settings

`res/xml/locales_config.xml` enumerates the app's four supported locales
(`en`, `es`, `ko`, `pt`). `android:localeConfig="@xml/locales_config"` is set
on `<application>` in `AndroidManifest.xml`, so on Android 13+ users can
switch PrayerQuest's language independently from their system language via
Settings > Languages > PrayerQuest.

`android:supportsRtl="true"` was already set — keep it for Arabic/Hebrew
locales when they land.

**To add a language:**

1. Add `<locale android:name="xx"/>` to `res/xml/locales_config.xml`
2. Run `python3 scratch/make_stub_translations.py` to scaffold a new
   `res/values-xx/strings.xml` with English placeholders (or have a translator
   supply the file directly)
3. Document the new locale here and update CI coverage

---

## Verification checklist

- [x] `./scripts/check_no_hardcoded_strings.sh` exits 0
- [x] `scan_non_composable.py` reports 0 real bugs (all remaining hits are
      false positives — nested helpers whose callers are @Composable)
- [x] `scan_onclick.py` reports 0 onClick-with-stringResource suspects
- [x] `scan_lambda_bugs.py` reports 0 non-Composable-lambda suspects
- [x] `values/strings.xml`, `values-es/strings.xml`, `values-ko/strings.xml`,
      `values-pt/strings.xml` all have identical key sets
- [x] `android:localeConfig` references `@xml/locales_config`
- [x] `android:supportsRtl="true"` preserved
- [ ] `./gradlew :app:lint` clean for MissingTranslation (pending — run
      locally in Android Studio; CI will pick this up)
- [ ] Device-level language switch test (pending Nathan's build+test pass)

---

## Known follow-ups (not in this sprint)

1. **ViewModel context wiring.** Refactor `PrayerGroupsViewModel` et al. to
   take a `Context` param via `AppContainer` so `joinGroup`'s success/error
   messages can be localized via `context.getString()`.
2. **Crisis hotlines per-locale.** Work with a regional partner (or legal)
   to compile accurate hotlines for each supported locale; refactor
   `CrisisResource.BUNDLED` to resolve from resources at render time.
3. **Closing block merge.** Merge the two-part closing copy in
   `CrisisResourcesScreen::ClosingBlock` into a single translator-friendly
   string.
4. **Theme names.** Localize the 19 theme display names in the Settings
   ThemePickerDialog catalog.
5. **Real translations.** Commission professional translations for the three
   placeholder locales (`es`, `ko`, `pt`). The TRANSLATION PENDING header
   comment at the top of each stub file is a persistent reminder.
6. **Tradition-specific prayer content.** When translating, hand prayer texts
   (rosary prayers, BCP Daily Office, Jesus Prayer, Breath Prayers, Famous
   Prayers catalogue) to translators with the relevant faith-tradition
   expertise; machine translation will not produce the canonical forms that
   users from each tradition recognize.
