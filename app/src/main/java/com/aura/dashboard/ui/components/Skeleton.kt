package com.aura.dashboard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aura.dashboard.ui.theme.AuraTheme

/** A soft sweeping highlight used for loading placeholders. */
fun Modifier.shimmer(): Modifier = composed {
    val colors = AuraTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    val base = if (colors.isDark) Color.White.copy(alpha = 0.06f) else colors.accent.primary.copy(alpha = 0.07f)
    val highlight = if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.85f)
    drawWithContent {
        val width = size.width
        drawRect(base)
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(width * progress - width * 0.5f, 0f),
                end = Offset(width * progress + width * 0.5f, size.height),
            ),
        )
    }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp)) {
    Box(modifier.clip(shape).background(Color.Transparent).shimmer())
}
