import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Load release ad-unit IDs from local.properties so real IDs never land in
// source control. Debug builds fall back to Google's canonical test IDs —
// NEVER click your own production ads during development (AdMob flags invalid
// traffic aggressively and can suspend the account).
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
fun adUnit(key: String, testId: String): String =
    localProperties.getProperty(key) ?: testId

// Google's canonical test ad-unit IDs — safe to ship in debug builds.
// https://developers.google.com/admob/android/test-ads
val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"

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
    }

    buildTypes {
        debug {
            // Test ad-unit IDs — always safe for emulators and dev devices.
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$TEST_BANNER_ID\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$TEST_INTERSTITIAL_ID\"")
            buildConfigField("String", "ADMOB_REWARDED_STREAK_SAVE_ID", "\"$TEST_REWARDED_ID\"")
            buildConfigField("String", "ADMOB_REWARDED_BONUS_XP_ID", "\"$TEST_REWARDED_ID\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$TEST_APP_OPEN_ID\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production IDs — read from local.properties. Falls back to test
            // IDs if local.properties is missing the key (prevents a broken
            // release build, but release APKs should always have these set).
            buildConfigField(
                "String", "ADMOB_BANNER_ID",
                "\"${adUnit("ADMOB_BANNER_ID", TEST_BANNER_ID)}\""
            )
            buildConfigField(
                "String", "ADMOB_INTERSTITIAL_ID",
                "\"${adUnit("ADMOB_INTERSTITIAL_ID", TEST_INTERSTITIAL_ID)}\""
            )
            buildConfigField(
                "String", "ADMOB_REWARDED_STREAK_SAVE_ID",
                "\"${adUnit("ADMOB_REWARDED_STREAK_SAVE_ID", TEST_REWARDED_ID)}\""
            )
            buildConfigField(
                "String", "ADMOB_REWARDED_BONUS_XP_ID",
                "\"${adUnit("ADMOB_REWARDED_BONUS_XP_ID", TEST_REWARDED_ID)}\""
            )
            buildConfigField(
                "String", "ADMOB_APP_OPEN_ID",
                "\"${adUnit("ADMOB_APP_OPEN_ID", TEST_APP_OPEN_ID)}\""
            )
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
