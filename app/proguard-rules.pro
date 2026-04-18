# ── PrayerQuest ProGuard / R8 rules ────────────────────────────────────
#
# Port of ScriptureQuest's shipping rules, adapted to PrayerQuest's
# dependency surface (Firebase Auth/Firestore/Messaging + Gson for asset
# parsing, no Retrofit/Moshi). If you add a new reflective library,
# mirror the pattern below.

# Preserve stack-trace line numbers for Play Console crash reports. Without
# these, `adb logcat` traces show obfuscated class names with no source
# context, which makes prod bug triage miserable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotation attributes (needed by Gson @SerializedName, Room @Entity,
# Firestore @DocumentId, kotlinx.serialization if adopted later, etc.).
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ── Room ───────────────────────────────────────────────────────────────
# Room generates its DAO impls at build time via KSP; the annotations and
# entity fields must survive R8.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── Kotlin Coroutines ──────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── kotlinx.serialization ──────────────────────────────────────────────
# Not used at MVP but cheap to keep future-proof — the rules only kick in
# if a class actually carries @Serializable.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,includedescriptorclasses class com.prayerquest.app.**$$serializer { *; }
-keepclassmembers class com.prayerquest.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.prayerquest.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ── Jetpack Compose ────────────────────────────────────────────────────
# Compose uses inline value classes heavily (Color, Dp, TextUnit, etc.);
# R8 can shred them if we're not careful.
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.graphics.Color { *; }
-keep class androidx.compose.ui.unit.Dp { *; }
-keep class androidx.compose.ui.unit.TextUnit { *; }
-keep class androidx.compose.ui.unit.Density { *; }
# Preserve any kotlin inline class (value class) to avoid unboxing surprises
# in minified builds. This is cheap and saves hours of debugging.
-keepclassmembers class * {
    @kotlin.jvm.JvmInline <init>(...);
}
-keepclasseswithmembers class * {
    @kotlin.jvm.JvmInline *;
}

# ── Google Play Billing ────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── Google AdMob / GMS ─────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**

# ── Firebase (Auth, Firestore, Messaging, Analytics) ───────────────────
# Firestore relies heavily on reflection to (de)serialize documents into
# Kotlin data classes. Keeping the whole sdk + our Firestore DTO package
# is the safest policy; the APK size hit is negligible next to the time
# lost debugging a shrunk-class serialization failure in prod.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.common.**

# Keep Firestore-mapped DTOs (group + member + request documents).
-keep class com.prayerquest.app.firebase.** { *; }

# Firestore also introspects property annotations (@PropertyName,
# @DocumentId, etc.) on data-class fields. Preserve them.
-keepclassmembers class com.prayerquest.app.** {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
}

# ── WorkManager ────────────────────────────────────────────────────────
# WorkManager loads workers by reflection via the class name string in
# the WorkRequest; every Worker must survive R8.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# ── Gson (famous prayers, suggested packs, names of God, gratitude prompts) ──
# PrayerQuest loads every asset JSON via Gson; the DTO classes must keep
# their field names so `@SerializedName` (and default field matching) work.
-keepattributes Signature
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep asset-parsing DTOs and their nested types. `data/prayer` holds the
# JSON importers and their `*Json` shells; `data/gratitude` holds the
# gratitude-prompt DTOs.
-keep class com.prayerquest.app.data.prayer.** { *; }
-keep class com.prayerquest.app.data.gratitude.** { *; }

# ── Room entities + DAOs (package-scope keep for the whole data layer) ─
# Even though @Entity/@Dao keeps catch most cases, some entities use
# typealias + @Relation + embedded objects that R8 doesn't always trace.
-keep class com.prayerquest.app.data.entity.** { *; }
-keep class com.prayerquest.app.data.dao.** { *; }

# ── Keep Application class (reflection entry point for the manifest) ──
-keep class com.prayerquest.app.PrayerQuestApplication { *; }

# ── Misc suppressions to silence harmless warnings ─────────────────────
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
