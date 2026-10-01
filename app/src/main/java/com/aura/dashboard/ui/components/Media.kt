package com.aura.dashboard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.aura.dashboard.ui.theme.AuraTheme
import java.io.File

val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader> { error("AppIconLoader not provided") }

/** Real launcher icon of an installed app, or a lettered placeholder if unavailable. */
@Composable
fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val loader = LocalAppIconLoader.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon: ImageBitmap? by produceState(loader.cached(packageName), packageName, sizePx) {
        value = loader.load(packageName, sizePx)
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        LetterTile(label, modifier.size(size))
    }
}

@Composable
private fun LetterTile(label: String, modifier: Modifier) {
    val colors = AuraTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30))
            .background(Brush.linearGradient(listOf(colors.accent.container, colors.glowSecondary))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = colors.accent.onContainer,
        )
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
