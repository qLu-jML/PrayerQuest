package com.prayerquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Data class representing a custom color scheme for PrayerQuest.
 *
 * Ported one-to-one from ScriptureQuest's `AppColorScheme` so PrayerQuest's
 * 19-theme catalog (18 shared + 1 PrayerQuest default) renders with identical
 * palette richness (10 semantic color slots, not the 3-slot preview the app
 * shipped with before).
 *
 * Provides Material 3 conversion via [toMaterialLight] / [toMaterialDark] —
 * the same derivations (tinted surfaces, auto-contrast text) ScriptureQuest
 * uses so themes look correct without hand-tuning every token.
 */
data class AppTheme(
    val id: String,
    val name: String,
    val description: String,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondary: Color,
    val backgroundPaper: Color,
    val backgroundDefault: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val isBuiltIn: Boolean = true
) {
    fun toMaterialLight(): ColorScheme {
        val onPrimary = primary.contrastOn()
        val onSecondary = secondary.contrastOn()
        val onTertiary = accent.contrastOn()
        val onBackground = backgroundPaper.contrastOn()

        // In light mode, surfaceVariant must be a LIGHT color. If the theme's
        // backgroundDefault is dark (several themes use it for dark mode only),
        // derive a tinted variant from backgroundPaper instead so chip /
        // surface-variant backed text stays readable.
        val lightSurfaceVariant = if (backgroundDefault.luminance() > 0.4f) {
            backgroundDefault
        } else {
            backgroundPaper.darken(0.06f)
        }

        return lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryLight,
            onPrimaryContainer = primaryLight.contrastOn(),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondary.lighten(0.2f),
            onSecondaryContainer = secondary.lighten(0.2f).contrastOn(),
            tertiary = accent,
            onTertiary = onTertiary,
            tertiaryContainer = accent.lighten(0.2f),
            onTertiaryContainer = accent.lighten(0.2f).contrastOn(),
            background = backgroundPaper,
            onBackground = onBackground,
            surface = backgroundPaper,
            onSurface = onBackground,
            surfaceVariant = lightSurfaceVariant,
            onSurfaceVariant = Color(0xFF444444),
            error = Color(0xFFB3261E),
            onError = Color(0xFFF5F5F5),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B)
        )
    }

    fun toMaterialDark(): ColorScheme {
        val isDarkBackground = backgroundDefault.luminance() < 0.2f
        val darkBg = if (isDarkBackground) backgroundDefault else Color(0xFF1A1A1A)
        val darkSurface = if (isDarkBackground) backgroundDefault.lighten(0.1f) else Color(0xFF2A2A2A)

        val onPrimary = primaryLight.contrastOn()
        val onSecondary = secondary.contrastOn()
        val onTertiary = accent.contrastOn()
        val onBackground = darkBg.contrastOn()
        val onSurface = darkSurface.contrastOn()

        return darkColorScheme(
            primary = primaryLight,
            onPrimary = onPrimary,
            primaryContainer = primaryDark,
            onPrimaryContainer = primaryDark.contrastOn(),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondary.darken(0.3f),
            onSecondaryContainer = secondary.darken(0.3f).contrastOn(),
            tertiary = accent,
            onTertiary = onTertiary,
            tertiaryContainer = accent.darken(0.3f),
            onTertiaryContainer = accent.darken(0.3f).contrastOn(),
            background = darkBg,
            onBackground = onBackground,
            surface = darkSurface,
            onSurface = onSurface,
            surfaceVariant = darkSurface.lighten(0.1f),
            onSurfaceVariant = darkSurface.lighten(0.1f).contrastOn(),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF5F5F5)
        )
    }
}

fun AppTheme.toMaterialColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) toMaterialDark() else toMaterialLight()

/** Perceived luminance, 0.0 (black) → 1.0 (white). */
fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/** Dark text on light backgrounds, light text on dark backgrounds. */
fun Color.contrastOn(): Color =
    if (this.luminance() > 0.5f) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)

fun Color.lighten(factor: Float): Color {
    val amount = factor.coerceIn(0f, 1f)
    return Color(
        red = (red + (1f - red) * amount).coerceIn(0f, 1f),
        green = (green + (1f - green) * amount).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
        alpha = alpha
    )
}

fun Color.darken(factor: Float): Color {
    val amount = factor.coerceIn(0f, 1f)
    return Color(
        red = (red * (1f - amount)).coerceIn(0f, 1f),
        green = (green * (1f - amount)).coerceIn(0f, 1f),
        blue = (blue * (1f - amount)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/**
 * All built-in themes. PrayerQuest default first, then the 18 shared
 * ScriptureQuest themes alphabetically. (ScriptureQuest's own "Scripture
 * Quest" theme is intentionally excluded — PrayerQuest is its own brand.)
 */
object ThemeRepository {

    // PrayerQuest (default) — Calming indigo + warm gold, echoes the app's
    // prayer/devotion personality. Leads the list the same way ScriptureQuest
    // leads with its own "scripture_quest" theme.
    private val prayerQuest = AppTheme(
        id = "prayer_quest",
        name = "Prayer Quest",
        description = "Calming indigo with warm gold accents",
        primary = Color(0xFF3949AB),
        primaryLight = Color(0xFF9FA8DA),
        primaryDark = Color(0xFF1A237E),
        secondary = Color(0xFFFFA000),
        backgroundPaper = Color(0xFFFFF8F0),
        backgroundDefault = Color(0xFF1A1A2E),
        accent = Color(0xFFAD1457),
        success = Color(0xFF43A047),
        warning = Color(0xFFFBC02D),
        info = Color(0xFF42A5F5),
        isBuiltIn = true
    )

    // 90s Grandma's House — Vintage mauves and sage greens
    private val grandmasHouse = AppTheme(
        id = "90s_grandma",
        name = "90s Grandma's House",
        description = "Vintage mauves and sage greens",
        primary = Color(0xFF80567B),
        primaryLight = Color(0xFF7D5972),
        primaryDark = Color(0xFF5A4C7B),
        secondary = Color(0xFF517B6F),
        backgroundPaper = Color(0xFFF0E8ED),
        backgroundDefault = Color(0xFF2A2430),
        accent = Color(0xFF47617B),
        success = Color(0xFF5F6A58),
        warning = Color(0xFF885875),
        info = Color(0xFF4C6A71),
        isBuiltIn = true
    )

    // Afternoon Delight — Bright blues and vibrant oranges
    private val afternoonDelight = AppTheme(
        id = "afternoon_delight",
        name = "Afternoon Delight",
        description = "Bright blues and vibrant oranges",
        primary = Color(0xFF00B5EC),
        primaryLight = Color(0xFF01FFFF),
        primaryDark = Color(0xFF0090BD),
        secondary = Color(0xFFFA9835),
        backgroundPaper = Color(0xFFDFFFFE),
        backgroundDefault = Color(0xFFE8FFFF),
        accent = Color(0xFFEA6500),
        success = Color(0xFF34FF00),
        warning = Color(0xFFFFE833),
        info = Color(0xFFBD00FF),
        isBuiltIn = true
    )

    // Cherry Blossom — Soft pinks and warm whites
    private val cherryBlossom = AppTheme(
        id = "cherry_blossom",
        name = "Cherry Blossom",
        description = "Soft pinks and warm whites",
        primary = Color(0xFFFF6B9D),
        primaryLight = Color(0xFFFFB3D1),
        primaryDark = Color(0xFFE8548A),
        secondary = Color(0xFFFEC5BB),
        backgroundPaper = Color(0xFFFFE5E0),
        backgroundDefault = Color(0xFF2D1B2E),
        accent = Color(0xFFF0ADA0),
        success = Color(0xFFA8D8B8),
        warning = Color(0xFFFFB3D1),
        info = Color(0xFF3D2B3E),
        isBuiltIn = true
    )

    // Color Fiesta — Bright yellow with hot pink and purple accents
    private val colorFiesta = AppTheme(
        id = "color_fiesta",
        name = "Color Fiesta",
        description = "Bright yellow with hot pink and purple accents",
        primary = Color(0xFFFFE44D),
        primaryLight = Color(0xFFFFF176),
        primaryDark = Color(0xFFF4C20D),
        secondary = Color(0xFFFF1493),
        backgroundPaper = Color(0xFF1A1040),
        backgroundDefault = Color(0xFF0F0A28),
        accent = Color(0xFF7B68EE),
        success = Color(0xFF00E676),
        warning = Color(0xFFFF9100),
        info = Color(0xFF448AFF),
        isBuiltIn = true
    )

    // Desert Sand — Warm terracotta and sandy beiges
    private val desertSand = AppTheme(
        id = "desert_sand",
        name = "Desert Sand",
        description = "Warm terracotta and sandy beiges",
        primary = Color(0xFFD4A373),
        primaryLight = Color(0xFFE8C9A0),
        primaryDark = Color(0xFFB8824A),
        secondary = Color(0xFFF4E8D4),
        backgroundPaper = Color(0xFFFFF4E8),
        backgroundDefault = Color(0xFF3A2E2A),
        accent = Color(0xFFE07A5F),
        success = Color(0xFF95B88A),
        warning = Color(0xFFE8D4C0),
        info = Color(0xFF4A3E3A),
        isBuiltIn = true
    )

    // Electric Dreams — Neon colors and vibrant energy
    private val electricDreams = AppTheme(
        id = "electric_dreams",
        name = "Electric Dreams",
        description = "Neon colors and vibrant energy",
        primary = Color(0xFFF038FF),
        primaryLight = Color(0xFFD17FE0),
        primaryDark = Color(0xFFB020D0),
        secondary = Color(0xFF3772FF),
        backgroundPaper = Color(0xFF1A1A2E),
        backgroundDefault = Color(0xFF0F0F23),
        accent = Color(0xFF70E4EF),
        success = Color(0xFFE2EF70),
        warning = Color(0xFFEFEF70),
        info = Color(0xFF5A97E0),
        isBuiltIn = true
    )

    // Espresso — Rich browns and cream
    private val espresso = AppTheme(
        id = "espresso",
        name = "Espresso",
        description = "Rich browns and cream",
        primary = Color(0xFFC9ADA7),
        primaryLight = Color(0xFFE8D4CF),
        primaryDark = Color(0xFFB5958F),
        secondary = Color(0xFFF2E9E4),
        backgroundPaper = Color(0xFFFFF9F6),
        backgroundDefault = Color(0xFF22181C),
        accent = Color(0xFF9A8C98),
        success = Color(0xFFA8B8A0),
        warning = Color(0xFFE0D4CF),
        info = Color(0xFF3A2E32),
        isBuiltIn = true
    )

    // Fall Mood — Warm autumn colors and earthy tones
    private val fallMood = AppTheme(
        id = "fall_mood",
        name = "Fall Mood",
        description = "Warm autumn colors and earthy tones",
        primary = Color(0xFFBB9457),
        primaryLight = Color(0xFFFFE6A7),
        primaryDark = Color(0xFF8C6F4E),
        secondary = Color(0xFF99582A),
        backgroundPaper = Color(0xFFFFE7C2),
        backgroundDefault = Color(0xFF3D281B),
        accent = Color(0xFF6F1D1B),
        success = Color(0xFFA3673D),
        warning = Color(0xFFA3673D),
        info = Color(0xFF7B3F31),
        isBuiltIn = true
    )

    // Forest Haven — Deep greens and mossy tones
    private val forestHaven = AppTheme(
        id = "forest_haven",
        name = "Forest Haven",
        description = "Deep greens and mossy tones",
        primary = Color(0xFF6A994E),
        primaryLight = Color(0xFFA7C957),
        primaryDark = Color(0xFF557B3E),
        secondary = Color(0xFFBC4749),
        backgroundPaper = Color(0xFF3A4A28),
        backgroundDefault = Color(0xFF283618),
        accent = Color(0xFFD46A6C),
        success = Color(0xFFA7C957),
        warning = Color(0xFFBC4749),
        info = Color(0xFFA03537),
        isBuiltIn = true
    )

    // Golden Meadow — Warm golds and fresh greens
    private val goldenMeadow = AppTheme(
        id = "golden_meadow",
        name = "Golden Meadow",
        description = "Warm golds and fresh greens",
        primary = Color(0xFFF3DE2C),
        primaryLight = Color(0xFFFBB02D),
        primaryDark = Color(0xFFD95A00),
        secondary = Color(0xFF7CB518),
        backgroundPaper = Color(0xFFFFFBE8),
        backgroundDefault = Color(0xFFFFF8D0),
        accent = Color(0xFFFB6107),
        success = Color(0xFFACC700),
        warning = Color(0xFFFCA20A),
        info = Color(0xFF5C8001),
        isBuiltIn = true
    )

    // Late Spring — Vibrant coral and lush greens
    private val lateSpring = AppTheme(
        id = "late_spring",
        name = "Late Spring",
        description = "Vibrant coral and lush greens",
        primary = Color(0xFFF9C74F),
        primaryLight = Color(0xFFF9844A),
        primaryDark = Color(0xFFF3722C),
        secondary = Color(0xFF90BE6D),
        backgroundPaper = Color(0xFFFFF8E8),
        backgroundDefault = Color(0xFF277DA1),
        accent = Color(0xFFF94144),
        success = Color(0xFF43AA8B),
        warning = Color(0xFFF8961E),
        info = Color(0xFF577590),
        isBuiltIn = true
    )

    // Late Summer Vibes — Ocean blues and golden sunset hues
    private val lateSummerVibes = AppTheme(
        id = "late_summer",
        name = "Late Summer Vibes",
        description = "Ocean blues and golden sunset hues",
        primary = Color(0xFF0A9396),
        primaryLight = Color(0xFF94D2BD),
        primaryDark = Color(0xFF005F73),
        secondary = Color(0xFFEE9B00),
        backgroundPaper = Color(0xFFE9D8A6),
        backgroundDefault = Color(0xFF001219),
        accent = Color(0xFFCA6702),
        success = Color(0xFF94D2BD),
        warning = Color(0xFFEE9B00),
        info = Color(0xFF005F73),
        isBuiltIn = true
    )

    // Man in Black — True dark mode with soft whites
    private val manInBlack = AppTheme(
        id = "man_in_black",
        name = "Man in Black",
        description = "True dark mode with soft whites",
        primary = Color(0xFFE8E8E8),
        primaryLight = Color(0xFFF5F5F5),
        primaryDark = Color(0xFFCCCCCC),
        secondary = Color(0xFFB8B8B8),
        backgroundPaper = Color(0xFF1A1A1A),
        backgroundDefault = Color(0xFF0D0D0D),
        accent = Color(0xFF4A9EFF),
        success = Color(0xFF5AB88A),
        warning = Color(0xFFD4D4D4),
        info = Color(0xFFA0A0A0),
        isBuiltIn = true
    )

    // Nordic Winter — Cool blues and icy whites
    private val nordicWinter = AppTheme(
        id = "nordic_winter",
        name = "Nordic Winter",
        description = "Cool blues and icy whites",
        primary = Color(0xFFA8DADC),
        primaryLight = Color(0xFFC4EAEB),
        primaryDark = Color(0xFF8AC4C6),
        secondary = Color(0xFFF1FAEE),
        backgroundPaper = Color(0xFFFFFFFF),
        backgroundDefault = Color(0xFF1D3557),
        accent = Color(0xFF457B9D),
        success = Color(0xFF7BC47F),
        warning = Color(0xFFE0EBE0),
        info = Color(0xFF2D4567),
        isBuiltIn = true
    )

    // Ocean Depths — Deep teals and aquamarine
    private val oceanDepths = AppTheme(
        id = "ocean_depths",
        name = "Ocean Depths",
        description = "Deep teals and aquamarine",
        primary = Color(0xFF52B788),
        primaryLight = Color(0xFF74D4A8),
        primaryDark = Color(0xFF3A9B6F),
        secondary = Color(0xFF95D5B2),
        backgroundPaper = Color(0xFFB7F0D4),
        backgroundDefault = Color(0xFF1B4332),
        accent = Color(0xFF40916C),
        success = Color(0xFF74C69D),
        warning = Color(0xFF74B895),
        info = Color(0xFF2D6A4F),
        isBuiltIn = true
    )

    // Purple Cyberpunk — Neon purples and electric blues
    private val purpleCyberpunk = AppTheme(
        id = "purple_cyberpunk",
        name = "Purple Cyberpunk",
        description = "Neon purples and electric blues",
        primary = Color(0xFFF72585),
        primaryLight = Color(0xFF4CC9F0),
        primaryDark = Color(0xFFB5179E),
        secondary = Color(0xFF4361EE),
        backgroundPaper = Color(0xFF1A0A2E),
        backgroundDefault = Color(0xFF0D0520),
        accent = Color(0xFF7209B7),
        success = Color(0xFF4CC9F0),
        warning = Color(0xFFF72585),
        info = Color(0xFF4895EF),
        isBuiltIn = true
    )

    // Purple Prince — Royal purples and elegant tones
    private val purplePrince = AppTheme(
        id = "purple_prince",
        name = "Purple Prince",
        description = "Royal purples and elegant tones",
        primary = Color(0xFF9D4EDD),
        primaryLight = Color(0xFFC77DFF),
        primaryDark = Color(0xFF7B2CBF),
        secondary = Color(0xFFE0AAFF),
        backgroundPaper = Color(0xFFF5EAFF),
        backgroundDefault = Color(0xFF240046),
        accent = Color(0xFF5A189A),
        success = Color(0xFF7BC47F),
        warning = Color(0xFFFFD166),
        info = Color(0xFF4CC9F0),
        isBuiltIn = true
    )

    // Spring Transition — Fresh spring greens and coral accents
    private val springTransition = AppTheme(
        id = "spring_transition",
        name = "Spring Transition",
        description = "Fresh spring greens and coral accents",
        primary = Color(0xFF06D6A0),
        primaryLight = Color(0xFF2A9D8F),
        primaryDark = Color(0xFF073B4C),
        secondary = Color(0xFFEF476F),
        backgroundPaper = Color(0xFFF8F4EF),
        backgroundDefault = Color(0xFF264653),
        accent = Color(0xFF118AB2),
        success = Color(0xFF06D6A0),
        warning = Color(0xFFFFD166),
        info = Color(0xFFF4A261),
        isBuiltIn = true
    )

    private val builtInThemes: List<AppTheme> = listOf(
        prayerQuest,
        afternoonDelight,
        cherryBlossom,
        colorFiesta,
        desertSand,
        electricDreams,
        espresso,
        fallMood,
        forestHaven,
        goldenMeadow,
        grandmasHouse,
        lateSpring,
        lateSummerVibes,
        manInBlack,
        nordicWinter,
        oceanDepths,
        purpleCyberpunk,
        purplePrince,
        springTransition
    )

    val default: AppTheme = prayerQuest

    fun getThemeById(id: String): AppTheme? =
        builtInThemes.firstOrNull { it.id == id }

    fun getAllBuiltInThemes(): List<AppTheme> = builtInThemes

    /**
     * Fall back to default when an unknown ID is passed — e.g. the old
     * "scripture_quest" theme that used to ship with the app has been
     * removed; users who selected it will land on [prayerQuest].
     */
    fun getThemeByIdOrDefault(id: String): AppTheme =
        getThemeById(id) ?: default
}
