package com.aura.dashboard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aura.dashboard.data.apps.AppIconLoader
import com.aura.dashboard.domain.model.AppStyle
import com.aura.dashboard.domain.model.IconStyle
import com.aura.dashboard.ui.apps.AppGlyphs
import com.aura.dashboard.ui.theme.AuraTheme
import java.io.File

val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader> { error("AppIconLoader not provided") }

/** Whether Quick App icons are drawn in the app's style or use each app's original icon. */
val LocalIconStyle = staticCompositionLocalOf { IconStyle.THEMED }

/** An app's icon: themed to match the current appearance, or the real launcher icon. */
@Composable
fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    if (LocalIconStyle.current == IconStyle.THEMED) {
        ThemedAppIcon(packageName, label, modifier, size)
    } else {
        OriginalAppIcon(packageName, label, modifier, size)
    }
}

@Composable
private fun OriginalAppIcon(packageName: String, label: String, modifier: Modifier, size: Dp) {
    val loader = LocalAppIconLoader.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon: ImageBitmap? by produceState(loader.cached(packageName), packageName, sizePx) {
        value = loader.load(packageName, sizePx)
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        ThemedAppIcon(packageName, label, modifier, size)
    }
}

/**
 * A glyph tile that follows the app's appearance style and accent color, so the Quick Apps
 * grid looks like part of Aura rather than a mix of unrelated launcher icons.
 */
@Composable
fun ThemedAppIcon(packageName: String, label: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val colors = AuraTheme.colors
    val glyph = remember(packageName) { AppGlyphs.forPackage(packageName) }
    val accent = colors.accent
    val shape = if (colors.style == AppStyle.MINIMAL) CircleShape else RoundedCornerShape(size * 0.3f)
    val (background: Brush, tint: Color, border: Color?) = when (colors.style) {
        AppStyle.GLASS -> Triple(
            Brush.linearGradient(
                if (colors.isDark) listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f))
                else listOf(Color.White.copy(alpha = 0.95f), accent.container.copy(alpha = 0.75f)),
            ),
            accent.primary,
            if (colors.isDark) Color.White.copy(alpha = 0.14f) else Color.White,
        )
        AppStyle.TRANSPARENT -> Triple(
            Brush.linearGradient(listOf(Color.White.copy(alpha = if (colors.isDark) 0.08f else 0.35f), Color.Transparent)),
            colors.textPrimary,
            if (colors.isDark) Color.White.copy(alpha = 0.22f) else Color.White,
        )
        AppStyle.SOLID -> Triple(
            Brush.linearGradient(listOf(accent.gradientStart, accent.primary)),
            Color.White,
            null,
        )
        AppStyle.PASTEL -> {
            val hue = AppGlyphs.hueFor(packageName)
            Triple(
                Brush.linearGradient(
                    if (colors.isDark) listOf(Color.hsl(hue, 0.32f, 0.30f), Color.hsl(hue, 0.30f, 0.22f))
                    else listOf(Color.hsl(hue, 0.70f, 0.93f), Color.hsl(hue, 0.60f, 0.86f)),
                ),
                if (colors.isDark) Color.hsl(hue, 0.70f, 0.82f) else Color.hsl(hue, 0.55f, 0.36f),
                null,
            )
        }
        AppStyle.MINIMAL -> Triple(
            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            accent.primary,
            accent.primary.copy(alpha = 0.55f),
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            Icon(glyph, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        } else {
            Text(
                text = label.firstOrNull()?.uppercase() ?: "?",
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Profile picture, or initials on the accent gradient. */
@Composable
fun Avatar(name: String, imagePath: String?, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val colors = AuraTheme.colors
    val base = modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, Color.White.copy(alpha = if (colors.isDark) 0.15f else 0.9f), CircleShape)
    if (imagePath != null) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base.background(colors.accent.container),
        )
    } else {
        Box(
            modifier = base.background(Brush.linearGradient(listOf(colors.accent.gradientStart, colors.accent.gradientEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials(name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "✦"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
