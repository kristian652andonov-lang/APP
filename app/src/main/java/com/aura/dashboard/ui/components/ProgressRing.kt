package com.aura.dashboard.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.dashboard.ui.theme.AuraTheme

/** Circular progress with a gradient arc that animates to its value. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    strokeWidth: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = AuraTheme.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progressRing",
    )
    Box(
        modifier = modifier
            .size(size)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            drawArc(
                color = colors.accent.primary.copy(alpha = if (colors.isDark) 0.22f else 0.14f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(stroke),
            )
            if (animated > 0f) {
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(colors.accent.gradientStart, colors.accent.gradientEnd, colors.accent.gradientStart),
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        content()
    }
}
