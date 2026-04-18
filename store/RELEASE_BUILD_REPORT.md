# PrayerQuest — Release Build Hardening Report

Summary of what the release-build hardening pass covered, which validations
ran, and what Nathan still needs to verify on his Windows box before the
first Play-Console upload.

## What changed

1. `app/build.gradle.kts`
   - `isMinifyEnabled = true`, `isShrinkResources = true` on the release
     build type.
   - `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
     "proguard-rules.pro")`.
   - New `signingConfigs.release` block that reads four properties from
     `project.findProperty(...)` (so they resolve from
     `~/.gradle/gradle.properties`, NOT project-level
     `gradle.properties`): `uploadKeystorePath`, `uploadKeystorePassword`,
     `uploadKeyAlias`, `uploadKeyPassword`. A missing property triggers a
     `logger.warn` at configure time so the omission is obvious in the build
     log.
   - Release `buildConfigField` values for the five AdMob unit IDs come from
     `~/.gradle/gradle.properties` (keys `ADMOB_BANNER_ID`,
     `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_STREAK_SAVE_ID`,
     `ADMOB_REWARDED_BONUS_XP_ID`, `ADMOB_APP_OPEN_ID`). Missing keys fall
     back to the string `"REPLACE_WITH_REAL_AD_UNIT_ID"` AND log a warning
     — and the `AdManager` runtime fail-safe refuses to initialize if that
     placeholder ships.
   - Release build fails fast with a `GradleException` if any release
     `buildConfigField` still starts with `ca-app-pub-3940256099942544` (the
     Google test-ad prefix). The only path that could leak a test ID is
     therefore an explicit `-PADMOB_*=ca-app-pub-3940256099942544/…`
     override, which immediately stops the build.
   - New `BILLING_PREMIUM_MONTHLY_ID` buildConfigField
     (`"prayerquest_premium_monthly"`) consumed by
     `BillingManager.PREMIUM_MONTHLY_ID` — single source of truth that
     matches the Play Console SKU.

2. `app/proguard-rules.pro`
   - Rewritten from the ScriptureQuest template and adapted to
     PrayerQuest's dependency surface (Firebase Auth/Firestore/Messaging,
     Gson, WorkManager, Billing, AdMob, Compose inline classes,
     kotlinx.serialization for future-proofing, Room @Entity/@Dao/
     @Database, asset-parsing DTOs under `data/prayer/**` and
     `data/gratitude/**`).

3. `app/src/main/java/com/prayerquest/app/ads/AdManager.kt`
   - Added a runtime fail-safe at the top of `initialize(...)`: in
     `!BuildConfig.DEBUG` builds, if any ad unit ID starts with
     `ca-app-pub-3940256099942544` OR equals
     `REPLACE_WITH_REAL_AD_UNIT_ID`, the method throws
     `IllegalStateException` with an actionable message.

4. `app/src/main/java/com/prayerquest/app/billing/BillingManager.kt`
   - `PREMIUM_MONTHLY_ID` now reads from
     `BuildConfig.BILLING_PREMIUM_MONTHLY_ID` (value
     `"prayerquest_premium_monthly"`). The previous hard-coded value
     (`"prayer_quest_premium_monthly"`) was out of sync with
     `store/LAUNCH_CHECKLIST.md §5` and would have caused a silent paywall
     failure at purchase time.

5. `gradle.properties.template` (new, committed)
   - Documents every key Nathan needs to add to
     `~/.gradle/gradle.properties` (never to the project-level
     `gradle.properties`, which remains tracked for shared JVM / Kotlin
     flags only).

6. `store/SIGNING_SETUP.md` (new, committed)
   - Keystore generation command
     (`keytool -genkey -v -keystore upload.jks -keyalg RSA -keysize 2048
     -validity 10000 -alias prayerquest-upload`), the property block for
     `~/.gradle/gradle.properties`, verification via
     `./gradlew :app:signingReport`, troubleshooting tips, and a nag about
     backing up the keystore immediately.

## What ran successfully in the sandbox

Verified against `Gradle 9.3.1` + `OpenJDK 11.0.30` on the Linux sandbox
(no Android SDK). These validated the configure-phase logic added to
`build.gradle.kts`:

- `./gradlew help` with no properties set — emitted both expected warnings
  (missing signing props, placeholder ad-unit IDs) and completed
  `BUILD SUCCESSFUL`.
- `./gradlew help` with a full set of `-P` overrides pointing at a
  throwaway keystore and fictitious AdMob IDs — warnings suppressed,
  `BUILD SUCCESSFUL`.
- `./gradlew help` with `-PADMOB_BANNER_ID=ca-app-pub-3940256099942544/…`
  — release block correctly threw
  `❌ PrayerQuest release build cannot use Google test ad unit IDs:
  ADMOB_BANNER_ID`. Fail-safe wired correctly.

## What could not be verified in the sandbox

The sandbox has no Android SDK and no ability to install one, so the
following could not be run here. They will need a pass on Nathan's
Windows box with Android Studio installed:

- `./gradlew :app:compileReleaseKotlin` (needs the SDK).
- `./gradlew :app:lintRelease`.
- `./gradlew :app:assembleRelease` and `:app:bundleRelease` (both stop at
  `:app:minifyReleaseWithR8 > SDK location not found`).
- Full R8 / resource-shrink pass against the new `proguard-rules.pro` —
  the first real release build is the only way to know whether any of
  the rules are insufficient (usually shows up as a `ClassNotFoundException`
  or a Firestore `NoSuchMethodError` at runtime).
- Actual APK signing with a real keystore.

## Nathan's next steps on Windows

1. Generate the upload keystore — see `store/SIGNING_SETUP.md` §1.
2. Copy the block from `gradle.properties.template` into
   `%USERPROFILE%\.gradle\gradle.properties` and fill in real values.
3. Run `./gradlew :app:signingReport` and confirm the release variant is
   signed with the new key.
4. Run `./gradlew :app:bundleRelease`. Expect either:
   - ✅ `BUILD SUCCESSFUL`, `.aab` at `app/build/outputs/bundle/release/
     app-release.aab`. No warnings from the PrayerQuest configure block.
   - ❌ A warning from the `⚠️  PrayerQuest release ...` block. Fix the
     missing / placeholder value before uploading anything.
5. Install the signed APK (`bundletool build-apks` against the `.aab`) on
   a real device. Confirm:
   - No crash on launch from `AdManager.initialize`.
   - Ads load real creatives (not the Google "Test Ad" overlay).
   - Paywall launches the purchase flow against the
     `prayerquest_premium_monthly` SKU (use a license-tested Play
     account so the charge is waived).

## Placeholder / secret policy (reminder)

- `~/.gradle/gradle.properties` holds every real secret. It lives outside
  this repository and is never committed — the sandbox version of this
  file does not exist, which is how we were able to exercise the
  "missing value" warning path above.
- `gradle.properties` at the repo root keeps its pre-existing
  non-sensitive build flags only; do not paste secrets into it.
- `*.jks` / `*.keystore` are already covered by `.gitignore`; the
  throwaway keystore used for the sandbox checks above lives at
  `/tmp/pqtest-keys/throwaway.jks` and was never copied into the repo.
