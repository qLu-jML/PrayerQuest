package com.prayerquest.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
// PrayerQuest Brand Palette & Semantic Colors
// ═══════════════════════════════════════════════════════════════════════════
// These colors are THEME-INDEPENDENT and remain consistent across all themes.
// For theme-specific colors (primary, secondary, background), use ThemeRepository.
// ═══════════════════════════════════════════════════════════════════════════

// ────────────────────────────────────────────────────────────────────────────
// DEFAULT THEME COLORS (used in "prayer_quest" theme)
// ────────────────────────────────────────────────────────────────────────────

// Primary: Calming Indigo (prayer, devotion, depth)
val Indigo300 = Color(0xFF9FA8DA)
val Indigo500 = Color(0xFF5C6BC0)
val Indigo700 = Color(0xFF3949AB)

// Secondary: Warm Gold (faith, light, hope)
val Gold300 = Color(0xFFFFE082)
val Gold500 = Color(0xFFFFCA28)
val Gold700 = Color(0xFFFFA000)

// Tertiary: Soft Rose (love, warmth, gratitude)
val Rose300 = Color(0xFFF48FB1)
val Rose500 = Color(0xFFEC407A)
val Rose700 = Color(0xFFAD1457)

// Neutrals: Warm parchment tones
val Parchment = Color(0xFFFFF8F0)
val ParchmentDark = Color(0xFFF5EDE0)
val Ink = Color(0xFF2D2B29)
val InkLight = Color(0xFF4A4743)
val Stone = Color(0xFF8A8580)
val StoneLight = Color(0xFFD4CFC8)

// Dark mode neutrals
val Grey900 = Color(0xFF1A1A2E)           // Deep navy-black
val Grey800 = Color(0xFF222240)
val Grey700 = Color(0xFF2E2E50)
val Grey200 = Color(0xFFCAC8D8)
val Grey100 = Color(0xFFE8E6F0)

// ────────────────────────────────────────────────────────────────────────────
// SEMANTIC COLORS (ALWAYS CONSTANT across all themes)
// ────────────────────────────────────────────────────────────────────────────

val FlameRed = Color(0xFFE53935)           // Streak flame
val SuccessGreen = Color(0xFF43A047)       // Answered prayers
val WarningGold = Color(0xFFFBC02D)        // Streak warning
val ErrorRed = Color(0xFFC62828)

// Gratitude section accent
val GratitudeGreen = Color(0xFF66BB6A)
val GratitudeGold = Color(0xFFFFD54F)

// Group section accent
val CommunityBlue = Color(0xFF42A5F5)
val CommunityTeal = Color(0xFF26A69A)

// ────────────────────────────────────────────────────────────────────────────
// LITURGICAL ACCENTS — used by the Home Liturgical Day card and the Library
// seasonal-pack pin. Deliberately muted so they read as "a stained-glass hint"
// rather than "kitsch cathedral". DD §3.5.4 calls for subtle, not loud.
// ────────────────────────────────────────────────────────────────────────────
val StainedGlassViolet = Color(0xFF6A4FA8)   // Deep jewel-tone violet
val StainedGlassAmber = Color(0xFFE6B93A)    // Warm candle-glow amber
