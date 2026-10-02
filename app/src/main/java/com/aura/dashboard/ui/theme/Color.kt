package com.aura.dashboard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.aura.dashboard.domain.model.AccentColor
import com.aura.dashboard.domain.model.AppStyle

@Immutable
data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

fun accentPalette(accent: AccentColor, dark: Boolean): AccentPalette = if (!dark) {
    when (accent) {
        AccentColor.LAVENDER -> AccentPalette(Color(0xFF6457E6), Color.White, Color(0xFFECE8FF), Color(0xFF2E2475), Color(0xFF9D8CFF), Color(0xFF7AA8FF))
        AccentColor.OCEAN -> AccentPalette(Color(0xFF2268D6), Color.White, Color(0xFFE2ECFF), Color(0xFF0F2F66), Color(0xFF6FA8FF), Color(0xFF6CCBEF))
        AccentColor.ROSE -> AccentPalette(Color(0xFFB83C76), Color.White, Color(0xFFFFE4EF), Color(0xFF6A1A3D), Color(0xFFFF8DB8), Color(0xFFC69CFF))
        AccentColor.MINT -> AccentPalette(Color(0xFF17806A), Color.White, Color(0xFFD9F4EC), Color(0xFF0C4A3C), Color(0xFF5FD3B4), Color(0xFF7FB8FF))
        AccentColor.PEACH -> AccentPalette(Color(0xFFB5532B), Color.White, Color(0xFFFFE8DC), Color(0xFF6B2A10), Color(0xFFFFAA80), Color(0xFFFF8DB8))
    }
} else {
    when (accent) {
        AccentColor.LAVENDER -> AccentPalette(Color(0xFFB7ABFF), Color(0xFF221A5C), Color(0xFF3A3170), Color(0xFFE6E1FF), Color(0xFFA594FF), Color(0xFF7FA9FF))
        AccentColor.OCEAN -> AccentPalette(Color(0xFF94BDFF), Color(0xFF0B2A5E), Color(0xFF22406F), Color(0xFFDCE8FF), Color(0xFF7FB0FF), Color(0xFF6FD0F2))
        AccentColor.ROSE -> AccentPalette(Color(0xFFFFA3C7), Color(0xFF5A1032), Color(0xFF6B2E4B), Color(0xFFFFE0EC), Color(0xFFFF8DB8), Color(0xFFC3A0FF))
        AccentColor.MINT -> AccentPalette(Color(0xFF86DFC6), Color(0xFF053A2E), Color(0xFF1F5447), Color(0xFFD2F5EA), Color(0xFF6BD6B9), Color(0xFF86B9FF))
        AccentColor.PEACH -> AccentPalette(Color(0xFFFFBA98), Color(0xFF5A2208), Color(0xFF6E3A24), Color(0xFFFFE3D6), Color(0xFFFFAA80), Color(0xFFFF97BF))
    }
}

/** Colors beyond the Material scheme: glass surfaces, background glows and status colors. */
@Immutable
data class AuraColors(
    val isDark: Boolean,
    val background: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val glowTertiary: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val shadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val accent: AccentPalette,
    val style: AppStyle = AppStyle.GLASS,
    /** Multiplier for card shadows (0 = flat). */
    val elevationScale: Float = 1f,
    /** Card/tile border width in dp (0 = none). */
    val borderWidth: Float = 1f,
    /** Strength of the soft background glows. */
    val glowStrength: Float = 1f,
) {
    val accentGradient: Brush get() = Brush.linearGradient(listOf(accent.gradientStart, accent.gradientEnd))
}

/** Base palette for light/dark, then adjusted for the chosen [AppStyle]. */
fun auraColors(dark: Boolean, accent: AccentPalette, style: AppStyle = AppStyle.GLASS): AuraColors =
    baseColors(dark, accent).withStyle(style)

private fun AuraColors.withStyle(style: AppStyle): AuraColors = when (style) {
    AppStyle.GLASS -> copy(style = style)
    AppStyle.TRANSPARENT -> copy(
        style = style,
        glass = if (isDark) Color(0x17FFFFFF) else Color(0x5CFFFFFF),
        glassStrong = if (isDark) Color(0x2EFFFFFF) else Color(0xB3FFFFFF),
        glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0xF2FFFFFF),
        elevationScale = 0.15f,
        glowStrength = 1.15f,
    )
    AppStyle.SOLID -> copy(
        style = style,
        glass = if (isDark) Color(0xFF1E1B29) else Color.White,
        glassStrong = if (isDark) Color(0xFF24212F) else Color.White,
        glassBorder = Color.Transparent,
        borderWidth = 0f,
        elevationScale = 0.6f,
        glowStrength = 0.8f,
    )
    AppStyle.PASTEL -> copy(
        style = style,
        glass = if (isDark) accent.container.copy(alpha = 0.62f) else lerp(accent.container, Color.White, 0.3f).copy(alpha = 0.94f),
        glassStrong = if (isDark) accent.container.copy(alpha = 0.85f) else lerp(accent.container, Color.White, 0.55f),
        glassBorder = Color.Transparent,
        borderWidth = 0f,
        elevationScale = 0f,
        glowStrength = 1.1f,
    )
    AppStyle.MINIMAL -> copy(
        style = style,
        glass = Color.Transparent,
        glassStrong = if (isDark) Color(0xF2121019) else Color(0xF2F7F6FC),
        glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x2E1C1A2B),
        borderWidth = 1.2f,
        elevationScale = 0f,
        glowStrength = 0.45f,
    )
}

private fun baseColors(dark: Boolean, accent: AccentPalette): AuraColors = if (!dark) {
    AuraColors(
        isDark = false,
        background = Color(0xFFF7F6FC),
        glowPrimary = Color(0xFFE2DAFF),
        glowSecondary = Color(0xFFD6E5FF),
        glowTertiary = Color(0xFFF8DFF0),
        glass = Color(0xD9FFFFFF),
        glassStrong = Color(0xF2FFFFFF),
        glassBorder = Color(0xB3FFFFFF),
        shadow = Color(0xFF5B4BC4),
        textPrimary = Color(0xFF1C1A2B),
        textSecondary = Color(0xFF66627A),
        textTertiary = Color(0xFF73708A),
        divider = Color(0xFFE9E6F2),
        success = Color(0xFF1A7F5C),
        warning = Color(0xFFA86508),
        danger = Color(0xFFD23F4A),
        accent = accent,
    )
} else {
    AuraColors(
        isDark = true,
        background = Color(0xFF121019),
        glowPrimary = Color(0xFF2C2154),
        glowSecondary = Color(0xFF172A4F),
        glowTertiary = Color(0xFF3A1D3C),
        glass = Color(0xCC1E1B29),
        glassStrong = Color(0xF21E1B29),
        glassBorder = Color(0x14FFFFFF),
        shadow = Color(0xFF000000),
        textPrimary = Color(0xFFF1EFF8),
        textSecondary = Color(0xFFB0ACC4),
        textTertiary = Color(0xFF8A86A0),
        divider = Color(0xFF2C2939),
        success = Color(0xFF6FD6A8),
        warning = Color(0xFFF2B866),
        danger = Color(0xFFFF8A8F),
        accent = accent,
    )
}
