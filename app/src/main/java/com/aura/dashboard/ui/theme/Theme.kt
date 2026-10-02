package com.aura.dashboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.aura.dashboard.domain.model.AccentColor
import com.aura.dashboard.domain.model.AppStyle
import com.aura.dashboard.domain.model.ThemeMode

private val LocalAuraColors = staticCompositionLocalOf { auraColors(false, accentPalette(AccentColor.LAVENDER, false)) }

val AuraShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object AuraTheme {
    val colors: AuraColors
        @Composable @ReadOnlyComposable get() = LocalAuraColors.current
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.LAVENDER,
    style: AppStyle = AppStyle.GLASS,
    content: @Composable () -> Unit,
) {
    val palette = remember(accent, darkTheme) { accentPalette(accent, darkTheme) }
    val colors = remember(palette, darkTheme, style) { auraColors(darkTheme, palette, style) }
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.container,
            onPrimaryContainer = palette.onContainer,
            secondary = palette.gradientEnd,
            secondaryContainer = palette.container,
            onSecondaryContainer = palette.onContainer,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.background,
            onSurface = colors.textPrimary,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF262335),
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF15131D),
            surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF1A1824),
            surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1E1B29),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF24212F),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF2A2736),
            outline = androidx.compose.ui.graphics.Color(0xFF4A4660),
            outlineVariant = colors.divider,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.container,
            onPrimaryContainer = palette.onContainer,
            secondary = palette.gradientEnd,
            secondaryContainer = palette.container,
            onSecondaryContainer = palette.onContainer,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.background,
            onSurface = colors.textPrimary,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEFEDF7),
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
            surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFFBFAFE),
            surfaceContainer = androidx.compose.ui.graphics.Color(0xFFF7F5FD),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFF1EFF8),
            outline = androidx.compose.ui.graphics.Color(0xFFC9C5D8),
            outlineVariant = colors.divider,
            error = colors.danger,
        )
    }
    CompositionLocalProvider(LocalAuraColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AuraTypography,
            shapes = AuraShapes,
            content = content,
        )
    }
}
