import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Secrets loading
// ─────────────────────────────────────────────────────────────────────────────
// Sensitive values (upload keystore creds, real AdMob unit IDs) live OUTSIDE
// this file. `project.findProperty(key)` checks, in order:
//   1. Project-level `gradle.properties` (committed — DO NOT paste secrets here)
//   2. User-level `~/.gradle/gradle.properties` (outside the repo — this is where
//      Nathan's real values live)
//   3. Command-line `-Pkey=value` overrides
//
// See `gradle.properties.template` at the repo root for the full list of keys
// the release build expects, and `store/SIGNING_SETUP.md` for the setup
// walk-through.
fun prop(key: String): String? =
    (project.findProperty(key) as? String)?.takeIf { it.isNotBlank() }

// Google's canonical test ad-unit IDs — safe to ship in debug builds.
// https://developers.google.com/admob/android/test-ads
// These strings MUST NOT appear in a release BuildConfig; the AdManager has a
// runtime fail-safe that throws on launch if they do.
private val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
private val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
private val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
private val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

// Placeholder marker. Release AdMob unit IDs that still equal this string at
// build time emit a clear Gradle warning — real IDs can only land via
// `~/.gradle/gradle.properties`. (Plain `val` — Kotlin DSL scripts are
// function bodies, so `const val` is not allowed here.)
private val AD_UNIT_PLACEHOLDER = "REPLACE_WITH_REAL_AD_UNIT_ID"

// Google's canonical test-ad-unit prefix. Release BuildConfig values that
// start with this string would mean the build accidentally shipped test
// ads — the release block throws in that case.
private val TEST_AD_UNIT_PREFIX = "ca-app-pub-3940256099942544"

// Billing SKU — matches the subscription product ID in Play Console
// (see store/LAUNCH_CHECKLIST.md §5). Exposed via BuildConfig and consumed
// by BillingManager.PREMIUM_MONTHLY_ID so there is a single source of truth.
private val PLAY_CONSOLE_SUB_SKU = "prayerquest_premium_monthly"

// ─────────────────────────────────────────────────────────────────────────────

android {
    namespace = "com.prayerquest.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.prayerquest.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export for migrations
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Billing SKU — same value in every build variant. If Play Console ever
        // renames the product, update PLAY_CONSOLE_SUB_SKU above (one place).
        buildConfigField("String", "BILLING_PREMIUM_MONTHLY_ID", "\"$PLAY_CONSOLE_SUB_SKU\"")
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Signing
    // ─────────────────────────────────────────────────────────────────────
    // The release keystore itself lives OUTSIDE the repo (e.g. `~/keys/
    // prayerquest-upload.jks`). Its path and credentials are pulled from
    // `~/.gradle/gradle.properties` via `prop(...)` — never committed here,
    // never referenced from project-level gradle.properties.
    //
    // If any required property is missing, we configure the release signingConfig
    // with empty values so Gradle prints a clear error (instead of a cryptic
    // `keystore was tampered with` message) and emit a warning at configure time
    // so the miss is obvious in the build log.
    signingConfigs {
        create("release") {
            val keystorePath = prop("uploadKeystorePath")
            val keystorePassword = prop("uploadKeystorePassword")
            val uploadKeyAlias = prop("uploadKeyAlias")
            val uploadKeyPassword = prop("uploadKeyPassword")

            val missing = buildList {
                if (keystorePath == null) add("uploadKeystorePath")
                if (keystorePassword == null) add("uploadKeystorePassword")
                if (uploadKeyAlias == null) add("uploadKeyAlias")
                if (uploadKeyPassword == null) add("uploadKeyPassword")
            }

            if (missing.isNotEmpty()) {
                logger.warn(
                    "⚠️  PrayerQuest release signing: missing ${missing.joinToString()} " +
                        "in ~/.gradle/gradle.properties — the release build will fail. " +
                        "See store/SIGNING_SETUP.md for the one-time setup."
                )
            } else {
                // Resolve the keystore path relative to the user's home if it
                // starts with `~`, otherwise treat it as-is (absolute or repo-
                // relative is fine).
                val resolvedPath = if (keystorePath!!.startsWith("~")) {
                    keystorePath.replaceFirst("~", System.getProperty("user.home"))
                } else {
                    keystorePath
                }
                storeFile = file(resolvedPath)
                storePassword = keystorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Debug builds always use Google's test ad unit IDs — safe for
            // emulators, dev devices, and anyone who isn't the Play-approved
            // release channel. Never click your own production ads during
            // development (AdMob flags invalid traffic and can suspend the
            // account).
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$TEST_BANNER_ID\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$TEST_INTERSTITIAL_ID\"")
            buildConfigField("String", "ADMOB_REWARDED_STREAK_SAVE_ID", "\"$TEST_REWARDED_ID\"")
            buildConfigField("String", "ADMOB_REWARDED_BONUS_XP_ID", "\"$TEST_REWARDED_ID\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$TEST_APP_OPEN_ID\"")
        }
        release {
            // R8 / resource shrinking — required for a shippable build.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            // Real ad unit IDs come exclusively from `~/.gradle/gradle.properties`.
            // If any key is unset, the build still runs with the placeholder
            // string — but (a) AdMob refuses the request at runtime so there's
            // no unintended revenue leak, and (b) we emit a Gradle warning here
            // so the omission is obvious in the build log.
            val bannerId = prop("ADMOB_BANNER_ID") ?: AD_UNIT_PLACEHOLDER
            val interstitialId = prop("ADMOB_INTERSTITIAL_ID") ?: AD_UNIT_PLACEHOLDER
            val rewardedStreakId = prop("ADMOB_REWARDED_STREAK_SAVE_ID") ?: AD_UNIT_PLACEHOLDER
            val rewardedBonusId = prop("ADMOB_REWARDED_BONUS_XP_ID") ?: AD_UNIT_PLACEHOLDER
            val appOpenId = prop("ADMOB_APP_OPEN_ID") ?: AD_UNIT_PLACEHOLDER

            val placeholders = listOfNotNull(
                "ADMOB_BANNER_ID".takeIf { bannerId == AD_UNIT_PLACEHOLDER },
                "ADMOB_INTERSTITIAL_ID".takeIf { interstitialId == AD_UNIT_PLACEHOLDER },
                "ADMOB_REWARDED_STREAK_SAVE_ID".takeIf { rewardedStreakId == AD_UNIT_PLACEHOLDER },
                "ADMOB_REWARDED_BONUS_XP_ID".takeIf { rewardedBonusId == AD_UNIT_PLACEHOLDER },
                "ADMOB_APP_OPEN_ID".takeIf { appOpenId == AD_UNIT_PLACEHOLDER },
            )
            if (placeholders.isNotEmpty()) {
                logger.warn(
                    "⚠️  PrayerQuest release AdMob: ${placeholders.joinToString()} " +
                        "still equal \"$AD_UNIT_PLACEHOLDER\". Real IDs must be set in " +
                        "~/.gradle/gradle.properties before shipping. See " +
                        "gradle.properties.template."
                )
            }

            // Fail-safe: test IDs never belong in a release BuildConfig.
            val leakedTestIds = listOfNotNull(
                "ADMOB_BANNER_ID".takeIf { bannerId.startsWith(TEST_AD_UNIT_PREFIX) },
                "ADMOB_INTERSTITIAL_ID".takeIf { interstitialId.startsWith(TEST_AD_UNIT_PREFIX) },
                "ADMOB_REWARDED_STREAK_SAVE_ID".takeIf { rewardedStreakId.startsWith(TEST_AD_UNIT_PREFIX) },
                "ADMOB_REWARDED_BONUS_XP_ID".takeIf { rewardedBonusId.startsWith(TEST_AD_UNIT_PREFIX) },
                "ADMOB_APP_OPEN_ID".takeIf { appOpenId.startsWith(TEST_AD_UNIT_PREFIX) },
            )
            if (leakedTestIds.isNotEmpty()) {
                throw GradleException(
                    "❌ PrayerQuest release build cannot use Google test ad unit IDs: " +
                        "${leakedTestIds.joinToString()}. These were detected in release " +
                        "BuildConfig. Fix ~/.gradle/gradle.properties before rebuilding."
                )
            }

            buildConfigField("String", "ADMOB_BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$interstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_STREAK_SAVE_ID", "\"$rewardedStreakId\"")
            buildConfigField("String", "ADMOB_REWARDED_BONUS_XP_ID", "\"$rewardedBonusId\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$appOpenId\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // lifecycle-process gives us ProcessLifecycleOwner, used by
    // AppOpenAdManager to detect the app moving to the foreground so we can
    // show app-open ads. Must live at the app level — NOT just runtime-ktx.
    implementation(libs.androidx.lifecycle.process)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // JSON parsing
    implementation(libs.google.gson)

    // EXIF — used by PhotoStorage to bake camera orientation into pixels
    // and strip GPS/device metadata before saving to app-private storage.
    implementation(libs.androidx.exifinterface)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    // Google Sign-In
    implementation(libs.google.play.services.auth)

    // Ads & Billing (scaffolding for Phase 5)
    implementation(libs.google.play.billing)
    implementation(libs.google.gms.ads)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
