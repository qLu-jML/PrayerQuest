package com.prayerquest.app.ui.theme

import androidx.compose.ui.graphics.Color

data class AppTheme(
    val id: String,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean = true,
    val lightPrimary: Long,       // Color hex as Long (0xFF...)
    val lightOnPrimary: Long,
    val lightPrimaryContainer: Long,
    val lightOnPrimaryContainer: Long,
    val lightSecondary: Long,
    val lightOnSecondary: Long,
    val lightSecondaryContainer: Long,
    val lightBackground: Long,
    val lightSurface: Long,
    val lightOnBackground: Long,
    val lightOnSurface: Long,
    val lightSurfaceVariant: Long,
    val darkPrimary: Long,
    val darkOnPrimary: Long,
    val darkPrimaryContainer: Long,
    val darkOnPrimaryContainer: Long,
    val darkSecondary: Long,
    val darkOnSecondary: Long,
    val darkSecondaryContainer: Long,
    val darkBackground: Long,
    val darkSurface: Long,
    val darkOnBackground: Long,
    val darkOnSurface: Long,
    val darkSurfaceVariant: Long
)

object ThemeRepository {
    private val builtInThemes = listOf(
        // 1. PrayerQuest Default — Calming Indigo primary, Warm Gold secondary
        AppTheme(
            id = "prayer_quest",
            name = "Prayer Quest",
            description = "Calming indigo with warm gold accents",
            isBuiltIn = true,
            lightPrimary = 0xFF3949AB,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFF9FA8DA,
            lightOnPrimaryContainer = 0xFF2D2B29,
            lightSecondary = 0xFFFFA000,
            lightOnSecondary = 0xFFFFFFFF,
            lightSecondaryContainer = 0xFFFFE082,
            lightBackground = 0xFFFFF8F0,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF2D2B29,
            lightOnSurface = 0xFF2D2B29,
            lightSurfaceVariant = 0xFFF5EDE0,
            darkPrimary = 0xFF9FA8DA,
            darkOnPrimary = 0xFF1A1A2E,
            darkPrimaryContainer = 0xFF3949AB,
            darkOnPrimaryContainer = 0xFFE8E6F0,
            darkSecondary = 0xFFFFE082,
            darkOnSecondary = 0xFF1A1A2E,
            darkSecondaryContainer = 0xFFFFA000,
            darkBackground = 0xFF1A1A2E,
            darkSurface = 0xFF222240,
            darkOnBackground = 0xFFE8E6F0,
            darkOnSurface = 0xFFE8E6F0,
            darkSurfaceVariant = 0xFF2E2E50
        ),

        // 2. ScriptureQuest Classic — Amber primary, Sage Green secondary
        AppTheme(
            id = "scripture_quest",
            name = "Scripture Quest",
            description = "Classic amber with sage green accents",
            isBuiltIn = true,
            lightPrimary = 0xFFF57F17,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFFFFD54F,
            lightOnPrimaryContainer = 0xFF2D2B29,
            lightSecondary = 0xFF558B2F,
            lightOnSecondary = 0xFFFFFFFF,
            lightSecondaryContainer = 0xFFC5E1A5,
            lightBackground = 0xFFFEF8E7,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF2D2B29,
            lightOnSurface = 0xFF2D2B29,
            lightSurfaceVariant = 0xFFF5EDE0,
            darkPrimary = 0xFFFFD54F,
            darkOnPrimary = 0xFF1A1A2E,
            darkPrimaryContainer = 0xFFF57F17,
            darkOnPrimaryContainer = 0xFFE8E6F0,
            darkSecondary = 0xFFC5E1A5,
            darkOnSecondary = 0xFF1A1A2E,
            darkSecondaryContainer = 0xFF558B2F,
            darkBackground = 0xFF1A1A2E,
            darkSurface = 0xFF222240,
            darkOnBackground = 0xFFE8E6F0,
            darkOnSurface = 0xFFE8E6F0,
            darkSurfaceVariant = 0xFF2E2E50
        ),

        // 3. Ocean Depths — Deep blue primary, Teal secondary
        AppTheme(
            id = "ocean_depths",
            name = "Ocean Depths",
            description = "Deep blue with teal accents",
            isBuiltIn = true,
            lightPrimary = 0xFF1565C0,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFF64B5F6,
            lightOnPrimaryContainer = 0xFF0D3B66,
            lightSecondary = 0xFF00897B,
            lightOnSecondary = 0xFFFFFFFF,
            lightSecondaryContainer = 0xFF80DEEA,
            lightBackground = 0xFFE0F2F1,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF0D3B66,
            lightOnSurface = 0xFF0D3B66,
            lightSurfaceVariant = 0xFFB2DFDB,
            darkPrimary = 0xFF64B5F6,
            darkOnPrimary = 0xFF0D1B2A,
            darkPrimaryContainer = 0xFF1565C0,
            darkOnPrimaryContainer = 0xFFE3F2FD,
            darkSecondary = 0xFF80DEEA,
            darkOnSecondary = 0xFF0D1B2A,
            darkSecondaryContainer = 0xFF00897B,
            darkBackground = 0xFF0D1B2A,
            darkSurface = 0xFF0D2438,
            darkOnBackground = 0xFFE3F2FD,
            darkOnSurface = 0xFFE3F2FD,
            darkSurfaceVariant = 0xFF1A3A52
        ),

        // 4. Forest Prayer — Forest green primary, Earth brown secondary
        AppTheme(
            id = "forest_prayer",
            name = "Forest Prayer",
            description = "Forest green with earthy brown tones",
            isBuiltIn = true,
            lightPrimary = 0xFF2E7D32,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFF81C784,
            lightOnPrimaryContainer = 0xFF1B5E20,
            lightSecondary = 0xFF5D4037,
            lightOnSecondary = 0xFFFFFFFF,
            lightSecondaryContainer = 0xFFD7CCC8,
            lightBackground = 0xFFF1F8E9,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF1B5E20,
            lightOnSurface = 0xFF1B5E20,
            lightSurfaceVariant = 0xFFC8E6C9,
            darkPrimary = 0xFF81C784,
            darkOnPrimary = 0xFF1B4D0B,
            darkPrimaryContainer = 0xFF2E7D32,
            darkOnPrimaryContainer = 0xFFC8E6C9,
            darkSecondary = 0xFFD7CCC8,
            darkOnSecondary = 0xFF1B4D0B,
            darkSecondaryContainer = 0xFF5D4037,
            darkBackground = 0xFF1B4D0B,
            darkSurface = 0xFF263238,
            darkOnBackground = 0xFFC8E6C9,
            darkOnSurface = 0xFFC8E6C9,
            darkSurfaceVariant = 0xFF37474F
        ),

        // 5. Royal Purple — Deep purple primary, Gold secondary
        AppTheme(
            id = "royal_purple",
            name = "Royal Purple",
            description = "Deep purple with gold accents",
            isBuiltIn = true,
            lightPrimary = 0xFF6A1B9A,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFFCE93D8,
            lightOnPrimaryContainer = 0xFF4A148C,
            lightSecondary = 0xFFFFB300,
            lightOnSecondary = 0xFF000000,
            lightSecondaryContainer = 0xFFFFE082,
            lightBackground = 0xFFF3E5F5,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF4A148C,
            lightOnSurface = 0xFF4A148C,
            lightSurfaceVariant = 0xFFE1BEE7,
            darkPrimary = 0xFFCE93D8,
            darkOnPrimary = 0xFF38006B,
            darkPrimaryContainer = 0xFF6A1B9A,
            darkOnPrimaryContainer = 0xFFF3E5F5,
            darkSecondary = 0xFFFFE082,
            darkOnSecondary = 0xFF38006B,
            darkSecondaryContainer = 0xFFFFB300,
            darkBackground = 0xFF38006B,
            darkSurface = 0xFF311B92,
            darkOnBackground = 0xFFF3E5F5,
            darkOnSurface = 0xFFF3E5F5,
            darkSurfaceVariant = 0xFF512DA8
        ),

        // 6. Sunrise Faith — Warm orange primary, Pink secondary
        AppTheme(
            id = "sunrise_faith",
            name = "Sunrise Faith",
            description = "Warm orange with pink accents",
            isBuiltIn = true,
            lightPrimary = 0xFFE65100,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFFFFB74D,
            lightOnPrimaryContainer = 0xFFBF360C,
            lightSecondary = 0xFFAD1457,
            lightOnSecondary = 0xFFFFFFFF,
            lightSecondaryContainer = 0xFFF48FB1,
            lightBackground = 0xFFFFF3E0,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFFBF360C,
            lightOnSurface = 0xFFBF360C,
            lightSurfaceVariant = 0xFFFFE0B2,
            darkPrimary = 0xFFFFB74D,
            darkOnPrimary = 0xFF662D00,
            darkPrimaryContainer = 0xFFE65100,
            darkOnPrimaryContainer = 0xFFFFF3E0,
            darkSecondary = 0xFFF48FB1,
            darkOnSecondary = 0xFF662D00,
            darkSecondaryContainer = 0xFFAD1457,
            darkBackground = 0xFF662D00,
            darkSurface = 0xFF5D2E2A,
            darkOnBackground = 0xFFFFF3E0,
            darkOnSurface = 0xFFFFF3E0,
            darkSurfaceVariant = 0xFF8D6E63
        ),

        // 7. Midnight Watch — Dark theme optimized for night prayer
        AppTheme(
            id = "midnight_watch",
            name = "Midnight Watch",
            description = "Dark theme for night prayer sessions",
            isBuiltIn = true,
            lightPrimary = 0xFF42A5F5,
            lightOnPrimary = 0xFF001A4D,
            lightPrimaryContainer = 0xFF1976D2,
            lightOnPrimaryContainer = 0xFFFFFFFF,
            lightSecondary = 0xFFC0C0C0,
            lightOnSecondary = 0xFF1A1A2E,
            lightSecondaryContainer = 0xFFE8E8E8,
            lightBackground = 0xFFF5F5F5,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF1A1A2E,
            lightOnSurface = 0xFF1A1A2E,
            lightSurfaceVariant = 0xFFE0E0E0,
            darkPrimary = 0xFF42A5F5,
            darkOnPrimary = 0xFF0D1B2A,
            darkPrimaryContainer = 0xFF1976D2,
            darkOnPrimaryContainer = 0xFFE3F2FD,
            darkSecondary = 0xFFC0C0C0,
            darkOnSecondary = 0xFF0D1B2A,
            darkSecondaryContainer = 0xFF808080,
            darkBackground = 0xFF0D1B2A,
            darkSurface = 0xFF0F2847,
            darkOnBackground = 0xFFE3F2FD,
            darkOnSurface = 0xFFE3F2FD,
            darkSurfaceVariant = 0xFF1A3F5A
        ),

        // 8. Olive Garden — Olive primary, Terracotta secondary
        AppTheme(
            id = "olive_garden",
            name = "Olive Garden",
            description = "Olive with warm terracotta tones",
            isBuiltIn = true,
            lightPrimary = 0xFF827717,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFFCEDC1E,
            lightOnPrimaryContainer = 0xFF4D4300,
            lightSecondary = 0xFFA1887F,
            lightOnSecondary = 0xFFFFFFFF,
            lightSecondaryContainer = 0xFFE0D7D0,
            lightBackground = 0xFFFFFBEE,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF4D4300,
            lightOnSurface = 0xFF4D4300,
            lightSurfaceVariant = 0xFFE7E1D7,
            darkPrimary = 0xFFCEDC1E,
            darkOnPrimary = 0xFF4D4300,
            darkPrimaryContainer = 0xFF827717,
            darkOnPrimaryContainer = 0xFFFFFBEE,
            darkSecondary = 0xFFE0D7D0,
            darkOnSecondary = 0xFF4D4300,
            darkSecondaryContainer = 0xFFA1887F,
            darkBackground = 0xFF4D4300,
            darkSurface = 0xFF5C5345,
            darkOnBackground = 0xFFFFFBEE,
            darkOnSurface = 0xFFFFFBEE,
            darkSurfaceVariant = 0xFF78736D
        ),

        // 9. Crimson Grace — Deep red primary, Gold secondary
        AppTheme(
            id = "crimson_grace",
            name = "Crimson Grace",
            description = "Deep red with gold accents",
            isBuiltIn = true,
            lightPrimary = 0xFFB71C1C,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFFEF5350,
            lightOnPrimaryContainer = 0xFF7F0000,
            lightSecondary = 0xFFFFC107,
            lightOnSecondary = 0xFF000000,
            lightSecondaryContainer = 0xFFFFE082,
            lightBackground = 0xFFFFF3E0,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF7F0000,
            lightOnSurface = 0xFF7F0000,
            lightSurfaceVariant = 0xFFFFD8D8,
            darkPrimary = 0xFFEF5350,
            darkOnPrimary = 0xFF5E0000,
            darkPrimaryContainer = 0xFFB71C1C,
            darkOnPrimaryContainer = 0xFFFFF3E0,
            darkSecondary = 0xFFFFE082,
            darkOnSecondary = 0xFF5E0000,
            darkSecondaryContainer = 0xFFFFC107,
            darkBackground = 0xFF5E0000,
            darkSurface = 0xFF6B2C2C,
            darkOnBackground = 0xFFFFF3E0,
            darkOnSurface = 0xFFFFF3E0,
            darkSurfaceVariant = 0xFF8D5C5C
        ),

        // 10. Sky Blue — Light blue primary, Coral secondary
        AppTheme(
            id = "sky_blue",
            name = "Sky Blue",
            description = "Light blue with coral accents",
            isBuiltIn = true,
            lightPrimary = 0xFF0288D1,
            lightOnPrimary = 0xFFFFFFFF,
            lightPrimaryContainer = 0xFF81D4FA,
            lightOnPrimaryContainer = 0xFF01579B,
            lightSecondary = 0xFFFF7043,
            lightOnSecondary = 0xFFFFFFFF,
            lightSecondaryContainer = 0xFFFFCCBC,
            lightBackground = 0xFFE1F5FE,
            lightSurface = 0xFFFFFFFF,
            lightOnBackground = 0xFF01579B,
            lightOnSurface = 0xFF01579B,
            lightSurfaceVariant = 0xFFB3E5FC,
            darkPrimary = 0xFF81D4FA,
            darkOnPrimary = 0xFF003D82,
            darkPrimaryContainer = 0xFF0288D1,
            darkOnPrimaryContainer = 0xFFE1F5FE,
            darkSecondary = 0xFFFFCCBC,
            darkOnSecondary = 0xFF003D82,
            darkSecondaryContainer = 0xFFFF7043,
            darkBackground = 0xFF003D82,
            darkSurface = 0xFF0D3B66,
            darkOnBackground = 0xFFE1F5FE,
            darkOnSurface = 0xFFE1F5FE,
            darkSurfaceVariant = 0xFF1A5F7A
        )
    )

    fun getThemeById(id: String): AppTheme? {
        return builtInThemes.firstOrNull { it.id == id }
    }

    fun getAllBuiltInThemes(): List<AppTheme> {
        return builtInThemes
    }

    fun getThemeByIdOrDefault(id: String): AppTheme {
        return getThemeById(id) ?: getThemeById("prayer_quest") ?: builtInThemes.first()
    }

    fun parseCustomTheme(json: String): AppTheme? {
        return try {
            // Basic JSON parsing for custom theme
            // In production, use kotlinx.serialization or Gson
            if (json.isBlank() || json == "[]") return null
            // For now, return null as parsing requires proper JSON library
            null
        } catch (e: Exception) {
            null
        }
    }

    fun customThemeToJson(theme: AppTheme): String {
        // Serialize custom theme to JSON
        // In production, use kotlinx.serialization or Gson
        return ""
    }
}
